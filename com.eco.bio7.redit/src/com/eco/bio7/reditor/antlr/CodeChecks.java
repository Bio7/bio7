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
package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

/**
 * Checks of the tokens after the parse (with quick fixes, see CodeFix): a
 * missing comma between arguments, an unclosed string, return without
 * parentheses (the generic parser error is replaced), 1:length(x) and the
 * pipe %>% without magrittr.
 */
public final class CodeChecks {

	/* The packages which provide %>% */
	private static final Set<String> PIPE_PACKAGES = Set.of("magrittr", "dplyr", "tidyverse", "tidyr", "purrr", "dbplyr", "dtplyr", "stringr", "forcats",
			"rvest", "sf");
	private static final Set<String> EXPRESSION_END_WORDS = Set.of("TRUE", "FALSE", "NULL", "NA", "Inf", "NaN", "NA_integer_", "NA_real_",
			"NA_character_", ")", "]", "}");
	private static final Set<String> EXPRESSION_START_WORDS = Set.of("TRUE", "FALSE", "NULL", "NA", "Inf", "NaN", "(", "function", "!", "-", "~");
	private static final Pattern LIBRARY_LINE = Pattern.compile("(?m)^[ \\t]*(?:library|require|requireNamespace)\\s*\\(.*$");

	private CodeChecks() {
	}

	/**
	 * Applies the checks.
	 *
	 * @param tokens  the tokens of the parsed code
	 * @param errors  the errors and warnings (changed)
	 * @param imports the arguments of the library() calls (or null)
	 */
	public static void apply(List<? extends Token> tokens, List<ErrorWarnStore> errors, Collection<String> imports) {
		List<Token> code = new ArrayList<>();
		for (Token t : tokens) {
			if (t.getChannel() == Token.DEFAULT_CHANNEL && t.getType() != Token.EOF) {
				code.add(t);
			}
		}
		if (code.isEmpty()) {
			return;
		}
		String text = code.get(0).getInputStream().getText(Interval.of(0, code.get(0).getInputStream().size() - 1));
		/* The bracket of each code token (the innermost open one) */
		String[] inside = insideBrackets(code);
		unclosedStrings(code, errors);
		multiLineStrings(code, errors);
		returnWithoutParentheses(code, errors);
		missingCommas(code, inside, errors);
		seqAlong(code, text, errors);
		pipe(code, text, errors, imports);
	}

	/* ------------------------------------------------------------ helpers */

	private static boolean isLineBreak(Token t) {
		String x = t.getText();
		return x != null && x.trim().isEmpty() || x != null && x.trim().startsWith("#");
	}

	private static boolean isGeneric(ErrorWarnStore e) {
		String m = e.getMsg();
		return m == null || !(m.startsWith("Err") || m.startsWith("Warn"));
	}

	private static String type(Token t) {
		String n = RLexer.VOCABULARY.getSymbolicName(t.getType());
		return n == null ? "" : n;
	}

	private static boolean endsExpression(Token t) {
		String ty = type(t);
		return ty.equals("ID") || ty.equals("STRING") || ty.equals("INT") || ty.equals("FLOAT") || ty.equals("HEX") || ty.equals("COMPLEX")
				|| EXPRESSION_END_WORDS.contains(t.getText());
	}

	private static boolean startsExpression(Token t) {
		String ty = type(t);
		return ty.equals("ID") || ty.equals("STRING") || ty.equals("INT") || ty.equals("FLOAT") || ty.equals("HEX") || ty.equals("COMPLEX")
				|| EXPRESSION_START_WORDS.contains(t.getText());
	}

	/* The innermost open bracket of each code token ("" outside of brackets) */
	private static String[] insideBrackets(List<Token> code) {
		String[] inside = new String[code.size()];
		java.util.ArrayDeque<String> stack = new java.util.ArrayDeque<>();
		for (int i = 0; i < code.size(); i++) {
			inside[i] = stack.isEmpty() ? "" : stack.peek();
			String x = code.get(i).getText();
			switch (x) {
			case "(", "[", "[[", "{" -> stack.push(x);
			case ")", "]", "}" -> {
				if (!stack.isEmpty()) {
					stack.pop();
				}
			}
			default -> {
			}
			}
		}
		return inside;
	}

	private static int indexOf(List<Token> code, Token t) {
		for (int i = 0; i < code.size(); i++) {
			if (code.get(i).getStartIndex() == t.getStartIndex()) {
				return i;
			}
		}
		return -1;
	}

	/* The previous code token (not a line break) */
	private static Token previous(List<Token> code, int i) {
		for (int j = i - 1; j >= 0; j--) {
			if (!isLineBreak(code.get(j))) {
				return code.get(j);
			}
		}
		return null;
	}

	/* The end of the last code token in the line of the token (not a comment) */
	private static int endOfLine(List<Token> code, int i) {
		int line = code.get(i).getLine();
		int end = code.get(i).getStopIndex() + 1;
		for (int j = i; j < code.size() && code.get(j).getLine() == line; j++) {
			if (!isLineBreak(code.get(j))) {
				end = code.get(j).getStopIndex() + 1;
			}
		}
		return end;
	}

	/**
	 * The offset for a new library() call in R code: after the last library() or
	 * require() line, else at the start of the first line with code.
	 */
	public static int libraryInsertOffset(String code) {
		int insert = -1;
		Matcher m = LIBRARY_LINE.matcher(code);
		while (m.find()) {
			int nl = code.indexOf('\n', m.end());
			insert = nl < 0 ? code.length() : nl + 1;
		}
		if (insert >= 0) {
			return insert;
		}
		Matcher first = Pattern.compile("(?m)^[ \\t]*[^\\s#]").matcher(code);
		return first.find() ? first.start() : 0;
	}

	/* ------------------------------------------------------------ the checks */

	/* "abc (a quote without its closing one): the closing quote at the end of the line */
	private static void unclosedStrings(List<Token> code, List<ErrorWarnStore> errors) {
		for (int i = 0; i < code.size(); i++) {
			Token t = code.get(i);
			String x = t.getText();
			if (!type(t).equals("UNKNOWN") || !(x.equals("\"") || x.equals("'") || x.equals("`"))) {
				continue;
			}
			int line = t.getLine();
			/* The parser errors of this line are consequences */
			errors.removeIf(e -> e.getOffendingSymbol() instanceof Token o && o.getLine() == line && (isGeneric(e) || e.getMsg().startsWith("Err22")));
			int end = endOfLine(code, i);
			CodeFix fix = new CodeFix().add("Insert the closing " + x + " at the end of line " + line, end, 0, x);
			errors.add(new ErrorWarnStore(t, line, t.getCharPositionInLine(), "Err28####The string is not closed (missing " + x + ")!####" + fix.encode()));
		}
	}

	/*
	 * A string over several lines (allowed in R) which contains code or comments:
	 * probably a missing closing quote (the string ends at the next quote of the
	 * file)
	 */
	private static void multiLineStrings(List<Token> code, List<ErrorWarnStore> errors) {
		Pattern codeLine = Pattern.compile("^\\s*(#|[A-Za-z_.][\\w.]*\\s*(<-|=(?!=)|\\())");
		for (Token t : code) {
			String x = t.getText();
			if (!type(t).equals("STRING") || x.indexOf('\n') < 0 || x.length() < 2) {
				continue;
			}
			String[] lines = x.split("\\R", -1);
			boolean looksLikeCode = false;
			for (int i = 1; i < lines.length - 1; i++) {
				if (codeLine.matcher(lines[i]).find()) {
					looksLikeCode = true;
					break;
				}
			}
			if (!looksLikeCode) {
				continue;
			}
			String quote = x.substring(0, 1);
			int endOfFirstLine = t.getStartIndex() + lines[0].length();
			int lastLine = t.getLine() + lines.length - 1;
			/* The parser errors of the lines of the string are consequences */
			errors.removeIf(e -> isGeneric(e) && e.getOffendingSymbol() instanceof Token o && o.getLine() >= t.getLine() && o.getLine() <= lastLine + 1);
			CodeFix fix = new CodeFix().add("Insert the closing " + quote + " at the end of line " + t.getLine(), endOfFirstLine, 0, quote);
			errors.add(new ErrorWarnStore(t, t.getLine(), t.getCharPositionInLine(), "Warn28####The string continues over " + lines.length
					+ " lines (to line " + lastLine + "): is the closing " + quote + " missing?####" + fix.encode()));
		}
	}

	/* return x -> return(x) */
	private static void returnWithoutParentheses(List<Token> code, List<ErrorWarnStore> errors) {
		for (ErrorWarnStore e : new ArrayList<>(errors)) {
			if (!isGeneric(e) || !(e.getOffendingSymbol() instanceof Token t)) {
				continue;
			}
			int i = indexOf(code, t);
			Token p = i < 0 ? null : previous(code, i);
			if (p == null || !p.getText().equals("return") || t.getText().equals("(") || t.getLine() != p.getLine()) {
				continue;
			}
			/* The end of the returned expression: the end of the line (in the brackets of the line) */
			int end = endOfExpression(code, i);
			CodeFix fix = new CodeFix().add("Change to return(...)",
					List.of(new CodeFix.Edit(p.getStopIndex() + 1, t.getStartIndex() - p.getStopIndex() - 1, "("), new CodeFix.Edit(end, 0, ")")));
			errors.remove(e);
			/* The following errors of the block are consequences (e.g., the closing brace) */
			int after = end;
			errors.removeIf(x -> isGeneric(x) && x.getOffendingSymbol() instanceof Token o && o.getStartIndex() >= after && o.getText().equals("}"));
			errors.add(new ErrorWarnStore(p, p.getLine(), p.getCharPositionInLine(), "Err29####return needs parentheses: return(...)!####" + fix.encode()));
		}
	}

	/* The end of the expression which starts at the token: the line, without a closing brace of the block */
	private static int endOfExpression(List<Token> code, int i) {
		int depth = 0;
		int end = code.get(i).getStopIndex() + 1;
		int line = code.get(i).getLine();
		for (int j = i; j < code.size(); j++) {
			Token t = code.get(j);
			String x = t.getText();
			if (isLineBreak(t) && depth == 0 || x.equals(";")) {
				break;
			}
			if (x.equals("(") || x.equals("[") || x.equals("[[") || x.equals("{")) {
				depth++;
			} else if (x.equals(")") || x.equals("]") || x.equals("}")) {
				if (depth == 0) {
					break;
				}
				depth--;
			}
			if (!isLineBreak(t)) {
				end = t.getStopIndex() + 1;
			}
			if (depth == 0 && t.getLine() != line && isLineBreak(t)) {
				break;
			}
		}
		return end;
	}

	/* f(a b) -> f(a, b) */
	private static void missingCommas(List<Token> code, String[] inside, List<ErrorWarnStore> errors) {
		for (ErrorWarnStore e : new ArrayList<>(errors)) {
			if (!isGeneric(e) || !(e.getOffendingSymbol() instanceof Token t)) {
				continue;
			}
			int i = indexOf(code, t);
			if (i < 0 || !(inside[i].equals("(") || inside[i].equals("[") || inside[i].equals("[["))) {
				continue;
			}
			Token p = previous(code, i);
			if (p == null || !endsExpression(p) || !startsExpression(t) || p.getText().equals("return") || p.getText().equals("function")) {
				continue;
			}
			CodeFix fix = new CodeFix().add("Insert ',' after '" + p.getText() + "'", p.getStopIndex() + 1, 0, ",");
			errors.remove(e);
			errors.add(new ErrorWarnStore(t, t.getLine(), t.getCharPositionInLine(),
					"Err27####Missing ',' between the arguments (or an operator)?####" + fix.encode()));
		}
	}

	/* 1:length(x) -> seq_along(x), 1:nrow(x) -> seq_len(nrow(x)) */
	private static void seqAlong(List<Token> code, String text, List<ErrorWarnStore> errors) {
		for (int i = 0; i + 3 < code.size(); i++) {
			Token one = code.get(i);
			if (!(one.getText().equals("1") || one.getText().equals("1L")) || !code.get(i + 1).getText().equals(":") || !code.get(i + 3).getText().equals("(")) {
				continue;
			}
			String f = code.get(i + 2).getText();
			if (!Set.of("length", "nrow", "ncol", "NROW", "NCOL").contains(f)) {
				continue;
			}
			/* The closing bracket of the call */
			int depth = 0;
			Token close = null;
			for (int j = i + 3; j < code.size(); j++) {
				String x = code.get(j).getText();
				if (x.equals("(")) {
					depth++;
				} else if (x.equals(")") && --depth == 0) {
					close = code.get(j);
					break;
				}
			}
			if (close == null) {
				continue;
			}
			String arg = text.substring(code.get(i + 3).getStopIndex() + 1, close.getStartIndex()).trim();
			String replacement = f.equals("length") ? "seq_along(" + arg + ")" : "seq_len(" + f + "(" + arg + "))";
			CodeFix fix = new CodeFix().add("Replace with " + replacement, one.getStartIndex(), close.getStopIndex() + 1 - one.getStartIndex(), replacement);
			errors.add(new ErrorWarnStore(one, one.getLine(), one.getCharPositionInLine(),
					"Warn24####1:" + f + "(...) is 1:0 (the numbers 1 and 0) if it is 0: " + replacement + " is safe!####" + fix.encode()));
		}
	}

	/* %>% without a package which provides it: library(magrittr) or the native pipe |> */
	private static void pipe(List<Token> code, String text, List<ErrorWarnStore> errors, Collection<String> imports) {
		if (imports != null) {
			for (String i : imports) {
				String name = i.replace("\"", "").replace("'", "");
				int comma = name.indexOf(',');
				if (PIPE_PACKAGES.contains((comma >= 0 ? name.substring(0, comma) : name).trim())) {
					return;
				}
			}
		}
		if (com.eco.bio7.reditors.RLiveNames.exists("%>%")) {
			return;
		}
		for (Token t : code) {
			if (t.getText().equals("%>%")) {
				int insert = libraryInsertOffset(text);
				CodeFix fix = new CodeFix().add("Replace with the native pipe |>", t.getStartIndex(), t.getStopIndex() + 1 - t.getStartIndex(), "|>")
						.add("Add library(magrittr)", insert, 0, "library(magrittr)\n");
				errors.add(new ErrorWarnStore(t, t.getLine(), t.getCharPositionInLine(),
						"Warn26####%>% needs the package magrittr (e.g., with library(dplyr)) or the native pipe |>!####" + fix.encode()));
				/* Once */
				return;
			}
		}
	}
}
