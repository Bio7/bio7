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

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Haralick texture features of a Gray-Level Co-occurrence Matrix (GLCM) for
 * each pixel (a sliding window), e.g., as feature layers for a pixel
 * classification.
 * <p>
 * The values of the image are quantized to a number of gray levels in a fixed
 * range (the same for a preview of a selection and for the complete image).
 * For each pixel the co-occurrences of the gray levels with the given distance
 * are counted in the window around the pixel in the four directions (0, 45, 90
 * and 135 degrees) in one symmetric matrix (rotation invariant). The features
 * of the normalized matrix are the values of the pixel (Haralick et al. 1973:
 * Textural Features for Image Classification). The rows are computed in
 * parallel and the window is updated column by column.
 */
public final class Haralick {

	/** The names of the features (the order of the feature indices). */
	public static final String[] FEATURES = { "ASM", "Contrast", "Correlation", "Variance", "IDM", "SumAverage", "SumVariance", "SumEntropy",
			"Entropy", "DiffVariance", "DiffEntropy", "IMC1", "IMC2" };

	public static final int ASM = 0, CONTRAST = 1, CORRELATION = 2, VARIANCE = 3, IDM = 4, SUM_AVERAGE = 5, SUM_VARIANCE = 6, SUM_ENTROPY = 7,
			ENTROPY = 8, DIFF_VARIANCE = 9, DIFF_ENTROPY = 10, IMC1 = 11, IMC2 = 12;

	/* The directions 0, 45, 90 and 135 degrees (image y axis downwards) */
	private static final int[][] DIRECTIONS = { { 1, 0 }, { 1, -1 }, { 0, -1 }, { -1, -1 } };

	private Haralick() {
	}

	/**
	 * The index of a feature name (case insensitive, "Energy" = ASM, "Homogeneity"
	 * = IDM) or -1.
	 */
	public static int featureIndex(String name) {
		String n = name.trim();
		if (n.equalsIgnoreCase("Energy")) {
			return ASM;
		}
		if (n.equalsIgnoreCase("Homogeneity")) {
			return IDM;
		}
		for (int i = 0; i < FEATURES.length; i++) {
			if (FEATURES[i].equalsIgnoreCase(n)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Computes the feature layers.
	 *
	 * @param ip       the image (one channel)
	 * @param min      the minimum of the quantization range
	 * @param max      the maximum of the quantization range
	 * @param window   the width of the square window (odd, >= 3)
	 * @param distance the distance of the pixel pairs (>= 1)
	 * @param levels   the number of gray levels (2 to 256)
	 * @param features the indices of the features (see {@link #FEATURES})
	 * @param canceled true if the computation should stop (may be null)
	 * @return the layers in the order of the features
	 */
	public static FloatProcessor[] compute(ImageProcessor ip, double min, double max, int window, int distance, int levels, int[] features,
			BooleanSupplier canceled) {
		final int width = ip.getWidth();
		final int height = ip.getHeight();
		final int radius = Math.max(1, window / 2);
		final int d = Math.max(1, distance);
		final int ng = Math.max(2, Math.min(256, levels));
		final int[] q = quantize(ip, min, max, ng);
		final float[][] out = new float[features.length][width * height];
		IntStream.range(0, height).parallel().forEach(y -> {
			if (canceled != null && canceled.getAsBoolean()) {
				return;
			}
			Glcm glcm = new Glcm(ng);
			int y0 = Math.max(0, y - radius), y1 = Math.min(height - 1, y + radius);
			/* The window starts empty and grows column by column (each pair counted once) */
			int x0 = 0, x1 = -1;
			double[] values = new double[FEATURES.length];
			for (int x = 0; x < width; x++) {
				int nx0 = Math.max(0, x - radius), nx1 = Math.min(width - 1, x + radius);
				/* Remove the columns left of the new window (the pairs within the old window) */
				while (x0 < nx0) {
					column(glcm, q, width, x0, x0, x1, y0, y1, d, -1);
					x0++;
				}
				/* Add the columns right of the old window (the pairs within the new window) */
				while (x1 < nx1) {
					x1++;
					column(glcm, q, width, x1, x0, x1, y0, y1, d, 1);
				}
				glcm.features(values);
				int index = y * width + x;
				for (int f = 0; f < features.length; f++) {
					out[f][index] = (float) values[features[f]];
				}
			}
		});
		FloatProcessor[] result = new FloatProcessor[features.length];
		for (int f = 0; f < features.length; f++) {
			result[f] = new FloatProcessor(width, height, out[f]);
		}
		return result;
	}

	/* The gray levels 0 ... levels - 1 of the pixels */
	private static int[] quantize(ImageProcessor ip, double min, double max, int levels) {
		int width = ip.getWidth(), height = ip.getHeight();
		int[] q = new int[width * height];
		double range = max - min;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double v = ip.getf(x, y);
				int level = 0;
				if (range > 0 && !Double.isNaN(v)) {
					level = (int) ((v - min) / range * levels);
					level = Math.max(0, Math.min(levels - 1, level));
				}
				q[y * width + x] = level;
			}
		}
		return q;
	}

	/*
	 * Adds (sign 1) or removes (sign -1) all pairs of the window [x0, x1] x [y0,
	 * y1] with a pixel in the column (each pair once).
	 */
	private static void column(Glcm glcm, int[] q, int width, int col, int x0, int x1, int y0, int y1, int d, int sign) {
		int px0 = Math.max(x0, col - d), px1 = Math.min(x1, col + d);
		for (int py = y0; py <= y1; py++) {
			for (int px = px0; px <= px1; px++) {
				int a = q[py * width + px];
				for (int[] dir : DIRECTIONS) {
					int qx = px + dir[0] * d, qy = py + dir[1] * d;
					if (qx < x0 || qx > x1 || qy < y0 || qy > y1 || (px != col && qx != col)) {
						continue;
					}
					glcm.add(a, q[qy * width + qx], sign);
				}
			}
		}
	}

	/* A symmetric co-occurrence matrix with the cells in use */
	private static final class Glcm {
		final int ng;
		final int[] counts;
		/* The cells with counts (may contain cells which are 0 again) */
		int[] active = new int[256];
		int activeSize;
		final boolean[] listed;
		long total;
		final double[] px, pSum, pDiff;

		Glcm(int ng) {
			this.ng = ng;
			counts = new int[ng * ng];
			listed = new boolean[ng * ng];
			px = new double[ng];
			pSum = new double[2 * ng - 1];
			pDiff = new double[ng];
		}

		void add(int a, int b, int sign) {
			cell(a * ng + b, sign);
			cell(b * ng + a, sign);
			total += 2L * sign;
		}

		private void cell(int c, int sign) {
			counts[c] += sign;
			if (counts[c] > 0 && !listed[c]) {
				if (activeSize == active.length) {
					compact();
					if (activeSize == active.length) {
						active = Arrays.copyOf(active, active.length * 2);
					}
				}
				active[activeSize++] = c;
				listed[c] = true;
			}
		}

		/* Removes the cells which are 0 from the active list */
		private void compact() {
			int n = 0;
			for (int i = 0; i < activeSize; i++) {
				int c = active[i];
				if (counts[c] > 0) {
					active[n++] = c;
				} else {
					listed[c] = false;
				}
			}
			activeSize = n;
		}

		void features(double[] f) {
			Arrays.fill(f, 0);
			if (total <= 0) {
				return;
			}
			Arrays.fill(px, 0);
			Arrays.fill(pSum, 0);
			Arrays.fill(pDiff, 0);
			double asm = 0, entropy = 0, idm = 0, sumIJ = 0;
			for (int k = 0; k < activeSize; k++) {
				int c = active[k];
				if (counts[c] == 0) {
					continue;
				}
				int i = c / ng, j = c % ng;
				double p = counts[c] / (double) total;
				asm += p * p;
				entropy -= p * Math.log(p);
				int diff = Math.abs(i - j);
				idm += p / (1.0 + diff * diff);
				sumIJ += i * (double) j * p;
				px[i] += p;
				pSum[i + j] += p;
				pDiff[diff] += p;
			}
			/* The matrix is symmetric: the marginals of the rows and columns are the same */
			double mu = 0, hx = 0;
			for (int i = 0; i < ng; i++) {
				mu += i * px[i];
				if (px[i] > 0) {
					hx -= px[i] * Math.log(px[i]);
				}
			}
			double var = 0;
			for (int i = 0; i < ng; i++) {
				var += (i - mu) * (i - mu) * px[i];
			}
			double hxy1 = 0;
			for (int k = 0; k < activeSize; k++) {
				int c = active[k];
				if (counts[c] == 0) {
					continue;
				}
				double p = counts[c] / (double) total;
				hxy1 -= p * Math.log(px[c / ng] * px[c % ng]);
			}
			double sumAverage = 0, sumEntropy = 0;
			for (int s = 0; s < pSum.length; s++) {
				sumAverage += s * pSum[s];
				if (pSum[s] > 0) {
					sumEntropy -= pSum[s] * Math.log(pSum[s]);
				}
			}
			double sumVariance = 0;
			for (int s = 0; s < pSum.length; s++) {
				sumVariance += (s - sumAverage) * (s - sumAverage) * pSum[s];
			}
			double contrast = 0, diffMean = 0, diffEntropy = 0;
			for (int n = 0; n < ng; n++) {
				contrast += n * (double) n * pDiff[n];
				diffMean += n * pDiff[n];
				if (pDiff[n] > 0) {
					diffEntropy -= pDiff[n] * Math.log(pDiff[n]);
				}
			}
			double diffVariance = 0;
			for (int n = 0; n < ng; n++) {
				diffVariance += (n - diffMean) * (n - diffMean) * pDiff[n];
			}
			f[ASM] = asm;
			f[CONTRAST] = contrast;
			f[CORRELATION] = var > 1e-12 ? (sumIJ - mu * mu) / var : 0;
			f[VARIANCE] = var;
			f[IDM] = idm;
			f[SUM_AVERAGE] = sumAverage;
			f[SUM_VARIANCE] = sumVariance;
			f[SUM_ENTROPY] = sumEntropy;
			f[ENTROPY] = entropy;
			f[DIFF_VARIANCE] = diffVariance;
			f[DIFF_ENTROPY] = diffEntropy;
			/* HX = HY and HXY2 = HX + HY (the product of the marginals) */
			f[IMC1] = hx > 1e-12 ? (entropy - hxy1) / hx : 0;
			f[IMC2] = Math.sqrt(Math.max(0, 1 - Math.exp(-2 * (2 * hx - entropy))));
		}
	}
}
