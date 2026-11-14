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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.Bio7Plugin;

/**
 * Run to Cursor: R steps (n) until it reaches the line of the cursor in the
 * editor (in the current function), stops at an error or the script ends.
 * Embedded R.
 */
public class DebugRunToCursorAction extends Action {

	public DebugRunToCursorAction() {
		super("Run to Cursor");
		setId("RunToCursor");
		setText("Run to Cursor - Continue to the line of the cursor in the editor (without a breakpoint).");
		setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/rdebug/runtoline_co.svg"));
	}

	@Override
	public void run() {
		IEditorPart edit = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (!(edit instanceof ITextEditor editor)) {
			return;
		}
		if (!(editor.getSelectionProvider().getSelection() instanceof ITextSelection sel)) {
			return;
		}
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		try {
			int line = doc.getLineOfOffset(sel.getOffset()) + 1;
			DebugProgress.runToLine(line);
		} catch (BadLocationException e) {
			/* No line */
		}
	}
}
