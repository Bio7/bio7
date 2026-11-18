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

import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import com.eco.bio7.gdal.internal.GdalCompat;
import com.eco.bio7.gdal.internal.GdalNative;

/**
 * A coordinate system (compatible with the Java bindings of GDAL). The axis
 * order is the traditional one of GIS (x = longitude), as the code written for
 * GDAL 1 and 2 expects. Release it with delete().
 */
public class SpatialReference implements Cloneable {

	private MemorySegment handle;

	public SpatialReference() {
		this("");
	}

	/** A coordinate system of a WKT definition ("" an empty one). */
	public SpatialReference(String wkt) {
		GdalNative g = GdalCompat.n();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment h = (MemorySegment) g.OSRNewSpatialReference.invokeExact(GdalNative.cstringOrNull(a, wkt));
			if (GdalNative.isNull(h)) {
				/* An invalid definition: an empty coordinate system (as the bindings, which throw only on import) */
				h = (MemorySegment) g.OSRNewSpatialReference.invokeExact(MemorySegment.NULL);
			}
			init(h);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	private SpatialReference(MemorySegment h, boolean unused) {
		init(h);
	}

	private void init(MemorySegment h) {
		this.handle = h;
		GdalNative g = GdalCompat.n();
		try {
			if (g.OSRSetAxisMappingStrategy != null) {
				/* OAMS_TRADITIONAL_GIS_ORDER */
				g.OSRSetAxisMappingStrategy.invokeExact(h, 0);
			}
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	MemorySegment handle() {
		if (handle == null) {
			throw new IllegalStateException("The spatial reference was deleted");
		}
		return handle;
	}

	public synchronized void delete() {
		if (handle != null) {
			MemorySegment h = handle;
			handle = null;
			try {
				GdalCompat.n().OSRDestroySpatialReference.invokeExact(h);
			} catch (Throwable t) {
				throw GdalCompat.wrap(t);
			}
		}
	}

	public int ImportFromWkt(String wkt) {
		try (Arena a = Arena.ofConfined()) {
			/* OSRImportFromWkt takes char** (the pointer is advanced) */
			MemorySegment p = a.allocate(ADDRESS);
			p.set(ADDRESS, 0, GdalNative.cstring(a, wkt == null ? "" : wkt));
			return (int) GdalCompat.n().OSRImportFromWkt.invokeExact(handle(), p);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int ImportFromProj4(String proj4) {
		try (Arena a = Arena.ofConfined()) {
			return (int) GdalCompat.n().OSRImportFromProj4.invokeExact(handle(), GdalNative.cstring(a, proj4));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int ImportFromEPSG(int code) {
		try {
			return (int) GdalCompat.n().OSRImportFromEPSG.invokeExact(handle(), code);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int SetFromUserInput(String definition) {
		try (Arena a = Arena.ofConfined()) {
			return (int) GdalCompat.n().OSRSetFromUserInput.invokeExact(handle(), GdalNative.cstring(a, definition));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String ExportToWkt() {
		return export(0);
	}

	public int ExportToWkt(String[] out) {
		out[0] = ExportToWkt();
		return 0;
	}

	public String ExportToPrettyWkt() {
		return export(1);
	}

	public String ExportToProj4() {
		return export(2);
	}

	private String export(int kind) {
		GdalNative g = GdalCompat.n();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment out = a.allocate(ADDRESS);
			int unused = switch (kind) {
			case 0 -> (int) g.OSRExportToWkt.invokeExact(handle(), out);
			case 1 -> (int) g.OSRExportToPrettyWkt.invokeExact(handle(), out, 0);
			default -> (int) g.OSRExportToProj4.invokeExact(handle(), out);
			};
			MemorySegment s = out.get(ADDRESS, 0);
			String text = GdalNative.isNull(s) ? "" : s.reinterpret(Long.MAX_VALUE).getString(0);
			if (!GdalNative.isNull(s)) {
				g.VSIFree.invokeExact(s);
			}
			return text;
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int IsGeographic() {
		try {
			return (int) GdalCompat.n().OSRIsGeographic.invokeExact(handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int IsProjected() {
		try {
			return (int) GdalCompat.n().OSRIsProjected.invokeExact(handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int IsLocal() {
		try {
			return (int) GdalCompat.n().OSRIsLocal.invokeExact(handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int IsSame(SpatialReference other) {
		try {
			return (int) GdalCompat.n().OSRIsSame.invokeExact(handle(), other.handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String GetAttrValue(String name) {
		return GetAttrValue(name, 0);
	}

	/** A value of the WKT tree (e.g., "PROJCS|PROJECTION"), null if it doesn't exist. */
	public String GetAttrValue(String name, int child) {
		try (Arena a = Arena.ofConfined()) {
			return GdalNative.string((MemorySegment) GdalCompat.n().OSRGetAttrValue.invokeExact(handle(), GdalNative.cstring(a, name), child));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String GetAuthorityCode(String key) {
		try (Arena a = Arena.ofConfined()) {
			return GdalNative.string((MemorySegment) GdalCompat.n().OSRGetAuthorityCode.invokeExact(handle(),
					GdalNative.cstringOrNull(a, key)));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String GetAuthorityName(String key) {
		try (Arena a = Arena.ofConfined()) {
			return GdalNative.string((MemorySegment) GdalCompat.n().OSRGetAuthorityName.invokeExact(handle(),
					GdalNative.cstringOrNull(a, key)));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	@Override
	public SpatialReference clone() {
		return Clone();
	}

	public SpatialReference Clone() {
		try {
			return new SpatialReference((MemorySegment) GdalCompat.n().OSRClone.invokeExact(handle()), true);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	@Override
	public String toString() {
		return ExportToPrettyWkt();
	}
}
