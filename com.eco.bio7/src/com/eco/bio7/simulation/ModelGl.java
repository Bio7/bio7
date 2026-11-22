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

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * Draws the glTF models and the images (billboards) of the agents with OpenGL
 * (GL2: the 3D view of Bio7 and the WorldWind globe).
 */
final class ModelGl {

	/* The textures of a GL context (the images of the models and the billboards) */
	private static final Map<GLContext, Map<Object, Texture>> TEXTURES = new WeakHashMap<>();

	private ModelGl() {
	}

	/* The texture of an image (created once per GL context; null if it can't be created) */
	private static Texture texture(GL2 gl, Object key, java.util.function.Supplier<BufferedImage> image) {
		Map<Object, Texture> m = TEXTURES.computeIfAbsent(gl.getContext(), k -> new java.util.IdentityHashMap<>());
		if (m.containsKey(key)) {
			return m.get(key);
		}
		Texture t = null;
		try {
			BufferedImage img = image.get();
			if (img != null) {
				t = AWTTextureIO.newTexture(gl.getGLProfile(), img, true);
				t.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
				t.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
			}
		} catch (RuntimeException e) {
			System.err.println("A texture could not be created: " + e.getMessage());
		}
		m.put(key, t);
		return t;
	}

	/**
	 * Draws the meshes of a pose in the current coordinates (the model: base at
	 * z = 0, height 1, front +y). Lighting and the color material must be
	 * enabled by the caller.
	 */
	static void meshes(GL2 gl, List<Gltf.Mesh> meshes) {
		gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
		for (Gltf.Mesh m : meshes) {
			/* Reused buffers (no new native memory in each frame) */
			FloatBuffer pos = floats(0, m.positions), nor = floats(1, m.normals);
			IntBuffer idx = ints(m.indices);
			Texture tex = m.texture == null || m.uvs == null ? null : texture(gl, m.texture, () -> {
				try {
					return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(m.texture));
				} catch (java.io.IOException e) {
					return null;
				}
			});
			if (tex != null) {
				/* glTF: v = 0 is the top row; the texture is stored with the top row first */
				float[] uv = m.uvs.clone();
				if (!tex.getMustFlipVertically()) {
					for (int i = 1; i < uv.length; i += 2) {
						uv[i] = 1 - uv[i];
					}
				}
				gl.glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
				gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, floats(2, uv));
				tex.enable(gl);
				tex.bind(gl);
				gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
			}
			gl.glColor4f(m.color[0], m.color[1], m.color[2], m.color[3]);
			gl.glVertexPointer(3, GL.GL_FLOAT, 0, pos);
			gl.glNormalPointer(GL.GL_FLOAT, 0, nor);
			gl.glDrawElements(GL.GL_TRIANGLES, m.indices.length, GL.GL_UNSIGNED_INT, idx);
			if (tex != null) {
				tex.disable(gl);
				gl.glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
			}
		}
		gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
	}

	/**
	 * Draws an image as a billboard (facing the camera) centered at x, y, z of the
	 * current coordinates: the size (the larger side) and the angle in degrees
	 * (clockwise in the view).
	 */
	static void billboard(GL2 gl, BufferedImage img, double x, double y, double z, double size, double angle) {
		if (img == null) {
			return;
		}
		Texture tex = texture(gl, img, () -> img);
		if (tex == null) {
			return;
		}
		double[] mv = new double[16];
		gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, mv, 0);
		/* The directions of the camera in the current coordinates (the rows of the rotation) */
		double[] r = norm(mv[0], mv[4], mv[8]), u = norm(mv[1], mv[5], mv[9]);
		double a = Math.toRadians(-angle), ca = Math.cos(a), sa = Math.sin(a);
		double[] rr = { r[0] * ca + u[0] * sa, r[1] * ca + u[1] * sa, r[2] * ca + u[2] * sa };
		double[] uu = { -r[0] * sa + u[0] * ca, -r[1] * sa + u[1] * ca, -r[2] * sa + u[2] * ca };
		double w = size / 2 * img.getWidth() / Math.max(img.getWidth(), img.getHeight()), h = size / 2 * img.getHeight() / Math.max(img.getWidth(), img.getHeight());
		boolean flip = tex.getMustFlipVertically();
		float top = flip ? 0 : 1, bottom = flip ? 1 : 0;
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_COLOR_BUFFER_BIT);
		gl.glDisable(GLLightingFunc.GL_LIGHTING);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_ALPHA_TEST);
		gl.glAlphaFunc(GL.GL_GREATER, 0.1f);
		tex.enable(gl);
		tex.bind(gl);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
		gl.glColor4f(1, 1, 1, 1);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, bottom);
		gl.glVertex3d(x - rr[0] * w - uu[0] * h, y - rr[1] * w - uu[1] * h, z - rr[2] * w - uu[2] * h);
		gl.glTexCoord2f(1, bottom);
		gl.glVertex3d(x + rr[0] * w - uu[0] * h, y + rr[1] * w - uu[1] * h, z + rr[2] * w - uu[2] * h);
		gl.glTexCoord2f(1, top);
		gl.glVertex3d(x + rr[0] * w + uu[0] * h, y + rr[1] * w + uu[1] * h, z + rr[2] * w + uu[2] * h);
		gl.glTexCoord2f(0, top);
		gl.glVertex3d(x - rr[0] * w + uu[0] * h, y - rr[1] * w + uu[1] * h, z - rr[2] * w + uu[2] * h);
		gl.glEnd();
		tex.disable(gl);
		gl.glPopAttrib();
	}

	/* The buffers of the meshes per thread of OpenGL (the 3D view and WorldWind draw in their own threads; the data is read by the draw call) */
	private static final class Scratch {
		final FloatBuffer[] floats = new FloatBuffer[3];
		IntBuffer ints;
	}

	private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

	private static FloatBuffer floats(int slot, float[] data) {
		Scratch sc = SCRATCH.get();
		FloatBuffer b = sc.floats[slot];
		if (b == null || b.capacity() < data.length) {
			b = Buffers.newDirectFloatBuffer(Math.max(data.length, b == null ? 0 : b.capacity() * 2));
			sc.floats[slot] = b;
		}
		b.clear();
		b.put(data);
		b.flip();
		return b;
	}

	private static IntBuffer ints(int[] data) {
		Scratch sc = SCRATCH.get();
		IntBuffer b = sc.ints;
		if (b == null || b.capacity() < data.length) {
			b = Buffers.newDirectIntBuffer(Math.max(data.length, b == null ? 0 : b.capacity() * 2));
			sc.ints = b;
		}
		b.clear();
		b.put(data);
		b.flip();
		return b;
	}

	private static double[] norm(double x, double y, double z) {
		double l = Math.sqrt(x * x + y * y + z * z);
		return l == 0 ? new double[] { x, y, z } : new double[] { x / l, y / l, z / l };
	}
}
