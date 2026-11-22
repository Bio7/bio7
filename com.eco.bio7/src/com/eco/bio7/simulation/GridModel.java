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

	@Override
	protected void prepareDisplay() {
		GridDisplay.prepare();
	}

	@Override
	protected void display() {
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
