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

import org.eclipse.swt.widgets.Display;
import org.rosuda.REngine.REXP;

import com.eco.bio7.pyffm.PyException;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rbridge.backend.RBackend;

import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;

/**
 * The API of the embedded Python of Bio7 for Java, Groovy and BeanShell
 * scripts (as RServeUtil for R; Python scripts use the module bio7). The
 * variables are created in the namespace of the Python console and of the
 * scripts (__main__).
 *
 * <pre>
 * PythonUtil.toPython("x", 1.0, 2.0, 3.0);           // numpy array
 * PyValue mean = PythonUtil.fromPython("float(x.mean())");
 * PythonUtil.imageToPython("img");                   // the current ImageJ image
 * PythonUtil.pythonToR("x", "xr");                   // Python -> R
 * PythonUtil.rToPython("summary(cars)", "s");        // R -> Python
 * </pre>
 */
public final class PythonUtil {

	private PythonUtil() {
	}

	/** Returns true if the embedded Python runs. */
	public static boolean isAlive() {
		return EmbeddedPython.isRunning();
	}

	/**
	 * Starts the embedded Python with the Python of the preferences (if not
	 * running).
	 *
	 * @throws PyException with the reason if it can't be started
	 */
	public static void start() throws PyException {
		String error = EmbeddedPython.start();
		if (error != null) {
			throw new PyException(error);
		}
	}

	/* The running session (started if necessary) */
	private static PySession py() throws PyException {
		if (!EmbeddedPython.isRunning()) {
			start();
		}
		return EmbeddedPython.session();
	}

	/* ---------------- Java <-> Python ---------------- */

	/** Assigns numbers (a numpy array). */
	public static void toPython(String name, double... values) throws PyException {
		py().assign(name, values);
		PythonShellView.refreshOpenViews();
	}

	/** Assigns integers (a numpy int32 array). */
	public static void toPython(String name, int... values) throws PyException {
		py().assign(name, values);
		PythonShellView.refreshOpenViews();
	}

	/** Assigns strings (a str for one string, else a list). */
	public static void toPython(String name, String... values) throws PyException {
		py().assign(name, values);
		PythonShellView.refreshOpenViews();
	}

	/** Assigns booleans (a numpy bool array). */
	public static void toPython(String name, boolean... values) throws PyException {
		py().assign(name, values);
		PythonShellView.refreshOpenViews();
	}

	/** Assigns a matrix (rows) as 2-d numpy array. */
	public static void toPython(String name, double[][] matrix) throws PyException {
		py().assign(name, matrix);
		PythonShellView.refreshOpenViews();
	}

	/** Assigns any value (arrays with shape, lists, dicts, DataFrames). */
	public static void toPython(String name, PyValue value) throws PyException {
		py().assign(name, value);
		PythonShellView.refreshOpenViews();
	}

	/** Evaluates a Python expression and returns its value. */
	public static PyValue fromPython(String expression) throws PyException {
		return py().eval(expression);
	}

	/** Evaluates a Python expression and returns its value. */
	public static PyValue eval(String expression) throws PyException {
		return py().eval(expression);
	}

	/** Executes Python statements (an error raises an exception with the traceback). */
	public static void exec(String code) throws PyException {
		py().exec(code);
		PythonShellView.refreshOpenViews();
	}

	/** Executes Python code like a script: the output and errors in the console, new plots in the view. */
	public static void run(String code) throws PyException {
		py().runAsync(code);
	}

	/** Returns the numbers of an expression (e.g., a numpy array) as double[]. */
	public static double[] fromPythonDoubles(String expression) throws PyException {
		PyValue v = fromPython(expression);
		if (v instanceof PyValue.Array a) {
			return a.asDoubles();
		}
		throw new PyException("Not a numeric array: " + v);
	}

	/* ---------------- ImageJ ---------------- */

	/** Transfers the current ImageJ image as numpy array (see {@link PythonConvert#image}). */
	public static void imageToPython(String name) throws PyException {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			throw new PyException("No ImageJ image is open");
		}
		imageToPython(name, imp);
	}

	/** Transfers an image as numpy array. */
	public static void imageToPython(String name, ImagePlus imp) throws PyException {
		py().assign(name, PythonConvert.image(imp));
		PythonShellView.refreshOpenViews();
	}

	/** Creates and shows an ImageJ image from a numpy array (2-d, stack or RGB). */
	public static ImagePlus imageFromPython(String expression, String title) throws PyException {
		ImagePlus imp = PythonConvert.toImage(fromPython(expression), title);
		Display.getDefault().asyncExec(imp::show);
		return imp;
	}

	/** Transfers the ImageJ results table as pandas DataFrame (a dict without pandas). */
	public static void resultsTableToPython(String name) throws PyException {
		ResultsTable rt = ResultsTable.getResultsTable();
		if (rt == null) {
			throw new PyException("The ImageJ results table is not available");
		}
		py().assign(name, PythonConvert.resultsTable(rt));
		PythonShellView.refreshOpenViews();
	}

	/* ---------------- R <-> Python ---------------- */

	/**
	 * Transfers the value of a Python expression to R (embedded R or Rserve):
	 * arrays keep their shape (matrices, arrays), DataFrames become data frames,
	 * dicts named lists.
	 */
	public static void pythonToR(String pythonExpression, String rName) throws Exception {
		REXP r = PythonConvert.toR(fromPython(pythonExpression));
		backend().assign(rName, r);
		/* The new variable in the R-Shell */
		com.eco.bio7.rbridge.RServeUtil.listRObjects();
	}

	/**
	 * Transfers the value of an R expression to Python: vectors and matrices
	 * become numpy arrays, data frames pandas DataFrames, named lists dicts.
	 */
	public static void rToPython(String rExpression, String pythonName) throws Exception {
		REXP r = backend().eval(rExpression);
		py().assign(pythonName, PythonConvert.fromR(r));
		PythonShellView.refreshOpenViews();
		PythonShellView.refreshOpenViews();
	}

	/** Alias of {@link #pythonToR}. */
	public static void toR(String pythonExpression, String rName) throws Exception {
		pythonToR(pythonExpression, rName);
	}

	/** Alias of {@link #rToPython}. */
	public static void fromR(String rExpression, String pythonName) throws Exception {
		rToPython(rExpression, pythonName);
	}

	private static RBackend backend() throws PyException {
		RBackend b = RBridge.getBackend();
		if (b == null) {
			throw new PyException("R is not running (please start R)");
		}
		return b;
	}
}
