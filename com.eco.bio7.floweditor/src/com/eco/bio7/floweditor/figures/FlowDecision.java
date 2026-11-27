package com.eco.bio7.floweditor.figures;

import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

/**
 * A decision of a flow: a diamond in the orange accent (as a loop: both control
 * the flow). The input is the top corner, the outputs are fixed: "true" at the
 * left corner, "false" at the right corner (labelled beside the diamond).
 */
public final class FlowDecision extends Shape implements RunningFigure {

	private boolean running;

	public void setRunning(boolean running) {
		if (this.running != running) {
			this.running = running;
			repaint();
		}
	}

	/** The width beside the diamond for the labels of the outputs. */
	public static int zone(int width) {
		return Math.max(0, Math.min(34, width / 5));
	}

	/* The bounds of the diamond (between the zones of the labels) */
	private Rectangle diamondBounds() {
		Rectangle r = getBounds().getCropped(new Insets(2));
		int z = zone(getBounds().width);
		return new Rectangle(r.x + z, r.y, Math.max(4, r.width - 2 * z), r.height);
	}

	/**
	 * The output of a value (in the coordinates of the parent, as the bounds): the
	 * left corner (true) or the right corner (false).
	 */
	public Point port(boolean value) {
		Rectangle d = diamondBounds();
		return value ? d.getLeft() : d.getRight();
	}

	/** The input (the top corner, in the coordinates of the parent). */
	public Point input() {
		return diamondBounds().getTop();
	}

	private PointList diamond() {
		Rectangle r = diamondBounds();
		PointList d = new PointList(4);
		d.addPoint(r.getTop());
		d.addPoint(r.getLeft());
		d.addPoint(r.getBottom());
		d.addPoint(r.getRight());
		return d;
	}

	public void paintFigure(Graphics g) {
		FlowColors.Palette p = FlowColors.get();
		if (!p.orangeText.equals(getLocalForegroundColor())) {
			setForegroundColor(p.orangeText);
		}
		g.setAntialias(SWT.ON);
		PointList d = diamond();
		g.setBackgroundColor(running ? p.runFill : p.orangeTint);
		g.fillPolygon(d);
		g.setForegroundColor(running ? p.runBorder : p.orange);
		g.setLineWidthFloat(running ? 2.5f : 1.5f);
		g.setLineJoin(SWT.JOIN_ROUND);
		g.drawPolygon(d);
		/* The labels of the outputs above the lines (short if there is little space) */
		int z = zone(getBounds().width);
		if (z >= 8) {
			g.setForegroundColor(p.orangeText);
			g.setLineWidthFloat(1f);
			label(g, z >= 26 ? "true" : "T", port(true), true);
			label(g, z >= 26 ? "false" : "F", port(false), false);
		}
	}

	private void label(Graphics g, String text, Point port, boolean left) {
		Dimension size = FigureUtilities.getTextExtents(text, getFont());
		int x = left ? port.x - size.width - 3 : port.x + 3;
		g.drawText(text, x, port.y - size.height - 1);
	}

	protected void fillShape(Graphics g) {
		/* Painted in paintFigure */
	}

	protected void outlineShape(Graphics g) {
		/* Painted in paintFigure */
	}
}
