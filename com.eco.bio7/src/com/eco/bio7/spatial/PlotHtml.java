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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates an HTML page with WebGL of a 3D plot (see Plot3D.exportHtml): the
 * geometry, the axes, the title and the legends as data and a small WebGL
 * viewer (no dependencies). The plot can be rotated with the mouse and zoomed
 * with the mouse wheel.
 */
final class PlotHtml {

	private PlotHtml() {
	}

	static String create(Plot3D plot) {
		String title = plot.title();
		return page("{\"frames\":[" + frame(plot) + "]}", title);
	}

	/* The page with the data of the frames ({"frames": [...]} or {"data": compressed frames, ...}) */
	static String page(String data, String title) {
		String page = TEMPLATE.replace("/*DATA*/", data);
		return page.replace("<title></title>", "<title>" + escape(title != null ? title.split("\n")[0] : "3D Plot") + "</title>");
	}

	/* The data of the plot (a frame of the page) as JSON */
	static String frame(Plot3D plot) {
		PlotGeometry g = plot.geometry();
		StringBuilder json = new StringBuilder(1 << 16);
		json.append('{');
		double[] box = plot.boxSize();
		json.append("\"box\":[").append(num(box[0])).append(',').append(num(box[1])).append(',').append(num(box[2])).append("],");
		/* The triangles (opaque and transparent separated) */
		float[] t = g.triangles.toArray();
		StringBuilder opaque = new StringBuilder(), transparent = new StringBuilder();
		for (int i = 0; i < t.length; i += 30) {
			StringBuilder target = t[i + 9] < 1 ? transparent : opaque;
			for (int k = i; k < i + 30; k++) {
				if (target.length() > 0) {
					target.append(',');
				}
				target.append(num(t[k]));
			}
		}
		json.append("\"tri\":[").append(opaque).append("],");
		json.append("\"triT\":[").append(transparent).append("],");
		json.append("\"lines\":[").append(floats(g.lines.toArray())).append("],");
		json.append("\"points\":[").append(floats(g.points.toArray())).append("],");
		/* The labels of the data (e.g., the percentages of a pie chart) */
		json.append("\"labels\":[");
		for (int i = 0; i < g.labels.size(); i++) {
			PlotGeometry.Label l = g.labels.get(i);
			json.append(i > 0 ? "," : "").append("{\"p\":[").append(num(l.x())).append(',').append(num(l.y())).append(',').append(num(l.z()))
					.append("],\"t\":").append(string(html(l.text()))).append('}');
		}
		json.append("],");
		boolean axes = !plot.isPie();
		json.append("\"axes\":").append(axes).append(',');
		boolean[] grids = plot.grids();
		json.append("\"grids\":[").append(grids[0]).append(',').append(grids[1]).append(',').append(grids[2]).append("],");
		/* The ticks of the axes: the positions in the box and the numbers */
		json.append("\"ticks\":[");
		double[][] ranges = plot.ranges();
		for (int k = 0; k < 3; k++) {
			json.append(k > 0 ? "," : "").append(axes ? ticks(ranges[k], box[k], plot.logAxes()[k], plot.numberFormats()[k], plot.categories()[k])
					: "{\"major\":[],\"minor\":[]}");
		}
		json.append("],");
		String[] axisTitles = plot.axisTitlesOrDefault();
		json.append("\"axisTitles\":[").append(string(html(axisTitles[0]))).append(',').append(string(html(axisTitles[1]))).append(',')
				.append(string(html(axisTitles[2]))).append("],");
		String title = plot.title();
		json.append("\"title\":").append(string(title != null ? htmlLines(title) : "")).append(',');
		/* The legend */
		Plot3D.LegendData legend = plot.legendData();
		json.append("\"legend\":[");
		for (int i = 0; i < legend.labels().length; i++) {
			json.append(i > 0 ? "," : "").append("{\"t\":").append(string(html(legend.labels()[i]))).append(",\"c\":")
					.append(string(hex(legend.colors()[i]))).append('}');
		}
		json.append("],");
		/* The color bar */
		Plot3D.ColorBarData bar = plot.colorBarData();
		if (bar != null) {
			json.append("\"bar\":{\"title\":").append(string(html(bar.title()))).append(",\"colors\":[");
			for (int i = 0; i < bar.colors().length; i++) {
				json.append(i > 0 ? "," : "").append(string(hex(bar.colors()[i])));
			}
			json.append("],\"ticks\":").append(barTicks(bar)).append('}');
		} else {
			json.append("\"bar\":null");
		}
		/* The colors of the texts (title, numbers, axis titles, legend; null = default) */
		int[] colors = plot.textColorsOf();
		json.append(",\"colors\":[");
		for (int i = 0; i < 4; i++) {
			json.append(i > 0 ? "," : "").append(colors[i] >= 0 ? string(hex(colors[i])) : "null");
		}
		json.append(']');
		/* The font families of the texts (title, numbers, axis titles, legend; null = default) */
		String[] fonts = plot.fontsOf();
		json.append(",\"fonts\":[");
		for (int i = 0; i < 4; i++) {
			json.append(i > 0 ? "," : "").append(fonts[i] != null && !fonts[i].isBlank() ? string(fonts[i]) : "null");
		}
		json.append(']');
		json.append('}');
		return json.toString();
	}

	/* The major ticks (positions and numbers) and the minor ticks of a log axis */
	private static String ticks(double[] range, double size, boolean log, String format, String[] categories) {
		double lo = range[0], hi = range[1];
		if (categories != null) {
			/* The names of the categories at 0, 1, 2, ... */
			StringBuilder c = new StringBuilder("{\"major\":[");
			for (int i = 0; i < categories.length; i++) {
				c.append(i > 0 ? "," : "").append('[').append(num((i - lo) / (hi - lo) * size)).append(',').append(string(html(categories[i]))).append(']');
			}
			return c.append("],\"minor\":[]}").toString();
		}
		double step = SpatialScene.niceStep((hi - lo) / 6);
		if (log && hi - lo >= 1) {
			step = Math.max(1, Math.round(step));
		}
		StringBuilder s = new StringBuilder("{\"major\":[");
		int count = 0;
		for (double v = Math.ceil(lo / step - 1e-9) * step; v <= hi + step * 1e-6 && count < 200; v += step, count++) {
			double value = Math.round(v / step) * step;
			String text = log ? SpatialScene.formatLogValue(value, step, format) : SpatialScene.formatPlotValue(value, step, format);
			s.append(count > 0 ? "," : "").append("[").append(num((value - lo) / (hi - lo) * size)).append(',').append(string(html(text)))
					.append(']');
		}
		s.append("],\"minor\":[");
		if (log && hi - lo <= 12 && step == 1) {
			List<String> minor = new ArrayList<>();
			for (int n = (int) Math.floor(lo); n < hi; n++) {
				for (int k = 2; k < 10; k++) {
					double v = n + Math.log10(k);
					if (v > lo && v < hi) {
						minor.add(num((v - lo) / (hi - lo) * size));
					}
				}
			}
			s.append(String.join(",", minor));
		}
		return s.append("]}").toString();
	}

	private static String barTicks(Plot3D.ColorBarData bar) {
		double step = SpatialScene.niceStep((bar.max() - bar.min()) / 5);
		if (bar.log() && bar.max() - bar.min() >= 1) {
			step = Math.max(1, Math.round(step));
		}
		StringBuilder s = new StringBuilder("[");
		int count = 0;
		for (double v = Math.ceil(bar.min() / step - 1e-9) * step; v <= bar.max() + step * 1e-6 && count < 50; v += step, count++) {
			double value = Math.round(v / step) * step;
			String text = bar.log() ? SpatialScene.formatLogValue(value, step, bar.format())
					: SpatialScene.formatPlotValue(value, step, bar.format());
			s.append(count > 0 ? "," : "").append('[').append(num((value - bar.min()) / (bar.max() - bar.min()))).append(',')
					.append(string(html(text))).append(']');
		}
		return s.append(']').toString();
	}

	/* A text with the math markup as HTML (superscripts and subscripts) */
	static String html(String text) {
		if (text == null) {
			return "";
		}
		StringBuilder s = new StringBuilder();
		for (SpatialScene.Segment segment : SpatialScene.parseMath(text)) {
			String e = escape(segment.text());
			s.append(segment.level() > 0 ? "<sup>" + e + "</sup>" : segment.level() < 0 ? "<sub>" + e + "</sub>" : e);
		}
		return s.toString();
	}

	private static String htmlLines(String text) {
		StringBuilder s = new StringBuilder();
		String[] lines = text.split("\n");
		for (int i = 0; i < lines.length; i++) {
			s.append(i > 0 ? "<br>" : "").append(html(lines[i]));
		}
		return s.toString();
	}

	static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/* A JSON string */
	static String string(String text) {
		StringBuilder s = new StringBuilder("\"");
		for (char c : text.toCharArray()) {
			switch (c) {
			case '"' -> s.append("\\\"");
			case '\\' -> s.append("\\\\");
			case '\n' -> s.append("\\n");
			case '<' -> s.append("\\u003c");
			default -> {
				if (c < 0x20) {
					s.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
				} else {
					s.append(c);
				}
			}
			}
		}
		return s.append('"').toString();
	}

	private static String hex(int rgb) {
		return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
	}

	private static String floats(float[] values) {
		StringBuilder s = new StringBuilder(values.length * 6);
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				s.append(',');
			}
			s.append(num(values[i]));
		}
		return s.toString();
	}

	/* A number with at most 4 decimals */
	static String num(double v) {
		if (!Double.isFinite(v)) {
			return "0";
		}
		double r = Math.round(v * 10000) / 10000.0;
		if (r == Math.rint(r) && Math.abs(r) < 1e15) {
			return Long.toString((long) r);
		}
		return Double.toString(r);
	}

	/* The WebGL viewer (the data replaces the DATA comment) */
	private static final String TEMPLATE = """
			<!DOCTYPE html>
			<html>
			<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<title></title>
			<style>
			html, body { margin: 0; height: 100%; overflow: hidden; background: #ffffff; font-family: Verdana, Arial, sans-serif; }
			#c { width: 100%; height: 100%; display: block; cursor: grab; }
			#overlay { position: absolute; left: 0; top: 0; width: 100%; height: 100%; pointer-events: none; overflow: hidden; }
			.num { position: absolute; transform: translate(-50%, -50%); white-space: nowrap; font-size: 11px; color: #222; }
			.axis { position: absolute; transform: translate(-50%, -50%); white-space: nowrap; font-size: 13px; font-weight: bold; color: #222; }
			.data { position: absolute; transform: translate(-50%, -50%); white-space: nowrap; font-size: 13px; font-weight: bold; color: #111; }
			#title { position: absolute; top: 10px; left: 0; width: 100%; text-align: center; font-weight: bold; font-size: 20px; color: #111; pointer-events: none; }
			.panel { position: absolute; background: rgba(255, 255, 255, 0.85); border: 1px solid #999; padding: 8px 10px; font-size: 12px; color: #222; }
			.panel b { display: block; margin-bottom: 4px; }
			.sw { display: inline-block; width: 12px; height: 12px; margin-right: 6px; vertical-align: -1px; border: 1px solid #666; }
			#hint { position: absolute; bottom: 6px; left: 10px; font-size: 11px; color: #888; }
			#player { position: absolute; bottom: 10px; left: 50%; transform: translateX(-50%); background: rgba(255, 255, 255, 0.92); border: 1px solid #aaa; border-radius: 6px; padding: 6px 10px; display: flex; gap: 10px; align-items: center; font-size: 12px; }
			#player button { font-family: inherit; font-size: 12px; min-width: 64px; padding: 3px 8px; }
			#player input[type=range] { width: 320px; }
			sup, sub { font-size: 70%; }
			</style>
			</head>
			<body>
			<canvas id="c"></canvas>
			<div id="overlay"></div>
			<div id="title"></div>
			<div id="legend" class="panel" style="display:none; left:10px"></div>
			<div id="bar" class="panel" style="display:none; right:10px"></div>
			<div id="hint">Drag: rotate &middot; Wheel: zoom &middot; Double click: reset</div>
			<div id="player" style="display:none"><button id="play">Play</button><input type="range" id="frame" min="0" max="0" value="0"><span id="frameLabel"></span>
			<label>Speed <select id="fps"><option value="2">2/s</option><option value="5">5/s</option><option value="10">10/s</option><option value="20">20/s</option><option value="30">30/s</option><option value="60">60/s</option></select></label>
			<label><input type="checkbox" id="loop" checked> Loop</label></div>
			<script>
			const PLOT = /*DATA*/;
			let D = null;
			const canvas = document.getElementById('c');
			const gl = canvas.getContext('webgl', { antialias: true });
			const overlay = document.getElementById('overlay');
			if (!gl) { document.body.innerHTML = '<p style="padding:20px">WebGL is not available in this browser.</p>'; }

			function shader(type, src) {
			  const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s);
			  if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) { throw gl.getShaderInfoLog(s); }
			  return s;
			}
			function program(vs, fs) {
			  const p = gl.createProgram();
			  gl.attachShader(p, shader(gl.VERTEX_SHADER, vs)); gl.attachShader(p, shader(gl.FRAGMENT_SHADER, fs));
			  gl.linkProgram(p); return p;
			}
			const triProgram = program(
			  'attribute vec3 p; attribute vec3 n; attribute vec4 c; uniform mat4 mvp; uniform mat3 nm; varying vec4 vc; varying vec3 vn;' +
			  'void main() { gl_Position = mvp * vec4(p, 1.0); vn = nm * n; vc = c; }',
			  'precision mediump float; varying vec4 vc; varying vec3 vn;' +
			  'void main() { vec3 n = normalize(vn); float l = 0.32 + 0.58 * abs(n.z) + 0.18 * abs(n.y); gl_FragColor = vec4(min(vc.rgb * l, 1.0), vc.a); }');
			const lineProgram = program(
			  'attribute vec3 p; attribute vec4 c; uniform mat4 mvp; varying vec4 vc; void main() { gl_Position = mvp * vec4(p, 1.0); vc = c; }',
			  'precision mediump float; varying vec4 vc; void main() { gl_FragColor = vc; }');
			const pointProgram = program(
			  'attribute vec3 p; attribute vec4 c; attribute float r; uniform mat4 mvp; uniform float ps; varying vec4 vc;' +
			  'void main() { gl_Position = mvp * vec4(p, 1.0); gl_PointSize = max(2.0, 2.0 * r * ps / gl_Position.w); vc = c; }',
			  'precision mediump float; varying vec4 vc;' +
			  'void main() { vec2 q = gl_PointCoord * 2.0 - 1.0; float d = dot(q, q); if (d > 1.0) discard;' +
			  ' float z = sqrt(1.0 - d); float l = 0.3 + 0.6 * z + 0.25 * pow(z, 12.0); gl_FragColor = vec4(min(vc.rgb * l, 1.0), vc.a); }');

			function buffer(data) {
			  const b = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, b);
			  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(data), gl.STATIC_DRAW); return { b: b, n: data.length };
			}
			/* Only the attributes of the program are enabled (the buffers of a former frame are deleted) */
			const maxAttributes = gl ? gl.getParameter(gl.MAX_VERTEX_ATTRIBS) : 0;
			function resetAttributes() { for (let i = 0; i < maxAttributes; i++) gl.disableVertexAttribArray(i); }
			function attribute(prog, name, size, stride, offset) {
			  const loc = gl.getAttribLocation(prog, name); if (loc < 0) return;
			  gl.enableVertexAttribArray(loc); gl.vertexAttribPointer(loc, size, gl.FLOAT, false, stride * 4, offset * 4);
			}
			/* The camera: rotation around the center (z up) */
			let start = null, theta = 0, phi = 0, dist = 1;
			function eye() {
			  return [center[0] + dist * Math.cos(phi) * Math.cos(theta), center[1] + dist * Math.cos(phi) * Math.sin(theta), center[2] + dist * Math.sin(phi)];
			}
			function perspective(fovy, aspect, near, far) {
			  const f = 1 / Math.tan(fovy / 2), nf = 1 / (near - far);
			  return [f / aspect, 0, 0, 0, 0, f, 0, 0, 0, 0, (far + near) * nf, -1, 0, 0, 2 * far * near * nf, 0];
			}
			function lookAt(e, c, up) {
			  let z = [e[0] - c[0], e[1] - c[1], e[2] - c[2]]; let l = Math.hypot(z[0], z[1], z[2]); z = z.map(function (a) { return a / l; });
			  let x = [up[1] * z[2] - up[2] * z[1], up[2] * z[0] - up[0] * z[2], up[0] * z[1] - up[1] * z[0]];
			  l = Math.hypot(x[0], x[1], x[2]); x = x.map(function (a) { return a / l; });
			  const y = [z[1] * x[2] - z[2] * x[1], z[2] * x[0] - z[0] * x[2], z[0] * x[1] - z[1] * x[0]];
			  return [x[0], y[0], z[0], 0, x[1], y[1], z[1], 0, x[2], y[2], z[2], 0,
			    -(x[0] * e[0] + x[1] * e[1] + x[2] * e[2]), -(y[0] * e[0] + y[1] * e[1] + y[2] * e[2]), -(z[0] * e[0] + z[1] * e[1] + z[2] * e[2]), 1];
			}
			function multiply(a, b) {
			  const r = new Array(16);
			  for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) {
			    let s = 0; for (let k = 0; k < 4; k++) s += a[k * 4 + j] * b[i * 4 + k]; r[i * 4 + j] = s;
			  }
			  return r;
			}
			let mvp, view, W = 1, H = 1;
			function project(p) {
			  const x = mvp[0] * p[0] + mvp[4] * p[1] + mvp[8] * p[2] + mvp[12], y = mvp[1] * p[0] + mvp[5] * p[1] + mvp[9] * p[2] + mvp[13];
			  const w = mvp[3] * p[0] + mvp[7] * p[1] + mvp[11] * p[2] + mvp[15];
			  if (w <= 0) return null;
			  return [(x / w + 1) / 2 * W, (1 - y / w) / 2 * H];
			}

			/* The data of a frame: the buffers, the grids, the labels, the title, the legend and the color bar */
			let triBuffer = null, triTBuffer = null, lineBuffer = null, pointBuffer = null, sides = null, box = [10, 10, 7.5], center = [5, 5, 3.75], size = 10;
			let numberLabels = [[], [], []], axisLabels = [], dataLabels = [], labelPool = [];
			const gridColor = [0.45, 0.45, 0.45, 0.8], minorColor = [0.45, 0.45, 0.45, 0.25];
			const dynamic = gl.createBuffer();
			function free(b) { if (b) gl.deleteBuffer(b.b); }
			function sideLines(axis, value) {
			  const u = (axis + 1) % 3, v = (axis + 2) % 3, out = [];
			  function add(along, across, s, color) {
			    const a = [0, 0, 0], b = [0, 0, 0];
			    a[axis] = value; b[axis] = value; a[along] = s; b[along] = s; a[across] = 0; b[across] = box[across];
			    out.push(a[0], a[1], a[2], color[0], color[1], color[2], color[3], b[0], b[1], b[2], color[0], color[1], color[2], color[3]);
			  }
			  [[u, v], [v, u]].forEach(function (pair) {
			    const along = pair[0], across = pair[1];
			    add(along, across, 0, gridColor); add(along, across, box[along], gridColor);
			    D.ticks[along].major.forEach(function (t) { add(along, across, t[0], gridColor); });
			    D.ticks[along].minor.forEach(function (t) { add(along, across, t, minorColor); });
			  });
			  return buffer(out);
			}
			function element(cls, html, color) { const d = document.createElement('div'); d.className = cls; d.innerHTML = html; if (color) d.style.color = color; overlay.appendChild(d); return d; }
			function place(e, p) { const s = project(p); if (!s) { e.style.display = 'none'; return; } e.style.display = ''; e.style.left = s[0] + 'px'; e.style.top = s[1] + 'px'; }
			const titleElement = document.getElementById('title'), legendElement = document.getElementById('legend'), barElement = document.getElementById('bar');
			function build(F) {
			  D = F;
			  const colors = D.colors || [null, null, null, null];
			  [triBuffer, triTBuffer, lineBuffer, pointBuffer].forEach(free);
			  if (sides) sides.forEach(function (pair) { free(pair[0]); free(pair[1]); });
			  triBuffer = buffer(D.tri); triTBuffer = buffer(D.triT); lineBuffer = buffer(D.lines); pointBuffer = buffer(D.points);
			  box = D.box; center = [box[0] / 2, box[1] / 2, box[2] / 2]; size = Math.max(box[0], box[1], box[2]);
			  sides = [[sideLines(0, 0), sideLines(0, box[0])], [sideLines(1, 0), sideLines(1, box[1])], [sideLines(2, 0), sideLines(2, box[2])]];
			  /* The camera of the first frame (the camera is kept for the next frames) */
			  if (!start) { start = { theta: -0.95, phi: 0.42, dist: size * 3.4 }; theta = start.theta; phi = start.phi; dist = start.dist; }
			  /* The labels: the elements of the former frame are reused (no flicker), new elements are hidden until they are placed */
			  const pool = labelPool; labelPool = []; let used = 0;
			  /* The font families (CSS, the default font as the fallback) */
			  const fonts = (D.fonts || [null, null, null, null]).map(function (f) { return f ? '"' + f.replace(/"/g, '') + '", Verdana, Arial, sans-serif' : ''; });
			  const fontOf = { num: fonts[1], axis: fonts[2], data: fonts[1] };
			  function label(cls, html, color) {
			    let d = used < pool.length ? pool[used] : null; used++;
			    if (!d) { d = document.createElement('div'); d.style.display = 'none'; overlay.appendChild(d); }
			    if (d.className !== cls) d.className = cls;
			    if (d.innerHTML !== html) d.innerHTML = html;
			    d.style.color = color || '';
			    d.style.fontFamily = fontOf[cls] || '';
			    labelPool.push(d); return d;
			  }
			  numberLabels = [[], [], []]; axisLabels = [];
			  if (D.axes) {
			    for (let k = 0; k < 3; k++) {
			      D.ticks[k].major.forEach(function (t) { numberLabels[k].push(label('num', t[1], colors[1])); });
			      axisLabels.push(label('axis', D.axisTitles[k], colors[2]));
			    }
			  }
			  dataLabels = D.labels.map(function (l) { return { p: l.p, e: label('data', l.t, colors[1]) }; });
			  /* The elements which are not used any more */
			  for (let i = used; i < pool.length; i++) pool[i].remove();
			  /* The title, the legend and the color bar only if they changed */
			  function set(e, html) { if (e.innerHTML !== html) e.innerHTML = html; }
			  set(titleElement, D.title); titleElement.style.color = colors[0] || ''; titleElement.style.fontFamily = fonts[0];
			  legendElement.style.fontFamily = fonts[3]; barElement.style.fontFamily = fonts[3];
			  legendElement.style.display = D.legend.length > 0 ? '' : 'none'; legendElement.style.color = colors[3] || '';
			  set(legendElement, D.legend.map(function (l) { return '<div><span class="sw" style="background:' + l.c + '"></span>' + l.t + '</div>'; }).join(''));
			  barElement.style.display = D.bar ? '' : 'none'; barElement.style.color = colors[3] || '';
			  if (D.bar) {
			    const h = 220;
			    let html = '<b>' + D.bar.title + '</b><div style="position:relative; height:' + h + 'px; width:90px">' +
			      '<div style="position:absolute; left:0; top:0; width:16px; height:' + h + 'px; border:1px solid #555; background:linear-gradient(to top,' + D.bar.colors.join(',') + ')"></div>';
			    D.bar.ticks.forEach(function (t) {
			      const y = (1 - t[0]) * h;
			      html += '<div style="position:absolute; left:17px; top:' + y + 'px; width:5px; border-top:1px solid #555"></div>' +
			        '<div style="position:absolute; left:25px; top:' + (y - 8) + 'px; white-space:nowrap">' + t[1] + '</div>';
			    });
			    set(barElement, html + '</div>');
			  }
			}
			function layoutPanels() {
			  const top = titleElement.offsetHeight + 20;
			  legendElement.style.top = top + 'px'; barElement.style.top = top + 'px';
			}

			function drawLines(b, count) {
			  resetAttributes(); gl.useProgram(lineProgram); gl.bindBuffer(gl.ARRAY_BUFFER, b);
			  attribute(lineProgram, 'p', 3, 7, 0); attribute(lineProgram, 'c', 4, 7, 3);
			  gl.uniformMatrix4fv(gl.getUniformLocation(lineProgram, 'mvp'), false, new Float32Array(mvp));
			  gl.drawArrays(gl.LINES, 0, count / 7);
			}
			function drawTriangles(t) {
			  if (t.n === 0) return;
			  resetAttributes(); gl.useProgram(triProgram); gl.bindBuffer(gl.ARRAY_BUFFER, t.b);
			  attribute(triProgram, 'p', 3, 10, 0); attribute(triProgram, 'n', 3, 10, 3); attribute(triProgram, 'c', 4, 10, 6);
			  gl.uniformMatrix4fv(gl.getUniformLocation(triProgram, 'mvp'), false, new Float32Array(mvp));
			  const nm = [view[0], view[1], view[2], view[4], view[5], view[6], view[8], view[9], view[10]];
			  gl.uniformMatrix3fv(gl.getUniformLocation(triProgram, 'nm'), false, new Float32Array(nm));
			  gl.drawArrays(gl.TRIANGLES, 0, t.n / 10);
			}

			function draw() {
			  const ratio = window.devicePixelRatio || 1;
			  W = canvas.clientWidth; H = canvas.clientHeight;
			  if (canvas.width !== Math.round(W * ratio) || canvas.height !== Math.round(H * ratio)) { canvas.width = Math.round(W * ratio); canvas.height = Math.round(H * ratio); }
			  gl.viewport(0, 0, canvas.width, canvas.height);
			  gl.clearColor(1, 1, 1, 1); gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
			  gl.enable(gl.DEPTH_TEST); gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
			  const e = eye(), fov = 30 * Math.PI / 180;
			  view = lookAt(e, center, [0, 0, 1]);
			  mvp = multiply(perspective(fov, W / H, dist * 0.02, dist * 20), view);
			  /* The sides behind the data and the front edges of the axes */
			  const back = [0, 0, 0], front = [0, 0, 0];
			  for (let k = 0; k < 3; k++) { const minIsBack = e[k] > center[k]; back[k] = minIsBack ? 0 : box[k]; front[k] = minIsBack ? box[k] : 0; }
			  if (D.axes) {
			    const fixed = [2, 1, 0];
			    for (let g = 0; g < 3; g++) {
			      if (!D.grids[g]) continue;
			      const k = fixed[g], side = sides[k][back[k] === 0 ? 0 : 1]; drawLines(side.b, side.n);
			    }
			  }
			  drawTriangles(triBuffer);
			  if (pointBuffer.n > 0) {
			    resetAttributes(); gl.useProgram(pointProgram); gl.bindBuffer(gl.ARRAY_BUFFER, pointBuffer.b);
			    attribute(pointProgram, 'p', 3, 8, 0); attribute(pointProgram, 'c', 4, 8, 3); attribute(pointProgram, 'r', 1, 8, 7);
			    gl.uniformMatrix4fv(gl.getUniformLocation(pointProgram, 'mvp'), false, new Float32Array(mvp));
			    gl.uniform1f(gl.getUniformLocation(pointProgram, 'ps'), canvas.height / (2 * Math.tan(fov / 2)));
			    gl.drawArrays(gl.POINTS, 0, pointBuffer.n / 8);
			  }
			  if (lineBuffer.n > 0) drawLines(lineBuffer.b, lineBuffer.n);
			  gl.depthMask(false); drawTriangles(triTBuffer); gl.depthMask(true);
			  if (D.axes) {
			    /* The z axis at the vertical front edge which is left on the screen */
			    const zA = [front[0], back[1]], zB = [back[0], front[1]];
			    const sa = project([zA[0], zA[1], back[2]]), sb = project([zB[0], zB[1], back[2]]);
			    const zEdge = !sa || !sb || sa[0] <= sb[0] ? zA : zB;
			    const startP = [[0, front[1], back[2]], [front[0], 0, back[2]], [zEdge[0], zEdge[1], 0]];
			    const out = [[0, Math.sign(front[1] - back[1]), 0], [Math.sign(front[0] - back[0]), 0, 0],
			      [Math.sign(zEdge[0] - center[0]), Math.sign(zEdge[1] - center[1]), 0]];
			    const tick = size * 0.02, lines = [], c = [0.15, 0.15, 0.15, 1];
			    for (let k = 0; k < 3; k++) {
			      const a = startP[k].slice(), b = startP[k].slice(); b[k] = box[k];
			      lines.push(a[0], a[1], a[2], c[0], c[1], c[2], c[3], b[0], b[1], b[2], c[0], c[1], c[2], c[3]);
			      D.ticks[k].major.forEach(function (t, i) {
			        const p = startP[k].slice(); p[k] = t[0];
			        lines.push(p[0], p[1], p[2], c[0], c[1], c[2], c[3], p[0] + out[k][0] * tick, p[1] + out[k][1] * tick, p[2] + out[k][2] * tick, c[0], c[1], c[2], c[3]);
			        place(numberLabels[k][i], [p[0] + out[k][0] * tick * 4, p[1] + out[k][1] * tick * 4, p[2] + out[k][2] * tick * 4]);
			      });
			      const m = startP[k].slice(); m[k] = box[k] / 2;
			      place(axisLabels[k], [m[0] + out[k][0] * tick * 10, m[1] + out[k][1] * tick * 10, m[2] + out[k][2] * tick * 10]);
			    }
			    gl.bindBuffer(gl.ARRAY_BUFFER, dynamic); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(lines), gl.DYNAMIC_DRAW);
			    drawLines(dynamic, lines.length);
			  }
			  dataLabels.forEach(function (l) { place(l.e, l.p); });
			  layoutPanels();
			}

			let pending = false;
			function redraw() { if (!pending) { pending = true; requestAnimationFrame(function () { pending = false; draw(); }); } }
			let drag = null;
			canvas.addEventListener('mousedown', function (ev) { drag = [ev.clientX, ev.clientY]; canvas.style.cursor = 'grabbing'; });
			window.addEventListener('mouseup', function () { drag = null; canvas.style.cursor = 'grab'; });
			window.addEventListener('mousemove', function (ev) {
			  if (!drag) return;
			  theta -= (ev.clientX - drag[0]) * 0.01; phi = Math.max(-1.55, Math.min(1.55, phi + (ev.clientY - drag[1]) * 0.01));
			  drag = [ev.clientX, ev.clientY]; redraw();
			});
			canvas.addEventListener('wheel', function (ev) { ev.preventDefault(); dist *= ev.deltaY > 0 ? 1.1 : 1 / 1.1; redraw(); }, { passive: false });
			canvas.addEventListener('dblclick', function () { theta = start.theta; phi = start.phi; dist = start.dist; redraw(); });
			canvas.addEventListener('touchstart', function (ev) { if (ev.touches.length === 1) drag = [ev.touches[0].clientX, ev.touches[0].clientY]; }, { passive: true });
			canvas.addEventListener('touchmove', function (ev) {
			  if (!drag || ev.touches.length !== 1) return; ev.preventDefault();
			  theta -= (ev.touches[0].clientX - drag[0]) * 0.01; phi = Math.max(-1.55, Math.min(1.55, phi + (ev.touches[0].clientY - drag[1]) * 0.01));
			  drag = [ev.touches[0].clientX, ev.touches[0].clientY]; redraw();
			}, { passive: false });
			canvas.addEventListener('touchend', function () { drag = null; });
			window.addEventListener('resize', redraw);

			/* The frames (a recording, see PlotRecorder): Play/Pause, the timeline, the speed and a loop */
			let FRAMES = [], frameIndex = 0, playing = false, last = 0, acc = 0;
			async function load() {
			  if (!PLOT.data) return PLOT.frames;
			  const s = atob(PLOT.data), z = new Uint8Array(s.length);
			  for (let i = 0; i < s.length; i++) z[i] = s.charCodeAt(i);
			  const text = await new Response(new Blob([z]).stream().pipeThrough(new DecompressionStream('deflate'))).text();
			  return JSON.parse(text);
			}
			function showFrame(k) {
			  /* Built and drawn at once (no frame without the labels) */
			  frameIndex = k; build(FRAMES[k]); draw();
			  document.getElementById('frame').value = k; document.getElementById('frameLabel').textContent = (k + 1) + ' / ' + FRAMES.length;
			}
			function tick(now) {
			  if (!playing) return;
			  acc += Math.min(0.25, (now - last) / 1000) * parseFloat(document.getElementById('fps').value); last = now;
			  if (acc >= 1) {
			    acc = 0;
			    let k = frameIndex + 1;
			    if (k >= FRAMES.length) { if (document.getElementById('loop').checked) k = 0; else { playing = false; updatePlay(); return; } }
			    showFrame(k);
			  }
			  requestAnimationFrame(tick);
			}
			function updatePlay() { document.getElementById('play').textContent = playing ? 'Pause' : 'Play'; }
			load().then(function (frames) {
			  FRAMES = frames; build(FRAMES[0]); draw();
			  if (FRAMES.length > 1) {
			    const player = document.getElementById('player'); player.style.display = '';
			    const slider = document.getElementById('frame'); slider.max = FRAMES.length - 1;
			    slider.addEventListener('input', function () { showFrame(parseInt(slider.value, 10)); });
			    const fps = document.getElementById('fps'), rate = String(PLOT.fps || 10);
			    if (!Array.from(fps.options).some(function (o) { return o.value === rate; })) { const o = document.createElement('option'); o.value = rate; o.textContent = rate + '/s'; fps.appendChild(o); }
			    fps.value = rate; document.getElementById('loop').checked = PLOT.loop !== false;
			    document.getElementById('play').addEventListener('click', function () { playing = !playing; updatePlay(); last = performance.now(); acc = 0; if (playing) requestAnimationFrame(tick); });
			    showFrame(0);
			    if (PLOT.autoplay) { playing = true; updatePlay(); last = performance.now(); requestAnimationFrame(tick); }
			  }
			}).catch(function (e) { document.body.insertAdjacentHTML('beforeend', '<p style="position:absolute;top:40px;left:10px;color:#b00">' + e + '</p>'); });
			</script>
			</body>
			</html>
			""";
}
