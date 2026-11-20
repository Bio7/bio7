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
package com.eco.bio7.documents;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Quarto documents (*.qmd): the Quarto program (https://quarto.org, not part of
 * Bio7) and the kind of the output of a document (its YAML header, format:).
 */
public final class Quarto {

	private Quarto() {
	}

	/** The file is a Quarto document. */
	public static boolean isQuarto(String fileName) {
		return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".qmd");
	}

	/**
	 * The Quarto program: the PATH, the installers of Quarto (macOS, Linux,
	 * Windows) and the Quarto bundled with other R IDEs; null if not found.
	 */
	public static String binary() {
		String exe = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "quarto.exe" : "quarto";
		/* QUARTO_PATH: the program (as for the R package quarto) */
		String env = System.getenv("QUARTO_PATH");
		if (env != null && new File(env).isFile()) {
			return env.replace("\\", "/");
		}
		Set<String> dirs = new LinkedHashSet<>();
		String path = System.getenv("PATH");
		if (path != null) {
			for (String d : path.split(File.pathSeparator)) {
				dirs.add(d);
			}
		}
		String home = System.getProperty("user.home");
		String local = env("LOCALAPPDATA", home + "/AppData/Local");
		String programs = env("ProgramFiles", "C:/Program Files");
		for (String d : new String[] { "/Applications/quarto/bin", "/usr/local/bin", "/opt/homebrew/bin", "/opt/quarto/bin", "/usr/bin", home + "/.local/bin",
				"/Applications/RStudio.app/Contents/Resources/app/quarto/bin", "/Applications/Positron.app/Contents/Resources/app/quarto/bin",
				"/usr/lib/rstudio/resources/app/bin/quarto/bin", local + "/Programs/Quarto/bin", programs + "/Quarto/bin",
				programs + "/RStudio/resources/app/bin/quarto/bin", local + "/Programs/Positron/resources/app/quarto/bin" }) {
			dirs.add(d);
		}
		for (String d : dirs) {
			File f = new File(d, exe);
			if (f.isFile() && f.canExecute()) {
				return f.getPath().replace("\\", "/");
			}
		}
		return null;
	}

	private static String env(String name, String fallback) {
		String v = System.getenv(name);
		return v == null || v.isBlank() ? fallback : v.replace("\\", "/");
	}

	/**
	 * The kind of the output of a document: "Html", "Pdf" or "Word" (the first
	 * format of its YAML header: format: html, or a list of formats); "Html"
	 * without a format (the default of Quarto).
	 */
	public static String docType(String document) {
		String format = firstFormat(document);
		if (format == null) {
			return "Html";
		}
		switch (format) {
		case "pdf":
		case "beamer":
		case "typst":
			return "Pdf";
		case "docx":
			return "Word";
		default:
			/* html, revealjs, dashboard, ... */
			return "Html";
		}
	}

	/** The file extension of the output of a document (html, pdf, docx). */
	public static String extension(String document) {
		String t = docType(document);
		return t.equals("Pdf") ? "pdf" : t.equals("Word") ? "docx" : "html";
	}

	/* The first format of the YAML header, null if none */
	private static String firstFormat(String document) {
		String[] lines = document.split("\\r?\\n");
		if (lines.length == 0 || !lines[0].trim().equals("---")) {
			return null;
		}
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];
			String t = line.trim();
			if (t.equals("---") || t.equals("...")) {
				return null;
			}
			if (!line.startsWith("format:")) {
				continue;
			}
			String value = line.substring("format:".length()).trim();
			if (!value.isEmpty()) {
				/* format: html (or format: "html") */
				return value.replaceAll("[\"']", "").split("[\\s:+]")[0].toLowerCase(Locale.ROOT);
			}
			/* format: followed by the formats as keys (  html:, or - html) */
			for (int k = i + 1; k < lines.length; k++) {
				String n = lines[k];
				if (n.isBlank()) {
					continue;
				}
				if (!Character.isWhitespace(n.charAt(0)) && !n.startsWith("-")) {
					return null;
				}
				String key = n.trim().replaceFirst("^-\\s*", "");
				return key.replaceAll("[\"']", "").split("[\\s:+]")[0].toLowerCase(Locale.ROOT);
			}
			return null;
		}
		return null;
	}
}
