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

import com.eco.bio7.gdal.internal.GdalCompat;
import com.eco.bio7.gdal.internal.GdalNative;

/**
 * The functions of GDAL (compatible with the class gdal of the Java bindings
 * of GDAL), built on the FFM bridge com.eco.bio7.gdal: GDAL must be loaded
 * (com.eco.bio7.gdal.Gdal.load), else an UnsatisfiedLinkError is thrown.
 */
public class gdal {

	/** Nothing to do: the drivers are registered when GDAL is loaded. */
	public static void AllRegister() {
		GdalCompat.n();
	}

	public static String VersionInfo() {
		return VersionInfo("VERSION_NUM");
	}

	public static String VersionInfo(String request) {
		GdalNative g = GdalCompat.n();
		try (Arena a = Arena.ofConfined()) {
			return GdalNative.string((MemorySegment) g.GDALVersionInfo.invokeExact(GdalNative.cstring(a, request)));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static void SetConfigOption(String key, String value) {
		GdalNative g = GdalCompat.n();
		try (Arena a = Arena.ofConfined()) {
			g.CPLSetConfigOption.invokeExact(GdalNative.cstring(a, key), GdalNative.cstring(a, value));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	/** Errors are not printed (the quiet handler of GDAL); "CPLQuietErrorHandler" or any name. */
	public static int PushErrorHandler(String name) {
		return PushErrorHandler();
	}

	public static int PushErrorHandler() {
		GdalNative g = GdalCompat.n();
		try {
			if (g.CPLQuietErrorHandler != null) {
				g.CPLPushErrorHandler.invokeExact(g.CPLQuietErrorHandler);
			}
			return 0;
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static void PopErrorHandler() {
		GdalNative g = GdalCompat.n();
		try {
			if (g.CPLQuietErrorHandler != null) {
				g.CPLPopErrorHandler.invokeExact();
			}
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static int GetLastErrorNo() {
		try {
			return (int) GdalCompat.n().CPLGetLastErrorNo.invokeExact();
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static String GetLastErrorMsg() {
		try {
			return GdalNative.string((MemorySegment) GdalCompat.n().CPLGetLastErrorMsg.invokeExact());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static void ErrorReset() {
		try {
			GdalCompat.n().CPLErrorReset.invokeExact();
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	/** The size of a data type in bits. */
	public static int GetDataTypeSize(int type) {
		return com.eco.bio7.gdal.GdalDataType.of(type).bytes * 8;
	}

	public static int GetDriverCount() {
		try {
			return (int) GdalCompat.n().GDALGetDriverCount.invokeExact();
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static Driver GetDriver(int index) {
		try {
			MemorySegment d = (MemorySegment) GdalCompat.n().GDALGetDriver.invokeExact(index);
			return GdalNative.isNull(d) ? null : new Driver(d);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static Driver GetDriverByName(String name) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment d = (MemorySegment) GdalCompat.n().GDALGetDriverByName.invokeExact(GdalNative.cstring(a, name));
			return GdalNative.isNull(d) ? null : new Driver(d);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static Dataset Open(String path) {
		return Open(path, 0);
	}

	/** Opens a raster (GA_ReadOnly or GA_Update); null if it can't be opened (see GetLastErrorMsg). */
	public static Dataset Open(String path, int access) {
		GdalNative g = GdalCompat.n();
		/* GDAL_OF_RASTER (2), GDAL_OF_UPDATE (1) */
		int flags = 0x02 | (access == 1 ? 0x01 : 0);
		try (Arena a = Arena.ofConfined()) {
			g.CPLErrorReset.invokeExact();
			MemorySegment h = (MemorySegment) g.GDALOpenEx.invokeExact(GdalNative.cstring(a, path), flags, MemorySegment.NULL, MemorySegment.NULL,
					MemorySegment.NULL);
			return GdalNative.isNull(h) ? null : new Dataset(h);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public static int ReprojectImage(Dataset src, Dataset dst) {
		return ReprojectImage(src, dst, null, null, 0, 0.0, 0.0);
	}

	public static int ReprojectImage(Dataset src, Dataset dst, String srcWkt) {
		return ReprojectImage(src, dst, srcWkt, null, 0, 0.0, 0.0);
	}

	public static int ReprojectImage(Dataset src, Dataset dst, String srcWkt, String dstWkt) {
		return ReprojectImage(src, dst, srcWkt, dstWkt, 0, 0.0, 0.0);
	}

	public static int ReprojectImage(Dataset src, Dataset dst, String srcWkt, String dstWkt, int resampleAlg) {
		return ReprojectImage(src, dst, srcWkt, dstWkt, resampleAlg, 0.0, 0.0);
	}

	public static int ReprojectImage(Dataset src, Dataset dst, String srcWkt, String dstWkt, int resampleAlg, double warpMemoryLimit) {
		return ReprojectImage(src, dst, srcWkt, dstWkt, resampleAlg, warpMemoryLimit, 0.0);
	}

	/** Reprojects the source into the destination (GDALReprojectImage): CE_None (0) or an error. */
	public static int ReprojectImage(Dataset src, Dataset dst, String srcWkt, String dstWkt, int resampleAlg, double warpMemoryLimit, double maxError) {
		GdalNative g = GdalCompat.n();
		try (Arena a = Arena.ofConfined()) {
			return (int) g.GDALReprojectImage.invokeExact(src.handle(), GdalNative.cstring(a, srcWkt), dst.handle(), GdalNative.cstring(a, dstWkt),
					resampleAlg, warpMemoryLimit, maxError, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}
}
