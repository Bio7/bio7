package com.eco.bio7.markdownedit.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;

/**
 * The parts of a markdown document which are not prose: the YAML header, code
 * chunks and fenced code (the R code has its own analysis), inline code, math,
 * URLs, link targets and HTML comments. E.g., they are not checked for spelling.
 */
public final class MarkdownCodeRanges {

	private MarkdownCodeRanges() {
	}

	/* Inline code, math, URLs, link targets and HTML comments (not checked for spelling) */
	private static final java.util.regex.Pattern INLINE_CODE = java.util.regex.Pattern.compile(
			"(`+)[^`]*?\\1|\\$\\$[\\s\\S]*?\\$\\$|(?<![\\w$])\\$(?=\\S)[^$\\n]*?\\S\\$|<!--[\\s\\S]*?-->|\\]\\([^)\\n]*\\)|<?https?://[^\\s)>\\]]+>?|\\{[^}\\n]*\\}");
	private static final java.util.regex.Pattern FENCE = java.util.regex.Pattern.compile("(?m)^\\s{0,3}(```|~~~)");

	/*
	 * The ranges of a markdown document which are not prose: the YAML header, the
	 * code chunks and fenced code blocks (the R code has its own analysis), inline
	 * code, math, URLs and link targets
	 */
	public static List<int[]> of(IDocument doc) {
		List<int[]> ranges = new ArrayList<>();
		if (doc == null) {
			return ranges;
		}
		String text = doc.get();
		/* The YAML header */
		java.util.regex.Matcher y = java.util.regex.Pattern.compile("\\A---\\s*\\R[\\s\\S]*?\\R(?:---|\\.\\.\\.)\\s*(?:\\R|\\z)").matcher(text);
		if (y.find()) {
			ranges.add(new int[] { y.start(), y.end() });
		}
		/* Fenced code (also the R chunks): from a fence to the next fence */
		java.util.regex.Matcher f = FENCE.matcher(text);
		int open = -1;
		while (f.find()) {
			if (open < 0) {
				open = f.start();
			} else {
				int end = text.indexOf('\n', f.end());
				ranges.add(new int[] { open, end < 0 ? text.length() : end });
				open = -1;
			}
		}
		if (open >= 0) {
			ranges.add(new int[] { open, text.length() });
		}
		java.util.regex.Matcher m = INLINE_CODE.matcher(text);
		while (m.find()) {
			ranges.add(new int[] { m.start(), m.end() });
		}
		return ranges;
	}

}
