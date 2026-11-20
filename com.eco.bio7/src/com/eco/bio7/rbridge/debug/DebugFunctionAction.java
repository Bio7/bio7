/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.rbridge.debug;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Debug Function (context menu of the R editor): R stops at the next call of
 * the selected function (or the name at the cursor); a selected call (e.g.,
 * f(3)) is run at once. Also for functions of packages or without a file.
 */
public class DebugFunctionAction implements IEditorActionDelegate {

	private ITextEditor editor;

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		editor = targetEditor instanceof ITextEditor t ? t : null;
	}

	@Override
	public void run(IAction action) {
		if (editor == null || !(editor.getSelectionProvider().getSelection() instanceof ITextSelection sel)) {
			return;
		}
		String text = sel.getText();
		if (text == null || text.isBlank()) {
			text = nameAt(editor.getDocumentProvider().getDocument(editor.getEditorInput()), sel.getOffset());
		}
		DebugProgress.debugFunction(text);
	}

	/* The name (also pkg::name) at an offset of the document */
	private static String nameAt(IDocument doc, int offset) {
		try {
			IRegion line = doc.getLineInformationOfOffset(offset);
			String s = doc.get(line.getOffset(), line.getLength());
			int i = offset - line.getOffset();
			int a = i;
			while (a > 0 && isNameChar(s.charAt(a - 1))) {
				a--;
			}
			int b = i;
			while (b < s.length() && isNameChar(s.charAt(b))) {
				b++;
			}
			return s.substring(a, b);
		} catch (BadLocationException e) {
			return "";
		}
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == ':';
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
