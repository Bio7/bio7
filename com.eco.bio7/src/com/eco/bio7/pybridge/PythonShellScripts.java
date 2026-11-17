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

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.JavaScriptInterpreter;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.jobs.ImageMacroWorkspaceJob;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rbridge.RState;

/**
 * The scripts of the context menu of the Python-Shell (a folder of the
 * preferences, as in the R-Shell): a folder is a submenu, a file an action
 * ('_' in a name: a space; names starting with '_' are hidden). Python scripts
 * get the names of the selected objects in the list bio7_shell_vars.
 */
final class PythonShellScripts {

	private static final String TYPES = ".*\\.(py|R|r|ijm|groovy|bsh|js|txt)";

	private PythonShellScripts() {
	}

	/** True if the folder contains scripts (or subfolders). */
	static boolean hasScripts(File folder) {
		File[] files = folder.listFiles();
		return files != null && Arrays.stream(files).anyMatch(PythonShellScripts::shown);
	}

	/** Creates the submenus and actions of the folder. */
	static void createMenus(Menu menu, File folder, Consumer<File> action) {
		File[] files = folder.listFiles();
		if (files == null) {
			return;
		}
		Arrays.sort(files, Comparator.comparing((File f) -> f.isFile()).thenComparing(f -> f.getName().toLowerCase()));
		for (File f : files) {
			if (!shown(f)) {
				continue;
			}
			if (f.isDirectory()) {
				MenuItem item = new MenuItem(menu, SWT.CASCADE);
				item.setText(f.getName().replace("_", " "));
				Menu sub = new Menu(menu);
				item.setMenu(sub);
				createMenus(sub, f, action);
			} else {
				String name = f.getName();
				MenuItem item = new MenuItem(menu, SWT.PUSH);
				item.setText(name.substring(0, name.lastIndexOf('.')).replace("_", " "));
				item.addListener(SWT.Selection, _ -> action.accept(f));
			}
		}
	}

	private static boolean shown(File f) {
		String name = f.getName();
		if (name.startsWith("_") || name.startsWith(".")) {
			return false;
		}
		return f.isDirectory() || name.matches(TYPES);
	}

	/** Runs a script which is not a Python script with its interpreter (R, ImageJ macro, Groovy, BeanShell, JavaScript) or opens a text. */
	static void run(File file) {
		String name = file.getName();
		String path = file.getAbsolutePath();
		if (name.endsWith(".R") || name.endsWith(".r")) {
			if (!RTools.isAliveDialog()) {
				return;
			}
			if (RState.isBusy()) {
				Bio7Dialog.message("R is busy!");
				return;
			}
			RState.setBusy(true);
			RInterpreterJob job = new RInterpreterJob(null, path);
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					RState.setBusy(false);
					if (RTools.getDisplayNumber() > 0) {
						RTools.closeAndDisplay();
					}
				}
			});
			job.setUser(true);
			job.schedule();
		} else if (name.endsWith(".ijm")) {
			new ImageMacroWorkspaceJob(file).schedule();
		} else if (name.endsWith(".groovy")) {
			GroovyInterpreter.interpretJob(null, path);
		} else if (name.endsWith(".bsh")) {
			BeanShellInterpreter.interpretJob(null, path);
		} else if (name.endsWith(".js")) {
			JavaScriptInterpreter.interpretJob(null, path);
		} else if (name.endsWith(".txt")) {
			try {
				IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
						EFS.getLocalFileSystem().getStore(file.toURI()));
			} catch (Exception e) {
				Bio7Dialog.message(e.getMessage());
			}
		}
	}
}
