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
 * A graphics device of R implemented in Java: the graphics engine of R calls
 * these methods to draw (in the thread of R).
 * <p>
 * The coordinates are device units (pixels): x from the left, y from the top.
 * The methods must not throw (an exception is caught and ignored) and must not
 * wait for the UI thread (R waits meanwhile): hand the result to the UI
 * asynchronously.
 *
 * @see Java2DGraphicsDevice
 */
public interface RGraphicsDevice {

	/** The width in pixels (the device size, updated by resize). */
	double width();

	/** The height in pixels. */
	double height();

	/** Starts a new page filled with gc.fill (if not transparent). */
	void newPage(GraphicsContext gc);

	void line(double x1, double y1, double x2, double y2, GraphicsContext gc);

	void polyline(double[] x, double[] y, GraphicsContext gc);

	/** A polygon: filled with gc.fill, outlined with gc.col. */
	void polygon(double[] x, double[] y, GraphicsContext gc);

	void rect(double x0, double y0, double x1, double y1, GraphicsContext gc);

	void circle(double x, double y, double r, GraphicsContext gc);

	/**
	 * A path of several polygons.
	 *
	 * @param nper    the number of points of each polygon
	 * @param winding true: the non-zero winding rule, false: even-odd
	 */
	void path(double[] x, double[] y, int[] nper, boolean winding, GraphicsContext gc);

	/**
	 * A text.
	 *
	 * @param rot  the rotation in degrees (counterclockwise)
	 * @param hadj the horizontal adjustment (0 left, 0.5 center, 1 right)
	 */
	void text(double x, double y, String text, double rot, double hadj, GraphicsContext gc);

	/** The width of a text in pixels. */
	double strWidth(String text, GraphicsContext gc);

	/**
	 * The metrics of a character.
	 *
	 * @param codePoint the Unicode code point (0: the maximum of the font)
	 * @return ascent, descent and width in pixels
	 */
	double[] metricInfo(int codePoint, GraphicsContext gc);

	/**
	 * An image.
	 *
	 * @param abgr        the pixels in R's color format, row by row from the top
	 * @param x           the left of the image (device units)
	 * @param y           the bottom of the image
	 * @param width       the width (device units)
	 * @param height      the height (negative if the y axis points down)
	 * @param rot         the rotation in degrees
	 * @param interpolate true: smooth scaling
	 */
	void raster(int[] abgr, int w, int h, double x, double y, double width, double height, double rot, boolean interpolate, GraphicsContext gc);

	/** Sets the clipping rectangle. */
	void clip(double x0, double x1, double y0, double y1);

	/** 1: drawing starts, 0: drawing stops (e.g., to show the result). */
	default void mode(int mode) {
	}

	/**
	 * Holds (level > 0) or releases (level < 0) the output, see dev.hold().
	 *
	 * @return the current level
	 */
	default int holdflush(int level) {
		return 0;
	}

	/**
	 * Returns true if the device supports locator() (clicks in the plot). The
	 * device state is read when R opens the device.
	 */
	default boolean supportsLocator() {
		return false;
	}

	/**
	 * Waits for a click for locator() or identify() (in the thread of R, which waits
	 * meanwhile: the UI must not wait for R).
	 *
	 * @return the position in device units, or null to stop (e.g., a right click)
	 */
	default double[] locator() {
		return null;
	}

	default void activate() {
	}

	default void deactivate() {
	}

	/** The device was closed in R (dev.off()). */
	void close();
}
