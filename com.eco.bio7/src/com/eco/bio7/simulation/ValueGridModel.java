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

import java.util.ArrayList;

import ij.ImagePlus;

/**
 * A simulation on a grid of values (e.g., a diffusion or a reaction-diffusion
 * model): each cell has a double value which is displayed with a color map
 * from min to max in an ImageJ image (and in the 2D view of the HTML export).
 *
 * <pre>
 * public class Heat extends ValueGridModel {
 * 	&#64;Param(min = 0, max = 0.25)
 * 	double diffusion = 0.2;
 *
 * 	public void init() {
 * 		size(100, 100);
 * 		range(0, 1);
 * 		set(50, 50, 1);
 * 	}
 *
 * 	public void step() {
 * 		for (int y = 0; y &lt; height; y++)
 * 			for (int x = 0; x &lt; width; x++)
 * 				setNext(x, y, get(x, y) + diffusion * (sum4(x, y) - 4 * get(x, y)));
 * 		swap();
 * 	}
 * }
 * </pre>
 */
public abstract class ValueGridModel extends Simulation {

	/** The default color map (viridis). */
	public static final String[] VIRIDIS = { "#440154", "#3b528b", "#21908c", "#5dc963", "#fde725" };

	/** The size of the grid (cells). */
	public int width, height;

	/** The values of the cells (index y * width + x). */
	public double[] values = new double[0];

	/** The next values (written with setNext, exchanged with swap()). */
	public double[] next = new double[0];

	/** true: the grid is a torus (the neighbors wrap at the borders), false: outside cells are 0. */
	public boolean wrap = true;

	private double min = 0, max = 1;
	private String[] colorMap = VIRIDIS;
	private final GridDisplay grid = new GridDisplay(modelName());

	/** Creates a grid of w * h cells (all 0). */
	public void size(int w, int h) {
		width = w;
		height = h;
		values = new double[w * h];
		next = new double[w * h];
	}

	/** The values of the first and the last color of the color map. */
	public void range(double min, double max) {
		this.min = min;
		this.max = max;
	}

	/** The colors of the color map from min to max (e.g., "#000000", "#ffffff"). */
	public void colorMap(String... colors) {
		if (colors.length >= 2) {
			colorMap = colors;
		}
	}

	/** The value of a cell (wraps at the borders with wrap = true, else 0 outside). */
	public double get(int x, int y) {
		if (wrap) {
			x = ((x % width) + width) % width;
			y = ((y % height) + height) % height;
		} else if (x < 0 || y < 0 || x >= width || y >= height) {
			return 0;
		}
		return values[y * width + x];
	}

	/** Sets the value of a cell (now). */
	public void set(int x, int y, double value) {
		values[y * width + x] = value;
	}

	/** Sets the next value of a cell (visible after swap()). */
	public void setNext(int x, int y, double value) {
		next[y * width + x] = value;
	}

	/** Exchanges the values and the next values. */
	public void swap() {
		double[] temp = values;
		values = next;
		next = temp;
	}

	/** The sum of the 4 neighbors (left, right, up, down), e.g., for a diffusion: sum4 - 4 * value. */
	public double sum4(int x, int y) {
		return get(x - 1, y) + get(x + 1, y) + get(x, y - 1) + get(x, y + 1);
	}

	/** The discrete Laplacian (sum4 - 4 * value): the change of a diffusion per unit of the rate. */
	public double laplacian(int x, int y) {
		return sum4(x, y) - 4 * get(x, y);
	}

	/** The sum of the 8 neighbors (Moore neighborhood). */
	public double sum8(int x, int y) {
		return sum4(x, y) + get(x - 1, y - 1) + get(x + 1, y - 1) + get(x - 1, y + 1) + get(x + 1, y + 1);
	}

	/** The mean of all values. */
	public double mean() {
		double s = 0;
		for (int i = 0; i < values.length; i++) {
			s += values[i];
		}
		return values.length == 0 ? 0 : s / values.length;
	}

	/* ===================== Agents on the grid ===================== */

	/**
	 * The agents on the grid (turtles on the patches, see {@link GridAgent}),
	 * drawn on top of the grid; Setup removes them. Add them with addAgent.
	 */
	public ArrayList<GridAgent> agents = new ArrayList<>();

	/* The shapes of the agents when the grid was shown (drawn in another thread) */
	private volatile java.util.List<Shape> shownAgents = java.util.List.of();

	/** Adds an agent (e.g., an own subclass of GridAgent) and returns it. */
	public <A extends GridAgent> A addAgent(A agent) {
		agents.add(agent);
		return agent;
	}

	/** Removes an agent. */
	public void removeAgent(GridAgent agent) {
		agents.remove(agent);
	}

	/** Moves the agent in its heading (0 up, clockwise): on a torus (wrap) or it stops at the edge. */
	public void forward(GridAgent agent, double distance) {
		GridAgents.forward(agent, distance, width, height, wrap);
	}

	/** Moves the agent to the position (cells; on a torus or limited to the grid). */
	public void moveTo(GridAgent agent, double x, double y) {
		agent.x = x;
		agent.y = y;
		GridAgents.keep(agent, width, height, wrap);
	}

	/** Turns the agent to the position (cells; the shortest way on a torus). */
	public void face(GridAgent agent, double x, double y) {
		GridAgents.face(agent, x, y, width, height, wrap);
	}

	/** The distance of two agents in cells (the shortest way on a torus). */
	public double agentDistance(GridAgent a, GridAgent b) {
		return GridAgents.distance(a, b, width, height, wrap);
	}

	/** The agents in the cell (x, y). */
	public ArrayList<GridAgent> agentsAt(int x, int y) {
		return GridAgents.at(agents, x, y);
	}

	/** The number of the agents in the cell (x, y). */
	public int countAgentsAt(int x, int y) {
		return GridAgents.at(agents, x, y).size();
	}

	/** The other agents within the radius (cells) of the agent. */
	public ArrayList<GridAgent> agentsNear(GridAgent agent, double radius) {
		return GridAgents.near(agents, agent, radius, width, height, wrap);
	}

	/* The shapes of the agents for the display (called by display()) */
	void showAgents() {
		shownAgents = GridAgents.shapes(agents);
	}

	/** The shapes of the step and (on top) the agents. */
	@Override
	public java.util.List<Shape> getDrawing() {
		java.util.List<Shape> d = super.getDrawing();
		d.addAll(shownAgents);
		return d;
	}

	@Override
	protected void prepareDisplay() {
		agents = new ArrayList<>();
		shownAgents = java.util.List.of();
		if (!headless) {
			GridDisplay.prepare();
		}
	}

	@Override
	protected void display() {
		showAgents();
		int[][] ramp = GridDisplay.ramp(colorMap);
		double[] v = values;
		double lo = min, span = max - min == 0 ? 1 : max - min;
		grid.show(width, height, i -> GridDisplay.rampColor(ramp, v[i], lo, span), this::display, this);
	}

	/** The ImageJ image of the grid (null before the first display). */
	public ImagePlus getImage() {
		return grid.getImage();
	}
}
