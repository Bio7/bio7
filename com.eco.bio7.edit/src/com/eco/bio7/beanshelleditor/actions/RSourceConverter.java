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

public class RSourceConverter extends Action {

	public RSourceConverter() {
		super("format");
		setId("com.eco.bio7.r_source_converter");

		setText("R Source Conversion");
	}

	public void run() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		

		ITextEditor editor2 = (ITextEditor) editor;
		IDocumentProvider dp = editor2.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor2.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		int offset = selection.getOffset();
		int length = selection.getLength();

		String code = selection.getText();
		String quote="\"";
		String quote2="\\\"";
		String back="/";
		String back2="\\\\";
        String code2=code.replace(quote, quote2);
		String code3=code2.replace(back, back2);
        String eval="RServe.getConnection().eval("+quote+code3+quote+");";


		
		try {
			doc.replace(offset, length, eval);
		} catch (BadLocationException e) {
			
			e.printStackTrace();
		}

	}

	public void dispose() {
		

	}

}