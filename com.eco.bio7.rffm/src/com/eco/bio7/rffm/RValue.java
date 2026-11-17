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
 * An R object as Java data (a copy). Read with doubles(), ints(), strings(),
 * booleans(), bytes(), firstDouble(), firstString(), elements(), matrix() and
 * create with RValue.of(...), listOf(...), dataFrame(...), factor(...).
 * <p>
 * The types as records: The vector types of R map to one record
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

	/* ---------------- Creating values ---------------- */

	/** NULL of R. */
	RValue NULL = Null.INSTANCE;

	/** A numeric vector. */
	static RValue of(double... values) {
		return new Num(values, Map.of());
	}

	/** An integer vector. */
	static RValue of(int... values) {
		return new Int(values, Map.of());
	}

	/** A character vector (null: NA). */
	static RValue of(String... values) {
		return new Str(values, Map.of());
	}

	/** A logical vector. */
	static RValue of(boolean... values) {
		return new Logical(values);
	}

	/** A raw vector. */
	static RValue of(byte... values) {
		return new Raw(values, Map.of());
	}

	/** A list without names. */
	static RValue listOf(RValue... elements) {
		return new List(elements);
	}

	/** A list with names (in the order of the map). */
	static RValue listOf(Map<String, RValue> elements) {
		return List.named(elements);
	}

	/** A data frame: the columns (vectors of the same length) by name. */
	static RValue dataFrame(Map<String, RValue> columns) {
		int rows = columns.isEmpty() ? 0 : columns.values().iterator().next().length();
		return new List(java.util.List.copyOf(columns.values()), attributes("names", new Str(columns.keySet().toArray(String[]::new)),
				"class", new Str("data.frame"), "row.names", new Int(NA_INTEGER, -rows)));
	}

	/** A factor: the labels (null: NA), the levels sorted as in R. */
	static RValue factor(String... labels) {
		String[] levels = java.util.Arrays.stream(labels).filter(java.util.Objects::nonNull).distinct().sorted().toArray(String[]::new);
		java.util.List<String> index = java.util.Arrays.asList(levels);
		int[] codes = new int[labels.length];
		for (int i = 0; i < labels.length; i++) {
			codes[i] = labels[i] == null ? NA_INTEGER : index.indexOf(labels[i]) + 1;
		}
		return new Int(codes, attributes("levels", new Str(levels), "class", new Str("factor")));
	}

	/** This value with one attribute more (or replaced; null removes it). */
	default RValue with(String attribute, RValue value) {
		Map<String, RValue> a = new LinkedHashMap<>(attributes());
		if (value == null) {
			a.remove(attribute);
		} else {
			a.put(attribute, value);
		}
		return switch (this) {
		case Null n -> n;
		case Logical l -> new Logical(l.values(), a);
		case Int i -> new Int(i.values(), a);
		case Num d -> new Num(d.values(), a);
		case Str s -> new Str(s.values(), a);
		case Raw r -> new Raw(r.values(), a);
		case List l -> new List(l.values(), a);
		case Other o -> new Other(o.type(), a);
		};
	}

	/** This value with names. */
	default RValue withNames(String... names) {
		return with("names", new Str(names));
	}

	/** This value as a matrix or an array with the dimensions. */
	default RValue withDim(int... dim) {
		return with("dim", new Int(dim));
	}

	/* ---------------- The kind of value ---------------- */

	default boolean isNull() {
		return this instanceof Null;
	}

	/** Numbers: a numeric or an integer vector (not a factor). */
	default boolean isNumeric() {
		return this instanceof Num || this instanceof Int i && !i.isFactor();
	}

	/** A character vector. */
	default boolean isCharacter() {
		return this instanceof Str;
	}

	default boolean isLogical() {
		return this instanceof Logical;
	}

	default boolean isRaw() {
		return this instanceof Raw;
	}

	/** A list (also a data frame). */
	default boolean isList() {
		return this instanceof List;
	}

	default boolean isFactor() {
		return this instanceof Int i && i.isFactor();
	}

	default boolean isDataFrame() {
		return this instanceof List l && l.isDataFrame();
	}

	/** A matrix (two dimensions). */
	default boolean isMatrix() {
		int[] d = dim();
		return d != null && d.length == 2;
	}

	/** The type of R, e.g., "double", "character", "list" (for messages). */
	default String typeName() {
		return switch (this) {
		case Null n -> "NULL";
		case Logical l -> "logical";
		case Int i -> i.isFactor() ? "factor" : "integer";
		case Num d -> "double";
		case Str s -> "character";
		case Raw r -> "raw";
		case List l -> l.isDataFrame() ? "data.frame" : "list";
		case Other o -> o.type();
		};
	}

	/* ---------------- Reading values ---------------- */

	/**
	 * The values as numbers: numeric, integer and logical vectors (NA: NA_REAL),
	 * character vectors are parsed (not a number: NA_REAL).
	 */
	default double[] doubles() {
		return switch (this) {
		case Num d -> d.values();
		case Int i -> toDoubles(i.values());
		case Logical l -> toDoubles(l.values());
		case Str s -> {
			double[] v = new double[s.values().length];
			for (int k = 0; k < v.length; k++) {
				v[k] = parseDouble(s.values()[k]);
			}
			yield v;
		}
		default -> throw wrongType("numbers");
		};
	}

	/** The values as integers: integer and logical vectors, numbers are truncated (NA, NaN: NA_INTEGER). */
	default int[] ints() {
		return switch (this) {
		case Int i -> i.values();
		case Logical l -> l.values();
		case Num d -> {
			int[] v = new int[d.values().length];
			for (int k = 0; k < v.length; k++) {
				double x = d.values()[k];
				v[k] = Double.isNaN(x) ? NA_INTEGER : (int) x;
			}
			yield v;
		}
		default -> throw wrongType("integers");
		};
	}

	/**
	 * The values as text: a character vector (NA: null), the labels of a factor,
	 * numbers and logical values as text (NA: "NA").
	 */
	default String[] strings() {
		return switch (this) {
		case Str s -> s.values();
		case Int i when i.isFactor() -> i.factorLabels();
		case Int i -> {
			String[] t = new String[i.values().length];
			for (int k = 0; k < t.length; k++) {
				t[k] = i.values()[k] == NA_INTEGER ? "NA" : Integer.toString(i.values()[k]);
			}
			yield t;
		}
		case Num d -> {
			String[] t = new String[d.values().length];
			for (int k = 0; k < t.length; k++) {
				t[k] = isNA(d.values()[k]) ? "NA" : Double.toString(d.values()[k]);
			}
			yield t;
		}
		case Logical l -> {
			String[] t = new String[l.values().length];
			for (int k = 0; k < t.length; k++) {
				t[k] = l.values()[k] == NA_INTEGER ? "NA" : l.values()[k] != 0 ? "TRUE" : "FALSE";
			}
			yield t;
		}
		default -> throw wrongType("text");
		};
	}

	/** The values as booleans: logical values (NA: false), numbers (not 0: true). */
	default boolean[] booleans() {
		return switch (this) {
		case Logical l -> l.asBooleans();
		case Int i -> {
			boolean[] b = new boolean[i.values().length];
			for (int k = 0; k < b.length; k++) {
				b[k] = i.values()[k] != 0 && i.values()[k] != NA_INTEGER;
			}
			yield b;
		}
		case Num d -> {
			boolean[] b = new boolean[d.values().length];
			for (int k = 0; k < b.length; k++) {
				b[k] = d.values()[k] != 0 && !Double.isNaN(d.values()[k]);
			}
			yield b;
		}
		default -> throw wrongType("logical values");
		};
	}

	/** The bytes of a raw vector. */
	default byte[] bytes() {
		if (this instanceof Raw r) {
			return r.values();
		}
		throw wrongType("bytes (a raw vector)");
	}

	/** The first number (see {@link #doubles()}). */
	default double firstDouble() {
		double[] v = doubles();
		if (v.length == 0) {
			throw new RTypeException("Expected a number but R returned an empty " + typeName() + " vector");
		}
		return v[0];
	}

	/** The first integer (see {@link #ints()}). */
	default int firstInt() {
		int[] v = ints();
		if (v.length == 0) {
			throw new RTypeException("Expected an integer but R returned an empty " + typeName() + " vector");
		}
		return v[0];
	}

	/** The first text (see {@link #strings()}). */
	default String firstString() {
		String[] v = strings();
		if (v.length == 0) {
			throw new RTypeException("Expected a text but R returned an empty " + typeName() + " vector");
		}
		return v[0];
	}

	/** True if the first value is TRUE (a logical vector, e.g., of is.numeric(x)). */
	default boolean isTrue() {
		return this instanceof Logical l && l.values().length > 0 && l.values()[0] == 1;
	}

	/** The elements of a list (also the columns of a data frame). */
	default java.util.List<RValue> elements() {
		if (this instanceof List l) {
			return l.values();
		}
		throw wrongType("a list");
	}

	/** An element of a list (from 0). */
	default RValue element(int index) {
		return elements().get(index);
	}

	/** An element of a list by name, or null. */
	default RValue element(String name) {
		if (this instanceof List l) {
			return l.get(name);
		}
		throw wrongType("a list");
	}

	/** A matrix as rows: [row][column] (numbers with two dimensions). */
	default double[][] matrix() {
		int[] d = dim();
		if (d == null || d.length != 2) {
			throw new RTypeException("Expected a matrix but R returned a " + typeName() + " without two dimensions");
		}
		double[] v = doubles();
		double[][] m = new double[d[0]][d[1]];
		for (int r = 0; r < d[0]; r++) {
			for (int c = 0; c < d[1]; c++) {
				m[r][c] = v[c * d[0] + r];
			}
		}
		return m;
	}

	private RTypeException wrongType(String expected) {
		return new RTypeException("Expected " + expected + " but R returned " + typeName());
	}

	private static double[] toDoubles(int[] v) {
		double[] d = new double[v.length];
		for (int k = 0; k < d.length; k++) {
			d[k] = v[k] == NA_INTEGER ? NA_REAL : v[k];
		}
		return d;
	}

	private static double parseDouble(String s) {
		if (s == null) {
			return NA_REAL;
		}
		try {
			return Double.parseDouble(s.strip());
		} catch (NumberFormatException e) {
			return NA_REAL;
		}
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
