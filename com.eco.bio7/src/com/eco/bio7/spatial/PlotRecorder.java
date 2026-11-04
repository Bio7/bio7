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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;

/**
 * Records a plot of the plot API (see Plot3D) frame by frame, e.g., a plot
 * which is updated in each step of a simulation (see Plot3D.update), and
 * exports the recording to an HTML page with WebGL: the plot with the axes, the
 * title and the legends of each frame, Play/Pause, a timeline, the speed and a
 * loop (the view can be rotated and zoomed while the recording plays) or for
 * Blender as an animation (see exportBlender).
 *
 * <pre>
 * PlotRecorder recorder = new PlotRecorder().speed(10);
 * for (int t = 0; t &lt; 100; t++) {
 * 	step(); // the model
 * 	plot.clearLayers().add(Plot3D.scatter(x, y, z)).update();
 * 	recorder.frame(plot);
 * }
 * recorder.export("/path/recording.html");
 * </pre>
 */
public class PlotRecorder {

	private final List<String> frames = new ArrayList<>();
	/* The plots of the frames (for the Blender export, see blender) */
	private final List<Plot3D> plots = new ArrayList<>();
	private boolean blender = true;
	private double speed = 10;
	private boolean loop = true, autoplay;
	private int maxFrames = 10000;
	private String title;

	/**
	 * Records the current state of a plot as a frame (the plot is computed if it
	 * was not displayed or updated).
	 *
	 * @param plot the plot
	 * @return the recorder
	 */
	public synchronized PlotRecorder frame(Plot3D plot) {
		if (frames.size() >= maxFrames) {
			return this;
		}
		frames.add(plot.frameJson());
		if (blender) {
			plots.add(plot.snapshot());
		}
		if (title == null) {
			title = plot.title();
		}
		return this;
	}

	/** The speed of the playback in frames per second (default 10). */
	public PlotRecorder speed(double framesPerSecond) {
		this.speed = Math.max(0.5, Math.min(120, framesPerSecond));
		return this;
	}

	/** Plays the recording again from the start (default true, a check box of the page). */
	public PlotRecorder loop(boolean loop) {
		this.loop = loop;
		return this;
	}

	/** Plays the recording when the page is opened (default false). */
	public PlotRecorder autoplay(boolean autoplay) {
		this.autoplay = autoplay;
		return this;
	}

	/** The maximum number of the frames (default 10000, further frames are ignored). */
	public PlotRecorder maxFrames(int maxFrames) {
		this.maxFrames = Math.max(1, maxFrames);
		return this;
	}

	/**
	 * Keeps the geometry of the frames for the Blender export (default true, false
	 * needs less memory for a long recording which is only exported to HTML).
	 */
	public PlotRecorder blender(boolean keep) {
		this.blender = keep;
		return this;
	}

	/**
	 * Exports the recording for Blender as an animation: a glTF file (*.glb) with
	 * the objects of all frames (an object which doesn't change is exported once)
	 * and a Python script with the same name (*.py) which imports the file and
	 * animates the visibility of the objects and the texts (the frames 1 ... n of
	 * the timeline of Blender with the speed of the recording), e.g., to render a
	 * film. In Blender: Scripting - Open - Run Script.
	 * 
	 * @param file the glTF file (*.glb), the script is written next to it
	 * @return the path of the Python script
	 * @throws IOException if a file cannot be written
	 */
	public synchronized String exportBlender(String file) throws IOException {
		if (plots.isEmpty()) {
			throw new IllegalStateException("No frames with the geometry are recorded (see frame and blender)");
		}
		Path glb = Path.of(file);
		String name = glb.getFileName().toString();
		String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
		Path script = glb.resolveSibling(base + ".py");
		Files.writeString(script, PlotGltf.writeAnimation(plots, glb, speed), StandardCharsets.UTF_8);
		return script.toString();
	}

	/** Returns the number of the recorded frames. */
	public synchronized int frames() {
		return frames.size();
	}

	/** Removes the recorded frames. */
	public synchronized PlotRecorder clear() {
		frames.clear();
		plots.clear();
		title = null;
		return this;
	}

	/** Returns the HTML page of the recording. */
	public synchronized String toHtml() {
		if (frames.isEmpty()) {
			throw new IllegalStateException("No frames are recorded (see frame)");
		}
		/* The frames as a JSON array (compressed, the web browser decompresses the data) */
		byte[] json = ("[" + String.join(",", frames) + "]").getBytes(StandardCharsets.UTF_8);
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		deflater.setInput(json);
		deflater.finish();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1 << 16];
		while (!deflater.finished()) {
			out.write(buffer, 0, deflater.deflate(buffer));
		}
		deflater.end();
		String data = "{\"data\":\"" + Base64.getEncoder().encodeToString(out.toByteArray()) + "\",\"fps\":" + PlotHtml.num(speed) + ",\"loop\":" + loop
				+ ",\"autoplay\":" + autoplay + "}";
		return PlotHtml.page(data, title);
	}

	/**
	 * Writes the HTML page of the recording.
	 *
	 * @param file the HTML file
	 * @return the recorder
	 * @throws IOException if the file cannot be written
	 */
	public PlotRecorder export(String file) throws IOException {
		Files.writeString(Path.of(file), toHtml(), StandardCharsets.UTF_8);
		return this;
	}
}
