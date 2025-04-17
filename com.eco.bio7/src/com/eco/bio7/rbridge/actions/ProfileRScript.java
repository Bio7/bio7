package com.eco.bio7.rbridge.actions;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.collection.Work;

public class ProfileRScript {

	private String path;

	public String profileSource(IEditorPart rEditor, boolean selection) {
		StringBuffer buff = new StringBuffer();
		buff.append("library(profvis)");
		buff.append(System.lineSeparator());
		buff.append("p<-profvis({");
		buff.append(System.lineSeparator());
		if (selection) {
			buff.append(getSelectedText(rEditor));
		} else {
			buff.append(getText(rEditor));
		}
		buff.append(System.lineSeparator());
		buff.append("})");
		buff.append(System.lineSeparator());
		buff.append("p");
		String editorScript = buff.toString();
		return editorScript;
	}

	public String profileSourceRserve(IEditorPart rEditor, boolean selection) {
		// IWorkbenchPart workbenchPart =
		// PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		// IFile file = (IFile)
		// workbenchPart.getSite().getPage().getActiveEditor().getEditorInput().getAdapter(IFile.class);

		/*
		 * String pathParent = new File(file.getRawLocation().toOSString()).getParent();
		 * String dirTemp = pathParent.replace("\\", "/"); String dir = dirTemp + "/";
		 */

		File tempFile = null;
		try {
			tempFile = File.createTempFile("profile", ".html");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		/* Delete files when finishes! */
		tempFile.deleteOnExit();

		String dirTemp = tempFile.toString().replace("\\", "/");
		path = dirTemp;
		StringBuffer buff = new StringBuffer();
		buff.append("library(profvis)");
		buff.append(System.lineSeparator());
		buff.append("p<-profvis(expr={");
		buff.append(System.lineSeparator());
		if (selection) {
			buff.append(getSelectedText(rEditor));
		} else {
			buff.append(getText(rEditor));
		}
		buff.append(System.lineSeparator());

		buff.append("})");
		buff.append(System.lineSeparator());
		buff.append("htmlwidgets::saveWidget(p,\"" + path + "\",selfcontained = TRUE)");
		System.out.println(path.replace("/", "\\"));
		String editorScript = buff.toString();
		return editorScript;
	}

	private String getText(IEditorPart rEditor) {
		ITextEditor editor = (ITextEditor) rEditor;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		return doc.get();
	}

	private String getSelectedText(IEditorPart rEditor) {
		ITextEditor editor = (ITextEditor) rEditor;

		ISelectionProvider sp = editor.getSelectionProvider();

		ISelection selectionsel = sp.getSelection();

		ITextSelection selection = (ITextSelection) selectionsel;

		return selection.getText();
	}

	public void openWebBrowser() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(new Runnable() {

			public void run() {

				String temp = "file:///" + path;
				String url = temp.replace("\\", "/");
				System.out.println(url);

				Work.openView("com.eco.bio7.browser.Browser");
				BrowserView b = BrowserView.getBrowserInstance();
				b.browser.setJavascriptEnabled(true);
				b.setLocation(url);

			}
		});
	}

}
