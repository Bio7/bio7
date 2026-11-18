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

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.eco.bio7.gdal.internal.GdalNative;

/**
 * A vector layer of a dataset (OGRLayer): its fields, features, extent and
 * coordinate system; filters (attributes, extent); new fields and features
 * (a layer of a dataset created or opened for writing). The result of SQL
 * ({@link GdalDataset#executeSql}) must be closed.
 */
public final class VectorLayer implements AutoCloseable {

	/* OGRFieldType */
	private static final int OFT_INTEGER = 0, OFT_REAL = 2, OFT_INTEGER64 = 12;

	private final GdalDataset dataset;
	private MemorySegment handle;
	/* The result of SQL (released by close) */
	private final boolean sqlResult;

	VectorLayer(GdalDataset dataset, MemorySegment handle) {
		this(dataset, handle, false);
	}

	VectorLayer(GdalDataset dataset, MemorySegment handle, boolean sqlResult) {
		this.dataset = dataset;
		this.handle = handle;
		this.sqlResult = sqlResult;
	}

	MemorySegment h() {
		dataset.handle();
		if (handle == null) {
			throw new GdalException("The layer is closed");
		}
		return handle;
	}

	/** Releases the result of SQL (nothing to do for the other layers: they belong to their dataset). */
	@Override
	public synchronized void close() {
		if (sqlResult && handle != null && !dataset.isClosed()) {
			MemorySegment h = handle;
			handle = null;
			try {
				Gdal.native_().GDALDatasetReleaseResultSet.invokeExact(dataset.handle(), h);
			} catch (Throwable t) {
				throw Gdal.wrap(t);
			}
		}
	}

	/* ---------------- Filters ---------------- */

	/**
	 * Only the features of an attribute filter (an SQL WHERE clause, e.g. "area >
	 * 1000 AND type = 'forest'"); null: all. Some drivers (e.g., GeoPackage) check
	 * the filter only when the features are read (an invalid filter: no features).
	 */
	public void setAttributeFilter(String where) {
		try (Arena a = Arena.ofConfined()) {
			int err = (int) Gdal.native_().OGR_L_SetAttributeFilter.invokeExact(h(), GdalNative.cstringOrNull(a, where));
			if (err != 0) {
				throw Gdal.error("Invalid attribute filter: " + where);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** Only the features which intersect the rectangle (in the coordinates of the layer). */
	public void setSpatialFilter(double minX, double minY, double maxX, double maxY) {
		try {
			Gdal.native_().OGR_L_SetSpatialFilterRect.invokeExact(h(), minX, minY, maxX, maxY);
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** All features again (no spatial filter). */
	public void clearSpatialFilter() {
		try {
			Gdal.native_().OGR_L_SetSpatialFilter.invokeExact(h(), MemorySegment.NULL);
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/* ---------------- Writing ---------------- */

	/** A new attribute field (before the features are added). */
	public void createField(String name, FieldType type) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment fld = (MemorySegment) Gdal.native_().OGR_Fld_Create.invokeExact(GdalNative.cstring(a, name), type.code);
			try {
				int err = (int) Gdal.native_().OGR_L_CreateField.invokeExact(h(), fld, 1);
				if (err != 0) {
					throw Gdal.error("The field could not be created: " + name);
				}
			} finally {
				Gdal.native_().OGR_Fld_Destroy.invokeExact(fld);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/**
	 * Adds a feature: the attributes by field name (numbers, strings, booleans or
	 * null; unknown names are an error), the geometry as WKT (in the coordinates of
	 * the layer; null: without geometry).
	 *
	 * @return the id of the new feature
	 */
	public long addFeature(java.util.Map<String, ?> attributes, String wkt) {
		GdalNative g = Gdal.native_();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment defn = (MemorySegment) g.OGR_L_GetLayerDefn.invokeExact(h());
			MemorySegment f = (MemorySegment) g.OGR_F_Create.invokeExact(defn);
			try {
				if (attributes != null) {
					for (java.util.Map.Entry<String, ?> e : attributes.entrySet()) {
						int i = (int) g.OGR_FD_GetFieldIndex.invokeExact(defn, GdalNative.cstring(a, e.getKey()));
						if (i < 0) {
							throw new GdalException("The layer " + name() + " has no field " + e.getKey());
						}
						Object v = e.getValue();
						if (v == null) {
							if (g.OGR_F_SetFieldNull != null) {
								g.OGR_F_SetFieldNull.invokeExact(f, i);
							}
						} else if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) {
							g.OGR_F_SetFieldInteger64.invokeExact(f, i, ((Number) v).longValue());
						} else if (v instanceof Number n) {
							g.OGR_F_SetFieldDouble.invokeExact(f, i, n.doubleValue());
						} else if (v instanceof Boolean b) {
							g.OGR_F_SetFieldInteger64.invokeExact(f, i, b ? 1L : 0L);
						} else {
							g.OGR_F_SetFieldString.invokeExact(f, i, GdalNative.cstring(a, String.valueOf(v)));
						}
					}
				}
				if (wkt != null) {
					MemorySegment text = a.allocate(ADDRESS);
					text.set(ADDRESS, 0, GdalNative.cstring(a, wkt));
					MemorySegment out = a.allocate(ADDRESS);
					int err = (int) g.OGR_G_CreateFromWkt.invokeExact(text, MemorySegment.NULL, out);
					MemorySegment geom = out.get(ADDRESS, 0);
					if (err != 0 || GdalNative.isNull(geom)) {
						throw new GdalException("Invalid WKT: " + (wkt.length() > 80 ? wkt.substring(0, 80) + "..." : wkt));
					}
					/* The feature owns the geometry */
					int unused = (int) g.OGR_F_SetGeometryDirectly.invokeExact(f, geom);
				}
				g.CPLErrorReset.invokeExact();
				int err = (int) g.OGR_L_CreateFeature.invokeExact(h(), f);
				if (err != 0) {
					throw Gdal.error("The feature could not be added to " + name());
				}
				return (long) g.OGR_F_GetFID.invokeExact(f);
			} finally {
				g.OGR_F_Destroy.invokeExact(f);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** Writes the changes to the file. */
	public void sync() {
		try {
			int unused = (int) Gdal.native_().OGR_L_SyncToDisk.invokeExact(h());
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The index of a field (-1 if it doesn't exist). */
	public int fieldIndex(String name) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment defn = (MemorySegment) Gdal.native_().OGR_L_GetLayerDefn.invokeExact(h());
			return (int) Gdal.native_().OGR_FD_GetFieldIndex.invokeExact(defn, GdalNative.cstring(a, name));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public String name() {
		try {
			return GdalNative.string((MemorySegment) Gdal.native_().OGR_L_GetName.invokeExact(h()));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The number of features (counted if the format doesn't know it). */
	public long featureCount() {
		try {
			return (long) Gdal.native_().OGR_L_GetFeatureCount.invokeExact(h(), 1);
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The names of the attribute fields. */
	public List<String> fieldNames() {
		List<String> out = new ArrayList<>();
		try {
			MemorySegment defn = (MemorySegment) Gdal.native_().OGR_L_GetLayerDefn.invokeExact(h());
			int n = (int) Gdal.native_().OGR_FD_GetFieldCount.invokeExact(defn);
			for (int i = 0; i < n; i++) {
				MemorySegment f = (MemorySegment) Gdal.native_().OGR_FD_GetFieldDefn.invokeExact(defn, i);
				out.add(GdalNative.string((MemorySegment) Gdal.native_().OGR_Fld_GetNameRef.invokeExact(f)));
			}
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
		return out;
	}

	/** The type of the geometries (OGRwkbGeometryType: 1 point, 2 line string, 3 polygon, 4-6 multi..., 0 unknown). */
	public int geometryType() {
		try {
			return (int) Gdal.native_().OGR_L_GetGeomType.invokeExact(h());
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The coordinate system (null if unknown). Close it after use. */
	public SpatialReference spatialReference() {
		try {
			return SpatialReference.copyOf((MemorySegment) Gdal.native_().OGR_L_GetSpatialRef.invokeExact(h()));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The extent {minX, minY, maxX, maxY} (null if unknown). */
	public double[] extent() {
		try (Arena a = Arena.ofConfined()) {
			/* OGREnvelope: MinX, MaxX, MinY, MaxY */
			MemorySegment e = a.allocate(JAVA_DOUBLE, 4);
			int err = (int) Gdal.native_().OGR_L_GetExtent.invokeExact(h(), e, 1);
			if (err != 0) {
				return null;
			}
			double[] v = e.toArray(JAVA_DOUBLE);
			return new double[] { v[0], v[2], v[1], v[3] };
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** All features (for large layers see {@link #forEach(Consumer)}). */
	public List<Feature> features() {
		List<Feature> out = new ArrayList<>();
		forEach(out::add);
		return out;
	}

	/** Reads the features one after the other (from the start of the layer). */
	public synchronized void forEach(Consumer<Feature> action) {
		try {
			MemorySegment defn = (MemorySegment) Gdal.native_().OGR_L_GetLayerDefn.invokeExact(h());
			int n = (int) Gdal.native_().OGR_FD_GetFieldCount.invokeExact(defn);
			String[] names = new String[n];
			int[] types = new int[n];
			for (int i = 0; i < n; i++) {
				MemorySegment f = (MemorySegment) Gdal.native_().OGR_FD_GetFieldDefn.invokeExact(defn, i);
				names[i] = GdalNative.string((MemorySegment) Gdal.native_().OGR_Fld_GetNameRef.invokeExact(f));
				types[i] = (int) Gdal.native_().OGR_Fld_GetType.invokeExact(f);
			}
			Gdal.native_().OGR_L_ResetReading.invokeExact(h());
			while (true) {
				MemorySegment f = (MemorySegment) Gdal.native_().OGR_L_GetNextFeature.invokeExact(h());
				if (GdalNative.isNull(f)) {
					break;
				}
				Feature feature;
				try {
					feature = read(f, names, types);
				} finally {
					Gdal.native_().OGR_F_Destroy.invokeExact(f);
				}
				action.accept(feature);
			}
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	private static Feature read(MemorySegment f, String[] names, int[] types) throws Throwable {
		Map<String, Object> attributes = new LinkedHashMap<>();
		for (int i = 0; i < names.length; i++) {
			Object v = null;
			if ((int) Gdal.native_().OGR_F_IsFieldSetAndNotNull.invokeExact(f, i) != 0) {
				v = switch (types[i]) {
				case OFT_INTEGER, OFT_INTEGER64 -> (long) Gdal.native_().OGR_F_GetFieldAsInteger64.invokeExact(f, i);
				case OFT_REAL -> (double) Gdal.native_().OGR_F_GetFieldAsDouble.invokeExact(f, i);
				default -> GdalNative.string((MemorySegment) Gdal.native_().OGR_F_GetFieldAsString.invokeExact(f, i));
				};
			}
			attributes.put(names[i], v);
		}
		long fid = (long) Gdal.native_().OGR_F_GetFID.invokeExact(f);
		MemorySegment g = (MemorySegment) Gdal.native_().OGR_F_GetGeometryRef.invokeExact(f);
		String wkt = null;
		if (!GdalNative.isNull(g)) {
			try (Arena a = Arena.ofConfined()) {
				MemorySegment out = a.allocate(ADDRESS);
				int err = (int) Gdal.native_().OGR_G_ExportToWkt.invokeExact(g, out);
				MemorySegment s = out.get(ADDRESS, 0);
				if (err == 0 && !GdalNative.isNull(s)) {
					wkt = s.reinterpret(Long.MAX_VALUE).getString(0);
				}
				Gdal.free(s);
			}
			/* No GeoJSON here: OGR_G_ExportToJson of GDAL 3.13 doesn't free its memory (Feature.geoJson() creates it in Java) */
		}
		return new Feature(fid, attributes, wkt);
	}

	@Override
	public String toString() {
		return "VectorLayer(" + name() + ", " + featureCount() + " features)";
	}
}
