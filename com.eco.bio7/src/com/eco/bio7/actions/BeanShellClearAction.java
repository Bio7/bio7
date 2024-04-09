/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.compile.BeanShellInterpreter;

public class BeanShellClearAction extends Action {

	protected final IWorkbenchWindow window;

	public BeanShellClearAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.bsh_clear");

	}

	public void run() {
		BeanShellInterpreter.doInterpret("clear();", null);

	}

}