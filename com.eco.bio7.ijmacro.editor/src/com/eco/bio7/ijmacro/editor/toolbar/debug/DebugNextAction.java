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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;
import ij.macro.Interpreter;

public class DebugNextAction extends Action {

	private DebugVariablesView debugVariablesView;

	public DebugNextAction(DebugVariablesView debugVariablesView) {
		super("Step");
		this.debugVariablesView = debugVariablesView;
		setId("Step");
		setText("Step");
        setToolTipText("Executes the highlighted statement and advances to the next.\nThe variable names and values in the \"Debug\" window are updated.");
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/ijmacrodebug/stepover_co.png");
		this.setImageDescriptor(desc);
	}

	public void run() {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editore != null) {
			IJMacroEditor editor = (IJMacroEditor) editore;
			Interpreter interp = Interpreter.getInstance();

			if (interp == null) {

				debugVariablesView.getDebugStopAction().setEnabled(false);
				ITextSelection selection = editor.getTextSelection(editor);
				editor.selectAndReveal(selection.getOffset(), 0);
				editor.setMarkerExpression(null);
				DebugTraceAction.setFastTrace(false);
				DebugMarkerAction.setMarkerCount(0);

			}
			editor.setDebugMode(ij.macro.Debugger.STEP);
			
		}
		

	}
}