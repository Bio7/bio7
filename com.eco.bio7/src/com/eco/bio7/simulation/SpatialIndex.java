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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A fast search of the agents near a position: the agents are sorted into the
 * cells of a grid (the positions when the index is built). A search looks only
 * into the cells near the position, so the neighbors of all agents cost about
 * n instead of n * n distances (e.g., a flock of thousands of boids). The
 * results are in the order of the list of the agents (the same results as
 * {@link AgentModel#neighbors(Agent, double)}, in Java and in the HTML page).
 *
 * <pre>
 * SpatialIndex&lt;Boid&gt; index = index(radius); // in step(): the positions at the start of the step
 * for (Boid b : agents) {
 * 	for (Boid o : index.neighbors(b, radius)) {
 * 		...
 * 	}
 * }
 * </pre>
 *
 * A good cell size is the radius of the searches. Build the index again when
 * the agents have moved (e.g., once in each step).
 *
 * @param <A> the class of the agents
 */
public final class SpatialIndex<A extends Agent> {

	private final List<A> agents;
	private final double cell;
	private final double[] x, y, z;
	private final Map<Long, int[]> cells = new HashMap<>();
	private final java.util.IdentityHashMap<Agent, Integer> position = new java.util.IdentityHashMap<>();

	/** An index of the agents (their positions now) with the size of the cells (e.g., the radius of the searches). */
	public SpatialIndex(List<A> agents, double cellSize) {
		this.agents = new ArrayList<>(agents);
		this.cell = cellSize > 0 ? cellSize : 1;
		int n = this.agents.size();
		x = new double[n];
		y = new double[n];
		z = new double[n];
		Map<Long, int[]> count = new HashMap<>();
		long[] keys = new long[n];
		for (int i = 0; i < n; i++) {
			A a = this.agents.get(i);
			x[i] = a.x;
			y[i] = a.y;
			z[i] = a.z;
			keys[i] = key(c(x[i]), c(y[i]), c(z[i]));
			position.putIfAbsent(a, i);
			count.computeIfAbsent(keys[i], k -> new int[1])[0]++;
		}
		/* The indices of the agents in each cell (ascending) */
		Map<Long, Integer> fill = new HashMap<>();
		for (int i = 0; i < n; i++) {
			int[] list = cells.computeIfAbsent(keys[i], k -> new int[count.get(k)[0]]);
			int f = fill.merge(keys[i], 1, Integer::sum) - 1;
			list[f] = i;
		}
	}

	private long c(double v) {
		return (long) Math.floor(v / cell);
	}

	private static long key(long cx, long cy, long cz) {
		/* 21 bits for each coordinate of a cell (about +- one million cells) */
		return ((cx + (1L << 20)) & 0x1FFFFF) << 42 | ((cy + (1L << 20)) & 0x1FFFFF) << 21 | ((cz + (1L << 20)) & 0x1FFFFF);
	}

	/* The indices of the agents within the radius of the point (ascending), without the index skip */
	private int[] search(double px, double py, double pz, double radius, int skip) {
		double r2 = radius * radius;
		int[] found = new int[16];
		int n = 0;
		long x0 = c(px - radius), x1 = c(px + radius), y0 = c(py - radius), y1 = c(py + radius), z0 = c(pz - radius), z1 = c(pz + radius);
		for (long cx = x0; cx <= x1; cx++) {
			for (long cy = y0; cy <= y1; cy++) {
				for (long cz = z0; cz <= z1; cz++) {
					int[] list = cells.get(key(cx, cy, cz));
					if (list == null) {
						continue;
					}
					for (int i : list) {
						if (i == skip) {
							continue;
						}
						double dx = x[i] - px, dy = y[i] - py, dz = z[i] - pz;
						if (dx * dx + dy * dy + dz * dz <= r2) {
							if (n == found.length) {
								found = Arrays.copyOf(found, n * 2);
							}
							found[n++] = i;
						}
					}
				}
			}
		}
		int[] out = Arrays.copyOf(found, n);
		Arrays.sort(out);
		return out;
	}

	private ArrayList<A> agentsOf(int[] idx) {
		ArrayList<A> out = new ArrayList<>(idx.length);
		for (int i : idx) {
			out.add(agents.get(i));
		}
		return out;
	}

	private int indexOf(A agent) {
		Integer i = position.get(agent);
		return i == null ? -1 : i;
	}

	/** The agents within the radius of the point (in the order of the list). */
	public ArrayList<A> near(double px, double py, double pz, double radius) {
		return agentsOf(search(px, py, pz, radius, -1));
	}

	/** The other agents within the radius of the agent (its position when the index was built; in the order of the list). */
	public ArrayList<A> neighbors(A agent, double radius) {
		int i = indexOf(agent);
		return i < 0 ? agentsOf(search(agent.x, agent.y, agent.z, radius, -1)) : agentsOf(search(x[i], y[i], z[i], radius, i));
	}

	/** The nearest other agent within the radius (null if none; the first in the list for equal distances). */
	public A nearest(A agent, double radius) {
		int self = indexOf(agent);
		double px = self < 0 ? agent.x : x[self], py = self < 0 ? agent.y : y[self], pz = self < 0 ? agent.z : z[self];
		int best = -1;
		double bestD = Double.MAX_VALUE;
		for (int i : search(px, py, pz, radius, self)) {
			double dx = x[i] - px, dy = y[i] - py, dz = z[i] - pz, d2 = dx * dx + dy * dy + dz * dz;
			if (d2 < bestD) {
				bestD = d2;
				best = i;
			}
		}
		return best < 0 ? null : agents.get(best);
	}

	/** Calls the action for each pair of agents within the radius (once for a pair: a before b in the list). */
	public void forEachPair(double radius, BiConsumer<A, A> action) {
		for (int i = 0; i < agents.size(); i++) {
			for (int j : search(x[i], y[i], z[i], radius, i)) {
				if (j > i) {
					action.accept(agents.get(i), agents.get(j));
				}
			}
		}
	}

	/** The number of the agents in the index. */
	public int size() {
		return agents.size();
	}
}
