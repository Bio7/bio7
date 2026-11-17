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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * The Python code of the Python chunks of a markdown document as one Python
 * module: the other lines are empty, the line breaks stay. The lines and the
 * columns are the ones of the markdown document (the diagnostics and the
 * completion fit 1:1). All chunks together: a variable or import of a chunk is
 * known in the chunks below it (the chunks run in the same Python).
 */
public final class PythonChunkCode {

	private PythonChunkCode() {
	}

	/** Returns true if the document has a Python chunk. */
	public static boolean hasPythonChunks(IDocument doc) {
		return !InlineChunkOutputs.pythonChunks(doc).isEmpty();
	}

	/** The Python code of the document (the same lines, the other lines empty). */
	public static String virtualCode(IDocument doc) {
		int lines = doc.getNumberOfLines();
		boolean[] code = new boolean[lines];
		for (InlineChunkOutputs.Chunk c : InlineChunkOutputs.pythonChunks(doc)) {
			for (int line = c.startLine() + 1; line < c.endLine() && line < lines; line++) {
				code[line] = true;
			}
		}
		StringBuilder out = new StringBuilder(doc.getLength());
		try {
			for (int line = 0; line < lines; line++) {
				if (code[line]) {
					IRegion r = doc.getLineInformation(line);
					out.append(doc.get(r.getOffset(), r.getLength()));
				}
				if (line < lines - 1) {
					out.append('\n');
				}
			}
		} catch (BadLocationException e) {
			/* Changed meanwhile */
		}
		return out.toString();
	}

	/**
	 * The path for the analysis: a Python file in the folder of the document (the
	 * modules of the folder can be imported), null without a file.
	 */
	public static String analysisPath(IResource resource) {
		if (resource == null || resource.getLocation() == null) {
			return null;
		}
		return resource.getLocation().removeFileExtension().toOSString() + "_chunks.py";
	}
}
