/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.floweditor.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

/**
 * A note of a flow: an amber card with rounded corners, a folded corner at the
 * top right and an info icon.
 */
public class BentCornerFigure extends Figure implements RunningFigure {

	/** The size of the folded corner. */
	protected static int DEFAULT_CORNER_SIZE = 14;

	private int cornerSize;
	private boolean running;

	public BentCornerFigure() {
		setCornerSize(DEFAULT_CORNER_SIZE);
	}

	public int getCornerSize() {
		return cornerSize;
	}

	public void setCornerSize(int newSize) {
		cornerSize = newSize;
	}

	public void setRunning(boolean running) {
		if (this.running != running) {
			this.running = running;
			repaint();
		}
	}

	protected void paintFigure(Graphics graphics) {
		FlowColors.Palette p = FlowColors.get();
		/* The text in the color of the note */
		if (!p.noteText.equals(getLocalForegroundColor())) {
			setForegroundColor(p.noteText);
		}
		Graphics g = graphics;
		g.setAntialias(SWT.ON);
		Rectangle r = getBounds().getResized(-1, -1).getShrinked(1, 1);
		int c = Math.min(cornerSize, Math.min(r.width, r.height) / 2);
		int radius = 8;
		g.setBackgroundColor(running ? p.runFill : p.noteFill);
		g.fillRoundRectangle(r, radius, radius);
		g.setForegroundColor(running ? p.runBorder : p.noteBorder);
		g.setLineWidthFloat(running ? 2.5f : 1.5f);
		g.drawRoundRectangle(r, radius, radius);
		/* The folded corner: cut off (the color of the canvas) and folded down */
		Color canvas = getParent() != null ? getParent().getBackgroundColor() : null;
		if (canvas != null) {
			g.setBackgroundColor(canvas);
			g.fillPolygon(new int[] { r.right() - c, r.y - 2, r.right() + 2, r.y - 2, r.right() + 2, r.y + c });
		}
		g.setBackgroundColor(running ? p.runBorder : p.noteBorder);
		g.fillPolygon(new int[] { r.right() - c, r.y, r.right(), r.y + c, r.right() - c + 3, r.y + c });
		g.drawLine(r.right() - c, r.y, r.right(), r.y + c);
		/* The info icon */
		int ix = r.x + 10, iy = r.y + 10, d = 14;
		g.setForegroundColor(p.noteIcon);
		g.setLineWidthFloat(1.6f);
		g.drawOval(ix, iy, d, d);
		g.setLineCap(SWT.CAP_ROUND);
		g.drawLine(ix + d / 2, iy + 6, ix + d / 2, iy + 10);
		g.drawLine(ix + d / 2, iy + 3, ix + d / 2, iy + 3);
	}
}
