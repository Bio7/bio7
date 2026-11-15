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
package com.eco.bio7.pyffm;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A value exchanged with Python. Numbers are numpy arrays in Python (Python
 * lists or the array module if numpy isn't installed):
 * <ul>
 * <li>{@link Array}: a numeric or boolean array with a shape (a scalar has the
 * shape {}): float64, float32, int64, int32, int16, int8, uint8, uint16,
 * uint32, uint64, bool</li>
 * <li>{@link Str}: strings (a str or a list of str)</li>
 * <li>{@link PyList}, {@link PyDict}: list (tuple) and dict</li>
 * <li>{@link Frame}: a pandas DataFrame (a dict of columns without pandas)</li>
 * <li>{@link None}, {@link Other} (any other object: its type and repr)</li>
 * </ul>
 * The data of an array is kept in row-major (C) order as in numpy.
 */
public sealed interface PyValue permits PyValue.Array, PyValue.Str, PyValue.PyList, PyValue.PyDict, PyValue.Frame, PyValue.None, PyValue.Other {

	/** The number of elements. */
	int length();

	/* ---------------- The types ---------------- */

	/**
	 * A numeric or boolean array.
	 *
	 * @param data  a double[], float[], long[], int[], short[] or byte[] (also for
	 *              the unsigned types: the bits as in numpy) or boolean[]
	 * @param dtype the numpy type: "f8", "f4", "i8", "i4", "i2", "i1", "u1", "u2",
	 *              "u4", "u8", "b1"
	 * @param shape the shape ({} for a scalar)
	 */
	record Array(Object data, String dtype, int[] shape) implements PyValue {
		public Array {
			if (shape == null) {
				shape = new int[] { java.lang.reflect.Array.getLength(data) };
			}
			shape = shape.clone();
		}

		@Override
		public int length() {
			return java.lang.reflect.Array.getLength(data);
		}

		/** Returns true for a scalar (shape {}). */
		public boolean isScalar() {
			return shape.length == 0;
		}

		/** The element as double (unsigned types as unsigned values). */
		public double getDouble(int i) {
			return switch (data) {
			case double[] d -> d[i];
			case float[] f -> f[i];
			case long[] l -> dtype.equals("u8") ? unsignedLong(l[i]) : l[i];
			case int[] n -> dtype.equals("u4") ? Integer.toUnsignedLong(n[i]) : n[i];
			case short[] s -> dtype.equals("u2") ? Short.toUnsignedInt(s[i]) : s[i];
			case byte[] b -> dtype.equals("u1") ? Byte.toUnsignedInt(b[i]) : b[i];
			case boolean[] b -> b[i] ? 1 : 0;
			default -> throw new IllegalStateException("Unknown data: " + data);
			};
		}

		/** The data as double values (a copy, unsigned types as unsigned values). */
		public double[] asDoubles() {
			if (data instanceof double[] d) {
				return d.clone();
			}
			double[] d = new double[length()];
			for (int i = 0; i < d.length; i++) {
				d[i] = getDouble(i);
			}
			return d;
		}

		/** The data as int values (rounded towards zero). */
		public int[] asInts() {
			if (data instanceof int[] n && !dtype.equals("u4")) {
				return n.clone();
			}
			int[] n = new int[length()];
			for (int i = 0; i < n.length; i++) {
				n[i] = (int) getDouble(i);
			}
			return n;
		}

		/** The first element as double. */
		public double asDouble() {
			return getDouble(0);
		}

		/** A 2-d array as rows of doubles. */
		public double[][] asDoubleMatrix() {
			if (shape.length != 2) {
				throw new IllegalStateException("Not a 2-d array: shape " + Arrays.toString(shape));
			}
			double[][] m = new double[shape[0]][shape[1]];
			for (int r = 0; r < shape[0]; r++) {
				for (int c = 0; c < shape[1]; c++) {
					m[r][c] = getDouble(r * shape[1] + c);
				}
			}
			return m;
		}

		private static double unsignedLong(long v) {
			return v >= 0 ? v : (double) (v >>> 1) * 2.0 + (v & 1);
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Array a && dtype.equals(a.dtype) && Arrays.equals(shape, a.shape) && Arrays.deepEquals(new Object[] { data }, new Object[] { a.data });
		}

		@Override
		public int hashCode() {
			return dtype.hashCode() * 31 + Arrays.hashCode(shape);
		}

		@Override
		public String toString() {
			return "Array[" + dtype + ", shape " + Arrays.toString(shape) + "]";
		}
	}

	/** Strings (null elements are None in Python). */
	record Str(String[] values) implements PyValue {
		public Str {
			values = values.clone();
		}

		@Override
		public int length() {
			return values.length;
		}

		/** The first string. */
		public String asString() {
			return values.length == 0 ? null : values[0];
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Str s && Arrays.equals(values, s.values);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(values);
		}

		@Override
		public String toString() {
			return "Str" + Arrays.toString(values);
		}
	}

	/** A list or tuple. */
	record PyList(List<PyValue> values) implements PyValue {
		public PyList {
			values = List.copyOf(values);
		}

		@Override
		public int length() {
			return values.size();
		}
	}

	/** A dict (the keys as strings, in their order). */
	record PyDict(Map<String, PyValue> values) implements PyValue {
		public PyDict {
			values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
		}

		@Override
		public int length() {
			return values.size();
		}

		public PyValue get(String key) {
			return values.get(key);
		}
	}

	/**
	 * A table (a pandas DataFrame): the columns by name (each an Array or Str) and
	 * the row labels (null for the default 0..n-1).
	 */
	record Frame(Map<String, PyValue> columns, String[] index) implements PyValue {
		public Frame {
			columns = Collections.unmodifiableMap(new LinkedHashMap<>(columns));
			index = index == null ? null : index.clone();
		}

		/** The number of rows. */
		@Override
		public int length() {
			return columns.isEmpty() ? 0 : columns.values().iterator().next().length();
		}

		public PyValue column(String name) {
			return columns.get(name);
		}
	}

	/** None. */
	record None() implements PyValue {
		public static final None INSTANCE = new None();

		@Override
		public int length() {
			return 0;
		}
	}

	/** Any other object: its type and repr (not transferable). */
	record Other(String type, String repr) implements PyValue {
		@Override
		public int length() {
			return 1;
		}
	}

	/* ---------------- Factories ---------------- */

	static Array of(double... values) {
		return new Array(values.clone(), "f8", null);
	}

	static Array of(int... values) {
		return new Array(values.clone(), "i4", null);
	}

	static Array of(long... values) {
		return new Array(values.clone(), "i8", null);
	}

	static Array of(boolean... values) {
		return new Array(values.clone(), "b1", null);
	}

	/** Unsigned bytes (uint8, e.g., 8-bit image data). */
	static Array ofUnsigned(byte... values) {
		return new Array(values.clone(), "u1", null);
	}

	static Str of(String... values) {
		return new Str(values);
	}

	/** A scalar float. */
	static Array scalar(double value) {
		return new Array(new double[] { value }, "f8", new int[0]);
	}

	/** A 2-d float64 array from rows (all rows must have the same length). */
	static Array matrix(double[][] rows) {
		int n = rows.length, m = n == 0 ? 0 : rows[0].length;
		double[] d = new double[n * m];
		for (int r = 0; r < n; r++) {
			if (rows[r].length != m) {
				throw new IllegalArgumentException("The rows have different lengths");
			}
			System.arraycopy(rows[r], 0, d, r * m, m);
		}
		return new Array(d, "f8", new int[] { n, m });
	}

	/** An array with a shape (the data in row-major order, not copied). */
	static Array array(Object data, String dtype, int... shape) {
		return new Array(data, dtype, shape);
	}
}
