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

public class SetComment extends Action {

	private final IWorkbenchWindow window;

	private int startline;

	private int stopline;

	public SetComment(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

	}

	public void run() {

		int offset = 0;

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();

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
			for (int i = 0; startline <= stopline; i++) {
				off = doc.getLineOffset(startline);
				doc.replace(off, 0, "#");
				startline++;
			}
		} catch (BadLocationException e) {

			e.printStackTrace();
		}

	}

}