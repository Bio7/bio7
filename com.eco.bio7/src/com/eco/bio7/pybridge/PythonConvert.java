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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import com.eco.bio7.pyffm.PyValue;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import com.eco.bio7.rffm.RValue;
import com.eco.bio7.rffm.RException;

/**
 * Converts data of Bio7 (ImageJ images, the results table, R objects) into
 * values of the embedded Python (numpy arrays, pandas DataFrames) and back.
 * Arrays of Python are row-major (numpy), matrices of R column-major: the data
 * is reordered so that m[i, j] is the same element in both.
 */
public final class PythonConvert {

	private PythonConvert() {
	}

	/* ---------------- ImageJ ---------------- */

	/**
	 * An image as array: 8-bit uint8, 16-bit uint16, 32-bit float32 with the shape
	 * (height, width), a stack (slices, height, width), RGB uint8 (height, width,
	 * 3) or (slices, height, width, 3).
	 */
	public static PyValue.Array image(ImagePlus imp) {
		ImageStack stack = imp.getStack();
		int w = imp.getWidth(), h = imp.getHeight(), n = stack.getSize();
		int plane = w * h;
		int type = imp.getType();
		if (type == ImagePlus.COLOR_RGB) {
			byte[] rgb = new byte[n * plane * 3];
			for (int s = 0; s < n; s++) {
				int[] px = (int[]) stack.getProcessor(s + 1).getPixels();
				int o = s * plane * 3;
				for (int i = 0; i < plane; i++) {
					int c = px[i];
					rgb[o + 3 * i] = (byte) (c >> 16);
					rgb[o + 3 * i + 1] = (byte) (c >> 8);
					rgb[o + 3 * i + 2] = (byte) c;
				}
			}
			return new PyValue.Array(rgb, "u1", n == 1 ? new int[] { h, w, 3 } : new int[] { n, h, w, 3 });
		}
		int[] shape = n == 1 ? new int[] { h, w } : new int[] { n, h, w };
		switch (type) {
		case ImagePlus.GRAY8, ImagePlus.COLOR_256: {
			byte[] d = new byte[n * plane];
			for (int s = 0; s < n; s++) {
				System.arraycopy((byte[]) stack.getProcessor(s + 1).getPixels(), 0, d, s * plane, plane);
			}
			return new PyValue.Array(d, "u1", shape);
		}
		case ImagePlus.GRAY16: {
			short[] d = new short[n * plane];
			for (int s = 0; s < n; s++) {
				System.arraycopy((short[]) stack.getProcessor(s + 1).getPixels(), 0, d, s * plane, plane);
			}
			return new PyValue.Array(d, "u2", shape);
		}
		default: {
			float[] d = new float[n * plane];
			for (int s = 0; s < n; s++) {
				System.arraycopy((float[]) stack.getProcessor(s + 1).convertToFloat().getPixels(), 0, d, s * plane, plane);
			}
			return new PyValue.Array(d, "f4", shape);
		}
		}
	}

	/**
	 * An image from an array: 2-d (height, width) or 3-d (slices, height, width);
	 * uint8 (h, w, 3) or (n, h, w, 3) is RGB. uint8 and bool give 8-bit, uint16 and
	 * int16 16-bit, other types 32-bit images.
	 */
	public static ImagePlus toImage(PyValue value, String title) {
		PyValue.Array a = toArray(value);
		int[] s = a.shape();
		boolean rgb = (s.length == 3 || s.length == 4) && s[s.length - 1] == 3 && a.dtype().equals("u1");
		int dims = rgb ? s.length - 1 : s.length;
		if (dims != 2 && dims != 3) {
			throw new IllegalArgumentException("A 2-d (height, width) or 3-d (slices, height, width) array is expected");
		}
		int n = dims == 3 ? s[0] : 1, h = s[dims - 2], w = s[dims - 1], plane = w * h;
		ImageStack stack = new ImageStack(w, h);
		for (int k = 0; k < n; k++) {
			ImageProcessor ip;
			if (rgb) {
				byte[] d = (byte[]) a.data();
				int[] px = new int[plane];
				int o = k * plane * 3;
				for (int i = 0; i < plane; i++) {
					px[i] = 0xff000000 | (d[o + 3 * i] & 0xff) << 16 | (d[o + 3 * i + 1] & 0xff) << 8 | (d[o + 3 * i + 2] & 0xff);
				}
				ip = new ColorProcessor(w, h, px);
			} else if (a.data() instanceof byte[] b && (a.dtype().equals("u1"))) {
				byte[] px = new byte[plane];
				System.arraycopy(b, k * plane, px, 0, plane);
				ip = new ByteProcessor(w, h, px);
			} else if (a.data() instanceof boolean[] z) {
				byte[] px = new byte[plane];
				for (int i = 0; i < plane; i++) {
					px[i] = (byte) (z[k * plane + i] ? 255 : 0);
				}
				ip = new ByteProcessor(w, h, px);
			} else if (a.data() instanceof short[] sh && a.dtype().equals("u2")) {
				short[] px = new short[plane];
				System.arraycopy(sh, k * plane, px, 0, plane);
				ip = new ShortProcessor(w, h, px, null);
			} else {
				float[] px = new float[plane];
				for (int i = 0; i < plane; i++) {
					px[i] = (float) a.getDouble(k * plane + i);
				}
				ip = new FloatProcessor(w, h, px);
			}
			stack.addSlice(null, ip);
		}
		ImagePlus imp = new ImagePlus(title, stack);
		imp.resetDisplayRange();
		return imp;
	}

	/**
	 * A numeric array from a value: a numpy array, or nested lists of numbers with
	 * equal lengths (e.g., [[1, 2], [3, 4]], or a 2-d array without numpy) as
	 * float64 array with their shape.
	 *
	 * @throws IllegalArgumentException if the value is not numeric or not rectangular
	 */
	public static PyValue.Array toArray(PyValue value) {
		if (value instanceof PyValue.Array a) {
			return a;
		}
		if (value instanceof PyValue.PyList l && l.length() > 0) {
			List<PyValue.Array> rows = new ArrayList<>();
			for (PyValue e : l.values()) {
				rows.add(toArray(e));
			}
			int[] inner = rows.get(0).shape();
			int size = rows.get(0).length();
			for (PyValue.Array r : rows) {
				if (!java.util.Arrays.equals(r.shape(), inner)) {
					throw new IllegalArgumentException("The nested lists have different lengths (not a rectangular array)");
				}
			}
			double[] d = new double[rows.size() * size];
			for (int i = 0; i < rows.size(); i++) {
				System.arraycopy(rows.get(i).asDoubles(), 0, d, i * size, size);
			}
			int[] shape = new int[inner.length + 1];
			shape[0] = rows.size();
			System.arraycopy(inner, 0, shape, 1, inner.length);
			return new PyValue.Array(d, "f8", shape);
		}
		String type = switch (value) {
		case PyValue.Other o -> o.type();
		case PyValue.Str s -> "str";
		case PyValue.PyDict d -> "dict";
		case PyValue.Frame f -> "DataFrame";
		case PyValue.None n -> "None";
		default -> value.getClass().getSimpleName();
		};
		throw new IllegalArgumentException("A numpy array (or nested lists of numbers) is expected, not: " + type);
	}

	/** The results table: the numeric columns and the labels (a pandas DataFrame). */
	public static PyValue.Frame resultsTable(ResultsTable rt) {
		Map<String, PyValue> cols = new LinkedHashMap<>();
		int rows = rt.size();
		/* The labels are a special column of ImageJ (not found by name) */
		if (rows > 0 && rt.getLabel(0) != null) {
			String[] labels = new String[rows];
			for (int i = 0; i < rows; i++) {
				labels[i] = rt.getLabel(i);
			}
			cols.put("Label", new PyValue.Str(labels));
		}
		String[] headings = rt.getHeadings();
		for (String heading : headings) {
			if (heading == null || heading.isEmpty() || heading.equals("Label") || heading.equals(" ")) {
				continue;
			}
			int c = rt.getColumnIndex(heading);
			if (c < 0) {
				continue;
			}
			double[] d = new double[rows];
			boolean text = false;
			for (int i = 0; i < rows; i++) {
				d[i] = rt.getValueAsDouble(c, i);
				if (Double.isNaN(d[i]) && rt.getStringValue(c, i) != null && !rt.getStringValue(c, i).equals("NaN")) {
					text = true;
				}
			}
			if (text) {
				String[] s = new String[rows];
				for (int i = 0; i < rows; i++) {
					s[i] = rt.getStringValue(c, i);
				}
				cols.put(heading, new PyValue.Str(s));
			} else {
				cols.put(heading, PyValue.of(d));
			}
		}
		return new PyValue.Frame(cols, null);
	}

	/** The coordinates of a selection: a dict with the arrays x and y. */
	public static PyValue.PyDict roi(Roi roi) {
		FloatPolygon p = roi.getFloatPolygon();
		double[] x = new double[p.npoints], y = new double[p.npoints];
		for (int i = 0; i < p.npoints; i++) {
			x[i] = p.xpoints[i];
			y[i] = p.ypoints[i];
		}
		Map<String, PyValue> m = new LinkedHashMap<>();
		m.put("x", PyValue.of(x));
		m.put("y", PyValue.of(y));
		return new PyValue.PyDict(m);
	}

	/* ---------------- R (RValue) ---------------- */

	/** An R object as Python value (vectors, matrices, arrays, lists, data frames, factors). */
	public static PyValue fromR(RValue r) {
		if (r == null || r.isNull()) {
			return PyValue.None.INSTANCE;
		}
		int[] dim = r.dim();
		switch (r) {
		case RValue.List list when list.isDataFrame(): {
			Map<String, PyValue> cols = new LinkedHashMap<>();
			String[] names = list.names();
			for (int i = 0; i < list.length(); i++) {
				cols.put(names != null && i < names.length ? names[i] : "V" + (i + 1), fromR(list.element(i)));
			}
			String[] index = list.attribute("row.names") instanceof RValue.Str rn ? rn.values() : null;
			return new PyValue.Frame(cols, index);
		}
		case RValue.List list: {
			String[] names = list.names();
			boolean named = names != null && java.util.Arrays.stream(names).anyMatch(n -> n != null && !n.isEmpty());
			if (named) {
				Map<String, PyValue> m = new LinkedHashMap<>();
				for (int i = 0; i < list.length(); i++) {
					m.put(names[i], fromR(list.element(i)));
				}
				return new PyValue.PyDict(m);
			}
			List<PyValue> items = new ArrayList<>();
			for (RValue e : list.elements()) {
				items.add(fromR(e));
			}
			return new PyValue.PyList(items);
		}
		case RValue.Int i when i.isFactor():
			return new PyValue.Str(i.strings());
		case RValue.Str t:
			return new PyValue.Str(dim == null ? t.values() : reorder(t.values(), dim, true));
		case RValue.Logical l: {
			boolean[] b = l.booleans();
			return new PyValue.Array(dim == null ? b : reorder(b, dim, true), "b1", dim);
		}
		case RValue.Raw raw: {
			byte[] b = raw.bytes();
			return new PyValue.Array(dim == null ? b : reorder(b, dim, true), "u1", dim);
		}
		case RValue.Int i: {
			int[] v = i.values();
			return new PyValue.Array(dim == null ? v : reorder(v, dim, true), "i4", dim);
		}
		case RValue.Num n: {
			double[] v = n.values().clone();
			/* NA of R as NaN (numpy) */
			for (int k = 0; k < v.length; k++) {
				if (RValue.isNA(v[k])) {
					v[k] = Double.NaN;
				}
			}
			return new PyValue.Array(dim == null ? v : reorder(v, dim, true), "f8", dim);
		}
		default:
			return new PyValue.Other(r.typeName(), r.typeName());
		}
	}

	/** A Python value as R object (arrays keep their shape as dim). */
	public static RValue toR(PyValue v) throws RException {
		switch (v) {
		case PyValue.None n:
			return RValue.NULL;
		case PyValue.Str s:
			return RValue.of(s.values());
		case PyValue.Array a: {
			int[] shape = a.shape();
			boolean matrix = shape.length > 1;
			Object data = a.data();
			RValue r;
			if (data instanceof boolean[] b) {
				r = RValue.of(matrix ? reorder(b, shape, false) : b);
			} else if ((data instanceof int[] || data instanceof short[] || data instanceof byte[]) && !a.dtype().equals("u4")) {
				int[] d = a.asInts();
				r = RValue.of(matrix ? reorder(d, shape, false) : d);
			} else {
				double[] d = a.asDoubles();
				r = RValue.of(matrix ? reorder(d, shape, false) : d);
			}
			return matrix ? r.withDim(shape) : r;
		}
		case PyValue.PyList l: {
			RValue[] items = new RValue[l.length()];
			for (int i = 0; i < items.length; i++) {
				items[i] = toR(l.values().get(i));
			}
			return RValue.listOf(items);
		}
		case PyValue.PyDict d: {
			Map<String, RValue> items = new LinkedHashMap<>();
			for (Map.Entry<String, PyValue> e : d.values().entrySet()) {
				items.put(e.getKey(), toR(e.getValue()));
			}
			return RValue.listOf(items);
		}
		case PyValue.Frame f: {
			Map<String, RValue> columns = new LinkedHashMap<>();
			for (Map.Entry<String, PyValue> e : f.columns().entrySet()) {
				columns.put(e.getKey(), toR(e.getValue()));
			}
			return RValue.dataFrame(columns);
		}
		case PyValue.Other o:
			throw new RException("A Python object of the type " + o.type() + " can't be transferred to R");
		}
	}

	/* ---------------- The order of the elements ---------------- */

	/*
	 * The source index of each target element: column-major (R) to row-major
	 * (numpy) with the same dims, or back.
	 */
	private static int[] order(int[] dims, boolean colToRow) {
		int n = 1;
		for (int d : dims) {
			n *= d;
		}
		int k = dims.length;
		int[] col = new int[k], row = new int[k];
		col[0] = 1;
		for (int i = 1; i < k; i++) {
			col[i] = col[i - 1] * dims[i - 1];
		}
		row[k - 1] = 1;
		for (int i = k - 2; i >= 0; i--) {
			row[i] = row[i + 1] * dims[i + 1];
		}
		int[] map = new int[n];
		int[] idx = new int[k];
		for (int t = 0; t < n; t++) {
			/* t is the target index: its multi-index in the target order */
			int rest = t;
			if (colToRow) {
				for (int i = 0; i < k; i++) {
					idx[i] = rest / row[i];
					rest %= row[i];
				}
				int src = 0;
				for (int i = 0; i < k; i++) {
					src += idx[i] * col[i];
				}
				map[t] = src;
			} else {
				for (int i = k - 1; i >= 0; i--) {
					idx[i] = rest / col[i];
					rest %= col[i];
				}
				int src = 0;
				for (int i = 0; i < k; i++) {
					src += idx[i] * row[i];
				}
				map[t] = src;
			}
		}
		return map;
	}

	static double[] reorder(double[] v, int[] dims, boolean colToRow) {
		int[] map = order(dims, colToRow);
		double[] r = new double[map.length];
		for (int i = 0; i < map.length; i++) {
			r[i] = v[map[i]];
		}
		return r;
	}

	static int[] reorder(int[] v, int[] dims, boolean colToRow) {
		int[] map = order(dims, colToRow);
		int[] r = new int[map.length];
		for (int i = 0; i < map.length; i++) {
			r[i] = v[map[i]];
		}
		return r;
	}

	static boolean[] reorder(boolean[] v, int[] dims, boolean colToRow) {
		int[] map = order(dims, colToRow);
		boolean[] r = new boolean[map.length];
		for (int i = 0; i < map.length; i++) {
			r[i] = v[map[i]];
		}
		return r;
	}

	static byte[] reorder(byte[] v, int[] dims, boolean colToRow) {
		int[] map = order(dims, colToRow);
		byte[] r = new byte[map.length];
		for (int i = 0; i < map.length; i++) {
			r[i] = v[map[i]];
		}
		return r;
	}

	static String[] reorder(String[] v, int[] dims, boolean colToRow) {
		int[] map = order(dims, colToRow);
		String[] r = new String[map.length];
		for (int i = 0; i < map.length; i++) {
			r[i] = v[map[i]];
		}
		return r;
	}
}
