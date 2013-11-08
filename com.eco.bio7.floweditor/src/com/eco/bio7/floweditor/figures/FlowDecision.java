package com.eco.bio7.floweditor.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A triangular graphical figure.
 */
public final class FlowDecision extends Shape

{

	/** The points of the triangle. */
	protected PointList triangle = new PointList(4);

	/**
	 * @see Shape#fillShape(Graphics)
	 */
	protected void fillShape(Graphics g) {
		triangle.removeAllPoints();
		Rectangle drawRect = getBounds().getCropped(new Insets(1));

		triangle.addPoint(drawRect.getTop());
		triangle.addPoint(drawRect.getLeft());
		triangle.addPoint(drawRect.getBottom());
		triangle.addPoint(drawRect.getRight());

		g.fillPolygon(triangle);
	}

	/**
	 * @see Shape#outlineShape(Graphics)
	 */
	protected void outlineShape(Graphics g) {
		g.setAntialias(1);
		triangle.removeAllPoints();
		Rectangle drawRect = getBounds().getCropped(new Insets(1));
		triangle.addPoint(drawRect.getTop());
		triangle.addPoint(drawRect.getLeft());
		triangle.addPoint(drawRect.getBottom());
		triangle.addPoint(drawRect.getRight());
		g.drawPolygon(triangle);
	}

}
