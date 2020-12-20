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

import com.eco.bio7.Bio7Plugin;

public class DebugStepFinishAction extends Action {
	
	private DebugProgress progress;

	public DebugStepFinishAction() {
		super("Finish");

		setId("Finish");
		setText("Finish (f) - Finishes the current loop or function.");
        
		//ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepbystep_co.gif")));
		ImageDescriptor desc = Bio7Plugin.getImageDescriptor("/icons/rdebug/stepbystep_co.png");
		this.setImageDescriptor(desc);
	}

	public void run() {

		if(progress==null){
			 progress=new DebugProgress();
			 progress.progress("f");
			}
			
			else{
				progress.progress("f");
			}
	}
}