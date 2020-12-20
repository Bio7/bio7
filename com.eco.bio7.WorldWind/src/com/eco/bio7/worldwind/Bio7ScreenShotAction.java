/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.worldwind;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import ij.ImagePlus;

import java.awt.image.BufferedImage;
import java.io.File;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;



/**
 * @author tag
 * @version $Id: ScreenShotAction.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class Bio7ScreenShotAction implements RenderingListener {
	WorldWindow wwd;
	private boolean screenshot;
	

	public Bio7ScreenShotAction(WorldWindow wwd,boolean screen) {
		this.wwd = wwd;
        this.screenshot=screen;
	}

	public void stageChanged(RenderingEvent event) {
		
		//System.out.println("aa");
		if (event.getStage().equals(RenderingEvent.AFTER_BUFFER_SWAP)&&screenshot==true) {

			GLAutoDrawable glad = (GLAutoDrawable) event.getSource();
			int[] viewport = new int[4];
			glad.getGL().glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
			/*
			 * BufferedImage im=Screenshot.readToBufferedImage(viewport[2] + 10,
			 * viewport[3], false); glad.getGL().glViewport(0, 0, glad.getWidth(),
			 * glad.getHeight()); ImagePlus imp = new ImagePlus("Spatial", im); imp.show();
			 */

		}
        screenshot=false;
		this.wwd.removeRenderingListener(this);
	}
}
