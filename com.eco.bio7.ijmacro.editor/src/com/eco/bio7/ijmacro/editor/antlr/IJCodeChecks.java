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
package com.eco.bio7.ijmacro.editor.antlr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.Token;

/**
 * Checks of the tokens of an ImageJ macro after the parse, with quick fixes
 * (see CodeFix): unclosed and unopened brackets (marked at the bracket, not at
 * a later token), an unclosed string, a missing comma between arguments and a
 * missing ';' at the end of a line (ImageJ needs it, the grammar of the editor
 * accepts a line break). The generic parser errors which are only consequences
 * are removed.
 */
public final class IJCodeChecks {

	private record Open(Token token, String close) {
	}

	private static final Set<String> HEADER_WORDS = Set.of("if", "else", "for", "while", "do", "function", "macro");
	private static final Set<String> CONTINUATION = Set.of(")", "]", "}", ".", ",", "+", "-", "*", "/", "%", "&&", "||", "&", "|", "^", "==", "!=", "<",
			">", "<=", ">=", "=", "+=", "-=", "*=", "/=", "?", ":", "else", "{", ";");

	private IJCodeChecks() {
	}

	/** Applies the checks to the errors and warnings of the parse (changed). */
	public static void apply(List<? extends Token> tokens, List<ErrorWarnStore> errors) {
		List<Token> code = new ArrayList<>();
		for (Token t : tokens) {
			if (t.getChannel() == Token.DEFAULT_CHANNEL && t.getType() != Token.EOF && t.getText() != null && !t.getText().isBlank()) {
				code.add(t);
			}
		}
		if (code.isEmpty()) {
			return;
		}
		unclosedStrings(code, errors);
		brackets(code, errors);
		missingCommas(code, errors);
		missingSemicolons(code, errors);
	}

	/* ------------------------------------------------------------ helpers */

	private static boolean isGeneric(ErrorWarnStore e) {
		String m = e.getMsg();
		return m == null || !(m.startsWith("Err") || m.startsWith("Warn"));
	}

	private static String type(Token t) {
		String n = ImageJMacroLexer.VOCABULARY.getSymbolicName(t.getType());
		return n == null ? "" : n;
	}

	private static boolean endsExpression(Token t) {
		String ty = type(t);
		String x = t.getText();
		return ty.equals("Identifier") || ty.endsWith("Literal") || x.equals(")") || x.equals("]") || x.equals("true") || x.equals("false")
				|| x.equals("NaN") || x.equals("PI") || x.equals("++") || x.equals("--");
	}

	private static boolean startsExpression(Token t) {
		String ty = type(t);
		String x = t.getText();
		return ty.equals("Identifier") || ty.endsWith("Literal") || x.equals("(") || x.equals("!") || x.equals("true") || x.equals("false")
				|| x.equals("newArray");
	}

	private static int indexOf(List<Token> code, Token t) {
		for (int i = 0; i < code.size(); i++) {
			if (code.get(i).getStartIndex() == t.getStartIndex()) {
				return i;
			}
		}
		return -1;
	}

	/* The end of the last code token of the line of the token */
	private static int endOfLine(List<Token> code, int i) {
		int line = code.get(i).getLine();
		int end = code.get(i).getStopIndex() + 1;
		for (int j = i; j < code.size() && code.get(j).getLine() == line; j++) {
			end = code.get(j).getStopIndex() + 1;
		}
		return end;
	}

	private static void add(List<ErrorWarnStore> errors, Token t, String code, String message, CodeFix fix) {
		errors.add(new ErrorWarnStore(t, t.getLine(), t.getCharPositionInLine(), code + "####" + message + (fix == null || fix.isEmpty() ? "" : "####" + fix.encode())));
	}

	/* ------------------------------------------------------------ strings */

	/* "abc (a quote without its closing one): the closing quote at the end of the line */
	private static void unclosedStrings(List<Token> code, List<ErrorWarnStore> errors) {
		for (int i = 0; i < code.size(); i++) {
			Token t = code.get(i);
			String x = t.getText();
			if (type(t).equals("StringLiteral") || !(x.equals("\"") || x.equals("'"))) {
				continue;
			}
			int line = t.getLine();
			errors.removeIf(e -> isGeneric(e) && e.getOffendingSymbol() instanceof Token o && o.getLine() == line);
			/* Before a ';' at the end of the line (the end of the statement) */
			int end = endOfLine(code, i);
			int last = i;
			while (last + 1 < code.size() && code.get(last + 1).getLine() == line) {
				last++;
			}
			boolean semicolon = last > i && code.get(last).getText().equals(";");
			int at = semicolon ? code.get(last).getStartIndex() : end;
			add(errors, t, "Err28", "The string is not closed (missing " + x + ")!",
					new CodeFix().add("Insert the closing " + x + (semicolon ? " before the ';'" : " at the end of line " + line), at, 0, x));
		}
	}

	/* ------------------------------------------------------------ brackets */

	private static void brackets(List<Token> code, List<ErrorWarnStore> errors) {
		Deque<Open> stack = new ArrayDeque<>();
		List<Open> unclosed = new ArrayList<>();
		List<Token> unopened = new ArrayList<>();
		for (Token t : code) {
			switch (t.getText()) {
			case "(" -> stack.push(new Open(t, ")"));
			case "[" -> stack.push(new Open(t, "]"));
			case "{" -> stack.push(new Open(t, "}"));
			case ")", "]", "}" -> {
				if (!close(stack, t.getText(), unclosed)) {
					unopened.add(t);
				}
			}
			default -> {
			}
			}
		}
		unclosed.addAll(stack);
		for (Token t : unopened) {
			unopened(code, errors, t);
		}
		if (unclosed.isEmpty()) {
			return;
		}
		int first = Integer.MAX_VALUE;
		for (Open o : unclosed) {
			first = Math.min(first, o.token().getStartIndex());
		}
		/* The generic errors after the first unclosed bracket are consequences; the first after it is where the parser failed */
		int failedAt = Integer.MAX_VALUE;
		for (ErrorWarnStore e : new ArrayList<>(errors)) {
			if (isGeneric(e) && e.getOffendingSymbol() instanceof Token t && (t.getType() == Token.EOF || t.getStartIndex() >= first)) {
				if (t.getType() != Token.EOF && t.getStartIndex() > first) {
					failedAt = Math.min(failedAt, t.getStartIndex());
				}
				errors.remove(e);
			}
		}
		for (Open o : unclosed) {
			Token t = o.token();
			String close = o.close();
			int before = t.getStopIndex() + 1;
			int end = failedAt;
			String insert = close;
			if (close.equals("}")) {
				/*
				 * In a block every line is a statement (the parser fails only at the end): the
				 * block ends before the next line indented like the line of the bracket
				 */
				Token outdented = nextOutdentedLine(code, t);
				if (outdented != null) {
					end = outdented.getStartIndex();
				}
				insert = System.lineSeparator() + indentation(code, t) + "}";
			}
			for (Token c : code) {
				if (c.getStartIndex() >= end || c.getText().equals(";") && c.getStartIndex() > t.getStartIndex() && !close.equals("}")) {
					break;
				}
				if (c.getStartIndex() > t.getStartIndex()) {
					before = c.getStopIndex() + 1;
				}
			}
			add(errors, t, "Err26", "'" + t.getText() + "' is not closed (missing '" + close + "')!",
					new CodeFix().add("Insert '" + close + "'", before, 0, insert));
		}
	}

	/* The first code token of the line of the token */
	private static Token firstOfLine(List<Token> code, Token token) {
		for (Token t : code) {
			if (t.getLine() == token.getLine()) {
				return t;
			}
			if (t.getStartIndex() >= token.getStartIndex()) {
				break;
			}
		}
		return token;
	}

	/* The indentation (spaces and tabs) of the line of the bracket */
	private static String indentation(List<Token> code, Token open) {
		Token first = firstOfLine(code, open);
		int column = first.getCharPositionInLine();
		int lineStart = first.getStartIndex() - column;
		if (column <= 0 || lineStart < 0 || first.getInputStream() == null) {
			return "";
		}
		String ws = first.getInputStream().getText(org.antlr.v4.runtime.misc.Interval.of(lineStart, first.getStartIndex() - 1));
		return ws.matches("[ \\t]*") ? ws : "";
	}

	/*
	 * The first code token of a line after the bracket which is indented like the
	 * line of the bracket or less (null if none or the block is not indented)
	 */
	private static Token nextOutdentedLine(List<Token> code, Token open) {
		int indent = firstOfLine(code, open).getCharPositionInLine();
		int line = open.getLine();
		for (Token t : code) {
			if (t.getType() == Token.EOF || t.getStartIndex() <= open.getStartIndex() || t.getLine() <= line) {
				continue;
			}
			boolean firstLine = line == open.getLine();
			line = t.getLine();
			if (t.getCharPositionInLine() <= indent) {
				return firstLine ? null : t;
			}
		}
		return null;
	}

	private static boolean close(Deque<Open> stack, String x, List<Open> unclosed) {
		Open top = stack.peek();
		if (top == null) {
			return false;
		}
		if (top.close().equals(x)) {
			stack.pop();
			return true;
		}
		boolean found = false;
		for (Open o : stack) {
			if (o.close().equals(x)) {
				found = true;
				break;
			}
		}
		if (!found) {
			return false;
		}
		while (!stack.isEmpty() && !stack.peek().close().equals(x)) {
			unclosed.add(stack.pop());
		}
		return close(stack, x, unclosed);
	}

	/* A closing bracket without its opening one: insert the opening one where the parser failed, or remove it */
	private static void unopened(List<Token> code, List<ErrorWarnStore> errors, Token close) {
		String x = close.getText();
		String open = x.equals(")") ? "(" : x.equals("]") ? "[" : "{";
		int statementStart = 0;
		for (Token t : code) {
			if (t.getStartIndex() >= close.getStartIndex()) {
				break;
			}
			if (t.getText().equals(";") || t.getText().equals("{") || t.getText().equals("}")) {
				statementStart = t.getStopIndex() + 1;
			}
		}
		int start = statementStart;
		errors.removeIf(e -> isGeneric(e) && e.getOffendingSymbol() instanceof Token t && t.getStartIndex() >= start && t.getStartIndex() <= close.getStartIndex() + 1);
		CodeFix fix = new CodeFix().add("Remove '" + x + "'", close.getStartIndex(), close.getStopIndex() + 1 - close.getStartIndex(), "");
		add(errors, close, "Err30", "'" + x + "' has no opening '" + open + "'!", fix);
	}

	/* ------------------------------------------------------------ commas, semicolons */

	/* print(x y) -> print(x, y) */
	private static void missingCommas(List<Token> code, List<ErrorWarnStore> errors) {
		/* The innermost bracket of each token */
		String[] inside = new String[code.size()];
		int[] openAt = new int[code.size()];
		Deque<Integer> stack = new ArrayDeque<>();
		for (int i = 0; i < code.size(); i++) {
			inside[i] = stack.isEmpty() ? "" : code.get(stack.peek()).getText();
			openAt[i] = stack.isEmpty() ? -1 : code.get(stack.peek()).getStartIndex();
			String x = code.get(i).getText();
			if (x.equals("(") || x.equals("[") || x.equals("{")) {
				stack.push(i);
			} else if ((x.equals(")") || x.equals("]") || x.equals("}")) && !stack.isEmpty()) {
				stack.pop();
			}
		}
		for (ErrorWarnStore e : new ArrayList<>(errors)) {
			if (!isGeneric(e) || !(e.getOffendingSymbol() instanceof Token t)) {
				continue;
			}
			int i = indexOf(code, t);
			if (i <= 0 || !(inside[i].equals("(") || inside[i].equals("["))) {
				continue;
			}
			Token p = code.get(i - 1);
			if (!endsExpression(p) || !startsExpression(t)) {
				continue;
			}
			int from = openAt[i];
			int to = endOfLine(code, i);
			/* The errors of this call are consequences (e.g., at its opening bracket, at the end of the statement) */
			errors.removeIf(x -> isGeneric(x) && x.getOffendingSymbol() instanceof Token o && o.getStartIndex() >= from && o.getStartIndex() <= to);
			add(errors, t, "Err27", "Missing ',' between the arguments (or an operator)?",
					new CodeFix().add("Insert ',' after '" + p.getText() + "'", p.getStopIndex() + 1, 0, ","));
		}
	}

	/*
	 * A line which ends a statement without ';' (ImageJ: "';' expected"): the
	 * expression ends at the end of the line, outside of brackets, and the next
	 * line doesn't continue it; not after a header (if (...), for, function)
	 */
	private static void missingSemicolons(List<Token> code, List<ErrorWarnStore> errors) {
		int depth = 0;
		int lineStart = 0;
		for (int i = 0; i < code.size(); i++) {
			Token t = code.get(i);
			String x = t.getText();
			if (i > 0 && t.getLine() != code.get(i - 1).getLine()) {
				lineStart = i;
			}
			if (x.equals("(") || x.equals("[")) {
				depth++;
			} else if ((x.equals(")") || x.equals("]")) && depth > 0) {
				depth--;
			}
			boolean lastOfLine = i + 1 >= code.size() || code.get(i + 1).getLine() != t.getLine();
			if (!lastOfLine || depth != 0 || !endsExpression(t)) {
				continue;
			}
			Token next = i + 1 < code.size() ? code.get(i + 1) : null;
			if (next != null && CONTINUATION.contains(next.getText())) {
				continue;
			}
			/* A header: if (...), while (...), for (...), function f(...), else */
			String first = code.get(lineStart).getText();
			if (HEADER_WORDS.contains(first) || (x.equals(")") && lineStart > 0 && code.get(lineStart - 1).getText().equals("else"))) {
				continue;
			}
			int line = t.getLine();
			/* Another error of the line is more important */
			boolean other = false;
			for (ErrorWarnStore e : errors) {
				if (e.getOffendingSymbol() instanceof Token o && (o.getLine() == line || next != null && o.getLine() == next.getLine() && !isGeneric(e))) {
					other = true;
					break;
				}
			}
			if (other) {
				continue;
			}
			add(errors, t, "Err31", "Missing ';' at the end of the statement!",
					new CodeFix().add("Insert ';' at the end of line " + line, t.getStopIndex() + 1, 0, ";"));
		}
	}
}
