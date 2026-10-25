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

import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jogamp.opengl.GL2;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;

/**
 * Clipping of the volume and the iso surface in the 3D view (to make the
 * interior visible):
 * <ul>
 * <li>A clipping box: the parts outside of the box are removed (relative
 * positions 0 - 1 in the extent of the displayed data).</li>
 * <li>A free cutting plane (azimuth, elevation and offset). The part on the
 * front side of the plane is removed (flip removes the other side).</li>
 * <li>A slice: the cross-section of the plane with the data (the colors of the
 * volume, see VolumeRenderer) and the outline of the plane.</li>
 * </ul>
 * The clipping uses the OpenGL clip planes (fast, only the display is
 * clipped). With setCutMesh the iso surface mesh is cut (closed with caps, the
 * export contains the cut surface).
 * <p>
 * Voxels can be removed with a mask (cuts with an ImageJ selection, a mask
 * image or an array, see cutRoi and setMask). The mask is applied to the volume
 * and to the mesh of the iso surface.
 */
public class Clipping {

	/** The mouse rotates the view (no tool). */
	public static final int TOOL_NONE = 0;
	/** The mouse moves the cutting plane (Shift: rotates the plane). */
	public static final int TOOL_PLANE = 1;
	/** The mouse removes voxels along the view direction (Shift: restores voxels). */
	public static final int TOOL_ERASER = 2;
	/** A mouse click picks a point on the displayed data (see setPickListener). */
	public static final int TOOL_PICK = 3;

	/* The maximum size of a mask (per axis) */
	private static final int MAX_MASK_SIZE = 512;
	private static final int MAX_UNDO = 20;

	/**
	 * The removed voxels (immutable). The mask has its own resolution and is
	 * sampled with relative positions (0 - 1, the first row is y = 0 like in
	 * ImageJ).
	 */
	public static final class Mask {
		private final int width;
		private final int height;
		private final int depth;
		private final BitSet removed;

		private Mask(int width, int height, int depth, BitSet removed) {
			this.width = width;
			this.height = height;
			this.depth = depth;
			this.removed = removed;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public int getDepth() {
			return depth;
		}

		/** Returns true if the voxel (x, y, z of the mask) is removed. */
		public boolean isRemoved(int x, int y, int z) {
			return removed.get((z * height + y) * width + x);
		}

		/**
		 * Returns true if the voxel at the relative position (0 - 1) is removed.
		 */
		public boolean isRemoved(double rx, double ry, double rz) {
			int x = Math.max(0, Math.min(width - 1, (int) (rx * width)));
			int y = Math.max(0, Math.min(height - 1, (int) (ry * height)));
			int z = Math.max(0, Math.min(depth - 1, (int) (rz * depth)));
			return removed.get((z * height + y) * width + x);
		}

		/** Returns the number of removed voxels. */
		public int getRemovedCount() {
			return removed.cardinality();
		}
	}

	private boolean boxEnabled = false;
	/* xmin, xmax, ymin, ymax, zmin, zmax (relative 0 - 1) */
	private final double[] box = { 0, 1, 0, 1, 0, 1 };
	private boolean planeEnabled = false;
	private double azimuth = 0; // degree, around the vertical (y) axis from +x
	private double elevation = 0; // degree, above the x-z plane
	private double offset = 0; // -1 - 1 (relative to the half diagonal of the box)
	private boolean flip = false;
	private boolean outline = true;
	private boolean slice = false;
	private boolean cutMesh = false;
	private Mask mask;
	private final Deque<Mask> undo = new ArrayDeque<>();
	private int maxPlanes = -1;
	private int activePlanes = 0;
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	/* The mouse tools and the view (matrices of the last frame) */
	private int tool = TOOL_NONE;
	private int eraserSize = 5;
	private double[] modelview;
	private double[] projection;
	private int[] viewport;
	private double[] viewBounds;
	private boolean viewFlip;
	private int[] dataSize;
	private double lastX, lastY;
	private boolean shiftDrag;
	/* The removed voxels of an eraser stroke (published as a mask while dragging) */
	private BitSet stroke;
	private int strokeW, strokeH, strokeD;
	private long lastPublish;
	private java.util.function.Consumer<double[][]> pickListener;

	/**
	 * The region of the clipping (the kept side of the box and the plane and the
	 * kept voxels of the mask) for relative positions (0 - 1, y = 0 is the first
	 * row like in ImageJ).
	 */
	public static final class Region {
		private final List<double[]> planes;
		private final Mask mask;
		private final double[] extent;
		private final boolean flip;

		private Region(List<double[]> planes, Mask mask, double[] extent, boolean flip) {
			this.planes = planes;
			this.mask = mask;
			this.extent = extent;
			this.flip = flip;
		}

		/** Returns true if the position is kept (inside of the region). */
		public boolean contains(double rx, double ry, double rz) {
			if (mask != null && mask.isRemoved(rx, ry, rz)) {
				return false;
			}
			if (planes.isEmpty()) {
				return true;
			}
			double x = rx * extent[0], y = flip ? ry * extent[1] : (1 - ry) * extent[1], z = rz * extent[2];
			for (double[] p : planes) {
				if (p[0] * x + p[1] * y + p[2] * z + p[3] < 0) {
					return false;
				}
			}
			return true;
		}

		/** Returns true if the whole data is kept. */
		public boolean isAll() {
			return planes.isEmpty() && mask == null;
		}
	}

	/**
	 * Returns the region of the clipping.
	 *
	 * @param extent the displayed size of the data (width, height, depth with the
	 *               z scale)
	 * @param flip   true if the first row of the data is at the bottom
	 * @return the region
	 */
	public synchronized Region getRegion(double[] extent, boolean flip) {
		List<double[]> planes = getPlanes(new double[] { 0, extent[0], 0, extent[1], 0, extent[2] });
		return new Region(planes, mask, extent.clone(), flip);
	}

	/**
	 * Sets the listener of the pick tool (TOOL_PICK). The listener gets the view
	 * ray through the data: the start and the end point (relative positions 0 - 1,
	 * y = 0 is the first row) and is not called in the user interface thread.
	 */
	public void setPickListener(java.util.function.Consumer<double[][]> listener) {
		pickListener = listener;
	}

	/**
	 * Adds a listener which is called after a change of the clipping (not in the
	 * user interface thread).
	 */
	public void addChangeListener(Runnable listener) {
		listeners.add(listener);
	}

	public void removeChangeListener(Runnable listener) {
		listeners.remove(listener);
	}

	/* Called after a change (outside of the synchronized methods)! */
	private void changed() {
		for (Runnable listener : listeners) {
			listener.run();
		}
	}

	/**
	 * Returns true if a clipping is enabled.
	 */
	public synchronized boolean isActive() {
		return boxEnabled || planeEnabled;
	}

	public synchronized boolean isBoxEnabled() {
		return boxEnabled;
	}

	public void setBoxEnabled(boolean enabled) {
		synchronized (this) {
			boxEnabled = enabled;
		}
		changed();
	}

	/**
	 * Sets the clipping box (relative positions 0 - 1 in the extent of the
	 * displayed data, y is the vertical axis).
	 */
	public void setBox(double xmin, double xmax, double ymin, double ymax, double zmin, double zmax) {
		double[] v = { xmin, xmax, ymin, ymax, zmin, zmax };
		synchronized (this) {
			for (int i = 0; i < 6; i += 2) {
				box[i] = clamp(Math.min(v[i], v[i + 1]));
				box[i + 1] = clamp(Math.max(v[i], v[i + 1]));
			}
			boxEnabled = true;
		}
		changed();
	}

	/** Returns the box (xmin, xmax, ymin, ymax, zmin, zmax relative 0 - 1). */
	public synchronized double[] getBox() {
		return box.clone();
	}

	/** Sets the minimum or maximum of an axis (axis 0 = x, 1 = y, 2 = z). */
	public void setBoxLimit(int axis, boolean max, double value) {
		synchronized (this) {
			box[axis * 2 + (max ? 1 : 0)] = clamp(value);
		}
		changed();
	}

	public synchronized boolean isPlaneEnabled() {
		return planeEnabled;
	}

	public void setPlaneEnabled(boolean enabled) {
		synchronized (this) {
			planeEnabled = enabled;
		}
		changed();
	}

	/**
	 * Sets the cutting plane.
	 *
	 * @param azimuth   the direction of the normal around the vertical axis
	 *                  (degree, 0 = +x)
	 * @param elevation the elevation of the normal (degree, 90 = upwards)
	 * @param offset    the position along the normal (-1 - 1, 0 = the center)
	 */
	public void setPlane(double azimuth, double elevation, double offset) {
		synchronized (this) {
			this.azimuth = azimuth;
			this.elevation = Math.max(-90, Math.min(90, elevation));
			this.offset = Math.max(-1, Math.min(1, offset));
			planeEnabled = true;
		}
		changed();
	}

	public synchronized double[] getPlane() {
		return new double[] { azimuth, elevation, offset };
	}

	public synchronized boolean isFlip() {
		return flip;
	}

	/** Removes the other side of the plane. */
	public void setFlip(boolean flip) {
		synchronized (this) {
			this.flip = flip;
		}
		changed();
	}

	public synchronized boolean isOutline() {
		return outline;
	}

	/** Draws the outline of the cutting plane. */
	public synchronized void setOutline(boolean outline) {
		this.outline = outline;
	}

	public synchronized boolean isSlice() {
		return slice;
	}

	/** Displays the cross-section of the plane with the data (see VolumeRenderer). */
	public synchronized void setSlice(boolean slice) {
		this.slice = slice;
	}

	public synchronized boolean isCutMesh() {
		return cutMesh;
	}

	/**
	 * Cuts the mesh of the iso surface with the box and the plane (computed again,
	 * the cut surface is closed with caps and is exported). Else only the display
	 * is clipped.
	 */
	public void setCutMesh(boolean cutMesh) {
		synchronized (this) {
			this.cutMesh = cutMesh;
		}
		changed();
	}

	public synchronized int getTool() {
		return tool;
	}

	/**
	 * Sets the mouse tool of the 3D view (the left mouse button): TOOL_NONE,
	 * TOOL_PLANE or TOOL_ERASER.
	 */
	public void setTool(int tool) {
		synchronized (this) {
			this.tool = tool;
		}
		changed();
	}

	public synchronized int getEraserSize() {
		return eraserSize;
	}

	/** Sets the radius of the eraser (in voxels of the mask). */
	public synchronized void setEraserSize(int size) {
		eraserSize = Math.max(1, size);
	}

	/**
	 * Sets the size of the displayed data (the resolution of a new mask of the
	 * eraser).
	 */
	public synchronized void setDataSize(int w, int h, int d) {
		dataSize = w > 0 && h > 0 && d > 0 ? new int[] { w, h, d } : null;
	}

	/**
	 * Stores the matrices of the view for the mouse tools (if a tool is selected).
	 *
	 * @param gl     the OpenGL context (the scene coordinates of the current model
	 *               view matrix)
	 * @param bounds the extent of the data (x0, x1, y0, y1, z0, z1) or null
	 * @param flip   true if the first row of the data is at the bottom
	 */
	public synchronized void setView(GL2 gl, double[] bounds, boolean flip) {
		if (tool == TOOL_NONE || bounds == null) {
			viewBounds = null;
			return;
		}
		double[] m = new double[16];
		double[] p = new double[16];
		int[] v = new int[4];
		gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, m, 0);
		gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, p, 0);
		gl.glGetIntegerv(GL2.GL_VIEWPORT, v, 0);
		modelview = m;
		projection = p;
		viewport = v;
		viewBounds = bounds.clone();
		viewFlip = flip;
	}

	/**
	 * Starts a drag of the mouse tool.
	 *
	 * @param x     the x position (pixels of the OpenGL surface, from the left)
	 * @param y     the y position (pixels of the OpenGL surface, from the top)
	 * @param shift true if the shift key is pressed
	 * @return true if the tool handles the drag (else the view is rotated)
	 */
	public boolean toolPressed(double x, double y, boolean shift) {
		double[][] pickRay = null;
		synchronized (this) {
			if (tool == TOOL_NONE || viewBounds == null) {
				return false;
			}
			if (tool == TOOL_PICK) {
				pickRay = relativeRay(x, y);
			}
		}
		if (tool == TOOL_PICK) {
			java.util.function.Consumer<double[][]> listener = pickListener;
			if (pickRay != null && listener != null) {
				listener.accept(pickRay);
			}
			return true;
		}
		synchronized (this) {
			lastX = x;
			lastY = y;
			shiftDrag = shift;
			if (tool == TOOL_ERASER) {
				int w, h, d;
				if (mask != null) {
					w = mask.width;
					h = mask.height;
					d = mask.depth;
				} else if (dataSize != null) {
					int sx = step(dataSize[0]), sy = step(dataSize[1]), sz = step(dataSize[2]);
					w = (dataSize[0] + sx - 1) / sx;
					h = (dataSize[1] + sy - 1) / sy;
					d = (dataSize[2] + sz - 1) / sz;
				} else {
					return false;
				}
				stroke = combinable(w, h, d);
				strokeW = w;
				strokeH = h;
				strokeD = d;
				pushUndo();
				erase(x, y);
				lastPublish = 0;
			}
		}
		toolDragged(x, y);
		return true;
	}

	/** Continues the drag of the mouse tool (see toolPressed). */
	public void toolDragged(double x, double y) {
		synchronized (this) {
			if (viewBounds == null) {
				return;
			}
			double dx = x - lastX, dy = y - lastY;
			lastX = x;
			lastY = y;
			if (tool == TOOL_PLANE) {
				if (shiftDrag) {
					azimuth = (azimuth + dx * 0.5 + 360) % 360;
					elevation = Math.max(-90, Math.min(90, elevation - dy * 0.5));
				} else {
					offset = Math.max(-1, Math.min(1, offset + planeOffsetDelta(dx, dy)));
				}
				planeEnabled = true;
			} else if (tool == TOOL_ERASER && stroke != null) {
				erase(x, y);
				/* Published not more often than every 100 ms */
				long now = System.currentTimeMillis();
				if (now - lastPublish < 100) {
					return;
				}
				lastPublish = now;
				mask = new Mask(strokeW, strokeH, strokeD, (BitSet) stroke.clone());
			} else {
				return;
			}
		}
		changed();
	}

	/** Ends the drag of the mouse tool. */
	public void toolReleased() {
		synchronized (this) {
			if (stroke == null) {
				return;
			}
			mask = new Mask(strokeW, strokeH, strokeD, stroke);
			stroke = null;
		}
		changed();
	}

	/* The change of the plane offset for a mouse movement (along the normal on the screen)! */
	private double planeOffsetDelta(double dx, double dy) {
		double[] b = viewBounds;
		double az = Math.toRadians(azimuth), el = Math.toRadians(elevation);
		double[] n = { Math.cos(el) * Math.cos(az), Math.sin(el), Math.cos(el) * Math.sin(az) };
		double[] c = { (b[0] + b[1]) / 2, (b[2] + b[3]) / 2, (b[4] + b[5]) / 2 };
		double half = 0.5 * Math.sqrt(Math.pow(b[1] - b[0], 2) + Math.pow(b[3] - b[2], 2) + Math.pow(b[5] - b[4], 2));
		double[] p0 = new double[3], p1 = new double[3];
		for (int k = 0; k < 3; k++) {
			p0[k] = c[k] + n[k] * half * (offset - 0.5);
			p1[k] = c[k] + n[k] * half * (offset + 0.5);
		}
		double[] s0 = project(p0), s1 = project(p1);
		if (s0 == null || s1 == null) {
			return 0;
		}
		/* The screen vector of one unit of the offset */
		double vx = s1[0] - s0[0], vy = s1[1] - s0[1];
		double len2 = vx * vx + vy * vy;
		if (len2 < 1) {
			return 0; // the normal points to the viewer
		}
		return (dx * vx + dy * vy) / len2;
	}

	/* The part of the view ray inside of the bounds in relative coordinates (start, end) or null! */
	private double[][] relativeRay(double x, double y) {
		double[] a = unproject(x, y, 0), e = unproject(x, y, 1);
		if (a == null || e == null) {
			return null;
		}
		double[] b = viewBounds;
		double t0 = 0, t1 = 1;
		for (int k = 0; k < 3; k++) {
			double dk = e[k] - a[k];
			if (Math.abs(dk) < 1e-15) {
				if (a[k] < b[k * 2] || a[k] > b[k * 2 + 1]) {
					return null;
				}
				continue;
			}
			double ta = (b[k * 2] - a[k]) / dk, tb = (b[k * 2 + 1] - a[k]) / dk;
			t0 = Math.max(t0, Math.min(ta, tb));
			t1 = Math.min(t1, Math.max(ta, tb));
		}
		if (t0 > t1) {
			return null;
		}
		double[][] ray = new double[2][];
		double[] ts = { t0, t1 };
		for (int i = 0; i < 2; i++) {
			double px = a[0] + (e[0] - a[0]) * ts[i], py = a[1] + (e[1] - a[1]) * ts[i],
					pz = a[2] + (e[2] - a[2]) * ts[i];
			double ry = (py - b[2]) / (b[3] - b[2]);
			ray[i] = new double[] { (px - b[0]) / (b[1] - b[0]), viewFlip ? ry : 1 - ry, (pz - b[4]) / (b[5] - b[4]) };
		}
		return ray;
	}

	/* Removes (with shift restores) the voxels around the view ray through the position! */
	private void erase(double x, double y) {
		double[] a = unproject(x, y, 0), e = unproject(x, y, 1);
		if (a == null || e == null) {
			return;
		}
		double[] dir = { e[0] - a[0], e[1] - a[1], e[2] - a[2] };
		double len = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
		if (len == 0) {
			return;
		}
		for (int k = 0; k < 3; k++) {
			dir[k] /= len;
		}
		double[] b = viewBounds;
		/* The part of the ray inside of the bounds (slab method) */
		double t0 = -Double.MAX_VALUE, t1 = Double.MAX_VALUE;
		for (int k = 0; k < 3; k++) {
			if (Math.abs(dir[k]) < 1e-12) {
				if (a[k] < b[k * 2] || a[k] > b[k * 2 + 1]) {
					return;
				}
				continue;
			}
			double ta = (b[k * 2] - a[k]) / dir[k], tb = (b[k * 2 + 1] - a[k]) / dir[k];
			t0 = Math.max(t0, Math.min(ta, tb));
			t1 = Math.min(t1, Math.max(ta, tb));
		}
		if (t0 > t1) {
			return;
		}
		int[] size = { strokeW, strokeH, strokeD };
		/* The size of a voxel of the mask in scene units */
		double[] voxel = { (b[1] - b[0]) / size[0], (b[3] - b[2]) / size[1], (b[5] - b[4]) / size[2] };
		double minVoxel = Math.min(voxel[0], Math.min(voxel[1], voxel[2]));
		double radius = eraserSize * Math.max(voxel[0], voxel[1]);
		double r2 = radius * radius;
		int[] reach = new int[3];
		for (int k = 0; k < 3; k++) {
			reach[k] = (int) Math.ceil(radius / voxel[k]);
		}
		double stepT = Math.max(minVoxel, radius * 0.5);
		for (double t = t0; t <= t1 + stepT; t += stepT) {
			double tt = Math.min(t, t1);
			double px = a[0] + dir[0] * tt, py = a[1] + dir[1] * tt, pz = a[2] + dir[2] * tt;
			/* The voxel of the point (the first row at the top if not flipped) */
			int ci = (int) Math.floor((px - b[0]) / voxel[0]);
			int cj = (int) Math.floor((viewFlip ? py - b[2] : b[3] - py) / voxel[1]);
			int ck = (int) Math.floor((pz - b[4]) / voxel[2]);
			for (int k = Math.max(0, ck - reach[2]); k <= Math.min(size[2] - 1, ck + reach[2]); k++) {
				double vz = b[4] + (k + 0.5) * voxel[2] - pz;
				for (int j = Math.max(0, cj - reach[1]); j <= Math.min(size[1] - 1, cj + reach[1]); j++) {
					double rowY = (j + 0.5) * voxel[1];
					double vy = (viewFlip ? b[2] + rowY : b[3] - rowY) - py;
					for (int i = Math.max(0, ci - reach[0]); i <= Math.min(size[0] - 1, ci + reach[0]); i++) {
						double vx = b[0] + (i + 0.5) * voxel[0] - px;
						if (vx * vx + vy * vy + vz * vz <= r2) {
							stroke.set((k * size[1] + j) * size[0] + i, !shiftDrag);
						}
					}
				}
			}
		}
	}

	/* The scene coordinates of a window position (y from the top) at the depth 0 - 1! */
	private double[] unproject(double x, double y, double depth) {
		double[] inv = invert(multiply(projection, modelview));
		if (inv == null) {
			return null;
		}
		double[] ndc = { 2 * (x - viewport[0]) / viewport[2] - 1,
				2 * ((viewport[3] - y) - viewport[1]) / viewport[3] - 1, 2 * depth - 1, 1 };
		double[] o = transform(inv, ndc);
		if (Math.abs(o[3]) < 1e-15) {
			return null;
		}
		return new double[] { o[0] / o[3], o[1] / o[3], o[2] / o[3] };
	}

	/* The window position (y from the top) of a point in scene coordinates! */
	private double[] project(double[] p) {
		double[] c = transform(multiply(projection, modelview), new double[] { p[0], p[1], p[2], 1 });
		if (c[3] <= 0) {
			return null;
		}
		double nx = c[0] / c[3], ny = c[1] / c[3];
		return new double[] { viewport[0] + (nx + 1) / 2 * viewport[2],
				viewport[3] - (viewport[1] + (ny + 1) / 2 * viewport[3]) };
	}

	/* Matrices in the column major order of OpenGL! */
	static double[] multiply(double[] a, double[] b) {
		double[] r = new double[16];
		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 4; row++) {
				double sum = 0;
				for (int k = 0; k < 4; k++) {
					sum += a[k * 4 + row] * b[col * 4 + k];
				}
				r[col * 4 + row] = sum;
			}
		}
		return r;
	}

	static double[] transform(double[] m, double[] v) {
		double[] r = new double[4];
		for (int row = 0; row < 4; row++) {
			r[row] = m[row] * v[0] + m[4 + row] * v[1] + m[8 + row] * v[2] + m[12 + row] * v[3];
		}
		return r;
	}

	/* The inverse (Gauss-Jordan) or null! */
	private static double[] invert(double[] m) {
		double[][] a = new double[4][8];
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				a[row][col] = m[col * 4 + row];
			}
			a[row][4 + row] = 1;
		}
		for (int col = 0; col < 4; col++) {
			int pivot = col;
			for (int row = col + 1; row < 4; row++) {
				if (Math.abs(a[row][col]) > Math.abs(a[pivot][col])) {
					pivot = row;
				}
			}
			if (Math.abs(a[pivot][col]) < 1e-15) {
				return null;
			}
			double[] tmp = a[col];
			a[col] = a[pivot];
			a[pivot] = tmp;
			double f = a[col][col];
			for (int k = 0; k < 8; k++) {
				a[col][k] /= f;
			}
			for (int row = 0; row < 4; row++) {
				if (row != col && a[row][col] != 0) {
					double g = a[row][col];
					for (int k = 0; k < 8; k++) {
						a[row][k] -= g * a[col][k];
					}
				}
			}
		}
		double[] r = new double[16];
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				r[col * 4 + row] = a[row][4 + col];
			}
		}
		return r;
	}

	/** Resets the box, the plane (disabled), the mesh cut and removes the mask. */
	public void reset() {
		synchronized (this) {
			boxEnabled = false;
			planeEnabled = false;
			for (int i = 0; i < 6; i++) {
				box[i] = i % 2;
			}
			azimuth = 0;
			elevation = 0;
			offset = 0;
			flip = false;
			slice = false;
			cutMesh = false;
			tool = TOOL_NONE;
			if (mask != null) {
				pushUndo();
			}
			mask = null;
		}
		changed();
	}

	/** Returns the mask of the removed voxels or null. */
	public synchronized Mask getMask() {
		return mask;
	}

	/**
	 * Removes the voxels inside (or outside) of the selection of the image in all
	 * slices (the cuts are combined, see undoCut and clearMask).
	 *
	 * @param imp    the image with the selection (the size of the stack of the
	 *               volume or iso surface)
	 * @param inside true removes the inside, false the outside of the selection
	 * @return false if the image has no area selection
	 */
	public boolean cutRoi(ImagePlus imp, boolean inside) {
		return cutRoi(imp, inside, 1, Integer.MAX_VALUE);
	}

	/**
	 * Removes the voxels inside (or outside) of the selection of the image in a
	 * range of slices (the cuts are combined, see undoCut and clearMask).
	 *
	 * @param imp        the image with the selection (the size of the stack of the
	 *                   volume or iso surface)
	 * @param inside     true removes the inside, false the outside of the
	 *                   selection
	 * @param firstSlice the first slice (1 - n)
	 * @param lastSlice  the last slice (1 - n, larger values are the last slice)
	 * @return false if the image has no area selection
	 */
	public boolean cutRoi(ImagePlus imp, boolean inside, int firstSlice, int lastSlice) {
		if (imp == null) {
			return false;
		}
		Roi roi = imp.getRoi();
		if (roi == null || !roi.isArea()) {
			return false;
		}
		return cutRoi(roi, imp.getWidth(), imp.getHeight(), imp.getNSlices(), inside, firstSlice, lastSlice);
	}

	/**
	 * Removes the voxels inside (or outside) of a selection (in the pixel
	 * coordinates of a stack with the given size) in a range of slices.
	 *
	 * @param roi        the area selection
	 * @param w          the width of the stack
	 * @param h          the height of the stack
	 * @param d          the number of slices of the stack
	 * @param inside     true removes the inside, false the outside of the
	 *                   selection
	 * @param firstSlice the first slice (1 - n)
	 * @param lastSlice  the last slice (1 - n, larger values are the last slice)
	 * @return false if the selection is not an area
	 */
	public boolean cutRoi(Roi roi, int w, int h, int d, boolean inside, int firstSlice, int lastSlice) {
		if (roi == null || !roi.isArea() || w < 1 || h < 1 || d < 1) {
			return false;
		}
		int sx = step(w), sy = step(h), sz = step(d);
		int mw = (w + sx - 1) / sx, mh = (h + sy - 1) / sy, md = (d + sz - 1) / sz;
		/* The selection in one plane of the mask (true = in the selection) */
		Rectangle r = roi.getBounds();
		ImageProcessor roiMask = roi.getMask();
		boolean[] plane = new boolean[mw * mh];
		for (int y = 0; y < mh; y++) {
			int py = y * sy;
			for (int x = 0; x < mw; x++) {
				int px = x * sx;
				boolean in = px >= r.x && py >= r.y && px < r.x + r.width && py < r.y + r.height
						&& (roiMask == null || roiMask.get(px - r.x, py - r.y) != 0);
				plane[y * mw + x] = in == inside;
			}
		}
		int z0 = Math.max(0, (Math.max(1, firstSlice) - 1) / sz);
		int z1 = Math.min(md - 1, (int) ((Math.min((long) lastSlice, d) - 1) / sz));
		synchronized (this) {
			BitSet removed = combinable(mw, mh, md);
			for (int z = z0; z <= z1; z++) {
				for (int i = 0; i < plane.length; i++) {
					if (plane[i]) {
						removed.set(z * plane.length + i);
					}
				}
			}
			pushUndo();
			mask = new Mask(mw, mh, md, removed);
		}
		changed();
		return true;
	}

	/**
	 * Sets a mask image (a binary stack, e.g., of a segmentation): the voxels with
	 * the value 0 are removed (the current channel and frame). The previous cuts
	 * are replaced.
	 *
	 * @param imp the mask image (the size of the stack of the volume or iso
	 *            surface)
	 */
	public void setMask(ImagePlus imp) {
		if (imp == null || imp.getProcessor() == null) {
			return;
		}
		int w = imp.getWidth(), h = imp.getHeight(), d = imp.getNSlices();
		int sx = step(w), sy = step(h), sz = step(d);
		int mw = (w + sx - 1) / sx, mh = (h + sy - 1) / sy, md = (d + sz - 1) / sz;
		ImageStack stack = imp.getStack();
		BitSet removed = new BitSet(mw * mh * md);
		for (int z = 0; z < md; z++) {
			ImageProcessor ip = stack
					.getProcessor(imp.getStackIndex(imp.getChannel(), Math.min(d, z * sz + 1), imp.getFrame()));
			for (int y = 0; y < mh; y++) {
				for (int x = 0; x < mw; x++) {
					if (ip.getf(x * sx, y * sy) == 0) {
						removed.set((z * mh + y) * mw + x);
					}
				}
			}
		}
		setMask(new Mask(mw, mh, md, removed));
	}

	/**
	 * Sets a mask (e.g., of a model): the voxels with false are removed. The
	 * previous cuts are replaced.
	 *
	 * @param keep the kept voxels (index = (z * height + y) * width + x)
	 * @param w    the width
	 * @param h    the height
	 * @param d    the depth
	 */
	public void setMask(boolean[] keep, int w, int h, int d) {
		if (keep == null || w < 1 || h < 1 || d < 1 || keep.length < w * h * d) {
			throw new IllegalArgumentException("The size of the mask doesn't fit!");
		}
		BitSet removed = new BitSet(w * h * d);
		for (int i = 0; i < w * h * d; i++) {
			if (!keep[i]) {
				removed.set(i);
			}
		}
		setMask(new Mask(w, h, d, removed));
	}

	private void setMask(Mask m) {
		synchronized (this) {
			pushUndo();
			mask = m;
		}
		changed();
	}

	/** Inverts the mask (the removed voxels are kept and vice versa). */
	public void invertMask() {
		synchronized (this) {
			if (mask == null) {
				return;
			}
			int n = mask.width * mask.height * mask.depth;
			BitSet removed = (BitSet) mask.removed.clone();
			removed.flip(0, n);
			pushUndo();
			mask = new Mask(mask.width, mask.height, mask.depth, removed);
		}
		changed();
	}

	/** Undoes the last change of the mask (a cut, setMask, invertMask or clearMask). */
	public void undoCut() {
		synchronized (this) {
			if (undo.isEmpty()) {
				return;
			}
			Mask previous = undo.pop();
			mask = previous.removed == null ? null : previous;
		}
		changed();
	}

	/** Returns true if a change of the mask can be undone. */
	public synchronized boolean canUndoCut() {
		return !undo.isEmpty();
	}

	/** Removes the mask (all cuts). */
	public void clearMask() {
		synchronized (this) {
			if (mask == null) {
				return;
			}
			pushUndo();
			mask = null;
		}
		changed();
	}

	/* Saves the current mask for undo (null as a mask without voxels)! */
	private void pushUndo() {
		undo.push(mask != null ? mask : new Mask(0, 0, 0, null));
		while (undo.size() > MAX_UNDO) {
			undo.removeLast();
		}
	}

	/* The removed voxels of the current mask (copy) if the size is equal, else empty! */
	private BitSet combinable(int w, int h, int d) {
		if (mask != null && mask.width == w && mask.height == h && mask.depth == d) {
			return (BitSet) mask.removed.clone();
		}
		return new BitSet(w * h * d);
	}

	private static int step(int size) {
		return (size + MAX_MASK_SIZE - 1) / MAX_MASK_SIZE;
	}

	/* The clipped box in scene coordinates (x0, x1, y0, y1, z0, z1)! */
	private double[] clippedBox(double[] bounds) {
		double[] b = new double[6];
		for (int a = 0; a < 3; a++) {
			double min = bounds[a * 2], size = bounds[a * 2 + 1] - min;
			double lo = boxEnabled ? box[a * 2] : 0, hi = boxEnabled ? box[a * 2 + 1] : 1;
			b[a * 2] = min + lo * size;
			b[a * 2 + 1] = min + hi * size;
		}
		return b;
	}

	/* The plane (a, b, c, d): kept are the points with a x + b y + c z + d >= 0! */
	private double[] planeEquation(double[] bounds) {
		double az = Math.toRadians(azimuth), el = Math.toRadians(elevation);
		double nx = Math.cos(el) * Math.cos(az), ny = Math.sin(el), nz = Math.cos(el) * Math.sin(az);
		double cx = (bounds[0] + bounds[1]) / 2, cy = (bounds[2] + bounds[3]) / 2, cz = (bounds[4] + bounds[5]) / 2;
		double half = 0.5 * Math.sqrt(Math.pow(bounds[1] - bounds[0], 2) + Math.pow(bounds[3] - bounds[2], 2)
				+ Math.pow(bounds[5] - bounds[4], 2));
		double px = cx + nx * offset * half, py = cy + ny * offset * half, pz = cz + nz * offset * half;
		// The front side (the side of the normal) is removed: keep n . (p0 - p) >= 0
		double s = flip ? 1 : -1;
		return new double[] { s * nx, s * ny, s * nz, -s * (nx * px + ny * py + nz * pz) };
	}

	/**
	 * Enables the clip planes for the following rendering (the scene coordinates
	 * of the current model view matrix).
	 *
	 * @param gl     the OpenGL context
	 * @param bounds the extent of the data (x0, x1, y0, y1, z0, z1)
	 */
	public synchronized void begin(GL2 gl, double[] bounds) {
		activePlanes = 0;
		if (!isActive() || bounds == null) {
			return;
		}
		if (maxPlanes < 0) {
			int[] max = new int[1];
			gl.glGetIntegerv(GL2.GL_MAX_CLIP_PLANES, max, 0);
			maxPlanes = max[0];
		}
		for (double[] p : getPlanes(bounds)) {
			if (activePlanes >= maxPlanes) {
				break;
			}
			gl.glClipPlane(GL2.GL_CLIP_PLANE0 + activePlanes, p, 0);
			gl.glEnable(GL2.GL_CLIP_PLANE0 + activePlanes);
			activePlanes++;
		}
	}

	/**
	 * Returns the planes of the box and of the cutting plane (if enabled). Kept are
	 * the points with a x + b y + c z + d >= 0 (the normal (a, b, c) has the
	 * length 1, the distance is in the units of the bounds).
	 *
	 * @param bounds the extent of the data (x0, x1, y0, y1, z0, z1)
	 * @return the planes (a, b, c, d)
	 */
	public synchronized List<double[]> getPlanes(double[] bounds) {
		List<double[]> planes = new ArrayList<>();
		if (bounds == null) {
			return planes;
		}
		if (boxEnabled) {
			double[] b = clippedBox(bounds);
			planes.add(new double[] { 1, 0, 0, -b[0] });
			planes.add(new double[] { -1, 0, 0, b[1] });
			planes.add(new double[] { 0, 1, 0, -b[2] });
			planes.add(new double[] { 0, -1, 0, b[3] });
			planes.add(new double[] { 0, 0, 1, -b[4] });
			planes.add(new double[] { 0, 0, -1, b[5] });
		}
		if (planeEnabled) {
			planes.add(planeEquation(bounds));
		}
		return planes;
	}

	/**
	 * Disables the clip planes.
	 */
	public synchronized void end(GL2 gl) {
		for (int i = 0; i < activePlanes; i++) {
			gl.glDisable(GL2.GL_CLIP_PLANE0 + i);
		}
		activePlanes = 0;
	}

	/**
	 * Returns the polygon of the cutting plane inside of the (clipped) box (the
	 * corners in scene coordinates, sorted around the center) or null.
	 *
	 * @param bounds the extent of the data (x0, x1, y0, y1, z0, z1)
	 * @return the polygon or null
	 */
	public synchronized List<double[]> planePolygon(double[] bounds) {
		if (!planeEnabled || bounds == null) {
			return null;
		}
		double[] b = clippedBox(bounds);
		double[] p = planeEquation(bounds);
		double[][] corners = new double[8][];
		for (int i = 0; i < 8; i++) {
			corners[i] = new double[] { b[(i & 1)], b[2 + ((i >> 1) & 1)], b[4 + ((i >> 2) & 1)] };
		}
		int[][] edges = { { 0, 1 }, { 2, 3 }, { 4, 5 }, { 6, 7 }, { 0, 2 }, { 1, 3 }, { 4, 6 }, { 5, 7 }, { 0, 4 },
				{ 1, 5 }, { 2, 6 }, { 3, 7 } };
		List<double[]> points = new ArrayList<>();
		for (int[] e : edges) {
			double[] a = corners[e[0]], c = corners[e[1]];
			double da = p[0] * a[0] + p[1] * a[1] + p[2] * a[2] + p[3];
			double dc = p[0] * c[0] + p[1] * c[1] + p[2] * c[2] + p[3];
			if ((da > 0) != (dc > 0) && Math.abs(da - dc) > 1e-12) {
				double t = da / (da - dc);
				points.add(new double[] { a[0] + t * (c[0] - a[0]), a[1] + t * (c[1] - a[1]), a[2] + t * (c[2] - a[2]) });
			}
		}
		if (points.size() < 3) {
			return null;
		}
		// Sort the points around the center (in the plane)
		double[] center = new double[3];
		for (double[] q : points) {
			for (int k = 0; k < 3; k++) {
				center[k] += q[k] / points.size();
			}
		}
		double[] n = { p[0], p[1], p[2] };
		double[] u = Math.abs(n[1]) < 0.9 ? cross(n, new double[] { 0, 1, 0 }) : cross(n, new double[] { 1, 0, 0 });
		double[] v = cross(n, u);
		points.sort((q1, q2) -> Double.compare(angle(q1, center, u, v), angle(q2, center, u, v)));
		return points;
	}

	/**
	 * Draws the outline of the cutting plane (if enabled).
	 */
	public synchronized void drawOutline(GL2 gl, double[] bounds) {
		if (!outline) {
			return;
		}
		List<double[]> polygon = planePolygon(bounds);
		if (polygon == null) {
			return;
		}
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glLineWidth(2);
		gl.glColor3f(1f, 0.85f, 0.1f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		for (double[] q : polygon) {
			gl.glVertex3d(q[0], q[1], q[2]);
		}
		gl.glEnd();
		gl.glPopAttrib();
	}

	private static double angle(double[] q, double[] c, double[] u, double[] v) {
		double dx = q[0] - c[0], dy = q[1] - c[1], dz = q[2] - c[2];
		return Math.atan2(dx * v[0] + dy * v[1] + dz * v[2], dx * u[0] + dy * u[1] + dz * u[2]);
	}

	private static double[] cross(double[] a, double[] b) {
		double[] c = { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
		double l = Math.sqrt(c[0] * c[0] + c[1] * c[1] + c[2] * c[2]);
		return new double[] { c[0] / l, c[1] / l, c[2] / l };
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(1, v));
	}
}
