/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;
import com.eco.bio7.ijmacro.rename.CoordinatesUtil;
import com.eco.bio7.ijmacro.rename.DeleteBlockingExitPolicy;
import com.eco.bio7.ijmacro.rename.TextUtil;

public class RefactorRename extends AbstractHandler {

	private ISelection selection;
	private IEditorPart targetEditor;
	// private TokenStreamRewriter rewriter;
	// private boolean global;
	// private String args;

	private Point wordOffsetAndLen;
	private IJMacroEditor iJMacroEditor;
	/*
	 * public void setActiveEditor(final IAction action, final IEditorPart
	 * targetEditor) { this.targetEditor = targetEditor;
	 * action.setActionDefinitionId("com.eco.bio7.ijmacroeditor.refactor.rename");
	 * if (targetEditor != null)
	 * targetEditor.getSite().getKeyBindingService().registerAction(action); }
	 */

	public void run(final IAction action) {

		/*
		 * IResource resource = (IResource)
		 * targetEditor.getEditorInput().getAdapter(IResource.class); if (resource !=
		 * null) {
		 * 
		 * ITextEditor editor = (ITextEditor) targetEditor; iJMacroEditor =
		 * (IJMacroEditor) editor; ProjectionViewer viewer = iJMacroEditor.getViewer();
		 * if (viewer != null) { try { startEditing(viewer); } catch (ExecutionException
		 * e) {
		 * 
		 * e.printStackTrace(); } } }
		 */

	}

	/**
	 * Mostly based on code from
	 * {@link org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal}
	 */
	private void startEditing(ISourceViewer viewer) throws ExecutionException {
		Point selOffsetAndLen = viewer.getSelectedRange();
		int selStart = CoordinatesUtil.fromOffsetAndLengthToStartAndEnd(selOffsetAndLen).x;

		IDocument document = viewer.getDocument();
		try {
			String selectedText;
			if (selOffsetAndLen.y == 0) { // no characters selected
				String documentText = document.get();
				Point wordOffsetAndLen = TextUtil.findWordSurrounding(documentText, selStart);
				if (wordOffsetAndLen != null) {
					selectedText = document.get(wordOffsetAndLen.x, wordOffsetAndLen.y);
				} else {
					IRegion selectedLine = document.getLineInformationOfOffset(selStart);
					selectedText = document.get(selectedLine.getOffset(), selectedLine.getLength());
				}
			} else {
				selectedText = document.get(selOffsetAndLen.x, selOffsetAndLen.y);
			}

			LinkedPositionGroup linkedPositionGroup = new LinkedPositionGroup();

			FindReplaceDocumentAdapter findReplaceAdaptor = new FindReplaceDocumentAdapter(document);
			IRegion matchingRegion = findReplaceAdaptor.find(0, selectedText, true, true, true, false);
			while (matchingRegion != null) {
				linkedPositionGroup.addPosition(
						new LinkedPosition(document, matchingRegion.getOffset(), matchingRegion.getLength()));

				matchingRegion = findReplaceAdaptor.find(matchingRegion.getOffset() + matchingRegion.getLength(),
						selectedText, true, true, true, false);
			}
			if (linkedPositionGroup.isEmpty()) {
				return;
			}

			LinkedModeModel model = new LinkedModeModel();
			model.addGroup(linkedPositionGroup);
			model.forceInstall();

			LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
			ui.setExitPolicy(new DeleteBlockingExitPolicy(document));
			ui.enter();

			// by default the text being edited is selected so restore original
			// selection
			viewer.setSelectedRange(selOffsetAndLen.x, selOffsetAndLen.y);
		} catch (BadLocationException e) {
			throw new ExecutionException("Editing failed", e);
		}
	}

	/*
	 * public void selectionChanged(final IAction action, final ISelection
	 * selection) { this.selection = selection; }
	 */

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		/*
		 * IResource resource = (IResource)
		 * targetEditor.getEditorInput().getAdapter(IResource.class); if (resource !=
		 * null) {
		 */
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editore instanceof IJMacroEditor) {
			IJMacroEditor iJMacroEditor = (IJMacroEditor) editore;

			ProjectionViewer viewer = iJMacroEditor.getViewer();
			if (viewer != null) {
				try {
					startEditing(viewer);
				} catch (ExecutionException e) {

					e.printStackTrace();
				}
			}
		}

		return null;
	}

}
