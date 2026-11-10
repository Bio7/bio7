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

package com.eco.bio7.rffm.graphics;

/**
 * The graphics parameters of a drawing operation (R_GE_gcontext of R).
 *
 * @param col        the line color (R's ABGR: red in the lowest byte, alpha in
 *                   the highest, alpha 0 is transparent)
 * @param fill       the fill color (same format)
 * @param gamma      the gamma correction
 * @param lwd        the line width (1 = 1/96 inch)
 * @param lty        the line type (-1 blank, 0 solid, else 4-bit dash/gap
 *                   lengths)
 * @param lend       the line end: 1 round, 2 butt, 3 square
 * @param ljoin      the line join: 1 round, 2 mitre, 3 bevel
 * @param lmitre     the mitre limit
 * @param cex        the character expansion
 * @param ps         the font size in points
 * @param lineheight the line height (multiple of the font height)
 * @param fontface   1 plain, 2 bold, 3 italic, 4 bold italic, 5 symbol
 * @param fontfamily the font family ("" is the default, "serif", "sans",
 *                   "mono" or a name)
 */
public record GraphicsContext(int col, int fill, double gamma, double lwd, int lty, int lend, int ljoin, double lmitre, double cex, double ps,
		double lineheight, int fontface, String fontfamily) {

	/** LTY_BLANK: the line is not drawn. */
	public static final int LTY_BLANK = -1;

	/** The alpha (0-255) of an R color. */
	public static int alpha(int color) {
		return (color >>> 24) & 0xFF;
	}

	/** Returns true if an R color is fully transparent. */
	public static boolean isTransparent(int color) {
		return alpha(color) == 0;
	}

	/** An R color (ABGR) as ARGB (e.g., for java.awt.Color or images). */
	public static int toARGB(int color) {
		int r = color & 0xFF;
		int g = (color >>> 8) & 0xFF;
		int b = (color >>> 16) & 0xFF;
		return (alpha(color) << 24) | (r << 16) | (g << 8) | b;
	}

	/** The font size in points (cex * ps). */
	public double fontSize() {
		return cex * ps;
	}
}
