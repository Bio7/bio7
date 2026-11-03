/********************************************************************************
 * Copyright (c) 2026 Marcel Austenfeld
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ********************************************************************************/
package _util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import ij.plugin.filter.Convolver;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Filters of the feature stack without other libraries (formerly BoofCV and
 * Catalano). All methods are thread safe and return new float images.
 */
public final class Filters {

	private Filters() {
	}

	/**
	 * The Gabor filter response (the real part, convolved as float).
	 *
	 * @param size        the size of the kernel in standard deviations
	 * @param wavelength  the wavelength of the sinusoidal factor
	 * @param orientation the orientation in radians
	 * @param phaseOffset the phase offset in radians
	 * @param sigma       the standard deviation of the Gaussian envelope
	 * @param aspectRatio the spatial aspect ratio of the Gaussian envelope
	 */
	public static FloatProcessor gabor(ImageProcessor ip, int size, double wavelength, double orientation, double phaseOffset, double sigma,
			double aspectRatio) {
		double sigmaX = sigma;
		double sigmaY = sigma / aspectRatio;
		double cos = Math.cos(orientation), sin = Math.sin(orientation);
		int xMax = (int) Math.ceil(Math.max(1, Math.max(Math.abs(size * sigmaX * cos), Math.abs(size * sigmaY * sin))));
		int yMax = (int) Math.ceil(Math.max(1, Math.max(Math.abs(size * sigmaX * sin), Math.abs(size * sigmaY * cos))));
		int kw = 2 * xMax + 1, kh = 2 * yMax + 1;
		float[] kernel = new float[kw * kh];
		double norm = 1.0 / (2 * Math.PI * sigmaX * sigmaY);
		for (int y = -yMax; y <= yMax; y++) {
			for (int x = -xMax; x <= xMax; x++) {
				double xPrime = x * cos + y * sin;
				double yPrime = -x * sin + y * cos;
				double envelope = norm * Math.exp(-0.5 * (xPrime * xPrime / (sigmaX * sigmaX) + yPrime * yPrime / (sigmaY * sigmaY)));
				kernel[(y + yMax) * kw + (x + xMax)] = (float) (envelope * Math.cos(2 * Math.PI * xPrime / wavelength + phaseOffset));
			}
		}
		return convolve(ip, kernel, kw, kh, false);
	}

	/** The Laplacian (4-neighborhood, the edges extended). */
	public static FloatProcessor laplacian(ImageProcessor ip) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] src = toFloat(ip);
		float[] dst = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			int ym = Math.max(0, y - 1), yp = Math.min(h - 1, y + 1);
			for (int x = 0; x < w; x++) {
				int xm = Math.max(0, x - 1), xp = Math.min(w - 1, x + 1);
				dst[y * w + x] = src[ym * w + x] + src[yp * w + x] + src[y * w + xm] + src[y * w + xp] - 4 * src[y * w + x];
			}
		});
		return new FloatProcessor(w, h, dst);
	}

	/**
	 * The Sobel gradient in x (first) and y direction (second), scaled by 1/4
	 * and the edges extended.
	 */
	public static FloatProcessor[] sobel(ImageProcessor ip) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = toFloat(ip);
		float[] gx = new float[w * h], gy = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			int ym = Math.max(0, y - 1) * w, y0 = y * w, yp = Math.min(h - 1, y + 1) * w;
			for (int x = 0; x < w; x++) {
				int xm = Math.max(0, x - 1), xp = Math.min(w - 1, x + 1);
				gx[y0 + x] = 0.25f * ((s[ym + xp] + 2 * s[y0 + xp] + s[yp + xp]) - (s[ym + xm] + 2 * s[y0 + xm] + s[yp + xm]));
				gy[y0 + x] = 0.25f * ((s[yp + xm] + 2 * s[yp + x] + s[yp + xp]) - (s[ym + xm] + 2 * s[ym + x] + s[ym + xp]));
			}
		});
		return new FloatProcessor[] { new FloatProcessor(w, h, gx), new FloatProcessor(w, h, gy) };
	}

	/** The difference a - b. */
	public static FloatProcessor subtract(ImageProcessor a, ImageProcessor b) {
		int w = a.getWidth(), h = a.getHeight();
		float[] pa = toFloat(a), pb = toFloat(b);
		float[] dst = new float[w * h];
		for (int i = 0; i < dst.length; i++) {
			dst[i] = pa[i] - pb[i];
		}
		return new FloatProcessor(w, h, dst);
	}

	/**
	 * Convolves with the kernel of the ImageJ "Convolve..." options, e.g.,
	 * "text1=[\n-1 -1 -1\n-1 8 -1\n-1 -1 -1\n] normalize".
	 */
	public static FloatProcessor convolve(ImageProcessor ip, String options) {
		int start = options.indexOf('['), end = options.lastIndexOf(']');
		String text = start >= 0 && end > start ? options.substring(start + 1, end) : options;
		List<float[]> rows = new ArrayList<float[]>();
		for (String line : text.split("\\r?\\n|\\\\n")) {
			String l = line.trim();
			if (l.isEmpty()) {
				continue;
			}
			String[] values = l.split("[\\s,]+");
			float[] row = new float[values.length];
			for (int i = 0; i < values.length; i++) {
				row[i] = Float.parseFloat(values[i]);
			}
			rows.add(row);
		}
		if (rows.isEmpty()) {
			throw new IllegalArgumentException("No convolution kernel: " + options);
		}
		int kw = rows.get(0).length, kh = rows.size();
		if (kw % 2 == 0 || kh % 2 == 0) {
			throw new IllegalArgumentException("The width and height of the kernel must be odd: " + kw + " x " + kh);
		}
		float[] kernel = new float[kw * kh];
		for (int y = 0; y < kh; y++) {
			if (rows.get(y).length != kw) {
				throw new IllegalArgumentException("All rows of the kernel must have the same length!");
			}
			System.arraycopy(rows.get(y), 0, kernel, y * kw, kw);
		}
		boolean normalize = options.substring(end < 0 ? 0 : end).contains("normalize");
		return convolve(ip, kernel, kw, kh, normalize);
	}

	/* A float convolution with ImageJ (the edges extended) */
	private static FloatProcessor convolve(ImageProcessor ip, float[] kernel, int kw, int kh, boolean normalize) {
		FloatProcessor fp = new FloatProcessor(ip.getWidth(), ip.getHeight(), toFloat(ip).clone());
		Convolver convolver = new Convolver();
		convolver.setNormalize(normalize);
		convolver.convolveFloat(fp, kernel, kw, kh);
		return fp;
	}

	private static float[] toFloat(ImageProcessor ip) {
		if (ip instanceof FloatProcessor) {
			return (float[]) ip.getPixels();
		}
		return (float[]) ip.convertToFloat().getPixels();
	}
}
