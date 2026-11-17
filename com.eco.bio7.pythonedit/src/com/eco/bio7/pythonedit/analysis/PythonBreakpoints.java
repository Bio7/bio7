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
	/** A breakpoint with a condition or a hit count (a subtype of the breakpoint). */
	public static final String CONDITIONAL = "com.eco.bio7.pythonedit.conditionalbreakpoint";
	/** The condition of a breakpoint (a Python expression in the frame). */
	public static final String CONDITION = "condition";
	/** The hit count of a breakpoint: stops at the n-th hit (with a condition: the n-th time it is true). */
	public static final String HIT_COUNT = "hitCount";
	/** A log point: the expression whose value is printed (the debugger doesn't stop). */
	public static final String LOG = "logMessage";
	/** The line where the debugger stopped. */
	public static final String DEBUG_LINE = "com.eco.bio7.pythonedit.debugline";

	private PythonBreakpoints() {
	}

	/** The lines (from 1) of the breakpoints of a file. */
	public static List<Integer> lines(IFile file) {
		TreeSet<Integer> lines = new TreeSet<>();
		try {
			for (IMarker m : file.findMarkers(BREAKPOINT, true, IResource.DEPTH_ZERO)) {
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

	/**
	 * The breakpoints for the debugger as a Python list: [line, condition or None,
	 * hit count] for each breakpoint.
	 */
	public static String specs(IFile file) {
		StringBuilder b = new StringBuilder("[");
		try {
			IMarker[] markers = file.findMarkers(BREAKPOINT, true, IResource.DEPTH_ZERO);
			java.util.Arrays.sort(markers, java.util.Comparator.comparingInt(m -> m.getAttribute(IMarker.LINE_NUMBER, -1)));
			for (IMarker m : markers) {
				int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
				if (line <= 0) {
					continue;
				}
				String cond = m.getAttribute(CONDITION, "").strip();
				String log = m.getAttribute(LOG, "").strip();
				b.append(b.length() > 1 ? ", " : "").append('[').append(line).append(", ").append(cond.isEmpty() ? "None" : pyString(cond)).append(", ")
						.append(Math.max(0, m.getAttribute(HIT_COUNT, 0))).append(", ").append(log.isEmpty() ? "None" : pyString(log)).append(']');
			}
		} catch (CoreException e) {
			/* No breakpoints */
		}
		return b.append(']').toString();
	}

	/* A Python string literal */
	private static String pyString(String s) {
		return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "") + "'";
	}

	/** The breakpoint of a line (from 1) or null. */
	public static IMarker find(IFile file, int line) {
		try {
			for (IMarker m : file.findMarkers(BREAKPOINT, true, IResource.DEPTH_ZERO)) {
				if (m.getAttribute(IMarker.LINE_NUMBER, -1) == line) {
					return m;
				}
			}
		} catch (CoreException e) {
			/* None */
		}
		return null;
	}

	/** Sets or removes the breakpoint of a line (from 1). */
	public static void toggle(IFile file, IDocument doc, int line) {
		IMarker old = find(file, line);
		if (old != null) {
			try {
				old.delete();
			} catch (CoreException e) {
				/* Not removed */
			}
			return;
		}
		set(file, doc, line, "", 0, "");
	}

	/**
	 * Sets the breakpoint of a line (from 1) with a condition and a hit count
	 * (empty, 0: a simple breakpoint).
	 */
	public static void set(IFile file, IDocument doc, int line, String condition, int hitCount, String log) {
		try {
			IMarker old = find(file, line);
			if (old != null) {
				old.delete();
			}
			String cond = condition == null ? "" : condition.strip();
			String logExpr = log == null ? "" : log.strip();
			boolean conditional = !cond.isEmpty() || hitCount > 1 || !logExpr.isEmpty();
			IRegion r = doc.getLineInformation(line - 1);
			IMarker m = file.createMarker(conditional ? CONDITIONAL : BREAKPOINT);
			m.setAttribute(IMarker.LINE_NUMBER, line);
			m.setAttribute(IMarker.CHAR_START, r.getOffset());
			m.setAttribute(IMarker.CHAR_END, r.getOffset() + Math.max(1, r.getLength()));
			if (!cond.isEmpty()) {
				m.setAttribute(CONDITION, cond);
			}
			if (hitCount > 1) {
				m.setAttribute(HIT_COUNT, hitCount);
			}
			if (!logExpr.isEmpty()) {
				m.setAttribute(LOG, logExpr);
			}
			m.setAttribute(IMarker.MESSAGE, com.eco.bio7.util.BreakpointDialog.describe(line, cond, hitCount, logExpr));
		} catch (CoreException | BadLocationException e) {
			/* Not changed */
		}
	}

	/** Removes all breakpoints of a file. */
	public static void removeAll(IFile file) {
		try {
			file.deleteMarkers(BREAKPOINT, true, IResource.DEPTH_ZERO);
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

	/**
	 * The action of the ruler: the condition and the hit count of the breakpoint of
	 * the line (a new breakpoint if there is none).
	 */
	public static Action propertiesAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		return new Action("Breakpoint Properties...") {
			@Override
			public void run() {
				if (!(editor.getEditorInput() instanceof IFileEditorInput input)) {
					return;
				}
				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				int line = ruler.getLineOfLastMouseButtonActivity() + 1;
				if (doc == null || line <= 0 || line > doc.getNumberOfLines()) {
					return;
				}
				IFile file = input.getFile();
				IMarker m = find(file, line);
				String cond = m == null ? "" : m.getAttribute(CONDITION, "");
				int hits = m == null ? 0 : m.getAttribute(HIT_COUNT, 0);
				String log = m == null ? "" : m.getAttribute(LOG, "");
				String code = "";
				try {
					IRegion r = doc.getLineInformation(line - 1);
					code = doc.get(r.getOffset(), r.getLength()).strip();
				} catch (BadLocationException e) {
					/* No code */
				}
				com.eco.bio7.util.BreakpointDialog d = new com.eco.bio7.util.BreakpointDialog(editor.getSite().getShell(), "Python", line, code, cond, hits, log);
				if (d.open() == org.eclipse.jface.window.Window.OK) {
					set(file, doc, line, d.condition(), d.hitCount(), d.log());
				}
			}
		};
	}

	/** The action of the ruler: removes all breakpoints of the file. */
	public static Action removeAllAction(ITextEditor editor) {
		return new Action("Remove All Breakpoints") {
			@Override
			public void run() {
				if (editor.getEditorInput() instanceof IFileEditorInput input) {
					removeAll(input.getFile());
				}
			}
		};
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
