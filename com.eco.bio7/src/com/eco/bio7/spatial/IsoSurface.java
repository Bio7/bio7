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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.plugin.LutLoader;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;

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
	/* Viridis (used if no color LUT is available)! */
	private static final int[] VIRIDIS = { 0x440154, 0x3b528b, 0x21918c, 0x5ec962, 0xfde725 };

	/** One color (the selected color or the LUT color at the iso value). */
	public static final int COLOR_SINGLE = 0;
	/** The intensity of the stack (or of a color channel) inside of the surface (LUT). */
	public static final int COLOR_INTENSITY = 1;
	/** The depth (z position) of the surface (LUT). */
	public static final int COLOR_DEPTH = 2;
	/** The displayed colors of the image inside of the surface (RGB, composite or LUT colors). */
	public static final int COLOR_IMAGE = 3;

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
	private final int[] buffers = new int[4];
	private boolean hasBuffers = false;
	private boolean hasColorBuffer = false;
	private boolean upload = false;
	private int indexCount = 0;

	/* Settings */
	private boolean show = true;
	private double isoValue = 128;
	private float smoothing = 0f;
	private int meshSmoothing = 5;
	private int maxSize = 256;
	/* The display is reduced to maxSize only if enabled (the measurements use the full resolution) */
	private boolean reduceDisplay = false;
	/* The full resolution surface for the measurements (see getMeasurementData) and its state */
	private MeasureData fullData;
	private Object fullKey;
	private float zScale = 1.0f;
	private float opacity = 1.0f;
	private boolean headLight = true;
	private boolean lutColor = true;
	private float[] color = DEFAULT_COLOR.clone();

	/* The vertex colors */
	private int colorMode = COLOR_SINGLE;
	private int colorChannel = 0;
	private float[] colorData;
	private int[] imageColors; // The displayed colors (RGB) of the stack
	private double colorMin;
	private double colorMax = 255;
	private ColorModel colorLut;
	private float[] vertexColors;
	/* The opacity of a vertex (opacity mapping of the transfer function) or null */
	private float[] vertexAlpha;
	private TransferFunction transferFunction;
	/* The color table index (0 - 255) of every vertex (for the histogram) or null */
	private int[] vertexIndices;
	private boolean opacityMapping = false;
	private boolean colorsDirty = false;
	/* The colors of the measured objects (see Measurement3d) for the mesh or null */
	private float[] objectColors;
	private int[] objectColorsMesh;
	private boolean isoExplicit = false;
	/* A color array set with setColorData (kept by setData) */
	private boolean customColorData = false;
	private boolean customColorRange = false;
	private int[] customColorTable;
	private float colorSampleDepth = 1;

	/* The cut of the mesh (planes and mask) and the state of the last computation */
	private Clipping clipping;
	private Object computedClip;

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
		/* Reduced only if enabled (else the full resolution) */
		int limit = reduceDisplay ? maxSize : Math.max(w, Math.max(h, d));
		int sx = (w + limit - 1) / limit;
		int sy = (h + limit - 1) / limit;
		int sz = (d + limit - 1) / limit;
		int tw = (w + sx - 1) / sx;
		int th = (h + sy - 1) / sy;
		int td = (d + sz - 1) / sz;
		int channel = imp.getChannel();
		float[] values = readChannel(imp, channel, sx, sy, sz, tw, th, td);
		/* The values of another channel for the intensity colors! */
		int cc = colorChannel;
		boolean otherChannel = cc > 0 && cc != channel && cc <= imp.getNChannels();
		float[] colorValues = otherChannel ? readChannel(imp, cc, sx, sy, sz, tw, th, td) : values;
		int[] displayColors = readDisplayColors(imp, sx, sy, sz, tw, th, td);
		ColorModel otherLut = null;
		double otherMin = 0;
		double otherMax = 255;
		if (otherChannel && imp instanceof CompositeImage) {
			LUT channelLut = ((CompositeImage) imp).getChannelLut(cc);
			otherLut = channelLut;
			otherMin = channelLut.min;
			otherMax = channelLut.max;
		} else if (otherChannel) {
			otherLut = imp.getProcessor().getColorModel();
			otherMin = imp.getDisplayRangeMin();
			otherMax = imp.getDisplayRangeMax();
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
			fullData = null;
			fullKey = null;
			/* A reload of the same stack keeps a selected iso value! */
			boolean keepIso = imp == image && isoExplicit;
			image = imp;
			if (!keepIso) {
				isoValue = iso;
			}
			isoExplicit = keepIso;
			colorData = colorValues;
			customColorData = false;
			customColorRange = false;
			imageColors = displayColors;
			colorLut = otherChannel ? otherLut : lut;
			colorMin = otherChannel ? otherMin : displayMin;
			colorMax = otherChannel ? otherMax : displayMax;
		}
		update();
		return true;
	}

	/* The values of a channel (current frame) of a stack with a reduced resolution! */
	private static float[] readChannel(ImagePlus imp, int channel, int sx, int sy, int sz, int tw, int th,
			int td) {
		float[] values = new float[tw * th * td];
		ImageStack stack = imp.getStack();
		int d = imp.getNSlices();
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
		return values;
	}

	/*
	 * The colors of the stack like displayed in ImageJ: the RGB colors, the merged
	 * colors of the active channels of a composite image (added like in ImageJ) or
	 * the LUT colors (display range) of the channel!
	 */
	private static int[] readDisplayColors(ImagePlus imp, int sx, int sy, int sz, int tw, int th, int td) {
		int[] colors = new int[tw * th * td];
		ImageStack stack = imp.getStack();
		int d = imp.getNSlices();
		int frame = imp.getFrame();
		CompositeImage ci = imp instanceof CompositeImage ? (CompositeImage) imp : null;
		boolean composite = ci != null && ci.getMode() == IJ.COMPOSITE;
		int channels = imp.getNChannels();
		boolean[] active = composite ? ci.getActiveChannels() : null;
		for (int c = 1; c <= channels; c++) {
			if (composite ? (active != null && c <= active.length && !active[c - 1]) : c != imp.getChannel()) {
				continue;
			}
			ColorModel cm;
			double min, max;
			if (ci != null) {
				LUT channelLut = ci.getChannelLut(c);
				cm = channelLut;
				min = channelLut.min;
				max = channelLut.max;
			} else {
				cm = imp.getProcessor().getColorModel();
				min = imp.getDisplayRangeMin();
				max = imp.getDisplayRangeMax();
			}
			double range = max > min ? max - min : 1;
			int[] table = new int[256];
			for (int i = 0; i < 256; i++) {
				table[i] = cm.getRGB(i) & 0xffffff;
			}
			for (int z = 0; z < td; z++) {
				ImageProcessor ip = stack.getProcessor(imp.getStackIndex(c, Math.min(d, z * sz + 1), frame));
				boolean rgb = ip instanceof ColorProcessor;
				for (int y = 0; y < th; y++) {
					for (int x = 0; x < tw; x++) {
						int color;
						if (rgb) {
							color = ip.getPixel(x * sx, y * sy) & 0xffffff;
						} else {
							double v = (ip.getf(x * sx, y * sy) - min) / range * 255.0;
							int index = Double.isNaN(v) ? 0 : (int) Math.max(0, Math.min(255, Math.round(v)));
							color = table[index];
						}
						int i = (z * th + y) * tw + x;
						/* Added and clamped (composite)! */
						int old = colors[i];
						int red = Math.min(255, ((old >> 16) & 0xff) + ((color >> 16) & 0xff));
						int green = Math.min(255, ((old >> 8) & 0xff) + ((color >> 8) & 0xff));
						int blue = Math.min(255, (old & 0xff) + (color & 0xff));
						colors[i] = (red << 16) | (green << 8) | blue;
					}
				}
			}
		}
		return colors;
	}

	/**
	 * Sets the data of the iso surface (e.g., of a dynamic model) and computes the
	 * surface in the background. The values are copied.
	 * 
	 * @param values the values (index = (z * height + y) * width + x)
	 * @param w      the width (x)
	 * @param h      the height (y)
	 * @param d      the depth (z)
	 */
	public void setData(float[] values, int w, int h, int d) {
		setData(values, w, h, d, 1.0);
	}

	/**
	 * Sets the data of the iso surface (e.g., of a dynamic model) and computes the
	 * surface in the background. The values are copied. The iso value is kept if
	 * it has been set (see setIsoValue), else the center of the value range is
	 * used.
	 * 
	 * @param values     the values (index = (z * height + y) * width + x)
	 * @param w          the width (x)
	 * @param h          the height (y)
	 * @param d          the depth (z)
	 * @param voxelDepth the depth of a voxel relative to its width
	 */
	public void setData(float[] values, int w, int h, int d, double voxelDepth) {
		if (values == null || w < 1 || h < 1 || d < 1 || values.length < w * h * d) {
			throw new IllegalArgumentException("The size of the data doesn't fit!");
		}
		float[] copy = Arrays.copyOf(values, w * h * d);
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < copy.length; i++) {
			if (Float.isNaN(copy[i])) {
				copy[i] = 0;
			}
			min = Math.min(min, copy[i]);
			max = Math.max(max, copy[i]);
		}
		synchronized (this) {
			data = copy;
			nx = w;
			ny = h;
			nz = d;
			stepX = 1;
			stepY = 1;
			stepZ = (float) voxelDepth;
			width = w;
			height = h;
			depth = (float) (d * voxelDepth);
			displayMin = min;
			displayMax = max;
			pixelSize = 1;
			unit = "pixel";
			lut = null;
			image = null;
			if (!isoExplicit) {
				isoValue = (min + max) / 2.0;
			}
			imageColors = null;
			colorLut = null;
			/* A color array (see setColorData) of the same size is kept! */
			if (!customColorData || colorData == null || colorData.length != copy.length) {
				customColorData = false;
				colorData = copy;
				if (!customColorRange) {
					colorMin = min;
					colorMax = max;
				}
			}
		}
		update();
	}

	/**
	 * Sets an array for the colors of the surface (e.g., a heat map of another
	 * variable of a model). The array has the size and the layout of the data
	 * (see setData) and is kept for the following setData calls. The color mode is
	 * set to COLOR_INTENSITY (always, also after COLOR_DEPTH). The values are mapped to the colors of the color map
	 * (see setColorMap) with the color range (see setColorRange, default: the
	 * minimum and maximum of the array).
	 * 
	 * @param values the values (index = (z * height + y) * width + x)
	 */
	public void setColorData(float[] values) {
		float[] copy = values.clone();
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < copy.length; i++) {
			if (Float.isNaN(copy[i])) {
				copy[i] = 0;
			}
			min = Math.min(min, copy[i]);
			max = Math.max(max, copy[i]);
		}
		synchronized (this) {
			colorData = copy;
			customColorData = true;
			if (!customColorRange) {
				colorMin = min;
				colorMax = max;
			}
			colorMode = COLOR_INTENSITY;
			colorsDirty = true;
		}
	}

	/**
	 * Sets an array for the colors of the surface (values[x][y][z], see
	 * setColorData(float[])).
	 */
	public void setColorData(double[][][] values) {
		setColorData(flatten(values));
	}

	/**
	 * Sets an array for the colors of the surface (values[x][y][z], see
	 * setColorData(float[])).
	 */
	public void setColorData(float[][][] values) {
		int w = values.length, h = values[0].length, d = values[0][0].length;
		float[] f = new float[w * h * d];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				for (int z = 0; z < d; z++) {
					f[(z * h + y) * w + x] = values[x][y][z];
				}
			}
		}
		setColorData(f);
	}

	private static float[] flatten(double[][][] values) {
		int w = values.length, h = values[0].length, d = values[0][0].length;
		float[] f = new float[w * h * d];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				for (int z = 0; z < d; z++) {
					f[(z * h + y) * w + x] = (float) values[x][y][z];
				}
			}
		}
		return f;
	}

	/**
	 * Resets the color settings to the defaults: no color array (see
	 * setColorData), the value range of the data, the default color map (the LUT
	 * of the image or Viridis), no transfer function and the sample depth 1. The color mode is not
	 * changed. Useful at the start of a model, because the iso surface keeps the
	 * settings of a former model.
	 */
	public synchronized void resetColors() {
		customColorData = false;
		colorData = data;
		customColorRange = false;
		colorMin = displayMin;
		colorMax = displayMax;
		customColorTable = null;
		colorSampleDepth = 1;
		transferFunction = null;
		opacityMapping = false;
		colorsDirty = true;
	}

	/**
	 * Removes the color array (see setColorData). The colors are computed from the
	 * data again.
	 */
	public synchronized void clearColorData() {
		customColorData = false;
		colorData = data;
		if (!customColorRange) {
			colorMin = displayMin;
			colorMax = displayMax;
		}
		colorsDirty = true;
	}

	/**
	 * Sets the value range of the colors (the first and the last color of the
	 * color map).
	 * 
	 * @param min the value of the first color
	 * @param max the value of the last color
	 */
	public synchronized void setColorRange(double min, double max) {
		colorMin = min;
		colorMax = max;
		customColorRange = true;
		colorsDirty = true;
	}

	/**
	 * Uses the minimum and the maximum of the color values as the color range
	 * again.
	 */
	public synchronized void resetColorRange() {
		customColorRange = false;
		if (colorData != null) {
			float min = Float.MAX_VALUE;
			float max = -Float.MAX_VALUE;
			for (float v : colorData) {
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
			colorMin = min;
			colorMax = max;
		}
		colorsDirty = true;
	}

	/**
	 * Sets the color map of the colors by the name of an ImageJ LUT (e.g.,
	 * "Fire", "Spectrum", "Ice", "16 colors", any entry of the Lookup Tables menu)
	 * or "Viridis". null uses the default (the LUT of the image or Viridis).
	 * 
	 * @param name the name of the LUT
	 * @return true if the LUT has been found
	 */
	public boolean setColorMap(String name) {
		if (name == null) {
			setColorMap((ColorModel) null);
			return true;
		}
		if (name.equalsIgnoreCase("viridis")) {
			synchronized (this) {
				customColorTable = colorTable(null, true);
				colorsDirty = true;
			}
			return true;
		}
		ColorModel cm = LutLoader.getLut(name);
		if (cm == null) {
			return false;
		}
		setColorMap(cm);
		return true;
	}

	/**
	 * Sets the color map of the colors (e.g., an ImageJ LUT). null uses the
	 * default (the LUT of the image or Viridis).
	 * 
	 * @param cm the color model (256 colors)
	 */
	public synchronized void setColorMap(ColorModel cm) {
		customColorTable = cm != null ? colorTable(cm, false) : null;
		colorsDirty = true;
	}

	/**
	 * The distance (voxels) inside of the surface where the color values are
	 * sampled (default 1, 0 = at the surface).
	 * 
	 * @param voxels the distance in voxels
	 */
	public synchronized void setColorSampleDepth(float voxels) {
		colorSampleDepth = Math.max(0, voxels);
		colorsDirty = true;
	}

	/**
	 * Sets the data of the iso surface (e.g., of a dynamic model) and computes the
	 * surface in the background.
	 * 
	 * @param values the values (values[x][y][z])
	 */
	public void setData(double[][][] values) {
		int w = values.length;
		int h = values[0].length;
		int d = values[0][0].length;
		float[] f = new float[w * h * d];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				for (int z = 0; z < d; z++) {
					f[(z * h + y) * w + x] = (float) values[x][y][z];
				}
			}
		}
		setData(f, w, h, d);
	}

	/**
	 * Sets the data of the iso surface (e.g., of a dynamic model) and computes the
	 * surface in the background.
	 * 
	 * @param values the values (values[x][y][z])
	 */
	public void setData(float[][][] values) {
		int w = values.length;
		int h = values[0].length;
		int d = values[0][0].length;
		float[] f = new float[w * h * d];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				for (int z = 0; z < d; z++) {
					f[(z * h + y) * w + x] = values[x][y][z];
				}
			}
		}
		setData(f, w, h, d);
	}

	/**
	 * Computes the iso surface in the calling thread (e.g., in a time step of a
	 * model, so that the surface fits to the data of the step). The surface is
	 * displayed with the next frame.
	 */
	public void computeNow() {
		float[] values;
		int w, h, d;
		double iso;
		float sigma;
		int iterations;
		synchronized (this) {
			if (data == null) {
				return;
			}
			values = data;
			w = nx;
			h = ny;
			d = nz;
			iso = isoValue;
			/* The smoothing in voxels of the stack (the reduced display: in its voxels) */
			sigma = smoothing / stepX;
			iterations = meshSmoothing;
		}
		Cut cut = currentCut();
		Mesh mesh = computeMesh(values, w, h, d, iso, sigma, iterations, cut);
		setMesh(values, mesh, cut);
	}

	/**
	 * Loads the last loaded stack again (e.g., after a change of the maximum
	 * size).
	 */
	public boolean reload() {
		return image != null && load(image);
	}

	/**
	 * Returns the surface for the measurements (see Measurement3d): the displayed
	 * surface or, if the display is reduced, the surface of the stack in the full
	 * resolution with the same iso value, smoothing (in voxels of the stack), mesh
	 * smoothing and cut (computed and cached until a change). So the measured
	 * surface is the displayed surface in the full resolution and the reduction of
	 * the display doesn't change the measurements.
	 */
	MeasureData getMeasurementData() {
		MeasureData display = getMeasureData();
		if (display == null || !isReduced()) {
			return display;
		}
		ImagePlus imp;
		double iso;
		float sigma;
		int iterations;
		float voxelDepth;
		synchronized (this) {
			imp = image;
			iso = isoValue;
			/* The smoothing in voxels of the stack */
			sigma = smoothing;
			iterations = meshSmoothing;
			voxelDepth = depth / imp.getNSlices();
		}
		Cut cut = currentCut(1, 1, voxelDepth);
		List<Object> key = new ArrayList<>(List.of(imp, imp.getChannel(), imp.getFrame(), iso, sigma, iterations));
		key.addAll(cut.key);
		synchronized (this) {
			if (fullData != null && key.equals(fullKey)) {
				return fullData;
			}
		}
		int w = imp.getWidth(), h = imp.getHeight(), d = imp.getNSlices();
		float[] values = readChannel(imp, imp.getChannel(), 1, 1, 1, w, h, d);
		Mesh mesh = computeMesh(values, w, h, d, iso, sigma, iterations, cut);
		MeasureData m = new MeasureData();
		m.vertices = mesh.vertices;
		m.triangles = mesh.triangles;
		m.data = values;
		m.nx = w;
		m.ny = h;
		m.nz = d;
		m.stepX = 1;
		m.stepY = 1;
		m.stepZ = voxelDepth;
		m.pixelSize = display.pixelSize;
		m.unit = display.unit;
		m.isoValue = iso;
		m.image = imp;
		synchronized (this) {
			if (image == imp) {
				fullData = m;
				fullKey = key;
			}
		}
		return m;
	}

	/** Returns true if the display is reduced to the maximum size (see setMaxSize). */
	public synchronized boolean isReduceDisplay() {
		return reduceDisplay;
	}

	/**
	 * Reduces the displayed surface of large stacks to the maximum size (faster,
	 * see setMaxSize). The measurements always use the full resolution. Applied
	 * when the stack is loaded (default: false, the full resolution).
	 */
	public synchronized void setReduceDisplay(boolean reduce) {
		reduceDisplay = reduce;
	}

	/**
	 * Returns true if the displayed surface of the loaded stack has been reduced
	 * (see setReduceDisplay and setMaxSize). The measurements use the full
	 * resolution.
	 */
	public synchronized boolean isReduced() {
		return image != null && data != null
				&& (nx < image.getWidth() || ny < image.getHeight() || nz < image.getNSlices());
	}

	/** Returns a description of the reduction ("512 x 512 x 320 -> 256 x 256 x 160") or null. */
	public synchronized String getReduction() {
		if (!isReduced()) {
			return null;
		}
		return image.getWidth() + " x " + image.getHeight() + " x " + image.getNSlices() + " -> " + nx + " x " + ny
				+ " x " + nz;
	}

	/**
	 * Removes the iso surface.
	 */
	public synchronized void clear() {
		fullData = null;
		fullKey = null;
		data = null;
		image = null;
		meshVertices = null;
		meshTriangles = null;
		positions = null;
		normals = null;
		indices = null;
		colorData = null;
		imageColors = null;
		vertexColors = null;
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
				/* The smoothing in voxels of the stack (the reduced display: in its voxels) */
				sigma = smoothing / stepX;
				iterations = meshSmoothing;
			}
			Cut cut = currentCut();
			Mesh mesh = computeMesh(values, w, h, d, iso, sigma, iterations, cut);
			setMesh(values, mesh, cut);
		}
	}

	/* The cut of the mesh: planes in voxel coordinates and the mask of the removed voxels! */
	private static class Cut {
		/* a, b, c, d: kept are the voxels with a x + b y + c z + d >= 0 (distance in voxels) */
		final List<float[]> planes = new ArrayList<>();
		Clipping.Mask mask;
		/* The state (for the comparison with a later state) */
		final List<Object> key = new ArrayList<>();

		boolean isEmpty() {
			return planes.isEmpty() && mask == null;
		}
	}

	/*
	 * The current cut of the clipping (the planes of the box and the cutting plane
	 * if the mesh is cut, see Clipping.setCutMesh, and the mask)!
	 */
	private Cut currentCut() {
		float sx, sy, sz;
		synchronized (this) {
			sx = stepX;
			sy = stepY;
			sz = stepZ;
		}
		return currentCut(sx, sy, sz);
	}

	/* The cut for voxels of the size sx, sy, sz (pixels, without the z scale)! */
	private Cut currentCut(float sx, float sy, float sz) {
		Clipping clip;
		boolean flip = HeightMaps.isFlipVertically();
		float w, h, scaledDepth;
		synchronized (this) {
			clip = clipping;
			w = width;
			sz = sz * zScale;
			h = height;
			scaledDepth = depth * zScale;
		}
		Cut cut = new Cut();
		if (clip == null) {
			return cut;
		}
		cut.mask = clip.getMask();
		cut.key.add(cut.mask);
		if (clip.isCutMesh()) {
			/* The scene coordinates (octant) of a voxel: (x + 0.5) * sx, flip ? (y + 0.5) * sy : h - (y + 0.5) * sy, (z + 0.5) * sz */
			double[] bounds = { 0, w, 0, h, 0, scaledDepth };
			for (double[] p : clip.getPlanes(bounds)) {
				double b = flip ? p[1] * sy : -p[1] * sy;
				double c0 = p[0] * 0.5 * sx + (flip ? p[1] * 0.5 * sy : p[1] * (h - 0.5 * sy)) + p[2] * 0.5 * sz + p[3];
				/* The distance in voxels (of the width) */
				cut.planes.add(new float[] { (float) (p[0]), (float) (b / sx), (float) (p[2] * sz / sx), (float) (c0 / sx) });
				for (double v : p) {
					cut.key.add(v);
				}
			}
			cut.key.add(flip);
		}
		return cut;
	}

	/**
	 * Sets the clipping for the cut of the mesh (see Clipping.setCutMesh and the
	 * mask). The surface is computed again after a change of the cut.
	 * 
	 * @param clipping the clipping or null
	 */
	public void setClipping(Clipping clipping) {
		synchronized (this) {
			this.clipping = clipping;
		}
		if (clipping != null) {
			clipping.addChangeListener(this::clippingChanged);
		}
		clippingChanged();
	}

	/* Computes the surface again if the cut has been changed! */
	private void clippingChanged() {
		Cut cut = currentCut();
		synchronized (this) {
			Object previous = computedClip;
			if (previous == null ? cut.isEmpty() : previous.equals(cut.key)) {
				return;
			}
		}
		update();
	}

	private static Mesh computeMesh(float[] values, int w, int h, int d, double iso, float sigma, int iterations,
			Cut cut) {
		float[] smoothed = sigma > 0 ? gaussian(values, w, h, d, sigma) : values;
		if (cut != null && !cut.isEmpty()) {
			smoothed = cutField(smoothed, w, h, d, (float) iso, cut);
		}
		Mesh mesh = surfaceNets(smoothed, w, h, d, (float) iso);
		taubinSmoothing(mesh, iterations);
		return mesh;
	}

	/*
	 * The intersection of the inside of the surface with the kept half spaces and
	 * the kept voxels of the mask (the minimum of the signed distances), so the cut
	 * surface is closed with caps (CSG)!
	 */
	private static float[] cutField(float[] f, int w, int h, int d, float iso, Cut cut) {
		float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
		for (float v : f) {
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		/* The slope of the distance fields (value change per voxel) */
		float slope = Math.max(Math.max(max - iso, iso - min) / 2, 1e-6f);
		float[] maskField = null;
		if (cut.mask != null) {
			/* +1 kept, -1 removed, smoothed (the boundary at 0) */
			maskField = new float[w * h * d];
			int i = 0;
			for (int z = 0; z < d; z++) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						maskField[i++] = cut.mask.isRemoved((x + 0.5) / w, (y + 0.5) / h, (z + 0.5) / d) ? -1 : 1;
					}
				}
			}
			maskField = gaussian(maskField, w, h, d, 0.7f);
		}
		float[] g = new float[f.length];
		int i = 0;
		for (int z = 0; z < d; z++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					float v = f[i] - iso;
					for (float[] p : cut.planes) {
						v = Math.min(v, (p[0] * x + p[1] * y + p[2] * z + p[3]) * slope);
					}
					if (maskField != null) {
						v = Math.min(v, maskField[i] * slope);
					}
					g[i++] = v + iso;
				}
			}
		}
		return g;
	}

	/* Sets a computed mesh (if the data hasn't been changed in the meantime)! */
	private void setMesh(float[] values, Mesh mesh, Cut cut) {
		synchronized (this) {
			if (values != data) {
				return; // new data has been set
			}
			computedClip = cut.isEmpty() ? null : cut.key;
			meshVertices = mesh.vertices;
			meshTriangles = mesh.triangles;
			bake(HeightMaps.isFlipVertically());
			colorsDirty = true;
			upload = true;
		}
		notifyListener();
	}

	/*
	 * The vertex colors (RGB) of the color mode: the intensity of the color data
	 * one voxel inside of the surface or the depth (z), mapped with the LUT!
	 */
	private void ensureColors() {
		if (!colorsDirty) {
			return;
		}
		colorsDirty = false;
		vertexColors = null;
		vertexAlpha = null;
		vertexIndices = null;
		if (objectColors != null) {
			/* The colors of the measured objects (removed with a new mesh) */
			if (objectColorsMesh == meshTriangles && meshVertices != null
					&& objectColors.length == meshVertices.length) {
				vertexColors = objectColors;
				return;
			}
			objectColors = null;
			objectColorsMesh = null;
		}
		if (colorMode == COLOR_SINGLE || meshVertices == null || colorData == null
				|| colorData.length != nx * ny * nz) {
			return;
		}
		int n = meshVertices.length / 3;
		float[] colors = new float[n * 3];
		if (colorMode == COLOR_IMAGE && imageColors != null && !customColorData) {
			/* The displayed colors one voxel inside of the surface (the normals point outside)! */
			float[] inside = computeNormals(meshVertices, meshTriangles);
			for (int i = 0; i < n; i++) {
				sampleColor(imageColors, nx, ny, nz, meshVertices[i * 3] - inside[i * 3],
						meshVertices[i * 3 + 1] - inside[i * 3 + 1], meshVertices[i * 3 + 2] - inside[i * 3 + 2], colors,
						i * 3);
			}
			vertexColors = colors;
			return;
		}
		int[] table;
		float[] inward = null;
		/* Without displayed colors (data of a model): the intensity! */
		if (colorMode == COLOR_INTENSITY || colorMode == COLOR_IMAGE) {
			table = customColorTable != null ? customColorTable : colorTable(colorLut, false);
			inward = computeNormals(meshVertices, meshTriangles);
		} else {
			table = customColorTable != null ? customColorTable : colorTable(lut, true);
		}
		/* The transfer function: colors and (optional) opacities! */
		float[] alphaTable = null;
		float[] alphas = null;
		if (transferFunction != null) {
			table = transferFunction.getColorTable();
			if (opacityMapping) {
				alphaTable = transferFunction.getOpacityTable();
				alphas = new float[n];
			}
		}
		float depthInside = colorSampleDepth;
		int[] indices = new int[n];
		double range = colorMax > colorMin ? colorMax - colorMin : 1;
		for (int i = 0; i < n; i++) {
			float x = meshVertices[i * 3];
			float y = meshVertices[i * 3 + 1];
			float z = meshVertices[i * 3 + 2];
			double t;
			if (inward != null) {
				/* The normals point outside (lower values)! */
				float value = sample(colorData, nx, ny, nz, x - depthInside * inward[i * 3],
						y - depthInside * inward[i * 3 + 1], z - depthInside * inward[i * 3 + 2]);
				t = (value - colorMin) / range;
			} else {
				t = nz > 1 ? z / (nz - 1) : 0;
			}
			int index = (int) Math.max(0, Math.min(255, Math.round(t * 255)));
			int rgb = table[index];
			colors[i * 3] = ((rgb >> 16) & 0xff) / 255f;
			colors[i * 3 + 1] = ((rgb >> 8) & 0xff) / 255f;
			colors[i * 3 + 2] = (rgb & 0xff) / 255f;
			if (alphas != null) {
				alphas[i] = alphaTable[index];
			}
			indices[i] = index;
		}
		vertexColors = colors;
		vertexAlpha = alphas;
		vertexIndices = indices;
	}

	/**
	 * Sets a transfer function for the colors (and with the opacity mapping for
	 * the opacities) of the surface. The color values (see the color mode and
	 * setColorData) are mapped with the color range (see setColorRange) to the
	 * positions 0 - 1 of the function. The color mode COLOR_SINGLE is changed to
	 * COLOR_INTENSITY. null removes the transfer function.
	 * 
	 * @param tf the transfer function (copied) or null
	 */
	public synchronized void setTransferFunction(TransferFunction tf) {
		transferFunction = tf != null ? tf.copy() : null;
		if (transferFunction != null && colorMode == COLOR_SINGLE) {
			colorMode = COLOR_INTENSITY;
		}
		colorsDirty = true;
	}

	public synchronized TransferFunction getTransferFunction() {
		return transferFunction != null ? transferFunction.copy() : null;
	}

	/**
	 * Uses the opacities of the transfer function for the vertices (like the
	 * opacity mapping of ParaView). The surface becomes transparent where the
	 * opacity of the function is low.
	 */
	public synchronized void setOpacityMapping(boolean mapping) {
		opacityMapping = mapping;
		colorsDirty = true;
	}

	/** The data value of the first color (the position 0 of the transfer function). */
	public synchronized double getColorMin() {
		return colorMin;
	}

	/** The data value of the last color (the position 1 of the transfer function). */
	public synchronized double getColorMax() {
		return colorMax;
	}

	public synchronized boolean isOpacityMapping() {
		return opacityMapping;
	}

	/**
	 * Returns the histogram (256 bins) of the color values of the surface
	 * vertices (the values which are mapped with the transfer function) or, if no
	 * vertex colors are available, of all color values.
	 */
	public synchronized int[] getSurfaceHistogram() {
		if (colorsDirty) {
			ensureColors();
			// The new colors must be uploaded with the next rendering
			upload = true;
		}
		if (vertexIndices == null) {
			return getHistogram();
		}
		int[] histogram = new int[256];
		for (int index : vertexIndices) {
			histogram[index]++;
		}
		return histogram;
	}

	/**
	 * Returns the position (0 - 1) of the iso value in the color range (the color
	 * values of the same channel at the surface are close to it) or NaN.
	 */
	public synchronized double getIsoPosition() {
		if (colorMode != COLOR_INTENSITY || customColorData || colorMax <= colorMin) {
			return Double.NaN;
		}
		return Math.max(0, Math.min(1, (isoValue - colorMin) / (colorMax - colorMin)));
	}

	/**
	 * Returns the histogram (256 bins) of the color values in the color range
	 * (for the transfer function editor) or null.
	 */
	public synchronized int[] getHistogram() {
		if (colorData == null) {
			return null;
		}
		double range = colorMax > colorMin ? colorMax - colorMin : 1;
		int[] histogram = new int[256];
		for (float v : colorData) {
			int index = (int) Math.round((v - colorMin) / range * 255);
			if (index >= 0 && index <= 255) {
				histogram[index]++;
			}
		}
		return histogram;
	}

	/** Returns the LUT of the loaded stack (for the colors of a transfer function) or null. */
	public synchronized ColorModel getColorModel() {
		return colorLut != null ? colorLut : lut;
	}

	/* The colors of a LUT (or Viridis if no LUT or, optional, a gray LUT is available)! */
	private static int[] colorTable(ColorModel cm, boolean replaceGray) {
		int[] table = new int[256];
		boolean gray = true;
		if (cm != null) {
			for (int i = 0; i < 256; i++) {
				table[i] = cm.getRGB(i) & 0xffffff;
				int r = (table[i] >> 16) & 0xff, g = (table[i] >> 8) & 0xff, b = table[i] & 0xff;
				gray &= r == g && g == b;
			}
		}
		if (cm == null || (gray && replaceGray)) {
			for (int i = 0; i < 256; i++) {
				double f = i / 255.0 * (VIRIDIS.length - 1);
				int k = Math.min(VIRIDIS.length - 2, (int) f);
				double t = f - k;
				int c = 0;
				for (int shift = 16; shift >= 0; shift -= 8) {
					int a = (VIRIDIS[k] >> shift) & 0xff;
					int b = (VIRIDIS[k + 1] >> shift) & 0xff;
					c |= ((int) Math.round(a + t * (b - a))) << shift;
				}
				table[i] = c;
			}
		}
		return table;
	}

	/* Trilinear interpolation of RGB colors (clamped at the borders)! */
	private static void sampleColor(int[] c, int w, int h, int d, float x, float y, float z, float[] out,
			int offset) {
		x = Math.max(0, Math.min(w - 1, x));
		y = Math.max(0, Math.min(h - 1, y));
		z = Math.max(0, Math.min(d - 1, z));
		int x0 = (int) x, y0 = (int) y, z0 = (int) z;
		int x1 = Math.min(w - 1, x0 + 1), y1 = Math.min(h - 1, y0 + 1), z1 = Math.min(d - 1, z0 + 1);
		float tx = x - x0, ty = y - y0, tz = z - z0;
		int[] corners = { c[(z0 * h + y0) * w + x0], c[(z0 * h + y0) * w + x1], c[(z0 * h + y1) * w + x0],
				c[(z0 * h + y1) * w + x1], c[(z1 * h + y0) * w + x0], c[(z1 * h + y0) * w + x1],
				c[(z1 * h + y1) * w + x0], c[(z1 * h + y1) * w + x1] };
		float[] weights = { (1 - tx) * (1 - ty) * (1 - tz), tx * (1 - ty) * (1 - tz), (1 - tx) * ty * (1 - tz),
				tx * ty * (1 - tz), (1 - tx) * (1 - ty) * tz, tx * (1 - ty) * tz, (1 - tx) * ty * tz, tx * ty * tz };
		float r = 0, g = 0, b = 0;
		for (int k = 0; k < 8; k++) {
			r += weights[k] * ((corners[k] >> 16) & 0xff);
			g += weights[k] * ((corners[k] >> 8) & 0xff);
			b += weights[k] * (corners[k] & 0xff);
		}
		out[offset] = r / 255f;
		out[offset + 1] = g / 255f;
		out[offset + 2] = b / 255f;
	}

	/* Trilinear interpolation (clamped at the borders)! */
	static float sample(float[] f, int w, int h, int d, float x, float y, float z) {
		x = Math.max(0, Math.min(w - 1, x));
		y = Math.max(0, Math.min(h - 1, y));
		z = Math.max(0, Math.min(d - 1, z));
		int x0 = (int) x, y0 = (int) y, z0 = (int) z;
		int x1 = Math.min(w - 1, x0 + 1), y1 = Math.min(h - 1, y0 + 1), z1 = Math.min(d - 1, z0 + 1);
		float tx = x - x0, ty = y - y0, tz = z - z0;
		float c00 = f[(z0 * h + y0) * w + x0] * (1 - tx) + f[(z0 * h + y0) * w + x1] * tx;
		float c10 = f[(z0 * h + y1) * w + x0] * (1 - tx) + f[(z0 * h + y1) * w + x1] * tx;
		float c01 = f[(z1 * h + y0) * w + x0] * (1 - tx) + f[(z1 * h + y0) * w + x1] * tx;
		float c11 = f[(z1 * h + y1) * w + x0] * (1 - tx) + f[(z1 * h + y1) * w + x1] * tx;
		float c0 = c00 * (1 - ty) + c10 * ty;
		float c1 = c01 * (1 - ty) + c11 * ty;
		return c0 * (1 - tz) + c1 * tz;
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
			if (computedClip != null) {
				update(); // the cut planes depend on the orientation
			}
		}
		if (colorsDirty) {
			ensureColors();
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
		if (hasColorBuffer) {
			/* The vertex colors (with the opacity) are the ambient and diffuse material! */
			gl.glEnable(GL2.GL_COLOR_MATERIAL);
			gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[3]);
			gl.glColorPointer(4, GL.GL_FLOAT, 0, 0);
			gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
		}
		if (opacity < 1 || (hasColorBuffer && vertexAlpha != null)) {
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
		gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);

		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	private void uploadBuffers(GL2 gl) {
		upload = false;
		if (hasBuffers) {
			gl.glDeleteBuffers(4, buffers, 0);
			hasBuffers = false;
		}
		hasColorBuffer = false;
		indexCount = 0;
		if (positions == null || indices == null || indices.length == 0) {
			return;
		}
		gl.glGenBuffers(4, buffers, 0);
		FloatBuffer pos = Buffers.newDirectFloatBuffer(positions);
		FloatBuffer nor = Buffers.newDirectFloatBuffer(normals);
		IntBuffer idx = Buffers.newDirectIntBuffer(indices);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[0]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) positions.length * 4, pos, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[1]);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) normals.length * 4, nor, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffers[2]);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, (long) indices.length * 4, idx, GL.GL_STATIC_DRAW);
		if (vertexColors != null && vertexColors.length == positions.length) {
			/* RGBA with the opacity! */
			int n = vertexColors.length / 3;
			FloatBuffer col = Buffers.newDirectFloatBuffer(n * 4);
			for (int i = 0; i < n; i++) {
				float a = vertexAlpha != null && vertexAlpha.length == n ? opacity * vertexAlpha[i] : opacity;
				col.put(vertexColors[i * 3]).put(vertexColors[i * 3 + 1]).put(vertexColors[i * 3 + 2]).put(a);
			}
			col.rewind();
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, buffers[3]);
			gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) n * 16, col, GL.GL_STATIC_DRAW);
			hasColorBuffer = true;
		}
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
		float[] vertexColors; // RGB per vertex or null
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
			ensureColors();
			e.vertexColors = vertexColors;
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
			float[] vc = e.vertexColors;
			for (int i = 0; i < pos.length; i += 3) {
				sb.setLength(0);
				sb.append("v ").append(pos[i]).append(' ').append(pos[i + 1]).append(' ').append(pos[i + 2]);
				if (vc != null) {
					/* The vertex colors (an extension supported by, e.g., MeshLab and Blender)! */
					sb.append(' ').append(vc[i]).append(' ').append(vc[i + 1]).append(' ').append(vc[i + 2]);
				}
				sb.append('\n');
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
		float[] vc = e.vertexColors;
		for (int i = 0; i < p.length; i += 3) {
			buffer.putFloat(p[i]).putFloat(p[i + 1]).putFloat(p[i + 2]);
			buffer.putFloat(n[i]).putFloat(n[i + 1]).putFloat(n[i + 2]);
			if (vc != null) {
				buffer.put((byte) Math.round(vc[i] * 255)).put((byte) Math.round(vc[i + 1] * 255))
						.put((byte) Math.round(vc[i + 2] * 255));
			} else {
				buffer.put(r).put(g).put(b);
			}
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
		float[] vc = e.vertexColors;
		int colorBytes = vc != null ? vc.length * 4 : 0;
		int binLength = positionBytes + normalBytes + indexBytes + colorBytes;
		/* With vertex colors the base color is white (it is multiplied)! */
		float[] c = vc != null ? new float[] { 1, 1, 1 } : e.color;
		String json = "{\"asset\":{\"version\":\"2.0\",\"generator\":\"Bio7 iso surface\"},"
				+ "\"scene\":0,\"scenes\":[{\"nodes\":[0]}],\"nodes\":[{\"mesh\":0,\"name\":\"" + jsonText(e.title) + "\"}],"
				+ "\"meshes\":[{\"name\":\"isosurface\",\"primitives\":[{\"attributes\":{\"POSITION\":0,\"NORMAL\":1" + (vc != null ? ",\"COLOR_0\":3" : "") + "},\"indices\":2,\"material\":0,\"mode\":4}]}],"
				+ "\"materials\":[{\"name\":\"isosurface\",\"doubleSided\":true,"
				+ (e.opacity < 1 ? "\"alphaMode\":\"BLEND\"," : "")
				+ "\"pbrMetallicRoughness\":{\"baseColorFactor\":[" + c[0] + "," + c[1] + "," + c[2] + "," + e.opacity
				+ "],\"metallicFactor\":0.0,\"roughnessFactor\":0.5}}],"
				+ "\"buffers\":[{\"byteLength\":" + binLength + "}],"
				+ "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":" + positionBytes + ",\"target\":34962},"
				+ "{\"buffer\":0,\"byteOffset\":" + positionBytes + ",\"byteLength\":" + normalBytes + ",\"target\":34962},"
				+ "{\"buffer\":0,\"byteOffset\":" + (positionBytes + normalBytes) + ",\"byteLength\":" + indexBytes
				+ ",\"target\":34963}"
				+ (vc != null ? ",{\"buffer\":0,\"byteOffset\":" + (positionBytes + normalBytes + indexBytes)
						+ ",\"byteLength\":" + colorBytes + ",\"target\":34962}" : "")
				+ "],"
				+ "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":" + vertices
				+ ",\"type\":\"VEC3\",\"min\":[" + min[0] + "," + min[1] + "," + min[2] + "],\"max\":[" + max[0] + ","
				+ max[1] + "," + max[2] + "]}," + "{\"bufferView\":1,\"componentType\":5126,\"count\":" + vertices
				+ ",\"type\":\"VEC3\"}," + "{\"bufferView\":2,\"componentType\":5125,\"count\":" + idx.length
				+ ",\"type\":\"SCALAR\"}"
				+ (vc != null ? ",{\"bufferView\":3,\"componentType\":5126,\"count\":" + vertices + ",\"type\":\"VEC3\"}"
						: "")
				+ "]}";
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
		if (vc != null) {
			for (float v : vc) {
				buffer.putFloat(v);
			}
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
			if (e.vertexColors != null) {
				/* Binary color scalars are unsigned chars! */
				out.write("\nCOLOR_SCALARS colors 3\n".getBytes(StandardCharsets.US_ASCII));
				for (float v : e.vertexColors) {
					out.writeByte(Math.round(v * 255));
				}
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
		String title = image != null ? image.getTitle() : "Data";
		int vertices = positions != null ? positions.length / 3 : 0;
		int triangles = indices != null ? indices.length / 3 : 0;
		return title + ": " + nx + " x " + ny + " x " + nz + " voxels, " + vertices + " vertices, " + triangles
				+ " triangles";
	}

	/* A snapshot of the mesh and the data for the measurements (see Measurement3d)! */
	static class MeasureData {
		float[] vertices; // voxel coordinates of the data (y = row)
		int[] triangles;
		float[] data;
		int nx, ny, nz;
		float stepX, stepY, stepZ; // the size of a data voxel in pixels
		double pixelSize;
		String unit;
		double isoValue;
		ImagePlus image;
	}

	/*
	 * Sets the colors (RGB per vertex) of the measured objects for the mesh (the
	 * triangles) or null (the colors are removed with a new mesh)!
	 */
	synchronized void setObjectColors(float[] colors, int[] mesh) {
		objectColors = colors;
		objectColorsMesh = mesh;
		colorsDirty = true;
		upload = true;
	}

	/*
	 * Draws triangles of the mesh (the indices of the triangles) in a color on the
	 * surface (e.g., the triangles of a picked point)!
	 */
	synchronized void renderTriangles(GL2 gl, boolean cartesian, int[] mesh, int[] triangles, float[] rgb,
			boolean fill, float lineWidth) {
		if (!show || positions == null || indices == null || mesh != meshTriangles || triangles == null) {
			return;
		}
		float d = depth * zScale;
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_POLYGON_BIT | GL2.GL_LINE_BIT);
		gl.glPushMatrix();
		if (cartesian) {
			gl.glTranslatef(-width / 2, -height / 2, -d / 2);
		}
		gl.glScalef(1, 1, zScale);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glDisable(GL.GL_CULL_FACE);
		/* In front of the surface (the outlines in front of the filled triangles) */
		if (fill) {
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
			gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
			gl.glPolygonOffset(-2, -4);
		} else {
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_LINE);
			gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
			gl.glPolygonOffset(-3, -8);
			gl.glLineWidth(lineWidth);
		}
		gl.glColor3f(rgb[0], rgb[1], rgb[2]);
		gl.glBegin(GL.GL_TRIANGLES);
		for (int t : triangles) {
			for (int k = 0; k < 3; k++) {
				int v = indices[t * 3 + k] * 3;
				gl.glVertex3f(positions[v], positions[v + 1], positions[v + 2]);
			}
		}
		gl.glEnd();
		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	synchronized MeasureData getMeasureData() {
		if (data == null) {
			return null;
		}
		MeasureData m = new MeasureData();
		m.vertices = meshVertices;
		m.triangles = meshTriangles;
		m.data = data;
		m.nx = nx;
		m.ny = ny;
		m.nz = nz;
		m.stepX = stepX;
		m.stepY = stepY;
		m.stepZ = stepZ;
		m.pixelSize = pixelSize;
		m.unit = unit;
		m.isoValue = isoValue;
		m.image = image;
		return m;
	}

	/** Returns the size of the (reduced) data (width, height, depth) or null. */
	public synchronized int[] getDataSize() {
		return data != null ? new int[] { nx, ny, nz } : null;
	}

	/** Returns the loaded ImageJ image (null for the data of a model). */
	public synchronized ImagePlus getImage() {
		return image;
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
			isoExplicit = true;
		}
		update();
	}

	public float getSmoothing() {
		return smoothing;
	}

	/**
	 * The sigma (voxels of the stack) of the Gaussian smoothing of the stack (0 =
	 * off, the default). The measurements use the same smoothing.
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

	public void setZScale(float zScale) {
		synchronized (this) {
			this.zScale = Math.max(0.01f, zScale);
		}
		clippingChanged(); // the cut planes depend on the z scale
	}

	public float getOpacity() {
		return opacity;
	}

	public synchronized void setOpacity(float opacity) {
		this.opacity = Math.max(0.05f, Math.min(1, opacity));
		/* The opacity is part of the vertex colors! */
		if (vertexColors != null) {
			upload = true;
		}
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

	public synchronized int getColorMode() {
		return colorMode;
	}

	/**
	 * Selects the colors of the surface: COLOR_SINGLE (the selected color or the
	 * LUT color at the iso value), COLOR_INTENSITY (the intensity of the stack or
	 * of the color channel one voxel inside of the surface, with the LUT) or
	 * COLOR_DEPTH (the z position, with the LUT or Viridis for a gray LUT) or
	 * COLOR_IMAGE (the displayed colors of the image, e.g., of an RGB stack).
	 */
	public synchronized void setColorMode(int mode) {
		this.colorMode = mode;
		colorsDirty = true;
	}

	public synchronized int getColorChannel() {
		return colorChannel;
	}

	/**
	 * The channel of a multichannel stack for the intensity colors (0 = the
	 * channel of the surface). The stack is loaded again!
	 */
	public void setColorChannel(int channel) {
		synchronized (this) {
			this.colorChannel = Math.max(0, channel);
		}
		reload();
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
