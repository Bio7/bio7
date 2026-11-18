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
package org.gdal.gdal;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Hashtable;

import com.eco.bio7.gdal.internal.GdalCompat;
import com.eco.bio7.gdal.internal.GdalNative;

/** A dataset of GDAL (compatible with the Java bindings of GDAL). Release it with delete(). */
public class Dataset {

	private MemorySegment handle;

	Dataset(MemorySegment handle) {
		this.handle = handle;
	}

	MemorySegment handle() {
		if (handle == null) {
			throw new IllegalStateException("The dataset was deleted");
		}
		return handle;
	}

	/** Closes the dataset (GDALClose: written to its file). */
	public synchronized void delete() {
		if (handle != null) {
			MemorySegment h = handle;
			handle = null;
			try {
				GdalCompat.n().GDALClose.invokeExact(h);
			} catch (Throwable t) {
				throw GdalCompat.wrap(t);
			}
		}
	}

	public int getRasterXSize() {
		try {
			return (int) GdalCompat.n().GDALGetRasterXSize.invokeExact(handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int GetRasterXSize() {
		return getRasterXSize();
	}

	public int getRasterYSize() {
		try {
			return (int) GdalCompat.n().GDALGetRasterYSize.invokeExact(handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int GetRasterYSize() {
		return getRasterYSize();
	}

	public int getRasterCount() {
		try {
			return (int) GdalCompat.n().GDALGetRasterCount.invokeExact(handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int GetRasterCount() {
		return getRasterCount();
	}

	/** The band (1 based), null if it doesn't exist. */
	public Band GetRasterBand(int index) {
		try {
			MemorySegment b = (MemorySegment) GdalCompat.n().GDALGetRasterBand.invokeExact(handle(), index);
			return GdalNative.isNull(b) ? null : new Band(b, this);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	/** Fills the array (6 values) with the geotransform (the default 0,1,0,0,0,1 if none). */
	public void GetGeoTransform(double[] gt) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment s = a.allocate(JAVA_DOUBLE, 6);
			int unused = (int) GdalCompat.n().GDALGetGeoTransform.invokeExact(handle(), s);
			MemorySegment.copy(s, JAVA_DOUBLE, 0, gt, 0, Math.min(6, gt.length));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public double[] GetGeoTransform() {
		double[] gt = new double[6];
		GetGeoTransform(gt);
		return gt;
	}

	public int SetGeoTransform(double[] gt) {
		try (Arena a = Arena.ofConfined()) {
			return (int) GdalCompat.n().GDALSetGeoTransform.invokeExact(handle(), a.allocateFrom(JAVA_DOUBLE, gt));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String GetProjectionRef() {
		try {
			String s = GdalNative.string((MemorySegment) GdalCompat.n().GDALGetProjectionRef.invokeExact(handle()));
			return s == null ? "" : s;
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String GetProjection() {
		return GetProjectionRef();
	}

	public int SetProjection(String wkt) {
		try (Arena a = Arena.ofConfined()) {
			return (int) GdalCompat.n().GDALSetProjection.invokeExact(handle(), GdalNative.cstring(a, wkt == null ? "" : wkt));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public Driver GetDriver() {
		try {
			MemorySegment d = (MemorySegment) GdalCompat.n().GDALGetDatasetDriver.invokeExact(handle());
			return GdalNative.isNull(d) ? null : new Driver(d);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String GetDescription() {
		try {
			return GdalNative.string((MemorySegment) GdalCompat.n().GDALGetDescription.invokeExact(handle()));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String GetMetadataItem(String name) {
		return GetMetadataItem(name, null);
	}

	public String GetMetadataItem(String name, String domain) {
		try (Arena a = Arena.ofConfined()) {
			return GdalNative.string((MemorySegment) GdalCompat.n().GDALGetMetadataItem.invokeExact(handle(), GdalNative.cstring(a, name),
					GdalNative.cstringOrNull(a, domain)));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	/** The metadata of a domain ("" the default domain) as key -> value. */
	public Hashtable<String, String> GetMetadata_Dict(String domain) {
		Hashtable<String, String> out = new Hashtable<>();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment list = (MemorySegment) GdalCompat.n().GDALGetMetadata.invokeExact(handle(),
					GdalNative.cstringOrNull(a, domain));
			for (String s : GdalNative.strings(list)) {
				int i = s.indexOf('=');
				if (i > 0) {
					out.put(s.substring(0, i), s.substring(i + 1));
				}
			}
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
		return out;
	}

	public Hashtable<String, String> GetMetadata_Dict() {
		return GetMetadata_Dict("");
	}

	public void FlushCache() {
		try {
			GdalCompat.n().GDALFlushCache.invokeExact(handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}
}
