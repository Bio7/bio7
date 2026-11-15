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
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.console.PythonFinder;
import com.eco.bio7.pyffm.PyException;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyStartOptions;

/**
 * The embedded Python of Bio7 (CPython in the process of Bio7, see the plugin
 * com.eco.bio7.pyffm): the Python of the preferences, the output in the Bio7
 * console, matplotlib plots in the Python plot view, the module bio7 for the
 * data of ImageJ and R.
 */
public final class EmbeddedPython {

	/** The time the UI thread waits at most for Python to become free (milliseconds). */
	public static final long UI_WAIT_LIMIT_MS = 5_000;

	/** The preference: run Python embedded in Bio7 (else as a separate process). */
	public static final String PREF_EMBEDDED = "python_embedded";

	private static IOConsoleOutputStream out;
	private static IOConsoleOutputStream err;

	private EmbeddedPython() {
	}

	/** Returns true if the embedded Python is selected in the preferences. */
	public static boolean isSelected() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		return store.getBoolean(PREF_EMBEDDED);
	}

	/** Returns true if the embedded Python runs. */
	public static boolean isRunning() {
		return isAvailable() && PySession.current() != null;
	}

	/* The plugin of the embedded Python is optional */
	static boolean isAvailable() {
		try {
			Class.forName("com.eco.bio7.pyffm.PySession");
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}

	/** The session or null. */
	public static PySession session() {
		return isRunning() ? PySession.current() : null;
	}

	/** Returns true if Python evaluates. */
	public static boolean isBusy() {
		PySession s = session();
		return s != null && s.isBusy();
	}

	/**
	 * Starts the embedded Python (if not running) with the Python of the
	 * preferences.
	 *
	 * @return null if it runs, else the reason
	 */
	public static synchronized String start() {
		if (!isAvailable()) {
			return "The plugin of the embedded Python (com.eco.bio7.pyffm) is not installed.";
		}
		if (PySession.current() != null) {
			checkSelection();
			return null;
		}
		if (PySession.wasStarted()) {
			return "The embedded Python could not be started. It can only be started again after a restart of Bio7.";
		}
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String folder = store.getString("python_pipe_path");
		String exe = PythonFinder.executable(folder);
		if (!new File(exe).isAbsolute()) {
			return "Python 3 was not found. Please select the Python folder in the preferences (Preferences Python: 'Detect Python...').";
		}
		if (!folder.isBlank() && PythonFinder.executableIn(folder) == null) {
			print("No Python was found in the selected folder " + folder + " (Python preferences). Using " + exe + "\n", true);
		}
		connectConsole();
		PySession session;
		try {
			session = PySession.start(PyStartOptions.of(Path.of(exe)).withConsole((text, error) -> print(text, error)));
		} catch (PyException e) {
			return e.getMessage();
		} catch (LinkageError e) {
			return "The embedded Python needs Java 22 or later with --enable-native-access=ALL-UNNAMED (" + e + ")";
		}
		session.setWaitLimit(() -> Display.getCurrent() != null, UI_WAIT_LIMIT_MS);
		session.setPlotListener(PythonPlotView.listener());
		session.setCallHandler(new PythonCalls());
		/* The variables of the Python-Shell after the console and scripts (as for R) */
		session.setActivityListener(PythonShellView::refreshOpenViews);
		/* A job while a script or a long console line runs (status bar, Progress view, stop button) */
		session.setRunListener(new PythonJobs());
		Display display = Display.getDefault();
		display.syncExec(() -> PythonPlotView.initSize(session));
		print("Embedded Python " + session.version() + " (" + exe + ")\n", false);
		return null;
	}

	/*
	 * The embedded Python can't be changed while Bio7 runs: a note if another
	 * Python is selected in the preferences
	 */
	private static void checkSelection() {
		PySession s = PySession.current();
		String folder = Bio7Plugin.getDefault().getPreferenceStore().getString("python_pipe_path");
		String selected = PythonFinder.executableIn(folder);
		if (s == null || selected == null) {
			return;
		}
		try {
			if (!new File(selected).getCanonicalPath().equals(s.executable().toFile().getCanonicalPath())) {
				print("Note: the embedded Python runs with " + s.executable() + ". The selected Python (" + selected
						+ ") is used after a restart of Bio7.\n", true);
			}
		} catch (IOException e) {
			/* No note */
		}
	}

	/* The output of Python in the console of Bio7 (thread-safe streams of the IOConsole) */
	private static void connectConsole() {
		ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
		if (cpp == null || cpp.getIoc() == null) {
			return;
		}
		IOConsole ioc = cpp.getIoc();
		out = ioc.newOutputStream();
		err = ioc.newOutputStream();
		Display display = Display.getDefault();
		display.asyncExec(() -> err.setColor(display.getSystemColor(SWT.COLOR_RED)));
	}

	/** Prints to the console of Bio7 (errors red). */
	public static void print(String text, boolean error) {
		IOConsoleOutputStream s = error ? err : out;
		if (s == null) {
			(error ? System.err : System.out).print(text);
			return;
		}
		try {
			s.write(text);
		} catch (IOException e) {
			/* The console was closed */
		}
	}

	/** A line of the console (statements over several lines are completed by the next lines). */
	public static void console(String line) {
		/* The debugger stopped: the line is evaluated in the frame (or a command n, s, r, c, q) */
		if (com.eco.bio7.pybridge.debug.PythonDebugger.isPaused()) {
			com.eco.bio7.pybridge.debug.PythonDebugger.consoleLine(line);
			return;
		}
		PySession s = session();
		if (s != null) {
			s.console(line);
		}
	}

	/** Writes the prompt of the Python console. */
	public static void prompt() {
		PySession s = session();
		if (s != null) {
			s.prompt();
		}
	}

	/** Runs a script file (errors are printed to the console). */
	public static void runFile(String file) {
		PySession s = session();
		if (s != null) {
			s.runFile(Path.of(file));
		}
	}

	/** Interrupts the running Python code (KeyboardInterrupt). */
	public static void interrupt() {
		PySession s = session();
		if (s != null) {
			s.interrupt();
		}
	}
}
