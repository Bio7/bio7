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
 * The bounding box of a mesh model (the smallest and the largest coordinate of
 * the vertices along x, y and z): the size, the largest side and the center.
 */
public class ModelDimensions {

	private final float[] min = new float[3];
	private final float[] max = new float[3];
	private boolean empty = true;

	/** Creates empty dimensions (all sizes 0 until a vertex is added). */
	public ModelDimensions() {
	}

	/**
	 * Starts the bounding box at a vertex (the former vertices are discarded).
	 * 
	 * @param vertex the first vertex
	 */
	public void set(Tuple3 vertex) {
		empty = true;
		update(vertex);
	}

	/**
	 * Extends the bounding box to contain a vertex.
	 * 
	 * @param vertex a vertex
	 */
	public void update(Tuple3 vertex) {
		float[] v = { vertex.getX(), vertex.getY(), vertex.getZ() };
		for (int i = 0; i < 3; i++) {
			if (empty || v[i] < min[i]) {
				min[i] = v[i];
			}
			if (empty || v[i] > max[i]) {
				max[i] = v[i];
			}
		}
		empty = false;
	}

	/** Returns the size along x. */
	public float getWidth() {
		return max[0] - min[0];
	}

	/** Returns the size along y. */
	public float getHeight() {
		return max[1] - min[1];
	}

	/** Returns the size along z. */
	public float getDepth() {
		return max[2] - min[2];
	}

	/** Returns the largest of the width, the height and the depth. */
	public float getLargest() {
		return Math.max(getWidth(), Math.max(getHeight(), getDepth()));
	}

	/** Returns the center of the bounding box. */
	public Tuple3 getCenter() {
		return new Tuple3((min[0] + max[0]) / 2f, (min[1] + max[1]) / 2f, (min[2] + max[2]) / 2f);
	}

	/** Returns the smallest coordinates {x, y, z}. */
	public float[] getMin() {
		return min.clone();
	}

	/** Returns the largest coordinates {x, y, z}. */
	public float[] getMax() {
		return max.clone();
	}

	/** Prints the bounding box (the ranges, the centers and the sizes) to the console. */
	public void reportDimensions() {
		Tuple3 c = getCenter();
		String[] axes = { "x", "y", "z" };
		float[] center = { c.getX(), c.getY(), c.getZ() };
		float[] size = { getWidth(), getHeight(), getDepth() };
		for (int i = 0; i < 3; i++) {
			System.out.println(String.format("%s: %.4g to %.4g, center %.4g, size %.4g", axes[i], min[i], max[i], center[i], size[i]));
		}
	}
}
