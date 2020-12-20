/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.util.Util;

public class ToggleDebugConditionalBreakpointAction extends AbstractRulerActionDelegate implements IEditorActionDelegate {

	private class ToggleBreakpointAction extends Action {

		private final IVerticalRulerInfo mRulerInfo;
		private final ITextEditor mEditor;
		private int startline;
		private int stopline;

		public ToggleBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
			super("Toggle conditional line breakpoint");
			mEditor = editor;
			mRulerInfo = rulerInfo;
		}

		@Override
		public void run() {
			ConsolePageParticipant inst = ConsolePageParticipant.getConsolePageParticipantInstance();
			inst.setRDebugToolbarActions();
			String rDebugExpression = null;
			IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			InputDialog dlg = new InputDialog(Util.getShell(), "", "Enter Expressions:  e.g. 'if(x==5) browser()'", "", null);
			if (dlg.open() == Window.OK) {
				
				rDebugExpression = dlg.getValue();
				IResource resource = (IResource) editore.getEditorInput().getAdapter(IResource.class);
				if (resource != null) {
					try {

						ITextEditor editor = (ITextEditor) editore;
						ISelectionProvider sp = editor.getSelectionProvider();

						IDocumentProvider provider = mEditor.getDocumentProvider();

						ITextSelection selection = null;
						try {
							int line = mRulerInfo.getLineOfLastMouseButtonActivity();

							provider.connect(this);
							IDocument document = provider.getDocument(mEditor.getEditorInput());
							IRegion region = document.getLineInformation(line);
							selection = new TextSelection(document, region.getOffset(), region.getLength());
						} catch (CoreException e1) {
						} catch (BadLocationException e) {
						} finally {
							provider.disconnect(this);
						}

						if (selection != null && !selection.isEmpty()) {

							startline = selection.getStartLine() + 1;
							stopline = selection.getEndLine() + 1;

							IMarker marker;

							marker = resource.createMarker("com.eco.bio7.redit.debugMarker");
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
							marker.setAttribute(IMarker.LINE_NUMBER, new Integer(startline));
							marker.setAttribute(IMarker.MESSAGE, rDebugExpression);
							marker.setAttribute(IMarker.LOCATION, "" + startline);
							/* Mark it as a breakpoint with expression! */
							marker.setAttribute(IMarker.TEXT, "EXPRESSION");

						}

					} catch (CoreException e) {

					}
				}

			}
			
			
			else {
				return;
			}

		}
	}

	@Override
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		return new ToggleBreakpointAction(editor, rulerInfo);
	}
}