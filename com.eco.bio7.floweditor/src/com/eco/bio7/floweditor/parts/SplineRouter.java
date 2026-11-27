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
package com.eco.bio7.floweditor.parts;

import org.eclipse.draw2d.AbstractRouter;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Connections as curves (cubic Bézier splines): a connection leaves its source
 * and enters its target perpendicular to the side of the shape (e.g., the left
 * or right corner of a decision sideways, a file below or above). The curve is
 * a polyline of many points, so the arrow follows the direction of the curve.
 */
public class SplineRouter extends AbstractRouter {

	/* The number of the segments of a curve */
	private static final int SEGMENTS = 20;

	@Override
	public void route(Connection conn) {
		Point start = getStartPoint(conn).getCopy();
		Point end = getEndPoint(conn).getCopy();
		/* The directions perpendicular to the sides (absolute coordinates as the anchors) */
		PrecisionPoint out = direction(conn.getSourceAnchor(), start, end);
		PrecisionPoint in = direction(conn.getTargetAnchor(), end, start);
		double distance = Math.hypot(end.x - start.x, end.y - start.y);
		double d = Math.max(30, 0.45 * distance);
		double c1x = start.x + out.preciseX() * d, c1y = start.y + out.preciseY() * d;
		double c2x = end.x + in.preciseX() * d, c2y = end.y + in.preciseY() * d;
		PointList points = new PointList(SEGMENTS + 1);
		for (int i = 0; i <= SEGMENTS; i++) {
			double t = (double) i / SEGMENTS, u = 1 - t;
			double x = u * u * u * start.x + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * end.x;
			double y = u * u * u * start.y + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * end.y;
			PrecisionPoint p = new PrecisionPoint(x, y);
			conn.translateToRelative(p);
			points.addPoint(p);
		}
		conn.setPoints(points);
	}

	/*
	 * The direction of a curve at the point of an anchor: perpendicular to the
	 * side of the shape where the point is (left, right, top or bottom); without a
	 * shape the direction to the other end.
	 */
	private static PrecisionPoint direction(ConnectionAnchor anchor, Point at, Point other) {
		IFigure owner = anchor != null ? anchor.getOwner() : null;
		if (owner != null) {
			Rectangle r = owner.getBounds().getCopy();
			owner.translateToAbsolute(r);
			Point c = r.getCenter();
			double dx = (at.x - c.x) / Math.max(1.0, r.width / 2.0);
			double dy = (at.y - c.y) / Math.max(1.0, r.height / 2.0);
			if (Math.abs(dx) > 0.01 || Math.abs(dy) > 0.01) {
				return Math.abs(dx) >= Math.abs(dy) ? new PrecisionPoint(Math.signum(dx), 0) : new PrecisionPoint(0, Math.signum(dy));
			}
		}
		double len = Math.max(1, Math.hypot(other.x - at.x, other.y - at.y));
		return new PrecisionPoint((other.x - at.x) / len, (other.y - at.y) / len);
	}
}
