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

import java.awt.image.ColorModel;
import java.nio.ByteBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Renders an ImageJ stack as a volume with a 3D texture. The volume is drawn
 * with slices along the axis which is most aligned with the view direction
 * (back to front). The color of a voxel is the color of the LUT (or gray) and
 * the alpha value the intensity (scaled to the display range of the image).
 *
 * Opacity and threshold are applied while rendering. The colors, the size of
 * the texture and the stack are applied when a stack is loaded!
 */
public class VolumeRenderer {

	public static final int MODE_ALPHA_BLENDING = 0;
	public static final int MODE_MAXIMUM_INTENSITY = 1;

	private static int max3dTextureSize = 0;

	private ByteBuffer voxels; // RGBA, alpha = intensity
	private int texWidth;
	private int texHeight;
	private int texDepth;
	private float width; // the size in pixels of the image
	private float height;
	private float depth; // the number of slices multiplied with the calibrated voxel depth
	private int textureId = 0;
	private boolean upload = false;
	private ImagePlus image;

	private boolean show = true;
	private int mode = MODE_ALPHA_BLENDING;
	private float opacity = 0.05f;
	private float threshold = 0.04f;
	private float sampling = 1.0f;
	private float zScale = 1.0f;
	private int maxSize = 256;
	private boolean lutColors = true;

	/**
	 * Loads a stack (the current channel and frame of a hyperstack) as a volume.
	 * Must be called again if the stack, the LUT or the display range have been
	 * changed!
	 *
	 * @param imp the ImageJ image
	 * @return true if the stack has been loaded
	 */
	public synchronized boolean load(ImagePlus imp) {
		if (imp == null || imp.getProcessor() == null) {
			return false;
		}
		int w = imp.getWidth();
		int h = imp.getHeight();
		int d = imp.getNSlices();
		/* Reduce the resolution of large stacks (memory and graphics card limit)! */
		int limit = maxSize;
		if (max3dTextureSize > 0) {
			limit = Math.min(limit, max3dTextureSize);
		}
		int stepX = (w + limit - 1) / limit;
		int stepY = (h + limit - 1) / limit;
		int stepZ = (d + limit - 1) / limit;
		int tw = (w + stepX - 1) / stepX;
		int th = (h + stepY - 1) / stepY;
		int td = (d + stepZ - 1) / stepZ;

		ImageStack stack = imp.getStack();
		int channel = imp.getChannel();
		int frame = imp.getFrame();
		double min = imp.getDisplayRangeMin();
		double max = imp.getDisplayRangeMax();
		double range = max > min ? max - min : 1;
		int[] lut = createLut(imp.getProcessor().getColorModel());

		ByteBuffer buffer = Buffers.newDirectByteBuffer(tw * th * td * 4);
		for (int z = 0; z < td; z++) {
			int slice = Math.min(d, z * stepZ + 1);
			ImageProcessor ip = stack.getProcessor(imp.getStackIndex(channel, slice, frame));
			boolean rgb = ip instanceof ColorProcessor;
			for (int y = 0; y < th; y++) {
				int py = y * stepY;
				for (int x = 0; x < tw; x++) {
					int px = x * stepX;
					int color;
					int intensity;
					if (rgb) {
						color = ip.getPixel(px, py);
						intensity = (int) Math.round(0.299 * ((color >> 16) & 0xff) + 0.587 * ((color >> 8) & 0xff)
								+ 0.114 * (color & 0xff));
					} else {
						/* Scaled to the display range like the image in ImageJ! */
						double value = (ip.getf(px, py) - min) / range * 255.0;
						intensity = Double.isNaN(value) ? 0 : (int) Math.max(0, Math.min(255, Math.round(value)));
						color = lutColors ? lut[intensity] : (intensity << 16) | (intensity << 8) | intensity;
					}
					buffer.put((byte) ((color >> 16) & 0xff));
					buffer.put((byte) ((color >> 8) & 0xff));
					buffer.put((byte) (color & 0xff));
					buffer.put((byte) intensity);
				}
			}
		}
		buffer.rewind();

		/* The calibrated depth of a voxel in pixel units! */
		Calibration cal = imp.getCalibration();
		double voxelDepth = 1;
		if (cal != null && cal.pixelWidth > 0 && cal.pixelDepth > 0) {
			voxelDepth = cal.pixelDepth / cal.pixelWidth;
		}
		voxels = buffer;
		texWidth = tw;
		texHeight = th;
		texDepth = td;
		width = w;
		height = h;
		depth = (float) (d * voxelDepth);
		image = imp;
		upload = true;
		return true;
	}

	/**
	 * Loads the last loaded stack again (e.g., after a LUT change).
	 *
	 * @return true if the stack has been loaded
	 */
	public boolean reload() {
		return image != null && load(image);
	}

	/* The RGB colors of the LUT (or a gray LUT)! */
	private static int[] createLut(ColorModel cm) {
		int[] lut = new int[256];
		for (int i = 0; i < 256; i++) {
			lut[i] = cm != null ? cm.getRGB(i) & 0xffffff : (i << 16) | (i << 8) | i;
		}
		return lut;
	}

	/**
	 * Removes the volume.
	 */
	public synchronized void clear() {
		voxels = null;
		image = null;
		upload = true;
	}

	/**
	 * Must be called if the OpenGL context has been created again. The texture is
	 * then created again with the next rendering.
	 */
	public synchronized void contextLost() {
		textureId = 0;
		upload = voxels != null;
	}

	/**
	 * Returns true if a volume is loaded and displayed.
	 */
	public synchronized boolean isDisplayed() {
		return show && voxels != null;
	}

	/**
	 * Renders the volume (Cartesian: centered at the origin, octant: from the
	 * origin). Must be called after the opaque objects of the scene.
	 *
	 * @param gl        the OpenGL context
	 * @param cartesian true for the Cartesian mode
	 */
	public synchronized void render(GL2 gl, boolean cartesian) {
		if (max3dTextureSize == 0) {
			int[] size = new int[1];
			gl.glGetIntegerv(GL2.GL_MAX_3D_TEXTURE_SIZE, size, 0);
			max3dTextureSize = size[0];
		}
		if (upload) {
			uploadTexture(gl);
		}
		if (!show || textureId == 0) {
			return;
		}
		float w = width;
		float h = height;
		float d = getDepth();
		float x0 = cartesian ? -w / 2 : 0;
		float y0 = cartesian ? -h / 2 : 0;
		float z0 = cartesian ? -d / 2 : 0;

		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_TEXTURE_BIT
				| GL2.GL_CURRENT_BIT);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL.GL_CULL_FACE);
		gl.glDisable(GL.GL_TEXTURE_2D);
		/* Transparent slices: no depth values (the depth test against the scene remains)! */
		gl.glDepthMask(false);
		gl.glEnable(GL.GL_BLEND);

		float alpha;
		if (mode == MODE_MAXIMUM_INTENSITY) {
			/* A black background inside of the volume for the maximum! */
			gl.glDisable(GL2.GL_ALPHA_TEST);
			gl.glBlendEquation(GL.GL_FUNC_ADD);
			gl.glBlendFunc(GL.GL_ONE, GL.GL_ZERO);
			gl.glColor4f(0, 0, 0, 1);
			drawBox(gl, x0, y0, z0, w, h, d);
			gl.glBlendEquation(GL2.GL_MAX);
			gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
			alpha = 1.0f;
		} else {
			gl.glBlendEquation(GL.GL_FUNC_ADD);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			/* More slices are more transparent! */
			alpha = opacity / sampling;
		}
		gl.glEnable(GL2.GL_ALPHA_TEST);
		gl.glAlphaFunc(GL.GL_GREATER, threshold * alpha);

		gl.glEnable(GL2.GL_TEXTURE_3D);
		gl.glBindTexture(GL2.GL_TEXTURE_3D, textureId);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
		gl.glColor4f(1, 1, 1, alpha);
		drawSlices(gl, x0, y0, z0, w, h, d);

		gl.glBindTexture(GL2.GL_TEXTURE_3D, 0);
		gl.glPopAttrib();
	}

	/*
	 * Draws the slices along the axis which is most aligned with the view
	 * direction, from the back to the front!
	 */
	private void drawSlices(GL2 gl, float x0, float y0, float z0, float w, float h, float d) {
		float[] mv = new float[16];
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mv, 0);
		/* The direction to the viewer in object coordinates (row 3 of the rotation)! */
		float vx = mv[2];
		float vy = mv[6];
		float vz = mv[10];
		float ax = Math.abs(vx);
		float ay = Math.abs(vy);
		float az = Math.abs(vz);
		int axis = (ax >= ay && ax >= az) ? 0 : (ay >= az ? 1 : 2);
		float toViewer = axis == 0 ? vx : (axis == 1 ? vy : vz);
		int voxelsAlongAxis = axis == 0 ? texWidth : (axis == 1 ? texHeight : texDepth);
		int n = Math.max(2, Math.round(voxelsAlongAxis * sampling));
		boolean flip = HeightMaps.isFlipVertically();

		gl.glBegin(GL2.GL_QUADS);
		for (int i = 0; i < n; i++) {
			/* The back is on the side away from the viewer! */
			int k = toViewer > 0 ? i : n - 1 - i;
			float f = (k + 0.5f) / n;
			if (axis == 2) {
				float z = z0 + f * d;
				vertex(gl, 0, 0, f, x0, y0, z, flip);
				vertex(gl, 1, 0, f, x0 + w, y0, z, flip);
				vertex(gl, 1, 1, f, x0 + w, y0 + h, z, flip);
				vertex(gl, 0, 1, f, x0, y0 + h, z, flip);
			} else if (axis == 1) {
				float y = y0 + f * h;
				vertex(gl, 0, f, 0, x0, y, z0, flip);
				vertex(gl, 1, f, 0, x0 + w, y, z0, flip);
				vertex(gl, 1, f, 1, x0 + w, y, z0 + d, flip);
				vertex(gl, 0, f, 1, x0, y, z0 + d, flip);
			} else {
				float x = x0 + f * w;
				vertex(gl, f, 0, 0, x, y0, z0, flip);
				vertex(gl, f, 1, 0, x, y0 + h, z0, flip);
				vertex(gl, f, 1, 1, x, y0 + h, z0 + d, flip);
				vertex(gl, f, 0, 1, x, y0, z0 + d, flip);
			}
		}
		gl.glEnd();
	}

	/*
	 * A vertex at the relative position (u, v, r) of the volume. Like in ImageJ the
	 * first image row is at the top (the texture row 0), else flipped!
	 */
	private static void vertex(GL2 gl, float u, float v, float r, float x, float y, float z, boolean flip) {
		gl.glTexCoord3f(u, flip ? v : 1 - v, r);
		gl.glVertex3f(x, y, z);
	}

	private static void drawBox(GL2 gl, float x0, float y0, float z0, float w, float h, float d) {
		float x1 = x0 + w;
		float y1 = y0 + h;
		float z1 = z0 + d;
		gl.glBegin(GL2.GL_QUADS);
		/* Back and front */
		gl.glVertex3f(x0, y0, z0);
		gl.glVertex3f(x1, y0, z0);
		gl.glVertex3f(x1, y1, z0);
		gl.glVertex3f(x0, y1, z0);
		gl.glVertex3f(x0, y0, z1);
		gl.glVertex3f(x1, y0, z1);
		gl.glVertex3f(x1, y1, z1);
		gl.glVertex3f(x0, y1, z1);
		/* Bottom and top */
		gl.glVertex3f(x0, y0, z0);
		gl.glVertex3f(x1, y0, z0);
		gl.glVertex3f(x1, y0, z1);
		gl.glVertex3f(x0, y0, z1);
		gl.glVertex3f(x0, y1, z0);
		gl.glVertex3f(x1, y1, z0);
		gl.glVertex3f(x1, y1, z1);
		gl.glVertex3f(x0, y1, z1);
		/* Left and right */
		gl.glVertex3f(x0, y0, z0);
		gl.glVertex3f(x0, y1, z0);
		gl.glVertex3f(x0, y1, z1);
		gl.glVertex3f(x0, y0, z1);
		gl.glVertex3f(x1, y0, z0);
		gl.glVertex3f(x1, y1, z0);
		gl.glVertex3f(x1, y1, z1);
		gl.glVertex3f(x1, y0, z1);
		gl.glEnd();
	}

	private void uploadTexture(GL2 gl) {
		upload = false;
		if (textureId != 0) {
			gl.glDeleteTextures(1, new int[] { textureId }, 0);
			textureId = 0;
		}
		if (voxels == null) {
			return;
		}
		int[] id = new int[1];
		gl.glGenTextures(1, id, 0);
		textureId = id[0];
		gl.glBindTexture(GL2.GL_TEXTURE_3D, textureId);
		gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_EDGE);
		gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
		voxels.rewind();
		gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL.GL_RGBA8, texWidth, texHeight, texDepth, 0, GL.GL_RGBA,
				GL.GL_UNSIGNED_BYTE, voxels);
		gl.glBindTexture(GL2.GL_TEXTURE_3D, 0);
	}

	/**
	 * Returns a description of the loaded stack.
	 */
	public synchronized String getInfo() {
		if (voxels == null) {
			return "No stack loaded";
		}
		String title = image != null ? image.getTitle() : "";
		return title + ": " + (int) width + " x " + (int) height + " x " + image.getNSlices() + " (Texture: "
				+ texWidth + " x " + texHeight + " x " + texDepth + ")";
	}

	public synchronized float getWidth() {
		return width;
	}

	public synchronized float getHeight() {
		return height;
	}

	/**
	 * Returns the depth of the volume (slices multiplied with the calibrated voxel
	 * depth and the z scale).
	 */
	public synchronized float getDepth() {
		return depth * zScale;
	}

	public boolean isShow() {
		return show;
	}

	public void setShow(boolean show) {
		this.show = show;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public float getOpacity() {
		return opacity;
	}

	/**
	 * The opacity of a slice (0-1) in the alpha blending mode.
	 */
	public void setOpacity(float opacity) {
		this.opacity = Math.max(0, Math.min(1, opacity));
	}

	public float getThreshold() {
		return threshold;
	}

	/**
	 * Voxels with an intensity (0-1) below or equal to the threshold are not
	 * displayed.
	 */
	public void setThreshold(float threshold) {
		this.threshold = Math.max(0, Math.min(1, threshold));
	}

	public float getSampling() {
		return sampling;
	}

	/**
	 * The number of slices relative to the number of voxels along the slice axis.
	 */
	public void setSampling(float sampling) {
		this.sampling = Math.max(0.25f, sampling);
	}

	public float getZScale() {
		return zScale;
	}

	/**
	 * Scales the calibrated depth of the volume.
	 */
	public synchronized void setZScale(float zScale) {
		this.zScale = Math.max(0.01f, zScale);
	}

	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * The maximum texture size of a dimension. Larger stacks are reduced (applied
	 * when a stack is loaded).
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = Math.max(8, maxSize);
	}

	public boolean isLutColors() {
		return lutColors;
	}

	/**
	 * Use the colors of the LUT, else gray values (applied when a stack is loaded).
	 */
	public void setLutColors(boolean lutColors) {
		this.lutColors = lutColors;
	}
}
