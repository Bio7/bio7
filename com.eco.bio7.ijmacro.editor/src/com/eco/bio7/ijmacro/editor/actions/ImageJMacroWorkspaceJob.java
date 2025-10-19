/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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
package com.eco.bio7.ijmacro.editor.actions;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import ij.IJ;

public class ImageJMacroWorkspaceJob extends Job implements IJobChangeListener {

	private String content;
	private IPreferenceStore store;
	private String path;
	private Process process;

	public ImageJMacroWorkspaceJob(String content, String path) {
		super("ImageJ macro in progress....");
		this.content = content;
		this.path = path;
		store = IJMacroEditorPlugin.getDefault().getPreferenceStore();

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("ImageJ macro is running.....", IProgressMonitor.UNKNOWN);
		boolean evalExt = store.getBoolean("EVALUATE_EXTERNAL");
		String ijpath = store.getString("LOCATION_EXTERNAL");
		String javaArgs = store.getString("OPTIONS_EXTERNA");
		if (evalExt) {

			try {
				process = new ProcessBuilder(ijpath, javaArgs, path).start();

				// get the error stream of the process and print it
				InputStream error = process.getErrorStream();
				for (int i = 0; i < error.available(); i++) {
					System.out.println("" + error.read());
				}

				try {
					while (!process.waitFor(1, TimeUnit.SECONDS)) {
						// check for cancelling!
						if (monitor.isCanceled()) {
							process.destroy();
							return Status.CANCEL_STATUS;
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			IJ.runMacro(content);
		}

		if (monitor.isCanceled()) {
			try {
				process.destroy();
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
			return Status.CANCEL_STATUS;
		}

		return Status.OK_STATUS;
	}

	public void aboutToRun(IJobChangeEvent event) {

	}

	public void awake(IJobChangeEvent event) {

	}

	public void done(IJobChangeEvent event) {

	}

	public void running(IJobChangeEvent event) {

	}

	public void scheduled(IJobChangeEvent event) {

	}

	public void sleeping(IJobChangeEvent event) {

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("ImageJ macro is running. Press Escape on panel to abort!.....", IProgressMonitor.UNKNOWN);

		IJ.runMacro(content);
		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
	}
		return Status.OK_STATUS;
	}
}
