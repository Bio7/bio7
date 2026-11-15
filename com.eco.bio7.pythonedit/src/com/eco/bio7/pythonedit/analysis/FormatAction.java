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
package com.eco.bio7.pythonedit.analysis;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Formats the Python code of the editor with ruff or black (of the selected
 * Python): one change of the document (one undo).
 */
public class FormatAction extends Action {

	public static final String COMMAND = "com.eco.bio7.pythoneditor.format";
	private final ITextEditor editor;

	public FormatAction(ITextEditor editor) {
		super("Format");
		this.editor = editor;
		setActionDefinitionId(COMMAND);
		setToolTipText("Formats the Python code (ruff or black)");
	}

	@Override
	public void run() {
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (doc == null) {
			return;
		}
		if (!AnalysisClient.isAvailable()) {
			MessageDialog.openInformation(editor.getSite().getShell(), "Format", "No Python was found (see the Python preferences).");
			return;
		}
		String text = doc.get();
		Map<String, Object> args = new LinkedHashMap<>();
		args.put("source", text);
		args.put("path", editor.getEditorInput() instanceof IFileEditorInput fi && fi.getFile().getLocation() != null ? fi.getFile().getLocation().toOSString() : null);
		@SuppressWarnings("unchecked")
		Map<String, Object>[] result = new Map[1];
		BusyIndicator.showWhile(Display.getCurrent(), () -> result[0] = AnalysisClient.get().request("format", args, 60000));
		Map<String, Object> r = result[0];
		if (r == null) {
			MessageDialog.openInformation(editor.getSite().getShell(), "Format", "The code could not be formatted (Python didn't respond).");
			return;
		}
		if (r.get("error") instanceof String error) {
			MessageDialog.openInformation(editor.getSite().getShell(), "Format", error);
			return;
		}
		if (!(r.get("text") instanceof String formatted) || formatted.equals(text) || !text.equals(doc.get())) {
			return;
		}
		/* The cursor stays on its line (as far as possible) */
		int line = 0;
		if (editor.getSelectionProvider().getSelection() instanceof ITextSelection sel) {
			line = sel.getStartLine();
		}
		try {
			doc.replace(0, doc.getLength(), formatted);
			int target = Math.min(line, doc.getNumberOfLines() - 1);
			editor.selectAndReveal(doc.getLineOffset(target), 0);
		} catch (BadLocationException e) {
			/* The document changed */
		}
	}
}
