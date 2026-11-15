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
package com.eco.bio7.markdownedit.chunks;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * The R code of the R chunks of a markdown document as one R document: the
 * other text (markdown, fences, other chunks) is replaced by spaces, the line
 * breaks stay. The offsets and lines are the ones of the markdown document (the
 * errors and the scope of the R parser fit 1:1). All chunks together: a variable
 * of a chunk is known in the chunks below it (as knitr runs them).
 */
public final class RChunkCode {

	private RChunkCode() {
	}

	/** Returns true if the document has an R chunk. */
	public static boolean hasRChunks(IDocument doc) {
		return !InlineChunkOutputs.allChunks(doc).isEmpty();
	}

	/** The [start, end) offsets of the code of each R chunk (the lines between the fences). */
	public static List<int[]> ranges(IDocument doc) {
		List<int[]> ranges = new java.util.ArrayList<>();
		for (InlineChunkOutputs.Chunk c : InlineChunkOutputs.allChunks(doc)) {
			try {
				if (c.endLine() > c.startLine() + 1) {
					int start = doc.getLineOffset(c.startLine() + 1);
					IRegion last = doc.getLineInformation(c.endLine() - 1);
					ranges.add(new int[] { start, last.getOffset() + last.getLength() });
				}
			} catch (BadLocationException e) {
				/* Changed meanwhile */
			}
		}
		return ranges;
	}

	/** The R code of the document (same length, the other text replaced by spaces). */
	public static String virtualCode(IDocument doc) {
		String text = doc.get();
		char[] out = new char[text.length()];
		for (int i = 0; i < out.length; i++) {
			char c = text.charAt(i);
			out[i] = c == '\n' || c == '\r' ? c : ' ';
		}
		List<InlineChunkOutputs.Chunk> chunks = InlineChunkOutputs.allChunks(doc);
		for (InlineChunkOutputs.Chunk c : chunks) {
			try {
				/* The lines between the fences */
				for (int line = c.startLine() + 1; line < c.endLine(); line++) {
					IRegion r = doc.getLineInformation(line);
					text.getChars(r.getOffset(), r.getOffset() + r.getLength(), out, r.getOffset());
				}
			} catch (BadLocationException | IndexOutOfBoundsException e) {
				/* Changed meanwhile */
			}
		}
		return new String(out);
	}
}
