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

package com.eco.bio7.rbridge;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Finds the R installations of the computer and checks them: each candidate
 * (the path of the preferences, R_HOME, the PATH, the registry on Windows and
 * the usual folders) is started once with Rscript and reports its version,
 * architecture, library paths and the installed Rserve package.
 */
public final class RLocator {

	/** The marker of the output of the probe (the lines after it are key=value). */
	private static final String MARKER = "BIO7PROBE";
	/* The probe (single quotes only: one argument of Rscript on all systems) */
	private static final String PROBE = "cat('" + MARKER + "\\n');" + "cat('version=', as.character(getRversion()), '\\n', sep='');"
			+ "cat('arch=', R.version$arch, '\\n', sep='');" + "cat('platform=', R.version$platform, '\\n', sep='');"
			+ "cat('home=', R.home(), '\\n', sep='');" + "cat('lib=', paste(.libPaths(), collapse='|'), '\\n', sep='');"
			+ "cat('userLib=', path.expand(strsplit(Sys.getenv('R_LIBS_USER'), .Platform$path.sep)[[1]][1]), '\\n', sep='');"
			+ "p <- suppressWarnings(tryCatch(utils::packageDescription('Rserve'), error=function(e) NULL));"
			+ "if (is.list(p)) { cat('rserve=', p$Version, '\\n', sep=''); cat('rserveBuilt=', p$Built, '\\n', sep='');"
			+ " cat('rserveLib=', dirname(system.file(package='Rserve')), '\\n', sep='');"
			+ " l <- tryCatch({ suppressWarnings(loadNamespace('Rserve')); 'OK' }, error=function(e) gsub('[\\r\\n]+', ' ', conditionMessage(e)));"
			+ " cat('rserveLoad=', l, '\\n', sep='') }";

	private RLocator() {
	}

	/**
	 * An R installation: the result of a probe.
	 *
	 * @param home            the home of R (R.home()), or the tested path if the
	 *                        probe failed
	 * @param version         the version of R (e.g., 4.6.0) or null if R could not
	 *                        be started
	 * @param arch            the architecture (e.g., aarch64, x86_64)
	 * @param platform        the platform of R (e.g., aarch64-apple-darwin23)
	 * @param libraryPaths    the library paths (.libPaths())
	 * @param userLibrary     the user library (R_LIBS_USER, may not exist yet) or
	 *                        null
	 * @param rserveVersion   the version of the installed Rserve package or null
	 * @param rserveBuilt     the "Built" field of the Rserve package (the R version
	 *                        and platform it was built with) or null
	 * @param rserveLibrary   the library which contains the Rserve package or null
	 * @param rserveLoadError the error if the installed Rserve package can't be
	 *                        loaded (e.g., another architecture), else null
	 * @param error           the error if R could not be started, else null
	 */
	public record RInstallation(String home, String version, String arch, String platform, List<String> libraryPaths, String userLibrary,
			String rserveVersion, String rserveBuilt, String rserveLibrary, String rserveLoadError, String error) {

		/** Returns true if R could be started. */
		public boolean isValid() {
			return version != null;
		}

		/** Returns true if the Rserve package is installed. */
		public boolean hasRserve() {
			return rserveVersion != null;
		}

		/** Returns true if the Rserve package is installed and can be loaded. */
		public boolean isRserveUsable() {
			return hasRserve() && rserveLoadError == null;
		}

		/**
		 * Returns true if the installed Rserve package was built for another minor
		 * version of R (e.g., built with R 4.4, running in R 4.6): it should be
		 * installed again.
		 */
		public boolean isRserveBuiltForOtherR() {
			String built = rserveBuiltRVersion();
			return version != null && built != null && !minor(built).equals(minor(version));
		}

		/** The version of R the Rserve package was built with (e.g., 4.6.0) or null. */
		public String rserveBuiltRVersion() {
			if (rserveBuilt == null) {
				return null;
			}
			/* "R 4.6.0; aarch64-apple-darwin23; ..." */
			String built = rserveBuilt.trim();
			if (!built.startsWith("R ")) {
				return null;
			}
			return built.substring(2).split(";")[0].trim();
		}

		/** The minor version of R (e.g., 4.6) or null. */
		public String minorVersion() {
			return version == null ? null : minor(version);
		}

		/** The operating system of R: "windows", "mac" or "linux" (from the platform). */
		public String os() {
			String p = platform == null ? "" : platform.toLowerCase(Locale.ROOT);
			if (p.contains("mingw") || p.contains("windows")) {
				return "windows";
			}
			if (p.contains("darwin") || p.contains("apple")) {
				return "mac";
			}
			if (p.isEmpty()) {
				String os = RLocator.os();
				return os.equals("Windows") ? "windows" : os.equals("Mac") ? "mac" : "linux";
			}
			return "linux";
		}

		/** The normalized architecture of R: "aarch64" or "x86_64" (else as reported). */
		public String normalizedArch() {
			return normalizeArch(arch);
		}

		/** A short description (e.g., "R 4.6.0 (aarch64), Rserve 1.8-15"). */
		public String describe() {
			if (!isValid()) {
				return "R could not be started in " + (home == null || home.isEmpty() ? "the PATH" : home) + (error != null ? ": " + error : "");
			}
			return "R " + version + " (" + arch + ") in " + home + ", " + describeRserve();
		}

		/** A short description of the installed Rserve package. */
		public String describeRserve() {
			if (!hasRserve()) {
				return "Rserve is not installed";
			}
			String text = "Rserve " + rserveVersion;
			if (rserveLoadError != null) {
				return text + " (can't be loaded: " + rserveLoadError + ")";
			}
			if (isRserveBuiltForOtherR()) {
				return text + " (built for R " + rserveBuiltRVersion() + ")";
			}
			return text;
		}
	}

	/**
	 * The result of the check of the build tools (for compiling Rserve).
	 *
	 * @param available true if a C compiler and make were found
	 * @param detail    what was found (or not)
	 * @param help      how to install the tools (empty if available)
	 */
	public record BuildTools(boolean available, String detail, String help) {
	}

	/**
	 * Tests the R of a home folder (e.g., the path of the preferences).
	 *
	 * @param home the home of R (the folder with bin/R) or empty for the R of the
	 *             PATH
	 * @return the installation (isValid is false if R could not be started)
	 */
	public static RInstallation probe(String home) {
		String h = home == null ? "" : home.trim();
		String rscript = rscript(h);
		if (rscript == null) {
			return failed(h, "Rscript was not found in the folder");
		}
		List<String> command = new ArrayList<>();
		command.add(rscript);
		/* Not --vanilla: the library paths of the user (.Renviron, .Rprofile) like in the console of Bio7 */
		command.add("--no-save");
		command.add("--no-restore");
		command.add("-e");
		command.add(PROBE);
		ProcessOutput result = run(command, 30);
		if (result.error() != null) {
			return failed(h, result.error());
		}
		return parse(h, result.output());
	}

	private static RInstallation failed(String home, String error) {
		return new RInstallation(home, null, null, null, List.of(), null, null, null, null, null, error);
	}

	/* The values of the probe (the lines after the marker) */
	static RInstallation parse(String home, String output) {
		int start = output.indexOf(MARKER);
		if (start < 0) {
			String text = output.strip();
			return failed(home, text.isEmpty() ? "no output of R" : text.lines().reduce((_, b) -> b).orElse(text));
		}
		Map<String, String> values = new LinkedHashMap<>();
		for (String line : output.substring(start + MARKER.length()).split("\\R")) {
			int eq = line.indexOf('=');
			if (eq > 0) {
				values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
			}
		}
		String version = values.get("version");
		if (version == null || version.isEmpty()) {
			return failed(home, "the version of R was not reported");
		}
		List<String> libs = new ArrayList<>();
		for (String lib : values.getOrDefault("lib", "").split("\\|")) {
			if (!lib.isBlank()) {
				libs.add(lib.trim());
			}
		}
		String rserve = values.get("rserve");
		boolean hasRserve = !(rserve == null || rserve.isEmpty() || rserve.equals("NA"));
		String load = values.get("rserveLoad");
		String loadError = !hasRserve || load == null || load.equals("OK") ? null : load;
		String userLib = values.get("userLib");
		return new RInstallation(values.getOrDefault("home", home), version, values.get("arch"), values.get("platform"), libs,
				userLib == null || userLib.isEmpty() || userLib.equals("NA") ? null : userLib, hasRserve ? rserve : null, values.get("rserveBuilt"),
				values.get("rserveLib"), loadError, null);
	}

	/**
	 * Finds the R installations of the computer: the preferred path first (e.g.,
	 * the path of the preferences), then R_HOME, the R of the PATH, the registry
	 * (Windows) and the usual folders. Each R is listed once (by its home).
	 *
	 * @param preferredHome the path of the preferences or null
	 * @return the installations which could be started
	 */
	public static List<RInstallation> find(String preferredHome) {
		Map<String, RInstallation> found = new LinkedHashMap<>();
		for (String candidate : candidates(preferredHome)) {
			RInstallation r = probe(candidate);
			if (r.isValid()) {
				String key = canonical(r.home());
				found.putIfAbsent(key, r);
			}
		}
		return new ArrayList<>(found.values());
	}

	/**
	 * The folders which can contain R (not tested, see probe): the preferred path,
	 * R_HOME, the PATH (the empty entry) and the usual folders of the system.
	 */
	public static List<String> candidates(String preferredHome) {
		Set<String> list = new LinkedHashSet<>();
		if (preferredHome != null && !preferredHome.isBlank()) {
			list.add(preferredHome.trim());
		}
		String env = System.getenv("R_HOME");
		if (env != null && !env.isBlank()) {
			list.add(env.trim());
		}
		/* The R of the PATH */
		list.add("");
		String os = os();
		if (os.equals("Windows")) {
			list.addAll(windowsRegistry());
			for (String variable : new String[] { "ProgramFiles", "ProgramW6432", "LOCALAPPDATA" }) {
				String base = System.getenv(variable);
				if (base != null) {
					addVersions(list, new File(base, variable.equals("LOCALAPPDATA") ? "Programs/R" : "R"), "");
				}
			}
		} else if (os.equals("Mac")) {
			list.add("/Library/Frameworks/R.framework/Resources");
			/* The installed versions (e.g., of rig: 4.6-arm64) */
			addVersions(list, new File("/Library/Frameworks/R.framework/Versions"), "Resources");
			list.add("/opt/homebrew/lib/R");
			list.add("/opt/homebrew/opt/r/lib/R");
			list.add("/usr/local/lib/R");
			list.add("/usr/local/opt/r/lib/R");
		} else {
			list.add("/usr/lib/R");
			list.add("/usr/lib64/R");
			list.add("/usr/local/lib/R");
			list.add("/usr/local/lib64/R");
			/* The versions of rig and of the Posit builds: /opt/R/<version>/lib/R */
			addVersions(list, new File("/opt/R"), "lib/R");
		}
		/* Only the folders which exist (the empty entry is the PATH) */
		List<String> existing = new ArrayList<>();
		for (String c : list) {
			if (c.isEmpty() || new File(c).isDirectory()) {
				existing.add(c);
			}
		}
		return existing;
	}

	/* The subfolders of a folder (newest name first), each with a relative path */
	private static void addVersions(Set<String> list, File parent, String relative) {
		File[] versions = parent.listFiles(f -> f.isDirectory() && !f.getName().equals("Current"));
		if (versions == null) {
			return;
		}
		Arrays.sort(versions, Comparator.comparing(File::getName).reversed());
		for (File v : versions) {
			list.add((relative.isEmpty() ? v : new File(v, relative)).getAbsolutePath());
		}
	}

	/**
	 * Finds the home of R to start without starting R (only the files are tested):
	 * the preferred path if it contains R, else the first candidate which contains
	 * R (see candidates).
	 *
	 * @param preferredHome the path of the preferences or null
	 * @return the home, the empty string for the R of the PATH, or null if no R
	 *         was found
	 */
	public static String resolveHome(String preferredHome) {
		for (String candidate : candidates(preferredHome)) {
			if (consoleExecutableOf(candidate) != null) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * The executable of R for the console of Bio7 (Rterm.exe on Windows, else R)
	 * of a home, or null if it doesn't exist. For the empty home the executable of
	 * the PATH is returned (if found).
	 */
	public static String consoleExecutableOf(String home) {
		boolean windows = os().equals("Windows");
		if (home == null || home.isEmpty()) {
			String exe = windows ? "Rterm.exe" : "R";
			return which(exe) != null ? exe : null;
		}
		String[] relative = windows ? new String[] { "bin/x64/Rterm.exe", "bin/Rterm.exe" } : new String[] { "bin/R" };
		for (String r : relative) {
			File file = new File(home, r);
			if (file.isFile()) {
				return file.getAbsolutePath();
			}
		}
		return null;
	}

	/**
	 * The Rscript of a home (the Rscript of the PATH for an empty home) or null if
	 * it doesn't exist.
	 */
	public static String rscript(String home) {
		boolean windows = os().equals("Windows");
		if (home == null || home.isEmpty()) {
			String exe = windows ? "Rscript.exe" : "Rscript";
			return which(exe) != null ? exe : null;
		}
		String[] relative = windows ? new String[] { "bin/x64/Rscript.exe", "bin/Rscript.exe" } : new String[] { "bin/Rscript" };
		for (String r : relative) {
			File file = new File(home, r);
			if (file.isFile()) {
				return file.getAbsolutePath();
			}
		}
		return null;
	}

	/** Finds an executable in the PATH (the absolute path) or returns null. */
	public static String which(String executable) {
		String path = System.getenv("PATH");
		if (path == null) {
			return null;
		}
		for (String dir : path.split(File.pathSeparator)) {
			if (dir.isBlank()) {
				continue;
			}
			File file = new File(dir.trim(), executable);
			if (file.isFile() && file.canExecute()) {
				return file.getAbsolutePath();
			}
		}
		return null;
	}

	/**
	 * Checks if Rserve can be compiled with this R: the Xcode command line tools on
	 * macOS, else the C compiler of R (R CMD config CC) and make (Rtools on
	 * Windows).
	 */
	public static BuildTools checkBuildTools(RInstallation r) {
		String os = r.os();
		if (os.equals("mac")) {
			ProcessOutput out = run(List.of("xcode-select", "-p"), 20);
			if (out.error() == null && out.exitCode() == 0 && !out.output().isBlank()) {
				return new BuildTools(true, "Xcode command line tools in " + out.output().strip(), "");
			}
			return new BuildTools(false, "The Xcode command line tools are not installed.",
					"Install them with the shell command: xcode-select --install");
		}
		String rscript = rscript(r.home());
		if (rscript == null) {
			return new BuildTools(false, "Rscript was not found.", "");
		}
		String script = "cat('" + MARKER + "\\n');" + "cc <- tryCatch(system2(file.path(R.home('bin'), 'R'), c('CMD', 'config', 'CC'), stdout=TRUE)[1], error=function(e) '');"
				+ "exe <- strsplit(trimws(cc), ' +')[[1]][1];" + "cat('cc=', cc, '\\n', sep='');"
				+ "cat('ccPath=', if (is.na(exe) || !nzchar(exe)) '' else Sys.which(exe), '\\n', sep='');"
				+ "cat('make=', Sys.which(Sys.getenv('MAKE', 'make')), '\\n', sep='')";
		ProcessOutput out = run(List.of(rscript, "--no-save", "--no-restore", "-e", script), 60);
		if (out.error() != null) {
			return new BuildTools(false, out.error(), "");
		}
		Map<String, String> values = new LinkedHashMap<>();
		int start = out.output().indexOf(MARKER);
		for (String line : (start < 0 ? "" : out.output().substring(start)).split("\\R")) {
			int eq = line.indexOf('=');
			if (eq > 0) {
				values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
			}
		}
		String cc = values.getOrDefault("ccPath", "");
		String make = values.getOrDefault("make", "");
		if (!cc.isEmpty() && !make.isEmpty()) {
			return new BuildTools(true, "Compiler " + cc + ", make " + make, "");
		}
		String missing = (cc.isEmpty() ? "the C compiler (" + values.getOrDefault("cc", "?") + ")" : "") + (cc.isEmpty() && make.isEmpty() ? " and " : "")
				+ (make.isEmpty() ? "make" : "");
		if (os.equals("windows")) {
			return new BuildTools(false, "Not found: " + missing + ".", "Install Rtools " + rtoolsVersion(r) + " for R " + r.minorVersion()
					+ " from https://cran.r-project.org/bin/windows/Rtools/ and start Bio7 again.");
		}
		return new BuildTools(false, "Not found: " + missing + ".",
				"Install the build tools, e.g., Debian/Ubuntu: sudo apt install build-essential r-base-dev, Fedora: sudo dnf install gcc make R-devel");
	}

	/* The Rtools version of an R version on Windows (e.g., R 4.5 -> 45) */
	private static String rtoolsVersion(RInstallation r) {
		String minor = r.minorVersion();
		return minor == null ? "" : minor.replace(".", "");
	}

	/** The minor version of a version (e.g., 4.6.0 -> 4.6). */
	public static String minor(String v) {
		String[] parts = v.split("\\.");
		return parts.length >= 2 ? parts[0] + "." + parts[1] : v;
	}

	/** Normalizes an architecture (arm64 -> aarch64, amd64 -> x86_64). */
	public static String normalizeArch(String arch) {
		if (arch == null) {
			return "";
		}
		String a = arch.toLowerCase(Locale.ROOT);
		if (a.equals("arm64")) {
			return "aarch64";
		}
		if (a.equals("amd64") || a.equals("x64") || a.equals("x86-64")) {
			return "x86_64";
		}
		return a;
	}

	/**
	 * The output of a process.
	 *
	 * @param output   the output (stdout and stderr)
	 * @param exitCode the exit code (-1 if the process could not be run)
	 * @param error    the error if the process could not be run or timed out
	 */
	record ProcessOutput(String output, int exitCode, String error) {
	}

	/* Runs a short process (no input) and returns its output */
	static ProcessOutput run(List<String> command, int timeoutSeconds) {
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.redirectErrorStream(true);
			process = builder.start();
			process.getOutputStream().close();
			final Process p = process;
			final StringBuilder out = new StringBuilder();
			Thread reader = new Thread(() -> {
				try {
					String text = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
					synchronized (out) {
						out.append(text);
					}
				} catch (IOException e) {
					/* The process was stopped */
				}
			}, "Bio7 R Probe");
			reader.setDaemon(true);
			reader.start();
			if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return new ProcessOutput("", -1, command.get(0) + " did not answer within " + timeoutSeconds + " seconds");
			}
			reader.join(2000);
			synchronized (out) {
				return new ProcessOutput(out.toString(), process.exitValue(), null);
			}
		} catch (IOException e) {
			return new ProcessOutput("", -1, e.getMessage());
		} catch (InterruptedException e) {
			if (process != null) {
				process.destroyForcibly();
			}
			Thread.currentThread().interrupt();
			return new ProcessOutput("", -1, "interrupted");
		}
	}

	/* The install paths of R in the registry (the machine and the user, all versions) */
	private static List<String> windowsRegistry() {
		List<String> paths = new ArrayList<>();
		try {
			com.sun.jna.platform.win32.WinReg.HKEY[] roots = { com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE,
					com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER };
			for (com.sun.jna.platform.win32.WinReg.HKEY root : roots) {
				for (String key : new String[] { "SOFTWARE\\R-core\\R64", "SOFTWARE\\R-core\\R" }) {
					if (!com.sun.jna.platform.win32.Advapi32Util.registryKeyExists(root, key)) {
						continue;
					}
					/* The current version, then the installed versions (the subkeys) */
					addRegistryPath(paths, root, key);
					for (String version : com.sun.jna.platform.win32.Advapi32Util.registryGetKeys(root, key)) {
						addRegistryPath(paths, root, key + "\\" + version);
					}
				}
			}
		} catch (RuntimeException | LinkageError e) {
			/* No registry (not Windows) or no access */
		}
		return paths;
	}

	private static void addRegistryPath(List<String> paths, com.sun.jna.platform.win32.WinReg.HKEY root, String key) {
		try {
			if (com.sun.jna.platform.win32.Advapi32Util.registryValueExists(root, key, "InstallPath")) {
				String path = com.sun.jna.platform.win32.Advapi32Util.registryGetStringValue(root, key, "InstallPath");
				if (path != null && !path.isBlank() && !paths.contains(path)) {
					paths.add(path);
				}
			}
		} catch (RuntimeException e) {
			/* The value can't be read */
		}
	}

	private static String canonical(String path) {
		try {
			return new File(path).getCanonicalPath();
		} catch (IOException e) {
			return path;
		}
	}

	static String os() {
		String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return name.contains("win") ? "Windows" : name.contains("mac") ? "Mac" : "Linux";
	}
}
