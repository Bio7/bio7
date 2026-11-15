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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.swt.widgets.Display;
import org.rosuda.REngine.REXP;

import com.eco.bio7.pyffm.PyCallHandler;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rbridge.backend.RBackend;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.ResultsTable;

/**
 * The functions of Bio7 for the embedded Python (the module bio7): the data of
 * ImageJ and R. Called in the thread of Python. Further functions can be
 * registered (e.g., by plugins or scripts) with {@link #register}.
 */
public final class PythonCalls implements PyCallHandler {

	/** A function for Python: string arguments and an optional value. */
	@FunctionalInterface
	public interface Function {
		PyValue call(String[] args, PyValue value) throws Exception;
	}

	private static final Map<String, Function> FUNCTIONS = new ConcurrentHashMap<>();

	/**
	 * Registers a function callable in Python with bio7.call(name, *args,
	 * value=...).
	 */
	public static void register(String name, Function function) {
		FUNCTIONS.put(name, function);
	}

	@Override
	public PyValue call(String function, String[] args, PyValue value) throws Exception {
		switch (function) {
		case "image": {
			ImagePlus imp = image(arg(args, 0));
			return PythonConvert.image(imp);
		}
		case "show_image": {
			String title = arg(args, 0);
			ImagePlus imp = PythonConvert.toImage(value, title == null ? "Python" : title);
			/* Shown before the call returns (as ImagePlus.show()): a following macro or bio7.image() finds it */
			Display display = Display.getDefault();
			if (display.getThread() == Thread.currentThread()) {
				imp.show();
			} else {
				display.syncExec(imp::show);
			}
			return null;
		}
		case "open_url": {
			/* The web browser of Python (plotly, bokeh, folium, webbrowser.open): the browser view of Bio7 */
			String url = required(args, 0, "URL");
			Display.getDefault().asyncExec(() -> com.eco.bio7.collection.BrowserUtil.setLocationWithJS(url));
			return null;
		}
		case "progress": {
			/* bio7.progress, bio7.task, tqdm: key, done, total (empty: unknown), text */
			double total = arg(args, 2) == null ? -1 : Double.parseDouble(args[2]);
			PythonJobs.progress(required(args, 0, "key"), Double.parseDouble(required(args, 1, "done")), total, arg(args, 3));
			return null;
		}
		case "progress_end":
			PythonJobs.finishTask(required(args, 0, "key"));
			return null;
		case "ij_macro":
			/* An ImageJ macro (code, argument): the value of its return statement */
			return runMacro(required(args, 0, "macro code"), arg(args, 1));
		case "ij_macro_file": {
			java.nio.file.Path file = java.nio.file.Path.of(required(args, 0, "macro file"));
			if (!java.nio.file.Files.isRegularFile(file) && IJ.getDirectory("macros") != null) {
				/* Relative to the macros folder of ImageJ */
				file = java.nio.file.Path.of(IJ.getDirectory("macros"), file.toString());
			}
			if (!java.nio.file.Files.isRegularFile(file)) {
				throw new IllegalArgumentException("The macro file was not found: " + args[0]);
			}
			return runMacro(java.nio.file.Files.readString(file), arg(args, 1));
		}
		case "ij_run": {
			/* An ImageJ command with options (as run() of the macro language) on the current image */
			IJ.run(required(args, 0, "command"), arg(args, 1) == null ? "" : arg(args, 1));
			return null;
		}
		case "results_table": {
			ResultsTable rt = ResultsTable.getResultsTable();
			if (rt == null || rt.size() == 0) {
				throw new IllegalStateException("The ImageJ results table is empty");
			}
			return PythonConvert.resultsTable(rt);
		}
		case "roi": {
			ImagePlus imp = image(null);
			Roi roi = imp.getRoi();
			if (roi == null) {
				throw new IllegalStateException("The image has no selection");
			}
			return PythonConvert.roi(roi);
		}
		case "r": {
			REXP x = r().eval(required(args, 0, "R code"));
			/* The code may have changed the R workspace: the variables of the R-Shell */
			com.eco.bio7.rbridge.RServeUtil.listRObjects();
			return PythonConvert.fromR(x);
		}
		case "to_r": {
			String name = required(args, 0, "name");
			r().assign(name, PythonConvert.toR(value == null ? PyValue.None.INSTANCE : value));
			/* The new variable in the R-Shell */
			com.eco.bio7.rbridge.RServeUtil.listRObjects();
			return null;
		}
		default: {
			if (function.startsWith("java_")) {
				/* The Java API (bio7.java): classes, objects, methods, fields */
				return PythonJava.call(function, args, value);
			}
			Function f = FUNCTIONS.get(function);
			if (f == null) {
				throw new IllegalArgumentException("Unknown function of Bio7: " + function);
			}
			return f.call(args, value);
		}
		}
	}

	/*
	 * Runs an ImageJ macro in this thread (as the macro jobs of Bio7): an error is
	 * an exception with the message of ImageJ (no dialog)
	 */
	private static PyValue runMacro(String code, String argument) {
		ij.macro.Interpreter interp = new ij.macro.Interpreter();
		interp.setIgnoreErrors(true);
		String result;
		try {
			result = interp.run(code, argument == null ? "" : argument);
		} catch (RuntimeException e) {
			/* exit() of the macro ends it (as in ImageJ) */
			if ("Macro canceled".equals(e.getMessage())) {
				return null;
			}
			throw e;
		}
		/* With ignored errors the message is set (wasError() stays false) */
		String msg = interp.getErrorMessage();
		if (msg != null || interp.wasError()) {
			int line = interp.getLineNumber();
			throw new IllegalStateException("ImageJ macro error" + (line > 0 ? " in line " + line : "") + ": "
					+ (msg == null || msg.isBlank() ? "the macro was aborted" : msg));
		}
		return result == null ? null : PyValue.of(result);
	}

	private static String arg(String[] args, int i) {
		return i < args.length && !args[i].isEmpty() ? args[i] : null;
	}

	private static String required(String[] args, int i, String what) {
		String a = arg(args, i);
		if (a == null) {
			throw new IllegalArgumentException("The " + what + " is missing");
		}
		return a;
	}

	private static ImagePlus image(String title) {
		ImagePlus imp = title == null ? WindowManager.getCurrentImage() : WindowManager.getImage(title);
		if (imp == null) {
			throw new IllegalStateException(title == null ? "No ImageJ image is open" : "No ImageJ image with the title: " + title);
		}
		return imp;
	}

	private static RBackend r() {
		RBackend b = RBridge.getBackend();
		if (b == null) {
			throw new IllegalStateException("R is not running (please start R)");
		}
		return b;
	}
}
