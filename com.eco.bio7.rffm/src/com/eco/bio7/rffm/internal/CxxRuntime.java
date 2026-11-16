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
package com.eco.bio7.rffm.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The C++ runtime (libstdc++) of a process with R and Python (Linux). A process
 * can only load one libstdc++.so.6: the first loaded is used by all libraries.
 * R or Python of conda (or another build) may need a newer one than the system
 * (e.g., "version CXXABI_1.3.15 not found"); loaded after an older one (e.g.,
 * numpy of the system Python) it can't be started. Before the first runtime is
 * loaded, the newest libstdc++ of the known installations is loaded (it is
 * backward compatible). The folders: the runtime's own, the system and the
 * system property bio7.native.libdirs (set by the application: the folders of
 * the other runtimes).
 * <p>
 * The same class is in com.eco.bio7.pyffm (the bundles are independent).
 */
public final class CxxRuntime {

	/** The system property with more library folders (separated by File.pathSeparator). */
	public static final String LIBDIRS = "bio7.native.libdirs";

	private static final Pattern GLIBCXX = Pattern.compile("GLIBCXX_3\\.4\\.(\\d+)");

	private static volatile boolean done;

	private CxxRuntime() {
	}

	/**
	 * Loads the newest libstdc++.so.6 of the folders (and of LIBDIRS and the
	 * system) globally, if none is loaded yet. Linux only; never fails.
	 */
	public static synchronized void preloadNewest(List<Path> ownDirs) {
		if (done || !System.getProperty("os.name", "").toLowerCase().startsWith("linux")) {
			return;
		}
		try {
			if (alreadyLoaded()) {
				done = true;
				return;
			}
			Set<Path> dirs = new LinkedHashSet<>();
			String extra = System.getProperty(LIBDIRS, "");
			for (String d : extra.split(File.pathSeparator)) {
				if (!d.isBlank()) {
					dirs.add(Path.of(d.trim()));
				}
			}
			dirs.addAll(ownDirs);
			for (String d : new String[] { "/usr/lib/x86_64-linux-gnu", "/lib/x86_64-linux-gnu", "/usr/lib/aarch64-linux-gnu", "/usr/lib64", "/usr/lib" }) {
				dirs.add(Path.of(d));
			}
			Path best = null;
			int bestVersion = -1;
			for (Path d : dirs) {
				Path f = d.resolve("libstdc++.so.6");
				if (!Files.isRegularFile(f)) {
					continue;
				}
				int v = glibcxxVersion(f);
				if (v > bestVersion) {
					best = f;
					bestVersion = v;
				}
			}
			if (best == null) {
				return;
			}
			/* libgcc_s of the same folder first (libstdc++ needs it, a newer one may be needed) */
			Path gcc = best.resolveSibling("libgcc_s.so.1");
			if (Files.isRegularFile(gcc)) {
				dlopenGlobal(gcc);
			}
			if (dlopenGlobal(best)) {
				done = true;
			}
		} catch (Throwable t) {
			/* As before: the libraries find their runtime themselves */
		}
	}

	/* The highest GLIBCXX_3.4.N version the library defines (-1 if none) */
	private static int glibcxxVersion(Path f) {
		try {
			String s = new String(Files.readAllBytes(f), StandardCharsets.ISO_8859_1);
			Matcher m = GLIBCXX.matcher(s);
			int max = -1;
			while (m.find()) {
				max = Math.max(max, Integer.parseInt(m.group(1)));
			}
			return max;
		} catch (Exception e) {
			return -1;
		}
	}

	/* Returns true if a libstdc++ is in the process (then it can't be replaced) */
	private static boolean alreadyLoaded() {
		try {
			List<String> maps = new ArrayList<>(Files.readAllLines(Path.of("/proc/self/maps")));
			return maps.stream().anyMatch(l -> l.contains("libstdc++.so"));
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean dlopenGlobal(Path lib) throws Throwable {
		Linker linker = Linker.nativeLinker();
		MethodHandle dlopen = linker.downcallHandle(linker.defaultLookup().find("dlopen").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
		try (Arena a = Arena.ofConfined()) {
			/* RTLD_NOW | RTLD_GLOBAL */
			MemorySegment h = (MemorySegment) dlopen.invokeExact(a.allocateFrom(lib.toString()), 0x2 | 0x100);
			return h.address() != 0;
		}
	}
}
