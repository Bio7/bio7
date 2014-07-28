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

package com.eco.bio7.rbridge.debug;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;

public class DebugContinueAction extends Action {

	public DebugContinueAction() {
		super("Continue");

		setId("Continue");
		setText("Continue");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(
				Display.getCurrent(), getClass().getResourceAsStream(
						"/pics/stepreturn_co.gif")));

		this.setImageDescriptor(desc);
	}

	public void run() {
		
		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

		if (selectionConsole.equals("R")) {
		
		ConsolePageParticipant.pipeInputToConsole("c", true, false);
		System.out.println("c");
        ConsolePageParticipant inst=ConsolePageParticipant.getConsolePageParticipantInstance();
        IPreferenceStore store=Bio7Plugin.getDefault().getPreferenceStore();
		int port=store.getInt("R_DEBUG_PORT");
		
			
			
		
		}
		else {
			Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
		}
		
	}

}