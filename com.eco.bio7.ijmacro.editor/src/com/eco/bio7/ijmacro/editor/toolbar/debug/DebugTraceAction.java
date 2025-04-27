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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

import ij.macro.Interpreter;

public class DebugTraceAction extends Action {

	private static boolean fastTrace = false;
	private DebugVariablesView debugVariablesView;
	
	public static void setFastTrace(boolean fastTrace) {
		DebugTraceAction.fastTrace = fastTrace;
	}

	

	public DebugTraceAction(DebugVariablesView debugVariablesView) {
		super("Trace");
		this.debugVariablesView=debugVariablesView;
		setId("Trace");
		setText("Trace/Fast Trace");
		setToolTipText("Runs the macro, displaying variable names and values in the \"Debug\" window\nas they are encountered. Fast Trace - Same as above, but faster.");
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/ijmacrodebug/stepreturn_co.png");

		this.setImageDescriptor(desc);
	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editore != null) {
			IJMacroEditor editor = (IJMacroEditor) editore;
			if (fastTrace == false) {

				editor.setDebugMode(ij.macro.Debugger.TRACE);
				fastTrace = true;
			} else {
				editor.setDebugMode(ij.macro.Debugger.FAST_TRACE);
				fastTrace = false;
			}
		}
		
	}
}