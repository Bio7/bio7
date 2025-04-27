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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;
import ij.IJ;
import ij.macro.Interpreter;

public class DebugStopAction extends Action {

	private DebugVariablesView debugVariablesView;

	public DebugStopAction(DebugVariablesView debugVariablesView) {
		super("Abort");
		this.debugVariablesView = debugVariablesView;
		setId("Abort");
		setText("Abort");
		setToolTipText("Exits the macro.");
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/ijmacrodebug/terminatedlaunch_obj.png");
		this.setImageDescriptor(desc);
	}

	public void run() {

		Interpreter.abort();
		IJ.beep();
		//Table table = DebugVariablesView.getDebugVariablesGrid();
		/*if (table != null) {
			table.removeAll();
		}*/
		DebugTraceAction.setFastTrace(false);
		DebugMarkerAction.setMarkerCount(0);
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editore != null) {

			IJMacroEditor editor = (IJMacroEditor) editore;

			editor.setMarkerExpression(null);
			debugVariablesView.getDebugStopAction().setEnabled(false);
			
			//IEditorPart edit = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			//ITextEditor editor = (ITextEditor) edit;
			IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			try {
				resource.deleteMarkers("com.eco.bio7.ijmacroeditor.debugrulermarkarrow", false, IResource.DEPTH_ZERO);
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			//ITextSelection selection = editor.getTextSelection(editor);
			//editor.selectAndReveal(selection.getOffset(), 0);

		}

	}

}