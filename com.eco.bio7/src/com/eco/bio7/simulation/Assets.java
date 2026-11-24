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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The images and the glTF models of the simulations (loaded once, again if the file changed). */
final class Assets {

	private record Entry(long modified, Object value) {
	}

	private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();

	private Assets() {
	}

	/** The image of a file (null if it can't be read). */
	static BufferedImage image(String file) {
		Object o = load(file, true);
		return o instanceof BufferedImage b ? b : null;
	}

	/** The 3D model of a file (glTF; OBJ and STL converted; null if it can't be read). */
	static Gltf model(String file) {
		Object o = load(file, false);
		return o instanceof Gltf g ? g : null;
	}

	private static Object load(String file, boolean image) {
		if (file == null) {
			return null;
		}
		File f = new File(file);
		long modified = f.lastModified();
		Entry e = CACHE.get(file);
		if (e != null && e.modified == modified) {
			return e.value;
		}
		Object value = null;
		try {
			value = image ? javax.imageio.ImageIO.read(f) : ModelConverter.converted(file) ? Gltf.parse(ModelConverter.glb(f), f.getAbsoluteFile().getParentFile()) : Gltf.load(f);
		} catch (Exception ex) {
			System.err.println("The " + (image ? "image" : "3D model") + " " + file + " could not be read: " + ex.getMessage());
		}
		CACHE.put(file, new Entry(modified, value));
		return value;
	}

	/** The pose of a model for an agent: the animation and a phase (the agents do not move in the same rhythm). */
	static List<Gltf.Mesh> pose(Gltf g, String animation, int phase, double seconds, Map<String, List<Gltf.Mesh>> frame) {
		int a = g.animationIndex(animation);
		int group = a < 0 ? 0 : Math.floorMod(phase, 4);
		String key = System.identityHashCode(g) + "/" + a + "/" + group;
		return frame.computeIfAbsent(key, k -> g.pose(a, seconds + group * g.duration(a) / 4));
	}

	/** The color r, g, b, a (0 - 1) of #rrggbb or #rrggbbaa (null: null). */
	static float[] rgba(String color) {
		if (color == null || color.isEmpty()) {
			return null;
		}
		int c = Simulation.rgb(color);
		float a = 1;
		if (color.length() == 9) {
			try {
				a = Integer.parseInt(color.substring(7, 9), 16) / 255f;
			} catch (NumberFormatException e) {
				a = 1;
			}
		}
		return new float[] { ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, a };
	}

	static java.awt.Color awt(String color) {
		float[] c = rgba(color);
		return c == null ? null : new java.awt.Color(c[0], c[1], c[2], c[3]);
	}
}
