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
 * A simulation on a grid of cells (e.g., a cellular automaton like the Game of
 * Life): the cells have integer states which are displayed with the colors of
 * the palette in an ImageJ image (and in the 2D view of the HTML export).
 *
 * <pre>
 * public class Life extends GridModel {
 * 	&#64;Param(min = 0, max = 1, restart = true)
 * 	double density = 0.3;
 *
 * 	public void init() {
 * 		size(120, 120);
 * 		palette("#ffffff", "#1f3a93");
 * 		for (int i = 0; i &lt; cells.length; i++)
 * 			cells[i] = random() &lt; density ? 1 : 0;
 * 	}
 *
 * 	public void step() {
 * 		for (int y = 0; y &lt; height; y++)
 * 			for (int x = 0; x &lt; width; x++) {
 * 				int n = countNeighbors(x, y, 1);
 * 				setNext(x, y, get(x, y) == 1 ? (n == 2 || n == 3 ? 1 : 0) : (n == 3 ? 1 : 0));
 * 			}
 * 		swap();
 * 	}
 * }
 * </pre>
 */
public abstract class GridModel extends Simulation {

	/** The size of the grid (cells). */
	public int width, height;

	/** The states of the cells (index y * width + x). */
	public int[] cells = new int[0];

	/** The next states (written with setNext, exchanged with swap()). */
	public int[] next = new int[0];

	/** true: the grid is a torus (the neighbors wrap at the borders), false: outside cells are 0. */
	public boolean wrap = true;

	private String[] palette = { "#ffffff", "#000000" };

	final GridDisplay grid = new GridDisplay(modelName());

	/** Creates a grid of w * h cells (all 0). */
	public void size(int w, int h) {
		width = w;
		height = h;
		cells = new int[w * h];
		next = new int[w * h];
	}

	/* The colors of the states (for the subclasses) */
	String[] paletteColors() {
		return palette;
	}

	/** The colors of the states 0, 1, 2, ... (e.g., "#ffffff", "#000000"). */
	public void palette(String... colors) {
		palette = colors;
	}

	/** The state of a cell (wraps at the borders with wrap = true, else 0 outside). */
	public int get(int x, int y) {
		if (wrap) {
			x = ((x % width) + width) % width;
			y = ((y % height) + height) % height;
		} else if (x < 0 || y < 0 || x >= width || y >= height) {
			return 0;
		}
		return cells[y * width + x];
	}

	/** Sets the state of a cell (now). */
	public void set(int x, int y, int state) {
		cells[y * width + x] = state;
	}

	/** Sets the next state of a cell (visible after swap()). */
	public void setNext(int x, int y, int state) {
		next[y * width + x] = state;
	}

	/** Exchanges the cells and the next states (after all cells got their next state). */
	public void swap() {
		int[] temp = cells;
		cells = next;
		next = temp;
	}

	/** The number of the 8 neighbors (Moore neighborhood) with the state. */
	public int countNeighbors(int x, int y, int state) {
		int n = 0;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				if ((dx != 0 || dy != 0) && get(x + dx, y + dy) == state) {
					n++;
				}
			}
		}
		return n;
	}

	/** The number of the 4 neighbors (left, right, up, down: von Neumann neighborhood) with the state. */
	public int count4(int x, int y, int state) {
		return (get(x - 1, y) == state ? 1 : 0) + (get(x + 1, y) == state ? 1 : 0) + (get(x, y - 1) == state ? 1 : 0) + (get(x, y + 1) == state ? 1 : 0);
	}

	/** The number of the cells with the state. */
	public int count(int state) {
		int n = 0;
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] == state) {
				n++;
			}
		}
		return n;
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
		int[] colors = new int[palette.length];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = Simulation.rgb(palette[i]);
		}
		int[] c = cells;
		int last = colors.length - 1;
		grid.show(width, height, i -> last < 0 ? 0 : colors[Math.max(0, Math.min(last, c[i]))], this::display, this);
	}

	/** The ImageJ image of the grid (null before the first display). */
	public ImagePlus getImage() {
		return grid.getImage();
	}
}
