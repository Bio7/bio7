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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A 3D model of glTF 2.0 (.glb or .gltf): the meshes (triangles with normals,
 * texture coordinates, the base color and the texture of the material), the
 * nodes, the skins and the animations (translation, rotation and scale of the
 * nodes; linear, step and cubic spline interpolation). {@link #pose(int, double)}
 * returns the meshes of an animation at a time (skinned on the CPU) in the
 * coordinates of the simulation: z up, the front of the model (+z of glTF) in
 * the direction of +y, the model centered with its base at z = 0 and a height
 * of 1.
 */
public final class Gltf {

	/** A mesh of a pose: the triangles and the material. */
	public static final class Mesh {
		/** The positions and the normals (x, y, z per vertex), the texture coordinates (u, v). */
		public float[] positions, normals, uvs;
		/** The indices of the triangles. */
		public int[] indices;
		/** The base color (r, g, b, a: 0 - 1). */
		public float[] color;
		/** The texture (the bytes of a PNG or JPEG image, null: none) and its number in the model. */
		public byte[] texture;
		public int textureIndex = -1;
	}

	/* A primitive of the model (the data of the file) */
	private static final class Primitive {
		float[] pos, nor, uv, weights;
		int[] joints, idx;
		float[] color = { 1, 1, 1, 1 };
		int texture = -1;
	}

	private static final class Node {
		int parent = -1, mesh = -1, skin = -1;
		int[] children = new int[0];
		double[] t = { 0, 0, 0 }, r = { 0, 0, 0, 1 }, s = { 1, 1, 1 };
		double[] matrix;
	}

	private static final class Channel {
		int node, path; // path: 0 translation, 1 rotation, 2 scale
		float[] times, values;
		String interpolation;
	}

	private static final class Animation {
		String name;
		double duration;
		final List<Channel> channels = new ArrayList<>();
	}

	private final List<Node> nodes = new ArrayList<>();
	private final List<List<Primitive>> meshes = new ArrayList<>();
	private final List<int[]> skinJoints = new ArrayList<>();
	private final List<double[][]> skinInverse = new ArrayList<>();
	private final List<Animation> animations = new ArrayList<>();
	private final List<byte[]> textures = new ArrayList<>();
	private int[] roots = new int[0];

	/* The normalization: center and scale (from the rest pose) */
	private double cx, cy, minZ, scale = 1;

	private Gltf() {
	}

	/** Reads a glTF model (.glb or .gltf with embedded or external buffers). */
	public static Gltf load(File file) throws IOException {
		return parse(Files.readAllBytes(file.toPath()), file.getParentFile());
	}

	/** Reads a glTF model from its bytes (a .glb or the JSON of a .gltf with embedded buffers). */
	public static Gltf parse(byte[] data, File folder) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		String json;
		byte[] bin = null;
		if (data.length >= 12 && bb.getInt(0) == 0x46546C67) {
			/* glb: the header, the JSON chunk and the binary chunk */
			int offset = 12;
			int length = bb.getInt(offset);
			json = new String(data, offset + 8, length, StandardCharsets.UTF_8);
			offset += 8 + length;
			if (offset + 8 <= data.length) {
				int binLength = bb.getInt(offset);
				bin = java.util.Arrays.copyOfRange(data, offset + 8, offset + 8 + binLength);
			}
		} else {
			json = new String(data, StandardCharsets.UTF_8);
		}
		Gltf g = new Gltf();
		g.read(new ObjectMapper().readTree(json), bin, folder);
		return g;
	}

	/* ===================== Reading ===================== */

	private void read(JsonNode root, byte[] glbBin, File folder) throws IOException {
		List<byte[]> buffers = new ArrayList<>();
		for (JsonNode b : root.path("buffers")) {
			String uri = b.path("uri").asText(null);
			if (uri == null) {
				buffers.add(glbBin == null ? new byte[0] : glbBin);
			} else {
				buffers.add(uriBytes(uri, folder));
			}
		}
		Accessors acc = new Accessors(root, buffers);
		/* The textures (the bytes of the images) */
		for (JsonNode t : root.path("textures")) {
			int source = t.path("source").asInt(-1);
			JsonNode img = source >= 0 ? root.path("images").path(source) : null;
			byte[] bytes = null;
			if (img != null && !img.isMissingNode()) {
				if (img.has("bufferView")) {
					bytes = acc.view(img.get("bufferView").asInt());
				} else if (img.has("uri")) {
					bytes = uriBytes(img.get("uri").asText(), folder);
				}
			}
			textures.add(bytes);
		}
		/* The meshes */
		JsonNode materials = root.path("materials");
		for (JsonNode m : root.path("meshes")) {
			List<Primitive> prims = new ArrayList<>();
			for (JsonNode p : m.path("primitives")) {
				if (p.path("mode").asInt(4) != 4) {
					continue;
				}
				JsonNode a = p.path("attributes");
				if (!a.has("POSITION")) {
					continue;
				}
				Primitive q = new Primitive();
				q.pos = acc.floats(a.get("POSITION").asInt());
				q.nor = a.has("NORMAL") ? acc.floats(a.get("NORMAL").asInt()) : null;
				q.uv = a.has("TEXCOORD_0") ? acc.floats(a.get("TEXCOORD_0").asInt()) : null;
				if (a.has("JOINTS_0") && a.has("WEIGHTS_0")) {
					float[] j = acc.floats(a.get("JOINTS_0").asInt());
					q.joints = new int[j.length];
					for (int i = 0; i < j.length; i++) {
						q.joints[i] = Math.round(j[i]);
					}
					q.weights = acc.floats(a.get("WEIGHTS_0").asInt());
				}
				int n = q.pos.length / 3;
				if (p.has("indices")) {
					float[] f = acc.floats(p.get("indices").asInt());
					q.idx = new int[f.length];
					for (int i = 0; i < f.length; i++) {
						q.idx[i] = (int) f[i];
					}
				} else {
					q.idx = new int[n];
					for (int i = 0; i < n; i++) {
						q.idx[i] = i;
					}
				}
				if (q.nor == null) {
					q.nor = flatNormals(q.pos, q.idx);
				}
				if (p.has("material")) {
					JsonNode mat = materials.path(p.get("material").asInt());
					JsonNode pbr = mat.path("pbrMetallicRoughness");
					JsonNode f = pbr.path("baseColorFactor");
					if (f.isArray() && f.size() == 4) {
						for (int i = 0; i < 4; i++) {
							q.color[i] = (float) f.get(i).asDouble();
						}
					}
					if (pbr.has("baseColorTexture") && q.uv != null) {
						q.texture = pbr.path("baseColorTexture").path("index").asInt(-1);
					}
				}
				prims.add(q);
			}
			meshes.add(prims);
		}
		/* The nodes */
		for (JsonNode n : root.path("nodes")) {
			Node node = new Node();
			node.mesh = n.path("mesh").asInt(-1);
			node.skin = n.path("skin").asInt(-1);
			if (n.has("children")) {
				node.children = new int[n.get("children").size()];
				for (int i = 0; i < node.children.length; i++) {
					node.children[i] = n.get("children").get(i).asInt();
				}
			}
			if (n.has("matrix")) {
				node.matrix = new double[16];
				for (int i = 0; i < 16; i++) {
					node.matrix[i] = n.get("matrix").get(i).asDouble();
				}
			}
			read(n.path("translation"), node.t);
			read(n.path("rotation"), node.r);
			read(n.path("scale"), node.s);
			nodes.add(node);
		}
		for (int i = 0; i < nodes.size(); i++) {
			for (int c : nodes.get(i).children) {
				if (c >= 0 && c < nodes.size()) {
					nodes.get(c).parent = i;
				}
			}
		}
		JsonNode scenes = root.path("scenes");
		JsonNode scene = scenes.path(root.path("scene").asInt(0));
		if (scene.has("nodes")) {
			roots = new int[scene.get("nodes").size()];
			for (int i = 0; i < roots.length; i++) {
				roots[i] = scene.get("nodes").get(i).asInt();
			}
		} else {
			roots = java.util.stream.IntStream.range(0, nodes.size()).filter(i -> nodes.get(i).parent < 0).toArray();
		}
		/* The skins */
		for (JsonNode s : root.path("skins")) {
			int[] joints = new int[s.path("joints").size()];
			for (int i = 0; i < joints.length; i++) {
				joints[i] = s.get("joints").get(i).asInt();
			}
			double[][] inv = new double[joints.length][];
			if (s.has("inverseBindMatrices")) {
				float[] f = acc.floats(s.get("inverseBindMatrices").asInt());
				for (int j = 0; j < joints.length; j++) {
					inv[j] = new double[16];
					for (int k = 0; k < 16; k++) {
						inv[j][k] = f[j * 16 + k];
					}
				}
			} else {
				for (int j = 0; j < joints.length; j++) {
					inv[j] = identity();
				}
			}
			skinJoints.add(joints);
			skinInverse.add(inv);
		}
		/* The animations */
		int k = 0;
		for (JsonNode a : root.path("animations")) {
			Animation anim = new Animation();
			anim.name = a.path("name").asText("animation " + k);
			k++;
			JsonNode samplers = a.path("samplers");
			for (JsonNode c : a.path("channels")) {
				String path = c.path("target").path("path").asText();
				int p = path.equals("translation") ? 0 : path.equals("rotation") ? 1 : path.equals("scale") ? 2 : -1;
				if (p < 0 || !c.path("target").has("node")) {
					continue;
				}
				JsonNode sm = samplers.path(c.path("sampler").asInt());
				Channel ch = new Channel();
				ch.node = c.path("target").path("node").asInt();
				ch.path = p;
				ch.times = acc.floats(sm.path("input").asInt());
				ch.values = acc.floats(sm.path("output").asInt());
				ch.interpolation = sm.path("interpolation").asText("LINEAR");
				if (ch.times.length > 0) {
					anim.duration = Math.max(anim.duration, ch.times[ch.times.length - 1]);
				}
				anim.channels.add(ch);
			}
			animations.add(anim);
		}
		normalize();
	}

	private static void read(JsonNode n, double[] target) {
		if (n.isArray() && n.size() == target.length) {
			for (int i = 0; i < target.length; i++) {
				target[i] = n.get(i).asDouble();
			}
		}
	}

	private static byte[] uriBytes(String uri, File folder) throws IOException {
		if (uri.startsWith("data:")) {
			return Base64.getDecoder().decode(uri.substring(uri.indexOf(',') + 1));
		}
		File f = new File(folder, java.net.URLDecoder.decode(uri, StandardCharsets.UTF_8));
		return Files.readAllBytes(f.toPath());
	}

	/* The accessors of the buffers */
	private static final class Accessors {
		final JsonNode root;
		final List<byte[]> buffers;

		Accessors(JsonNode root, List<byte[]> buffers) {
			this.root = root;
			this.buffers = buffers;
		}

		byte[] view(int index) {
			JsonNode v = root.path("bufferViews").path(index);
			byte[] b = buffers.get(v.path("buffer").asInt());
			int off = v.path("byteOffset").asInt(0);
			return java.util.Arrays.copyOfRange(b, off, off + v.path("byteLength").asInt());
		}

		float[] floats(int index) {
			JsonNode a = root.path("accessors").path(index);
			int count = a.path("count").asInt();
			String type = a.path("type").asText();
			int comps = switch (type) {
			case "SCALAR" -> 1;
			case "VEC2" -> 2;
			case "VEC3" -> 3;
			case "VEC4", "MAT2" -> 4;
			case "MAT3" -> 9;
			case "MAT4" -> 16;
			default -> 1;
			};
			int ct = a.path("componentType").asInt(5126);
			boolean normalized = a.path("normalized").asBoolean(false);
			float[] out = new float[count * comps];
			if (!a.has("bufferView")) {
				return out;
			}
			JsonNode v = root.path("bufferViews").path(a.get("bufferView").asInt());
			ByteBuffer b = ByteBuffer.wrap(buffers.get(v.path("buffer").asInt())).order(ByteOrder.LITTLE_ENDIAN);
			int size = switch (ct) {
			case 5120, 5121 -> 1;
			case 5122, 5123 -> 2;
			default -> 4;
			};
			int stride = v.path("byteStride").asInt(0);
			if (stride == 0) {
				stride = size * comps;
			}
			int base = v.path("byteOffset").asInt(0) + a.path("byteOffset").asInt(0);
			for (int i = 0; i < count; i++) {
				for (int c = 0; c < comps; c++) {
					int o = base + i * stride + c * size;
					float f = switch (ct) {
					case 5120 -> normalized ? Math.max(b.get(o) / 127f, -1) : b.get(o);
					case 5121 -> normalized ? (b.get(o) & 0xff) / 255f : (b.get(o) & 0xff);
					case 5122 -> normalized ? Math.max(b.getShort(o) / 32767f, -1) : b.getShort(o);
					case 5123 -> normalized ? (b.getShort(o) & 0xffff) / 65535f : (b.getShort(o) & 0xffff);
					case 5125 -> (float) (b.getInt(o) & 0xffffffffL);
					default -> b.getFloat(o);
					};
					out[i * comps + c] = f;
				}
			}
			return out;
		}
	}

	private static float[] flatNormals(float[] pos, int[] idx) {
		float[] n = new float[pos.length];
		for (int i = 0; i + 2 < idx.length; i += 3) {
			int a = idx[i] * 3, b = idx[i + 1] * 3, c = idx[i + 2] * 3;
			float ux = pos[b] - pos[a], uy = pos[b + 1] - pos[a + 1], uz = pos[b + 2] - pos[a + 2];
			float vx = pos[c] - pos[a], vy = pos[c + 1] - pos[a + 1], vz = pos[c + 2] - pos[a + 2];
			float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
			for (int k : new int[] { a, b, c }) {
				n[k] += nx;
				n[k + 1] += ny;
				n[k + 2] += nz;
			}
		}
		return n;
	}

	/* ===================== The animations ===================== */

	/** The names of the animations. */
	public String[] animationNames() {
		return animations.stream().map(a -> a.name).toArray(String[]::new);
	}

	/** The duration of an animation in seconds (0 without animations). */
	public double duration(int animation) {
		return animation >= 0 && animation < animations.size() ? animations.get(animation).duration : 0;
	}

	/**
	 * The number of an animation: its name (also a part of it, ignoring the case)
	 * or number; null or "" the first animation, "none" (or a model without
	 * animations) -1 (the rest pose).
	 */
	public int animationIndex(String animation) {
		if (animations.isEmpty() || "none".equalsIgnoreCase(animation)) {
			return -1;
		}
		if (animation == null || animation.isEmpty()) {
			return 0;
		}
		for (int i = 0; i < animations.size(); i++) {
			if (animations.get(i).name.equalsIgnoreCase(animation)) {
				return i;
			}
		}
		for (int i = 0; i < animations.size(); i++) {
			if (animations.get(i).name.toLowerCase(java.util.Locale.ROOT).contains(animation.toLowerCase(java.util.Locale.ROOT))) {
				return i;
			}
		}
		try {
			int k = Integer.parseInt(animation.trim());
			return k >= 0 && k < animations.size() ? k : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * The meshes of the model in the pose of the animation at the time (seconds,
	 * repeated; animation -1: the rest pose) in the coordinates of the
	 * simulation (z up, the front to +y, the base at z = 0, the height 1).
	 */
	public List<Mesh> pose(int animation, double time) {
		int n = nodes.size();
		double[][] local = new double[n][];
		double[][] tr = new double[n][], ro = new double[n][], sc = new double[n][];
		for (int i = 0; i < n; i++) {
			Node node = nodes.get(i);
			tr[i] = node.t.clone();
			ro[i] = node.r.clone();
			sc[i] = node.s.clone();
		}
		boolean[] animated = new boolean[n];
		if (animation >= 0 && animation < animations.size()) {
			Animation a = animations.get(animation);
			double tt = a.duration > 0 ? time % a.duration : 0;
			if (tt < 0) {
				tt += a.duration;
			}
			for (Channel c : a.channels) {
				if (c.node < 0 || c.node >= n) {
					continue;
				}
				double[] v = sample(c, tt);
				if (c.path == 0) {
					tr[c.node] = v;
				} else if (c.path == 1) {
					ro[c.node] = v;
				} else {
					sc[c.node] = v;
				}
				animated[c.node] = true;
			}
		}
		for (int i = 0; i < n; i++) {
			Node node = nodes.get(i);
			local[i] = node.matrix != null && !animated[i] ? node.matrix : trs(tr[i], ro[i], sc[i]);
		}
		double[][] global = new double[n][];
		for (int r : roots) {
			globals(r, identity(), local, global);
		}
		for (int i = 0; i < n; i++) {
			if (global[i] == null) {
				globals(i, identity(), local, global);
			}
		}
		List<Mesh> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			Node node = nodes.get(i);
			if (node.mesh < 0 || node.mesh >= meshes.size()) {
				continue;
			}
			double[][] joints = null;
			if (node.skin >= 0 && node.skin < skinJoints.size()) {
				int[] js = skinJoints.get(node.skin);
				double[][] inv = skinInverse.get(node.skin);
				joints = new double[js.length][];
				for (int j = 0; j < js.length; j++) {
					joints[j] = mul(global[js[j]] != null ? global[js[j]] : identity(), inv[j]);
				}
			}
			for (Primitive p : meshes.get(node.mesh)) {
				out.add(posed(p, global[i], joints));
			}
		}
		return out;
	}

	private void globals(int i, double[] parent, double[][] local, double[][] global) {
		if (global[i] != null) {
			return;
		}
		global[i] = mul(parent, local[i]);
		for (int c : nodes.get(i).children) {
			if (c >= 0 && c < nodes.size()) {
				globals(c, global[i], local, global);
			}
		}
	}

	/* The value of a channel at the time */
	private static double[] sample(Channel c, double t) {
		int comps = c.path == 1 ? 4 : 3;
		boolean cubic = "CUBICSPLINE".equals(c.interpolation);
		int stride = cubic ? comps * 3 : comps, off = cubic ? comps : 0;
		float[] ts = c.times;
		int k = 0;
		while (k + 1 < ts.length && ts[k + 1] <= t) {
			k++;
		}
		double[] a = new double[comps];
		for (int i = 0; i < comps; i++) {
			a[i] = c.values[Math.min(c.values.length - 1, k * stride + off + i)];
		}
		if (k + 1 >= ts.length || "STEP".equals(c.interpolation) || t <= ts[0]) {
			return a;
		}
		double w = (t - ts[k]) / Math.max(1e-9, ts[k + 1] - ts[k]);
		double[] b = new double[comps];
		for (int i = 0; i < comps; i++) {
			b[i] = c.values[Math.min(c.values.length - 1, (k + 1) * stride + off + i)];
		}
		if (c.path == 1) {
			/* The rotation: the shorter way, normalized */
			double d = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
			double sgn = d < 0 ? -1 : 1, l = 0;
			double[] q = new double[4];
			for (int i = 0; i < 4; i++) {
				q[i] = a[i] * (1 - w) + sgn * b[i] * w;
				l += q[i] * q[i];
			}
			l = Math.sqrt(l);
			for (int i = 0; i < 4; i++) {
				q[i] /= l;
			}
			return q;
		}
		for (int i = 0; i < comps; i++) {
			a[i] = a[i] * (1 - w) + b[i] * w;
		}
		return a;
	}

	/* A mesh of a primitive in the pose (in the coordinates of the simulation) */
	private Mesh posed(Primitive p, double[] world, double[][] joints) {
		int n = p.pos.length / 3;
		Mesh m = new Mesh();
		m.positions = new float[n * 3];
		m.normals = new float[n * 3];
		double[] mt = world;
		for (int v = 0; v < n; v++) {
			double x = p.pos[v * 3], y = p.pos[v * 3 + 1], z = p.pos[v * 3 + 2];
			double nx = p.nor[v * 3], ny = p.nor[v * 3 + 1], nz = p.nor[v * 3 + 2];
			if (joints != null) {
				/* The skinned matrix of the vertex: the weighted joint matrices */
				mt = new double[16];
				for (int k = 0; k < 4; k++) {
					double w = p.weights[v * 4 + k];
					int j = p.joints[v * 4 + k];
					if (w == 0 || j < 0 || j >= joints.length) {
						continue;
					}
					for (int q = 0; q < 16; q++) {
						mt[q] += w * joints[j][q];
					}
				}
			}
			double px = mt[0] * x + mt[4] * y + mt[8] * z + mt[12], py = mt[1] * x + mt[5] * y + mt[9] * z + mt[13], pz = mt[2] * x + mt[6] * y + mt[10] * z + mt[14];
			double qx = mt[0] * nx + mt[4] * ny + mt[8] * nz, qy = mt[1] * nx + mt[5] * ny + mt[9] * nz, qz = mt[2] * nx + mt[6] * ny + mt[10] * nz;
			double l = Math.sqrt(qx * qx + qy * qy + qz * qz);
			if (l > 0) {
				qx /= l;
				qy /= l;
				qz /= l;
			}
			/* glTF (y up, front +z) -> the simulation (z up, front +y): (x, y, z) -> (-x, z, y) */
			m.positions[v * 3] = (float) ((-px - cx) * scale);
			m.positions[v * 3 + 1] = (float) ((pz - cy) * scale);
			m.positions[v * 3 + 2] = (float) ((py - minZ) * scale);
			m.normals[v * 3] = (float) -qx;
			m.normals[v * 3 + 1] = (float) qz;
			m.normals[v * 3 + 2] = (float) qy;
		}
		m.uvs = p.uv;
		m.indices = p.idx;
		m.color = p.color;
		if (p.texture >= 0 && p.texture < textures.size()) {
			m.texture = textures.get(p.texture);
			m.textureIndex = p.texture;
		}
		return m;
	}

	/* The center and the height of the rest pose (or of the first animation at 0) */
	private void normalize() {
		cx = 0;
		cy = 0;
		minZ = 0;
		scale = 1;
		List<Mesh> rest = pose(animations.isEmpty() ? -1 : 0, 0);
		double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y0 = Double.MAX_VALUE, y1 = -Double.MAX_VALUE, z0 = Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
		for (Mesh m : rest) {
			for (int i = 0; i + 2 < m.positions.length; i += 3) {
				x0 = Math.min(x0, m.positions[i]);
				x1 = Math.max(x1, m.positions[i]);
				y0 = Math.min(y0, m.positions[i + 1]);
				y1 = Math.max(y1, m.positions[i + 1]);
				z0 = Math.min(z0, m.positions[i + 2]);
				z1 = Math.max(z1, m.positions[i + 2]);
			}
		}
		if (x0 == Double.MAX_VALUE) {
			return;
		}
		/* The positions are (-px, pz, py) without an offset: the center of these coordinates */
		cx = (x0 + x1) / 2;
		cy = (y0 + y1) / 2;
		minZ = z0;
		scale = 1 / Math.max(1e-12, z1 - z0);
	}

	/* ===================== Matrices (column major as glTF) ===================== */

	private static double[] identity() {
		return new double[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
	}

	private static double[] mul(double[] a, double[] b) {
		double[] r = new double[16];
		for (int c = 0; c < 4; c++) {
			for (int row = 0; row < 4; row++) {
				double s = 0;
				for (int k = 0; k < 4; k++) {
					s += a[k * 4 + row] * b[c * 4 + k];
				}
				r[c * 4 + row] = s;
			}
		}
		return r;
	}

	private static double[] trs(double[] t, double[] q, double[] s) {
		double x = q[0], y = q[1], z = q[2], w = q[3];
		return new double[] { (1 - 2 * (y * y + z * z)) * s[0], (2 * (x * y + z * w)) * s[0], (2 * (x * z - y * w)) * s[0], 0, (2 * (x * y - z * w)) * s[1], (1 - 2 * (x * x + z * z)) * s[1],
				(2 * (y * z + x * w)) * s[1], 0, (2 * (x * z + y * w)) * s[2], (2 * (y * z - x * w)) * s[2], (1 - 2 * (x * x + y * y)) * s[2], 0, t[0], t[1], t[2], 1 };
	}
}
