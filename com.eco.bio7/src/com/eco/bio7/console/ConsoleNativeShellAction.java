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

package com.eco.bio7.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.Bio7Plugin;

public class ConsoleNativeShellAction extends Action {

	public ConsoleNativeShellAction() {
		super("Shell");

		setId("Shell");
		setText("Shell");

		ImageDescriptor desc = ImageDescriptor.createFromImage(Bio7Plugin.getImageDescriptor("/icons/views/console.png").createImage());

		this.setImageDescriptor(desc);
	}

	public void run() {


		ConsoleInterpreterAction inst = ConsoleInterpreterAction.getInstance();
		inst.startShell();

	}

}