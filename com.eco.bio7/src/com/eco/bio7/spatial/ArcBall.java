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

package com.eco.bio7.spatial;

import com.jogamp.nativewindow.util.Point;

/**
 * The rotation of the 3D view with the mouse (a virtual trackball, after K.
 * Shoemake's arcball): a drag rotates the scene like a ball under the mouse.
 * <ul>
 * <li>The ball is round (the radius is half of the smaller side of the view),
 * a drag rotates the same in every direction (also in a wide view).</li>
 * <li>Outside of the ball the points are on a hyperbolic sheet (T. Holroyd,
 * "Virtual Trackballs Revisited"): the rotation continues smoothly without a
 * jump at the edge of the ball (near the edge it turns around the view
 * axis).</li>
 * <li>The rotation is the angle between the points on the ball (a drag of
 * half the radius from the center rotates by 30 degrees, like the former
 * arcball vertically), a unit quaternion.</li>
 * </ul>
 * The positions are window pixels from the top left (the units of the mouse
 * events and of setBounds).
 */
class ArcBall {

	private static final double EPSILON = 1.0e-9;

	/* The point of the start of the drag on the ball (unit vector) */
	private final double[] start = new double[3];
	/* The center and the radius of the ball (window pixels) */
	private double centerX, centerY, radius;

	/**
	 * Creates the arcball of a view.
	 *
	 * @param width  the width of the view (pixels)
	 * @param height the height of the view (pixels)
	 */
	public ArcBall(float width, float height) {
		setBounds(width, height);
		start[2] = 1;
	}

	/**
	 * Sets the size of the view (the center and the radius of the ball).
	 *
	 * @param width  the width of the view (pixels)
	 * @param height the height of the view (pixels)
	 */
	public void setBounds(float width, float height) {
		double w = Math.max(2, width), h = Math.max(2, height);
		centerX = w / 2.0;
		centerY = h / 2.0;
		radius = Math.min(w, h) / 2.0;
	}

	/**
	 * Maps a window position to a unit vector: on the ball inside of the radius /
	 * sqrt(2), else on the hyperbolic sheet (Holroyd), normalized.
	 */
	void mapToSphere(double px, double py, double[] v) {
		double x = (px - centerX) / radius;
		double y = (centerY - py) / radius;
		double d2 = x * x + y * y;
		double z;
		if (d2 <= 0.5) {
			/* The sphere */
			z = Math.sqrt(1.0 - d2);
		} else {
			/* The hyperbola z = 1 / (2 d) (the same height and slope at d = 1 / sqrt(2)) */
			z = 0.5 / Math.sqrt(d2);
		}
		double length = Math.sqrt(d2 + z * z);
		v[0] = x / length;
		v[1] = y / length;
		v[2] = z / length;
	}

	/** Mouse down: the start of a drag. */
	public void click(Point point) {
		mapToSphere(point.getX(), point.getY(), start);
	}

	/**
	 * Mouse drag: the rotation from the start of the drag to the point as a unit
	 * quaternion (the identity if the point didn't move).
	 *
	 * @param point    the window position
	 * @param rotation the rotation or null
	 */
	public void drag(Point point, Quaternion rotation) {
		if (rotation == null) {
			return;
		}
		double[] end = new double[3];
		mapToSphere(point.getX(), point.getY(), end);
		/* The axis (perpendicular to both points) and the angle between the points */
		double ax = start[1] * end[2] - start[2] * end[1];
		double ay = start[2] * end[0] - start[0] * end[2];
		double az = start[0] * end[1] - start[1] * end[0];
		double sin = Math.sqrt(ax * ax + ay * ay + az * az);
		double cos = start[0] * end[0] + start[1] * end[1] + start[2] * end[2];
		if (sin < EPSILON) {
			/* The same point: no rotation */
			rotation.setIdentity();
			return;
		}
		double angle = Math.atan2(sin, cos);
		double s = Math.sin(angle / 2.0) / sin;
		rotation.set(ax * s, ay * s, az * s, Math.cos(angle / 2.0));
	}
}
