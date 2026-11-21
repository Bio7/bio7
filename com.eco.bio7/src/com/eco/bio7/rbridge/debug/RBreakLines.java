/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.rbridge.debug;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Where a breakpoint of an R script can stop: the debugger inserts the stop
 * (browser()) as R code, which must be valid R at that place.
 * <ul>
 * <li>A line which starts a statement (top level or in { }): the stop is a line
 * before it.</li>
 * <li>A line with a function (e.g., a method of a class in a list:
 * {@code maxAge = function() 50,} or {@code move = function(world) {}): the stop
 * is in the function (after its "{" or the body without braces is wrapped:
 * {@code function() { stop; 50 }}).</li>
 * <li>Else (e.g., an argument of a call, a continued expression, "else"): the
 * next line where R can stop.</li>
 * </ul>
 * A small scanner of R: strings (also raw strings), comments, brackets and the
 * headers of if, for, while and function.
 */
final class RBreakLines {

	/** Insert a line before the line. */
	static final int LINE = 0;
	/** Insert the stop into the line (see {@link #inline(int, String)}). */
	static final int INLINE = 1;

	private static final Set<String> HEADERS = Set.of("if", "for", "while", "function");
	private static final String OPERATORS = "+-*/^<>=!&|~?:,$@%";

	private final List<String> lines;
	private final String text;
	private final int[] lineStart;
	/* For each character: code (not a string or comment) */
	private final boolean[] code;
	/* For each character: the depth of the brackets before it */
	private final int[] depth;
	/* For each line: the innermost open bracket at its start (0: none) */
	private final char[] openAtStart;
	/* For each line: the number of open brackets at its start */
	private final int[] depthAtStart;
	/* For each ")": it closes the header of if, for, while or function */
	private final boolean[] headerClose;
	/* For each character: in a comment */
	private final boolean[] comment;
	/* For each line: it starts in a string (of several lines) */
	private final boolean[] inString;

	RBreakLines(List<String> lines) {
		this.lines = lines;
		this.text = String.join("\n", lines);
		int n = text.length();
		lineStart = new int[lines.size() + 1];
		for (int i = 0, p = 0; i < lines.size(); i++) {
			lineStart[i] = p;
			p += lines.get(i).length() + 1;
		}
		lineStart[lines.size()] = n + 1;
		code = new boolean[n];
		depth = new int[n];
		headerClose = new boolean[n];
		comment = new boolean[n];
		openAtStart = new char[lines.size()];
		depthAtStart = new int[lines.size()];
		inString = new boolean[lines.size()];
		scan();
	}

	private void scan() {
		int n = text.length();
		Deque<Character> stack = new ArrayDeque<>();
		Deque<Boolean> header = new ArrayDeque<>();
		int line = 0;
		int i = 0;
		while (i < n) {
			while (line < lines.size() && lineStart[line] <= i) {
				openAtStart[line] = stack.isEmpty() ? 0 : stack.peek();
				depthAtStart[line] = stack.size();
				line++;
			}
			char c = text.charAt(i);
			depth[i] = stack.size();
			if (c == '#') {
				/* A comment: to the end of the line */
				while (i < n && text.charAt(i) != '\n') {
					depth[i] = stack.size();
					comment[i] = true;
					i++;
				}
				continue;
			}
			int raw = rawStringEnd(i);
			if (raw > i) {
				for (int k = i; k < raw; k++) {
					depth[k] = stack.size();
				}
				/* The start and the end count as code (a value) */
				code[i] = true;
				code[raw - 1] = true;
				markInString(i, raw);
				i = raw;
				continue;
			}
			if (c == '"' || c == '\'' || c == '`') {
				int k = i + 1;
				while (k < n && text.charAt(k) != c) {
					if (text.charAt(k) == '\\' && c != '`') {
						k++;
					}
					k++;
				}
				for (int j = i; j <= Math.min(k, n - 1); j++) {
					depth[j] = stack.size();
				}
				/* The quotes count as code (the start and the end of a value) */
				code[i] = true;
				if (k < n) {
					code[k] = true;
				}
				markInString(i, k);
				i = k + 1;
				continue;
			}
			code[i] = true;
			if (c == '(' || c == '[' || c == '{') {
				header.push(c == '(' && HEADERS.contains(wordBefore(i)));
				stack.push(c);
			} else if (c == ')' || c == ']' || c == '}') {
				if (!stack.isEmpty()) {
					stack.pop();
					headerClose[i] = header.pop() && c == ')';
				}
				depth[i] = stack.size() + 1;
			}
			i++;
		}
		while (line < lines.size()) {
			openAtStart[line] = stack.isEmpty() ? 0 : stack.peek();
			depthAtStart[line] = stack.size();
			line++;
		}
	}

	/* The lines which start in the string from "from" (its quote) to "to" */
	private void markInString(int from, int to) {
		for (int l = 0; l < lines.size(); l++) {
			if (lineStart[l] > from && lineStart[l] <= to) {
				inString[l] = true;
			}
		}
	}

	/* The end (exclusive) of a raw string r"(...)" starting at i, else i */
	private int rawStringEnd(int i) {
		char c = text.charAt(i);
		if ((c != 'r' && c != 'R') || (i > 0 && isWordChar(text.charAt(i - 1)))) {
			return i;
		}
		int k = i + 1;
		if (k >= text.length() || (text.charAt(k) != '"' && text.charAt(k) != '\'')) {
			return i;
		}
		char quote = text.charAt(k++);
		int dashes = 0;
		while (k < text.length() && text.charAt(k) == '-') {
			dashes++;
			k++;
		}
		if (k >= text.length() || "([{".indexOf(text.charAt(k)) < 0) {
			return i;
		}
		char close = ")]}".charAt("([{".indexOf(text.charAt(k)));
		String end = close + "-".repeat(dashes) + quote;
		int e = text.indexOf(end, k + 1);
		return e < 0 ? text.length() : e + end.length();
	}

	private static boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '.' || c == '_';
	}

	/* The word (code) before position i, skipping blanks: "" if none */
	private String wordBefore(int i) {
		int k = i - 1;
		while (k >= 0 && (text.charAt(k) == ' ' || text.charAt(k) == '\t')) {
			k--;
		}
		int end = k + 1;
		while (k >= 0 && isWordChar(text.charAt(k)) && code[k]) {
			k--;
		}
		return text.substring(k + 1, end);
	}

	/* The last code character before position i (not blank), -1 if none */
	private int lastCodeBefore(int i) {
		for (int k = i - 1; k >= 0; k--) {
			char c = text.charAt(k);
			if (code[k] && !Character.isWhitespace(c)) {
				return k;
			}
		}
		return -1;
	}

	/** The line (0-based) starts a statement: a line can be inserted before it. */
	boolean startsStatement(int line) {
		char open = openAtStart[line];
		if (inString[line] || (open != 0 && open != '{')) {
			return false;
		}
		/*
		 * The line must start with code: not empty or a comment (the stop could become
		 * the last value of a block) and not a closing bracket (the value of the block
		 * would be the one of the stop)
		 */
		int f = nextCode(lineStart[line], line);
		if (f < 0 || ")]}".indexOf(text.charAt(f)) >= 0) {
			return false;
		}
		String first = lines.get(line).strip();
		if (first.startsWith("else") && (first.length() == 4 || !isWordChar(first.charAt(4)))) {
			return false;
		}
		int prev = lastCodeBefore(lineStart[line]);
		if (prev < 0) {
			return true;
		}
		char p = text.charAt(prev);
		if (p == '{' || p == ';') {
			return true;
		}
		if (OPERATORS.indexOf(p) >= 0 || p == '(' || p == '[') {
			/* A continued expression */
			return false;
		}
		if (p == ')' && headerClose[prev]) {
			/* The body of if, for, while or function */
			return false;
		}
		String word = wordBefore(prev + 1);
		return !word.equals("else") && !word.equals("repeat");
	}

	/**
	 * A line (0-based) in a statement (an argument of a call, a continued
	 * expression): the line where this statement starts, -1 if none (e.g., an
	 * empty line, a comment, "}" or "else": R stops at the next statement).
	 */
	int enclosingStatement(int line) {
		if (inString[line]) {
			return -1;
		}
		int f = nextCode(lineStart[line], line);
		if (f < 0 || text.charAt(f) == '}') {
			return -1;
		}
		String first = lines.get(line).strip();
		if (first.startsWith("else") && (first.length() == 4 || !isWordChar(first.charAt(4)))) {
			return -1;
		}
		/* The last statement before it which is not deeper (a statement in between would have ended it) */
		for (int l = line - 1; l >= 0; l--) {
			if (depthAtStart[l] <= depthAtStart[line] && startsStatement(l)) {
				return l;
			}
		}
		return -1;
	}

	/** The line (0-based) is in the top level code (not in a function or a block). */
	boolean topLevel(int line) {
		return openAtStart[line] == 0 && !inString[line];
	}

	/**
	 * The line (0-based) with the stop in its function: the new text of the line,
	 * null if the line has no function (whose body starts on this line).
	 */
	String inline(int line, String stop) {
		String s = lines.get(line);
		int base = lineStart[line];
		for (int col = 0; col < s.length(); col++) {
			int i = base + col;
			if (!code[i]) {
				continue;
			}
			int paren = -1;
			if (s.startsWith("function", col) && (col == 0 || !isWordChar(s.charAt(col - 1)))) {
				paren = nextCode(i + "function".length(), line);
			} else if (s.charAt(col) == '\\') {
				paren = nextCode(i + 1, line);
			}
			if (paren < 0 || text.charAt(paren) != '(') {
				continue;
			}
			int close = matching(paren);
			if (close < 0 || close >= lineStart[line + 1]) {
				continue;
			}
			int body = nextCode(close + 1, line);
			if (body < 0) {
				continue;
			}
			if (text.charAt(body) == '{') {
				/* After the "{" of the body */
				int at = body - base + 1;
				return s.substring(0, at) + " " + stop + "; " + s.substring(at);
			}
			int end = bodyEnd(body, line);
			if (end < 0) {
				continue;
			}
			int a = body - base;
			int b = end - base;
			return s.substring(0, a) + "{ " + stop + "; " + s.substring(a, b).stripTrailing() + " }" + s.substring(b);
		}
		return null;
	}

	/* The next code character (not blank) from i in the line, -1 if none */
	private int nextCode(int i, int line) {
		for (int k = i; k < lineStart[line + 1] - 1; k++) {
			if (code[k] && !Character.isWhitespace(text.charAt(k))) {
				return k;
			}
			if (comment[k]) {
				return -1;
			}
		}
		return -1;
	}

	/* The bracket which closes the bracket at i, -1 if none */
	private int matching(int i) {
		for (int k = i + 1; k < text.length(); k++) {
			if (code[k] && depth[k] == depth[i] + 1 && ")]}".indexOf(text.charAt(k)) >= 0) {
				return k;
			}
		}
		return -1;
	}

	/*
	 * The end (exclusive) of a body without braces which starts at i: a "," or ";"
	 * or a closing bracket at its depth, a comment or the end of the line; -1 if it
	 * continues on the next line
	 */
	private int bodyEnd(int i, int line) {
		int d = depth[i];
		int last = -1;
		int lineEnd = lineStart[line + 1] - 1;
		for (int k = i; k < lineEnd; k++) {
			char c = text.charAt(k);
			if (comment[k]) {
				break;
			}
			if (!code[k]) {
				continue;
			}
			if (depth[k] == d && (c == ',' || c == ';' || ")]}".indexOf(c) >= 0)) {
				return k;
			}
			if (!Character.isWhitespace(c)) {
				last = k;
			}
		}
		if (last < 0) {
			return -1;
		}
		/* An open bracket or an operator at the end: the body continues */
		/* A closing bracket belongs to the depth outside of it */
		int level = ")]}".indexOf(text.charAt(last)) >= 0 ? depth[last] - 1 : depth[last];
		if (level != d || (OPERATORS.indexOf(text.charAt(last)) >= 0 && text.charAt(last) != ',') || "([{".indexOf(text.charAt(last)) >= 0) {
			return -1;
		}
		/* After the last code character (a string may follow, it is part of the body) */
		int end = last + 1;
		while (end < lineEnd && !code[end] && !comment[end]) {
			end++;
		}
		return end;
	}

	/**
	 * The lines (0-based) which start the statement after a loop (for, while or
	 * repeat with a body in braces) in the same block. At the end of a loop R
	 * restores the stepping mode of the function from the start of the loop: a loop
	 * which started without it (e.g., R stopped at a breakpoint in the loop) would
	 * run the rest of the function without a stop.
	 */
	List<Integer> afterLoops() {
		List<Integer> out = new java.util.ArrayList<>();
		for (int l = 0; l < lines.size(); l++) {
			if (!startsStatement(l)) {
				continue;
			}
			int f = nextCode(lineStart[l], l);
			String word = wordAt(f);
			int body;
			if (word.equals("repeat")) {
				body = nextCodeFrom(f + word.length());
			} else if (word.equals("for") || word.equals("while")) {
				int paren = nextCodeFrom(f + word.length());
				int close = paren < 0 || text.charAt(paren) != '(' ? -1 : matching(paren);
				body = close < 0 ? -1 : nextCodeFrom(close + 1);
			} else {
				continue;
			}
			if (body < 0 || text.charAt(body) != '{') {
				continue;
			}
			int end = matching(body);
			if (end < 0) {
				continue;
			}
			int endLine = lineOf(end);
			/* Only "}" at the end of its line (no else, no more code) */
			if (endLine + 1 >= lines.size() || nextCode(end + 1, endLine) >= 0) {
				continue;
			}
			for (int next = endLine + 1; next < lines.size(); next++) {
				if (nextCode(lineStart[next], next) < 0) {
					/* An empty line or a comment */
					continue;
				}
				if (depthAtStart[next] == depthAtStart[l] && startsStatement(next)) {
					out.add(next);
				}
				break;
			}
		}
		return out;
	}

	/* The word (code) which starts at i, "" if none */
	private String wordAt(int i) {
		if (i < 0) {
			return "";
		}
		int k = i;
		while (k < text.length() && isWordChar(text.charAt(k)) && code[k]) {
			k++;
		}
		return text.substring(i, k);
	}

	/* The next code character (not blank, not a comment) from i, also on the next lines; -1 if none */
	private int nextCodeFrom(int i) {
		for (int k = i; k < text.length(); k++) {
			if (code[k] && !Character.isWhitespace(text.charAt(k))) {
				return k;
			}
		}
		return -1;
	}

	/* The line (0-based) of a character */
	private int lineOf(int i) {
		int l = 0;
		while (l + 1 < lines.size() && lineStart[l + 1] <= i) {
			l++;
		}
		return l;
	}

	/** The number of lines. */
	int size() {
		return lines.size();
	}
}
