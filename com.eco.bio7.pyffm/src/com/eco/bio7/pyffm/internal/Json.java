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
package com.eco.bio7.pyffm.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader and writer for the headers exchanged with Python
 * (objects as LinkedHashMap, arrays as List, numbers as Double or Long,
 * strings, booleans, null).
 */
public final class Json {

	private final String s;
	private int i;

	private Json(String s) {
		this.s = s;
	}

	public static Object parse(String text) {
		Json j = new Json(text);
		j.ws();
		Object v = j.value();
		j.ws();
		if (j.i != j.s.length()) {
			throw new IllegalArgumentException("JSON: unexpected text at " + j.i);
		}
		return v;
	}

	private void ws() {
		while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
	}

	private Object value() {
		if (i >= s.length()) {
			throw new IllegalArgumentException("JSON: unexpected end");
		}
		char c = s.charAt(i);
		switch (c) {
		case '{': {
			i++;
			Map<String, Object> m = new LinkedHashMap<>();
			ws();
			if (s.charAt(i) == '}') {
				i++;
				return m;
			}
			while (true) {
				ws();
				String k = string();
				ws();
				expect(':');
				ws();
				m.put(k, value());
				ws();
				if (s.charAt(i) == ',') {
					i++;
					continue;
				}
				expect('}');
				return m;
			}
		}
		case '[': {
			i++;
			List<Object> l = new ArrayList<>();
			ws();
			if (s.charAt(i) == ']') {
				i++;
				return l;
			}
			while (true) {
				ws();
				l.add(value());
				ws();
				if (s.charAt(i) == ',') {
					i++;
					continue;
				}
				expect(']');
				return l;
			}
		}
		case '"':
			return string();
		case 't':
			literal("true");
			return Boolean.TRUE;
		case 'f':
			literal("false");
			return Boolean.FALSE;
		case 'n':
			literal("null");
			return null;
		case 'N':
			literal("NaN");
			return Double.NaN;
		case 'I':
			literal("Infinity");
			return Double.POSITIVE_INFINITY;
		default:
			return number();
		}
	}

	private void literal(String word) {
		if (!s.startsWith(word, i)) {
			throw new IllegalArgumentException("JSON: unexpected text at " + i);
		}
		i += word.length();
	}

	private void expect(char c) {
		if (i >= s.length() || s.charAt(i) != c) {
			throw new IllegalArgumentException("JSON: '" + c + "' expected at " + i);
		}
		i++;
	}

	private Object number() {
		int start = i;
		if (s.startsWith("-Infinity", i)) {
			i += 9;
			return Double.NEGATIVE_INFINITY;
		}
		boolean real = false;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c == '.' || c == 'e' || c == 'E') {
				real = true;
			} else if (!(c == '-' || c == '+' || Character.isDigit(c))) {
				break;
			}
			i++;
		}
		String n = s.substring(start, i);
		if (n.isEmpty()) {
			throw new IllegalArgumentException("JSON: value expected at " + start);
		}
		return real ? (Object) Double.parseDouble(n) : (Object) Long.parseLong(n);
	}

	private String string() {
		expect('"');
		StringBuilder b = new StringBuilder();
		while (true) {
			char c = s.charAt(i++);
			if (c == '"') {
				return b.toString();
			}
			if (c != '\\') {
				b.append(c);
				continue;
			}
			char e = s.charAt(i++);
			switch (e) {
			case 'n' -> b.append('\n');
			case 't' -> b.append('\t');
			case 'r' -> b.append('\r');
			case 'b' -> b.append('\b');
			case 'f' -> b.append('\f');
			case 'u' -> {
				b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
				i += 4;
			}
			default -> b.append(e);
			}
		}
	}

	/** Writes a value (Map, List, String, Number, Boolean, null) as JSON. */
	public static String write(Object v) {
		StringBuilder b = new StringBuilder();
		write(b, v);
		return b.toString();
	}

	private static void write(StringBuilder b, Object v) {
		switch (v) {
		case null -> b.append("null");
		case String str -> quote(b, str);
		case Boolean bool -> b.append(bool);
		case Double d when d.isNaN() -> b.append("NaN");
		case Double d when d.isInfinite() -> b.append(d > 0 ? "Infinity" : "-Infinity");
		case Number n -> b.append(n);
		case Map<?, ?> m -> {
			b.append('{');
			boolean first = true;
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (!first) {
					b.append(',');
				}
				first = false;
				quote(b, String.valueOf(e.getKey()));
				b.append(':');
				write(b, e.getValue());
			}
			b.append('}');
		}
		case List<?> l -> {
			b.append('[');
			for (int k = 0; k < l.size(); k++) {
				if (k > 0) {
					b.append(',');
				}
				write(b, l.get(k));
			}
			b.append(']');
		}
		case int[] a -> {
			b.append('[');
			for (int k = 0; k < a.length; k++) {
				if (k > 0) {
					b.append(',');
				}
				b.append(a[k]);
			}
			b.append(']');
		}
		default -> quote(b, v.toString());
		}
	}

	private static void quote(StringBuilder b, String str) {
		b.append('"');
		for (int k = 0; k < str.length(); k++) {
			char c = str.charAt(k);
			switch (c) {
			case '"' -> b.append("\\\"");
			case '\\' -> b.append("\\\\");
			case '\n' -> b.append("\\n");
			case '\r' -> b.append("\\r");
			case '\t' -> b.append("\\t");
			default -> {
				if (c < 0x20) {
					b.append(String.format("\\u%04x", (int) c));
				} else {
					b.append(c);
				}
			}
			}
		}
		b.append('"');
	}
}
