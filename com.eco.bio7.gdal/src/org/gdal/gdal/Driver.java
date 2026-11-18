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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Vector;

import com.eco.bio7.gdal.internal.GdalCompat;
import com.eco.bio7.gdal.internal.GdalNative;

/** A driver of GDAL (compatible with the Java bindings of GDAL). */
public class Driver {

	private final MemorySegment handle;

	Driver(MemorySegment handle) {
		this.handle = handle;
	}

	public String getShortName() {
		try {
			return GdalNative.string((MemorySegment) GdalCompat.n().GDALGetDriverShortName.invokeExact(handle));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String getLongName() {
		try {
			return GdalNative.string((MemorySegment) GdalCompat.n().GDALGetDriverLongName.invokeExact(handle));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public String GetDescription() {
		try {
			return GdalNative.string((MemorySegment) GdalCompat.n().GDALGetDescription.invokeExact(handle));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public Dataset Create(String name, int xsize, int ysize) {
		return Create(name, xsize, ysize, 1, 1, (String[]) null);
	}

	public Dataset Create(String name, int xsize, int ysize, int bands) {
		return Create(name, xsize, ysize, bands, 1, (String[]) null);
	}

	public Dataset Create(String name, int xsize, int ysize, int bands, int type) {
		return Create(name, xsize, ysize, bands, type, (String[]) null);
	}

	public Dataset Create(String name, int xsize, int ysize, int bands, String[] options) {
		return Create(name, xsize, ysize, bands, 1, options);
	}

	public Dataset Create(String name, int xsize, int ysize, int bands, int type, Vector<?> options) {
		return Create(name, xsize, ysize, bands, type, strings(options));
	}

	/** A new dataset (e.g., the driver MEM for a raster in memory); null if it can't be created. */
	public Dataset Create(String name, int xsize, int ysize, int bands, int type, String[] options) {
		GdalNative g = GdalCompat.n();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment h = (MemorySegment) g.GDALCreate.invokeExact(handle, GdalNative.cstring(a, name), xsize, ysize, bands, type,
					GdalNative.stringList(a, options == null ? null : Arrays.asList(options)));
			return GdalNative.isNull(h) ? null : new Dataset(h);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public Dataset CreateCopy(String name, Dataset src) {
		return CreateCopy(name, src, 0, (String[]) null);
	}

	public Dataset CreateCopy(String name, Dataset src, int strict) {
		return CreateCopy(name, src, strict, (String[]) null);
	}

	public Dataset CreateCopy(String name, Dataset src, String[] options) {
		return CreateCopy(name, src, 0, options);
	}

	public Dataset CreateCopy(String name, Dataset src, Vector<?> options) {
		return CreateCopy(name, src, 0, strings(options));
	}

	public Dataset CreateCopy(String name, Dataset src, int strict, Vector<?> options) {
		return CreateCopy(name, src, strict, strings(options));
	}

	/** A copy of a dataset (e.g., the driver VRT with the name "" for a virtual copy); null if it fails. */
	public Dataset CreateCopy(String name, Dataset src, int strict, String[] options) {
		GdalNative g = GdalCompat.n();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment h = (MemorySegment) g.GDALCreateCopy.invokeExact(handle, GdalNative.cstring(a, name), src.handle(), strict,
					GdalNative.stringList(a, options == null ? null : Arrays.asList(options)), MemorySegment.NULL, MemorySegment.NULL);
			return GdalNative.isNull(h) ? null : new Dataset(h);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	private static String[] strings(Vector<?> v) {
		return v == null ? null : v.stream().map(String::valueOf).toArray(String[]::new);
	}
}
