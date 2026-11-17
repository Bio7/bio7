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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The installation of R: the commands for this system (run in a terminal), the
 * download page, winget (Windows) and a runner for processes with a log.
 */
public final class RInstaller {

	private RInstaller() {
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
			list.add(new InstallCommand("Xcode command line tools (to compile R packages)", "xcode-select --install"));
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
			list.add(new InstallCommand("Build tools to compile R packages (Debian/Ubuntu)", "sudo apt install -y build-essential r-base-dev"));
			list.add(new InstallCommand("Build tools to compile R packages (Fedora)", "sudo dnf install -y gcc make R-devel"));
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
		return writeTerminalScript(command, "Finished. Click 'Search Again' in the R Setup.");
	}

	/**
	 * As above, with the message shown when the command has finished successfully
	 * (e.g., what to do next in Bio7).
	 */
	public static File writeTerminalScript(InstallCommand command, String finished) throws IOException {
		String script = "#!/bin/bash\n" + "echo 'Bio7: " + command.label().replace("'", "") + "'\n" + "cat <<'BIO7_COMMAND'\n" + command.command() + "\nBIO7_COMMAND\n"
				+ "echo\n" + "(\n" + command.command() + "\n)\n" + "status=$?\n" + "echo\n"
				+ "if [ $status -eq 0 ]; then echo \"" + finished.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$").replace("`", "\\`")
				+ "\"; else echo \"The command ended with the exit code $status.\"; fi\n"
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
