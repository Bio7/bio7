/*******************************************************************************
 * Copyright (c) 2004-2019 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.ijmacro.editor.toolbar.debug;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;
import ij.macro.Interpreter;

public class DebugStepFinishAction extends Action {

	private DebugVariablesView debugVariablesView;

	public DebugStepFinishAction(DebugVariablesView debugVariablesView) {
		super("Run");
		this.debugVariablesView = debugVariablesView;
		setId("Run");
		setText("Run");
		setToolTipText("Runs the macro to completion at normal speed.");
		//ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepbystep_co.gif")));
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/ijmacrodebug/resume_co.png");
		this.setImageDescriptor(desc);
	}

	public void run() {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editore != null) {
			IJMacroEditor editor = (IJMacroEditor) editore;
			editor.setDebugMode(ij.macro.Debugger.RUN_TO_COMPLETION);
			int numLines = editor.getDocument().getNumberOfLines();
			int lineOffset = 0;
			try {
				lineOffset = editor.getDocument().getLineOffset(numLines - 1);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			editor.selectAndReveal(lineOffset, 0);
			DebugMarkerAction.setMarkerCount(0);
			editor.setMarkerExpression(null);
			DebugTraceAction.setFastTrace(false);
			Interpreter interp = Interpreter.getInstance();
			if (interp != null) {

				debugVariablesView.getDebugStopAction().setEnabled(false);

			}
			IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			try {
				resource.deleteMarkers("com.eco.bio7.ijmacroeditor.debugrulermarkarrow", false, IResource.DEPTH_ZERO);
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}
}