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
	/** R is started when it is used (e.g., a script, a chunk, the API), as GDAL is loaded. */
	public static final String R_ON_USE = "START_R_ON_USE";
	/** Python is started when it is used (e.g., a script, a chunk, the API). */
	public static final String PYTHON_ON_USE = "START_PYTHON_ON_USE";

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

	/**
	 * Starts R if it is not running and the preference "Start R when it is used"
	 * is set (as the toolbar action, in a job: the user interface stays usable).
	 *
	 * @return true if R runs
	 */
	public static boolean startROnUse() {
		if (com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			return true;
		}
		if (!option(R_ON_USE)) {
			return false;
		}
		return startR();
	}

	/**
	 * Starts R if it is not running (without the preference), as the toolbar
	 * action: the console reads its input for R (e.g., Return for the next plot).
	 *
	 * @return true if R runs
	 */
	public static boolean startR() {
		if (com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			return true;
		}
		if (startWithConsole(true)) {
			return com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning();
		}
		String error = BackgroundStart.run("Starting R", com.eco.bio7.rbridge.backend.Backends::startEmbedded);
		if (error != null) {
			System.err.println("R could not be started: " + error);
			return false;
		}
		return true;
	}

	/**
	 * Starts the embedded Python if it is not running and the preference "Start
	 * Python when it is used" is set (in a job: the user interface stays usable).
	 *
	 * @return null if Python runs, else the reason
	 */
	public static String startPythonOnUse() {
		if (com.eco.bio7.pybridge.EmbeddedPython.isRunning()) {
			return null;
		}
		if (!option(PYTHON_ON_USE)) {
			return NOT_RUNNING_PYTHON;
		}
		/* As the toolbar action: the console reads its input for Python (e.g., input()) */
		if (startWithConsole(false)) {
			return com.eco.bio7.pybridge.EmbeddedPython.isRunning() ? null : "Python could not be started (see the console)";
		}
		return BackgroundStart.run("Starting Python", com.eco.bio7.pybridge.EmbeddedPython::start);
	}

	/*
	 * Starts R or Python with the console of Bio7 (in its thread, the start itself
	 * runs in a job): the console switches to the language as with the toolbar
	 * actions. Returns false if the console is not available.
	 */
	private static boolean startWithConsole(boolean r) {
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return false;
		}
		boolean[] done = new boolean[1];
		Runnable start = () -> {
			ConsoleInterpreterAction console = ConsoleInterpreterAction.getInstance();
			if (console == null) {
				return;
			}
			if (r) {
				console.startR();
			} else {
				console.startPython();
			}
			done[0] = true;
		};
		if (display.getThread() == Thread.currentThread()) {
			start.run();
		} else {
			display.syncExec(start);
		}
		return done[0];
	}

	/** The message if R is not running (and not started when it is used). */
	public static final String NOT_RUNNING_R = "R is not running!\nPlease start R (toolbar action \"Start R\") or activate \"Start R when it is used\" (Preferences > Bio7 > Startup).";
	/** The message if Python is not running (and not started when it is used). */
	public static final String NOT_RUNNING_PYTHON = "Python is not running!\nPlease start Python (toolbar action \"Start Python\") or activate \"Start Python when it is used\" (Preferences > Bio7 > Startup).";

	/** A preference of the starts (true without the plugin). */
	public static boolean option(String key) {
		Bio7Plugin plugin = Bio7Plugin.getDefault();
		return plugin == null || plugin.getPreferenceStore().getBoolean(key);
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
