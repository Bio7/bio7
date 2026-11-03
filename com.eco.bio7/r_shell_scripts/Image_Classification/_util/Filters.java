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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
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

	/*
	 * A float convolution in parallel (rows) with the edges extended like the
	 * ImageJ Convolver (normalize: divided by the sum of the kernel if not 0).
	 */
	static FloatProcessor convolve(ImageProcessor ip, float[] kernel, int kw, int kh, boolean normalize) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = toFloat(ip);
		double sum = 0;
		for (float k : kernel) {
			sum += k;
		}
		float scale = normalize && sum != 0 ? (float) (1 / sum) : 1f;
		int rx = kw / 2, ry = kh / 2;
		/* The taps which are not 0 */
		int n = 0;
		for (float k : kernel) {
			if (k != 0) {
				n++;
			}
		}
		int[] tdx = new int[n], tdy = new int[n];
		float[] tk = new float[n];
		n = 0;
		for (int y = 0; y < kh; y++) {
			for (int x = 0; x < kw; x++) {
				float k = kernel[y * kw + x];
				if (k != 0) {
					tdx[n] = x - rx;
					tdy[n] = y - ry;
					tk[n++] = k * scale;
				}
			}
		}
		float[] out = new float[w * h];
		int taps = n;
		IntStream.range(0, h).parallel().forEach(y -> {
			boolean inner = y >= ry && y < h - ry;
			for (int x = 0; x < w; x++) {
				float acc = 0;
				if (inner && x >= rx && x < w - rx) {
					for (int t = 0; t < taps; t++) {
						acc += tk[t] * s[(y + tdy[t]) * w + x + tdx[t]];
					}
				} else {
					for (int t = 0; t < taps; t++) {
						int xx = Math.max(0, Math.min(w - 1, x + tdx[t])), yy = Math.max(0, Math.min(h - 1, y + tdy[t]));
						acc += tk[t] * s[yy * w + xx];
					}
				}
				out[y * w + x] = acc;
			}
		});
		return new FloatProcessor(w, h, out);
	}

	/**
	 * The Kuwahara filter for float images: the mean of the quadrant (of the
	 * window with the width size) with the smallest variance (integral images,
	 * the edges extended).
	 */
	public static FloatProcessor kuwahara(ImageProcessor ip, int size) {
		int w = ip.getWidth(), h = ip.getHeight();
		int r = Math.max(1, (size - 1) / 2);
		int q = r + 1;
		float[] s = toFloat(ip);
		/* The integral images of the padded image (sum and sum of squares) */
		int pw = w + 2 * r, ph = h + 2 * r;
		double[] sum = new double[(pw + 1) * (ph + 1)], sq = new double[(pw + 1) * (ph + 1)];
		for (int y = 0; y < ph; y++) {
			int sy = Math.max(0, Math.min(h - 1, y - r));
			double rowSum = 0, rowSq = 0;
			for (int x = 0; x < pw; x++) {
				double v = s[sy * w + Math.max(0, Math.min(w - 1, x - r))];
				rowSum += v;
				rowSq += v * v;
				int i = (y + 1) * (pw + 1) + x + 1;
				sum[i] = sum[i - (pw + 1)] + rowSum;
				sq[i] = sq[i - (pw + 1)] + rowSq;
			}
		}
		float[] out = new float[w * h];
		double n = q * q;
		/* The quadrants: top left, top right, bottom right, bottom left (the order of the ImageJ plugin) */
		int[][] corners = { { -r, -r }, { 0, -r }, { 0, 0 }, { -r, 0 } };
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				double best = Double.MAX_VALUE, mean = 0;
				for (int[] c : corners) {
					int x0 = x + c[0] + r, y0 = y + c[1] + r;
					int a = y0 * (pw + 1) + x0, b2 = y0 * (pw + 1) + x0 + q, c2 = (y0 + q) * (pw + 1) + x0, d = (y0 + q) * (pw + 1) + x0 + q;
					double sm = sum[d] - sum[b2] - sum[c2] + sum[a];
					double var = (sq[d] - sq[b2] - sq[c2] + sq[a]) - sm * sm / n;
					if (var < best) {
						best = var;
						mean = sm / n;
					}
				}
				out[y * w + x] = (float) mean;
			}
		});
		return new FloatProcessor(w, h, out);
	}

	/**
	 * An 8-bit image of the values in a fixed range (the same for a preview of a
	 * selection and the complete image).
	 */
	public static ByteProcessor toByte(ImageProcessor ip, double min, double max) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = toFloat(ip);
		byte[] out = new byte[w * h];
		double scale = max > min ? 256.0 / (max - min) : 0;
		for (int i = 0; i < out.length; i++) {
			int v = (int) ((s[i] - min) * scale);
			out[i] = (byte) Math.max(0, Math.min(255, v));
		}
		return new ByteProcessor(w, h, out);
	}

	/** The gradient magnitude of the Gaussian blurred image (central differences). */
	public static FloatProcessor gradientMagnitude(ImageProcessor ip, double sigma) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = blur(toFloat(ip), w, h, sigma);
		float[] out = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			int ym = Math.max(0, y - 1), yp = Math.min(h - 1, y + 1);
			for (int x = 0; x < w; x++) {
				int xm = Math.max(0, x - 1), xp = Math.min(w - 1, x + 1);
				float gx = 0.5f * (s[y * w + xp] - s[y * w + xm]);
				float gy = 0.5f * (s[yp * w + x] - s[ym * w + x]);
				out[y * w + x] = (float) Math.sqrt(gx * gx + gy * gy);
			}
		});
		return new FloatProcessor(w, h, out);
	}

	/** The names of the membrane projections (the order of the layers). */
	public static final String[] MEMBRANE_PROJECTIONS = { "Mean", "Max", "Min", "Sum", "Std", "Median" };

	/**
	 * Membrane projections (Weka Trainable Segmentation): the image is
	 * convolved with a line (the patch size long and the thickness wide)
	 * rotated in 30 orientations (6 degrees) and the responses are projected
	 * (see {@link #MEMBRANE_PROJECTIONS}).
	 */
	public static FloatProcessor[] membraneProjections(ImageProcessor ip, int patchSize, int thickness) {
		int w = ip.getWidth(), h = ip.getHeight();
		int orientations = 30;
		float[][] responses = new float[orientations][];
		IntStream.range(0, orientations).parallel().forEach(o -> {
			double angle = Math.PI * o / orientations;
			/* The sparse line kernel (bilinear rasterized) */
			Map<Long, Float> taps = new LinkedHashMap<Long, Float>();
			double cos = Math.cos(angle), sin = Math.sin(angle);
			int half = patchSize / 2;
			double across = (thickness - 1) / 2.0;
			for (int t = -half; t <= half; t++) {
				for (double u = -across; u <= across + 1e-9; u += 1) {
					double px = t * sin + u * cos, py = t * cos - u * sin;
					int x0 = (int) Math.floor(px), y0 = (int) Math.floor(py);
					double fx = px - x0, fy = py - y0;
					addTap(taps, x0, y0, (1 - fx) * (1 - fy));
					addTap(taps, x0 + 1, y0, fx * (1 - fy));
					addTap(taps, x0, y0 + 1, (1 - fx) * fy);
					addTap(taps, x0 + 1, y0 + 1, fx * fy);
				}
			}
			int r = half + (int) Math.ceil(across) + 1, kw = 2 * r + 1;
			float[] kernel = new float[kw * kw];
			for (Map.Entry<Long, Float> e : taps.entrySet()) {
				int dx = (int) (e.getKey() >> 32), dy = (int) (long) e.getKey().intValue();
				kernel[(dy + r) * kw + dx + r] += e.getValue();
			}
			responses[o] = (float[]) convolve(ip, kernel, kw, kw, false).getPixels();
		});
		float[][] out = new float[MEMBRANE_PROJECTIONS.length][w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			float[] v = new float[orientations];
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				double sum = 0, sq = 0;
				float max = -Float.MAX_VALUE, min = Float.MAX_VALUE;
				for (int o = 0; o < orientations; o++) {
					float r = responses[o][i];
					v[o] = r;
					sum += r;
					sq += r * (double) r;
					max = Math.max(max, r);
					min = Math.min(min, r);
				}
				double mean = sum / orientations;
				Arrays.sort(v);
				out[0][i] = (float) mean;
				out[1][i] = max;
				out[2][i] = min;
				out[3][i] = (float) sum;
				out[4][i] = (float) Math.sqrt(Math.max(0, sq / orientations - mean * mean));
				out[5][i] = 0.5f * (v[orientations / 2 - 1] + v[orientations / 2]);
			}
		});
		FloatProcessor[] result = new FloatProcessor[out.length];
		for (int k = 0; k < out.length; k++) {
			result[k] = new FloatProcessor(w, h, out[k]);
		}
		return result;
	}

	private static void addTap(Map<Long, Float> taps, int x, int y, double weight) {
		if (weight > 1e-9) {
			taps.merge(((long) x << 32) | (y & 0xffffffffL), (float) weight, Float::sum);
		}
	}

	/** The directions of the neighbors (the order of the layers). */
	public static final int[][] NEIGHBORS = { { 1, 0 }, { 1, -1 }, { 0, -1 }, { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } };

	/**
	 * The values of the neighbors in the 8 directions at the distance (Weka
	 * Trainable Segmentation, the edges extended): the context of a pixel.
	 */
	public static FloatProcessor[] neighbors(ImageProcessor ip, int distance) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = toFloat(ip);
		FloatProcessor[] result = new FloatProcessor[NEIGHBORS.length];
		IntStream.range(0, NEIGHBORS.length).parallel().forEach(k -> {
			int dx = NEIGHBORS[k][0] * distance, dy = NEIGHBORS[k][1] * distance;
			float[] out = new float[w * h];
			for (int y = 0; y < h; y++) {
				int yy = Math.max(0, Math.min(h - 1, y + dy));
				for (int x = 0; x < w; x++) {
					out[y * w + x] = s[yy * w + Math.max(0, Math.min(w - 1, x + dx))];
				}
			}
			result[k] = new FloatProcessor(w, h, out);
		});
		return result;
	}

	/**
	 * The bilateral filter (edge preserving smoothing).
	 *
	 * @param spatialSigma the sigma of the distances (pixels)
	 * @param rangeSigma   the sigma of the value differences (absolute)
	 */
	public static FloatProcessor bilateral(ImageProcessor ip, double spatialSigma, double rangeSigma) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] s = toFloat(ip);
		int r = Math.max(1, (int) Math.ceil(2.5 * spatialSigma));
		int kw = 2 * r + 1;
		float[] spatial = new float[kw * kw];
		for (int y = -r; y <= r; y++) {
			for (int x = -r; x <= r; x++) {
				spatial[(y + r) * kw + x + r] = (float) Math.exp(-(x * x + y * y) / (2 * spatialSigma * spatialSigma));
			}
		}
		double rangeFactor = rangeSigma > 0 ? -1.0 / (2 * rangeSigma * rangeSigma) : 0;
		float[] out = new float[w * h];
		IntStream.range(0, h).parallel().forEach(y -> {
			for (int x = 0; x < w; x++) {
				float c = s[y * w + x];
				double sum = 0, norm = 0;
				for (int dy = -r; dy <= r; dy++) {
					int yy = Math.max(0, Math.min(h - 1, y + dy)) * w;
					for (int dx = -r; dx <= r; dx++) {
						float v = s[yy + Math.max(0, Math.min(w - 1, x + dx))];
						double d = v - c;
						double weight = spatial[(dy + r) * kw + dx + r] * Math.exp(d * d * rangeFactor);
						sum += weight * v;
						norm += weight;
					}
				}
				out[y * w + x] = (float) (sum / norm);
			}
		});
		return new FloatProcessor(w, h, out);
	}

	/**
	 * The anisotropic diffusion of Perona and Malik (edge preserving smoothing,
	 * 4-neighborhood, the conduction exp(-(gradient / kappa)^2)).
	 *
	 * @param iterations the number of iterations
	 * @param kappa      the gradient threshold of the edges (absolute)
	 * @param lambda     the time step (0 - 0.25)
	 */
	public static FloatProcessor anisotropicDiffusion(ImageProcessor ip, int iterations, double kappa, double lambda) {
		int w = ip.getWidth(), h = ip.getHeight();
		float[] a = toFloat(ip).clone(), b = new float[w * h];
		double k2 = kappa > 0 ? kappa * kappa : 1e-12;
		double step = Math.max(0, Math.min(0.25, lambda));
		for (int it = 0; it < iterations; it++) {
			float[] src = a, dst = b;
			IntStream.range(0, h).parallel().forEach(y -> {
				int ym = Math.max(0, y - 1), yp = Math.min(h - 1, y + 1);
				for (int x = 0; x < w; x++) {
					int xm = Math.max(0, x - 1), xp = Math.min(w - 1, x + 1);
					float c = src[y * w + x];
					double dn = src[ym * w + x] - c, ds = src[yp * w + x] - c, de = src[y * w + xp] - c, dw = src[y * w + xm] - c;
					double flux = Math.exp(-dn * dn / k2) * dn + Math.exp(-ds * ds / k2) * ds + Math.exp(-de * de / k2) * de + Math.exp(-dw * dw / k2) * dw;
					dst[y * w + x] = (float) (c + step * flux);
				}
			});
			a = dst;
			b = src;
		}
		return new FloatProcessor(w, h, a);
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
