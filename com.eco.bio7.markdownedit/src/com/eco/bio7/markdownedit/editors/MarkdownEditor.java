package com.eco.bio7.markdownedit.editors;

import org.eclipse.ui.editors.text.TextEditor;

public class MarkdownEditor extends TextEditor {

	private ColorManager colorManager;

	public MarkdownEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new MarkdownConfiguration(colorManager,this));
		setDocumentProvider(new MarkdownDocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}
