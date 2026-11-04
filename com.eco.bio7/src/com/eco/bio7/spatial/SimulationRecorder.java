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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Records a simulation written in Java (e.g., a Bio7 model) step by step and
 * exports it to an HTML page which plays the recording: Play/Pause, Step,
 * Reset, the speed, a timeline to jump to a step, a loop and the chart (see
 * HtmlSimulation for the views). The model runs in Java (any model, the exact
 * results), the page only plays the recorded frames (the settings of the model
 * cannot be changed in the page).
 * <p>
 * Each step adds a frame with the data of the view: a grid of cells (GRID_2D,
 * with optional agents), points and lines (SCENE_3D) or a surface
 * (SURFACE_3D) and the values of the chart.
 *
 * <pre>
 * SimulationRecorder recorder = new SimulationRecorder("Game of Life").view(HtmlSimulation.View.GRID_2D)
 * 		.palette(0xFFFFFF, 0x1F3A93).chart("Population");
 * for (int t = 0; t &lt; 300; t++) {
 * 	step(); // the model
 * 	recorder.frame().grid(width, height, cells).chart("Population", alive);
 * }
 * recorder.export("/path/life.html");
 * </pre>
 *
 * In a Bio7 model the frame can be recorded in the run method (e.g., if
 * SpatialUtil.canStep() is true) and exported after a number of frames.
 */
public class SimulationRecorder {

	private final String title;
	private String description = "";
	private HtmlSimulation.View view = HtmlSimulation.View.GRID_2D;
	private int[] palette;
	private int[] colorMap = PlotLegend.VIRIDIS;
	private double[] range;
	private double[] box = { 1, 1, 1 };
	private double radius = 0.01;
	private int pointColor = 0x1F77B4, lineColor = 0x555555, agentColor = 0xD62728;
	private double agentRadius = 0.5;
	private double speed = 20;
	private String chartTitle;
	private boolean loop = true;
	private boolean autoplay;
	private String background = "#ffffff";
	private final List<Frame> frames = new ArrayList<>();
	private int maxFrames = 100000;

	/** Creates a recorder with the title of the page. */
	public SimulationRecorder(String title) {
		this.title = title != null ? title : "Simulation";
	}

	/** A description below the title. */
	public SimulationRecorder description(String description) {
		this.description = description != null ? description : "";
		return this;
	}

	/** The view (default GRID_2D). */
	public SimulationRecorder view(HtmlSimulation.View view) {
		this.view = view;
		return this;
	}

	/** The colors (0xRRGGBB) of the integer states of a grid (state 0, 1, 2, ...). */
	public SimulationRecorder palette(int... colors) {
		this.palette = colors.clone();
		return this;
	}

	/** The color map for continuous values of a grid or a surface (e.g., PlotLegend.VIRIDIS). */
	public SimulationRecorder colorMap(int[] ramp) {
		if (ramp == null || ramp.length < 2) {
			throw new IllegalArgumentException("The color map needs at least two colors");
		}
		this.colorMap = ramp.clone();
		return this;
	}

	/** The range of the values of the color map (and the height of a surface), default: the range of all frames. */
	public SimulationRecorder range(double min, double max) {
		if (!(max > min)) {
			throw new IllegalArgumentException("The maximum must be larger than the minimum");
		}
		this.range = new double[] { min, max };
		return this;
	}

	/** The size of the box of the 3D views (the points are in these coordinates, default 1, 1, 1). */
	public SimulationRecorder box(double width, double height, double depth) {
		this.box = new double[] { width, height, depth };
		return this;
	}

	/** The radius of the points (in the coordinates of the box) and their color (if not each point has a color). */
	public SimulationRecorder points(double radius, int color) {
		this.radius = radius;
		this.pointColor = color;
		return this;
	}

	/** The color of the lines (if not each line has a color). */
	public SimulationRecorder lineColor(int color) {
		this.lineColor = color;
		return this;
	}

	/** The radius (in cells) and the color of the agents of a grid (if not each agent has a color). */
	public SimulationRecorder agents(double radius, int color) {
		this.agentRadius = radius;
		this.agentColor = color;
		return this;
	}

	/** The default speed in frames per second (default 20). */
	public SimulationRecorder speed(double framesPerSecond) {
		this.speed = framesPerSecond;
		return this;
	}

	/** Displays a chart of the recorded values (see Frame.chart). */
	public SimulationRecorder chart(String title) {
		this.chartTitle = title != null ? title : "";
		return this;
	}

	/** Plays the recording again from the start at the end (default true, a check box of the page). */
	public SimulationRecorder loop(boolean loop) {
		this.loop = loop;
		return this;
	}

	/** Plays the recording when the page is opened (default false). */
	public SimulationRecorder autoplay(boolean autoplay) {
		this.autoplay = autoplay;
		return this;
	}

	/** The background color of the view (e.g., "#ffffff"). */
	public SimulationRecorder background(String color) {
		this.background = color;
		return this;
	}

	/** The maximum number of the frames (default 100000, further frames are ignored). */
	public SimulationRecorder maxFrames(int maxFrames) {
		this.maxFrames = Math.max(1, maxFrames);
		return this;
	}

	/**
	 * Adds a new frame (a step of the simulation) and returns it for the data.
	 *
	 * @return the frame
	 */
	public synchronized Frame frame() {
		Frame frame = new Frame();
		if (frames.size() < maxFrames) {
			frames.add(frame);
		}
		return frame;
	}

	/** Returns the number of the recorded frames. */
	public synchronized int frames() {
		return frames.size();
	}

	/** Removes the recorded frames. */
	public synchronized SimulationRecorder clear() {
		frames.clear();
		return this;
	}

	/**
	 * Records a number of steps: the function is called for each step with a new
	 * frame (it computes the step of the model and adds the data to the frame).
	 *
	 * @param steps the number of the steps
	 * @param step  the step of the model and the data of the frame
	 * @return the recorder
	 */
	public SimulationRecorder record(int steps, Consumer<Frame> step) {
		for (int i = 0; i < steps; i++) {
			step.accept(frame());
		}
		return this;
	}

	/* ----------------------------- A frame ----------------------------- */

	/** The data of a step: the grid, the agents, the points, the lines, the surface and the chart values. */
	public static final class Frame {
		int gridWidth, gridHeight;
		byte[] gridStates;
		float[] gridValues;
		float[] agentX, agentY;
		int[] agentColors;
		float[] points;
		int[] pointColors;
		float[] lines;
		int[] lineColors;
		int surfaceNx, surfaceNy;
		float[] surface;
		final Map<String, Double> chart = new LinkedHashMap<>();

		Frame() {
		}

		/** A grid of integer states (0 - 255, the colors of the palette), index y * width + x. */
		public Frame grid(int width, int height, int[] states) {
			checkSize(width, height, states.length);
			byte[] b = new byte[states.length];
			for (int i = 0; i < b.length; i++) {
				b[i] = (byte) Math.max(0, Math.min(255, states[i]));
			}
			return states(width, height, b);
		}

		/** A grid of integer states (0 - 255) as bytes, index y * width + x. */
		public Frame grid(int width, int height, byte[] states) {
			checkSize(width, height, states.length);
			return states(width, height, states.clone());
		}

		/** A grid of integer states as a 2D array cells[x][y] (e.g., the grid of a Bio7 model). */
		public Frame grid(int[][] cells) {
			int width = cells.length, height = cells[0].length;
			byte[] b = new byte[width * height];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					b[y * width + x] = (byte) Math.max(0, Math.min(255, cells[x][y]));
				}
			}
			return states(width, height, b);
		}

		/** A grid of continuous values (the colors of the color map), index y * width + x. */
		public Frame grid(int width, int height, double[] values) {
			checkSize(width, height, values.length);
			gridWidth = width;
			gridHeight = height;
			gridValues = toFloat(values);
			gridStates = null;
			return this;
		}

		/** A grid of continuous values as a 2D array values[x][y]. */
		public Frame grid(double[][] values) {
			int width = values.length, height = values[0].length;
			double[] v = new double[width * height];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					v[y * width + x] = values[x][y];
				}
			}
			return grid(width, height, v);
		}

		private Frame states(int width, int height, byte[] states) {
			gridWidth = width;
			gridHeight = height;
			gridStates = states;
			gridValues = null;
			return this;
		}

		/** Agents on the grid at the positions x, y (in cells, e.g., 10.5 = the center of the cell 10). */
		public Frame agents(double[] x, double[] y) {
			if (x.length != y.length) {
				throw new IllegalArgumentException("x and y need the same number of values");
			}
			agentX = toFloat(x);
			agentY = toFloat(y);
			agentColors = null;
			return this;
		}

		/** Agents on the grid with a color (0xRRGGBB) for each agent. */
		public Frame agents(double[] x, double[] y, int[] colors) {
			agents(x, y);
			if (colors.length != x.length) {
				throw new IllegalArgumentException("A color for each agent is needed");
			}
			agentColors = colors.clone();
			return this;
		}

		/** Points (spheres) of the 3D view (in the coordinates of the box). */
		public Frame points(double[] x, double[] y, double[] z) {
			if (x.length != y.length || x.length != z.length) {
				throw new IllegalArgumentException("x, y and z need the same number of values");
			}
			points = new float[x.length * 3];
			for (int i = 0; i < x.length; i++) {
				points[i * 3] = (float) x[i];
				points[i * 3 + 1] = (float) y[i];
				points[i * 3 + 2] = (float) z[i];
			}
			pointColors = null;
			return this;
		}

		/** Points (spheres) with a color (0xRRGGBB) for each point. */
		public Frame points(double[] x, double[] y, double[] z, int[] colors) {
			points(x, y, z);
			if (colors.length != x.length) {
				throw new IllegalArgumentException("A color for each point is needed");
			}
			pointColors = colors.clone();
			return this;
		}

		/** Lines of the 3D view: x0, y0, z0, x1, y1, z1 for each line (in the coordinates of the box). */
		public Frame lines(double[] xyz) {
			if (xyz.length % 6 != 0) {
				throw new IllegalArgumentException("Six values (two points) are needed for each line");
			}
			lines = toFloat(xyz);
			lineColors = null;
			return this;
		}

		/** Lines with a color (0xRRGGBB) for each line. */
		public Frame lines(double[] xyz, int[] colors) {
			lines(xyz);
			if (colors.length != xyz.length / 6) {
				throw new IllegalArgumentException("A color for each line is needed");
			}
			lineColors = colors.clone();
			return this;
		}

		/** A surface of the grid z[i][j] (SURFACE_3D, the height and the colors of the range). */
		public Frame surface(double[][] z) {
			surfaceNx = z.length;
			surfaceNy = z[0].length;
			surface = new float[surfaceNx * surfaceNy];
			for (int i = 0; i < surfaceNx; i++) {
				for (int j = 0; j < surfaceNy; j++) {
					surface[i * surfaceNy + j] = (float) z[i][j];
				}
			}
			return this;
		}

		/** A value of a series of the chart. */
		public Frame chart(String name, double value) {
			chart.put(name, value);
			return this;
		}

		private static void checkSize(int width, int height, int length) {
			if (width <= 0 || height <= 0 || width * height != length) {
				throw new IllegalArgumentException("The grid needs width * height values");
			}
		}

		private static float[] toFloat(double[] v) {
			float[] f = new float[v.length];
			for (int i = 0; i < v.length; i++) {
				f[i] = (float) v[i];
			}
			return f;
		}
	}

	/* ----------------------------- The export ----------------------------- */

	/** Returns the HTML page of the recording. */
	public synchronized String toHtml() {
		if (frames.isEmpty()) {
			throw new IllegalStateException("No frames are recorded (see frame or record)");
		}
		/* The range of the continuous values of all frames */
		double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
		for (Frame f : frames) {
			for (float[] values : new float[][] { f.gridValues, f.surface }) {
				if (values != null) {
					for (float v : values) {
						if (Float.isFinite(v)) {
							min = Math.min(min, v);
							max = Math.max(max, v);
						}
					}
				}
			}
		}
		if (range != null) {
			min = range[0];
			max = range[1];
		} else if (!(max > min)) {
			min = 0;
			max = 1;
		}
		StringBuilder r = new StringBuilder(1 << 16);
		r.append("{\"min\":").append(PlotHtml.num(min)).append(",\"max\":").append(PlotHtml.num(max));
		r.append(",\"box\":[").append(PlotHtml.num(box[0])).append(',').append(PlotHtml.num(box[1])).append(',').append(PlotHtml.num(box[2])).append(']');
		r.append(",\"radius\":").append(PlotHtml.num(radius));
		r.append(",\"pointColor\":").append(PlotHtml.string(hex(pointColor)));
		r.append(",\"lineColor\":").append(PlotHtml.string(hex(lineColor)));
		r.append(",\"agentColor\":").append(PlotHtml.string(hex(agentColor)));
		r.append(",\"agentRadius\":").append(PlotHtml.num(agentRadius));
		r.append(",\"palette\":");
		if (palette != null) {
			r.append('[');
			for (int i = 0; i < palette.length; i++) {
				r.append(i > 0 ? "," : "").append(PlotHtml.string(hex(palette[i])));
			}
			r.append(']');
		} else {
			r.append("null");
		}
		/* The series of the chart (one value per frame, null if missing) */
		List<String> names = new ArrayList<>();
		for (Frame f : frames) {
			for (String name : f.chart.keySet()) {
				if (!names.contains(name)) {
					names.add(name);
				}
			}
		}
		r.append(",\"series\":[");
		for (int k = 0; k < names.size(); k++) {
			r.append(k > 0 ? "," : "").append("{\"name\":").append(PlotHtml.string(names.get(k))).append(",\"values\":[");
			for (int i = 0; i < frames.size(); i++) {
				Double v = frames.get(i).chart.get(names.get(k));
				r.append(i > 0 ? "," : "").append(v != null && Double.isFinite(v) ? number(v) : "null");
			}
			r.append("]}");
		}
		/* The data of all frames in one block (compressed, aligned for the floats) */
		Blob blob = new Blob();
		r.append("],\"frames\":[");
		for (int i = 0; i < frames.size(); i++) {
			Frame f = frames.get(i);
			r.append(i > 0 ? "," : "").append('{');
			List<String> parts = new ArrayList<>();
			if (f.gridStates != null) {
				parts.add("\"w\":" + f.gridWidth + ",\"h\":" + f.gridHeight + ",\"gs\":" + blob.add(f.gridStates));
			} else if (f.gridValues != null) {
				parts.add("\"w\":" + f.gridWidth + ",\"h\":" + f.gridHeight + ",\"gv\":" + blob.add(f.gridValues));
			}
			if (f.agentX != null) {
				parts.add("\"ax\":" + blob.add(f.agentX) + ",\"ay\":" + blob.add(f.agentY));
				if (f.agentColors != null) {
					parts.add("\"ac\":" + blob.add(rgb(f.agentColors)));
				}
			}
			if (f.points != null) {
				parts.add("\"p\":" + blob.add(f.points));
				if (f.pointColors != null) {
					parts.add("\"pc\":" + blob.add(rgb(f.pointColors)));
				}
			}
			if (f.lines != null) {
				parts.add("\"l\":" + blob.add(f.lines));
				if (f.lineColors != null) {
					parts.add("\"lc\":" + blob.add(rgb(f.lineColors)));
				}
			}
			if (f.surface != null) {
				parts.add("\"nx\":" + f.surfaceNx + ",\"ny\":" + f.surfaceNy + ",\"s\":" + blob.add(f.surface));
			}
			r.append(String.join(",", parts)).append('}');
		}
		r.append("],\"data\":\"").append(blob.compressed()).append("\"}");
		HtmlSimulation sim = new HtmlSimulation(title).description(description).view(view).colorMap(colorMap).speed(speed).background(background).autoplay(autoplay)
				.checkbox("loop", "Loop (play again from the start)", loop).timeline(frames.size()).code(PLAYER.replace("/*RECORDING*/", r.toString()));
		if (chartTitle != null) {
			sim.chart(chartTitle);
		}
		return sim.toHtml();
	}

	/**
	 * Writes the HTML page of the recording.
	 *
	 * @param file the HTML file
	 * @return the recorder
	 * @throws IOException if the file cannot be written
	 */
	public SimulationRecorder export(String file) throws IOException {
		Files.writeString(Path.of(file), toHtml(), StandardCharsets.UTF_8);
		return this;
	}

	private static String hex(int rgb) {
		return String.format(java.util.Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
	}

	private static String number(double v) {
		if (v == Math.rint(v) && Math.abs(v) < 1e15) {
			return Long.toString((long) v);
		}
		return Double.toString(v);
	}

	private static byte[] rgb(int[] colors) {
		byte[] b = new byte[colors.length * 3];
		for (int i = 0; i < colors.length; i++) {
			b[i * 3] = (byte) (colors[i] >> 16);
			b[i * 3 + 1] = (byte) (colors[i] >> 8);
			b[i * 3 + 2] = (byte) colors[i];
		}
		return b;
	}

	/*
	 * The binary data of the frames: the arrays are appended (aligned to 4 bytes
	 * for the floats) and referenced with [offset, length], the block is
	 * compressed (zlib, DecompressionStream('deflate') of the web browser).
	 */
	private static final class Blob {
		private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(1 << 20);

		String add(byte[] b) {
			int offset = out.size();
			out.write(b, 0, b.length);
			while (out.size() % 4 != 0) {
				out.write(0);
			}
			return "[" + offset + "," + b.length + "]";
		}

		/*
		 * The floats as 16 bit values of their range (1/65535 of the range, enough for
		 * the display): [offset, length, min, max], 65535 = not finite (NaN).
		 */
		String add(float[] f) {
			float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
			for (float v : f) {
				if (Float.isFinite(v)) {
					min = Math.min(min, v);
					max = Math.max(max, v);
				}
			}
			if (!(max >= min)) {
				min = 0;
				max = 0;
			}
			double span = max - min;
			ByteBuffer buffer = ByteBuffer.allocate(f.length * 2).order(ByteOrder.LITTLE_ENDIAN);
			for (float v : f) {
				int q = !Float.isFinite(v) ? 65535 : span > 0 ? (int) Math.round((v - min) / span * 65534) : 0;
				buffer.putShort((short) q);
			}
			String ref = add(buffer.array());
			return ref.substring(0, ref.length() - 1) + "," + Float.toString(min) + "," + Float.toString(max) + "]";
		}

		String compressed() {
			java.util.zip.Deflater deflater = new java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION);
			deflater.setInput(out.toByteArray());
			deflater.finish();
			java.io.ByteArrayOutputStream z = new java.io.ByteArrayOutputStream();
			byte[] buffer = new byte[1 << 16];
			while (!deflater.finished()) {
				int n = deflater.deflate(buffer);
				z.write(buffer, 0, n);
			}
			deflater.end();
			return Base64.getEncoder().encodeToString(z.toByteArray());
		}
	}

	/* The player of the recording (the code of the HtmlSimulation) */
	private static final String PLAYER = """
			const REC = /*RECORDING*/;
			/* The data of the frames: decompressed in prepare(sim) before the start */
			let DATA = null;
			async function prepare(sim) {
			  const s = atob(REC.data), z = new Uint8Array(s.length);
			  for (let i = 0; i < s.length; i++) z[i] = s.charCodeAt(i);
			  const stream = new Blob([z]).stream().pipeThrough(new DecompressionStream('deflate'));
			  DATA = await new Response(stream).arrayBuffer();
			}
			function bytes(ref) { return new Uint8Array(DATA, ref[0], ref[1]); }
			/* The 16 bit values of the range [ref[2], ref[3]] as floats (65535 = NaN) */
			function floats(ref) {
			  const q = new Uint16Array(DATA, ref[0], ref[1] / 2), f = new Float32Array(q.length), min = ref[2], scale = (ref[3] - ref[2]) / 65534;
			  for (let i = 0; i < q.length; i++) f[i] = q[i] === 65535 ? NaN : min + q[i] * scale;
			  return f;
			}
			function rgbFloats(b) { const c = new Float32Array(b.length); for (let i = 0; i < b.length; i++) c[i] = b[i] / 255; return c; }
			function hexColors(b) { const c = []; for (let i = 0; i < b.length; i += 3) c.push('rgb(' + b[i] + ',' + b[i + 1] + ',' + b[i + 2] + ')'); return c; }

			/* Displays the frame k */
			function show(sim, k) {
			  const f = REC.frames[k];
			  sim.frame = k;
			  if (f.w) {
			    sim.width = f.w; sim.height = f.h;
			    if (f.gs) { sim.cells = bytes(f.gs); sim.palette = REC.palette || null; }
			    else { sim.cells = floats(f.gv); sim.palette = null; }
			  }
			  sim.agents = f.ax ? { x: floats(f.ax), y: floats(f.ay), colors: f.ac ? hexColors(bytes(f.ac)) : null, color: REC.agentColor, radius: REC.agentRadius } : null;
			  sim.points = f.p ? { xyz: floats(f.p), colors: f.pc ? rgbFloats(bytes(f.pc)) : null, color: REC.pointColor, radius: REC.radius } : null;
			  if (f.l) {
			    const xyz = floats(f.l);
			    let colors = null;
			    if (f.lc) {
			      /* One color for each line: the color for both points */
			      const c = rgbFloats(bytes(f.lc)); colors = new Float32Array(xyz.length);
			      for (let i = 0; i < c.length / 3; i++) for (let v = 0; v < 2; v++) for (let q = 0; q < 3; q++) colors[i * 6 + v * 3 + q] = c[i * 3 + q];
			    }
			    sim.lines = { xyz: xyz, colors: colors, color: REC.lineColor };
			  } else {
			    sim.lines = null;
			  }
			  sim.surface = f.s ? { nx: f.nx, ny: f.ny, z: floats(f.s), min: REC.min, max: REC.max } : null;
			}

			/* The values of the chart up to the frame k */
			function chartTo(sim, k) {
			  sim.clearChart();
			  const t = sim.t;
			  for (let i = 0; i <= k; i++) {
			    sim.t = i;
			    REC.series.forEach(function (s) { if (s.values[i] !== null) sim.chart(s.name, s.values[i]); });
			  }
			  sim.t = t;
			}

			function setup(sim) {
			  sim.box = REC.box; sim.min = REC.min; sim.max = REC.max;
			  show(sim, 0);
			  chartTo(sim, 0);
			}

			function step(sim) {
			  let k = sim.frame + 1;
			  if (k >= REC.frames.length) {
			    if (!sim.p.loop) return false;
			    /* Again from the start (sim.t is 0 after the step) */
			    show(sim, 0); sim.t = -1; chartTo(sim, -1); sim.t = 0;
			    REC.series.forEach(function (s) { if (s.values[0] !== null) sim.chart(s.name, s.values[0]); });
			    sim.t = -1;
			    return true;
			  }
			  show(sim, k);
			  const t = sim.t; sim.t = k;
			  REC.series.forEach(function (s) { if (s.values[k] !== null) sim.chart(s.name, s.values[k]); });
			  sim.t = t;
			}

			function seek(sim, k) {
			  show(sim, k);
			  chartTo(sim, k);
			}
			""";
}
