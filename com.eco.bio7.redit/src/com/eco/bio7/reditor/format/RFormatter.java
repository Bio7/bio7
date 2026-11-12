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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import com.eco.bio7.reditor.format.RTokenizer.Kind;
import com.eco.bio7.reditor.format.RTokenizer.Token;

/**
 * The built-in R formatter (no R needed): indents the lines by the nesting of
 * braces, parentheses and brackets (and continued statements) and normalizes
 * the spaces between the tokens. The line breaks of the code stay (except for
 * the option braceOnNewLine and the blank lines); strings and comments are not
 * changed.
 * <p>
 * Safe: the result is checked to contain the same tokens as the source (only
 * whitespace, and '=' to '&lt;-' if wanted, may change), else an
 * {@link IllegalStateException} is thrown and nothing should be replaced.
 */
public final class RFormatter {

	/* Operators without spaces around them */
	private static final Set<String> TIGHT_OPERATORS = Set.of("::", ":::", "$", "@", ":", "^", "**");
	/* Operators which can be unary */
	private static final Set<String> UNARY_OPERATORS = Set.of("-", "+", "!", "~", "?");
	/* Keywords before '(' of a header */
	private static final Set<String> HEADER_KEYWORDS = Set.of("if", "for", "while");
	/* Words after which an operator is unary (e.g., 'else -1') */
	private static final Set<String> KEYWORDS_BEFORE_OPERAND = Set.of("else", "in", "repeat");

	private RFormatter() {
	}

	/** Formats R code. */
	public static String format(String text, RFormatterOptions options) {
		return format(text, options, "");
	}

	/**
	 * Formats R code.
	 *
	 * @param baseIndent the indentation of the code (e.g., of a selection), put in
	 *                   front of each line
	 */
	public static String format(String text, RFormatterOptions options, String baseIndent) {
		String delimiter = lineDelimiter(text);
		List<Token> tokens = RTokenizer.tokenize(text);
		boolean endsWithNewline = !tokens.isEmpty() && tokens.get(tokens.size() - 1).kind == Kind.NEWLINE;
		markHeaders(tokens);
		List<List<Token>> lines = splitLines(tokens);
		if (options.braceOnNewLine) {
			lines = splitBraces(lines);
		} else {
			lines = joinBraces(lines);
		}
		String result = new RFormatter.Printer(options, baseIndent, delimiter).print(lines, endsWithNewline);
		verify(text, result, options);
		return result;
	}

	private static String lineDelimiter(String text) {
		int i = text.indexOf('\n');
		if (i > 0 && text.charAt(i - 1) == '\r') {
			return "\r\n";
		}
		if (i >= 0) {
			return "\n";
		}
		return text.indexOf('\r') >= 0 ? "\r" : System.lineSeparator();
	}

	/* Marks each ')' which closes the header of 'if', 'for', 'while', 'function' or '\' */
	private static void markHeaders(List<Token> tokens) {
		Deque<Boolean> stack = new ArrayDeque<>();
		Token previous = null;
		for (Token t : tokens) {
			if (t.kind == Kind.NEWLINE || t.kind == Kind.COMMENT) {
				continue;
			}
			if (t.kind == Kind.OPEN) {
				boolean header = t.text.equals("(") && previous != null
						&& ((previous.kind == Kind.WORD && (HEADER_KEYWORDS.contains(previous.text) || previous.text.equals("function")))
								|| previous.is(Kind.OPERATOR, "\\"));
				stack.push(header);
			} else if (t.kind == Kind.CLOSE && !stack.isEmpty()) {
				t.closesHeader = stack.pop() && t.text.equals(")");
			}
			previous = t;
		}
	}

	private static List<List<Token>> splitLines(List<Token> tokens) {
		List<List<Token>> lines = new ArrayList<>();
		List<Token> line = new ArrayList<>();
		for (Token t : tokens) {
			if (t.kind == Kind.NEWLINE) {
				lines.add(line);
				line = new ArrayList<>();
			} else {
				line.add(t);
			}
		}
		/* The last line (empty if the text ends with a line break) */
		lines.add(line);
		return lines;
	}

	/*
	 * A '{' of a block: after the ')' of a header (if, for, while, function),
	 * 'else' or 'repeat' (not after any ')': 'f(a)' and '{' on the next line are
	 * two expressions)
	 */
	private static boolean isBlockBraceAfter(Token previous) {
		return previous != null && (previous.closesHeader || previous.is(Kind.WORD, "else") || previous.is(Kind.WORD, "repeat"));
	}

	/* braceOnNewLine: 'if (x) {' becomes 'if (x)' and '{' on the next line */
	private static List<List<Token>> splitBraces(List<List<Token>> lines) {
		List<List<Token>> result = new ArrayList<>();
		for (List<Token> line : lines) {
			List<Token> rest = line;
			while (true) {
				int split = -1;
				for (int i = 1; i < rest.size(); i++) {
					if (rest.get(i).is(Kind.OPEN, "{") && isBlockBraceAfter(rest.get(i - 1))) {
						split = i;
						break;
					}
				}
				if (split < 0) {
					result.add(rest);
					break;
				}
				result.add(new ArrayList<>(rest.subList(0, split)));
				rest = new ArrayList<>(rest.subList(split, rest.size()));
			}
		}
		return result;
	}

	/* Not braceOnNewLine: a line starting with the '{' of a block is joined to the line of its header */
	private static List<List<Token>> joinBraces(List<List<Token>> lines) {
		List<List<Token>> result = new ArrayList<>();
		for (List<Token> line : lines) {
			if (!line.isEmpty() && line.get(0).is(Kind.OPEN, "{") && !result.isEmpty()) {
				List<Token> previous = result.get(result.size() - 1);
				if (!previous.isEmpty() && isBlockBraceAfter(previous.get(previous.size() - 1))) {
					line.get(0).spaceBefore = true;
					previous.addAll(line);
					continue;
				}
			}
			result.add(line);
		}
		return result;
	}

	/* ---------------- Printing ---------------- */

	/* An open '(' '[' '{': the indentation level of its line */
	private record Open(String bracket, int level) {
	}

	/* Words before '(' or '[' which are no call (a space stays) */
	private static final Set<String> NO_CALL_WORDS = Set.of("in", "else", "repeat");

	private static final class Printer {
		private final RFormatterOptions o;
		private final String baseIndent;
		private final String delimiter;
		private final String unit;
		private final StringBuilder out = new StringBuilder();
		private final Deque<Open> stack = new ArrayDeque<>();
		/* The previous code line continues on the next line */
		private boolean continues;

		Printer(RFormatterOptions o, String baseIndent, String delimiter) {
			this.o = o;
			this.baseIndent = baseIndent == null ? "" : baseIndent;
			this.delimiter = delimiter;
			this.unit = o.indentUnit();
		}

		String print(List<List<Token>> lines, boolean endsWithNewline) {
			int blank = 0;
			boolean first = true;
			for (List<Token> line : lines) {
				if (line.isEmpty()) {
					blank++;
					continue;
				}
				if (!first) {
					/* The end of the previous line */
					out.append(delimiter);
				}
				int keep = Math.min(blank, Math.max(0, o.maxBlankLines));
				for (int i = 0; i < keep; i++) {
					out.append(delimiter);
				}
				blank = 0;
				first = false;
				printLine(line);
			}
			/* Trailing blank lines are dropped */
			if (endsWithNewline && !first) {
				out.append(delimiter);
			}
			return out.toString();
		}

		private boolean blockContext() {
			return stack.isEmpty() || stack.peek().bracket().equals("{");
		}

		private void printLine(List<Token> line) {
			Token firstToken = line.get(0);
			boolean commentOnly = line.size() == 1 && firstToken.kind == Kind.COMMENT;
			int level;
			if (firstToken.kind == Kind.CLOSE) {
				/* A closing bracket at the start: the level of the line of its opening one */
				level = stack.isEmpty() ? 0 : stack.peek().level();
			} else {
				level = stack.isEmpty() ? 0 : stack.peek().level() + 1;
				if (continues && blockContext() && !firstToken.is(Kind.OPEN, "{")) {
					level += Math.max(0, o.continuationIndent);
				}
			}
			out.append(baseIndent).append(unit.repeat(level));

			Token previous = null;
			boolean previousUnary = false;
			Token lastCode = null;
			for (Token t : line) {
				boolean unary = t.kind == Kind.OPERATOR && isUnary(t, previous);
				if (previous != null) {
					out.append(space(previous, previousUnary, t, unary));
				}
				String text = t.text;
				switch (t.kind) {
				case OPEN -> stack.push(new Open(t.text, level));
				case CLOSE -> {
					if (!stack.isEmpty()) {
						stack.pop();
					}
				}
				case OPERATOR -> {
					if (t.text.equals("=") && o.replaceAssignment && blockContext()) {
						text = "<-";
						t.text = text;
					}
				}
				case COMMENT -> text = stripTrailing(text);
				default -> {
				}
				}
				out.append(text);
				if (t.kind != Kind.COMMENT) {
					lastCode = t;
				}
				previous = t;
				previousUnary = unary;
			}
			if (!commentOnly) {
				/* A trailing operator, a header without '{' or 'else': the statement continues */
				continues = lastCode != null && (lastCode.kind == Kind.OPERATOR || lastCode.closesHeader || lastCode.is(Kind.WORD, "else"));
			}
		}

		/* previous: the token before t on the line (null at the start of the line) */
		private boolean isUnary(Token t, Token previous) {
			if (!UNARY_OPERATORS.contains(t.text)) {
				return false;
			}
			if (t.text.equals("!")) {
				return true;
			}
			if (previous == null) {
				/* At the start of a line */
				return true;
			}
			return switch (previous.kind) {
			case OPERATOR, OPEN, COMMA, SEMICOLON -> true;
			case WORD -> KEYWORDS_BEFORE_OPERAND.contains(previous.text);
			/* The body after a header: 'function(x) -x' */
			case CLOSE -> previous.closesHeader;
			default -> false;
			};
		}

		/* The space between two tokens of a line */
		private String space(Token a, boolean aUnary, Token b, boolean bUnary) {
			if (b.kind == Kind.COMMENT) {
				return " ";
			}
			if (aUnary) {
				return "";
			}
			if (b.kind == Kind.COMMA || b.kind == Kind.SEMICOLON) {
				return "";
			}
			if (a.kind == Kind.COMMA) {
				return o.spaceAfterComma ? " " : "";
			}
			if (a.kind == Kind.SEMICOLON) {
				return " ";
			}
			if (a.kind == Kind.OPEN) {
				if (a.text.equals("{")) {
					return b.is(Kind.CLOSE, "}") ? "" : " ";
				}
				return "";
			}
			if (b.kind == Kind.CLOSE) {
				if (b.text.equals("}")) {
					return a.is(Kind.OPEN, "{") ? "" : " ";
				}
				return "";
			}
			if (a.kind == Kind.OPERATOR && !a.text.equals("\\")) {
				return operatorSpace(a, b);
			}
			if (b.kind == Kind.OPERATOR && !bUnary) {
				return operatorSpace(b, b);
			}
			if (b.kind == Kind.OPERATOR) {
				/* A unary operator after a word (e.g., 'else -1') */
				return " ";
			}
			if (b.is(Kind.OPEN, "(")) {
				if (a.kind == Kind.WORD && HEADER_KEYWORDS.contains(a.text)) {
					return o.spaceBeforeKeywordParenthesis ? " " : "";
				}
				if (isCallee(a)) {
					/* A call (or 'function(' ) */
					return "";
				}
				/* E.g., 'for (i in (1:3))', 'if (a) (b)' */
				return " ";
			}
			if (b.is(Kind.OPEN, "[")) {
				if (isCallee(a)) {
					return "";
				}
				return b.spaceBefore ? " " : "";
			}
			if (b.is(Kind.OPEN, "{")) {
				return " ";
			}
			if (isWordLike(a) && isWordLike(b)) {
				return " ";
			}
			return b.spaceBefore ? " " : "";
		}

		private String operatorSpace(Token operator, Token next) {
			if (TIGHT_OPERATORS.contains(operator.text)) {
				return "";
			}
			return o.spaceAroundOperators ? " " : (next.spaceBefore ? " " : "");
		}

		/* A token which is called or indexed by a following '(' or '[' */
		private static boolean isCallee(Token a) {
			if (a.kind == Kind.WORD) {
				return !NO_CALL_WORDS.contains(a.text) && !HEADER_KEYWORDS.contains(a.text);
			}
			if (a.kind == Kind.CLOSE) {
				return !a.closesHeader;
			}
			return a.kind == Kind.STRING || a.kind == Kind.NUMBER || a.is(Kind.OPERATOR, "\\");
		}

		private static boolean isWordLike(Token t) {
			return t.kind == Kind.WORD || t.kind == Kind.NUMBER || t.kind == Kind.STRING || t.kind == Kind.CLOSE;
		}
	}

	private static String stripTrailing(String s) {
		int end = s.length();
		while (end > 0 && (s.charAt(end - 1) == ' ' || s.charAt(end - 1) == '\t')) {
			end--;
		}
		return s.substring(0, end);
	}

	/* ---------------- Check ---------------- */

	/* The result must have the same tokens as the source (except '=' to '<-' and trailing spaces of comments) */
	private static void verify(String source, String result, RFormatterOptions options) {
		List<Token> a = significant(RTokenizer.tokenize(source));
		List<Token> b = significant(RTokenizer.tokenize(result));
		if (a.size() != b.size()) {
			throw new IllegalStateException("The formatter would change the code (" + a.size() + " tokens, formatted " + b.size() + ")");
		}
		for (int i = 0; i < a.size(); i++) {
			String x = a.get(i).kind == Kind.COMMENT ? stripTrailing(a.get(i).text) : a.get(i).text;
			String y = b.get(i).kind == Kind.COMMENT ? stripTrailing(b.get(i).text) : b.get(i).text;
			boolean assignment = options.replaceAssignment && x.equals("=") && y.equals("<-");
			if (!x.equals(y) && !assignment) {
				throw new IllegalStateException("The formatter would change the code at token " + (i + 1) + ": '" + x + "' -> '" + y + "'");
			}
		}
	}

	private static List<Token> significant(List<Token> tokens) {
		List<Token> result = new ArrayList<>();
		for (Token t : tokens) {
			if (t.kind != Kind.NEWLINE) {
				result.add(t);
			}
		}
		return result;
	}
}
