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
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.image.Util;

import ij.macro.Interpreter;

public class DebugSetVariableAction extends Action {

	public DebugSetVariableAction() {
		super("SetVar");

		setId("SetVar");
		setText("Set Variable");
		setToolTipText("Sets or changes a defined variable expression in debug mode.");
		// ImageDescriptor desc = ImageDescriptor.createFromImage(new
		// Image(Display.getCurrent(),
		// getClass().getResourceAsStream("/pics/help.gif")));
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/ijmacrodebug/variable_view.png");
		this.setImageDescriptor(desc);
	}

	public void run() {

		SetDebugMacroValueDialog dialog = new SetDebugMacroValueDialog(Util.getShell(), "Set Value");

		// get the new values from the dialog
		if (dialog.open() == Window.OK) {

			String name = dialog.getUser();
			String value = dialog.getPassword();
			Interpreter interpreter = Interpreter.getInstance();
			if (interpreter != null) {

				if (isNumeric(value)) {
					interpreter.setVariable(name, Double.parseDouble(value));
				} else {
					interpreter.setVariable(name, value);
				}
			}

		}

	}

	public static boolean isNumeric(String strNum) {
		try {
			double d = Double.parseDouble(strNum);
		} catch (NumberFormatException | NullPointerException nfe) {
			return false;
		}
		return true;
	}

}