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
package com.eco.bio7.rbridge;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.eco.bio7.rbridge.backend.EmbeddedR;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RSession;
import com.eco.bio7.rffm.RValue;

import ij.ImagePlus;
import ij.WindowManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * The API of the embedded R of Bio7 (the FFM bridge, no Rserve) for Java,
 * Groovy and BeanShell scripts, as {@link com.eco.bio7.pybridge.PythonUtil}
 * for Python. Plain Java types: the variables are created in the global
 * environment of R (visible in the console and the R-Shell).
 *
 * <pre>
 * RUtil.toR("x", 1.0, 2.0, 3.0);                     // numeric vector
 * RUtil.toR("m", new double[][] { { 1, 2 }, { 3, 4 } }); // matrix
 * Map&lt;String, Object&gt; cols = new LinkedHashMap&lt;&gt;();
 * cols.put("id", new int[] { 1, 2 });
 * cols.put("name", new String[] { "a", "b" });
 * RUtil.dataFrameToR("df", cols);                    // data frame
 * RUtil.exec("fit &lt;- lm(dist ~ speed, cars)");
 * double[] coef = RUtil.fromRDoubles("coef(fit)");   // R -&gt; Java
 * String text = RUtil.print("summary(fit)");          // the printed output
 * </pre>
 */
public final class RUtil {

	private RUtil() {
	}

	/** Returns true if the embedded R (FFM) runs. */
	public static boolean isAlive() {
		return EmbeddedR.isRunning();
	}

	/** The session of the embedded R (for the full API of com.eco.bio7.rffm). */
	public static RSession session() throws RException {
		RSession s = EmbeddedR.session();
		if (s == null) {
			throw new RException("The embedded R is not running: please start R (Preferences R: embedded R)");
		}
		return s;
	}

	/* ---------------- Java -> R ---------------- */

	/** Assigns a numeric vector. */
	public static void toR(String name, double... values) throws RException {
		session().assign(name, values);
		changed();
	}

	/** Assigns an integer vector. */
	public static void toR(String name, int... values) throws RException {
		session().assign(name, values);
		changed();
	}

	/** Assigns a character vector. */
	public static void toR(String name, String... values) throws RException {
		session().assign(name, values);
		changed();
	}

	/** Assigns a logical vector. */
	public static void toR(String name, boolean... values) throws RException {
		session().assign(name, values);
		changed();
	}

	/** Assigns a matrix (the rows of Java). */
	public static void toR(String name, double[][] matrix) throws RException {
		session().assign(name, matrix);
		changed();
	}

	/** Assigns any value (e.g., an RValue of a former call). */
	public static void toR(String name, RValue value) throws RException {
		session().assign(name, value);
		changed();
	}

	/**
	 * Assigns a data frame. The columns (in the order of the map, e.g., a
	 * LinkedHashMap) are double[], int[], String[] or boolean[] of the same
	 * length.
	 */
	public static void dataFrameToR(String name, Map<String, ?> columns) throws RException {
		Map<String, RValue> cols = new LinkedHashMap<>();
		int n = -1;
		for (Map.Entry<String, ?> e : columns.entrySet()) {
			RValue v = vector(e.getKey(), e.getValue());
			if (n >= 0 && v.length() != n) {
				throw new RException("The column " + e.getKey() + " has " + v.length() + " values (not " + n + ")");
			}
			n = v.length();
			cols.put(e.getKey(), v);
		}
		session().assign(name, dataFrame(cols, Math.max(n, 0)));
		changed();
	}

	/** Assigns a list with names (the values as for {@link #dataFrameToR} or RValue). */
	public static void listToR(String name, Map<String, ?> entries) throws RException {
		Map<String, RValue> list = new LinkedHashMap<>();
		for (Map.Entry<String, ?> e : entries.entrySet()) {
			list.put(e.getKey(), vector(e.getKey(), e.getValue()));
		}
		session().assign(name, RValue.List.named(list));
		changed();
	}

	/** Transfers an ImageJ image (the current one if null) as numeric matrix (rows = y). */
	public static void imageToR(String name, ImagePlus imp) throws RException {
		if (imp == null) {
			imp = WindowManager.getCurrentImage();
			if (imp == null) {
				throw new RException("No ImageJ image is open");
			}
		}
		ImageProcessor ip = imp.getProcessor();
		int w = ip.getWidth(), h = ip.getHeight();
		double[][] rows = new double[h][w];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				rows[y][x] = ip.getPixelValue(x, y);
			}
		}
		toR(name, rows);
	}

	/* ---------------- Computed in R ---------------- */

	/** Evaluates R code (statements), errors as exception. */
	public static void exec(String code) throws RException {
		session().voidEval(code);
		changed();
	}

	/** Evaluates R code and returns the value. */
	public static RValue eval(String code) throws RException {
		return session().eval(code);
	}

	/** The printed output of an expression (as in the console). */
	public static String print(String code) throws RException {
		String[] lines = fromRStrings("utils::capture.output(print(" + code + "))");
		return String.join("\n", lines);
	}

	/* ---------------- R -> Java ---------------- */

	/** The result as doubles (numeric, integer or logical; NA as NaN). */
	public static double[] fromRDoubles(String code) throws RException {
		return doubles(eval(code));
	}

	/** The first value of a numeric result. */
	public static double fromRDouble(String code) throws RException {
		double[] d = fromRDoubles(code);
		if (d.length == 0) {
			throw new RException("The result is empty: " + code);
		}
		return d[0];
	}

	/** The result as integers (also a factor: its codes). */
	public static int[] fromRInts(String code) throws RException {
		RValue v = eval(code);
		if (v instanceof RValue.Int i) {
			return i.values();
		}
		return Arrays.stream(doubles(v)).mapToInt(d -> (int) Math.round(d)).toArray();
	}

	/** The result as strings (also a factor: its labels). */
	public static String[] fromRStrings(String code) throws RException {
		return strings(eval(code));
	}

	/** The first string of the result. */
	public static String fromRString(String code) throws RException {
		String[] s = fromRStrings(code);
		return s.length == 0 ? null : s[0];
	}

	/** A matrix as rows. */
	public static double[][] fromRMatrix(String code) throws RException {
		RValue v = eval(code);
		int[] d = v.dim();
		if (d == null || d.length != 2) {
			throw new RException("Not a matrix: " + code);
		}
		double[] values = doubles(v);
		double[][] m = new double[d[0]][d[1]];
		for (int r = 0; r < d[0]; r++) {
			for (int c = 0; c < d[1]; c++) {
				m[r][c] = values[c * d[0] + r];
			}
		}
		return m;
	}

	/**
	 * A list or data frame: the elements by name (in order). Convert the
	 * elements with {@link #doubles}, {@link #strings}.
	 */
	public static Map<String, RValue> fromRList(String code) throws RException {
		RValue v = eval(code);
		if (!(v instanceof RValue.List list)) {
			throw new RException("Not a list or data frame: " + code);
		}
		String[] names = list.names();
		Map<String, RValue> map = new LinkedHashMap<>();
		for (int i = 0; i < list.length(); i++) {
			map.put(names != null && names[i] != null && !names[i].isEmpty() ? names[i] : String.valueOf(i + 1), list.values().get(i));
		}
		return map;
	}

	/** Creates and shows an ImageJ image (32-bit) from a numeric matrix (rows = y). */
	public static ImagePlus imageFromR(String code, String title) throws RException {
		double[][] m = fromRMatrix(code);
		int h = m.length, w = h == 0 ? 0 : m[0].length;
		FloatProcessor fp = new FloatProcessor(w, h);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				fp.setf(x, y, (float) m[y][x]);
			}
		}
		fp.resetMinAndMax();
		ImagePlus imp = new ImagePlus(title, fp);
		Display.getDefault().asyncExec(imp::show);
		return imp;
	}

	/* ---------------- Conversion of RValue ---------------- */

	/** Numbers of a value (numeric, integer, logical; NA as NaN). */
	public static double[] doubles(RValue v) throws RException {
		return switch (v) {
		case RValue.Num n -> Arrays.stream(n.values()).map(d -> RValue.isNA(d) ? Double.NaN : d).toArray();
		case RValue.Int i -> Arrays.stream(i.values()).mapToDouble(x -> x == RValue.NA_INTEGER ? Double.NaN : x).toArray();
		case RValue.Logical l -> Arrays.stream(l.values()).mapToDouble(x -> x == RValue.NA_INTEGER ? Double.NaN : x).toArray();
		default -> throw new RException("Not numeric: " + v.getClass().getSimpleName());
		};
	}

	/** Strings of a value (character, factor labels, numbers as text). */
	public static String[] strings(RValue v) throws RException {
		return switch (v) {
		case RValue.Str s -> s.values();
		case RValue.Int i when i.isFactor() -> i.factorLabels();
		case RValue.Num n -> Arrays.stream(n.values()).mapToObj(d -> RValue.isNA(d) ? null : String.valueOf(d)).toArray(String[]::new);
		case RValue.Int i -> Arrays.stream(i.values()).mapToObj(x -> x == RValue.NA_INTEGER ? null : String.valueOf(x)).toArray(String[]::new);
		default -> throw new RException("Not a character vector: " + v.getClass().getSimpleName());
		};
	}

	/* ---------------- Internal ---------------- */

	private static RValue vector(String name, Object o) throws RException {
		return switch (o) {
		case RValue v -> v;
		case double[] d -> new RValue.Num(d);
		case int[] i -> new RValue.Int(i);
		case String[] s -> new RValue.Str(s);
		case String s -> new RValue.Str(s);
		case boolean[] b -> new RValue.Logical(b);
		case Double d -> new RValue.Num(d);
		case Integer i -> new RValue.Int(i);
		case Boolean b -> new RValue.Logical(b);
		case double[][] m -> RValue.Num.matrix(m);
		case null -> RValue.Null.INSTANCE;
		default -> throw new RException("Not supported for " + name + ": " + o.getClass().getSimpleName()
				+ " (double[], int[], String[], boolean[], double[][] or RValue)");
		};
	}

	private static RValue dataFrame(Map<String, RValue> cols, int nrow) {
		return new RValue.List(java.util.List.copyOf(cols.values()), Map.of( //
				"names", new RValue.Str(cols.keySet().toArray(String[]::new)), //
				"class", new RValue.Str("data.frame"), //
				/* The compact row names of R: c(NA, -n) */
				"row.names", new RValue.Int(RValue.NA_INTEGER, -nrow)));
	}

	/* The new variables in the R-Shell */
	private static void changed() {
		try {
			RServeUtil.listRObjects();
		} catch (RuntimeException | LinkageError e) {
			/* No view */
		}
	}
}
