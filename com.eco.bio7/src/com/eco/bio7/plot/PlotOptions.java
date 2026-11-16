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
package com.eco.bio7.plot;

import java.util.Locale;

/**
 * The attributes of a plot of the variables of the R-Shell and the
 * Python-Shell (see PlotView). Empty labels and limits: the defaults
 * of R or matplotlib. The style "modern": a left aligned bold title with
 * subtitle and caption, a light grid behind the data, no box, points with a
 * white border; "classic": the defaults of R or matplotlib.
 */
public record PlotOptions(String kind, String style, String palette, String title, String subtitle, String caption, String xLabel, String yLabel,
		double fontSize, double titleSize, String color, double opacity, int bins, double lineWidth, String lineStyle, double pointSize, String pointShape,
		String legend, String xMin, String xMax, String yMin, String yMax, boolean grid, boolean logY, boolean horizontal, boolean valueLabels,
		boolean observations) {

	/** The plot types (the id and the label of the menus). */
	public static final String[][] KINDS = { { "line", "Line" }, { "area", "Area" }, { "scatter", "Scatter" }, { "hist", "Histogram" }, { "density", "Density" },
			{ "ecdf", "ECDF (Cumulative)" }, { "box", "Boxplot" }, { "violin", "Violin" }, { "strip", "Strip (Jitter)" }, { "bar", "Barplot" }, { "pie", "Pie" },
			{ "pairs", "Scatter Matrix" }, { "corr", "Correlation Matrix" }, { "heatmap", "Heatmap" }, { "contour", "Contour" }, { "surface", "3D Surface" },
			{ "qq", "Q-Q Plot" } };

	/** The styles (the id and the label). */
	public static final String[][] STYLES = { { "modern", "Modern" }, { "classic", "Classic" } };

	/** The palettes (the id, the label and the colors). */
	public static final String[][] PALETTES = { { "modern", "Modern", "#2B5C8F,#D95F02,#1B9E77,#7570B3,#E7298A,#66A61E,#E6AB02,#A6761D" },
			{ "okabe", "Okabe-Ito (color blind safe)", "#0072B2,#E69F00,#009E73,#D55E00,#56B4E9,#CC79A7,#F0E442,#000000" },
			{ "viridis", "Viridis", "#440154,#46337E,#365C8D,#277F8E,#1FA187,#4AC16D,#A0DA39,#FDE725" },
			{ "set2", "Set2 (pastel)", "#66C2A5,#FC8D62,#8DA0CB,#E78AC3,#A6D854,#FFD92F,#E5C494,#B3B3B3" },
			{ "gray", "Gray", "#252525,#636363,#969696,#BDBDBD,#4D4D4D,#7F7F7F,#A6A6A6,#D9D9D9" } };

	/** The point shapes (the id and the label). */
	public static final String[][] SHAPES = { { "circle", "Circle" }, { "square", "Square" }, { "triangle", "Triangle" }, { "diamond", "Diamond" } };

	/** The line styles (the id and the label). */
	public static final String[][] LINES = { { "solid", "Solid" }, { "dashed", "Dashed" }, { "dotted", "Dotted" } };

	/** The positions of the legend (the id and the label). */
	public static final String[][] LEGENDS = { { "auto", "Automatic" }, { "topleft", "Top left" }, { "topright", "Top right" }, { "bottomleft", "Bottom left" },
			{ "bottomright", "Bottom right" }, { "none", "None" } };

	/** The palette of the modern style (the first color is the default color). */
	public static final String[] MODERN = colors("modern");

	/** The defaults. */
	public static final PlotOptions DEFAULT = new PlotOptions("line", "modern", "modern", "", "", "", "", "", 11, 14, MODERN[0], 1.0, 30, 2.2, "solid", 1.0,
			"circle", "auto", "", "", "", "", false, false, false, true, true);

	/* The last options of the dialog (the menu entries use them with their type) */
	private static PlotOptions last = DEFAULT;

	public static synchronized PlotOptions last() {
		return last;
	}

	static synchronized void setLast(PlotOptions o) {
		last = o;
	}

	/** The options with another plot type. */
	public PlotOptions withKind(String k) {
		return new PlotOptions(k, style, palette, title, subtitle, caption, xLabel, yLabel, fontSize, titleSize, color, opacity, bins, lineWidth, lineStyle,
				pointSize, pointShape, legend, xMin, xMax, yMin, yMax, grid, logY, horizontal, valueLabels, observations);
	}

	/** True for the modern style. */
	public boolean modern() {
		return "modern".equals(style);
	}

	/** The colors of the plot: the color of the dialog, then the other colors of the palette. */
	public String[] plotColors() {
		String[] p = colors(palette);
		java.util.List<String> list = new java.util.ArrayList<>();
		list.add(color);
		for (String c : p) {
			if (!c.equalsIgnoreCase(color)) {
				list.add(c);
			}
		}
		return list.toArray(String[]::new);
	}

	/** The colors of a palette. */
	public static String[] colors(String palette) {
		for (String[] p : PALETTES) {
			if (p[0].equals(palette)) {
				return p[2].split(",");
			}
		}
		return PALETTES[0][2].split(",");
	}

	/** A limit of an axis (null if empty or not a number). */
	public static Double limit(String text) {
		try {
			return text == null || text.isBlank() ? null : Double.parseDouble(text.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** The label of the plot type. */
	public static String label(String kind) {
		for (String[] k : KINDS) {
			if (k[0].equals(kind)) {
				return k[1];
			}
		}
		return kind;
	}

	/** A number for the code (no locale). */
	public static String num(double d) {
		return String.format(Locale.ROOT, "%.6g", d);
	}
}
