package com.eco.bio7.rbridge.actions;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.popup.actions.JavaFXWebBrowser;

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
		IWorkbenchPart workbenchPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		IFile file = (IFile) workbenchPart.getSite().getPage().getActiveEditor().getEditorInput().getAdapter(IFile.class);

		String pathParent = new File(file.getRawLocation().toOSString()).getParent();
		String dir = pathParent.replace("\\", "/");
		dir = dir + "/";
		path = dir + "profile.html";
		System.out.println("path: " + path);

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
		buff.append("htmlwidgets::saveWidget(p,\"" + path + "\")");
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
	
	public void openWebBrowser(){
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(new Runnable() {

			public void run() {

				IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
				boolean openInJavaFXBrowser = store.getBoolean("javafxbrowser");

				String temp = "file:///" + path;
				String url = temp.replace("\\", "/");
				System.out.println(url);
				if (openInJavaFXBrowser == false) {
					Work.openView("com.eco.bio7.browser.Browser");
					BrowserView b = BrowserView.getBrowserInstance();
					b.browser.setJavascriptEnabled(true);
					b.setLocation(url);
				} else {
					new JavaFXWebBrowser().createBrowser(url);
				}
			}
		});
	}

}
