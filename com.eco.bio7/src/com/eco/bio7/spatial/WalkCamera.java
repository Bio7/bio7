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

/**
 * The camera of the walkthrough and the flythrough of the 3D view: a position
 * and a viewing direction given by the heading (the angle around the vertical
 * y axis, -90 degrees looks along -z) and the pitch (up and down, limited to
 * +-89 degrees). The look at point is in the viewing direction at a fixed
 * distance. Not thread safe (used in the OpenGL thread and the event thread of
 * the 3D view like the former fields).
 */
final class WalkCamera {

	/* The start of the walkthrough: above the ground in front of the scene, looking along -z */
	private static final double START_X = 0, START_Y = 1, START_Z = 9, START_HEADING = -90;
	/* The distance of the look at point */
	private static final double LOOK_DISTANCE = 100;
	/* The largest pitch (the view up vector is the y axis) */
	private static final double MAX_PITCH = 89;
	/* The turn of an arrow key (degrees) */
	static final double TURN_STEP = 5;
	/* The vertical step of the keys Page Up and Page Down (plus the vertical speed) */
	static final double RISE_STEP = 1;

	private double x, y, z;
	private double heading, pitch;

	WalkCamera() {
		reset();
	}

	/** The start position and direction. */
	void reset() {
		x = START_X;
		y = START_Y;
		z = START_Z;
		heading = START_HEADING;
		pitch = 0;
	}

	/** Turns around the vertical axis (degrees, positive to the right). */
	void turn(double degrees) {
		heading += degrees;
	}

	/** Tilts the view up (positive) or down (degrees, limited). */
	void tilt(double degrees) {
		pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch + degrees));
	}

	/**
	 * Moves horizontally in the heading (negative: backwards); with fly also up or
	 * down in the viewing direction (the vertical distance per step).
	 */
	void move(double distance, boolean fly, double verticalDistance) {
		double h = Math.toRadians(heading);
		x += Math.cos(h) * distance;
		z += Math.sin(h) * distance;
		if (fly && distance > 0) {
			y += verticalDistance * Math.sin(Math.toRadians(pitch));
		}
	}

	/** Moves sideways (positive: to the right of the heading). */
	void strafe(double distance) {
		double h = Math.toRadians(heading);
		x -= Math.sin(h) * distance;
		z += Math.cos(h) * distance;
	}

	/** Moves up (positive) or down; not below the ground (y = 0). */
	void rise(double distance) {
		if (distance < 0 && y + distance <= 0) {
			return;
		}
		y += distance;
	}

	/** Returns the position and the look at point {x, y, z, look x, look y, look z}. */
	double[] getCamera() {
		double h = Math.toRadians(heading), p = Math.toRadians(pitch);
		double c = Math.cos(p);
		return new double[] { x, y, z, x + LOOK_DISTANCE * c * Math.cos(h), y + LOOK_DISTANCE * Math.sin(p),
				z + LOOK_DISTANCE * c * Math.sin(h) };
	}

	double getX() {
		return x;
	}

	double getY() {
		return y;
	}

	double getZ() {
		return z;
	}
}
