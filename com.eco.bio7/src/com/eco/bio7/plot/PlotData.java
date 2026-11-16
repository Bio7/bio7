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

import java.util.List;

/**
 * The data of a plot of the shells: expressions of the workspace (variables or
 * columns, e.g., df$col in R, df['col'] in Python) for the roles of the plot
 * type. Empty: not used.
 *
 * @param x     the x values (line, area, scatter) or the x axis (heatmap,
 *              contour, surface)
 * @param ys    the values: lines, y of a scatter plot, the variables of the
 *              distributions, the y axis of a matrix plot (the first)
 * @param group a factor which splits the values (distributions), colors a
 *              scatter plot or gives the categories of bar and pie plots
 * @param z     a matrix (heatmap, contour, surface)
 */
public record PlotData(String x, List<String> ys, String group, String z) {

	public PlotData {
		x = x == null ? "" : x.trim();
		ys = ys == null ? List.of() : ys.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
		group = group == null ? "" : group.trim();
		z = z == null ? "" : z.trim();
	}

	/** The data of selected variables (the values). */
	public static PlotData of(List<String> names) {
		return new PlotData("", names, "", "");
	}

	/** All expressions (for the title of the dialog and the source). */
	public String describe() {
		StringBuilder s = new StringBuilder(String.join(", ", ys));
		if (!z.isEmpty()) {
			s.insert(0, z + (s.isEmpty() ? "" : ", "));
		}
		if (!x.isEmpty()) {
			s.insert(0, x + " / ");
		}
		if (!group.isEmpty()) {
			s.append(" by ").append(group);
		}
		return s.toString();
	}

	/* ---------------- The roles of the plot types ---------------- */

	/** Uses the plot type X values (or an x axis)? */
	public static boolean usesX(String kind) {
		return kind.matches("line|area|scatter|heatmap|contour|surface");
	}

	/** Uses the plot type a group? */
	public static boolean usesGroup(String kind) {
		return kind.matches("scatter|hist|density|ecdf|box|violin|strip|qq|bar|pie");
	}

	/** Uses the plot type a matrix? */
	public static boolean usesZ(String kind) {
		return kind.matches("heatmap|contour|surface");
	}

	/** A short help for the data of the plot type (the dialog). */
	public static String help(String kind) {
		return switch (kind) {
		case "line", "area" -> "Y: one or more variables (a line each); X: the x positions (optional, else the index).";
		case "scatter" -> "X and Y (several Y: a color each); Group: colors the points (optional). Without X: Y over the index.";
		case "hist", "density", "ecdf", "box", "violin", "strip" -> "Y: one or more variables; Group: splits the values into the groups (optional).";
		case "qq" -> "Y: a variable; Group: a Q-Q plot of each group (optional).";
		case "bar" -> "Y: values or a factor (counts); Group: the categories (the mean of Y in each group, or the counts).";
		case "pie" -> "Y: values or a factor (counts); Group: the categories (the sum of Y in each group, or the counts).";
		case "pairs", "corr" -> "Y: two or more variables (or a data frame).";
		case "heatmap" -> "Z: a matrix or data frame (else the Y variables as columns).";
		default -> "Z: a matrix or data frame (else the Y variables as columns); X, Y: the values of the axes (optional).";
		};
	}
}
