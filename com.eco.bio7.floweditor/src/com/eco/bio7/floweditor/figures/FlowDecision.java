package com.eco.bio7.floweditor.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

/**
 * A decision of a flow: a diamond in the orange accent (as a loop: both control
 * the flow).
 */
public final class FlowDecision extends Shape implements RunningFigure {

	private boolean running;

	public void setRunning(boolean running) {
		if (this.running != running) {
			this.running = running;
			repaint();
		}
	}

	private PointList diamond() {
		Rectangle r = getBounds().getCropped(new Insets(2));
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
	}

	protected void fillShape(Graphics g) {
		/* Painted in paintFigure */
	}

	protected void outlineShape(Graphics g) {
		/* Painted in paintFigure */
	}
}
