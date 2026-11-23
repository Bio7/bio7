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
 * '#000000']) or the color map (or the colors of sim.ramp) with sim.min and sim.max and optional agents
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
	private boolean asynchronous = false;
	private String background = null;
	private String theme = "auto";
	private String layout = "classic", accent = "blue";
	private String sceneBackground = "auto", sceneLighting = "studio", sceneShape = "sphere", sceneFloor = "grid";
	private boolean sceneShadows = true, sceneRotate = false, sceneControls = true;
	private double[] mapBounds;
	private String mapBasemap = "topo", mapAttribution = "";
	private double mapOpacity = 0.75;
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

	/**
	 * The functions setup(sim), step(sim) and the functions of the buttons can
	 * return a Promise (e.g., a model of R in webR): the calls run one after the
	 * other (default false: synchronous functions).
	 */
	public HtmlSimulation asynchronous(boolean asynchronous) {
		this.asynchronous = asynchronous;
		return this;
	}

	/** Starts the simulation when the page is opened (default false). */
	public HtmlSimulation autoplay(boolean autoplay) {
		this.autoplay = autoplay;
		return this;
	}

	/**
	 * The layout of the page: "classic" (a sidebar with the controls, the default),
	 * "dashboard" (a top bar with Play/Pause, the view, the settings on the right),
	 * "focus" (the view fills the window, floating panels which can be hidden) or
	 * "story" (one column: the title, the view, the controls, the settings in a
	 * grid, the chart). All layouts adapt to narrow windows (phones).
	 */
	public HtmlSimulation layout(String layout) {
		this.layout = layout == null ? "classic" : layout;
		return this;
	}

	/** The accent color of the page: "blue" (default), "teal", "violet", "orange" or "graphite". */
	public HtmlSimulation accent(String accent) {
		this.accent = accent == null ? "blue" : accent;
		return this;
	}

	/**
	 * The theme of the page: "auto" (the light or dark setting of the system, the
	 * default), "light" or "dark". The page has a button to change it.
	 */
	public HtmlSimulation theme(String theme) {
		this.theme = "light".equals(theme) || "dark".equals(theme) ? theme : "auto";
		return this;
	}

	/**
	 * The look of the 3D views (SCENE_3D, SURFACE_3D). The page has a "View" panel
	 * to change it (see sceneControls).
	 *
	 * @param background "auto" (studio or night as the theme), "studio", "night",
	 *                   "sky", "sunset", "ocean", "paper" or "plain"
	 * @param lighting   "studio", "soft", "dramatic" or "flat"
	 * @param shape      the shape of the points: "sphere", "cube", "cone", "glider"
	 *                   (cones and gliders point in the direction of the movement)
	 *                   or "point" (fast point sprites)
	 * @param floor      "grid", "plane" or "none"
	 * @param shadows    soft shadows of the points on the floor
	 */
	public HtmlSimulation scene(String background, String lighting, String shape, String floor, boolean shadows) {
		this.sceneBackground = background == null ? "auto" : background;
		this.sceneLighting = lighting == null ? "studio" : lighting;
		this.sceneShape = shape == null ? "sphere" : shape;
		this.sceneFloor = floor == null ? "grid" : floor;
		this.sceneShadows = shadows;
		return this;
	}

	/**
	 * Shows the 2D grid on a map (online base maps with Leaflet): the grid covers
	 * the geographic bounds (WGS84 degrees; the grid is drawn in Web Mercator, exact
	 * for a grid in EPSG:3857). NaN bounds: the map shows sim.mapBounds of the
	 * model (e.g., agents with sim.geoAgents = {lon, lat, colors, sizes, labels}). The page has a "Map" panel (the base map, the
	 * opacity, the map on or off). Colors with an alpha (#rrggbbaa) show the map.
	 *
	 * @param basemap     "topo" (OpenTopoMap, CC-BY-SA) or "osm" (OpenStreetMap: only
	 *                    for a page on a web server, its tile servers block a local
	 *                    file); both need no key and show their attribution
	 * @param opacity     the opacity of the grid (0 - 1)
	 * @param attribution the source of the data (shown on the map), e.g., "Terrain Tiles"
	 */
	public HtmlSimulation map(double south, double west, double north, double east, String basemap, double opacity, String attribution) {
		this.mapBounds = new double[] { south, west, north, east };
		this.mapBasemap = basemap == null ? "topo" : basemap;
		this.mapOpacity = opacity;
		this.mapAttribution = attribution == null ? "" : attribution;
		return this;
	}

	/** Rotates the camera of the 3D view slowly (also a switch of the page). */
	public HtmlSimulation autoRotate(boolean rotate) {
		this.sceneRotate = rotate;
		return this;
	}

	/** Shows the "View" panel of the 3D views in the page (default: true). */
	public HtmlSimulation sceneControls(boolean show) {
		this.sceneControls = show;
		return this;
	}

	/** The background color of the view (e.g., "#ffffff", null: the color of the theme). */
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
		config.append("\"async\":").append(asynchronous).append(',');
		config.append("\"timeline\":").append(timeline).append(',');
		config.append("\"theme\":").append(PlotHtml.string(theme)).append(',');
		config.append("\"layout\":").append(PlotHtml.string(layout)).append(',');
		config.append("\"accent\":").append(PlotHtml.string(accent)).append(',');
		config.append("\"scene\":{\"background\":").append(PlotHtml.string(sceneBackground)).append(",\"lighting\":").append(PlotHtml.string(sceneLighting)).append(",\"shape\":")
				.append(PlotHtml.string(sceneShape)).append(",\"floor\":").append(PlotHtml.string(sceneFloor)).append(",\"shadows\":").append(sceneShadows).append(",\"rotate\":").append(sceneRotate).append("},");
		config.append("\"sceneControls\":").append(sceneControls).append(',');
		if (mapBounds != null) {
			String bounds = Double.isNaN(mapBounds[0]) ? "null"
					: "[[" + PlotHtml.num(mapBounds[0]) + "," + PlotHtml.num(mapBounds[1]) + "],[" + PlotHtml.num(mapBounds[2]) + "," + PlotHtml.num(mapBounds[3]) + "]]";
			config.append("\"map\":{\"bounds\":").append(bounds).append(",\"basemap\":").append(PlotHtml.string(mapBasemap)).append(",\"opacity\":").append(PlotHtml.num(mapOpacity)).append(",\"attribution\":")
					.append(PlotHtml.string(PlotHtml.escape(mapAttribution))).append("},");
		} else {
			config.append("\"map\":null,");
		}
		config.append("\"background\":").append(background == null ? "null" : PlotHtml.string(background));
		config.append('}');
		String page = TEMPLATE.replace("/*GLTF*/\n", GLTF_JS).replace("/*DRAW3D*/\n", DRAW3D_JS).replace("/*CANVAS*/\n", CANVAS_JS).replace("/*CHARTS*/\n", CHARTS_JS).replace("/*MODELS3D*/\n", MODELS3D_JS).replace("<title></title>", "<title>" + PlotHtml.escape(title) + "</title>")
				.replace("/*CONFIG*/", config.toString());
		if (mapBounds != null) {
			/* Leaflet for the map (online, a fixed version with the hashes of the files) */
			page = page.replace("<!--MAP-->", """
					<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="">
					<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>""");
		}
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
	/* The JavaScript of the shapes and images on a canvas */
	private static final String CANVAS_JS = """
			/* The images of the HTML page (embedded by the export: ASSETS.images, data URIs) */
			const imageCache = {};
			function assetImage(name) {
			  if (typeof ASSETS === 'undefined' || !ASSETS.images || !ASSETS.images[name]) return null;
			  let im = imageCache[name];
			  if (!im) { im = new Image(); im.onload = function () { render(); }; im.src = ASSETS.images[name]; imageCache[name] = im; }
			  return im.complete && im.naturalWidth ? im : null;
			}
			/* Draws the shapes of a model on a canvas (m: the conversion of the coordinates and sizes to pixels) */
			function drawShapes(ctx, shapes, m) {
			  ctx.lineCap = 'round'; ctx.lineJoin = 'round';
			  for (const s of shapes) {
			    const p = s.p, stroke = s.s ? cssHex(s.s) : null, fill = s.f ? cssHex(s.f) : null;
			    ctx.lineWidth = Math.max(0.5, s.w || 1) * m.px;
			    if (s.k === 'line' || s.k === 'line3') {
			      const k = s.k === 'line3' ? 3 : 2; if (!stroke) continue;
			      ctx.strokeStyle = stroke; ctx.beginPath(); ctx.moveTo(m.x(p[0]), m.y(p[1])); ctx.lineTo(m.x(p[k]), m.y(p[k + 1])); ctx.stroke();
			    } else if (s.k === 'path' || s.k === 'polygon' || s.k === 'rect') {
			      const q = s.k === 'rect' ? [p[0], p[1], p[0] + p[2], p[1], p[0] + p[2], p[1] + p[3], p[0], p[1] + p[3]] : p;
			      if (q.length < 4) continue;
			      ctx.beginPath(); ctx.moveTo(m.x(q[0]), m.y(q[1])); for (let i = 2; i + 1 < q.length; i += 2) ctx.lineTo(m.x(q[i]), m.y(q[i + 1]));
			      if (s.k !== 'path') { ctx.closePath(); if (fill) { ctx.fillStyle = fill; ctx.fill(); } }
			      if (stroke) { ctx.strokeStyle = stroke; ctx.stroke(); }
			    } else if (s.k === 'circle') {
			      ctx.beginPath(); ctx.arc(m.x(p[0]), m.y(p[1]), Math.max(0.5, m.radius(p[2], p[1])), 0, 2 * Math.PI);
			      if (fill) { ctx.fillStyle = fill; ctx.fill(); } if (stroke) { ctx.strokeStyle = stroke; ctx.stroke(); }
			    } else if (s.k === 'text') {
			      ctx.fillStyle = stroke || cssColor('--text'); ctx.textAlign = 'left'; ctx.font = Math.max(6, m.size(s.w || 1)) + 'px ' + cssColor('--font');
			      ctx.fillText(s.t, m.x(p[0]), m.y(p[1]));
			    } else if (s.k === 'image') {
			      const im = assetImage(s.t); if (!im) continue;
			      const size = m.size(s.w || 1), k = size / Math.max(im.naturalWidth, im.naturalHeight);
			      ctx.save(); ctx.translate(m.x(p[0]), m.y(p[1])); ctx.rotate((s.a || 0) * Math.PI / 180);
			      ctx.drawImage(im, -im.naturalWidth * k / 2, -im.naturalHeight * k / 2, im.naturalWidth * k, im.naturalHeight * k); ctx.restore();
			    }
			  }
			}
			/* A color #rrggbb or #rrggbbaa as a CSS color */
			function cssHex(c) { if (c.length === 9) { const a = parseInt(c.substr(7, 2), 16) / 255; return 'rgba(' + parseInt(c.substr(1, 2), 16) + ',' + parseInt(c.substr(3, 2), 16) + ',' + parseInt(c.substr(5, 2), 16) + ',' + a + ')'; } return c; }
""";

	/* The JavaScript of the charts */
	private static final String CHARTS_JS = """
			function niceStep(span) { const p = Math.pow(10, Math.floor(Math.log10(span / 5))), f = span / 5 / p; return (f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10) * p; }
			function chartNumber(v) { return Math.abs(v) >= 1e5 || (Math.abs(v) < 1e-3 && v !== 0) ? v.toExponential(1) : +v.toFixed(6); }
			function drawOneChart(canvas, title, kind, data, bars) {
			  const r = resizeCanvas(canvas), ctx = canvas.getContext('2d'), W = canvas.width, H = canvas.height;
			  ctx.fillStyle = cssColor('--card'); ctx.fillRect(0, 0, W, H);
			  if (!(W > 0) || !(H > 0)) return;
			  const left = 60 * r, right = 150 * r, top = 26 * r, bottom = 26 * r, pw = W - left - right, ph = H - top - bottom;
			  ctx.font = '600 ' + (12 * r) + 'px ' + cssColor('--font'); ctx.fillStyle = cssColor('--text'); ctx.textAlign = 'left';
			  ctx.fillText(title, left, 17 * r);
			  ctx.strokeStyle = cssColor('--border'); ctx.lineWidth = r; ctx.strokeRect(left, top, pw, ph);
			  ctx.font = (10 * r) + 'px ' + cssColor('--font');
			  if (kind === 'bars') {
			    const n = Math.min(bars.labels.length, bars.values.length); if (n === 0) return;
			    let ymin = 0, ymax = 0; for (let i = 0; i < n; i++) { ymin = Math.min(ymin, bars.values[i]); ymax = Math.max(ymax, bars.values[i]); }
			    if (ymax === ymin) ymax = ymin + 1; ymax *= 1.05;
			    const Y = function (v) { return top + ph - (v - ymin) / (ymax - ymin) * ph; }, ys = niceStep(ymax - ymin);
			    ctx.textAlign = 'right';
			    for (let v = Math.ceil(ymin / ys) * ys; v <= ymax; v += ys) { ctx.strokeStyle = cssColor('--grid'); ctx.beginPath(); ctx.moveTo(left, Y(v)); ctx.lineTo(left + pw, Y(v)); ctx.stroke(); ctx.fillStyle = cssColor('--muted'); ctx.fillText(chartNumber(v), left - 5 * r, Y(v) + 3 * r); }
			    const bw = pw / n; ctx.textAlign = 'center';
			    /* Every k-th label if they do not fit */
			    let widest = 0; for (let i = 0; i < n; i++) widest = Math.max(widest, ctx.measureText(String(bars.labels[i])).width);
			    const every = Math.max(1, Math.ceil(widest * 1.15 / bw));
			    for (let i = 0; i < n; i++) {
			      ctx.fillStyle = CATEGORY[0]; const y = Y(bars.values[i]), z = Y(0); ctx.fillRect(left + i * bw + bw * 0.15, Math.min(y, z), Math.max(1, bw * 0.7), Math.abs(z - y));
			      const label = String(bars.labels[i]); ctx.fillStyle = cssColor('--muted');
			      if (i % every === 0) ctx.fillText(label, left + i * bw + bw / 2, top + ph + 15 * r);
			    }
			    return;
			  }
			  let xmin = Infinity, xmax = -Infinity, ymin = Infinity, ymax = -Infinity;
			  data.forEach(function (s) { s.forEach(function (q) { xmin = Math.min(xmin, q[0]); xmax = Math.max(xmax, q[0]); ymin = Math.min(ymin, q[1]); ymax = Math.max(ymax, q[1]); }); });
			  if (!isFinite(xmin)) return;
			  if (xmax === xmin) xmax = xmin + 1; if (ymax === ymin) { ymax += 1; ymin -= 1; }
			  const pad = (ymax - ymin) * 0.05; ymin -= pad; ymax += pad;
			  function X(v) { return left + (v - xmin) / (xmax - xmin) * pw; } function Y(v) { return top + ph - (v - ymin) / (ymax - ymin) * ph; }
			  ctx.fillStyle = cssColor('--muted');
			  const ys = niceStep(ymax - ymin); ctx.textAlign = 'right';
			  for (let v = Math.ceil(ymin / ys) * ys; v <= ymax; v += ys) {
			    ctx.strokeStyle = cssColor('--grid'); ctx.beginPath(); ctx.moveTo(left, Y(v)); ctx.lineTo(left + pw, Y(v)); ctx.stroke();
			    ctx.fillText(chartNumber(v), left - 5 * r, Y(v) + 3 * r);
			  }
			  const xs = niceStep(xmax - xmin); ctx.textAlign = 'center';
			  for (let v = Math.ceil(xmin / xs) * xs; v <= xmax; v += xs) ctx.fillText(+v.toFixed(6), X(v), top + ph + 15 * r);
			  let k = 0;
			  data.forEach(function (s, name) {
			    const c = CATEGORY[k % CATEGORY.length]; ctx.strokeStyle = c; ctx.lineWidth = 1.8 * r; ctx.beginPath();
			    /* At most about 2000 points (every n-th value) */
			    const n = Math.max(1, Math.floor(s.length / 2000));
			    for (let i = 0; i < s.length; i += n) { const x = X(s[i][0]), y = Y(s[i][1]); if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y); }
			    ctx.stroke();
			    if (kind === 'xy' && s.length) { const q = s[s.length - 1]; ctx.fillStyle = c; ctx.beginPath(); ctx.arc(X(q[0]), Y(q[1]), 3.5 * r, 0, 2 * Math.PI); ctx.fill(); }
			    ctx.fillStyle = c; ctx.fillRect(left + pw + 12 * r, top + k * 18 * r, 12 * r, 10 * r);
			    ctx.fillStyle = cssColor('--text'); ctx.textAlign = 'left'; const last = s[s.length - 1][1];
			    ctx.fillText(name + ': ' + (Math.abs(last) >= 1e5 || (Math.abs(last) < 1e-3 && last !== 0) ? last.toExponential(2) : +last.toFixed(3)), left + pw + 28 * r, top + k * 18 * r + 9 * r);
			    k++;
			  });
			}

""";

	/* The JavaScript of the 3D view (draw3d) */
	private static final String DRAW3D_JS = """
			function draw3d() {
			  if (!gl) init3d(); if (!gl) return;
			  const ratio = resizeCanvas(view3d);
			  const bgName = sceneBackground(), bgDef = BACKGROUNDS[bgName], dark = sceneDark();
			  viewbox.style.background = bgDef.css || viewBackground();
			  const box = sim.box || [1, 1, 1], center = [box[0] / 2, box[1] / 2, box[2] / 2], size = Math.max(box[0], box[1], box[2]);
			  if (!(dist > 0)) dist = size * 3.2;
			  gl.viewport(0, 0, view3d.width, view3d.height); gl.clearColor(0, 0, 0, 0); gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
			  gl.enable(gl.DEPTH_TEST); gl.disable(gl.BLEND); gl.depthMask(true);
			  const e = [center[0] + dist * Math.cos(phi) * Math.cos(theta), center[1] + dist * Math.cos(phi) * Math.sin(theta), center[2] + dist * Math.sin(phi)];
			  const fov = 30 * Math.PI / 180, viewM = lookAt(e, center, [0, 0, 1]);
			  spriteView = viewM;
			  const mvp = new Float32Array(multiply(perspective(fov, view3d.width / view3d.height, dist * 0.02, dist * 20), viewM));
			  const light = LIGHTS[scene.lighting] || LIGHTS.studio;
			  function lit(data, buffer) {
			    if (data.length === 0) return;
			    useProgram(litProgram); gl.bindBuffer(gl.ARRAY_BUFFER, buffer); gl.bufferData(gl.ARRAY_BUFFER, data instanceof Float32Array ? data : new Float32Array(data), gl.DYNAMIC_DRAW);
			    attribute(litProgram, 'p', 3, 9, 0); attribute(litProgram, 'n', 3, 9, 3); attribute(litProgram, 'c', 3, 9, 6);
			    gl.uniformMatrix4fv(gl.getUniformLocation(litProgram, 'mvp'), false, mvp);
			    gl.uniform3fv(gl.getUniformLocation(litProgram, 'eye'), e);
			    gl.uniform3fv(gl.getUniformLocation(litProgram, 'light'), [center[0] - e[0] * 0.15 - size * 0.4, center[1] - size * 0.6, size * 1.6]);
			    gl.uniform4fv(gl.getUniformLocation(litProgram, 'k'), light.k);
			    gl.uniform3fv(gl.getUniformLocation(litProgram, 'sky'), dark ? [0.72, 0.8, 1.0] : [1.0, 1.0, 1.0]);
			    gl.uniform3fv(gl.getUniformLocation(litProgram, 'ground'), dark ? [0.28, 0.25, 0.24] : [0.62, 0.58, 0.54]);
			    gl.drawArrays(gl.TRIANGLES, 0, data.length / 9);
			  }
			  function lines(data, buffer, alpha) {
			    if (data.length === 0) return;
			    useProgram(lineProgram); gl.bindBuffer(gl.ARRAY_BUFFER, buffer); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.DYNAMIC_DRAW);
			    attribute(lineProgram, 'p', 3, 6, 0); attribute(lineProgram, 'c', 3, 6, 3);
			    gl.uniformMatrix4fv(gl.getUniformLocation(lineProgram, 'mvp'), false, mvp);
			    gl.uniform1f(gl.getUniformLocation(lineProgram, 'alpha'), alpha === undefined ? 1 : alpha);
			    if (alpha !== undefined && alpha < 1) { gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA); }
			    gl.drawArrays(gl.LINES, 0, data.length / 6); gl.disable(gl.BLEND);
			  }
			  /* The floor (a plane, a grid or none) and the edges of the box */
			  const edge = dark ? [0.55, 0.62, 0.75] : [0.5, 0.55, 0.62], gridC = dark ? [0.42, 0.48, 0.6] : [0.62, 0.66, 0.72];
			  if (scene.floor === 'plane') {
			    const fc = dark ? [0.17, 0.2, 0.27] : [0.93, 0.94, 0.96], m = size * 0.04, z0 = -size * 0.002;
			    const q = [[-m, -m], [box[0] + m, -m], [box[0] + m, box[1] + m], [-m, box[1] + m]], fl = [];
			    [0, 1, 2, 0, 2, 3].forEach(function (i) { fl.push(q[i][0], q[i][1], z0, 0, 0, 1, fc[0], fc[1], fc[2]); });
			    gl.enable(gl.POLYGON_OFFSET_FILL); gl.polygonOffset(2, 2); lit(fl, buffers.floor); gl.disable(gl.POLYGON_OFFSET_FILL);
			  }
			  const f = [], g = [];
			  function seg(out, a, b, c) { out.push(a[0], a[1], a[2], c[0], c[1], c[2], b[0], b[1], b[2], c[0], c[1], c[2]); }
			  if (scene.floor !== 'none') {
			    for (let k = 1; k < 10; k++) { const x = box[0] * k / 10, y = box[1] * k / 10; seg(g, [x, 0, 0], [x, box[1], 0], gridC); seg(g, [0, y, 0], [box[0], y, 0], gridC); }
			  }
			  const corners = [[0, 0, 0], [box[0], 0, 0], [box[0], box[1], 0], [0, box[1], 0]];
			  for (let k = 0; k < 4; k++) {
			    const a = corners[k], b = corners[(k + 1) % 4];
			    seg(f, a, b, edge); seg(f, [a[0], a[1], box[2]], [b[0], b[1], box[2]], edge); seg(f, a, [a[0], a[1], box[2]], edge);
			  }
			  lines(g, buffers.line, scene.floor === 'plane' ? 0.45 : 0.7);
			  lines(f, buffers.frame, 0.85);
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
			    gl.enable(gl.POLYGON_OFFSET_FILL); gl.polygonOffset(1, 1); lit(t, buffers.tri); gl.disable(gl.POLYGON_OFFSET_FILL);
			  }
			  /* The lines (and the pillars, lit) */
			  drawShapes3d(mvp, lines, size, lit);
			  const l = sim.lines;
			  if (l && l.xyz && l.xyz.length >= 6) {
			    const data = [], c = color3(l.color, dark ? '#c8d0dc' : '#555555');
			    for (let i = 0; i + 2 < l.xyz.length; i += 3) {
			      const cc = l.colors ? [l.colors[i], l.colors[i + 1], l.colors[i + 2]] : c;
			      data.push(l.xyz[i], l.xyz[i + 1], l.xyz[i + 2], cc[0], cc[1], cc[2]);
			    }
			    lines(data, buffers.line);
			  }
			  /* The objects of the environment (glTF models of the page) */
			  const sc = sim.scenery;
			  if (sc && sc.models && sc.models.length) {
			    const idx = [];
			    for (let k = 0; k < sc.models.length; k++) if (assetModel(sc.models[k])) idx.push(k);
			    if (idx.length) drawAgentModels(sc, idx, null, mvp, e, center, size, light, dark);
			  }
			  /* The points: shapes (lit triangles) or point sprites, with soft shadows */
			  let p = sim.points;
			  if (p && p.xyz && (p.images || p.models)) {
			    /* The agents with a 3D model or an image: models and billboards, the others as points or shapes */
			    const keep = [], special = [], xyz = [], cols = p.colors ? [] : null, dirsAll = pointDirections(p);
			    for (let i = 0; i < p.xyz.length / 3; i++) {
			      if ((p.models && assetModel(p.models[i])) || (p.images && p.images[i])) special.push(i);
			      else { keep.push(i); xyz.push(p.xyz[i * 3], p.xyz[i * 3 + 1], p.xyz[i * 3 + 2]); if (cols) cols.push(p.colors[i * 3], p.colors[i * 3 + 1], p.colors[i * 3 + 2]); }
			    }
			    drawAgentModels(p, special, dirsAll, mvp, e, center, size, light, dark);
			    p = keep.length ? Object.assign({}, p, { xyz: xyz, colors: cols, dirs: dirsAll ? keep.reduce(function (a, i) { a.push(dirsAll[i * 3], dirsAll[i * 3 + 1], dirsAll[i * 3 + 2]); return a; }, []) : null, images: null, models: null }) : null;
			  }
			  if (p && p.xyz && p.xyz.length >= 3) {
			    const n = Math.floor(p.xyz.length / 3), radius = p.radius || size * 0.01, c = color3(p.color, '#1f77b4');
			    if (scene.shadows && scene.floor !== 'none') {
			      const sd = [];
			      for (let i = 0; i < n; i++) {
			        const x = p.xyz[i * 3], y = p.xyz[i * 3 + 1], z = p.xyz[i * 3 + 2], h = Math.max(0, z) / (box[2] || 1);
			        const r = radius * (1.3 + 1.2 * h), a = (dark ? 0.55 : 0.32) * (1 - 0.6 * Math.min(1, h)), zz = size * 0.001;
			        const qd = [[-1, -1], [1, -1], [1, 1], [-1, 1]];
			        [0, 1, 2, 0, 2, 3].forEach(function (k) { sd.push(x + qd[k][0] * r, y + qd[k][1] * r, zz, qd[k][0], qd[k][1], a); });
			      }
			      useProgram(shadowProgram); gl.bindBuffer(gl.ARRAY_BUFFER, buffers.shadow); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(sd), gl.DYNAMIC_DRAW);
			      attribute(shadowProgram, 'p', 3, 6, 0); attribute(shadowProgram, 'q', 3, 6, 3);
			      gl.uniformMatrix4fv(gl.getUniformLocation(shadowProgram, 'mvp'), false, mvp);
			      gl.uniform3fv(gl.getUniformLocation(shadowProgram, 'color'), dark ? [0, 0, 0] : [0.12, 0.14, 0.2]);
			      gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA); gl.depthMask(false);
			      gl.drawArrays(gl.TRIANGLES, 0, sd.length / 6);
			      gl.depthMask(true); gl.disable(gl.BLEND);
			    }
			    const geo = SHAPES[scene.shape];
			    if (geo) {
			      const dirs = pointDirections(p), per = geo.length / 6, out = new Float32Array(n * per * 9);
			      let o = 0;
			      for (let i = 0; i < n; i++) {
			        const px = p.xyz[i * 3], py = p.xyz[i * 3 + 1], pz = p.xyz[i * 3 + 2];
			        const cc = p.colors ? [p.colors[i * 3], p.colors[i * 3 + 1], p.colors[i * 3 + 2]] : c;
			        /* The frame of the shape: forward (the direction), side and up */
			        let fx = 1, fy = 0, fz = 0;
			        if (dirs) { const dx = dirs[i * 3], dy = dirs[i * 3 + 1], dz = dirs[i * 3 + 2], dl = Math.hypot(dx, dy, dz); if (dl > 1e-12) { fx = dx / dl; fy = dy / dl; fz = dz / dl; } }
			        let sx = -fy, sy = fx, sz = 0, sl = Math.hypot(sx, sy);
			        if (sl < 1e-6) { sx = 0; sy = 1; sz = 0; sl = 1; }
			        sx /= sl; sy /= sl;
			        const ux = fy * sz - fz * sy, uy = fz * sx - fx * sz, uz = fx * sy - fy * sx;
			        for (let k = 0; k < geo.length; k += 6) {
			          const a = geo[k], b = geo[k + 1], d = geo[k + 2], na = geo[k + 3], nb = geo[k + 4], nd = geo[k + 5];
			          out[o++] = px + radius * (a * fx + b * sx + d * ux); out[o++] = py + radius * (a * fy + b * sy + d * uy); out[o++] = pz + radius * (a * fz + b * sz + d * uz);
			          out[o++] = na * fx + nb * sx + nd * ux; out[o++] = na * fy + nb * sy + nd * uy; out[o++] = na * fz + nb * sz + nd * uz;
			          out[o++] = cc[0]; out[o++] = cc[1]; out[o++] = cc[2];
			        }
			      }
			      lit(out, buffers.shape);
			    } else {
			      const data = [];
			      for (let i = 0; i + 2 < p.xyz.length; i += 3) {
			        const cc = p.colors ? [p.colors[i], p.colors[i + 1], p.colors[i + 2]] : c;
			        data.push(p.xyz[i], p.xyz[i + 1], p.xyz[i + 2], cc[0], cc[1], cc[2]);
			      }
			      useProgram(pointProgram); gl.bindBuffer(gl.ARRAY_BUFFER, buffers.point); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.DYNAMIC_DRAW);
			      attribute(pointProgram, 'p', 3, 6, 0); attribute(pointProgram, 'c', 3, 6, 3);
			      gl.uniformMatrix4fv(gl.getUniformLocation(pointProgram, 'mvp'), false, mvp);
			      gl.uniform1f(gl.getUniformLocation(pointProgram, 'ps'), view3d.height / (2 * Math.tan(fov / 2)));
			      gl.uniform1f(gl.getUniformLocation(pointProgram, 'radius'), radius);
			      gl.drawArrays(gl.POINTS, 0, data.length / 6);
			    }
			  }
			}
""";

	/* The JavaScript of the glTF models (inserted into the template, which would be too long for a constant) */
	private static final String GLTF_JS = """
			/* ----- glTF models (embedded by the export: ASSETS.models, base64 of .glb) ----- */
			const Gltf3 = (function () {
			  function quatMat(t, q, s) {
			    const x = q[0], y = q[1], z = q[2], w = q[3];
			    return [(1 - 2 * (y * y + z * z)) * s[0], 2 * (x * y + z * w) * s[0], 2 * (x * z - y * w) * s[0], 0, 2 * (x * y - z * w) * s[1], (1 - 2 * (x * x + z * z)) * s[1], 2 * (y * z + x * w) * s[1], 0,
			      2 * (x * z + y * w) * s[2], 2 * (y * z - x * w) * s[2], (1 - 2 * (x * x + y * y)) * s[2], 0, t[0], t[1], t[2], 1];
			  }
			  function mul(a, b) { const r = new Array(16); for (let c = 0; c < 4; c++) for (let row = 0; row < 4; row++) { let s = 0; for (let k = 0; k < 4; k++) s += a[k * 4 + row] * b[c * 4 + k]; r[c * 4 + row] = s; } return r; }
			  const I = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1];
			  function parse(buf) {
			    const dv = new DataView(buf); let json, bin = null;
			    if (dv.getUint32(0, true) === 0x46546C67) {
			      const len = dv.getUint32(12, true); json = JSON.parse(new TextDecoder().decode(new Uint8Array(buf, 20, len)));
			      const o = 20 + len; if (o + 8 <= buf.byteLength) bin = new Uint8Array(buf, o + 8, dv.getUint32(o, true));
			    } else json = JSON.parse(new TextDecoder().decode(new Uint8Array(buf)));
			    const buffers = (json.buffers || []).map(function (b) { if (!b.uri) return bin; const t = atob(b.uri.substring(b.uri.indexOf(',') + 1)), u = new Uint8Array(t.length); for (let i = 0; i < t.length; i++) u[i] = t.charCodeAt(i); return u; });
			    function view(i) { const v = json.bufferViews[i], b = buffers[v.buffer]; return new Uint8Array(b.buffer, b.byteOffset + (v.byteOffset || 0), v.byteLength); }
			    function acc(i) {
			      const a = json.accessors[i], comps = { SCALAR: 1, VEC2: 2, VEC3: 3, VEC4: 4, MAT2: 4, MAT3: 9, MAT4: 16 }[a.type] || 1, out = new Float32Array(a.count * comps);
			      if (a.bufferView === undefined) return out;
			      const v = json.bufferViews[a.bufferView], b = buffers[v.buffer], d = new DataView(b.buffer, b.byteOffset);
			      const ct = a.componentType, size = ct === 5120 || ct === 5121 ? 1 : ct === 5122 || ct === 5123 ? 2 : 4, stride = v.byteStride || size * comps, base = (v.byteOffset || 0) + (a.byteOffset || 0), nz = !!a.normalized;
			      for (let i = 0; i < a.count; i++) for (let c = 0; c < comps; c++) {
			        const o = base + i * stride + c * size; let f;
			        if (ct === 5126) f = d.getFloat32(o, true); else if (ct === 5121) f = nz ? d.getUint8(o) / 255 : d.getUint8(o); else if (ct === 5123) f = nz ? d.getUint16(o, true) / 65535 : d.getUint16(o, true);
			        else if (ct === 5125) f = d.getUint32(o, true); else if (ct === 5120) f = nz ? Math.max(d.getInt8(o) / 127, -1) : d.getInt8(o); else f = nz ? Math.max(d.getInt16(o, true) / 32767, -1) : d.getInt16(o, true);
			        out[i * comps + c] = f;
			      }
			      return out;
			    }
			    const m = { nodes: [], meshes: [], skins: [], anims: [], textures: [], roots: [] };
			    (json.textures || []).forEach(function (t) { const img = json.images && json.images[t.source]; m.textures.push(img ? (img.bufferView !== undefined ? { bytes: view(img.bufferView), mime: img.mimeType || 'image/png' } : { uri: img.uri }) : null); });
			    (json.meshes || []).forEach(function (me) {
			      const prims = [];
			      (me.primitives || []).forEach(function (p) {
			        if ((p.mode === undefined ? 4 : p.mode) !== 4 || p.attributes.POSITION === undefined) return;
			        const q = { pos: acc(p.attributes.POSITION), color: [1, 1, 1, 1], tex: -1 };
			        q.nor = p.attributes.NORMAL !== undefined ? acc(p.attributes.NORMAL) : null;
			        q.uv = p.attributes.TEXCOORD_0 !== undefined ? acc(p.attributes.TEXCOORD_0) : null;
			        if (p.attributes.JOINTS_0 !== undefined && p.attributes.WEIGHTS_0 !== undefined) { q.joints = acc(p.attributes.JOINTS_0); q.weights = acc(p.attributes.WEIGHTS_0); }
			        const n = q.pos.length / 3;
			        q.idx = p.indices !== undefined ? Uint32Array.from(acc(p.indices)) : Uint32Array.from({ length: n }, function (_, i) { return i; });
			        if (!q.nor) { q.nor = new Float32Array(q.pos.length); for (let i = 0; i + 2 < q.idx.length; i += 3) { const a = q.idx[i] * 3, b = q.idx[i + 1] * 3, c = q.idx[i + 2] * 3, P = q.pos;
			          const ux = P[b] - P[a], uy = P[b + 1] - P[a + 1], uz = P[b + 2] - P[a + 2], vx = P[c] - P[a], vy = P[c + 1] - P[a + 1], vz = P[c + 2] - P[a + 2];
			          for (const k of [a, b, c]) { q.nor[k] += uy * vz - uz * vy; q.nor[k + 1] += uz * vx - ux * vz; q.nor[k + 2] += ux * vy - uy * vx; } } }
			        if (p.material !== undefined && json.materials) { const pbr = json.materials[p.material].pbrMetallicRoughness || {};
			          if (pbr.baseColorFactor) q.color = pbr.baseColorFactor.slice(); if (pbr.baseColorTexture && q.uv) q.tex = pbr.baseColorTexture.index; }
			        prims.push(q);
			      });
			      m.meshes.push(prims);
			    });
			    (json.nodes || []).forEach(function (n) { m.nodes.push({ parent: -1, mesh: n.mesh === undefined ? -1 : n.mesh, skin: n.skin === undefined ? -1 : n.skin, children: n.children || [], matrix: n.matrix || null,
			      t: n.translation || [0, 0, 0], r: n.rotation || [0, 0, 0, 1], s: n.scale || [1, 1, 1] }); });
			    m.nodes.forEach(function (n, i) { n.children.forEach(function (c) { if (m.nodes[c]) m.nodes[c].parent = i; }); });
			    const sc = json.scenes && json.scenes[json.scene || 0];
			    m.roots = sc && sc.nodes ? sc.nodes : m.nodes.map(function (n, i) { return n.parent < 0 ? i : -1; }).filter(function (i) { return i >= 0; });
			    (json.skins || []).forEach(function (sk) { const inv = sk.inverseBindMatrices !== undefined ? acc(sk.inverseBindMatrices) : null;
			      m.skins.push({ joints: sk.joints, inv: sk.joints.map(function (_, j) { return inv ? Array.from(inv.subarray(j * 16, j * 16 + 16)) : I; }) }); });
			    (json.animations || []).forEach(function (a, k) {
			      const an = { name: a.name || ('animation ' + k), duration: 0, channels: [] };
			      (a.channels || []).forEach(function (c) { const path = { translation: 0, rotation: 1, scale: 2 }[c.target.path]; if (path === undefined || c.target.node === undefined) return;
			        const sm = a.samplers[c.sampler], ch = { node: c.target.node, path: path, times: acc(sm.input), values: acc(sm.output), interp: sm.interpolation || 'LINEAR' };
			        if (ch.times.length) an.duration = Math.max(an.duration, ch.times[ch.times.length - 1]); an.channels.push(ch); });
			      m.anims.push(an);
			    });
			    m.norm = { cx: 0, cy: 0, minZ: 0, scale: 1 };
			    const rest = pose(m, m.anims.length ? 0 : -1, 0);
			    let x0 = Infinity, x1 = -Infinity, y0 = Infinity, y1 = -Infinity, z0 = Infinity, z1 = -Infinity;
			    rest.forEach(function (me) { for (let i = 0; i + 2 < me.pos.length; i += 3) { x0 = Math.min(x0, me.pos[i]); x1 = Math.max(x1, me.pos[i]); y0 = Math.min(y0, me.pos[i + 1]); y1 = Math.max(y1, me.pos[i + 1]); z0 = Math.min(z0, me.pos[i + 2]); z1 = Math.max(z1, me.pos[i + 2]); } });
			    if (isFinite(x0)) m.norm = { cx: (x0 + x1) / 2, cy: (y0 + y1) / 2, minZ: z0, scale: 1 / Math.max(1e-12, z1 - z0) };
			    return m;
			  }
			  function sample(c, t) {
			    const comps = c.path === 1 ? 4 : 3, cubic = c.interp === 'CUBICSPLINE', stride = cubic ? comps * 3 : comps, off = cubic ? comps : 0, ts = c.times;
			    let k = 0; while (k + 1 < ts.length && ts[k + 1] <= t) k++;
			    const a = []; for (let i = 0; i < comps; i++) a.push(c.values[Math.min(c.values.length - 1, k * stride + off + i)]);
			    if (k + 1 >= ts.length || c.interp === 'STEP' || t <= ts[0]) return a;
			    const w = (t - ts[k]) / Math.max(1e-9, ts[k + 1] - ts[k]), b = []; for (let i = 0; i < comps; i++) b.push(c.values[Math.min(c.values.length - 1, (k + 1) * stride + off + i)]);
			    if (c.path === 1) { const d = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3], sg = d < 0 ? -1 : 1; let l = 0; const q = []; for (let i = 0; i < 4; i++) { q.push(a[i] * (1 - w) + sg * b[i] * w); l += q[i] * q[i]; } l = Math.sqrt(l); return q.map(function (v) { return v / l; }); }
			    return a.map(function (v, i) { return v * (1 - w) + b[i] * w; });
			  }
			  function pose(m, anim, time) {
			    const n = m.nodes.length, tr = [], ro = [], sc = [], animated = new Array(n).fill(false);
			    for (let i = 0; i < n; i++) { tr.push(m.nodes[i].t); ro.push(m.nodes[i].r); sc.push(m.nodes[i].s); }
			    if (anim >= 0 && anim < m.anims.length) { const a = m.anims[anim]; let tt = a.duration > 0 ? time % a.duration : 0; if (tt < 0) tt += a.duration;
			      a.channels.forEach(function (c) { if (c.node >= n) return; const v = sample(c, tt); if (c.path === 0) tr[c.node] = v; else if (c.path === 1) ro[c.node] = v; else sc[c.node] = v; animated[c.node] = true; }); }
			    const local = [], global = new Array(n).fill(null);
			    for (let i = 0; i < n; i++) local.push(m.nodes[i].matrix && !animated[i] ? m.nodes[i].matrix : quatMat(tr[i], ro[i], sc[i]));
			    function walk(i, parent) { if (global[i]) return; global[i] = mul(parent, local[i]); m.nodes[i].children.forEach(function (c) { if (c < n) walk(c, global[i]); }); }
			    m.roots.forEach(function (r) { walk(r, I); }); for (let i = 0; i < n; i++) if (!global[i]) walk(i, I);
			    const out = [], N = m.norm || { cx: 0, cy: 0, minZ: 0, scale: 1 };
			    m.nodes.forEach(function (node, ni) {
			      if (node.mesh < 0 || !m.meshes[node.mesh]) return;
			      let joints = null;
			      if (node.skin >= 0 && m.skins[node.skin]) { const sk = m.skins[node.skin]; joints = sk.joints.map(function (j, k) { return mul(global[j] || I, sk.inv[k]); }); }
			      m.meshes[node.mesh].forEach(function (p) {
			        const nv = p.pos.length / 3, pos = new Float32Array(nv * 3), nor = new Float32Array(nv * 3); let mt = global[ni];
			        for (let v = 0; v < nv; v++) {
			          if (joints) { mt = new Array(16).fill(0); for (let k = 0; k < 4; k++) { const w = p.weights[v * 4 + k], j = Math.round(p.joints[v * 4 + k]); if (!w || !joints[j]) continue; for (let q = 0; q < 16; q++) mt[q] += w * joints[j][q]; } }
			          const x = p.pos[v * 3], y = p.pos[v * 3 + 1], z = p.pos[v * 3 + 2], nx = p.nor[v * 3], ny = p.nor[v * 3 + 1], nz = p.nor[v * 3 + 2];
			          const px = mt[0] * x + mt[4] * y + mt[8] * z + mt[12], py = mt[1] * x + mt[5] * y + mt[9] * z + mt[13], pz = mt[2] * x + mt[6] * y + mt[10] * z + mt[14];
			          let qx = mt[0] * nx + mt[4] * ny + mt[8] * nz, qy = mt[1] * nx + mt[5] * ny + mt[9] * nz, qz = mt[2] * nx + mt[6] * ny + mt[10] * nz; const l = Math.hypot(qx, qy, qz) || 1;
			          /* glTF (y up, front +z) -> z up, front +y: (x, y, z) -> (-x, z, y); centered, the base at 0, the height 1 */
			          pos[v * 3] = (-px - N.cx) * N.scale; pos[v * 3 + 1] = (pz - N.cy) * N.scale; pos[v * 3 + 2] = (py - N.minZ) * N.scale;
			          nor[v * 3] = -qx / l; nor[v * 3 + 1] = qz / l; nor[v * 3 + 2] = qy / l;
			        }
			        out.push({ pos: pos, nor: nor, uv: p.uv, idx: p.idx, color: p.color, tex: p.tex });
			      });
			    });
			    return out;
			  }
			  function animationIndex(m, name) {
			    if (!m.anims.length || name === 'none') return -1;
			    if (!name) return 0;
			    const l = String(name).toLowerCase();
			    for (let i = 0; i < m.anims.length; i++) if (m.anims[i].name.toLowerCase() === l) return i;
			    for (let i = 0; i < m.anims.length; i++) if (m.anims[i].name.toLowerCase().indexOf(l) >= 0) return i;
			    const k = parseInt(name, 10); return k >= 0 && k < m.anims.length ? k : 0;
			  }
			  return { parse: parse, pose: pose, animationIndex: animationIndex };
			})();
			const modelCache = {};
			function assetModel(name) {
			  if (!name || typeof ASSETS === 'undefined' || !ASSETS.models || !ASSETS.models[name]) return null;
			  if (modelCache[name] === undefined) {
			    try { const t = atob(ASSETS.models[name]), u = new Uint8Array(t.length); for (let i = 0; i < t.length; i++) u[i] = t.charCodeAt(i); modelCache[name] = Gltf3.parse(u.buffer); }
			    catch (e) { report(new Error('The 3D model ' + name + ' could not be read: ' + e.message)); modelCache[name] = null; }
			  }
			  return modelCache[name];
			}
""";

	/* The JavaScript of the models, images and shapes of the 3D view */
	private static final String MODELS3D_JS = """
			/* The textures of the models and the images */
			const textureCache = new Map();
			function glTexture(key, source) {
			  if (textureCache.has(key)) return textureCache.get(key);
			  textureCache.set(key, null);
			  const img = new Image();
			  img.onload = function () {
			    const t = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, t);
			    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, false);
			    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img);
			    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
			    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
			    textureCache.set(key, { tex: t, w: img.naturalWidth, h: img.naturalHeight }); render();
			  };
			  img.src = typeof source === 'string' ? source : URL.createObjectURL(new Blob([source.bytes], { type: source.mime }));
			  return null;
			}
			/* Billboards (images facing the camera): [x, y, z, size, angle, image name] */
			function drawSprites(list, mvp) {
			  if (!list.length) return;
			  const byName = new Map(); list.forEach(function (s) { if (!byName.has(s[5])) byName.set(s[5], []); byName.get(s[5]).push(s); });
			  const vm = spriteView, rx = vm[0], ry = vm[4], rz = vm[8], ux = vm[1], uy = vm[5], uz = vm[9];
			  useProgram(spriteProgram); gl.uniformMatrix4fv(gl.getUniformLocation(spriteProgram, 'mvp'), false, mvp);
			  gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
			  byName.forEach(function (items, name) {
			    if (typeof ASSETS === 'undefined' || !ASSETS.images[name]) return;
			    const t = glTexture('img:' + name, ASSETS.images[name]); if (!t) return;
			    const data = [], aw = t.w / Math.max(t.w, t.h), ah = t.h / Math.max(t.w, t.h);
			    items.forEach(function (s) {
			      const a = -(s[4] || 0) * Math.PI / 180, ca = Math.cos(a), sa = Math.sin(a), w = s[3] / 2 * aw, h = s[3] / 2 * ah;
			      const R = [(rx * ca + ux * sa) * w, (ry * ca + uy * sa) * w, (rz * ca + uz * sa) * w], U = [(-rx * sa + ux * ca) * h, (-ry * sa + uy * ca) * h, (-rz * sa + uz * ca) * h];
			      const c = [[-1, -1, 0, 1], [1, -1, 1, 1], [1, 1, 1, 0], [-1, 1, 0, 0]];
			      [0, 1, 2, 0, 2, 3].forEach(function (k) { const q = c[k]; data.push(s[0] + R[0] * q[0] + U[0] * q[1], s[1] + R[1] * q[0] + U[1] * q[1], s[2] + R[2] * q[0] + U[2] * q[1], q[2], q[3]); });
			    });
			    gl.bindBuffer(gl.ARRAY_BUFFER, buffers.sprite); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.DYNAMIC_DRAW);
			    attribute(spriteProgram, 'p', 3, 5, 0); attribute(spriteProgram, 'uv', 2, 5, 3);
			    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, t.tex); gl.uniform1i(gl.getUniformLocation(spriteProgram, 'tex'), 0);
			    gl.drawArrays(gl.TRIANGLES, 0, data.length / 5);
			  });
			  gl.disable(gl.BLEND);
			}
			let spriteView = null, animating3d = false;
			/* The agents with glTF models (posed for the time, the heading: given or the direction of the movement) and images */
			function drawAgentModels(p, indices, dirs, mvp, e, center, size, light, dark) {
			  const radius = p.radius || size * 0.01, sprites = [], poses = new Map(), seconds = performance.now() / 1000;
			  let animated = false;
			  useProgram(meshProgram);
			  gl.uniformMatrix4fv(gl.getUniformLocation(meshProgram, 'mvp'), false, mvp);
			  gl.uniform3fv(gl.getUniformLocation(meshProgram, 'eye'), e);
			  gl.uniform3fv(gl.getUniformLocation(meshProgram, 'light'), [center[0] - e[0] * 0.15 - size * 0.4, center[1] - size * 0.6, size * 1.6]);
			  gl.uniform4fv(gl.getUniformLocation(meshProgram, 'k'), light.k);
			  gl.uniform3fv(gl.getUniformLocation(meshProgram, 'sky'), dark ? [0.72, 0.8, 1.0] : [1.0, 1.0, 1.0]);
			  gl.uniform3fv(gl.getUniformLocation(meshProgram, 'ground'), dark ? [0.28, 0.25, 0.24] : [0.62, 0.58, 0.54]);
			  indices.forEach(function (i) {
			    const x = p.xyz[i * 3], y = p.xyz[i * 3 + 1], z = p.xyz[i * 3 + 2], sz = p.sizes && p.sizes[i] > 0 ? p.sizes[i] : 4 * radius;
			    let h = p.headings && !isNaN(p.headings[i]) ? p.headings[i] : NaN;
			    if (isNaN(h)) h = dirs && (dirs[i * 3] || dirs[i * 3 + 1]) ? Math.atan2(dirs[i * 3], dirs[i * 3 + 1]) * 180 / Math.PI : 0;
			    const m = p.models ? assetModel(p.models[i]) : null;
			    if (!m) { sprites.push([x, y, z, sz, 0, p.images[i]]); return; }
			    const a = Gltf3.animationIndex(m, p.animations ? p.animations[i] : null), group = a < 0 ? 0 : i % 4;
			    if (a >= 0) animated = true;
			    const key = p.models[i] + '/' + a + '/' + group;
			    if (!poses.has(key)) poses.set(key, Gltf3.pose(m, a, seconds + (a >= 0 ? group * m.anims[a].duration / 4 : 0)));
			    const r = -h * Math.PI / 180, c = Math.cos(r) * sz, s = Math.sin(r) * sz;
			    const model = [c, s, 0, 0, -s, c, 0, 0, 0, 0, sz, 0, x, y, z, 1];
			    gl.uniformMatrix4fv(gl.getUniformLocation(meshProgram, 'model'), false, new Float32Array(model));
			    poses.get(key).forEach(function (me) {
			      gl.bindBuffer(gl.ARRAY_BUFFER, buffers.mp); gl.bufferData(gl.ARRAY_BUFFER, me.pos, gl.DYNAMIC_DRAW); attribute(meshProgram, 'p', 3, 3, 0);
			      gl.bindBuffer(gl.ARRAY_BUFFER, buffers.mn); gl.bufferData(gl.ARRAY_BUFFER, me.nor, gl.DYNAMIC_DRAW); attribute(meshProgram, 'n', 3, 3, 0);
			      const tex = me.tex >= 0 && m.textures[me.tex] ? glTexture(m.textures[me.tex].bytes || m.textures[me.tex].uri, m.textures[me.tex].bytes ? m.textures[me.tex] : m.textures[me.tex].uri) : null;
			      if (tex && me.uv) { gl.bindBuffer(gl.ARRAY_BUFFER, buffers.mu); gl.bufferData(gl.ARRAY_BUFFER, me.uv, gl.DYNAMIC_DRAW); attribute(meshProgram, 'uv', 2, 2, 0);
			        gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, tex.tex); gl.uniform1i(gl.getUniformLocation(meshProgram, 'tex'), 0); }
			      gl.uniform1f(gl.getUniformLocation(meshProgram, 'useTex'), tex && me.uv ? 1 : 0);
			      gl.uniform4fv(gl.getUniformLocation(meshProgram, 'color'), me.color);
			      gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, buffers.mi);
			      const ext = gl.getExtension('OES_element_index_uint');
			      if (ext) { gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, me.idx, gl.DYNAMIC_DRAW); gl.drawElements(gl.TRIANGLES, me.idx.length, gl.UNSIGNED_INT, 0); }
			      else { gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, Uint16Array.from(me.idx), gl.DYNAMIC_DRAW); gl.drawElements(gl.TRIANGLES, me.idx.length, gl.UNSIGNED_SHORT, 0); }
			    });
			  });
			  drawSprites(sprites, mvp);
			  /* Animated models: the view is drawn again (each frame) */
			  if (animated && !animating3d) { animating3d = true; requestAnimationFrame(function again() { if (!animating3d) return; animating3d = false; draw3d(); }); }
			}
			/* The shapes of an agent model: lines in 3D, 2D shapes on the floor, pillars, images (billboards standing on the floor) */
			function drawShapes3d(mvp, lines, size, lit) {
			  const d = sim.drawing; if (!d || !d.length) return;
			  const segs = [], sprites = [], z0 = size * 0.002;
			  /* The pillars: a vertical cylinder from the floor (x, y, the radius, the height), the top a little smaller and lighter */
			  const pil = [];
			  for (const s of d) {
			    if (s.k !== 'pillar' || !s.p || s.p.length < 4) continue;
			    const x = s.p[0], y = s.p[1], r0 = s.p[2], r1 = s.p[2] * 0.85, h = s.p[3], c = color3(s.f || '#9aa6b8'), ct = [Math.min(1, c[0] * 1.2), Math.min(1, c[1] * 1.2), Math.min(1, c[2] * 1.2)], seg = 32;
			    for (let j = 0; j < seg; j++) {
			      const a1 = 2 * Math.PI * j / seg, a2 = 2 * Math.PI * (j + 1) / seg, c1 = Math.cos(a1), s1 = Math.sin(a1), c2 = Math.cos(a2), s2 = Math.sin(a2);
			      const v = function (cc, ss, r, z, col) { pil.push(x + r * cc, y + r * ss, z, cc, ss, 0.15, col[0], col[1], col[2]); };
			      v(c1, s1, r0, 0, c); v(c2, s2, r0, 0, c); v(c2, s2, r1, h, c);
			      v(c1, s1, r0, 0, c); v(c2, s2, r1, h, c); v(c1, s1, r1, h, c);
			      pil.push(x, y, h, 0, 0, 1, ct[0], ct[1], ct[2], x + r1 * c1, y + r1 * s1, h, 0, 0, 1, ct[0], ct[1], ct[2], x + r1 * c2, y + r1 * s2, h, 0, 0, 1, ct[0], ct[1], ct[2]);
			    }
			  }
			  if (pil.length && lit) lit(pil, buffers.tri);
			  function seg(a, b, c) { segs.push(a[0], a[1], a[2], c[0], c[1], c[2], b[0], b[1], b[2], c[0], c[1], c[2]); }
			  function outline(s) { const p = s.p; if (s.k === 'rect') return [p[0], p[1], p[0] + p[2], p[1], p[0] + p[2], p[1] + p[3], p[0], p[1] + p[3]];
			    if (s.k === 'circle') { const q = []; for (let i = 0; i < 48; i++) { const a = 2 * Math.PI * i / 48; q.push(p[0] + p[2] * Math.cos(a), p[1] + p[2] * Math.sin(a)); } return q; } return p; }
			  for (const s of d) {
			    if (s.k === 'image') { sprites.push([s.p[0], s.p[1], (s.w || 1) / 2, s.w || 1, s.a || 0, s.t]); continue; }
			    if (s.k === 'text' || s.k === 'pillar') continue;
			    if (s.k === 'line3') { if (s.s) seg([s.p[0], s.p[1], s.p[2]], [s.p[3], s.p[4], s.p[5]], color3(s.s)); continue; }
			    const q = outline(s), closed = s.k !== 'line' && s.k !== 'path';
			    if (s.f && closed && q.length >= 6) {
			      /* A transparent fill on the floor (a fan of triangles) */
			      const c = color3(s.f), alpha = s.f.length === 9 ? parseInt(s.f.substr(7, 2), 16) / 255 : 1, data = [];
			      for (let i = 2; i + 3 < q.length; i += 2) data.push(q[0], q[1], z0 * 0.5, c[0], c[1], c[2], q[i], q[i + 1], z0 * 0.5, c[0], c[1], c[2], q[i + 2], q[i + 3], z0 * 0.5, c[0], c[1], c[2]);
			      useProgram(lineProgram); gl.bindBuffer(gl.ARRAY_BUFFER, buffers.fill); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.DYNAMIC_DRAW);
			      attribute(lineProgram, 'p', 3, 6, 0); attribute(lineProgram, 'c', 3, 6, 3);
			      gl.uniformMatrix4fv(gl.getUniformLocation(lineProgram, 'mvp'), false, mvp); gl.uniform1f(gl.getUniformLocation(lineProgram, 'alpha'), alpha);
			      gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA); gl.depthMask(false);
			      gl.drawArrays(gl.TRIANGLES, 0, data.length / 6); gl.depthMask(true); gl.disable(gl.BLEND);
			    }
			    if (s.s) { const c = color3(s.s); for (let i = 0; i + 3 < q.length; i += 2) seg([q[i], q[i + 1], z0], [q[i + 2], q[i + 3], z0], c); if (closed && q.length >= 6) seg([q[q.length - 2], q[q.length - 1], z0], [q[0], q[1], z0], c); }
			  }
			  lines(segs, buffers.line);
			  drawSprites(sprites, mvp);
			}
""";

	private static final String TEMPLATE = """
			<!DOCTYPE html>
			<html>
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<title></title>
			<!--MAP-->
			<style>
			:root {
			  --bg: #eef1f6; --card: #ffffff; --text: #1d2433; --muted: #5f6b7d; --border: #dde2ea; --grid: #edf0f4;
			  --accent: #2f6fed; --accent-text: #ffffff; --accent-soft: #e6eeff; --button: #f3f5f9; --button-hover: #e7ebf2;
			  --view: #ffffff; --frame: #9aa4b2; --error: #c62828; --shadow: 0 1px 2px rgba(16, 24, 40, .06), 0 4px 14px rgba(16, 24, 40, .06);
			  --font: system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
			}
			:root[data-theme="dark"] {
			  --bg: #0f1218; --card: #181c25; --text: #e6e9ef; --muted: #98a2b3; --border: #2a303c; --grid: #232834;
			  --accent: #5b8cff; --accent-text: #0b1020; --accent-soft: #1d2a4a; --button: #222836; --button-hover: #2b3242;
			  --view: #12151c; --frame: #5c6575; --error: #ff6b6b; --shadow: 0 1px 2px rgba(0, 0, 0, .4), 0 6px 18px rgba(0, 0, 0, .25);
			}
			* { box-sizing: border-box; }
			html, body { margin: 0; height: 100%; font-family: var(--font); color: var(--text); background: var(--bg); }
			body { display: flex; gap: 14px; padding: 14px; }
			#side { width: 300px; min-width: 300px; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; }
			.card { background: var(--card); border: 1px solid var(--border); border-radius: 14px; box-shadow: var(--shadow); }
			#side > .card { padding: 14px; }
			.head { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; padding: 2px 2px 0 2px; }
			#side h1 { font-size: 19px; line-height: 1.25; margin: 0; font-weight: 650; letter-spacing: -0.01em; }
			#desc { font-size: 13px; line-height: 1.45; color: var(--muted); padding: 0 2px; }
			#desc:empty { display: none; }
			#main { flex: 1; display: flex; flex-direction: column; gap: 14px; min-width: 0; }
			#viewbox { flex: 1; position: relative; min-height: 0; overflow: hidden; background: var(--view); }
			#view, #view3d { position: absolute; left: 0; top: 0; width: 100%; height: 100%; display: block; }
			#chartbox { position: relative; overflow: hidden; display: grid; grid-template-columns: repeat(auto-fit, minmax(330px, 1fr)); gap: 6px; }
			#chartbox .chartcell { height: 210px; position: relative; }
			#chartbox canvas { width: 100%; height: 100%; display: block; }
			#chart { width: 100%; height: 100%; display: block; }
			.buttons { display: flex; gap: 8px; margin-bottom: 12px; }
			button { font-family: inherit; font-size: 13px; font-weight: 550; padding: 7px 12px; border: 1px solid var(--border); border-radius: 9px; background: var(--button); color: var(--text); cursor: pointer; transition: background .15s, color .15s, border-color .15s, transform .05s; }
			button:hover { background: var(--button-hover); }
			button:active { transform: translateY(1px); }
			button:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
			button.play { flex: 1; background: var(--accent); color: var(--accent-text); border-color: var(--accent); }
			button.play:hover { filter: brightness(1.08); background: var(--accent); }
			button.play.on { background: var(--accent-soft); color: var(--accent); }
			button.icon { width: 34px; height: 34px; padding: 0; font-size: 16px; line-height: 1; flex: none; border-radius: 50%; }
			#controls .control > button { width: 100%; }
			.control { margin: 12px 0; font-size: 13px; }
			.control:first-child { margin-top: 0; }
			.control:last-child { margin-bottom: 0; }
			.control label { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 6px; color: var(--text); }
			.control .value { color: var(--accent); font-weight: 600; font-variant-numeric: tabular-nums; }
			input[type=range] { width: 100%; accent-color: var(--accent); margin: 0; }
			.control input[type=number], .control input[type=text], .control select { width: 100%; padding: 7px 9px; font-family: inherit; font-size: 13px; color: var(--text); background: var(--bg); border: 1px solid var(--border); border-radius: 8px; }
			.control.check { display: flex; align-items: center; gap: 10px; }
			.control.check label { display: inline; margin: 0; cursor: pointer; }
			.control.check input { appearance: none; -webkit-appearance: none; width: 36px; height: 20px; flex: none; margin: 0; border-radius: 10px; background: var(--border); position: relative; cursor: pointer; transition: background .15s; }
			.control.check input::after { content: ""; position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 50%; background: #ffffff; box-shadow: 0 1px 2px rgba(0, 0, 0, .3); transition: left .15s; }
			.control.check input:checked { background: var(--accent); }
			.control.check input:checked::after { left: 18px; }
			h2 { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); margin: 18px 0 8px 0; }
			#controls > h2:first-child, #scenebox > h2:first-child { margin-top: 0; }
			#controls:empty { display: none; }
			#status { display: inline-block; font-size: 12px; color: var(--muted); background: var(--bg); border-radius: 999px; padding: 3px 10px; margin-top: 2px; font-variant-numeric: tabular-nums; }
			#error { color: var(--error); font-size: 12px; white-space: pre-wrap; border: 1px solid var(--error); border-radius: 10px; padding: 8px 10px; }
			#error:empty { display: none; }
			#hint { position: absolute; bottom: 8px; left: 12px; font-size: 11px; color: var(--muted); pointer-events: none; }
			.foot { font-size: 11px; color: var(--muted); padding: 0 2px; margin-top: auto; }
			#mapview { position: absolute; left: 0; top: 0; width: 100%; height: 100%; display: none; background: var(--view); }
			.simgrid { image-rendering: pixelated; image-rendering: crisp-edges; }
			.leaflet-container { font-family: var(--font); }
			#mapbox:empty { display: none; }
			/* ----- The accent colors ----- */
			:root[data-accent="teal"] { --accent: #0d9488; --accent-soft: #ccfbf1; }
			:root[data-accent="violet"] { --accent: #7c3aed; --accent-soft: #ede9fe; }
			:root[data-accent="orange"] { --accent: #ea580c; --accent-soft: #ffedd5; }
			:root[data-accent="graphite"] { --accent: #334155; --accent-soft: #e2e8f0; }
			:root[data-theme="dark"][data-accent="teal"] { --accent: #2dd4bf; --accent-soft: #123b38; }
			:root[data-theme="dark"][data-accent="violet"] { --accent: #a78bfa; --accent-soft: #2a2147; }
			:root[data-theme="dark"][data-accent="orange"] { --accent: #fb923c; --accent-soft: #3b2414; }
			:root[data-theme="dark"][data-accent="graphite"] { --accent: #cbd5e1; --accent-soft: #263040; }
			/* ----- Layout: dashboard (a top bar, the view and the chart, the settings on the right) ----- */
			body.layout-dashboard { display: grid; grid-template-columns: minmax(0, 1fr) 340px; grid-template-rows: auto minmax(0, 1fr); grid-template-areas: "top top" "main side"; }
			body.layout-dashboard #topbar { grid-area: top; display: flex; align-items: center; gap: 24px; padding: 12px 18px; }
			body.layout-dashboard #topbar .titles { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
			body.layout-dashboard #topbar .head { padding: 0; }
			body.layout-dashboard #topbar #desc { padding: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 70ch; }
			body.layout-dashboard #runcard { display: flex; align-items: center; gap: 18px; padding: 0; border: none; box-shadow: none; background: none; }
			body.layout-dashboard #runcard .buttons { margin: 0; }
			body.layout-dashboard #runcard .control { margin: 0; min-width: 190px; }
			body.layout-dashboard #main { grid-area: main; }
			body.layout-dashboard #side { grid-area: side; width: auto; min-width: 0; }
			/* ----- Layout: focus (the view fills the window, floating panels) ----- */
			body.layout-focus { display: block; padding: 0; overflow: hidden; }
			body.layout-focus #main { position: fixed; inset: 0; gap: 0; }
			body.layout-focus #viewbox { border-radius: 0; border: none; box-shadow: none; }
			body.layout-focus #side { position: fixed; top: 16px; left: 16px; width: 320px; min-width: 0; max-height: calc(100% - 32px); z-index: 1000; padding: 12px; border-radius: 18px;
			  background: color-mix(in srgb, var(--bg) 72%, transparent); -webkit-backdrop-filter: blur(16px); backdrop-filter: blur(16px); border: 1px solid var(--border); box-shadow: var(--shadow);
			  transition: transform .3s ease, opacity .3s ease; }
			body.layout-focus #chartbox { position: fixed; right: 16px; bottom: 34px; width: min(520px, 42vw); max-height: 60vh; overflow-y: auto; grid-template-columns: 1fr; z-index: 1000; transition: transform .3s ease, opacity .3s ease; }
			body.layout-focus.panels-hidden #side { transform: translateX(calc(-100% - 24px)); opacity: 0; pointer-events: none; }
			body.layout-focus.panels-hidden #chartbox { transform: translateY(calc(100% + 40px)); opacity: 0; pointer-events: none; }
			#panelToggle { display: none; }
			body.layout-focus #panelToggle { display: block; position: fixed; top: 16px; right: 16px; z-index: 1001; box-shadow: var(--shadow); }
			body.layout-focus .leaflet-top.leaflet-right { top: 58px; }
			body.layout-focus #hint { left: 50%; transform: translateX(-50%); bottom: 10px; }
			/* ----- Layout: story (one column for reading and embedding) ----- */
			body.layout-story { display: block; height: auto; min-height: 100%; padding: 28px 16px 40px 16px; }
			#column { max-width: 1060px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
			body.layout-story .head h1 { font-size: clamp(22px, 3.2vw, 32px); }
			body.layout-story #desc { font-size: 15px; max-width: 75ch; }
			body.layout-story #viewbox { flex: none; width: 100%; aspect-ratio: 16 / 10; max-height: 78vh; min-height: 260px; }
			body.layout-story #runcard { display: flex; flex-wrap: wrap; align-items: center; gap: 12px 24px; padding: 12px 16px; }
			body.layout-story #runcard .buttons { margin: 0; }
			body.layout-story #runcard .control { margin: 0; min-width: 200px; flex: 1; }
			#panels { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; align-items: start; }
			#panels > .card { padding: 14px; }
			body.layout-story #chartbox .chartcell { height: 260px; }
			.simimage, .simtext { background: none; border: none; }
			/* ----- Narrow windows (phones): one column ----- */
			@media (max-width: 900px) {
			  body.layout-dashboard { display: flex; flex-direction: column; height: auto; min-height: 100%; }
			  body.layout-dashboard #topbar { flex-wrap: wrap; }
			  body.layout-dashboard #topbar #desc { white-space: normal; }
			  body.layout-dashboard #main #viewbox { flex: none; height: 70vw; min-height: 280px; }
			  body.layout-dashboard #main { order: 1; }
			  body.layout-dashboard #side { order: 2; }
			  body.layout-dashboard #runcard { flex-wrap: wrap; width: 100%; gap: 10px 16px; }
			  body.layout-dashboard #runcard .control { min-width: 0; flex: 1 1 160px; }
			}
			@media (max-width: 760px) {
			  body.layout-focus #side { left: 8px; right: 8px; top: auto; bottom: 8px; width: auto; max-height: 46vh; }
			  body.layout-focus.panels-hidden #side { transform: translateY(calc(100% + 24px)); }
			  body.layout-focus #chartbox { display: none; }
			  body.layout-story { padding: 14px 10px 28px 10px; }
			  body.layout-story #viewbox { aspect-ratio: 4 / 5; height: auto; }
			  body.layout-focus #viewbox { height: 100%; min-height: 0; flex: 1; }
			}
			sup, sub { font-size: 70%; }
			@media (max-width: 760px) {
			  body { flex-direction: column; height: auto; min-height: 100%; padding: 10px; }
			  #side { width: auto; min-width: 0; overflow: visible; }
			  #viewbox { flex: none; height: 70vw; min-height: 260px; }
			}
			</style>
			</head>
			<body>
			<div id="side">
			<div class="head"><h1 id="title"></h1><button id="theme" class="icon" title="Light or dark theme" aria-label="Light or dark theme">&#9680;</button></div>
			<div id="desc"></div>
			<div class="card" id="runcard">
			<div class="buttons"><button id="play" class="play">Play</button><button id="step">Step</button><button id="reset">Reset</button></div>
			<div class="control"><label>Speed <span class="value" id="speedValue"></span></label><input type="range" id="speed" min="0" max="100" step="1"></div>
			<div id="status">Step 0</div>
			<div class="control" id="timelineBox" style="display:none"><label>Timeline</label><input type="range" id="timeline" min="0" max="0" step="1" value="0"></div>
			</div>
			<div id="controls" class="card"></div>
			<div id="scenebox" class="card" style="display:none"></div>
			<div id="mapbox" class="card" style="display:none"></div>
			<div id="error"></div>
			<div class="foot" id="foot">Created with Bio7</div>
			</div>
			<button id="panelToggle" title="Show or hide the panels" aria-label="Show or hide the panels">&#9776; Panels</button>
			<div id="main">
			<div id="viewbox" class="card"><canvas id="view"></canvas><canvas id="view3d" style="display:none"></canvas><div id="mapview"></div><div id="hint"></div></div>
			<div id="chartbox" class="card" style="display:none"><div class="chartcell"><canvas id="chart"></canvas></div></div>
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
			/* ----- The theme (light, dark or the setting of the system) ----- */
			const themeButton = document.getElementById('theme');
			function storedTheme() { try { return localStorage.getItem('bio7-sim-theme'); } catch (e) { return null; } }
			function systemDark() { return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches; }
			let theme = storedTheme() || CONFIG.theme;
			function applyTheme() {
			  const dark = theme === 'dark' || (theme === 'auto' && systemDark());
			  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
			  themeButton.innerHTML = dark ? '&#9788;' : '&#9790;';
			}
			applyTheme();
			/* ----- The layout of the page (classic, dashboard, focus, story) and the accent color ----- */
			(function () {
			  const layout = CONFIG.layout || 'classic';
			  document.body.classList.add('layout-' + layout);
			  document.documentElement.setAttribute('data-accent', CONFIG.accent || 'blue');
			  const $ = function (id) { return document.getElementById(id); };
			  const side = $('side'), main = $('main'), head = side.querySelector('.head');
			  if (layout === 'dashboard') {
			    const top = document.createElement('header'); top.id = 'topbar'; top.className = 'card';
			    const titles = document.createElement('div'); titles.className = 'titles';
			    const desc = $('desc'), run = $('runcard');
			    desc.title = desc.textContent;
			    titles.appendChild(head); titles.appendChild(desc);
			    top.appendChild(titles); top.appendChild(run);
			    document.body.insertBefore(top, document.body.firstChild);
			  } else if (layout === 'story') {
			    const col = document.createElement('div'); col.id = 'column';
			    const panels = document.createElement('div'); panels.id = 'panels';
			    const parts = [head, $('desc'), $('viewbox'), $('runcard'), panels, $('chartbox'), $('error'), $('foot')];
			    [$('controls'), $('scenebox'), $('mapbox')].forEach(function (e) { panels.appendChild(e); });
			    parts.forEach(function (e) { col.appendChild(e); });
			    document.body.appendChild(col);
			    side.remove(); main.remove();
			  } else if (layout === 'focus') {
			    $('panelToggle').addEventListener('click', function () { document.body.classList.toggle('panels-hidden'); });
			    /* Phones: the panels start hidden (the view first) */
			    if (window.matchMedia && window.matchMedia('(max-width: 760px)').matches) document.body.classList.add('panels-hidden');
			  }
			  if (layout !== 'focus') $('panelToggle').remove();
			})();
			themeButton.addEventListener('click', function () {
			  theme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
			  try { localStorage.setItem('bio7-sim-theme', theme); } catch (e) { }
			  applyTheme(); if (typeof render === 'function') render();
			});
			if (window.matchMedia) window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () { if (theme === 'auto') { applyTheme(); render(); } });
			/* A color of the theme (a CSS variable) */
			function cssColor(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }
			/* The background of the view: the color of the export or of the theme */
			function viewBackground() { return CONFIG.background || cssColor('--view'); }
			function userFunction(name) { try { return new Function('return typeof ' + name + ' === "function" ? ' + name + ' : null;')(); } catch (e) { return null; } }
			function report(e) { errorElement.textContent = 'Error: ' + (e && e.message ? e.message : e); playing = false; updatePlay(); console.error(e); }

			/* ----- The colors ----- */
			/* A color #rrggbb or #rrggbbaa: [r, g, b, a] (0 - 255) */
			function hexColor(h) { return [parseInt(h.substr(1, 2), 16), parseInt(h.substr(3, 2), 16), parseInt(h.substr(5, 2), 16), h.length >= 9 ? parseInt(h.substr(7, 2), 16) : 255]; }
			const ramp = CONFIG.colorMap.map(hexColor);
			/* The color of t (0 - 1) on the color map (or on another ramp, e.g., sim.ramp of the model) */
			function rampColor(t, r) {
			  r = r || ramp;
			  if (!(t >= 0)) t = 0; if (t > 1) t = 1;
			  const p = t * (r.length - 1), a = Math.min(r.length - 2, Math.floor(p)), w = p - a;
			  return [r[a][0] + w * (r[a + 1][0] - r[a][0]), r[a][1] + w * (r[a + 1][1] - r[a][1]), r[a][2] + w * (r[a + 1][2] - r[a][2]), r[a][3] + w * (r[a + 1][3] - r[a][3])];
			}
			const CATEGORY = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd', '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'];

			/* ----- The simulation object ----- */
			const series = new Map(), moreCharts = new Map();
			function moreChart(title, kind) {
			  if (!moreCharts.has(title)) moreCharts.set(title, { kind: kind, series: new Map(), labels: [], values: [], add: function (name, x, y) { if (!this.series.has(name)) this.series.set(name, []); this.series.get(name).push([x, y]); } });
			  return moreCharts.get(title);
			}
			const sim = {
			  p: {}, t: 0, width: 0, height: 0, cells: null, palette: null, min: 0, max: 1, agents: null,
			  box: [1, 1, 1], points: null, lines: null, surface: null,
			  random: Math.random,
			  chart: function (name, value) { if (!series.has(name)) series.set(name, []); series.get(name).push([this.t, value]); },
			  /* Further charts with a title: lines over the steps, XY points (e.g., a phase plot), bars (replaced) */
			  chartLine: function (title, name, x, y) { moreChart(title, 'line').add(name, x, y); },
			  chartXY: function (title, name, x, y) { moreChart(title, 'xy').add(name, x, y); },
			  chartBars: function (title, labels, values) { const c = moreChart(title, 'bars'); c.labels = labels; c.values = values; },
			  clearChart: function () { series.clear(); moreCharts.clear(); },
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
			      const fn = userFunction(c.fn);
			      if (!fn) { report(new Error('The function ' + c.fn + '(sim) is not defined')); return; }
			      if (CONFIG.async) { enqueue(async function () { await fn(sim); render(); }); return; }
			      try { fn(sim); render(); }
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
			/* sim.status(text): a message in the status line (e.g., while prepare(sim) loads) */
			sim.status = function (text) { statusElement.textContent = text; };
			const speedInput = document.getElementById('speed'), speedValue = document.getElementById('speedValue');
			/* The speed slider: 0.5 - 500 steps per second (logarithmic) */
			function speedFromSlider(v) { return 0.5 * Math.pow(1000, v / 100); }
			speedInput.value = Math.round(100 * Math.log(stepsPerSecond / 0.5) / Math.log(1000));
			function showSpeed() { speedValue.textContent = (stepsPerSecond < 10 ? stepsPerSecond.toFixed(1) : Math.round(stepsPerSecond)) + ' steps/s'; }
			showSpeed();
			speedInput.addEventListener('input', function () { stepsPerSecond = speedFromSlider(parseFloat(speedInput.value)); showSpeed(); });
			function updatePlay() { playButton.textContent = playing ? 'Pause' : 'Play'; playButton.classList.toggle('on', playing); }
			playButton.addEventListener('click', function () { playing = !playing; updatePlay(); lastTime = performance.now(); accumulator = 0; if (playing) requestAnimationFrame(loop); });
			document.getElementById('step').addEventListener('click', function () {
			  if (CONFIG.async) { enqueue(async function () { if (await doStepAsync()) render(); }); return; }
			  if (doStep()) render();
			});
			/* Asynchronous models (CONFIG.async): setup, the steps and the buttons one after the other */
			let queue = Promise.resolve(), busy = false;
			function enqueue(task) {
			  busy = true;
			  queue = queue.then(task).catch(function (e) { playing = false; updatePlay(); report(e); }).then(function () { busy = false; });
			  return queue;
			}
			async function doStepAsync() {
			  if (typeof step !== 'function') throw new Error('The code needs a function step(sim)');
			  if (await step(sim) === false) { playing = false; updatePlay(); return false; }
			  sim.t++; return true;
			}
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
			  if (CONFIG.async) {
			    return enqueue(async function () {
			      errorElement.textContent = ''; series.clear(); moreCharts.clear(); sim.t = 0;
			      sim.cells = null; sim.agents = null; sim.points = null; sim.lines = null; sim.surface = null; sim.geoAgents = null;
			      if (typeof setup !== 'function') throw new Error('The code needs a function setup(sim)');
			      try { await setup(sim); } finally { render(); }
			    });
			  }
			  errorElement.textContent = ''; series.clear(); moreCharts.clear(); sim.t = 0;
			  sim.cells = null; sim.agents = null; sim.points = null; sim.lines = null; sim.surface = null; sim.geoAgents = null;
			  try { if (typeof setup !== 'function') throw new Error('The code needs a function setup(sim)'); setup(sim); }
			  catch (e) { report(e); }
			  render();
			}
			function loop(now) {
			  if (!playing) return;
			  accumulator += Math.min(0.25, (now - lastTime) / 1000) * stepsPerSecond; lastTime = now;
			  if (CONFIG.async) {
			    /* One step at a time (the next when the step has finished) */
			    if (!busy && accumulator >= 1) { accumulator = Math.min(accumulator - 1, 2); enqueue(async function () { if (await doStepAsync()) render(); }); }
			    requestAnimationFrame(loop);
			    return;
			  }
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
			  if (mapActive()) { view.style.display = 'none'; initMap(); if (leafletMap) document.getElementById('mapview').style.display = 'block'; }
			  const r = resizeCanvas(view), ctx = view.getContext('2d');
			  ctx.fillStyle = viewBackground(); ctx.fillRect(0, 0, view.width, view.height);
			  const w = sim.width, h = sim.height;
			  if (!sim.cells || !(w > 0) || !(h > 0)) return;
			  if (offscreen.width !== w || offscreen.height !== h) { offscreen.width = w; offscreen.height = h; }
			  const octx = offscreen.getContext('2d'), image = octx.createImageData(w, h), d = image.data;
			  const pal = sim.palette ? sim.palette.map(hexColor) : null, span = sim.max - sim.min || 1, ownRamp = sim.ramp && sim.ramp.length > 1 ? sim.ramp.map(hexColor) : null;
			  for (let i = 0; i < w * h; i++) {
			    const v = sim.cells[i];
			    let c;
			    if (pal) { c = pal[Math.max(0, Math.min(pal.length - 1, v | 0))]; } else { c = rampColor((v - sim.min) / span, ownRamp); }
			    d[i * 4] = c[0]; d[i * 4 + 1] = c[1]; d[i * 4 + 2] = c[2]; d[i * 4 + 3] = v !== v ? 0 : c[3];
			  }
			  octx.putImageData(image, 0, 0);
			  /* On the map: the grid is an overlay of the map */
			  if (mapActive()) { view.style.display = 'none'; updateMapOverlay(); return; }
			  view.style.display = '';
			  const scale = Math.min(view.width / w, view.height / h), ox = (view.width - w * scale) / 2, oy = (view.height - h * scale) / 2;
			  ctx.imageSmoothingEnabled = false; ctx.drawImage(offscreen, ox, oy, w * scale, h * scale);
			  ctx.strokeStyle = cssColor('--frame'); ctx.lineWidth = r; ctx.strokeRect(ox, oy, w * scale, h * scale);
			  const a = sim.agents;
			  if (a && a.x) {
			    ctx.fillStyle = a.color || '#d62728';
			    const radius = Math.max(1, (a.radius || 0.5) * scale);
			    for (let i = 0; i < a.x.length; i++) {
			      if (a.colors) ctx.fillStyle = a.colors[i];
			      ctx.beginPath(); ctx.arc(ox + a.x[i] * scale, oy + a.y[i] * scale, radius, 0, 2 * Math.PI); ctx.fill();
			    }
			  }
			  /* The shapes and images of the model (in cells) */
			  if (sim.drawing && sim.drawing.length) {
			    ctx.save(); ctx.beginPath(); ctx.rect(ox, oy, w * scale, h * scale); ctx.clip();
			    drawShapes(ctx, sim.drawing, { x: function (v) { return ox + v * scale; }, y: function (v) { return oy + v * scale; }, len: function (v) { return v * scale; }, radius: function (v) { return v * scale; }, size: function (v) { return v * scale; }, px: r });
			    ctx.restore();
			  }
			}
			/*CANVAS*/

			/* ----- The map (Leaflet, online): the grid as an overlay ----- */
			const BASEMAPS = {
			  osm: { label: 'OpenStreetMap', url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors', maxZoom: 19 },
			  topo: { label: 'OpenTopoMap (terrain)', url: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', attribution: 'Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, SRTM | Map style: &copy; <a href="https://opentopomap.org">OpenTopoMap</a> (CC-BY-SA)', maxZoom: 17 },
			};
			/*
			 * The tile servers of OpenStreetMap need the address of the page (Referer, tile usage policy): a local file
			 * (file://) has none and gets "Access blocked", so a local page shows OpenTopoMap (also OpenStreetMap data).
			 */
			const localPage = location.protocol === 'file:';
			if (localPage) delete BASEMAPS.osm;
			const mapState = CONFIG.map ? { on: true, basemap: BASEMAPS[CONFIG.map.basemap] ? CONFIG.map.basemap : 'topo', opacity: CONFIG.map.opacity } : null;
			let leafletMap = null, tileLayer = null, gridOverlay = null;
			function mapAvailable() { return CONFIG.map && typeof L !== 'undefined'; }
			function mapActive() { return mapAvailable() && mapState.on; }
			function setBasemap() {
			  if (!leafletMap) return;
			  if (tileLayer) leafletMap.removeLayer(tileLayer);
			  const b = BASEMAPS[mapState.basemap];
			  tileLayer = L.tileLayer(b.url, { attribution: b.attribution, maxZoom: b.maxZoom, subdomains: 'abc', r: (window.devicePixelRatio || 1) > 1 ? '@2x' : '' }).addTo(leafletMap);
			}
			function mapBounds() { return CONFIG.map.bounds || sim.mapBounds || null; }
			function initMap() {
			  if (leafletMap || !mapAvailable() || !mapBounds()) return;
			  const div = document.getElementById('mapview');
			  div.style.display = 'block';
			  leafletMap = L.map(div, { zoomSnap: 0.25, preferCanvas: true, zoomControl: false });
			  L.control.zoom({ position: CONFIG.layout === 'focus' ? 'topright' : 'topleft' }).addTo(leafletMap);
			  leafletMap.attributionControl.setPrefix('<a href="https://leafletjs.com">Leaflet</a>');
			  if (CONFIG.map.attribution) leafletMap.attributionControl.addAttribution('Data: ' + CONFIG.map.attribution);
			  setBasemap();
			  leafletMap.fitBounds(mapBounds(), { padding: [10, 10] });
			  if (CONFIG.map.bounds) {
			    gridOverlay = L.imageOverlay(offscreen.toDataURL(), CONFIG.map.bounds, { opacity: mapState.opacity, className: 'simgrid', interactive: false }).addTo(leafletMap);
			    L.rectangle(CONFIG.map.bounds, { color: '#2f6fed', weight: 1, fill: false, interactive: false }).addTo(leafletMap);
			  }
			}
			/* The agents of a geospatial model (sim.geoAgents): points on the map, else on the canvas (longitude and latitude) */
			let geoMarkers = [], geoImages = [], geoShapes = null;
			/* The shapes of a geospatial agent model on the map (longitude and latitude, the radius in km) */
			function drawGeoShapes() {
			  if (!leafletMap) return;
			  if (geoShapes) geoShapes.clearLayers(); else geoShapes = L.layerGroup().addTo(leafletMap);
			  for (const s of sim.drawing || []) {
			    const p = s.p, o = { color: s.s ? s.s.substr(0, 7) : '#000', opacity: s.s ? (s.s.length === 9 ? parseInt(s.s.substr(7, 2), 16) / 255 : 1) : 0, weight: s.w || 1, interactive: false,
			      fill: !!s.f, fillColor: s.f ? s.f.substr(0, 7) : null, fillOpacity: s.f ? (s.f.length === 9 ? parseInt(s.f.substr(7, 2), 16) / 255 : 1) : 0 };
			    const ll = function (q) { const r = []; for (let i = 0; i + 1 < q.length; i += 2) r.push([q[i + 1], q[i]]); return r; };
			    if (s.k === 'line' || s.k === 'path') L.polyline(ll(p), o).addTo(geoShapes);
			    else if (s.k === 'line3') L.polyline([[p[1], p[0]], [p[4], p[3]]], o).addTo(geoShapes);
			    else if (s.k === 'polygon') L.polygon(ll(p), o).addTo(geoShapes);
			    else if (s.k === 'rect') L.polygon(ll([p[0], p[1], p[0] + p[2], p[1], p[0] + p[2], p[1] + p[3], p[0], p[1] + p[3]]), o).addTo(geoShapes);
			    else if (s.k === 'circle') L.circle([p[1], p[0]], Object.assign({ radius: p[2] * 1000 }, o)).addTo(geoShapes);
			    else if (s.k === 'text') L.marker([p[1], p[0]], { interactive: false, icon: L.divIcon({ className: 'simtext', iconSize: null, html: '<span style="white-space:nowrap;font-size:' + (s.w || 12) + 'px;color:' + (s.s ? s.s.substr(0, 7) : 'inherit') + '">' + String(s.t).replace(/</g, '&lt;') + '</span>' }) }).addTo(geoShapes);
			    else if (s.k === 'image' && typeof ASSETS !== 'undefined' && ASSETS.images[s.t]) {
			      const sz = s.w || 24;
			      L.marker([p[1], p[0]], { interactive: false, icon: L.divIcon({ className: 'simimage', iconSize: [sz, sz], html: '<img src="' + ASSETS.images[s.t] + '" style="width:' + sz + 'px;height:' + sz + 'px;object-fit:contain;transform:rotate(' + (s.a || 0) + 'deg)">' }) }).addTo(geoShapes);
			    }
			  }
			}
			function drawGeoAgents() {
			  const g = sim.geoAgents;
			  if (!g || !g.lon) { geoMarkers.forEach(function (m) { m.remove(); }); geoMarkers = []; geoImages.forEach(function (m) { if (m) m.remove(); }); geoImages = []; return; }
			  const n = g.lon.length;
			  if (mapActive()) {
			    initMap(); if (!leafletMap) return;
			    while (geoMarkers.length < n) geoMarkers.push(L.circleMarker([0, 0], { weight: 1, color: '#ffffff', fillOpacity: 0.9, interactive: true }).addTo(leafletMap));
			    while (geoMarkers.length > n) geoMarkers.pop().remove();
			    drawGeoShapes();
			    for (let i = 0; i < n; i++) {
			      const m = geoMarkers[i];
			      /* An image of the agent: a marker with the image (rotated to the heading) instead of the point */
			      if (g.images && g.images[i] && typeof ASSETS !== 'undefined' && ASSETS.images[g.images[i]]) {
			        m.setStyle({ opacity: 0, fillOpacity: 0 });
			        if (!geoImages[i]) geoImages[i] = L.marker([0, 0], { interactive: false, keyboard: false }).addTo(leafletMap);
			        const sz = Math.max(8, g.sizes[i] * 4), key = g.images[i] + '/' + sz + '/' + Math.round(g.headings[i]);
			        geoImages[i].setLatLng([g.lat[i], g.lon[i]]);
			        if (geoImages[i]._bio7 !== key) {
			          geoImages[i]._bio7 = key;
			          geoImages[i].setIcon(L.divIcon({ className: 'simimage', iconSize: [sz, sz], html: '<img src="' + ASSETS.images[g.images[i]] + '" style="width:' + sz + 'px;height:' + sz + 'px;object-fit:contain;transform:rotate(' + g.headings[i] + 'deg)">' }));
			        }
			        continue;
			      } else if (geoImages[i]) { geoImages[i].remove(); geoImages[i] = null; }
			      m.setLatLng([g.lat[i], g.lon[i]]);
			      m.setRadius(g.sizes[i]);
			      m.setStyle({ fillColor: g.colors[i].substr(0, 7), fillOpacity: mapState.opacity });
			      if (g.labels[i]) { if (!m.getTooltip()) m.bindTooltip(g.labels[i]); else m.setTooltipContent(g.labels[i]); }
			    }
			    return;
			  }
			  const b = sim.mapBounds, r = resizeCanvas(view), ctx = view.getContext('2d');
			  view.style.display = '';
			  ctx.fillStyle = viewBackground(); ctx.fillRect(0, 0, view.width, view.height);
			  if (!b) return;
			  const dw = b[1][1] - b[0][1], dh = b[1][0] - b[0][0], k = Math.cos((b[0][0] + b[1][0]) / 2 * Math.PI / 180);
			  const scale = Math.min(view.width / (dw * k), view.height / dh) * 0.95, ox = (view.width - dw * k * scale) / 2, oy = (view.height - dh * scale) / 2;
			  ctx.strokeStyle = cssColor('--frame'); ctx.lineWidth = r; ctx.strokeRect(ox, oy, dw * k * scale, dh * scale);
			  if (sim.drawing && sim.drawing.length) drawShapes(ctx, sim.drawing, { x: function (v) { return ox + (v - b[0][1]) * k * scale; }, y: function (v) { return oy + (b[1][0] - v) * scale; },
			    len: function (v) { return v * scale; }, radius: function (km) { return km / 110.574 * scale; }, size: function (v) { return v * r; }, px: r });
			  for (let i = 0; i < n; i++) {
			    const gi = g.images && g.images[i] ? assetImage(g.images[i]) : null;
			    if (gi) {
			      const sz = Math.max(8, g.sizes[i] * 4) * r, q = sz / Math.max(gi.naturalWidth, gi.naturalHeight);
			      ctx.save(); ctx.translate(ox + (g.lon[i] - b[0][1]) * k * scale, oy + (b[1][0] - g.lat[i]) * scale); ctx.rotate(g.headings[i] * Math.PI / 180);
			      ctx.drawImage(gi, -gi.naturalWidth * q / 2, -gi.naturalHeight * q / 2, gi.naturalWidth * q, gi.naturalHeight * q); ctx.restore();
			      continue;
			    }
			    ctx.fillStyle = g.colors[i].substr(0, 7);
			    ctx.beginPath(); ctx.arc(ox + (g.lon[i] - b[0][1]) * k * scale, oy + (b[1][0] - g.lat[i]) * scale, g.sizes[i] * r, 0, 2 * Math.PI); ctx.fill();
			    if (g.labels[i]) { ctx.fillStyle = cssColor('--text'); ctx.font = (10 * r) + 'px ' + cssColor('--font'); ctx.fillText(g.labels[i], ox + (g.lon[i] - b[0][1]) * k * scale + 6 * r, oy + (b[1][0] - g.lat[i]) * scale - 4 * r); }
			  }
			}
			let overlayPending = false;
			let shapeOverlay = null; const shapeCanvas = document.createElement('canvas');
			function updateMapOverlay() {
			  initMap();
			  document.getElementById('mapview').style.display = 'block';
			  if (!gridOverlay || overlayPending) return;
			  /* At most one update of the image per frame */
			  overlayPending = true;
			  requestAnimationFrame(function () {
			    overlayPending = false; gridOverlay.setUrl(offscreen.toDataURL());
			    /* The shapes and images (in cells): a finer transparent image over the grid */
			    const d = sim.drawing && sim.drawing.length ? sim.drawing : null;
			    if (!d && !shapeOverlay) return;
			    const k = Math.max(1, Math.ceil(1600 / Math.max(sim.width, sim.height)));
			    shapeCanvas.width = sim.width * k; shapeCanvas.height = sim.height * k;
			    const c = shapeCanvas.getContext('2d'); c.clearRect(0, 0, shapeCanvas.width, shapeCanvas.height);
			    if (d) drawShapes(c, d, { x: function (v) { return v * k; }, y: function (v) { return v * k; }, len: function (v) { return v * k; }, radius: function (v) { return v * k; }, size: function (v) { return v * k; }, px: Math.max(1, k / 4) });
			    if (!shapeOverlay) shapeOverlay = L.imageOverlay(shapeCanvas.toDataURL(), CONFIG.map.bounds, { opacity: 1, interactive: false }).addTo(leafletMap);
			    else shapeOverlay.setUrl(shapeCanvas.toDataURL());
			  });
			}
			/* The panel of the map */
			(function () {
			  const box = document.getElementById('mapbox');
			  if (!CONFIG.map) return;
			  box.style.display = '';
			  const h = document.createElement('h2'); h.textContent = 'Map'; box.appendChild(h);
			  if (typeof L === 'undefined') {
			    const p = document.createElement('div'); p.className = 'control'; p.textContent = 'The map needs an internet connection (Leaflet and the map tiles are loaded online). The grid is shown without the map.'; box.appendChild(p);
			    return;
			  }
			  const c1 = document.createElement('div'); c1.className = 'control check'; box.appendChild(c1);
			  const on = document.createElement('input'); on.type = 'checkbox'; on.id = 'm_on'; on.checked = true;
			  const l1 = document.createElement('label'); l1.htmlFor = 'm_on'; l1.textContent = 'Show the map';
			  c1.appendChild(on); c1.appendChild(l1);
			  on.addEventListener('change', function () {
			    mapState.on = on.checked;
			    document.getElementById('mapview').style.display = mapState.on ? 'block' : 'none';
			    render(); if (mapState.on && leafletMap) leafletMap.invalidateSize();
			  });
			  const c2 = document.createElement('div'); c2.className = 'control'; box.appendChild(c2);
			  const l2 = document.createElement('label'); l2.textContent = 'Base map'; c2.appendChild(l2);
			  const sel = document.createElement('select'); c2.appendChild(sel);
			  if (localPage) {
			    const note = document.createElement('div'); note.className = 'value'; note.style.cssText = 'font-size: 11px; color: var(--muted); margin-top: 6px; font-weight: normal;';
			    note.textContent = 'OpenStreetMap tiles need the page on a web server (http or https); a local file is blocked by the tile servers.';
			    c2.appendChild(note);
			  }
			  Object.keys(BASEMAPS).forEach(function (k) { const o = document.createElement('option'); o.value = k; o.textContent = BASEMAPS[k].label; sel.appendChild(o); });
			  sel.value = mapState.basemap;
			  sel.addEventListener('change', function () { mapState.basemap = sel.value; setBasemap(); });
			  const c3 = document.createElement('div'); c3.className = 'control'; box.appendChild(c3);
			  const l3 = document.createElement('label'); l3.textContent = CONFIG.map.bounds ? 'Opacity of the grid ' : 'Opacity of the points '; const val = document.createElement('span'); val.className = 'value'; val.textContent = Math.round(mapState.opacity * 100) + ' %'; l3.appendChild(val); c3.appendChild(l3);
			  const op = document.createElement('input'); op.type = 'range'; op.min = 0; op.max = 1; op.step = 0.05; op.value = mapState.opacity; c3.appendChild(op);
			  op.addEventListener('input', function () { mapState.opacity = parseFloat(op.value); val.textContent = Math.round(mapState.opacity * 100) + ' %'; if (gridOverlay) gridOverlay.setOpacity(mapState.opacity); drawGeoAgents(); });
			  const b = document.createElement('div'); b.className = 'control'; box.appendChild(b);
			  const fit = document.createElement('button'); fit.textContent = CONFIG.map.bounds ? 'Zoom to the grid' : 'Zoom to the area'; fit.style.width = '100%'; b.appendChild(fit);
			  fit.addEventListener('click', function () { if (leafletMap && mapBounds()) leafletMap.fitBounds(mapBounds(), { padding: [10, 10] }); });
			})();
			window.addEventListener('resize', function () { if (leafletMap) leafletMap.invalidateSize(); });

			/* ----- The 3D view (WebGL): lighting, backgrounds, shapes, shadows ----- */
			let gl = null, litProgram, lineProgram, pointProgram, shadowProgram, meshProgram, spriteProgram, buffers = {};
			let theta = -0.95, phi = 0.45, dist = 0;
			const viewbox = document.getElementById('viewbox');
			const BACKGROUNDS = {
			  studio: { label: 'Studio', css: 'radial-gradient(ellipse at 50% 30%, #ffffff 0%, #e6ebf2 55%, #c4ccd9 100%)', dark: false },
			  night: { label: 'Night', css: 'radial-gradient(ellipse at 50% 25%, #2c3958 0%, #121a2c 60%, #070a12 100%)', dark: true },
			  sky: { label: 'Sky', css: 'linear-gradient(180deg, #7fb6f2 0%, #cfe6fb 55%, #f4efe7 100%)', dark: false },
			  sunset: { label: 'Sunset', css: 'linear-gradient(180deg, #23194a 0%, #8f3f71 50%, #f29e5c 100%)', dark: true },
			  ocean: { label: 'Ocean', css: 'linear-gradient(180deg, #06263a 0%, #087e8b 62%, #a9d3e3 100%)', dark: true },
			  paper: { label: 'Paper', css: 'linear-gradient(180deg, #fbfaf7 0%, #efece5 100%)', dark: false },
			  plain: { label: 'Plain (theme color)', css: null, dark: null }
			};
			const LIGHTS = {
			  studio: { label: 'Studio', k: [0.45, 0.72, 0.35, 0.16] },
			  soft: { label: 'Soft', k: [0.66, 0.42, 0.12, 0.08] },
			  dramatic: { label: 'Dramatic', k: [0.20, 1.05, 0.60, 0.30] },
			  flat: { label: 'Flat (no shading)', k: [1.00, 0.00, 0.00, 0.00] }
			};
			const SHAPE_LABELS = { sphere: 'Spheres', cube: 'Cubes', cone: 'Cones (direction)', glider: 'Gliders (triangles, direction)', point: 'Points (fast)' };
			const FLOORS = { grid: 'Grid', plane: 'Plane', none: 'None' };
			const sceneKey = 'bio7-sim-scene:' + CONFIG.title;
			const scene = Object.assign({ background: 'auto', lighting: 'studio', shape: 'sphere', floor: 'grid', shadows: true, rotate: false }, CONFIG.scene || {});
			try { const stored = JSON.parse(localStorage.getItem(sceneKey) || 'null'); if (stored) Object.assign(scene, stored); } catch (e) { }
			function saveScene() { try { localStorage.setItem(sceneKey, JSON.stringify(scene)); } catch (e) { } }
			/* The shape, the floor and the shadows of the model (once, if the viewer has no own settings of the view) */
			let modelSceneApplied = false;
			function applyModelScene(shape, floor, shadows) {
			  if (modelSceneApplied || shape === undefined || shape === null) return;
			  modelSceneApplied = true;
			  let own = false; try { own = localStorage.getItem(sceneKey) !== null; } catch (e) { }
			  if (own) return;
			  if (shape && SHAPES[shape]) scene.shape = shape;
			  if (floor === 'plane' || floor === 'grid' || floor === 'none') scene.floor = floor;
			  if (shadows !== undefined && shadows !== null) scene.shadows = !!Number(shadows);
			  ['shape', 'floor', 'shadows'].forEach(function (k) {
			    const el = document.getElementById('s_' + k);
			    if (el) { if (el.type === 'checkbox') el.checked = !!scene[k]; else el.value = scene[k]; }
			  });
			}
			/* The objects of the environment (3D models placed by the model): xyz, models, headings, sizes, animations */
			function sceneryOf(names, values, animations) {
			  const list = function (v) { return v === undefined || v === null ? [] : typeof v === 'string' ? [v] : Array.from(v); };
			  const m = list(names), v = list(values).map(Number), a = list(animations), sc = { xyz: [], models: [], headings: [], sizes: [], animations: [], images: [] };
			  for (let k = 0; k < m.length && k * 5 + 4 < v.length; k++) {
			    sc.xyz.push(v[k * 5], v[k * 5 + 1], v[k * 5 + 2]); sc.headings.push(v[k * 5 + 3]); sc.sizes.push(v[k * 5 + 4]);
			    sc.models.push(m[k]); sc.animations.push(a[k] || null); sc.images.push(null);
			  }
			  return sc;
			}
			function themeDark() { return document.documentElement.getAttribute('data-theme') === 'dark'; }
			function sceneBackground() { let b = scene.background; if (b === 'auto') b = themeDark() ? 'night' : 'studio'; return BACKGROUNDS[b] ? b : 'studio'; }
			function sceneDark() { const b = BACKGROUNDS[sceneBackground()]; return b.dark === null ? themeDark() : b.dark; }

			/* The shapes (unit size, the direction of the movement is +x): x, y, z, nx, ny, nz per vertex */
			const SHAPES = (function () {
			  const sh = {};
			  function tri(out, a, b, c, n) {
			    if (!n) { const u = [b[0] - a[0], b[1] - a[1], b[2] - a[2]], v = [c[0] - a[0], c[1] - a[1], c[2] - a[2]]; n = [u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]]; const l = Math.hypot(n[0], n[1], n[2]) || 1; n = [n[0] / l, n[1] / l, n[2] / l]; }
			    [a, b, c].forEach(function (q) { out.push(q[0], q[1], q[2], n[0], n[1], n[2]); });
			  }
			  /* A sphere (latitude and longitude) with smooth normals */
			  const sp = [], la = 9, lo = 16;
			  function P(i, j) { const t = Math.PI * i / la, f = 2 * Math.PI * j / lo; return [Math.sin(t) * Math.cos(f), Math.sin(t) * Math.sin(f), Math.cos(t)]; }
			  for (let i = 0; i < la; i++) for (let j = 0; j < lo; j++) {
			    const a = P(i, j), b = P(i + 1, j), c = P(i + 1, j + 1), d = P(i, j + 1);
			    [a, b, c, a, c, d].forEach(function (q) { sp.push(q[0], q[1], q[2], q[0], q[1], q[2]); });
			  }
			  sh.sphere = sp;
			  /* A cube */
			  const cu = [], k = 0.8;
			  const faces = [[[1, 0, 0], [0, 1, 0], [0, 0, 1]], [[-1, 0, 0], [0, 0, 1], [0, 1, 0]], [[0, 1, 0], [0, 0, 1], [1, 0, 0]], [[0, -1, 0], [1, 0, 0], [0, 0, 1]], [[0, 0, 1], [1, 0, 0], [0, 1, 0]], [[0, 0, -1], [0, 1, 0], [1, 0, 0]]];
			  faces.forEach(function (f) {
			    const n = f[0], u = f[1], v = f[2], q = function (a, b) { return [k * (n[0] + a * u[0] + b * v[0]), k * (n[1] + a * u[1] + b * v[1]), k * (n[2] + a * u[2] + b * v[2])]; };
			    tri(cu, q(-1, -1), q(1, -1), q(1, 1), n); tri(cu, q(-1, -1), q(1, 1), q(-1, 1), n);
			  });
			  sh.cube = cu;
			  /* A cone along the direction */
			  const co = [], seg = 14, tip = [1.3, 0, 0];
			  for (let j = 0; j < seg; j++) {
			    const f1 = 2 * Math.PI * j / seg, f2 = 2 * Math.PI * (j + 1) / seg;
			    const a = [-0.7, 0.65 * Math.cos(f1), 0.65 * Math.sin(f1)], b = [-0.7, 0.65 * Math.cos(f2), 0.65 * Math.sin(f2)];
			    const na = [0.45, Math.cos(f1), Math.sin(f1)], nb = [0.45, Math.cos(f2), Math.sin(f2)];
			    co.push(tip[0], tip[1], tip[2], (na[0] + nb[0]) / 2, (na[1] + nb[1]) / 2, (na[2] + nb[2]) / 2);
			    co.push(b[0], b[1], b[2], nb[0], nb[1], nb[2]); co.push(a[0], a[1], a[2], na[0], na[1], na[2]);
			    tri(co, [-0.7, 0, 0], a, b, [-1, 0, 0]);
			  }
			  sh.cone = co;
			  /* A glider of triangles: two wings and a fin */
			  const gd = [], N = [1.4, 0, 0], L = [-0.9, 1.0, -0.05], R = [-0.9, -1.0, -0.05], T = [-0.45, 0, 0.05], F = [-0.85, 0, 0.6], B = [0.1, 0, 0.05];
			  tri(gd, N, L, T); tri(gd, N, T, R); tri(gd, B, F, T, [0, 1, 0]); tri(gd, T, F, [-0.9, 0, 0.05], [0, 1, 0]);
			  sh.glider = gd;
			  return sh;
			})();


			/*GLTF*/
			function init3d() {
			  gl = view3d.getContext('webgl', { antialias: true, alpha: true, premultipliedAlpha: false });
			  if (!gl) { report('WebGL is not available in this browser'); return; }
			  function shader(type, src) { const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s); if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw gl.getShaderInfoLog(s); return s; }
			  function program(vs, fs) { const p = gl.createProgram(); gl.attachShader(p, shader(gl.VERTEX_SHADER, vs)); gl.attachShader(p, shader(gl.FRAGMENT_SHADER, fs)); gl.linkProgram(p); return p; }
			  /* Lighting: a key light, the sky and the ground (ambient), highlights and a rim light */
			  litProgram = program(
			    'attribute vec3 p; attribute vec3 n; attribute vec3 c; uniform mat4 mvp; varying vec3 vp; varying vec3 vn; varying vec3 vc;' +
			    'void main() { gl_Position = mvp * vec4(p, 1.0); vp = p; vn = n; vc = c; }',
			    'precision mediump float; varying vec3 vp; varying vec3 vn; varying vec3 vc;' +
			    'uniform vec3 eye; uniform vec3 light; uniform vec4 k; uniform vec3 sky; uniform vec3 ground;' +
			    'void main() { vec3 n = normalize(vn); vec3 v = normalize(eye - vp); if (dot(n, v) < 0.0) n = -n; vec3 l = normalize(light);' +
			    ' vec3 amb = mix(ground, sky, 0.5 + 0.5 * n.z) * k.x; float d = max(dot(n, l), 0.0) * k.y;' +
			    ' float s = pow(max(dot(n, normalize(l + v)), 0.0), 48.0) * k.z; float rim = pow(1.0 - max(dot(n, v), 0.0), 3.0) * k.w;' +
			    ' gl_FragColor = vec4(min(vc * (amb + d) + vec3(s) + rim * sky * 0.6, 1.0), 1.0); }');
			  lineProgram = program(
			    'attribute vec3 p; attribute vec3 c; uniform mat4 mvp; varying vec3 vc; void main() { gl_Position = mvp * vec4(p, 1.0); vc = c; }',
			    'precision mediump float; varying vec3 vc; uniform float alpha; void main() { gl_FragColor = vec4(vc, alpha); }');
			  pointProgram = program(
			    'attribute vec3 p; attribute vec3 c; uniform mat4 mvp; uniform float ps; uniform float radius; varying vec3 vc;' +
			    'void main() { gl_Position = mvp * vec4(p, 1.0); gl_PointSize = max(2.0, 2.0 * radius * ps / gl_Position.w); vc = c; }',
			    'precision mediump float; varying vec3 vc;' +
			    'void main() { vec2 q = gl_PointCoord * 2.0 - 1.0; float d = dot(q, q); if (d > 1.0) discard; float z = sqrt(1.0 - d);' +
			    ' gl_FragColor = vec4(min(vc * (0.3 + 0.6 * z) + 0.25 * pow(z, 12.0), 1.0), 1.0); }');
			  /* Soft shadows: a dark disc below each agent */
			  shadowProgram = program(
			    'attribute vec3 p; attribute vec3 q; uniform mat4 mvp; varying vec3 vq; void main() { gl_Position = mvp * vec4(p, 1.0); vq = q; }',
			    'precision mediump float; varying vec3 vq; uniform vec3 color; void main() { float d = length(vq.xy); if (d > 1.0) discard; gl_FragColor = vec4(color, vq.z * (1.0 - smoothstep(0.25, 1.0, d))); }');
			  /* The glTF models (the color or the texture of the material, lighted as the shapes) and the images (billboards) */
			  meshProgram = program(
			    'attribute vec3 p; attribute vec3 n; attribute vec2 uv; uniform mat4 mvp; uniform mat4 model; varying vec3 vp; varying vec3 vn; varying vec2 vuv;' +
			    'void main() { vec4 w = model * vec4(p, 1.0); gl_Position = mvp * w; vp = w.xyz; vn = (model * vec4(n, 0.0)).xyz; vuv = uv; }',
			    'precision mediump float; varying vec3 vp; varying vec3 vn; varying vec2 vuv; uniform vec4 color; uniform sampler2D tex; uniform float useTex;' +
			    'uniform vec3 eye; uniform vec3 light; uniform vec4 k; uniform vec3 sky; uniform vec3 ground;' +
			    'void main() { vec3 base = color.rgb; if (useTex > 0.5) base *= texture2D(tex, vuv).rgb; vec3 n = normalize(vn); vec3 v = normalize(eye - vp); if (dot(n, v) < 0.0) n = -n; vec3 l = normalize(light);' +
			    ' vec3 amb = mix(ground, sky, 0.5 + 0.5 * n.z) * k.x; float d = max(dot(n, l), 0.0) * k.y; float s = pow(max(dot(n, normalize(l + v)), 0.0), 48.0) * k.z * 0.4;' +
			    ' gl_FragColor = vec4(min(base * (amb + d) + vec3(s), 1.0), 1.0); }');
			  spriteProgram = program(
			    'attribute vec3 p; attribute vec2 uv; uniform mat4 mvp; varying vec2 vuv; void main() { gl_Position = mvp * vec4(p, 1.0); vuv = uv; }',
			    'precision mediump float; varying vec2 vuv; uniform sampler2D tex; void main() { vec4 c = texture2D(tex, vuv); if (c.a < 0.1) discard; gl_FragColor = c; }');
			  ['tri', 'line', 'point', 'frame', 'shadow', 'shape', 'floor', 'mp', 'mn', 'mu', 'mi', 'sprite', 'fill'].forEach(function (k) { buffers[k] = gl.createBuffer(); });
			  let drag = null;
			  view3d.addEventListener('pointerdown', function (ev) { drag = [ev.clientX, ev.clientY]; });
			  window.addEventListener('pointerup', function () { drag = null; });
			  window.addEventListener('pointermove', function (ev) {
			    if (!drag) return; theta -= (ev.clientX - drag[0]) * 0.01; phi = Math.max(-1.55, Math.min(1.55, phi + (ev.clientY - drag[1]) * 0.01));
			    drag = [ev.clientX, ev.clientY]; draw3d();
			  });
			  view3d.addEventListener('wheel', function (ev) { ev.preventDefault(); dist *= ev.deltaY > 0 ? 1.1 : 1 / 1.1; draw3d(); }, { passive: false });
			  view3d.addEventListener('dblclick', function () { theta = -0.95; phi = 0.45; dist = 0; draw3d(); });
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
			function useProgram(prog) { gl.useProgram(prog); for (let i = 0; i < 8; i++) gl.disableVertexAttribArray(i); }
			function attribute(prog, name, size, stride, offset) { const loc = gl.getAttribLocation(prog, name); if (loc < 0) return; gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, size, gl.FLOAT, false, stride * 4, offset * 4); }
			function color3(c, fallback) { const h = hexColor(c || fallback); return [h[0] / 255, h[1] / 255, h[2] / 255]; }
			/* The directions of the points (given or from the last positions) */
			let lastPoints = null, lastDirs = null, lastStep = -1;
			function pointDirections(p) {
			  if (p.dirs && p.dirs.length === p.xyz.length) return p.dirs;
			  const n = p.xyz.length;
			  if (sim.t !== lastStep) {
			    const dirs = new Array(n);
			    for (let i = 0; i < n; i += 3) {
			      let dx = 0, dy = 0, dz = 0;
			      if (lastPoints && lastPoints.length === n) { dx = p.xyz[i] - lastPoints[i]; dy = p.xyz[i + 1] - lastPoints[i + 1]; dz = p.xyz[i + 2] - lastPoints[i + 2]; }
			      if (dx * dx + dy * dy + dz * dz < 1e-18 && lastDirs && lastDirs.length === n) { dx = lastDirs[i]; dy = lastDirs[i + 1]; dz = lastDirs[i + 2]; }
			      dirs[i] = dx; dirs[i + 1] = dy; dirs[i + 2] = dz;
			    }
			    lastPoints = Array.from(p.xyz); lastDirs = dirs; lastStep = sim.t;
			  }
			  return lastDirs && lastDirs.length === n ? lastDirs : null;
			}
			/*DRAW3D*/
			/*MODELS3D*/
			/* Auto-rotation of the camera */
			let rotating = false;
			function rotateLoop() {
			  if (!scene.rotate || CONFIG.view === 'GRID_2D') { rotating = false; return; }
			  theta += 0.004; draw3d(); requestAnimationFrame(rotateLoop);
			}
			function startRotation() { if (scene.rotate && !rotating && CONFIG.view !== 'GRID_2D') { rotating = true; requestAnimationFrame(rotateLoop); } }
			/* The settings of the 3D view in the page */
			(function () {
			  const box = document.getElementById('scenebox');
			  if (CONFIG.view === 'GRID_2D' || CONFIG.sceneControls === false) return;
			  box.style.display = '';
			  const h = document.createElement('h2'); h.textContent = 'View'; box.appendChild(h);
			  function select(label, key, options) {
			    const div = document.createElement('div'); div.className = 'control'; box.appendChild(div);
			    const l = document.createElement('label'); l.textContent = label; div.appendChild(l);
			    const s = document.createElement('select'); s.id = 's_' + key; div.appendChild(s);
			    Object.keys(options).forEach(function (k) { const o = document.createElement('option'); o.value = k; o.textContent = options[k]; s.appendChild(o); });
			    s.value = scene[key];
			    s.addEventListener('change', function () { scene[key] = s.value; saveScene(); draw3d(); });
			  }
			  function check(label, key, after) {
			    const div = document.createElement('div'); div.className = 'control check'; box.appendChild(div);
			    const input = document.createElement('input'); input.type = 'checkbox'; input.id = 's_' + key; input.checked = !!scene[key];
			    const l = document.createElement('label'); l.htmlFor = input.id; l.textContent = label;
			    div.appendChild(input); div.appendChild(l);
			    input.addEventListener('change', function () { scene[key] = input.checked; saveScene(); draw3d(); if (after) after(); });
			  }
			  const bgs = { auto: 'Automatic (theme)' }; Object.keys(BACKGROUNDS).forEach(function (k) { bgs[k] = BACKGROUNDS[k].label; });
			  select('Background', 'background', bgs);
			  const ls = {}; Object.keys(LIGHTS).forEach(function (k) { ls[k] = LIGHTS[k].label; });
			  select('Lighting', 'lighting', ls);
			  if (CONFIG.view === 'SCENE_3D') select('Shape of the points', 'shape', SHAPE_LABELS);
			  select('Floor', 'floor', FLOORS);
			  if (CONFIG.view === 'SCENE_3D') check('Shadows', 'shadows');
			  check('Rotate the view', 'rotate', startRotation);
			})();
			startRotation();

			/* ----- The chart ----- */
			const chartBox = document.getElementById('chartbox'), chartCanvas = document.getElementById('chart'), moreCanvases = new Map();
			if (CONFIG.chart !== null) chartBox.style.display = '';
			else chartCanvas.parentNode.style.display = 'none';
			function drawChart() {
			  if (CONFIG.chart !== null) drawOneChart(chartCanvas, CONFIG.chart.replace(/<[^>]*>/g, ''), 'line', series);
			  moreCharts.forEach(function (c, title) {
			    let canvas = moreCanvases.get(title);
			    if (!canvas) {
			      const cell = document.createElement('div'); cell.className = 'chartcell'; canvas = document.createElement('canvas'); cell.appendChild(canvas); chartBox.appendChild(cell);
			      moreCanvases.set(title, canvas); chartBox.style.display = '';
			    }
			    drawOneChart(canvas, title, c.kind, c.series, c);
			  });
			}
			/*CHARTS*/
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
			    if (CONFIG.view === 'GRID_2D') { view.style.display = ''; view3d.style.display = 'none'; viewbox.style.background = ''; if (!mapActive()) document.getElementById('mapview').style.display = 'none'; draw2d(); if (sim.geoAgents) drawGeoAgents(); }
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
