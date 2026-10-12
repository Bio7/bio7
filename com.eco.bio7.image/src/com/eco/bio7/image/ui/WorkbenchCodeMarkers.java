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
package com.eco.bio7.image.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import ij.swt.CodeMarker;

/**
 * The markers with quick fixes which tools place in a workbench text editor
 * with the {@link ij.swt.CodeEditor} API. The markers are annotations of the
 * types "com.eco.bio7.image.codeMarker.*" (see plugin.xml) in the annotation
 * model of the editor. They are not resource markers, so the markers, quick
 * fixes and the Problems view of the editors are not changed. A quick fix is
 * applied with a double-click on the marked text. All methods must be called
 * in the UI thread!
 *
 * @author M. Austenfeld
 */
final class WorkbenchCodeMarkers {

	private static final String[] TYPES = { "com.eco.bio7.image.codeMarker.error", "com.eco.bio7.image.codeMarker.warning", "com.eco.bio7.image.codeMarker.info" };

	/* Removed when the text widget of the editor is disposed! */
	private static final Map<ITextEditor, WorkbenchCodeMarkers> registry = new HashMap<ITextEditor, WorkbenchCodeMarkers>();

	private final ITextEditor editor;
	private final Map<CodeMarker, MarkerAnnotation> markers = new LinkedHashMap<CodeMarker, MarkerAnnotation>();
	private IAnnotationModel model;

	/** An annotation which remembers the marked text, so that a quick fix does not overwrite changed text! */
	private static final class MarkerAnnotation extends Annotation {

		private final CodeMarker marker;
		private final String original;

		MarkerAnnotation(CodeMarker marker, String original) {
			super(TYPES[marker.getSeverity()], false, text(marker));
			this.marker = marker;
			this.original = original;
		}

		private static String text(CodeMarker marker) {
			if (!marker.hasQuickFix()) {
				return marker.getMessage();
			}
			return marker.getMessage() + "\nQuick fix (double-click the marked text): " + marker.getFixDescription();
		}
	}

	private WorkbenchCodeMarkers(ITextEditor editor) {
		this.editor = editor;
	}

	/** Returns the markers of the editor, null if the editor has no markers and create is false. */
	static WorkbenchCodeMarkers of(ITextEditor editor, boolean create) {
		WorkbenchCodeMarkers m = registry.get(editor);
		if (m == null && create) {
			StyledText text = textWidget(editor);
			if (text == null || annotationModel(editor) == null) {
				return null;
			}
			m = new WorkbenchCodeMarkers(editor);
			WorkbenchCodeMarkers markers = m;
			text.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					markers.applyAt(text, e.x, e.y);
				}
			});
			text.addDisposeListener(e -> registry.remove(editor));
			registry.put(editor, m);
		}
		return m;
	}

	/** True if tools have placed markers in the editor which are still present. */
	static boolean hasMarkers(ITextEditor editor) {
		WorkbenchCodeMarkers m = registry.get(editor);
		return m != null && !m.list().isEmpty();
	}

	/** True if markers can be placed in the editor. */
	static boolean supported(ITextEditor editor) {
		return textWidget(editor) != null && annotationModel(editor) != null;
	}

	private static StyledText textWidget(ITextEditor editor) {
		Control control = editor.getAdapter(Control.class);
		return control instanceof StyledText ? (StyledText) control : null;
	}

	private static ITextViewer viewer(ITextEditor editor) {
		ITextOperationTarget target = editor.getAdapter(ITextOperationTarget.class);
		return target instanceof ITextViewer ? (ITextViewer) target : null;
	}

	private static IDocument document(ITextEditor editor) {
		IDocumentProvider provider = editor.getDocumentProvider();
		return provider == null ? null : provider.getDocument(editor.getEditorInput());
	}

	/* The annotation model of the document, else the model of the viewer! */
	private static IAnnotationModel annotationModel(ITextEditor editor) {
		IDocumentProvider provider = editor.getDocumentProvider();
		IAnnotationModel model = provider == null ? null : provider.getAnnotationModel(editor.getEditorInput());
		if (model == null && viewer(editor) instanceof ISourceViewer) {
			model = ((ISourceViewer) viewer(editor)).getAnnotationModel();
		}
		return model;
	}

	/* Forgets the markers if the editor has a new annotation model (e.g., another input)! */
	private IAnnotationModel model() {
		IAnnotationModel current = annotationModel(editor);
		if (current != model) {
			if (model != null) {
				for (MarkerAnnotation a : markers.values()) {
					model.removeAnnotation(a);
				}
			}
			markers.clear();
			model = current;
		}
		return model;
	}

	/* The current position or null if the marker is not present anymore (an empty marker is removed)! */
	private Position position(MarkerAnnotation annotation) {
		IAnnotationModel m = model();
		Position p = m == null ? null : m.getPosition(annotation);
		if (p != null && !p.isDeleted() && p.getLength() == 0) {
			/* The marked text has been deleted! */
			m.removeAnnotation(annotation);
			return null;
		}
		return p == null || p.isDeleted() ? null : p;
	}

	boolean add(CodeMarker marker) {
		IDocument doc = document(editor);
		IAnnotationModel m = model();
		if (doc == null || m == null || marker == null || marker.getEnd() > doc.getLength() || markers.containsKey(marker)) {
			return false;
		}
		try {
			MarkerAnnotation annotation = new MarkerAnnotation(marker, doc.get(marker.getStart(), marker.getEnd() - marker.getStart()));
			m.addAnnotation(annotation, new Position(marker.getStart(), marker.getEnd() - marker.getStart()));
			markers.put(marker, annotation);
			return true;
		} catch (BadLocationException e) {
			return false;
		}
	}

	boolean remove(CodeMarker marker) {
		MarkerAnnotation annotation = markers.remove(marker);
		IAnnotationModel m = model();
		if (annotation == null || m == null) {
			return false;
		}
		boolean present = position(annotation) != null;
		m.removeAnnotation(annotation);
		return present;
	}

	boolean has(CodeMarker marker) {
		MarkerAnnotation annotation = markers.get(marker);
		return annotation != null && position(annotation) != null;
	}

	List<CodeMarker> list() {
		List<CodeMarker> list = new ArrayList<CodeMarker>();
		for (Iterator<MarkerAnnotation> it = markers.values().iterator(); it.hasNext();) {
			MarkerAnnotation annotation = it.next();
			if (position(annotation) == null) {
				it.remove();
			} else {
				list.add(annotation.marker);
			}
		}
		return list;
	}

	boolean apply(CodeMarker marker) {
		MarkerAnnotation annotation = markers.get(marker);
		return annotation != null && apply(annotation);
	}

	private boolean apply(MarkerAnnotation annotation) {
		Position p = position(annotation);
		IDocument doc = document(editor);
		if (p == null || doc == null || !annotation.marker.hasQuickFix()) {
			return false;
		}
		int offset = p.getOffset();
		int length = p.getLength();
		try {
			if (!doc.get(offset, length).equals(annotation.original)) {
				return false;
			}
			markers.remove(annotation.marker);
			model.removeAnnotation(annotation);
			String replacement = annotation.marker.getReplacement();
			/* A document change can be undone in the editor! */
			doc.replace(offset, length, replacement);
			editor.selectAndReveal(offset + replacement.length(), 0);
			return true;
		} catch (BadLocationException e) {
			return false;
		}
	}

	void clear() {
		IAnnotationModel m = model();
		if (m != null) {
			for (MarkerAnnotation a : markers.values()) {
				m.removeAnnotation(a);
			}
		}
		markers.clear();
	}

	/* Applies the quick fix of the smallest marker at the clicked text! */
	private void applyAt(StyledText text, int x, int y) {
		if (markers.isEmpty()) {
			return;
		}
		int widgetOffset = text.getOffsetAtPoint(new Point(x, y));
		if (widgetOffset < 0) {
			return;
		}
		int offset = widgetOffset;
		ITextViewer viewer = viewer(editor);
		if (viewer instanceof ITextViewerExtension5) {
			offset = ((ITextViewerExtension5) viewer).widgetOffset2ModelOffset(widgetOffset);
		}
		MarkerAnnotation found = null;
		int foundLength = Integer.MAX_VALUE;
		for (MarkerAnnotation annotation : new ArrayList<MarkerAnnotation>(markers.values())) {
			Position p = position(annotation);
			if (p != null && annotation.marker.hasQuickFix() && p.includes(offset) && p.getLength() < foundLength) {
				found = annotation;
				foundLength = p.getLength();
			}
		}
		if (found != null) {
			apply(found);
		}
	}
}
