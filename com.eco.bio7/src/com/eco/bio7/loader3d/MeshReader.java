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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads triangle meshes from STL (binary and ASCII), PLY (ASCII and binary),
 * OBJ (with MTL materials) and OFF files. Polygons are triangulated (fan).
 */
public class MeshReader {

	/** The supported file extensions. */
	public static final String[] FORMATS = { "stl", "ply", "obj", "off" };

	private MeshReader() {
	}

	/** Returns true if the file extension is supported (see FORMATS). */
	public static boolean isSupported(File file) {
		String extension = extension(file);
		for (String format : FORMATS) {
			if (format.equals(extension)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Reads a mesh (the format is selected by the file extension).
	 *
	 * @param file the file
	 * @return the mesh
	 * @throws IOException if the file can't be read or the format is unknown
	 */
	public static MeshData read(File file) throws IOException {
		MeshData mesh;
		switch (extension(file)) {
		case "stl":
			mesh = readStl(file);
			break;
		case "ply":
			mesh = readPly(file);
			break;
		case "obj":
			mesh = readObj(file);
			break;
		case "off":
			mesh = readOff(file);
			break;
		default:
			throw new IOException("Unknown mesh format: " + file.getName() + " (supported: stl, ply, obj, off)");
		}
		String name = file.getName();
		mesh.name = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
		if (mesh.triangles.length == 0) {
			throw new IOException("The file contains no triangles: " + file.getName());
		}
		return mesh;
	}

	private static String extension(File file) {
		String name = file.getName().toLowerCase(Locale.ROOT);
		return name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
	}

	/* ------------------------------ STL ------------------------------ */

	private static MeshData readStl(File file) throws IOException {
		long length = file.length();
		/* Binary: 80 bytes header, the count and 50 bytes per triangle */
		if (length >= 84) {
			try (InputStream in = new FileInputStream(file)) {
				byte[] head = in.readNBytes(84);
				long count = ByteBuffer.wrap(head, 80, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL;
				if (84 + count * 50 == length) {
					return readStlBinary(file, (int) count);
				}
			}
		}
		return readStlAscii(file);
	}

	private static MeshData readStlBinary(File file, int count) throws IOException {
		float[] pos = new float[count * 9];
		try (InputStream in = new BufferedInputStream(new FileInputStream(file), 1 << 16)) {
			in.skipNBytes(84);
			byte[] record = new byte[50];
			ByteBuffer buffer = ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN);
			for (int t = 0; t < count; t++) {
				if (in.readNBytes(record, 0, 50) != 50) {
					throw new EOFException("The STL file is too short: " + file.getName());
				}
				for (int k = 0; k < 9; k++) {
					pos[t * 9 + k] = buffer.getFloat(12 + k * 4); // after the normal
				}
			}
		}
		return triangleSoup(pos, "STL");
	}

	private static MeshData readStlAscii(File file) throws IOException {
		FloatList pos = new FloatList();
		try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.ISO_8859_1)) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("vertex")) {
					String[] t = line.split("\\s+");
					pos.add(Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]));
				}
			}
		}
		if (pos.size() % 9 != 0) {
			throw new IOException("Invalid ASCII STL file: " + file.getName());
		}
		return triangleSoup(pos.toArray(), "STL");
	}

	/* Every triangle with its own three vertices */
	private static MeshData triangleSoup(float[] pos, String format) {
		MeshData m = new MeshData();
		m.format = format;
		m.positions = pos;
		m.triangles = new int[pos.length / 3];
		for (int i = 0; i < m.triangles.length; i++) {
			m.triangles[i] = i;
		}
		return m;
	}

	/* ------------------------------ PLY ------------------------------ */

	private static class PlyProperty {
		String name;
		String type;
		/* A list: the type of the count (else null) */
		String countType;
	}

	private static class PlyElement {
		String name;
		int count;
		List<PlyProperty> properties = new ArrayList<>();
	}

	private static MeshData readPly(File file) throws IOException {
		try (InputStream in = new BufferedInputStream(new FileInputStream(file), 1 << 16)) {
			/* The header (ASCII lines up to end_header) */
			List<PlyElement> elements = new ArrayList<>();
			String format = null;
			String line;
			if (!"ply".equals(readLine(in))) {
				throw new IOException("Not a PLY file: " + file.getName());
			}
			while ((line = readLine(in)) != null) {
				String[] t = line.trim().split("\\s+");
				if (t[0].equals("format")) {
					format = t[1];
				} else if (t[0].equals("element")) {
					PlyElement e = new PlyElement();
					e.name = t[1];
					e.count = Integer.parseInt(t[2]);
					elements.add(e);
				} else if (t[0].equals("property") && !elements.isEmpty()) {
					PlyProperty p = new PlyProperty();
					if (t[1].equals("list")) {
						p.countType = t[2];
						p.type = t[3];
						p.name = t[4];
					} else {
						p.type = t[1];
						p.name = t[2];
					}
					elements.get(elements.size() - 1).properties.add(p);
				} else if (t[0].equals("end_header")) {
					break;
				}
			}
			if (format == null) {
				throw new IOException("Invalid PLY header: " + file.getName());
			}
			PlyInput input;
			if (format.equals("ascii")) {
				input = new PlyAscii(in);
			} else {
				input = new PlyBinary(in, format.equals("binary_little_endian") ? ByteOrder.LITTLE_ENDIAN
						: ByteOrder.BIG_ENDIAN);
			}
			FloatList pos = new FloatList();
			FloatList col = new FloatList();
			FloatList nor = new FloatList();
			FloatList tex = new FloatList();
			IntList tri = new IntList();
			for (PlyElement e : elements) {
				boolean vertex = e.name.equals("vertex"), face = e.name.equals("face");
				Map<String, Integer> index = new HashMap<>();
				for (int k = 0; k < e.properties.size(); k++) {
					index.put(e.properties.get(k).name, k);
				}
				boolean hasColor = vertex && (index.containsKey("red") || index.containsKey("r"));
				boolean hasNormal = vertex && index.containsKey("nx");
				String uName = index.containsKey("s") ? "s" : index.containsKey("u") ? "u" : "texture_u";
				String vName = index.containsKey("t") ? "t" : index.containsKey("v") ? "v" : "texture_v";
				boolean hasTex = vertex && index.containsKey(uName) && index.containsKey(vName);
				double[] values = new double[e.properties.size()];
				for (int i = 0; i < e.count; i++) {
					int[] list = null;
					for (int k = 0; k < e.properties.size(); k++) {
						PlyProperty p = e.properties.get(k);
						if (p.countType != null) {
							int n = (int) input.read(p.countType);
							int[] items = new int[n];
							for (int j = 0; j < n; j++) {
								items[j] = (int) input.read(p.type);
							}
							if (face && (p.name.equals("vertex_indices") || p.name.equals("vertex_index"))) {
								list = items;
							}
						} else {
							values[k] = input.read(p.type);
						}
					}
					if (vertex) {
						pos.add((float) values[index.get("x")], (float) values[index.get("y")],
								(float) values[index.get("z")]);
						if (hasColor) {
							String r = index.containsKey("red") ? "red" : "r";
							String g = index.containsKey("green") ? "green" : "g";
							String b = index.containsKey("blue") ? "blue" : "b";
							float scale = isFloatType(e.properties.get(index.get(r)).type) ? 1f : 1f / 255f;
							col.add((float) values[index.get(r)] * scale, (float) values[index.get(g)] * scale,
									(float) values[index.get(b)] * scale);
						}
						if (hasNormal) {
							nor.add((float) values[index.get("nx")], (float) values[index.get("ny")],
									(float) values[index.get("nz")]);
						}
						if (hasTex) {
							tex.add((float) values[index.get(uName)], (float) values[index.get(vName)]);
						}
					} else if (face && list != null) {
						for (int j = 1; j + 1 < list.length; j++) {
							tri.add(list[0], list[j], list[j + 1]);
						}
					}
				}
			}
			MeshData m = new MeshData();
			m.format = "PLY";
			m.positions = pos.toArray();
			m.triangles = tri.toArray();
			int n = m.positions.length;
			m.colors = col.size() == n ? col.toArray() : null;
			m.normals = nor.size() == n ? nor.toArray() : null;
			m.texCoords = tex.size() == n / 3 * 2 && n > 0 ? tex.toArray() : null;
			checkIndices(m, file);
			return m;
		}
	}

	private static boolean isFloatType(String type) {
		return type.startsWith("float") || type.startsWith("double");
	}

	private interface PlyInput {
		double read(String type) throws IOException;
	}

	/* The values of an ASCII PLY file (whitespace separated) */
	private static class PlyAscii implements PlyInput {
		private final InputStream in;

		PlyAscii(InputStream in) {
			this.in = in;
		}

		public double read(String type) throws IOException {
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = in.read()) != -1 && Character.isWhitespace(c)) {
			}
			if (c == -1) {
				throw new EOFException("The PLY file is too short");
			}
			sb.append((char) c);
			while ((c = in.read()) != -1 && !Character.isWhitespace(c)) {
				sb.append((char) c);
			}
			return Double.parseDouble(sb.toString());
		}
	}

	/* The values of a binary PLY file */
	private static class PlyBinary implements PlyInput {
		private final DataInputStream in;
		private final boolean little;
		private final byte[] bytes = new byte[8];
		private final ByteBuffer buffer;

		PlyBinary(InputStream in, ByteOrder order) {
			this.in = new DataInputStream(in);
			this.little = order == ByteOrder.LITTLE_ENDIAN;
			this.buffer = ByteBuffer.wrap(bytes).order(order);
		}

		public double read(String type) throws IOException {
			switch (type) {
			case "char":
			case "int8":
				return in.readByte();
			case "uchar":
			case "uint8":
				return in.readUnsignedByte();
			case "short":
			case "int16":
				in.readFully(bytes, 0, 2);
				return buffer.getShort(0);
			case "ushort":
			case "uint16":
				in.readFully(bytes, 0, 2);
				return buffer.getShort(0) & 0xffff;
			case "int":
			case "int32":
				in.readFully(bytes, 0, 4);
				return buffer.getInt(0);
			case "uint":
			case "uint32":
				in.readFully(bytes, 0, 4);
				return buffer.getInt(0) & 0xffffffffL;
			case "float":
			case "float32":
				in.readFully(bytes, 0, 4);
				return buffer.getFloat(0);
			case "double":
			case "float64":
				in.readFully(bytes, 0, 8);
				return buffer.getDouble(0);
			default:
				throw new IOException("Unknown PLY type: " + type + (little ? "" : " (big endian)"));
			}
		}
	}

	/* A line of the PLY header (ASCII) */
	private static String readLine(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c;
		while ((c = in.read()) != -1 && c != '\n') {
			if (c != '\r') {
				sb.append((char) c);
			}
		}
		return c == -1 && sb.length() == 0 ? null : sb.toString();
	}

	/* ------------------------------ OBJ ------------------------------ */

	private static MeshData readObj(File file) throws IOException {
		FloatList v = new FloatList(), vc = new FloatList(), vt = new FloatList(), vn = new FloatList();
		/* The vertices of the mesh: a combination of v, vt and vn */
		Map<Long, Integer> combined = new HashMap<>();
		IntList posIndex = new IntList(), texIndex = new IntList(), norIndex = new IntList();
		IntList tri = new IntList();
		MeshData m = new MeshData();
		m.format = "OBJ";
		String material = null;
		int groupStart = 0;
		boolean vertexColors = false;
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1 << 16)) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '#') {
					continue;
				}
				String[] t = line.split("\\s+");
				switch (t[0]) {
				case "v":
					v.add(Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]));
					/* Vertex colors (an extension of e.g. MeshLab: v x y z r g b) */
					if (t.length >= 7) {
						vc.add(Float.parseFloat(t[4]), Float.parseFloat(t[5]), Float.parseFloat(t[6]));
						vertexColors = true;
					} else {
						vc.add(1, 1, 1);
					}
					break;
				case "vt":
					vt.add(Float.parseFloat(t[1]), t.length > 2 ? Float.parseFloat(t[2]) : 0);
					break;
				case "vn":
					vn.add(Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]));
					break;
				case "f": {
					int[] corners = new int[t.length - 1];
					for (int k = 1; k < t.length; k++) {
						String[] parts = t[k].split("/", -1);
						int pi = objIndex(parts[0], v.size() / 3);
						int ti = parts.length > 1 && !parts[1].isEmpty() ? objIndex(parts[1], vt.size() / 2) : -1;
						int ni = parts.length > 2 && !parts[2].isEmpty() ? objIndex(parts[2], vn.size() / 3) : -1;
						long key = ((long) pi * 2097152L + (ti + 1)) * 2097152L + (ni + 1);
						Integer index = combined.get(key);
						if (index == null) {
							index = posIndex.size();
							combined.put(key, index);
							posIndex.add(pi);
							texIndex.add(ti);
							norIndex.add(ni);
						}
						corners[k - 1] = index;
					}
					for (int k = 1; k + 1 < corners.length; k++) {
						tri.add(corners[0], corners[k], corners[k + 1]);
					}
					break;
				}
				case "usemtl": {
					int triangles = tri.size() / 3;
					if (triangles > groupStart) {
						m.groups.add(new MeshData.Group(groupStart, triangles - groupStart, material));
					}
					groupStart = triangles;
					material = line.substring(6).trim();
					break;
				}
				case "mtllib":
					for (int k = 1; k < t.length; k++) {
						readMtl(new File(file.getParentFile(), t[k]), m.materials);
					}
					break;
				default:
					break; // g, o, s, l, ...
				}
			}
		}
		int triangles = tri.size() / 3;
		if (triangles > groupStart) {
			m.groups.add(new MeshData.Group(groupStart, triangles - groupStart, material));
		}
		/* The vertices of the mesh */
		int n = posIndex.size();
		float[] pos = new float[n * 3];
		float[] col = vertexColors ? new float[n * 3] : null;
		boolean hasTex = vt.size() > 0, hasNor = vn.size() > 0;
		float[] tex = hasTex ? new float[n * 2] : null;
		float[] nor = hasNor ? new float[n * 3] : null;
		float[] vArray = v.toArray(), vcArray = vc.toArray(), vtArray = vt.toArray(), vnArray = vn.toArray();
		for (int i = 0; i < n; i++) {
			int p = posIndex.get(i);
			System.arraycopy(vArray, p * 3, pos, i * 3, 3);
			if (col != null) {
				System.arraycopy(vcArray, p * 3, col, i * 3, 3);
			}
			if (tex != null && texIndex.get(i) >= 0) {
				System.arraycopy(vtArray, texIndex.get(i) * 2, tex, i * 2, 2);
			}
			if (nor != null && norIndex.get(i) >= 0) {
				System.arraycopy(vnArray, norIndex.get(i) * 3, nor, i * 3, 3);
			}
		}
		m.positions = pos;
		m.colors = col;
		m.texCoords = tex;
		m.normals = nor;
		m.triangles = tri.toArray();
		m.positionIndex = posIndex.toArray();
		checkIndices(m, file);
		return m;
	}

	/* An OBJ index (1 - n or negative: relative to the end) as 0 - n-1 */
	private static int objIndex(String text, int count) throws IOException {
		int i = Integer.parseInt(text);
		int index = i > 0 ? i - 1 : count + i;
		if (index < 0 || index >= count) {
			throw new IOException("Invalid OBJ index: " + text);
		}
		return index;
	}

	/* The materials of an MTL file (missing files are ignored) */
	private static void readMtl(File file, Map<String, MeshData.Material> materials) {
		if (!file.isFile()) {
			System.out.println("The material file doesn't exist: " + file);
			return;
		}
		try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			MeshData.Material current = null;
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '#') {
					continue;
				}
				String[] t = line.split("\\s+");
				if (t[0].equals("newmtl")) {
					current = new MeshData.Material();
					current.name = line.substring(6).trim();
					materials.put(current.name, current);
				} else if (current == null) {
					continue;
				} else if (t[0].equals("Ka") && t.length >= 4) {
					current.ambient = rgb(t);
				} else if (t[0].equals("Kd") && t.length >= 4) {
					current.diffuse = rgb(t);
				} else if (t[0].equals("Ks") && t.length >= 4) {
					current.specular = rgb(t);
				} else if (t[0].equals("Ns")) {
					current.shininess = Float.parseFloat(t[1]);
				} else if (t[0].equals("d")) {
					current.alpha = Float.parseFloat(t[t.length - 1]);
				} else if (t[0].equals("Tr")) {
					current.alpha = 1 - Float.parseFloat(t[1]);
				} else if (t[0].equals("map_Kd")) {
					/* The file name is the last part (options like -s are skipped) */
					File texture = new File(file.getParentFile(), t[t.length - 1]);
					current.texture = texture.getAbsolutePath();
				}
			}
		} catch (IOException | RuntimeException e) {
			System.out.println("The material file can't be read: " + file + " (" + e.getMessage() + ")");
		}
	}

	private static float[] rgb(String[] t) {
		return new float[] { Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]) };
	}

	/* ------------------------------ OFF ------------------------------ */

	private static MeshData readOff(File file) throws IOException {
		/* The lines without comments and empty lines */
		List<String[]> lines = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.ISO_8859_1)) {
			String text;
			while ((text = br.readLine()) != null) {
				int comment = text.indexOf('#');
				if (comment >= 0) {
					text = text.substring(0, comment);
				}
				text = text.trim();
				if (!text.isEmpty()) {
					lines.add(text.split("\\s+"));
				}
			}
		}
		if (lines.isEmpty() || !lines.get(0)[0].endsWith("OFF")) {
			throw new IOException("Not an OFF file: " + file.getName());
		}
		String header = lines.get(0)[0];
		boolean color = header.contains("C"), normal = header.contains("N");
		/* The counts in the header line or in the next line */
		int line = 0;
		String[] counts = lines.get(0).length >= 3 ? java.util.Arrays.copyOfRange(lines.get(0), 1, lines.get(0).length)
				: lines.get(++line);
		int nv = Integer.parseInt(counts[0]), nf = Integer.parseInt(counts[1]);
		line++;
		float[] pos = new float[nv * 3];
		float[] col = color ? new float[nv * 3] : null;
		float[] nor = normal ? new float[nv * 3] : null;
		for (int v = 0; v < nv; v++) {
			String[] t = lines.get(line++);
			for (int k = 0; k < 3; k++) {
				pos[v * 3 + k] = Float.parseFloat(t[k]);
			}
			int next = 3;
			if (normal) {
				for (int k = 0; k < 3; k++) {
					nor[v * 3 + k] = Float.parseFloat(t[next + k]);
				}
				next += 3;
			}
			if (color && t.length >= next + 3) {
				setColor(col, v, t, next);
			}
		}
		IntList tri = new IntList();
		/* Face colors (n i0 ... r g b [a]) are used as vertex colors */
		float[] faceColors = null;
		for (int f = 0; f < nf; f++) {
			String[] t = lines.get(line++);
			int n = Integer.parseInt(t[0]);
			int[] corners = new int[n];
			for (int k = 0; k < n; k++) {
				corners[k] = Integer.parseInt(t[k + 1]);
			}
			if (t.length >= n + 4) {
				if (faceColors == null) {
					faceColors = col != null ? col : new float[nv * 3];
					if (col == null) {
						java.util.Arrays.fill(faceColors, 1);
					}
				}
				for (int corner : corners) {
					setColor(faceColors, corner, t, n + 1);
				}
			}
			for (int k = 1; k + 1 < n; k++) {
				tri.add(corners[0], corners[k], corners[k + 1]);
			}
		}
		MeshData m = new MeshData();
		m.format = "OFF";
		m.positions = pos;
		m.colors = faceColors != null ? faceColors : col;
		m.normals = nor;
		m.triangles = tri.toArray();
		checkIndices(m, file);
		return m;
	}

	/* r g b (0 - 1 or 0 - 255) of the tokens from the index */
	private static void setColor(float[] colors, int vertex, String[] t, int index) {
		float r = Float.parseFloat(t[index]), g = Float.parseFloat(t[index + 1]), b = Float.parseFloat(t[index + 2]);
		boolean bytes = !(t[index].contains(".") || t[index + 1].contains(".") || t[index + 2].contains("."))
				&& (r > 1 || g > 1 || b > 1);
		float scale = bytes ? 1f / 255f : 1f;
		colors[vertex * 3] = r * scale;
		colors[vertex * 3 + 1] = g * scale;
		colors[vertex * 3 + 2] = b * scale;
	}

	private static void checkIndices(MeshData m, File file) throws IOException {
		int n = m.positions.length / 3;
		for (int index : m.triangles) {
			if (index < 0 || index >= n) {
				throw new IOException("Invalid vertex index " + index + " in " + file.getName());
			}
		}
	}

	/* ------------------------------ Lists ------------------------------ */

	static final class FloatList {
		private float[] data = new float[1024];
		private int size;

		void add(float... values) {
			if (size + values.length > data.length) {
				data = java.util.Arrays.copyOf(data, Math.max(data.length * 2, size + values.length));
			}
			System.arraycopy(values, 0, data, size, values.length);
			size += values.length;
		}

		int size() {
			return size;
		}

		float[] toArray() {
			return java.util.Arrays.copyOf(data, size);
		}
	}

	static final class IntList {
		private int[] data = new int[1024];
		private int size;

		void add(int... values) {
			if (size + values.length > data.length) {
				data = java.util.Arrays.copyOf(data, Math.max(data.length * 2, size + values.length));
			}
			System.arraycopy(values, 0, data, size, values.length);
			size += values.length;
		}

		int get(int i) {
			return data[i];
		}

		int size() {
			return size;
		}

		int[] toArray() {
			return java.util.Arrays.copyOf(data, size);
		}
	}
}
