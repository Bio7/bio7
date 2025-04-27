/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.ijmacro.editor.toolbar.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

public class DebugIjMacroScript extends Action {

	private IEditorPart part;
	private DebugVariablesView debugVariablesView;

	public DebugIjMacroScript(DebugVariablesView debugVariablesView) {
		super("Debug");
		 this.debugVariablesView=debugVariablesView;
		setId("Debug");
		setText("Debug Action");
		setToolTipText(
				"Starts running the macro in debug mode and opens the \"Debug\" window, which\n"
				+ "initially displays the memory usage, number of open images, and the active image's title.\n"
				+ "The macro stops running at the first executable line of code, which is highlighted.\n"
				+ "Use one of the following commands to continue execution.");
		//ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/rundebug.gif")));
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/ijmacrodebug/rundebug.png");
		this.setImageDescriptor(desc);
	}

	public void dispose() {

	}

	public void init(IWorkbenchWindow window) {

	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editore != null) {

			IJMacroEditor editor = (IJMacroEditor) editore;
			if (editor.isDirty()) {
				editor.doSave(new NullProgressMonitor());
			}
			editor.enableDebugging();
			/*
			 * Changes Mac OS 9 (CR) and Windows (CRLF) line separators to line feeds (LF).
			 */
			editor.fixLineEndings();
			editor.runMacro(true);
			
			debugVariablesView.getDebugStopAction().setEnabled(true);

			//editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		}

	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		part = targetEditor;
	}

}
