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

import java.awt.Font;
import java.util.Arrays;
import java.util.Comparator;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * An axis orientation overlay (like in Blender) in the top right corner of the
 * 3D view: the x, y and z axes of the scene rotated like the camera, a filled
 * circle with the name for the positive and a circle for the negative
 * direction of every axis.
 */
public class AxisGizmo {

	private static final float[][] COLORS = { { 0.90f, 0.25f, 0.33f }, { 0.53f, 0.80f, 0.20f },
			{ 0.25f, 0.55f, 0.90f } };
	private static final String[] NAMES = { "X", "Y", "Z" };
	/* The size and the margin in points */
	private static final int SIZE = 110;
	private static final int MARGIN = 6;
	private static final float AXIS_LENGTH = 0.62f;
	private static final float RADIUS = 0.16f;

	/* The rotation of the scene (columns: the axes in eye coordinates) */
	private final float[] axes = new float[9];
	private boolean valid;
	private TextRenderer renderer;
	private GLContext rendererContext;
	private int rendererFontSize;

	/**
	 * Stores the rotation of the current modelview matrix (call it with the
	 * transformation of the scene).
	 */
	public void capture(GL2 gl) {
		float[] m = new float[16];
		gl.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, m, 0);
		for (int k = 0; k < 3; k++) {
			float x = m[k * 4], y = m[k * 4 + 1], z = m[k * 4 + 2];
			float l = (float) Math.sqrt(x * x + y * y + z * z);
			if (l == 0) {
				return;
			}
			axes[k * 3] = x / l;
			axes[k * 3 + 1] = y / l;
			axes[k * 3 + 2] = z / l;
		}
		valid = true;
	}

	/**
	 * Draws the overlay in the top right corner.
	 *
	 * @param width      the width of the view in pixels
	 * @param height     the height of the view in pixels
	 * @param pixelScale the pixels per point (HiDPI)
	 */
	public void render(GL2 gl, int width, int height, float pixelScale) {
		if (!valid) {
			return;
		}
		int size = Math.round(SIZE * pixelScale);
		int margin = Math.round(MARGIN * pixelScale);
		if (width < size * 2 || height < size * 2) {
			return;
		}
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_VIEWPORT_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT
				| GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_SCISSOR_BIT);
		gl.glViewport(width - size - margin, height - size - margin, size, size);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glDisable(GL.GL_CULL_FACE);
		gl.glDisable(GL.GL_SCISSOR_TEST);
		for (int i = 0; i < 6; i++) {
			gl.glDisable(GL2.GL_CLIP_PLANE0 + i);
		}
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glColorMask(true, true, true, true);
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(-1, 1, -1, 1, -1, 1);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		/* The six directions, drawn from the back to the front */
		Integer[] order = { 0, 1, 2, 3, 4, 5 };
		Arrays.sort(order, Comparator.comparingDouble(d -> direction(d)[2]));
		TextRenderer text = textRenderer(32);
		gl.glLineWidth(2 * pixelScale);
		for (int d : order) {
			int axis = d % 3;
			boolean positive = d < 3;
			float[] p = direction(d);
			float x = p[0] * AXIS_LENGTH, y = p[1] * AXIS_LENGTH;
			float[] c = COLORS[axis];
			if (positive) {
				gl.glColor4f(c[0], c[1], c[2], 1);
				gl.glBegin(GL.GL_LINES);
				gl.glVertex2f(0, 0);
				gl.glVertex2f(x, y);
				gl.glEnd();
				circle(gl, x, y, GL.GL_TRIANGLE_FAN);
				/* The letter fitted into the circle (independent of the font scaling) */
				java.awt.geom.Rectangle2D bounds = text.getBounds("X");
				float scale = (float) (RADIUS * 1.1f / Math.max(1, bounds.getHeight() * 0.75));
				java.awt.geom.Rectangle2D letter = text.getBounds(NAMES[axis]);
				text.begin3DRendering();
				text.setColor(0.1f, 0.1f, 0.1f, 1);
				text.draw3D(NAMES[axis], (float) (x - letter.getWidth() * scale / 2),
						(float) (y - bounds.getHeight() * 0.75 * scale / 2), 0, scale);
				text.end3DRendering();
				/* The text renderer changes the state */
				gl.glDisable(GL.GL_DEPTH_TEST);
				gl.glDisable(GL.GL_TEXTURE_2D);
				gl.glEnable(GL.GL_BLEND);
			} else {
				gl.glColor4f(c[0], c[1], c[2], 0.25f);
				circle(gl, x, y, GL.GL_TRIANGLE_FAN);
				gl.glColor4f(c[0], c[1], c[2], 0.9f);
				circle(gl, x, y, GL.GL_LINE_LOOP);
			}
		}

		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	/* The direction (0 - 2: +x, +y, +z, 3 - 5: -x, -y, -z) in eye coordinates */
	private float[] direction(int d) {
		int axis = d % 3;
		float sign = d < 3 ? 1 : -1;
		return new float[] { axes[axis * 3] * sign, axes[axis * 3 + 1] * sign, axes[axis * 3 + 2] * sign };
	}

	private static void circle(GL2 gl, float x, float y, int mode) {
		gl.glBegin(mode);
		if (mode == GL.GL_TRIANGLE_FAN) {
			gl.glVertex2f(x, y);
		}
		int segments = 24;
		for (int i = 0; i <= segments; i++) {
			double a = 2 * Math.PI * i / segments;
			gl.glVertex2f(x + RADIUS * (float) Math.cos(a), y + RADIUS * (float) Math.sin(a));
		}
		gl.glEnd();
	}

	/* A text renderer of the current context and the font size */
	private TextRenderer textRenderer(int fontSize) {
		GLContext context = GLContext.getCurrent();
		if (renderer == null || rendererContext != context || rendererFontSize != fontSize) {
			if (renderer != null && rendererContext == context) {
				renderer.dispose();
			}
			renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, fontSize), true, false);
			rendererContext = context;
			rendererFontSize = fontSize;
		}
		return renderer;
	}
}
