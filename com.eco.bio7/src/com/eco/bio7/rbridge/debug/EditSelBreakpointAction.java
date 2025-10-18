/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
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

			ConsolePageParticipant inst = ConsolePageParticipant.getConsolePageParticipantInstance();

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

							InputDialog dlg = new InputDialog(Util.getShell(), "Expression", "Enter Expressions:  e.g. 'if(x==5) browser()'", expression, null);

							if (dlg.open() == Window.OK) {
								
								try {
									markersfind[i].setAttribute(IMarker.MESSAGE, dlg.getValue());
								} catch (CoreException e) {
									
									e.printStackTrace();
								}
							}
						}
					}

					IMarker[] markersfindAfter = findMyMarkers(resource);
					if (markersfindAfter.length < 1) {
						inst.deleteDebugToolbarActions();
					}

				}
			}

		}
	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "com.eco.bio7.redit.debugMarker";

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