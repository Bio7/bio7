/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.console;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Finds the Python interpreter (CPython 3) of the operating system: in the
 * folder of the preferences, on the PATH and in the usual installation folders
 * (python.org, Homebrew, pyenv, conda, Windows Store, Linux packages).
 */
public final class PythonFinder {

	/** A found interpreter: its folder, executable and version (e.g., "Python 3.12.4"). */
	public record Found(String folder, String executable, String version) {
		@Override
		public String toString() {
			return version + "  -  " + folder;
		}
	}

	private PythonFinder() {
	}

	private static boolean windows() {
		return System.getProperty("os.name", "").toLowerCase().startsWith("windows");
	}

	private static boolean mac() {
		return System.getProperty("os.name", "").toLowerCase().startsWith("mac");
	}

	/* The names of the executable (python3 first: python may be Python 2 on Linux and macOS) */
	private static String[] names() {
		return windows() ? new String[] { "python.exe", "python3.exe" } : new String[] { "python3", "python" };
	}

	/** The executable in the folder (null if none). */
	public static String executableIn(String folder) {
		if (folder == null || folder.isBlank()) {
			return null;
		}
		/* The folder of the executable or the folder of the installation (e.g., ~/anaconda3: its bin folder) */
		for (String sub : new String[] { "", "bin", "Scripts" }) {
			File dir = sub.isEmpty() ? new File(folder) : new File(folder, sub);
			for (String n : names()) {
				File f = new File(dir, n);
				if (f.isFile() && f.canExecute() && !isAppStoreAlias(f) && !isMacStub(f)) {
					return f.getAbsolutePath();
				}
			}
		}
		return null;
	}

	/**
	 * The executable to start: in the folder of the preferences, else the first
	 * found (PATH, installation folders), else "python3"/"python" (the PATH of the
	 * process).
	 */
	public static String executable(String folder) {
		String exe = executableIn(folder);
		if (exe != null) {
			return exe;
		}
		for (String dir : candidateFolders()) {
			exe = executableIn(dir);
			if (exe != null) {
				return exe;
			}
		}
		return windows() ? "python" : "python3";
	}

	/** Detects the interpreters (runs each with --version). */
	public static List<Found> detect() {
		Map<String, Found> found = new LinkedHashMap<>();
		for (String dir : candidateFolders()) {
			String exe = executableIn(dir);
			if (exe == null) {
				continue;
			}
			String key;
			try {
				key = new File(exe).getCanonicalPath();
			} catch (IOException e) {
				key = exe;
			}
			if (found.containsKey(key)) {
				continue;
			}
			String version = version(exe);
			if (version != null && version.startsWith("Python 3")) {
				found.put(key, new Found(dir, exe, version));
			}
		}
		return new ArrayList<>(found.values());
	}

	/* The output of python --version (null if it can't be run) */
	private static String version(String exe) {
		try {
			Process p = new ProcessBuilder(exe, "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return null;
			}
			String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			return p.exitValue() == 0 && out.startsWith("Python") ? out : null;
		} catch (IOException | InterruptedException e) {
			return null;
		}
	}

	/* The folders to search: the PATH, then the usual installation folders of the OS */
	private static List<String> candidateFolders() {
		Set<String> dirs = new LinkedHashSet<>();
		String home = System.getProperty("user.home");
		/* The virtual environment or conda environment Bio7 was started from */
		String venv = System.getenv("VIRTUAL_ENV");
		if (venv != null) {
			dirs.add(new File(venv, windows() ? "Scripts" : "bin").getPath());
		}
		String conda = System.getenv("CONDA_PREFIX");
		if (conda != null) {
			dirs.add(windows() ? conda : new File(conda, "bin").getPath());
		}
		String path = System.getenv("PATH");
		if (path == null) {
			path = System.getenv("Path");
		}
		if (path != null) {
			for (String d : path.split(File.pathSeparator)) {
				/* The system folders last (e.g., the old Python of macOS in /usr/bin) */
				if (!d.isBlank() && (windows() || !(d.equals("/usr/bin") || d.equals("/bin")))) {
					dirs.add(d);
				}
			}
		}
		if (windows()) {
			String local = System.getenv("LOCALAPPDATA");
			if (local != null) {
				addVersions(dirs, new File(local, "Programs\\Python"), "Python3", "");
			}
			for (String pf : new String[] { System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"), "C:\\" }) {
				if (pf != null) {
					addVersions(dirs, new File(pf), "Python3", "");
					addVersions(dirs, new File(pf, "Python"), "Python3", "");
				}
			}
			for (String c : new String[] { "miniconda3", "anaconda3", "miniforge3", "mambaforge" }) {
				dirs.add(new File(home, c).getPath());
				if (local != null) {
					dirs.add(new File(local, c).getPath());
				}
				dirs.add("C:\\ProgramData\\" + c);
			}
			dirs.add(new File(home, ".pyenv\\pyenv-win\\shims").getPath());
		} else {
			if (mac()) {
				dirs.add("/opt/homebrew/bin");
				addVersions(dirs, new File("/Library/Frameworks/Python.framework/Versions"), "3", "bin");
				addVersions(dirs, new File(home, "Library/Frameworks/Python.framework/Versions"), "3", "bin");
			}
			dirs.add("/usr/local/bin");
			dirs.add(new File(home, ".pyenv/shims").getPath());
			dirs.add(new File(home, ".local/bin").getPath());
			for (String c : new String[] { "miniconda3", "anaconda3", "miniforge3", "mambaforge", "opt/miniconda3", "opt/anaconda3" }) {
				dirs.add(new File(home, c + "/bin").getPath());
			}
			dirs.add("/opt/conda/bin");
			dirs.add("/usr/bin");
			dirs.add("/bin");
		}
		return new ArrayList<>(dirs);
	}

	/* The version folders (e.g., Python312, 3.12) of a folder, newest first */
	private static void addVersions(Set<String> dirs, File parent, String prefix, String sub) {
		File[] files = parent.listFiles(f -> f.isDirectory() && f.getName().startsWith(prefix));
		if (files == null) {
			return;
		}
		java.util.Arrays.sort(files, (a, b) -> compareVersions(b.getName(), a.getName()));
		for (File f : files) {
			dirs.add(sub.isEmpty() ? f.getPath() : new File(f, sub).getPath());
		}
	}

	/* Compares names with numbers (Python39 < Python312, 3.9 < 3.12) */
	private static int compareVersions(String a, String b) {
		String[] x = a.replaceAll("[^0-9.]", "").split("\\.");
		String[] y = b.replaceAll("[^0-9.]", "").split("\\.");
		if (x.length == 1 && y.length == 1 && x[0].length() > 1 && y[0].length() > 1) {
			/* Python39 / Python312: major digit, then the minor number */
			x = new String[] { x[0].substring(0, 1), x[0].substring(1) };
			y = new String[] { y[0].substring(0, 1), y[0].substring(1) };
		}
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int u = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
			int v = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
			if (u != v) {
				return Integer.compare(u, v);
			}
		}
		return 0;
	}

	/* The Windows Store alias (WindowsApps\python.exe) opens the store if Python isn't installed */
	private static boolean isAppStoreAlias(File f) {
		return windows() && f.getPath().toLowerCase().contains("windowsapps") && f.length() == 0;
	}

	/* /usr/bin/python3 of macOS asks to install the developer tools if they are missing */
	private static boolean isMacStub(File f) {
		if (!mac() || !f.getPath().equals("/usr/bin/python3")) {
			return false;
		}
		return !new File("/Library/Developer/CommandLineTools/usr/bin/python3").exists()
				&& !new File("/Applications/Xcode.app/Contents/Developer/usr/bin/python3").exists();
	}
}
