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

import java.util.Locale;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

/**
 * The numbers of the transfer between the Bio7 table and R with full precision:
 * numbers are transferred as doubles (binary), not as texts. The parser of R
 * (as.numeric of a text) doesn't round numbers with 17 digits always correctly
 * and as.character in R writes 15 digits only.
 */
public final class RNumbers {

	private RNumbers() {
	}

	/**
	 * The numbers of the texts of a column (Java parses correctly rounded): "NA"
	 * is NA, "NaN", "Inf" and "-Inf" are the special values. Returns null if a text
	 * is not a decimal number (e.g., a text or an empty cell: the column is not
	 * numeric, like the former test in R).
	 */
	public static double[] parse(String[] texts) {
		double[] d = new double[texts.length];
		for (int i = 0; i < texts.length; i++) {
			String t = texts[i] == null ? "" : texts[i].trim();
			if (t.equals("NA")) {
				d[i] = REXPDouble.NA;
				continue;
			}
			String lower = t.toLowerCase(Locale.ROOT);
			if (lower.equals("nan")) {
				d[i] = Double.NaN;
			} else if (lower.equals("inf") || lower.equals("+inf") || lower.equals("infinity") || lower.equals("+infinity")) {
				d[i] = Double.POSITIVE_INFINITY;
			} else if (lower.equals("-inf") || lower.equals("-infinity")) {
				d[i] = Double.NEGATIVE_INFINITY;
			} else if (isDecimal(t)) {
				d[i] = Double.parseDouble(t);
			} else {
				return null;
			}
		}
		return d;
	}

	/* A decimal number (digits, a point, an exponent; not the suffixes and hex numbers of Java) */
	private static boolean isDecimal(String t) {
		return t.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");
	}

	/**
	 * Assigns the texts of a column as a numeric vector (doubles) in R.
	 *
	 * @return false if the column is not numeric (nothing assigned)
	 */
	public static boolean assign(RConnection connection, String name, String[] texts) {
		double[] d = parse(texts);
		if (d == null) {
			return false;
		}
		try {
			connection.assign(name, new REXPDouble(d));
			return true;
		} catch (REngineException e) {
			return false;
		}
	}

	/**
	 * The texts of a numeric vector of R (a plain double vector, not a date or
	 * another class) with full precision or null (not a plain double vector).
	 *
	 * @param connection the connection
	 * @param expression the vector in R (e.g., "df[,2]")
	 */
	public static String[] texts(RConnection connection, String expression) {
		try {
			REXP plain = connection.eval("try(is.double(" + expression + ") && is.null(oldClass(" + expression + ")))");
			if (plain == null || !plain.isLogical() || ((org.rosuda.REngine.REXPLogical) plain).isTRUE().length == 0
					|| !((org.rosuda.REngine.REXPLogical) plain).isTRUE()[0]) {
				return null;
			}
			REXP x = connection.eval("try(" + expression + ")");
			if (!(x instanceof REXPDouble)) {
				return null;
			}
			double[] d = x.asDoubles();
			String[] s = new String[d.length];
			for (int i = 0; i < d.length; i++) {
				s[i] = format(d[i]);
			}
			return s;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * A double as the shortest text which gives the same double (like R writes
	 * numbers: NA, NaN, Inf, integers without ".0").
	 */
	public static String format(double v) {
		if (REXPDouble.isNA(v)) {
			return "NA";
		}
		if (Double.isNaN(v)) {
			return "NaN";
		}
		if (Double.isInfinite(v)) {
			return v > 0 ? "Inf" : "-Inf";
		}
		if (v == Math.rint(v) && Math.abs(v) < 1e15) {
			return String.valueOf((long) v);
		}
		return Double.toString(v).replace('E', 'e');
	}
}
