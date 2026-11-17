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
package com.eco.bio7.pybridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jface.preference.IPreferenceStore;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.console.PythonFinder;
import com.eco.bio7.pyffm.PySession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Python of Bio7 and its packages: the kind of the environment (venv,
 * conda, system), pip (always in a separate process, never in the embedded
 * Python), a Bio7 environment (venv) and the package information of PyPI.
 */
public final class PythonEnvironment {

	private static final ObjectMapper JSON = new ObjectMapper();

	/** The default folder of the Bio7 environment. */
	public static final String DEFAULT_ENV = new File(System.getProperty("user.home"), ".bio7" + File.separator + "python-env").getPath();

	/**
	 * The information of a Python.
	 *
	 * @param executable        the executable
	 * @param version           the version (e.g., 3.12.4)
	 * @param kind              "venv", "conda" or "system"
	 * @param externallyManaged true if the system manages the packages (PEP 668:
	 *                          pip refuses to install, e.g., Homebrew, Linux)
	 * @param pip               the version of pip or null if pip is missing
	 */
	public record Info(String executable, String version, String kind, boolean externallyManaged, String pip) {
		/** Returns true if pip can install packages into this Python. */
		public boolean canInstall() {
			return hasInstaller() && !externallyManaged;
		}

		/** Returns true if the packages can be installed: pip of the Python, or uv (preferences). */
		public boolean hasInstaller() {
			return pip != null || useUv();
		}
	}

	/** A package: name, installed version, latest version (null if unknown or current). */
	public record Package(String name, String version, String latest) {
	}

	private PythonEnvironment() {
	}

	/** The Python of Bio7: the running embedded Python, else the one of the preferences (null if none). */
	public static String executable() {
		PySession s = EmbeddedPython.session();
		if (s != null) {
			return s.executable().toString();
		}
		return preferenceExecutable();
	}

	/** The Python of the preferences (also if another Python is running in Bio7), null if none. */
	public static String preferenceExecutable() {
		Bio7Plugin plugin = Bio7Plugin.getDefault();
		String exe = PythonFinder.executable(plugin == null ? "" : plugin.getPreferenceStore().getString("python_pipe_path"));
		return new File(exe).isAbsolute() ? exe : null;
	}

	private static final String PROBE = """
			import sys, os, sysconfig, json
			stdlib = sysconfig.get_path("stdlib")
			venv = sys.prefix != sys.base_prefix
			conda = os.path.exists(os.path.join(sys.prefix, "conda-meta"))
			managed = (not venv) and os.path.exists(os.path.join(stdlib, "EXTERNALLY-MANAGED"))
			try:
			    import pip
			    pipv = pip.__version__
			except Exception:
			    pipv = None
			print("BIO7ENV" + json.dumps({"version": "%d.%d.%d" % sys.version_info[:3], "kind": "venv" if venv else ("conda" if conda else "system"),
			                              "managed": managed, "pip": pipv}))
			""";

	/** Probes a Python (runs it once). */
	public static Info info(String executable) throws IOException {
		Result r = runCode(executable, PROBE, null);
		int at = r.output.indexOf("BIO7ENV");
		if (r.exit != 0 || at < 0) {
			throw new IOException("Python could not be started: " + executable + "\n" + r.output.strip());
		}
		JsonNode n = JSON.readTree(r.output.substring(at + 7).strip());
		return new Info(executable, n.path("version").asText(), n.path("kind").asText(), n.path("managed").asBoolean(), n.path("pip").isNull() ? null : n.path("pip").asText());
	}

	/** The installed packages (sorted by name). */
	public static List<Package> list(String executable) throws IOException {
		Result r = pip(executable, List.of("list", "--format=json", "--disable-pip-version-check"), null, null);
		if (r.exit != 0) {
			throw new IOException(r.output.strip());
		}
		Map<String, Package> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (JsonNode n : JSON.readTree(jsonPart(r.output))) {
			map.put(n.path("name").asText(), new Package(n.path("name").asText(), n.path("version").asText(), null));
		}
		return new ArrayList<>(map.values());
	}

	/** The normalized name of a package (PEP 503: lower case, - for _ and .). */
	public static String normalize(String name) {
		return name.toLowerCase(java.util.Locale.ROOT).replaceAll("[-_.]+", "-");
	}

	/**
	 * The packages which an install would install or change (normalized name:
	 * version), resolved by pip without installing (pip install --dry-run
	 * --report, pip 22.2 or later). Null if pip can't do it (old pip, no
	 * resolution).
	 *
	 * @param installArgs the arguments after "install"
	 */
	public static Map<String, String> dryRun(String executable, List<String> installArgs, Process[] cancel) throws IOException {
		File report = File.createTempFile("bio7pip", ".json");
		try {
			List<String> cmd = new ArrayList<>(List.of(executable, "-m", "pip", "install", "--dry-run", "--quiet", "--report", report.getAbsolutePath()));
			cmd.addAll(installArgs);
			Result r = run(cmd, null, cancel);
			if (r.exit != 0 || report.length() == 0) {
				return null;
			}
			Map<String, String> plan = new TreeMap<>();
			for (JsonNode n : JSON.readTree(report).path("install")) {
				JsonNode meta = n.path("metadata");
				if (!meta.path("name").asText().isEmpty()) {
					plan.put(normalize(meta.path("name").asText()), meta.path("version").asText());
				}
			}
			return plan;
		} finally {
			report.delete();
		}
	}

	/** The packages with a newer version (name: latest version); needs the internet. */
	public static Map<String, String> outdated(String executable) throws IOException {
		Result r = pip(executable, List.of("list", "--outdated", "--format=json", "--disable-pip-version-check"), null, null);
		if (r.exit != 0) {
			throw new IOException(r.output.strip());
		}
		Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (JsonNode n : JSON.readTree(jsonPart(r.output))) {
			map.put(n.path("name").asText(), n.path("latest_version").asText());
		}
		return map;
	}

	/* The JSON array of the output (pip may print warnings before it) */
	private static String jsonPart(String output) {
		int start = output.indexOf('[');
		int end = output.lastIndexOf(']');
		return start >= 0 && end > start ? output.substring(start, end + 1) : "[]";
	}

	/** The summary, latest version and needed Python of a package on PyPI (null if not found). */
	public static String[] pypiInfo(String name) throws IOException, InterruptedException {
		String base = name.split("[<>=!~\\[; ]", 2)[0].strip();
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://pypi.org/pypi/" + URLEncoder.encode(base, StandardCharsets.UTF_8) + "/json"))
				.timeout(Duration.ofSeconds(20)).header("Accept", "application/json").GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() == 404) {
			return null;
		}
		if (response.statusCode() != 200) {
			throw new IOException("PyPI: HTTP " + response.statusCode());
		}
		JsonNode info = JSON.readTree(response.body()).path("info");
		return new String[] { info.path("name").asText(base), info.path("version").asText(), info.path("summary").asText(""), info.path("requires_python").asText("") };
	}

	/* ---------------- Processes ---------------- */

	/** The result of a process: the exit code and the output. */
	public record Result(int exit, String output) {
	}

	/**
	 * Runs a process (the output line by line to the consumer, if any).
	 *
	 * @param cancel a holder for the process (to cancel it), may be null
	 */
	public static Result run(List<String> command, Consumer<String> lines, Process[] cancel) throws IOException {
		return run(command, null, lines, cancel);
	}

	/**
	 * Runs Python code. The code is read from stdin (python -): Windows removes
	 * the quotes of code given with -c (Java doesn't escape them on the command
	 * line).
	 *
	 * @param cancel a holder for the process (to cancel it), may be null
	 */
	public static Result runCode(String executable, String code, Process[] cancel) throws IOException {
		return run(List.of(executable, "-"), code, null, cancel);
	}

	private static Result run(List<String> command, String stdin, Consumer<String> lines, Process[] cancel) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
		pb.environment().put("PYTHONUNBUFFERED", "1");
		pb.environment().put("PIP_DISABLE_PIP_VERSION_CHECK", "1");
		pb.environment().put("PYTHONIOENCODING", "utf-8");
		/* The settings of another Python must not interfere */
		pb.environment().remove("PYTHONHOME");
		pb.environment().remove("PYTHONPATH");
		Process p = pb.start();
		if (cancel != null) {
			cancel[0] = p;
		}
		if (stdin != null) {
			try (java.io.OutputStream in = p.getOutputStream()) {
				in.write(stdin.getBytes(StandardCharsets.UTF_8));
			}
		}
		StringBuilder out = new StringBuilder();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = r.readLine()) != null) {
				out.append(line).append('\n');
				if (lines != null) {
					lines.accept(line);
				}
			}
		}
		try {
			if (!p.waitFor(10, TimeUnit.MINUTES)) {
				p.destroyForcibly();
			}
		} catch (InterruptedException e) {
			p.destroyForcibly();
			Thread.currentThread().interrupt();
		}
		return new Result(p.isAlive() ? -1 : p.exitValue(), out.toString());
	}

	/**
	 * pip with arguments (e.g., install numpy): the output line by line. With the
	 * preference 'use uv' (and uv installed): uv pip ... --python executable (the
	 * environment needs no pip).
	 */
	public static Result pip(String executable, List<String> args, Consumer<String> lines, Process[] cancel) throws IOException {
		String uv = useUv() ? uvExecutable() : null;
		if (uv != null && !args.isEmpty()) {
			List<String> cmd = new ArrayList<>(List.of(uv, "pip", args.get(0), "--python", executable));
			if (args.get(0).equals("freeze") || args.get(0).equals("list")) {
				/* Only the data: uv writes "Using Python ... environment at ..." (stderr is in the output, e.g., of requirements.txt) */
				cmd.add("--quiet");
			}
			for (String a : args.subList(1, args.size())) {
				/* Options of pip which uv doesn't know (uv uninstall doesn't ask) */
				if (a.equals("-y") || a.equals("--yes") || a.equals("--disable-pip-version-check")) {
					continue;
				}
				cmd.add(a);
			}
			return run(cmd, lines, cancel);
		}
		List<String> cmd = new ArrayList<>(List.of(executable, "-m", "pip"));
		cmd.addAll(args);
		return run(cmd, lines, cancel);
	}

	/* ---------------- uv ---------------- */

	/** The preference: packages are installed with uv instead of pip (default: pip). */
	public static final String PREF_USE_UV = "python_use_uv";

	/** Returns true if uv is selected in the preferences (also if it is not installed). */
	public static boolean uvSelected() {
		Bio7Plugin plugin = Bio7Plugin.getDefault();
		return plugin != null && plugin.getPreferenceStore().getBoolean(PREF_USE_UV);
	}

	/** Returns true if the packages are installed with uv: selected and installed. */
	public static boolean useUv() {
		return uvSelected() && uvExecutable() != null;
	}

	/** The command shown in the output: "uv pip" or "python -m pip". */
	public static String pipLabel() {
		return useUv() ? "uv pip" : "python -m pip";
	}

	/**
	 * The executable of uv (null if not found): the PATH, then the folders of its
	 * installers (~/.local/bin, ~/.cargo/bin, Homebrew).
	 */
	public static String uvExecutable() {
		boolean win = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).startsWith("windows");
		String name = win ? "uv.exe" : "uv";
		List<String> dirs = new ArrayList<>();
		String path = System.getenv("PATH");
		if (path == null) {
			path = System.getenv("Path");
		}
		if (path != null) {
			for (String d : path.split(File.pathSeparator)) {
				if (!d.isBlank()) {
					dirs.add(d.trim().replace("\"", ""));
				}
			}
		}
		String home = System.getProperty("user.home");
		dirs.add(new File(home, ".local" + File.separator + "bin").getPath());
		dirs.add(new File(home, ".cargo" + File.separator + "bin").getPath());
		if (!win) {
			dirs.add("/opt/homebrew/bin");
			dirs.add("/usr/local/bin");
			dirs.add("/home/linuxbrew/.linuxbrew/bin");
		}
		for (String d : dirs) {
			File f = new File(d, name);
			if (f.isFile() && f.canExecute()) {
				return f.getAbsolutePath();
			}
		}
		return null;
	}

	/**
	 * Creates a virtual environment (venv) with a base Python.
	 *
	 * @return the Python executable of the environment
	 */
	public static String createEnvironment(String baseExecutable, Path folder, Consumer<String> lines, Process[] cancel) throws IOException {
		Result r = run(List.of(baseExecutable, "-m", "venv", folder.toString()), lines, cancel);
		if (r.exit != 0 && r.output.contains("ensurepip")) {
			/*
			 * Debian/Ubuntu without the package python3-venv: venv can't install pip. The
			 * environment is created without pip, pip is installed afterwards (installPip).
			 */
			if (lines != null) {
				lines.accept("This Python can't install pip into a venv (ensurepip is missing): the environment is created without pip.");
			}
			r = run(List.of(baseExecutable, "-m", "venv", "--without-pip", folder.toString()), lines, cancel);
		}
		if (r.exit != 0) {
			throw new IOException("The environment could not be created:\n" + r.output.strip());
		}
		String exe = PythonFinder.executableIn(binFolder(folder).toString());
		if (exe == null) {
			throw new IOException("No Python in the new environment: " + binFolder(folder));
		}
		return exe;
	}

	/** Returns true if pip can be run with the Python (python -m pip). */
	public static boolean hasPip(String executable) {
		try {
			return run(List.of(executable, "-m", "pip", "--version"), null, null).exit == 0;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Installs pip into a Python without pip (e.g., a venv of Debian/Ubuntu without
	 * python3-venv, an environment of uv venv): ensurepip of the Python, else pip
	 * of the base Python of the venv (pip --python), else the installer of pip
	 * (get-pip.py of bootstrap.pypa.io) if the download is allowed.
	 *
	 * @param allowDownload asked before get-pip.py is downloaded (may be null: no
	 *                      download)
	 * @throws IOException with a hint (the packages of the system) if pip can't be
	 *                     installed
	 */
	public static void installPip(String executable, java.util.function.BooleanSupplier allowDownload, Consumer<String> lines, Process[] cancel) throws IOException {
		Consumer<String> log = lines == null ? s -> {
		} : lines;
		log.accept("\n$ python -m ensurepip --upgrade");
		Result r = run(List.of(executable, "-m", "ensurepip", "--upgrade", "--default-pip"), lines, cancel);
		if (r.exit == 0 && hasPip(executable)) {
			log.accept("pip is installed.");
			return;
		}
		/* The base Python of the venv: its pip installs pip into the venv (pip 22.3 or later) */
		Result b = runCode(executable, "import sys\nprint('BIO7BASE' + (getattr(sys, '_base_executable', '') or ''))", cancel);
		int at = b.output.indexOf("BIO7BASE");
		String base = at < 0 ? "" : b.output.substring(at + 8).strip();
		if (!base.isEmpty() && !new File(base).equals(new File(executable)) && hasPip(base)) {
			log.accept("\n$ " + base + " -m pip --python " + executable + " install pip");
			r = run(List.of(base, "-m", "pip", "--python", executable, "install", "pip"), lines, cancel);
			if (r.exit == 0 && hasPip(executable)) {
				log.accept("pip is installed.");
				return;
			}
		}
		if (allowDownload != null && allowDownload.getAsBoolean()) {
			File script = File.createTempFile("bio7getpip", ".py");
			try {
				log.accept("\nDownloading the installer of pip: " + GET_PIP);
				HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
				HttpRequest request = HttpRequest.newBuilder(URI.create(GET_PIP)).timeout(Duration.ofSeconds(120)).GET().build();
				HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(script.toPath()));
				if (response.statusCode() != 200) {
					throw new IOException("The installer of pip could not be downloaded (HTTP " + response.statusCode() + ")");
				}
				log.accept("$ python get-pip.py");
				r = run(List.of(executable, script.getAbsolutePath()), lines, cancel);
				if (r.exit == 0 && hasPip(executable)) {
					log.accept("pip is installed.");
					return;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted");
			} finally {
				script.delete();
			}
		}
		throw new IOException("pip could not be installed for " + executable + ".\n\n"
				+ "On Debian/Ubuntu install the venv support of Python (e.g., sudo apt install python3-venv, or python3.X-venv for the version),"
				+ " on Fedora python3-pip, then create the environment again.");
	}

	/**
	 * The command which installs the venv support and pip of the system Python
	 * (Linux, needs sudo: run in the Terminal), null if unknown (not Linux, an
	 * unknown distribution).
	 */
	public static com.eco.bio7.rbridge.RInstaller.InstallCommand systemPipCommand(String executable) {
		if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).startsWith("linux")) {
			return null;
		}
		String release;
		try {
			release = java.nio.file.Files.readString(Path.of("/etc/os-release")).toLowerCase(java.util.Locale.ROOT);
		} catch (IOException | RuntimeException e) {
			return null;
		}
		/* The version of the Python (Debian/Ubuntu: python3.X-venv) */
		String minor = "";
		try {
			Result r = runCode(executable, "import sys\nprint('BIO7VER%d.%d' % sys.version_info[:2])", null);
			int at = r.output.indexOf("BIO7VER");
			if (at >= 0) {
				minor = r.output.substring(at + 7).strip();
			}
		} catch (IOException e) {
			/* The package without the version */
		}
		List<String> ids = new ArrayList<>();
		ids.add(osReleaseValue(release, "id"));
		ids.addAll(List.of(osReleaseValue(release, "id_like").split("\\s+")));
		for (String id : ids) {
			switch (id) {
			case "debian", "ubuntu", "linuxmint", "pop", "raspbian" -> {
				String pkg = minor.matches("3\\.\\d+") ? "python" + minor + "-venv" : "python3-venv";
				return new com.eco.bio7.rbridge.RInstaller.InstallCommand("Debian/Ubuntu: venv and pip of Python " + minor,
						"sudo apt update && sudo apt install -y " + pkg + " python3-pip");
			}
			case "fedora", "rhel", "centos", "rocky", "almalinux" -> {
				return new com.eco.bio7.rbridge.RInstaller.InstallCommand("Fedora/RHEL: pip of Python", "sudo dnf install -y python3-pip");
			}
			case "opensuse", "suse", "opensuse-leap", "opensuse-tumbleweed", "sles" -> {
				return new com.eco.bio7.rbridge.RInstaller.InstallCommand("openSUSE: pip of Python", "sudo zypper install -y python3-pip");
			}
			case "arch", "manjaro", "endeavouros" -> {
				return new com.eco.bio7.rbridge.RInstaller.InstallCommand("Arch Linux: pip of Python", "sudo pacman -S --needed python-pip");
			}
			default -> {
			}
			}
		}
		return null;
	}

	/* A value of /etc/os-release (lower case, without quotes) or "" */
	private static String osReleaseValue(String release, String key) {
		for (String line : release.split("\\R")) {
			String l = line.trim();
			if (l.startsWith(key + "=")) {
				return l.substring(key.length() + 1).replace("\"", "").replace("'", "").trim();
			}
		}
		return "";
	}

	/** The official installer of pip (PyPA). */
	public static final String GET_PIP = "https://bootstrap.pypa.io/get-pip.py";

	/** The folder of the executables of an environment (bin, Scripts on Windows). */
	public static Path binFolder(Path environment) {
		return environment.resolve(System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "Scripts" : "bin");
	}

	/** Selects a Python folder in the preferences (Python, embedded). */
	public static void select(Path binFolder) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		store.setValue("python_pipe_path", binFolder.toString());
		store.setValue(EmbeddedPython.PREF_EMBEDDED, true);
		store.setValue("python_3x", true);
		/*
		 * Saved at once: else the preferences are only written when Bio7 ends normally
		 * (ended otherwise, e.g., from the IDE, the next start uses the former Python)
		 */
		if (store instanceof org.eclipse.jface.preference.IPersistentPreferenceStore p && store.needsSaving()) {
			try {
				p.save();
			} catch (IOException e) {
				/* Saved when Bio7 ends */
			}
		}
	}

	/**
	 * Splits the text of the install field into the arguments of pip (package
	 * names with versions, e.g., numpy "pandas>=2"). Requirement files are
	 * passed with -r.
	 */
	public static List<String> packageArgs(String text) {
		List<String> args = new ArrayList<>();
		for (String part : text.trim().split("\\s+")) {
			if (!part.isEmpty()) {
				args.add(part);
			}
		}
		return args;
	}
}
