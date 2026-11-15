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
			Display.getDefault().asyncExec(imp::show);
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
			return PythonConvert.fromR(x);
		}
		case "to_r": {
			String name = required(args, 0, "name");
			r().assign(name, PythonConvert.toR(value == null ? PyValue.None.INSTANCE : value));
			return null;
		}
		default: {
			Function f = FUNCTIONS.get(function);
			if (f == null) {
				throw new IllegalArgumentException("Unknown function of Bio7: " + function);
			}
			return f.call(args, value);
		}
		}
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
