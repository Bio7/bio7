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

import org.eclipse.ui.PlatformUI;

import com.eco.bio7.collection.Work;
import com.eco.bio7.spatial.SpatialStructure;
import com.eco.bio7.spatial.SpatialUtil;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A simulation of agents in a box (e.g., a swarm): the agents (subclasses of
 * {@link Agent}) are displayed as spheres in the 3D view of Bio7 (and in the 3D
 * view of the HTML export). The box goes from 0 to boxWidth (x), boxHeight (y)
 * and boxDepth (z, the height).
 *
 * <pre>
 * public class Walkers extends AgentModel&lt;Walkers.Walker&gt; {
 * 	static class Walker extends Agent {
 * 		Walker(double x, double y, double z) {
 * 			super(x, y, z);
 * 		}
 * 	}
 *
 * 	public void init() {
 * 		box(100, 100, 50);
 * 		for (int i = 0; i &lt; 100; i++)
 * 			agents.add(new Walker(random() * 100, random() * 100, random() * 50));
 * 	}
 *
 * 	public void step() {
 * 		for (Walker w : agents)
 * 			w.z = Math.max(0, Math.min(50, w.z + random() - 0.5));
 * 	}
 * }
 * </pre>
 *
 * @param <A> the class of the agents
 */
public abstract class AgentModel<A extends Agent> extends Simulation {

	/** The agents. */
	public ArrayList<A> agents = new ArrayList<>();

	/** The size of the box. */
	public double boxWidth = 100, boxHeight = 100, boxDepth = 100;

	/** The radius of the spheres (in the units of the box, 0: 1 % of the box). */
	public double agentRadius = 0;

	/** The color of the agents without an own color. */
	public String agentColor = "#1f77b4";

	/**
	 * The shape of the agents without an image or a model: "sphere" (default) or
	 * "glider" (a glider of triangles which points in the direction of its movement,
	 * climbs, dives and banks into turns; its size: agentRadius).
	 */
	public String agentShape = "sphere";

	/** The floor: "none" (the edges of the box, default), "grid" (lines) or "plane" (a dark floor with a grid). */
	public String floor = "none";

	/** Soft shadows of the agents on the floor. */
	public boolean shadows;

	/**
	 * The agent (its index) which the custom camera of the 3D view follows from
	 * behind (with the split view: the global view beside it); -1: none. Only in
	 * Bio7 (the HTML export shows the orbit camera).
	 */
	public int followAgent = -1;

	/**
	 * An object of the environment: a 3D model (glTF, loaded with loadModel) at a
	 * place of the box. It is no agent (not in agents) and stays until it is
	 * removed (removeModel, clearModels; Setup removes all).
	 *
	 * @param model     the name of the model (loadModel)
	 * @param x         the position (the units of the box, z the height of its base)
	 * @param heading   the heading in degrees (0 the direction of y, clockwise)
	 * @param size      the height of the model (the units of the box; 0: the diameter of an agent)
	 * @param animation the animation (its name or number, null: the first, "none": still)
	 */
	public record Placed(String model, double x, double y, double z, double heading, double size, String animation) {
	}

	/* The objects of the environment (placeModel) */
	private final List<Placed> scenery = new ArrayList<>();

	/**
	 * Places a 3D model (glTF, loaded with loadModel) in the environment, e.g., a
	 * house, a tree or a rock: it stays until it is removed (not drawn again in each
	 * step). Returns its index (removeModel).
	 */
	public int placeModel(String model, double x, double y, double z, double heading, double size) {
		return placeModel(model, x, y, z, heading, size, null);
	}

	/** Places a 3D model with an animation (its name or number, "none": still), see placeModel. */
	public int placeModel(String model, double x, double y, double z, double heading, double size, String animation) {
		synchronized (scenery) {
			scenery.add(new Placed(model, x, y, z, heading, size, animation));
			return scenery.size() - 1;
		}
	}

	/** Removes the object of the environment with the index (placeModel); the later ones move up by one. */
	public void removeModel(int index) {
		synchronized (scenery) {
			if (index >= 0 && index < scenery.size()) {
				scenery.remove(index);
			}
		}
	}

	/** Removes all objects of the environment. */
	public void clearModels() {
		synchronized (scenery) {
			scenery.clear();
		}
	}

	/** The objects of the environment (a copy). */
	public List<Placed> getModels() {
		synchronized (scenery) {
			return new ArrayList<>(scenery);
		}
	}

	/* Replaces the objects of the environment (the state of a script) */
	void setModels(List<Placed> placed) {
		synchronized (scenery) {
			scenery.clear();
			scenery.addAll(placed);
		}
	}

	/* The positions and colors of the last step for the 3D view (another thread) */
	private volatile double[] shownXyz = new double[0];
	private volatile int[] shownRgb = new int[0];

	/** Sets the size of the box. */
	public void box(double width, double height, double depth) {
		/* Only a new size changes the bounds of the view (a script model sets the box after each step) */
		boolean changed = width != boxWidth || height != boxHeight || depth != boxDepth;
		boxWidth = width;
		boxHeight = height;
		boxDepth = depth;
		if (changed) {
			bounds();
		}
	}

	/** The distance of two agents (3D); the same as {@link #agentDistance(Agent, Agent)}. */
	public double distance(Agent a, Agent b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y) + (a.z - b.z) * (a.z - b.z));
	}

	/** The distance of two agents (3D): the same name as for the agents of a grid (GridModel.agentDistance). */
	public double agentDistance(Agent a, Agent b) {
		return distance(a, b);
	}

	/**
	 * The camera of the 3D view follows the agent with this index (the split view
	 * shows the global view beside it); a negative index: no camera (the same as
	 * the field followAgent, camera_follow in Python and R).
	 */
	public void cameraFollow(int index) {
		followAgent = index < 0 ? -1 : index;
	}

	/**
	 * All pairs of agents closer than the distance (with a spatial index: fast for
	 * thousands of agents) as indices of the list: i0, j0, i1, j1, ... (i < j,
	 * ordered by i and j; as neighbor_pairs in Python and R and as links()).
	 */
	public int[] neighborPairs(double distance) {
		java.util.IdentityHashMap<Agent, Integer> idx = indices();
		java.util.List<long[]> pairs = new ArrayList<>();
		index(distance).forEachPair(distance, (a, b) -> {
			int i = idx.get(a), j = idx.get(b);
			pairs.add(new long[] { Math.min(i, j), Math.max(i, j) });
		});
		pairs.sort((p, q) -> p[0] != q[0] ? Long.compare(p[0], q[0]) : Long.compare(p[1], q[1]));
		int[] out = new int[pairs.size() * 2];
		for (int k = 0; k < pairs.size(); k++) {
			out[2 * k] = (int) pairs.get(k)[0];
			out[2 * k + 1] = (int) pairs.get(k)[1];
		}
		return out;
	}

	/** The other agents within the radius of the agent (in the order of the list). */
	public ArrayList<A> neighbors(A agent, double radius) {
		ArrayList<A> found = new ArrayList<>();
		double r2 = radius * radius;
		for (int i = 0; i < agents.size(); i++) {
			A o = agents.get(i);
			if (o != agent) {
				double dx = o.x - agent.x, dy = o.y - agent.y, dz = o.z - agent.z;
				if (dx * dx + dy * dy + dz * dz <= r2) {
					found.add(o);
				}
			}
		}
		return found;
	}

	/**
	 * A spatial index of the agents (their positions now; the size of the cells:
	 * e.g., the radius of the searches): fast searches of the neighbors of many
	 * agents (see {@link SpatialIndex}). Build it again after the agents moved
	 * (e.g., at the start of each step).
	 */
	public SpatialIndex<A> index(double cellSize) {
		return new SpatialIndex<>(agents, cellSize);
	}

	/** The nearest other agent within the radius (null if none). */
	public A nearest(A agent, double radius) {
		A best = null;
		double r2 = radius * radius;
		for (int i = 0; i < agents.size(); i++) {
			A o = agents.get(i);
			if (o != agent) {
				double dx = o.x - agent.x, dy = o.y - agent.y, dz = o.z - agent.z, d2 = dx * dx + dy * dy + dz * dz;
				if (d2 <= r2) {
					r2 = d2;
					best = o;
				}
			}
		}
		return best;
	}

	/* ===================== Networks ===================== */

	/** The color of the links (drawn as lines between the agents; null or "": not drawn). */
	public String linkColor = "#8a94a6";

	/** The width of the links in pixels. */
	public double linkWidth = 1;

	/* The links: an agent -> its linked agents (in the order of the links) and the color of the link (null: linkColor) */
	private final java.util.LinkedHashMap<Agent, java.util.LinkedHashMap<Agent, String>> links = new java.util.LinkedHashMap<>();

	/** Links two agents (undirected; no link of an agent with itself, a link once). */
	public void link(A a, A b) {
		link(a, b, null);
	}

	/** Links two agents with a color of the link (e.g., "#d62728" for a path of an infection). */
	public void link(A a, A b, String color) {
		if (a == null || b == null || a == b) {
			return;
		}
		links.computeIfAbsent(a, k -> new java.util.LinkedHashMap<>()).put(b, color);
		links.computeIfAbsent(b, k -> new java.util.LinkedHashMap<>()).put(a, color);
	}

	/** Removes the link of two agents. */
	public void unlink(A a, A b) {
		java.util.LinkedHashMap<Agent, String> la = links.get(a), lb = links.get(b);
		if (la != null) {
			la.remove(b);
		}
		if (lb != null) {
			lb.remove(a);
		}
	}

	/** Removes all links of the agent (e.g., before it is removed from the list). */
	public void removeLinks(A a) {
		java.util.LinkedHashMap<Agent, String> la = links.remove(a);
		if (la != null) {
			for (Agent o : la.keySet()) {
				java.util.LinkedHashMap<Agent, String> lo = links.get(o);
				if (lo != null) {
					lo.remove(a);
				}
			}
		}
	}

	/** Removes all links. */
	public void clearLinks() {
		links.clear();
	}

	/** True if the agents are linked. */
	public boolean linked(A a, A b) {
		java.util.LinkedHashMap<Agent, String> la = links.get(a);
		return la != null && la.containsKey(b);
	}

	/**
	 * The agents linked with the agent (in the order of the links). Remove the
	 * links of an agent (removeLinks) when it is removed from the list.
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<A> linkNeighbors(A a) {
		ArrayList<A> out = new ArrayList<>();
		java.util.LinkedHashMap<Agent, String> la = links.get(a);
		if (la != null) {
			for (Agent o : la.keySet()) {
				out.add((A) o);
			}
		}
		return out;
	}

	/** The number of links of the agent. */
	public int degree(A a) {
		java.util.LinkedHashMap<Agent, String> la = links.get(a);
		return la == null ? 0 : la.size();
	}

	/** The color of the link of two agents (null: linkColor or no link). */
	public String linkColorOf(A a, A b) {
		java.util.LinkedHashMap<Agent, String> la = links.get(a);
		return la == null ? null : la.get(b);
	}

	/** Sets the color of an existing link (null: linkColor). */
	public void setLinkColor(A a, A b, String color) {
		if (linked(a, b)) {
			link(a, b, color);
		}
	}

	/* The index of each agent in the list */
	private java.util.IdentityHashMap<Agent, Integer> indices() {
		java.util.IdentityHashMap<Agent, Integer> idx = new java.util.IdentityHashMap<>();
		for (int i = 0; i < agents.size(); i++) {
			idx.putIfAbsent(agents.get(i), i);
		}
		return idx;
	}

	/** The links as pairs of the indices of the agents in the list: i0, j0, i1, j1, ... (i < j; i ascending); the same as getLinks(). */
	public int[] links() {
		return getLinks();
	}

	/** The links as pairs of the indices of the agents in the list (i, j with i < j; i ascending). */
	public int[] getLinks() {
		java.util.IdentityHashMap<Agent, Integer> idx = indices();
		java.util.List<Integer> out = new ArrayList<>();
		for (int i = 0; i < agents.size(); i++) {
			java.util.LinkedHashMap<Agent, String> la = links.get(agents.get(i));
			if (la == null) {
				continue;
			}
			for (Agent o : la.keySet()) {
				Integer j = idx.get(o);
				if (j != null && j > i) {
					out.add(i);
					out.add(j);
				}
			}
		}
		return out.stream().mapToInt(Integer::intValue).toArray();
	}

	/** The number of links (between agents of the list). */
	public int linkCount() {
		return getLinks().length / 2;
	}

	/* The colors of the links of getLinks() (null: linkColor) */
	private String[] linkColors(int[] pairs) {
		String[] c = new String[pairs.length / 2];
		for (int k = 0; k < c.length; k++) {
			java.util.LinkedHashMap<Agent, String> la = links.get(agents.get(pairs[k * 2]));
			c[k] = la == null ? null : la.get(agents.get(pairs[k * 2 + 1]));
		}
		return c;
	}

	/** A random network: each pair of agents is linked with the probability p (Erdos-Renyi). */
	public void linkRandom(double p) {
		int n = agents.size();
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				if (random() < p) {
					link(agents.get(i), agents.get(j));
				}
			}
		}
	}

	/**
	 * A small-world network (Watts-Strogatz): a ring of the agents (in the order
	 * of the list), each linked with its k nearest (k / 2 on each side), then each
	 * link is moved to a random agent with the probability p.
	 */
	public void linkSmallWorld(int k, double p) {
		int n = agents.size(), half = Math.max(1, k / 2);
		if (n < 2) {
			return;
		}
		for (int i = 0; i < n; i++) {
			for (int d = 1; d <= half; d++) {
				link(agents.get(i), agents.get((i + d) % n));
			}
		}
		for (int i = 0; i < n; i++) {
			for (int d = 1; d <= half; d++) {
				A a = agents.get(i), b = agents.get((i + d) % n);
				if (random() < p && linked(a, b)) {
					/* A new partner (not the agent itself, not linked yet; at most n tries) */
					for (int tries = 0; tries < n; tries++) {
						A c = agents.get(randomInt(n));
						if (c != a && !linked(a, c)) {
							unlink(a, b);
							link(a, c);
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * A scale-free network (Barabasi-Albert): the first m + 1 agents are linked
	 * with each other, each further agent links with m agents before it, chosen
	 * with a probability proportional to their number of links.
	 */
	public void linkScaleFree(int m) {
		int n = agents.size();
		m = Math.max(1, m);
		java.util.List<Integer> ends = new ArrayList<>();
		int start = Math.min(n, m + 1);
		for (int i = 0; i < start; i++) {
			for (int j = i + 1; j < start; j++) {
				link(agents.get(i), agents.get(j));
				ends.add(i);
				ends.add(j);
			}
		}
		for (int i = start; i < n; i++) {
			java.util.List<Integer> chosen = new ArrayList<>();
			while (chosen.size() < Math.min(m, i)) {
				int j = ends.isEmpty() ? randomInt(i) : ends.get(randomInt(ends.size()));
				if (!chosen.contains(j)) {
					chosen.add(j);
				}
			}
			for (int j : chosen) {
				link(agents.get(i), agents.get(j));
				ends.add(i);
				ends.add(j);
			}
		}
	}

	/** Links all agents within the distance (a network of the neighborhood, with a spatial index). */
	public void linkNearby(double radius) {
		index(radius).forEachPair(radius, this::link);
	}

	/** The groups of the agents which are connected by links (each in the order of the list; isolated agents are a group). */
	public ArrayList<ArrayList<A>> components() {
		ArrayList<ArrayList<A>> out = new ArrayList<>();
		java.util.Set<Agent> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		java.util.IdentityHashMap<Agent, Integer> idx = indices();
		for (A a : agents) {
			if (seen.contains(a)) {
				continue;
			}
			java.util.List<Integer> group = new ArrayList<>();
			java.util.ArrayDeque<A> queue = new java.util.ArrayDeque<>();
			queue.add(a);
			seen.add(a);
			while (!queue.isEmpty()) {
				A c = queue.poll();
				group.add(idx.get(c));
				for (A o : linkNeighbors(c)) {
					if (idx.containsKey(o) && seen.add(o)) {
						queue.add(o);
					}
				}
			}
			java.util.Collections.sort(group);
			ArrayList<A> g = new ArrayList<>();
			for (int i : group) {
				g.add(agents.get(i));
			}
			out.add(g);
		}
		return out;
	}

	/** The number of links on the shortest path between two agents (-1: not connected). */
	public int shortestPath(A a, A b) {
		if (a == b) {
			return 0;
		}
		java.util.IdentityHashMap<Agent, Integer> dist = new java.util.IdentityHashMap<>();
		java.util.ArrayDeque<A> queue = new java.util.ArrayDeque<>();
		dist.put(a, 0);
		queue.add(a);
		while (!queue.isEmpty()) {
			A c = queue.poll();
			for (A o : linkNeighbors(c)) {
				if (!dist.containsKey(o)) {
					dist.put(o, dist.get(c) + 1);
					if (o == b) {
						return dist.get(o);
					}
					queue.add(o);
				}
			}
		}
		return -1;
	}

	/** The clustering coefficient of an agent: the share of the pairs of its linked agents which are linked too (0 with less than 2). */
	public double clustering(A a) {
		ArrayList<A> nb = linkNeighbors(a);
		int k = nb.size();
		if (k < 2) {
			return 0;
		}
		int closed = 0;
		for (int i = 0; i < k; i++) {
			for (int j = i + 1; j < k; j++) {
				if (linked(nb.get(i), nb.get(j))) {
					closed++;
				}
			}
		}
		return 2.0 * closed / (k * (k - 1.0));
	}

	/** Places the agents on a circle in the middle of the box (in the order of the list; z: the middle of the box). */
	public void layoutCircle() {
		int n = agents.size();
		double cx = boxWidth / 2, cy = boxHeight / 2, r = Math.min(boxWidth, boxHeight) * 0.42;
		for (int i = 0; i < n; i++) {
			/* cosDeg and sinDeg: the same numbers in Java and in the page */
			double a = 360.0 * i / Math.max(1, n);
			A ag = agents.get(i);
			ag.x = cx + r * cosDeg(a);
			ag.y = cy + r * sinDeg(a);
			ag.z = boxDepth / 2;
		}
	}

	/**
	 * Arranges the network in x and y (a spring layout of Fruchterman and
	 * Reingold: the links pull, all agents push each other away), in the box; z is
	 * kept. Call it once (e.g., 100 iterations in init()) or a few iterations in
	 * each step (a moving layout).
	 */
	public void layoutSpring(int iterations) {
		int n = agents.size();
		if (n == 0) {
			return;
		}
		double w = boxWidth, h = boxHeight, k = Math.sqrt(w * h / n);
		int[] pairs = getLinks();
		double[] px = new double[n], py = new double[n];
		for (int i = 0; i < n; i++) {
			px[i] = agents.get(i).x;
			py[i] = agents.get(i).y;
		}
		for (int it = 0; it < iterations; it++) {
			double temp = Math.max(w, h) * 0.1 * (1 - (double) it / iterations);
			double[] dx = new double[n], dy = new double[n];
			for (int i = 0; i < n; i++) {
				for (int j = i + 1; j < n; j++) {
					double ex = px[i] - px[j], ey = py[i] - py[j], d = Math.sqrt(ex * ex + ey * ey);
					if (d < 1e-9) {
						/* At the same place: apart by their indices */
						ex = 0.01 * k * cosDeg(37.0 * (i + j));
						ey = 0.01 * k * sinDeg(37.0 * (i + j));
						d = 0.01 * k;
					}
					double f = k * k / d;
					dx[i] += ex / d * f;
					dy[i] += ey / d * f;
					dx[j] -= ex / d * f;
					dy[j] -= ey / d * f;
				}
			}
			for (int p = 0; p + 1 < pairs.length; p += 2) {
				int i = pairs[p], j = pairs[p + 1];
				double ex = px[i] - px[j], ey = py[i] - py[j], d = Math.max(1e-9, Math.sqrt(ex * ex + ey * ey)), f = d * d / k;
				dx[i] -= ex / d * f;
				dy[i] -= ey / d * f;
				dx[j] += ex / d * f;
				dy[j] += ey / d * f;
			}
			for (int i = 0; i < n; i++) {
				double d = Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]);
				if (d > 0) {
					double s = Math.min(d, temp) / d;
					px[i] = Math.max(0, Math.min(w, px[i] + dx[i] * s));
					py[i] = Math.max(0, Math.min(h, py[i] + dy[i] * s));
				}
			}
		}
		for (int i = 0; i < n; i++) {
			agents.get(i).x = px[i];
			agents.get(i).y = py[i];
		}
	}

	/** Keeps the agent in the box (the position is limited to the box). */
	public void keepInBox(Agent a) {
		a.x = Math.max(0, Math.min(boxWidth, a.x));
		a.y = Math.max(0, Math.min(boxHeight, a.y));
		a.z = Math.max(0, Math.min(boxDepth, a.z));
	}

	/** Moves the agent to the other side of the box if it left it (x and y: a torus, z: limited). */
	public void wrapInBox(Agent a) {
		a.x = ((a.x % boxWidth) + boxWidth) % boxWidth;
		a.y = ((a.y % boxHeight) + boxHeight) % boxHeight;
		a.z = Math.max(0, Math.min(boxDepth, a.z));
	}

	@Override
	String[] displayViews() {
		return new String[] { "com.eco.bio7.spatial" };
	}

	@Override
	String[] optionsView() {
		return new String[] { "com.eco.bio7.discrete3d.Options3d", "Options3D" };
	}

	@Override
	protected void prepareDisplay() {
		agents = new ArrayList<>();
		links.clear();
		/* The environment is built again by init() */
		clearModels();
		if (!Simulation.hasDisplay() || headless) {
			return;
		}
		Work.openView("com.eco.bio7.spatial");
		bounds();
		startDrawing(10);
		/* The clicks, drags and keys of the 3D view */
		SpatialStructure.setModelInput(viewInput);
	}

	/* ----- The input of the 3D view: positions on the floor of the box ----- */

	/* The matrices of the last frame of each viewport (the main view, the split view): the floor under the mouse */
	private record ViewFrame(double[] modelview, double[] projection, int[] viewport, double scale, double ox, double oy, long time) {
	}

	private final java.util.concurrent.ConcurrentHashMap<String, ViewFrame> frames = new java.util.concurrent.ConcurrentHashMap<>();

	private final SpatialStructure.ModelInput viewInput = new SpatialStructure.ModelInput() {
		/* Only the model of the view (not an earlier model) */
		private boolean active() {
			return com.eco.bio7.methods.Compiled.getModel() == AgentModel.this;
		}

		@Override
		public boolean handles(String event) {
			return active() && AgentModel.this.handles(event);
		}

		@Override
		public void pointer(String event, double x, double y, int surfaceHeight) {
			double[] p = floorAt(x, y, surfaceHeight);
			if (p != null && active()) {
				input(event, p[0], p[1], null, true);
			}
		}

		@Override
		public void key(String key) {
			if (active()) {
				input("key", 0, 0, key, false);
			}
		}
	};

	/*
	 * The point of the floor (z = 0, in the box) under a position of the OpenGL
	 * surface (pixels from the left and the top), or null
	 */
	double[] floorAt(double x, double y, int surfaceHeight) {
		/* The viewports drawn in the last half second (the split view may be switched off) */
		long newest = 0;
		for (ViewFrame f : frames.values()) {
			newest = Math.max(newest, f.time());
		}
		double gx = x, gy = surfaceHeight - y;
		ViewFrame best = null;
		for (ViewFrame f : frames.values()) {
			int[] v = f.viewport();
			if (f.time() < newest - 500_000_000L || gx < v[0] || gy < v[1] || gx > v[0] + v[2] || gy > v[1] + v[3]) {
				continue;
			}
			/* The smallest viewport (the split view is drawn on top of the main view) */
			if (best == null || (long) v[2] * v[3] < (long) best.viewport()[2] * best.viewport()[3]) {
				best = f;
			}
		}
		if (best == null) {
			return null;
		}
		int[] v = best.viewport();
		double nx = 2 * (gx - v[0]) / v[2] - 1, ny = 2 * (gy - v[1]) / v[3] - 1;
		double[] inv = invert(multiply(best.projection(), best.modelview()));
		if (inv == null) {
			return null;
		}
		double[] a = unproject(inv, nx, ny, -1), b = unproject(inv, nx, ny, 1);
		if (a == null || b == null || Math.abs(b[2] - a[2]) < 1e-12) {
			return null;
		}
		double t = -a[2] / (b[2] - a[2]);
		if (t < 0 || t > 1) {
			return null;
		}
		double px = a[0] + t * (b[0] - a[0]), py = a[1] + t * (b[1] - a[1]);
		double mx = (px - best.ox()) / best.scale(), my = (py - best.oy()) / best.scale();
		return mx >= 0 && my >= 0 && mx <= boxWidth && my <= boxHeight ? new double[] { mx, my } : null;
	}

	private static double[] unproject(double[] inv, double nx, double ny, double nz) {
		double[] o = new double[4];
		double[] in = { nx, ny, nz, 1 };
		for (int r = 0; r < 4; r++) {
			o[r] = inv[r] * in[0] + inv[4 + r] * in[1] + inv[8 + r] * in[2] + inv[12 + r] * in[3];
		}
		return Math.abs(o[3]) < 1e-15 ? null : new double[] { o[0] / o[3], o[1] / o[3], o[2] / o[3] };
	}

	/* 4x4 matrices of OpenGL (column-major) */
	private static double[] multiply(double[] a, double[] b) {
		double[] r = new double[16];
		for (int c = 0; c < 4; c++) {
			for (int rr = 0; rr < 4; rr++) {
				double sum = 0;
				for (int k = 0; k < 4; k++) {
					sum += a[k * 4 + rr] * b[c * 4 + k];
				}
				r[c * 4 + rr] = sum;
			}
		}
		return r;
	}

	private static double[] invert(double[] m) {
		double[] a = new double[32];
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 4; c++) {
				a[r * 8 + c] = m[c * 4 + r];
			}
			a[r * 8 + 4 + r] = 1;
		}
		for (int c = 0; c < 4; c++) {
			int piv = c;
			for (int r = c + 1; r < 4; r++) {
				if (Math.abs(a[r * 8 + c]) > Math.abs(a[piv * 8 + c])) {
					piv = r;
				}
			}
			if (Math.abs(a[piv * 8 + c]) < 1e-15) {
				return null;
			}
			for (int k = 0; k < 8; k++) {
				double tmp = a[c * 8 + k];
				a[c * 8 + k] = a[piv * 8 + k];
				a[piv * 8 + k] = tmp;
			}
			double d = a[c * 8 + c];
			for (int k = 0; k < 8; k++) {
				a[c * 8 + k] /= d;
			}
			for (int r = 0; r < 4; r++) {
				if (r != c) {
					double f = a[r * 8 + c];
					for (int k = 0; k < 8; k++) {
						a[r * 8 + k] -= f * a[c * 8 + k];
					}
				}
			}
		}
		double[] inv = new double[16];
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 4; c++) {
				inv[c * 4 + r] = a[r * 8 + 4 + c];
			}
		}
		return inv;
	}

	/* The run method of the 3D view draws the agents (the view may be created later) */
	private static void startDrawing(int tries) {
		if (SpatialStructure.getSpatialStructureInstance() != null) {
			if (!SpatialUtil.isStarted()) {
				SpatialUtil.startStop();
			}
		} else if (tries > 0) {
			PlatformUI.getWorkbench().getDisplay().timerExec(200, () -> startDrawing(tries - 1));
		}
	}

	private double scale() {
		return 1000.0 / Math.max(1e-12, Math.max(boxWidth, Math.max(boxHeight, boxDepth)));
	}

	private void bounds() {
		double s = scale();
		Simulation.ui(() -> SpatialUtil.setDataBounds(-boxWidth * s / 2, boxWidth * s / 2, -boxHeight * s / 2, boxHeight * s / 2, 0, boxDepth * s));
	}

	/*
	 * The agents shown in the 3D view (a snapshot of display()): also the
	 * orientation of the gliders (yaw and pitch of the direction of the last
	 * movement, the bank into a turn, degrees) and the settings of the scene
	 */
	private record Shown(double[] xyz, int[] rgb, String[] images, String[] models, String[] animations, double[] headings, double[] sizes, List<Shape> drawing,
			java.util.Map<String, String> imageFiles, java.util.Map<String, String> modelFiles, double[] yaw, double[] pitch, double[] bank,
			String shape, String floor, boolean shadows, int follow, List<Placed> scenery, int[] links, String[] linkColors, String linkColor, double linkWidth) {
	}

	private volatile Shown shown;
	/* The last position of an agent: x, y, heading, z, the direction dx, dy, dz, the yaw and the bank */
	private final java.util.IdentityHashMap<Agent, double[]> lastPositions = new java.util.IdentityHashMap<>();

	@Override
	protected void display() {
		int n = agents.size();
		double[] xyz = new double[n * 3], headings = new double[n], sizes = new double[n];
		double[] yaw = new double[n], pitch = new double[n], bank = new double[n];
		int[] rgb = new int[n];
		String[] images = new String[n], models = new String[n], animations = new String[n];
		int base = Simulation.rgb(agentColor);
		java.util.IdentityHashMap<Agent, double[]> now = new java.util.IdentityHashMap<>();
		for (int i = 0; i < n; i++) {
			A a = agents.get(i);
			xyz[i * 3] = a.x;
			xyz[i * 3 + 1] = a.y;
			xyz[i * 3 + 2] = a.z;
			rgb[i] = a.color == null ? base : Simulation.rgb(a.color);
			images[i] = a.image;
			models[i] = a.model;
			animations[i] = a.animation;
			sizes[i] = a.size;
			/* The heading: given, else the direction of the last movement */
			double[] before;
			synchronized (lastPositions) {
				before = lastPositions.get(a);
			}
			double h = before == null ? 0 : before[2];
			if (before != null && (before[0] != a.x || before[1] != a.y)) {
				h = headingOf(a.x - before[0], a.y - before[1]);
			}
			headings[i] = Double.isNaN(a.heading) ? h : a.heading;
			/*
			 * The direction in 3D: the last movement (not a jump to the other side of the
			 * box, e.g., wrapInBox), else the direction before
			 */
			double dx = 1, dy = 0, dz = 0, lastYaw = Double.NaN, lastBank = 0;
			if (before != null) {
				dx = before[4];
				dy = before[5];
				dz = before[6];
				lastYaw = before[7];
				lastBank = before[8];
				double mx = a.x - before[0], my = a.y - before[1], mz = a.z - before[3];
				boolean jump = Math.abs(mx) > boxWidth / 2 || Math.abs(my) > boxHeight / 2;
				if (!jump && mx * mx + my * my + mz * mz > 0) {
					dx = mx;
					dy = my;
					dz = mz;
				}
			}
			/* A given heading (0: the direction of y, clockwise) sets the yaw */
			yaw[i] = Double.isNaN(a.heading) ? Math.toDegrees(Math.atan2(dy, dx)) : 90 - a.heading;
			pitch[i] = Math.toDegrees(Math.atan2(dz, Math.hypot(dx, dy)));
			/* The bank: the glider rolls into a turn (smoothed over the steps) */
			if (!Double.isNaN(lastYaw)) {
				double turn = yaw[i] - lastYaw;
				turn -= 360 * Math.rint(turn / 360);
				lastBank = 0.85 * lastBank + 0.15 * Math.max(-50, Math.min(50, -turn * 12));
			}
			bank[i] = lastBank;
			now.put(a, new double[] { a.x, a.y, headings[i], a.z, dx, dy, dz, yaw[i], lastBank });
		}
		synchronized (lastPositions) {
			lastPositions.clear();
			lastPositions.putAll(now);
		}
		shownRgb = rgb;
		shownXyz = xyz;
		int[] pairs = getLinks();
		shown = new Shown(xyz, rgb, images, models, animations, headings, sizes, getDrawing(), getImageFiles(), getModelFiles(), yaw, pitch, bank,
				agentShape, floor, shadows, followAgent, getModels(), pairs, linkColors(pairs), linkColor, linkWidth);
		camera(followAgent);
	}

	/* The custom camera of the 3D view is switched on (following an agent) or off: once for a change */
	private int cameraFollow = -1;

	private void camera(int follow) {
		if ((follow >= 0) == (cameraFollow >= 0) || !Simulation.hasDisplay()) {
			cameraFollow = follow;
			return;
		}
		cameraFollow = follow;
		boolean on = follow >= 0;
		Simulation.ui(() -> {
			if (SpatialStructure.getSpatialStructureInstance() == null) {
				return;
			}
			/* The camera in the space of the walkthrough (it follows the agent with every rotation of the global view) */
			com.eco.bio7.spatial.SpatialCamera.setCustomCameraWalkthroughCoordinates(on);
			SpatialUtil.setSplitView(on);
			SpatialUtil.setCustomCamera(on);
		});
	}

	@Override
	public void run(GL2 gl, GLU glu, GLUT glut) {
		Shown sh = shown;
		double[] xyz = sh != null ? sh.xyz : shownXyz;
		int[] rgb = sh != null ? sh.rgb : shownRgb;
		double s = scale(), ox = -boxWidth * s / 2, oy = -boxHeight * s / 2;
		double r = (agentRadius > 0 ? agentRadius : Math.max(boxWidth, Math.max(boxHeight, boxDepth)) * 0.01) * s;
		/* The matrices of this viewport: clicks and drags on the floor */
		double[] mv = new double[16], pr = new double[16];
		int[] vp = new int[4];
		gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, mv, 0);
		gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, pr, 0);
		gl.glGetIntegerv(GL2.GL_VIEWPORT, vp, 0);
		frames.put(vp[0] + "," + vp[1] + "," + vp[2] + "," + vp[3], new ViewFrame(mv, pr, vp, s, ox, oy, System.nanoTime()));
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glEnable(GL2.GL_NORMALIZE);
		/* The box */
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glColor3f(0.55f, 0.55f, 0.55f);
		double w = boxWidth * s, h = boxHeight * s, d = boxDepth * s;
		gl.glBegin(GL2.GL_LINES);
		double[][] c = { { 0, 0 }, { w, 0 }, { w, h }, { 0, h } };
		for (int k = 0; k < 4; k++) {
			double[] a = c[k], b = c[(k + 1) % 4];
			gl.glVertex3d(ox + a[0], oy + a[1], 0);
			gl.glVertex3d(ox + b[0], oy + b[1], 0);
			gl.glVertex3d(ox + a[0], oy + a[1], d);
			gl.glVertex3d(ox + b[0], oy + b[1], d);
			gl.glVertex3d(ox + a[0], oy + a[1], 0);
			gl.glVertex3d(ox + a[0], oy + a[1], d);
		}
		gl.glEnd();
		/* The floor (a grid or a dark plane) and the soft shadows of the agents */
		if (sh != null) {
			floor(gl, sh, w, h, d, ox, oy, s, r);
		}
		if (sh != null) {
			shapes(gl, glu, glut, sh.drawing, sh.imageFiles, s, ox, oy);
			links(gl, sh, s, ox, oy);
		}
		gl.glEnable(GL2.GL_LIGHTING);
		/* The agents: spheres, gliders, 3D models (glTF) or images */
		int n = Math.min(rgb.length, xyz.length / 3);
		boolean gliders = sh != null && "glider".equals(sh.shape);
		if (gliders) {
			/* Both sides of the wings lit (a glider is open, not a closed body) */
			gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
		}
		/* The objects of the environment (3D models, lit as the agents) */
		double seconds0 = System.nanoTime() / 1e9;
		java.util.Map<String, List<Gltf.Mesh>> sceneryPoses = new java.util.HashMap<>();
		if (sh != null) {
			for (int k = 0; k < sh.scenery.size(); k++) {
				Placed pl = sh.scenery.get(k);
				Gltf model = pl.model() != null ? Assets.model(sh.modelFiles.get(pl.model())) : null;
				if (model == null) {
					continue;
				}
				double size = pl.size() > 0 ? pl.size() * s : 4 * r;
				gl.glPushMatrix();
				gl.glTranslated(ox + pl.x() * s, oy + pl.y() * s, pl.z() * s);
				gl.glRotated(-pl.heading(), 0, 0, 1);
				gl.glScaled(size, size, size);
				/* The animation phase after the agents (another index) */
				ModelGl.meshes(gl, Assets.pose(model, pl.animation(), n + k, seconds0, sceneryPoses));
				gl.glPopMatrix();
			}
		}
		/* The custom camera behind the followed agent (the coordinates of the walkthrough: x, z, -y) */
		if (sh != null && sh.follow >= 0 && sh.follow < n) {
			int i = sh.follow;
			double px = ox + xyz[i * 3] * s, py = oy + xyz[i * 3 + 1] * s, pz = xyz[i * 3 + 2] * s;
			double yr = Math.toRadians(sh.yaw[i]), behind = r * 12, above = r * 3;
			com.eco.bio7.spatial.SpatialCamera.setCustomCamera(px - Math.cos(yr) * behind, pz + above, -(py - Math.sin(yr) * behind), px, pz, -py);
		}
		double seconds = System.nanoTime() / 1e9;
		java.util.Map<String, List<Gltf.Mesh>> frame = new java.util.HashMap<>();
		for (int i = 0; i < n; i++) {
			double px = ox + xyz[i * 3] * s, py = oy + xyz[i * 3 + 1] * s, pz = xyz[i * 3 + 2] * s;
			double size = sh != null && sh.sizes[i] > 0 ? sh.sizes[i] * s : 4 * r;
			Gltf model = sh != null && sh.models[i] != null ? Assets.model(sh.modelFiles.get(sh.models[i])) : null;
			if (model != null) {
				gl.glPushMatrix();
				gl.glTranslated(px, py, pz);
				gl.glRotated(-sh.headings[i], 0, 0, 1);
				gl.glScaled(size, size, size);
				ModelGl.meshes(gl, Assets.pose(model, sh.animations[i], i, seconds, frame));
				gl.glPopMatrix();
				continue;
			}
			if (sh != null && sh.images[i] != null) {
				ModelGl.billboard(gl, Assets.image(sh.imageFiles.get(sh.images[i])), px, py, pz, size, 0);
				continue;
			}
			int col = rgb[i];
			float cr = ((col >> 16) & 255) / 255f, cg = ((col >> 8) & 255) / 255f, cb = (col & 255) / 255f;
			if (gliders) {
				gl.glPushMatrix();
				gl.glTranslated(px, py, pz);
				gl.glRotated(sh.yaw[i], 0, 0, 1);
				gl.glRotated(-sh.pitch[i], 0, 1, 0);
				gl.glRotated(sh.bank[i], 1, 0, 0);
				gl.glScaled(r, r, r);
				glider(gl, cr, cg, cb);
				gl.glPopMatrix();
				continue;
			}
			gl.glColor3f(cr, cg, cb);
			gl.glPushMatrix();
			gl.glTranslated(px, py, pz);
			glut.glutSolidSphere(r, 12, 8);
			gl.glPopMatrix();
		}
		if (gliders) {
			gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_FALSE);
		}
	}

	/*
	 * The glider (as in the example BoidSphereModel): the nose along +x, the wings
	 * in the x-y plane, the fin up (+z), in units of the radius; a dart with a ridge
	 * on the top and the bottom, a notch at the tail and a fin
	 */
	private static final double[] NOSE = { 2.2, 0, 0 }, LEFT = { -1.3, 1.7, 0 }, RIGHT = { -1.3, -1.7, 0 }, TAIL = { -0.7, 0, 0 },
			TOP = { -0.2, 0, 0.28 }, BOTTOM = { -0.2, 0, -0.22 }, FIN = { -1.25, 0, 0.95 };

	private static void glider(GL2 gl, float r, float g, float b) {
		gl.glBegin(GL2.GL_TRIANGLES);
		/* The top (the color of the agent) */
		gl.glColor3f(r, g, b);
		triangle(gl, NOSE, LEFT, TOP);
		triangle(gl, NOSE, TOP, RIGHT);
		triangle(gl, LEFT, TAIL, TOP);
		triangle(gl, TOP, TAIL, RIGHT);
		/* The bottom (darker) */
		gl.glColor3f(r * 0.45f, g * 0.45f, b * 0.55f);
		triangle(gl, NOSE, BOTTOM, LEFT);
		triangle(gl, NOSE, RIGHT, BOTTOM);
		triangle(gl, LEFT, BOTTOM, TAIL);
		triangle(gl, BOTTOM, RIGHT, TAIL);
		/* The fin (light) */
		gl.glColor3f(0.85f + r * 0.15f, 0.9f, 1.0f);
		triangle(gl, TOP, TAIL, FIN);
		gl.glEnd();
	}

	/* A triangle with its normal (for the lighting) */
	private static void triangle(GL2 gl, double[] a, double[] b, double[] c) {
		double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		double wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
		gl.glNormal3d(uy * wz - uz * wy, uz * wx - ux * wz, ux * wy - uy * wx);
		gl.glVertex3d(a[0], a[1], a[2]);
		gl.glVertex3d(b[0], b[1], b[2]);
		gl.glVertex3d(c[0], c[1], c[2]);
	}

	/*
	 * The floor at z = 0: "grid" (lines every tenth of the box) or "plane" (a dark
	 * surface, lighter in the middle, with the grid); the shadows: a soft disc below
	 * each agent (larger and fainter the higher it is)
	 */
	private static void floor(GL2 gl, Shown sh, double w, double h, double d, double ox, double oy, double s, double r) {
		boolean plane = "plane".equals(sh.floor), grid = plane || "grid".equals(sh.floor);
		if (!grid && !sh.shadows) {
			return;
		}
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_COLOR_BUFFER_BIT);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		if (plane) {
			/* A fan of triangles: light in the centre, dark at the edge */
			gl.glBegin(GL2.GL_TRIANGLE_FAN);
			gl.glColor3f(0.20f, 0.25f, 0.32f);
			gl.glVertex3d(ox + w / 2, oy + h / 2, -0.2);
			gl.glColor3f(0.06f, 0.08f, 0.11f);
			gl.glVertex3d(ox, oy, -0.2);
			gl.glVertex3d(ox + w, oy, -0.2);
			gl.glVertex3d(ox + w, oy + h, -0.2);
			gl.glVertex3d(ox, oy + h, -0.2);
			gl.glVertex3d(ox, oy, -0.2);
			gl.glEnd();
		}
		gl.glDepthMask(false);
		if (grid) {
			gl.glLineWidth(1f);
			gl.glBegin(GL2.GL_LINES);
			for (int k = 1; k < 10; k++) {
				float alpha = k == 5 ? 0.32f : 0.14f;
				if (plane) {
					gl.glColor4f(0.55f, 0.70f, 0.90f, alpha);
				} else {
					gl.glColor4f(0.45f, 0.45f, 0.5f, alpha + 0.2f);
				}
				double x = ox + w * k / 10, y = oy + h * k / 10;
				gl.glVertex3d(x, oy, 0.3);
				gl.glVertex3d(x, oy + h, 0.3);
				gl.glVertex3d(ox, y, 0.3);
				gl.glVertex3d(ox + w, y, 0.3);
			}
			gl.glEnd();
		}
		if (sh.shadows) {
			int n = sh.xyz.length / 3;
			for (int i = 0; i < n; i++) {
				double t = Math.max(0, Math.min(1, sh.xyz[i * 3 + 2] * s / Math.max(1e-9, d)));
				double rr = r * (1.4 + 1.6 * t);
				float alpha = (float) ((plane ? 0.5 : 0.35) * (1 - 0.7 * t));
				double x = ox + sh.xyz[i * 3] * s, y = oy + sh.xyz[i * 3 + 1] * s;
				gl.glBegin(GL2.GL_TRIANGLE_FAN);
				gl.glColor4f(0f, 0f, 0f, alpha);
				gl.glVertex3d(x, y, 0.6);
				gl.glColor4f(0f, 0f, 0f, 0f);
				for (int k = 0; k <= 16; k++) {
					double a = k * Math.PI / 8;
					gl.glVertex3d(x + rr * Math.cos(a), y + rr * Math.sin(a), 0.6);
				}
				gl.glEnd();
			}
		}
		gl.glDepthMask(true);
		gl.glPopAttrib();
	}

	/* The links of the network: lines between the agents (the color of a link or linkColor) */
	private static void links(GL2 gl, Shown sh, double s, double ox, double oy) {
		int[] p = sh.links;
		float[] base = Assets.rgba(sh.linkColor);
		if (p.length < 2 || (base == null && java.util.Arrays.stream(sh.linkColors).allMatch(java.util.Objects::isNull))) {
			return;
		}
		int n = sh.xyz.length / 3;
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glLineWidth((float) Math.max(1, sh.linkWidth));
		gl.glBegin(GL2.GL_LINES);
		for (int k = 0; k + 1 < p.length; k += 2) {
			int i = p[k], j = p[k + 1];
			if (i >= n || j >= n) {
				continue;
			}
			float[] c = sh.linkColors[k / 2] != null ? Assets.rgba(sh.linkColors[k / 2]) : base;
			if (c == null) {
				continue;
			}
			gl.glColor4fv(c, 0);
			gl.glVertex3d(ox + sh.xyz[i * 3] * s, oy + sh.xyz[i * 3 + 1] * s, sh.xyz[i * 3 + 2] * s);
			gl.glVertex3d(ox + sh.xyz[j * 3] * s, oy + sh.xyz[j * 3 + 1] * s, sh.xyz[j * 3 + 2] * s);
		}
		gl.glEnd();
		gl.glLineWidth(1);
		gl.glDisable(GL2.GL_BLEND);
	}

	/* The quadric of the pillars */
	private com.jogamp.opengl.glu.GLUquadric quadric;

	/* The shapes: 2D shapes on the floor (or at a height z), lines and paths in 3D, the solids (pillars, boxes, spheres, points, polygons; lit), texts and images (billboards standing at the height) */
	private void shapes(GL2 gl, GLU glu, GLUT glut, List<Shape> drawing, java.util.Map<String, String> imageFiles, double s, double ox, double oy) {
		solids(gl, glu, glut, drawing, s, ox, oy);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		for (Shape sh : drawing) {
			double[] p = sh.p;
			float[] stroke = Assets.rgba(sh.stroke), fill = Assets.rgba(sh.fill);
			/* The height above the floor (a small offset: not hidden by the floor) */
			double z0 = 0.5 + sh.z * s;
			gl.glLineWidth((float) Math.max(1, sh.width));
			switch (sh.kind) {
			case "line3" -> {
				if (stroke != null) {
					gl.glColor4fv(stroke, 0);
					gl.glBegin(GL2.GL_LINES);
					gl.glVertex3d(ox + p[0] * s, oy + p[1] * s, p[2] * s);
					gl.glVertex3d(ox + p[3] * s, oy + p[4] * s, p[5] * s);
					gl.glEnd();
				}
			}
			case "path3" -> {
				if (stroke != null && p.length >= 6) {
					gl.glColor4fv(stroke, 0);
					gl.glBegin(GL2.GL_LINE_STRIP);
					for (int i = 0; i + 2 < p.length; i += 3) {
						gl.glVertex3d(ox + p[i] * s, oy + p[i + 1] * s, p[i + 2] * s);
					}
					gl.glEnd();
				}
			}
			case "polygon3" -> {
				if (stroke != null && p.length >= 9) {
					gl.glColor4fv(stroke, 0);
					gl.glBegin(GL2.GL_LINE_LOOP);
					for (int i = 0; i + 2 < p.length; i += 3) {
						gl.glVertex3d(ox + p[i] * s, oy + p[i + 1] * s, p[i + 2] * s);
					}
					gl.glEnd();
				}
			}
			case "box" -> {
				if (stroke != null && p.length >= 6) {
					gl.glColor4fv(stroke, 0);
					gl.glBegin(GL2.GL_LINES);
					double[][] c = boxCorners(p, s, ox, oy);
					int[] e = { 0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7 };
					for (int k : e) {
						gl.glVertex3d(c[k][0], c[k][1], c[k][2]);
					}
					gl.glEnd();
				}
			}
			case "line", "path", "polygon", "rect", "circle" -> {
				double[] q = outline(sh);
				boolean closed = !sh.kind.equals("line") && !sh.kind.equals("path");
				if (fill != null && closed && q.length >= 6) {
					gl.glColor4fv(fill, 0);
					gl.glBegin(GL2.GL_POLYGON);
					for (int i = 0; i + 1 < q.length; i += 2) {
						gl.glVertex3d(ox + q[i] * s, oy + q[i + 1] * s, z0 - 0.25);
					}
					gl.glEnd();
				}
				if (stroke != null) {
					gl.glColor4fv(stroke, 0);
					gl.glBegin(closed ? GL2.GL_LINE_LOOP : GL2.GL_LINE_STRIP);
					for (int i = 0; i + 1 < q.length; i += 2) {
						gl.glVertex3d(ox + q[i] * s, oy + q[i + 1] * s, z0);
					}
					gl.glEnd();
				}
			}
			case "text" -> {
				if (sh.text != null) {
					float[] col = stroke != null ? stroke : new float[] { 0.1f, 0.1f, 0.1f, 1 };
					gl.glColor4fv(col, 0);
					gl.glRasterPos3d(ox + p[0] * s, oy + p[1] * s, z0);
					glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, sh.text);
				}
			}
			case "image" -> ModelGl.billboard(gl, Assets.image(imageFiles.get(sh.text)), ox + p[0] * s, oy + p[1] * s, (sh.z + sh.width / 2) * s, sh.width * s, sh.angle);
			default -> {
			}
			}
		}
		gl.glLineWidth(1);
		gl.glDisable(GL2.GL_BLEND);
	}

	/* The solids (lit): pillars from the floor, boxes, spheres, points and flat polygons in 3D; a fill with alpha is transparent */
	private void solids(GL2 gl, GLU glu, GLUT glut, List<Shape> drawing, double s, double ox, double oy) {
		boolean any = false;
		for (Shape sh : drawing) {
			double[] p = sh.p;
			float[] fill;
			switch (sh.kind) {
			case "pillar" -> {
				if (p.length < 4) {
					continue;
				}
				fill = Assets.rgba(sh.fill != null ? sh.fill : "#9aa6b8");
			}
			case "sphere", "point3" -> {
				if (p.length < 4) {
					continue;
				}
				fill = Assets.rgba(sh.fill != null ? sh.fill : "#d0d4dc");
			}
			case "box" -> {
				if (p.length < 6 || sh.fill == null) {
					continue;
				}
				fill = Assets.rgba(sh.fill);
			}
			case "polygon3" -> {
				if (p.length < 9 || sh.fill == null) {
					continue;
				}
				fill = Assets.rgba(sh.fill);
			}
			default -> {
				continue;
			}
			}
			if (fill == null) {
				continue;
			}
			if (!any) {
				any = true;
				gl.glEnable(GL2.GL_LIGHTING);
				gl.glEnable(GL2.GL_NORMALIZE);
				/* Both sides of the polygons lit */
				gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
				gl.glEnable(GL2.GL_BLEND);
				gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
				if (quadric == null) {
					quadric = glu.gluNewQuadric();
					glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);
				}
			}
			gl.glColor4f(fill[0], fill[1], fill[2], fill[3]);
			switch (sh.kind) {
			case "pillar" -> {
				gl.glPushMatrix();
				gl.glTranslated(ox + p[0] * s, oy + p[1] * s, 0);
				glu.gluCylinder(quadric, p[2] * s, p[2] * s * 0.85, p[3] * s, 32, 1);
				gl.glTranslated(0, 0, p[3] * s);
				gl.glColor4f(Math.min(1, fill[0] * 1.2f), Math.min(1, fill[1] * 1.2f), Math.min(1, fill[2] * 1.2f), fill[3]);
				glu.gluDisk(quadric, 0, p[2] * s * 0.85, 32, 1);
				gl.glPopMatrix();
			}
			case "sphere", "point3" -> {
				double r = sh.kind.equals("sphere") ? p[3] : p[3] / 2;
				gl.glPushMatrix();
				gl.glTranslated(ox + p[0] * s, oy + p[1] * s, p[2] * s);
				glu.gluSphere(quadric, Math.max(1e-6, r * s), sh.kind.equals("sphere") ? 32 : 12, sh.kind.equals("sphere") ? 20 : 8);
				gl.glPopMatrix();
			}
			case "box" -> {
				double[][] c = boxCorners(p, s, ox, oy);
				/* The faces (corners 0-3 below, 4-7 above) with their normals */
				int[][] f = { { 0, 3, 2, 1 }, { 4, 5, 6, 7 }, { 0, 1, 5, 4 }, { 1, 2, 6, 5 }, { 2, 3, 7, 6 }, { 3, 0, 4, 7 } };
				double[][] n = { { 0, 0, -1 }, { 0, 0, 1 }, { 0, -1, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { -1, 0, 0 } };
				gl.glBegin(GL2.GL_QUADS);
				for (int k = 0; k < 6; k++) {
					gl.glNormal3d(n[k][0], n[k][1], n[k][2]);
					for (int i : f[k]) {
						gl.glVertex3d(c[i][0], c[i][1], c[i][2]);
					}
				}
				gl.glEnd();
			}
			case "polygon3" -> {
				double[] nn = newell(p);
				gl.glBegin(GL2.GL_POLYGON);
				gl.glNormal3d(nn[0], nn[1], nn[2]);
				for (int i = 0; i + 2 < p.length; i += 3) {
					gl.glVertex3d(ox + p[i] * s, oy + p[i + 1] * s, p[i + 2] * s);
				}
				gl.glEnd();
			}
			default -> {
			}
			}
		}
		if (any) {
			gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_FALSE);
			gl.glDisable(GL2.GL_BLEND);
			gl.glDisable(GL2.GL_NORMALIZE);
			gl.glDisable(GL2.GL_LIGHTING);
		}
	}

	/* The 8 corners of a box (x, y, z, w, h, d of the model) in the coordinates of OpenGL: 0-3 below, 4-7 above */
	private static double[][] boxCorners(double[] p, double s, double ox, double oy) {
		double x0 = ox + p[0] * s, y0 = oy + p[1] * s, z0 = p[2] * s, x1 = x0 + p[3] * s, y1 = y0 + p[4] * s, z1 = z0 + p[5] * s;
		return new double[][] { { x0, y0, z0 }, { x1, y0, z0 }, { x1, y1, z0 }, { x0, y1, z0 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 }, { x0, y1, z1 } };
	}

	/* The normal of a polygon in 3D (x0, y0, z0, x1, ...; the method of Newell) */
	static double[] newell(double[] p) {
		double nx = 0, ny = 0, nz = 0;
		int n = p.length / 3;
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			nx += (p[i * 3 + 1] - p[j * 3 + 1]) * (p[i * 3 + 2] + p[j * 3 + 2]);
			ny += (p[i * 3 + 2] - p[j * 3 + 2]) * (p[i * 3] + p[j * 3]);
			nz += (p[i * 3] - p[j * 3]) * (p[i * 3 + 1] + p[j * 3 + 1]);
		}
		double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
		return l == 0 ? new double[] { 0, 0, 1 } : new double[] { nx / l, ny / l, nz / l };
	}

	/* The points of the outline of a 2D shape (x0, y0, x1, y1, ...) */
	static double[] outline(Shape sh) {
		double[] p = sh.p;
		return switch (sh.kind) {
		case "rect" -> new double[] { p[0], p[1], p[0] + p[2], p[1], p[0] + p[2], p[1] + p[3], p[0], p[1] + p[3] };
		case "circle" -> {
			double[] q = new double[96];
			for (int i = 0; i < 48; i++) {
				double a = 2 * Math.PI * i / 48;
				q[i * 2] = p[0] + p[2] * Math.cos(a);
				q[i * 2 + 1] = p[1] + p[2] * Math.sin(a);
			}
			yield q;
		}
		default -> p;
		};
	}
}
