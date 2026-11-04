/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.spatial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * The geometry of a 3D plot (see Plot3D) in the coordinates of the plot box:
 * triangles with normals, lines, points (drawn as spheres) and labels, each
 * with a color (RGBA). The same geometry is drawn in the 3D view (OpenGL) and
 * exported to WebGL (see Plot3D.exportHtml).
 */
final class PlotGeometry {

	/* x, y, z, nx, ny, nz, r, g, b, a */
	final FloatList triangles = new FloatList();
	/* x, y, z, r, g, b, a (two vertices for each line) */
	final FloatList lines = new FloatList();
	/* x, y, z, r, g, b, a, radius */
	final FloatList points = new FloatList();
	final List<Label> labels = new ArrayList<>();

	/** A text at a position (facing the camera). */
	record Label(float x, float y, float z, String text, float size) {
	}

	/* The display list of the 3D view and the context of the list */
	private int list = -1;
	private Object listContext;

	/* Appends the geometry of another plot geometry (e.g., of a layer) */
	void append(PlotGeometry other) {
		triangles.add(other.triangles.toArray());
		lines.add(other.lines.toArray());
		points.add(other.points.toArray());
		labels.addAll(other.labels);
	}

	static float[] rgba(int rgb, float alpha) {
		return new float[] { ((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f, alpha };
	}

	/* A triangle (the normal from the vertices if n is null) */
	void triangle(double[] a, double[] b, double[] c, double[] n, float[] color) {
		if (n == null) {
			n = normal(a, b, c);
		}
		vertex(a, n, color);
		vertex(b, n, color);
		vertex(c, n, color);
	}

	/* A triangle with a normal and a color for each vertex (smooth shading) */
	void triangle(double[] a, double[] na, float[] ca, double[] b, double[] nb, float[] cb, double[] c, double[] nc, float[] cc) {
		vertex(a, na, ca);
		vertex(b, nb, cb);
		vertex(c, nc, cc);
	}

	/* A quad (a, b, c, d counterclockwise) as two triangles */
	void quad(double[] a, double[] b, double[] c, double[] d, float[] color) {
		double[] n = normal(a, b, c);
		triangle(a, b, c, n, color);
		triangle(a, c, d, n, color);
	}

	private void vertex(double[] p, double[] n, float[] c) {
		triangles.add((float) p[0], (float) p[1], (float) p[2], (float) n[0], (float) n[1], (float) n[2], c[0], c[1], c[2], c[3]);
	}

	void line(double[] a, double[] b, float[] color) {
		line(a, color, b, color);
	}

	void line(double[] a, float[] ca, double[] b, float[] cb) {
		lines.add((float) a[0], (float) a[1], (float) a[2], ca[0], ca[1], ca[2], ca[3]);
		lines.add((float) b[0], (float) b[1], (float) b[2], cb[0], cb[1], cb[2], cb[3]);
	}

	void point(double[] p, float[] color, double radius) {
		points.add((float) p[0], (float) p[1], (float) p[2], color[0], color[1], color[2], color[3], (float) radius);
	}

	void label(double[] p, String text, float size) {
		labels.add(new Label((float) p[0], (float) p[1], (float) p[2], text, size));
	}

	static double[] normal(double[] a, double[] b, double[] c) {
		double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
		double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
		double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
		return length > 0 ? new double[] { nx / length, ny / length, nz / length } : new double[] { 0, 0, 1 };
	}

	/*
	 * Draws the geometry in the 3D view: a display list (created again in a new
	 * context) with the opaque triangles, the lines, the points (spheres) and then
	 * the transparent triangles.
	 */
	void draw(GL2 gl, GLUT glut) {
		Object context = gl.getContext();
		if (list == -1 || listContext != context || !gl.glIsList(list)) {
			list = gl.glGenLists(1);
			listContext = context;
			gl.glNewList(list, GL2.GL_COMPILE);
			compile(gl, glut);
			gl.glEndList();
		}
		gl.glCallList(list);
	}

	/* Deletes the display list (in the OpenGL thread, e.g., if the plot was updated) */
	void dispose(GL2 gl) {
		if (list != -1 && listContext == gl.getContext() && gl.glIsList(list)) {
			gl.glDeleteLists(list, 1);
		}
		list = -1;
		listContext = null;
	}

		private void compile(GL2 gl, GLUT glut) {
		gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
		gl.glEnable(GLLightingFunc.GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_AMBIENT_AND_DIFFUSE);
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
		gl.glEnable(GLLightingFunc.GL_NORMALIZE);
		gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
		gl.glDisable(GL.GL_CULL_FACE);
		gl.glDisable(GL.GL_TEXTURE_2D);
		/* The opaque triangles (with an offset that the lines on surfaces are visible) */
		gl.glEnable(GLLightingFunc.GL_LIGHTING);
		gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(1, 1);
		drawTriangles(gl, false);
		/* The points as spheres */
		float[] p = points.toArray();
		for (int i = 0; i < p.length; i += 8) {
			gl.glColor4f(p[i + 3], p[i + 4], p[i + 5], p[i + 6]);
			gl.glPushMatrix();
			gl.glTranslatef(p[i], p[i + 1], p[i + 2]);
			glut.glutSolidSphere(p[i + 7], 12, 9);
			gl.glPopMatrix();
		}
		/* The lines */
		gl.glDisable(GLLightingFunc.GL_LIGHTING);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glLineWidth(2.5f);
		float[] l = lines.toArray();
		gl.glBegin(GL.GL_LINES);
		for (int i = 0; i < l.length; i += 7) {
			gl.glColor4f(l[i + 3], l[i + 4], l[i + 5], l[i + 6]);
			gl.glVertex3f(l[i], l[i + 1], l[i + 2]);
		}
		gl.glEnd();
		/* The transparent triangles (after the opaque objects, without depth writes) */
		gl.glEnable(GLLightingFunc.GL_LIGHTING);
		gl.glDepthMask(false);
		drawTriangles(gl, true);
		gl.glDepthMask(true);
		gl.glPopAttrib();
	}

	private void drawTriangles(GL2 gl, boolean transparent) {
		float[] t = triangles.toArray();
		gl.glBegin(GL.GL_TRIANGLES);
		for (int i = 0; i < t.length; i += 30) {
			if ((t[i + 9] < 1) != transparent) {
				continue;
			}
			for (int v = i; v < i + 30; v += 10) {
				gl.glNormal3f(t[v + 3], t[v + 4], t[v + 5]);
				gl.glColor4f(t[v + 6], t[v + 7], t[v + 8], t[v + 9]);
				gl.glVertex3f(t[v], t[v + 1], t[v + 2]);
			}
		}
		gl.glEnd();
	}

	/* A growing list of floats */
	static final class FloatList {
		private float[] data = new float[1024];
		private int size;

		void add(float... values) {
			if (size + values.length > data.length) {
				data = Arrays.copyOf(data, Math.max(data.length * 2, size + values.length));
			}
			System.arraycopy(values, 0, data, size, values.length);
			size += values.length;
		}

		int size() {
			return size;
		}

		float[] toArray() {
			return Arrays.copyOf(data, size);
		}
	}
}
