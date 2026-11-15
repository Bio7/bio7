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

/**
 * Converts a markdown file with pandoc (no R): PDF (with a LaTeX engine), Word,
 * ODT, RTF and EPUB. Pandoc and LaTeX are searched in the PATH and in the usual
 * installation folders (an application started from the Dock or the Finder
 * doesn't have the PATH of the shell).
 */
public final class Pandoc {

	private Pandoc() {
	}

	/* The folders of programs (macOS, Linux, Windows), the PATH first */
	private static List<String> folders() {
		Set<String> dirs = new LinkedHashSet<>();
		String path = System.getenv("PATH");
		if (path != null) {
			for (String d : path.split(File.pathSeparator)) {
				dirs.add(d);
			}
		}
		String home = System.getProperty("user.home");
		/* macOS and Linux: Homebrew, MacTeX, Quarto, RStudio, conda, TinyTeX */
		for (String d : new String[] { "/opt/homebrew/bin", "/usr/local/bin", "/usr/bin", "/Library/TeX/texbin",
				"/Applications/RStudio.app/Contents/Resources/app/quarto/bin/tools/aarch64", "/Applications/RStudio.app/Contents/Resources/app/quarto/bin/tools/x86_64",
				"/Applications/RStudio.app/Contents/Resources/app/quarto/bin/tools", "/Applications/quarto/bin/tools/aarch64", "/Applications/quarto/bin/tools/x86_64",
				"/Applications/quarto/bin/tools", "/opt/quarto/bin/tools", "/usr/lib/rstudio/resources/app/bin/quarto/bin/tools", home + "/anaconda3/bin",
				home + "/miniconda3/bin", home + "/opt/anaconda3/bin", home + "/.local/bin", home + "/Library/TinyTeX/bin/universal-darwin",
				home + "/.TinyTeX/bin/x86_64-linux", home + "/.TinyTeX/bin/aarch64-linux" }) {
			dirs.add(d);
		}
		/* Windows: the installers of pandoc (per user, all users), Chocolatey, Scoop, Quarto, RStudio, MiKTeX, TinyTeX */
		String local = env("LOCALAPPDATA", home + "/AppData/Local");
		String roaming = env("APPDATA", home + "/AppData/Roaming");
		String programs = env("ProgramFiles", "C:/Program Files");
		String programData = env("ProgramData", "C:/ProgramData");
		for (String d : new String[] { local + "/Pandoc", programs + "/Pandoc", programData + "/chocolatey/bin", home + "/scoop/shims", programs + "/Quarto/bin/tools",
				local + "/Programs/Quarto/bin/tools", programs + "/RStudio/resources/app/bin/quarto/bin/tools", programs + "/RStudio/bin/quarto/bin/tools",
				programs + "/RStudio/bin/pandoc", local + "/Programs/MiKTeX/miktex/bin/x64", programs + "/MiKTeX/miktex/bin/x64", roaming + "/TinyTeX/bin/windows",
				roaming + "/TinyTeX/bin/win32", home + "/miniconda3/Library/bin", home + "/anaconda3/Library/bin" }) {
			dirs.add(d);
		}
		/* TeX Live (Windows: C:/texlive/<year>/bin/windows; Linux: /usr/local/texlive/<year>/bin/...), the newest year first */
		for (String root : new String[] { "C:/texlive", "/usr/local/texlive" }) {
			File[] years = new File(root).listFiles(File::isDirectory);
			if (years != null) {
				java.util.Arrays.sort(years, java.util.Comparator.comparing(File::getName).reversed());
				for (File y : years) {
					File[] bins = new File(y, "bin").listFiles(File::isDirectory);
					if (bins != null) {
						for (File b : bins) {
							dirs.add(b.getPath());
						}
					}
				}
			}
		}
		return new ArrayList<>(dirs);
	}

	private static String env(String name, String fallback) {
		String v = System.getenv(name);
		return v != null && !v.isBlank() ? v.replace('\\', '/') : fallback;
	}

	/**
	 * R code which makes pandoc (and LaTeX) available to rmarkdown in R: the
	 * existing program folders (Homebrew, Quarto, RStudio, conda, TeX, ...) are
	 * appended to the PATH of R (an application started from the desktop has
	 * no PATH of the shell) and RSTUDIO_PANDOC is set if R had no pandoc.
	 */
	public static String rSetup() {
		StringBuilder dirs = new StringBuilder();
		for (String d : folders()) {
			if (new File(d).isDirectory()) {
				dirs.append(dirs.length() > 0 ? ", " : "").append(rString(d));
			}
		}
		String pandoc = find();
		String pandocDir = pandoc == null ? null : new File(pandoc).getParent();
		return "local({\n" //
				+ "  none <- !nzchar(Sys.getenv('RSTUDIO_PANDOC')) && !nzchar(Sys.which('pandoc'))\n" //
				+ "  p <- strsplit(Sys.getenv('PATH'), .Platform$path.sep, fixed = TRUE)[[1]]\n" //
				+ "  add <- setdiff(c(" + dirs + "), p)\n" //
				+ "  if (length(add) > 0) Sys.setenv(PATH = paste(c(p, add), collapse = .Platform$path.sep))\n" //
				+ (pandocDir == null ? "" : "  if (none) Sys.setenv(RSTUDIO_PANDOC = " + rString(pandocDir) + ")\n") //
				+ "  invisible(NULL)\n" //
				+ "})";
	}

	private static String rString(String s) {
		return "'" + s.replace('\\', '/').replace("'", "\\'") + "'";
	}

	/** The pandoc executable or null. */
	public static String find() {
		return findProgram("pandoc");
	}

	private static String findProgram(String name) {
		boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
		for (String d : folders()) {
			File f = new File(d, windows ? name + ".exe" : name);
			if (f.isFile() && f.canExecute()) {
				return f.getAbsolutePath();
			}
		}
		return null;
	}

	/** The file extension of an output format of the YAML header (e.g., pdf_document). */
	public static String extension(String output) {
		return switch (output) {
		case "pdf_document", "pdf" -> "pdf";
		case "word_document", "word", "docx" -> "docx";
		case "odt_document", "odt" -> "odt";
		case "rtf_document", "rtf" -> "rtf";
		case "epub", "epub_document" -> "epub";
		default -> "html";
		};
	}

	/**
	 * Converts the markdown file to the format of the extension (pdf, docx, odt,
	 * rtf, epub), next to the file.
	 *
	 * @return the created file
	 * @throws IOException with the messages of pandoc if it failed
	 */
	public static Path convert(Path markdownFile, String extension) throws IOException {
		String pandoc = find();
		if (pandoc == null) {
			throw new IOException("Pandoc was not found. Please install pandoc (https://pandoc.org) to create " + extension.toUpperCase()
					+ " files (HTML works without it).");
		}
		String name = markdownFile.getFileName().toString();
		String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
		Path out = markdownFile.resolveSibling(base + "." + extension);
		List<String> cmd = new ArrayList<>(List.of(pandoc, name, "--standalone", "-o", out.getFileName().toString()));
		if (extension.equals("pdf")) {
			String engine = findProgram("xelatex");
			if (engine == null) {
				engine = findProgram("pdflatex");
			}
			if (engine == null) {
				throw new IOException("A PDF needs LaTeX (e.g., TeX Live, MacTeX or MiKTeX; xelatex or pdflatex was not found).");
			}
			cmd.add("--pdf-engine=" + engine);
		}
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(markdownFile.getParent().toFile());
		pb.redirectErrorStream(true);
		/* The programs of pandoc (LaTeX) in the PATH */
		Map<String, String> env = pb.environment();
		env.put("PATH", String.join(File.pathSeparator, folders()));
		Path log = Files.createTempFile("bio7pandoc", ".log");
		pb.redirectOutput(log.toFile());
		Process p = pb.start();
		try {
			if (!p.waitFor(5, TimeUnit.MINUTES)) {
				p.destroyForcibly();
				throw new IOException("Pandoc did not finish within 5 minutes.");
			}
		} catch (InterruptedException e) {
			p.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted");
		}
		String messages = Files.readString(log, StandardCharsets.UTF_8).trim();
		Files.deleteIfExists(log);
		if (p.exitValue() != 0 || !Files.exists(out)) {
			throw new IOException("Pandoc failed" + (messages.isEmpty() ? "." : ":\n" + messages));
		}
		return out;
	}
}
