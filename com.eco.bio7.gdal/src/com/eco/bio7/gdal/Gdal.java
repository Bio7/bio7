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
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.eco.bio7.gdal.internal.GdalNative;

/**
 * GDAL for Java: the GDAL library of an installation (conda, Homebrew, the
 * packages of Linux, QGIS, OSGeo4W) called with the Foreign Function and Memory
 * API (Java 22+; no JNI library, no compiled code).
 *
 * <pre>
 * Gdal.load();                                   // the installed GDAL (or Gdal.load(path))
 * try (GdalDataset ds = Gdal.open("dem.tif")) {
 *     float[] heights = ds.band(1).readFloats(); // row by row
 *     double[] gt = ds.geoTransform();           // the georeferencing
 *     String crs = ds.spatialReference().authority();  // e.g. "EPSG:32632"
 * }
 * Gdal.warp("dem.tif", "dem_wgs84.tif", "-t_srs", "EPSG:4326");   // the command line tools
 * Gdal.translate("dem.tif", "dem.png", "-of", "PNG", "-scale");
 * Gdal.vectorTranslate("roads.shp", "roads.gpkg", "-f", "GPKG");
 * </pre>
 *
 * All errors of GDAL are {@link GdalException}s (with the message of GDAL).
 * Datasets, spatial references and transformations are {@link AutoCloseable}.
 */
public final class Gdal {

	/** The flags of GDALOpenEx. */
	static final int OF_UPDATE = 0x01, OF_RASTER = 0x02, OF_VECTOR = 0x04, OF_VERBOSE_ERROR = 0x40;

	private static volatile GdalNative n;
	private static String version = "";

	private Gdal() {
	}

	/* ---------------- Loading ---------------- */

	/** True if GDAL is loaded in this process. */
	public static boolean isLoaded() {
		return n != null;
	}

	/** The loaded library (null if GDAL is not loaded). */
	public static Path library() {
		GdalNative g = n;
		return g == null ? null : g.library;
	}

	/**
	 * Loads the GDAL library which is found first ({@link GdalLibraryFinder}).
	 *
	 * @return the version (e.g., "3.10.2")
	 */
	public static String load() {
		Path lib = GdalLibraryFinder.find().orElseThrow(() -> new GdalException(
				"No GDAL library was found. Please install GDAL (e.g., conda install -c conda-forge gdal, brew install gdal, apt install gdal-bin, OSGeo4W) or select the library."));
		return load(lib);
	}

	/**
	 * Loads the GDAL library (a file, or a folder with the library), sets the data
	 * folders of GDAL and PROJ of its installation and registers all drivers.
	 * Only one library can be loaded in a process.
	 *
	 * @return the version (e.g., "3.10.2")
	 */
	public static synchronized String load(Path library) {
		Path lib = GdalLibraryFinder.resolve(library);
		if (lib == null) {
			throw new GdalException("No GDAL library at " + library);
		}
		if (n != null) {
			if (n.library.equals(lib.toAbsolutePath().normalize())) {
				return version;
			}
			throw new GdalException("GDAL is already loaded from " + n.library + " (a restart is needed to use another library)");
		}
		GdalNative g;
		try {
			g = new GdalNative(lib.toAbsolutePath().normalize());
		} catch (IllegalArgumentException | UnsatisfiedLinkError e) {
			String reason = GdalLibraryFinder.loadError(lib);
			throw new GdalException("The GDAL library could not be loaded: " + lib + "\n" + (reason != null ? reason : e.getMessage()), e);
		}
		try (Arena a = Arena.ofConfined()) {
			/* The data of the installation (if not set by the environment) */
			Path data = GdalLibraryFinder.gdalData(lib);
			if (data != null && System.getenv("GDAL_DATA") == null) {
				g.CPLSetConfigOption.invokeExact(GdalNative.cstring(a, "GDAL_DATA"), GdalNative.cstring(a, data.toString()));
			}
			Path proj = GdalLibraryFinder.projData(lib);
			if (proj != null && System.getenv("PROJ_DATA") == null && System.getenv("PROJ_LIB") == null) {
				if (g.OSRSetPROJSearchPaths != null) {
					/*
					 * The search paths replace the ones of PROJ: also the folder of the user
					 * (datum grids downloaded there, e.g., us_noaa_conus.tif for NAD27), first
					 */
					List<String> paths = new java.util.ArrayList<>();
					Path user = GdalLibraryFinder.userProjData();
					if (java.nio.file.Files.isDirectory(user)) {
						paths.add(user.toString());
					}
					paths.add(proj.toString());
					g.OSRSetPROJSearchPaths.invokeExact(GdalNative.stringList(a, paths));
				} else {
					g.CPLSetConfigOption.invokeExact(GdalNative.cstring(a, "PROJ_LIB"), GdalNative.cstring(a, proj.toString()));
				}
			}
			/* The errors are exceptions (not printed by GDAL) */
			if (g.CPLQuietErrorHandler != null) {
				MemorySegment unused = (MemorySegment) g.CPLSetErrorHandler.invokeExact(g.CPLQuietErrorHandler);
			}
			g.GDALAllRegister.invokeExact();
			version = GdalNative.string((MemorySegment) g.GDALVersionInfo.invokeExact(GdalNative.cstring(a, "RELEASE_NAME")));
		} catch (Throwable t) {
			throw new GdalException("GDAL could not be initialized: " + t, t);
		}
		n = g;
		GdalNative.loaded = g;
		return version;
	}

	/** The version of the loaded GDAL (e.g., "3.10.2"). */
	public static String version() {
		native_();
		return version;
	}

	/** A value of GDALVersionInfo (e.g., "--version", "BUILD_INFO", "LICENSE"). */
	public static String versionInfo(String request) {
		GdalNative g = native_();
		try (Arena a = Arena.ofConfined()) {
			return GdalNative.string((MemorySegment) g.GDALVersionInfo.invokeExact(GdalNative.cstring(a, request)));
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/** Sets a configuration option of GDAL (CPLSetConfigOption, e.g., GDAL_NUM_THREADS). */
	public static void setConfigOption(String key, String value) {
		GdalNative g = native_();
		try (Arena a = Arena.ofConfined()) {
			g.CPLSetConfigOption.invokeExact(GdalNative.cstring(a, key), GdalNative.cstring(a, value));
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/* ---------------- Datasets ---------------- */

	/** Opens a raster or vector dataset for reading (a file, a folder, a URL with /vsicurl/, ...). */
	public static GdalDataset open(String path) {
		return open(path, OF_RASTER | OF_VECTOR);
	}

	public static GdalDataset open(Path path) {
		return open(path.toString());
	}

	/** Opens a dataset for reading and writing. */
	public static GdalDataset openForUpdate(String path) {
		return open(path, OF_RASTER | OF_VECTOR | OF_UPDATE);
	}

	static GdalDataset open(String path, int flags) {
		GdalNative g = native_();
		try (Arena a = Arena.ofConfined()) {
			g.CPLErrorReset.invokeExact();
			MemorySegment h = (MemorySegment) g.GDALOpenEx.invokeExact(GdalNative.cstring(a, path), flags | OF_VERBOSE_ERROR, MemorySegment.NULL,
					MemorySegment.NULL, MemorySegment.NULL);
			if (GdalNative.isNull(h)) {
				throw error("The file could not be opened: " + path);
			}
			return new GdalDataset(h, path);
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/**
	 * Creates a raster dataset (e.g., driver "GTiff"; options of the driver, e.g.
	 * "COMPRESS=DEFLATE"). Write the bands and close it (or {@link GdalDataset#flush()}).
	 */
	public static GdalDataset create(String path, String driver, int width, int height, int bands, GdalDataType type, String... options) {
		GdalNative g = native_();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment drv = (MemorySegment) g.GDALGetDriverByName.invokeExact(GdalNative.cstring(a, driver));
			if (GdalNative.isNull(drv)) {
				throw new GdalException("Unknown GDAL driver: " + driver);
			}
			g.CPLErrorReset.invokeExact();
			MemorySegment h = (MemorySegment) g.GDALCreate.invokeExact(drv, GdalNative.cstring(a, path), width, height, bands, type.code,
					GdalNative.stringList(a, Arrays.asList(options)));
			if (GdalNative.isNull(h)) {
				throw error("The dataset could not be created: " + path + " (" + driver + ")");
			}
			return new GdalDataset(h, path);
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/**
	 * Creates a vector dataset (e.g., driver "GPKG", "ESRI Shapefile", "GeoJSON",
	 * "FlatGeobuf", or "MEM" in memory with the name ""): add layers with
	 * {@link GdalDataset#createLayer}.
	 */
	public static GdalDataset createVector(String path, String driver, String... options) {
		GdalNative g = native_();
		try (Arena a = Arena.ofConfined()) {
			MemorySegment drv = (MemorySegment) g.GDALGetDriverByName.invokeExact(GdalNative.cstring(a, driver));
			if (GdalNative.isNull(drv) && "MEM".equals(driver)) {
				/* GDAL before 3.11: the vector driver in memory is "Memory" */
				drv = (MemorySegment) g.GDALGetDriverByName.invokeExact(GdalNative.cstring(a, "Memory"));
			}
			if (GdalNative.isNull(drv)) {
				throw new GdalException("Unknown GDAL driver: " + driver);
			}
			g.CPLErrorReset.invokeExact();
			MemorySegment h = (MemorySegment) g.GDALCreate.invokeExact(drv, GdalNative.cstring(a, path), 0, 0, 0, GdalDataType.UNKNOWN.code,
					GdalNative.stringList(a, Arrays.asList(options)));
			if (GdalNative.isNull(h)) {
				throw error("The vector dataset could not be created: " + path + " (" + driver + ")");
			}
			return new GdalDataset(h, path);
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/** The drivers (formats) of this GDAL. */
	public static List<GdalDriver> drivers() {
		GdalNative g = native_();
		List<GdalDriver> out = new ArrayList<>();
		try (Arena a = Arena.ofConfined()) {
			int count = (int) g.GDALGetDriverCount.invokeExact();
			MemorySegment raster = GdalNative.cstring(a, "DCAP_RASTER"), vector = GdalNative.cstring(a, "DCAP_VECTOR"),
					create = GdalNative.cstring(a, "DCAP_CREATE"), ext = GdalNative.cstring(a, "DMD_EXTENSIONS");
			for (int i = 0; i < count; i++) {
				MemorySegment d = (MemorySegment) g.GDALGetDriver.invokeExact(i);
				out.add(new GdalDriver(GdalNative.string((MemorySegment) g.GDALGetDriverShortName.invokeExact(d)),
						GdalNative.string((MemorySegment) g.GDALGetDriverLongName.invokeExact(d)), item(g, d, raster) != null,
						item(g, d, vector) != null, item(g, d, create) != null, String.valueOf(item(g, d, ext) == null ? "" : item(g, d, ext))));
			}
		} catch (Throwable t) {
			throw wrap(t);
		}
		return out;
	}

	private static String item(GdalNative g, MemorySegment obj, MemorySegment key) throws Throwable {
		return GdalNative.string((MemorySegment) g.GDALGetMetadataItem.invokeExact(obj, key, MemorySegment.NULL));
	}

	/* ---------------- The command line tools (gdal_utils.h) ---------------- */

	/** gdalinfo: the description of a dataset (options of gdalinfo, e.g. "-stats", "-json"). */
	public static String info(String path, String... options) {
		try (GdalDataset ds = open(path, OF_RASTER)) {
			return ds.info(options);
		}
	}

	/** gdal_translate: converts a raster (options of gdal_translate, e.g. "-of", "PNG", "-scale"). */
	public static void translate(String source, String destination, String... options) {
		try (GdalDataset src = open(source, OF_RASTER); GdalDataset out = translate(src, destination, options)) {
			out.flush();
		}
	}

	/** gdal_translate of an open dataset; the result is open (e.g., "-of", "MEM" with destination ""). */
	public static GdalDataset translate(GdalDataset source, String destination, String... options) {
		GdalNative g = utils(native_().GDALTranslate);
		try (Arena a = Arena.ofConfined()) {
			MemorySegment opts = (MemorySegment) g.GDALTranslateOptionsNew.invokeExact(GdalNative.stringList(a, Arrays.asList(options)), MemorySegment.NULL);
			if (GdalNative.isNull(opts)) {
				throw error("Invalid options of gdal_translate: " + Arrays.toString(options));
			}
			try {
				MemorySegment usage = a.allocate(JAVA_INT);
				g.CPLErrorReset.invokeExact();
				MemorySegment h = (MemorySegment) g.GDALTranslate.invokeExact(GdalNative.cstring(a, destination), source.handle(), opts, usage);
				if (GdalNative.isNull(h)) {
					throw error("gdal_translate failed: " + destination);
				}
				return new GdalDataset(h, destination);
			} finally {
				g.GDALTranslateOptionsFree.invokeExact(opts);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/** gdalwarp: reprojects, cuts or resamples a raster (options of gdalwarp, e.g. "-t_srs", "EPSG:4326"). */
	public static void warp(String source, String destination, String... options) {
		try (GdalDataset src = open(source, OF_RASTER); GdalDataset out = warp(src, destination, options)) {
			out.flush();
		}
	}

	/** gdalwarp of an open dataset; the result is open (e.g., "-of", "MEM" with destination ""). */
	public static GdalDataset warp(GdalDataset source, String destination, String... options) {
		GdalNative g = utils(native_().GDALWarp);
		try (Arena a = Arena.ofConfined()) {
			MemorySegment opts = (MemorySegment) g.GDALWarpAppOptionsNew.invokeExact(GdalNative.stringList(a, Arrays.asList(options)), MemorySegment.NULL);
			if (GdalNative.isNull(opts)) {
				throw error("Invalid options of gdalwarp: " + Arrays.toString(options));
			}
			try {
				MemorySegment sources = a.allocate(ADDRESS);
				sources.set(ADDRESS, 0, source.handle());
				MemorySegment usage = a.allocate(JAVA_INT);
				g.CPLErrorReset.invokeExact();
				MemorySegment h = (MemorySegment) g.GDALWarp.invokeExact(GdalNative.cstring(a, destination), MemorySegment.NULL, 1, sources, opts, usage);
				if (GdalNative.isNull(h)) {
					throw error("gdalwarp failed: " + destination);
				}
				return new GdalDataset(h, destination);
			} finally {
				g.GDALWarpAppOptionsFree.invokeExact(opts);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/** ogr2ogr: converts vectors (options of ogr2ogr, e.g. "-f", "GPKG", "-t_srs", "EPSG:4326"). */
	public static void vectorTranslate(String source, String destination, String... options) {
		try (GdalDataset src = open(source, OF_VECTOR); GdalDataset out = vectorTranslate(src, destination, options)) {
			out.flush();
		}
	}

	/** ogr2ogr of an open dataset; the result is open (e.g., "-f", "Memory" with destination ""). */
	public static GdalDataset vectorTranslate(GdalDataset source, String destination, String... options) {
		GdalNative g = utils(native_().GDALVectorTranslate);
		try (Arena a = Arena.ofConfined()) {
			MemorySegment opts = (MemorySegment) g.GDALVectorTranslateOptionsNew.invokeExact(GdalNative.stringList(a, Arrays.asList(options)),
					MemorySegment.NULL);
			if (GdalNative.isNull(opts)) {
				throw error("Invalid options of ogr2ogr: " + Arrays.toString(options));
			}
			try {
				MemorySegment sources = a.allocate(ADDRESS);
				sources.set(ADDRESS, 0, source.handle());
				MemorySegment usage = a.allocate(JAVA_INT);
				g.CPLErrorReset.invokeExact();
				MemorySegment h = (MemorySegment) g.GDALVectorTranslate.invokeExact(GdalNative.cstring(a, destination), MemorySegment.NULL, 1, sources,
						opts, usage);
				if (GdalNative.isNull(h)) {
					throw error("ogr2ogr failed: " + destination);
				}
				return new GdalDataset(h, destination);
			} finally {
				g.GDALVectorTranslateOptionsFree.invokeExact(opts);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/**
	 * gdaldem: "hillshade", "slope", "aspect", "color-relief", "TRI", "TPI",
	 * "roughness" of an elevation model (options of gdaldem, e.g. "-z", "2",
	 * "-alg", "Horn", "-compute_edges").
	 */
	public static void dem(String source, String destination, String processing, String... options) {
		demFile(source, destination, processing, null, options);
	}

	/** gdaldem color-relief: the colors of a color file (lines "elevation r g b [a]", also "nv" for no data). */
	public static void colorRelief(String source, String destination, String colorFile, String... options) {
		demFile(source, destination, "color-relief", colorFile, options);
	}

	private static void demFile(String source, String destination, String processing, String colorFile, String... options) {
		try (GdalDataset src = open(source, OF_RASTER); GdalDataset out = dem(src, destination, processing, colorFile, options)) {
			out.flush();
		}
	}

	/** gdaldem of an open dataset; the result is open (e.g., "-of", "MEM" with destination ""). */
	public static GdalDataset dem(GdalDataset source, String destination, String processing, String colorFile, String... options) {
		GdalNative g = utils(native_().GDALDEMProcessing);
		try (Arena a = Arena.ofConfined()) {
			MemorySegment opts = (MemorySegment) g.GDALDEMProcessingOptionsNew.invokeExact(GdalNative.stringList(a, Arrays.asList(options)),
					MemorySegment.NULL);
			if (GdalNative.isNull(opts)) {
				throw error("Invalid options of gdaldem: " + Arrays.toString(options));
			}
			try {
				g.CPLErrorReset.invokeExact();
				MemorySegment h = (MemorySegment) g.GDALDEMProcessing.invokeExact(GdalNative.cstring(a, destination), source.handle(),
						GdalNative.cstring(a, processing), GdalNative.cstringOrNull(a, colorFile), opts, a.allocate(JAVA_INT));
				if (GdalNative.isNull(h)) {
					throw error("gdaldem " + processing + " failed: " + destination);
				}
				return new GdalDataset(h, destination);
			} finally {
				g.GDALDEMProcessingOptionsFree.invokeExact(opts);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/** gdalbuildvrt: a virtual mosaic (or stack with "-separate") of raster files (options of gdalbuildvrt). */
	public static void buildVrt(String destination, List<String> sources, String... options) {
		try (GdalDataset out = buildVrt(destination, null, sources, options)) {
			out.flush();
		}
	}

	/** gdalbuildvrt of open datasets; the result is open (destination "" for a virtual raster in memory). */
	public static GdalDataset buildVrt(String destination, GdalDataset[] sources, String... options) {
		return buildVrt(destination, sources, null, options);
	}

	private static GdalDataset buildVrt(String destination, GdalDataset[] datasets, List<String> names, String... options) {
		GdalNative g = utils(native_().GDALBuildVRT);
		try (Arena a = Arena.ofConfined()) {
			MemorySegment opts = (MemorySegment) g.GDALBuildVRTOptionsNew.invokeExact(GdalNative.stringList(a, Arrays.asList(options)), MemorySegment.NULL);
			if (GdalNative.isNull(opts)) {
				throw error("Invalid options of gdalbuildvrt: " + Arrays.toString(options));
			}
			try {
				int n;
				MemorySegment handles = MemorySegment.NULL, files = MemorySegment.NULL;
				if (datasets != null) {
					n = datasets.length;
					handles = a.allocate(ADDRESS, Math.max(1, n));
					for (int i = 0; i < n; i++) {
						handles.setAtIndex(ADDRESS, i, datasets[i].handle());
					}
				} else {
					n = names.size();
					files = GdalNative.stringList(a, names);
				}
				g.CPLErrorReset.invokeExact();
				MemorySegment h = (MemorySegment) g.GDALBuildVRT.invokeExact(GdalNative.cstring(a, destination), n, handles, files, opts, a.allocate(JAVA_INT));
				if (GdalNative.isNull(h)) {
					throw error("gdalbuildvrt failed: " + destination);
				}
				return new GdalDataset(h, destination);
			} finally {
				g.GDALBuildVRTOptionsFree.invokeExact(opts);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/**
	 * gdal_rasterize: vectors into a new raster (options of gdal_rasterize: the
	 * size "-tr" or "-ts", the values "-burn" or "-a field", "-of", "-ot", ...).
	 */
	public static void rasterize(String vectorSource, String destination, String... options) {
		try (GdalDataset src = open(vectorSource, OF_VECTOR); GdalDataset out = rasterize(src, destination, options)) {
			out.flush();
		}
	}

	/** gdal_rasterize of an open vector dataset into a new raster; the result is open (e.g., "-of", "MEM" with destination ""). */
	public static GdalDataset rasterize(GdalDataset vectors, String destination, String... options) {
		return rasterize(vectors, null, destination, options);
	}

	/** gdal_rasterize into an existing raster (opened for update or created): burns the vectors into its bands. */
	public static void rasterize(GdalDataset vectors, GdalDataset target, String... options) {
		rasterize(vectors, target, null, options);
	}

	private static GdalDataset rasterize(GdalDataset vectors, GdalDataset target, String destination, String... options) {
		GdalNative g = utils(native_().GDALRasterize);
		try (Arena a = Arena.ofConfined()) {
			MemorySegment opts = (MemorySegment) g.GDALRasterizeOptionsNew.invokeExact(GdalNative.stringList(a, Arrays.asList(options)), MemorySegment.NULL);
			if (GdalNative.isNull(opts)) {
				throw error("Invalid options of gdal_rasterize: " + Arrays.toString(options));
			}
			try {
				g.CPLErrorReset.invokeExact();
				/* Local variables: a conditional expression as argument of invokeExact has the type Object */
				MemorySegment dest = target == null ? GdalNative.cstring(a, destination) : MemorySegment.NULL;
				MemorySegment targetHandle = target == null ? MemorySegment.NULL : target.handle();
				MemorySegment h = (MemorySegment) g.GDALRasterize.invokeExact(dest, targetHandle, vectors.handle(), opts, a.allocate(JAVA_INT));
				if (GdalNative.isNull(h)) {
					throw error("gdal_rasterize failed" + (destination != null ? ": " + destination : ""));
				}
				return target == null ? new GdalDataset(h, destination) : target;
			} finally {
				g.GDALRasterizeOptionsFree.invokeExact(opts);
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw wrap(t);
		}
	}

	/* ---------------- Internal ---------------- */

	static GdalNative native_() {
		GdalNative g = n;
		if (g == null) {
			throw new GdalException("GDAL is not loaded (Gdal.load())");
		}
		return g;
	}

	private static GdalNative utils(Object function) {
		if (function == null) {
			throw new GdalException("The tools of GDAL (gdal_utils.h) need GDAL 2.1 or later");
		}
		return native_();
	}

	/** An exception with the last error message of GDAL. */
	static GdalException error(String what) {
		GdalNative g = n;
		String msg = null;
		if (g != null) {
			try {
				msg = GdalNative.string((MemorySegment) g.CPLGetLastErrorMsg.invokeExact());
			} catch (Throwable t) {
				/* No message */
			}
		}
		return new GdalException(msg == null || msg.isBlank() ? what : what + ": " + msg);
	}

	static GdalException wrap(Throwable t) {
		return t instanceof GdalException e ? e : new GdalException(String.valueOf(t), t);
	}

	/** Frees memory of GDAL (VSIFree). */
	static void free(MemorySegment p) {
		if (!GdalNative.isNull(p)) {
			try {
				native_().VSIFree.invokeExact(p);
			} catch (Throwable t) {
				/* Not freed */
			}
		}
	}
}
