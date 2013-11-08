package com.eco.bio7.javaeditor.actions;

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

	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		try {
			int off;
			int offend;

			off = selection.getOffset();
			offend = off + selection.getLength();

			if (doc.getChar(off) == '/' && doc.getChar(off + 1) == '*' && doc.getChar(offend - 2) == '*' && doc.getChar(offend - 1) == '/') {
				doc.replace(off, 1, "");
				doc.replace(offend - 2, 1, "");
				doc.replace(off, 1, "");
				doc.replace(offend - 4, 1, "");
			}

		} catch (BadLocationException e) {

			e.printStackTrace();
		}
	}

}