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

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eco.bio7.gdal.internal.GdalNative;

/**
 * A dataset of GDAL: a raster (bands of pixels with georeferencing) and/or
 * vector layers. Close it after use (try-with-resources).
 */
public final class GdalDataset implements AutoCloseable {

	private MemorySegment handle;
	private final String path;

	GdalDataset(MemorySegment handle, String path) {
		this.handle = handle;
		this.path = path;
	}

	MemorySegment handle() {
		if (handle == null) {
			throw new GdalException("The dataset is closed: " + path);
		}
		return handle;
	}

	/** The path (or name) the dataset was opened or created with. */
	public String path() {
		return path;
	}

	/* ---------------- Raster ---------------- */

	public int width() {
		try {
			return (int) Gdal.native_().GDALGetRasterXSize.invokeExact(handle());
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public int height() {
		try {
			return (int) Gdal.native_().GDALGetRasterYSize.invokeExact(handle());
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public int bandCount() {
		try {
			return (int) Gdal.native_().GDALGetRasterCount.invokeExact(handle());
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The band (1 to {@link #bandCount()}). */
	public GdalBand band(int index) {
		if (index < 1 || index > bandCount()) {
			throw new GdalException("No band " + index + " (bands: " + bandCount() + ")");
		}
		try {
			MemorySegment b = (MemorySegment) Gdal.native_().GDALGetRasterBand.invokeExact(handle(), index);
			return new GdalBand(this, b, index);
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/**
	 * The georeferencing (affine transformation): x = gt[0] + col * gt[1] + row *
	 * gt[2], y = gt[3] + col * gt[4] + row * gt[5]; null if the dataset has none.
	 */
	public double[] geoTransform() {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment gt = a.allocate(JAVA_DOUBLE, 6);
			int err = (int) Gdal.native_().GDALGetGeoTransform.invokeExact(handle(), gt);
			return err == 0 ? gt.toArray(JAVA_DOUBLE) : null;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public void setGeoTransform(double[] gt) {
		if (gt == null || gt.length != 6) {
			throw new GdalException("A geotransform has 6 values");
		}
		try (Arena a = Arena.ofConfined()) {
			int err = (int) Gdal.native_().GDALSetGeoTransform.invokeExact(handle(), a.allocateFrom(JAVA_DOUBLE, gt));
			if (err != 0) {
				throw Gdal.error("The geotransform could not be set");
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The coordinate system as WKT ("" if none). */
	public String projection() {
		try {
			String s = GdalNative.string((MemorySegment) Gdal.native_().GDALGetProjectionRef.invokeExact(handle()));
			return s == null ? "" : s;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** Sets the coordinate system (WKT, or "EPSG:4326", a PROJ string, ...). */
	public void setProjection(String crs) {
		String wkt = crs;
		if (crs != null && !crs.isBlank() && !crs.stripLeading().matches("(?s)^[A-Z_]+\\[.*")) {
			try (SpatialReference srs = SpatialReference.fromUserInput(crs)) {
				wkt = srs.toWkt();
			}
		}
		try (Arena a = Arena.ofConfined()) {
			int err = (int) Gdal.native_().GDALSetProjection.invokeExact(handle(), GdalNative.cstring(a, wkt == null ? "" : wkt));
			if (err != 0) {
				throw Gdal.error("The projection could not be set");
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The coordinate system (null if the dataset has none). Close it after use. */
	public SpatialReference spatialReference() {
		String wkt = projection();
		return wkt.isBlank() ? null : SpatialReference.fromWkt(wkt);
	}

	/** The extent {minX, minY, maxX, maxY} in the coordinates of the dataset (null without georeferencing). */
	public double[] bounds() {
		double[] gt = geoTransform();
		if (gt == null) {
			return null;
		}
		int w = width(), h = height();
		double[] xs = { gt[0], gt[0] + w * gt[1], gt[0] + h * gt[2], gt[0] + w * gt[1] + h * gt[2] };
		double[] ys = { gt[3], gt[3] + w * gt[4], gt[3] + h * gt[5], gt[3] + w * gt[4] + h * gt[5] };
		return new double[] { Arrays.stream(xs).min().getAsDouble(), Arrays.stream(ys).min().getAsDouble(), Arrays.stream(xs).max().getAsDouble(),
				Arrays.stream(ys).max().getAsDouble() };
	}

	/** The coordinates {x, y} of a pixel position (column, row; 0.5 is the center of the first pixel). */
	public double[] pixelToWorld(double column, double row) {
		double[] gt = requireGeoTransform();
		return new double[] { gt[0] + column * gt[1] + row * gt[2], gt[3] + column * gt[4] + row * gt[5] };
	}

	/** The pixel position {column, row} of coordinates of the dataset. */
	public double[] worldToPixel(double x, double y) {
		double[] gt = requireGeoTransform();
		double det = gt[1] * gt[5] - gt[2] * gt[4];
		if (det == 0) {
			throw new GdalException("The geotransform can't be inverted");
		}
		double dx = x - gt[0], dy = y - gt[3];
		return new double[] { (gt[5] * dx - gt[2] * dy) / det, (-gt[4] * dx + gt[1] * dy) / det };
	}

	private double[] requireGeoTransform() {
		double[] gt = geoTransform();
		if (gt == null) {
			throw new GdalException("The dataset has no georeferencing: " + path);
		}
		return gt;
	}

	/* ---------------- Information ---------------- */

	/** The short name of the driver (e.g., "GTiff"). */
	public String driverName() {
		try {
			MemorySegment d = (MemorySegment) Gdal.native_().GDALGetDatasetDriver.invokeExact(handle());
			return GdalNative.isNull(d) ? "" : GdalNative.string((MemorySegment) Gdal.native_().GDALGetDriverShortName.invokeExact(d));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The metadata of a domain (null or "" for the default domain), e.g. the tags of a GeoTIFF. */
	public Map<String, String> metadata(String domain) {
		Map<String, String> out = new LinkedHashMap<>();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment list = (MemorySegment) Gdal.native_().GDALGetMetadata.invokeExact(handle(),
					GdalNative.cstringOrNull(a, domain));
			for (String s : GdalNative.strings(list)) {
				int i = s.indexOf('=');
				if (i > 0) {
					out.put(s.substring(0, i), s.substring(i + 1));
				}
			}
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
		return out;
	}

	/** The files of the dataset (e.g., a shapefile: .shp, .shx, .dbf, .prj). */
	public List<String> files() {
		try {
			MemorySegment list = (MemorySegment) Gdal.native_().GDALGetFileList.invokeExact(handle());
			List<String> out = GdalNative.strings(list);
			if (!GdalNative.isNull(list)) {
				Gdal.native_().CSLDestroy.invokeExact(list);
			}
			return out;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The output of gdalinfo for this dataset (options of gdalinfo, e.g. "-stats", "-json"). */
	public String info(String... options) {
		GdalNative g = Gdal.native_();
		if (g.GDALInfo == null) {
			throw new GdalException("gdalinfo needs GDAL 2.1 or later");
		}
		try (Arena a = Arena.ofConfined()) {
			MemorySegment opts = (MemorySegment) g.GDALInfoOptionsNew.invokeExact(GdalNative.stringList(a, Arrays.asList(options)), MemorySegment.NULL);
			try {
				MemorySegment text = (MemorySegment) g.GDALInfo.invokeExact(handle(), opts);
				String s = GdalNative.string(text);
				Gdal.free(text);
				return s == null ? "" : s;
			} finally {
				if (!GdalNative.isNull(opts)) {
					g.GDALInfoOptionsFree.invokeExact(opts);
				}
			}
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/* ---------------- Vector ---------------- */

	public int layerCount() {
		try {
			return (int) Gdal.native_().GDALDatasetGetLayerCount.invokeExact(handle());
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The vector layer (0 to {@link #layerCount()} - 1). */
	public VectorLayer layer(int index) {
		if (index < 0 || index >= layerCount()) {
			throw new GdalException("No layer " + index + " (layers: " + layerCount() + ")");
		}
		try {
			return new VectorLayer(this, (MemorySegment) Gdal.native_().GDALDatasetGetLayer.invokeExact(handle(), index));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/**
	 * A new vector layer (a dataset of a vector driver, see
	 * {@link Gdal#createVector}): its coordinate system (or null), the type of
	 * the geometries and the options of the driver (e.g., "GEOMETRY_NAME=geom").
	 */
	public VectorLayer createLayer(String name, SpatialReference srs, GeometryType type, String... options) {
		try (Arena a = Arena.ofConfined()) {
			Gdal.native_().CPLErrorReset.invokeExact();
			/* A local variable: a conditional expression as argument of invokeExact has the type Object */
			MemorySegment srsHandle = srs == null ? MemorySegment.NULL : srs.handle();
			MemorySegment l = (MemorySegment) Gdal.native_().GDALDatasetCreateLayer.invokeExact(handle(), GdalNative.cstring(a, name), srsHandle,
					type.code, GdalNative.stringList(a, java.util.Arrays.asList(options)));
			if (GdalNative.isNull(l)) {
				throw Gdal.error("The layer could not be created: " + name);
			}
			return new VectorLayer(this, l);
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** Executes SQL (the dialect of the driver, e.g. of GeoPackage or SQLite). The result is a layer: close it after use. */
	public VectorLayer executeSql(String sql) {
		return executeSql(sql, null);
	}

	/**
	 * Executes SQL in a dialect ("OGRSQL", "SQLITE", "INDIRECT_SQLITE" or null
	 * for the dialect of the driver). The result is a layer (null for statements
	 * without result): close it after use.
	 */
	public VectorLayer executeSql(String sql, String dialect) {
		try (Arena a = Arena.ofConfined()) {
			Gdal.native_().CPLErrorReset.invokeExact();
			MemorySegment l = (MemorySegment) Gdal.native_().GDALDatasetExecuteSQL.invokeExact(handle(), GdalNative.cstring(a, sql), MemorySegment.NULL,
					GdalNative.cstringOrNull(a, dialect));
			if (GdalNative.isNull(l)) {
				int err = (int) Gdal.native_().CPLGetLastErrorNo.invokeExact();
				if (err != 0) {
					throw Gdal.error("SQL failed: " + sql);
				}
				return null;
			}
			return new VectorLayer(this, l, true);
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public List<VectorLayer> layers() {
		List<VectorLayer> out = new ArrayList<>();
		for (int i = 0; i < layerCount(); i++) {
			out.add(layer(i));
		}
		return out;
	}

	/* ---------------- Closing ---------------- */

	/** Writes the cached data (of a created or updated dataset). */
	public void flush() {
		try {
			Gdal.native_().GDALFlushCache.invokeExact(handle());
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public boolean isClosed() {
		return handle == null;
	}

	@Override
	public synchronized void close() {
		if (handle != null) {
			MemorySegment h = handle;
			handle = null;
			try {
				Gdal.native_().GDALClose.invokeExact(h);
			} catch (Throwable t) {
				throw Gdal.wrap(t);
			}
		}
	}

	@Override
	public String toString() {
		return handle == null ? "GdalDataset(closed: " + path + ")"
				: "GdalDataset(" + path + ", " + driverName() + ", " + width() + " x " + height() + ", bands " + bandCount() + ", layers " + layerCount() + ")";
	}
}
