/*******************************************************************************
 * Part of the ruler and guide support of the Bio7 flow editor, adapted from
 * the GEF Logic example (org.eclipse.gef.examples.logic) by uniera.org and
 * M. Austenfeld.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     uniera.org - initial API
 *     M. Austenfeld - Bio7
 *******************************************************************************/
package com.eco.bio7.floweditor.ruler;

import java.io.Serializable;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;


/**
 * 
 * Comment on GuideDeleteCommand here
 * 
 * @author Song Sun
 * 
 */
public interface IArea extends Cloneable, Serializable {
	Object clone();
	String PROP_LOCATION = "Location";
	String PROP_LOCATION_X = "Location.X";
	String PROP_LOCATION_Y = "Location.Y";
	String PROP_SIZE = "Size";
	String PROP_SIZE_WIDTH = "Size.Width";
	String PROP_SIZE_HEIGHT = "Size.Height";
	String PROP_BACKGROUND_COLOR = "Background Color";
	String PROP_OPAQUE = "Text.opaque";
	Integer OPAQUE_FALSE = new Integer(0);
	Integer OPAQUE_TRUE = new Integer(1);

	Point getLocation();
	void setLocation(Point location);
	Dimension getSize();
	void setSize(Dimension size);
	RGB getBackgroundColor();
	void setBackgroundColor(RGB color);

	IGuide getHorizontalGuide();
	IGuide getVerticalGuide();
	void setHorizontalGuide(IGuide guide);
	void setVerticalGuide(IGuide guide);
	
	Image getIcon();
}
