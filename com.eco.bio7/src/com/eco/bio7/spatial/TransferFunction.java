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

import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A piecewise linear transfer function (like in ParaView): colors and opacities
 * are defined by control points on the normalized value range 0 - 1 (the
 * minimum and maximum of the data or of the display range) and are interpolated
 * linearly between the points. The function is evaluated as a table of 256
 * colors and opacities (see getTable).
 */
public class TransferFunction {

	/** The names of the presets (see fromPreset). */
	public static final String[] PRESETS = { "Viridis", "Cool to Warm", "Fire", "Grays", "Rainbow", "Ice",
			"Black-Body" };

	/** A color control point: position (0 - 1) and the color (0 - 1). */
	public static class ColorPoint {
		public double x;
		public double r, g, b;

		public ColorPoint(double x, double r, double g, double b) {
			this.x = x;
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}

	/** An opacity control point: position (0 - 1) and the opacity (0 - 1). */
	public static class OpacityPoint {
		public double x;
		public double a;

		public OpacityPoint(double x, double a) {
			this.x = x;
			this.a = a;
		}
	}

	private final List<ColorPoint> colors = new ArrayList<>();
	/* The name of the colors (a preset, "Image LUT") or null (edited colors) */
	private String colorName;
	private final List<OpacityPoint> opacities = new ArrayList<>();

	/**
	 * Creates a transfer function with the Viridis colors and a linear opacity
	 * ramp (0 - 1).
	 */
	public TransferFunction() {
		setPreset("Viridis");
		setLinearOpacity();
	}

	/**
	 * Creates a transfer function with the colors of a preset and a linear
	 * opacity ramp.
	 *
	 * @param name the name of the preset (see PRESETS)
	 * @return the transfer function
	 */
	public static TransferFunction fromPreset(String name) {
		TransferFunction tf = new TransferFunction();
		tf.setPreset(name);
		return tf;
	}

	/**
	 * Creates a transfer function with the colors of a LUT (e.g., of an image)
	 * and a linear opacity ramp.
	 *
	 * @param cm the color model (256 colors)
	 * @return the transfer function
	 */
	public static TransferFunction fromColorModel(ColorModel cm) {
		TransferFunction tf = new TransferFunction();
		tf.setColors(cm);
		return tf;
	}

	/** Returns a copy of the transfer function. */
	public synchronized TransferFunction copy() {
		TransferFunction tf = new TransferFunction();
		tf.colors.clear();
		tf.opacities.clear();
		for (ColorPoint c : colors) {
			tf.colors.add(new ColorPoint(c.x, c.r, c.g, c.b));
		}
		for (OpacityPoint o : opacities) {
			tf.opacities.add(new OpacityPoint(o.x, o.a));
		}
		tf.colorName = colorName;
		return tf;
	}

	/**
	 * Sets the colors of a preset (the opacities are kept).
	 *
	 * @param name the name of the preset (see PRESETS)
	 */
	public synchronized void setPreset(String name) {
		int[] rgb;
		switch (name) {
		case "Cool to Warm":
			rgb = new int[] { 0x3b4cc0, 0x7396f5, 0xb0cbfc, 0xdddddd, 0xf6bfa6, 0xea7b60, 0xb40426 };
			break;
		case "Fire":
			rgb = new int[] { 0x000000, 0x5a0080, 0xc0007a, 0xff4a00, 0xffb400, 0xffffff };
			break;
		case "Grays":
			rgb = new int[] { 0x000000, 0xffffff };
			break;
		case "Rainbow":
			rgb = new int[] { 0x0000ff, 0x00ffff, 0x00ff00, 0xffff00, 0xff0000 };
			break;
		case "Ice":
			rgb = new int[] { 0x000000, 0x1b2a6e, 0x2d7fc0, 0x6fd0e8, 0xffffff };
			break;
		case "Black-Body":
			rgb = new int[] { 0x000000, 0xb22222, 0xe36905, 0xeed214, 0xffffff };
			break;
		default:
			rgb = new int[] { 0x440154, 0x3b528b, 0x21918c, 0x5ec962, 0xfde725 };
		}
		colorName = name;
		colors.clear();
		for (int i = 0; i < rgb.length; i++) {
			colors.add(new ColorPoint(i / (double) (rgb.length - 1), ((rgb[i] >> 16) & 0xff) / 255.0,
					((rgb[i] >> 8) & 0xff) / 255.0, (rgb[i] & 0xff) / 255.0));
		}
	}

	/**
	 * Sets the colors of a LUT (9 control points, the opacities are kept).
	 *
	 * @param cm the color model (256 colors)
	 */
	public synchronized void setColors(ColorModel cm) {
		if (cm == null) {
			return;
		}
		colorName = "Image LUT";
		colors.clear();
		for (int i = 0; i <= 8; i++) {
			int index = Math.min(255, i * 32);
			int rgb = cm.getRGB(index);
			colors.add(new ColorPoint(index / 255.0, ((rgb >> 16) & 0xff) / 255.0, ((rgb >> 8) & 0xff) / 255.0,
					(rgb & 0xff) / 255.0));
		}
	}

	/** A linear opacity ramp from 0 (minimum) to 1 (maximum). */
	public synchronized void setLinearOpacity() {
		opacities.clear();
		opacities.add(new OpacityPoint(0, 0));
		opacities.add(new OpacityPoint(1, 1));
	}

	/**
	 * A linear opacity ramp from the opacity 0 at the position low to the opacity
	 * 1 at the position high (e.g., a window of the values).
	 */
	public synchronized void setLinearOpacity(double low, double high) {
		opacities.clear();
		low = clamp(low);
		high = Math.max(low, clamp(high));
		if (low > 0) {
			opacities.add(new OpacityPoint(0, 0));
		}
		opacities.add(new OpacityPoint(low, 0));
		opacities.add(new OpacityPoint(high, 1));
		if (high < 1) {
			opacities.add(new OpacityPoint(1, 1));
		}
	}

	/** A constant opacity. */
	public synchronized void setConstantOpacity(double a) {
		opacities.clear();
		opacities.add(new OpacityPoint(0, clamp(a)));
		opacities.add(new OpacityPoint(1, clamp(a)));
	}

	/** Reverses the colors (the opacities are kept). */
	public synchronized void reverseColors() {
		colorName = null;
		for (ColorPoint c : colors) {
			c.x = 1 - c.x;
		}
		sortColors();
	}

	/**
	 * Returns the name of the colors (a preset or "Image LUT") or null if the
	 * colors have been edited.
	 */
	public synchronized String getColorName() {
		return colorName;
	}

	/** Marks the colors as edited (no preset). */
	public synchronized void setColorsEdited() {
		colorName = null;
	}

	/** Adds a color control point (the color is interpolated). */
	public synchronized ColorPoint addColorPoint(double x) {
		double[] c = colorAt(clamp(x));
		ColorPoint p = new ColorPoint(clamp(x), c[0], c[1], c[2]);
		colors.add(p);
		colorName = null;
		sortColors();
		return p;
	}

	/** Adds an opacity control point. */
	public synchronized OpacityPoint addOpacityPoint(double x, double a) {
		OpacityPoint p = new OpacityPoint(clamp(x), clamp(a));
		opacities.add(p);
		sortOpacities();
		return p;
	}

	/** Removes a color control point (at least two points are kept). */
	public synchronized void removeColorPoint(ColorPoint p) {
		if (colors.size() > 2) {
			colors.remove(p);
			colorName = null;
		}
	}

	/** Removes an opacity control point (at least two points are kept). */
	public synchronized void removeOpacityPoint(OpacityPoint p) {
		if (opacities.size() > 2) {
			opacities.remove(p);
		}
	}

	public synchronized List<ColorPoint> getColorPoints() {
		return colors;
	}

	public synchronized List<OpacityPoint> getOpacityPoints() {
		return opacities;
	}

	/** Sorts the control points after a change of the positions. */
	public synchronized void sortColors() {
		colors.sort(Comparator.comparingDouble(p -> p.x));
	}

	public synchronized void sortOpacities() {
		opacities.sort(Comparator.comparingDouble(p -> p.x));
	}

	/**
	 * Returns the interpolated color at the position x (0 - 1).
	 *
	 * @param x the position
	 * @return the color (r, g, b: 0 - 1)
	 */
	public synchronized double[] colorAt(double x) {
		if (colors.isEmpty()) {
			return new double[] { x, x, x };
		}
		ColorPoint first = colors.get(0), last = colors.get(colors.size() - 1);
		if (x <= first.x) {
			return new double[] { first.r, first.g, first.b };
		}
		if (x >= last.x) {
			return new double[] { last.r, last.g, last.b };
		}
		for (int i = 0; i < colors.size() - 1; i++) {
			ColorPoint a = colors.get(i), b = colors.get(i + 1);
			if (x >= a.x && x <= b.x) {
				double t = b.x > a.x ? (x - a.x) / (b.x - a.x) : 0;
				return new double[] { a.r + t * (b.r - a.r), a.g + t * (b.g - a.g), a.b + t * (b.b - a.b) };
			}
		}
		return new double[] { last.r, last.g, last.b };
	}

	/**
	 * Returns the interpolated opacity at the position x (0 - 1).
	 *
	 * @param x the position
	 * @return the opacity (0 - 1)
	 */
	public synchronized double opacityAt(double x) {
		if (opacities.isEmpty()) {
			return x;
		}
		OpacityPoint first = opacities.get(0), last = opacities.get(opacities.size() - 1);
		if (x <= first.x) {
			return first.a;
		}
		if (x >= last.x) {
			return last.a;
		}
		for (int i = 0; i < opacities.size() - 1; i++) {
			OpacityPoint a = opacities.get(i), b = opacities.get(i + 1);
			if (x >= a.x && x <= b.x) {
				double t = b.x > a.x ? (x - a.x) / (b.x - a.x) : 0;
				return a.a + t * (b.a - a.a);
			}
		}
		return last.a;
	}

	/**
	 * Returns the table of 256 colors and opacities (index = the value scaled to
	 * 0 - 255).
	 *
	 * @return the colors (0xRRGGBB) and opacities (0 - 1): {rgb[256], alpha[256]}
	 */
	public synchronized Object[] getTable() {
		int[] rgb = new int[256];
		float[] alpha = new float[256];
		for (int i = 0; i < 256; i++) {
			double x = i / 255.0;
			double[] c = colorAt(x);
			rgb[i] = ((int) Math.round(c[0] * 255) << 16) | ((int) Math.round(c[1] * 255) << 8)
					| (int) Math.round(c[2] * 255);
			alpha[i] = (float) opacityAt(x);
		}
		return new Object[] { rgb, alpha };
	}

	/** Returns the 256 colors (0xRRGGBB) of the table. */
	public int[] getColorTable() {
		return (int[]) getTable()[0];
	}

	/** Returns the 256 opacities (0 - 1) of the table. */
	public float[] getOpacityTable() {
		return (float[]) getTable()[1];
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(1, v));
	}
}
