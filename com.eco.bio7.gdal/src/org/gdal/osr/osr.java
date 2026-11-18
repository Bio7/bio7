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
package org.gdal.osr;

/** The functions of OSR (compatible with the Java bindings of GDAL). */
public class osr {

	public static CoordinateTransformation CreateCoordinateTransformation(SpatialReference src, SpatialReference dst) {
		try {
			return new CoordinateTransformation(src, dst);
		} catch (RuntimeException e) {
			return null;
		}
	}
}
