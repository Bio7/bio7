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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The library folders of the embedded R and Python for each other (Linux): the
 * runtime started first loads the newest C++ runtime (libstdc++) of both (see
 * CxxRuntime of the bridges), else a runtime which needs a newer one than the
 * one loaded (e.g., R of conda after numpy of the system Python) can't start.
 * The folders are passed in the system property bio7.native.libdirs.
 */
public final class NativeLibDirs {

	private static final String PROPERTY = "bio7.native.libdirs";

	private NativeLibDirs() {
	}

	/** Adds the library folders of an R home (e.g., <prefix>/lib/R: <prefix>/lib/R/lib and <prefix>/lib of conda). */
	public static void addR(String rHome) {
		if (rHome == null || rHome.isBlank()) {
			return;
		}
		Path home = Path.of(rHome);
		add(home.resolve("lib"));
		if (home.getParent() != null) {
			add(home.getParent());
		}
	}

	/** Adds the library folders of a Python executable (its prefix; for a venv the base Python of pyvenv.cfg). */
	public static void addPython(String executable) {
		if (executable == null || executable.isBlank()) {
			return;
		}
		Path bin = Path.of(executable).toAbsolutePath().getParent();
		if (bin == null || bin.getParent() == null) {
			return;
		}
		Path prefix = bin.getParent();
		add(prefix.resolve("lib"));
		/* A venv: home = <base>/bin */
		Path cfg = prefix.resolve("pyvenv.cfg");
		if (Files.isRegularFile(cfg)) {
			try {
				for (String line : Files.readAllLines(cfg)) {
					String l = line.trim();
					if (l.startsWith("home") && l.contains("=")) {
						Path baseBin = Path.of(l.substring(l.indexOf('=') + 1).trim());
						if (baseBin.getParent() != null) {
							add(baseBin.getParent().resolve("lib"));
						}
					}
				}
			} catch (IOException | RuntimeException e) {
				/* Not a readable venv */
			}
		}
	}

	private static synchronized void add(Path dir) {
		if (!Files.isDirectory(dir)) {
			return;
		}
		Set<String> dirs = new LinkedHashSet<>();
		for (String d : System.getProperty(PROPERTY, "").split(File.pathSeparator)) {
			if (!d.isBlank()) {
				dirs.add(d);
			}
		}
		dirs.add(dir.toString());
		System.setProperty(PROPERTY, String.join(File.pathSeparator, dirs));
	}
}
