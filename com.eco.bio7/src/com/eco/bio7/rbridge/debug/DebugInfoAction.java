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

package com.eco.bio7.rbridge.debug;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;

import com.eco.bio7.util.Util;

public class DebugInfoAction extends Action {
	

	public DebugInfoAction() {
		super("DebugInfo");

		setId("DebugInfo");
		setText("(?)");

        
		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/help.gif")));

		this.setImageDescriptor(desc);
	}

	public void run() {
		ToolTip infoTip = new ToolTip(Util.getShell(), SWT.BALLOON | SWT.ICON_INFORMATION);
		infoTip.setText("Info!");
		infoTip.setAutoHide(false);
		infoTip.setMessage(""
				+ "Console commands:\n"
				+ "n     		-     	next\n"
				+ "s     		-     	step into\n"
				+ "f     		-     	finish\n"
				+ "c or cont 	-	continue\n"
				+ "Q     		-     	quit\n"
				+ "where 		-     	show stack\n"
				+ "help  		-     	show help\n"
				+ "<expr>		-     	evaluate expression");
		infoTip.setVisible(true);
	}

}