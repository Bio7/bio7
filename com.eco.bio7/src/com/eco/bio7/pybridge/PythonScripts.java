/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.pybridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.console.PythonFinder;

/**
 * Runs a Python script file (the script actions, the scripts of the R-Shell,
 * the startup scripts, batch files): in the embedded Python if it is selected
 * in the preferences (started if necessary), else with the Python of the
 * preferences as a separate process (the output in the console).
 */
public final class PythonScripts {

	private PythonScripts() {
	}

	/** Runs a script file (returns at once). */
	public static void runFile(String path) {
		if (EmbeddedPython.isSelected()) {
			Display display = Display.getDefault();
			Runnable start = () -> {
				/* The console with the embedded Python (started if not running) */
				com.eco.bio7.console.ConsoleInterpreterAction.getInstance().startPython();
				if (EmbeddedPython.isRunning()) {
					EmbeddedPython.runFile(path);
					EmbeddedPython.prompt();
				}
			};
			if (display.getThread() == Thread.currentThread()) {
				start.run();
			} else {
				display.asyncExec(start);
			}
			return;
		}
		runProcess(path);
	}

	/* The Python of the preferences in a process: the output in the console, the job can stop it */
	private static void runProcess(String path) {
		String folder = Bio7Plugin.getDefault().getPreferenceStore().getString("python_pipe_path");
		String exe = PythonFinder.executable(folder);
		Job job = new Job("Python: " + new File(path).getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Python process running...", IProgressMonitor.UNKNOWN);
				Process proc;
				try {
					ProcessBuilder pb = new ProcessBuilder(exe, "-u", path);
					File dir = new File(path).getAbsoluteFile().getParentFile();
					if (dir != null) {
						pb.directory(dir);
					}
					pb.redirectErrorStream(true);
					proc = pb.start();
				} catch (IOException e) {
					System.out.println("Python could not be started (" + exe + "): " + e.getMessage()
							+ "\nPlease select the Python folder in the preferences (Preferences Python: 'Detect Python...').");
					return Status.OK_STATUS;
				}
				Process p = proc;
				Thread reader = new Thread(() -> {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
						String line;
						while ((line = in.readLine()) != null) {
							System.out.println(line);
						}
					} catch (IOException e) {
						/* The process has ended */
					}
				}, "Python process output");
				reader.setDaemon(true);
				reader.start();
				while (p.isAlive()) {
					if (monitor.isCanceled()) {
						p.descendants().forEach(ProcessHandle::destroy);
						p.destroy();
						System.out.println("Python process stopped.");
						return Status.CANCEL_STATUS;
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				try {
					reader.join(2000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(false);
		job.schedule();
	}
}
