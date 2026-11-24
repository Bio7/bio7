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
package com.eco.bio7.ijmacro.editor.toolbar.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The ruler action "Toggle Breakpoint" of the ImageJ macro editor (also a
 * double-click in the ruler, as in the R and Python editors): sets a breakpoint
 * in the line or removes it.
 */
public class ToggleDebugBreakpointAction extends AbstractRulerActionDelegate implements IEditorActionDelegate {

	/** The marker of the breakpoints of the ImageJ macro editor. */
	public static final String MARKER = "com.eco.bio7.ijmacro.editor.debugrulermark";

	private static class ToggleAction extends Action {

		private final IVerticalRulerInfo rulerInfo;
		private final ITextEditor editor;

		ToggleAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
			super("Toggle Breakpoint");
			this.editor = editor;
			this.rulerInfo = rulerInfo;
		}

		@Override
		public void run() {
			IResource resource = editor.getEditorInput().getAdapter(IResource.class);
			IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			int line = rulerInfo.getLineOfLastMouseButtonActivity() + 1;
			if (resource == null || doc == null || line <= 0 || line > doc.getNumberOfLines()) {
				return;
			}
			try {
				/* A breakpoint in this line: removed */
				IMarker old = markerAt(resource, line);
				if (old != null) {
					old.delete();
					return;
				}
				create(resource, line, null);
			} catch (CoreException e) {
				/* Not changed */
			}
		}
	}

	/** The breakpoint of the line (from 1; null if there is none). */
	static IMarker markerAt(IResource resource, int line) throws CoreException {
		for (IMarker m : resource.findMarkers(MARKER, true, IResource.DEPTH_ZERO)) {
			if (m.getAttribute(IMarker.LINE_NUMBER, -1) == line) {
				return m;
			}
		}
		return null;
	}

	/** Creates a breakpoint in the line (from 1); expression: the condition "variable operator value" (null: none). */
	static IMarker create(IResource resource, int line, String expression) throws CoreException {
		IMarker marker = resource.createMarker(MARKER);
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
		marker.setAttribute(IMarker.LINE_NUMBER, line);
		marker.setAttribute(IMarker.LOCATION, "" + line);
		marker.setAttribute(IMarker.MESSAGE, expression);
		/* A breakpoint with a condition is marked as such (the debugger runs to the expression) */
		marker.setAttribute(IMarker.TEXT, expression == null ? "-" : "EXPRESSION");
		return marker;
	}

	@Override
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		return new ToggleAction(editor, rulerInfo);
	}
}
