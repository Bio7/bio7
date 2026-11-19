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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds the GDAL library of an installation (no library is shipped): conda
 * (Anaconda, Miniconda, Miniforge and their environments), Homebrew, MacPorts,
 * the packages of Linux, QGIS and OSGeo4W. Also the data folders of GDAL
 * (GDAL_DATA) and PROJ (proj.db) of an installation.
 */
public final class GdalLibraryFinder {

	public static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
	public static final boolean WINDOWS = OS.startsWith("windows");
	public static final boolean MAC = OS.startsWith("mac");

	/* libgdal.dylib, libgdal.36.dylib; libgdal.so, libgdal.so.36; gdal.dll, gdal310.dll */
	private static final Pattern NAME = WINDOWS ? Pattern.compile("(?i)gdal\\d*\\.dll")
			: MAC ? Pattern.compile("libgdal(\\.\\d+)*\\.dylib") : Pattern.compile("libgdal\\.so(\\.\\d+)*");

	private GdalLibraryFinder() {
	}

	/** The first library found (see {@link #candidates()}). */
	public static Optional<Path> find() {
		List<Path> c = candidates();
		return c.isEmpty() ? Optional.empty() : Optional.of(c.get(0));
	}

	/** The libraries found, the preferred first (an active conda environment, then the package managers). */
	public static List<Path> candidates() {
		Set<Path> result = new LinkedHashSet<>();
		String env = System.getenv("GDAL_LIBRARY_PATH");
		if (env != null && !env.isBlank()) {
			add(result, resolve(Path.of(env)));
		}
		for (Path folder : folders()) {
			if (Files.isDirectory(folder)) {
				add(result, resolve(folder));
			}
		}
		return new ArrayList<>(result);
	}

	private static void add(Set<Path> result, Path p) {
		if (p != null) {
			result.add(p.toAbsolutePath().normalize());
		}
	}

	/**
	 * The reason why a library can't be loaded (the message of the system loader:
	 * e.g., a missing dependency or a wrong architecture), null if unknown.
	 */
	public static String loadError(Path library) {
		if (WINDOWS) {
			return null;
		}
		try {
			java.lang.foreign.Linker linker = java.lang.foreign.Linker.nativeLinker();
			java.lang.foreign.SymbolLookup libc = linker.defaultLookup();
			java.lang.invoke.MethodHandle dlopen = linker.downcallHandle(libc.find("dlopen").orElseThrow(),
					java.lang.foreign.FunctionDescriptor.of(java.lang.foreign.ValueLayout.ADDRESS, java.lang.foreign.ValueLayout.ADDRESS,
							java.lang.foreign.ValueLayout.JAVA_INT));
			java.lang.invoke.MethodHandle dlerror = linker.downcallHandle(libc.find("dlerror").orElseThrow(),
					java.lang.foreign.FunctionDescriptor.of(com.eco.bio7.gdal.internal.GdalNative.C_POINTER));
			try (java.lang.foreign.Arena a = java.lang.foreign.Arena.ofConfined()) {
				/* RTLD_NOW (2): all dependencies are resolved */
				java.lang.foreign.MemorySegment h = (java.lang.foreign.MemorySegment) dlopen.invokeExact(a.allocateFrom(library.toString()), 2);
				if (!com.eco.bio7.gdal.internal.GdalNative.isNull(h)) {
					return null;
				}
				return com.eco.bio7.gdal.internal.GdalNative.string((java.lang.foreign.MemorySegment) dlerror.invokeExact());
			}
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * The library of a file or a folder (the library in the folder, also in its
	 * lib or bin folder), null if none.
	 */
	public static Path resolve(Path fileOrFolder) {
		if (fileOrFolder == null) {
			return null;
		}
		if (Files.isRegularFile(fileOrFolder)) {
			return fileOrFolder;
		}
		if (!Files.isDirectory(fileOrFolder)) {
			return null;
		}
		for (Path dir : List.of(fileOrFolder, fileOrFolder.resolve("lib"), fileOrFolder.resolve("bin"), fileOrFolder.resolve("Library").resolve("bin"))) {
			Path lib = inFolder(dir);
			if (lib != null) {
				return lib;
			}
		}
		return null;
	}

	/* The library of a folder: the unversioned name first (the link of the package manager) */
	private static Path inFolder(Path dir) {
		if (!Files.isDirectory(dir)) {
			return null;
		}
		String plain = WINDOWS ? "gdal.dll" : MAC ? "libgdal.dylib" : "libgdal.so";
		if (Files.isRegularFile(dir.resolve(plain))) {
			return dir.resolve(plain);
		}
		List<Path> found = new ArrayList<>();
		try (DirectoryStream<Path> s = Files.newDirectoryStream(dir)) {
			for (Path p : s) {
				if (NAME.matcher(p.getFileName().toString()).matches() && Files.isRegularFile(p)) {
					found.add(p);
				}
			}
		} catch (IOException | RuntimeException e) {
			return null;
		}
		/* The newest version (the longest, then the highest name) */
		found.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
		return found.isEmpty() ? null : found.get(0);
	}

	/* The folders of the installations */
	private static List<Path> folders() {
		List<Path> f = new ArrayList<>();
		String home = System.getProperty("user.home", "");
		String conda = System.getenv("CONDA_PREFIX");
		if (conda != null && !conda.isBlank()) {
			f.add(Path.of(conda));
		}
		List<Path> condaRoots = new ArrayList<>();
		for (String name : List.of("anaconda3", "miniconda3", "miniforge3", "mambaforge", "micromamba", "Anaconda3", "Miniconda3")) {
			condaRoots.add(Path.of(home, name));
		}
		if (WINDOWS) {
			String programData = System.getenv().getOrDefault("ProgramData", "C:\\ProgramData");
			for (String name : List.of("anaconda3", "miniconda3", "miniforge3", "Anaconda3", "Miniconda3")) {
				condaRoots.add(Path.of(programData, name));
			}
			f.add(Path.of("C:\\OSGeo4W", "bin"));
			f.add(Path.of("C:\\OSGeo4W64", "bin"));
			/* The OSGeo4W installer for the current user (without administrator rights) */
			String local = System.getenv("LOCALAPPDATA");
			if (local != null && !local.isBlank()) {
				f.add(Path.of(local, "Programs", "OSGeo4W", "bin"));
			}
			String pf = System.getenv().getOrDefault("ProgramFiles", "C:\\Program Files");
			f.addAll(matching(Path.of(pf), "QGIS.*", "bin"));
		} else if (MAC) {
			f.add(Path.of("/opt/homebrew/lib"));
			f.add(Path.of("/usr/local/lib"));
			f.add(Path.of("/opt/local/lib"));
			condaRoots.add(Path.of("/opt/anaconda3"));
			condaRoots.add(Path.of("/opt/miniconda3"));
			condaRoots.add(Path.of("/opt/homebrew/Caskroom/miniforge/base"));
			f.addAll(matching(Path.of("/Applications"), "QGIS.*\\.app", "Contents/MacOS/lib"));
			f.addAll(matching(Path.of("/Applications"), "QGIS.*\\.app", "Contents/Frameworks"));
		} else {
			for (String d : List.of("/usr/lib/x86_64-linux-gnu", "/usr/lib/aarch64-linux-gnu", "/usr/lib64", "/usr/lib", "/usr/local/lib")) {
				f.add(Path.of(d));
			}
			condaRoots.add(Path.of("/opt/conda"));
			condaRoots.add(Path.of("/opt/anaconda3"));
		}
		for (Path root : condaRoots) {
			f.add(root);
			f.addAll(matching(root.resolve("envs"), ".*", ""));
		}
		return f;
	}

	private static List<Path> matching(Path dir, String regex, String sub) {
		List<Path> out = new ArrayList<>();
		if (!Files.isDirectory(dir)) {
			return out;
		}
		try (DirectoryStream<Path> s = Files.newDirectoryStream(dir)) {
			for (Path p : s) {
				if (p.getFileName().toString().matches(regex)) {
					out.add(sub.isEmpty() ? p : p.resolve(sub));
				}
			}
		} catch (IOException | RuntimeException e) {
			/* Not readable */
		}
		out.sort(null);
		return out;
	}

	/* ---------------- The data of GDAL and PROJ ---------------- */

	/** The data folder of GDAL of the installation of the library (GDAL_DATA), null if not found. */
	public static Path gdalData(Path library) {
		return dataFolder(library, "gdal", "gdalvrt.xsd", "header.dxf", "gdalicon.png");
	}

	/** The data folder of PROJ (with proj.db) of the installation of the library, null if not found. */
	public static Path projData(Path library) {
		return dataFolder(library, "proj", "proj.db");
	}

	/**
	 * The folder of PROJ for the files of the user (e.g., datum grids downloaded by
	 * hand or with projsync): %LOCALAPPDATA%\proj, ~/Library/Application
	 * Support/proj, ~/.local/share/proj (XDG_DATA_HOME). It may not exist.
	 */
	public static Path userProjData() {
		String home = System.getProperty("user.home", "");
		if (WINDOWS) {
			String local = System.getenv("LOCALAPPDATA");
			return local == null || local.isBlank() ? Path.of(home, "AppData", "Local", "proj") : Path.of(local, "proj");
		}
		if (MAC) {
			return Path.of(home, "Library", "Application Support", "proj");
		}
		String xdg = System.getenv("XDG_DATA_HOME");
		return xdg == null || xdg.isBlank() ? Path.of(home, ".local", "share", "proj") : Path.of(xdg, "proj");
	}

	/**
	 * The datum grids of PROJ in the folders (GeoTIFF grids of the PROJ CDN, e.g.,
	 * us_noaa_conus.tif, de_adv_BETA2007.tif, and NTv2 *.gsb files).
	 */
	public static List<Path> datumGrids(Path... dirs) {
		List<Path> grids = new ArrayList<>();
		for (Path dir : dirs) {
			if (dir == null || !Files.isDirectory(dir)) {
				continue;
			}
			try (var files = Files.list(dir)) {
				files.filter(p -> {
					String n = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
					return n.matches("[a-z]{2}_[a-z0-9]+_.+\\.tif") || n.endsWith(".gsb");
				}).forEach(grids::add);
			} catch (IOException | RuntimeException e) {
				/* Not readable */
			}
		}
		return grids;
	}

	private static Path dataFolder(Path library, String name, String... marker) {
		if (library == null || library.getParent() == null) {
			return null;
		}
		Path lib = library.getParent();
		List<Path> roots = new ArrayList<>();
		for (Path p = lib; p != null && roots.size() < 4; p = p.getParent()) {
			roots.add(p);
		}
		for (Path root : roots) {
			for (Path dir : List.of(root.resolve("share").resolve(name), root.resolve("Resources").resolve(name), root.resolve("Library").resolve("share").resolve(name),
					root.resolve("apps").resolve("gdal").resolve("share").resolve(name), root.resolve("apps").resolve(name).resolve("share").resolve(name),
					root.resolve("Resources").resolve("share").resolve(name))) {
				for (String m : marker) {
					if (Files.isRegularFile(dir.resolve(m))) {
						return dir;
					}
				}
			}
		}
		return null;
	}
}
