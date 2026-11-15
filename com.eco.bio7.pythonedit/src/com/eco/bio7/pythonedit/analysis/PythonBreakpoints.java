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
import java.util.List;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The breakpoints of Python scripts (markers of the file, kept with the file)
 * and the line of a stopped debugger.
 */
public final class PythonBreakpoints {

	/** A breakpoint (persistent marker). */
	public static final String BREAKPOINT = "com.eco.bio7.pythonedit.breakpoint";
	/** The line where the debugger stopped. */
	public static final String DEBUG_LINE = "com.eco.bio7.pythonedit.debugline";

	private PythonBreakpoints() {
	}

	/** The lines (from 1) of the breakpoints of a file. */
	public static List<Integer> lines(IFile file) {
		TreeSet<Integer> lines = new TreeSet<>();
		try {
			for (IMarker m : file.findMarkers(BREAKPOINT, false, IResource.DEPTH_ZERO)) {
				int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
				if (line > 0) {
					lines.add(line);
				}
			}
		} catch (CoreException e) {
			/* No breakpoints */
		}
		return new ArrayList<>(lines);
	}

	/** Sets or removes the breakpoint of a line (from 1). */
	public static void toggle(IFile file, IDocument doc, int line) {
		try {
			for (IMarker m : file.findMarkers(BREAKPOINT, false, IResource.DEPTH_ZERO)) {
				if (m.getAttribute(IMarker.LINE_NUMBER, -1) == line) {
					m.delete();
					return;
				}
			}
			IRegion r = doc.getLineInformation(line - 1);
			IMarker m = file.createMarker(BREAKPOINT);
			m.setAttribute(IMarker.LINE_NUMBER, line);
			m.setAttribute(IMarker.CHAR_START, r.getOffset());
			m.setAttribute(IMarker.CHAR_END, r.getOffset() + Math.max(1, r.getLength()));
			m.setAttribute(IMarker.MESSAGE, "Breakpoint (line " + line + ")");
		} catch (CoreException | BadLocationException e) {
			/* Not changed */
		}
	}

	/** Removes all breakpoints of a file. */
	public static void removeAll(IFile file) {
		try {
			file.deleteMarkers(BREAKPOINT, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			/* Not removed */
		}
	}

	/** Marks the line of the stopped debugger (from 1; 0 removes the mark). */
	public static void showDebugLine(IFile file, IDocument doc, int line) {
		try {
			file.deleteMarkers(DEBUG_LINE, false, IResource.DEPTH_ZERO);
			if (line > 0 && doc != null && line <= doc.getNumberOfLines()) {
				IRegion r = doc.getLineInformation(line - 1);
				IMarker m = file.createMarker(DEBUG_LINE);
				m.setAttribute(IMarker.LINE_NUMBER, line);
				m.setAttribute(IMarker.CHAR_START, r.getOffset());
				m.setAttribute(IMarker.CHAR_END, r.getOffset() + Math.max(1, r.getLength()));
			}
		} catch (CoreException | BadLocationException e) {
			/* No mark */
		}
	}

	/** The action of a double click on the ruler: sets or removes the breakpoint of the line. */
	public static Action toggleAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		return new Action("Toggle Breakpoint") {
			@Override
			public void run() {
				if (!(editor.getEditorInput() instanceof IFileEditorInput input)) {
					return;
				}
				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				int line = ruler.getLineOfLastMouseButtonActivity();
				if (doc != null && line >= 0) {
					toggle(input.getFile(), doc, line + 1);
				}
			}
		};
	}
}
