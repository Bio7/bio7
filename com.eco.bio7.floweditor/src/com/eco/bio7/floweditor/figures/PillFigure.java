/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
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
package com.eco.bio7.floweditor.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

/**
 * The start or the end of a flow: a pill in the green accent with an icon (a
 * triangle for the start, a square for the end) and its name.
 */
public class PillFigure extends Figure implements RunningFigure {

	private final boolean start;
	private boolean running;

	public PillFigure(boolean start) {
		this.start = start;
	}

	public void setRunning(boolean running) {
		if (this.running != running) {
			this.running = running;
			repaint();
		}
	}

	protected void paintFigure(Graphics g) {
		FlowColors.Palette p = FlowColors.get();
		g.setAntialias(SWT.ON);
		g.setTextAntialias(SWT.ON);
		Rectangle r = getBounds().getResized(-1, -1).getShrinked(1, 1);
		int radius = Math.min(r.height, r.width);
		g.setBackgroundColor(running ? p.runFill : p.greenTint);
		g.fillRoundRectangle(r, radius, radius);
		if (running) {
			FlowColors.paintRunning(g, r, radius);
		} else {
			g.setForegroundColor(p.green);
			g.setLineWidthFloat(1.5f);
			g.drawRoundRectangle(r, radius, radius);
		}
		/* The icon and the name, centered (only the icon if the name doesn't fit) */
		String name = start ? "Start" : "End";
		g.setFont(FlowColors.bold());
		Dimension text = FigureUtilities.getTextExtents(name, FlowColors.bold());
		int icon = Math.max(6, Math.min(10, r.height / 3));
		int gap = 6;
		boolean withName = icon + gap + text.width + radiusPad(r) <= r.width;
		if (!withName) {
			icon = Math.max(8, Math.min(16, Math.min(r.width, r.height) * 2 / 5));
		}
		int x = withName ? r.x + (r.width - icon - gap - text.width) / 2 : r.x + (r.width - icon) / 2;
		int cy = r.y + r.height / 2;
		g.setBackgroundColor(p.green);
		if (start) {
			g.fillPolygon(new int[] { x, cy - icon / 2, x + icon, cy - icon / 2, x + icon / 2, cy + icon / 2 });
		} else {
			g.fillRoundRectangle(new Rectangle(x, cy - icon / 2, icon, icon), 3, 3);
		}
		if (withName) {
			g.setForegroundColor(p.greenText);
			g.drawText(name, x + icon + gap, cy - text.height / 2);
		}
	}

	/* The room the rounded ends take */
	private static int radiusPad(Rectangle r) {
		return Math.min(r.height, r.width) / 2 + 6;
	}
}
