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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.swt.graphics.ImageData;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.plugin.AVI_Reader;
import ij.plugin.DICOM;
import ij.plugin.ImageInfo;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.LUT;

/**
 * Creates the thumbnail of a file in the background (no SWT resources). The
 * images are opened with ImageJ and scaled with averaging. Thumbnails are
 * cached by {@link ThumbnailCache}. Thread safe.
 */
final class ThumbnailLoader {

	/* The icons for files without a preview (shared, by name and size)! */
	private static final Map<String, ThumbData> icons = new HashMap<String, ThumbData>();

	private final int pixelSize;
	private final String lutSamplePath;

	/**
	 * @param pixelSize     the size of the thumbnails in pixels
	 * @param lutSamplePath the image which shows a LUT (null to show the error icon)
	 */
	ThumbnailLoader(int pixelSize, String lutSamplePath) {
		this.pixelSize = pixelSize;
		this.lutSamplePath = lutSamplePath;
	}

	/** Returns the cached thumbnail or creates (and caches) it. */
	ThumbData load(File file) {
		String key = ThumbnailCache.key(file, pixelSize);
		ThumbData d = ThumbnailCache.get(key);
		if (d != null) {
			return d;
		}
		d = create(file);
		/* Icons (also the error icon) are not cached, a file can become readable! */
		if (!isIcon(d)) {
			ThumbnailCache.put(key, d);
		}
		return d;
	}

	private static boolean isIcon(ThumbData d) {
		synchronized (icons) {
			return icons.containsValue(d);
		}
	}

	private ThumbData create(File file) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		String ext = dot >= 0 ? name.substring(dot).toLowerCase() : "";
		try {
			switch (ext) {
			case ".txt":
				return icon("txt");
			case ".roi":
				return icon("roi");
			case ".zip":
				return fromZip(file);
			case ".avi":
				ImageStack stack = new AVI_Reader().makeStack(file.getAbsolutePath(), 1, 1, true, false, false);
				return stack == null ? icon("error") : fromImage(new ImagePlus("avi", stack), true);
			case ".lut":
				ImagePlus sample = lutSamplePath == null ? null : openSilent(lutSamplePath);
				if (sample == null) {
					return icon("error");
				}
				LUT lut = Opener.openLut(file.getAbsolutePath());
				if (lut == null) {
					return icon("error");
				}
				sample.setLut(lut);
				return fromImage(sample, false);
			default:
				return fromImage(openSilent(file.getAbsolutePath()), false);
			}
		} catch (Exception | LinkageError e) {
			if (Boolean.getBoolean("bio7.thumbnails.debug")) {
				e.printStackTrace();
			}
			return icon("error");
		}
	}

	private static ImagePlus openSilent(String path) {
		Opener opener = new Opener();
		opener.setSilentMode(true);
		return opener.openImage(path);
	}

	private ThumbData fromImage(ImagePlus plus, boolean forceStack) {
		if (plus == null) {
			return icon("error");
		}
		boolean stack = forceStack || plus.getStackSize() > 1 || plus.isHyperStack() || plus.isComposite();
		String info = new ImageInfo().getImageInfo(plus);
		int width = plus.getWidth(), height = plus.getHeight();
		ImageProcessor rgb;
		if (plus.isComposite()) {
			/* The channels are combined like they are displayed! */
			new ImageConverter(plus).convertToRGB();
			rgb = plus.getProcessor();
		} else {
			/* Only the current slice is converted (uses the LUT and the display range)! */
			rgb = plus.getProcessor().convertToRGB();
		}
		int[] scaled = fit(width, height, pixelSize);
		if (scaled[0] != width || scaled[1] != height) {
			rgb.setInterpolationMethod(ImageProcessor.BILINEAR);
			rgb = rgb.resize(scaled[0], scaled[1], true);
		}
		int[] pixels = (int[]) ((ColorProcessor) (rgb instanceof ColorProcessor ? rgb : rgb.convertToRGB())).getPixels();
		ImageData data = ThumbData.fromRGB(pixels, rgb.getWidth(), rgb.getHeight());
		plus.flush();
		return new ThumbData(data, width, height, stack, info);
	}

	/** The size of an image which fits into a square (images are not enlarged). */
	static int[] fit(int w, int h, int size) {
		if (w <= size && h <= size) {
			return new int[] { w, h };
		}
		double f = (double) size / Math.max(w, h);
		return new int[] { Math.max(1, (int) Math.round(w * f)), Math.max(1, (int) Math.round(h * f)) };
	}

	/* An icon of the plugin (txt, roi, error) in the thumbnail size! */
	private ThumbData icon(String kind) {
		String key = kind + pixelSize;
		synchronized (icons) {
			ThumbData d = icons.get(key);
			if (d != null) {
				return d;
			}
			/* The smallest icon which is not smaller than the thumbnail (50, 100 or 200 pixels)! */
			int res = pixelSize <= 50 ? 50 : pixelSize <= 100 ? 100 : 200;
			ImageData data;
			try (InputStream in = ThumbnailLoader.class.getResourceAsStream("/images/" + kind + res + ".jpg")) {
				data = ThumbData.convert(new ImageData(in));
			} catch (IOException | RuntimeException e) {
				int[] gray = new int[pixelSize * pixelSize];
				java.util.Arrays.fill(gray, 0x808080);
				data = ThumbData.fromRGB(gray, pixelSize, pixelSize);
			}
			if (data.width != pixelSize) {
				data = data.scaledTo(pixelSize, Math.max(1, data.height * pixelSize / data.width));
			}
			d = new ThumbData(data, 0, 0, false, null);
			icons.put(key, d);
			return d;
		}
	}

	/*
	 * Opens a ZIP archive like ImageJ (a TIFF or DICOM image as the first entry, a
	 * ROI set) without the dialogs and log messages of ImageJ. ZipFile is used
	 * because it also reads archives which ZipInputStream cannot read (e.g.,
	 * "only DEFLATED entries can have EXT descriptor"). Messages are printed to the
	 * Bio7 console (System.out)!
	 */
	private ThumbData fromZip(File file) {
		try (ZipFile zip = new ZipFile(file)) {
			ZipEntry first = null;
			for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements();) {
				ZipEntry entry = en.nextElement();
				if (entry.getName().toLowerCase().endsWith(".roi")) {
					return icon("roi");
				}
				if (first == null && !entry.isDirectory()) {
					first = entry;
				}
			}
			String name = first == null ? "" : first.getName();
			String lower = name.toLowerCase();
			ImagePlus imp = null;
			if (lower.endsWith(".tif") || lower.endsWith(".tiff")) {
				try (InputStream in = zip.getInputStream(first)) {
					imp = new Opener().openTiff(in, name);
				}
			} else if (lower.endsWith(".dcm")) {
				try (InputStream in = zip.getInputStream(first)) {
					DICOM dcm = new DICOM(in);
					dcm.run(name);
					imp = dcm.getWidth() > 0 ? dcm : null;
				}
			} else {
				message(file, "the ZIP archive does not contain a TIFF or DICOM image or a ROI set");
				return icon("zip");
			}
			if (imp == null) {
				message(file, "the image " + name + " in the ZIP archive cannot be opened");
				return icon("zip");
			}
			return fromImage(imp, false);
		} catch (IOException e) {
			message(file, "the ZIP archive cannot be read (" + e.getMessage() + ")");
			return icon("zip");
		}
	}

	/* The files which have been reported (a message once per file version and session)! */
	private static final Set<String> reported = new HashSet<String>();

	/* A message for the Bio7 console (System.out is displayed there)! */
	private static void message(File file, String text) {
		synchronized (reported) {
			if (!reported.add(file.getAbsolutePath() + "|" + file.lastModified())) {
				return;
			}
		}
		System.out.println("Thumbnails: " + file.getAbsolutePath() + ": " + text);
	}
}
