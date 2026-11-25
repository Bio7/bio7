/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

/**
 * A file of a flow (a script, an ImageJ macro, a program ...): a card in the
 * colors of the theme with an icon in the accent of the type (scripts blue,
 * ImageJ macros violet), the type and the name of the file. The tooltip shows
 * the path.
 */
public class FileFigure extends Figure implements RunningFigure {

	private String path = "";
	private boolean running;
	private final Label tip = new Label();

	public FileFigure() {
		setToolTip(tip);
	}

	/** The path of the file (shown: its type and name). */
	public void setPath(String path) {
		this.path = path == null ? "" : path;
		tip.setText(this.path);
		repaint();
	}

	public String getPath() {
		return path;
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
		int radius = FlowColors.RADIUS;
		g.setBackgroundColor(running ? p.runFill : p.surface);
		g.fillRoundRectangle(r, radius, radius);
		if (running) {
			FlowColors.paintRunning(g, r, radius);
		} else {
			g.setForegroundColor(p.border);
			g.setLineWidthFloat(1.5f);
			g.drawRoundRectangle(r, radius, radius);
		}
		boolean macro = FlowColors.isMacro(path);
		String type = FlowColors.typeName(path) + (running ? " \u00b7 running" : "");
		String name = FlowColors.fileName(path);
		Font small = FlowColors.small();
		Font bold = FlowColors.bold();
		Dimension typeSize = FigureUtilities.getTextExtents("Ag", small);
		Dimension nameSize = FigureUtilities.getTextExtents("Ag", bold);

		if (r.width < 90) {
			paintCompact(g, r, p, macro, name, small);
			return;
		}
		/* The icon: a tinted square with the glyph of a file */
		int pad = Math.min(10, Math.max(4, r.height / 6));
		int box = Math.max(14, Math.min(28, r.height - 2 * pad));
		int bx = r.x + pad;
		int by;
		boolean wide = r.width >= 130 || r.height < typeSize.height + nameSize.height + box / 2 + 2 * pad;
		if (wide) {
			by = r.y + (r.height - box) / 2;
		} else {
			by = r.y + pad;
		}
		g.setBackgroundColor(macro ? p.violetTint : p.blueTint);
		g.fillRoundRectangle(new Rectangle(bx, by, box, box), 7, 7);
		paintGlyph(g, bx, by, box, macro ? p.violet : p.blue, macro);

		int right = r.right() - pad;
		if (wide) {
			/* Icon left, the type over the name */
			int tx = bx + box + 8;
			int h = typeSize.height + 1 + nameSize.height;
			int ty = r.y + (r.height - h) / 2;
			g.setFont(small);
			g.setForegroundColor(p.secondary);
			g.drawText(ellipsize(type, small, right - tx), tx, ty);
			g.setFont(bold);
			g.setForegroundColor(p.text);
			g.drawText(ellipsize(name, bold, right - tx), tx, ty + typeSize.height + 1);
		} else {
			/* The type beside the icon, the name below */
			int tx = bx + box + 6;
			g.setFont(small);
			g.setForegroundColor(p.secondary);
			g.drawText(ellipsize(type, small, right - tx), tx, by + (box - typeSize.height) / 2);
			g.setFont(bold);
			g.setForegroundColor(p.text);
			int ny = by + box + Math.max(4, (r.bottom() - by - box - nameSize.height) / 2 - 2);
			g.drawText(ellipsize(name, bold, right - r.x - pad), r.x + pad, Math.min(ny, r.bottom() - nameSize.height - 2));
		}
	}

	/* A small card (e.g., 50 x 50): the icon at the top, the name below in the small font */
	private void paintCompact(Graphics g, Rectangle r, FlowColors.Palette p, boolean macro, String name, Font small) {
		Dimension textSize = FigureUtilities.getTextExtents("Ag", small);
		int box = Math.max(14, Math.min(28, Math.min(r.width - 12, r.height - textSize.height - 12)));
		int h = box + 3 + textSize.height;
		int bx = r.x + (r.width - box) / 2;
		int by = r.y + Math.max(4, (r.height - h) / 2);
		g.setBackgroundColor(macro ? p.violetTint : p.blueTint);
		g.fillRoundRectangle(new Rectangle(bx, by, box, box), 7, 7);
		paintGlyph(g, bx, by, box, macro ? p.violet : p.blue, macro);
		String shown = ellipsize(name, small, r.width - 8);
		int tw = FigureUtilities.getTextWidth(shown, small);
		g.setFont(small);
		g.setForegroundColor(p.text);
		g.drawText(shown, r.x + (r.width - tw) / 2, Math.min(by + box + 3, r.bottom() - textSize.height - 2));
	}

	/* A page with a folded corner and lines (a macro: brackets) */
	private static void paintGlyph(Graphics g, int bx, int by, int box, org.eclipse.swt.graphics.Color color, boolean macro) {
		double s = box / 28.0;
		int x0 = bx + (int) Math.round(8 * s), y0 = by + (int) Math.round(5 * s);
		int w = (int) Math.round(12 * s), h = (int) Math.round(18 * s), f = (int) Math.round(4 * s);
		g.setForegroundColor(color);
		g.setLineWidthFloat(1.6f);
		g.setLineCap(SWT.CAP_ROUND);
		g.setLineJoin(SWT.JOIN_ROUND);
		g.drawPolygon(new int[] { x0, y0, x0 + w - f, y0, x0 + w, y0 + f, x0 + w, y0 + h, x0, y0 + h });
		g.drawPolyline(new int[] { x0 + w - f, y0, x0 + w - f, y0 + f, x0 + w, y0 + f });
		int ly = y0 + (int) Math.round(9 * s);
		int lx = x0 + (int) Math.round(3 * s), lw = w - (int) Math.round(6 * s);
		if (macro) {
			int m = x0 + w / 2, d = Math.max(2, (int) Math.round(2.5 * s));
			g.drawPolyline(new int[] { m - 1, ly, m - 1 - d, ly + d, m - 1, ly + 2 * d });
			g.drawPolyline(new int[] { m + 1, ly, m + 1 + d, ly + d, m + 1, ly + 2 * d });
		} else {
			g.drawLine(lx, ly, lx + lw, ly);
			g.drawLine(lx, ly + (int) Math.round(4 * s), lx + lw, ly + (int) Math.round(4 * s));
		}
	}

	/* The text shortened with "..." to the width */
	static String ellipsize(String text, Font font, int width) {
		if (width <= 0 || text.isEmpty()) {
			return "";
		}
		if (FigureUtilities.getTextWidth(text, font) <= width) {
			return text;
		}
		String dots = "\u2026";
		int n = text.length();
		while (n > 0 && FigureUtilities.getTextWidth(text.substring(0, n) + dots, font) > width) {
			n--;
		}
		return n == 0 ? "" : text.substring(0, n) + dots;
	}
}
