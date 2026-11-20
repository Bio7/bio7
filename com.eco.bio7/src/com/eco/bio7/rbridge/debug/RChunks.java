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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The R code of an R Markdown or Quarto document for the debugger: the lines of
 * the R chunks stay, all other lines (text, YAML header, chunk fences, chunks of
 * other languages, chunks with eval = FALSE) become empty lines. The lines keep
 * their numbers, so the breakpoints, stops and steps are the lines of the
 * document. The R code is debugged as a script (not knitted: inline R code,
 * child documents and the cache are not used).
 */
final class RChunks {

	/* ```{r label, options} (also indented, e.g., in a list) */
	private static final Pattern START = Pattern.compile("^\\s*(`{3,})\\s*\\{\\s*([A-Za-z0-9_.]*)(.*)\\}\\s*$");
	private static final Pattern PLAIN_START = Pattern.compile("^\\s*(`{3,}|~{3,}).*$");
	private static final Pattern EVAL_FALSE = Pattern.compile("(^|[\\s,])eval\\s*=\\s*(FALSE|F)\\b");
	private static final Pattern QUARTO_EVAL_FALSE = Pattern.compile("^\\s*#\\|\\s*eval\\s*:\\s*false\\s*$", Pattern.CASE_INSENSITIVE);

	private RChunks() {
	}

	/** The file is an R Markdown or Quarto document. */
	static boolean isDocument(Path file) {
		String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
		return n.endsWith(".rmd") || n.endsWith(".qmd") || n.endsWith(".rmarkdown");
	}

	/** The document has params in its YAML header (they are set for the debugged code). */
	static boolean hasParams(List<String> lines) {
		int end = yamlEnd(lines);
		for (int i = 1; i < end; i++) {
			if (lines.get(i).startsWith("params:")) {
				return true;
			}
		}
		return false;
	}

	/** The lines of the document: the R chunks, other lines empty. */
	static List<String> code(List<String> lines) {
		List<String> out = new ArrayList<>(lines.size());
		int i = 0;
		/* The YAML header */
		int yaml = yamlEnd(lines);
		for (; i < yaml; i++) {
			out.add("");
		}
		while (i < lines.size()) {
			String line = lines.get(i);
			Matcher m = START.matcher(line);
			String fence = null;
			boolean r = false;
			if (m.matches()) {
				fence = m.group(1);
				r = m.group(2).equalsIgnoreCase("r") && !EVAL_FALSE.matcher(m.group(3)).find();
			} else {
				Matcher p = PLAIN_START.matcher(line);
				if (p.matches()) {
					/* A code block of the text (not run) */
					fence = p.group(1);
				}
			}
			if (fence == null) {
				out.add("");
				i++;
				continue;
			}
			/* The chunk: to the closing fence */
			int end = i + 1;
			while (end < lines.size() && !isEnd(lines.get(end), fence)) {
				end++;
			}
			if (r) {
				/* Quarto: the options at the start of the chunk (#| eval: false) */
				for (int k = i + 1; k < end && lines.get(k).trim().startsWith("#|"); k++) {
					if (QUARTO_EVAL_FALSE.matcher(lines.get(k)).matches()) {
						r = false;
					}
				}
			}
			out.add("");
			for (int k = i + 1; k < end; k++) {
				out.add(r ? lines.get(k) : "");
			}
			if (end < lines.size()) {
				out.add("");
			}
			i = end + 1;
		}
		return out;
	}

	private static boolean isEnd(String line, String fence) {
		String t = line.trim();
		char c = fence.charAt(0);
		int n = 0;
		while (n < t.length() && t.charAt(n) == c) {
			n++;
		}
		return n >= fence.length() && n == t.length();
	}

	/* The end (exclusive) of the YAML header at the start, 0 if none */
	private static int yamlEnd(List<String> lines) {
		if (lines.isEmpty() || !lines.get(0).trim().equals("---")) {
			return 0;
		}
		for (int i = 1; i < lines.size(); i++) {
			String t = lines.get(i).trim();
			if (t.equals("---") || t.equals("...")) {
				return i + 1;
			}
		}
		return 0;
	}
}
