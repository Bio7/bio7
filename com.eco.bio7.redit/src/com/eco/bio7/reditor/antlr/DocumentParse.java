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
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.jface.text.Document;

import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;

/**
 * Parses R code without an R editor (e.g., the R chunks of a markdown
 * document, the other text replaced by spaces: the offsets and lines are the
 * ones of the document): the errors and warnings of the code analysis of the R
 * editor and the scope (variables, functions) at an offset for the code
 * completion. No outline, folding or markers (see Parse for the R editor).
 */
public final class DocumentParse {

	private DocumentParse() {
	}

	/** The result of a parse: the listener of the scope and the errors and warnings. */
	public record Result(RRefPhaseListen ref, ArrayList<ErrorWarnStore> errorsAndWarnings) {
	}

	/** The errors and warnings of the R code (as the R editor marks them). */
	public static ArrayList<ErrorWarnStore> errorsAndWarnings(String code) {
		return parse(code, -1).errorsAndWarnings();
	}

	/** The errors and warnings and the packages of the library() and require() calls. */
	public static Result parseAll(String code) {
		return parse(code, -1);
	}

	/**
	 * The errors and warnings of separate code parts of one document (e.g., the R
	 * chunks of a markdown document, as knitr parses each chunk on its own): the
	 * syntax errors of each part, the warnings of all parts together (variables of
	 * the parts above are known). A part with a syntax error is left out of the
	 * common analysis (it would continue into the next part).
	 *
	 * @param code  the code of the document (the other text replaced by spaces)
	 * @param parts the [start, end) offsets of the parts
	 */
	public static Result parseParts(String code, List<int[]> parts) {
		ArrayList<ErrorWarnStore> all = new ArrayList<>();
		List<int[]> failed = new ArrayList<>();
		for (int[] part : parts) {
			Result r = parse(only(code, List.of(part), true), -1);
			boolean error = false;
			for (ErrorWarnStore e : r.errorsAndWarnings()) {
				String msg = e.getMsg();
				/* The warnings of a part alone are wrong (the variables of the parts above are not known) */
				if (msg == null || !msg.startsWith("Warn")) {
					all.add(e);
					error = true;
				}
			}
			if (error) {
				failed.add(part);
			}
		}
		Result whole = parse(failed.isEmpty() ? code : only(code, failed, false), -1);
		for (ErrorWarnStore e : whole.errorsAndWarnings()) {
			String msg = e.getMsg();
			if (msg != null && msg.startsWith("Warn")) {
				all.add(e);
			}
		}
		return new Result(whole.ref(), all);
	}

	/* The code with only the parts (keep = true) or without them (spaces, the line breaks stay) */
	private static String only(String code, List<int[]> parts, boolean keep) {
		char[] c = code.toCharArray();
		boolean[] inPart = new boolean[c.length];
		for (int[] p : parts) {
			for (int i = Math.max(0, p[0]); i < Math.min(c.length, p[1]); i++) {
				inPart[i] = true;
			}
		}
		for (int i = 0; i < c.length; i++) {
			if (inPart[i] != keep && c[i] != '\n' && c[i] != '\r') {
				c[i] = ' ';
			}
		}
		return new String(c);
	}

	/** The scope at the offset (the variables and functions, a function call) for the code completion. */
	public static RRefPhaseListen parseFromOffset(String code, int offset) {
		return parse(code, offset).ref();
	}

	private static Result parse(String code, int offset) {
		RLexer lexer = new RLexer(CharStreams.fromString(code));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		UnderlineListener li = new UnderlineListener();
		RFilter filter = new RFilter(tokens);
		/* No messages of the filter on the console */
		filter.removeErrorListeners();
		filter.stream();
		tokens.seek(0);
		RParser parser = new RParser(tokens);
		/* The error messages of the R editor (custom strategy) */
		parser.setErrorHandler(new RErrorStrategy());
		parser.setBuildParseTree(true);
		lexer.removeErrorListeners();
		parser.removeErrorListeners();
		parser.addErrorListener(li);
		ParseTreeWalker walker = new ParseTreeWalker();
		/* Fast SLL parse first, the full LL parse only if needed */
		RuleContext tree = TwoStageParse.prog(parser, tokens, li);
		RBaseListen list = new RBaseListen(tokens, new Document(code), parser);
		list.startStop.clear();
		walker.walk(list, tree);
		RRefPhaseListen ref = offset >= 0 ? new RRefPhaseListen(tokens, list, parser, offset) : new RRefPhaseListen(tokens, list, parser);
		walker.walk(ref, tree);
		/* Unclosed brackets are marked where they are (not at the next token) */
		BracketCheck.apply(tokens.getTokens(), li.getErrWarn());
		CodeChecks.apply(tokens.getTokens(), li.getErrWarn(), ref.getCurrentPackageImports() == null ? null : ref.getCurrentPackageImports().keySet());
		return new Result(ref, li.getErrWarn());
	}
}
