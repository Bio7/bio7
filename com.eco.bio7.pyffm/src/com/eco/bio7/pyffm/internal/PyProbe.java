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
package com.eco.bio7.pyffm.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.eco.bio7.pyffm.PyException;

/**
 * Asks a Python executable for its installation: the version, the shared
 * library (libpython), the home (the standard library) and the paths of the
 * modules (also of a venv or conda environment).
 */
public final class PyProbe {

	/** The oldest supported Python. */
	public static final int MIN_MINOR = 8;

	/**
	 * An installation.
	 *
	 * @param executable the executable
	 * @param version    the version (e.g., "3.12.4")
	 * @param minor      the minor version (3.minor)
	 * @param library    the shared library
	 * @param home       PYTHONHOME (sys.base_prefix)
	 * @param stdlib     the folder of the standard library
	 * @param sysPath    sys.path of the executable (without the current folder)
	 */
	public record Installation(Path executable, String version, int minor, Path library, String home, String stdlib, List<String> sysPath) {
	}

	private static final String SCRIPT = """
			import sys, sysconfig, json, os
			v = sysconfig.get_config_vars()
			d = {"version": "%d.%d.%d" % sys.version_info[:3], "major": sys.version_info[0], "minor": sys.version_info[1],
			     "base_prefix": sys.base_prefix, "prefix": sys.prefix, "executable": sys.executable,
			     "path": [p for p in sys.path if p], "stdlib": sysconfig.get_path("stdlib"),
			     "LIBDIR": v.get("LIBDIR"), "LDLIBRARY": v.get("LDLIBRARY"), "INSTSONAME": v.get("INSTSONAME"),
			     "MULTIARCH": v.get("MULTIARCH"), "FRAMEWORK": v.get("PYTHONFRAMEWORK"), "FRAMEWORKPREFIX": v.get("PYTHONFRAMEWORKPREFIX"),
			     "SHARED": v.get("Py_ENABLE_SHARED"), "GIL_DISABLED": v.get("Py_GIL_DISABLED"), "dll": None}
			if sys.platform == "win32":
			    import ctypes
			    b = ctypes.create_unicode_buffer(1024)
			    ctypes.windll.kernel32.GetModuleFileNameW(ctypes.c_void_p(sys.dllhandle), b, 1024)
			    d["dll"] = b.value
			print("BIO7PROBE" + json.dumps(d))
			""";

	private PyProbe() {
	}

	/** Probes the executable (runs it once). */
	@SuppressWarnings("unchecked")
	public static Installation probe(Path executable) throws PyException {
		String out;
		try {
			Process p = new ProcessBuilder(executable.toString(), "-c", SCRIPT).redirectErrorStream(true).start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				throw new PyException("Python did not respond: " + executable);
			}
			out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new PyException("Python could not be started: " + executable + " (" + e.getMessage() + ")", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PyException("Interrupted");
		}
		int at = out.indexOf("BIO7PROBE");
		if (at < 0) {
			throw new PyException("Not a Python 3 interpreter: " + executable + "\n" + out.trim());
		}
		Map<String, Object> d = (Map<String, Object>) Json.parse(out.substring(at + 9).trim());
		int major = ((Number) d.get("major")).intValue();
		int minor = ((Number) d.get("minor")).intValue();
		String version = (String) d.get("version");
		if (major != 3 || minor < MIN_MINOR) {
			throw new PyException("Embedded Python needs Python 3." + MIN_MINOR + " or later (this is Python " + version + ").");
		}
		if (truthy(d.get("GIL_DISABLED"))) {
			throw new PyException("The free-threaded Python build (" + version + "t) is not supported. Please select a standard Python.");
		}
		String home = (String) d.get("base_prefix");
		String stdlib = (String) d.get("stdlib");
		if (stdlib == null || !Files.isDirectory(Path.of(stdlib).resolve("encodings"))) {
			throw new PyException("The standard library of Python was not found: " + stdlib);
		}
		Path library = library(d, home, minor);
		if (library == null) {
			throw new PyException("The shared library of Python (libpython) was not found for " + executable
					+ (truthy(d.get("SHARED")) || isWindows() ? "." : ".\nThis Python is built without a shared library (e.g., pyenv without --enable-shared).")
					+ "\nPlease select another Python (e.g., from python.org, Homebrew or conda).");
		}
		List<String> path = new ArrayList<>((List<String>) d.get("path"));
		return new Installation(executable, version, minor, library, home, stdlib, path);
	}

	private static boolean truthy(Object o) {
		return o instanceof Number n ? n.intValue() != 0 : o instanceof Boolean b && b;
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().startsWith("windows");
	}

	/* The shared library: the DLL of the process (Windows), the framework (macOS), else in LIBDIR */
	private static Path library(Map<String, Object> d, String home, int minor) {
		Set<Path> candidates = new LinkedHashSet<>();
		if (d.get("dll") instanceof String dll) {
			candidates.add(Path.of(dll));
		}
		if (isWindows()) {
			candidates.add(Path.of(home, "python3" + minor + ".dll"));
			candidates.add(Path.of((String) d.get("prefix"), "python3" + minor + ".dll"));
		}
		String framework = (String) d.get("FRAMEWORK");
		String frameworkPrefix = (String) d.get("FRAMEWORKPREFIX");
		if (framework != null && !framework.isEmpty() && frameworkPrefix != null && !frameworkPrefix.isEmpty()) {
			candidates.add(Path.of(frameworkPrefix, framework + ".framework", "Versions", "3." + minor, framework));
			candidates.add(Path.of(home, framework));
		}
		String libdir = (String) d.get("LIBDIR");
		if (libdir != null) {
			for (String key : new String[] { "INSTSONAME", "LDLIBRARY" }) {
				if (d.get(key) instanceof String name && !name.isEmpty() && !name.endsWith(".a")) {
					candidates.add(Path.of(libdir, name));
					if (d.get("MULTIARCH") instanceof String arch && !arch.isEmpty()) {
						candidates.add(Path.of(libdir, arch, name));
					}
				}
			}
			for (String suffix : new String[] { ".dylib", ".so", ".so.1.0" }) {
				candidates.add(Path.of(libdir, "libpython3." + minor + suffix));
			}
		}
		for (String suffix : new String[] { ".dylib", ".so", ".so.1.0" }) {
			candidates.add(Path.of(home, "lib", "libpython3." + minor + suffix));
		}
		for (Path p : candidates) {
			if (Files.isRegularFile(p)) {
				return p;
			}
		}
		return null;
	}
}
