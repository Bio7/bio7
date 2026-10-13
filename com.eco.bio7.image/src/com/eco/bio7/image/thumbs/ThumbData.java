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

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/**
 * The pixels and the information of a thumbnail. The pixels are stored as
 * {@link ImageData} (no operating system handle) in the format
 * {@link #PALETTE} with 24 bits, so that thumbnails can be copied row by row
 * into the image of the visible area (see {@link ThumbnailGrid}).
 */
final class ThumbData {

	/** The pixel format of all thumbnails (0xRRGGBB like an ImageJ ColorProcessor). */
	static final PaletteData PALETTE = new PaletteData(0xFF0000, 0xFF00, 0xFF);

	/** The thumbnail pixels (24 bit, {@link #PALETTE}). */
	final ImageData data;
	/** The size of the original image (0 if unknown). */
	final int width, height;
	/** True if the file is a stack, hyperstack or a composite image. */
	final boolean stack;
	/** The text of the tooltip (null for the file information). */
	final String info;

	ThumbData(ImageData data, int width, int height, boolean stack, String info) {
		this.data = data;
		this.width = width;
		this.height = height;
		this.stack = stack;
		this.info = info;
	}

	/** Creates the image data from 0xRRGGBB pixels. */
	static ImageData fromRGB(int[] pixels, int w, int h) {
		ImageData data = new ImageData(w, h, 24, PALETTE);
		for (int y = 0; y < h; y++) {
			data.setPixels(0, y, w, pixels, y * w);
		}
		return data;
	}

	/** Converts image data of any format (e.g., a loaded JPEG icon) to the thumbnail format. */
	static ImageData convert(ImageData src) {
		int[] pixels = new int[src.width * src.height];
		int[] row = new int[src.width];
		PaletteData palette = src.palette;
		for (int y = 0; y < src.height; y++) {
			src.getPixels(0, y, src.width, row, 0);
			for (int x = 0; x < src.width; x++) {
				RGB rgb = palette.getRGB(row[x]);
				pixels[y * src.width + x] = (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
			}
		}
		return fromRGB(pixels, src.width, src.height);
	}

	/** The number of bytes of the pixels (for the memory cache). */
	long bytes() {
		return (long) data.bytesPerLine * data.height;
	}
}
