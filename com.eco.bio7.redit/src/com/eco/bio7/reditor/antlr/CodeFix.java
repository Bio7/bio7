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

/**
 * The quick fixes of a message of the code analysis as text (the replacement
 * part of the message, stored in the marker): proposals of edits (an offset, a
 * length and a text). Format: proposals separated by '¶', a proposal is
 * "label¦edit§edit", an edit "offset,length,text" (line breaks as \n).
 */
public final class CodeFix {

	/** An edit of the document. */
	public record Edit(int offset, int length, String text) {
	}

	/** A quick fix: a label and its edits. */
	public record Proposal(String label, List<Edit> edits) {
	}

	private final List<Proposal> proposals = new ArrayList<>();

	/** Adds a quick fix with one edit. */
	public CodeFix add(String label, int offset, int length, String text) {
		proposals.add(new Proposal(label, List.of(new Edit(offset, length, text))));
		return this;
	}

	/** Adds a quick fix with several edits. */
	public CodeFix add(String label, List<Edit> edits) {
		proposals.add(new Proposal(label, List.copyOf(edits)));
		return this;
	}

	public boolean isEmpty() {
		return proposals.isEmpty();
	}

	/** The text of the fixes (the replacement of the message). */
	public String encode() {
		StringBuilder sb = new StringBuilder();
		for (Proposal p : proposals) {
			if (sb.length() > 0) {
				sb.append('¶');
			}
			sb.append(clean(p.label())).append('¦');
			for (int i = 0; i < p.edits().size(); i++) {
				Edit e = p.edits().get(i);
				sb.append(i > 0 ? "§" : "").append(e.offset()).append(',').append(e.length()).append(',')
						.append(e.text().replace("\r", "\\r").replace("\n", "\\n").replace("#", "\\x23"));
			}
		}
		return sb.toString();
	}

	/** The fixes of a text (see encode()); empty if it is not one. */
	public static List<Proposal> decode(String text) {
		List<Proposal> list = new ArrayList<>();
		if (text == null || text.indexOf('¦') < 0) {
			return list;
		}
		for (String p : text.split("¶")) {
			int bar = p.indexOf('¦');
			if (bar < 0) {
				continue;
			}
			List<Edit> edits = new ArrayList<>();
			for (String e : p.substring(bar + 1).split("§")) {
				int c1 = e.indexOf(',');
				int c2 = c1 < 0 ? -1 : e.indexOf(',', c1 + 1);
				if (c2 < 0) {
					continue;
				}
				try {
					String t = e.substring(c2 + 1).replace("\\n", "\n").replace("\\r", "\r").replace("\\x23", "#");
					edits.add(new Edit(Integer.parseInt(e.substring(0, c1).trim()), Integer.parseInt(e.substring(c1 + 1, c2).trim()), t));
				} catch (NumberFormatException ex) {
					/* Not an edit */
				}
			}
			if (!edits.isEmpty()) {
				list.add(new Proposal(p.substring(0, bar), edits));
			}
		}
		return list;
	}

	/* A label without the separators */
	private static String clean(String label) {
		return label.replace('¶', ' ').replace('¦', ' ').replace('§', ' ').replace("####", " ");
	}
}
