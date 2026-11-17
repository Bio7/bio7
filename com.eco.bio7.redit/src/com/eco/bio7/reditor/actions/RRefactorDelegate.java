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
package com.eco.bio7.reditor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.reditor.antlr.refactor.RRefactor;

/**
 * The base of the refactorings of the R editor: computes the new text with
 * {@link RRefactor} and changes only the changed part of the document (one
 * undo, the markers and the folding of the rest stay).
 */
public abstract class RRefactorDelegate implements IEditorActionDelegate {

	protected IEditorPart targetEditor;
	private final String definitionId;

	protected RRefactorDelegate(String definitionId) {
		this.definitionId = definitionId;
	}

	@Override
	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
		action.setActionDefinitionId(definitionId);
		if (targetEditor != null) {
			targetEditor.getSite().getKeyBindingService().registerAction(action);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/** The title of the dialogs. */
	protected abstract String title();

	/** The result of the refactoring (null: canceled). */
	protected abstract RRefactor.Result compute(RRefactor refactor, String text, ITextSelection selection, Shell shell);

	@Override
	public void run(final IAction action) {
		if (!(targetEditor instanceof ITextEditor editor) || !(editor.getSelectionProvider().getSelection() instanceof ITextSelection sel)) {
			return;
		}
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (doc == null) {
			return;
		}
		Shell shell = editor.getSite().getShell();
		String text = doc.get();
		RRefactor refactor = new RRefactor(text);
		if (refactor.hasErrors()) {
			MessageDialog.openInformation(shell, title(), "The code has syntax errors. Please correct them first.");
			return;
		}
		RRefactor.Result r = compute(refactor, text, sel, shell);
		if (r == null) {
			return;
		}
		if (r.error() != null) {
			MessageDialog.openInformation(shell, title(), r.error());
			return;
		}
		try {
			if (!r.text().equals(text)) {
				replaceChanged(doc, text, r.text());
			}
			editor.selectAndReveal(r.offset(), r.length());
		} catch (BadLocationException e) {
			return;
		}
		if (r.message() != null) {
			editor.getEditorSite().getActionBars().getStatusLineManager().setMessage(title() + ": " + r.message().replace('\n', ' '));
		}
	}

	/* Replaces only the changed part (one edit) */
	static void replaceChanged(IDocument doc, String text, String changed) throws BadLocationException {
		int prefix = 0, max = Math.min(text.length(), changed.length());
		while (prefix < max && text.charAt(prefix) == changed.charAt(prefix)) {
			prefix++;
		}
		int suffix = 0;
		while (suffix < max - prefix && text.charAt(text.length() - 1 - suffix) == changed.charAt(changed.length() - 1 - suffix)) {
			suffix++;
		}
		doc.replace(prefix, text.length() - prefix - suffix, changed.substring(prefix, changed.length() - suffix));
	}

	/** Asks for a name (null: canceled). */
	protected String askName(Shell shell, String message, String initial) {
		IInputValidator valid = s -> s.trim().matches("[a-zA-Z.][a-zA-Z0-9._]*") && !s.trim().matches("\\.\\d.*") ? null : "Not a valid R name";
		InputDialog d = new InputDialog(shell, title(), message, initial, valid);
		return d.open() == Window.OK ? d.getValue().trim() : null;
	}
}
