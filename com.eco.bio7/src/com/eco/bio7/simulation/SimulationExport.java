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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.eco.bio7.spatial.HtmlSimulation;

/**
 * Exports a simulation model of the simulation API (a Java source file of a
 * subclass of {@link GridModel} or {@link AgentModel}) as an interactive HTML
 * page: the model is translated to JavaScript ({@link JavaToJs}), runs with the
 * JavaScript version of the API and is displayed with the page of
 * {@link HtmlSimulation} (a 2D grid or a 3D view, the settings of the Param
 * fields, Play/Pause, Step, Reset, the speed and a chart).
 */
public class SimulationExport {

	/** The result of a translation: the page or the errors. */
	public static class Result {
		public HtmlSimulation page;
		public String javaScript;
		public List<String> errors = new ArrayList<>();
		public List<String> warnings = new ArrayList<>();
		public String modelName;
		/** The title and the description of the page (the options or the defaults). */
		public String title, description;
		/** "GridModel", "ValueGridModel" or "AgentModel". */
		public String kind;

		public boolean ok() {
			return errors.isEmpty();
		}
	}

	/** The options of the page. */
	public static class Options {
		public String title;
		public String description;
		public double speed = 10;
		/** The seed of the setup (the field Seed of the page; default 42). */
		public int seed = Simulation.DEFAULT_SEED;
		public boolean autoplay;
		/** The theme of the page: "auto", "light" or "dark". */
		public String theme = "auto";
		/** The layout of the page: "classic", "dashboard", "focus" or "story". */
		public String layout = "classic";
		/** The accent color: "blue", "teal", "violet", "orange" or "graphite". */
		public String accent = "blue";
		/** The 3D view (agent models): background, lighting, shape, floor, shadows, rotation. */
		public String background = "auto", lighting = "studio", shape = "sphere", floor = "grid";
		public boolean shadows = true, rotate = false;
		/** The data of a geospatial model (GeoGridModel.exportData() of the running model). */
		public java.util.Map<String, Object> geo;
		/** The base map of a geospatial model: "topo" (OpenTopoMap, default) or "osm" (OpenStreetMap, only on a web server). */
		public String basemap = "topo";
		/** The opacity of the grid over the map (0 - 1). */
		public double mapOpacity = 0.75;
		/** The images and the 3D models (glTF) of the model: the name and the file (embedded in the page). */
		public java.util.Map<String, String> images = new java.util.LinkedHashMap<>(), models = new java.util.LinkedHashMap<>();
	}

	/** The JavaScript version of the simulation API (com.eco.bio7.simulation). */
	public static final String RUNTIME = """
			/* ----- The simulation API of Bio7 (the JavaScript version of com.eco.bio7.simulation) ----- */
			class SimRandom {
			  constructor(seed) { this.state = seed | 0; }
			  nextDouble() {
			    this.state = (this.state + 0x6D2B79F5) | 0;
			    let t = this.state;
			    t = Math.imul(t ^ (t >>> 15), t | 1);
			    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
			    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
			  }
			  nextInt(n) { return Math.trunc(this.nextDouble() * n); }
			}
			const Bio7Sim = {
			  array(sizes, leaf) {
			    const make = function (d) {
			      const n = sizes[d];
			      if (d === sizes.length - 1) {
			        if (leaf === 'null') return new Array(n).fill(null);
			        if (leaf === 'false') return new Array(n).fill(false);
			        return new (globalThis[leaf])(n);
			      }
			      const a = new Array(n);
			      for (let i = 0; i < n; i++) a[i] = make(d + 1);
			      return a;
			    };
			    return make(0);
			  },
			  remove(list, o) { const i = list.indexOf(o); if (i < 0) return false; list.splice(i, 1); return true; },
			  removeIf(list, f) { let n = 0; for (let i = list.length - 1; i >= 0; i--) if (f(list[i])) { list.splice(i, 1); n++; } return n > 0; },
			  getOrDefault(map, k, d) { return map.has(k) ? map.get(k) : d; },
			  str(x) {
			    if (typeof x !== 'number') return String(x);
			    if (!isFinite(x)) return String(x);
			    if (x === 0) return Object.is(x, -0) ? '-0.0' : '0.0';
			    const a = Math.abs(x);
			    if (a >= 1e7 || a < 1e-3) { const e = x.toExponential().split('e'); return (e[0].includes('.') ? e[0] : e[0] + '.0') + 'E' + parseInt(e[1], 10); }
			    const s = String(x);
			    return s.includes('.') ? s : s + '.0';
			  },
			  floorMod(a, b) { return ((a % b) + b) % b; },
			  rint(x) { const f = Math.floor(x), r = x - f; return r > 0.5 ? f + 1 : r < 0.5 ? f : (f % 2 === 0 ? f : f + 1); },
			  arraycopy(src, sp, dst, dp, n) { if (src === dst && sp < dp) { for (let i = n - 1; i >= 0; i--) dst[dp + i] = src[sp + i]; } else { for (let i = 0; i < n; i++) dst[dp + i] = src[sp + i]; } },
			  copyOf(a, n) { const b = new a.constructor(n); for (let i = 0; i < Math.min(n, a.length); i++) b[i] = a[i]; return b; },
			  /* IntStream.range(a, b) (also .parallel(), it runs as a loop in the browser) */
			  intRange(from, to) {
			    return { parallel() { return this; }, sequential() { return this; },
			      forEach(f) { for (let i = from; i < to; i++) f(i); },
			      sum() { let s = 0; for (let i = from; i < to; i++) s += i; return s; } };
			  },
			  shuffle(list, model) { for (let i = list.length - 1; i > 0; i--) { const j = model.randomInt(i + 1), t = list[i]; list[i] = list[j]; list[j] = t; } },
			  format(f, ...args) {
			    let k = 0;
			    return f.replace(/%(\\.(\\d+))?([dsf%n])/g, function (m, p, digits, c) {
			      if (c === '%') return '%'; if (c === 'n') return '\\n';
			      const v = args[k++];
			      if (c === 'f') return Number(v).toFixed(digits === undefined ? 6 : parseInt(digits, 10));
			      if (c === 'd') return String(Math.trunc(v));
			      return String(v);
			    });
			  }
			};
			class Simulation {
			  constructor() { this.t = 0; this._random = new SimRandom(42); this._sim = null; }
			  init() {}
			  step() {}
			  seed(s) { this._random = new SimRandom(s); this._seed = s | 0; }
			  randomAt(index, k = 0) {
			    let h = Math.imul(this._seed === undefined ? 42 : this._seed, 0x9E3779B1) ^ Math.imul(this.t, 0x85EBCA77) ^ Math.imul(index, 0xC2B2AE3D) ^ Math.imul(k, 0x27D4EB2F);
			    h ^= h >>> 16; h = Math.imul(h, 0x7FEB352D); h ^= h >>> 15; h = Math.imul(h, 0x846CA68B); h ^= h >>> 16;
			    return (h >>> 0) / 4294967296;
			  }
			  random(min, max) { const r = this._random.nextDouble(); return min === undefined ? r : min + r * (max - min); }
			  randomInt(a, b) { return b === undefined ? this._random.nextInt(a) : a + this._random.nextInt(b - a); }
			  chance(p) { return this._random.nextDouble() < p; }
			  randomGaussian(mean, sd) {
			    let u, v, s;
			    do { u = 2 * this._random.nextDouble() - 1; v = 2 * this._random.nextDouble() - 1; s = u * u + v * v; } while (s >= 1 || s === 0);
			    const g = u * Math.sqrt(-2 * Simulation._log(s) / s);
			    return mean === undefined ? g : mean + sd * g;
			  }
			  randomExponential(rate) { return -Simulation._log(1 - this._random.nextDouble()) / rate; }
			  randomPoisson(lambda) {
			    if (lambda > 30) return Math.max(0, Math.round(lambda + Math.sqrt(lambda) * this.randomGaussian()));
			    const limit = Simulation._exp(-lambda); let p = 1, k = 0;
			    do { k++; p *= this._random.nextDouble(); } while (p > limit);
			    return k - 1;
			  }
			  randomElement(list) { return list.length === 0 ? null : list[this._random.nextInt(list.length)]; }
			  shuffle(list) { for (let i = list.length - 1; i > 0; i--) { const j = this._random.nextInt(i + 1), t = list[i]; list[i] = list[j]; list[j] = t; } }
			  static _log(x) {
			    if (!(x > 0)) return x === 0 ? -Infinity : NaN;
			    if (x === Infinity) return x;
			    let e = 0, m = x;
			    while (m >= 1.4142135623730951) { m *= 0.5; e++; }
			    while (m < 0.7071067811865476) { m *= 2; e--; }
			    const z = (m - 1) / (m + 1), z2 = z * z; let t = 0;
			    for (let k = 27; k >= 1; k -= 2) t = t * z2 + 1.0 / k;
			    return 2 * z * t + e * 0.6931471805599453;
			  }
			  static _exp(x) {
			    if (x !== x) return x;
			    if (x > 709.7) return Infinity;
			    if (x < -745.2) return 0;
			    const k = Math.floor(x / 0.6931471805599453 + 0.5), r = (x - k * 0.6931471803691238) - k * 1.9082149292705877e-10;
			    let t = 1;
			    for (let n = 20; n >= 1; n--) t = 1 + t * r / n;
			    return t * Math.pow(2, k);
			  }
			  static clamp(v, min, max) { return Math.max(min, Math.min(max, v)); }
			  static lerp(a, b, t) { return a + (b - a) * t; }
			  static mapRange(v, inMin, inMax, outMin, outMax) { return outMin + (v - inMin) * (outMax - outMin) / (inMax - inMin); }
			  static wrapAround(v, size) { return ((v % size) + size) % size; }
			  static sq(v) { return v * v; }
			  static distance(x1, y1, x2, y2) { return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)); }
			  static distance3(x1, y1, z1, x2, y2, z2) { return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1)); }
			  static logistic(x) { return 1 / (1 + Simulation._exp(-x)); }
			  static logisticGrowth(n, r, k) { return r * n * (1 - n / k); }
			  static sum(a) { let s = 0; for (let i = 0; i < a.length; i++) s += a[i]; return s; }
			  static mean(a) { return a.length === 0 ? 0 : Simulation.sum(a) / a.length; }
			  static variance(a) { const n = a.length; if (n < 2) return 0; const m = Simulation.mean(a); let s = 0; for (let i = 0; i < n; i++) s += (a[i] - m) * (a[i] - m); return s / (n - 1); }
			  static sd(a) { return Math.sqrt(Simulation.variance(a)); }
			  static min(a) { let m = Infinity; for (let i = 0; i < a.length; i++) m = Math.min(m, a[i]); return m; }
			  static max(a) { let m = -Infinity; for (let i = 0; i < a.length; i++) m = Math.max(m, a[i]); return m; }
			  static rk4(f, t, y, dt) {
			    const n = y.length, k1 = f(t, y), y2 = new Float64Array(n), y3 = new Float64Array(n), y4 = new Float64Array(n), r = new Float64Array(n);
			    for (let i = 0; i < n; i++) y2[i] = y[i] + dt / 2 * k1[i];
			    const k2 = f(t + dt / 2, y2);
			    for (let i = 0; i < n; i++) y3[i] = y[i] + dt / 2 * k2[i];
			    const k3 = f(t + dt / 2, y3);
			    for (let i = 0; i < n; i++) y4[i] = y[i] + dt * k3[i];
			    const k4 = f(t + dt, y4);
			    for (let i = 0; i < n; i++) r[i] = y[i] + dt / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
			    return r;
			  }
			  chart(a, b, c) { if (!this._sim) return; if (c === undefined) this._sim.chart(a, b); else this._sim.chartLine(a, b, this.t, c); }
			  chartXY(chart, name, x, y) { if (this._sim) this._sim.chartXY(chart, name, x, y); }
			  chartBars(chart, labels, values) { if (this._sim) this._sim.chartBars(chart, Array.from(labels), Array.from(values)); }
			  chartColors(chart, ...colors) { if (this._sim) this._sim.chartColors(chart, colors.length === 1 && Array.isArray(colors[0]) ? Array.from(colors[0]) : colors); }
			  stop() { this._stopped = true; }
			  isStopped() { return !!this._stopped; }
			  record(name, value) { (this._records || (this._records = {}))[name] = value; }
			  _shape(k, p, s, f, t, w, a, z) {
			    /* 3D shapes in the 2D view of a grid: not drawn, a message once (as in Bio7) */
			    if (!this._warned3d && ['box', 'sphere', 'point3', 'path3', 'polygon3', 'pillar', 'cone'].indexOf(k) >= 0 && !(this instanceof AgentModel) && !(this instanceof GeoAgentModel)) {
			      this._warned3d = true; console.warn('The model draws a 3D shape (' + k + '): 3D shapes are drawn only in the 3D view of an agent model (the 2D view of a grid ignores them)');
			    }
			    const d = { k: k, p: p, s: s || null, f: f || null, t: t === undefined ? null : t, w: w === undefined ? 1 : w, a: a || 0 }; if (z) d.z = z; (this._drawing || (this._drawing = [])).push(d); }
			  drawLine(x1, y1, x2, y2, color, width, z) { this._shape('line', [x1, y1, x2, y2], color, null, null, width, 0, z); }
			  drawLine3(x1, y1, z1, x2, y2, z2, color, width) { this._shape('line3', [x1, y1, z1, x2, y2, z2], color, null, null, width); }
			  drawPath(xs, ys, color, width, z) { const p = []; for (let i = 0; i < Math.min(xs.length, ys.length); i++) p.push(xs[i], ys[i]); this._shape('path', p, color, null, null, width, 0, z); }
			  drawPolygon(xs, ys, fill, stroke, z) { const p = []; for (let i = 0; i < Math.min(xs.length, ys.length); i++) p.push(xs[i], ys[i]); this._shape('polygon', p, stroke, fill, null, 1, 0, z); }
			  drawCircle(x, y, radius, fill, stroke, z) { this._shape('circle', [x, y, radius], stroke, fill, null, 1, 0, z); }
			  drawRect(x, y, w, h, fill, stroke, z) { this._shape('rect', [x, y, w, h], stroke, fill, null, 1, 0, z); }
			  drawText(x, y, text, color, size, z) { this._shape('text', [x, y], color, null, String(text), size, 0, z); }
			  drawTextOverlay(x, y, text, color, size) { this._shape('text', [x, y], color, null, String(text), size, 0, 0); }
			  drawRectOverlay(x, y, w, h, fill, stroke) { this._shape('rect', [x, y, w, h], stroke, fill, null, 1, 0, 0); }
			  perspectiveLabel(label) { }
			  perspectiveIcon(icon) { }
			  globeGrid(show) { }
			  globeOutline(color, meters) { }
			  drawImage(name, x, y, size, angle, z) { this._shape('image', [x, y], null, null, name, size, angle, z); }
			  drawPoint3(x, y, z, size, color) { this._shape('point3', [x, y, z, size], null, color, null, 1); }
			  drawPath3(xs, ys, zs, color, width) { const p = []; for (let i = 0; i < Math.min(xs.length, ys.length, zs.length); i++) p.push(xs[i], ys[i], zs[i]); this._shape('path3', p, color, null, null, width); }
			  drawBox(x, y, z, w, h, d, fill, stroke) { this._shape('box', [x, y, z, w, h, d], stroke, fill, null, 1); }
			  drawSphere(x, y, z, radius, fill) { this._shape('sphere', [x, y, z, radius], null, fill, null, 1); }
			  drawPolygon3(xs, ys, zs, fill, stroke) { const p = []; for (let i = 0; i < Math.min(xs.length, ys.length, zs.length); i++) p.push(xs[i], ys[i], zs[i]); this._shape('polygon3', p, stroke, fill, null, 1); }
			  drawPillar(x, y, radius, height, color) { this._shape('pillar', [x, y, radius, height], null, color, null, 1); }
			  drawCone(x, y, radius, height, color) { this._shape('cone', [x, y, radius, height], null, color, null, 1); }
			  loadImage(name, file) {}
			  loadModel(name, file) {}
			  randomBinomial(n, p) { let k = 0; for (let i = 0; i < n; i++) if (this.random() < p) k++; return k; }
			  randomWeighted(w) {
			    let total = 0; for (let i = 0; i < w.length; i++) total += Math.max(0, w[i]);
			    if (total <= 0) return -1;
			    let r = this.random() * total;
			    for (let i = 0; i < w.length; i++) { r -= Math.max(0, w[i]); if (r < 0) return i; }
			    return w.length - 1;
			  }
			  static dot(a, b) { let s = 0; for (let i = 0; i < Math.min(a.length, b.length); i++) s += a[i] * b[i]; return s; }
			  static norm(a) { return Math.sqrt(Simulation.dot(a, a)); }
			  static matMul(a, b) { const n = a.length, m = b[0].length, k = b.length, r = []; for (let i = 0; i < n; i++) { r.push(new Float64Array(m)); for (let j = 0; j < m; j++) { let s = 0; for (let q = 0; q < k; q++) s += a[i][q] * b[q][j]; r[i][j] = s; } } return r; }
			  static matVec(a, v) { const r = new Float64Array(a.length); for (let i = 0; i < a.length; i++) r[i] = Simulation.dot(a[i], v); return r; }
			  static transpose(a) { const r = []; for (let j = 0; j < a[0].length; j++) { r.push(new Float64Array(a.length)); for (let i = 0; i < a.length; i++) r[j][i] = a[i][j]; } return r; }
			  static solve(a, b) {
			    const n = b.length, m = [];
			    for (let i = 0; i < n; i++) { const row = new Float64Array(n + 1); for (let j = 0; j < n; j++) row[j] = a[i][j]; row[n] = b[i]; m.push(row); }
			    for (let c = 0; c < n; c++) {
			      let p = c; for (let r = c + 1; r < n; r++) if (Math.abs(m[r][c]) > Math.abs(m[p][c])) p = r;
			      const tmp = m[c]; m[c] = m[p]; m[p] = tmp;
			      if (m[c][c] === 0) return new Float64Array(n).fill(NaN);
			      for (let r = c + 1; r < n; r++) { const f = m[r][c] / m[c][c]; for (let j = c; j <= n; j++) m[r][j] -= f * m[c][j]; }
			    }
			    const x = new Float64Array(n);
			    for (let i = n - 1; i >= 0; i--) { let s = m[i][n]; for (let j = i + 1; j < n; j++) s -= m[i][j] * x[j]; x[i] = s / m[i][i]; }
			    return x;
			  }
			  static inverse(a) { const n = a.length, r = []; for (let i = 0; i < n; i++) r.push(new Float64Array(n)); for (let j = 0; j < n; j++) { const e = new Float64Array(n); e[j] = 1; const col = Simulation.solve(a, e); for (let i = 0; i < n; i++) r[i][j] = col[i]; } return r; }
			  static determinant(a) {
			    const n = a.length, m = a.map(function (r) { return Float64Array.from(r); }); let det = 1;
			    for (let c = 0; c < n; c++) {
			      let p = c; for (let r = c + 1; r < n; r++) if (Math.abs(m[r][c]) > Math.abs(m[p][c])) p = r;
			      if (m[p][c] === 0) return 0;
			      if (p !== c) { const tmp = m[c]; m[c] = m[p]; m[p] = tmp; det = -det; }
			      det *= m[c][c];
			      for (let r = c + 1; r < n; r++) { const f = m[r][c] / m[c][c]; for (let j = c; j < n; j++) m[r][j] -= f * m[c][j]; }
			    }
			    return det;
			  }
			  static linearRegression(x, y) {
			    const n = Math.min(x.length, y.length); let mx = 0, my = 0;
			    for (let i = 0; i < n; i++) { mx += x[i]; my += y[i]; }
			    mx /= n; my /= n; let sxy = 0, sxx = 0, syy = 0;
			    for (let i = 0; i < n; i++) { sxy += (x[i] - mx) * (y[i] - my); sxx += (x[i] - mx) * (x[i] - mx); syy += (y[i] - my) * (y[i] - my); }
			    const b = sxx === 0 ? 0 : sxy / sxx;
			    return Float64Array.of(my - b * mx, b, sxx === 0 || syy === 0 ? 0 : sxy * sxy / (sxx * syy));
			  }
			  static correlation(x, y) { const r = Simulation.linearRegression(x, y); return r[1] < 0 ? -Math.sqrt(r[2]) : Math.sqrt(r[2]); }
			  static quantile(values, p) {
			    if (values.length === 0) return NaN;
			    const v = Float64Array.from(values).sort(), h = (v.length - 1) * Math.max(0, Math.min(1, p)), lo = Math.floor(h), hi = Math.min(v.length - 1, lo + 1);
			    return v[lo] + (h - lo) * (v[hi] - v[lo]);
			  }
			  static median(values) { return Simulation.quantile(values, 0.5); }
			  static normalPdf(x, mean, sd) { const z = (x - mean) / sd; return Simulation._exp(-0.5 * z * z) / (sd * 2.5066282746310002); }
			  static normalCdf(x, mean, sd) {
			    const z = (x - mean) / (sd * 1.4142135623730951), a = Math.abs(z), t = 1 / (1 + 0.5 * a);
			    const erfc = t * Simulation._exp(-a * a - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * 0.17087277)))))))));
			    return z >= 0 ? 1 - 0.5 * erfc : 0.5 * erfc;
			  }
			  static _corner(x, y) { let h = (Math.imul(x, 374761393) + Math.imul(y, 668265263)) | 0; h = Math.imul(h ^ (h >>> 13), 1274126177); h = h ^ (h >>> 16); return (h & 0xffffff) / 16777216; }
			  static noise(x, y) {
			    const ix = Math.floor(x) | 0, iy = Math.floor(y) | 0, fx = x - ix, fy = y - iy, sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
			    const a = Simulation._corner(ix, iy), b = Simulation._corner(ix + 1, iy), c = Simulation._corner(ix, iy + 1), d = Simulation._corner(ix + 1, iy + 1);
			    return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy;
			  }
			  static fractalNoise(x, y, octaves) {
			    let sum = 0, amplitude = 1, total = 0, f = 1;
			    for (let k = 0; k < octaves; k++) { sum += amplitude * Simulation.noise(x * f + k * 17.3, y * f + k * 11.1); total += amplitude; amplitude *= 0.5; f *= 2; }
			    return sum / total;
			  }
			  static cosDeg(d) { return GeoAgentModel.cosDeg(d); }
			  static sinDeg(d) { return GeoAgentModel.sinDeg(d); }
			  static headingOf(dx, dy) { const h = Math.atan2(dx, dy) * 57.29577951308232; return h < 0 ? h + 360 : h; }
			  /* The shapes of the step and (on top) the agents of a grid */
			  _drawn() { const d = this._drawing || []; return this._gridAgents ? d.concat(Bio7GridAgents.shapes(this.agents)) : d; }
			  _prepare() {}
			  _show(sim) {}
			}
			/* An agent on a grid (as GridAgent of Java) */
			class GridAgent {
			  constructor(x, y) { this.x = x === undefined ? 0.5 : x; this.y = y === undefined ? 0.5 : y; this.heading = 0; this.color = '#e53935'; this.image = null; this.size = 0.8; this.shape = 'circle'; }
			  cellX() { return Math.floor(this.x); }
			  cellY() { return Math.floor(this.y); }
			}
			/* The agents of the grid models (as GridAgents of Java: the same numbers) */
			const Bio7GridAgents = {
			  keep(a, w, h, wrap) { if (wrap) { a.x = ((a.x % w) + w) % w; a.y = ((a.y % h) + h) % h; } else { a.x = Math.max(0, Math.min(w - 1e-9, a.x)); a.y = Math.max(0, Math.min(h - 1e-9, a.y)); } },
			  delta(d, size, wrap) { return wrap ? d - size * Bio7GridAgents.rint(d / size) : d; },
			  rint(v) { const f = Math.floor(v), r = v - f; return r > 0.5 ? f + 1 : r < 0.5 ? f : (f % 2 === 0 ? f : f + 1); },
			  shapes(agents) {
			    const out = [];
			    for (const a of agents) {
			      if (!a) continue;
			      const size = a.size > 0 ? a.size : 0.8;
			      if (a.image) { out.push({ k: 'image', p: [a.x, a.y], s: null, f: null, t: a.image, w: size, a: a.heading }); continue; }
			      const color = a.color || '#e53935', shape = a.shape || 'circle';
			      if (shape === 'arrow') {
			        const r = size / 2, d = [0, 140, 180, 220], f = [1, 1, 0.35, 1], p = [];
			        for (let k = 0; k < 4; k++) p.push(a.x + r * f[k] * Simulation.sinDeg(a.heading + d[k]), a.y - r * f[k] * Simulation.cosDeg(a.heading + d[k]));
			        out.push({ k: 'polygon', p: p, s: '#00000066', f: color, t: null, w: 1, a: 0 });
			      } else if (shape === 'square') out.push({ k: 'rect', p: [a.x - size / 2, a.y - size / 2, size, size], s: '#00000066', f: color, t: null, w: 1, a: 0 });
			      else out.push({ k: 'circle', p: [a.x, a.y, size / 2], s: '#00000066', f: color, t: null, w: 1, a: 0 });
			    }
			    return out;
			  },
			  methods: {
			    addAgent(a) { this.agents.push(a); return a; },
			    removeAgent(a) { const i = this.agents.indexOf(a); if (i >= 0) this.agents.splice(i, 1); },
			    forward(a, d) { a.x += d * Simulation.sinDeg(a.heading); a.y -= d * Simulation.cosDeg(a.heading); Bio7GridAgents.keep(a, this.width, this.height, this.wrap); },
			    moveTo(a, x, y) { a.x = x; a.y = y; Bio7GridAgents.keep(a, this.width, this.height, this.wrap); },
			    face(a, x, y) { const dx = Bio7GridAgents.delta(x - a.x, this.width, this.wrap), dy = Bio7GridAgents.delta(y - a.y, this.height, this.wrap); if (dx !== 0 || dy !== 0) a.heading = Simulation.headingOf(dx, -dy); },
			    agentDistance(a, b) { const dx = Bio7GridAgents.delta(b.x - a.x, this.width, this.wrap), dy = Bio7GridAgents.delta(b.y - a.y, this.height, this.wrap); return Math.sqrt(dx * dx + dy * dy); },
			    agentsAt(x, y) { return this.agents.filter(function (a) { return a.cellX() === x && a.cellY() === y; }); },
			    countAgentsAt(x, y) { return this.agentsAt(x, y).length; },
			    agentsNear(a, r) { const self = this; return this.agents.filter(function (o) { return o !== a && self.agentDistance(a, o) <= r; }); }
			  }
			};
			class GridModel extends Simulation {
			  constructor() { super(); this.width = 0; this.height = 0; this.cells = new Int32Array(0); this.next = new Int32Array(0); this.wrap = true; this._palette = ['#ffffff', '#000000']; this.agents = []; this._gridAgents = true; }
			  _prepare() { this.agents = []; }
			  size(w, h) { this.width = w; this.height = h; this.cells = new Int32Array(w * h); this.next = new Int32Array(w * h); }
			  palette(...colors) { this._palette = colors; }
			  get(x, y) {
			    const w = this.width, h = this.height;
			    if (this.wrap) { x = ((x % w) + w) % w; y = ((y % h) + h) % h; }
			    else if (x < 0 || y < 0 || x >= w || y >= h) return 0;
			    return this.cells[y * w + x];
			  }
			  set(x, y, state) { this.cells[y * this.width + x] = state; }
			  setNext(x, y, state) { this.next[y * this.width + x] = state; }
			  swap() { const t = this.cells; this.cells = this.next; this.next = t; }
			  countNeighbors(x, y, state) {
			    let n = 0;
			    for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) if ((dx !== 0 || dy !== 0) && this.get(x + dx, y + dy) === state) n++;
			    return n;
			  }
			  count4(x, y, state) { return (this.get(x - 1, y) === state ? 1 : 0) + (this.get(x + 1, y) === state ? 1 : 0) + (this.get(x, y - 1) === state ? 1 : 0) + (this.get(x, y + 1) === state ? 1 : 0); }
			  count(state) { let n = 0; for (let i = 0; i < this.cells.length; i++) if (this.cells[i] === state) n++; return n; }
			  _show(sim) { sim.width = this.width; sim.height = this.height; sim.cells = this.cells; sim.palette = this._palette; }
			}
			class ValueGridModel extends Simulation {
			  constructor() { super(); this.width = 0; this.height = 0; this.values = new Float64Array(0); this.next = new Float64Array(0); this.wrap = true; this._min = 0; this._max = 1; this._ramp = ['#440154', '#3b528b', '#21908c', '#5dc963', '#fde725']; this.agents = []; this._gridAgents = true; }
			  _prepare() { this.agents = []; }
			  size(w, h) { this.width = w; this.height = h; this.values = new Float64Array(w * h); this.next = new Float64Array(w * h); }
			  range(min, max) { this._min = min; this._max = max; }
			  colorMap(...colors) { if (colors.length >= 2) this._ramp = colors; }
			  get(x, y) {
			    const w = this.width, h = this.height;
			    if (this.wrap) { x = ((x % w) + w) % w; y = ((y % h) + h) % h; }
			    else if (x < 0 || y < 0 || x >= w || y >= h) return 0;
			    return this.values[y * w + x];
			  }
			  set(x, y, value) { this.values[y * this.width + x] = value; }
			  setNext(x, y, value) { this.next[y * this.width + x] = value; }
			  swap() { const t = this.values; this.values = this.next; this.next = t; }
			  sum4(x, y) { return this.get(x - 1, y) + this.get(x + 1, y) + this.get(x, y - 1) + this.get(x, y + 1); }
			  laplacian(x, y) { return this.sum4(x, y) - 4 * this.get(x, y); }
			  sum8(x, y) { return this.sum4(x, y) + this.get(x - 1, y - 1) + this.get(x + 1, y - 1) + this.get(x - 1, y + 1) + this.get(x + 1, y + 1); }
			  mean() { let s = 0; for (let i = 0; i < this.values.length; i++) s += this.values[i]; return this.values.length === 0 ? 0 : s / this.values.length; }
			  _show(sim) { sim.width = this.width; sim.height = this.height; sim.cells = this.values; sim.palette = null; sim.min = this._min; sim.max = this._max; sim.ramp = this._ramp; }
			}
			Simulation.DEFAULT_SEED = 42;
			ValueGridModel.VIRIDIS = ['#440154', '#3b528b', '#21908c', '#5dc963', '#fde725'];
			Object.assign(GridModel.prototype, Bio7GridAgents.methods);
			Object.assign(ValueGridModel.prototype, Bio7GridAgents.methods);
			class GeoGridModel extends GridModel {
			  constructor() { super(); this.originX = 0; this.originY = 0; this.cellWidth = 1; this.cellHeight = -1; this.cellSizeMeters = 1; this.south = 0; this.west = 0; this.north = 0; this.east = 0; this.crs = ''; this._work = {}; this._shown = null; }
			  _prepare() {
			    this.agents = [];
			    const g = GEO;
			    ['originX', 'originY', 'cellWidth', 'cellHeight', 'cellSizeMeters', 'south', 'west', 'north', 'east'].forEach(k => { this[k] = g[k]; });
			    this.size(g.width, g.height);
			    this._work = {};
			    for (const k in g.layers) this._work[k] = Float64Array.from(g.layers[k]);
			    this._shown = null;
			  }
			  layer(name) { return this._work[name] || null; }
			  showValues(values, min, max, ...colors) { this._shown = { values: values, min: min, max: max, colors: colors.length >= 2 ? colors : ValueGridModel.VIRIDIS }; }
			  showStates() { this._shown = null; }
			  showBackground(name) { }
			  cellShape(shape) { }
			  backgroundNames() { return []; }
			  _inside(v, n) { return v >= 0 && v < n ? v : -1; }
			  colAt(lon, lat) {
			    const M = 20037508.342789244;
			    if (GEO.mode === 'mercator') return this._inside(Math.floor((lon * M / 180 - this.originX) / this.cellWidth), this.width);
			    if (GEO.mode === 'geographic') return this._inside(Math.floor((lon - this.originX) / this.cellWidth), this.width);
			    return this._inside(Math.floor((lon - this.west) / (this.east - this.west) * this.width), this.width);
			  }
			  rowAt(lon, lat) {
			    const M = 20037508.342789244;
			    if (GEO.mode === 'mercator') return this._inside(Math.floor((Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180) * M / 180 - this.originY) / this.cellHeight), this.height);
			    if (GEO.mode === 'geographic') return this._inside(Math.floor((lat - this.originY) / this.cellHeight), this.height);
			    return this._inside(Math.floor((this.north - lat) / (this.north - this.south) * this.height), this.height);
			  }
			  lon(x) {
			    const M = 20037508.342789244;
			    if (GEO.mode === 'mercator') return (this.originX + (x + 0.5) * this.cellWidth) / M * 180;
			    if (GEO.mode === 'geographic') return this.originX + (x + 0.5) * this.cellWidth;
			    return this.west + (x + 0.5) / this.width * (this.east - this.west);
			  }
			  lat(y) {
			    const M = 20037508.342789244;
			    if (GEO.mode === 'mercator') { const my = (this.originY + (y + 0.5) * this.cellHeight) / M * 180; return 180 / Math.PI * (2 * Math.atan(Math.exp(my * Math.PI / 180)) - Math.PI / 2); }
			    if (GEO.mode === 'geographic') return this.originY + (y + 0.5) * this.cellHeight;
			    return this.north - (y + 0.5) / this.height * (this.north - this.south);
			  }
			  value(layer, x, y) { x = Math.max(0, Math.min(this.width - 1, x)); y = Math.max(0, Math.min(this.height - 1, y)); return layer[y * this.width + x]; }
			  gradient(layer, x, y) {
			    const dzdx = (this.value(layer, x + 1, y) - this.value(layer, x - 1, y)) / (2 * this.cellSizeMeters);
			    const dzdy = (this.value(layer, x, y + 1) - this.value(layer, x, y - 1)) / (2 * this.cellSizeMeters);
			    return Math.sqrt(dzdx * dzdx + dzdy * dzdy);
			  }
			  slopeDegrees(layer, x, y) { return Math.atan(this.gradient(layer, x, y)) * 180 / Math.PI; }
			  _show(sim) {
			    if (!this._shown) { super._show(sim); sim.ramp = null; return; }
			    sim.width = this.width; sim.height = this.height; sim.cells = this._shown.values; sim.palette = null; sim.min = this._shown.min; sim.max = this._shown.max; sim.ramp = this._shown.colors;
			  }
			}
			/* The layers of a geospatial model (base64 of little endian doubles) */
			function bio7Doubles(text) { const bin = atob(text), bytes = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i); return new Float64Array(bytes.buffer); }
			class GeoAgent {
			  constructor(lon = 0, lat = 0) { this.lon = lon; this.lat = lat; this.color = null; this.size = 0; this.label = null; this.altitude = 0; this.heading = NaN; this.image = null; this.model = null; this.animation = null; }
			}
			class GeoAgentModel extends Simulation {
			  constructor() { super(); this.agents = []; this.south = -60; this.west = -180; this.north = 75; this.east = 180; this.agentSize = 4; this.agentColor = '#d62728'; this.agentShape = 'point'; this.agentSizeMeters = 300; this.viewPitch = 0; }
			  static cosDeg(deg) {
			    let d = deg % 360;
			    if (d > 180) d -= 360; else if (d < -180) d += 360;
			    d = Math.abs(d);
			    return d <= 90 ? GeoAgentModel.cosLat(d) : -GeoAgentModel.cosLat(180 - d);
			  }
			  static sinDeg(deg) { return GeoAgentModel.cosDeg(90 - deg); }
			  static _compass(dlon, dlat, lat) { const h = Math.atan2(dlon * Math.cos(lat * Math.PI / 180), dlat) * 180 / Math.PI; return h < 0 ? h + 360 : h; }
			  area(south, west, north, east) { this.south = south; this.west = west; this.north = north; this.east = east; }
			  distanceKm(a, b, c, d) {
			    if (c === undefined) return GeoAgentModel.distanceKm(a.lon, a.lat, b.lon, b.lat);
			    return GeoAgentModel.distanceKm(a, b, c, d);
			  }
			  static distanceKm(lon1, lat1, lon2, lat2) {
			    const p1 = lat1 * (Math.PI / 180), p2 = lat2 * (Math.PI / 180), dp = p2 - p1, dl = (lon2 - lon1) * (Math.PI / 180);
			    const h = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
			    return 2 * GeoAgentModel.EARTH_RADIUS * Math.asin(Math.min(1, Math.sqrt(h)));
			  }
			  static cosLat(latDeg) {
			    const x = latDeg * (Math.PI / 180), x2 = x * x, C = GeoAgentModel.COS;
			    let c = C[11];
			    for (let k = 10; k >= 0; k--) c = c * x2 + C[k];
			    return c;
			  }
			  move(a, eastKm, northKm) {
			    a.lat += northKm / 110.574;
			    a.lon += eastKm / (111.320 * GeoAgentModel.cosLat(a.lat));
			    if (a.lon > 180) a.lon -= 360; else if (a.lon < -180) a.lon += 360;
			    a.lat = Math.max(-89.9, Math.min(89.9, a.lat));
			  }
			  moveTowards(a, lon, lat, km) {
			    const eastKm = (lon - a.lon) * 111.320 * GeoAgentModel.cosLat((lat + a.lat) / 2), northKm = (lat - a.lat) * 110.574;
			    const d = Math.sqrt(eastKm * eastKm + northKm * northKm);
			    if (d <= km) { a.lon = lon; a.lat = lat; return true; }
			    this.move(a, eastKm / d * km, northKm / d * km);
			    return false;
			  }
			  neighborsKm(agent, km) { const found = []; for (const o of this.agents) if (o !== agent && this.distanceKm(agent, o) <= km) found.push(o); return found; }
			  _prepare() { this.agents = []; }
			  _show(sim) {
			    const n = this.agents.length, g = { lon: new Array(n), lat: new Array(n), colors: new Array(n), sizes: new Array(n), labels: new Array(n) };
			    for (let i = 0; i < n; i++) {
			      const a = this.agents[i];
			      g.lon[i] = a.lon; g.lat[i] = a.lat; g.colors[i] = a.color || this.agentColor; g.sizes[i] = a.size > 0 ? a.size : this.agentSize; g.labels[i] = a.label;
			      /* An image (and its heading: given or the direction of the last movement) */
			      if (a.image || a.model) { if (!g.images) { g.images = new Array(n).fill(null); g.headings = new Array(n).fill(0); } g.images[i] = a.image; if (a._plon !== undefined && (a._plon !== a.lon || a._plat !== a.lat)) a._h = GeoAgentModel._compass(a.lon - a._plon, a.lat - a._plat, a.lat); g.headings[i] = isNaN(a.heading) ? (a._h || 0) : a.heading; }
			      a._plon = a.lon; a._plat = a.lat;
			    }
			    sim.cells = null; sim.geoAgents = g; sim.mapBounds = [[this.south, this.west], [this.north, this.east]];
			  }
			}
			GeoAgentModel.EARTH_RADIUS = 6371.0088;
			GeoAgentModel.COS = [1.0, -0.5, 0.041666666666666664, -0.001388888888888889, 2.48015873015873e-05, -2.755731922398589e-07, 2.08767569878681e-09, -1.1470745597729725e-11, 4.779477332387385e-14, -1.5619206968586225e-16, 4.110317623312165e-19, -8.896791392450574e-22];
			class Agent {
			  constructor(x = 0, y = 0, z = 0) { this.x = x; this.y = y; this.z = z; this.color = null; this.image = null; this.model = null; this.animation = null; this.heading = NaN; this.size = 0; }
			}
			/* The spatial index of the agents (as SpatialIndex of Java: the same results in the same order) */
			class SpatialIndex {
			  constructor(agents, cellSize) {
			    this.agents = agents.slice(); this.cell = cellSize > 0 ? cellSize : 1; const n = this.agents.length;
			    this.x = new Float64Array(n); this.y = new Float64Array(n); this.z = new Float64Array(n); this.cells = new Map(); this.position = new Map();
			    for (let i = 0; i < n; i++) {
			      const a = this.agents[i]; this.x[i] = a.x; this.y[i] = a.y; this.z[i] = a.z;
			      if (!this.position.has(a)) this.position.set(a, i);
			      const k = this._key(this._c(a.x), this._c(a.y), this._c(a.z)); let list = this.cells.get(k); if (!list) { list = []; this.cells.set(k, list); } list.push(i);
			    }
			  }
			  _c(v) { return Math.floor(v / this.cell); }
			  _key(cx, cy, cz) { return cx + ',' + cy + ',' + cz; }
			  _search(px, py, pz, radius, skip) {
			    const r2 = radius * radius, found = [];
			    const x0 = this._c(px - radius), x1 = this._c(px + radius), y0 = this._c(py - radius), y1 = this._c(py + radius), z0 = this._c(pz - radius), z1 = this._c(pz + radius);
			    for (let cx = x0; cx <= x1; cx++) for (let cy = y0; cy <= y1; cy++) for (let cz = z0; cz <= z1; cz++) {
			      const list = this.cells.get(this._key(cx, cy, cz)); if (!list) continue;
			      for (const i of list) { if (i === skip) continue; const dx = this.x[i] - px, dy = this.y[i] - py, dz = this.z[i] - pz; if (dx * dx + dy * dy + dz * dz <= r2) found.push(i); }
			    }
			    return found.sort(function (a, b) { return a - b; });
			  }
			  _index(agent) { const i = this.position.get(agent); return i === undefined ? -1 : i; }
			  near(px, py, pz, radius) { const ag = this.agents; return this._search(px, py, pz, radius, -1).map(function (i) { return ag[i]; }); }
			  neighbors(agent, radius) { const i = this._index(agent), ag = this.agents; return (i < 0 ? this._search(agent.x, agent.y, agent.z, radius, -1) : this._search(this.x[i], this.y[i], this.z[i], radius, i)).map(function (k) { return ag[k]; }); }
			  nearest(agent, radius) {
			    const self = this._index(agent), px = self < 0 ? agent.x : this.x[self], py = self < 0 ? agent.y : this.y[self], pz = self < 0 ? agent.z : this.z[self];
			    let best = -1, bestD = Number.MAX_VALUE;
			    for (const i of this._search(px, py, pz, radius, self)) { const dx = this.x[i] - px, dy = this.y[i] - py, dz = this.z[i] - pz, d2 = dx * dx + dy * dy + dz * dz; if (d2 < bestD) { bestD = d2; best = i; } }
			    return best < 0 ? null : this.agents[best];
			  }
			  forEachPair(radius, action) { for (let i = 0; i < this.agents.length; i++) for (const j of this._search(this.x[i], this.y[i], this.z[i], radius, i)) if (j > i) action(this.agents[i], this.agents[j]); }
			  size() { return this.agents.length; }
			}
			class AgentModel extends Simulation {
			  constructor() { super(); this.agents = []; this.boxWidth = 100; this.boxHeight = 100; this.boxDepth = 100; this.agentRadius = 0; this.agentColor = '#1f77b4';
			    this.agentShape = 'sphere'; this.floor = 'none'; this.shadows = false; this.followAgent = -1; this._scenery = []; this.linkColor = '#8a94a6'; this.linkWidth = 1; this._links = new Map(); }
			  index(cellSize) { return new SpatialIndex(this.agents, cellSize); }
			  /* The network (as AgentModel of Java): an agent -> its linked agents and the color of the link */
			  link(a, b, color) {
			    if (!a || !b || a === b) return;
			    if (!this._links.has(a)) this._links.set(a, new Map()); if (!this._links.has(b)) this._links.set(b, new Map());
			    this._links.get(a).set(b, color === undefined ? null : color); this._links.get(b).set(a, color === undefined ? null : color);
			  }
			  unlink(a, b) { const la = this._links.get(a), lb = this._links.get(b); if (la) la.delete(b); if (lb) lb.delete(a); }
			  removeLinks(a) { const la = this._links.get(a); this._links.delete(a); if (la) for (const o of la.keys()) { const lo = this._links.get(o); if (lo) lo.delete(a); } }
			  clearLinks() { this._links = new Map(); }
			  linked(a, b) { const la = this._links.get(a); return !!la && la.has(b); }
			  linkNeighbors(a) { const la = this._links.get(a); return la ? Array.from(la.keys()) : []; }
			  degree(a) { const la = this._links.get(a); return la ? la.size : 0; }
			  linkColorOf(a, b) { const la = this._links.get(a); return la && la.has(b) ? la.get(b) : null; }
			  setLinkColor(a, b, color) { if (this.linked(a, b)) this.link(a, b, color); }
			  _indices() { const idx = new Map(); for (let i = 0; i < this.agents.length; i++) if (!idx.has(this.agents[i])) idx.set(this.agents[i], i); return idx; }
			  getLinks() {
			    const idx = this._indices(), out = [];
			    for (let i = 0; i < this.agents.length; i++) { const la = this._links.get(this.agents[i]); if (!la) continue; for (const o of la.keys()) { const j = idx.get(o); if (j !== undefined && j > i) out.push(i, j); } }
			    return out;
			  }
			  linkCount() { return this.getLinks().length / 2; }
			  links() { return this.getLinks(); }
			  cameraFollow(index) { this.followAgent = index < 0 ? -1 : index; }
			  neighborPairs(distance) {
			    const idx = this._indices(), pairs = [];
			    this.index(distance).forEachPair(distance, function (a, b) { const i = idx.get(a), j = idx.get(b); pairs.push([Math.min(i, j), Math.max(i, j)]); });
			    pairs.sort(function (p, q) { return p[0] !== q[0] ? p[0] - q[0] : p[1] - q[1]; });
			    const out = []; for (const p of pairs) out.push(p[0], p[1]); return out;
			  }
			  linkRandom(p) { const n = this.agents.length; for (let i = 0; i < n; i++) for (let j = i + 1; j < n; j++) if (this.random() < p) this.link(this.agents[i], this.agents[j]); }
			  linkSmallWorld(k, p) {
			    const n = this.agents.length, half = Math.max(1, Math.trunc(k / 2)); if (n < 2) return;
			    for (let i = 0; i < n; i++) for (let d = 1; d <= half; d++) this.link(this.agents[i], this.agents[(i + d) % n]);
			    for (let i = 0; i < n; i++) for (let d = 1; d <= half; d++) {
			      const a = this.agents[i], b = this.agents[(i + d) % n];
			      if (this.random() < p && this.linked(a, b)) { for (let t = 0; t < n; t++) { const c = this.agents[this.randomInt(n)]; if (c !== a && !this.linked(a, c)) { this.unlink(a, b); this.link(a, c); break; } } }
			    }
			  }
			  linkScaleFree(m) {
			    const n = this.agents.length; m = Math.max(1, m); const ends = [], start = Math.min(n, m + 1);
			    for (let i = 0; i < start; i++) for (let j = i + 1; j < start; j++) { this.link(this.agents[i], this.agents[j]); ends.push(i, j); }
			    for (let i = start; i < n; i++) {
			      const chosen = [];
			      while (chosen.length < Math.min(m, i)) { const j = ends.length === 0 ? this.randomInt(i) : ends[this.randomInt(ends.length)]; if (chosen.indexOf(j) < 0) chosen.push(j); }
			      for (const j of chosen) { this.link(this.agents[i], this.agents[j]); ends.push(i, j); }
			    }
			  }
			  linkNearby(radius) { const self = this; this.index(radius).forEachPair(radius, function (a, b) { self.link(a, b); }); }
			  components() {
			    const out = [], seen = new Set(), idx = this._indices();
			    for (const a of this.agents) {
			      if (seen.has(a)) continue;
			      const group = [], queue = [a]; seen.add(a);
			      while (queue.length) { const c = queue.shift(); group.push(idx.get(c)); for (const o of this.linkNeighbors(c)) if (idx.has(o) && !seen.has(o)) { seen.add(o); queue.push(o); } }
			      group.sort(function (x, y) { return x - y; }); out.push(group.map((i) => this.agents[i]));
			    }
			    return out;
			  }
			  shortestPath(a, b) {
			    if (a === b) return 0;
			    const dist = new Map([[a, 0]]), queue = [a];
			    while (queue.length) { const c = queue.shift(); for (const o of this.linkNeighbors(c)) if (!dist.has(o)) { dist.set(o, dist.get(c) + 1); if (o === b) return dist.get(o); queue.push(o); } }
			    return -1;
			  }
			  clustering(a) {
			    const nb = this.linkNeighbors(a), k = nb.length; if (k < 2) return 0;
			    let closed = 0; for (let i = 0; i < k; i++) for (let j = i + 1; j < k; j++) if (this.linked(nb[i], nb[j])) closed++;
			    return 2.0 * closed / (k * (k - 1.0));
			  }
			  layoutCircle() {
			    const n = this.agents.length, cx = this.boxWidth / 2, cy = this.boxHeight / 2, r = Math.min(this.boxWidth, this.boxHeight) * 0.42;
			    for (let i = 0; i < n; i++) { const a = 360.0 * i / Math.max(1, n), ag = this.agents[i]; ag.x = cx + r * Simulation.cosDeg(a); ag.y = cy + r * Simulation.sinDeg(a); ag.z = this.boxDepth / 2; }
			  }
			  layoutSpring(iterations) {
			    const n = this.agents.length; if (n === 0) return;
			    const w = this.boxWidth, h = this.boxHeight, k = Math.sqrt(w * h / n), pairs = this.getLinks(), px = new Float64Array(n), py = new Float64Array(n);
			    for (let i = 0; i < n; i++) { px[i] = this.agents[i].x; py[i] = this.agents[i].y; }
			    for (let it = 0; it < iterations; it++) {
			      const temp = Math.max(w, h) * 0.1 * (1 - it / iterations), dx = new Float64Array(n), dy = new Float64Array(n);
			      for (let i = 0; i < n; i++) for (let j = i + 1; j < n; j++) {
			        let ex = px[i] - px[j], ey = py[i] - py[j], d = Math.sqrt(ex * ex + ey * ey);
			        if (d < 1e-9) { ex = 0.01 * k * Simulation.cosDeg(37.0 * (i + j)); ey = 0.01 * k * Simulation.sinDeg(37.0 * (i + j)); d = 0.01 * k; }
			        const f = k * k / d; dx[i] += ex / d * f; dy[i] += ey / d * f; dx[j] -= ex / d * f; dy[j] -= ey / d * f;
			      }
			      for (let p = 0; p + 1 < pairs.length; p += 2) {
			        const i = pairs[p], j = pairs[p + 1], ex = px[i] - px[j], ey = py[i] - py[j], d = Math.max(1e-9, Math.sqrt(ex * ex + ey * ey)), f = d * d / k;
			        dx[i] -= ex / d * f; dy[i] -= ey / d * f; dx[j] += ex / d * f; dy[j] += ey / d * f;
			      }
			      for (let i = 0; i < n; i++) { const d = Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]); if (d > 0) { const s = Math.min(d, temp) / d; px[i] = Math.max(0, Math.min(w, px[i] + dx[i] * s)); py[i] = Math.max(0, Math.min(h, py[i] + dy[i] * s)); } }
			    }
			    for (let i = 0; i < n; i++) { this.agents[i].x = px[i]; this.agents[i].y = py[i]; }
			  }
			  box(w, h, d) { this.boxWidth = w; this.boxHeight = h; this.boxDepth = d; }
			  /* The objects of the environment (3D models which stay until they are removed) */
			  placeModel(model, x, y, z, heading, size, animation) { this._scenery.push([model, x, y, z, heading || 0, size || 0, animation || null]); return this._scenery.length - 1; }
			  removeModel(index) { if (index >= 0 && index < this._scenery.length) this._scenery.splice(index, 1); }
			  clearModels() { this._scenery = []; }
			  distance(a, b) { return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y) + (a.z - b.z) * (a.z - b.z)); }
			  agentDistance(a, b) { return this.distance(a, b); }
			  neighbors(agent, radius) {
			    const found = [], r2 = radius * radius;
			    for (const o of this.agents) { if (o !== agent) { const dx = o.x - agent.x, dy = o.y - agent.y, dz = o.z - agent.z; if (dx * dx + dy * dy + dz * dz <= r2) found.push(o); } }
			    return found;
			  }
			  nearest(agent, radius) {
			    let best = null, r2 = radius * radius;
			    for (const o of this.agents) { if (o !== agent) { const dx = o.x - agent.x, dy = o.y - agent.y, dz = o.z - agent.z, d2 = dx * dx + dy * dy + dz * dz; if (d2 <= r2) { r2 = d2; best = o; } } }
			    return best;
			  }
			  keepInBox(a) { a.x = Math.max(0, Math.min(this.boxWidth, a.x)); a.y = Math.max(0, Math.min(this.boxHeight, a.y)); a.z = Math.max(0, Math.min(this.boxDepth, a.z)); }
			  wrapInBox(a) { a.x = ((a.x % this.boxWidth) + this.boxWidth) % this.boxWidth; a.y = ((a.y % this.boxHeight) + this.boxHeight) % this.boxHeight; a.z = Math.max(0, Math.min(this.boxDepth, a.z)); }
			  _prepare() { this.agents = []; this._scenery = []; this._links = new Map(); }
			  _show(sim) {
			    /* The shape, the floor and the shadows of the model; the objects of the environment */
			    sim.applyModelScene(this.agentShape, this.floor, this.shadows ? 1 : 0);
			    sim.scenery = sim.sceneryOf(this._scenery.map(function (p) { return p[0]; }), [].concat.apply([], this._scenery.map(function (p) { return p.slice(1, 6); })), this._scenery.map(function (p) { return p[6]; }));
			    const n = this.agents.length, xyz = new Array(n * 3), colors = new Array(n * 3), dirs = new Array(n * 3);
			    let own = false;
			    const base = this.agentColor;
			    for (let i = 0; i < n; i++) {
			      const a = this.agents[i];
			      xyz[i * 3] = a.x; xyz[i * 3 + 1] = a.y; xyz[i * 3 + 2] = a.z;
			      /* The direction of the movement (for cones and gliders): the last move of the agent */
			      if (a._px !== undefined) { const dx = a.x - a._px, dy = a.y - a._py, dz = a.z - a._pz; if (dx * dx + dy * dy + dz * dz > 1e-18 && Math.abs(dx) + Math.abs(dy) < Math.max(this.boxWidth, this.boxHeight) / 2) { a._dx = dx; a._dy = dy; a._dz = dz; } }
			      a._px = a.x; a._py = a.y; a._pz = a.z;
			      dirs[i * 3] = a._dx || 0; dirs[i * 3 + 1] = a._dy || 0; dirs[i * 3 + 2] = a._dz || 0;
			      const c = a.color || base;
			      if (a.color) own = true;
			      colors[i * 3] = parseInt(c.substr(1, 2), 16) / 255; colors[i * 3 + 1] = parseInt(c.substr(3, 2), 16) / 255; colors[i * 3 + 2] = parseInt(c.substr(5, 2), 16) / 255;
			    }
			    const size = Math.max(this.boxWidth, this.boxHeight, this.boxDepth);
			    sim.box = [this.boxWidth, this.boxHeight, this.boxDepth];
			    sim.points = { xyz: xyz, dirs: dirs, colors: own ? colors : null, color: base, radius: this.agentRadius > 0 ? this.agentRadius : size * 0.01 };
			    /* Images and 3D models (glTF) of the agents */
			    if (this.agents.some(function (a) { return a.image || a.model; })) {
			      const p = sim.points; p.images = []; p.models = []; p.animations = []; p.headings = []; p.sizes = [];
			      for (const a of this.agents) { p.images.push(a.image); p.models.push(a.model); p.animations.push(a.animation); p.headings.push(isNaN(a.heading) ? NaN : a.heading); p.sizes.push(a.size); }
			    }
			    /* The links of the network */
			    const pairs = this.getLinks(), lc = [];
			    for (let k = 0; k < pairs.length; k += 2) lc.push(this.linkColorOf(this.agents[pairs[k]], this.agents[pairs[k + 1]]) || '');
			    sim.links = pairs.length ? { pairs: pairs, colors: lc, color: this.linkColor || '', width: this.linkWidth } : null;
			  }
			}
			""";

	/**
	 * Translates the model and creates the page.
	 *
	 * @param source    the Java source of the model
	 * @param unitName  the name of the file (e.g., "Life.java")
	 * @param classpath the classpath with the API of Bio7
	 * @param options   the options of the page (null: defaults)
	 */
	public static Result create(String source, String unitName, String[] classpath, Options options) {
		Result result = new Result();
		JavaToJs translator = new JavaToJs();
		String classes = translator.translate(source, unitName, classpath);
		result.errors.addAll(translator.getErrors());
		/* The errors in the order of the lines */
		result.errors.sort(java.util.Comparator.comparingInt(SimulationExport::lineOf));
		result.warnings.addAll(translator.getWarnings());
		result.modelName = translator.getModelName();
		result.kind = translator.getKind();
		if (classes == null) {
			return result;
		}
		if (options == null) {
			options = new Options();
		}
		String name = translator.getModelName();
		if ("GeoGridModel".equals(translator.getKind()) && options.geo == null) {
			result.errors.add("The data of the geospatial model is missing: compile and run the model in Bio7 first (load() reads the layers with GDAL, the page gets the loaded data)");
			return result;
		}
		List<JavaToJs.ParamInfo> params = translator.getParams();
		StringBuilder js = new StringBuilder(assets(options)).append(RUNTIME).append("\n/* ----- The model (translated from ").append(unitName).append(") ----- */\n").append(classes);
		/* The settings of the page -> the fields of the model */
		if (options.geo != null) {
			js.append(geoData(options.geo));
		}
		js.append("const PARAMS = [");
		for (int i = 0; i < params.size(); i++) {
			JavaToJs.ParamInfo p = params.get(i);
			js.append(i > 0 ? ", " : "").append("{ name: ").append(JavaToJs.quote(p.name)).append(", field: ").append(JavaToJs.quote(p.jsName)).append(", kind: ").append(JavaToJs.quote(p.kind))
					.append(", restart: ").append(p.restart).append(" }");
		}
		js.append("];\n");
		js.append("""
				let model = null;
				function applyParams(sim, all) {
				  for (const p of PARAMS) {
				    if (!all && p.restart) continue;
				    let v = sim.p[p.name];
				    if (p.kind === 'int') v = Math.trunc(Number(v)); else if (p.kind === 'double') v = Number(v); else if (p.kind === 'boolean') v = !!v; else v = String(v);
				    model[p.field] = v;
				  }
				}
				function setup(sim) {
				""");
		js.append("  if (!model) model = new ").append(name).append("();\n");
		js.append("""
				  /* The seed of the field Seed of the page (the same seed: the same run as in Bio7) */
				  const seed = sim.seed === undefined ? 42 : sim.seed;
				  model._sim = sim; model.t = 0; model._random = new SimRandom(seed); model._seed = seed; model._stopped = false;
				  sim.clearChart();
				  model._prepare();
				  applyParams(sim, true);
				  model._drawing = [];
				  model.init();
				  model._show(sim);
				  sim.drawing = model._drawn();
				}
				function step(sim) {
				  applyParams(sim, false);
				  model._drawing = [];
				  model._stopped = false;
				  model.step();
				  model.t++;
				  model._show(sim);
				  sim.drawing = model._drawn();
				  /* stop() of the model: the page stops */
				  if (model._stopped) return 'stop';
				}
				""");
		/* The input of the view: input(sim, ev) calls click(x, y), drag(x, y) or key(key) of the model */
		if (!translator.getInputs().isEmpty()) {
			js.append("""
					function input(sim, ev) {
					  applyParams(sim, false);
					  if (ev.type === 'key') { if (typeof model.key === 'function') model.key(ev.key); }
					  else if (typeof model[ev.type] === 'function') model[ev.type](ev.x, ev.y);
					  model._show(sim);
					  sim.drawing = model._drawn();
					}
					""");
		}
		/* The actions: buttons of the page */
		for (JavaToJs.ActionInfo act : translator.getActions()) {
			js.append("function action_").append(act.jsName).append("(sim) { applyParams(sim, false); model.").append(act.jsName).append("(); model._show(sim); sim.drawing = model._drawn(); }\n");
		}
		result.javaScript = js.toString();
		String title = options.title != null && !options.title.isBlank() ? options.title : words(name);
		String description = options.description != null ? options.description : translator.getDescription();
		result.title = title;
		result.description = description;
		HtmlSimulation page = new HtmlSimulation(title).description(escape(description)).view(!translator.getKind().equals("AgentModel") ? HtmlSimulation.View.GRID_2D : HtmlSimulation.View.SCENE_3D)
				.speed(options.speed).seed(options.seed).autoplay(options.autoplay).theme(options.theme).layout(options.layout).accent(options.accent)
				.scene(options.background, options.lighting, options.shape, options.floor, options.shadows).autoRotate(options.rotate)
				.inputs(translator.getInputs());
		List<String> restart = new ArrayList<>();
		/* The groups as the settings of Bio7: the settings of a group, then its actions; the actions without a group at the end */
		java.util.Map<String, List<Object>> groups = new java.util.LinkedHashMap<>();
		for (JavaToJs.ParamInfo p : params) {
			groups.computeIfAbsent(p.group, k -> new ArrayList<>()).add(p);
		}
		for (JavaToJs.ActionInfo act : translator.getActions()) {
			groups.computeIfAbsent(act.group.isEmpty() ? "\u0000" : act.group, k -> new ArrayList<>()).add(act);
		}
		boolean headings = groups.keySet().stream().anyMatch(g -> !g.isEmpty() && !g.equals("\u0000"));
		if (!groups.isEmpty() && !headings) {
			page.heading("Settings");
		}
		for (java.util.Map.Entry<String, List<Object>> g : groups.entrySet()) {
			if (!g.getKey().isEmpty() && !g.getKey().equals("\u0000")) {
				page.heading(escape(g.getKey()));
			} else if (g.getKey().equals("\u0000") && headings) {
				page.heading("Actions");
			}
			for (Object o : g.getValue()) {
				if (o instanceof JavaToJs.ActionInfo act) {
					page.button(escape(act.label), "action_" + act.jsName);
					continue;
				}
				JavaToJs.ParamInfo p = (JavaToJs.ParamInfo) o;
				control(page, p);
				if (p.restart) {
					restart.add(p.name);
				}
			}
		}
		if (!restart.isEmpty()) {
			page.restartOn(restart.toArray(new String[0]));
		}
		if ("GeoAgentModel".equals(translator.getKind())) {
			/* The map shows the area of the model (set in init) */
			page.map(Double.NaN, Double.NaN, Double.NaN, Double.NaN, options.basemap, 1, "");
		}
		if (options.geo != null) {
			java.util.Map<String, Object> g = options.geo;
			page.map(((Number) g.get("south")).doubleValue(), ((Number) g.get("west")).doubleValue(), ((Number) g.get("north")).doubleValue(), ((Number) g.get("east")).doubleValue(), options.basemap,
					options.mapOpacity, String.valueOf(g.get("sources")));
		}
		if (!translator.getChartNames().isEmpty()) {
			page.chart(escape(String.join(", ", translator.getChartNames())));
		}
		page.code(result.javaScript);
		result.page = page;
		return result;
	}

	/** The images and the 3D models as JavaScript (const ASSETS: data URIs of the images, base64 of the models). */
	static String assets(Options options) {
		StringBuilder b = new StringBuilder("const ASSETS = { images: {");
		boolean first = true;
		for (java.util.Map.Entry<String, String> e : options.images.entrySet()) {
			byte[] data = bytes(e.getValue());
			if (data == null) {
				continue;
			}
			String name = e.getValue().toLowerCase(java.util.Locale.ROOT);
			String mime = name.endsWith(".png") ? "image/png" : name.endsWith(".gif") ? "image/gif" : name.endsWith(".svg") ? "image/svg+xml" : name.endsWith(".webp") ? "image/webp" : "image/jpeg";
			b.append(first ? "" : ", ").append(JavaToJs.quote(e.getKey())).append(": \"data:").append(mime).append(";base64,").append(java.util.Base64.getEncoder().encodeToString(data)).append('"');
			first = false;
		}
		b.append("}, models: {");
		first = true;
		for (java.util.Map.Entry<String, String> e : options.models.entrySet()) {
			/* OBJ and STL as binary glTF (the page reads only glTF) */
			byte[] data = ModelConverter.converted(e.getValue()) ? converted(e.getValue()) : bytes(e.getValue());
			if (data == null) {
				continue;
			}
			b.append(first ? "" : ", ").append(JavaToJs.quote(e.getKey())).append(": \"").append(java.util.Base64.getEncoder().encodeToString(data)).append('"');
			first = false;
		}
		return b.append("} };\n").toString();
	}

	private static byte[] converted(String file) {
		try {
			return ModelConverter.glb(new java.io.File(file));
		} catch (IOException | RuntimeException e) {
			System.err.println("The 3D model " + file + " could not be converted for the page: " + e.getMessage());
			return null;
		}
	}

	private static byte[] bytes(String file) {
		try {
			return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(file));
		} catch (IOException | RuntimeException e) {
			System.err.println("The file " + file + " could not be embedded in the page: " + e.getMessage());
			return null;
		}
	}

	/* The data of a geospatial model as JavaScript (const GEO) */
	@SuppressWarnings("unchecked")
	static String geoData(java.util.Map<String, Object> g) {
		StringBuilder b = new StringBuilder("const GEO = {");
		for (String k : new String[] { "width", "height", "originX", "originY", "cellWidth", "cellHeight", "cellSizeMeters", "south", "west", "north", "east" }) {
			Object v = g.get(k);
			b.append(k).append(": ").append(v instanceof Double d ? Double.toString(d) : String.valueOf(v)).append(", ");
		}
		b.append("mode: ").append(JavaToJs.quote(String.valueOf(g.get("mode")))).append(", layers: {");
		java.util.Map<String, double[]> layers = (java.util.Map<String, double[]>) g.get("layers");
		boolean first = true;
		for (java.util.Map.Entry<String, double[]> e : layers.entrySet()) {
			java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(e.getValue().length * 8).order(java.nio.ByteOrder.LITTLE_ENDIAN);
			for (double d : e.getValue()) {
				buf.putDouble(d);
			}
			b.append(first ? "" : ", ").append(JavaToJs.quote(e.getKey())).append(": bio7Doubles(\"").append(java.util.Base64.getEncoder().encodeToString(buf.array())).append("\")");
			first = false;
		}
		return b.append("} };\n").toString();
	}

	/* The control of a setting */
	static void control(HtmlSimulation page, JavaToJs.ParamInfo p) {
		String label = escape(p.label);
		switch (p.kind) {
		case "boolean" -> page.checkbox(p.name, label, Boolean.TRUE.equals(p.value));
		case "string" -> {
			if (p.options.length > 0) {
				page.choice(p.name, label, p.options, String.valueOf(p.value));
			} else {
				page.text(p.name, label, String.valueOf(p.value));
			}
		}
		default -> {
			double v = ((Number) p.value).doubleValue();
			if (!Double.isNaN(p.min) && !Double.isNaN(p.max)) {
				double st = !Double.isNaN(p.step) ? p.step : p.kind.equals("int") ? 1 : (p.max - p.min) / 100;
				page.slider(p.name, label, p.min, p.max, st, v);
			} else {
				page.number(p.name, label, v);
			}
		}
		}
	}

	/** Translates the model and writes the page (throws an exception with the errors). */
	public static Result export(String source, String unitName, String[] classpath, Options options, String file) throws IOException {
		Result result = create(source, unitName, classpath, options);
		if (result.ok()) {
			result.page.export(file);
		}
		return result;
	}

	private static int lineOf(String message) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("^Line (\\d+)").matcher(message);
		return m.find() ? Integer.parseInt(m.group(1)) : 0;
	}

	/* "GameOfLife" -> "Game Of Life" */
	static String words(String name) {
		return name.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
	}

	static String escape(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
