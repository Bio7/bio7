package com.eco.bio7.beanshelleditor.actions;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class ScriptFormatterSelectAction extends Action {

	public ScriptFormatterSelectAction() {
		super("format");
		setId("com.eco.bio7.Java_format_selected");

		setText("Format Selected Source");
	}

	public void run() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor2 = (ITextEditor) editor;
		IDocumentProvider dp = editor2.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor2.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		int offset = selection.getOffset();
		int length = selection.getLength();

		String code = selection.getText();

		String formattedString = null;

		ICodeFormatter formatter = ToolFactory.createCodeFormatter();

		formattedString = formatter.format(code, 0, null, null);

		// doc.set(formattedString);
		try {
			doc.replace(offset, length, formattedString);
		} catch (BadLocationException e) {

			e.printStackTrace();
		}

	}

	public void dispose() {

	}

}