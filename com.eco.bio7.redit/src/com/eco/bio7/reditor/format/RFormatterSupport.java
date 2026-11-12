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

package com.eco.bio7.reditor.format;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The built-in R formatter in the R editor: the preferences and the
 * formatting of the document or the selected lines (in the document, undoable;
 * no R needed).
 */
public final class RFormatterSupport {

	/** The formatter of the editor: {@link #JAVA}, {@link #FORMATR} or {@link #STYLER}. */
	public static final String FORMATTER = "FORMATTER";
	public static final String JAVA = "JAVA";
	public static final String FORMATR = "FORMATR";
	public static final String STYLER = "STYLER";

	public static final String INDENT_SIZE = "JFORMAT_INDENT_SIZE";
	public static final String USE_TABS = "JFORMAT_USE_TABS";
	public static final String CONTINUATION_INDENT = "JFORMAT_CONTINUATION_INDENT";
	public static final String SPACE_OPERATORS = "JFORMAT_SPACE_OPERATORS";
	public static final String SPACE_AFTER_COMMA = "JFORMAT_SPACE_AFTER_COMMA";
	public static final String SPACE_KEYWORD_PARENTHESIS = "JFORMAT_SPACE_KEYWORD_PARENTHESIS";
	public static final String REPLACE_ASSIGNMENT = "JFORMAT_REPLACE_ASSIGNMENT";
	public static final String BRACE_NEW_LINE = "JFORMAT_BRACE_NEW_LINE";
	public static final String MAX_BLANK_LINES = "JFORMAT_MAX_BLANK_LINES";

	private RFormatterSupport() {
	}

	/** Sets the default values of the preferences. */
	public static void initializeDefaults(IPreferenceStore store) {
		RFormatterOptions d = new RFormatterOptions();
		store.setDefault(FORMATTER, JAVA);
		store.setDefault(INDENT_SIZE, d.indentSize);
		store.setDefault(USE_TABS, d.useTabs);
		store.setDefault(CONTINUATION_INDENT, d.continuationIndent);
		store.setDefault(SPACE_OPERATORS, d.spaceAroundOperators);
		store.setDefault(SPACE_AFTER_COMMA, d.spaceAfterComma);
		store.setDefault(SPACE_KEYWORD_PARENTHESIS, d.spaceBeforeKeywordParenthesis);
		store.setDefault(REPLACE_ASSIGNMENT, d.replaceAssignment);
		store.setDefault(BRACE_NEW_LINE, d.braceOnNewLine);
		store.setDefault(MAX_BLANK_LINES, d.maxBlankLines);
	}

	/** The selected formatter ({@link #JAVA}, {@link #FORMATR} or {@link #STYLER}). */
	public static String formatter(IPreferenceStore store) {
		String f = store.getString(FORMATTER);
		return FORMATR.equals(f) || STYLER.equals(f) ? f : JAVA;
	}

	/** The options of the built-in formatter from the preferences. */
	public static RFormatterOptions options(IPreferenceStore store) {
		RFormatterOptions o = new RFormatterOptions();
		o.indentSize = Math.max(0, store.getInt(INDENT_SIZE));
		o.useTabs = store.getBoolean(USE_TABS);
		o.continuationIndent = Math.max(0, store.getInt(CONTINUATION_INDENT));
		o.spaceAroundOperators = store.getBoolean(SPACE_OPERATORS);
		o.spaceAfterComma = store.getBoolean(SPACE_AFTER_COMMA);
		o.spaceBeforeKeywordParenthesis = store.getBoolean(SPACE_KEYWORD_PARENTHESIS);
		o.replaceAssignment = store.getBoolean(REPLACE_ASSIGNMENT);
		o.braceOnNewLine = store.getBoolean(BRACE_NEW_LINE);
		o.maxBlankLines = Math.max(0, store.getInt(MAX_BLANK_LINES));
		return o;
	}

	/** Formats the whole document of the editor (UI thread; one undo step). */
	public static void formatDocument(ITextEditor editor, IPreferenceStore store) {
		IDocument doc = document(editor);
		if (doc == null) {
			return;
		}
		String text = doc.get();
		String formatted;
		try {
			formatted = RFormatter.format(text, options(store));
		} catch (RuntimeException e) {
			error(e);
			return;
		}
		if (formatted.equals(text)) {
			return;
		}
		/* The caret stays on its line, the view on its top line */
		StyledText widget = widget(editor);
		int topIndex = widget == null ? -1 : widget.getTopIndex();
		int caretLine = 0;
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		if (selection != null) {
			caretLine = Math.max(0, selection.getStartLine());
		}
		try {
			doc.replace(0, doc.getLength(), formatted);
			int line = Math.min(caretLine, doc.getNumberOfLines() - 1);
			IRegion region = doc.getLineInformation(line);
			editor.selectAndReveal(region.getOffset() + leadingWhitespace(doc.get(region.getOffset(), region.getLength())).length(), 0);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		if (widget != null && topIndex >= 0) {
			widget.setTopIndex(topIndex);
		}
	}

	/**
	 * Formats the selected lines (whole lines; the indentation of the first line is
	 * kept for all). Without a selection: the line of the caret.
	 */
	public static void formatSelection(ITextEditor editor, IPreferenceStore store) {
		IDocument doc = document(editor);
		if (doc == null) {
			return;
		}
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		try {
			int startLine = selection.getStartLine();
			int endLine = selection.getEndLine();
			/* A selection ending at the start of a line doesn't include that line */
			if (selection.getLength() > 0 && endLine > startLine && doc.getLineOffset(endLine) == selection.getOffset() + selection.getLength()) {
				endLine--;
			}
			int start = doc.getLineOffset(startLine);
			IRegion last = doc.getLineInformation(endLine);
			int end = last.getOffset() + last.getLength();
			String text = doc.get(start, end - start);
			String indent = leadingWhitespace(text);
			String formatted;
			try {
				formatted = RFormatter.format(text, options(store), indent);
			} catch (RuntimeException e) {
				error(e);
				return;
			}
			if (!formatted.equals(text)) {
				doc.replace(start, end - start, formatted);
			}
			editor.selectAndReveal(start, formatted.length());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private static IDocument document(ITextEditor editor) {
		if (editor == null || editor.getDocumentProvider() == null) {
			return null;
		}
		return editor.getDocumentProvider().getDocument(editor.getEditorInput());
	}

	private static StyledText widget(ITextEditor editor) {
		Control c = editor.getAdapter(Control.class);
		return c instanceof StyledText s ? s : null;
	}

	private static String leadingWhitespace(String s) {
		int i = 0;
		while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
			i++;
		}
		return s.substring(0, i);
	}

	private static void error(RuntimeException e) {
		MessageDialog.openError(Display.getDefault().getActiveShell(), "Format R Source",
				"The code was not formatted (nothing was changed):\n" + e.getMessage());
	}
}
