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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import com.eco.bio7.gdal.internal.GdalNative;

/**
 * A coordinate reference system of GDAL (OGRSpatialReference). The axis order
 * is the traditional one of GIS (x = easting or longitude, y = northing or
 * latitude), also for EPSG:4326. Close it after use.
 */
public final class SpatialReference implements AutoCloseable {

	/* OAMS_TRADITIONAL_GIS_ORDER */
	private static final int TRADITIONAL_GIS_ORDER = 0;

	private MemorySegment handle;

	private SpatialReference(MemorySegment handle) {
		this.handle = handle;
		try {
			if (Gdal.native_().OSRSetAxisMappingStrategy != null) {
				Gdal.native_().OSRSetAxisMappingStrategy.invokeExact(handle, TRADITIONAL_GIS_ORDER);
			}
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	MemorySegment handle() {
		if (handle == null) {
			throw new GdalException("The spatial reference is closed");
		}
		return handle;
	}

	/** A coordinate system of the EPSG database (e.g., 4326 for WGS84 longitude/latitude). */
	public static SpatialReference fromEpsg(int code) {
		SpatialReference s = empty();
		try {
			int err = (int) Gdal.native_().OSRImportFromEPSG.invokeExact(s.handle, code);
			if (err != 0) {
				s.close();
				throw Gdal.error("Unknown EPSG code " + code + " (is the PROJ database found?)");
			}
			return s;
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** A coordinate system of a WKT definition. */
	public static SpatialReference fromWkt(String wkt) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment h = (MemorySegment) Gdal.native_().OSRNewSpatialReference.invokeExact(GdalNative.cstring(a, wkt));
			if (GdalNative.isNull(h)) {
				throw Gdal.error("Invalid WKT of a coordinate system");
			}
			return new SpatialReference(h);
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** A coordinate system of a definition: "EPSG:32632", WKT, a PROJ string, "WGS84", a file with a definition, ... */
	public static SpatialReference fromUserInput(String definition) {
		SpatialReference s = empty();
		try (Arena a = Arena.ofConfined()) {
			int err = (int) Gdal.native_().OSRSetFromUserInput.invokeExact(s.handle, GdalNative.cstring(a, definition));
			if (err != 0) {
				s.close();
				throw Gdal.error("Unknown coordinate system: " + definition);
			}
			return s;
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/* A copy of a reference owned by GDAL (e.g., of a layer) */
	static SpatialReference copyOf(MemorySegment owned) {
		try {
			return GdalNative.isNull(owned) ? null : new SpatialReference((MemorySegment) Gdal.native_().OSRClone.invokeExact(owned));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	private static SpatialReference empty() {
		try {
			return new SpatialReference((MemorySegment) Gdal.native_().OSRNewSpatialReference.invokeExact(MemorySegment.NULL));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public String toWkt() {
		return export(0);
	}

	public String toPrettyWkt() {
		return export(1);
	}

	public String toProj4() {
		return export(2);
	}

	private String export(int kind) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment out = a.allocate(ADDRESS);
			int err = switch (kind) {
			case 0 -> (int) Gdal.native_().OSRExportToWkt.invokeExact(handle(), out);
			case 1 -> (int) Gdal.native_().OSRExportToPrettyWkt.invokeExact(handle(), out, 0);
			default -> (int) Gdal.native_().OSRExportToProj4.invokeExact(handle(), out);
			};
			MemorySegment s = out.get(ADDRESS, 0);
			String text = GdalNative.isNull(s) ? "" : s.reinterpret(Long.MAX_VALUE).getString(0);
			Gdal.free(s);
			if (err != 0 && text.isEmpty()) {
				throw Gdal.error("The coordinate system could not be exported");
			}
			return text;
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The name (e.g., "WGS 84 / UTM zone 32N"). */
	public String name() {
		try {
			if (Gdal.native_().OSRGetName == null) {
				return "";
			}
			String s = GdalNative.string((MemorySegment) Gdal.native_().OSRGetName.invokeExact(handle()));
			return s == null ? "" : s;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The authority and code (e.g., "EPSG:32632"), null if unknown. */
	public String authority() {
		try {
			String name = GdalNative.string((MemorySegment) Gdal.native_().OSRGetAuthorityName.invokeExact(handle(), MemorySegment.NULL));
			String code = GdalNative.string((MemorySegment) Gdal.native_().OSRGetAuthorityCode.invokeExact(handle(), MemorySegment.NULL));
			return name == null || code == null ? null : name + ":" + code;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public boolean isGeographic() {
		try {
			return (int) Gdal.native_().OSRIsGeographic.invokeExact(handle()) != 0;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public boolean isProjected() {
		try {
			return (int) Gdal.native_().OSRIsProjected.invokeExact(handle()) != 0;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The transformation of coordinates of this system into the target system. Close it after use. */
	public CoordinateTransform transformTo(SpatialReference target) {
		try {
			MemorySegment ct = (MemorySegment) Gdal.native_().OCTNewCoordinateTransformation.invokeExact(handle(), target.handle());
			if (GdalNative.isNull(ct)) {
				throw Gdal.error("No transformation from " + this + " to " + target);
			}
			return new CoordinateTransform(ct);
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	@Override
	public synchronized void close() {
		if (handle != null) {
			MemorySegment h = handle;
			handle = null;
			try {
				Gdal.native_().OSRDestroySpatialReference.invokeExact(h);
			} catch (Throwable t) {
				throw Gdal.wrap(t);
			}
		}
	}

	@Override
	public String toString() {
		if (handle == null) {
			return "SpatialReference(closed)";
		}
		String a = authority();
		return "SpatialReference(" + (a != null ? a + " " : "") + name() + ")";
	}
}
