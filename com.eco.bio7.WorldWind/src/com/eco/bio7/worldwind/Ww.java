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

package com.eco.bio7.worldwind;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

/**
 * This class gives access to the WorldWind GLCanvas.
 * 
 * @author Bio7
 * 
 */
public class Ww {

	/**
	 * 
	 * This method returns the WorldWindowGLCanvas.
	 * 
	 * @return the WorldWindowGLCanvas.
	 */
	public static WorldWindowGLCanvas getWwd() {
		
		return WorldWindView.getWwd();
	}

}
