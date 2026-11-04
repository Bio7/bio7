/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.spatial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The charts of the plot API (see the factory methods of Plot3D): line graphs,
 * box plots, violin plots, histograms, area charts (with axes) and radar
 * charts, rose charts and treemaps (without axes, like the pie chart).
 */
public final class PlotCharts {

	private PlotCharts() {
	}

	/* ----------------------------- Helpers ----------------------------- */

	static float[] rgba(int rgb, float alpha) {
		return PlotGeometry.rgba(rgb, alpha);
	}

	static int seriesColor(int[] colors, int k) {
		return colors != null && k < colors.length ? colors[k] : Plot3D.CATEGORY_COLORS[k % Plot3D.CATEGORY_COLORS.length];
	}

	/* The finite values sorted */
	static double[] sorted(double[] v) {
		return Arrays.stream(v).filter(Double::isFinite).sorted().toArray();
	}

	/* A quantile (linear interpolation) of sorted values */
	static double quantile(double[] s, double p) {
		if (s.length == 0) {
			return Double.NaN;
		}
		double h = (s.length - 1) * p;
		int i = (int) Math.floor(h);
		return i + 1 < s.length ? s[i] + (h - i) * (s[i + 1] - s[i]) : s[i];
	}

	static String[] defaultNames(String[] names, int n, String prefix) {
		String[] result = new String[n];
		for (int k = 0; k < n; k++) {
			result[k] = names != null && k < names.length && names[k] != null ? names[k] : prefix + " " + (k + 1);
		}
		return result;
	}

	/* The z coordinate of the box of a value */
	static double z(Plot3D plot, double v) {
		return plot.box(2, plot.value(2, v));
	}

	/* The size of one unit of an axis in the box (e.g., the distance of two categories) */
	static double unit(Plot3D plot, int axis) {
		return plot.box(axis, 1) - plot.box(axis, 0);
	}

	/* ----------------------------- Line graph ----------------------------- */

	/**
	 * A line graph: several series of values over x, each series a line (with
	 * markers) at its own position in the depth (the y axis shows the names of
	 * the series).
	 */
	public static final class LineGraph extends Plot3D.Layer<LineGraph> {
		final double[] x;
		final double[][] series;
		String[] names;
		int[] colors;
		boolean markers = true, fill;
		double markerSize = 0.08;

		LineGraph(double[] x, double[][] series) {
			if (x == null || series == null || series.length == 0) {
				throw new IllegalArgumentException("The line graph needs x and at least one series");
			}
			for (double[] s : series) {
				if (s.length != x.length) {
					throw new IllegalArgumentException("Each series needs a value for each x");
				}
			}
			this.x = x.clone();
			this.series = series.clone();
		}

		/** The names of the series (the legend and the y axis). */
		public LineGraph names(String... names) {
			this.names = names.clone();
			return this;
		}

		/** The colors of the series (0xRRGGBB). */
		public LineGraph colors(int... colors) {
			this.colors = colors.clone();
			return this;
		}

		/** Markers (spheres) at the values (default true) and their radius (the box has the size 10). */
		public LineGraph markers(boolean markers, double radius) {
			this.markers = markers;
			this.markerSize = radius;
			return this;
		}

		/** Fills the area below the lines (default false). */
		public LineGraph fill(boolean fill) {
			this.fill = fill;
			return this;
		}

		@Override
		String[] categories(int axis) {
			return axis == 1 ? defaultNames(names, series.length, "Series") : null;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (double v : x) {
				add(plot, extent, 0, v);
			}
			for (double[] s : series) {
				for (double v : s) {
					add(plot, extent, 2, v);
				}
			}
			if (fill && !plot.logAxes()[2]) {
				add(plot, extent, 2, 0);
			}
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
			String[] n = defaultNames(names, series.length, "Series");
			for (int k = 0; k < series.length; k++) {
				labels.add(n[k]);
				colors.add(seriesColor(this.colors, k));
			}
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			for (int k = 0; k < series.length; k++) {
				float[] c = rgba(seriesColor(colors, k), alpha);
				double[] last = null;
				for (int i = 0; i < x.length; i++) {
					double[] p = plot.position(x[i], k, series[k][i]);
					if (p == null) {
						last = null;
						continue;
					}
					if (last != null) {
						g.line(last, p, c);
						if (fill) {
							double[] a0 = { last[0], last[1], 0 }, b0 = { p[0], p[1], 0 };
							float[] fc = rgba(seriesColor(colors, k), Math.min(alpha, 0.55f));
							g.quad(a0, b0, p, last, fc);
						}
					}
					if (markers) {
						g.point(p, c, markerSize);
					}
					last = p;
				}
			}
		}
	}

	/* ----------------------------- Box plot ----------------------------- */

	/**
	 * A box plot of groups (the x axis shows the names of the groups): the box
	 * from the first to the third quartile, the median, the whiskers (1.5 times
	 * the interquartile range) and the outliers.
	 */
	public static final class BoxPlot extends Plot3D.Layer<BoxPlot> {
		final double[][] samples;
		final String[] labels;
		int[] colors;
		double width = 0.6;
		boolean outliers = true;

		BoxPlot(double[][] samples, String[] labels) {
			if (samples == null || samples.length == 0) {
				throw new IllegalArgumentException("The box plot needs at least one group");
			}
			this.samples = samples.clone();
			this.labels = labels != null ? labels.clone() : null;
		}

		/** The colors of the groups (0xRRGGBB). */
		public BoxPlot colors(int... colors) {
			this.colors = colors.clone();
			return this;
		}

		/** The width of the boxes (a fraction of the distance of the groups, default 0.6). */
		public BoxPlot width(double width) {
			this.width = Math.max(0.05, Math.min(1, width));
			return this;
		}

		/** Displays the outliers (default true). */
		public BoxPlot outliers(boolean outliers) {
			this.outliers = outliers;
			return this;
		}

		@Override
		String[] categories(int axis) {
			return axis == 0 ? defaultNames(labels, samples.length, "Group") : axis == 1 ? new String[] { "" } : null;
		}

		@Override
		double[] preferredAspect() {
			return new double[] { 1, 0.3, 0.75 };
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (double[] s : samples) {
				for (double v : s) {
					add(plot, extent, 2, v);
				}
			}
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double sx = unit(plot, 0);
			double cy = plot.box(1, 0), halfDepth = plot.boxSize()[1] * 0.42;
			float[] dark = rgba(0x222222, 1);
			double t = plot.boxSize()[2] * 0.004;
			for (int i = 0; i < samples.length; i++) {
				double[] s = sorted(samples[i]);
				if (s.length == 0) {
					continue;
				}
				float[] c = rgba(seriesColor(colors, i), alpha);
				double q1 = quantile(s, 0.25), q2 = quantile(s, 0.5), q3 = quantile(s, 0.75), iqr = q3 - q1;
				double low = s[0], high = s[s.length - 1];
				for (double v : s) {
					if (v >= q1 - 1.5 * iqr) {
						low = v;
						break;
					}
				}
				for (int k = s.length - 1; k >= 0; k--) {
					if (s[k] <= q3 + 1.5 * iqr) {
						high = s[k];
						break;
					}
				}
				double cx = plot.box(0, i), hw = sx * width / 2, hd = Math.min(hw, halfDepth);
				Plot3D.Bars.box(g, cx - hw, cx + hw, cy - hd, cy + hd, z(plot, q1), Math.max(z(plot, q1) + t, z(plot, q3)), c);
				/* The median (a thin dark slab) */
				double zm = z(plot, q2);
				Plot3D.Bars.box(g, cx - hw * 1.03, cx + hw * 1.03, cy - hd * 1.03, cy + hd * 1.03, zm - t, zm + t, dark);
				/* The whiskers and their caps */
				g.line(new double[] { cx, cy, z(plot, q3) }, new double[] { cx, cy, z(plot, high) }, dark);
				g.line(new double[] { cx, cy, z(plot, q1) }, new double[] { cx, cy, z(plot, low) }, dark);
				for (double v : new double[] { low, high }) {
					double zz = z(plot, v);
					g.line(new double[] { cx - hw / 2, cy, zz }, new double[] { cx + hw / 2, cy, zz }, dark);
					g.line(new double[] { cx, cy - hd / 2, zz }, new double[] { cx, cy + hd / 2, zz }, dark);
				}
				if (outliers) {
					for (double v : s) {
						if (v < low || v > high) {
							g.point(new double[] { cx, cy, z(plot, v) }, c, Math.min(0.1, hw * 0.15));
						}
					}
				}
			}
		}
	}

	/* ----------------------------- Violin plot ----------------------------- */

	/**
	 * A violin plot of groups: the density of the values (a Gaussian kernel
	 * density) as a solid of revolution around the vertical axis of each group
	 * with the box of the quartiles and the median inside.
	 */
	public static final class Violin extends Plot3D.Layer<Violin> {
		final double[][] samples;
		final String[] labels;
		int[] colors;
		double width = 0.8, bandwidth = 0;
		boolean quartiles = true;

		Violin(double[][] samples, String[] labels) {
			if (samples == null || samples.length == 0) {
				throw new IllegalArgumentException("The violin plot needs at least one group");
			}
			this.samples = samples.clone();
			this.labels = labels != null ? labels.clone() : null;
			alpha = 0.9f;
		}

		/** The colors of the groups (0xRRGGBB). */
		public Violin colors(int... colors) {
			this.colors = colors.clone();
			return this;
		}

		/** The maximum width of the violins (a fraction of the distance of the groups, default 0.8). */
		public Violin width(double width) {
			this.width = Math.max(0.05, Math.min(1, width));
			return this;
		}

		/** The bandwidth of the kernel (0 = the rule of Silverman). */
		public Violin bandwidth(double bandwidth) {
			this.bandwidth = Math.max(0, bandwidth);
			return this;
		}

		/** Displays the quartiles and the median inside (default true). */
		public Violin quartiles(boolean quartiles) {
			this.quartiles = quartiles;
			return this;
		}

		@Override
		String[] categories(int axis) {
			return axis == 0 ? defaultNames(labels, samples.length, "Group") : axis == 1 ? new String[] { "" } : null;
		}

		@Override
		double[] preferredAspect() {
			return new double[] { 1, 0.3, 0.75 };
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (double[] s : samples) {
				for (double v : s) {
					add(plot, extent, 2, v);
				}
			}
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double sx = unit(plot, 0), cy = plot.box(1, 0), halfDepth = plot.boxSize()[1] * 0.45;
			int levels = 64, segments = 28;
			for (int i = 0; i < samples.length; i++) {
				double[] s = sorted(samples[i]);
				if (s.length < 2) {
					continue;
				}
				float[] c = rgba(seriesColor(colors, i), alpha);
				int n = s.length;
				double mean = Arrays.stream(s).average().orElse(0);
				double sd = Math.sqrt(Arrays.stream(s).map(v -> (v - mean) * (v - mean)).sum() / (n - 1));
				double iqr = quantile(s, 0.75) - quantile(s, 0.25);
				double spread = Math.min(sd, iqr / 1.34);
				double h = bandwidth > 0 ? bandwidth : 0.9 * (spread > 0 ? spread : sd > 0 ? sd : 1) * Math.pow(n, -0.2);
				/* The density at the levels (from the minimum to the maximum) */
				double lo = s[0], hi = s[n - 1];
				double[] zv = new double[levels], d = new double[levels];
				double dMax = 0;
				for (int j = 0; j < levels; j++) {
					zv[j] = lo + (hi - lo) * j / (levels - 1);
					double sum = 0;
					for (double v : s) {
						double u = (zv[j] - v) / h;
						sum += Math.exp(-0.5 * u * u);
					}
					d[j] = sum;
					dMax = Math.max(dMax, sum);
				}
				double cx = plot.box(0, i), maxR = Math.min(sx * width / 2, halfDepth);
				double[][][] ring = new double[levels][segments + 1][];
				double[] r = new double[levels], zb = new double[levels];
				for (int j = 0; j < levels; j++) {
					r[j] = dMax > 0 ? maxR * d[j] / dMax : 0;
					zb[j] = z(plot, zv[j]);
				}
				for (int j = 0; j < levels; j++) {
					for (int a = 0; a <= segments; a++) {
						double angle = 2 * Math.PI * a / segments;
						ring[j][a] = new double[] { cx + r[j] * Math.cos(angle), cy + r[j] * Math.sin(angle), zb[j] };
					}
				}
				for (int j = 0; j < levels - 1; j++) {
					/* The normal of the surface of revolution (radial and the slope of the radius) */
					double slope = (r[j + 1] - r[j]) / Math.max(1e-9, zb[j + 1] - zb[j]);
					for (int a = 0; a < segments; a++) {
						double a0 = 2 * Math.PI * a / segments, a1 = 2 * Math.PI * (a + 1) / segments;
						double[] n0 = { Math.cos(a0), Math.sin(a0), -slope }, n1 = { Math.cos(a1), Math.sin(a1), -slope };
						g.triangle(ring[j][a], n0, c, ring[j][a + 1], n1, c, ring[j + 1][a + 1], n1, c);
						g.triangle(ring[j][a], n0, c, ring[j + 1][a + 1], n1, c, ring[j + 1][a], n0, c);
					}
				}
				if (quartiles) {
					double q1 = quantile(s, 0.25), q2 = quantile(s, 0.5), q3 = quantile(s, 0.75), w = maxR * 0.12;
					Plot3D.Bars.box(g, cx - w, cx + w, cy - w, cy + w, z(plot, q1), z(plot, q3), rgba(0x222222, 1));
					g.point(new double[] { cx, cy, z(plot, q2) }, rgba(0xFFFFFF, 1), w * 1.4);
				}
			}
		}
	}

	/* ----------------------------- Histogram ----------------------------- */

	/**
	 * A histogram of the values of one or more groups (bars of the counts of the
	 * bins, the groups in the depth with their names on the y axis).
	 */
	public static final class Histogram extends Plot3D.Layer<Histogram> {
		final double[][] groups;
		String[] names;
		int[] colors;
		int bins;
		double binWidth;
		boolean density;
		double width = 0.92;

		Histogram(double[][] groups, String[] names) {
			if (groups == null || groups.length == 0) {
				throw new IllegalArgumentException("The histogram needs values");
			}
			this.groups = groups.clone();
			this.names = names != null ? names.clone() : null;
		}

		/** The number of the bins (default: the rule of Sturges). */
		public Histogram bins(int bins) {
			this.bins = Math.max(1, bins);
			return this;
		}

		/** The width of the bins (instead of the number). */
		public Histogram binWidth(double width) {
			this.binWidth = width > 0 ? width : 0;
			return this;
		}

		/** The density (the area of the bars is 1) instead of the counts. */
		public Histogram density(boolean density) {
			this.density = density;
			return this;
		}

		/** The colors of the groups (0xRRGGBB). */
		public Histogram colors(int... colors) {
			this.colors = colors.clone();
			return this;
		}

		/* The edges of the bins and the counts (or densities) of the groups */
		private double[] edges;
		private double[][] heights;

		private void compute() {
			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			int largest = 1;
			for (double[] gr : groups) {
				int n = 0;
				for (double v : gr) {
					if (Double.isFinite(v)) {
						min = Math.min(min, v);
						max = Math.max(max, v);
						n++;
					}
				}
				largest = Math.max(largest, n);
			}
			if (!(max >= min)) {
				min = 0;
				max = 1;
			}
			if (max == min) {
				max = min + 1;
			}
			int nb = binWidth > 0 ? (int) Math.ceil((max - min) / binWidth) : bins > 0 ? bins : (int) Math.ceil(Math.log(largest) / Math.log(2) + 1);
			nb = Math.max(1, Math.min(1000, nb));
			double w = binWidth > 0 ? binWidth : (max - min) / nb;
			edges = new double[nb + 1];
			for (int b = 0; b <= nb; b++) {
				edges[b] = min + b * w;
			}
			heights = new double[groups.length][nb];
			for (int k = 0; k < groups.length; k++) {
				int n = 0;
				for (double v : groups[k]) {
					if (Double.isFinite(v)) {
						int b = (int) Math.min(nb - 1, Math.floor((v - min) / w));
						heights[k][b]++;
						n++;
					}
				}
				if (density && n > 0) {
					for (int b = 0; b < nb; b++) {
						heights[k][b] /= n * w;
					}
				}
			}
		}

		@Override
		String[] categories(int axis) {
			if (axis != 1) {
				return null;
			}
			return groups.length == 1 && names == null ? new String[] { "" } : defaultNames(names, groups.length, "Group");
		}

		@Override
		double[] preferredAspect() {
			return groups.length == 1 ? new double[] { 1, 0.3, 0.75 } : null;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			compute();
			add(plot, extent, 0, edges[0]);
			add(plot, extent, 0, edges[edges.length - 1]);
			add(plot, extent, 2, 0);
			for (double[] h : heights) {
				for (double v : h) {
					add(plot, extent, 2, v);
				}
			}
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
			if (groups.length > 1 || names != null) {
				String[] n = defaultNames(names, groups.length, "Group");
				for (int k = 0; k < groups.length; k++) {
					labels.add(n[k]);
					colors.add(seriesColor(this.colors, k));
				}
			} else {
				super.legend(plot, index, labels, colors);
			}
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double depth = Math.min(unit(plot, 1) * 0.35, plot.boxSize()[1] * 0.4);
			double z0 = z(plot, plot.logAxes()[2] ? Math.pow(10, plot.ranges()[2][0]) : 0);
			for (int k = 0; k < groups.length; k++) {
				float[] c = rgba(groups.length == 1 && this.color != null ? this.color : seriesColor(colors, k), alpha);
				double cy = plot.box(1, k);
				for (int b = 0; b < heights[k].length; b++) {
					if (heights[k][b] <= 0) {
						continue;
					}
					double gap = (edges[b + 1] - edges[b]) * (1 - width) / 2;
					double x0 = plot.box(0, plot.value(0, edges[b] + gap)), x1 = plot.box(0, plot.value(0, edges[b + 1] - gap));
					double z1 = z(plot, heights[k][b]);
					if (Double.isFinite(x0) && Double.isFinite(x1) && Double.isFinite(z1)) {
						Plot3D.Bars.box(g, x0, x1, cy - depth, cy + depth, z0, z1, c);
					}
				}
			}
		}
	}

	/* ----------------------------- Area chart ----------------------------- */

	/**
	 * An area chart: stacked areas of several series over x (a solid slab) or the
	 * areas of the series in the depth (stacked false).
	 */
	public static final class Area extends Plot3D.Layer<Area> {
		final double[] x;
		final double[][] series;
		String[] names;
		int[] colors;
		boolean stacked = true;

		Area(double[] x, double[][] series) {
			if (x == null || series == null || series.length == 0) {
				throw new IllegalArgumentException("The area chart needs x and at least one series");
			}
			for (double[] s : series) {
				if (s.length != x.length) {
					throw new IllegalArgumentException("Each series needs a value for each x");
				}
			}
			this.x = x.clone();
			this.series = series.clone();
		}

		/** The names of the series. */
		public Area names(String... names) {
			this.names = names.clone();
			return this;
		}

		/** The colors of the series (0xRRGGBB). */
		public Area colors(int... colors) {
			this.colors = colors.clone();
			return this;
		}

		/** Stacked areas (default true) or the areas of the series in the depth. */
		public Area stacked(boolean stacked) {
			this.stacked = stacked;
			return this;
		}

		private static double v(double value) {
			return Double.isFinite(value) ? Math.max(0, value) : 0;
		}

		@Override
		String[] categories(int axis) {
			if (axis != 1) {
				return null;
			}
			return stacked ? new String[] { "" } : defaultNames(names, series.length, "Series");
		}

		@Override
		double[] preferredAspect() {
			return stacked ? new double[] { 1, 0.3, 0.75 } : null;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (double v : x) {
				add(plot, extent, 0, v);
			}
			add(plot, extent, 2, 0);
			for (int i = 0; i < x.length; i++) {
				double sum = 0;
				for (double[] s : series) {
					sum = stacked ? sum + v(s[i]) : Math.max(sum, v(s[i]));
				}
				add(plot, extent, 2, sum);
			}
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
			String[] n = defaultNames(names, series.length, "Series");
			for (int k = 0; k < series.length; k++) {
				labels.add(n[k]);
				colors.add(seriesColor(this.colors, k));
			}
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			int n = x.length;
			double[] lower = new double[n];
			float[] edge = rgba(0x222222, 0.9f);
			for (int k = 0; k < series.length; k++) {
				double[] upper = new double[n];
				for (int i = 0; i < n; i++) {
					upper[i] = (stacked ? lower[i] : 0) + v(series[k][i]);
				}
				double[] base = stacked ? lower : new double[n];
				double cy = plot.box(1, stacked ? 0 : k);
				double d = stacked ? plot.boxSize()[1] * 0.4 : Math.min(unit(plot, 1) * 0.3, plot.boxSize()[1] * 0.4);
				float[] c = rgba(seriesColor(colors, k), alpha);
				double[] bx = new double[n];
				for (int i = 0; i < n; i++) {
					bx[i] = plot.box(0, plot.value(0, x[i]));
				}
				for (int i = 0; i < n - 1; i++) {
					if (!Double.isFinite(bx[i]) || !Double.isFinite(bx[i + 1])) {
						continue;
					}
					double l0 = z(plot, base[i]), l1 = z(plot, base[i + 1]), u0 = z(plot, upper[i]), u1 = z(plot, upper[i + 1]);
					/* The front, the back, the top and (the first series) the bottom */
					g.quad(new double[] { bx[i], cy - d, l0 }, new double[] { bx[i + 1], cy - d, l1 }, new double[] { bx[i + 1], cy - d, u1 },
							new double[] { bx[i], cy - d, u0 }, c);
					g.quad(new double[] { bx[i + 1], cy + d, l1 }, new double[] { bx[i], cy + d, l0 }, new double[] { bx[i], cy + d, u0 },
							new double[] { bx[i + 1], cy + d, u1 }, c);
					g.quad(new double[] { bx[i], cy - d, u0 }, new double[] { bx[i + 1], cy - d, u1 }, new double[] { bx[i + 1], cy + d, u1 },
							new double[] { bx[i], cy + d, u0 }, c);
					if (k == 0 || !stacked) {
						g.quad(new double[] { bx[i], cy + d, l0 }, new double[] { bx[i + 1], cy + d, l1 }, new double[] { bx[i + 1], cy - d, l1 },
								new double[] { bx[i], cy - d, l0 }, c);
					}
					g.line(new double[] { bx[i], cy - d, u0 }, new double[] { bx[i + 1], cy - d, u1 }, edge);
				}
				/* The ends */
				for (int i : new int[] { 0, n - 1 }) {
					if (!Double.isFinite(bx[i])) {
						continue;
					}
					double l = z(plot, base[i]), u = z(plot, upper[i]);
					if (u > l) {
						g.quad(new double[] { bx[i], cy - d, l }, new double[] { bx[i], cy + d, l }, new double[] { bx[i], cy + d, u },
								new double[] { bx[i], cy - d, u }, c);
					}
				}
				if (stacked) {
					lower = upper;
				}
			}
		}
	}

	/* ----------------------------- Charts without axes ----------------------------- */

	/* The rings (with the values) and the spokes of a radar or rose chart */
	static void polarGrid(PlotGeometry g, double cx, double cy, double radius, int spokes, int rings, double max, String[] labels,
			double angleOffset) {
		float[] grid = rgba(0x888888, 0.8f);
		for (int q = 1; q <= rings; q++) {
			double r = radius * q / rings;
			int steps = Math.max(spokes, 72);
			for (int a = 0; a < steps; a++) {
				double a0 = 2 * Math.PI * a / steps, a1 = 2 * Math.PI * (a + 1) / steps;
				g.line(new double[] { cx + r * Math.cos(a0), cy + r * Math.sin(a0), 0 }, new double[] { cx + r * Math.cos(a1), cy + r * Math.sin(a1), 0 },
						grid);
			}
			double value = max * q / rings;
			g.label(new double[] { cx + r * Math.cos(Math.PI / 2 - angleOffset) + 0.15, cy + r * Math.sin(Math.PI / 2 - angleOffset), 0.05 },
					SpatialScene.formatPlotValue(value, SpatialScene.niceStep(max / rings) / 10, null), 1.0f);
		}
		for (int j = 0; j < spokes; j++) {
			double angle = Math.PI / 2 - 2 * Math.PI * j / spokes - angleOffset;
			g.line(new double[] { cx, cy, 0 }, new double[] { cx + radius * Math.cos(angle), cy + radius * Math.sin(angle), 0 }, grid);
			if (labels != null && j < labels.length) {
				double lr = radius * 1.14;
				g.label(new double[] { cx + lr * Math.cos(angle), cy + lr * Math.sin(angle), 0.05 }, labels[j], 1.35f);
			}
		}
	}

	/**
	 * A radar (spider) chart: the values of several series on the axes which
	 * radiate from the center (a filled polygon for each series, the series a
	 * little above each other).
	 */
	public static final class Radar extends Plot3D.Layer<Radar> {
		final double[][] series;
		final String[] axes;
		String[] names;
		int[] colors;
		double max;
		int rings = 4;

		Radar(double[][] series, String[] axes) {
			if (series == null || series.length == 0 || series[0].length < 3) {
				throw new IllegalArgumentException("The radar chart needs at least one series with three values");
			}
			for (double[] s : series) {
				if (s.length != series[0].length) {
					throw new IllegalArgumentException("Each series needs a value for each axis");
				}
			}
			this.series = series.clone();
			this.axes = axes != null ? axes.clone() : null;
			alpha = 0.35f;
		}

		/** The names of the series (the legend). */
		public Radar names(String... names) {
			this.names = names.clone();
			return this;
		}

		/** The colors of the series (0xRRGGBB). */
		public Radar colors(int... colors) {
			this.colors = colors.clone();
			return this;
		}

		/** The value at the outer ring (default: the maximum of the values). */
		public Radar max(double max) {
			this.max = max;
			return this;
		}

		/** The number of the rings (default 4). */
		public Radar rings(int rings) {
			this.rings = Math.max(1, rings);
			return this;
		}

		@Override
		boolean hasAxes() {
			return false;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
			String[] n = defaultNames(names, series.length, "Series");
			for (int k = 0; k < series.length; k++) {
				labels.add(n[k]);
				colors.add(seriesColor(this.colors, k));
			}
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			int m = series[0].length;
			double top = max;
			if (!(top > 0)) {
				for (double[] s : series) {
					for (double v : s) {
						if (Double.isFinite(v)) {
							top = Math.max(top, v);
						}
					}
				}
				/* A round value at the outer ring */
				double step = SpatialScene.niceStep(top / rings);
				top = Math.max(step, Math.ceil(top / step - 1e-9) * step);
			}
			double cx = 5, cy = 5, radius = 3.6;
			polarGrid(g, cx, cy, radius, m, rings, top, defaultNames(axes, m, "Axis"), 0);
			for (int k = 0; k < series.length; k++) {
				double zz = 0.06 + 0.12 * k;
				double[][] p = new double[m][];
				for (int j = 0; j < m; j++) {
					double angle = Math.PI / 2 - 2 * Math.PI * j / m;
					double r = radius * Math.max(0, Math.min(1.2, (Double.isFinite(series[k][j]) ? series[k][j] : 0) / top));
					p[j] = new double[] { cx + r * Math.cos(angle), cy + r * Math.sin(angle), zz };
				}
				float[] fill = rgba(seriesColor(colors, k), alpha), line = rgba(seriesColor(colors, k), 1);
				double[] center = { cx, cy, zz }, up = { 0, 0, 1 };
				for (int j = 0; j < m; j++) {
					g.triangle(center, p[j], p[(j + 1) % m], up, fill);
					g.line(p[j], p[(j + 1) % m], line);
					g.point(p[j], line, 0.08);
				}
			}
		}
	}

	/**
	 * A rose chart (a wind rose or a polar area chart of Nightingale): sectors of
	 * the same angle with the radius of the values (the classes of a sector
	 * stacked from the center).
	 */
	public static final class Rose extends Plot3D.Layer<Rose> {
		final double[][] values;
		final String[] sectors;
		String[] classes;
		int[] colors;
		double height = 0.5, gap = 0.08;
		boolean areaProportional;
		int rings = 4;

		Rose(double[][] values, String[] sectors) {
			if (values == null || values.length < 2) {
				throw new IllegalArgumentException("The rose chart needs at least two sectors");
			}
			this.values = values.clone();
			this.sectors = sectors != null ? sectors.clone() : null;
		}

		/** The names of the classes (the stacked parts of the sectors, the legend). */
		public Rose classes(String... classes) {
			this.classes = classes.clone();
			return this;
		}

		/** The colors of the classes (0xRRGGBB). */
		public Rose colors(int... colors) {
			this.colors = colors.clone();
			return this;
		}

		/** The area of the sectors proportional to the values (the radius the square root, like Nightingale). */
		public Rose areaProportional(boolean areaProportional) {
			this.areaProportional = areaProportional;
			return this;
		}

		/** The height of the sectors (default 0.5) and the gap between the sectors (a fraction, default 0.08). */
		public Rose shape(double height, double gap) {
			this.height = Math.max(0.02, height);
			this.gap = Math.max(0, Math.min(0.8, gap));
			return this;
		}

		@Override
		boolean hasAxes() {
			return false;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
		}

		private int classCount() {
			int n = 0;
			for (double[] v : values) {
				n = Math.max(n, v.length);
			}
			return n;
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
			int n = classCount();
			if (n > 1 || classes != null) {
				String[] names = defaultNames(classes, n, "Class");
				for (int k = 0; k < n; k++) {
					labels.add(names[k]);
					colors.add(seriesColor(this.colors, k));
				}
			} else {
				super.legend(plot, index, labels, colors);
			}
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			int n = values.length;
			double maxTotal = 0;
			for (double[] v : values) {
				double sum = 0;
				for (double d : v) {
					sum += Double.isFinite(d) ? Math.max(0, d) : 0;
				}
				maxTotal = Math.max(maxTotal, sum);
			}
			double step = SpatialScene.niceStep(maxTotal / rings);
			double top = Math.max(step, Math.ceil(maxTotal / step - 1e-9) * step);
			double cx = 5, cy = 5, radius = 3.8;
			polarGrid(g, cx, cy, radius, n, rings, top, defaultNames(sectors, n, "Sector"), 0);
			double sector = 2 * Math.PI / n, half = sector * (1 - gap) / 2;
			for (int s = 0; s < n; s++) {
				double center = Math.PI / 2 - s * sector;
				double c0 = 0;
				for (int k = 0; k < values[s].length; k++) {
					double v = Double.isFinite(values[s][k]) ? Math.max(0, values[s][k]) : 0;
					double c1 = c0 + v;
					double r0 = radius(c0, top, radius), r1 = radius(c1, top, radius);
					if (r1 > r0) {
						int singleColor = classCount() == 1 && this.color != null ? this.color : seriesColor(colors, k);
						annularSector(g, cx, cy, r0, r1, center - half, center + half, 0, height, rgba(singleColor, alpha));
					}
					c0 = c1;
				}
			}
		}

		private double radius(double value, double top, double radius) {
			double f = top > 0 ? value / top : 0;
			return radius * (areaProportional ? Math.sqrt(f) : f);
		}
	}

	/* An extruded sector of a ring (from the angle a0 to a1, counterclockwise) */
	static void annularSector(PlotGeometry g, double cx, double cy, double r0, double r1, double a0, double a1, double z0, double z1, float[] c) {
		int steps = Math.max(2, (int) Math.ceil((a1 - a0) / (2 * Math.PI) * 96));
		for (int k = 0; k < steps; k++) {
			double b0 = a0 + (a1 - a0) * k / steps, b1 = a0 + (a1 - a0) * (k + 1) / steps;
			double[] i0 = { cx + r0 * Math.cos(b0), cy + r0 * Math.sin(b0) }, i1 = { cx + r0 * Math.cos(b1), cy + r0 * Math.sin(b1) };
			double[] o0 = { cx + r1 * Math.cos(b0), cy + r1 * Math.sin(b0) }, o1 = { cx + r1 * Math.cos(b1), cy + r1 * Math.sin(b1) };
			/* The top and the bottom */
			g.quad(new double[] { i0[0], i0[1], z1 }, new double[] { o0[0], o0[1], z1 }, new double[] { o1[0], o1[1], z1 }, new double[] { i1[0], i1[1], z1 },
					c);
			g.quad(new double[] { i1[0], i1[1], z0 }, new double[] { o1[0], o1[1], z0 }, new double[] { o0[0], o0[1], z0 }, new double[] { i0[0], i0[1], z0 },
					c);
			/* The outer and the inner wall (normals of the circle) */
			double[] n0 = { Math.cos(b0), Math.sin(b0), 0 }, n1 = { Math.cos(b1), Math.sin(b1), 0 };
			g.triangle(new double[] { o0[0], o0[1], z0 }, n0, c, new double[] { o1[0], o1[1], z0 }, n1, c, new double[] { o1[0], o1[1], z1 }, n1, c);
			g.triangle(new double[] { o0[0], o0[1], z0 }, n0, c, new double[] { o1[0], o1[1], z1 }, n1, c, new double[] { o0[0], o0[1], z1 }, n0, c);
			if (r0 > 1e-6) {
				double[] m0 = { -n0[0], -n0[1], 0 }, m1 = { -n1[0], -n1[1], 0 };
				g.triangle(new double[] { i1[0], i1[1], z0 }, m1, c, new double[] { i0[0], i0[1], z0 }, m0, c, new double[] { i0[0], i0[1], z1 }, m0, c);
				g.triangle(new double[] { i1[0], i1[1], z0 }, m1, c, new double[] { i0[0], i0[1], z1 }, m0, c, new double[] { i1[0], i1[1], z1 }, m1, c);
			}
		}
		/* The side walls */
		for (double a : new double[] { a0, a1 }) {
			double[] i = { cx + r0 * Math.cos(a), cy + r0 * Math.sin(a) }, o = { cx + r1 * Math.cos(a), cy + r1 * Math.sin(a) };
			g.quad(new double[] { i[0], i[1], z0 }, new double[] { o[0], o[1], z0 }, new double[] { o[0], o[1], z1 }, new double[] { i[0], i[1], z1 }, c);
		}
	}

	/**
	 * A treemap: the values as rectangles with the area of the values (a squarified
	 * layout) as blocks (optional with the height of the values) and the labels.
	 */
	public static final class Treemap extends Plot3D.Layer<Treemap> {
		final double[] values;
		final String[] labels;
		int[] colors;
		boolean heights;
		double gap = 0.06;
		boolean showValues = true;

		Treemap(double[] values, String[] labels) {
			if (values == null || values.length == 0) {
				throw new IllegalArgumentException("The treemap needs values");
			}
			this.values = values.clone();
			this.labels = labels != null ? labels.clone() : null;
		}

		/** The colors of the rectangles (0xRRGGBB, default the colors of the categories, or a color map of the values). */
		public Treemap colors(int... colors) {
			this.colors = colors.clone();
			return this;
		}

		/** The heights of the blocks proportional to the values (default false: flat blocks). */
		public Treemap heights(boolean heights) {
			this.heights = heights;
			return this;
		}

		/** Displays the values below the labels (default true). */
		public Treemap showValues(boolean showValues) {
			this.showValues = showValues;
			return this;
		}

		@Override
		boolean hasAxes() {
			return false;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
		}

		@Override
		double[] colorValueRange() {
			double[] s = sorted(values);
			return s.length > 0 && s[s.length - 1] > s[0] ? new double[] { s[0], s[s.length - 1] } : new double[] { 0, 1 };
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			List<Integer> order = new ArrayList<>();
			for (int i = 0; i < values.length; i++) {
				if (Double.isFinite(values[i]) && values[i] > 0) {
					order.add(i);
				}
			}
			order.sort((a, b) -> Double.compare(values[b], values[a]));
			double total = order.stream().mapToDouble(i -> values[i]).sum();
			if (total <= 0) {
				return;
			}
			double[][] rects = new double[values.length][];
			squarify(order, values, total, 0, 0, 10, 10, rects);
			double[] range = colorValueRange();
			double maxValue = values[order.get(0)];
			for (int i : order) {
				double[] r = rects[i];
				double x0 = r[0] + gap, y0 = r[1] + gap, x1 = r[0] + r[2] - gap, y1 = r[1] + r[3] - gap;
				if (x1 <= x0 || y1 <= y0) {
					continue;
				}
				int rgb = colorMap != null ? PlotLegend.rampColor(colorMap, range[0], range[1], values[i]) : seriesColor(colors, order.indexOf(i));
				double h = heights ? 0.2 + 1.4 * values[i] / maxValue : 0.35;
				Plot3D.Bars.box(g, x0, x1, y0, y1, 0, h, rgba(rgb, alpha));
				double size = Math.max(0.55, Math.min(1.3, Math.sqrt((x1 - x0) * (y1 - y0)) / 2.2));
				String label = labels != null && i < labels.length ? labels[i] : "";
				String value = showValues ? SpatialScene.formatPlotValue(values[i], niceDecimals(values[i]), null) : "";
				double cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
				/* The label above the value (the texts face the camera) */
				if (!label.isEmpty()) {
					g.label(new double[] { cx, cy, h + 0.2 + (value.isEmpty() ? 0 : 0.32 * size) }, label, (float) size);
				}
				if (!value.isEmpty()) {
					g.label(new double[] { cx, cy, h + 0.2 }, value, (float) (size * 0.8));
				}
			}
		}

		private static double niceDecimals(double v) {
			return Math.abs(v) >= 100 ? 1 : Math.abs(v) >= 1 ? 0.1 : 0.001;
		}
	}

	/* A squarified treemap layout (Bruls, Huizing and van Wijk 2000) of the sorted values in a rectangle */
	static void squarify(List<Integer> items, double[] values, double total, double x, double y, double w, double h, double[][] rects) {
		List<Integer> rest = new ArrayList<>(items);
		double scale = w * h / total;
		while (!rest.isEmpty()) {
			double side = Math.min(w, h);
			List<Integer> row = new ArrayList<>();
			double rowSum = 0;
			double worst = Double.MAX_VALUE;
			while (!rest.isEmpty()) {
				int next = rest.get(0);
				double sum = rowSum + values[next] * scale;
				List<Integer> candidate = new ArrayList<>(row);
				candidate.add(next);
				double max = 0, min = Double.MAX_VALUE;
				for (int i : candidate) {
					max = Math.max(max, values[i] * scale);
					min = Math.min(min, values[i] * scale);
				}
				double ratio = Math.max(side * side * max / (sum * sum), sum * sum / (side * side * min));
				if (!row.isEmpty() && ratio > worst) {
					break;
				}
				row = candidate;
				rowSum = sum;
				worst = ratio;
				rest.remove(0);
			}
			/* The row along the shorter side */
			double thickness = rowSum / side;
			double offset = 0;
			for (int i : row) {
				double length = values[i] * scale / thickness;
				rects[i] = w >= h ? new double[] { x, y + offset, thickness, length } : new double[] { x + offset, y, length, thickness };
				offset += length;
			}
			if (w >= h) {
				x += thickness;
				w -= thickness;
			} else {
				y += thickness;
				h -= thickness;
			}
		}
	}
}
