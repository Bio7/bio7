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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.util.Util;

/**
 * The ruler action "Breakpoint Properties..." of the ImageJ macro editor (as in
 * the R and Python editors): the condition of the breakpoint of the line (a new
 * breakpoint if there is none).
 */
public class ToggleDebugConditionalBreakpointAction extends AbstractRulerActionDelegate implements IEditorActionDelegate {

	private static class PropertiesAction extends Action {

		private final IVerticalRulerInfo rulerInfo;
		private final ITextEditor editor;

		PropertiesAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
			super("Breakpoint Properties...");
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
				IMarker old = ToggleDebugBreakpointAction.markerAt(resource, line);
				String expression = old == null ? null : old.getAttribute(IMarker.MESSAGE, null);
				String code = "";
				try {
					IRegion r = doc.getLineInformation(line - 1);
					code = doc.get(r.getOffset(), r.getLength()).strip();
				} catch (BadLocationException e) {
					/* No code */
				}
				String info = "Line " + line + (code.isEmpty() ? "" : ": " + (code.length() > 70 ? code.substring(0, 70) + "…" : code));
				SetDebugBreakpointMacroValueDialog d = new SetDebugBreakpointMacroValueDialog(Util.getShell(), "Breakpoint Properties", expression, info);
				if (d.open() != Window.OK) {
					return;
				}
				if (old != null) {
					old.delete();
				}
				ToggleDebugBreakpointAction.create(resource, line, d.getExpression());
			} catch (CoreException e) {
				/* Not changed */
			}
		}
	}

	@Override
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		return new PropertiesAction(editor, rulerInfo);
	}
}
