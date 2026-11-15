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
package com.eco.bio7.pythonedit.analysis;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The hover of the Python editor: a marker with its quick fixes, else the help
 * of the name (signature, documentation) and the value of a variable of the
 * running Python.
 */
public class PythonTextHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

	private final ITextEditor editor;
	private volatile boolean marker;

	public PythonTextHover(ITextEditor editor) {
		this.editor = editor;
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		Point selection = textViewer.getSelectedRange();
		if (selection.x <= offset && offset < selection.x + selection.y) {
			return new Region(selection.x, selection.y);
		}
		return new Region(offset, 0);
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		Object info = getHoverInfo2(textViewer, hoverRegion);
		return info instanceof FixesInformationControl.Input in ? in.fixes().message() : (String) info;
	}

	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion region) {
		marker = false;
		int offset = region.getOffset();
		IDocument doc = textViewer.getDocument();
		if (textViewer instanceof ISourceViewer sv) {
			PythonFixes.MarkerFixes mf = PythonFixes.markerAt(sv, offset, false);
			if (mf != null) {
				marker = true;
				return new FixesInformationControl.Input(mf, doc);
			}
		}
		try {
			StringBuilder text = new StringBuilder();
			String word = dottedWord(doc, offset);
			if (word.isEmpty()) {
				return null;
			}
			/* The value of a variable of the running Python */
			Function<String, String> live = PythonEditorSupport.liveHover;
			if (live != null) {
				String value = live.apply(word);
				if (value != null && !value.isBlank()) {
					text.append(value.strip()).append("\n\n");
				}
			}
			int line = doc.getLineOfOffset(offset);
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("source", doc.get());
			args.put("path", path());
			args.put("line", line + 1);
			args.put("col", offset - doc.getLineOffset(line));
			Map<String, Object> help = AnalysisClient.get().request("help", args, 4000);
			if (help != null && help.get("text") instanceof String h && !h.isBlank()) {
				text.append(h.strip());
			}
			return text.isEmpty() ? null : text.toString().strip();
		} catch (BadLocationException e) {
			return null;
		}
	}

	private String path() {
		return editor.getEditorInput() instanceof IFileEditorInput fi && fi.getFile().getLocation() != null ? fi.getFile().getLocation().toOSString() : null;
	}

	/* The dotted name at the offset (e.g., np.mean) */
	static String dottedWord(IDocument doc, int offset) throws BadLocationException {
		int start = offset, end = offset, length = doc.getLength();
		while (start > 0 && (Character.isLetterOrDigit(doc.getChar(start - 1)) || doc.getChar(start - 1) == '_' || doc.getChar(start - 1) == '.')) {
			start--;
		}
		while (end < length && (Character.isLetterOrDigit(doc.getChar(end)) || doc.getChar(end) == '_')) {
			end++;
		}
		String w = doc.get(start, end - start);
		while (w.startsWith(".")) {
			w = w.substring(1);
		}
		return w;
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		/* One control for both inputs (a marker with its quick fixes or a text): independent of the order of the calls */
		return parent -> new FixesInformationControl(parent, false);
	}
}
