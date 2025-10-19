/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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
 * This class creates Individuals objects for the use in 3d.
 * 
 * @author Bio7
 * 
 */
public class Individual3d {
	private float x;
	private float y;
	private float z;
	private float size;
	private float alpha;
	private float r;
	private float g;
	private float b;

	public Individual3d(int x, int y, int z, int size, float alpha) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.size = size;
		this.alpha = alpha;
	}

	/**
	 * Returns the x-coordinate of the individual.
	 * 
	 * @return the x-coordinate as an integer value.
	 */
	public float getX() {
		return x;
	}

	/**
	 * Sets the x-coordinate of the individual.
	 * 
	 * @param x
	 *            the x-coordinate as an integer value.
	 */
	public void setX(float x) {
		this.x = x;
	}

	/**
	 * Returns the y-coordinate of the individual.
	 * 
	 * @return the y-coordinate as an integer value.
	 */
	public float getY() {
		return y;
	}

	/**
	 * Sets the y-coordinate of the individual.
	 * 
	 * @param y
	 *            the y-coordinate as an integer value.
	 */
	public void setY(float y) {
		this.y = y;
	}

	/**
	 * Returns the z-coordinate of the individual.
	 * 
	 * @return the z-coordinate as an integer value.
	 */

	public float getZ() {
		return z;
	}

	/**
	 * Sets the z-coordinate of the individual.
	 * 
	 * @param z
	 *            the z-coordinate as an integer value.
	 */
	public void setZ(float z) {
		this.z = z;
	}

	/**
	 * Returns the size of the individual.
	 * 
	 * @return the size as an integer value.
	 */
	public float getSize() {
		return size;
	}

	/**
	 * Sets the size of the individual.
	 * 
	 * @param size
	 *            the size as an integer value.
	 */
	public void setSize(float size) {
		this.size = size;
	}

	/**
	 * Returns the alpha value of the individual.
	 * 
	 * @return the alpha value as an integer value.
	 */
	public float getAlpha() {
		return alpha;
	}

	/**
	 * Sets the alpha of the individual.
	 * 
	 * @param alpha
	 *            the alpha as an integer value.
	 */
	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

}
