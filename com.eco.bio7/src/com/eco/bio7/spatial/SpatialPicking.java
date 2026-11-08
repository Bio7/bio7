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

import java.nio.IntBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;

/**
 * Picks the object under the mouse in the 3D view (the selection mode of
 * OpenGL): the objects are drawn between startPicking and endPicking with a
 * name (glPushName / glPopName), getSelection returns the name of the nearest
 * object at the mouse position.
 *
 * <pre>
 * pick.startPicking(gl, glu, getPressEvent());
 * gl.glPushName(100);
 * glut.glutSolidSphere(20, 10, 10);
 * gl.glPopName();
 * pick.endPicking(gl);
 * int id = pick.getSelection(); // 100 or -1
 * </pre>
 */
public class SpatialPicking {

	/* The number of values of the hit records (four values per hit with one name) */
	private final int bufferSize;
	/* The size of the picking area around the mouse (pixels) */
	private final double pickSize;
	private IntBuffer buffer;
	private int selection = -1;

	/** Creates a picking with a buffer of 1000 values and a picking area of 5 pixels. */
	public SpatialPicking() {
		this(1000, 5);
	}

	/**
	 * Creates a picking.
	 *
	 * @param buffSize the size of the buffer of the hits (values, four for a hit
	 *                 with one name)
	 * @param pickArea the size of the picking area around the mouse (pixels)
	 */
	public SpatialPicking(int buffSize, int pickArea) {
		this.bufferSize = Math.max(16, buffSize);
		this.pickSize = Math.max(1, pickArea);
	}

	/**
	 * Starts the picking at the position of a mouse event: the following objects
	 * are not drawn but tested at the mouse position (until endPicking).
	 *
	 * @param gl  the GL context
	 * @param glu the GLU context
	 * @param evt the mouse event (pixels of the 3D view from the top left)
	 */
	public void startPicking(GL2 gl, GLU glu, MouseEvent evt) {
		/* The projection and the viewport of the current view (the picking uses the same projection) */
		double[] projection = new double[16];
		gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, projection, 0);
		int[] viewport = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		buffer = Buffers.newDirectIntBuffer(bufferSize);
		gl.glSelectBuffer(bufferSize, buffer);
		gl.glRenderMode(GL2.GL_SELECT);
		gl.glInitNames();
		/* Only a small area around the mouse (the window y axis points down, OpenGL up) */
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		double x = evt.getX();
		double y = viewport[1] + viewport[3] - evt.getY();
		glu.gluPickMatrix(x, y, pickSize, pickSize, viewport, 0);
		gl.glMultMatrixd(projection, 0);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
	}

	/**
	 * Ends the picking: the drawing is normal again, the name of the nearest
	 * object at the mouse position is the selection (-1 if no object was hit).
	 *
	 * @param gl the GL context
	 */
	public void endPicking(GL2 gl) {
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		int hits = gl.glRenderMode(GL2.GL_RENDER);
		selection = nearest(hits);
	}

	/*
	 * The name of the nearest hit (the top of its name stack): a hit record is
	 * {count of names, min depth, max depth, names...}, the depths are unsigned
	 * integers (0 - 2^32 - 1). A negative number of hits: the buffer was too small.
	 */
	private int nearest(int hits) {
		if (buffer == null || hits <= 0) {
			return -1;
		}
		int name = -1;
		long nearest = Long.MAX_VALUE;
		int offset = 0;
		int limit = buffer.capacity();
		for (int i = 0; i < hits && offset + 3 <= limit; i++) {
			int names = buffer.get(offset);
			long minDepth = buffer.get(offset + 1) & 0xFFFFFFFFL;
			int first = offset + 3;
			if (names > 0 && first + names <= limit && minDepth < nearest) {
				nearest = minDepth;
				name = buffer.get(first + names - 1);
			}
			offset = first + Math.max(0, names);
		}
		return name;
	}

	/**
	 * Returns the name of the nearest object at the mouse position of the last
	 * picking.
	 *
	 * @return the name (see glPushName) or -1 if no object was hit
	 */
	public int getSelection() {
		return selection;
	}
}
