/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.simulation;

/**
 * An agent of an {@link AgentModel}: a position (x, y, z in the units of the
 * box of the model, z is the height) and an optional color (e.g., "#d62728",
 * null: the color of the model). Subclasses add their own fields (e.g., a
 * velocity) and methods.
 */
public class Agent {

	public double x, y, z;

	public String color;

	/** An image (loaded with loadImage) instead of the sphere: a billboard (null: none). */
	public String image;

	/** A 3D model (glTF, loaded with loadModel) instead of the sphere (null: none). */
	public String model;

	/** The animation of the model (its name or number, null: the first animation, "none": still). */
	public String animation;

	/** The heading in degrees (0 the direction of y, clockwise; NaN: the direction of the last movement). */
	public double heading = Double.NaN;

	/** The size of the image or the height of the model (0: the diameter of the sphere). */
	public double size;

	public Agent() {
	}

	public Agent(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
