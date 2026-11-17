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
package com.eco.bio7.rbridge.debug;

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

import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.util.BreakpointDialog;
import com.eco.bio7.util.Util;

/**
 * The ruler action "Breakpoint Properties..." of the R editor (as in the Python
 * editor): the condition, the hit count and the log point of the breakpoint of
 * the line (a new breakpoint if there is none).
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
			ConsolePageParticipant inst = ConsolePageParticipant.getConsolePageParticipantInstance();
			if (inst != null) {
				inst.setRDebugToolbarActions();
			}
			DebugVariablesView.showDebugView();
			try {
				IMarker old = null;
				for (IMarker m : resource.findMarkers(RBreakpoint.MARKER, true, IResource.DEPTH_ZERO)) {
					if (m.getAttribute(IMarker.LINE_NUMBER, -1) == line) {
						old = m;
					}
				}
				RBreakpoint bp = old == null ? new RBreakpoint("", 0, "", null) : RBreakpoint.of(old);
				String code = "";
				try {
					IRegion r = doc.getLineInformation(line - 1);
					code = doc.get(r.getOffset(), r.getLength()).strip();
				} catch (BadLocationException e) {
					/* No code */
				}
				/* An older expression with browser(): shown as the condition text */
				String condition = bp.legacy() != null ? bp.legacy() : bp.condition();
				BreakpointDialog d = new BreakpointDialog(Util.getShell(), "R", line, code, condition, bp.hitCount(), bp.log());
				if (d.open() != Window.OK) {
					return;
				}
				if (old != null) {
					old.delete();
				}
				IMarker m = resource.createMarker(RBreakpoint.MARKER);
				m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				m.setAttribute(IMarker.LINE_NUMBER, line);
				m.setAttribute(IMarker.LOCATION, "" + line);
				if (d.condition().contains("browser()")) {
					/* An expression of an older breakpoint (with browser()): stays the text of the breakpoint */
					m.setAttribute(IMarker.TEXT, "EXPRESSION");
					m.setAttribute(IMarker.MESSAGE, d.condition());
					return;
				}
				RBreakpoint nbp = new RBreakpoint(d.condition(), d.hitCount(), d.log(), null);
				if (nbp.isPlain()) {
					m.setAttribute(IMarker.TEXT, "-");
				} else {
					/* Mark it as a breakpoint with expression (the text of older code: the trace mode) */
					m.setAttribute(IMarker.TEXT, "EXPRESSION");
					m.setAttribute(IMarker.MESSAGE, nbp.legacyText());
					m.setAttribute(RBreakpoint.CONDITION, nbp.condition());
					m.setAttribute(RBreakpoint.HIT_COUNT, nbp.hitCount());
					m.setAttribute(RBreakpoint.LOG, nbp.log());
				}
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
