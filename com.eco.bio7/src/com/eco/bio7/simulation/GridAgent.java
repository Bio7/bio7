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
 * An agent on the grid of a {@link GridModel}, {@link ValueGridModel} or
 * {@link GeoGridModel} (a turtle on the patches): the position in cells (x to
 * the right, y down; the centre of the cell (i, j) is i + 0.5, j + 0.5), the
 * heading (degrees, 0 up, clockwise) and its look: a color with a shape
 * ("circle", "arrow" in the heading or "square"), or an image (loadImage,
 * turned to the heading). The size is in cells. Extend it with own fields:
 *
 * <pre>
 * static class Ant extends GridAgent {
 * 	boolean carrying;
 * }
 * </pre>
 *
 * The model moves the agents (forward, moveTo, face: on a torus or at the edge
 * of the grid) and finds them (agentsAt, agentsNear); they are drawn on the grid
 * in Bio7 and in the HTML page.
 */
public class GridAgent {

	/** The position in cells (the centre of the cell (i, j): i + 0.5, j + 0.5). */
	public double x = 0.5, y = 0.5;

	/** The heading in degrees: 0 up, 90 to the right (clockwise). */
	public double heading;

	/** The color of the shape (e.g., "#e53935"). */
	public String color = "#e53935";

	/** The name of an image (loadImage) instead of the shape (null: the shape). */
	public String image;

	/** The size in cells. */
	public double size = 0.8;

	/** The shape: "circle", "arrow" (points in the heading) or "square". */
	public String shape = "circle";

	/** An agent at the position 0, 0. */
	public GridAgent() {
	}

	/** An agent at the position (cells). */
	public GridAgent(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/** The column of the cell of the agent. */
	public int cellX() {
		return (int) Math.floor(x);
	}

	/** The row of the cell of the agent. */
	public int cellY() {
		return (int) Math.floor(y);
	}
}
