/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.floweditor.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * An figure that draws an ellipse filling its bounds.
 */
public class Ellipse extends Shape implements MouseListener {

	/**
	 * Constructs a new Ellipse with the default values of a Shape.
	 * 
	 * @since 2.0
	 */
	public Ellipse() {
		addMouseListener(this);
		/*
		 * MouseMotionListener listener = new MouseMotionListener.Stub() {
		 * public void mouseEntered(MouseEvent me) { ((Shape) me.getSource())
		 * .setBackgroundColor(ColorConstants.yellow); } public void
		 * mouseExited(MouseEvent me) { ((Shape) me.getSource())
		 * .setBackgroundColor(ColorConstants.white); } };
		 * addMouseMotionListener(listener);
		 */

	}

	/**
	 * Returns <code>true</code> if the given point (x,y) is contained within
	 * this ellipse.
	 * 
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @return <code>true</code>if the given point is contained
	 */
	public boolean containsPoint(int x, int y) {
		if (!super.containsPoint(x, y))
			return false;
		Rectangle r = getBounds();
		long ux = x - r.x - r.width / 2;
		long uy = y - r.y - r.height / 2;
		return ((ux * ux) << 10) / (r.width * r.width) + ((uy * uy) << 10) / (r.height * r.height) <= 256;
	}

	/**
	 * Fills the ellipse.
	 * 
	 * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
	 */
	protected void fillShape(Graphics graphics) {

		graphics.fillOval(getBounds());

	}

	/**
	 * Outlines the ellipse.
	 * 
	 * @see org.eclipse.draw2d.Shape#outlineShape(org.eclipse.draw2d.Graphics)
	 */
	protected void outlineShape(Graphics graphics) {
		Rectangle r = Rectangle.SINGLETON;
		r.setBounds(getBounds());
		r.width--;
		r.height--;
		r.shrink((lineWidth - 1) / 2, (lineWidth - 1) / 2);
		graphics.setAntialias(1);
		graphics.drawOval(r);

	}

	public void mouseDoubleClicked(MouseEvent arg0) {

	}

	public void mousePressed(MouseEvent arg0) {

	}

	public void mouseReleased(MouseEvent arg0) {
	}

}