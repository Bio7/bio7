/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.editors;

import org.eclipse.jface.action.*;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.*;

/**
 * Contributes interesting Java actions to the desktop's Edit menu and the
 * toolbar.
 */
public class ScriptActionContributor extends TextEditorActionContributor {

	protected RetargetTextEditorAction fContentAssistProposal;

	protected TextEditorAction fTogglePresentation;

	public ScriptActionContributor() {
		super();
		fContentAssistProposal = new RetargetTextEditorAction(
				ScriptEditorMessages.getResourceBundle(),
				"ContentAssistProposal.");
		fContentAssistProposal
				.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);

	}

	public void init(IActionBars bars) {
		super.init(bars);

		IMenuManager menuManager = bars.getMenuManager();
		IMenuManager editMenu = menuManager
				.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.add(new Separator());
			editMenu.add(fContentAssistProposal);

		}

		IToolBarManager toolBarManager = bars.getToolBarManager();
		if (toolBarManager != null) {
			toolBarManager.add(new Separator());
			toolBarManager.add(fTogglePresentation);
		}
	}

	private void doSetActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);

		ITextEditor editor = null;
		if (part instanceof ITextEditor)
			editor = (ITextEditor) part;

		fContentAssistProposal.setAction(getAction(editor,
				"ContentAssistProposal"));

		fTogglePresentation.setEditor(editor);
		fTogglePresentation.update();
	}

	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		doSetActiveEditor(part);
	}

	public void dispose() {
		doSetActiveEditor(null);
		super.dispose();
	}
}
