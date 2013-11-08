/**
 * (c) Copyleft uniera.org
 */
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
