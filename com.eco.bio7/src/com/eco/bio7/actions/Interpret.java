/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.rcp.StartBio7Utils;

public class Interpret extends Action {

	private IFile file;

	public Interpret(String text) {
		super(text);
		setId("com.eco.bio7.interpretgroovybeanshell");
		setActionDefinitionId("com.eco.bio7.interpretgroovybeanshellaction");

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

		if (file.getFileExtension().equals("bsh")) {

			String a = doc.get();
			com.eco.bio7.compile.BeanShellInterpreter.interpretJob(a, null);

			Quad2d quad2dInstance = Quad2d.getQuad2dInstance();
			if (quad2dInstance != null) {
				quad2dInstance.repaint();
			}
		} else if (file.getFileExtension().equals("groovy")) {

			String a = doc.get();
			com.eco.bio7.compile.GroovyInterpreter.interpretJob(a, null);

		} /*else if (file.getFileExtension().equals("js")) {
			
			String a = doc.get();
			
			}*/

	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {

	}

}
