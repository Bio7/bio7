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
 * Creates a Java graphics device when R code calls the device function (see
 * RSession.registerDevice), e.g., for options(device = ...). Called in the
 * thread of R.
 */
@FunctionalInterface
public interface RDeviceFactory {

	/**
	 * The device and its resolution.
	 *
	 * @param device    the device (its width and height in pixels)
	 * @param dpi       the resolution (pixels per inch)
	 * @param pointsize the default font size
	 * @param history   the number of plots to keep for RSession.history (0: none)
	 */
	record DeviceSpec(RGraphicsDevice device, double dpi, double pointsize, int history) {

		/** A device without a plot history. */
		public DeviceSpec(RGraphicsDevice device, double dpi, double pointsize) {
			this(device, dpi, pointsize, 0);
		}
	}

	/**
	 * @param width     the requested width in inches (the arguments of the R
	 *                  function)
	 * @param height    the requested height in inches
	 * @param pointsize the requested font size
	 */
	DeviceSpec create(double width, double height, double pointsize) throws Exception;
}
