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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Exports an interactive simulation to an HTML page (a single file without
 * dependencies, WebGL for 3D): Play/Pause, Step, Reset, the speed and controls
 * for the settings (sliders, number fields, text fields, check boxes, choices
 * and buttons) with a 2D grid view (e.g., a cellular automaton), a 3D view
 * (e.g., agents of a swarm model) or a dynamic 3D surface and an optional chart
 * of values over time.
 * <p>
 * The simulation runs in the web browser: the model is written in JavaScript
 * with the functions setup(sim) (called at the start and with Reset) and
 * step(sim) (one time step). The settings are available as sim.p.name. The
 * view displays (an optional function draw(sim) is called after the view is drawn):
 * <ul>
 * <li>GRID_2D: sim.cells (an array of sim.width * sim.height values, index y *
 * width + x) with sim.palette (colors of the integer states, e.g., ['#ffffff',
 * '#000000']) or the color map with sim.min and sim.max and optional agents
 * sim.agents = {x: [], y: [], color: '#d62728', radius: 0.5} (cell units).</li>
 * <li>SCENE_3D: sim.box = [w, h, d] and sim.points = {xyz: [x0, y0, z0, x1,
 * ...], colors: [r, g, b, ...] (0 - 1, optional), color: '#1f77b4', radius:
 * 0.01} and sim.lines = {xyz: [...], color: '#555555'} (pairs of points).</li>
 * <li>SURFACE_3D: sim.surface = {nx, ny, z: [...] (index i * ny + j), min, max}
 * colored with the color map (and points and lines like SCENE_3D).</li>
 * </ul>
 * sim.chart('Name', value) adds a value to a series of the chart (one value
 * per step), sim.clearChart() removes the values. sim.t is the number of the
 * steps, sim.random() a random number and sim.pause() pauses the simulation
 * (step(sim) can also return false to stop). An optional (async) function
 * prepare(sim) is called once before the start (e.g., to load data).
 * <p>
 * Example:
 *
 * <pre>
 * new HtmlSimulation("Game of Life").view(HtmlSimulation.View.GRID_2D).slider("density", "Start density", 0, 1, 0.01, 0.3)
 * 		.number("size", "Grid size", 100).restartOn("size", "density").chart("Population").code(js).export("/path/life.html");
 * </pre>
 */
public class HtmlSimulation {

	/** The view of the simulation. */
	public enum View {
		/** A grid of cells (e.g., a cellular automaton) with optional agents. */
		GRID_2D,
		/** Points (spheres) and lines in a 3D box (e.g., a swarm model). */
		SCENE_3D,
		/** A dynamic surface (e.g., a diffusion) in a 3D box. */
		SURFACE_3D
	}

	private final String title;
	private String description = "";
	private View view = View.GRID_2D;
	private final List<String> controls = new ArrayList<>();
	private final Set<String> names = new LinkedHashSet<>();
	private final Set<String> restart = new LinkedHashSet<>();
	private String chartTitle;
	private String code;
	private int[] colorMap = PlotLegend.VIRIDIS;
	private double speed = 20;
	private boolean autoplay = false;
	private String background = "#ffffff";
	private int timeline = 0;

	/**
	 * Creates a simulation with a title (the math markup of the plots, e.g., x^2,
	 * N_0, is possible).
	 */
	public HtmlSimulation(String title) {
		this.title = title != null ? title : "Simulation";
	}

	/** A description below the title (plain text, lines separated with \n). */
	public HtmlSimulation description(String description) {
		this.description = description != null ? description : "";
		return this;
	}

	/** The view (default GRID_2D). */
	public HtmlSimulation view(View view) {
		this.view = view;
		return this;
	}

	/** The colors of the color map (e.g., PlotLegend.VIRIDIS) for continuous values. */
	public HtmlSimulation colorMap(int[] ramp) {
		if (ramp == null || ramp.length < 2) {
			throw new IllegalArgumentException("The color map needs at least two colors");
		}
		this.colorMap = ramp.clone();
		return this;
	}

	/** The default speed in steps per second (default 20). */
	public HtmlSimulation speed(double stepsPerSecond) {
		this.speed = Math.max(0.5, Math.min(500, stepsPerSecond));
		return this;
	}

	/** Starts the simulation when the page is opened (default false). */
	public HtmlSimulation autoplay(boolean autoplay) {
		this.autoplay = autoplay;
		return this;
	}

	/** The background color of the view (e.g., "#ffffff"). */
	public HtmlSimulation background(String color) {
		this.background = color;
		return this;
	}

	/**
	 * Displays a timeline slider (0 to frames - 1) for the steps: moving the slider
	 * calls the function seek(sim, step) of the code (e.g., to display a recorded
	 * frame, see SimulationRecorder) and sets sim.t.
	 */
	public HtmlSimulation timeline(int frames) {
		this.timeline = Math.max(0, frames);
		return this;
	}

	/** Displays a chart of the values of sim.chart(name, value) over the steps. */
	public HtmlSimulation chart(String title) {
		this.chartTitle = title != null ? title : "";
		return this;
	}

	/* ----------------------------- The controls ----------------------------- */

	/** A slider for a number (sim.p.name). */
	public HtmlSimulation slider(String name, String label, double min, double max, double step, double value) {
		check(name);
		controls.add("{\"type\":\"slider\",\"name\":" + PlotHtml.string(name) + ",\"label\":" + PlotHtml.string(PlotHtml.html(label)) + ",\"min\":"
				+ PlotHtml.num(min) + ",\"max\":" + PlotHtml.num(max) + ",\"step\":" + number(step) + ",\"value\":" + number(value) + "}");
		return this;
	}

	/** A number field (sim.p.name). */
	public HtmlSimulation number(String name, String label, double value) {
		check(name);
		controls.add("{\"type\":\"number\",\"name\":" + PlotHtml.string(name) + ",\"label\":" + PlotHtml.string(PlotHtml.html(label)) + ",\"value\":"
				+ number(value) + "}");
		return this;
	}

	/** A text field (sim.p.name is a string). */
	public HtmlSimulation text(String name, String label, String value) {
		check(name);
		controls.add("{\"type\":\"text\",\"name\":" + PlotHtml.string(name) + ",\"label\":" + PlotHtml.string(PlotHtml.html(label)) + ",\"value\":"
				+ PlotHtml.string(value != null ? value : "") + "}");
		return this;
	}

	/** A check box (sim.p.name is true or false). */
	public HtmlSimulation checkbox(String name, String label, boolean value) {
		check(name);
		controls.add("{\"type\":\"checkbox\",\"name\":" + PlotHtml.string(name) + ",\"label\":" + PlotHtml.string(PlotHtml.html(label)) + ",\"value\":"
				+ value + "}");
		return this;
	}

	/** A choice of options (sim.p.name is the selected option). */
	public HtmlSimulation choice(String name, String label, String[] options, String value) {
		check(name);
		StringBuilder o = new StringBuilder();
		for (String option : options) {
			o.append(o.length() > 0 ? "," : "").append(PlotHtml.string(option));
		}
		controls.add("{\"type\":\"choice\",\"name\":" + PlotHtml.string(name) + ",\"label\":" + PlotHtml.string(PlotHtml.html(label)) + ",\"options\":["
				+ o + "],\"value\":" + PlotHtml.string(value != null ? value : options.length > 0 ? options[0] : "") + "}");
		return this;
	}

	/** A button which calls a function of the code with the simulation (e.g., "clear" for function clear(sim)). */
	public HtmlSimulation button(String label, String function) {
		if (function == null || !function.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
			throw new IllegalArgumentException("The button needs the name of a JavaScript function: " + function);
		}
		controls.add("{\"type\":\"button\",\"label\":" + PlotHtml.string(PlotHtml.html(label)) + ",\"fn\":" + PlotHtml.string(function) + "}");
		return this;
	}

	/** A heading between the controls. */
	public HtmlSimulation heading(String label) {
		controls.add("{\"type\":\"heading\",\"label\":" + PlotHtml.string(PlotHtml.html(label)) + "}");
		return this;
	}

	/** The settings which restart the simulation (setup) if they are changed (e.g., the size of a grid). */
	public HtmlSimulation restartOn(String... names) {
		restart.addAll(Arrays.asList(names));
		return this;
	}

	private void check(String name) {
		if (name == null || !name.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
			throw new IllegalArgumentException("The name of a setting must be a JavaScript name: " + name);
		}
		if (!names.add(name)) {
			throw new IllegalArgumentException("The name of the setting is used twice: " + name);
		}
	}

	private static String number(double v) {
		if (v == Math.rint(v) && Math.abs(v) < 1e15) {
			return Long.toString((long) v);
		}
		return Double.toString(v);
	}

	/* ----------------------------- The code and the export ----------------------------- */

	/** The JavaScript code of the model with the functions setup(sim) and step(sim). */
	public HtmlSimulation code(String javaScript) {
		this.code = javaScript;
		return this;
	}

	/** The JavaScript code of the model from a file (the code is embedded in the page, see scriptFile). */
	public HtmlSimulation codeFile(String file) throws IOException {
		this.code = Files.readString(Path.of(file), StandardCharsets.UTF_8);
		this.scriptFile = null;
		return this;
	}

	/* The JavaScript file of the model which is referenced by the page (see scriptFile) */
	private Path scriptFile;
	/* The script file is merged into the page (one file, see embedScript) */
	private boolean embedScript;

	/**
	 * Merges the script file (see scriptFile) into the page when it is exported:
	 * one HTML file without the script file (the script file stays the source to
	 * edit the model). Default false: the page loads the script file next to it.
	 * 
	 * @param embed true for one HTML file
	 * @return the simulation
	 */
	public HtmlSimulation embedScript(boolean embed) {
		this.embedScript = embed;
		return this;
	}

	/**
	 * The JavaScript code of the model in a separate file (e.g., edited in an
	 * editor of Bio7): the page loads the file (script src) and export copies the
	 * file next to the page (if it is in another folder). The page and the script
	 * file belong together (the script file is loaded when the page is opened).
	 * 
	 * @param file the JavaScript file with setup(sim) and step(sim) (see
	 *             writeScriptTemplate)
	 * @return the simulation
	 */
	public HtmlSimulation scriptFile(String file) {
		this.scriptFile = Path.of(file).toAbsolutePath().normalize();
		this.code = null;
		return this;
	}

	/**
	 * Writes a template of a JavaScript file of a model (setup and step with
	 * comments of the available values) if the file does not exist (e.g., to
	 * edit the model in an editor of Bio7, see scriptFile).
	 * 
	 * @param file the JavaScript file
	 * @return true if the template was written (false if the file exists)
	 * @throws IOException if the file cannot be written
	 */
	public static boolean writeScriptTemplate(String file) throws IOException {
		Path path = Path.of(file);
		if (Files.exists(path)) {
			return false;
		}
		if (path.getParent() != null) {
			Files.createDirectories(path.getParent());
		}
		Files.writeString(path, SCRIPT_TEMPLATE, StandardCharsets.UTF_8);
		return true;
	}

	private static final String SCRIPT_TEMPLATE = """
			/*
			 * The model of an HtmlSimulation of Bio7 (loaded by the HTML page).
			 * sim.p: the settings of the controls (e.g., sim.p.rate), sim.t: the step,
			 * sim.random(): a random number, sim.chart(name, value): a value of the chart.
			 * The views: GRID_2D (sim.width, sim.height, sim.cells, sim.palette or sim.min, sim.max),
			 * SCENE_3D (sim.box, sim.points, sim.lines), SURFACE_3D (sim.surface).
			 */

			/* Called at the start and with Reset */
			function setup(sim) {
			  const n = 100;
			  sim.width = n;
			  sim.height = n;
			  sim.cells = new Uint8Array(n * n);
			  sim.palette = ['#ffffff', '#1f3a93'];
			  for (let i = 0; i < sim.cells.length; i++) sim.cells[i] = sim.random() < 0.2 ? 1 : 0;
			}

			/* One step of the model (return false to stop) */
			function step(sim) {
			  let alive = 0;
			  for (let i = 0; i < sim.cells.length; i++) {
			    if (sim.random() < 0.01) sim.cells[i] = 1 - sim.cells[i];
			    alive += sim.cells[i];
			  }
			  sim.chart('Cells', alive);
			}
			""";

	/** Returns the HTML page. */
	public String toHtml() {
		if (scriptFile == null && (code == null || code.isBlank())) {
			throw new IllegalStateException("The simulation needs JavaScript code with setup(sim) and step(sim) (see code or scriptFile)");
		}
		StringBuilder config = new StringBuilder("{");
		config.append("\"title\":").append(PlotHtml.string(PlotHtml.html(title))).append(',');
		config.append("\"description\":").append(PlotHtml.string(PlotHtml.escape(description).replace("\n", "<br>"))).append(',');
		config.append("\"view\":").append(PlotHtml.string(view.name())).append(',');
		config.append("\"controls\":[").append(String.join(",", controls)).append("],");
		config.append("\"restart\":[");
		int i = 0;
		for (String r : restart) {
			config.append(i++ > 0 ? "," : "").append(PlotHtml.string(r));
		}
		config.append("],");
		config.append("\"chart\":").append(chartTitle != null ? PlotHtml.string(PlotHtml.html(chartTitle)) : "null").append(',');
		config.append("\"colorMap\":[");
		for (int k = 0; k < colorMap.length; k++) {
			config.append(k > 0 ? "," : "").append(PlotHtml.string(String.format(java.util.Locale.ROOT, "#%06X", colorMap[k] & 0xFFFFFF)));
		}
		config.append("],");
		config.append("\"speed\":").append(PlotHtml.num(speed)).append(',');
		config.append("\"autoplay\":").append(autoplay).append(',');
		config.append("\"timeline\":").append(timeline).append(',');
		config.append("\"background\":").append(PlotHtml.string(background));
		config.append('}');
		String page = TEMPLATE.replace("<title></title>", "<title>" + PlotHtml.escape(title) + "</title>").replace("/*CONFIG*/", config.toString());
		if (scriptFile != null && embedScript) {
			/* The script file merged into the page (read at the export) */
			String script;
			try {
				script = Files.readString(scriptFile, StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new IllegalStateException("The script file cannot be read: " + scriptFile, e);
			}
			return page.replace("/*USERCODE*/", "/* The model of " + scriptFile.getFileName() + " */\n"
					+ script.replace("</script", "<\\/script").replace("</SCRIPT", "<\\/SCRIPT"));
		}
		if (scriptFile != null) {
			/* The page loads the script file (next to the page) */
			return page.replace("<script>\n/*USERCODE*/\n</script>",
					"<script src=\"" + PlotHtml.escape(scriptFile.getFileName().toString()) + "\"></script>");
		}
		/* The code must not end the script element */
		String userCode = code.replace("</script", "<\\/script").replace("</SCRIPT", "<\\/SCRIPT");
		return page.replace("/*USERCODE*/", userCode);
	}

	/**
	 * Writes the HTML page.
	 *
	 * @param file the HTML file
	 * @return the simulation
	 * @throws IOException if the file cannot be written
	 */
	public HtmlSimulation export(String file) throws IOException {
		Path page = Path.of(file).toAbsolutePath().normalize();
		if (scriptFile != null && !Files.exists(scriptFile)) {
			throw new IOException("The script file does not exist: " + scriptFile);
		}
		Files.writeString(page, toHtml(), StandardCharsets.UTF_8);
		/* The script file next to the page (not for a merged script) */
		if (scriptFile != null && !embedScript) {
			Path target = page.resolveSibling(scriptFile.getFileName());
			if (!target.equals(scriptFile)) {
				Files.copy(scriptFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		}
		return this;
	}

	/** Opens a file (e.g., an exported page) with the default application of the system (the web browser). */
	public static void open(String file) {
		org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getDefault();
		display.asyncExec(() -> org.eclipse.swt.program.Program.launch(Path.of(file).toAbsolutePath().toString()));
	}

	/* The page: the controls, the views and the runtime of the simulation */
	private static final String TEMPLATE = """
			<!DOCTYPE html>
			<html>
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<title></title>
			<style>
			html, body { margin: 0; height: 100%; font-family: Verdana, Arial, sans-serif; color: #222; background: #f4f4f4; }
			body { display: flex; }
			#side { width: 270px; min-width: 270px; padding: 14px; box-sizing: border-box; background: #ffffff; border-right: 1px solid #ccc; overflow-y: auto; }
			#side h1 { font-size: 18px; margin: 0 0 6px 0; }
			#desc { font-size: 12px; color: #555; margin-bottom: 12px; }
			#main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
			#viewbox { flex: 1; position: relative; min-height: 0; }
			#view, #view3d { position: absolute; left: 0; top: 0; width: 100%; height: 100%; display: block; }
			#chartbox { height: 200px; border-top: 1px solid #ccc; background: #ffffff; position: relative; }
			#chart { width: 100%; height: 100%; display: block; }
			.buttons { display: flex; gap: 6px; margin-bottom: 10px; }
			button { font-family: inherit; font-size: 13px; padding: 5px 10px; border: 1px solid #999; border-radius: 4px; background: #f0f0f0; cursor: pointer; }
			button:hover { background: #e2e2e2; }
			button.play { min-width: 78px; font-weight: bold; }
			.control { margin: 9px 0; font-size: 12px; }
			.control label { display: block; margin-bottom: 3px; }
			.control .value { float: right; color: #555; font-variant-numeric: tabular-nums; }
			.control input[type=range] { width: 100%; }
			.control input[type=number], .control input[type=text], .control select { width: 100%; box-sizing: border-box; padding: 3px 5px; font-family: inherit; font-size: 12px; }
			.control.check label { display: inline; }
			h2 { font-size: 13px; margin: 14px 0 4px 0; border-bottom: 1px solid #ddd; padding-bottom: 2px; }
			#status { font-size: 12px; color: #555; margin: 6px 0 10px 0; font-variant-numeric: tabular-nums; }
			#error { color: #b00020; font-size: 12px; white-space: pre-wrap; }
			#hint { position: absolute; bottom: 6px; left: 10px; font-size: 11px; color: #888; pointer-events: none; }
			sup, sub { font-size: 70%; }
			</style>
			</head>
			<body>
			<div id="side">
			<h1 id="title"></h1>
			<div id="desc"></div>
			<div class="buttons"><button id="play" class="play">Play</button><button id="step">Step</button><button id="reset">Reset</button></div>
			<div class="control"><label>Speed <span class="value" id="speedValue"></span></label><input type="range" id="speed" min="0" max="100" step="1"></div>
			<div id="status">Step 0</div>
			<div class="control" id="timelineBox" style="display:none"><label>Timeline</label><input type="range" id="timeline" min="0" max="0" step="1" value="0"></div>
			<div id="controls"></div>
			<div id="error"></div>
			</div>
			<div id="main">
			<div id="viewbox"><canvas id="view"></canvas><canvas id="view3d" style="display:none"></canvas><div id="hint"></div></div>
			<div id="chartbox" style="display:none"><canvas id="chart"></canvas></div>
			</div>
			<script>
			/*USERCODE*/
			</script>
			<script>
			(function () {
			const CONFIG = /*CONFIG*/;
			document.getElementById('title').innerHTML = CONFIG.title;
			document.getElementById('desc').innerHTML = CONFIG.description;
			const errorElement = document.getElementById('error');
			function userFunction(name) { try { return new Function('return typeof ' + name + ' === "function" ? ' + name + ' : null;')(); } catch (e) { return null; } }
			function report(e) { errorElement.textContent = 'Error: ' + (e && e.message ? e.message : e); playing = false; updatePlay(); console.error(e); }

			/* ----- The colors ----- */
			function hexColor(h) { return [parseInt(h.substr(1, 2), 16), parseInt(h.substr(3, 2), 16), parseInt(h.substr(5, 2), 16)]; }
			const ramp = CONFIG.colorMap.map(hexColor);
			function rampColor(t) {
			  if (!(t >= 0)) t = 0; if (t > 1) t = 1;
			  const p = t * (ramp.length - 1), a = Math.min(ramp.length - 2, Math.floor(p)), w = p - a;
			  return [ramp[a][0] + w * (ramp[a + 1][0] - ramp[a][0]), ramp[a][1] + w * (ramp[a + 1][1] - ramp[a][1]), ramp[a][2] + w * (ramp[a + 1][2] - ramp[a][2])];
			}
			const CATEGORY = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd', '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'];

			/* ----- The simulation object ----- */
			const series = new Map();
			const sim = {
			  p: {}, t: 0, width: 0, height: 0, cells: null, palette: null, min: 0, max: 1, agents: null,
			  box: [1, 1, 1], points: null, lines: null, surface: null,
			  random: Math.random,
			  chart: function (name, value) { if (!series.has(name)) series.set(name, []); series.get(name).push([this.t, value]); },
			  clearChart: function () { series.clear(); },
			  pause: function () { playing = false; updatePlay(); },
			  rampColor: function (t) { const c = rampColor(t); return [c[0] / 255, c[1] / 255, c[2] / 255]; }
			};

			/* ----- The controls ----- */
			const controlsElement = document.getElementById('controls');
			function formatValue(v, step) { const d = step >= 1 ? 0 : Math.min(6, Math.ceil(-Math.log10(step) - 1e-9)); return Number(v).toFixed(d); }
			CONFIG.controls.forEach(function (c) {
			  if (c.type === 'heading') { const h = document.createElement('h2'); h.innerHTML = c.label; controlsElement.appendChild(h); return; }
			  const div = document.createElement('div'); div.className = 'control'; controlsElement.appendChild(div);
			  if (c.type === 'button') {
			    const b = document.createElement('button'); b.innerHTML = c.label; div.appendChild(b);
			    b.addEventListener('click', function () {
			      try { const fn = userFunction(c.fn); if (!fn) throw new Error('The function ' + c.fn + '(sim) is not defined'); fn(sim); render(); }
			      catch (e) { report(e); }
			    });
			    return;
			  }
			  sim.p[c.name] = c.value;
			  const changed = function () { if (CONFIG.restart.indexOf(c.name) >= 0) reset(); else render(); };
			  if (c.type === 'checkbox') {
			    div.className = 'control check';
			    const input = document.createElement('input'); input.type = 'checkbox'; input.checked = c.value; input.id = 'c_' + c.name;
			    const label = document.createElement('label'); label.htmlFor = input.id; label.innerHTML = ' ' + c.label;
			    div.appendChild(input); div.appendChild(label);
			    input.addEventListener('change', function () { sim.p[c.name] = input.checked; changed(); });
			    return;
			  }
			  const label = document.createElement('label'); label.innerHTML = c.label; div.appendChild(label);
			  let input;
			  if (c.type === 'slider') {
			    const value = document.createElement('span'); value.className = 'value'; value.textContent = formatValue(c.value, c.step); label.appendChild(value);
			    input = document.createElement('input'); input.type = 'range'; input.min = c.min; input.max = c.max; input.step = c.step; input.value = c.value;
			    input.addEventListener('input', function () { sim.p[c.name] = parseFloat(input.value); value.textContent = formatValue(input.value, c.step); if (CONFIG.restart.indexOf(c.name) < 0) render(); });
			    input.addEventListener('change', function () { if (CONFIG.restart.indexOf(c.name) >= 0) reset(); });
			  } else if (c.type === 'number') {
			    input = document.createElement('input'); input.type = 'number'; input.value = c.value; input.step = 'any';
			    input.addEventListener('change', function () { const v = parseFloat(input.value); if (isFinite(v)) { sim.p[c.name] = v; changed(); } });
			  } else if (c.type === 'text') {
			    input = document.createElement('input'); input.type = 'text'; input.value = c.value;
			    input.addEventListener('change', function () { sim.p[c.name] = input.value; changed(); });
			  } else if (c.type === 'choice') {
			    input = document.createElement('select');
			    c.options.forEach(function (o) { const opt = document.createElement('option'); opt.value = o; opt.textContent = o; input.appendChild(opt); });
			    input.value = c.value;
			    input.addEventListener('change', function () { sim.p[c.name] = input.value; changed(); });
			  }
			  div.appendChild(input);
			});

			/* ----- Play, step, reset and the speed ----- */
			let playing = false, accumulator = 0, lastTime = 0, stepsPerSecond = CONFIG.speed;
			const playButton = document.getElementById('play'), statusElement = document.getElementById('status');
			const speedInput = document.getElementById('speed'), speedValue = document.getElementById('speedValue');
			/* The speed slider: 0.5 - 500 steps per second (logarithmic) */
			function speedFromSlider(v) { return 0.5 * Math.pow(1000, v / 100); }
			speedInput.value = Math.round(100 * Math.log(stepsPerSecond / 0.5) / Math.log(1000));
			function showSpeed() { speedValue.textContent = (stepsPerSecond < 10 ? stepsPerSecond.toFixed(1) : Math.round(stepsPerSecond)) + ' steps/s'; }
			showSpeed();
			speedInput.addEventListener('input', function () { stepsPerSecond = speedFromSlider(parseFloat(speedInput.value)); showSpeed(); });
			function updatePlay() { playButton.textContent = playing ? 'Pause' : 'Play'; }
			playButton.addEventListener('click', function () { playing = !playing; updatePlay(); lastTime = performance.now(); accumulator = 0; if (playing) requestAnimationFrame(loop); });
			document.getElementById('step').addEventListener('click', function () { if (doStep()) render(); });
			document.getElementById('reset').addEventListener('click', reset);
			function doStep() {
			  try {
			    if (typeof step !== 'function') throw new Error('The code needs a function step(sim)');
			    /* step(sim) returns false to stop (e.g., at the end of a recording) */
			    if (step(sim) === false) { playing = false; updatePlay(); return false; }
			    sim.t++; return true;
			  }
			  catch (e) { report(e); return false; }
			}
			function reset() {
			  errorElement.textContent = ''; series.clear(); sim.t = 0;
			  sim.cells = null; sim.agents = null; sim.points = null; sim.lines = null; sim.surface = null;
			  try { if (typeof setup !== 'function') throw new Error('The code needs a function setup(sim)'); setup(sim); }
			  catch (e) { report(e); }
			  render();
			}
			function loop(now) {
			  if (!playing) return;
			  accumulator += Math.min(0.25, (now - lastTime) / 1000) * stepsPerSecond; lastTime = now;
			  let n = 0;
			  while (accumulator >= 1 && n < 200) { if (!doStep()) break; accumulator -= 1; n++; }
			  if (n > 0) render();
			  requestAnimationFrame(loop);
			}

			/* ----- The 2D grid view ----- */
			const view = document.getElementById('view'), view3d = document.getElementById('view3d');
			const offscreen = document.createElement('canvas');
			function resizeCanvas(c) {
			  const r = window.devicePixelRatio || 1, w = Math.round(c.clientWidth * r), h = Math.round(c.clientHeight * r);
			  if (c.width !== w || c.height !== h) { c.width = w; c.height = h; }
			  return r;
			}
			function draw2d() {
			  const r = resizeCanvas(view), ctx = view.getContext('2d');
			  ctx.fillStyle = CONFIG.background; ctx.fillRect(0, 0, view.width, view.height);
			  const w = sim.width, h = sim.height;
			  if (!sim.cells || !(w > 0) || !(h > 0)) return;
			  if (offscreen.width !== w || offscreen.height !== h) { offscreen.width = w; offscreen.height = h; }
			  const octx = offscreen.getContext('2d'), image = octx.createImageData(w, h), d = image.data;
			  const pal = sim.palette ? sim.palette.map(hexColor) : null, span = sim.max - sim.min || 1;
			  for (let i = 0; i < w * h; i++) {
			    const v = sim.cells[i];
			    let c;
			    if (pal) { c = pal[Math.max(0, Math.min(pal.length - 1, v | 0))]; } else { c = rampColor((v - sim.min) / span); }
			    d[i * 4] = c[0]; d[i * 4 + 1] = c[1]; d[i * 4 + 2] = c[2]; d[i * 4 + 3] = 255;
			  }
			  octx.putImageData(image, 0, 0);
			  const scale = Math.min(view.width / w, view.height / h), ox = (view.width - w * scale) / 2, oy = (view.height - h * scale) / 2;
			  ctx.imageSmoothingEnabled = false; ctx.drawImage(offscreen, ox, oy, w * scale, h * scale);
			  ctx.strokeStyle = '#888'; ctx.lineWidth = r; ctx.strokeRect(ox, oy, w * scale, h * scale);
			  const a = sim.agents;
			  if (a && a.x) {
			    ctx.fillStyle = a.color || '#d62728';
			    const radius = Math.max(1, (a.radius || 0.5) * scale);
			    for (let i = 0; i < a.x.length; i++) {
			      if (a.colors) ctx.fillStyle = a.colors[i];
			      ctx.beginPath(); ctx.arc(ox + a.x[i] * scale, oy + a.y[i] * scale, radius, 0, 2 * Math.PI); ctx.fill();
			    }
			  }
			}

			/* ----- The 3D view (WebGL) ----- */
			let gl = null, triProgram, lineProgram, pointProgram, buffers = {};
			let theta = -0.95, phi = 0.45, dist = 0;
			function init3d() {
			  gl = view3d.getContext('webgl', { antialias: true });
			  if (!gl) { report('WebGL is not available in this browser'); return; }
			  function shader(type, src) { const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s); if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw gl.getShaderInfoLog(s); return s; }
			  function program(vs, fs) { const p = gl.createProgram(); gl.attachShader(p, shader(gl.VERTEX_SHADER, vs)); gl.attachShader(p, shader(gl.FRAGMENT_SHADER, fs)); gl.linkProgram(p); return p; }
			  triProgram = program(
			    'attribute vec3 p; attribute vec3 n; attribute vec3 c; uniform mat4 mvp; uniform mat3 nm; varying vec3 vc; varying vec3 vn;' +
			    'void main() { gl_Position = mvp * vec4(p, 1.0); vn = nm * n; vc = c; }',
			    'precision mediump float; varying vec3 vc; varying vec3 vn;' +
			    'void main() { vec3 n = normalize(vn); float l = 0.32 + 0.58 * abs(n.z) + 0.18 * abs(n.y); gl_FragColor = vec4(min(vc * l, 1.0), 1.0); }');
			  lineProgram = program(
			    'attribute vec3 p; attribute vec3 c; uniform mat4 mvp; varying vec3 vc; void main() { gl_Position = mvp * vec4(p, 1.0); vc = c; }',
			    'precision mediump float; varying vec3 vc; void main() { gl_FragColor = vec4(vc, 1.0); }');
			  pointProgram = program(
			    'attribute vec3 p; attribute vec3 c; uniform mat4 mvp; uniform float ps; uniform float radius; varying vec3 vc;' +
			    'void main() { gl_Position = mvp * vec4(p, 1.0); gl_PointSize = max(2.0, 2.0 * radius * ps / gl_Position.w); vc = c; }',
			    'precision mediump float; varying vec3 vc;' +
			    'void main() { vec2 q = gl_PointCoord * 2.0 - 1.0; float d = dot(q, q); if (d > 1.0) discard; float z = sqrt(1.0 - d);' +
			    ' gl_FragColor = vec4(min(vc * (0.3 + 0.6 * z) + 0.25 * pow(z, 12.0), 1.0), 1.0); }');
			  ['tri', 'line', 'point', 'frame'].forEach(function (k) { buffers[k] = gl.createBuffer(); });
			  let drag = null;
			  view3d.addEventListener('mousedown', function (ev) { drag = [ev.clientX, ev.clientY]; });
			  window.addEventListener('mouseup', function () { drag = null; });
			  window.addEventListener('mousemove', function (ev) {
			    if (!drag) return; theta -= (ev.clientX - drag[0]) * 0.01; phi = Math.max(-1.55, Math.min(1.55, phi + (ev.clientY - drag[1]) * 0.01));
			    drag = [ev.clientX, ev.clientY]; render();
			  });
			  view3d.addEventListener('wheel', function (ev) { ev.preventDefault(); dist *= ev.deltaY > 0 ? 1.1 : 1 / 1.1; render(); }, { passive: false });
			  view3d.addEventListener('dblclick', function () { theta = -0.95; phi = 0.45; dist = 0; render(); });
			  document.getElementById('hint').textContent = 'Drag: rotate \\u00B7 Wheel: zoom \\u00B7 Double click: reset';
			}
			function perspective(fovy, aspect, near, far) { const f = 1 / Math.tan(fovy / 2), nf = 1 / (near - far); return [f / aspect, 0, 0, 0, 0, f, 0, 0, 0, 0, (far + near) * nf, -1, 0, 0, 2 * far * near * nf, 0]; }
			function lookAt(e, c, up) {
			  let z = [e[0] - c[0], e[1] - c[1], e[2] - c[2]]; let l = Math.hypot(z[0], z[1], z[2]); z = z.map(function (a) { return a / l; });
			  let x = [up[1] * z[2] - up[2] * z[1], up[2] * z[0] - up[0] * z[2], up[0] * z[1] - up[1] * z[0]]; l = Math.hypot(x[0], x[1], x[2]); x = x.map(function (a) { return a / l; });
			  const y = [z[1] * x[2] - z[2] * x[1], z[2] * x[0] - z[0] * x[2], z[0] * x[1] - z[1] * x[0]];
			  return [x[0], y[0], z[0], 0, x[1], y[1], z[1], 0, x[2], y[2], z[2], 0, -(x[0] * e[0] + x[1] * e[1] + x[2] * e[2]), -(y[0] * e[0] + y[1] * e[1] + y[2] * e[2]), -(z[0] * e[0] + z[1] * e[1] + z[2] * e[2]), 1];
			}
			function multiply(a, b) { const r = new Array(16); for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) { let s = 0; for (let k = 0; k < 4; k++) s += a[k * 4 + j] * b[i * 4 + k]; r[i * 4 + j] = s; } return r; }
			function attribute(prog, name, size, stride, offset) { const loc = gl.getAttribLocation(prog, name); if (loc < 0) return; gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, size, gl.FLOAT, false, stride * 4, offset * 4); }
			function color3(c, fallback) { const h = hexColor(c || fallback); return [h[0] / 255, h[1] / 255, h[2] / 255]; }
			function draw3d() {
			  if (!gl) init3d(); if (!gl) return;
			  const ratio = resizeCanvas(view3d);
			  const box = sim.box || [1, 1, 1], center = [box[0] / 2, box[1] / 2, box[2] / 2], size = Math.max(box[0], box[1], box[2]);
			  if (!(dist > 0)) dist = size * 3.2;
			  const bg = hexColor(CONFIG.background);
			  gl.viewport(0, 0, view3d.width, view3d.height); gl.clearColor(bg[0] / 255, bg[1] / 255, bg[2] / 255, 1); gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
			  gl.enable(gl.DEPTH_TEST);
			  const e = [center[0] + dist * Math.cos(phi) * Math.cos(theta), center[1] + dist * Math.cos(phi) * Math.sin(theta), center[2] + dist * Math.sin(phi)];
			  const fov = 30 * Math.PI / 180, viewM = lookAt(e, center, [0, 0, 1]);
			  const mvp = multiply(perspective(fov, view3d.width / view3d.height, dist * 0.02, dist * 20), viewM);
			  function lines(data, buffer) {
			    if (data.length === 0) return;
			    gl.useProgram(lineProgram); gl.bindBuffer(gl.ARRAY_BUFFER, buffer); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.DYNAMIC_DRAW);
			    attribute(lineProgram, 'p', 3, 6, 0); attribute(lineProgram, 'c', 3, 6, 3);
			    gl.uniformMatrix4fv(gl.getUniformLocation(lineProgram, 'mvp'), false, new Float32Array(mvp)); gl.drawArrays(gl.LINES, 0, data.length / 6);
			  }
			  /* The box and the grid of the floor */
			  const f = [], g = [0.55, 0.55, 0.55], gl2 = [0.82, 0.82, 0.82];
			  function seg(a, b, c) { f.push(a[0], a[1], a[2], c[0], c[1], c[2], b[0], b[1], b[2], c[0], c[1], c[2]); }
			  for (let k = 1; k < 10; k++) { const x = box[0] * k / 10, y = box[1] * k / 10; seg([x, 0, 0], [x, box[1], 0], gl2); seg([0, y, 0], [box[0], y, 0], gl2); }
			  const corners = [[0, 0, 0], [box[0], 0, 0], [box[0], box[1], 0], [0, box[1], 0]];
			  for (let k = 0; k < 4; k++) {
			    const a = corners[k], b = corners[(k + 1) % 4];
			    seg(a, b, g); seg([a[0], a[1], box[2]], [b[0], b[1], box[2]], g); seg(a, [a[0], a[1], box[2]], g);
			  }
			  lines(f, buffers.frame);
			  /* The surface */
			  const s = sim.surface;
			  if (s && s.z && s.nx > 1 && s.ny > 1) {
			    const span = (s.max - s.min) || 1, t = [], P = function (i, j) { return [box[0] * i / (s.nx - 1), box[1] * j / (s.ny - 1), box[2] * Math.max(0, Math.min(1, (s.z[i * s.ny + j] - s.min) / span))]; };
			    const pos = [], col = [], nor = [];
			    for (let i = 0; i < s.nx; i++) for (let j = 0; j < s.ny; j++) {
			      pos.push(P(i, j)); const c = rampColor((s.z[i * s.ny + j] - s.min) / span); col.push([c[0] / 255, c[1] / 255, c[2] / 255]);
			    }
			    for (let i = 0; i < s.nx; i++) for (let j = 0; j < s.ny; j++) {
			      const a = pos[Math.min(s.nx - 1, i + 1) * s.ny + j], b = pos[Math.max(0, i - 1) * s.ny + j], c = pos[i * s.ny + Math.min(s.ny - 1, j + 1)], d = pos[i * s.ny + Math.max(0, j - 1)];
			      const u = [a[0] - b[0], a[1] - b[1], a[2] - b[2]], v = [c[0] - d[0], c[1] - d[1], c[2] - d[2]];
			      nor.push([u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]]);
			    }
			    function V(k) { const p = pos[k], n = nor[k], c = col[k]; t.push(p[0], p[1], p[2], n[0], n[1], n[2], c[0], c[1], c[2]); }
			    for (let i = 0; i < s.nx - 1; i++) for (let j = 0; j < s.ny - 1; j++) {
			      const k = i * s.ny + j; V(k); V(k + s.ny); V(k + s.ny + 1); V(k); V(k + s.ny + 1); V(k + 1);
			    }
			    gl.useProgram(triProgram); gl.bindBuffer(gl.ARRAY_BUFFER, buffers.tri); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(t), gl.DYNAMIC_DRAW);
			    attribute(triProgram, 'p', 3, 9, 0); attribute(triProgram, 'n', 3, 9, 3); attribute(triProgram, 'c', 3, 9, 6);
			    gl.uniformMatrix4fv(gl.getUniformLocation(triProgram, 'mvp'), false, new Float32Array(mvp));
			    gl.uniformMatrix3fv(gl.getUniformLocation(triProgram, 'nm'), false, new Float32Array([viewM[0], viewM[1], viewM[2], viewM[4], viewM[5], viewM[6], viewM[8], viewM[9], viewM[10]]));
			    gl.enable(gl.POLYGON_OFFSET_FILL); gl.polygonOffset(1, 1); gl.drawArrays(gl.TRIANGLES, 0, t.length / 9); gl.disable(gl.POLYGON_OFFSET_FILL);
			  }
			  /* The lines */
			  const l = sim.lines;
			  if (l && l.xyz && l.xyz.length >= 6) {
			    const data = [], c = color3(l.color, '#555555');
			    for (let i = 0; i + 2 < l.xyz.length; i += 3) {
			      const cc = l.colors ? [l.colors[i], l.colors[i + 1], l.colors[i + 2]] : c;
			      data.push(l.xyz[i], l.xyz[i + 1], l.xyz[i + 2], cc[0], cc[1], cc[2]);
			    }
			    lines(data, buffers.line);
			  }
			  /* The points (spheres) */
			  const p = sim.points;
			  if (p && p.xyz && p.xyz.length >= 3) {
			    const data = [], c = color3(p.color, '#1f77b4');
			    for (let i = 0; i + 2 < p.xyz.length; i += 3) {
			      const cc = p.colors ? [p.colors[i], p.colors[i + 1], p.colors[i + 2]] : c;
			      data.push(p.xyz[i], p.xyz[i + 1], p.xyz[i + 2], cc[0], cc[1], cc[2]);
			    }
			    gl.useProgram(pointProgram); gl.bindBuffer(gl.ARRAY_BUFFER, buffers.point); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.DYNAMIC_DRAW);
			    attribute(pointProgram, 'p', 3, 6, 0); attribute(pointProgram, 'c', 3, 6, 3);
			    gl.uniformMatrix4fv(gl.getUniformLocation(pointProgram, 'mvp'), false, new Float32Array(mvp));
			    gl.uniform1f(gl.getUniformLocation(pointProgram, 'ps'), view3d.height / (2 * Math.tan(fov / 2)));
			    gl.uniform1f(gl.getUniformLocation(pointProgram, 'radius'), p.radius || size * 0.01);
			    gl.drawArrays(gl.POINTS, 0, data.length / 6);
			  }
			}

			/* ----- The chart ----- */
			const chartBox = document.getElementById('chartbox'), chartCanvas = document.getElementById('chart');
			if (CONFIG.chart !== null) chartBox.style.display = '';
			function drawChart() {
			  if (CONFIG.chart === null) return;
			  const r = resizeCanvas(chartCanvas), ctx = chartCanvas.getContext('2d'), W = chartCanvas.width, H = chartCanvas.height;
			  ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, W, H);
			  const left = 60 * r, right = 150 * r, top = 26 * r, bottom = 26 * r, pw = W - left - right, ph = H - top - bottom;
			  ctx.font = 'bold ' + (12 * r) + 'px Verdana'; ctx.fillStyle = '#222'; ctx.textAlign = 'left';
			  ctx.fillText(CONFIG.chart.replace(/<[^>]*>/g, ''), left, 17 * r);
			  let xmin = Infinity, xmax = -Infinity, ymin = Infinity, ymax = -Infinity;
			  series.forEach(function (s) { s.forEach(function (q) { xmin = Math.min(xmin, q[0]); xmax = Math.max(xmax, q[0]); ymin = Math.min(ymin, q[1]); ymax = Math.max(ymax, q[1]); }); });
			  ctx.strokeStyle = '#999'; ctx.lineWidth = r; ctx.strokeRect(left, top, pw, ph);
			  if (!isFinite(xmin)) return;
			  if (xmax === xmin) xmax = xmin + 1; if (ymax === ymin) { ymax += 1; ymin -= 1; }
			  const pad = (ymax - ymin) * 0.05; ymin -= pad; ymax += pad;
			  function X(v) { return left + (v - xmin) / (xmax - xmin) * pw; } function Y(v) { return top + ph - (v - ymin) / (ymax - ymin) * ph; }
			  ctx.font = (10 * r) + 'px Verdana'; ctx.fillStyle = '#444';
			  function nice(span) { const p = Math.pow(10, Math.floor(Math.log10(span / 5))), f = span / 5 / p; return (f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10) * p; }
			  const ys = nice(ymax - ymin); ctx.textAlign = 'right';
			  for (let v = Math.ceil(ymin / ys) * ys; v <= ymax; v += ys) {
			    ctx.strokeStyle = '#e4e4e4'; ctx.beginPath(); ctx.moveTo(left, Y(v)); ctx.lineTo(left + pw, Y(v)); ctx.stroke();
			    ctx.fillText(Math.abs(v) >= 1e5 || (Math.abs(v) < 1e-3 && v !== 0) ? v.toExponential(1) : +v.toFixed(6), left - 5 * r, Y(v) + 3 * r);
			  }
			  const xs = nice(xmax - xmin); ctx.textAlign = 'center';
			  for (let v = Math.ceil(xmin / xs) * xs; v <= xmax; v += xs) ctx.fillText(+v.toFixed(6), X(v), top + ph + 15 * r);
			  let k = 0;
			  series.forEach(function (s, name) {
			    const c = CATEGORY[k % CATEGORY.length]; ctx.strokeStyle = c; ctx.lineWidth = 1.8 * r; ctx.beginPath();
			    /* At most about 2000 points (every n-th value) */
			    const n = Math.max(1, Math.floor(s.length / 2000));
			    for (let i = 0; i < s.length; i += n) { const x = X(s[i][0]), y = Y(s[i][1]); if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y); }
			    ctx.stroke();
			    ctx.fillStyle = c; ctx.fillRect(left + pw + 12 * r, top + k * 18 * r, 12 * r, 10 * r);
			    ctx.fillStyle = '#222'; ctx.textAlign = 'left'; const last = s[s.length - 1][1];
			    ctx.fillText(name + ': ' + (Math.abs(last) >= 1e5 || (Math.abs(last) < 1e-3 && last !== 0) ? last.toExponential(2) : +last.toFixed(3)), left + pw + 28 * r, top + k * 18 * r + 9 * r);
			    k++;
			  });
			}

			/* The timeline (e.g., the frames of a recording) */
			const timelineInput = document.getElementById('timeline');
			if (CONFIG.timeline > 0) {
			  document.getElementById('timelineBox').style.display = '';
			  timelineInput.max = CONFIG.timeline - 1;
			  timelineInput.addEventListener('input', function () {
			    const k = parseInt(timelineInput.value, 10), fn = userFunction('seek');
			    try { if (fn) fn(sim, k); sim.t = k; } catch (e) { report(e); }
			    render();
			  });
			}
			function render() {
			  statusElement.textContent = CONFIG.timeline > 0 ? 'Step ' + sim.t + ' of ' + (CONFIG.timeline - 1) : 'Step ' + sim.t;
			  if (CONFIG.timeline > 0) timelineInput.value = sim.t;
			  try {
			    if (CONFIG.view === 'GRID_2D') { view.style.display = ''; view3d.style.display = 'none'; draw2d(); }
			    else { view.style.display = 'none'; view3d.style.display = ''; draw3d(); }
			    if (typeof draw === 'function') { draw(sim); }
			    drawChart();
			  } catch (e) { report(e); }
			}
			window.addEventListener('resize', render);
			/* An optional (asynchronous) function prepare(sim) is called before the start, e.g., to load data */
			const prepareFunction = userFunction('prepare');
			statusElement.textContent = prepareFunction ? 'Loading ...' : statusElement.textContent;
			Promise.resolve(prepareFunction ? prepareFunction(sim) : null).then(function () {
			  reset();
			  if (CONFIG.autoplay) { playing = true; updatePlay(); lastTime = performance.now(); requestAnimationFrame(loop); }
			}).catch(report);
			})();
			</script>
			</body>
			</html>
			""";
}
