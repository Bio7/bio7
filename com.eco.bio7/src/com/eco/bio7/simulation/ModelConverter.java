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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts the 3D models of other formats to binary glTF (.glb): Wavefront OBJ
 * (with the materials of the .mtl files: the color, the transparency and the
 * texture) and STL (binary or ASCII, one color). Bio7 reads the converted model
 * with {@link Gltf} and the HTML export embeds it, so both need only one loader.
 * <p>
 * OBJ is read with y up (as glTF), STL with z up (as most CAD programs).
 */
final class ModelConverter {

	private ModelConverter() {
	}

	/** True for a file which is converted (.obj or .stl). */
	static boolean converted(String file) {
		String f = file == null ? "" : file.toLowerCase(Locale.ROOT);
		return f.endsWith(".obj") || f.endsWith(".stl");
	}

	/** The model as binary glTF (a .glb or .gltf file is returned as it is). */
	static byte[] glb(File file) throws IOException {
		String name = file.getName().toLowerCase(Locale.ROOT);
		if (name.endsWith(".obj")) {
			return write(obj(file));
		}
		if (name.endsWith(".stl")) {
			return write(stl(file));
		}
		return Files.readAllBytes(file.toPath());
	}

	/* A part of the model with one material */
	static final class Part {
		final List<Float> pos = new ArrayList<>(), nor = new ArrayList<>(), uv = new ArrayList<>();
		final List<Integer> idx = new ArrayList<>();
		final Map<String, Integer> vertices = new HashMap<>();
		float[] color = { 0.8f, 0.8f, 0.8f, 1 };
		byte[] texture;
		String mime;
		/* false if a face has no normals (glTF: flat normals computed by the reader) */
		boolean normals = true, uvs = true;
	}

	/* ===================== OBJ ===================== */

	private static final class Material {
		float[] color = { 0.8f, 0.8f, 0.8f, 1 };
		File texture;
	}

	static List<Part> obj(File file) throws IOException {
		List<float[]> v = new ArrayList<>(), vt = new ArrayList<>(), vn = new ArrayList<>();
		Map<String, Material> materials = new HashMap<>();
		Map<String, Part> parts = new LinkedHashMap<>();
		String current = "";
		File folder = file.getAbsoluteFile().getParentFile();
		for (String raw : Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1)) {
			String line = raw.strip();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			String[] t = line.split("\\s+");
			switch (t[0]) {
			case "v" -> v.add(new float[] { num(t, 1), num(t, 2), num(t, 3) });
			case "vt" -> vt.add(new float[] { num(t, 1), t.length > 2 ? num(t, 2) : 0 });
			case "vn" -> vn.add(new float[] { num(t, 1), num(t, 2), num(t, 3) });
			case "mtllib" -> {
				/* The file name may contain spaces */
				String name = line.substring(t[0].length()).strip();
				mtl(new File(folder, name), materials);
			}
			case "usemtl" -> current = t.length > 1 ? line.substring(t[0].length()).strip() : "";
			case "f" -> {
				if (t.length < 4) {
					continue;
				}
				String key = current;
				Part p = parts.computeIfAbsent(key, k -> {
					Part q = new Part();
					Material m = materials.get(k);
					if (m != null) {
						q.color = m.color.clone();
						if (m.texture != null && m.texture.isFile()) {
							texture(q, m.texture);
						}
					}
					return q;
				});
				int[] face = new int[t.length - 1];
				for (int i = 1; i < t.length; i++) {
					face[i - 1] = vertex(p, t[i], v, vt, vn);
				}
				/* A polygon as a fan of triangles */
				for (int i = 1; i + 1 < face.length; i++) {
					if (face[0] >= 0 && face[i] >= 0 && face[i + 1] >= 0) {
						p.idx.add(face[0]);
						p.idx.add(face[i]);
						p.idx.add(face[i + 1]);
					}
				}
			}
			default -> {
			}
			}
		}
		List<Part> out = new ArrayList<>();
		for (Part p : parts.values()) {
			if (!p.idx.isEmpty()) {
				out.add(p);
			}
		}
		if (out.isEmpty()) {
			throw new IOException("no faces in the OBJ file");
		}
		return out;
	}

	/* The index of a vertex v, v/vt, v//vn or v/vt/vn of a face in the part (-1: invalid) */
	private static int vertex(Part p, String spec, List<float[]> v, List<float[]> vt, List<float[]> vn) {
		Integer known = p.vertices.get(spec);
		if (known != null) {
			return known;
		}
		String[] s = spec.split("/", -1);
		float[] pos = at(v, s[0]);
		if (pos == null) {
			return -1;
		}
		float[] uv = s.length > 1 ? at(vt, s[1]) : null, nor = s.length > 2 ? at(vn, s[2]) : null;
		if (nor == null) {
			p.normals = false;
		}
		if (uv == null) {
			p.uvs = false;
		}
		int index = p.pos.size() / 3;
		for (float f : pos) {
			p.pos.add(f);
		}
		float[] n = nor != null ? nor : new float[] { 0, 0, 1 };
		for (float f : n) {
			p.nor.add(f);
		}
		/* glTF: v = 0 is the top of the image (OBJ: the bottom) */
		p.uv.add(uv != null ? uv[0] : 0);
		p.uv.add(uv != null ? 1 - uv[1] : 0);
		p.vertices.put(spec, index);
		return index;
	}

	/* An element of a list by the index of OBJ (from 1; negative: from the end) */
	private static float[] at(List<float[]> list, String s) {
		if (s == null || s.isEmpty()) {
			return null;
		}
		try {
			int i = Integer.parseInt(s);
			int k = i > 0 ? i - 1 : list.size() + i;
			return k >= 0 && k < list.size() ? list.get(k) : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static float num(String[] t, int i) {
		try {
			return i < t.length ? Float.parseFloat(t[i]) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/* The materials of a .mtl file: Kd (the color), d or Tr (the transparency), map_Kd (the texture) */
	private static void mtl(File file, Map<String, Material> materials) {
		if (!file.isFile()) {
			System.err.println("The materials " + file + " of the OBJ model were not found.");
			return;
		}
		try {
			Material m = null;
			for (String raw : Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1)) {
				String line = raw.strip();
				String[] t = line.split("\\s+");
				if (t[0].equals("newmtl")) {
					m = new Material();
					materials.put(line.substring(t[0].length()).strip(), m);
				} else if (m == null) {
					continue;
				} else if (t[0].equals("Kd") && t.length >= 4) {
					m.color[0] = num(t, 1);
					m.color[1] = num(t, 2);
					m.color[2] = num(t, 3);
				} else if (t[0].equals("d") && t.length >= 2) {
					m.color[3] = num(t, 1);
				} else if (t[0].equals("Tr") && t.length >= 2) {
					m.color[3] = 1 - num(t, 1);
				} else if (t[0].equals("map_Kd") && t.length >= 2) {
					/* The options (e.g., -s 1 1 1) are skipped: the file is the last part */
					m.texture = new File(file.getAbsoluteFile().getParentFile(), t[t.length - 1].replace('\\', '/'));
				}
			}
			/* A texture is shown as it is (as most viewers do; glTF would multiply it with the color Kd) */
			for (Material q : materials.values()) {
				if (q.texture != null) {
					q.color[0] = q.color[1] = q.color[2] = 1;
				}
			}
		} catch (IOException e) {
			System.err.println("The materials " + file + " could not be read: " + e.getMessage());
		}
	}

	/* The texture of a part: PNG and JPEG as they are, other images (e.g., BMP, GIF) converted to PNG */
	private static void texture(Part p, File f) {
		try {
			String n = f.getName().toLowerCase(Locale.ROOT);
			if (n.endsWith(".png")) {
				p.texture = Files.readAllBytes(f.toPath());
				p.mime = "image/png";
			} else if (n.endsWith(".jpg") || n.endsWith(".jpeg")) {
				p.texture = Files.readAllBytes(f.toPath());
				p.mime = "image/jpeg";
			} else {
				java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
				if (img != null) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					javax.imageio.ImageIO.write(img, "png", out);
					p.texture = out.toByteArray();
					p.mime = "image/png";
				} else {
					System.err.println("The texture " + f + " can't be read (PNG or JPEG).");
				}
			}
		} catch (IOException e) {
			System.err.println("The texture " + f + " could not be read: " + e.getMessage());
		}
	}

	/* ===================== STL ===================== */

	/* STL (z up) -> glTF (y up): x, z, -y */
	static List<Part> stl(File file) throws IOException {
		byte[] data = Files.readAllBytes(file.toPath());
		Part p = new Part();
		p.uvs = false;
		ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		boolean binary = data.length >= 84 && 84 + 50L * Integer.toUnsignedLong(bb.getInt(80)) == data.length;
		if (binary) {
			int n = bb.getInt(80);
			for (int i = 0; i < n; i++) {
				int o = 84 + i * 50;
				float[] nn = { bb.getFloat(o), bb.getFloat(o + 4), bb.getFloat(o + 8) };
				float[][] c = new float[3][];
				for (int k = 0; k < 3; k++) {
					int q = o + 12 + k * 12;
					c[k] = new float[] { bb.getFloat(q), bb.getFloat(q + 4), bb.getFloat(q + 8) };
				}
				triangle(p, nn, c);
			}
		} else {
			String text = new String(data, StandardCharsets.ISO_8859_1);
			float[] nn = { 0, 0, 0 };
			List<float[]> c = new ArrayList<>();
			for (String raw : text.split("\\R")) {
				String[] t = raw.strip().split("\\s+");
				if (t[0].equals("facet") && t.length >= 5) {
					nn = new float[] { num(t, 2), num(t, 3), num(t, 4) };
					c.clear();
				} else if (t[0].equals("vertex") && t.length >= 4) {
					c.add(new float[] { num(t, 1), num(t, 2), num(t, 3) });
				} else if (t[0].equals("endfacet") && c.size() >= 3) {
					for (int k = 1; k + 1 < c.size(); k++) {
						triangle(p, nn, new float[][] { c.get(0), c.get(k), c.get(k + 1) });
					}
				}
			}
		}
		if (p.idx.isEmpty()) {
			throw new IOException("no triangles in the STL file");
		}
		return List.of(p);
	}

	private static void triangle(Part p, float[] n, float[][] c) {
		/* The normal of the file or (if missing) of the triangle */
		float nx = n[0], ny = n[1], nz = n[2];
		if (nx == 0 && ny == 0 && nz == 0) {
			float ux = c[1][0] - c[0][0], uy = c[1][1] - c[0][1], uz = c[1][2] - c[0][2];
			float vx = c[2][0] - c[0][0], vy = c[2][1] - c[0][1], vz = c[2][2] - c[0][2];
			nx = uy * vz - uz * vy;
			ny = uz * vx - ux * vz;
			nz = ux * vy - uy * vx;
		}
		double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (l > 0) {
			nx /= l;
			ny /= l;
			nz /= l;
		}
		for (float[] q : c) {
			p.idx.add(p.pos.size() / 3);
			p.pos.add(q[0]);
			p.pos.add(q[2]);
			p.pos.add(-q[1]);
			p.nor.add(nx);
			p.nor.add(nz);
			p.nor.add(-ny);
			p.uv.add(0f);
			p.uv.add(0f);
		}
	}

	/* ===================== glTF writer ===================== */

	/* The parts as binary glTF: one node with one mesh, a primitive and a material per part */
	static byte[] write(List<Part> parts) throws IOException {
		ByteArrayOutputStream bin = new ByteArrayOutputStream();
		StringBuilder views = new StringBuilder(), accessors = new StringBuilder(), prims = new StringBuilder(), mats = new StringBuilder(), images = new StringBuilder(), textures = new StringBuilder();
		int[] count = { 0, 0 }; // bufferViews, accessors
		int nImages = 0;
		for (int k = 0; k < parts.size(); k++) {
			Part p = parts.get(k);
			int n = p.pos.size() / 3;
			float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, max = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
			for (int i = 0; i < n; i++) {
				for (int c = 0; c < 3; c++) {
					float f = p.pos.get(i * 3 + c);
					min[c] = Math.min(min[c], f);
					max[c] = Math.max(max[c], f);
				}
			}
			int pos = floats(bin, views, accessors, count, p.pos, 3, "VEC3", min, max);
			int nor = p.normals ? floats(bin, views, accessors, count, p.nor, 3, "VEC3", null, null) : -1;
			boolean textured = p.texture != null && p.uvs;
			int uv = textured ? floats(bin, views, accessors, count, p.uv, 2, "VEC2", null, null) : -1;
			int idx = indices(bin, views, accessors, count, p.idx);
			int tex = -1;
			if (textured) {
				int view = view(bin, views, count, p.texture, -1);
				images.append(nImages > 0 ? "," : "").append("{\"bufferView\":").append(view).append(",\"mimeType\":\"").append(p.mime).append("\"}");
				textures.append(nImages > 0 ? "," : "").append("{\"source\":").append(nImages).append(",\"sampler\":0}");
				tex = nImages++;
			}
			prims.append(k > 0 ? "," : "").append("{\"attributes\":{\"POSITION\":").append(pos);
			if (nor >= 0) {
				prims.append(",\"NORMAL\":").append(nor);
			}
			if (uv >= 0) {
				prims.append(",\"TEXCOORD_0\":").append(uv);
			}
			prims.append("},\"indices\":").append(idx).append(",\"material\":").append(k).append(",\"mode\":4}");
			mats.append(k > 0 ? "," : "").append("{\"pbrMetallicRoughness\":{\"baseColorFactor\":[").append(clamp(p.color[0])).append(',').append(clamp(p.color[1])).append(',').append(clamp(p.color[2])).append(',').append(clamp(p.color[3])).append(']');
			if (tex >= 0) {
				mats.append(",\"baseColorTexture\":{\"index\":").append(tex).append('}');
			}
			mats.append(",\"metallicFactor\":0,\"roughnessFactor\":0.8}").append(p.color[3] < 1 ? ",\"alphaMode\":\"BLEND\"" : "").append(",\"doubleSided\":true}");
		}
		StringBuilder json = new StringBuilder("{\"asset\":{\"version\":\"2.0\",\"generator\":\"Bio7\"},\"scene\":0,\"scenes\":[{\"nodes\":[0]}],\"nodes\":[{\"mesh\":0}],");
		json.append("\"meshes\":[{\"primitives\":[").append(prims).append("]}],\"materials\":[").append(mats).append("],");
		if (nImages > 0) {
			json.append("\"images\":[").append(images).append("],\"textures\":[").append(textures).append("],\"samplers\":[{\"magFilter\":9729,\"minFilter\":9987}],");
		}
		json.append("\"accessors\":[").append(accessors).append("],\"bufferViews\":[").append(views).append("],\"buffers\":[{\"byteLength\":").append(bin.size()).append("}]}");
		byte[] j = pad(json.toString().getBytes(StandardCharsets.UTF_8), (byte) ' ');
		byte[] b = pad(bin.toByteArray(), (byte) 0);
		ByteBuffer out = ByteBuffer.allocate(12 + 8 + j.length + 8 + b.length).order(ByteOrder.LITTLE_ENDIAN);
		out.putInt(0x46546C67).putInt(2).putInt(out.capacity());
		out.putInt(j.length).putInt(0x4E4F534A).put(j);
		out.putInt(b.length).putInt(0x004E4942).put(b);
		return out.array();
	}

	private static float clamp(float f) {
		return Math.max(0, Math.min(1, f));
	}

	private static int floats(ByteArrayOutputStream bin, StringBuilder views, StringBuilder accessors, int[] count, List<Float> data, int comps, String type, float[] min, float[] max) {
		ByteBuffer bb = ByteBuffer.allocate(data.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
		for (float f : data) {
			bb.putFloat(f);
		}
		int view = view(bin, views, count, bb.array(), 34962);
		accessors.append(count[1] > 0 ? "," : "").append("{\"bufferView\":").append(view).append(",\"componentType\":5126,\"count\":").append(data.size() / comps).append(",\"type\":\"").append(type).append('"');
		if (min != null) {
			accessors.append(",\"min\":[").append(min[0]).append(',').append(min[1]).append(',').append(min[2]).append("],\"max\":[").append(max[0]).append(',').append(max[1]).append(',').append(max[2]).append(']');
		}
		accessors.append('}');
		return count[1]++;
	}

	private static int indices(ByteArrayOutputStream bin, StringBuilder views, StringBuilder accessors, int[] count, List<Integer> data) {
		ByteBuffer bb = ByteBuffer.allocate(data.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
		for (int i : data) {
			bb.putInt(i);
		}
		int view = view(bin, views, count, bb.array(), 34963);
		accessors.append(count[1] > 0 ? "," : "").append("{\"bufferView\":").append(view).append(",\"componentType\":5125,\"count\":").append(data.size()).append(",\"type\":\"SCALAR\"}");
		return count[1]++;
	}

	/* A buffer view (aligned to 4 bytes; target -1: none, e.g., an image) */
	private static int view(ByteArrayOutputStream bin, StringBuilder views, int[] count, byte[] data, int target) {
		while (bin.size() % 4 != 0) {
			bin.write(0);
		}
		views.append(count[0] > 0 ? "," : "").append("{\"buffer\":0,\"byteOffset\":").append(bin.size()).append(",\"byteLength\":").append(data.length);
		if (target > 0) {
			views.append(",\"target\":").append(target);
		}
		views.append('}');
		bin.writeBytes(data);
		return count[0]++;
	}

	private static byte[] pad(byte[] b, byte fill) {
		int n = (b.length + 3) / 4 * 4;
		byte[] out = java.util.Arrays.copyOf(b, n);
		for (int i = b.length; i < n; i++) {
			out[i] = fill;
		}
		return out;
	}
}
