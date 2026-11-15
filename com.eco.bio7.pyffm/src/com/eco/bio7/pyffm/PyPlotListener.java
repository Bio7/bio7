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
package com.eco.bio7.pyffm;

/**
 * Receives the images of matplotlib figures (the plot device of the embedded
 * Python): a new or changed figure, or a figure drawn again in a new size.
 * Called in the thread of Python: copy the data and hand it to the UI
 * asynchronously.
 */
public interface PyPlotListener {

	/**
	 * A figure was drawn.
	 *
	 * @param figure the number of the figure (matplotlib)
	 * @param rgba   the pixels (red, green, blue, alpha per pixel, row by row)
	 * @param width  the width in pixels
	 * @param height the height in pixels
	 * @param scale  the device scale the figure was drawn for (pixels per point)
	 */
	void figure(int figure, byte[] rgba, int width, int height, double scale);

	/** The figures were closed (e.g., plt.close()): the open figures. */
	default void figuresChanged(int[] open) {
	}
}
