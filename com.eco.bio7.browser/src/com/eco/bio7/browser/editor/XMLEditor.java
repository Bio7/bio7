package com.eco.bio7.browser.editor;

import org.eclipse.ui.editors.text.TextEditor;



public class XMLEditor extends TextEditor  {

	private ColorManager colorManager;
	

	public XMLEditor() {
		super();
		
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new XMLConfiguration(colorManager,this));
		setDocumentProvider(new XMLDocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	

}
