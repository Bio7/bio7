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
package com.eco.bio7.reditor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.eco.bio7.reditor.antlr.refactor.RefactorDocUtil;
import com.eco.bio7.reditors.REditor;

public class SurroundTryCatch implements IEditorActionDelegate {

	private IEditorPart targetEditor;

	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
		action.setActionDefinitionId("com.eco.bio7.reditor.surround.trycatch");

		if (targetEditor != null)
			targetEditor.getSite().getKeyBindingService().registerAction(action);

	}

	public void run(final IAction action) {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		REditor reditor = (REditor) editore;
		IDocumentProvider dp = reditor.getDocumentProvider();
		IDocument doc = dp.getDocument(reditor.getEditorInput());

		ISelectionProvider sp = reditor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;
		int off = selection.getOffset();
		int numWhite = RefactorDocUtil.getLeadingWhitespaceNumber(off, doc);
		String ls = System.lineSeparator();
		String space = "  ";
		/* If the selected text is indented we consider the indention here! */
		String indent = "";
		if (numWhite > 0) {
			indent = String.format("%-" + (numWhite) + "s", "");
		}
		StringBuffer buf = new StringBuffer();

		buf.append("out <- tryCatch({" + ls);
		buf.append(indent + space + space + selection.getText() + ls);
		buf.append(indent + space + "}, " + ls);
		buf.append(indent + space + "warning = function(w) {" + ls);
		buf.append(indent + space + space + "message(\"Warning message:\")" + ls);
		buf.append(indent + space + space + "message(w)" + ls);
		buf.append(indent + space + "}, " + ls);
		buf.append(indent + space + "error = function(e) {" + ls);
		buf.append(indent + space + space + "message(\"Error message:\")" + ls);
		buf.append(indent + space + space + "message(e)" + ls);
		buf.append(indent + space + "}, " + ls);
		buf.append(indent + space + "finally = {" + ls);
		buf.append(indent + space + space + "#cleanup" + ls);
		buf.append(indent + "})");
		String rep = buf.toString();
		try {
			doc.replace(off, selection.getLength(), buf.toString());
		} catch (BadLocationException e) {
			
			e.printStackTrace();
		}
		/* Scroll to the selection! */
		reditor.selectAndReveal(selection.getOffset(), rep.length());

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {

	}

}
