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

package com.eco.bio7.rbridge.backend;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RConfig;
import com.eco.bio7.rbridge.RLocator;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.plot.RPlotCanvasDevice;
import com.eco.bio7.rffm.RConsole;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RSession;
import com.eco.bio7.rffm.RStartOptions;

/**
 * Embedded R in the console of Bio7 (experimental): starts R from the
 * installation of the preferences, connects the console (output, prompts,
 * input, interrupt) and registers the device of the ImageJ plot canvas.
 */
public final class EmbeddedR {

	/** The R function of the plot canvas device (options(device = ...)). */
	public static final String CANVAS_DEVICE = "bio7PlotCanvas";
	public static final String VECTOR_DEVICE = "bio7PlotVector";

	private static IOConsoleOutputStream out;
	/* The messages of R (the error stream of R: e.g., loading packages) */
	private static IOConsoleOutputStream message;
	private static IOConsoleOutputStream warning;
	private static IOConsoleOutputStream err;

	private EmbeddedR() {
	}

	/**
	 * Starts embedded R (in the UI thread or any thread).
	 *
	 * @return null if R runs, else the error
	 */
	static synchronized String start() {
		if (Backends.embedded() != null) {
			return null;
		}
		if (RSession.wasStarted()) {
			return "Embedded R was stopped and can only be started again after a restart of Bio7.";
		}
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String preferred = store.getString(PreferenceConstants.PATH_R);
		String home = RLocator.resolveHome(preferred);
		if (home == null) {
			return "R was not found (see 'R and Rserve Setup' in the R preferences).";
		}
		if (home.isEmpty()) {
			/* The R of the PATH: its home */
			RLocator.RInstallation r = RLocator.probe("");
			if (!r.isValid()) {
				return "R of the PATH could not be started: " + r.error();
			}
			home = r.home();
		}
		connectConsole();
		/* The size of the ImageJ view for the plot canvas device */
		Display.getDefault().syncExec(RPlotCanvasDevice::updateViewSize);
		RStartOptions options = RStartOptions.of(Path.of(home)).withConsole(new RConsole() {
			@Override
			public void write(String text, boolean error) {
				print(error ? message : out, text, error);
			}

			@Override
			public void writeError(String text) {
				print(err, text, true);
			}

			@Override
			public void writeWarning(String text) {
				print(warning, text, true);
			}
		});
		RSession session;
		try {
			session = RSession.start(options);
		} catch (RException e) {
			return e.getMessage();
		}
		EmbeddedRBackend backend = new EmbeddedRBackend(session);
		Backends.setEmbedded(backend);
		/* The R editor evaluates with the same connection */
		com.eco.bio7.reditors.REditor.setEvaluatorSupplier(com.eco.bio7.rbridge.RBridge::getBackend);
		com.eco.bio7.worldwind.WorldWindView.setEvaluatorSupplier(com.eco.bio7.rbridge.RBridge::getBackend);
		try {
			session.registerDevice(CANVAS_DEVICE, RPlotCanvasDevice.factory(session));
			session.registerDevice(VECTOR_DEVICE, com.eco.bio7.rbridge.plot.RPlotVectorDevice.factory(session));
		} catch (RException | LinkageError e) {
			print("The plot devices could not be registered: " + e.getMessage() + "\n", true);
		}
		RServe.setRrunning(true);
		RConfig.config(backend);
		print("Embedded R " + session.version() + " (experimental) in " + home + "\n", false);
		return null;
	}

	/* The output of R in the console of Bio7 (thread-safe streams of the IOConsole) */
	private static void connectConsole() {
		ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
		if (cpp == null || cpp.getIoc() == null) {
			return;
		}
		IOConsole ioc = cpp.getIoc();
		out = ioc.newOutputStream();
		message = ioc.newOutputStream();
		warning = ioc.newOutputStream();
		err = ioc.newOutputStream();
		Display display = Display.getDefault();
		/* Errors red, warnings orange; the messages (e.g., of loading packages) as the output */
		display.asyncExec(() -> {
			err.setColor(display.getSystemColor(SWT.COLOR_RED));
			warning.setColor(new org.eclipse.swt.graphics.Color(230, 140, 0));
		});
	}

	private static void print(String text, boolean error) {
		print(error ? err : out, text, error);
	}

	private static void print(IOConsoleOutputStream s, String text, boolean error) {
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

	/** Returns true if embedded R runs. */
	public static boolean isRunning() {
		return Backends.embedded() != null;
	}

	/** A line of the console of Bio7 for embedded R. */
	public static void console(String line) {
		if (Backends.embedded() instanceof EmbeddedRBackend b) {
			b.session().console(line);
		}
	}

	/** Interrupts embedded R (like Ctrl+C). */
	public static void interrupt() {
		if (Backends.embedded() instanceof EmbeddedRBackend b) {
			b.session().interrupt();
		}
	}

	/** Ends embedded R (when Bio7 ends): R can't be started again in this process. */
	public static void close() {
		if (Backends.embedded() instanceof EmbeddedRBackend b) {
			Backends.setEmbedded(null);
			b.session().close();
		}
	}

	/**
	 * Sends a console line and returns the output of R until R asks for the next
	 * input (e.g., a step of the debugger); null if embedded R doesn't run.
	 */
	public static String consoleCapture(String line, long timeoutMillis, boolean echo) {
		return consoleCapture(line, timeoutMillis, echo, null);
	}

	/** As consoleCapture; the output shown in the console passes the filter. */
	public static String consoleCapture(String line, long timeoutMillis, boolean echo, java.util.function.UnaryOperator<String> filter) {
		if (Backends.embedded() instanceof EmbeddedRBackend b) {
			try {
				return b.session().consoleCapture(line, timeoutMillis, echo, filter);
			} catch (RException e) {
				return null;
			}
		}
		return null;
	}

	/** Returns true if embedded R waits for console input within an evaluation (browser()). */
	public static boolean isBrowsing() {
		return Backends.embedded() instanceof EmbeddedRBackend b && b.session().isWaitingForConsoleInput();
	}

	/** The session or null. */
	public static RSession session() {
		return Backends.embedded() instanceof EmbeddedRBackend b ? b.session() : null;
	}
}
