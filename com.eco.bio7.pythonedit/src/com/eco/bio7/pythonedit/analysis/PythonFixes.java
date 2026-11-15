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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/**
 * The quick fixes of the code analysis: edits (line from 1, column from 0) or
 * the installation of a package. Stored as JSON in the attribute "bio7.fixes"
 * of a marker.
 */
public final class PythonFixes {

	/** The type of the markers of the code analysis. */
	public static final String MARKER = "com.eco.bio7.pythonedit.problem";
	public static final String FIXES = "bio7.fixes";

	/** A marker at a position: its message, severity and quick fixes. */
	public record MarkerFixes(String message, int severity, List<ICompletionProposal> proposals) {
	}

	private PythonFixes() {
	}

	/** The offset of a line (from 1) and a column (from 0), clamped to the document. */
	static int offset(IDocument doc, int line, int col) throws BadLocationException {
		if (line > doc.getNumberOfLines()) {
			return doc.getLength();
		}
		int start = doc.getLineOffset(Math.max(0, line - 1));
		int length = doc.getLineLength(Math.max(0, line - 1));
		return start + Math.max(0, Math.min(col, length));
	}

	/** The proposals of the JSON of a marker. */
	@SuppressWarnings("unchecked")
	public static List<ICompletionProposal> proposals(String fixesJson) {
		List<ICompletionProposal> list = new ArrayList<>();
		if (fixesJson == null || fixesJson.isEmpty()) {
			return list;
		}
		Object parsed = Json.parse(fixesJson);
		if (!(parsed instanceof List<?> fixes)) {
			return list;
		}
		for (Object f : fixes) {
			if (f instanceof Map<?, ?> m) {
				list.add(new FixProposal((Map<String, Object>) m));
			}
		}
		return list;
	}

	/** The marker at an offset (else on the line of the offset): message and fixes; null if none. */
	public static MarkerFixes markerAt(ISourceViewer viewer, int offset, boolean sameLine) {
		IAnnotationModel model = viewer.getAnnotationModel();
		IDocument doc = viewer.getDocument();
		if (model == null || doc == null) {
			return null;
		}
		MarkerFixes onLine = null;
		Iterator<Annotation> it = model.getAnnotationIterator();
		while (it.hasNext()) {
			if (!(it.next() instanceof MarkerAnnotation ma) || ma.isMarkedDeleted()) {
				continue;
			}
			IMarker marker = ma.getMarker();
			try {
				if (!marker.exists() || !MARKER.equals(marker.getType())) {
					continue;
				}
			} catch (Exception e) {
				continue;
			}
			Position pos = model.getPosition(ma);
			if (pos == null) {
				continue;
			}
			MarkerFixes mf = new MarkerFixes(marker.getAttribute(IMarker.MESSAGE, ""), marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING),
					proposals(marker.getAttribute(FIXES, "")));
			if (offset >= pos.getOffset() && offset <= pos.getOffset() + pos.getLength()) {
				return mf;
			}
			if (sameLine && onLine == null) {
				try {
					if (doc.getLineOfOffset(pos.getOffset()) == doc.getLineOfOffset(offset)) {
						onLine = mf;
					}
				} catch (BadLocationException e) {
					/* Next */
				}
			}
		}
		return onLine;
	}

	/** The position of a marker with quick fixes on a line (from 0) or null. */
	public static Position fixablePositionOnLine(ISourceViewer viewer, int line) {
		IAnnotationModel model = viewer == null ? null : viewer.getAnnotationModel();
		IDocument doc = viewer == null ? null : viewer.getDocument();
		if (model == null || doc == null || line < 0) {
			return null;
		}
		Iterator<Annotation> it = model.getAnnotationIterator();
		while (it.hasNext()) {
			if (!(it.next() instanceof MarkerAnnotation ma) || ma.isMarkedDeleted()) {
				continue;
			}
			try {
				IMarker m = ma.getMarker();
				if (!m.exists() || !MARKER.equals(m.getType()) || m.getAttribute(FIXES, "").isEmpty()) {
					continue;
				}
				Position pos = model.getPosition(ma);
				if (pos != null && doc.getLineOfOffset(pos.getOffset()) == line) {
					return pos;
				}
			} catch (Exception e) {
				/* Next */
			}
		}
		return null;
	}

	/** A quick fix: edits of the document or the installation of a package. */
	static final class FixProposal implements ICompletionProposal {
		private final Map<String, Object> fix;

		FixProposal(Map<String, Object> fix) {
			this.fix = fix;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void apply(IDocument document) {
			if (fix.get("install") instanceof String pkg) {
				Consumer<String> c = PythonEditorSupport.installPackage;
				if (c != null) {
					c.accept(pkg);
				}
				return;
			}
			if (!(fix.get("edits") instanceof List<?> edits)) {
				return;
			}
			try {
				/* The offsets first (all from the same text), applied from the end */
				List<int[]> ranges = new ArrayList<>();
				List<String> texts = new ArrayList<>();
				for (Object o : edits) {
					Map<String, Object> e = (Map<String, Object>) o;
					int start = offset(document, num(e, "line"), num(e, "col"));
					int end = offset(document, num(e, "endLine"), num(e, "endCol"));
					ranges.add(new int[] { start, Math.max(start, end), texts.size() });
					texts.add(String.valueOf(e.getOrDefault("text", "")));
				}
				ranges.sort(Comparator.comparingInt((int[] r) -> r[0]).reversed());
				for (int[] r : ranges) {
					document.replace(r[0], r[1] - r[0], texts.get(r[2]));
				}
			} catch (BadLocationException e) {
				/* The document changed in the meantime */
			}
		}

		private static int num(Map<String, Object> e, String key) {
			return e.get(key) instanceof Number n ? n.intValue() : 0;
		}

		@Override
		public Point getSelection(IDocument document) {
			return null;
		}

		@Override
		public String getAdditionalProposalInfo() {
			return null;
		}

		@Override
		public String getDisplayString() {
			return String.valueOf(fix.getOrDefault("title", "Fix"));
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public IContextInformation getContextInformation() {
			return null;
		}
	}
}
