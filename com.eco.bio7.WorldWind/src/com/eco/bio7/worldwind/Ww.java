/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.worldwind;

import com.eco.bio7.worldwind.swt.WorldWindowNewtCanvasSWT;

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
	public static WorldWindowNewtCanvasSWT getWwd() {
		
		return WorldWindView.getWwd();
	}

}
