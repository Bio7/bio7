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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.Token;

/**
 * Marks unclosed brackets where they are: an unclosed '(', '[', '[[' or '{'
 * continues the expression over the following lines (as in R), so the parser
 * reports the error at a later token (e.g., the next function or line). The
 * bracket gets the error and the generic errors of the parser after it (only
 * consequences) are removed; the errors of the code analysis stay.
 */
public final class BracketCheck {

	/* Strings and comments are tokens of their own (their brackets don't count) */
	private record Open(Token token, String close, int closes) {
	}

	private BracketCheck() {
	}

	/**
	 * Adds the errors of the unclosed brackets of the tokens.
	 *
	 * @param tokens the tokens of the parsed code
	 * @param errors the errors and warnings of the parse (changed)
	 */
	public static void apply(List<? extends Token> tokens, List<ErrorWarnStore> errors) {
		Deque<Open> stack = new ArrayDeque<>();
		List<Open> unclosed = new ArrayList<>();
		List<Token> unopened = new ArrayList<>();
		for (Token t : tokens) {
			if (t.getChannel() != Token.DEFAULT_CHANNEL || t.getType() == Token.EOF) {
				continue;
			}
			String x = t.getText();
			if (x == null) {
				continue;
			}
			switch (x) {
			case "(" -> stack.push(new Open(t, ")", 1));
			case "[" -> stack.push(new Open(t, "]", 1));
			case "[[" -> stack.push(new Open(t, "]", 2));
			case "{" -> stack.push(new Open(t, "}", 1));
			case ")", "]", "}" -> {
				if (!close(stack, x, unclosed)) {
					unopened.add(t);
				}
			}
			default -> {
			}
			}
		}
		for (Open o : stack) {
			unclosed.add(o);
		}
		for (Token c : unopened) {
			unopened(tokens, errors, c);
		}
		if (unclosed.isEmpty()) {
			return;
		}
		int first = Integer.MAX_VALUE;
		for (Open o : unclosed) {
			first = Math.min(first, o.token().getStartIndex());
		}
		/*
		 * The generic errors of the parser after the first unclosed bracket are
		 * consequences; the first of them is where the parser failed (the next
		 * statement): the closing bracket belongs before it
		 */
		int failedAt = Integer.MAX_VALUE;
		for (Iterator<ErrorWarnStore> it = errors.iterator(); it.hasNext();) {
			ErrorWarnStore e = it.next();
			String msg = e.getMsg();
			boolean ownMessage = msg != null && (msg.startsWith("Err") || msg.startsWith("Warn"));
			if (!ownMessage && e.getOffendingSymbol() instanceof Token t && (t.getType() == Token.EOF || t.getStartIndex() >= first)) {
				failedAt = Math.min(failedAt, t.getType() == Token.EOF ? Integer.MAX_VALUE : t.getStartIndex());
				it.remove();
			}
		}
		for (Open o : unclosed) {
			Token t = o.token();
			String open = t.getText();
			/* The missing closing brackets ('[[' with one ']' needs one more) */
			String close = o.close().repeat(o.closes());
			/*
			 * Err26 (quick fix): the replacement is "text@offset[@offset2]": the closing
			 * bracket after the last code before the failed statement and at the end of
			 * the line of the bracket
			 */
			int end = failedAt;
			String insert = close;
			if (close.equals("}")) {
				/*
				 * In a block every line is a statement (the parser fails only at the end): the
				 * block ends before the next line indented like the line of the bracket (the
				 * errors of the parser in an unclosed block are not reliable)
				 */
				Token outdented = nextOutdentedLine(tokens, t);
				if (outdented != null) {
					end = outdented.getStartIndex();
				}
				insert = System.lineSeparator() + indentation(tokens, t) + "}";
			}
			int before = endOfCodeBefore(tokens, end, t);
			int lineEnd = close.equals("}") ? -1 : endOfCodeInLine(tokens, t);
			String fix = insert.replace("\n", "\\n").replace("\r", "\\r") + "@" + before + (lineEnd != before && lineEnd >= 0 ? "@" + lineEnd : "");
			errors.add(new ErrorWarnStore(t, t.getLine(), t.getCharPositionInLine(),
					"Err26####'" + open + "' is not closed (missing '" + close + "')!####" + fix));
		}
	}

	/* The first code token of the line of the token */
	private static Token firstOfLine(List<? extends Token> tokens, Token token) {
		for (Token t : tokens) {
			if (t.getLine() == token.getLine() && t.getChannel() == Token.DEFAULT_CHANNEL && isCode(t)) {
				return t;
			}
			if (t.getStartIndex() >= token.getStartIndex()) {
				break;
			}
		}
		return token;
	}

	/* The indentation (spaces and tabs) of the line of the bracket */
	private static String indentation(List<? extends Token> tokens, Token open) {
		Token first = firstOfLine(tokens, open);
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
	 * line of the bracket or less (null if none)
	 */
	private static Token nextOutdentedLine(List<? extends Token> tokens, Token open) {
		int indent = firstOfLine(tokens, open).getCharPositionInLine();
		int line = open.getLine();
		for (Token t : tokens) {
			if (t.getType() == Token.EOF) {
				break;
			}
			if (t.getStartIndex() <= open.getStartIndex() || t.getChannel() != Token.DEFAULT_CHANNEL || !isCode(t) || t.getLine() <= line) {
				continue;
			}
			/* The first code token of a new line */
			boolean firstLine = line == open.getLine();
			line = t.getLine();
			if (t.getCharPositionInLine() <= indent) {
				/* The code of the block is not indented: no indentation to follow */
				return firstLine ? null : t;
			}
		}
		return null;
	}

	/* The end of the last code token before the offset (not a comment or a line break), after the bracket */
	private static int endOfCodeBefore(List<? extends Token> tokens, int offset, Token open) {
		int end = open.getStopIndex() + 1;
		for (Token t : tokens) {
			if (t.getType() == Token.EOF || t.getStartIndex() >= offset) {
				break;
			}
			if (t.getChannel() == Token.DEFAULT_CHANNEL && t.getStartIndex() > open.getStartIndex() && isCode(t)) {
				end = t.getStopIndex() + 1;
			}
		}
		return end;
	}

	/*
	 * The end of the last code token in the line of the bracket; -1 if the line
	 * continues (ends with an operator or a comma: the bracket can't be there)
	 */
	private static int endOfCodeInLine(List<? extends Token> tokens, Token open) {
		Token last = null;
		for (Token t : tokens) {
			if (t.getType() == Token.EOF || t.getLine() > open.getLine()) {
				break;
			}
			if (t.getLine() == open.getLine() && t.getChannel() == Token.DEFAULT_CHANNEL && t.getStartIndex() >= open.getStartIndex() && isCode(t)) {
				last = t;
			}
		}
		if (last == null) {
			return -1;
		}
		String x = last.getText();
		if (x.matches("[-+*/^~,=<>!&|$@:]+|%.*%|\\|>|<-|<<-|->") || last == open) {
			return -1;
		}
		return last.getStopIndex() + 1;
	}

	/* Code (not a line break, which may contain a comment in the grammar of R) */
	private static boolean isCode(Token t) {
		String x = t.getText();
		if (x == null) {
			return false;
		}
		String trimmed = x.trim();
		return !trimmed.isEmpty() && !trimmed.startsWith("#");
	}

	/*
	 * A closing bracket without its opening one: the opening bracket belongs where
	 * the parser failed in the statement (e.g., mean 1, 2) -> mean(1, 2), if x > 1)
	 * -> if (x > 1)); or the closing bracket is too much
	 */
	private static void unopened(List<? extends Token> tokens, List<ErrorWarnStore> errors, Token close) {
		String x = close.getText();
		String open = switch (x) {
		case ")" -> "(";
		case "]" -> "[";
		default -> "{";
		};
		/* The start of the statement: after the last line break or ';' before the bracket */
		int statementStart = 0;
		Token previous = null;
		Token failed = null;
		Token beforeFailed = null;
		List<Token> code = new ArrayList<>();
		for (Token t : tokens) {
			if (t.getStartIndex() >= close.getStartIndex() || t.getType() == Token.EOF) {
				break;
			}
			if (t.getChannel() != Token.DEFAULT_CHANNEL) {
				continue;
			}
			String tx = t.getText();
			if (tx.equals(";") || tx.trim().isEmpty() || tx.trim().startsWith("#")) {
				statementStart = t.getStopIndex() + 1;
				code.clear();
				continue;
			}
			code.add(t);
		}
		/* The first parser error of the statement (the place of the missing opening bracket) */
		int firstError = Integer.MAX_VALUE;
		for (ErrorWarnStore e : errors) {
			if (e.getOffendingSymbol() instanceof Token t && t.getStartIndex() >= statementStart && t.getStartIndex() < close.getStartIndex()
					&& !(e.getMsg() != null && e.getMsg().startsWith("Warn"))) {
				firstError = Math.min(firstError, t.getStartIndex());
			}
		}
		for (Token t : code) {
			if (t.getStartIndex() == firstError) {
				failed = t;
				beforeFailed = previous;
				break;
			}
			previous = t;
		}
		int statementStartFinal = statementStart;
		/* The errors of the parser in the statement are consequences (also "Too many parentheses") */
		errors.removeIf(e -> e.getOffendingSymbol() instanceof Token t && t.getStartIndex() >= statementStartFinal && t.getStartIndex() <= close.getStartIndex()
				&& (e.getMsg() == null || !(e.getMsg().startsWith("Warn") || e.getMsg().startsWith("Err26"))));
		CodeFix fix = new CodeFix();
		if (failed != null && beforeFailed != null && !open.equals("{")) {
			String word = beforeFailed.getText();
			if (Set.of("if", "while", "for", "switch").contains(word)) {
				/* if (x > 1) */
				fix.add("Insert '" + open + "' before '" + failed.getText() + "'", failed.getStartIndex(), 0, open);
			} else if (word.matches("[A-Za-z.][A-Za-z0-9._]*|\\)|\\]")) {
				/* mean(1, 2), m[1, 2] (the space between the name and the bracket is removed) */
				fix.add("Insert '" + open + "' after '" + word + "'", beforeFailed.getStopIndex() + 1, failed.getStartIndex() - beforeFailed.getStopIndex() - 1, open);
			}
		}
		fix.add("Remove '" + x + "'", close.getStartIndex(), close.getStopIndex() + 1 - close.getStartIndex(), "");
		errors.add(new ErrorWarnStore(close, close.getLine(), close.getCharPositionInLine(),
				"Err30####'" + x + "' has no opening '" + open + "'!####" + fix.encode()));
	}

	/* A closing bracket: its opening one is closed (the brackets opened after it are unclosed); false if it has none */
	private static boolean close(Deque<Open> stack, String x, List<Open> unclosed) {
		Open top = stack.peek();
		if (top == null) {
			/* Too many closing brackets */
			return false;
		}
		if (top.close().equals(x)) {
			stack.pop();
			if (top.closes() == 2) {
				/* '[[' needs ']]' */
				stack.push(new Open(top.token(), "]", 1));
			}
			return true;
		}
		/* Is there an opening bracket of this kind below? Then the ones above it are not closed */
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
}
