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
 * folder of the preferences, on the PATH, in the registry of Windows (PEP 514),
 * in the environments of conda (~/.conda/environments.txt) and in the usual
 * installation folders (python.org, Python install manager, Homebrew, pyenv,
 * uv, conda, Microsoft Store, Linux packages).
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
		File dir = executableFolder(folder);
		if (dir == null) {
			return null;
		}
		for (String n : names()) {
			File f = new File(dir, n);
			if (usable(f)) {
				return f.getAbsolutePath();
			}
		}
		return null;
	}

	/*
	 * The folder of the executable: the folder or a subfolder of the installation
	 * (e.g., ~/anaconda3: its bin folder). Null if none.
	 */
	private static File executableFolder(String folder) {
		if (folder == null || folder.isBlank()) {
			return null;
		}
		for (String sub : new String[] { "", "bin", "Scripts" }) {
			File dir = sub.isEmpty() ? new File(folder) : new File(folder, sub);
			for (String n : names()) {
				if (usable(new File(dir, n))) {
					return dir;
				}
			}
		}
		return null;
	}

	private static boolean usable(File f) {
		return f.isFile() && f.canExecute() && !isAppStoreStub(f) && !isMacStub(f);
	}

	/*
	 * The executables of the folder: python3 (or python), then the versioned
	 * executables on Linux and macOS (e.g., python3.12 of deadsnakes or Homebrew),
	 * newest first
	 */
	private static List<String> executablesIn(String folder) {
		List<String> list = new ArrayList<>();
		String exe = executableIn(folder);
		if (exe == null) {
			return list;
		}
		list.add(exe);
		if (!windows()) {
			File[] versioned = new File(exe).getParentFile().listFiles(f -> f.getName().matches("python3\\.\\d+") && usable(f));
			if (versioned != null) {
				java.util.Arrays.sort(versioned, (a, b) -> compareVersions(b.getName(), a.getName()));
				for (File f : versioned) {
					list.add(f.getAbsolutePath());
				}
			}
		}
		return list;
	}

	/**
	 * True if the interpreter is the one Bio7 starts for its folder (the
	 * preferences store a folder: a versioned executable such as python3.12 in
	 * /usr/bin can only be used to create a Bio7 environment).
	 */
	public static boolean selectableAsFolder(Found found) {
		return found.executable().equals(executableIn(found.folder()));
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
		/* Python may have been installed from the Microsoft Store in the meantime */
		STORE_STUBS.clear();
		Map<String, Found> found = new LinkedHashMap<>();
		for (String dir : candidateFolders()) {
			for (String exe : executablesIn(dir)) {
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
					/* The folder of the executable (the preferences store it) */
					found.put(key, new Found(new File(exe).getParent(), exe, version));
				}
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
				/* Windows allows quoted entries (e.g., "C:\Program Files\Python312") */
				d = d.trim().replace("\"", "");
				/* The system folders last (e.g., the old Python of macOS in /usr/bin) */
				if (!d.isBlank() && (windows() || !(d.equals("/usr/bin") || d.equals("/bin")))) {
					dirs.add(d);
				}
			}
		}
		/* The installations and environments of conda (in any folder, e.g., C:/Users/Public/anaconda3) */
		try {
			for (String line : java.nio.file.Files.readAllLines(new File(home, ".conda" + File.separator + "environments.txt").toPath(), StandardCharsets.UTF_8)) {
				if (!line.isBlank()) {
					dirs.add(line.trim());
				}
			}
		} catch (IOException | RuntimeException e) {
			/* No conda */
		}
		if (windows()) {
			/* The installations registered by the installers (PEP 514), also in custom folders */
			dirs.addAll(windowsRegistry());
			String local = System.getenv("LOCALAPPDATA");
			if (local != null) {
				/* The Python install manager (python.org, since Python 3.14) */
				addVersions(dirs, new File(local, "Python"), "pythoncore-3", "");
				dirs.add(new File(local, "Python\\bin").getPath());
				/* The installer of python.org (until Python 3.15) */
				addVersions(dirs, new File(local, "Programs\\Python"), "Python3", "");
			}
			/* The Pythons installed by uv */
			String roaming = System.getenv("APPDATA");
			if (roaming != null) {
				addVersions(dirs, new File(roaming, "uv\\python"), "cpython-3", "");
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
				/* The installation for all users of Anaconda */
				String pub = System.getenv("PUBLIC");
				dirs.add(new File(pub != null ? pub : "C:\\Users\\Public", c).getPath());
			}
			/* pyenv-win: its shims are .bat files, so the installed versions */
			String pyenv = System.getenv("PYENV_ROOT");
			if (pyenv != null) {
				addVersions(dirs, new File(pyenv, "versions"), "3", "");
			}
			addVersions(dirs, new File(home, ".pyenv\\pyenv-win\\versions"), "3", "");
		} else {
			if (mac()) {
				dirs.add("/opt/homebrew/bin");
				addVersions(dirs, new File("/Library/Frameworks/Python.framework/Versions"), "3", "bin");
				addVersions(dirs, new File(home, "Library/Frameworks/Python.framework/Versions"), "3", "bin");
			} else {
				/* Homebrew on Linux */
				dirs.add("/home/linuxbrew/.linuxbrew/bin");
				dirs.add(new File(home, ".linuxbrew/bin").getPath());
			}
			dirs.add("/usr/local/bin");
			dirs.add(new File(home, ".pyenv/shims").getPath());
			/* The versions of pyenv (the shims start the version of the current folder only) */
			String pyenv = System.getenv("PYENV_ROOT");
			addVersions(dirs, new File(pyenv != null ? pyenv : new File(home, ".pyenv").getPath(), "versions"), "3", "bin");
			dirs.add(new File(home, ".local/bin").getPath());
			/* The Pythons installed by uv (e.g., cpython-3.12.4-linux-x86_64-gnu) */
			addVersions(dirs, new File(home, ".local/share/uv/python"), "cpython-3", "bin");
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

	private static final java.util.regex.Pattern VERSION = java.util.regex.Pattern.compile("\\d+(\\.\\d+)*");

	/*
	 * Compares names by their first version number (Python39 < Python312, 3.9 <
	 * 3.12, pythoncore-3.9-64 < pythoncore-3.14-64, cpython-3.12.4-linux-x86_64-gnu);
	 * 32-bit installations (Python312-32, pythoncore-3.12-32) are older than the
	 * 64-bit ones of the same version
	 */
	private static int compareVersions(String a, String b) {
		int[] x = versionNumbers(a);
		int[] y = versionNumbers(b);
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int u = i < x.length ? x[i] : 0;
			int v = i < y.length ? y[i] : 0;
			if (u != v) {
				return Integer.compare(u, v);
			}
		}
		return Boolean.compare(!a.endsWith("-32"), !b.endsWith("-32"));
	}

	private static int[] versionNumbers(String name) {
		java.util.regex.Matcher m = VERSION.matcher(name);
		if (!m.find()) {
			return new int[0];
		}
		String[] parts = m.group().split("\\.");
		if (parts.length == 1 && parts[0].length() > 1) {
			/* Python39 / Python312: major digit, then the minor number */
			parts = new String[] { parts[0].substring(0, 1), parts[0].substring(1) };
		}
		int[] numbers = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			try {
				numbers[i] = Integer.parseInt(parts[i]);
			} catch (NumberFormatException e) {
				numbers[i] = 0;
			}
		}
		return numbers;
	}

	/* The installations in the registry (PEP 514): HKCU, HKLM (64 and 32 bit) */
	private static List<String> windowsRegistry() {
		List<String> folders = new ArrayList<>();
		try {
			com.sun.jna.platform.win32.WinReg.HKEY cu = com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
			com.sun.jna.platform.win32.WinReg.HKEY lm = com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;
			addRegistry(folders, cu, "SOFTWARE\\Python");
			addRegistry(folders, lm, "SOFTWARE\\Python");
			addRegistry(folders, lm, "SOFTWARE\\WOW6432Node\\Python");
		} catch (RuntimeException | LinkageError e) {
			/* No registry or no access */
		}
		return folders;
	}

	/* Software\Python\<company>\<tag>\InstallPath: ExecutablePath or the default value (the folder) */
	private static void addRegistry(List<String> folders, com.sun.jna.platform.win32.WinReg.HKEY root, String key) {
		if (!com.sun.jna.platform.win32.Advapi32Util.registryKeyExists(root, key)) {
			return;
		}
		for (String company : com.sun.jna.platform.win32.Advapi32Util.registryGetKeys(root, key)) {
			/* The py launcher registers itself, it isn't an interpreter */
			if (company.equalsIgnoreCase("PyLauncher")) {
				continue;
			}
			String[] tags;
			try {
				tags = com.sun.jna.platform.win32.Advapi32Util.registryGetKeys(root, key + "\\" + company);
			} catch (RuntimeException e) {
				continue;
			}
			/* Newest first */
			java.util.Arrays.sort(tags, (a, b) -> compareVersions(b, a));
			for (String tag : tags) {
				String install = key + "\\" + company + "\\" + tag + "\\InstallPath";
				try {
					if (!com.sun.jna.platform.win32.Advapi32Util.registryKeyExists(root, install)) {
						continue;
					}
					String folder = null;
					if (com.sun.jna.platform.win32.Advapi32Util.registryValueExists(root, install, "ExecutablePath")) {
						String exe = com.sun.jna.platform.win32.Advapi32Util.registryGetStringValue(root, install, "ExecutablePath");
						folder = exe == null ? null : new File(exe).getParent();
					}
					if (folder == null && com.sun.jna.platform.win32.Advapi32Util.registryValueExists(root, install, "")) {
						folder = com.sun.jna.platform.win32.Advapi32Util.registryGetStringValue(root, install, "");
					}
					if (folder != null && !folder.isBlank()) {
						folders.add(folder);
					}
				} catch (RuntimeException e) {
					/* The value can't be read */
				}
			}
		}
	}

	/* The results of the Windows Store aliases (cleared by detect()) */
	private static final Map<String, Boolean> STORE_STUBS = new java.util.concurrent.ConcurrentHashMap<>();

	/*
	 * The Windows Store aliases (WindowsApps\python.exe) are empty files. If
	 * Python of the Microsoft Store is installed they start it, else they ask to
	 * install it (exit code 9009).
	 */
	private static boolean isAppStoreStub(File f) {
		if (!windows() || f.length() != 0 || !f.getPath().toLowerCase().contains("windowsapps")) {
			return false;
		}
		return STORE_STUBS.computeIfAbsent(f.getAbsolutePath(), p -> version(p) == null);
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
