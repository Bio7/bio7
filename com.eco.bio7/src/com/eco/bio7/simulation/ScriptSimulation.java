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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.widgets.Display;

import com.eco.bio7.methods.Compiled;
import com.eco.bio7.pyffm.PyValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simulation models written in Python or R (the modules bio7sim of Python and
 * R). The script calls simulation(model) which starts a Java model of the same
 * kind (a {@link GridModel}, {@link ValueGridModel}, {@link AgentModel},
 * {@link GeoGridModel} or {@link GeoAgentModel}): its init() and step() run
 * the functions of the script in the embedded Python or R and display the
 * state of the script with the displays of the Java models (ImageJ, the 3D
 * view, the WorldWind globe). The settings and the buttons of the script are
 * shown in the Settings view; the HTML export runs the script in the web
 * browser (Pyodide or webR).
 */
public final class ScriptSimulation {

	private ScriptSimulation() {
	}

	/** The languages of the script models. */
	public static final String PYTHON = "python", R = "r";

	/* ===================== The model of a script ===================== */

	/** The description of a script model (sent by simulation(model) of the script). */
	public static final class Spec {
		public String name, kind, language, description = "", source;
		public final List<JavaToJs.ParamInfo> params = new ArrayList<>();
		public final List<JavaToJs.ActionInfo> actions = new ArrayList<>();
	}

	/** The values of the state of the script (after the setup, a step or an action). */
	interface State {
		boolean has(String key);

		double number(String key, double otherwise);

		double[] doubles(String key);

		int[] ints(String key);

		String[] strings(String key);
	}

	/** The interpreter of the script. */
	interface Engine {
		/* The protocol of the modules: op is setup, step, action, set or load */
		State run(String op, String arg) throws Exception;
	}

	/** A Java model of a script (the proxies of the kinds). */
	public interface Host {
		/** The script of the model. */
		Script script();
	}

	/** The script of a model: the description, the interpreter and a thread for the calls of the user interface. */
	public static final class Script {
		final Spec spec;
		final Engine engine;
		private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "Bio7 simulation script");
			t.setDaemon(true);
			return t;
		});

		Script(Spec spec, Engine engine) {
			this.spec = spec;
			this.engine = engine;
		}

		/** The description of the model. */
		public Spec spec() {
			return spec;
		}

		/** The source file of the script (null if unknown). */
		public File sourceFile() {
			return spec.source == null || spec.source.isBlank() ? null : new File(spec.source);
		}

		/* Runs the code in the thread of the script (not in the thread of the user interface: Python and R may wait for it) */
		void later(Runnable code) {
			worker.execute(() -> {
				try {
					code.run();
				} catch (RuntimeException e) {
					report(e);
				}
			});
		}

		State run(String op, String arg) {
			State st;
			try {
				st = engine.run(op, arg == null ? "" : arg);
			} catch (Exception e) {
				throw new ScriptException(e);
			}
			/* The input functions of the model (click, drag, key) */
			if (st != null && st.has("inputs")) {
				inputs = java.util.Set.of(st.strings("inputs"));
			}
			return st;
		}

		/* The input functions which the model defines (click, drag, key) */
		private volatile java.util.Set<String> inputs = java.util.Set.of();

		/** The input functions of the model: "click", "drag", "key". */
		public java.util.Set<String> inputs() {
			return inputs;
		}

		/* An input of a view (in the thread of the script, as an action): the argument when it runs (a merged drag) */
		void input(Simulation model, java.util.function.Supplier<String> arg) {
			later(() -> {
				synchronized (model) {
					State st = run("input", arg.get());
					((Applier) model).apply(st);
					charts(model, st);
				}
				model.display();
			});
		}

		/** Sets a setting (in the thread of the script), then the setup (restart) or the display. */
		public void set(Simulation model, JavaToJs.ParamInfo p, Object value) {
			p.value = value;
			String text = String.valueOf(value);
			later(() -> {
				synchronized (model) {
					run("set", PYTHON.equals(spec.language) ? "{" + JavaToJs.quote(p.name) + ": " + json(p, value) + "}" : p.name + "\u001f" + text);
				}
				if (p.restart) {
					model.setup();
				} else {
					model.display();
				}
			});
		}

		/** Calls an action of the script (in the thread of the script) and displays the model. */
		public void action(Simulation model, String name) {
			later(() -> {
				synchronized (model) {
					State st = run("action", name);
					((Applier) model).apply(st);
					charts(model, st);
				}
				model.display();
			});
		}

		/** The settings and actions in their groups (the actions without a group at the end). */
		public Map<String, List<Object>> groups() {
			Map<String, List<Object>> groups = new LinkedHashMap<>();
			for (JavaToJs.ParamInfo p : spec.params) {
				groups.computeIfAbsent(p.group, k -> new ArrayList<>()).add(p);
			}
			for (JavaToJs.ActionInfo a : spec.actions) {
				groups.computeIfAbsent(a.group.isEmpty() ? "\u0000" : a.group, k -> new ArrayList<>()).add(a);
			}
			return groups;
		}

		private static String json(JavaToJs.ParamInfo p, Object value) {
			return switch (p.kind) {
			case "boolean" -> Boolean.TRUE.equals(value) ? "true" : "false";
			case "int" -> String.valueOf(Math.round(((Number) value).doubleValue()));
			case "double" -> String.valueOf(((Number) value).doubleValue());
			default -> JavaToJs.quote(String.valueOf(value));
			};
		}
	}

	/** An error of the script (the message of Python or R). */
	static final class ScriptException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		ScriptException(Exception cause) {
			super(cause.getMessage(), cause);
		}
	}

	/* The model applies the state of the script */
	interface Applier {
		void apply(State s);
	}

	/* Prints the error of the script and pauses the simulation */
	static void report(Throwable e) {
		Throwable c = e instanceof ScriptException && e.getCause() != null ? e.getCause() : e;
		System.err.println("The simulation script failed: " + c.getMessage());
		try {
			com.eco.bio7.time.Time.setPause(true);
		} catch (RuntimeException | LinkageError ex) {
			/* No time thread (a test) */
		}
	}

	private static final ObjectMapper JSON = new ObjectMapper();

	/* The charts and the drawing of the state */
	static void charts(Simulation model, State s) {
		String[] names = s.strings("chart_names");
		double[] values = s.doubles("chart_values");
		for (int i = 0; i < Math.min(names.length, values.length); i++) {
			model.chart(names[i], values[i]);
		}
		try {
			String more = text(s, "more_charts");
			if (more != null) {
				for (JsonNode c : JSON.readTree(more)) {
					String title = c.path("t").asText(), kind = c.path("k").asText();
					switch (kind) {
					case "line" -> model.chart(title, c.path("n").asText(), c.path("y").asDouble());
					case "xy" -> model.chartXY(title, c.path("n").asText(), c.path("x").asDouble(), c.path("y").asDouble());
					case "bars" -> {
						List<String> labels = new ArrayList<>();
						List<Double> vals = new ArrayList<>();
						for (JsonNode l : list(c.path("l"))) {
							labels.add(l.asText());
						}
						for (JsonNode v : list(c.path("v"))) {
							vals.add(v.asDouble());
						}
						model.chartBars(title, labels.toArray(new String[0]), vals.stream().mapToDouble(Double::doubleValue).toArray());
					}
					default -> {
					}
					}
				}
			}
			String draw = text(s, "draw");
			if (draw != null) {
				/* The drawing of the script replaces the drawing of the model (an action adds to the shapes of the step) */
				model.clearDrawing();
				for (JsonNode d : JSON.readTree(draw)) {
					List<Double> p = new ArrayList<>();
					for (JsonNode v : list(d.path("p"))) {
						p.add(v.asDouble());
					}
					model.addShape(new Shape(d.path("k").asText(), p.stream().mapToDouble(Double::doubleValue).toArray(), nullable(d.get("s")), nullable(d.get("f")), nullable(d.get("t")),
							d.path("w").asDouble(1), d.path("a").asDouble(0), d.path("z").asDouble(0)));
				}
			}
		} catch (IOException e) {
			System.err.println("The charts or the drawing of the script could not be read: " + e.getMessage());
		}
	}

	private static String text(State s, String key) {
		String[] t = s.strings(key);
		return t.length == 0 ? null : t[0];
	}

	/* A JSON value or array as a list (R writes a vector of length 1 as a value) */
	private static Iterable<JsonNode> list(JsonNode n) {
		if (n == null || n.isMissingNode() || n.isNull()) {
			return List.of();
		}
		return n.isArray() ? n : List.of(n);
	}

	private static String nullable(JsonNode n) {
		return n == null || n.isNull() ? null : n.asText();
	}

	/* A path of the script: relative to the folder of the script (up to the project), else as the model */
	static String resolveScript(Script script, String file, java.util.function.UnaryOperator<String> otherwise) {
		File f = new File(file);
		File source = script == null ? null : script.sourceFile();
		if (!f.isAbsolute() && source != null) {
			for (File dir = source.getParentFile(); dir != null; dir = dir.getParentFile()) {
				File candidate = new File(dir, file);
				if (candidate.exists()) {
					return candidate.getPath();
				}
				if (new File(dir, ".project").exists()) {
					break;
				}
			}
		}
		return otherwise.apply(file);
	}

	/* The script draws charts (chart, chart_xy, chart_bars): the Charts view at the start */
	static boolean scriptCharts(Script script) {
		File f = script == null ? null : script.sourceFile();
		if (f == null || !f.exists()) {
			return false;
		}
		try {
			return java.util.regex.Pattern.compile("[.$]chart(_xy|_bars)?\\s*\\(").matcher(java.nio.file.Files.readString(f.toPath())).find();
		} catch (IOException e) {
			return false;
		}
	}

	/* The model of the last started script (the images and models of load_image, load_model) */
	private static volatile Simulation current;

	/* The agents fields of the state (images, 3D models, animations, headings, sizes) */
	private static String[] strings(State s, String key, int n) {
		String[] v = colors(s.strings(key));
		return v.length == n ? v : new String[n];
	}

	/* ===================== The models (one per kind) ===================== */

	/* The script of the model which is created (read by the constructors and by modelName() during the construction) */
	private static Script creating;

	/* The life cycle of the proxies: the setup in the thread of the script, errors are reported */
	private static void initScript(Simulation model, Script script) {
		try {
			/* The seed of the settings (the field Seed) */
			State s = script.run("setup", Integer.toString(model.getStartSeed()));
			((Applier) model).apply(s);
			charts(model, s);
			if (s.number("stopped", 0) != 0) {
				model.stop();
			}
		} catch (RuntimeException e) {
			report(e);
		}
	}

	private static void stepScript(Simulation model, Script script) {
		try {
			State s = script.run("step", "");
			((Applier) model).apply(s);
			charts(model, s);
			/* stop() of the script: the simulation pauses */
			if (s.number("stopped", 0) != 0) {
				model.stop();
			}
		} catch (RuntimeException e) {
			report(e);
		}
	}

	/* Runs the code in the thread of the user interface (and waits) */
	static void inUi(Runnable code) {
		if (Display.getCurrent() != null || !Simulation.hasDisplay()) {
			code.run();
		} else {
			org.eclipse.ui.PlatformUI.getWorkbench().getDisplay().syncExec(code);
		}
	}

	private static String[] colors(String[] c) {
		String[] out = new String[c.length];
		for (int i = 0; i < c.length; i++) {
			out[i] = c[i] == null || c[i].isEmpty() ? null : c[i];
		}
		return out;
	}

	/** A grid with integer states of a script. */
	public static final class Grid extends GridModel implements Host, Applier {
		private final Script script = creating;

		@Override
		boolean usesCharts() {
			return scriptCharts(script);
		}

		@Override
		String resolve(String file) {
			return resolveScript(script, file, super::resolve);
		}

		@Override
		public Script script() {
			return script;
		}

		@Override
		String modelName() {
			return script != null ? script.spec.name : creating.spec.name;
		}

		@Override
		public void setup() {
			if (Display.getCurrent() != null) {
				script.later(super::setup);
			} else {
				super.setup();
			}
		}

		@Override
		public void init() {
			initScript(this, script);
		}

		@Override
		public void step() {
			stepScript(this, script);
		}

		@Override
		public void apply(State s) {
			int w = (int) s.number("width", width), h = (int) s.number("height", height);
			if (w != width || h != height) {
				size(w, h);
			}
			int[] c = s.ints("cells");
			if (c.length == w * h) {
				cells = c;
			}
			String[] p = s.strings("palette");
			if (p.length > 0) {
				palette(p);
			}
		}
	}

	/** A grid with double values of a script. */
	public static final class Values extends ValueGridModel implements Host, Applier {
		private final Script script = creating;

		@Override
		boolean usesCharts() {
			return scriptCharts(script);
		}

		@Override
		String resolve(String file) {
			return resolveScript(script, file, super::resolve);
		}

		@Override
		public Script script() {
			return script;
		}

		@Override
		String modelName() {
			return script != null ? script.spec.name : creating.spec.name;
		}

		@Override
		public void setup() {
			if (Display.getCurrent() != null) {
				script.later(super::setup);
			} else {
				super.setup();
			}
		}

		@Override
		public void init() {
			initScript(this, script);
		}

		@Override
		public void step() {
			stepScript(this, script);
		}

		@Override
		public void apply(State s) {
			int w = (int) s.number("width", width), h = (int) s.number("height", height);
			if (w != width || h != height) {
				size(w, h);
			}
			double[] v = s.doubles("values");
			if (v.length == w * h) {
				values = v;
			}
			range(s.number("min", 0), s.number("max", 1));
			String[] c = s.strings("colors");
			if (c.length >= 2) {
				colorMap(c);
			}
		}
	}

	/** Agents in a 3D box of a script. */
	public static final class Agents extends AgentModel<Agent> implements Host, Applier {
		private final Script script = creating;

		@Override
		boolean usesCharts() {
			return scriptCharts(script);
		}

		@Override
		String resolve(String file) {
			return resolveScript(script, file, super::resolve);
		}

		@Override
		public Script script() {
			return script;
		}

		@Override
		String modelName() {
			return script != null ? script.spec.name : creating.spec.name;
		}

		@Override
		public void setup() {
			if (Display.getCurrent() != null) {
				script.later(super::setup);
			} else {
				super.setup();
			}
		}

		@Override
		protected void prepareDisplay() {
			inUi(super::prepareDisplay);
		}

		@Override
		public void init() {
			initScript(this, script);
		}

		@Override
		public void step() {
			stepScript(this, script);
		}

		@Override
		public void apply(State s) {
			double[] b = s.doubles("box");
			if (b.length == 3) {
				box(b[0], b[1], b[2]);
			}
			agentRadius = s.number("radius", agentRadius);
			String[] color = s.strings("color");
			if (color.length > 0) {
				agentColor = color[0];
			}
			/* The shape of the agents, the floor, the shadows and the agent of the follow camera (-1: none) */
			String[] shape = s.strings("shape");
			if (shape.length > 0 && shape[0] != null && !shape[0].isEmpty()) {
				agentShape = shape[0];
			}
			String[] fl = s.strings("floor");
			if (fl.length > 0 && fl[0] != null && !fl[0].isEmpty()) {
				floor = fl[0];
			}
			shadows = s.number("shadows", shadows ? 1 : 0) != 0;
			followAgent = (int) s.number("follow", followAgent);
			/* The objects of the environment (all of them in each state): x, y, z, heading and size for each model */
			String[] placedModels = s.strings("scenery_models");
			double[] placed = s.doubles("scenery");
			String[] placedAnimations = strings(s, "scenery_animations", placedModels.length);
			List<Placed> scenery = new ArrayList<>();
			for (int k = 0; k < placedModels.length && k * 5 + 4 < placed.length; k++) {
				scenery.add(new Placed(placedModels[k], placed[k * 5], placed[k * 5 + 1], placed[k * 5 + 2], placed[k * 5 + 3], placed[k * 5 + 4], placedAnimations[k]));
			}
			setModels(scenery);
			double[] xyz = s.doubles("xyz");
			String[] c = colors(s.strings("colors"));
			int n = xyz.length / 3;
			String[] images = strings(s, "images", n), models = strings(s, "models", n), animations = strings(s, "animations", n);
			double[] headings = s.doubles("headings"), sizes = s.doubles("sizes");
			ArrayList<Agent> list = new ArrayList<>(agents);
			while (list.size() > n) {
				list.remove(list.size() - 1);
			}
			while (list.size() < n) {
				list.add(new Agent());
			}
			for (int i = 0; i < n; i++) {
				Agent a = list.get(i);
				a.x = xyz[i * 3];
				a.y = xyz[i * 3 + 1];
				a.z = xyz[i * 3 + 2];
				a.color = i < c.length ? c[i] : null;
				a.image = images[i];
				a.model = models[i];
				a.animation = animations[i];
				a.heading = i < headings.length ? headings[i] : Double.NaN;
				a.size = i < sizes.length ? sizes[i] : 0;
			}
			agents = list;
			/* The links of the network (pairs of the indices from 0), their colors ("": linkColor) */
			clearLinks();
			double[] pairs = s.doubles("links");
			String[] lc = strings(s, "link_colors", pairs.length / 2);
			for (int k = 0; k + 1 < pairs.length; k += 2) {
				int i = (int) pairs[k], j = (int) pairs[k + 1];
				if (i >= 0 && j >= 0 && i < n && j < n) {
					link(list.get(i), list.get(j), lc[k / 2]);
				}
			}
			String[] linkC = s.strings("link_color");
			if (linkC.length > 0) {
				linkColor = linkC[0];
			}
			linkWidth = s.number("link_width", linkWidth);
		}
	}

	/** A geospatial grid of a script (the data is loaded by load() of the script with the GDAL of Bio7). */
	public static final class GeoGrid extends GeoGridModel implements Host, Applier {
		private final Script script = creating;

		@Override
		boolean usesCharts() {
			return scriptCharts(script);
		}

		@Override
		public Script script() {
			return script;
		}

		@Override
		String modelName() {
			return script != null ? script.spec.name : creating.spec.name;
		}

		@Override
		public void setup() {
			if (Display.getCurrent() != null) {
				script.later(super::setup);
			} else {
				super.setup();
			}
		}

		@Override
		protected void load() {
			loading = this;
			try {
				script.run("load", "");
			} finally {
				loading = null;
			}
		}

		@Override
		public void init() {
			initScript(this, script);
		}

		@Override
		public void step() {
			stepScript(this, script);
		}

		@Override
		String resolve(String file) {
			return resolveScript(script, file, super::resolve);
		}

		@Override
		public void apply(State s) {
			int[] c = s.ints("cells");
			if (c.length == width * height) {
				cells = c;
			}
			String[] p = s.strings("palette");
			if (p.length > 0) {
				palette(p);
			}
			if (s.has("shown")) {
				String[] colors = s.strings("colors");
				showValues(s.doubles("shown"), s.number("min", 0), s.number("max", 1), colors.length >= 2 ? colors : VIRIDIS_COLORS);
			} else {
				showStates();
			}
		}

		/* The geometry of the grid for the script */
		PyValue geometry() {
			Map<String, PyValue> g = new LinkedHashMap<>();
			g.put("width", PyValue.of(width));
			g.put("height", PyValue.of(height));
			g.put("originX", PyValue.of(originX));
			g.put("originY", PyValue.of(originY));
			g.put("cellWidth", PyValue.of(cellWidth));
			g.put("cellHeight", PyValue.of(cellHeight));
			g.put("cellSizeMeters", PyValue.of(cellSizeMeters));
			g.put("south", PyValue.of(south));
			g.put("west", PyValue.of(west));
			g.put("north", PyValue.of(north));
			g.put("east", PyValue.of(east));
			g.put("mode", new PyValue.Str(new String[] { mode() }));
			g.put("crs", new PyValue.Str(new String[] { crs == null ? "" : crs }));
			return new PyValue.PyDict(g);
		}
	}

	private static final String[] VIRIDIS_COLORS = ValueGridModel.VIRIDIS;

	/* The geospatial grid whose load() runs (the functions of load() of the script) */
	private static volatile GeoGrid loading;

	/** Agents on the Earth of a script. */
	public static final class GeoAgents extends GeoAgentModel<GeoAgent> implements Host, Applier {
		private final Script script = creating;

		@Override
		boolean usesCharts() {
			return scriptCharts(script);
		}

		@Override
		String resolve(String file) {
			return resolveScript(script, file, super::resolve);
		}

		@Override
		public Script script() {
			return script;
		}

		@Override
		String modelName() {
			return script != null ? script.spec.name : creating.spec.name;
		}

		@Override
		public void setup() {
			if (Display.getCurrent() != null) {
				script.later(super::setup);
			} else {
				super.setup();
			}
		}

		@Override
		protected void prepareDisplay() {
			inUi(super::prepareDisplay);
		}

		@Override
		public void init() {
			initScript(this, script);
		}

		@Override
		public void step() {
			stepScript(this, script);
		}

		@Override
		public void apply(State s) {
			double[] area = s.doubles("area");
			if (area.length == 4) {
				area(area[0], area[1], area[2], area[3]);
			}
			agentSize = s.number("size", agentSize);
			String[] color = s.strings("color"), shape = s.strings("shape");
			if (color.length > 0) {
				agentColor = color[0];
			}
			if (shape.length > 0) {
				agentShape = shape[0];
			}
			agentSizeMeters = s.number("meters", agentSizeMeters);
			viewPitch = s.number("pitch", viewPitch);
			double[] lon = s.doubles("lon"), lat = s.doubles("lat"), alt = s.doubles("altitude"), heading = s.doubles("heading"), sizes = s.doubles("sizes");
			String[] colors = colors(s.strings("colors")), labels = colors(s.strings("labels"));
			int n = Math.min(lon.length, lat.length);
			String[] images = strings(s, "images", n), models = strings(s, "models", n), animations = strings(s, "animations", n);
			ArrayList<GeoAgent> list = new ArrayList<>(agents);
			while (list.size() > n) {
				list.remove(list.size() - 1);
			}
			while (list.size() < n) {
				list.add(new GeoAgent());
			}
			for (int i = 0; i < n; i++) {
				GeoAgent a = list.get(i);
				a.lon = lon[i];
				a.lat = lat[i];
				a.altitude = i < alt.length ? alt[i] : 0;
				a.heading = i < heading.length ? heading[i] : Double.NaN;
				a.size = i < sizes.length ? sizes[i] : 0;
				a.color = i < colors.length ? colors[i] : null;
				a.label = i < labels.length ? labels[i] : null;
				a.image = images[i];
				a.model = models[i];
				a.animation = animations[i];
			}
			agents = list;
		}
	}

	/* ===================== The functions for Python and R (bio7.call, .bio7_call) ===================== */

	/**
	 * The functions of the modules bio7sim (called by Python and R): sim_start
	 * (the description of the model as JSON) starts the model, sim_geo the
	 * functions of load() of a geospatial model.
	 */
	public static PyValue call(String function, String[] args, PyValue value) throws Exception {
		if (function.equals("sim_start")) {
			start(parse(args[0]));
			return null;
		}
		if (function.equals("sim_asset")) {
			Simulation m = current;
			if (m == null) {
				throw new IllegalStateException("No simulation model");
			}
			String file = m.resolve(args[2]);
			if (args[0].equals("image")) {
				m.loadImage(args[1], file);
			} else {
				m.loadModel(args[1], file);
			}
			return null;
		}
		GeoGrid g = loading;
		if (g == null) {
			throw new IllegalStateException("The functions of the data (grid, raster, vector) can only be used in load() of a geospatial model");
		}
		String op = args[0];
		switch (op) {
		case "grid" -> {
			g.grid(args[1], Integer.parseInt(args[2].trim()));
			return g.geometry();
		}
		case "raster" -> {
			return PyValue.of(g.raster(args[1], Integer.parseInt(args[2].trim()), args[3]));
		}
		case "vector" -> {
			return PyValue.of(args[2].isEmpty() ? g.vector(args[1], Double.parseDouble(args[3].trim())) : g.vector(args[1], args[2]));
		}
		case "add_layer" -> {
			g.addLayer(args[1], doubles(value));
			return null;
		}
		case "attribution" -> {
			g.attribution(args[1]);
			return null;
		}
		case "globe" -> {
			g.showOnGlobe();
			return null;
		}
		default -> throw new IllegalArgumentException("Unknown function " + op);
		}
	}

	/* Creates the model of the kind and starts it (in another thread: the script waits for the call) */
	private static synchronized void start(Spec spec) {
		Script script = new Script(spec, PYTHON.equals(spec.language) ? new PythonEngine() : new REngine());
		Simulation model;
		creating = script;
		try {
			model = switch (spec.kind) {
			case "GridModel" -> new Grid();
			case "ValueGridModel" -> new Values();
			case "AgentModel" -> new Agents();
			case "GeoGridModel" -> new GeoGrid();
			case "GeoAgentModel" -> new GeoAgents();
			default -> throw new IllegalArgumentException("Unknown kind of model: " + spec.kind);
			};
		} finally {
			creating = null;
		}
		Compiled.setModel(model);
		current = model;
		/* Unknown file of the script (e.g., run from the editor): the file of the active editor */
		if (spec.source == null && Simulation.hasDisplay()) {
			Display.getDefault().asyncExec(() -> spec.source = activeScriptFile(spec.language));
		}
		/* The setup waits until the script has finished (the interpreter is free) */
		script.later(model::setup);
	}

	/* The file of the active editor if it is a script of the language */
	private static String activeScriptFile(String language) {
		try {
			org.eclipse.ui.IEditorPart editor = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (editor != null && editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput in) {
				String name = in.getFile().getName().toLowerCase(java.util.Locale.ROOT);
				if (PYTHON.equals(language) ? name.endsWith(".py") : name.endsWith(".r")) {
					return in.getFile().getLocation().toOSString();
				}
			}
		} catch (RuntimeException e) {
			/* No editor */
		}
		return null;
	}

	static Spec parse(String json) throws IOException {
		JsonNode n = new ObjectMapper().readTree(json);
		Spec s = new Spec();
		s.name = n.path("name").asText("Model");
		s.kind = n.path("kind").asText();
		s.language = n.path("language").asText(PYTHON);
		s.description = n.path("description").asText("");
		s.source = n.hasNonNull("source") ? n.get("source").asText() : null;
		for (JsonNode p : n.path("params")) {
			JavaToJs.ParamInfo i = new JavaToJs.ParamInfo();
			i.name = p.path("name").asText();
			i.jsName = i.name;
			i.label = p.path("label").asText(i.name);
			i.kind = p.path("kind").asText("double");
			i.restart = p.path("restart").asBoolean(false);
			i.group = p.path("group").asText("");
			i.min = number(p.get("min"));
			i.max = number(p.get("max"));
			i.step = number(p.get("step"));
			List<String> options = new ArrayList<>();
			for (JsonNode o : p.path("options")) {
				options.add(o.asText());
			}
			i.options = options.toArray(new String[0]);
			JsonNode v = p.get("value");
			i.value = switch (i.kind) {
			case "boolean" -> v != null && v.asBoolean();
			case "int" -> v == null ? 0 : (int) Math.round(v.asDouble());
			case "double" -> v == null ? 0.0 : v.asDouble();
			default -> v == null ? "" : v.asText();
			};
			s.params.add(i);
		}
		for (JsonNode a : n.path("actions")) {
			JavaToJs.ActionInfo i = new JavaToJs.ActionInfo();
			i.name = a.path("name").asText();
			i.jsName = i.name;
			i.label = a.path("label").asText(i.name);
			i.group = a.path("group").asText("");
			s.actions.add(i);
		}
		return s;
	}

	private static double number(JsonNode n) {
		return n == null || n.isNull() || !n.isNumber() ? Double.NaN : n.asDouble();
	}

	/* ===================== The interpreters ===================== */

	/* The embedded Python (the module bio7sim is installed at the start of Python) */
	static final class PythonEngine implements Engine {
		@Override
		public State run(String op, String arg) throws Exception {
			com.eco.bio7.pyffm.PySession s = com.eco.bio7.pybridge.EmbeddedPython.session();
			if (s == null) {
				throw new IllegalStateException("The embedded Python is not running");
			}
			return new PyState(s.eval("__import__('bio7sim')._do(" + JavaToJs.quote(op) + ", " + JavaToJs.quote(arg) + ")"));
		}
	}

	/* The embedded R (the functions of bio7sim are attached at the start of R) */
	static final class REngine implements Engine {
		@Override
		public State run(String op, String arg) throws Exception {
			com.eco.bio7.rffm.RSession s = com.eco.bio7.rffm.RSession.current();
			if (s == null) {
				throw new IllegalStateException("The embedded R is not running");
			}
			return new RState(s.eval(".bio7sim_do(" + JavaToJs.quote(op) + ", " + JavaToJs.quote(arg) + ")"));
		}
	}

	/* The state of Python: a dict of numpy arrays, numbers and lists of strings */
	static final class PyState implements State {
		private final Map<String, PyValue> values;

		PyState(PyValue v) {
			values = v instanceof PyValue.PyDict d ? d.values() : Map.of();
		}

		@Override
		public boolean has(String key) {
			PyValue v = values.get(key);
			return v != null && !(v instanceof PyValue.None);
		}

		@Override
		public double number(String key, double otherwise) {
			double[] d = doubles(key);
			return d.length > 0 ? d[0] : otherwise;
		}

		@Override
		public double[] doubles(String key) {
			PyValue v = values.get(key);
			if (v instanceof PyValue.Array a) {
				return PythonArrays.doubles(a.data());
			}
			if (v instanceof PyValue.PyList l) {
				double[] d = new double[l.values().size()];
				for (int i = 0; i < d.length; i++) {
					PyValue e = l.values().get(i);
					d[i] = e instanceof PyValue.Array a && a.length() > 0 ? PythonArrays.doubles(a.data())[0] : Double.NaN;
				}
				return d;
			}
			return new double[0];
		}

		@Override
		public int[] ints(String key) {
			PyValue v = values.get(key);
			if (v instanceof PyValue.Array a && a.data() instanceof int[] i) {
				return i;
			}
			double[] d = doubles(key);
			int[] r = new int[d.length];
			for (int i = 0; i < d.length; i++) {
				r[i] = (int) d[i];
			}
			return r;
		}

		@Override
		public String[] strings(String key) {
			PyValue v = values.get(key);
			if (v instanceof PyValue.Str s) {
				return s.values();
			}
			if (v instanceof PyValue.PyList l) {
				String[] r = new String[l.values().size()];
				for (int i = 0; i < r.length; i++) {
					r[i] = l.values().get(i) instanceof PyValue.Str s ? s.asString() : "";
				}
				return r;
			}
			return new String[0];
		}
	}

	/* The arrays of numpy (all types) as doubles */
	static final class PythonArrays {
		static double[] doubles(Object data) {
			if (data instanceof double[] d) {
				return d;
			}
			int n = java.lang.reflect.Array.getLength(data);
			double[] r = new double[n];
			for (int i = 0; i < n; i++) {
				Object e = java.lang.reflect.Array.get(data, i);
				r[i] = e instanceof Boolean b ? (b ? 1 : 0) : ((Number) e).doubleValue();
			}
			return r;
		}
	}

	/* The state of R: a named list of vectors */
	static final class RState implements State {
		private final com.eco.bio7.rffm.RValue list;

		RState(com.eco.bio7.rffm.RValue v) {
			list = v;
		}

		private com.eco.bio7.rffm.RValue get(String key) {
			return list instanceof com.eco.bio7.rffm.RValue.List l ? l.get(key) : null;
		}

		@Override
		public boolean has(String key) {
			com.eco.bio7.rffm.RValue v = get(key);
			return v != null && !v.isNull();
		}

		@Override
		public double number(String key, double otherwise) {
			double[] d = doubles(key);
			return d.length > 0 ? d[0] : otherwise;
		}

		@Override
		public double[] doubles(String key) {
			com.eco.bio7.rffm.RValue v = get(key);
			return v == null || v.isNull() ? new double[0] : v.doubles();
		}

		@Override
		public int[] ints(String key) {
			com.eco.bio7.rffm.RValue v = get(key);
			if (v == null || v.isNull()) {
				return new int[0];
			}
			if (v instanceof com.eco.bio7.rffm.RValue.Int) {
				return v.ints();
			}
			double[] d = v.doubles();
			int[] r = new int[d.length];
			for (int i = 0; i < d.length; i++) {
				r[i] = (int) d[i];
			}
			return r;
		}

		@Override
		public String[] strings(String key) {
			com.eco.bio7.rffm.RValue v = get(key);
			return v == null || v.isNull() ? new String[0] : v.strings();
		}
	}

	/* The numbers of a value of Python or R (e.g., a layer) */
	private static double[] doubles(PyValue v) {
		if (v instanceof PyValue.Array a) {
			return PythonArrays.doubles(a.data());
		}
		throw new IllegalArgumentException("Numbers expected");
	}

	/* ===================== The modules ===================== */

	/** The source of the module bio7sim of Python (the API of the simulation). */
	public static String pythonModule() {
		return resource("bio7sim.py");
	}

	/** The source of the functions of the simulation for R. */
	public static String rModule() {
		return resource("bio7sim.R");
	}

	private static String resource(String name) {
		try (InputStream in = ScriptSimulation.class.getResourceAsStream(name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException | NullPointerException e) {
			throw new IllegalStateException("The module " + name + " is missing", e);
		}
	}

	/** Installs the module bio7sim in the embedded Python (at its start). */
	public static void installPython(com.eco.bio7.pyffm.PySession session) {
		try {
			session.exec("import sys as _s, types as _t\n_m = _t.ModuleType('bio7sim')\n_m.__file__ = 'bio7sim.py'\nexec(compile(" + JavaToJs.quote(pythonModule())
					+ ", 'bio7sim.py', 'exec'), _m.__dict__)\n_s.modules['bio7sim'] = _m\ndel _s, _t, _m\n");
		} catch (Exception e) {
			System.err.println("The simulation module bio7sim could not be installed in Python: " + e.getMessage());
		}
	}

	/** Installs the functions of the simulation in the embedded R (at its start). */
	public static void installR(com.eco.bio7.rffm.RSession session) {
		try {
			session.voidEval(rModule());
		} catch (Exception e) {
			System.err.println("The simulation functions could not be installed in R: " + e.getMessage());
		}
	}
}
