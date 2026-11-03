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

import java.util.stream.IntStream;

import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Texture measures for the segmentation of images (feature layers of a pixel
 * classification). All methods are thread safe, compute the rows in parallel
 * and return new float images (the edges are extended).
 */
public final class Texture {

	/** The names of the Laws' texture energy maps (rotation invariant pairs). */
	public static final String[] LAWS = { "L5E5", "L5S5", "L5R5", "E5E5", "E5S5", "E5R5", "S5S5", "S5R5", "R5R5" };

	private static final float[][] LAWS_KERNELS = { { 1, 4, 6, 4, 1 }, { -1, -2, 0, 2, 1 }, { -1, 0, 2, 0, -1 }, { 1, -4, 6, -4, 1 } };
	/* The kernel indices (L5, E5, S5, R5) of the maps */
	private static final int[][] LAWS_PAIRS = { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 1, 1 }, { 1, 2 }, { 1, 3 }, { 2, 2 }, { 2, 3 }, { 3, 3 } };

	private Texture() {
	}

	/**
	 * The entropy of the histogram of the gray levels in the square window
	 * around each pixel.
	 *
	 * @param min    the minimum of the quantization range
	 * @param max    the maximum of the quantization range
	 * @param radius the radius of the window (width 2 * radius + 1)
	 * @param bins   the number of gray levels
	 */
	public static FloatProcessor localEntropy(ImageProcessor ip, double min, double max, int radius, int bins) {
		int w = ip.getWidth(), h = ip.getHeight();
		int r = Math.max(1, radius);
		int[] q = quantize(ip, min, max, bins);
		float[] out = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			int[] hist = new int[bins];
			int y0 = Math.max(0, y - r), y1 = Math.min(h - 1, y + r);
			int x0 = 0, x1 = -1, n = 0;
			for (int x = 0; x < w; x++) {
				int nx0 = Math.max(0, x - r), nx1 = Math.min(w - 1, x + r);
				while (x1 < nx1) {
					x1++;
					for (int yy = y0; yy <= y1; yy++) {
						hist[q[yy * w + x1]]++;
					}
					n += y1 - y0 + 1;
				}
				while (x0 < nx0) {
					for (int yy = y0; yy <= y1; yy++) {
						hist[q[yy * w + x0]]--;
					}
					n -= y1 - y0 + 1;
					x0++;
				}
				double e = 0;
				for (int b = 0; b < bins; b++) {
					if (hist[b] > 0) {
						double p = hist[b] / (double) n;
						e -= p * Math.log(p);
					}
				}
				out[y * w + x] = (float) e;
			}
		});
		return new FloatProcessor(w, h, out);
	}

	/**
	 * The rotation invariant uniform Local Binary Pattern (riu2) with 8 neighbors
	 * on a circle (bilinear interpolation): the number of neighbors >= the center
	 * for uniform patterns (0 - 8) and 9 for the other patterns.
	 */
	public static FloatProcessor lbp(ImageProcessor ip, double radius) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = toFloat(ip);
		float[] out = new float[w * h];
		int p = 8;
		double[] dx = new double[p], dy = new double[p];
		for (int i = 0; i < p; i++) {
			dx[i] = radius * Math.cos(2 * Math.PI * i / p);
			dy[i] = -radius * Math.sin(2 * Math.PI * i / p);
		}
		IntStream.range(0, h).parallel().forEach(y -> {
			int[] bits = new int[p];
			for (int x = 0; x < w; x++) {
				float c = s[y * w + x];
				int ones = 0;
				for (int i = 0; i < p; i++) {
					bits[i] = bilinear(s, w, h, x + dx[i], y + dy[i]) >= c ? 1 : 0;
					ones += bits[i];
				}
				int transitions = 0;
				for (int i = 0; i < p; i++) {
					if (bits[i] != bits[(i + 1) % p]) {
						transitions++;
					}
				}
				out[y * w + x] = transitions <= 2 ? ones : p + 1;
			}
		});
		return new FloatProcessor(w, h, out);
	}

	/**
	 * The Laws' texture energy maps (see {@link #LAWS}): the local mean is
	 * removed, the image is convolved with the 5x5 masks and the energy is the
	 * local mean of the absolute responses (pairs averaged, rotation invariant).
	 *
	 * @param radius the radius of the window for the local means
	 */
	public static FloatProcessor[] laws(ImageProcessor ip, int radius) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = toFloat(ip);
		float[] mean = boxMean(s, w, h, radius);
		float[] centered = new float[w * h];
		for (int i = 0; i < centered.length; i++) {
			centered[i] = s[i] - mean[i];
		}
		/* The image convolved with the four vertical kernels (shared by the maps) */
		float[][] vertical = new float[4][];
		IntStream.range(0, 4).parallel().forEach(k -> vertical[k] = convolveColumns(centered, w, h, LAWS_KERNELS[k]));
		FloatProcessor[] maps = new FloatProcessor[LAWS_PAIRS.length];
		IntStream.range(0, LAWS_PAIRS.length).parallel().forEach(m -> {
			int a = LAWS_PAIRS[m][0], b = LAWS_PAIRS[m][1];
			float[] ab = energy(convolveRows(vertical[a], w, h, LAWS_KERNELS[b]), w, h, radius);
			if (a != b) {
				float[] ba = energy(convolveRows(vertical[b], w, h, LAWS_KERNELS[a]), w, h, radius);
				for (int i = 0; i < ab.length; i++) {
					ab[i] = 0.5f * (ab[i] + ba[i]);
				}
			}
			maps[m] = new FloatProcessor(w, h, ab);
		});
		return maps;
	}

	/**
	 * The structure tensor of the gradients (of the Gaussian blurred image with
	 * the gradient sigma) integrated with the integration sigma: the largest and
	 * smallest eigenvalue and the coherence ((l1 - l2) / (l1 + l2))^2.
	 */
	public static FloatProcessor[] structureTensor(ImageProcessor ip, double gradientSigma, double integrationSigma) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = blur(toFloat(ip), w, h, gradientSigma);
		float[] jxx = new float[w * h], jyy = new float[w * h], jxy = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			int ym = Math.max(0, y - 1), yp = Math.min(h - 1, y + 1);
			for (int x = 0; x < w; x++) {
				int xm = Math.max(0, x - 1), xp = Math.min(w - 1, x + 1);
				float gx = 0.5f * (s[y * w + xp] - s[y * w + xm]);
				float gy = 0.5f * (s[yp * w + x] - s[ym * w + x]);
				int i = y * w + x;
				jxx[i] = gx * gx;
				jyy[i] = gy * gy;
				jxy[i] = gx * gy;
			}
		});
		float[] a = blur(jxx, w, h, integrationSigma), b = blur(jyy, w, h, integrationSigma), c = blur(jxy, w, h, integrationSigma);
		float[] l1 = new float[w * h], l2 = new float[w * h], coherence = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				double half = 0.5 * (a[i] + b[i]);
				double root = Math.sqrt(0.25 * (a[i] - b[i]) * (a[i] - b[i]) + c[i] * (double) c[i]);
				double e1 = half + root, e2 = half - root;
				l1[i] = (float) e1;
				l2[i] = (float) e2;
				coherence[i] = e1 + e2 > 1e-12 ? (float) (((e1 - e2) / (e1 + e2)) * ((e1 - e2) / (e1 + e2))) : 0f;
			}
		});
		return new FloatProcessor[] { new FloatProcessor(w, h, l1), new FloatProcessor(w, h, l2), new FloatProcessor(w, h, coherence) };
	}

	/**
	 * The Hessian matrix of the Gaussian blurred image (scale normalized with
	 * sigma^2): the largest and smallest eigenvalue and the determinant.
	 */
	public static FloatProcessor[] hessian(ImageProcessor ip, double sigma) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = blur(toFloat(ip), w, h, sigma);
		double norm = sigma * sigma;
		float[] l1 = new float[w * h], l2 = new float[w * h], det = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			int ym = Math.max(0, y - 1), yp = Math.min(h - 1, y + 1);
			for (int x = 0; x < w; x++) {
				int xm = Math.max(0, x - 1), xp = Math.min(w - 1, x + 1);
				int i = y * w + x;
				double lxx = norm * (s[y * w + xp] - 2 * s[i] + s[y * w + xm]);
				double lyy = norm * (s[yp * w + x] - 2 * s[i] + s[ym * w + x]);
				double lxy = norm * 0.25 * (s[yp * w + xp] - s[yp * w + xm] - s[ym * w + xp] + s[ym * w + xm]);
				double half = 0.5 * (lxx + lyy);
				double root = Math.sqrt(0.25 * (lxx - lyy) * (lxx - lyy) + lxy * lxy);
				l1[i] = (float) (half + root);
				l2[i] = (float) (half - root);
				det[i] = (float) (lxx * lyy - lxy * lxy);
			}
		});
		return new FloatProcessor[] { new FloatProcessor(w, h, l1), new FloatProcessor(w, h, l2), new FloatProcessor(w, h, det) };
	}

	/* The gray levels 0 ... levels - 1 of the pixels */
	static int[] quantize(ImageProcessor ip, double min, double max, int levels) {
		int w = ip.getWidth(), h = ip.getHeight();
		int[] q = new int[w * h];
		double range = max - min;
		for (int i = 0; i < q.length; i++) {
			double v = ip.getf(i);
			int level = 0;
			if (range > 0 && !Double.isNaN(v)) {
				level = Math.max(0, Math.min(levels - 1, (int) ((v - min) / range * levels)));
			}
			q[i] = level;
		}
		return q;
	}

	private static float bilinear(float[] s, int w, int h, double x, double y) {
		x = Math.max(0, Math.min(w - 1, x));
		y = Math.max(0, Math.min(h - 1, y));
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
		int x1 = Math.min(w - 1, x0 + 1), y1 = Math.min(h - 1, y0 + 1);
		double fx = x - x0, fy = y - y0;
		double top = s[y0 * w + x0] * (1 - fx) + s[y0 * w + x1] * fx;
		double bottom = s[y1 * w + x0] * (1 - fx) + s[y1 * w + x1] * fx;
		return (float) (top * (1 - fy) + bottom * fy);
	}

	private static float[] energy(float[] response, int w, int h, int radius) {
		float[] abs = new float[response.length];
		for (int i = 0; i < abs.length; i++) {
			abs[i] = Math.abs(response[i]);
		}
		return boxMean(abs, w, h, radius);
	}

	/* The mean in the square window (separable running sums, the edges extended) */
	private static float[] boxMean(float[] s, int w, int h, int radius) {
		int r = Math.max(1, radius);
		float[] tmp = new float[w * h], out = new float[w * h];
		float n = 2 * r + 1;
		IntStream.range(0, h).parallel().forEach(y -> {
			double sum = 0;
			for (int k = -r; k <= r; k++) {
				sum += s[y * w + Math.max(0, Math.min(w - 1, k))];
			}
			for (int x = 0; x < w; x++) {
				tmp[y * w + x] = (float) (sum / n);
				sum += s[y * w + Math.min(w - 1, x + r + 1)] - s[y * w + Math.max(0, x - r)];
			}
		});
		IntStream.range(0, w).parallel().forEach(x -> {
			double sum = 0;
			for (int k = -r; k <= r; k++) {
				sum += tmp[Math.max(0, Math.min(h - 1, k)) * w + x];
			}
			for (int y = 0; y < h; y++) {
				out[y * w + x] = (float) (sum / n);
				sum += tmp[Math.min(h - 1, y + r + 1) * w + x] - tmp[Math.max(0, y - r) * w + x];
			}
		});
		return out;
	}

	private static float[] convolveRows(float[] s, int w, int h, float[] k) {
		int r = k.length / 2;
		float[] out = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				float sum = 0;
				for (int i = -r; i <= r; i++) {
					sum += k[i + r] * s[y * w + Math.max(0, Math.min(w - 1, x + i))];
				}
				out[y * w + x] = sum;
			}
		});
		return out;
	}

	private static float[] convolveColumns(float[] s, int w, int h, float[] k) {
		int r = k.length / 2;
		float[] out = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				float sum = 0;
				for (int i = -r; i <= r; i++) {
					sum += k[i + r] * s[Math.max(0, Math.min(h - 1, y + i)) * w + x];
				}
				out[y * w + x] = sum;
			}
		});
		return out;
	}

	private static float[] blur(float[] s, int w, int h, double sigma) {
		FloatProcessor fp = new FloatProcessor(w, h, s.clone());
		if (sigma > 0) {
			new GaussianBlur().blurGaussian(fp, sigma, sigma, 0.0002);
		}
		return (float[]) fp.getPixels();
	}

	private static float[] toFloat(ImageProcessor ip) {
		if (ip instanceof FloatProcessor) {
			return (float[]) ip.getPixels();
		}
		return (float[]) ip.convertToFloat().getPixels();
	}
}
