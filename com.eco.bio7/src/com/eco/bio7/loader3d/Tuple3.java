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

package com.eco.bio7.loader3d;

/**
 * Three float values, e.g., a vertex, a normal or a texture coordinate of a
 * mesh model (see OBJModel.getVerts).
 */
public class Tuple3 {

	private float x, y, z;

	/**
	 * Creates a tuple.
	 * 
	 * @param x the first value
	 * @param y the second value
	 * @param z the third value
	 */
	public Tuple3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getZ() {
		return z;
	}

	public void setZ(float z) {
		this.z = z;
	}

	/** Returns the values as an array {x, y, z}. */
	public float[] toArray() {
		return new float[] { x, y, z };
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}
}
