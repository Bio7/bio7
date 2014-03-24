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

package com.eco.bio7.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ConsoleNativeShellAction extends Action {

	public ConsoleNativeShellAction() {
		super("Shell");

		setId("Shell");
		setText("Shell");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/konsole.gif")));

		this.setImageDescriptor(desc);
	}

	public void run() {


		ConsoleInterpreterAction inst = ConsoleInterpreterAction.getInstance();
		inst.startShell();

	}

}