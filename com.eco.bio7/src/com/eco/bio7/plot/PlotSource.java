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
package com.eco.bio7.plot;

import java.util.Locale;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The source of a plot of the shells: to the clipboard or into an opened
 * editor of the language (R: *.R, *.Rmd; Python: *.py) for a customization.
 */
public final class PlotSource {

	private PlotSource() {
	}

	/** Copies the source to the clipboard. UI thread. */
	public static void copy(String code) {
		Clipboard cb = new Clipboard(Display.getCurrent());
		try {
			cb.setContents(new Object[] { code }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			cb.dispose();
		}
	}

	/**
	 * Inserts the source at the cursor of the active editor of the language (else
	 * of the first opened one); without such an editor the source is copied to
	 * the clipboard. Returns the message for the user. UI thread.
	 */
	public static String insert(String code, String language) {
		ITextEditor editor = editor(language);
		if (editor == null) {
			copy(code);
			return "No " + language + " editor is open (" + extensions(language) + "): the source was copied to the clipboard.\n\nPaste it into a new "
					+ language + " file.";
		}
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		int offset = doc.getLength();
		if (editor.getSelectionProvider() != null && editor.getSelectionProvider().getSelection() instanceof ITextSelection sel && sel.getOffset() >= 0) {
			offset = sel.getOffset() + sel.getLength();
		}
		try {
			/* At the start of the next line of the cursor */
			int line = doc.getLineOfOffset(offset);
			if (offset > doc.getLineOffset(line)) {
				offset = line + 1 < doc.getNumberOfLines() ? doc.getLineOffset(line + 1) : doc.getLength();
			}
			String text = (offset > 0 && offset == doc.getLength() && !doc.get(offset - 1, 1).equals("\n") ? "\n" : "") + code + (code.endsWith("\n") ? "" : "\n");
			doc.replace(offset, 0, text);
			editor.selectAndReveal(offset, text.length());
			editor.getSite().getPage().activate(editor);
			return "The " + language + " source of the plot was inserted into " + editor.getEditorInput().getName() + " (line " + (doc.getLineOfOffset(offset) + 1)
					+ ").";
		} catch (BadLocationException e) {
			copy(code);
			return "The source could not be inserted: it was copied to the clipboard.";
		}
	}

	/* The active editor of the language, else the first opened one */
	private static ITextEditor editor(String language) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ? null
				: PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null) {
			return null;
		}
		IEditorPart active = page.getActiveEditor();
		if (matches(active, language)) {
			return (ITextEditor) active;
		}
		for (IEditorReference ref : page.getEditorReferences()) {
			IEditorPart part = ref.getEditor(false);
			if (matches(part, language)) {
				return (ITextEditor) part;
			}
		}
		return null;
	}

	private static boolean matches(IEditorPart part, String language) {
		if (!(part instanceof ITextEditor) || part.getEditorInput() == null) {
			return false;
		}
		String name = part.getEditorInput().getName().toLowerCase(Locale.ROOT);
		return "R".equals(language) ? name.endsWith(".r") || name.endsWith(".rmd") || name.endsWith(".qmd") : name.endsWith(".py");
	}

	private static String extensions(String language) {
		return "R".equals(language) ? "*.R, *.Rmd" : "*.py";
	}
}
