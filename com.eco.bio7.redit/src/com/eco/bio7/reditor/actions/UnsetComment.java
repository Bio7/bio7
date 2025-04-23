package com.eco.bio7.reditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class UnsetComment extends Action {

	private final IWorkbenchWindow window;

	int startline;

	int stopline;

	public UnsetComment(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setActionDefinitionId("com.eco.bio7.r_editor_unset_comment");
	}

	public void run() {

		int offset = 0;

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		startline = selection.getStartLine();
		stopline = selection.getEndLine();

		

		try {
			int off;
			char rep = '#';
			for (int i = 0; startline <= stopline; i++) {
				off = doc.getLineOffset(startline);
				char ch = doc.getChar(off);

				if (rep == ch) {
					doc.replace(off, 1, "");
				}
				startline++;

			}
		} catch (BadLocationException e) {

			e.printStackTrace();
		}

	}

}