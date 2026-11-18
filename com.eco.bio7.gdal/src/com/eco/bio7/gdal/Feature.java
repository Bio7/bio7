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
package com.eco.bio7.gdal;

import java.util.Map;

/**
 * A feature of a vector layer: its id, attributes (Long, Double or String
 * values, null if not set) and geometry (WKT, null without geometry).
 */
public record Feature(long fid, Map<String, Object> attributes, String wkt) {

	/** The geometry with its coordinates (parsed WKT), null without geometry. */
	public Geometry geometry() {
		return wkt == null ? null : Geometry.parse(wkt);
	}

	/**
	 * The geometry as GeoJSON (2D), null without geometry. Created when called
	 * (not for every feature read).
	 */
	public String geoJson() {
		return wkt == null ? null : geometry().toGeoJson();
	}
}
