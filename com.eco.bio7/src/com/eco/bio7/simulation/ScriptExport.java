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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * The HTML export of a simulation model of Python or R: the page runs the same
 * script in the web browser, Python with Pyodide (Python and numpy in
 * WebAssembly), R with webR (R in WebAssembly). Both are loaded from their
 * servers when the page is opened (an internet connection is needed). The page
 * gets the settings, the buttons and the chart of the running model and the
 * loaded data of a geospatial model.
 */
public final class ScriptExport {

	/** The version of Pyodide (Python 3.12, numpy 2.0) loaded by the pages of Python models. */
	public static final String PYODIDE = "https://cdn.jsdelivr.net/pyodide/v0.27.7/full/";

	/** The version of webR (R 4.5) loaded by the pages of R models. */
	public static final String WEBR = "https://webr.r-wasm.org/v0.5.4/webr.mjs";

	private ScriptExport() {
	}

	/* The state of the script -> the views of the page (as _show of the Java models) */
	private static final String SHOW = """
			/* A value of length 1 (R returns vectors) */
			function bio7One(x) { return x !== null && typeof x === 'object' && x.length !== undefined && typeof x !== 'string' ? x[0] : x; }
			function bio7Show(sim, s) {
			  const kind = BIO7.kind;
			  for (const k of ['width', 'height', 'min', 'max', 'radius', 'color', 'size']) if (k in s) s[k] = bio7One(s[k]);
			  if (kind === 'GridModel' || kind === 'GeoGridModel') {
			    sim.width = s.width; sim.height = s.height;
			    if (s.palette && !Array.isArray(s.palette)) s.palette = [s.palette];
			    if (s.shown) { sim.cells = s.shown; sim.palette = null; sim.min = s.min; sim.max = s.max; sim.ramp = s.colors; }
			    else { sim.cells = s.cells; sim.palette = s.palette; sim.ramp = null; }
			  } else if (kind === 'ValueGridModel') {
			    sim.width = s.width; sim.height = s.height; sim.cells = s.values; sim.palette = null; sim.min = s.min; sim.max = s.max; sim.ramp = s.colors;
			  } else if (kind === 'AgentModel') {
			    const xyz = s.xyz, n = xyz.length / 3, colors = s.colors || [], dirs = new Array(n * 3), rgb = new Array(n * 3), last = bio7Show.last || [];
			    let own = false;
			    const box = s.box, base = s.color;
			    for (let i = 0; i < n; i++) {
			      /* The direction of the last movement (for cones and gliders) */
			      const p = last[i];
			      let dx = 0, dy = 0, dz = 0;
			      if (p) { dx = xyz[i * 3] - p[0]; dy = xyz[i * 3 + 1] - p[1]; dz = xyz[i * 3 + 2] - p[2]; if (Math.abs(dx) + Math.abs(dy) >= Math.max(box[0], box[1]) / 2) { dx = p[3]; dy = p[4]; dz = p[5]; } }
			      if (dx * dx + dy * dy + dz * dz < 1e-18 && p) { dx = p[3]; dy = p[4]; dz = p[5]; }
			      last[i] = [xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2], dx, dy, dz];
			      dirs[i * 3] = dx; dirs[i * 3 + 1] = dy; dirs[i * 3 + 2] = dz;
			      const c = colors[i] || base;
			      if (colors[i]) own = true;
			      rgb[i * 3] = parseInt(c.substr(1, 2), 16) / 255; rgb[i * 3 + 1] = parseInt(c.substr(3, 2), 16) / 255; rgb[i * 3 + 2] = parseInt(c.substr(5, 2), 16) / 255;
			    }
			    last.length = n; bio7Show.last = last;
			    const size = Math.max(box[0], box[1], box[2]);
			    sim.box = [box[0], box[1], box[2]];
			    sim.points = { xyz: Array.from(xyz), dirs: dirs, colors: own ? rgb : null, color: base, radius: s.radius > 0 ? s.radius : size * 0.01 };
		    /* The shape, the floor and the shadows of the model; the objects of the environment */
		    sim.applyModelScene(bio7One(s.shape), bio7One(s.floor), bio7One(s.shadows));
		    sim.scenery = sim.sceneryOf(s.scenery_models, s.scenery, s.scenery_animations);
			    if (s.images || s.models) {
			      const arr = function (v) { return v === undefined || v === null ? [] : Array.isArray(v) ? v : Array.from(v); }, nn = function (v) { return v || null; };
			      sim.points.images = arr(s.images).map(nn); sim.points.models = arr(s.models).map(nn); sim.points.animations = arr(s.animations).map(nn);
			      sim.points.headings = arr(s.headings).map(function (v) { return v === null ? NaN : v; }); sim.points.sizes = arr(s.sizes).map(Number);
			    }
			    /* The links of the network (pairs of indices from 0) */
			    if (s.links !== undefined && s.links !== null && s.links.length) {
			      const lc = s.link_colors === undefined || s.link_colors === null ? [] : typeof s.link_colors === 'string' ? [s.link_colors] : Array.from(s.link_colors);
			      sim.links = { pairs: Array.from(s.links).map(Number), colors: lc, color: bio7One(s.link_color) || '', width: s.link_width === undefined ? 1 : Number(bio7One(s.link_width)) };
			    } else sim.links = null;
			  } else if (kind === 'GeoAgentModel') {
			    const n = s.lon.length, g = { lon: Array.from(s.lon), lat: Array.from(s.lat), colors: new Array(n), sizes: new Array(n), labels: new Array(n) };
			    for (let i = 0; i < n; i++) { g.colors[i] = (s.colors && s.colors[i]) || s.color; g.sizes[i] = s.sizes && s.sizes[i] > 0 ? s.sizes[i] : s.size; g.labels[i] = (s.labels && s.labels[i]) || null; }
			    if (s.images && Array.from(s.images).some(function (v) { return v; })) {
			      /* The images of the agents and their headings (given or the direction of the last movement) */
			      const last = bio7Show.geo || [], hs = s.heading ? Array.from(s.heading) : [];
			      g.images = Array.from(s.images).map(function (v) { return v || null; }); g.headings = new Array(n);
			      for (let i = 0; i < n; i++) {
			        const q = last[i]; let h = q ? q[2] : 0;
			        if (q && (q[0] !== g.lon[i] || q[1] !== g.lat[i])) { h = Math.atan2((g.lon[i] - q[0]) * Math.cos(g.lat[i] * Math.PI / 180), g.lat[i] - q[1]) * 180 / Math.PI; if (h < 0) h += 360; }
			        g.headings[i] = hs[i] !== undefined && hs[i] !== null && !isNaN(hs[i]) ? hs[i] : h;
			        last[i] = [g.lon[i], g.lat[i], g.headings[i]];
			      }
			      last.length = n; bio7Show.geo = last;
			    }
			    sim.cells = null; sim.geoAgents = g; sim.mapBounds = [[s.area[0], s.area[1]], [s.area[2], s.area[3]]];
			  }
			  const names = [].concat(s.chart_names || []), values = [].concat(Array.from(s.chart_values || []));
			  for (let i = 0; i < Math.min(names.length, values.length); i++) sim.chart(names[i], values[i]);
			  /* The drawing and the further charts (JSON of the script) */
			  if (s.draw !== undefined && s.draw !== null) sim.drawing = JSON.parse(bio7One(s.draw) || '[]');
			  if (s.more_charts) {
			    const arr = function (v) { return Array.isArray(v) ? v : [v]; };
			    for (const c of JSON.parse(bio7One(s.more_charts) || '[]')) {
			      if (c.k === 'line') sim.chartLine(c.t, c.n, c.x, c.y); else if (c.k === 'xy') sim.chartXY(c.t, c.n, c.x, c.y); else if (c.k === 'bars') sim.chartBars(c.t, arr(c.l), arr(c.v));
			    }
			  }
			}
			/* The values of the settings (the changed ones) */
			const bio7Sent = {};
			function bio7Changed(sim, all) {
			  const changed = [];
			  for (const p of PARAMS) {
			    if (!all && p.restart) continue;
			    let v = sim.p[p.name];
			    if (p.kind === 'int') v = Math.round(Number(v)); else if (p.kind === 'double') v = Number(v); else if (p.kind === 'boolean') v = !!v; else v = String(v);
			    if (all || bio7Sent[p.name] !== v) { bio7Sent[p.name] = v; changed.push([p.name, v]); }
			  }
			  return changed;
			}
			""";

	/* Python in the browser: Pyodide */
	private static final String PYTHON = """
			let bio7py = null, bio7sim = null;
			function bio7LoadScript(url) {
			  return new Promise(function (ok, no) {
			    const s = document.createElement('script'); s.src = url; s.onload = ok;
			    s.onerror = function () { no(new Error('Python (Pyodide) could not be loaded from ' + url + ' (an internet connection is needed)')); };
			    document.head.appendChild(s);
			  });
			}
			async function prepare(sim) {
			  sim.status('Loading Python (Pyodide) ...');
			  await bio7LoadScript(BIO7.runtime + 'pyodide.js');
			  bio7py = await loadPyodide({ indexURL: BIO7.runtime });
			  sim.status('Loading numpy ...');
			  await bio7py.loadPackage('numpy');
			  /* Further packages of the imports of the script (e.g., scipy, pandas) */
			  try { await bio7py.loadPackagesFromImports(BIO7.model); } catch (e) { console.warn(e); }
			  bio7py.FS.writeFile('/home/pyodide/bio7sim.py', BIO7.api);
			  bio7py.runPython('import sys\\nif "/home/pyodide" not in sys.path: sys.path.insert(0, "/home/pyodide")\\nimport bio7sim\\nbio7sim.BROWSER = True');
			  bio7sim = bio7py.pyimport('bio7sim');
			  if (typeof GEO !== 'undefined') bio7sim.GEO = bio7py.toPy(bio7Geo());
			  sim.status('Starting the model ...');
			  bio7py.runPython(BIO7.model, { filename: BIO7.file });
			  bio7Do('load', '');
			}
			function bio7Do(op, arg) {
			  const r = bio7sim._do(op, arg);
			  try { return r.toJs({ dict_converter: Object.fromEntries }); } finally { r.destroy(); }
			}
			function bio7Set(sim, all) {
			  const changed = bio7Changed(sim, all);
			  if (changed.length) bio7Do('set', JSON.stringify(Object.fromEntries(changed)));
			}
			function setup(sim) { bio7Set(sim, true); bio7Show.last = null; bio7Show.geo = null; bio7Show(sim, bio7Do('setup', String(sim.seed))); }
			/* stop() of the script: the page stops */
			function step(sim) { bio7Set(sim, false); const s = bio7Do('step', ''); bio7Show(sim, s); if (Number(bio7One(s.stopped))) return 'stop'; }
			function bio7Action(sim, name) { bio7Set(sim, false); bio7Show(sim, bio7Do('action', name)); }
			/* The input of the view: click(x, y), drag(x, y) or key(key) of the model */
			function input(sim, ev) { bio7Set(sim, false); bio7Show(sim, bio7Do('input', ev.type === 'key' ? 'key\\u001f' + ev.key : ev.type + '\\u001f' + ev.x + '\\u001f' + ev.y)); }
			""";

	/* R in the browser: webR (asynchronous) */
	private static final String R = """
			let bio7webR = null;
			/*
			 * webR: a page opened as a file has no origin in Firefox ("null"), webR can't build its URLs
			 * with it (new URL(url, location.origin)); then the module is loaded as text with the address
			 * of the page as the base and imported from a blob
			 */
			async function bio7ImportWebR() {
			  if (location.origin !== 'null') return import(BIO7.runtime);
			  const code = await (await fetch(BIO7.runtime)).text();
			  const fixed = code.replace(/new URL\\((\\w+),location\\.origin\\)/g, 'new URL($1,location.href)');
			  const url = URL.createObjectURL(new Blob([fixed], { type: 'text/javascript' }));
			  try { return await import(url); } finally { URL.revokeObjectURL(url); }
			}
			async function prepare(sim) {
			  sim.status('Loading R (webR) ...');
			  let WebR;
			  try { WebR = (await bio7ImportWebR()).WebR; }
			  catch (e) { throw new Error('R (webR) could not be loaded from ' + BIO7.runtime + ' (an internet connection is needed)'); }
			  /* The base URL of webR (its folder): a page opened as a file has no origin in Firefox (null) */
			  bio7webR = new WebR({ baseUrl: BIO7.runtime.replace(/[^/]*$/, '') });
			  await bio7webR.init();
			  await bio7webR.evalRVoid('.bio7sim_browser <- TRUE');
			  if (typeof GEO !== 'undefined') {
			    const g = bio7Geo(), layers = {};
			    for (const k in g.layers) layers[k] = g.layers[k];
			    g.layers = await new bio7webR.RList(layers);
			    await bio7webR.objs.globalEnv.bind('.bio7sim_geo', await new bio7webR.RList(g));
			  }
			  await bio7webR.evalRVoid(BIO7.api);
			  sim.status('Starting the model ...');
			  await bio7webR.evalRVoid(BIO7.model);
			  await bio7Do('load', '');
			}
			/* An R value (toJs) as JavaScript: a named list as an object, a vector as an array */
			function bio7Plain(o) {
			  if (o.type === 'list') {
			    if (!o.names) return o.values.map(bio7Plain);
			    const r = {};
			    o.names.forEach(function (n, i) { r[n] = bio7Plain(o.values[i]); });
			    return r;
			  }
			  if (o.type === 'null') return null;
			  return o.values;
			}
			async function bio7Do(op, arg) {
			  const r = await bio7webR.evalR('.bio7sim_do(' + JSON.stringify(op) + ', ' + JSON.stringify(arg) + ')');
			  try { return bio7Plain(await r.toJs()); } finally { bio7webR.destroy(r); }
			}
			async function bio7Set(sim, all) {
			  for (const c of bio7Changed(sim, all)) await bio7Do('set', c[0] + '\\u001f' + c[1]);
			}
			async function setup(sim) { await bio7Set(sim, true); bio7Show.last = null; bio7Show.geo = null; bio7Show(sim, await bio7Do('setup', String(sim.seed))); }
			/* m$stop() of the script: the page stops */
			async function step(sim) { await bio7Set(sim, false); const s = await bio7Do('step', ''); bio7Show(sim, s); if (Number(bio7One(s.stopped))) return 'stop'; }
			async function bio7Action(sim, name) { await bio7Set(sim, false); bio7Show(sim, await bio7Do('action', name)); }
			/* The input of the view: m$click(m, x, y), m$drag(m, x, y) or m$key(m, key) of the model */
			async function input(sim, ev) { await bio7Set(sim, false); bio7Show(sim, await bio7Do('input', ev.type === 'key' ? 'key\\u001f' + ev.key : ev.type + '\\u001f' + ev.x + '\\u001f' + ev.y)); }
			""";

	/* The layers of a geospatial model (base64 of little endian doubles) */
	private static final String DOUBLES = "function bio7Doubles(text) { const bin = atob(text), bytes = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i); return new Float64Array(bytes.buffer); }\n";

	/* The data of a geospatial model as plain numbers (for Python and R) */
	private static final String GEO = """
			function bio7Geo() {
			  const g = {};
			  for (const k in GEO) if (k !== 'layers') g[k] = GEO[k];
			  g.layers = {};
			  for (const k in GEO.layers) g.layers[k] = Array.from(GEO.layers[k]);
			  return g;
			}
			""";

	/**
	 * Creates the page of a running model of Python or R.
	 *
	 * @param model    the model (started by simulation() of the script)
	 * @param script   the source of the script
	 * @param fileName the name of the file of the script
	 * @param options  the options of the page (null: defaults)
	 */
	public static SimulationExport.Result create(Simulation model, String script, String fileName, SimulationExport.Options options) {
		SimulationExport.Result result = new SimulationExport.Result();
		if (!(model instanceof ScriptSimulation.Host host)) {
			result.errors.add("The model is not a model of Python or R");
			return result;
		}
		ScriptSimulation.Spec spec = host.script().spec();
		result.modelName = spec.name;
		result.kind = spec.kind;
		if (options == null) {
			options = new SimulationExport.Options();
		}
		if (model instanceof GeoGridModel geo && options.geo == null) {
			Map<String, Object> data = geo.exportData();
			if (geo.width == 0) {
				result.errors.add("The data of the geospatial model is missing: run the script in Bio7 first (load() reads the layers with GDAL, the page gets the loaded data)");
				return result;
			}
			options.geo = data;
		}
		boolean python = ScriptSimulation.PYTHON.equals(spec.language);
		StringBuilder js = new StringBuilder();
		js.append("/* ----- The model of ").append(python ? "Python" : "R").append(" (").append(fileName).append(") runs in the web browser with ")
				.append(python ? "Pyodide" : "webR").append(" ----- */\n");
		js.append("const BIO7 = { kind: ").append(JavaToJs.quote(spec.kind)).append(", file: ").append(JavaToJs.quote(fileName)).append(", runtime: ")
				.append(JavaToJs.quote(python ? PYODIDE : WEBR)).append(",\n  api: ").append(JavaToJs.quote(python ? ScriptSimulation.pythonModule() : ScriptSimulation.rModule()))
				.append(",\n  model: ").append(JavaToJs.quote(script)).append(" };\n");
		options.images = model.getImageFiles();
		options.models = model.getModelFiles();
		js.append(SimulationExport.assets(options));
		js.append("const PARAMS = [");
		for (int i = 0; i < spec.params.size(); i++) {
			JavaToJs.ParamInfo p = spec.params.get(i);
			js.append(i > 0 ? ", " : "").append("{ name: ").append(JavaToJs.quote(p.name)).append(", kind: ").append(JavaToJs.quote(p.kind)).append(", restart: ").append(p.restart).append(" }");
		}
		js.append("];\n");
		if (options.geo != null) {
			js.append(DOUBLES).append(SimulationExport.geoData(options.geo)).append(GEO);
		}
		js.append(SHOW).append(python ? PYTHON : R);
		/* The buttons */
		for (JavaToJs.ActionInfo act : spec.actions) {
			js.append("function action_").append(jsName(act.name)).append("(sim) { return bio7Action(sim, ").append(JavaToJs.quote(act.name)).append("); }\n");
		}
		result.javaScript = js.toString();
		String title = options.title != null && !options.title.isBlank() ? options.title : SimulationExport.words(spec.name);
		String description = options.description != null ? options.description : spec.description;
		result.title = title;
		result.description = description;
		HtmlSimulation page = new HtmlSimulation(title).description(SimulationExport.escape(description))
				.view(spec.kind.equals("AgentModel") ? HtmlSimulation.View.SCENE_3D : HtmlSimulation.View.GRID_2D).speed(options.speed).seed(options.seed).autoplay(options.autoplay).theme(options.theme)
				.layout(options.layout).accent(options.accent).scene(options.background, options.lighting, options.shape, options.floor, options.shadows).autoRotate(options.rotate)
				.asynchronous(!python).inputs(host.script().inputs().stream().sorted().toList());
		/* The settings and the buttons in the groups (as in Bio7) */
		Map<String, List<Object>> groups = new LinkedHashMap<>(host.script().groups());
		boolean headings = groups.keySet().stream().anyMatch(g -> !g.isEmpty() && !g.equals("\u0000"));
		if (!groups.isEmpty() && !headings) {
			page.heading("Settings");
		}
		List<String> restart = new ArrayList<>();
		for (Map.Entry<String, List<Object>> g : groups.entrySet()) {
			if (!g.getKey().isEmpty() && !g.getKey().equals("\u0000")) {
				page.heading(SimulationExport.escape(g.getKey()));
			} else if (g.getKey().equals("\u0000") && headings) {
				page.heading("Actions");
			}
			for (Object o : g.getValue()) {
				if (o instanceof JavaToJs.ActionInfo act) {
					page.button(SimulationExport.escape(act.label), "action_" + jsName(act.name));
					continue;
				}
				JavaToJs.ParamInfo p = (JavaToJs.ParamInfo) o;
				SimulationExport.control(page, p);
				if (p.restart) {
					restart.add(p.name);
				}
			}
		}
		if (!restart.isEmpty()) {
			page.restartOn(restart.toArray(new String[0]));
		}
		if (spec.kind.equals("GeoAgentModel")) {
			page.map(Double.NaN, Double.NaN, Double.NaN, Double.NaN, options.basemap, 1, "");
		}
		if (options.geo != null) {
			Map<String, Object> g = options.geo;
			page.map(((Number) g.get("south")).doubleValue(), ((Number) g.get("west")).doubleValue(), ((Number) g.get("north")).doubleValue(), ((Number) g.get("east")).doubleValue(), options.basemap,
					options.mapOpacity, String.valueOf(g.get("sources")));
		}
		if (!model.getSeries().isEmpty()) {
			page.chart(SimulationExport.escape(String.join(", ", model.getSeries().keySet())));
		}
		page.code(result.javaScript);
		result.page = page;
		return result;
	}

	/* A name of Python or R as a name of JavaScript (e.g., add.glider -> add_glider) */
	private static String jsName(String name) {
		return name.replaceAll("[^A-Za-z0-9_$]", "_");
	}
}
