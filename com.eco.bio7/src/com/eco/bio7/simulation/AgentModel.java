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

	/* The agents shown in the 3D view (a snapshot of display()) */
	private record Shown(double[] xyz, int[] rgb, String[] images, String[] models, String[] animations, double[] headings, double[] sizes, List<Shape> drawing,
			java.util.Map<String, String> imageFiles, java.util.Map<String, String> modelFiles) {
	}

	private volatile Shown shown;
	private final java.util.IdentityHashMap<Agent, double[]> lastPositions = new java.util.IdentityHashMap<>();

	@Override
	protected void display() {
		int n = agents.size();
		double[] xyz = new double[n * 3], headings = new double[n], sizes = new double[n];
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
			now.put(a, new double[] { a.x, a.y, headings[i] });
		}
		synchronized (lastPositions) {
			lastPositions.clear();
			lastPositions.putAll(now);
		}
		shownRgb = rgb;
		shownXyz = xyz;
		shown = new Shown(xyz, rgb, images, models, animations, headings, sizes, getDrawing(), getImageFiles(), getModelFiles());
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
		if (sh != null) {
			shapes(gl, glut, sh.drawing, sh.imageFiles, s, ox, oy);
		}
		gl.glEnable(GL2.GL_LIGHTING);
		/* The agents: spheres, 3D models (glTF) or images */
		int n = Math.min(rgb.length, xyz.length / 3);
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
			gl.glColor3f(((col >> 16) & 255) / 255f, ((col >> 8) & 255) / 255f, (col & 255) / 255f);
			gl.glPushMatrix();
			gl.glTranslated(px, py, pz);
			glut.glutSolidSphere(r, 12, 8);
			gl.glPopMatrix();
		}
	}

	/* The shapes: 2D shapes on the floor, lines in 3D, texts and images (billboards standing on the floor) */
	private static void shapes(GL2 gl, GLUT glut, List<Shape> drawing, java.util.Map<String, String> imageFiles, double s, double ox, double oy) {
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
