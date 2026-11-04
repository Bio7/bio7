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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Exports a 3D plot (see Plot3D) to glTF binary (*.glb), e.g., for Blender, and
 * creates a Blender Python script which imports the file and adds the texts
 * (title, axis titles, numbers), a camera and the lights.
 * <p>
 * Each layer is a mesh with vertex colors (COLOR_0) and a material (the color
 * of the layer as the base color). Blender doesn't render points and lines:
 * the points are spheres and the lines are tubes. The box, the grids behind
 * the data (seen from the camera of the script) and the axes are the object
 * "Frame". The plot has the size of the plot box (the largest side 10, z up);
 * glTF is y up (the Blender importer converts it to z up again).
 */
final class PlotGltf {

	/* The camera of the Blender script (like the start view of the HTML page) */
	static final double THETA = -0.95, PHI = 0.42, DISTANCE = 3.4;

	private PlotGltf() {
	}

	/* ----------------------------- The meshes ----------------------------- */

	/* A triangle mesh with normals and colors */
	private static final class Mesh {
		final String name;
		final PlotGeometry.FloatList positions = new PlotGeometry.FloatList(), normals = new PlotGeometry.FloatList(),
				colors = new PlotGeometry.FloatList();
		int[] indices = new int[1024];
		int indexCount;
		int vertexCount;
		float[] baseColor = { 1, 1, 1, 1 };

		Mesh(String name) {
			this.name = name;
		}

		int vertex(double x, double y, double z, double nx, double ny, double nz, float[] c) {
			positions.add((float) x, (float) y, (float) z);
			normals.add((float) nx, (float) ny, (float) nz);
			colors.add(c[0], c[1], c[2], c[3]);
			return vertexCount++;
		}

		void triangle(int a, int b, int c) {
			if (indexCount + 3 > indices.length) {
				indices = Arrays.copyOf(indices, indices.length * 2);
			}
			indices[indexCount++] = a;
			indices[indexCount++] = b;
			indices[indexCount++] = c;
		}

		boolean isEmpty() {
			return indexCount == 0;
		}

		/* The same data with another name */
		Mesh rename(String newName) {
			Mesh m = new Mesh(newName);
			m.positions.add(positions.toArray());
			m.normals.add(normals.toArray());
			m.colors.add(colors.toArray());
			m.indices = Arrays.copyOf(indices, indexCount);
			m.indexCount = indexCount;
			m.vertexCount = vertexCount;
			m.baseColor = baseColor.clone();
			return m;
		}
	}

	/* An icosphere (subdivided once): the unit vertices and the triangles */
	private static final double[][] SPHERE_VERTICES;
	private static final int[][] SPHERE_TRIANGLES;
	static {
		double t = (1 + Math.sqrt(5)) / 2;
		List<double[]> v = new ArrayList<>(Arrays.asList(new double[][] { { -1, t, 0 }, { 1, t, 0 }, { -1, -t, 0 }, { 1, -t, 0 }, { 0, -1, t },
				{ 0, 1, t }, { 0, -1, -t }, { 0, 1, -t }, { t, 0, -1 }, { t, 0, 1 }, { -t, 0, -1 }, { -t, 0, 1 } }));
		int[][] f = { { 0, 11, 5 }, { 0, 5, 1 }, { 0, 1, 7 }, { 0, 7, 10 }, { 0, 10, 11 }, { 1, 5, 9 }, { 5, 11, 4 }, { 11, 10, 2 }, { 10, 7, 6 },
				{ 7, 1, 8 }, { 3, 9, 4 }, { 3, 4, 2 }, { 3, 2, 6 }, { 3, 6, 8 }, { 3, 8, 9 }, { 4, 9, 5 }, { 2, 4, 11 }, { 6, 2, 10 }, { 8, 6, 7 },
				{ 9, 8, 1 } };
		java.util.Map<Long, Integer> middle = new java.util.HashMap<>();
		List<int[]> faces = new ArrayList<>();
		for (int[] face : f) {
			int[] m = new int[3];
			for (int k = 0; k < 3; k++) {
				int a = face[k], b = face[(k + 1) % 3];
				long key = Math.min(a, b) * 1000L + Math.max(a, b);
				Integer index = middle.get(key);
				if (index == null) {
					double[] pa = v.get(a), pb = v.get(b);
					v.add(new double[] { (pa[0] + pb[0]) / 2, (pa[1] + pb[1]) / 2, (pa[2] + pb[2]) / 2 });
					index = v.size() - 1;
					middle.put(key, index);
				}
				m[k] = index;
			}
			faces.add(new int[] { face[0], m[0], m[2] });
			faces.add(new int[] { face[1], m[1], m[0] });
			faces.add(new int[] { face[2], m[2], m[1] });
			faces.add(new int[] { m[0], m[1], m[2] });
		}
		SPHERE_VERTICES = new double[v.size()][];
		for (int i = 0; i < v.size(); i++) {
			double[] p = v.get(i);
			double l = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
			SPHERE_VERTICES[i] = new double[] { p[0] / l, p[1] / l, p[2] / l };
		}
		SPHERE_TRIANGLES = faces.toArray(new int[0][]);
	}

	private static void sphere(Mesh m, double x, double y, double z, double r, float[] c) {
		int first = m.vertexCount;
		for (double[] v : SPHERE_VERTICES) {
			m.vertex(x + r * v[0], y + r * v[1], z + r * v[2], v[0], v[1], v[2], c);
		}
		for (int[] t : SPHERE_TRIANGLES) {
			m.triangle(first + t[0], first + t[1], first + t[2]);
		}
	}

	/* A tube (a prism with 6 sides) from a to b with the colors of the ends */
	private static void tube(Mesh m, double[] a, double[] b, double r, float[] ca, float[] cb) {
		double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 1e-9) {
			return;
		}
		double[] d = { dx / length, dy / length, dz / length };
		/* Two vectors perpendicular to the direction */
		double[] h = Math.abs(d[2]) < 0.9 ? new double[] { 0, 0, 1 } : new double[] { 1, 0, 0 };
		double[] u = cross(d, h);
		double ul = Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
		u = new double[] { u[0] / ul, u[1] / ul, u[2] / ul };
		double[] w = cross(d, u);
		int sides = 6, first = m.vertexCount;
		for (int k = 0; k < sides; k++) {
			double angle = 2 * Math.PI * k / sides, cos = Math.cos(angle), sin = Math.sin(angle);
			double nx = u[0] * cos + w[0] * sin, ny = u[1] * cos + w[1] * sin, nz = u[2] * cos + w[2] * sin;
			m.vertex(a[0] + r * nx, a[1] + r * ny, a[2] + r * nz, nx, ny, nz, ca);
			m.vertex(b[0] + r * nx, b[1] + r * ny, b[2] + r * nz, nx, ny, nz, cb);
		}
		for (int k = 0; k < sides; k++) {
			int a0 = first + 2 * k, b0 = a0 + 1, a1 = first + 2 * ((k + 1) % sides), b1 = a1 + 1;
			m.triangle(a0, a1, b1);
			m.triangle(a0, b1, b0);
		}
	}

	private static double[] cross(double[] u, double[] v) {
		return new double[] { u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0] };
	}

	/* The mesh of a layer: the triangles, the points (spheres) and the lines (tubes) */
	private static Mesh layerMesh(String name, PlotGeometry g, double lineRadius) {
		Mesh m = new Mesh(name);
		float[] t = g.triangles.toArray();
		for (int i = 0; i < t.length; i += 10) {
			int v = m.vertex(t[i], t[i + 1], t[i + 2], t[i + 3], t[i + 4], t[i + 5], new float[] { t[i + 6], t[i + 7], t[i + 8], t[i + 9] });
			if ((v + 1) % 3 == 0) {
				m.triangle(v - 2, v - 1, v);
			}
		}
		float[] p = g.points.toArray();
		for (int i = 0; i < p.length; i += 8) {
			sphere(m, p[i], p[i + 1], p[i + 2], p[i + 7], new float[] { p[i + 3], p[i + 4], p[i + 5], p[i + 6] });
		}
		float[] l = g.lines.toArray();
		for (int i = 0; i + 13 < l.length; i += 14) {
			/* Transparent lines (e.g., the grid lines of a surface) are thinner */
			double r = l[i + 6] < 0.99 ? lineRadius * 0.45 : lineRadius;
			tube(m, new double[] { l[i], l[i + 1], l[i + 2] }, new double[] { l[i + 7], l[i + 8], l[i + 9] }, r,
					new float[] { l[i + 3], l[i + 4], l[i + 5], l[i + 6] }, new float[] { l[i + 10], l[i + 11], l[i + 12], l[i + 13] });
		}
		/* The base color of the material: the mean color (one color for a layer with one color) */
		float[] c = { 0, 0, 0, 0 };
		float[] colors = m.colors.toArray();
		for (int i = 0; i < colors.length; i += 4) {
			for (int k = 0; k < 4; k++) {
				c[k] += colors[i + k];
			}
		}
		int n = Math.max(1, colors.length / 4);
		m.baseColor = new float[] { c[0] / n, c[1] / n, c[2] / n, c[3] / n };
		return m;
	}

	/* ----------------------------- The frame and the texts ----------------------------- */

	/* The camera position of the Blender script */
	static double[] eye(double[] box) {
		double size = Math.max(box[0], Math.max(box[1], box[2]));
		double d = size * DISTANCE;
		return new double[] { box[0] / 2 + d * Math.cos(PHI) * Math.cos(THETA), box[1] / 2 + d * Math.cos(PHI) * Math.sin(THETA),
				box[2] / 2 + d * Math.sin(PHI) };
	}

	/* A text of the plot (in the coordinates of the plot) */
	record Text(double x, double y, double z, String text, double size, boolean bold, int kind) {
		/* The kinds (the slots of the text colors of the plot) */
		static final int TITLE = 0, NUMBER = 1, AXIS = 2;
	}

	/* The ticks of an axis: the positions in the box and the numbers (plain text) */
	private static List<Object[]> ticks(double[] range, double size, boolean log, String format, String[] categories) {
		List<Object[]> ticks = new ArrayList<>();
		double lo = range[0], hi = range[1];
		if (categories != null) {
			for (int i = 0; i < categories.length; i++) {
				ticks.add(new Object[] { (i - lo) / (hi - lo) * size, plain(categories[i]) });
			}
			return ticks;
		}
		double step = SpatialScene.niceStep((hi - lo) / 6);
		if (log && hi - lo >= 1) {
			step = Math.max(1, Math.round(step));
		}
		int count = 0;
		for (double v = Math.ceil(lo / step - 1e-9) * step; v <= hi + step * 1e-6 && count < 200; v += step, count++) {
			double value = Math.round(v / step) * step;
			String text = log ? SpatialScene.formatLogValue(value, step, format) : SpatialScene.formatPlotValue(value, step, format);
			ticks.add(new Object[] { (value - lo) / (hi - lo) * size, plain(text) });
		}
		return ticks;
	}

	/*
	 * The frame (box, grids behind the data, axes with ticks) as tubes and the
	 * texts at the front edges (seen from the camera of the script, like the plot
	 * view of Bio7).
	 */
	private static Mesh frame(Plot3D plot, List<Text> texts) {
		double[] box = plot.boxSize();
		double[] e = eye(box);
		double[] back = new double[3], front = new double[3];
		for (int k = 0; k < 3; k++) {
			boolean minIsBack = e[k] > box[k] / 2;
			back[k] = minIsBack ? 0 : box[k];
			front[k] = minIsBack ? box[k] : 0;
		}
		List<List<Object[]>> ticks = new ArrayList<>();
		for (int k = 0; k < 3; k++) {
			ticks.add(ticks(plot.ranges()[k], box[k], plot.logAxes()[k], plot.numberFormats()[k], plot.categories()[k]));
		}
		Mesh m = new Mesh("Frame");
		float[] grid = { 0.55f, 0.55f, 0.55f, 1 }, axis = { 0.12f, 0.12f, 0.12f, 1 };
		double size = Math.max(box[0], Math.max(box[1], box[2]));
		double gridRadius = size * 0.0018, axisRadius = size * 0.0035;
		/* The grids of the sides behind the data (fixed axis 2 = floor, 1, 0) */
		boolean[] grids = plot.grids();
		int[] fixed = { 2, 1, 0 };
		for (int g = 0; g < 3; g++) {
			if (!grids[g]) {
				continue;
			}
			int k = fixed[g], u = (k + 1) % 3, v = (k + 2) % 3;
			for (int[] pair : new int[][] { { u, v }, { v, u } }) {
				int along = pair[0], across = pair[1];
				List<Double> lines = new ArrayList<>(List.of(0.0, box[along]));
				for (Object[] t : ticks.get(along)) {
					lines.add((Double) t[0]);
				}
				for (double s : lines) {
					double[] a = new double[3], b = new double[3];
					a[k] = back[k];
					b[k] = back[k];
					a[along] = s;
					b[along] = s;
					a[across] = 0;
					b[across] = box[across];
					tube(m, a, b, gridRadius, grid, grid);
				}
			}
		}
		/* The z axis at the vertical front edge which is left from the camera */
		double[] zA = { front[0], back[1] }, zB = { back[0], front[1] };
		double[] zEdge = screenX(e, box, zA[0], zA[1], back[2]) <= screenX(e, box, zB[0], zB[1], back[2]) ? zA : zB;
		double[][] start = { { 0, front[1], back[2] }, { front[0], 0, back[2] }, { zEdge[0], zEdge[1], 0 } };
		double[][] out = { { 0, Math.signum(front[1] - back[1]), 0 }, { Math.signum(front[0] - back[0]), 0, 0 },
				{ Math.signum(zEdge[0] - box[0] / 2), Math.signum(zEdge[1] - box[1] / 2), 0 } };
		double tick = size * 0.02;
		String[] titles = plot.axisTitlesOrDefault();
		for (int k = 0; k < 3; k++) {
			double[] a = start[k].clone(), b = start[k].clone();
			b[k] = box[k];
			tube(m, a, b, axisRadius, axis, axis);
			for (Object[] t : ticks.get(k)) {
				double[] p = start[k].clone();
				p[k] = (Double) t[0];
				double[] q = { p[0] + out[k][0] * tick, p[1] + out[k][1] * tick, p[2] + out[k][2] * tick };
				tube(m, p, q, axisRadius, axis, axis);
				texts.add(new Text(p[0] + out[k][0] * tick * 4, p[1] + out[k][1] * tick * 4, p[2] + out[k][2] * tick * 4, (String) t[1],
						size * 0.028, false, Text.NUMBER));
			}
			double[] c = start[k].clone();
			c[k] = box[k] / 2;
			if (!titles[k].isBlank())
			texts.add(new Text(c[0] + out[k][0] * tick * 10, c[1] + out[k][1] * tick * 10, c[2] + out[k][2] * tick * 10, plain(titles[k]),
					size * 0.038, true, Text.AXIS));
		}
		return m;
	}

	/* The horizontal screen position of a point (seen from the camera to the center of the box) */
	private static double screenX(double[] e, double[] box, double x, double y, double z) {
		double[] f = { box[0] / 2 - e[0], box[1] / 2 - e[1], box[2] / 2 - e[2] };
		double[] right = cross(f, new double[] { 0, 0, 1 });
		double[] d = { x - e[0], y - e[1], z - e[2] };
		double depth = d[0] * f[0] + d[1] * f[1] + d[2] * f[2];
		return (d[0] * right[0] + d[1] * right[1] + d[2] * right[2]) / Math.max(1e-9, depth);
	}

	/* A text with the math markup as plain text (x^2, x^(n+1), N0) */
	static String plain(String text) {
		if (text == null) {
			return "";
		}
		StringBuilder s = new StringBuilder();
		for (SpatialScene.Segment segment : SpatialScene.parseMath(text)) {
			String t = segment.text().replace('−', '-');
			if (segment.level() > 0) {
				s.append('^').append(t.length() > 1 && !t.matches("-?[0-9.]+") ? "(" + t + ")" : t);
			} else {
				s.append(t);
			}
		}
		return s.toString();
	}

	/* ----------------------------- The glTF file ----------------------------- */

	/* The meshes of the plot (the layers and the frame) and the texts */
	static List<Mesh> meshes(Plot3D plot, List<Text> texts) {
		List<Mesh> meshes = new ArrayList<>();
		double size = Math.max(plot.boxSize()[0], Math.max(plot.boxSize()[1], plot.boxSize()[2]));
		List<PlotGeometry> layers = plot.layerGeometries();
		for (int i = 0; i < layers.size(); i++) {
			Mesh m = layerMesh(plot.layerName(i), layers.get(i), size * 0.004);
			if (!m.isEmpty()) {
				meshes.add(m);
			}
			for (PlotGeometry.Label label : layers.get(i).labels) {
				texts.add(new Text(label.x(), label.y(), label.z(), plain(label.text()), size * 0.03 * label.size(), true, Text.NUMBER));
			}
		}
		if (!plot.isPie()) {
			meshes.add(frame(plot, texts));
		}
		return meshes;
	}

	/* Writes the meshes as a glTF binary file (y up: x, z, -y) */
	static void writeGlb(Plot3D plot, Path file) throws IOException {
		String title = plot.title() != null ? plain(plot.title().split("\n")[0]) : "Plot3D";
		writeMeshes(meshes(plot, new ArrayList<>()), file, title);
	}

	/* Writes meshes as a glTF binary file (y up: x, z, -y), the nodes have the names of the meshes */
	static void writeMeshes(List<Mesh> meshes, Path file, String title) throws IOException {
		ByteArrayOutputStream bin = new ByteArrayOutputStream();
		StringBuilder bufferViews = new StringBuilder(), accessors = new StringBuilder(), gltfMeshes = new StringBuilder(),
				materials = new StringBuilder(), nodes = new StringBuilder();
		int view = 0, accessor = 0;
		for (int i = 0; i < meshes.size(); i++) {
			Mesh m = meshes.get(i);
			float[] p = m.positions.toArray(), n = m.normals.toArray(), c = m.colors.toArray();
			/* glTF vertex colors are linear (the colors of the plot are sRGB) */
			for (int k = 0; k < c.length; k++) {
				if (k % 4 != 3) {
					c[k] = linear(c[k]);
				}
			}
			float[] gp = new float[p.length], gn = new float[n.length];
			float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, max = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
			for (int k = 0; k < p.length; k += 3) {
				gp[k] = p[k];
				gp[k + 1] = p[k + 2];
				gp[k + 2] = -p[k + 1];
				gn[k] = n[k];
				gn[k + 1] = n[k + 2];
				gn[k + 2] = -n[k + 1];
				for (int a = 0; a < 3; a++) {
					min[a] = Math.min(min[a], gp[k + a]);
					max[a] = Math.max(max[a], gp[k + a]);
				}
			}
			int count = p.length / 3;
			int[] views = new int[4];
			views[0] = view++;
			appendView(bufferViews, bin, floats(gp), 34962);
			views[1] = view++;
			appendView(bufferViews, bin, floats(gn), 34962);
			views[2] = view++;
			appendView(bufferViews, bin, floats(c), 34962);
			views[3] = view++;
			appendView(bufferViews, bin, ints(Arrays.copyOf(m.indices, m.indexCount)), 34963);
			sep(accessors).append(String.format(Locale.ROOT,
					"{\"bufferView\":%d,\"componentType\":5126,\"count\":%d,\"type\":\"VEC3\",\"min\":[%s,%s,%s],\"max\":[%s,%s,%s]}", views[0], count,
					f(min[0]), f(min[1]), f(min[2]), f(max[0]), f(max[1]), f(max[2])));
			sep(accessors).append(String.format(Locale.ROOT, "{\"bufferView\":%d,\"componentType\":5126,\"count\":%d,\"type\":\"VEC3\"}", views[1], count));
			sep(accessors).append(String.format(Locale.ROOT, "{\"bufferView\":%d,\"componentType\":5126,\"count\":%d,\"type\":\"VEC4\"}", views[2], count));
			sep(accessors).append(String.format(Locale.ROOT, "{\"bufferView\":%d,\"componentType\":5125,\"count\":%d,\"type\":\"SCALAR\"}", views[3],
					m.indexCount));
			int a0 = accessor;
			accessor += 4;
			boolean transparent = m.baseColor[3] < 0.999;
			/* The base color 1 (white): the vertex colors are the colors (glTF multiplies them) */
			sep(materials).append("{\"name\":").append(PlotHtml.string(m.name)).append(",\"pbrMetallicRoughness\":{\"baseColorFactor\":[1,1,1,1],")
					.append("\"metallicFactor\":0,\"roughnessFactor\":0.6},\"doubleSided\":true").append(transparent ? ",\"alphaMode\":\"BLEND\"" : "")
					.append(",\"extras\":{\"color\":[").append(f(m.baseColor[0])).append(',').append(f(m.baseColor[1])).append(',')
					.append(f(m.baseColor[2])).append(',').append(f(m.baseColor[3])).append("]}}");
			sep(gltfMeshes).append("{\"name\":").append(PlotHtml.string(m.name)).append(",\"primitives\":[{\"attributes\":{\"POSITION\":").append(a0)
					.append(",\"NORMAL\":").append(a0 + 1).append(",\"COLOR_0\":").append(a0 + 2).append("},\"indices\":").append(a0 + 3)
					.append(",\"material\":").append(i).append(",\"mode\":4}]}");
			sep(nodes).append("{\"name\":").append(PlotHtml.string(m.name)).append(",\"mesh\":").append(i).append('}');
		}
		StringBuilder sceneNodes = new StringBuilder();
		for (int i = 0; i < meshes.size(); i++) {
			sep(sceneNodes).append(i);
		}
		String json = "{\"asset\":{\"version\":\"2.0\",\"generator\":\"Bio7 Plot3D\"},\"scene\":0,\"scenes\":[{\"name\":" + PlotHtml.string(title)
				+ ",\"nodes\":[" + sceneNodes + "]}],\"nodes\":[" + nodes + "],\"meshes\":[" + gltfMeshes + "],\"materials\":[" + materials
				+ "],\"accessors\":[" + accessors + "],\"bufferViews\":[" + bufferViews + "],\"buffers\":[{\"byteLength\":" + bin.size() + "}]}";
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		int jsonLength = (jsonBytes.length + 3) / 4 * 4, binLength = (bin.size() + 3) / 4 * 4;
		ByteBuffer out = ByteBuffer.allocate(12 + 8 + jsonLength + 8 + binLength).order(ByteOrder.LITTLE_ENDIAN);
		out.putInt(0x46546C67).putInt(2).putInt(out.capacity());
		out.putInt(jsonLength).putInt(0x4E4F534A).put(jsonBytes);
		for (int i = jsonBytes.length; i < jsonLength; i++) {
			out.put((byte) ' ');
		}
		out.putInt(binLength).putInt(0x004E4942).put(bin.toByteArray());
		for (int i = bin.size(); i < binLength; i++) {
			out.put((byte) 0);
		}
		Files.write(file, out.array());
	}

	private static float linear(float c) {
		return (float) (c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
	}

	private static StringBuilder sep(StringBuilder s) {
		if (s.length() > 0) {
			s.append(',');
		}
		return s;
	}

	private static void appendView(StringBuilder views, ByteArrayOutputStream bin, byte[] data, int target) {
		int offset = bin.size();
		bin.write(data, 0, data.length);
		while (bin.size() % 4 != 0) {
			bin.write(0);
		}
		sep(views).append("{\"buffer\":0,\"byteOffset\":").append(offset).append(",\"byteLength\":").append(data.length).append(",\"target\":")
				.append(target).append('}');
	}

	private static byte[] floats(float[] f) {
		ByteBuffer b = ByteBuffer.allocate(f.length * 4).order(ByteOrder.LITTLE_ENDIAN);
		b.asFloatBuffer().put(f);
		return b.array();
	}

	private static byte[] ints(int[] v) {
		ByteBuffer b = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
		b.asIntBuffer().put(v);
		return b.array();
	}

	private static String f(double v) {
		return Float.isFinite((float) v) ? Float.toString((float) v) : "0";
	}

	/* ----------------------------- The Blender script ----------------------------- */

	/* A Python string literal */
	private static String py(String s) {
		StringBuilder b = new StringBuilder("\"");
		for (char c : s.toCharArray()) {
			switch (c) {
			case '\\' -> b.append("\\\\");
			case '"' -> b.append("\\\"");
			case '\n' -> b.append("\\n");
			case '\r' -> b.append("\\r");
			default -> b.append(c);
			}
		}
		return b.append('"').toString();
	}

	/* The title texts of a plot (above the box) */
	static void titleTexts(Plot3D plot, List<Text> texts) {
		double[] box = plot.boxSize();
		double size = Math.max(box[0], Math.max(box[1], box[2]));
		if (plot.title() != null && !plot.title().isBlank()) {
			String[] lines = plot.title().split("\n");
			for (int i = 0; i < lines.length; i++) {
				texts.add(new Text(box[0] / 2, box[1] / 2, box[2] + size * (0.32 - 0.075 * i), plain(lines[i]), size * (i == 0 ? 0.06 : 0.042),
						i == 0, Text.TITLE));
			}
		}
	}

	static String blenderScript(Plot3D plot, Path glb) {
		List<Text> texts = new ArrayList<>();
		meshes(plot, texts);
		titleTexts(plot, texts);
		StringBuilder t = new StringBuilder();
		for (Text text : texts) {
			t.append(textEntry(text, 0, 0));
		}
		return script(plot, glb, t.toString(), "", 0, 24);
	}

	/* A text of the script: the position, the text, the size, bold, the kind and the frames (0, 0 = always) */
	private static String textEntry(Text text, int first, int last) {
		return String.format(Locale.ROOT, "    (%s, %s, %s, %s, %s, %s, %d, %d, %d),%n", f(text.x()), f(text.y()), f(text.z()), py(text.text()),
				f(text.size()), text.bold() ? "True" : "False", text.kind(), first, last);
	}

	/* An object of an animation: visible from the first to the last frame */
	private record Visible<T>(T value, String key, int first, int last) {
	}

	/*
	 * Exports the frames of a recording (see PlotRecorder) for Blender: one glTF
	 * file with the objects of all frames (an object which doesn't change is
	 * exported once for its frames) and a script which animates the visibility of
	 * the objects and the texts (the frames of Blender 1 ... n).
	 */
	static String writeAnimation(List<Plot3D> frames, Path glb, double fps) throws IOException {
		List<Mesh> meshes = new ArrayList<>();
		StringBuilder animation = new StringBuilder(), texts = new StringBuilder();
		java.util.Map<String, Visible<Mesh>> openMeshes = new java.util.LinkedHashMap<>();
		java.util.Map<String, Visible<Text>> openTexts = new java.util.LinkedHashMap<>();
		List<Visible<Mesh>> closedMeshes = new ArrayList<>();
		List<Visible<Text>> closedTexts = new ArrayList<>();
		for (int k = 1; k <= frames.size(); k++) {
			List<Text> frameTexts = new ArrayList<>();
			List<Mesh> frameMeshes = meshes(frames.get(k - 1), frameTexts);
			titleTexts(frames.get(k - 1), frameTexts);
			for (Mesh m : frameMeshes) {
				Visible<Mesh> open = openMeshes.get(m.name);
				if (open != null && open.last() == k - 1 && sameMesh(open.value(), m)) {
					openMeshes.put(m.name, new Visible<>(open.value(), m.name, open.first(), k));
				} else {
					if (open != null) {
						closedMeshes.add(open);
					}
					openMeshes.put(m.name, new Visible<>(m, m.name, k, k));
				}
			}
			for (Text t : frameTexts) {
				String key = t.kind() + "|" + t.text() + "|" + f(t.x()) + "|" + f(t.y()) + "|" + f(t.z());
				Visible<Text> open = openTexts.get(key);
				if (open != null && open.last() == k - 1) {
					openTexts.put(key, new Visible<>(open.value(), key, open.first(), k));
				} else {
					if (open != null) {
						closedTexts.add(open);
					}
					openTexts.put(key, new Visible<>(t, key, k, k));
				}
			}
		}
		closedMeshes.addAll(openMeshes.values());
		closedTexts.addAll(openTexts.values());
		int n = frames.size();
		for (Visible<Mesh> v : closedMeshes) {
			Mesh m = v.value();
			/* A unique name (the object names of Blender have at most 63 characters) */
			String base = m.name.length() > 40 ? m.name.substring(0, 40) : m.name;
			Mesh named = m.rename(base + (v.first() == 1 && v.last() == n ? "" : " " + v.first() + "-" + v.last()));
			meshes.add(named);
			animation.append(String.format(Locale.ROOT, "    %s: (%d, %d),%n", py(named.name), v.first(), v.last()));
		}
		for (Visible<Text> v : closedTexts) {
			boolean always = v.first() == 1 && v.last() == n;
			texts.append(textEntry(v.value(), always ? 0 : v.first(), always ? 0 : v.last()));
		}
		Plot3D first = frames.get(0);
		String title = first.title() != null ? plain(first.title().split("\n")[0]) : "Plot3D";
		writeMeshes(meshes, glb, title);
		return script(first, glb, texts.toString(), animation.toString(), n, fps);
	}

	private static boolean sameMesh(Mesh a, Mesh b) {
		return a.indexCount == b.indexCount && Arrays.equals(a.positions.toArray(), b.positions.toArray())
				&& Arrays.equals(a.colors.toArray(), b.colors.toArray())
				&& Arrays.equals(Arrays.copyOf(a.indices, a.indexCount), Arrays.copyOf(b.indices, b.indexCount));
	}

	/* The script for a plot (frames 0) or an animation */
	private static String script(Plot3D plot, Path glb, String texts, String animation, int frames, double fps) {
		double[] box = plot.boxSize();
		double[] e = eye(box);
		StringBuilder t = new StringBuilder(texts);
		/* The colors of the texts (linear for Blender, the default dark gray) */
		int[] colors = plot.textColorsOf();
		StringBuilder c = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			int rgb = colors[i] >= 0 ? colors[i] : 0x3A3A3A;
			c.append(i > 0 ? ", " : "").append(String.format(Locale.ROOT, "(%s, %s, %s, 1)", f(linear(((rgb >> 16) & 0xff) / 255f)),
					f(linear(((rgb >> 8) & 0xff) / 255f)), f(linear((rgb & 0xff) / 255f))));
		}
		String name = plot.title() != null ? plain(plot.title().split("\n")[0]) : "Plot3D";
		return SCRIPT.replace("%GLB%", py(glb.toAbsolutePath().toString())).replace("%FILE%", py(glb.getFileName().toString()))
				.replace("%NAME%", py(name)).replace("%TEXTS%", t.toString()).replace("%COLORS%", c.toString()).replace("%FONTS%", fontFiles(plot, false)).replace("%BOLD_FONTS%", fontFiles(plot, true))
				.replace("%ANIMATION%", animation)
				.replace("%FRAMES%", Integer.toString(frames)).replace("%FPS%", Integer.toString((int) Math.max(1, Math.round(fps))))
				.replace("%EYE%", f(e[0]) + ", " + f(e[1]) + ", " + f(e[2]))
				.replace("%CENTER%", f(box[0] / 2) + ", " + f(box[1] / 2) + ", " + f(box[2] / 2));
	}

	/*
	 * The font files of the text kinds of the script (title, numbers, axis
	 * titles) as Python values, the regular or the bold style
	 */
	private static String fontFiles(Plot3D plot, boolean bold) {
		String[] fonts = plot.fontsOf();
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			String file = fontFile(fonts[i], bold);
			s.append(i > 0 ? ", " : "").append(file != null ? py(file) : "None");
		}
		return s.toString();
	}

	/* The font files of the families (an empty string if no file was found) */
	private static final java.util.Map<String, String> FONT_FILES = new java.util.concurrent.ConcurrentHashMap<>();

	/*
	 * The font file of a font family (the regular style) in the font folders of
	 * the system or null: first the files with the name of the family in the file
	 * name, then all font files (the family names of the files).
	 */
	static String fontFile(String family) {
		return fontFile(family, false);
	}

	/* The font file of the regular or the bold style of a font family or null */
	static String fontFile(String family, boolean bold) {
		if (family == null || family.isBlank()) {
			return null;
		}
		String file = FONT_FILES.computeIfAbsent(family + (bold ? "\u0000bold" : ""), _ -> {
			String found = searchFontFile(family, true, bold);
			if (found == null) {
				found = searchFontFile(family, false, bold);
			}
			return found != null ? found : "";
		});
		return file.isEmpty() ? null : file;
	}

	private static String searchFontFile(String family, boolean byName, boolean bold) {
		String key = family.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
		List<Path> folders = new ArrayList<>();
		String home = System.getProperty("user.home");
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("mac")) {
			folders.add(Path.of("/System/Library/Fonts"));
			folders.add(Path.of("/Library/Fonts"));
			folders.add(Path.of(home, "Library", "Fonts"));
		} else if (os.contains("win")) {
			folders.add(Path.of(System.getenv().getOrDefault("WINDIR", "C:\\Windows"), "Fonts"));
			String local = System.getenv("LOCALAPPDATA");
			if (local != null) {
				folders.add(Path.of(local, "Microsoft", "Windows", "Fonts"));
			}
		} else {
			folders.add(Path.of("/usr/share/fonts"));
			folders.add(Path.of("/usr/local/share/fonts"));
			folders.add(Path.of(home, ".fonts"));
			folders.add(Path.of(home, ".local", "share", "fonts"));
		}
		String best = null;
		int bestScore = Integer.MAX_VALUE;
		for (Path folder : folders) {
			if (!Files.isDirectory(folder)) {
				continue;
			}
			try (java.util.stream.Stream<Path> files = Files.walk(folder, 4)) {
				for (Path p : (Iterable<Path>) files::iterator) {
					String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
					if (!(name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc"))) {
						continue;
					}
					if (byName && !name.replaceAll("[^a-z0-9]", "").contains(key)) {
						continue;
					}
					try {
						/*
						 * Blender loads the first font of a font collection (*.ttc), thus only the
						 * first font of a collection is used
						 */
						java.awt.Font[] fonts = java.awt.Font.createFonts(p.toFile());
						for (int f = 0; f < (name.endsWith(".ttc") ? Math.min(1, fonts.length) : fonts.length); f++) {
							java.awt.Font font = fonts[f];
							if (!font.getFamily(Locale.ROOT).equalsIgnoreCase(family)) {
								continue;
							}
							String fontName = font.getFontName(Locale.ROOT);
							String lower = fontName.toLowerCase(Locale.ROOT);
							if (lower.contains("italic") || lower.contains("oblique") || lower.contains("bold") != bold) {
								continue;
							}
							/* The regular style is preferred (the shortest font name of the family) */
							int score = fontName.length() - (lower.contains("regular") ? 7 : 0);
							if (score < bestScore) {
								bestScore = score;
								best = p.toAbsolutePath().toString();
							}
						}
					} catch (java.awt.FontFormatException | IOException e) {
						/* Not a font file which Java can read */
					}
				}
			} catch (IOException | RuntimeException e) {
				/* A folder which cannot be read */
			}
		}
		return best;
	}

	private static final String SCRIPT = """
			# A 3D plot of Bio7 (Plot3D) for Blender: imports the glTF file and adds the texts,
			# a camera and the lights in a new scene.
			# Run it in Blender: Scripting workspace - Open - Run Script (or blender --python <script>).
			import os
			import math
			import bpy

			GLB = %GLB%
			NAME = %NAME%
			# The animation (a recording): the frames of the objects (first, last), the number of the frames
			ANIMATION = {
			%ANIMATION%}
			FRAMES = %FRAMES%
			FPS = %FPS%
			# The texts: x, y, z, text, size, bold, kind (0 title, 1 number, 2 axis title), first and last frame (0 = always)
			TEXT_COLORS = [%COLORS%]
			# The font files of the kinds of the texts (None = the default font of Blender)
			TEXT_FONTS = [%FONTS%]
			TEXT_BOLD_FONTS = [%BOLD_FONTS%]
			TEXTS = [
			%TEXTS%]
			EYE = (%EYE%)
			CENTER = (%CENTER%)


			def find_glb():
			    if os.path.exists(GLB):
			        return GLB
			    # Next to the script (e.g., if the files were moved)
			    folders = []
			    try:
			        folders.append(os.path.dirname(os.path.abspath(__file__)))
			    except NameError:
			        pass
			    for text in bpy.data.texts:
			        if text.filepath:
			            folders.append(os.path.dirname(bpy.path.abspath(text.filepath)))
			    for folder in folders:
			        path = os.path.join(folder, %FILE%)
			        if os.path.exists(path):
			            return path
			    raise FileNotFoundError("The glTF file was not found: " + GLB)


			# A new scene (the existing scenes are kept)
			if bpy.context.window is not None:
			    scene = bpy.data.scenes.new(NAME)
			    bpy.context.window.scene = scene
			else:
			    # Background mode (blender --background --python): the current scene
			    scene = bpy.context.scene
			glb_file = find_glb()
			bpy.ops.import_scene.gltf(filepath=glb_file)
			imported = list(bpy.context.selected_objects)

			# The output of a rendered animation: a folder next to the glTF file (the
			# default '//' is the folder of the saved .blend file, without a saved file
			# the working directory of Blender which can be read-only, e.g., '/' on macOS)
			output = os.path.join(os.path.dirname(os.path.abspath(glb_file)), os.path.splitext(os.path.basename(glb_file))[0] + "_render")
			if not os.access(os.path.dirname(output), os.W_OK):
			    import tempfile
			    output = os.path.join(tempfile.gettempdir(), os.path.splitext(os.path.basename(glb_file))[0] + "_render")
			scene.render.filepath = os.path.join(output, "frame_")


			def visible(obj, first, last):
			    # The object is visible from the first to the last frame (keyframes of the visibility)
			    for attribute in ('hide_viewport', 'hide_render'):
			        if first > 1:
			            setattr(obj, attribute, True)
			            obj.keyframe_insert(attribute, frame=first - 1)
			        setattr(obj, attribute, False)
			        obj.keyframe_insert(attribute, frame=first)
			        if last < FRAMES:
			            setattr(obj, attribute, True)
			            obj.keyframe_insert(attribute, frame=last + 1)


			if FRAMES > 0:
			    scene.frame_start = 1
			    scene.frame_end = FRAMES
			    scene.render.fps = FPS
			    for obj in imported:
			        # The name of the glTF node (Blender adds .001 to a name which exists)
			        name = obj.name
			        if len(name) > 4 and name[-4] == '.' and name[-3:].isdigit():
			            name = name[:-4]
			        frames = ANIMATION.get(name)
			        if frames is not None:
			            visible(obj, frames[0], frames[1])

			# The vertex colors are the colors of the materials
			for obj in imported:
			    if obj.type != 'MESH' or not obj.data.color_attributes:
			        continue
			    attribute = obj.data.color_attributes[0].name
			    for slot in obj.material_slots:
			        material = slot.material
			        if material is None or not material.use_nodes:
			            continue
			        nodes = material.node_tree.nodes
			        bsdf = next((n for n in nodes if n.type == 'BSDF_PRINCIPLED'), None)
			        if bsdf is None:
			            continue
			        color = nodes.new('ShaderNodeVertexColor')
			        color.layer_name = attribute
			        color.location = (bsdf.location.x - 300, bsdf.location.y)
			        material.node_tree.links.new(color.outputs['Color'], bsdf.inputs['Base Color'])
			        material.node_tree.links.new(color.outputs['Alpha'], bsdf.inputs['Alpha'])

			# The camera (looks at the center of the plot)
			target = bpy.data.objects.new(NAME + " Center", None)
			target.location = CENTER
			scene.collection.objects.link(target)
			camera_data = bpy.data.cameras.new(NAME + " Camera")
			camera_data.angle = math.radians(40)
			camera = bpy.data.objects.new(NAME + " Camera", camera_data)
			camera.location = EYE
			scene.collection.objects.link(camera)
			track = camera.constraints.new('TRACK_TO')
			track.target = target
			track.track_axis = 'TRACK_NEGATIVE_Z'
			track.up_axis = 'UP_Y'
			scene.camera = camera

			# The texts face the camera
			text_materials = []
			for kind, color in enumerate(TEXT_COLORS):
			    material = bpy.data.materials.new(NAME + " Text " + ("Title", "Numbers", "Axis Titles")[kind])
			    material.diffuse_color = color
			    material.use_nodes = True
			    bsdf = material.node_tree.nodes.get('Principled BSDF')
			    if bsdf is not None:
			        bsdf.inputs['Base Color'].default_value = color
			    text_materials.append(material)
			texts = bpy.data.collections.new(NAME + " Texts")
			scene.collection.children.link(texts)
			for x, y, z, body, size, bold, kind, first, last in TEXTS:
			    curve = bpy.data.curves.new(body[:20] or "Text", type='FONT')
			    curve.body = body
			    curve.size = size
			    # The font of the text kind (the bold style if available)
			    font_file = None
			    for file in ((TEXT_BOLD_FONTS[kind], TEXT_FONTS[kind]) if bold else (TEXT_FONTS[kind],)):
			        if file and os.path.exists(file):
			            try:
			                curve.font = bpy.data.fonts.load(file, check_existing=True)
			                font_file = file
			                break
			            except RuntimeError:
			                pass
			    curve.align_x = 'CENTER'
			    curve.align_y = 'CENTER'
			    curve.extrude = size * 0.02
			    # A bold text with the default font of Blender (an offset distorts other fonts)
			    if bold and font_file is None:
			        curve.offset = size * 0.015
			    obj = bpy.data.objects.new("Text " + body[:20], curve)
			    obj.location = (x, y, z)
			    obj.data.materials.append(text_materials[kind])
			    # The texts cast no shadows on the plot
			    if hasattr(obj, 'visible_shadow'):
			        obj.visible_shadow = False
			    constraint = obj.constraints.new('TRACK_TO')
			    constraint.target = camera
			    constraint.track_axis = 'TRACK_Z'
			    constraint.up_axis = 'UP_Y'
			    texts.objects.link(obj)
			    if FRAMES > 0 and first > 0:
			        visible(obj, first, last)

			# The lights and a white background
			sun_data = bpy.data.lights.new(NAME + " Sun", type='SUN')
			sun_data.energy = 3.0
			sun = bpy.data.objects.new(NAME + " Sun", sun_data)
			sun.rotation_euler = (math.radians(40), math.radians(10), math.radians(-30))
			scene.collection.objects.link(sun)
			fill_data = bpy.data.lights.new(NAME + " Fill", type='SUN')
			fill_data.energy = 1.2
			fill = bpy.data.objects.new(NAME + " Fill", fill_data)
			fill.rotation_euler = (math.radians(-50), math.radians(-20), math.radians(140))
			scene.collection.objects.link(fill)
			world = bpy.data.worlds.new(NAME + " World")
			world.use_nodes = True
			background = world.node_tree.nodes.get('Background')
			if background is not None:
			    background.inputs['Color'].default_value = (1, 1, 1, 1)
			    background.inputs['Strength'].default_value = 1.0
			scene.world = world
			scene.view_settings.view_transform = 'Standard'
			scene.render.resolution_x = 1600
			scene.render.resolution_y = 1200
			if FRAMES > 0:
			    scene.frame_set(1)
			print("Bio7 plot imported:", NAME, "frames:", FRAMES, "render output:", scene.render.filepath)
			""";
}
