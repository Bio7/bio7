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
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

/**
 * A loop of a flow: a circle in the orange accent with the glyph of a loop over
 * the count (the label of the shape).
 */
public class Ellipse extends Shape implements RunningFigure {

	private boolean running;

	public void setRunning(boolean running) {
		if (this.running != running) {
			this.running = running;
			repaint();
		}
	}

	public boolean containsPoint(int x, int y) {
		if (!super.containsPoint(x, y))
			return false;
		Rectangle r = getBounds();
		long ux = x - r.x - r.width / 2;
		long uy = y - r.y - r.height / 2;
		return ((ux * ux) << 10) / (r.width * r.width) + ((uy * uy) << 10) / (r.height * r.height) <= 256;
	}

	public void paintFigure(Graphics g) {
		FlowColors.Palette p = FlowColors.get();
		/* The label (the count) in the text color of the accent */
		if (!p.orangeText.equals(getLocalForegroundColor())) {
			setForegroundColor(p.orangeText);
		}
		g.setAntialias(SWT.ON);
		Rectangle r = getBounds().getResized(-1, -1).getShrinked(1, 1);
		g.setBackgroundColor(running ? p.runFill : p.orangeTint);
		g.fillOval(r);
		g.setForegroundColor(running ? p.runBorder : p.orange);
		g.setLineWidthFloat(running ? 2.5f : 1.5f);
		g.drawOval(r);
		if (Math.min(r.width, r.height) >= 40) {
			/* The glyph of the loop above the center */
			int cx = r.x + r.width / 2;
			int cy = r.y + r.height / 2 - 11;
			int rad = 6;
			g.setForegroundColor(running ? p.runBorder : p.orange);
			g.setLineWidthFloat(1.8f);
			g.setLineCap(SWT.CAP_ROUND);
			g.drawArc(cx - rad, cy - rad, 2 * rad, 2 * rad, 60, 280);
			int ax = cx + 3, ay = cy - rad;
			g.drawPolyline(new int[] { ax - 1, ay - 3, ax + 2, ay, ax - 1, ay + 3 });
		}
	}

	protected void fillShape(Graphics graphics) {
		/* Painted in paintFigure */
	}

	protected void outlineShape(Graphics graphics) {
		/* Painted in paintFigure */
	}
}
