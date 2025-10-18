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

package com.eco.bio7.rbridge.debug;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.Bio7Plugin;

public class DebugStepIntoAction extends Action {
	
	private DebugProgress progress;

	public DebugStepIntoAction() {
		super("Step Into");

		setId("StepInto");
		setText("Step Into - Step into function calls");

		//ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepinto_co.gif")));
		ImageDescriptor desc = Bio7Plugin.getImageDescriptor("/icons/rdebug/stepinto_co.png");
		this.setImageDescriptor(desc);
	}

	public void run() {
		if(progress==null){
			 progress=new DebugProgress();
			 progress.progress("s");
			}
			
			else{
				progress.progress("s");
			}

	}
}