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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class DebugStepIntoAction extends Action {
	
	private DebugProgress progress;

	public DebugStepIntoAction() {
		super("Step Into");

		setId("StepInto");
		setText("Step Into - Step into function calls");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepinto_co.gif")));

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