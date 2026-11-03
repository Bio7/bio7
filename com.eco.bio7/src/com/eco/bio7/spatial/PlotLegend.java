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

/**
 * A legend of the 3D view (see SpatialUtil.setLegend and
 * SpatialUtil.setColorBar): the labels with the colors of categories (e.g., the
 * groups of a scatter plot) or a color bar of a continuous color ramp with the
 * numbers of the values (e.g., the heights of a surface).
 */
public final class PlotLegend {

	/** A color ramp (dark blue - green - yellow, like viridis). */
	public static final int[] VIRIDIS = { 0x440154, 0x3B528B, 0x21908C, 0x5DC963, 0xFDE725 };
	/** A color ramp (blue - cyan - green - yellow - red). */
	public static final int[] RAINBOW = { 0x30129F, 0x0099E6, 0x33BF4D, 0xFAD91A, 0xD91A1A };
	/** A color ramp (blue - white - red) for values below and above a center. */
	public static final int[] DIVERGING = { 0x2166AC, 0x92C5DE, 0xF7F7F7, 0xF4A582, 0xB2182B };
	/** A color ramp of the heights of a landscape (green - brown - white). */
	public static final int[] TERRAIN = { 0x2E8B3A, 0x9AB04A, 0x9C6B3A, 0xC8B8A8, 0xFAFAFA };

	final String title;
	/* The labels of the categories (null for a color bar) */
	final String[] labels;
	/* The colors (0xRRGGBB): of the categories or the ramp of the color bar from the minimum to the maximum */
	final int[] colors;
	/* The color bar: the values of the ends of the ramp, log10 values and the format of the numbers */
	final double min, max;
	final boolean log;
	final String format;

	private PlotLegend(String title, String[] labels, int[] colors, double min, double max, boolean log, String format) {
		this.title = title;
		this.labels = labels;
		this.colors = colors;
		this.min = min;
		this.max = max;
		this.log = log;
		this.format = format;
	}

	static PlotLegend categories(String title, String[] labels, int[] colors) {
		if (labels == null || colors == null || labels.length != colors.length) {
			throw new IllegalArgumentException("The legend needs a color for each label");
		}
		return new PlotLegend(title, labels.clone(), colors.clone(), 0, 0, false, null);
	}

	static PlotLegend colorBar(String title, double min, double max, int[] colors, boolean log, String format) {
		if (colors == null || colors.length < 2) {
			throw new IllegalArgumentException("The color bar needs at least two colors");
		}
		if (!(max > min)) {
			throw new IllegalArgumentException("The maximum of the color bar must be larger than the minimum");
		}
		return new PlotLegend(title, null, colors.clone(), min, max, log, format);
	}

	boolean isColorBar() {
		return labels == null;
	}

	/**
	 * Returns the color (0xRRGGBB) of a value of a color ramp (the colors are
	 * evenly distributed from the minimum to the maximum and interpolated), e.g.,
	 * to draw the data with the colors of a color bar.
	 *
	 * @param colors the colors of the ramp (0xRRGGBB)
	 * @param min    the value of the first color
	 * @param max    the value of the last color
	 * @param value  the value (clamped to the range)
	 * @return the color (0xRRGGBB)
	 */
	public static int rampColor(int[] colors, double min, double max, double value) {
		double t = max > min ? (value - min) / (max - min) : 0;
		t = Math.max(0, Math.min(1, Double.isNaN(t) ? 0 : t));
		double p = t * (colors.length - 1);
		int a = Math.min(colors.length - 2, (int) p);
		double w = p - a;
		int c0 = colors[a], c1 = colors[a + 1];
		int r = (int) Math.round(((c0 >> 16) & 0xff) * (1 - w) + ((c1 >> 16) & 0xff) * w);
		int g = (int) Math.round(((c0 >> 8) & 0xff) * (1 - w) + ((c1 >> 8) & 0xff) * w);
		int b = (int) Math.round((c0 & 0xff) * (1 - w) + (c1 & 0xff) * w);
		return (r << 16) | (g << 8) | b;
	}

	/**
	 * Sets the OpenGL color of a color (0xRRGGBB), e.g., of a color of a legend
	 * or of rampColor.
	 * 
	 * @param gl  the OpenGL context
	 * @param rgb the color (0xRRGGBB)
	 */
	public static void setColor(com.jogamp.opengl.GL2 gl, int rgb) {
		gl.glColor3f(((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f);
	}
}
