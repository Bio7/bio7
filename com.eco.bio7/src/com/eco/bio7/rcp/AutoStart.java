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
package com.eco.bio7.rcp;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.console.ConsoleInterpreterAction;

/**
 * Starts R and/or Python when Bio7 has started (preferences). The starts run
 * as for the buttons of the main toolbar: in a job, the user interface stays
 * usable. R and Python start one after the other (both set the environment and
 * the locale of the process at their start); the language of the console
 * (preference) starts last, its prompt is shown in the console.
 */
public final class AutoStart {

	public static final String START_R = "START_R_AT_STARTUP";
	public static final String START_PYTHON = "START_PYTHON_AT_STARTUP";
	/** "R" or "Python": the interpreter of the console after the start. */
	public static final String CONSOLE = "STARTUP_CONSOLE_INTERPRETER";

	/* The workbench is painted first */
	private static final int DELAY_MS = 800;
	/* The console view is created (at most 10 s) */
	private static final int RETRY_MS = 500;
	private static final int ATTEMPTS = 20;

	private AutoStart() {
	}

	/** Schedules the starts of the preferences (UI thread, after the workbench window is open). */
	public static void schedule(Display display) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean r = store.getBoolean(START_R);
		boolean python = store.getBoolean(START_PYTHON);
		if (!r && !python) {
			return;
		}
		boolean pythonLast = "Python".equals(store.getString(CONSOLE));
		display.timerExec(DELAY_MS, () -> start(display, r, python, pythonLast, 0));
	}

	private static void start(Display display, boolean r, boolean python, boolean pythonLast, int attempt) {
		if (display.isDisposed()) {
			return;
		}
		ConsoleInterpreterAction console = ConsoleInterpreterAction.getInstance();
		if (console == null) {
			/* The console of Bio7 is not shown yet: shown, then again */
			if (attempt == 0) {
				showConsole();
			}
			if (attempt < ATTEMPTS) {
				display.timerExec(RETRY_MS, () -> start(display, r, python, pythonLast, attempt + 1));
			} else {
				System.err.println("R and Python were not started at startup: the console of Bio7 is not available.");
			}
			return;
		}
		/* One after the other (each in a job, the user interface stays usable) */
		if (pythonLast) {
			if (r) {
				console.startR();
			}
			if (python) {
				console.startPython();
			}
		} else {
			if (python) {
				console.startPython();
			}
			if (r) {
				console.startR();
			}
		}
	}

	private static void showConsole() {
		try {
			StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
			if (utils != null && utils.cons != null) {
				org.eclipse.ui.console.ConsolePlugin.getDefault().getConsoleManager().showConsoleView(utils.cons);
			}
		} catch (RuntimeException e) {
			/* No console view in the perspective */
		}
	}
}
