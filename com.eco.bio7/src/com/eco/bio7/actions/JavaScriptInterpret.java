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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.compile.JavaScriptInterpreter;
import com.eco.bio7.rcp.StartBio7Utils;

import ij.IJ;

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

			String a = doc.get();
			JavaScriptInterpreter.interpretJob(a, null);

		}

		else if (file.getFileExtension().equals("ijm") || file.getFileExtension().equals("txt")) {
			String a = doc.get();

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.asyncExec(new Runnable() {

				public void run() {

					IJ.runMacro(a);
				}
			});
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {

	}

}
