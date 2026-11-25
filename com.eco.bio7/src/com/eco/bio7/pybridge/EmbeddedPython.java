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
		/* The first start can take a while (the probe of Python, libpython, the bootstrap): shown in the status bar */
		status("Starting the embedded Python (" + exe + ") ...", false);
		/* Linux: the C++ runtime of the selected R too (the newest is loaded first, e.g., R of conda) */
		try {
			String rHome = com.eco.bio7.rbridge.RLocator.resolveHome(store.getString(com.eco.bio7.preferences.PreferenceConstants.PATH_R));
			if (rHome != null && !rHome.isEmpty()) {
				NativeLibDirs.addR(rHome);
			}
		} catch (RuntimeException e) {
			/* No R: only the runtimes of Python and the system */
		}
		PySession session;
		try {
			session = PySession.start(PyStartOptions.of(Path.of(exe)).withConsole((text, error) -> print(text, error)));
		} catch (PyException e) {
			status("The embedded Python could not be started (see the console).", true);
			return e.getMessage();
		} catch (LinkageError e) {
			status("The embedded Python could not be started (see the console).", true);
			return "The embedded Python needs Java 22 or later with --enable-native-access=ALL-UNNAMED (" + e + ")";
		}
		status("Embedded Python " + session.version() + " started.", true);
		session.setWaitLimit(() -> Display.getCurrent() != null, UI_WAIT_LIMIT_MS);
		session.setPlotListener(PythonPlotView.listener());
		session.setCallHandler(new PythonCalls());
		/* The simulation API (from bio7sim import *) */
		com.eco.bio7.simulation.ScriptSimulation.installPython(session);
		/* The variables of the Python-Shell after the console and scripts (as for R) */
		session.setActivityListener(PythonShellView::refreshOpenViews);
		/* A job while a script or a long console line runs (status bar, Progress view, stop button) */
		session.setRunListener(new PythonJobs());
		Display display = Display.getDefault();
		display.syncExec(() -> PythonPlotView.initSize(session));
		/* The preferences of the plot view (Preferences Python Plot) */
		PythonPlotView.applyOptions(session);
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
		/*
		 * Not the canonical paths: python of a venv is a link to the base Python
		 * (Linux, macOS), the venv is only used if Python is started with its path
		 */
		Path sel = Path.of(selected).toAbsolutePath().normalize();
		Path run = s.executable().toAbsolutePath().normalize();
		if (!sel.equals(run)) {
			print("Note: the embedded Python runs with " + s.executable() + ". The selected Python (" + selected + ") is used after a restart of Bio7.\n", true);
		}
	}

	/* The message shown last by status (a later message isn't cleared by an earlier timer) */
	private static volatile String statusMessage;

	/*
	 * A message in the status bar of the workbench window (any thread). In the UI
	 * thread it is painted at once (the start of Python blocks the UI thread
	 * afterwards). clearLater: removed after a few seconds.
	 */
	private static void status(String message, boolean clearLater) {
		statusMessage = message;
		Runnable show = () -> {
			org.eclipse.jface.action.IStatusLineManager line = statusLine();
			if (line == null) {
				return;
			}
			line.setMessage(message);
			line.update(true);
			org.eclipse.ui.IWorkbenchWindow w = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (w != null && w.getShell() != null && !w.getShell().isDisposed()) {
				/* Painted before the UI thread is blocked */
				w.getShell().update();
			}
			if (clearLater) {
				Display.getCurrent().timerExec(6000, () -> {
					if (message.equals(statusMessage)) {
						org.eclipse.jface.action.IStatusLineManager l = statusLine();
						if (l != null) {
							l.setMessage(null);
						}
					}
				});
			}
		};
		try {
			if (!org.eclipse.ui.PlatformUI.isWorkbenchRunning()) {
				return;
			}
			Display display = org.eclipse.ui.PlatformUI.getWorkbench().getDisplay();
			if (Display.getCurrent() == display) {
				show.run();
			} else {
				/* Not syncExec: the UI thread may wait for this thread */
				display.asyncExec(show);
			}
		} catch (RuntimeException e) {
			/* No status bar */
		}
	}

	/* The status line of the workbench window (of its active part; UI thread), null if none */
	private static org.eclipse.jface.action.IStatusLineManager statusLine() {
		org.eclipse.ui.IWorkbenchWindow w = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (w == null && org.eclipse.ui.PlatformUI.getWorkbench().getWorkbenchWindowCount() > 0) {
			w = org.eclipse.ui.PlatformUI.getWorkbench().getWorkbenchWindows()[0];
		}
		org.eclipse.ui.IWorkbenchPage page = w == null ? null : w.getActivePage();
		org.eclipse.ui.IWorkbenchPart part = page == null ? null : page.getActivePart();
		if (part == null) {
			return null;
		}
		org.eclipse.ui.IWorkbenchPartSite site = part.getSite();
		if (site instanceof org.eclipse.ui.IViewSite v) {
			return v.getActionBars().getStatusLineManager();
		}
		if (site instanceof org.eclipse.ui.IEditorSite e) {
			return e.getActionBars().getStatusLineManager();
		}
		return null;
	}

	/**
	 * Makes the packages of an environment (venv) importable in the running
	 * embedded Python (a new Bio7 environment: no restart needed to import its
	 * packages). Only if the environment has the same base Python and version as
	 * the running Python: its site-packages are put in front of sys.path. Not in
	 * the UI thread.
	 *
	 * @return true if the packages can be imported now (or it is the running Python)
	 */
	public static boolean addEnvironment(String executable) {
		PySession s = PySession.current();
		if (s == null) {
			return false;
		}
		if (Path.of(executable).toAbsolutePath().normalize().equals(s.executable().toAbsolutePath().normalize())) {
			return true;
		}
		try {
			PythonEnvironment.Result r = PythonEnvironment.runCode(executable,
					"import sys, site, json\nprint('BIO7SITE' + json.dumps({'base': sys.base_prefix, 'ver': list(sys.version_info[:2]), 'site': site.getsitepackages()}))", null);
			int at = r.output().indexOf("BIO7SITE");
			if (at < 0) {
				return false;
			}
			s.assign("_bio7_env", com.eco.bio7.pyffm.PyValue.of(r.output().substring(at + 8).strip()));
			s.exec("import sys as _s, json as _j, importlib as _il\n_e = _j.loads(_bio7_env)\n"
					+ "_bio7_env_ok = _e['base'] == _s.base_prefix and _e['ver'] == list(_s.version_info[:2])\n"
					+ "if _bio7_env_ok:\n    for _p in reversed(_e['site']):\n        if _p not in _s.path:\n            _s.path.insert(0, _p)\n    _il.invalidate_caches()\n"
					+ "del _s, _j, _il, _e, _bio7_env");
			com.eco.bio7.pyffm.PyValue ok = s.eval("'yes' if _bio7_env_ok else 'no'");
			s.exec("del _bio7_env_ok");
			return ok instanceof com.eco.bio7.pyffm.PyValue.Str str && "yes".equals(str.asString());
		} catch (Exception e) {
			return false;
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

	/** Calls r when the code queued before (e.g., runFile) has finished; at once without Python. */
	public static void whenDone(Runnable r) {
		PySession s = session();
		if (s != null) {
			s.whenDone(r);
		} else {
			r.run();
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
