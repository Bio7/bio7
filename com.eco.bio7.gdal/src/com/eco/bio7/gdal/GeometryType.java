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

/** The geometry types of vector layers (OGRwkbGeometryType, 2D). */
public enum GeometryType {
	UNKNOWN(0), POINT(1), LINESTRING(2), POLYGON(3), MULTIPOINT(4), MULTILINESTRING(5), MULTIPOLYGON(6), GEOMETRYCOLLECTION(7), NONE(100);

	public final int code;

	GeometryType(int code) {
		this.code = code;
	}
}
