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
package org.gdal.ogr;

/** OGR (compatible with the Java bindings of GDAL): the drivers are registered when GDAL is loaded. */
public class ogr {

	/** Nothing to do: all drivers (also of OGR) are registered when GDAL is loaded. */
	public static void RegisterAll() {
		com.eco.bio7.gdal.internal.GdalCompat.n();
	}
}
