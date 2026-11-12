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

package com.eco.bio7.reditor.format;

import java.util.ArrayList;
import java.util.List;

/**
 * A tokenizer for R source code (for the formatter): keeps every character of
 * the code except the whitespace between tokens. Tolerant: an incomplete
 * string or an unknown character is a token too.
 */
final class RTokenizer {

	enum Kind {
		/** Identifier, keyword or `backtick name`. */
		WORD,
		NUMBER,
		/** String (also a raw string), may span lines. */
		STRING,
		/** From '#' to the end of the line. */
		COMMENT,
		OPERATOR,
		/** '(' '[' '{' */
		OPEN,
		/** ')' ']' '}' */
		CLOSE,
		COMMA,
		SEMICOLON,
		NEWLINE
	}

	static final class Token {
		final Kind kind;
		String text;
		/** Whitespace before the token (on the same line) in the source. */
		boolean spaceBefore;
		/** A ')' which closes the header of 'if', 'for', 'while' or 'function' (set by the formatter). */
		boolean closesHeader;

		Token(Kind kind, String text, boolean spaceBefore) {
			this.kind = kind;
			this.text = text;
			this.spaceBefore = spaceBefore;
		}

		boolean is(Kind k, String t) {
			return kind == k && text.equals(t);
		}

		@Override
		public String toString() {
			return kind + ":" + text;
		}
	}

	/* Longest operators first */
	private static final String[] OPERATORS = { "<<-", "->>", ":::", "|>", "<-", "->", "<=", ">=", "==", "!=", "&&", "||", "::", ":=", "**" };

	private RTokenizer() {
	}

	static List<Token> tokenize(String s) {
		List<Token> tokens = new ArrayList<>();
		int n = s.length();
		int i = 0;
		boolean space = false;
		while (i < n) {
			char c = s.charAt(i);
			if (c == '\r' || c == '\n') {
				int end = c == '\r' && i + 1 < n && s.charAt(i + 1) == '\n' ? i + 2 : i + 1;
				tokens.add(new Token(Kind.NEWLINE, s.substring(i, end), space));
				space = false;
				i = end;
				continue;
			}
			if (c == ' ' || c == '\t' || c == '\f' || c == ' ') {
				space = true;
				i++;
				continue;
			}
			int start = i;
			Kind kind;
			if (c == '#') {
				while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r') {
					i++;
				}
				kind = Kind.COMMENT;
			} else if ((c == 'r' || c == 'R') && i + 1 < n && (s.charAt(i + 1) == '"' || s.charAt(i + 1) == '\'') && rawStringEnd(s, i) > 0) {
				i = rawStringEnd(s, i);
				kind = Kind.STRING;
			} else if (c == '"' || c == '\'') {
				i = quotedEnd(s, i, c);
				kind = Kind.STRING;
			} else if (c == '`') {
				i = quotedEnd(s, i, '`');
				kind = Kind.WORD;
			} else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
				i = numberEnd(s, i);
				kind = Kind.NUMBER;
			} else if (Character.isLetter(c) || c == '.') {
				i++;
				while (i < n) {
					char d = s.charAt(i);
					if (Character.isLetterOrDigit(d) || d == '.' || d == '_') {
						i++;
					} else {
						break;
					}
				}
				kind = Kind.WORD;
			} else if (c == '(' || c == '[' || c == '{') {
				i++;
				kind = Kind.OPEN;
			} else if (c == ')' || c == ']' || c == '}') {
				i++;
				kind = Kind.CLOSE;
			} else if (c == ',') {
				i++;
				kind = Kind.COMMA;
			} else if (c == ';') {
				i++;
				kind = Kind.SEMICOLON;
			} else if (c == '%') {
				/* %op% (on one line), else a single '%' */
				int j = i + 1;
				while (j < n && s.charAt(j) != '%' && s.charAt(j) != '\n' && s.charAt(j) != '\r') {
					j++;
				}
				i = j < n && s.charAt(j) == '%' ? j + 1 : i + 1;
				kind = Kind.OPERATOR;
			} else {
				int len = 1;
				for (String op : OPERATORS) {
					if (s.startsWith(op, i)) {
						len = op.length();
						break;
					}
				}
				i += len;
				kind = Kind.OPERATOR;
			}
			tokens.add(new Token(kind, s.substring(start, i), space));
			space = false;
		}
		return tokens;
	}

	/* The end (exclusive) of a string or backtick name; unterminated: the end of the text */
	private static int quotedEnd(String s, int i, char quote) {
		int n = s.length();
		i++;
		while (i < n) {
			char c = s.charAt(i);
			if (c == '\\' && quote != '`') {
				i += 2;
				continue;
			}
			i++;
			if (c == quote) {
				return i;
			}
		}
		return n;
	}

	/* r"(...)", R'[...]', r"---{...}---": the end (exclusive) or -1 if it is no raw string */
	private static int rawStringEnd(String s, int i) {
		int n = s.length();
		char quote = s.charAt(i + 1);
		int j = i + 2;
		int dashes = 0;
		while (j < n && s.charAt(j) == '-') {
			dashes++;
			j++;
		}
		if (j >= n) {
			return -1;
		}
		char open = s.charAt(j);
		char close = open == '(' ? ')' : open == '[' ? ']' : open == '{' ? '}' : 0;
		if (close == 0) {
			return -1;
		}
		String end = close + "-".repeat(dashes) + quote;
		int k = s.indexOf(end, j + 1);
		return k < 0 ? n : k + end.length();
	}

	private static int numberEnd(String s, int i) {
		int n = s.length();
		if (s.charAt(i) == '0' && i + 1 < n && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
			i += 2;
			while (i < n && (Character.digit(s.charAt(i), 16) >= 0 || s.charAt(i) == '.')) {
				i++;
			}
			/* Hexadecimal exponent (p) */
			if (i < n && (s.charAt(i) == 'p' || s.charAt(i) == 'P')) {
				i = exponent(s, i);
			}
		} else {
			while (i < n && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
				i++;
			}
			if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
				i = exponent(s, i);
			}
		}
		if (i < n && (s.charAt(i) == 'L' || s.charAt(i) == 'i')) {
			i++;
		}
		return i;
	}

	private static int exponent(String s, int i) {
		int n = s.length();
		int j = i + 1;
		if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) {
			j++;
		}
		if (j < n && Character.isDigit(s.charAt(j))) {
			while (j < n && Character.isDigit(s.charAt(j))) {
				j++;
			}
			return j;
		}
		/* No exponent (e.g., an identifier follows): not part of the number */
		return i;
	}
}
