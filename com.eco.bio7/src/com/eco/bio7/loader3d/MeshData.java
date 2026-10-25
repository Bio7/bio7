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
package com.eco.bio7.loader3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A triangle mesh read from a file (see MeshReader): the positions, the
 * triangles and optional vertex colors, normals, texture coordinates and the
 * material groups of an OBJ file. The coordinates are in the units of the file.
 */
public class MeshData {

	/** A part of the triangles with one material (OBJ: usemtl). */
	public static class Group {
		/** The first triangle and the number of triangles. */
		public int start, count;
		/** The name of the material or null. */
		public String material;

		public Group(int start, int count, String material) {
			this.start = start;
			this.count = count;
			this.material = material;
		}
	}

	/** A material of an OBJ file (MTL). */
	public static class Material {
		public String name;
		public float[] ambient = { 0.2f, 0.2f, 0.2f };
		public float[] diffuse = { 0.8f, 0.8f, 0.8f };
		public float[] specular = { 0f, 0f, 0f };
		public float shininess = 0;
		public float alpha = 1;
		/** The texture file (absolute path) or null. */
		public String texture;
	}

	/** The checks of a mesh (see check). */
	public static class Report {
		public int vertices;
		public int triangles;
		/** The edges used by one triangle (holes, not watertight). */
		public int openEdges;
		/** The edges used by more than two triangles. */
		public int nonManifoldEdges;
		/** Triangles without an area or with a repeated vertex. */
		public int degenerateTriangles;
		/** The number of merged duplicate vertices (see weld). */
		public int mergedVertices;

		/** Returns true if the mesh is closed (needed for the volume). */
		public boolean isWatertight() {
			return openEdges == 0 && nonManifoldEdges == 0;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(vertices).append(" vertices, ").append(triangles).append(" triangles");
			if (mergedVertices > 0) {
				sb.append(", ").append(mergedVertices).append(" duplicate vertices merged");
			}
			sb.append(isWatertight() ? ", watertight" : ", not watertight");
			if (openEdges > 0) {
				sb.append(" (").append(openEdges).append(" open edges)");
			}
			if (nonManifoldEdges > 0) {
				sb.append(", ").append(nonManifoldEdges).append(" non-manifold edges");
			}
			if (degenerateTriangles > 0) {
				sb.append(", ").append(degenerateTriangles).append(" degenerate triangles");
			}
			return sb.toString();
		}
	}

	/** The name (the file name without the extension). */
	public String name;
	/** The format (STL, PLY, OBJ or OFF). */
	public String format;
	/** x, y, z of the vertices. */
	public float[] positions;
	/** Three vertex indices per triangle. */
	public int[] triangles;
	/** r, g, b (0 - 1) per vertex or null. */
	public float[] colors;
	/** x, y, z per vertex or null (computed for the display if missing). */
	public float[] normals;
	/** u, v per vertex or null. */
	public float[] texCoords;
	/** The material groups (OBJ) or an empty list. */
	public List<Group> groups = new ArrayList<>();
	/** The materials (OBJ) by name. */
	public Map<String, Material> materials = new LinkedHashMap<>();
	/**
	 * The index of the position of every vertex (OBJ: vertices with different
	 * texture coordinates or normals share a position) or null (the identity).
	 */
	public int[] positionIndex;

	public int getVertexCount() {
		return positions.length / 3;
	}

	public int getTriangleCount() {
		return triangles.length / 3;
	}

	/** Returns the bounding box (min x, y, z, max x, y, z). */
	public float[] getBounds() {
		float[] b = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE,
				-Float.MAX_VALUE };
		for (int i = 0; i < positions.length; i += 3) {
			for (int k = 0; k < 3; k++) {
				b[k] = Math.min(b[k], positions[i + k]);
				b[k + 3] = Math.max(b[k + 3], positions[i + k]);
			}
		}
		if (positions.length == 0) {
			Arrays.fill(b, 0);
		}
		return b;
	}

	/**
	 * Returns a mesh with merged duplicate vertices (the same position within a
	 * tolerance of 1e-6 of the size of the mesh) without degenerate triangles, as
	 * needed for the measurements (connected objects, volume, holes). The colors
	 * are kept (the first vertex of a position), the texture coordinates, normals
	 * and groups are removed.
	 *
	 * @return the welded mesh (report.mergedVertices is set)
	 */
	public MeshData weld() {
		float[] b = getBounds();
		double size = Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2]));
		double tolerance = size > 0 ? size * 1e-6 : 1e-12;
		int n = getVertexCount();
		Map<Key, Integer> map = new HashMap<>(n * 2);
		int[] remap = new int[n];
		float[] pos = new float[n * 3];
		float[] col = colors != null ? new float[n * 3] : null;
		int count = 0;
		for (int i = 0; i < n; i++) {
			Key key = new Key(Math.round(positions[i * 3] / tolerance), Math.round(positions[i * 3 + 1] / tolerance),
					Math.round(positions[i * 3 + 2] / tolerance));
			Integer index = map.get(key);
			if (index == null) {
				index = count++;
				map.put(key, index);
				System.arraycopy(positions, i * 3, pos, index * 3, 3);
				if (col != null) {
					System.arraycopy(colors, i * 3, col, index * 3, 3);
				}
			}
			remap[i] = index;
		}
		int[] tri = new int[triangles.length];
		int t = 0;
		for (int i = 0; i < triangles.length; i += 3) {
			int a = remap[triangles[i]], c = remap[triangles[i + 1]], d = remap[triangles[i + 2]];
			if (a == c || c == d || a == d) {
				continue; // collapsed
			}
			tri[t++] = a;
			tri[t++] = c;
			tri[t++] = d;
		}
		MeshData m = new MeshData();
		m.name = name;
		m.format = format;
		m.positions = Arrays.copyOf(pos, count * 3);
		m.colors = col != null ? Arrays.copyOf(col, count * 3) : null;
		m.triangles = Arrays.copyOf(tri, t);
		/* OBJ: vertices split by texture coordinates or normals are no duplicates of the file */
		int fileVertices = n;
		if (positionIndex != null) {
			fileVertices = (int) Arrays.stream(positionIndex).distinct().count();
		}
		m.mergedVertices = Math.max(0, fileVertices - count);
		return m;
	}

	/* The merged vertices of weld */
	private int mergedVertices;

	/* A quantized position */
	private record Key(long x, long y, long z) {
	}

	/**
	 * Checks the mesh: the open and non-manifold edges and the degenerate
	 * triangles (call it for a welded mesh, see weld).
	 *
	 * @return the report
	 */
	public Report check() {
		Report r = new Report();
		r.vertices = getVertexCount();
		r.triangles = getTriangleCount();
		r.mergedVertices = mergedVertices;
		long n = r.vertices;
		long[] edges = new long[triangles.length];
		for (int i = 0; i < triangles.length; i += 3) {
			for (int k = 0; k < 3; k++) {
				long a = triangles[i + k], c = triangles[i + (k + 1) % 3];
				edges[i + k] = Math.min(a, c) * n + Math.max(a, c);
			}
			/* Degenerate: a repeated vertex or no area */
			int a = triangles[i] * 3, c = triangles[i + 1] * 3, d = triangles[i + 2] * 3;
			if (a == c || c == d || a == d) {
				r.degenerateTriangles++;
				continue;
			}
			double ux = positions[c] - positions[a], uy = positions[c + 1] - positions[a + 1],
					uz = positions[c + 2] - positions[a + 2];
			double vx = positions[d] - positions[a], vy = positions[d + 1] - positions[a + 1],
					vz = positions[d + 2] - positions[a + 2];
			double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
			if (cx * cx + cy * cy + cz * cz == 0) {
				r.degenerateTriangles++;
			}
		}
		Arrays.sort(edges);
		for (int i = 0; i < edges.length;) {
			int j = i;
			while (j < edges.length && edges[j] == edges[i]) {
				j++;
			}
			int uses = j - i;
			if (uses == 1) {
				r.openEdges++;
			} else if (uses > 2) {
				r.nonManifoldEdges++;
			}
			i = j;
		}
		return r;
	}

	/**
	 * Computes smooth vertex normals (area weighted) if the mesh has no normals.
	 */
	public void ensureNormals() {
		if (normals != null && normals.length == positions.length) {
			return;
		}
		float[] nor = new float[positions.length];
		for (int i = 0; i < triangles.length; i += 3) {
			int a = triangles[i] * 3, b = triangles[i + 1] * 3, c = triangles[i + 2] * 3;
			float ux = positions[b] - positions[a], uy = positions[b + 1] - positions[a + 1],
					uz = positions[b + 2] - positions[a + 2];
			float vx = positions[c] - positions[a], vy = positions[c + 1] - positions[a + 1],
					vz = positions[c + 2] - positions[a + 2];
			float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
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
		normals = nor;
	}
}
