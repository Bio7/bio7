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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;

import com.eco.bio7.rbridge.RLocator.RInstallation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Installs the Rserve package for the cooperative mode: a binary of the Bio7
 * repository (chosen by the platform and the version of R, described by the
 * manifest.json of the repository) or compiled from the sources (with
 * -DCOOPERATIVE). The installation runs in a separate R process (not in the
 * console), so the result can be checked.
 */
public final class RserveInstaller {

	/** The repository of the binaries. */
	public static final String REPOSITORY = "https://raw.githubusercontent.com/Bio7/Rserve-Cooperative/master/";
	/** The manifest of the repository (the available binaries). */
	public static final String MANIFEST_URL = REPOSITORY + "manifest.json";

	/**
	 * A file of the repository.
	 *
	 * @param os      "windows", "mac" or "linux"
	 * @param arch    "aarch64", "x86_64" or "any"
	 * @param r       the minor version of R it was built with (e.g., 4.6)
	 * @param rserve  the version of Rserve
	 * @param url     the download location (or null for type "cran")
	 * @param sha256  the checksum or null (not checked)
	 * @param type    "binary" (a file of the repository) or "cran" (the binary of
	 *                CRAN, e.g., Windows where Rserve always runs cooperative)
	 */
	public record Artifact(String os, String arch, String r, String rserve, String url, String sha256, String type) {

		public boolean isCran() {
			return "cran".equals(type);
		}

		public String fileName() {
			return url == null ? "Rserve (CRAN)" : url.substring(url.lastIndexOf('/') + 1);
		}
	}

	/**
	 * The manifest.
	 *
	 * @param files     the binaries
	 * @param sourceUrl the location of the sources (a tar.gz) or null for CRAN
	 * @param sourceSha the checksum of the sources or null
	 * @param fromWeb   false if the built-in list was used (no connection)
	 */
	public record Manifest(List<Artifact> files, String sourceUrl, String sourceSha, boolean fromWeb) {
	}

	/**
	 * The binary for an R installation.
	 *
	 * @param artifact the file or null if no binary fits
	 * @param exact    true if it was built for the same minor version of R
	 * @param text     a description for the user
	 */
	public record Choice(Artifact artifact, boolean exact, String text) {
	}

	private RserveInstaller() {
	}

	/* The files of the repository (if the manifest can't be loaded) */
	private static List<Artifact> builtIn() {
		return List.of(new Artifact("mac", "aarch64", "4.6", "1.8-15", REPOSITORY + "Rserve_1.8-15_Mac_arm64.tgz", null, "binary"),
				new Artifact("mac", "x86_64", "4.1", "1.8-8", REPOSITORY + "Rserve_1.8-8.tgz", null, "binary"),
				new Artifact("linux", "x86_64", "4.3", "1.8-15", REPOSITORY + "Rserve_1.8-15_Linux_x86_64.tar.gz", null, "binary"),
				new Artifact("linux", "aarch64", "4.2", "1.8-15", REPOSITORY + "Rserve_1.8-15_Linux_aarch64.tar.gz", null, "binary"),
				new Artifact("windows", "any", "", "", null, null, "cran"));
	}

	/**
	 * Loads the manifest of the repository (the built-in list if it can't be
	 * loaded).
	 */
	public static Manifest loadManifest(Consumer<String> log) {
		try {
			HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
			HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(MANIFEST_URL)).timeout(Duration.ofSeconds(30)).build(),
					HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				return parseManifest(response.body());
			}
			log.accept("The manifest of the repository is not available (HTTP " + response.statusCode() + "), the built-in list is used.");
		} catch (IOException | RuntimeException e) {
			log.accept("The manifest of the repository could not be loaded (" + e.getMessage() + "), the built-in list is used.");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return new Manifest(builtIn(), null, null, false);
	}

	/** Parses a manifest (see manifest.json of the repository). */
	public static Manifest parseManifest(String json) throws IOException {
		JsonNode root = new ObjectMapper().readTree(json);
		String base = text(root, "base", REPOSITORY);
		List<Artifact> files = new ArrayList<>();
		for (JsonNode f : root.path("files")) {
			String type = text(f, "type", "binary");
			String file = text(f, "file", null);
			String url = text(f, "url", file == null ? null : base + file);
			if (!type.equals("cran") && url == null) {
				continue;
			}
			files.add(new Artifact(text(f, "os", ""), RLocator.normalizeArch(text(f, "arch", "any")), text(f, "r", ""), text(f, "rserve", ""), url,
					text(f, "sha256", null), type));
		}
		JsonNode source = root.path("source");
		String sourceFile = text(source, "file", null);
		String sourceUrl = text(source, "url", sourceFile == null ? null : base + sourceFile);
		return new Manifest(files, sourceUrl, text(source, "sha256", null), true);
	}

	private static String text(JsonNode node, String field, String fallback) {
		JsonNode v = node.get(field);
		return v == null || v.isNull() || v.asText().isBlank() ? fallback : v.asText().trim();
	}

	/**
	 * Chooses the binary for an R installation: the same system and architecture,
	 * the same minor version of R, else the newest one built for an older R.
	 */
	public static Choice choose(Manifest manifest, RInstallation r) {
		String os = r.os();
		String arch = r.normalizedArch();
		String minor = r.minorVersion();
		Artifact best = null;
		for (Artifact a : manifest.files()) {
			if (!a.os().equals(os) || !(a.arch().equals("any") || a.arch().equals(arch))) {
				continue;
			}
			if (a.isCran()) {
				return new Choice(a, true, "The Rserve binary of CRAN (on Windows Rserve always runs in the cooperative mode)");
			}
			if (a.r().equals(minor)) {
				return new Choice(a, true, a.fileName() + " (Rserve " + a.rserve() + ", built for R " + a.r() + ")");
			}
			if (compareVersion(a.r(), minor) < 0 && (best == null || compareVersion(a.r(), best.r()) > 0)) {
				best = a;
			}
		}
		if (best != null) {
			return new Choice(best, false, best.fileName() + " (Rserve " + best.rserve() + ", built for R " + best.r() + ", this is R " + minor
					+ ": it may not work, compiling is recommended)");
		}
		return new Choice(null, false, "No binary for R " + minor + " (" + os + ", " + arch + ") in the repository: please compile Rserve");
	}

	static int compareVersion(String a, String b) {
		String[] x = a.split("[.-]");
		String[] y = b.split("[.-]");
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int u = i < x.length ? parse(x[i]) : 0;
			int v = i < y.length ? parse(y[i]) : 0;
			if (u != v) {
				return Integer.compare(u, v);
			}
		}
		return 0;
	}

	private static int parse(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * The library to install into: the package install location of the
	 * preferences, else the first writable library of R, else the user library
	 * (created).
	 *
	 * @return the folder or null if no library is writable
	 */
	public static String targetLibrary(RInstallation r, String installLocation) {
		if (installLocation != null && !installLocation.isBlank() && isWritableOrCreatable(new File(installLocation.trim()))) {
			return installLocation.trim();
		}
		for (String lib : r.libraryPaths()) {
			if (Files.isWritable(Path.of(lib))) {
				return lib;
			}
		}
		if (r.userLibrary() != null && isWritableOrCreatable(new File(r.userLibrary()))) {
			return r.userLibrary();
		}
		return null;
	}

	private static boolean isWritableOrCreatable(File dir) {
		File f = dir;
		while (f != null && !f.exists()) {
			f = f.getParentFile();
		}
		return f != null && f.isDirectory() && Files.isWritable(f.toPath());
	}

	/**
	 * Installs a binary (downloaded and checked, or of CRAN for type "cran").
	 *
	 * @return true if Rserve was installed and can be loaded
	 */
	public static boolean installBinary(RInstallation r, Artifact artifact, String library, String cranRepository, Consumer<String> log,
			IProgressMonitor monitor) throws IOException, InterruptedException {
		new File(library).mkdirs();
		String install;
		if (artifact.isCran()) {
			log.accept("Installing Rserve from " + cranRepository + " into " + library);
			install = "install.packages('Rserve', repos = " + rString(cranRepository) + ", lib = " + rString(library) + ")";
		} else {
			Path file = download(artifact.url(), artifact.sha256(), log, monitor);
			log.accept("Installing " + artifact.fileName() + " into " + library);
			install = "install.packages(" + rString(file.toString()) + ", repos = NULL, lib = " + rString(library) + ")";
		}
		return runInstall(r, install, library, Map.of(), log, monitor);
	}

	/**
	 * Installs a local file (a binary of the repository or a source archive which
	 * is compiled for the cooperative mode).
	 *
	 * @return true if Rserve was installed and can be loaded
	 */
	public static boolean installFile(RInstallation r, String path, String library, Consumer<String> log, IProgressMonitor monitor)
			throws IOException, InterruptedException {
		new File(library).mkdirs();
		boolean source = isSourceArchive(Path.of(path));
		log.accept("Installing " + path + (source ? " (sources, compiled for the cooperative mode)" : "") + " into " + library);
		String install = "install.packages(" + rString(path) + ", repos = NULL" + (source ? ", type = 'source'" : "") + ", lib = " + rString(library) + ")";
		return runInstall(r, install, library, source ? Map.of("PKG_CPPFLAGS", cooperativeFlags()) : Map.of(), log, monitor);
	}

	/*
	 * The flags to compile Rserve for the cooperative mode. Rserve (e.g., 1.8-19)
	 * calls kill() without signal.h in the cooperative mode: current compilers
	 * (clang 16+, gcc 14+) reject the implicit declaration, so signal.h is
	 * included (not on Windows, where this code is not compiled).
	 */
	static String cooperativeFlags() {
		String flags = System.getenv("PKG_CPPFLAGS");
		String cooperative = RLocator.os().equals("Windows") ? "-DCOOPERATIVE" : "-DCOOPERATIVE -include signal.h";
		return (flags == null || flags.isBlank() ? "" : flags + " ") + cooperative;
	}

	/* A source archive contains no "Built:" field in its DESCRIPTION (only a tar.gz can be a source archive) */
	static boolean isSourceArchive(Path file) {
		String name = file.getFileName().toString();
		if (!name.endsWith(".tar.gz")) {
			return false;
		}
		try (InputStream in = new java.util.zip.GZIPInputStream(Files.newInputStream(file))) {
			byte[] header = new byte[512];
			while (in.readNBytes(header, 0, 512) == 512) {
				String entry = new String(header, 0, 100, StandardCharsets.UTF_8).trim().replace("\0", "");
				if (entry.isEmpty()) {
					return false;
				}
				String sizeText = new String(header, 124, 12, StandardCharsets.US_ASCII).replace("\0", "").trim();
				long size = sizeText.isEmpty() ? 0 : Long.parseLong(sizeText, 8);
				byte[] content = in.readNBytes((int) ((size + 511) / 512 * 512));
				if (entry.endsWith("/DESCRIPTION") && entry.split("/").length == 2) {
					return !new String(content, 0, (int) size, StandardCharsets.UTF_8).contains("\nBuilt:");
				}
			}
		} catch (IOException | RuntimeException e) {
			/* Not readable: install as given */
		}
		return false;
	}

	/**
	 * The R commands to install a file of the repository manually in the R
	 * console (for the user to copy).
	 */
	public static String manualCommands(Artifact artifact, String library, String cranRepository) {
		String lib = library == null ? "" : ", lib = " + rString(library);
		if (artifact.isCran()) {
			return "install.packages('Rserve', repos = " + rString(cranRepository) + lib + ")";
		}
		return "install.packages(" + rString(artifact.url()) + ", repos = NULL" + lib + ")";
	}

	/**
	 * The shell command to compile Rserve from a source archive for the
	 * cooperative mode manually.
	 */
	public static String manualCompileCommand(String library) {
		boolean windows = RLocator.os().equals("Windows");
		String lib = library == null ? "" : " --library=\"" + library + "\"";
		return windows ? "set PKG_CPPFLAGS=-DCOOPERATIVE && R CMD INSTALL" + lib + " Rserve_<version>.tar.gz"
				: "PKG_CPPFLAGS=\"-DCOOPERATIVE -include signal.h\" R CMD INSTALL" + lib + " Rserve_<version>.tar.gz";
	}

	/**
	 * Compiles and installs Rserve from the sources (the location of the manifest,
	 * else CRAN) for the cooperative mode (PKG_CPPFLAGS=-DCOOPERATIVE).
	 *
	 * @return true if Rserve was installed and can be loaded
	 */
	public static boolean compile(RInstallation r, Manifest manifest, String library, String cranRepository, Consumer<String> log,
			IProgressMonitor monitor) throws IOException, InterruptedException {
		new File(library).mkdirs();
		String install;
		if (manifest.sourceUrl() != null) {
			Path file = download(manifest.sourceUrl(), manifest.sourceSha(), log, monitor);
			install = "install.packages(" + rString(file.toString()) + ", repos = NULL, type = 'source', lib = " + rString(library) + ")";
		} else {
			install = "install.packages('Rserve', repos = " + rString(cranRepository) + ", type = 'source', lib = " + rString(library) + ")";
		}
		String cppFlags = cooperativeFlags();
		log.accept("Compiling Rserve with PKG_CPPFLAGS=" + cppFlags + " into " + library + " (this can take a minute)");
		return runInstall(r, install, library, Map.of("PKG_CPPFLAGS", cppFlags), log, monitor);
	}

	/* Runs the installation in Rscript and loads the installed package (in a new process) */
	private static boolean runInstall(RInstallation r, String install, String library, Map<String, String> env, Consumer<String> log,
			IProgressMonitor monitor) throws IOException, InterruptedException {
		String rscript = RLocator.rscript(r.home());
		if (rscript == null) {
			throw new IOException("Rscript was not found in " + r.home());
		}
		/*
		 * A failed installation is only a warning of install.packages (e.g., a non-zero
		 * exit status, in the language of R): each warning of install.packages is an
		 * error here
		 */
		String script = "options(warn = 1)\n" + "withCallingHandlers(" + install + ",\n"
				+ "  warning = function(w) { cl <- conditionCall(w); if (!is.null(cl) && identical(as.character(cl[[1]]), 'install.packages')) stop(conditionMessage(w), call. = FALSE) })\n"
				+ "v <- as.character(utils::packageVersion('Rserve', lib.loc = " + rString(library) + "))\n"
				+ "loadNamespace('Rserve', lib.loc = c(" + rString(library) + ", .libPaths()))\n"
				+ "cat('\\nBIO7_INSTALLED: Rserve', v, '\\n')\n";
		File file = File.createTempFile("bio7_install_rserve", ".R");
		file.deleteOnExit();
		Files.writeString(file.toPath(), script, StandardCharsets.UTF_8);
		int exit = runProcess(List.of(rscript, "--no-save", "--no-restore", file.getAbsolutePath()), env, log, monitor);
		file.delete();
		return exit == 0;
	}

	/* Downloads a file into a new temporary folder and checks it (if a checksum is given) */
	private static Path download(String url, String sha256, Consumer<String> log, IProgressMonitor monitor) throws IOException, InterruptedException {
		log.accept("Downloading " + url);
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
		HttpResponse<InputStream> response = client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5)).build(),
				HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() != 200) {
			response.body().close();
			throw new IOException("Download failed (HTTP " + response.statusCode() + "): " + url);
		}
		Path dir = Files.createTempDirectory("bio7-rserve");
		Path file = dir.resolve(url.substring(url.lastIndexOf('/') + 1));
		try (InputStream in = response.body()) {
			Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
		}
		file.toFile().deleteOnExit();
		dir.toFile().deleteOnExit();
		if (monitor.isCanceled()) {
			throw new InterruptedException("canceled");
		}
		if (sha256 != null) {
			String actual = sha256(file);
			if (!actual.equalsIgnoreCase(sha256)) {
				Files.deleteIfExists(file);
				throw new IOException("The checksum of the download is wrong (expected " + sha256 + ", got " + actual + ")");
			}
			log.accept("Checksum (SHA-256) OK");
		}
		return file;
	}

	static String sha256(Path file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream in = Files.newInputStream(file)) {
				byte[] buffer = new byte[65536];
				int n;
				while ((n = in.read(buffer)) > 0) {
					digest.update(buffer, 0, n);
				}
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	/** An R string literal (single quotes). */
	static String rString(String s) {
		return "'" + s.replace("\\", "/").replace("'", "\\'") + "'";
	}

	/** Runs a process, writes its output to the log and returns the exit code. */
	public static int runProcess(List<String> command, Map<String, String> env, Consumer<String> log, IProgressMonitor monitor)
			throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.environment().putAll(env);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		process.getOutputStream().close();
		Thread reader = new Thread(() -> {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = in.readLine()) != null) {
					log.accept(line);
				}
			} catch (IOException e) {
				/* The process was stopped */
			}
		}, "Bio7 Installer Output");
		reader.setDaemon(true);
		reader.start();
		while (!process.waitFor(200, java.util.concurrent.TimeUnit.MILLISECONDS)) {
			if (monitor.isCanceled()) {
				process.descendants().forEach(ProcessHandle::destroy);
				process.destroy();
				throw new InterruptedException("canceled");
			}
		}
		reader.join(2000);
		return process.exitValue();
	}

	/* ---------------- The installation of R ---------------- */

	/** The download page of R for this system. */
	public static String rDownloadPage() {
		return switch (RLocator.os()) {
		case "Windows" -> "https://cloud.r-project.org/bin/windows/base/";
		case "Mac" -> "https://cloud.r-project.org/bin/macosx/";
		default -> "https://cloud.r-project.org/bin/linux/";
		};
	}

	/** Returns true if R can be installed with winget (Windows). */
	public static boolean canInstallRWithWinget() {
		return RLocator.os().equals("Windows") && RLocator.which("winget.exe") != null;
	}

	/** Installs R with winget (Windows asks for the permission). */
	public static int installRWithWinget(Consumer<String> log, IProgressMonitor monitor) throws IOException, InterruptedException {
		log.accept("winget install --id RProject.R -e");
		return runProcess(List.of("winget", "install", "--id", "RProject.R", "-e", "--accept-package-agreements", "--accept-source-agreements"), Map.of(),
				log, monitor);
	}

	/**
	 * A shell command which installs R (or tools for R), run in the Terminal.
	 *
	 * @param label   the name for the list (e.g., the distribution)
	 * @param command the shell command (bash; may be several lines)
	 */
	public record InstallCommand(String label, String command) {
	}

	/** The commands to install R on this system (macOS, Linux; empty on Windows). */
	public static List<InstallCommand> rInstallCommands() {
		List<InstallCommand> list = new ArrayList<>();
		switch (RLocator.os()) {
		case "Mac" -> {
			/* The current package of the download page of CRAN for this processor */
			list.add(new InstallCommand("Installer package of CRAN (without Homebrew)", """
					ARCH=$(uname -m); [ "$ARCH" = "arm64" ] || ARCH=x86_64
					PKG=$(curl -fsSL https://cloud.r-project.org/bin/macosx/ | grep -oE "href=\\"[^\\"]*R-[0-9.]+-$ARCH\\.pkg\\"" | head -1 | sed -e 's/^href="//' -e 's/"$//')
					[ -n "$PKG" ] || { echo "The installer package was not found on CRAN."; exit 1; }
					echo "Downloading https://cloud.r-project.org/bin/macosx/$PKG"
					curl -fL -o /tmp/bio7-R.pkg "https://cloud.r-project.org/bin/macosx/$PKG" && sudo installer -pkg /tmp/bio7-R.pkg -target / && rm -f /tmp/bio7-R.pkg"""));
			list.add(new InstallCommand("Homebrew: R of CRAN", "brew install --cask r-app"));
			list.add(new InstallCommand("rig: several R versions side by side (Homebrew)", "brew tap r-lib/rig && brew install --cask rig && rig add release"));
			list.add(new InstallCommand("Xcode command line tools (to compile Rserve)", "xcode-select --install"));
		}
		case "Linux" -> {
			list.add(new InstallCommand("Ubuntu: current R of CRAN", """
					sudo apt update -qq && sudo apt install -y --no-install-recommends software-properties-common dirmngr wget
					wget -qO- https://cloud.r-project.org/bin/linux/ubuntu/marutter_pubkey.asc | sudo tee -a /etc/apt/trusted.gpg.d/cran_ubuntu_key.asc
					sudo add-apt-repository -y "deb https://cloud.r-project.org/bin/linux/ubuntu $(lsb_release -cs)-cran40/"
					sudo apt install -y --no-install-recommends r-base r-base-dev"""));
			list.add(new InstallCommand("Debian/Ubuntu: R of the distribution", "sudo apt update && sudo apt install -y r-base r-base-dev"));
			list.add(new InstallCommand("Fedora", "sudo dnf install -y R"));
			list.add(new InstallCommand("Rocky/AlmaLinux (EPEL)",
					"sudo dnf install -y epel-release dnf-plugins-core && sudo dnf config-manager --set-enabled crb && sudo dnf install -y R"));
			list.add(new InstallCommand("openSUSE", "sudo zypper install -y R-base R-base-devel"));
			list.add(new InstallCommand("Arch Linux", "sudo pacman -S --needed r"));
			list.add(new InstallCommand("rig: several R versions side by side (any distribution)",
					"curl -Ls https://github.com/r-lib/rig/releases/download/latest/rig-linux-$(uname -m)-latest.tar.gz | sudo tar xz -C /usr/local && sudo rig add release"));
			list.add(new InstallCommand("Build tools to compile Rserve (Debian/Ubuntu)", "sudo apt install -y build-essential r-base-dev"));
			list.add(new InstallCommand("Build tools to compile Rserve (Fedora)", "sudo dnf install -y gcc make R-devel"));
		}
		default -> {
		}
		}
		return list;
	}

	/** The index of the command which fits this system best (Linux: the distribution of /etc/os-release). */
	public static int preferredInstallCommand(List<InstallCommand> commands) {
		if (RLocator.os().equals("Mac")) {
			/* The installer package of CRAN (needs no Homebrew) */
			return 0;
		}
		String release = "";
		try {
			release = Files.readString(Path.of("/etc/os-release")).toLowerCase(java.util.Locale.ROOT);
		} catch (IOException | RuntimeException e) {
			/* Unknown distribution */
		}
		return preferredLinuxCommand(commands, release);
	}

	/* The command for the distribution of an /etc/os-release (lower case) */
	static int preferredLinuxCommand(List<InstallCommand> commands, String release) {
		Map<String, String> map = Map.of("ubuntu", "Ubuntu: current", "debian", "Debian/Ubuntu", "fedora", "Fedora", "rocky", "Rocky", "almalinux", "Rocky",
				"rhel", "Rocky", "centos", "Rocky", "opensuse", "openSUSE", "suse", "openSUSE", "arch", "Arch");
		/* The ID first (e.g., ID=ubuntu, ID_LIKE=debian), then ID_LIKE in its order (e.g., "rhel centos fedora") */
		List<String> ids = new ArrayList<>();
		ids.add(osReleaseValue(release, "id"));
		for (String like : osReleaseValue(release, "id_like").split("\\s+")) {
			ids.add(like);
		}
		for (String id : ids) {
			/* e.g., opensuse-tumbleweed */
			String key = id.startsWith("opensuse") ? "opensuse" : id;
			if (map.containsKey(key)) {
				return indexOf(commands, map.get(key));
			}
		}
		/* rig works on all distributions */
		return indexOf(commands, "rig");
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

	private static int indexOf(List<InstallCommand> commands, String labelStart) {
		for (int i = 0; i < commands.size(); i++) {
			if (commands.get(i).label().startsWith(labelStart)) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * Writes a script for the Terminal: it shows and runs the command (sudo can
	 * ask for the password) and keeps the Terminal open afterwards.
	 *
	 * @return the script (a path without spaces)
	 */
	public static File writeTerminalScript(InstallCommand command) throws IOException {
		String script = "#!/bin/bash\n" + "echo 'Bio7: " + command.label().replace("'", "") + "'\n" + "cat <<'BIO7_COMMAND'\n" + command.command() + "\nBIO7_COMMAND\n"
				+ "echo\n" + "(\n" + command.command() + "\n)\n" + "status=$?\n" + "echo\n"
				+ "if [ $status -eq 0 ]; then echo \"Finished. Click 'Search Again' in the R and Rserve Setup.\"; else echo \"The command ended with the exit code $status.\"; fi\n"
				+ "exec \"${SHELL:-/bin/bash}\" -l\n";
		File dir = new File(System.getProperty("java.io.tmpdir"));
		/* The Terminal gets the path as an argument: no spaces */
		if (dir.getAbsolutePath().contains(" ")) {
			dir = new File("/tmp");
		}
		File file = File.createTempFile("bio7_install_r", ".sh", dir);
		Files.writeString(file.toPath(), script, StandardCharsets.UTF_8);
		file.setExecutable(true);
		file.deleteOnExit();
		return file;
	}

	/** A hint how to install R for this system (the shell commands). */
	public static String rInstallHint() {
		return switch (RLocator.os()) {
		case "Windows" -> "Download and run the installer of R, or install it with winget (Windows asks for the permission).";
		case "Mac" -> "Download and open the installer package of R for your processor (Apple silicon: arm64, Intel: x86_64), "
				+ "or in the Terminal with Homebrew: brew install --cask r";
		default -> "Install R with the package manager in a terminal, e.g., Debian/Ubuntu: sudo apt install r-base r-base-dev, "
				+ "Fedora: sudo dnf install R";
		};
	}
}
