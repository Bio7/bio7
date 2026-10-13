/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.image.thumbs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.eclipse.swt.graphics.ImageData;

/**
 * Caches the thumbnails in memory (least recently used, limited size) and on
 * disk in the state location of the plugin. A thumbnail is identified by the
 * path, the modification time and the length of the file and the size of the
 * thumbnail in pixels, so that a changed file creates a new thumbnail. All
 * methods are thread safe.
 */
final class ThumbnailCache {

	private static final int MAGIC = 0xB107C0DE;
	private static final int VERSION = 1;
	/* The limits of the caches! */
	/* At most 256 MB and 1/8 of the heap (the grid also keeps released thumbnails softly)! */
	private static final long MEMORY_LIMIT = Math.min(256L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 8);
	private static final long DISK_LIMIT = 512L * 1024 * 1024;
	private static final int MAX_INFO = 16000;

	private static final LinkedHashMap<String, ThumbData> memory = new LinkedHashMap<String, ThumbData>(256, 0.75f, true);
	private static long memoryBytes = 0;
	private static File directory;
	private static boolean pruned = false;

	private ThumbnailCache() {
	}

	/** The key of a thumbnail of the file with the given size in pixels. */
	static String key(File file, int pixelSize) {
		return file.getAbsolutePath() + "|" + file.lastModified() + "|" + file.length() + "|" + pixelSize;
	}

	/** Returns the thumbnail from memory or disk, null if not cached. */
	static ThumbData get(String key) {
		synchronized (memory) {
			ThumbData d = memory.get(key);
			if (d != null) {
				return d;
			}
		}
		ThumbData d = read(key);
		if (d != null) {
			putMemory(key, d);
		}
		return d;
	}

	/** Stores the thumbnail in memory and on disk. */
	static void put(String key, ThumbData d) {
		putMemory(key, d);
		write(key, d);
	}

	private static void putMemory(String key, ThumbData d) {
		synchronized (memory) {
			ThumbData old = memory.put(key, d);
			if (old != null) {
				memoryBytes -= old.bytes();
			}
			memoryBytes += d.bytes();
			for (Iterator<Map.Entry<String, ThumbData>> it = memory.entrySet().iterator(); memoryBytes > MEMORY_LIMIT && it.hasNext();) {
				memoryBytes -= it.next().getValue().bytes();
				it.remove();
			}
		}
	}

	/** Removes all thumbnails from memory and disk. */
	static void clear() {
		synchronized (memory) {
			memory.clear();
			memoryBytes = 0;
		}
		File dir = directory();
		File[] files = dir == null ? null : dir.listFiles();
		if (files != null) {
			for (File f : files) {
				f.delete();
			}
		}
	}

	/* The cache folder in the state location of the plugin (null if not available)! */
	private static synchronized File directory() {
		if (directory == null) {
			try {
				directory = com.eco.bio7.image.ui.Activator.getDefault().getStateLocation().append("thumbcache").toFile();
			} catch (RuntimeException e) {
				directory = new File(System.getProperty("java.io.tmpdir"), "bio7thumbcache");
			}
			directory.mkdirs();
		}
		return directory;
	}

	private static File file(String key) {
		File dir = directory();
		if (dir == null) {
			return null;
		}
		try {
			byte[] hash = MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8));
			StringBuilder name = new StringBuilder();
			for (byte b : hash) {
				name.append(String.format("%02x", b));
			}
			return new File(dir, name + ".thumb");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	private static ThumbData read(String key) {
		File f = file(key);
		if (f == null || !f.isFile()) {
			return null;
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(f))))) {
			if (in.readInt() != MAGIC || in.readInt() != VERSION || !in.readUTF().equals(key)) {
				return null;
			}
			int w = in.readInt(), h = in.readInt(), width = in.readInt(), height = in.readInt();
			boolean stack = in.readBoolean();
			String info = in.readBoolean() ? in.readUTF() : null;
			byte[] rgb = new byte[w * h * 3];
			in.readFully(rgb);
			int[] pixels = new int[w * h];
			for (int i = 0, j = 0; i < pixels.length; i++, j += 3) {
				pixels[i] = ((rgb[j] & 0xFF) << 16) | ((rgb[j + 1] & 0xFF) << 8) | (rgb[j + 2] & 0xFF);
			}
			/* Marks the entry as recently used for the pruning! */
			f.setLastModified(System.currentTimeMillis());
			return new ThumbData(ThumbData.fromRGB(pixels, w, h), width, height, stack, info);
		} catch (IOException | RuntimeException e) {
			f.delete();
			return null;
		}
	}

	private static void write(String key, ThumbData d) {
		File f = file(key);
		if (f == null) {
			return;
		}
		ImageData data = d.data;
		File tmp = new File(f.getPath() + ".tmp" + Thread.currentThread().getId());
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(new FileOutputStream(tmp))))) {
			out.writeInt(MAGIC);
			out.writeInt(VERSION);
			out.writeUTF(key);
			out.writeInt(data.width);
			out.writeInt(data.height);
			out.writeInt(d.width);
			out.writeInt(d.height);
			out.writeBoolean(d.stack);
			out.writeBoolean(d.info != null);
			if (d.info != null) {
				out.writeUTF(d.info.length() > MAX_INFO ? d.info.substring(0, MAX_INFO) : d.info);
			}
			int[] row = new int[data.width];
			byte[] rgb = new byte[data.width * 3];
			for (int y = 0; y < data.height; y++) {
				data.getPixels(0, y, data.width, row, 0);
				for (int x = 0, j = 0; x < data.width; x++, j += 3) {
					rgb[j] = (byte) (row[x] >> 16);
					rgb[j + 1] = (byte) (row[x] >> 8);
					rgb[j + 2] = (byte) row[x];
				}
				out.write(rgb);
			}
		} catch (IOException | RuntimeException e) {
			tmp.delete();
			return;
		}
		if (!tmp.renameTo(f)) {
			tmp.delete();
		}
	}

	/** Deletes the least recently used files if the disk cache is too big (once per session). */
	static void prune() {
		synchronized (ThumbnailCache.class) {
			if (pruned) {
				return;
			}
			pruned = true;
		}
		File dir = directory();
		File[] files = dir == null ? null : dir.listFiles();
		if (files == null) {
			return;
		}
		long total = 0;
		for (File f : files) {
			total += f.length();
		}
		if (total <= DISK_LIMIT) {
			return;
		}
		Arrays.sort(files, Comparator.comparingLong(File::lastModified));
		for (File f : files) {
			if (total <= DISK_LIMIT * 3 / 4) {
				break;
			}
			total -= f.length();
			f.delete();
		}
	}
}
