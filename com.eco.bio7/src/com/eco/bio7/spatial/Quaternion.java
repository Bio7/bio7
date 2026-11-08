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
 * A rotation in 3D as a unit quaternion (x, y, z, w) in double precision, e.g.,
 * the rotation of the 3D view with the mouse (see ArcBall). The quaternion is
 * normalized after each operation, so that the product of many rotations stays
 * a rotation (no drift of the rounding errors).
 */
public final class Quaternion {

	private double x, y, z, w = 1;

	/** Creates the identity (no rotation). */
	public Quaternion() {
	}

	/**
	 * Creates a rotation from the components of a quaternion (normalized).
	 *
	 * @param x the x component (the axis times sin(angle / 2))
	 * @param y the y component
	 * @param z the z component
	 * @param w the scalar component (cos(angle / 2))
	 */
	public Quaternion(double x, double y, double z, double w) {
		set(x, y, z, w);
	}

	/**
	 * Returns the rotation by an angle around an axis.
	 *
	 * @param ax      the x component of the axis
	 * @param ay      the y component of the axis
	 * @param az      the z component of the axis
	 * @param radians the angle (radians, counterclockwise looking against the
	 *                axis)
	 * @return the rotation (the identity for an axis of length 0)
	 */
	public static Quaternion fromAxisAngle(double ax, double ay, double az, double radians) {
		double length = Math.sqrt(ax * ax + ay * ay + az * az);
		if (!(length > 0)) {
			return new Quaternion();
		}
		double s = Math.sin(radians / 2.0) / length;
		return new Quaternion(ax * s, ay * s, az * s, Math.cos(radians / 2.0));
	}

	/** Sets the identity (no rotation). */
	public void setIdentity() {
		x = y = z = 0;
		w = 1;
	}

	/** Sets the components (normalized; the identity for a zero quaternion). */
	public void set(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		normalize();
	}

	/** Sets this rotation to another rotation. */
	public void set(Quaternion q) {
		x = q.x;
		y = q.y;
		z = q.z;
		w = q.w;
	}

	/**
	 * Sets this rotation to the product a * b: first the rotation b, then the
	 * rotation a (like the product of their matrices). This rotation can be one
	 * of the factors.
	 */
	public void mul(Quaternion a, Quaternion b) {
		double nx = a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y;
		double ny = a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x;
		double nz = a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w;
		double nw = a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z;
		set(nx, ny, nz, nw);
	}

	/* A unit quaternion (rounding errors removed); the identity if it has no length */
	private void normalize() {
		double length = Math.sqrt(x * x + y * y + z * z + w * w);
		if (!(length > 1e-300) || Double.isInfinite(length)) {
			setIdentity();
			return;
		}
		x /= length;
		y /= length;
		z /= length;
		w /= length;
	}

	/** Returns the angle of the rotation (radians, 0 - pi). */
	public double getAngle() {
		return 2 * Math.acos(Math.min(1, Math.abs(w)));
	}

	/** Returns the components {x, y, z, w}. */
	public double[] getComponents() {
		return new double[] { x, y, z, w };
	}

	/**
	 * Writes the rotation as a 4 x 4 matrix in the column-major order of OpenGL
	 * (e.g., for glMultMatrixf).
	 *
	 * @param dest an array of at least 16 values
	 */
	public void get(float[] dest) {
		double xx = x * x, yy = y * y, zz = z * z;
		double xy = x * y, xz = x * z, yz = y * z;
		double wx = w * x, wy = w * y, wz = w * z;
		/* Column 0 */
		dest[0] = (float) (1 - 2 * (yy + zz));
		dest[1] = (float) (2 * (xy + wz));
		dest[2] = (float) (2 * (xz - wy));
		dest[3] = 0;
		/* Column 1 */
		dest[4] = (float) (2 * (xy - wz));
		dest[5] = (float) (1 - 2 * (xx + zz));
		dest[6] = (float) (2 * (yz + wx));
		dest[7] = 0;
		/* Column 2 */
		dest[8] = (float) (2 * (xz + wy));
		dest[9] = (float) (2 * (yz - wx));
		dest[10] = (float) (1 - 2 * (xx + yy));
		dest[11] = 0;
		/* Column 3 */
		dest[12] = 0;
		dest[13] = 0;
		dest[14] = 0;
		dest[15] = 1;
	}

	@Override
	public String toString() {
		return "Quaternion(" + x + ", " + y + ", " + z + ", " + w + ")";
	}
}
