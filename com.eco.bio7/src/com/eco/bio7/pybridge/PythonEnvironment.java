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
			return pip != null && !externallyManaged;
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
		Result r = run(List.of(executable, "-c", PROBE), null, null);
		int at = r.output.indexOf("BIO7ENV");
		if (r.exit != 0 || at < 0) {
			throw new IOException("Python could not be started: " + executable + "\n" + r.output.strip());
		}
		JsonNode n = JSON.readTree(r.output.substring(at + 7).strip());
		return new Info(executable, n.path("version").asText(), n.path("kind").asText(), n.path("managed").asBoolean(), n.path("pip").isNull() ? null : n.path("pip").asText());
	}

	/** The installed packages (sorted by name). */
	public static List<Package> list(String executable) throws IOException {
		Result r = run(List.of(executable, "-m", "pip", "list", "--format=json", "--disable-pip-version-check"), null, null);
		if (r.exit != 0) {
			throw new IOException(r.output.strip());
		}
		Map<String, Package> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (JsonNode n : JSON.readTree(jsonPart(r.output))) {
			map.put(n.path("name").asText(), new Package(n.path("name").asText(), n.path("version").asText(), null));
		}
		return new ArrayList<>(map.values());
	}

	/** The packages with a newer version (name: latest version); needs the internet. */
	public static Map<String, String> outdated(String executable) throws IOException {
		Result r = run(List.of(executable, "-m", "pip", "list", "--outdated", "--format=json", "--disable-pip-version-check"), null, null);
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

	/** pip with arguments (e.g., install numpy): the output line by line. */
	public static Result pip(String executable, List<String> args, Consumer<String> lines, Process[] cancel) throws IOException {
		List<String> cmd = new ArrayList<>(List.of(executable, "-m", "pip"));
		cmd.addAll(args);
		return run(cmd, lines, cancel);
	}

	/**
	 * Creates a virtual environment (venv) with a base Python.
	 *
	 * @return the Python executable of the environment
	 */
	public static String createEnvironment(String baseExecutable, Path folder, Consumer<String> lines, Process[] cancel) throws IOException {
		Result r = run(List.of(baseExecutable, "-m", "venv", folder.toString()), lines, cancel);
		if (r.exit != 0) {
			throw new IOException("The environment could not be created:\n" + r.output.strip());
		}
		String exe = PythonFinder.executableIn(binFolder(folder).toString());
		if (exe == null) {
			throw new IOException("No Python in the new environment: " + binFolder(folder));
		}
		return exe;
	}

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
