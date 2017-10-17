/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.compile.JavaScriptInterpreter;
import com.eco.bio7.documents.JavaFXWebBrowser;
import com.eco.bio7.image.Util;
import com.eco.bio7.rcp.StartBio7Utils;
import javafx.scene.web.WebEngine;

public class JavaScriptInterpret extends Action {

	private IFile file;

	public JavaScriptInterpret(String text) {
		super(text);
		setId("com.eco.bio7.interpretjavascript");
		setActionDefinitionId("com.eco.bio7.interpretjavascriptaction");

	}

	public void run() {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			utils.cons.clear();
		}
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor == null) {
			return;
		}
		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		if (editor.getEditorInput() instanceof IFileEditorInput) {

			file = ((IFileEditorInput) editor.getEditorInput()).getFile();
		}

		if (file.getFileExtension().equals("js")) {
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			boolean interpretJavascriptInBrowser = store.getBoolean("INTERPRET_JAVASCRIPT_IN_BROWSER");
			String content = doc.get();
			if (interpretJavascriptInBrowser) {
				String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");
				if (openInJavaFXBrowser.equals("SWT_BROWSER")) {
					// Work.openView("com.eco.bio7.browser.Browser");
					Display display = Util.getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							BrowserView b = BrowserView.getBrowserInstance();
							Browser browser = b.getBrowser();
							browser.execute(content);
						}
					});
				}
				JavaFXWebBrowser browser = JavaFXWebBrowser.getJavaFXWebBrowserInstance();
				if (browser != null) {
					WebEngine webEngine = browser.getWebEngine();
					webEngine.executeScript(content);
				}

			} else {
				JavaScriptInterpreter.interpretJob(content, null);
			}

		}

		/*else if (file.getFileExtension().equals("ijm") || file.getFileExtension().equals("txt")) {
			String content = doc.get();

			ImageJMacroWorkspaceJob job = new ImageJMacroWorkspaceJob(content);

			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

					}
				}
			});

			job.schedule();
		}*/

	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {

	}

}
