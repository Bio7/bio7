package com.eco.bio7.javaeditor.actions;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavaFormatterAction extends Action {

	
	public JavaFormatterAction() {
		super("format");
		setId("com.eco.bio7.Java_format");

		setText("Format Java");
	}

	public void run() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());

		String code = doc.get();

		String formattedString = null;

		ICodeFormatter formatter = ToolFactory.createCodeFormatter();

		formattedString = formatter.format(code, 0, null, null);

		doc.set(formattedString);

	}

	public void dispose() {
		

	}

}