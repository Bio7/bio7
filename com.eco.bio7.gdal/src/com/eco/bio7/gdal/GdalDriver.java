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

/**
 * A driver (format) of GDAL.
 *
 * @param shortName the name for the options (e.g., GTiff)
 * @param longName  the description (e.g., GeoTIFF)
 * @param raster    true if it reads or writes rasters
 * @param vector    true if it reads or writes vectors
 * @param create    true if it can create files (GDALCreate)
 * @param extensions the file extensions (e.g., "tif tiff"), may be empty
 */
public record GdalDriver(String shortName, String longName, boolean raster, boolean vector, boolean create, String extensions) {
}
