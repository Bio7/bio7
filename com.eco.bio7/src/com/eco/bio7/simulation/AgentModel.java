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
		boxWidth = width;
		boxHeight = height;
		boxDepth = depth;
		bounds();
	}

	/** The distance of two agents (3D). */
	public double distance(Agent a, Agent b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y) + (a.z - b.z) * (a.z - b.z));
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
		/* The environment is built again by init() */
		clearModels();
		if (!Simulation.hasDisplay()) {
			return;
		}
		Work.openView("com.eco.bio7.spatial");
		bounds();
		startDrawing(10);
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
			String shape, String floor, boolean shadows, int follow, List<Placed> scenery) {
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
		shown = new Shown(xyz, rgb, images, models, animations, headings, sizes, getDrawing(), getImageFiles(), getModelFiles(), yaw, pitch, bank,
				agentShape, floor, shadows, followAgent, getModels());
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

	/* The quadric of the pillars */
	private com.jogamp.opengl.glu.GLUquadric quadric;

	/* The shapes: 2D shapes on the floor, lines in 3D, pillars, texts and images (billboards standing on the floor) */
	private void shapes(GL2 gl, GLU glu, GLUT glut, List<Shape> drawing, java.util.Map<String, String> imageFiles, double s, double ox, double oy) {
		/* The pillars (lit, from the floor): x, y, the radius and the height */
		for (Shape sh : drawing) {
			if (!"pillar".equals(sh.kind) || sh.p.length < 4) {
				continue;
			}
			if (quadric == null) {
				quadric = glu.gluNewQuadric();
				glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);
			}
			float[] fill = Assets.rgba(sh.fill != null ? sh.fill : "#9aa6b8");
			double[] p = sh.p;
			gl.glEnable(GL2.GL_LIGHTING);
			gl.glPushMatrix();
			gl.glTranslated(ox + p[0] * s, oy + p[1] * s, 0);
			gl.glColor3f(fill[0], fill[1], fill[2]);
			glu.gluCylinder(quadric, p[2] * s, p[2] * s * 0.85, p[3] * s, 32, 1);
			gl.glTranslated(0, 0, p[3] * s);
			gl.glColor3f(Math.min(1, fill[0] * 1.2f), Math.min(1, fill[1] * 1.2f), Math.min(1, fill[2] * 1.2f));
			glu.gluDisk(quadric, 0, p[2] * s * 0.85, 32, 1);
			gl.glPopMatrix();
			gl.glDisable(GL2.GL_LIGHTING);
		}
		double z0 = 0.5;
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		for (Shape sh : drawing) {
			double[] p = sh.p;
			float[] stroke = Assets.rgba(sh.stroke), fill = Assets.rgba(sh.fill);
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
			case "line", "path", "polygon", "rect", "circle" -> {
				double[] q = outline(sh);
				boolean closed = !sh.kind.equals("line") && !sh.kind.equals("path");
				if (fill != null && closed && q.length >= 6) {
					gl.glColor4fv(fill, 0);
					gl.glBegin(GL2.GL_POLYGON);
					for (int i = 0; i + 1 < q.length; i += 2) {
						gl.glVertex3d(ox + q[i] * s, oy + q[i + 1] * s, z0 * 0.5);
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
			case "image" -> ModelGl.billboard(gl, Assets.image(imageFiles.get(sh.text)), ox + p[0] * s, oy + p[1] * s, sh.width * s / 2, sh.width * s, sh.angle);
			default -> {
			}
			}
		}
		gl.glLineWidth(1);
		gl.glDisable(GL2.GL_BLEND);
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
