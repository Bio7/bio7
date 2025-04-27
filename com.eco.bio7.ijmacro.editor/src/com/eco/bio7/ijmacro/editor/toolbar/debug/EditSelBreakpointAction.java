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
package com.eco.bio7.ijmacro.editor.toolbar.debug;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.image.Util;


public class EditSelBreakpointAction extends AbstractRulerActionDelegate implements IEditorActionDelegate {

	public int line;

	private class ToggleBreakpointAction extends Action {

		private final IVerticalRulerInfo mRulerInfo;
		private final ITextEditor mEditor;
		private int startline;
		private int stopline;

		public ToggleBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
			super("Toggle line breakpoint");
			mEditor = editor;
			mRulerInfo = rulerInfo;
		}

		@Override
		public void run() {

			IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			IResource resource = (IResource) editore.getEditorInput().getAdapter(IResource.class);

		

			if (resource != null) {
				ITextEditor editor = (ITextEditor) editore;
				ISelectionProvider sp = editor.getSelectionProvider();

				IDocumentProvider provider = mEditor.getDocumentProvider();

				ITextSelection selection = null;
				try {
					line = mRulerInfo.getLineOfLastMouseButtonActivity();

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

					String expression = null;
					IMarker[] markersfind = findMyMarkers(resource);
					for (int i = 0; i < markersfind.length; i++) {

						if (line + 1 == markersfind[i].getAttribute(IMarker.LINE_NUMBER, -1)) {
							try {
								expression = (String) markersfind[i].getAttribute(IMarker.MESSAGE);

							} catch (CoreException e) {

								e.printStackTrace();
							}
							
							
                             
							SetDebugBreakpointMacroValueDialog dialog = new SetDebugBreakpointMacroValueDialog(
									Util.getShell(), "Set", expression);
							
							

							// get the new values from the dialog
							if (dialog.open() == Window.OK) {
								
								

								String name = dialog.getName();
								String value = dialog.getValue();
								String operator = dialog.getOperator();

								String rDebugExpression = name + " " + operator + " " + value;
								try {
									markersfind[i].setAttribute(IMarker.MESSAGE, rDebugExpression);
								} catch (CoreException e) {
									
									e.printStackTrace();
								}
							}
						}
					}

					IMarker[] markersfindAfter = findMyMarkers(resource);
					if (markersfindAfter.length < 1) {
						
					}

				}
			}

		}
	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "com.eco.bio7.ijmacro.editor.debugrulermark";

		IMarker[] markers = null;
		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

	@Override
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		return new ToggleBreakpointAction(editor, rulerInfo);
	}
}