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

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.compile.BeanShellInterpreter;

public class BeanShellImportAction extends Action {

	protected final IWorkbenchWindow window;

	public BeanShellImportAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.bsh_import");
		
	}

	public void run() {
        //By calling the interpreter the imports are automatically done !!
		BeanShellInterpreter.doInterpret("", null);

	}

}