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

package com.eco.bio7.rffm;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An R object as Java data (a copy). The vector types of R map to one record
 * each; the attributes names, dim, dimnames, class, levels and row.names are
 * kept (other attributes are not transferred).
 * <p>
 * NA: R's own values are kept (integer and logical: {@link #NA_INTEGER}, double:
 * the NA bit pattern, see {@link #isNA(double)}, character: null).
 */
public sealed interface RValue permits RValue.Null, RValue.Logical, RValue.Int, RValue.Num, RValue.Str, RValue.Raw, RValue.List, RValue.Other {

	/** NA of integer and logical vectors (as in R). */
	int NA_INTEGER = Integer.MIN_VALUE;
	/** The bit pattern of NA_real_ in R. */
	long NA_REAL_BITS = 0x7FF00000000007A2L;
	/** NA_real_ of R (a NaN with a payload, distinct from NaN). */
	double NA_REAL = Double.longBitsToDouble(NA_REAL_BITS);

	/** Returns true if a double is R's NA (not any NaN). */
	static boolean isNA(double v) {
		return Double.isNaN(v) && (Double.doubleToRawLongBits(v) & 0xFFFFFFFFL) == 1954;
	}

	/** The attributes (names, dim, dimnames, class, levels, row.names), never null. */
	Map<String, RValue> attributes();

	/** The number of elements (0 for NULL). */
	int length();

	/** The attribute or null. */
	default RValue attribute(String name) {
		return attributes().get(name);
	}

	/** The names (the "names" attribute) or null. */
	default String[] names() {
		return attribute("names") instanceof Str s ? s.values() : null;
	}

	/** The dimensions (the "dim" attribute) or null. */
	default int[] dim() {
		return attribute("dim") instanceof Int i ? i.values() : null;
	}

	/** Returns true if the "class" attribute contains the class. */
	default boolean inherits(String cls) {
		return attribute("class") instanceof Str s && Arrays.asList(s.values()).contains(cls);
	}

	/* ---------------- The types ---------------- */

	/** NULL. */
	record Null() implements RValue {
		public static final Null INSTANCE = new Null();

		public Map<String, RValue> attributes() {
			return Map.of();
		}

		public int length() {
			return 0;
		}
	}

	/** A logical vector: 1 (TRUE), 0 (FALSE), NA_INTEGER (NA). */
	record Logical(int[] values, Map<String, RValue> attributes) implements RValue {
		public Logical {
			attributes = copy(attributes);
		}

		public Logical(boolean... values) {
			this(toInts(values), Map.of());
		}

		public int length() {
			return values.length;
		}

		public boolean isNA(int i) {
			return values[i] == NA_INTEGER;
		}

		/** The values as booleans (NA is false). */
		public boolean[] asBooleans() {
			boolean[] b = new boolean[values.length];
			for (int i = 0; i < b.length; i++) {
				b[i] = values[i] == 1;
			}
			return b;
		}

		private static int[] toInts(boolean[] b) {
			int[] v = new int[b.length];
			for (int i = 0; i < b.length; i++) {
				v[i] = b[i] ? 1 : 0;
			}
			return v;
		}
	}

	/** An integer vector (also a factor: the codes with the attribute "levels"). */
	record Int(int[] values, Map<String, RValue> attributes) implements RValue {
		public Int {
			attributes = copy(attributes);
		}

		public Int(int... values) {
			this(values, Map.of());
		}

		public int length() {
			return values.length;
		}

		/** Returns true if this is a factor. */
		public boolean isFactor() {
			return inherits("factor");
		}

		/** The levels of a factor or null. */
		public String[] levels() {
			return attribute("levels") instanceof Str s ? s.values() : null;
		}

		/** The labels of a factor (null for NA). */
		public String[] factorLabels() {
			String[] levels = levels();
			if (levels == null) {
				return null;
			}
			String[] labels = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				labels[i] = values[i] == NA_INTEGER || values[i] < 1 || values[i] > levels.length ? null : levels[values[i] - 1];
			}
			return labels;
		}
	}

	/** A numeric (double) vector, a matrix with the attribute "dim". */
	record Num(double[] values, Map<String, RValue> attributes) implements RValue {
		public Num {
			attributes = copy(attributes);
		}

		public Num(double... values) {
			this(values, Map.of());
		}

		/** A matrix (row-major Java array, stored column-major as in R). */
		public static Num matrix(double[][] rows) {
			int nrow = rows.length;
			int ncol = nrow == 0 ? 0 : rows[0].length;
			double[] v = new double[nrow * ncol];
			for (int r = 0; r < nrow; r++) {
				for (int c = 0; c < ncol; c++) {
					v[c * nrow + r] = rows[r][c];
				}
			}
			return new Num(v, Map.of("dim", new Int(nrow, ncol)));
		}

		public int length() {
			return values.length;
		}

		/** The matrix as rows (requires the attribute "dim" with 2 values). */
		public double[][] asMatrix() {
			int[] d = dim();
			if (d == null || d.length != 2) {
				throw new IllegalStateException("not a matrix");
			}
			double[][] m = new double[d[0]][d[1]];
			for (int r = 0; r < d[0]; r++) {
				for (int c = 0; c < d[1]; c++) {
					m[r][c] = values[c * d[0] + r];
				}
			}
			return m;
		}
	}

	/** A character vector (null is NA). */
	record Str(String[] values, Map<String, RValue> attributes) implements RValue {
		public Str {
			attributes = copy(attributes);
		}

		public Str(String... values) {
			this(values, Map.of());
		}

		public int length() {
			return values.length;
		}
	}

	/** A raw vector. */
	record Raw(byte[] values, Map<String, RValue> attributes) implements RValue {
		public Raw {
			attributes = copy(attributes);
		}

		public Raw(byte... values) {
			this(values, Map.of());
		}

		public int length() {
			return values.length;
		}
	}

	/** A list (also a data frame: class "data.frame" with names and row.names). */
	record List(java.util.List<RValue> values, Map<String, RValue> attributes) implements RValue {
		public List {
			values = java.util.List.copyOf(values);
			attributes = copy(attributes);
		}

		public List(RValue... values) {
			this(java.util.List.of(values), Map.of());
		}

		/** A list with names. */
		public static List named(Map<String, RValue> entries) {
			return new List(java.util.List.copyOf(entries.values()), Map.of("names", new Str(entries.keySet().toArray(String[]::new))));
		}

		public int length() {
			return values.size();
		}

		/** The element with a name or null. */
		public RValue get(String name) {
			String[] n = names();
			if (n != null) {
				for (int i = 0; i < n.length; i++) {
					if (name.equals(n[i])) {
						return values.get(i);
					}
				}
			}
			return null;
		}

		public boolean isDataFrame() {
			return inherits("data.frame");
		}
	}

	/** An object which is not transferred (e.g., a function or an environment): only its type. */
	record Other(String type, Map<String, RValue> attributes) implements RValue {
		public Other {
			attributes = copy(attributes);
		}

		public int length() {
			return 0;
		}
	}

	private static Map<String, RValue> copy(Map<String, RValue> attributes) {
		return attributes == null || attributes.isEmpty() ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
	}

	/** Attributes with names and values (e.g., attributes("names", names)). */
	static Map<String, RValue> attributes(Object... nameValuePairs) {
		Map<String, RValue> map = new LinkedHashMap<>();
		for (int i = 0; i + 1 < nameValuePairs.length; i += 2) {
			map.put((String) nameValuePairs[i], (RValue) nameValuePairs[i + 1]);
		}
		return map;
	}
}
