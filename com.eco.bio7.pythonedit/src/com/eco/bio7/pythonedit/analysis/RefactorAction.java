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
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The refactorings of the Python editor (the analysis process does the work):
 * rename, extract variable, extract function, inline variable, organize
 * imports, surround with try/except, insert docstring. The change is one edit
 * of the document (one undo).
 */
public class RefactorAction extends Action {

	/** The refactorings: kind, label, command id, description. */
	public static final String[][] KINDS = {
			{ "rename", "Rename...", "com.eco.bio7.pythoneditor.refactor.rename", "Renames the variable, function or class at the cursor in this file" },
			{ "extract_variable", "Extract Variable...", "com.eco.bio7.pythoneditor.refactor.extract.variable",
					"Assigns the selected expression to a new variable" },
			{ "extract_function", "Extract Function...", "com.eco.bio7.pythoneditor.refactor.extract.function",
					"Moves the selected statements into a new function (parameters and results are detected)" },
			{ "inline", "Inline Variable", "com.eco.bio7.pythoneditor.refactor.inline", "Replaces the variable at the cursor with its value" },
			{ "imports", "Organize Imports", "com.eco.bio7.pythoneditor.refactor.imports", "Sorts the imports (and removes unused imports with ruff)" },
			{ "surround", "Surround with try/except", "com.eco.bio7.pythoneditor.refactor.surround", "Surrounds the selected lines with try and except" },
			{ "docstring", "Insert Docstring", "com.eco.bio7.pythoneditor.refactor.docstring",
					"Inserts a docstring (NumPy style) into the function or class at the cursor" } };

	private final ITextEditor editor;
	private final String kind;

	public RefactorAction(ITextEditor editor, String[] k) {
		super(k[1]);
		this.editor = editor;
		this.kind = k[0];
		setActionDefinitionId(k[2]);
		setToolTipText(k[3]);
	}

	@Override
	public void run() {
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (doc == null || !(editor.getSelectionProvider().getSelection() instanceof ITextSelection sel)) {
			return;
		}
		String title = getText().replace("...", "");
		if (!AnalysisClient.isAvailable()) {
			MessageDialog.openInformation(editor.getSite().getShell(), title, "No Python was found (see the Python preferences).");
			return;
		}
		try {
			String text = doc.get();
			int start = sel.getOffset(), end = sel.getOffset() + sel.getLength();
			int line = doc.getLineOfOffset(start), endLine = doc.getLineOfOffset(end);
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("kind", kind);
			args.put("source", text);
			args.put("path", editor.getEditorInput() instanceof IFileEditorInput fi && fi.getFile().getLocation() != null ? fi.getFile().getLocation().toOSString() : null);
			args.put("line", line + 1);
			args.put("col", start - doc.getLineOffset(line));
			/* A selection which ends at the start of a line: the lines before */
			int lastLine = endLine > line && end == doc.getLineOffset(endLine) ? endLine - 1 : endLine;
			args.put("endLine", kind.equals("extract_variable") ? endLine + 1 : lastLine + 1);
			args.put("endCol", end - doc.getLineOffset(endLine));
			switch (kind) {
			case "rename" -> {
				String old = identifierAt(doc, start);
				if (old == null) {
					MessageDialog.openInformation(editor.getSite().getShell(), title, "Please place the cursor on a name.");
					return;
				}
				String name = askName(title, "New name for '" + old + "':", old);
				if (name == null || name.equals(old)) {
					return;
				}
				args.put("name", name);
			}
			case "extract_variable" -> {
				if (sel.getLength() == 0) {
					MessageDialog.openInformation(editor.getSite().getShell(), title, "Please select an expression.");
					return;
				}
				String name = askName(title, "Name of the variable:", "value");
				if (name == null) {
					return;
				}
				args.put("name", name);
			}
			case "extract_function" -> {
				if (sel.getLength() == 0) {
					MessageDialog.openInformation(editor.getSite().getShell(), title, "Please select one or more statements.");
					return;
				}
				String name = askName(title, "Name of the function:", "new_function");
				if (name == null) {
					return;
				}
				args.put("name", name);
			}
			default -> {
			}
			}
			@SuppressWarnings("unchecked")
			Map<String, Object>[] result = new Map[1];
			BusyIndicator.showWhile(Display.getCurrent(), () -> result[0] = AnalysisClient.get().request("refactor", args, 60000));
			Map<String, Object> r = result[0];
			if (r == null) {
				MessageDialog.openInformation(editor.getSite().getShell(), title, "Python didn't respond.");
				return;
			}
			if (r.get("error") instanceof String error) {
				MessageDialog.openInformation(editor.getSite().getShell(), title, error);
				return;
			}
			if (!(r.get("text") instanceof String changed) || !text.equals(doc.get())) {
				return;
			}
			if (!changed.equals(text)) {
				replaceChanged(doc, text, changed);
			}
			if (r.get("cursor") instanceof List<?> c && c.size() == 3) {
				int cl = ((Number) c.get(0)).intValue() - 1, cc = ((Number) c.get(1)).intValue(), len = ((Number) c.get(2)).intValue();
				if (cl >= 0 && cl < doc.getNumberOfLines()) {
					IRegion lr = doc.getLineInformation(cl);
					int off = lr.getOffset() + Math.min(cc, lr.getLength());
					editor.selectAndReveal(off, Math.min(len, lr.getOffset() + lr.getLength() - off));
				}
			}
			if (r.get("message") instanceof String m) {
				MessageDialog.openInformation(editor.getSite().getShell(), title, m);
			}
		} catch (BadLocationException e) {
			/* The document changed */
		}
	}

	/* Replaces only the changed part (one edit: the rest of the document and the markers stay) */
	private static void replaceChanged(IDocument doc, String text, String changed) throws BadLocationException {
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

	private String askName(String title, String message, String initial) {
		IInputValidator valid = s -> s.matches("[\\p{L}_][\\p{L}\\p{N}_]*") ? null : "Not a valid Python name";
		InputDialog d = new InputDialog(editor.getSite().getShell(), title, message, initial, valid);
		return d.open() == Window.OK ? d.getValue().trim() : null;
	}

	/* The identifier at the offset (or directly before it) */
	static String identifierAt(IDocument doc, int offset) throws BadLocationException {
		int s = offset, e = offset, n = doc.getLength();
		while (s > 0 && (Character.isLetterOrDigit(doc.getChar(s - 1)) || doc.getChar(s - 1) == '_')) {
			s--;
		}
		while (e < n && (Character.isLetterOrDigit(doc.getChar(e)) || doc.getChar(e) == '_')) {
			e++;
		}
		String w = doc.get(s, e - s);
		return w.isEmpty() || Character.isDigit(w.charAt(0)) ? null : w;
	}
}
