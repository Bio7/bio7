/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Elias Volanakis - initial API and implementation
  *******************************************************************************/
package com.eco.bio7.floweditor.model;

import org.eclipse.swt.graphics.Image;

/**
 * A rectangular shape.
 * 
 * @author Elias Volanakis
 */
public class TriangleShape extends Shape {
	/** A 16x16 pictogram of a rectangular shape. */
	private static final Image TRIANGLE_ICON = createImage("icons/start.gif");

	private static final long serialVersionUID = 1;

	public Image getIcon() {
		return TRIANGLE_ICON;
	}

	public String toString() {
		return "Start";
	}
}
