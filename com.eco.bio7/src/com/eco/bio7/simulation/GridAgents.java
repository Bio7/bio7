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
import java.util.List;

/* The movement, the search and the drawing of the agents of a grid (GridModel, ValueGridModel) */
final class GridAgents {

	private GridAgents() {
	}

	static void keep(GridAgent a, int width, int height, boolean wrap) {
		if (wrap) {
			a.x = ((a.x % width) + width) % width;
			a.y = ((a.y % height) + height) % height;
		} else {
			a.x = Math.max(0, Math.min(width - 1e-9, a.x));
			a.y = Math.max(0, Math.min(height - 1e-9, a.y));
		}
	}

	static void forward(GridAgent a, double distance, int width, int height, boolean wrap) {
		a.x += distance * Simulation.sinDeg(a.heading);
		a.y -= distance * Simulation.cosDeg(a.heading);
		keep(a, width, height, wrap);
	}

	/* The shortest difference on a torus */
	private static double delta(double d, int size, boolean wrap) {
		return wrap ? d - size * Math.rint(d / size) : d;
	}

	static void face(GridAgent a, double x, double y, int width, int height, boolean wrap) {
		double dx = delta(x - a.x, width, wrap), dy = delta(y - a.y, height, wrap);
		if (dx != 0 || dy != 0) {
			a.heading = Simulation.headingOf(dx, -dy);
		}
	}

	static double distance(GridAgent a, GridAgent b, int width, int height, boolean wrap) {
		double dx = delta(b.x - a.x, width, wrap), dy = delta(b.y - a.y, height, wrap);
		return Math.sqrt(dx * dx + dy * dy);
	}

	static <A extends GridAgent> ArrayList<A> at(List<A> agents, int x, int y) {
		ArrayList<A> out = new ArrayList<>();
		for (A a : agents) {
			if (a.cellX() == x && a.cellY() == y) {
				out.add(a);
			}
		}
		return out;
	}

	static <A extends GridAgent> ArrayList<A> near(List<A> agents, GridAgent a, double radius, int width, int height, boolean wrap) {
		ArrayList<A> out = new ArrayList<>();
		for (A o : agents) {
			if (o != a && distance(a, o, width, height, wrap) <= radius) {
				out.add(o);
			}
		}
		return out;
	}

	/* The shapes of the agents (drawn on top of the grid) */
	static List<Shape> shapes(List<? extends GridAgent> agents) {
		List<Shape> out = new ArrayList<>();
		List<? extends GridAgent> copy;
		try {
			copy = new ArrayList<>(agents);
		} catch (RuntimeException e) {
			return out;
		}
		for (GridAgent a : copy) {
			if (a == null) {
				continue;
			}
			double size = a.size > 0 ? a.size : 0.8;
			if (a.image != null && !a.image.isEmpty()) {
				out.add(new Shape("image", new double[] { a.x, a.y }, null, null, a.image, size, a.heading));
				continue;
			}
			String color = a.color != null ? a.color : "#e53935";
			String shape = a.shape != null ? a.shape : "circle";
			if (shape.equals("arrow")) {
				double r = size / 2;
				double[] d = { 0, 140, 180, 220 }, f = { 1, 1, 0.35, 1 }, p = new double[8];
				for (int k = 0; k < 4; k++) {
					p[k * 2] = a.x + r * f[k] * Simulation.sinDeg(a.heading + d[k]);
					p[k * 2 + 1] = a.y - r * f[k] * Simulation.cosDeg(a.heading + d[k]);
				}
				out.add(new Shape("polygon", p, "#00000066", color, null, 1, 0));
			} else if (shape.equals("square")) {
				out.add(new Shape("rect", new double[] { a.x - size / 2, a.y - size / 2, size, size }, "#00000066", color, null, 1, 0));
			} else {
				out.add(new Shape("circle", new double[] { a.x, a.y, size / 2 }, "#00000066", color, null, 1, 0));
			}
		}
		return out;
	}
}
