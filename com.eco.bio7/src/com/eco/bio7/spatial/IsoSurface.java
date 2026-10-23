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
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Creates and renders an iso surface of an ImageJ stack. The surface is
 * extracted with the Surface Nets algorithm (smoother and more regular than
 * marching cubes) from the (Gaussian smoothed) stack, the vertices are shared
 * by the faces and the mesh is smoothed with the Taubin method (without
 * shrinking). The surface is rendered with vertex buffers, smooth normals and a
 * head light and can be exported as a Wavefront *.obj file.
 */
public class IsoSurface {

	/* The 12 edges of a cell (corner indices, bit 0 = x, bit 1 = y, bit 2 = z)! */
	private static final int[][] CELL_EDGES = { { 0, 1 }, { 2, 3 }, { 4, 5 }, { 6, 7 }, { 0, 2 }, { 1, 3 }, { 4, 6 },
			{ 5, 7 }, { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 } };
	private static final float[] DEFAULT_COLOR = { 0.85f, 0.78f, 0.66f };

	/* The (reduced) stack */
	private float[] data;
	private int nx;
	private int ny;
	private int nz;
	private float stepX = 1;
	private float stepY = 1;
	private float stepZ = 1; // with the calibrated voxel depth
	private float width;
	private float height;
	private float depth;
	private double displayMin;
	private double displayMax;
	private double pixelSize = 1;
	private String unit = "pixel";
	private ImagePlus image;
	private ColorModel lut;

	/* The mesh in voxel coordinates and the baked world coordinates */
	private float[] meshVertices;
	private int[] meshTriangles;
	private float[] positions;
	private float[] normals;
	private int[] indices;
	private boolean bakedFlip;

	/* OpenGL */
	private final int[] buffers = new int[3];
	private boolean hasBuffers = false;
	private boolean upload = false;
	private int indexCount = 0;

	/* Settings */
	private boolean show = true;
	private double isoValue = 128;
	private float smoothing = 1.0f;
	private int meshSmoothing = 5;
	private int maxSize = 256;
	private float zScale = 1.0f;
	private float opacity = 1.0f;
	private boolean headLight = true;
	private boolean lutColor = true;
	private float[] color = DEFAULT_COLOR.clone();

	/* The background computation */
	private Thread worker;
	private boolean dirty = false;
	private Runnable updateListener;

	/**
	 * Loads a stack (the current channel and frame of a hyperstack) and computes
	 * the iso surface in the background. The default iso value is the threshold
	 * of the image (if set) or the center of the display range.
	 *
	 * @param imp the ImageJ image
	 * @return true if the stack has been loaded
	 */
	public boolean load(ImagePlus imp) {
		if (imp == null || imp.getProcessor() == null) {
			return false;
		}
		int w = imp.getWidth();
		int h = imp.getHeight();
		int d = imp.getNSlices();
		int sx = (w + maxSize - 1) / maxSize;
		int sy = (h + maxSize - 1) / maxSize;
		int sz = (d + maxSize - 1) / maxSize;
		int tw = (w + sx - 1) / sx;
		int th = (h + sy - 1) / sy;
		int td = (d + sz - 1) / sz;
		float[] values = new float[tw * th * td];
		ImageStack stack = imp.getStack();
		int channel = imp.getChannel();
		int frame = imp.getFrame();
		for (int z = 0; z < td; z++) {
			ImageProcessor ip = stack.getProcessor(imp.getStackIndex(channel, Math.min(d, z * sz + 1), frame));
			boolean rgb = ip instanceof ColorProcessor;
			for (int y = 0; y < th; y++) {
				for (int x = 0; x < tw; x++) {
					float value;
					if (rgb) {
						int c = ip.getPixel(x * sx, y * sy);
						value = (float) (0.299 * ((c >> 16) & 0xff) + 0.587 * ((c >> 8) & 0xff) + 0.114 * (c & 0xff));
					} else {
						value = ip.getf(x * sx, y * sy);
					}
					values[(z * th + y) * tw + x] = Float.isNaN(value) ? 0 : value;
				}
			}
		}
		Calibration cal = imp.getCalibration();
		double voxelDepth = 1;
		if (cal != null && cal.pixelWidth > 0 && cal.pixelDepth > 0) {
			voxelDepth = cal.pixelDepth / cal.pixelWidth;
		}
		ImageProcessor ip = imp.getProcessor();
		double iso;
		if (!(ip instanceof ColorProcessor) && ip.getMinThreshold() != ImageProcessor.NO_THRESHOLD) {
			iso = ip.getMinThreshold();
		} else {
			iso = (imp.getDisplayRangeMin() + imp.getDisplayRangeMax()) / 2.0;
		}
		synchronized (this) {
			data = values;
			nx = tw;
			ny = th;
			nz = td;
			stepX = sx;
			stepY = sy;
			stepZ = (float) (sz * voxelDepth);
			width = w;
			height = h;
			depth = (float) (d * voxelDepth);
			displayMin = imp.getDisplayRangeMin();
			displayMax = imp.getDisplayRangeMax();
			if (ip instanceof ColorProcessor) {
				displayMin = 0;
				displayMax = 255;
			}
			pixelSize = cal != null && cal.pixelWidth > 0 ? cal.pixelWidth : 1;
			unit = cal != null ? cal.getUnit() : "pixel";
			lut = ip instanceof ColorProcessor ? null : ip.getColorModel();
			image = imp;
			isoValue = iso;
		}
		update();
		return true;
	}

	/**
	 * Loads the last loaded stack again (e.g., after a change of the maximum
	 * size).
	 */
	public boolean reload() {
		return image != null && load(image);
	}

	/**
	 * Removes the iso surface.
	 */
	public synchronized void clear() {
		data = null;
		image = null;
		meshVertices = null;
		meshTriangles = null;
		positions = null;
		normals = null;
		indices = null;
		upload = true;
		notifyListener();
	}

	/**
	 * Computes the iso surface again in the background (e.g., after a change of
	 * the iso value or the smoothing).
	 */
	public synchronized void update() {
		if (data == null) {
			return;
		}
		dirty = true;
		if (worker == null || !worker.isAlive()) {
			worker = new Thread(this::computeLoop, "Bio7 Iso Surface");
			worker.setDaemon(true);
			worker.start();
		}
	}

	private void computeLoop() {
		while (true) {
			float[] values;
			int w, h, d;
			double iso;
			float sigma;
			int iterations;
			synchronized (this) {
				if (!dirty || data == null) {
					return;
				}
				dirty = false;
				values = data;
				w = nx;
				h = ny;
				d = nz;
				iso = isoValue;
				sigma = smoothing;
				iterations = meshSmoothing;
			}
			float[] smoothed = sigma > 0 ? gaussian(values, w, h, d, sigma) : values;
			Mesh mesh = surfaceNets(smoothed, w, h, d, (float) iso);
			taubinSmoothing(mesh, iterations);
			synchronized (this) {
				if (values != data) {
					continue; // a new stack has been loaded
				}
				meshVertices = mesh.vertices;
				meshTriangles = mesh.triangles;
				bake(HeightMaps.isFlipVertically());
				upload = true;
			}
			notifyListener();
		}
	}

	private void notifyListener() {
		Runnable listener = updateListener;
		if (listener != null) {
			listener.run();
		}
	}

	/* A separable Gaussian filter (sigma in voxels, clamped at the borders)! */
	private static float[] gaussian(float[] values, int w, int h, int d, float sigma) {
		int radius = Math.max(1, (int) Math.ceil(2.5 * sigma));
		float[] kernel = new float[2 * radius + 1];
		float sum = 0;
		for (int i = -radius; i <= radius; i++) {
			kernel[i + radius] = (float) Math.exp(-(i * i) / (2.0 * sigma * sigma));
			sum += kernel[i + radius];
		}
		for (int i = 0; i < kernel.length; i++) {
			kernel[i] /= sum;
		}
		float[] a = values.clone();
		float[] b = new float[values.length];
		int[] dims = { w, h, d };
		int[] strides = { 1, w, w * h };
		for (int axis = 0; axis < 3; axis++) {
			int n = dims[axis];
			if (n < 2) {
				continue;
			}
			int stride = strides[axis];
			for (int i = 0; i < a.length; i++) {
				int pos = (i / stride) % n;
				int base = i - pos * stride;
				float value = 0;
				for (int k = -radius; k <= radius; k++) {
					int p = Math.max(0, Math.min(n - 1, pos + k));
					value += kernel[k + radius] * a[base + p * stride];
				}
				b[i] = value;
			}
			float[] t = a;
			a = b;
			b = t;
		}
		return a;
	}

	/* A mesh with vertices (x, y, z) and triangles (vertex indices)! */
	static class Mesh {
		float[] vertices;
		int[] triangles;
	}

	/*
	 * Surface Nets: one vertex per cell with a sign change (the mean of the edge
	 * intersections) and a quad for each edge with a sign change (the four cells
	 * around the edge). The stack is padded with values below the iso value, so
	 * the surface is closed at the borders of the stack. Faces are oriented to the
	 * lower values (outside)!
	 */
	static Mesh surfaceNets(float[] f, int w, int h, int d, float iso) {
		float min = Float.MAX_VALUE;
		for (float v : f) {
			min = Math.min(min, v);
		}
		final float outside = Math.min(min, iso) - 1;
		int cw = w + 1;
		int ch = h + 1;
		int cd = d + 1;
		int[] cellVertex = new int[cw * ch * cd];
		Arrays.fill(cellVertex, -1);
		FloatList vertices = new FloatList();
		float[] v = new float[8];
		int count = 0;
		for (int z = -1; z < d; z++) {
			for (int y = -1; y < h; y++) {
				for (int x = -1; x < w; x++) {
					int mask = 0;
					for (int i = 0; i < 8; i++) {
						v[i] = value(f, w, h, d, x + (i & 1), y + ((i >> 1) & 1), z + ((i >> 2) & 1), outside);
						if (v[i] >= iso) {
							mask |= 1 << i;
						}
					}
					if (mask == 0 || mask == 255) {
						continue;
					}
					float px = 0, py = 0, pz = 0;
					int crossings = 0;
					for (int[] edge : CELL_EDGES) {
						float va = v[edge[0]];
						float vb = v[edge[1]];
						if ((va >= iso) == (vb >= iso)) {
							continue;
						}
						float t = (iso - va) / (vb - va);
						int a = edge[0];
						int b = edge[1];
						px += (a & 1) + t * ((b & 1) - (a & 1));
						py += ((a >> 1) & 1) + t * (((b >> 1) & 1) - ((a >> 1) & 1));
						pz += ((a >> 2) & 1) + t * (((b >> 2) & 1) - ((a >> 2) & 1));
						crossings++;
					}
					vertices.add(x + px / crossings, y + py / crossings, z + pz / crossings);
					cellVertex[((z + 1) * ch + (y + 1)) * cw + (x + 1)] = count++;
				}
			}
		}
		IntList triangles = new IntList();
		int[] p = new int[3];
		int[] q = new int[4];
		for (int z = -1; z < d; z++) {
			for (int y = -1; y < h; y++) {
				for (int x = -1; x < w; x++) {
					float a = value(f, w, h, d, x, y, z, outside);
					p[0] = x;
					p[1] = y;
					p[2] = z;
					for (int axis = 0; axis < 3; axis++) {
						int u = (axis + 1) % 3;
						int vv = (axis + 2) % 3;
						if (p[u] < 0 || p[vv] < 0) {
							continue;
						}
						float b = value(f, w, h, d, x + (axis == 0 ? 1 : 0), y + (axis == 1 ? 1 : 0),
								z + (axis == 2 ? 1 : 0), outside);
						boolean inside = a >= iso;
						if (inside == (b >= iso)) {
							continue;
						}
						/* The four cells around the edge (counter clockwise in the u, v plane)! */
						q[0] = cell(cellVertex, cw, ch, p, -1, -1, u, vv, 0, 0);
						q[1] = cell(cellVertex, cw, ch, p, -1, -1, u, vv, 1, 0);
						q[2] = cell(cellVertex, cw, ch, p, -1, -1, u, vv, 1, 1);
						q[3] = cell(cellVertex, cw, ch, p, -1, -1, u, vv, 0, 1);
						if (q[0] < 0 || q[1] < 0 || q[2] < 0 || q[3] < 0) {
							continue;
						}
						if (!inside) {
							int t = q[1];
							q[1] = q[3];
							q[3] = t;
						}
						/* Split the quad along the shorter diagonal! */
						if (distance(vertices, q[0], q[2]) <= distance(vertices, q[1], q[3])) {
							triangles.add(q[0], q[1], q[2]);
							triangles.add(q[0], q[2], q[3]);
						} else {
							triangles.add(q[0], q[1], q[3]);
							triangles.add(q[1], q[2], q[3]);
						}
					}
				}
			}
		}
		Mesh mesh = new Mesh();
		mesh.vertices = vertices.toArray();
		mesh.triangles = triangles.toArray();
		return mesh;
	}

	/* The vertex of the cell p minus du * e_u minus dv * e_v! */
	private static int cell(int[] cellVertex, int cw, int ch, int[] p, int su, int sv, int u, int v, int du, int dv) {
		int[] c = { p[0], p[1], p[2] };
		c[u] += su * du;
		c[v] += sv * dv;
		return cellVertex[((c[2] + 1) * ch + (c[1] + 1)) * cw + (c[0] + 1)];
	}

	private static float value(float[] f, int w, int h, int d, int x, int y, int z, float outside) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d) {
			return outside;
		}
		return f[(z * h + y) * w + x];
	}

	private static float distance(FloatList v, int a, int b) {
		float dx = v.get(a * 3) - v.get(b * 3);
		float dy = v.get(a * 3 + 1) - v.get(b * 3 + 1);
		float dz = v.get(a * 3 + 2) - v.get(b * 3 + 2);
		return dx * dx + dy * dy + dz * dz;
	}

	/* Taubin smoothing (lambda/mu) smooths the mesh without shrinking it! */
	static void taubinSmoothing(Mesh mesh, int iterations) {
		if (iterations <= 0 || mesh.triangles.length == 0) {
			return;
		}
		int n = mesh.vertices.length / 3;
		int[] tri = mesh.triangles;
		int[] degree = new int[n + 1];
		for (int i = 0; i < tri.length; i += 3) {
			degree[tri[i]] += 2;
			degree[tri[i + 1]] += 2;
			degree[tri[i + 2]] += 2;
		}
		int[] start = new int[n + 1];
		for (int i = 0; i < n; i++) {
			start[i + 1] = start[i] + degree[i];
		}
		int[] neighbors = new int[start[n]];
		int[] fill = Arrays.copyOf(start, n);
		for (int i = 0; i < tri.length; i += 3) {
			int a = tri[i], b = tri[i + 1], c = tri[i + 2];
			neighbors[fill[a]++] = b;
			neighbors[fill[a]++] = c;
			neighbors[fill[b]++] = a;
			neighbors[fill[b]++] = c;
			neighbors[fill[c]++] = a;
			neighbors[fill[c]++] = b;
		}
		float[] p = mesh.vertices;
		float[] next = new float[p.length];
		for (int it = 0; it < iterations; it++) {
			for (float factor : new float[] { 0.5f, -0.53f }) {
				for (int i = 0; i < n; i++) {
					int s = start[i], e = start[i + 1];
					if (e == s) {
						next[i * 3] = p[i * 3];
						next[i * 3 + 1] = p[i * 3 + 1];
						next[i * 3 + 2] = p[i * 3 + 2];
						continue;
					}
					float ax = 0, ay = 0, az = 0;
					for (int k = s; k < e; k++) {
						int j = neighbors[k];
						ax += p[j * 3];
						ay += p[j * 3 + 1];
						az += p[j * 3 + 2];
					}
					int m = e - s;
					next[i * 3] = p[i * 3] + factor * (ax / m - p[i * 3]);
					next[i * 3 + 1] = p[i * 3 + 1] + factor * (ay / m - p[i * 3 + 1]);
					next[i * 3 + 2] = p[i * 3 + 2] + factor * (az / m - p[i * 3 + 2]);
				}
				float[] t = p;
				p = next;
				next = t;
			}
		}
		mesh.vertices = p;
	}

	/*
	 * The world coordinates (pixel units, octant: from the origin) and the vertex
	 * normals. Like in ImageJ the first image row is at the top, else flipped. The
	 * mirroring reverses the orientation of the triangles!
	 */
	private void bake(boolean flip) {
		bakedFlip = flip;
		if (meshVertices == null) {
			positions = null;
			normals = null;
			indices = null;
			return;
		}
		int n = meshVertices.length / 3;
		float[] pos = new float[n * 3];
		for (int i = 0; i < n; i++) {
			pos[i * 3] = (meshVertices[i * 3] + 0.5f) * stepX;
			float y = (meshVertices[i * 3 + 1] + 0.5f) * stepY;
			pos[i * 3 + 1] = flip ? y : height - y;
			pos[i * 3 + 2] = (meshVertices[i * 3 + 2] + 0.5f) * stepZ;
		}
		int[] idx = meshTriangles.clone();
		if (!flip) {
			for (int i = 0; i < idx.length; i += 3) {
				int t = idx[i + 1];
				idx[i + 1] = idx[i + 2];
				idx[i + 2] = t;
			}
		}
		positions = pos;
		indices = idx;
		normals = computeNormals(pos, idx);
	}

	/* Area weighted vertex normals! */
	static float[] computeNormals(float[] pos, int[] idx) {
		float[] nor = new float[pos.length];
		for (int i = 0; i < idx.length; i += 3) {
			int a = idx[i] * 3, b = idx[i + 1] * 3, c = idx[i + 2] * 3;
			float ux = pos[b] - pos[a], uy = pos[b + 1] - pos[a + 1], uz = pos[b + 2] - pos[a + 2];
			float vx = pos[c] - pos[a], vy = pos[c + 1] - pos[a + 1], vz = pos[c + 2] - pos[a + 2];
			float nx = uy * vz - uz * vy;
			float ny = uz * vx - ux * vz;
			float nz = ux * vy - uy * vx;
			for (int k : new int[] { a, b, c }) {
				nor[k] += nx;
				nor[k + 1] += ny;
				nor[k + 2] += nz;
			}
		}
		for (int i = 0; i < nor.length; i += 3) {
			float l = (float) Math.sqrt(nor[i] * nor[i] + nor[i + 1] * nor[i + 1] + nor[i + 2] * nor[i + 2]);
			if (l > 0) {
				nor[i] /= l;
				nor[i + 1] /= l;
				nor[i + 2] /= l;
			}
		}
		return nor;
	}

	/**
	 * Must be called if the OpenGL context has been created again.
	 */
	public synchronized void contextLost() {
		hasBuffers = false;
		upload = positions != null;
	}

	/**
	 * Returns true if an iso surface is loaded and displayed.
	 */
	public synchronized boolean isDisplayed() {
		return show && data != null;
	}

	/**
	 * Renders the iso surface (Cartesian: centered at the origin, octant: from the
	 * origin).
	 */
	public synchronized void render(GL2 gl, boolean cartesian) {
		if (positions != null && bakedFlip != HeightMaps.isFlipVertically()) {
			bake(HeightMaps.isFlipVertically());
			upload = true;
		}
		if (upload) {
			uploadBuffers(gl);
		}
		if (!show || !hasBuffers || indexCount == 0) {
			return;
		}
		float d = depth * zScale;
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_LIGHTING_BIT | GL2.GL_CURRENT_BIT | GL2.GL_COLOR_BUFFER_BIT
				| GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_POLYGON_BIT);
		gl.glPushMatrix();
		if (cartesian) {
			gl.glTranslatef(-width / 2, -height / 2, -d / 2);
		}
		gl.glScalef(1, 1, zScale);

		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_NORMALIZE);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
		gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, new float[] { 0.25f, 0.25f, 0.25f, 1 }, 0);
		if (headLight) {
			/* A directional light from the viewer (in eye coordinates)! */
			for (int i = 0; i < 4; i++) {
				gl.glDisable(GL2.GL_LIGHT0 + i);
			}
			int light = GL2.GL_LIGHT0 + 7;
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glLightfv(light, GL2.GL_POSITION, new float[] { 0.3f, 0.4f, 1, 0 }, 0);
			gl.glPopMatrix();
			gl.glLightfv(light, GL2.GL_AMBIENT, new float[] { 0, 0, 0, 1 }, 0);
			gl.glLightfv(light, GL2.GL_DIFFUSE, new float[] { 0.85f, 0.85f, 0.85f, 1 }, 0);
			gl.glLightfv(light, GL2.GL_SPECULAR, new float[] { 0.6f, 0.6f, 0.6f, 1 }, 0);
			gl.glEnable(light);
		}
		float[] c = getRenderColor();
		float[] material = { c[0], c[1], c[2], opacity };
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, material, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, material, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[] { 0.35f, 0.35f, 0.35f, 1 }, 0);
		gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 48);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[0]);
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[1]);
		gl.glNormalPointer(GL.GL_FLOAT, 0, 0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffers[2]);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
		if (opacity < 1) {
			/* Transparent: the back faces first, then the front faces! */
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDepthMask(false);
			gl.glEnable(GL.GL_CULL_FACE);
			gl.glCullFace(GL.GL_FRONT);
			gl.glDrawElements(GL.GL_TRIANGLES, indexCount, GL.GL_UNSIGNED_INT, 0);
			gl.glCullFace(GL.GL_BACK);
			gl.glDrawElements(GL.GL_TRIANGLES, indexCount, GL.GL_UNSIGNED_INT, 0);
		} else {
			gl.glDisable(GL.GL_BLEND);
			gl.glDrawElements(GL.GL_TRIANGLES, indexCount, GL.GL_UNSIGNED_INT, 0);
		}
		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);

		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	private void uploadBuffers(GL2 gl) {
		upload = false;
		if (hasBuffers) {
			gl.glDeleteBuffers(3, buffers, 0);
			hasBuffers = false;
		}
		indexCount = 0;
		if (positions == null || indices == null || indices.length == 0) {
			return;
		}
		gl.glGenBuffers(3, buffers, 0);
		FloatBuffer pos = Buffers.newDirectFloatBuffer(positions);
		FloatBuffer nor = Buffers.newDirectFloatBuffer(normals);
		IntBuffer idx = Buffers.newDirectIntBuffer(indices);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[0]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) positions.length * 4, pos, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[1]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) normals.length * 4, nor, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffers[2]);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, (long) indices.length * 4, idx, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		hasBuffers = true;
		indexCount = indices.length;
	}

	/* The selected color or the color of the LUT at the iso value! */
	private float[] getRenderColor() {
		if (lutColor && lut != null) {
			double range = displayMax > displayMin ? displayMax - displayMin : 1;
			int index = (int) Math.max(0, Math.min(255, Math.round((isoValue - displayMin) / range * 255)));
			int rgb = lut.getRGB(index);
			int r = (rgb >> 16) & 0xff, g = (rgb >> 8) & 0xff, b = rgb & 0xff;
			/* A gray LUT: the default color, else the surface is too dark! */
			if (!(r == g && g == b)) {
				return new float[] { r / 255f, g / 255f, b / 255f };
			}
			return DEFAULT_COLOR;
		}
		return color;
	}

	/** The supported export formats (file extensions). */
	public static final String[] EXPORT_FORMATS = { "obj", "stl", "ply", "glb", "vtk" };

	/* A copy of the surface for the export (calibrated coordinates with the z scale)! */
	private static class ExportData {
		float[] positions;
		float[] normals;
		int[] indices;
		float[] color;
		float opacity;
		String title;
		String unit;
		double isoValue;
	}

	private ExportData exportData() throws IOException {
		ExportData e = new ExportData();
		float[] pos;
		int[] idx;
		float scale;
		float zs;
		synchronized (this) {
			if (positions == null || indices == null || indices.length == 0) {
				throw new IOException("No iso surface available!");
			}
			pos = positions;
			idx = indices;
			scale = (float) pixelSize;
			zs = zScale;
			e.color = getRenderColor();
			e.opacity = opacity;
			e.title = image != null ? image.getTitle() : "iso surface";
			e.unit = unit;
			e.isoValue = isoValue;
		}
		float[] p = new float[pos.length];
		for (int i = 0; i < pos.length; i += 3) {
			p[i] = pos[i] * scale;
			p[i + 1] = pos[i + 1] * scale;
			p[i + 2] = pos[i + 2] * zs * scale;
		}
		e.positions = p;
		e.indices = idx;
		/* The normals of the scaled surface! */
		e.normals = computeNormals(p, idx);
		return e;
	}

	/**
	 * Exports the iso surface. The format is selected by the file extension (obj,
	 * stl, ply, glb or vtk, see EXPORT_FORMATS). The coordinates are calibrated
	 * (the unit of the image).
	 * 
	 * @param file the file
	 * @throws IOException if the file can't be written or the format is unknown
	 */
	public void export(File file) throws IOException {
		String name = file.getName().toLowerCase();
		String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
		switch (extension) {
		case "obj":
			exportObj(file);
			break;
		case "stl":
			exportStl(file);
			break;
		case "ply":
			exportPly(file);
			break;
		case "glb":
			exportGlb(file);
			break;
		case "vtk":
			exportVtk(file);
			break;
		default:
			throw new IOException("Unknown export format: " + file.getName());
		}
	}

	/**
	 * Exports the iso surface as a Wavefront *.obj file (with a *.mtl file for the
	 * color). The coordinates are calibrated (the unit of the image).
	 * 
	 * @param file the *.obj file
	 * @throws IOException if the file can't be written
	 */
	public void exportObj(File file) throws IOException {
		ExportData e = exportData();
		float[] c = e.color;
		String name = file.getName();
		String base = name.toLowerCase().endsWith(".obj") ? name.substring(0, name.length() - 4) : name;
		File mtlFile = new File(file.getParentFile(), base + ".mtl");
		try (BufferedWriter out = new BufferedWriter(new FileWriter(mtlFile))) {
			out.write("# Bio7 iso surface material\n");
			out.write("newmtl isosurface\n");
			out.write("Ka " + c[0] * 0.25f + " " + c[1] * 0.25f + " " + c[2] * 0.25f + "\n");
			out.write("Kd " + c[0] + " " + c[1] + " " + c[2] + "\n");
			out.write("Ks 0.35 0.35 0.35\n");
			out.write("Ns 48\n");
			out.write("d " + e.opacity + "\n");
		}
		try (BufferedWriter out = new BufferedWriter(new FileWriter(file), 1 << 16)) {
			out.write("# Bio7 iso surface of " + e.title + ", iso value " + e.isoValue + ", unit " + e.unit + "\n");
			out.write("mtllib " + mtlFile.getName() + "\n");
			out.write("o " + base.replace(' ', '_') + "\n");
			StringBuilder sb = new StringBuilder(64);
			float[] pos = e.positions;
			for (int i = 0; i < pos.length; i += 3) {
				sb.setLength(0);
				sb.append("v ").append(pos[i]).append(' ').append(pos[i + 1]).append(' ').append(pos[i + 2])
						.append('\n');
				out.write(sb.toString());
			}
			float[] nor = e.normals;
			for (int i = 0; i < nor.length; i += 3) {
				sb.setLength(0);
				sb.append("vn ").append(nor[i]).append(' ').append(nor[i + 1]).append(' ').append(nor[i + 2])
						.append('\n');
				out.write(sb.toString());
			}
			out.write("usemtl isosurface\n");
			out.write("s 1\n");
			int[] idx = e.indices;
			for (int i = 0; i < idx.length; i += 3) {
				int a = idx[i] + 1, b = idx[i + 1] + 1, cc = idx[i + 2] + 1;
				sb.setLength(0);
				sb.append("f ").append(a).append("//").append(a).append(' ').append(b).append("//").append(b)
						.append(' ').append(cc).append("//").append(cc).append('\n');
				out.write(sb.toString());
			}
		}
	}

	/**
	 * Exports the iso surface as a binary STL file (e.g., for 3D printing).
	 * 
	 * @param file the *.stl file
	 * @throws IOException if the file can't be written
	 */
	public void exportStl(File file) throws IOException {
		ExportData e = exportData();
		float[] p = e.positions;
		int[] idx = e.indices;
		int triangles = idx.length / 3;
		ByteBuffer buffer = ByteBuffer.allocate(84 + triangles * 50).order(ByteOrder.LITTLE_ENDIAN);
		byte[] header = new byte[80];
		byte[] text = ("Bio7 iso surface of " + e.title + ", unit " + e.unit).getBytes(StandardCharsets.US_ASCII);
		/* A header must not start with "solid" (ASCII STL)! */
		System.arraycopy(text, 0, header, 0, Math.min(80, text.length));
		buffer.put(header);
		buffer.putInt(triangles);
		for (int i = 0; i < idx.length; i += 3) {
			int a = idx[i] * 3, b = idx[i + 1] * 3, c = idx[i + 2] * 3;
			float ux = p[b] - p[a], uy = p[b + 1] - p[a + 1], uz = p[b + 2] - p[a + 2];
			float vx = p[c] - p[a], vy = p[c + 1] - p[a + 1], vz = p[c + 2] - p[a + 2];
			float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
			float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (l > 0) {
				nx /= l;
				ny /= l;
				nz /= l;
			}
			buffer.putFloat(nx).putFloat(ny).putFloat(nz);
			for (int k : new int[] { a, b, c }) {
				buffer.putFloat(p[k]).putFloat(p[k + 1]).putFloat(p[k + 2]);
			}
			buffer.putShort((short) 0);
		}
		Files.write(file.toPath(), buffer.array());
	}

	/**
	 * Exports the iso surface as a binary PLY file with normals and vertex colors
	 * (e.g., for MeshLab, CloudCompare or ParaView).
	 * 
	 * @param file the *.ply file
	 * @throws IOException if the file can't be written
	 */
	public void exportPly(File file) throws IOException {
		ExportData e = exportData();
		float[] p = e.positions;
		float[] n = e.normals;
		int[] idx = e.indices;
		int vertices = p.length / 3;
		int triangles = idx.length / 3;
		String header = "ply\nformat binary_little_endian 1.0\ncomment Bio7 iso surface of " + e.title.replace('\n', ' ')
				+ ", iso value " + e.isoValue + ", unit " + e.unit + "\nelement vertex " + vertices
				+ "\nproperty float x\nproperty float y\nproperty float z\nproperty float nx\nproperty float ny\nproperty float nz\nproperty uchar red\nproperty uchar green\nproperty uchar blue\nelement face "
				+ triangles + "\nproperty list uchar int vertex_indices\nend_header\n";
		byte[] head = header.getBytes(StandardCharsets.US_ASCII);
		ByteBuffer buffer = ByteBuffer.allocate(head.length + vertices * 27 + triangles * 13)
				.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(head);
		byte r = (byte) Math.round(e.color[0] * 255), g = (byte) Math.round(e.color[1] * 255),
				b = (byte) Math.round(e.color[2] * 255);
		for (int i = 0; i < p.length; i += 3) {
			buffer.putFloat(p[i]).putFloat(p[i + 1]).putFloat(p[i + 2]);
			buffer.putFloat(n[i]).putFloat(n[i + 1]).putFloat(n[i + 2]);
			buffer.put(r).put(g).put(b);
		}
		for (int i = 0; i < idx.length; i += 3) {
			buffer.put((byte) 3).putInt(idx[i]).putInt(idx[i + 1]).putInt(idx[i + 2]);
		}
		Files.write(file.toPath(), buffer.array());
	}

	/**
	 * Exports the iso surface as a binary glTF 2.0 file (*.glb) with normals and
	 * a material (e.g., for Blender or web viewers).
	 * 
	 * @param file the *.glb file
	 * @throws IOException if the file can't be written
	 */
	public void exportGlb(File file) throws IOException {
		ExportData e = exportData();
		float[] p = e.positions;
		float[] n = e.normals;
		int[] idx = e.indices;
		int vertices = p.length / 3;
		float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		float[] max = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
		for (int i = 0; i < p.length; i += 3) {
			for (int k = 0; k < 3; k++) {
				min[k] = Math.min(min[k], p[i + k]);
				max[k] = Math.max(max[k], p[i + k]);
			}
		}
		int positionBytes = p.length * 4;
		int normalBytes = n.length * 4;
		int indexBytes = idx.length * 4;
		int binLength = positionBytes + normalBytes + indexBytes;
		float[] c = e.color;
		String json = "{\"asset\":{\"version\":\"2.0\",\"generator\":\"Bio7 iso surface\"},"
				+ "\"scene\":0,\"scenes\":[{\"nodes\":[0]}],\"nodes\":[{\"mesh\":0,\"name\":\"" + jsonText(e.title) + "\"}],"
				+ "\"meshes\":[{\"name\":\"isosurface\",\"primitives\":[{\"attributes\":{\"POSITION\":0,\"NORMAL\":1},\"indices\":2,\"material\":0,\"mode\":4}]}],"
				+ "\"materials\":[{\"name\":\"isosurface\",\"doubleSided\":true,"
				+ (e.opacity < 1 ? "\"alphaMode\":\"BLEND\"," : "")
				+ "\"pbrMetallicRoughness\":{\"baseColorFactor\":[" + c[0] + "," + c[1] + "," + c[2] + "," + e.opacity
				+ "],\"metallicFactor\":0.0,\"roughnessFactor\":0.5}}],"
				+ "\"buffers\":[{\"byteLength\":" + binLength + "}],"
				+ "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":" + positionBytes + ",\"target\":34962},"
				+ "{\"buffer\":0,\"byteOffset\":" + positionBytes + ",\"byteLength\":" + normalBytes + ",\"target\":34962},"
				+ "{\"buffer\":0,\"byteOffset\":" + (positionBytes + normalBytes) + ",\"byteLength\":" + indexBytes
				+ ",\"target\":34963}],"
				+ "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":" + vertices
				+ ",\"type\":\"VEC3\",\"min\":[" + min[0] + "," + min[1] + "," + min[2] + "],\"max\":[" + max[0] + ","
				+ max[1] + "," + max[2] + "]}," + "{\"bufferView\":1,\"componentType\":5126,\"count\":" + vertices
				+ ",\"type\":\"VEC3\"}," + "{\"bufferView\":2,\"componentType\":5125,\"count\":" + idx.length
				+ ",\"type\":\"SCALAR\"}]}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		/* The chunks are aligned to 4 bytes (JSON with spaces)! */
		int jsonLength = (jsonBytes.length + 3) & ~3;
		int total = 12 + 8 + jsonLength + 8 + binLength;
		ByteBuffer buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(0x46546C67).putInt(2).putInt(total);
		buffer.putInt(jsonLength).putInt(0x4E4F534A);
		buffer.put(jsonBytes);
		for (int i = jsonBytes.length; i < jsonLength; i++) {
			buffer.put((byte) ' ');
		}
		buffer.putInt(binLength).putInt(0x004E4942);
		for (float v : p) {
			buffer.putFloat(v);
		}
		for (float v : n) {
			buffer.putFloat(v);
		}
		for (int v : idx) {
			buffer.putInt(v);
		}
		Files.write(file.toPath(), buffer.array());
	}

	private static String jsonText(String text) {
		StringBuilder sb = new StringBuilder();
		for (char ch : text.toCharArray()) {
			if (ch == '"' || ch == '\\') {
				sb.append('\\').append(ch);
			} else if (ch < 0x20) {
				sb.append(' ');
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * Exports the iso surface as a binary legacy VTK file (polygonal data with
	 * normals, e.g., for ParaView).
	 * 
	 * @param file the *.vtk file
	 * @throws IOException if the file can't be written
	 */
	public void exportVtk(File file) throws IOException {
		ExportData e = exportData();
		float[] p = e.positions;
		float[] n = e.normals;
		int[] idx = e.indices;
		int vertices = p.length / 3;
		int triangles = idx.length / 3;
		/* The binary legacy VTK format is big endian! */
		try (DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(file), 1 << 16))) {
			String title = ("Bio7 iso surface of " + e.title + ", unit " + e.unit).replace('\n', ' ');
			out.write(("# vtk DataFile Version 3.0\n" + (title.length() > 250 ? title.substring(0, 250) : title)
					+ "\nBINARY\nDATASET POLYDATA\nPOINTS " + vertices + " float\n").getBytes(StandardCharsets.US_ASCII));
			for (float v : p) {
				out.writeFloat(v);
			}
			out.write(("\nPOLYGONS " + triangles + " " + triangles * 4 + "\n").getBytes(StandardCharsets.US_ASCII));
			for (int i = 0; i < idx.length; i += 3) {
				out.writeInt(3);
				out.writeInt(idx[i]);
				out.writeInt(idx[i + 1]);
				out.writeInt(idx[i + 2]);
			}
			out.write(("\nPOINT_DATA " + vertices + "\nNORMALS normals float\n").getBytes(StandardCharsets.US_ASCII));
			for (float v : n) {
				out.writeFloat(v);
			}
			out.write('\n');
		}
	}

	/**
	 * Returns a description of the stack and the iso surface.
	 */
	public synchronized String getInfo() {
		if (data == null) {
			return "No stack loaded";
		}
		String title = image != null ? image.getTitle() : "";
		int vertices = positions != null ? positions.length / 3 : 0;
		int triangles = indices != null ? indices.length / 3 : 0;
		return title + ": " + nx + " x " + ny + " x " + nz + " voxels, " + vertices + " vertices, " + triangles
				+ " triangles";
	}

	public synchronized float getWidth() {
		return width;
	}

	public synchronized float getHeight() {
		return height;
	}

	/**
	 * Returns the depth (the slices multiplied with the calibrated voxel depth and
	 * the z scale).
	 */
	public synchronized float getDepth() {
		return depth * zScale;
	}

	public synchronized double getDisplayMin() {
		return displayMin;
	}

	public synchronized double getDisplayMax() {
		return displayMax;
	}

	public synchronized double getIsoValue() {
		return isoValue;
	}

	/**
	 * Sets the iso value (in the units of the image) and computes the surface
	 * again.
	 */
	public void setIsoValue(double isoValue) {
		synchronized (this) {
			this.isoValue = isoValue;
		}
		update();
	}

	public float getSmoothing() {
		return smoothing;
	}

	/**
	 * The sigma (voxels) of the Gaussian smoothing of the stack (0 = off).
	 */
	public void setSmoothing(float smoothing) {
		synchronized (this) {
			this.smoothing = Math.max(0, smoothing);
		}
		update();
	}

	public int getMeshSmoothing() {
		return meshSmoothing;
	}

	/**
	 * The number of Taubin smoothing iterations of the mesh (0 = off).
	 */
	public void setMeshSmoothing(int iterations) {
		synchronized (this) {
			this.meshSmoothing = Math.max(0, iterations);
		}
		update();
	}

	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * The maximum number of voxels of a dimension (applied when a stack is
	 * loaded).
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = Math.max(8, maxSize);
	}

	public float getZScale() {
		return zScale;
	}

	public synchronized void setZScale(float zScale) {
		this.zScale = Math.max(0.01f, zScale);
	}

	public float getOpacity() {
		return opacity;
	}

	public void setOpacity(float opacity) {
		this.opacity = Math.max(0.05f, Math.min(1, opacity));
	}

	public boolean isShow() {
		return show;
	}

	public void setShow(boolean show) {
		this.show = show;
	}

	public boolean isHeadLight() {
		return headLight;
	}

	/**
	 * A light from the viewer (else the lights of the scene).
	 */
	public void setHeadLight(boolean headLight) {
		this.headLight = headLight;
	}

	public boolean isLutColor() {
		return lutColor;
	}

	/**
	 * Use the color of the LUT at the iso value (else the selected color).
	 */
	public void setLutColor(boolean lutColor) {
		this.lutColor = lutColor;
	}

	public void setColor(float r, float g, float b) {
		this.color = new float[] { r, g, b };
	}

	public float[] getColor() {
		return color.clone();
	}

	/**
	 * A listener which is called (in a background thread) when the surface has
	 * been computed.
	 */
	public void setUpdateListener(Runnable listener) {
		this.updateListener = listener;
	}

	/* Growable primitive lists! */
	static class FloatList {
		private float[] a = new float[3 * 1024];
		private int size;

		void add(float x, float y, float z) {
			if (size + 3 > a.length) {
				a = Arrays.copyOf(a, a.length * 2);
			}
			a[size++] = x;
			a[size++] = y;
			a[size++] = z;
		}

		float get(int i) {
			return a[i];
		}

		float[] toArray() {
			return Arrays.copyOf(a, size);
		}
	}

	static class IntList {
		private int[] a = new int[3 * 1024];
		private int size;

		void add(int x, int y, int z) {
			if (size + 3 > a.length) {
				a = Arrays.copyOf(a, a.length * 2);
			}
			a[size++] = x;
			a[size++] = y;
			a[size++] = z;
		}

		int[] toArray() {
			return Arrays.copyOf(a, size);
		}
	}
}
