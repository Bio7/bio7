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
package com.eco.bio7.rbridge.debug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.eco.bio7.Bio7Plugin;

/**
 * The state of an R debug session: the debugged copy of the script (the
 * breakpoints injected), the mapping of its lines to the lines of the editor,
 * the watch expressions and the R helpers of embedded R (variables, call stack,
 * watch expressions, break on error).
 * <p>
 * The debugged copy: the script is wrapped into the function .bio7debugfunc (R
 * steps line by line only inside of a function) and before every breakpoint a
 * line is inserted: browser(), if (condition) browser(), a hit count (#n: stop at
 * the n-th time) or a log point (log: expression: print the value, don't stop).
 * <p>
 * The watch expressions are evaluated in a child environment of the frame: in a
 * frame which R steps through, an evaluation would start the browser of R again.
 */
public final class DebugSession {

	private DebugSession() {
	}

	/**
	 * A position of the debug session: a file (the script or a file loaded with
	 * source()) and its line (1-based).
	 */
	public record Location(Path file, int line) {
		/** The name of the file. */
		public String name() {
			return file.getFileName().toString();
		}
	}

	/*
	 * A file of the session: its source, the name of its copy and the lines of the
	 * copy with an injected line (1-based, sorted); offset: the lines of the copy
	 * before the first line of the source (the wrapper of the script)
	 */
	private record DebugFile(Path source, String copyName, int[] injected, int offset) {
		int toSourceLine(int copyLine) {
			int line = copyLine - offset;
			for (int i : injected) {
				if (i < copyLine) {
					line--;
				} else if (i == copyLine) {
					/* The breakpoint itself: the next line of the copy is its source line */
					return line;
				}
			}
			return line;
		}
	}

	/* The files of the session by the name of their copy (the script first) */
	private static final Map<String, DebugFile> FILES = new java.util.concurrent.ConcurrentHashMap<>();
	/* The debugged script */
	private static volatile DebugFile script;
	/* The source line of the first stop in the script (the first breakpoint) */
	private static int firstBreakpoint;

	/** The preference of the watch expressions (one per line). */
	public static final String WATCH_PREFERENCE = "R_DEBUG_WATCH_EXPRESSIONS";

	/*
	 * The end of a wrapper (one line, also after an error or Stop): the variables
	 * of the wrapper are copied to the target environment (as if the code had run
	 * there), its functions get the target as environment (e.g., x <<- 1 in them
	 * changes the variable of the target); not the functions of the debugger
	 */
	private static String keep(String target) {
		/* One call (one step in the debugger at the end) */
		return ".bio7keep <- function(e, t) for (n in setdiff(ls(e, all.names = TRUE), c('source', 'setRefClass', '.bio7keep', 'ofile'))) { "
				+ "v <- get(n, envir = e); if (is.function(v) && identical(environment(v), e)) environment(v) <- t; assign(n, v, envir = t) }; "
				+ "on.exit(.bio7keep(environment(), " + target + "), add = TRUE)";
	}

	/* The classes of setRefClass are defined in the environment of a wrapper: their methods find the generators */
	private static final String LOCAL_CLASSES = "setRefClass <- function(..., where = parent.frame()) methods::setRefClass(..., where = where)";

	/*
	 * The wrapper of the script (one line, the lines of the script follow): R steps
	 * line by line only in a function. The counters of the hit counts are removed
	 * at the start. source() loads the debugged copies of other files (see
	 * .bio7dbg$src).
	 */
	private static String scriptWrapper(Path source) {
		/* ofile: the path of the script, as in source() (scripts find their folder with it) */
		return SCRIPT_WRAPPER.replace(".bio7debugfunc <- function() {",
				".bio7debugfunc <- function(ofile = " + rString(source.toAbsolutePath().toString().replace("\\", "/")) + ") {");
	}

	private static final String SCRIPT_WRAPPER = ".bio7debugfunc <- function() { rm(list = ls(pattern = '^\\\\.bio7hit_', all.names = TRUE, envir = .GlobalEnv), envir = .GlobalEnv); "
			+ LOCAL_CLASSES + "; "
			+ "source <- function(file, ...) if (exists('.bio7dbg', envir = .GlobalEnv, inherits = FALSE)) .bio7dbg$src(file, ..., caller = parent.frame()) else base::source(file, ...); "
			+ keep(".GlobalEnv");

	/*
	 * The wrapper of a file loaded with source() which has a breakpoint in its top
	 * level code (R steps there line by line only in a function): its variables go
	 * to the environment of source() at the end
	 */
	private static final String SOURCED_WRAPPER = "(function() { " + LOCAL_CLASSES + "; " + keep("parent.env(environment())");

	/**
	 * Writes the debugged copy of the script: the breakpoints (source line ->
	 * expression or null) injected, the script wrapped into .bio7debugfunc. A new
	 * session: the copies of other files are written after it
	 * ({@link #writeSourcedCopy(Path, Map)}). Returns the path of the copy.
	 */
	public static Path writeDebugCopy(Path source, Map<Integer, RBreakpoint> breakpoints) throws IOException {
		FILES.clear();
		Path copy = Files.createTempFile("bio7debug", ".R");
		copy.toFile().deleteOnExit();
		int[] first = new int[1];
		script = write(source, copy, breakpoints, true, first);
		firstBreakpoint = first[0];
		return copy;
	}

	/**
	 * Writes the debugged copy of a file which the script loads with source() (its
	 * breakpoints injected; wrapped only with a breakpoint in its top level code).
	 * Returns the path of the copy.
	 */
	public static Path writeSourcedCopy(Path source, Map<Integer, RBreakpoint> breakpoints) throws IOException {
		Path copy = Files.createTempFile("bio7debug", ".R");
		copy.toFile().deleteOnExit();
		write(source, copy, breakpoints, false, new int[1]);
		return copy;
	}

	/**
	 * A file changed during the session (its breakpoints or its code): a new copy
	 * (the running code keeps the old one and its lines). Returns the path of the
	 * copy.
	 */
	public static Path rewriteCopy(Path source, Map<Integer, RBreakpoint> breakpoints) throws IOException {
		Path copy = Files.createTempFile("bio7debug", ".R");
		copy.toFile().deleteOnExit();
		write(source, copy, breakpoints, isScript(source), new int[1]);
		return copy;
	}

	/** The file is the debugged script. */
	public static boolean isScript(Path source) {
		DebugFile f = script;
		return f != null && f.source().equals(source.toAbsolutePath());
	}

	/**
	 * Evaluates an R expression which returns a character vector, while R waits in
	 * the debugger (browser) or is idle (embedded R); null if R is not available or
	 * busy.
	 */
	static String[] rStringsNow(String expression) {
		try {
			if (!com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
				return null;
			}
			if (com.eco.bio7.rbridge.backend.EmbeddedR.isBrowsing()) {
				String out = com.eco.bio7.rbridge.backend.EmbeddedR.consoleCapture("cat('@@Q@@', paste(" + expression + ", collapse = '\\t'), '\\n', sep = '')", 10000, false);
				java.util.regex.Matcher m = java.util.regex.Pattern.compile("@@Q@@([^\\n]*)").matcher(out == null ? "" : out);
				if (!m.find()) {
					return null;
				}
				return m.group(1).isEmpty() ? new String[0] : m.group(1).split("\t");
			}
			if (com.eco.bio7.rbridge.backend.EmbeddedR.isBusy()) {
				return null;
			}
			return com.eco.bio7.rbridge.RUtil.fromRStrings(expression);
		} catch (Exception | LinkageError e) {
			return null;
		}
	}

	/** R code which uses the new copy of a changed file (see .bio7dbg$update): prints "@@U@@names". */
	public static String updateR(Path source, Path copy) {
		return ".bio7dbg$update(" + rString(source.toAbsolutePath().toString().replace("\\", "/")) + ", " + rString(copy.toAbsolutePath().toString().replace("\\", "/")) + ", "
				+ (isScript(source) ? "TRUE" : "FALSE") + ")";
	}

	/*
	 * Writes a copy: where each breakpoint stops (valid R): a line before a
	 * statement, in the function of the line (e.g., a method in a list of a class)
	 * or the next line where R can stop (e.g., for an argument of a call)
	 */
	private static DebugFile write(Path source, Path copy, Map<Integer, RBreakpoint> breakpoints, boolean isScript, int[] first) throws IOException {
		List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
		/* R Markdown, Quarto: the R chunks (the other lines empty, the line numbers stay) */
		boolean document = RChunks.isDocument(source);
		boolean params = document && RChunks.hasParams(lines);
		if (document) {
			lines = RChunks.code(lines);
		}
		String key = copy.getFileName().toString();
		RBreakLines where = new RBreakLines(lines);
		Map<Integer, String> before = new java.util.HashMap<>();
		Map<Integer, String> inline = new java.util.HashMap<>();
		boolean topLevelStop = false;
		for (int sourceLine : new java.util.TreeSet<>(breakpoints.keySet())) {
			RBreakpoint bp = breakpoints.get(sourceLine);
			for (int l = sourceLine - 1; l >= 0 && l < lines.size(); l++) {
				if (before.containsKey(l) || inline.containsKey(l)) {
					/* A breakpoint which stops there already */
					break;
				}
				/* The log points of other files name the file */
				String stop = injection(key, isScript ? "" : source.getFileName().toString().replace("\\", "/").replace("\"", "") + " ", l + 1, bp);
				if (where.startsStatement(l)) {
					before.put(l, stop);
					topLevelStop |= where.topLevel(l);
				} else {
					String text = where.inline(l, stop);
					if (text == null) {
						/*
						 * The line of the breakpoint is in a statement (e.g., an argument of a call):
						 * before this statement (not in a later function or statement)
						 */
						int e = l == sourceLine - 1 ? where.enclosingStatement(l) : -1;
						if (e >= 0) {
							if (!before.containsKey(e)) {
								before.put(e, injection(key, isScript ? "" : source.getFileName().toString().replace("\\", "/").replace("\"", "") + " ", e + 1, bp));
								topLevelStop |= where.topLevel(e);
							}
							l = e;
						} else {
							continue;
						}
					} else {
						inline.put(l, text);
					}
				}
				if (first[0] == 0 || l + 1 < first[0]) {
					first[0] = l + 1;
				}
				break;
			}
		}
		/*
		 * After a loop: a stop while the user steps (R leaves the stepping mode at the
		 * end of a loop which started without it, e.g., at a breakpoint in the loop).
		 * In functions and in the wrapped top level code.
		 */
		boolean wrapped = isScript || topLevelStop;
		for (int l : where.afterLoops()) {
			if (!before.containsKey(l) && !inline.containsKey(l) && (wrapped || !where.topLevel(l))) {
				before.put(l, injection(key, "", l + 1, AFTER_LOOP));
			}
		}
		String wrapper = isScript ? scriptWrapper(source) : topLevelStop ? SOURCED_WRAPPER : null;
		if (isScript && params) {
			/* The params of the YAML header (as in rmarkdown::render) */
			wrapper += "; params <- if (requireNamespace('rmarkdown', quietly = TRUE)) lapply(rmarkdown::yaml_front_matter("
					+ rString(source.toAbsolutePath().toString().replace("\\", "/")) + ")$params, function(p) if (is.list(p) && !is.null(p$value)) p$value else p) else list()";
		}
		List<String> out = new ArrayList<>();
		List<Integer> inj = new ArrayList<>();
		if (wrapper != null) {
			out.add(wrapper);
		}
		int offset = out.size();
		for (int i = 0; i < lines.size(); i++) {
			if (before.containsKey(i)) {
				out.add(before.get(i));
				inj.add(out.size());
			}
			out.add(inline.getOrDefault(i, lines.get(i)));
		}
		if (wrapper != null) {
			out.add(isScript ? "}" : "})()");
		}
		Files.write(copy, out, StandardCharsets.UTF_8);
		DebugFile f = new DebugFile(source.toAbsolutePath(), key, inj.stream().mapToInt(Integer::intValue).toArray(), offset);
		FILES.put(key, f);
		return f;
	}

	/*
	 * The line inserted before a breakpoint: browser(), a condition, a hit count
	 * (stop once, at the n-th time; with a condition: the n-th time it is true) and
	 * a log point (print the value instead of stopping), also combined. key: the
	 * name of the copy (the breakpoint stores "copy#line": browser() itself doesn't
	 * tell R's position)
	 */
	/* The stop after a loop: only while the user steps in this function or a caller (see .bio7dbg$arm) */
	private static final RBreakpoint AFTER_LOOP = new RBreakpoint(".bio7dbg$stepHere(environment())", 0, "", null);

	static String injection(String key, String fileLabel, int sourceLine, RBreakpoint bp) {
		String stop = "{ assign('.bio7bp', '" + key + "#" + sourceLine + "', envir = .GlobalEnv); browser() }";
		if (bp == null || bp.isPlain()) {
			return stop;
		}
		if (bp.legacy() != null) {
			/* An expression of the former dialog ("if (x == 5) browser()") */
			return bp.legacy().replace("browser()", stop);
		}
		String action = stop;
		if (!bp.log().isEmpty()) {
			/* Log point: print the value, don't stop */
			String x = bp.log();
			String label = x.replace("\\", "\\\\").replace("\"", "\\\"");
			action = "cat(\"\\u25B6 " + fileLabel + "line " + sourceLine + ": " + label + " = \", tryCatch(paste(format(" + x
					+ "), collapse = ' '), error = function(e) conditionMessage(e)), \"\\n\", sep = \"\")";
		}
		String cond = bp.condition().isEmpty() ? null : "isTRUE(" + bp.condition() + ")";
		if (bp.hitCount() > 1) {
			/* Counted only when the condition is true (&&) */
			String v = ".bio7hit_" + key.replaceAll("[^A-Za-z0-9]", "_") + "_" + sourceLine;
			String count = "(assign('" + v + "', get0('" + v + "', envir = .GlobalEnv, ifnotfound = 0) + 1, envir = .GlobalEnv) == " + bp.hitCount() + ")";
			return "if (" + (cond == null ? "" : cond + " && ") + count + ") " + action;
		}
		return cond == null ? action : "if (" + cond + ") " + action;
	}

	/**
	 * The line of the editor of a line of the debugged copy of the script (the
	 * wrapper and the injected lines removed); an injected line maps to the line of
	 * its breakpoint.
	 */
	public static int toSourceLine(int copyLine) {
		DebugFile f = script;
		return f == null ? copyLine : f.toSourceLine(copyLine);
	}

	/**
	 * The position of a line which R reports (a file and its line): a debugged copy
	 * is the line of its source, another file (e.g., loaded with source() without
	 * breakpoints) its line; null if the file is not known.
	 */
	public static Location location(String rFile, int line) {
		if (rFile == null || rFile.isBlank() || line <= 0) {
			return null;
		}
		String file = rFile.trim();
		String name = file.substring(Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\')) + 1);
		DebugFile f = FILES.get(name);
		if (f != null) {
			return new Location(f.source(), f.toSourceLine(line));
		}
		try {
			Path p = Path.of(file);
			if (p.isAbsolute() && Files.isRegularFile(p)) {
				return new Location(p, line);
			}
		} catch (RuntimeException e) {
			/* Not a path */
		}
		return null;
	}

	/** The line of R (a file and its line) is a line inserted into a debugged copy (a breakpoint, a stop after a loop). */
	public static boolean isInjected(String rFile, int line) {
		if (rFile == null) {
			return false;
		}
		String file = rFile.trim();
		DebugFile f = FILES.get(file.substring(Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\')) + 1));
		return f != null && java.util.Arrays.binarySearch(f.injected(), line) >= 0;
	}

	/** A position "file#line" (of R or of a breakpoint), null if unknown. */
	public static Location location(String fileHashLine) {
		if (fileHashLine == null) {
			return null;
		}
		int i = fileHashLine.lastIndexOf('#');
		if (i <= 0) {
			return null;
		}
		try {
			return location(fileHashLine.substring(0, i), Integer.parseInt(fileHashLine.substring(i + 1).trim()));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * The position of a breakpoint where R stopped ("copy#line", stored by the
	 * breakpoint: the line is the one of the source), null if unknown.
	 */
	public static Location breakpointLocation(String copyHashLine) {
		if (copyHashLine == null) {
			return null;
		}
		int i = copyHashLine.lastIndexOf('#');
		DebugFile f = i > 0 ? FILES.get(copyHashLine.substring(0, i).trim()) : null;
		if (f == null) {
			return null;
		}
		try {
			return new Location(f.source(), Integer.parseInt(copyHashLine.substring(i + 1).trim()));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** The debugged script (null if no session). */
	public static Path script() {
		DebugFile f = script;
		return f == null ? null : f.source();
	}

	/** The files of the session (the script and the files with breakpoints). */
	public static List<Path> files() {
		List<Path> out = new ArrayList<>();
		for (DebugFile f : FILES.values()) {
			out.add(f.source());
		}
		return out;
	}

	/**
	 * R code which tells .bio7dbg$src the copies of the files loaded with source()
	 * (source path -> copy path), after the helpers.
	 */
	public static String copiesR(Map<Path, Path> copies) {
		if (copies.isEmpty()) {
			return ".bio7dbg$copies <- character(0)";
		}
		List<String> sources = new ArrayList<>();
		List<String> paths = new ArrayList<>();
		for (Map.Entry<Path, Path> e : copies.entrySet()) {
			sources.add(e.getKey().toAbsolutePath().toString().replace("\\", "/"));
			paths.add(e.getValue().toAbsolutePath().toString().replace("\\", "/"));
		}
		return ".bio7dbg$copies <- stats::setNames(" + rStrings(paths) + ", normalizePath(" + rStrings(sources) + ", winslash = '/', mustWork = FALSE)); "
				+ ".bio7dbg$origin <- stats::setNames(normalizePath(" + rStrings(sources) + ", winslash = '/', mustWork = FALSE), normalizePath(" + rStrings(paths)
				+ ", winslash = '/', mustWork = FALSE))";
	}

	/** The source line of the first breakpoint (0 if none). */
	public static int firstBreakpoint() {
		return firstBreakpoint;
	}

	/** The watch expressions (stored in the preferences). */
	public static List<String> watchExpressions() {
		String s = Bio7Plugin.getDefault().getPreferenceStore().getString(WATCH_PREFERENCE);
		List<String> list = new ArrayList<>();
		for (String e : s.split("\n")) {
			if (!e.isBlank()) {
				list.add(e.trim());
			}
		}
		return list;
	}

	public static void setWatchExpressions(List<String> expressions) {
		Bio7Plugin.getDefault().getPreferenceStore().setValue(WATCH_PREFERENCE, String.join("\n", expressions));
	}

	/** A Java string as an R string literal. */
	public static String rString(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
	}

	/** Java strings as an R character vector. */
	public static String rStrings(List<String> list) {
		if (list.isEmpty()) {
			return "character(0)";
		}
		StringBuilder sb = new StringBuilder("c(");
		for (int i = 0; i < list.size(); i++) {
			sb.append(i > 0 ? ", " : "").append(rString(list.get(i)));
		}
		return sb.append(")").toString();
	}

	/**
	 * The R helpers of embedded R (defined once per debug session, outside of the
	 * debugger): the variables, the watch expressions and the call stack of a frame
	 * in one output (lines @@V@@, @@W@@, @@S@@), the handler of errors (@@E@@,
	 * browses the frame of the error) and the export of a variable for the table,
	 * ImageJ or a plot.
	 */
	public static final String HELPERS = String.join("\n", Arrays.asList(
			".bio7dbg <- local({",
			"  armed <- TRUE",
			"  one <- function(s) { s <- gsub('[\\t\\r\\n]+', ' ', s); if (nchar(s) > 300) paste0(substr(s, 1, 300), ' ...') else s }",
			"  size <- function(x) { b <- tryCatch(as.numeric(utils::object.size(x)), error = function(e) NA)",
			"    if (is.na(b)) '' else if (b >= 1048576) sprintf('%.1f MB', b / 1048576) else if (b >= 1024) sprintf('%.1f KB', b / 1024) else sprintf('%d B', as.integer(b)) }",
			"  ours <- function(sr) !is.null(sr) && isTRUE(grepl('bio7debug', utils::getSrcFilename(sr)))",
			/* The file of a srcref (full path), '' if none */
			"  srcfile <- function(sr) { if (is.null(sr)) return(''); f <- tryCatch(utils::getSrcFilename(sr, full.names = TRUE), error = function(e) character(0)); if (length(f) == 0L) '' else f[1] }",
			/*
			 * source() of the debugged script: a file with breakpoints is loaded as its
			 * debugged copy (in the folder of the file with chdir = TRUE), other files with
			 * their source references (the debugger shows their lines)
			 */
			"  copies <- character(0)",
			/* The files loaded with source() during the session (the next Debug of such a file starts the script again) */
			"  sourced <- character(0)",
			"  src <- function(file, ..., local = FALSE, chdir = FALSE, keep.source = TRUE, caller = parent.frame()) {",
			"    if (isTRUE(local)) local <- caller",
			"    f <- if (is.character(file) && length(file) == 1L) normalizePath(file, winslash = '/', mustWork = FALSE) else NA_character_",
			"    if (!is.na(f)) sourced <<- unique(c(sourced, f))",
			"    copy <- if (!is.na(f) && f %in% names(copies)) copies[[f]] else NA_character_",
			"    if (is.na(copy)) { r <- base::source(file, ..., local = local, chdir = chdir, keep.source = keep.source); if (length(pending)) applyPending(TRUE); return(invisible(r)) }",
			"    if (isTRUE(chdir)) { owd <- setwd(dirname(f)); on.exit(setwd(owd), add = TRUE) }",
			/* As source(): the expressions in envir, ofile is the path of the file (not of the copy) */
			"    envir <- if (isTRUE(local)) caller else if (isFALSE(local)) globalenv() else local",
			"    ofile <- f",
			"    exprs <- parse(copy, keep.source = TRUE, encoding = 'UTF-8')",
			"    value <- NULL",
			/* After each expression: a temporary breakpoint of Run to Cursor for definitions which exist now */
			"    for (i in seq_along(exprs)) { value <- withVisible(eval(exprs[i], envir)); if (length(pending)) applyPending(TRUE) }",
			"    invisible(value) }",
			/*
			 * source() for the whole session (also in the files loaded with source(): their
			 * source() calls load the debugged copies too): a global function, removed at
			 * the end (cleanup); not if the global environment has its own source()
			 */
			/*
			 * Without the compiler of R during the session (the stepping mode of R
			 * doesn't work in compiled functions); the former level at the end
			 */
			"  install <- function() {",
			"    if (!exists('.bio7jit', envir = globalenv(), inherits = FALSE)) assign('.bio7jit', tryCatch(compiler::enableJIT(0), error = function(e) NA), envir = globalenv())",
			"    if (!exists('source', envir = globalenv(), inherits = FALSE)) {",
			"      f <- function(file, ...) .bio7dbg$src(file, ..., caller = parent.frame()); attr(f, 'bio7dbg') <- TRUE",
			"      assign('source', f, envir = globalenv()) }",
			"    invisible() }",
			"  cleanup <- function() {",
			"    j <- get0('.bio7jit', envir = globalenv(), inherits = FALSE)",
			"    if (!is.null(j)) { if (!is.na(j)) try(compiler::enableJIT(j), silent = TRUE); rm('.bio7jit', envir = globalenv()) }",
			"    s <- get0('source', envir = globalenv(), inherits = FALSE)",
			"    if (isTRUE(attr(s, 'bio7dbg'))) rm('source', envir = globalenv())",
			"    invisible() }",
			/*
			 * The functions and classes which are already defined (e.g., loaded with
			 * source() before the session) from a file with breakpoints: defined again from
			 * its debugged copy (only these assignments, the other code of the file is not
			 * run again), so that their breakpoints stop
			 */
			"  fromFile <- function(x) {",
			/* The generator of a reference class is a function, too: its methods first */
			"    fs <- if (inherits(x, 'S7_class')) list(attr(x, 'constructor'), attr(x, 'validator'))",
			"      else if (methods::is(x, 'refObjectGenerator')) tryCatch(as.list(x$def@refMethods), error = function(e) list())",
			"      else if (is.function(x)) list(x) else list()",
			"    out <- character(0)",
			"    for (fn in fs) if (is.function(fn)) { f <- tryCatch(utils::getSrcFilename(fn, full.names = TRUE), error = function(e) character(0))",
			"      if (length(f) == 1L && nzchar(f)) { p <- normalizePath(f, winslash = '/', mustWork = FALSE); out <- c(out, if (p %in% names(origin)) origin[[p]] else p) } }",
			"    unique(out) }",
			/* The debugged copies of the session (copy -> source, normalized) */
			"  origin <- character(0)",
			/*
			 * A definition: a function, a class of setRefClass, R6 or S7 (not an S7
			 * generic: defined again it would lose the methods of other files)
			 */
			"  defs <- function(rhs) is.call(rhs) && (identical(rhs[[1]], as.name('function')) || any(grepl('^((methods|R6|S7)::)?(setRefClass|R6Class|new_class)$', deparse(rhs[[1]]))))",
			/* An S7 method: method(generic, class) <- function */
			"  s7method <- function(e) is.call(e) && length(e) == 3L && as.character(e[[1]])[1] %in% c('<-', '=') && is.call(e[[2]]) && any(deparse(e[[2]][[1]]) %in% c('method', 'S7::method')) && length(e[[2]]) >= 3L",
			/* The statements of parsed code (a wrapped copy: the statements in the wrapper) */
			"  statements <- function(exprs) {",
			"    if (length(exprs) == 1L) { e <- exprs[[1]]",
			"      if (is.call(e) && length(e) == 3L && identical(e[[2]], as.name('.bio7debugfunc'))) return(as.list(e[[3]][[3]])[-1])",
			"      if (is.call(e) && is.call(e[[1]]) && identical(e[[1]][[1]], as.name('(')) && is.call(e[[1]][[2]])) return(as.list(e[[1]][[2]][[3]])[-1]) }",
			"    as.list(exprs) }",
			/* The first definition of each name (name <- function / class) */
			"  assigns <- function(exprs) { out <- list()",
			"    for (e in statements(exprs)) if (is.call(e) && length(e) == 3L && as.character(e[[1]])[1] %in% c('<-', '=', '<<-') && is.name(e[[2]]) && defs(e[[3]])) {",
			"      n <- as.character(e[[2]]); if (is.null(out[[n]])) out[[n]] <- e }",
			"    out }",
			/* A function without source references: the same code as the definition of the file */
			"  same <- function(x, e) is.function(x) && is.call(e[[3]]) && identical(e[[3]][[1]], as.name('function')) &&",
			"    identical(deparse(utils::removeSource(x)), deparse(utils::removeSource(eval(e[[3]], globalenv()))))",
			/*
			 * Defines the functions and classes of a file again from its debugged copy in
			 * env: only the definitions whose name exists there (from this file, or the
			 * same code; all: every existing name), the other code is not run again
			 */
			"  redefine <- function(copy, source, env, all = FALSE) {",
			"    orig <- if (all) list() else tryCatch(assigns(parse(source, keep.source = FALSE)), error = function(e) list())",
			"    new <- tryCatch(assigns(parse(copy, keep.source = TRUE)), error = function(e) list())",
			"    done <- character(0)",
			"    for (n in names(new)) {",
			"      if (!exists(n, envir = env, inherits = FALSE) || (all && n %in% c('setRefClass', 'source', '.bio7keep'))) next",
			"      x <- get(n, envir = env)",
			"      if (!is.function(x) && !methods::is(x, 'refObjectGenerator') && !inherits(x, 'R6ClassGenerator') && !inherits(x, 'S7_class')) next",
			"      if (!all && !(source %in% fromFile(x)) && !(!is.null(orig[[n]]) && isTRUE(tryCatch(same(x, orig[[n]]), error = function(e) FALSE)))) next",
			"      tryCatch({ eval(new[[n]], envir = env); done <- c(done, n) }, error = function(err) NULL) }",
			/*
			 * The S7 methods of the copy: registered again if their generic or class was
			 * defined again, the method of the generic comes from this file (or all)
			 */
			"    if (requireNamespace('S7', quietly = TRUE)) for (e in statements(tryCatch(parse(copy, keep.source = TRUE), error = function(err) expression()))) if (s7method(e)) {",
			"      gen <- deparse(e[[2]][[2]]); cls <- deparse(e[[2]][[3]])",
			"      cur <- tryCatch(S7::method(eval(e[[2]][[2]], env), eval(e[[2]][[3]], env)), error = function(err) NULL)",
			"      if (!(all || gen %in% done || cls %in% done || (!is.null(cur) && source %in% fromFile(cur)))) next",
			"      tryCatch({ eval(e, envir = env); done <- c(done, paste0(gen, '(', cls, ')')) }, error = function(err) NULL) }",
			"    done }",
			/*
			 * The functions and classes which are already defined (e.g., loaded with
			 * source() before the session) from a file with breakpoints: defined again
			 * from its debugged copy, so that their breakpoints stop
			 */
			"  preload <- function() {",
			"    done <- character(0)",
			"    for (f in names(copies)) done <- c(done, redefine(copies[[f]], f, globalenv()))",
			"    if (length(done)) cat('Bio7 debugger: breakpoints active in the loaded', paste(unique(done), collapse = ', '), '\\n')",
			"    invisible(done) }",
			/* The frame of the debugged script (its functions are variables there), NULL if none */
			"  scriptFrame <- function() { fn <- get0('.bio7debugfunc', envir = globalenv(), inherits = FALSE)",
			"    for (i in rev(seq_len(sys.nframe()))) if (identical(sys.function(i), fn)) return(sys.frame(i))",
			"    NULL }",
			/*
			 * A file changed during the session (breakpoints or code): its new copy is
			 * used by source() and its functions and classes are defined again (the
			 * script: in its frame); the running code keeps the old version
			 */
			/*
			 * The methods of reference classes from a changed file: the objects keep copies
			 * of their methods (and subclasses copies of the inherited ones), a new class
			 * definition doesn't reach them. The methods (of the file) in all class
			 * definitions and in the objects reachable from the global environment and the
			 * frames get the code of the copy (its breakpoints). Returns the number.
			 */
			"  patchMethods <- function(source, copy) {",
			"    src <- function(fn) { f <- tryCatch(utils::getSrcFilename(fn, full.names = TRUE), error = function(e) character(0)); if (length(f)) { p <- normalizePath(f, winslash = '/', mustWork = FALSE); cp <- normalizePath(copies, winslash = '/', mustWork = FALSE)",
			"      if (p %in% names(origin)) origin[[p]] else if (p %in% cp) names(copies)[match(p, cp)] else p } else '' }",
			"    new <- list()",
			"    for (e in statements(tryCatch(parse(copy, keep.source = TRUE), error = function(err) expression()))) {",
			"      if (!is.call(e) || length(e) != 3L || !as.character(e[[1]])[1] %in% c('<-', '=')) next",
			"      r <- e[[3]]; if (!is.call(r) || !any(grepl('setRefClass$', deparse(r[[1]])))) next",
			"      m <- r$methods; if (!is.call(m) || !identical(m[[1]], as.name('list'))) next",
			"      for (n in setdiff(names(m), '')) if (is.call(m[[n]]) && identical(m[[n]][[1]], as.name('function'))) new[[n]] <- eval(m[[n]], globalenv()) }",
			"    if (!length(new)) return(0L)",
			"    patch <- function(f, n) { g <- new[[n]]",
			"      p <- function(h) { e <- environment(h); body(h) <- body(g); environment(h) <- e; attr(h, 'srcref') <- attr(g, 'srcref'); h }",
			"      if (isS4(f)) { f@.Data <- p(f@.Data); f } else p(f) }",
			"    count <- 0L",
			"    fix <- function(env) for (n in intersect(names(new), ls(env, all.names = TRUE))) {",
			"      f <- get(n, envir = env); if (is.function(f) && identical(src(f), source)) { assign(n, patch(f, n), envir = env); count <<- count + 1L } }",
			"    value <- function(v, env) tryCatch(get(v, envir = env), error = function(e) NULL)",
			"    roots <- list(globalenv()); fr <- scriptFrame(); if (!is.null(fr)) roots[[2L]] <- fr",
			"    for (w in roots) for (cl in tryCatch(methods::getClasses(w), error = function(e) character(0))) {",
			"      d <- tryCatch(methods::getClassDef(cl, where = w), error = function(e) NULL); if (methods::is(d, 'refClassRepresentation')) fix(d@refMethods) }",
			"    seen <- new.env()",
			"    visit <- function(x, depth = 0L) {",
			"      if (depth > 6L) return(invisible())",
			"      if (methods::is(x, 'envRefClass')) x <- as.environment(x) else if (!is.environment(x)) { if (is.list(x) && length(x) < 1e5) for (v in x) visit(v, depth + 1L); return(invisible()) }",
			"      key <- format(x); if (!is.null(seen[[key]])) return(invisible()); assign(key, TRUE, envir = seen)",
			"      if (exists('.refClassDef', envir = x, inherits = FALSE)) { fix(x); for (v in ls(x, all.names = TRUE)) visit(value(v, x), depth + 1L) } }",
			"    for (v in ls(globalenv(), all.names = TRUE)) visit(value(v, globalenv()))",
			"    for (i in seq_len(max(sys.nframe() - 1L, 0L))) { f <- sys.frame(i); for (v in ls(f, all.names = TRUE)) visit(value(v, f)) }",
			"    count }",
			"  update <- function(source, copy, script = FALSE) {",
			"    source <- normalizePath(source, winslash = '/', mustWork = FALSE)",
			"    origin[normalizePath(copy, winslash = '/', mustWork = FALSE)] <<- source",
			"    done <- if (script) { fr <- scriptFrame(); if (is.null(fr)) character(0) else redefine(copy, source, fr, all = TRUE) }",
			"      else { copies[source] <<- copy; redefine(copy, source, globalenv()) }",
			"    n <- tryCatch(patchMethods(source, copy), error = function(e) 0L); if (n > 0L) done <- c(done, paste(n, 'methods of objects'))",
			"    cat('@@U@@', paste(unique(done), collapse = ', '), '\\n', sep = '')",
			"    invisible(done) }",
			/*
			 * A temporary breakpoint of Run to Cursor in a file whose functions or classes
			 * don't exist yet (e.g., R waits before the class definition): the copy is
			 * used when they exist (after an expression of source(), or a step)
			 */
			"  pending <- character(0)",
			"  pendingScript <- logical(0)",
			"  later <- function(source, copy, script = FALSE) {",
			"    s <- normalizePath(source, winslash = '/', mustWork = FALSE); pending[s] <<- copy; pendingScript[s] <<- script; invisible() }",
			"  applyPending <- function(quiet = FALSE) {",
			"    for (s in names(pending)) { d <- character(0); utils::capture.output(d <- update(s, pending[[s]], pendingScript[[s]]))",
			"      if (length(d)) { pending <<- pending[names(pending) != s]; pendingScript <<- pendingScript[names(pendingScript) != s] } }",
			"    if (!quiet) cat('@@P@@', length(pending), '\\n', sep = '')",
			"    invisible() }",
			"  clearPending <- function() { pending <<- character(0); pendingScript <<- logical(0); invisible() }",
			/* The functions of the call stack (frame, file, first and last line), e.g., for Run to Cursor to a calling function */
			"  frames <- function() { n <- sys.nframe() - 1L; for (i in seq_len(n)) { sr <- attr(sys.function(i), 'srcref'); if (is.null(sr)) next",
			"    cat('@@F@@', i, '\\t', srcfile(sr), '\\t', sr[1], '\\t', sr[3], '\\n', sep = '') }; invisible() }",
			/*
			 * A stop in the calling function after a return (Next at the end of a function,
			 * Step Return, Run to Cursor to a line of a calling function): R doesn't go on
			 * stepping in a caller which runs without the stepping mode. The frames above
			 * frame "from" (to the browsed one) get an exit hook: it opens the browser in the
			 * caller (@@R@@), one n there continues stepping in the caller (see
			 * DebugProgress). Only callers with source references (code of the user).
			 */
			"  hooked <- list()",
			"  hooksActive <- FALSE",
			"  hookFrames <- function(from = sys.nframe() - 2L) { top <- sys.nframe() - 1L",
			"    if (top >= 1L && top > from) for (i in seq(top, max(from + 1L, 1L))) {",
			"      p <- sys.parents()[i]; if (p < 1L || is.null(attr(sys.function(p), 'srcref'))) next",
			"      env <- sys.frame(i); dup <- FALSE; for (h in hooked) if (identical(h, env)) { dup <- TRUE; break }",
			"      if (dup) next",
			"      hooked[[length(hooked) + 1L]] <<- env",
			"      do.call(on.exit, list(bquote(.(returned)(.(env), .(sys.frame(p)))), add = TRUE), envir = env) }",
			"    hooksActive <<- TRUE; invisible() }",
			"  returned <- function(env, caller) { found <- FALSE",
			"    for (k in seq_along(hooked)) if (identical(hooked[[k]], env)) { hooked[[k]] <<- NULL; found <- TRUE; break }",
			"    if (!found || !hooksActive) return(invisible())",
			"    cat('@@R@@\\n'); eval(quote(browser()), caller) }",
			"  dims <- function(x) { d <- dim(x); if (!is.null(d)) paste(d, collapse = ' x ') else as.character(length(x)) }",
			"  short <- function(x) one(tryCatch(",
			"    if (is.atomic(x) && is.null(dim(x)) && length(x) <= 10) paste(format(x), collapse = ' ')",
			"    else paste(utils::capture.output(utils::str(x, max.level = 0, vec.len = 3, give.attr = FALSE)), collapse = ' '),",
			"    error = function(e) '<unreadable>'))",
			"  vars <- function(env) for (v in sort(ls(envir = env))) {",
			"    x <- tryCatch(get(v, envir = env), error = function(e) NULL)",
			/* Not the functions of the debugger (source and setRefClass of the session) */
			"    if (v %in% c('source', 'setRefClass') && is.function(x) && any(grepl('bio7dbg|methods::setRefClass', deparse(body(x))))) next",
			"    if (v == 'ofile' && identical(env, scriptFrame())) next",
			"    cat('@@V@@', v, '\\t', class(x)[1], '\\t', dims(x), '\\t', size(x), '\\t', short(x), '\\n', sep = '') }",
			"  watch <- function(exprs, env) for (i in seq_along(exprs)) {",
			"    v <- tryCatch(list(value = eval(parse(text = exprs[i]), envir = new.env(parent = env))), error = function(e) e)",
			"    val <- if (inherits(v, 'error')) one(paste('Error:', conditionMessage(v))) else short(v$value)",
			"    cat('@@W@@', i, '\\t', val, '\\n', sep = '') }",
			"  stack <- function(calls) for (i in seq_along(calls)) {",
			"    cl <- calls[[i]]; sr <- attr(cl, 'srcref'); txt <- one(paste(deparse(cl, width.cutoff = 200L)[1], collapse = ''))",
			"    if (grepl('^(\\\\.bio7dbg|withCallingHandlers|eval\\\\(quote\\\\(browser|h\\\\(simpleError|\\\\.handleSimpleError|\\\\(function \\\\(expr\\\\)|withVisible\\\\(eval\\\\(expr|eval\\\\(expr, globalenv)', txt)) next",
			"    cat('@@S@@', i, '\\t', srcfile(sr), '#', if (is.null(sr)) NA else sr[1], '\\t', txt, '\\n', sep = '') }",
			"  state <- function(env, exprs, frame = 0) {",
			"    armed <<- FALSE",
			"    calls <- sys.calls(); calls <- calls[-length(calls)]",
			"    cat('@@FRAME@@', frame, '\\n', sep = '')",
			"    bp <- get0('.bio7bp', envir = .GlobalEnv, ifnotfound = NA); assign('.bio7bp', NA, envir = .GlobalEnv)",
			"    cat('@@BP@@', bp, '\\n', sep = '')",
			"    vars(env); watch(exprs, env); stack(calls); invisible() }",
			/* step: the user steps (n, s, f, Run to Cursor), the stops after loops are active (else Continue) */
			"  stepping <- FALSE",
			/* The frames of the call stack at the step: only there a loop end stops (not in a function which a step runs over) */
			"  stepEnvs <- list()",
			"  arm <- function(step = FALSE) { armed <<- TRUE; stepping <<- isTRUE(step); stepEnvs <<- if (stepping) head(sys.frames(), -1L) else list(); hooksActive <<- FALSE; invisible() }",
			"  stepHere <- function(env) { if (!stepping) return(FALSE); for (e in stepEnvs) if (identical(e, env)) return(TRUE); FALSE }",
			/*
			 * Step Into of a line: the methods which the line calls (obj$method() of a
			 * reference class or R6 object, generic(x) of S7) get debug(), the step is
			 * then n (R stops in the method, not in R's code of $ or of the dispatch);
			 * untarget() removes the marks after the step.
			 * code: the lines from the current one (parsed until complete). Prints
			 * "@@T@@n" (n methods, -1: not parsed).
			 */
			"  targets <- list()",
			"  stepInto <- function(code, env) {",
			"    targets <<- list(); exprs <- NULL",
			"    for (k in seq_along(code)) { exprs <- tryCatch(parse(text = code[seq_len(k)], keep.source = FALSE), error = function(err) NULL); if (!is.null(exprs)) break }",
			"    if (is.null(exprs) || length(exprs) == 0L) { cat('@@T@@-1\\n'); return(invisible()) }",
			"    simple <- function(x) is.name(x) || (is.call(x) && identical(x[[1]], as.name('$')) && simple(x[[2]]) && is.name(x[[3]]))",
			"    add <- function(fn) if (is.function(fn) && !is.primitive(fn)) targets[[length(targets) + 1L]] <<- fn",
			"    walk <- function(x) {",
			"      if (!is.call(x)) return(invisible())",
			"      f <- x[[1]]",
			"      if (is.call(f) && identical(f[[1]], as.name('$')) && simple(f[[2]])) {",
			"        obj <- tryCatch(eval(f[[2]], env), error = function(err) NULL)",
			"        if (methods::is(obj, 'envRefClass') || inherits(obj, 'R6')) add(tryCatch(eval(f, env), error = function(err) NULL))",
			"      } else if (is.name(f) && length(x) >= 2L && simple(x[[2]])) {",
			"        fn <- get0(as.character(f), envir = env)",
			"        if (inherits(fn, 'S7_generic')) add(tryCatch(S7::method(fn, object = eval(x[[2]], env)), error = function(err) NULL))",
			"      }",
			"      args <- as.list(x)[-1]",
			"      for (i in seq_along(args)) if (!(is.name(args[[i]]) && !nzchar(as.character(args[[i]])))) walk(args[[i]])",
			"      if (is.call(f)) walk(f)",
			"      invisible() }",
			"    for (e in exprs) walk(e)",
			"    for (fn in targets) debug(fn)",
			"    cat('@@T@@', length(targets), '\\n', sep = '')",
			"    invisible() }",
			/* After the step: the marks of the methods which were not called are removed */
			"  untarget <- function() { for (fn in targets) suppressWarnings(try(undebug(fn), silent = TRUE)); targets <<- list(); invisible() }",
			"  preview <- function(x) {",
			"    op <- options(width = 100); on.exit(options(op))",
			"    out <- tryCatch(utils::capture.output(",
			"      if (is.data.frame(x) || is.matrix(x)) print(utils::head(x, c(8L, 10L)))",
			"      else if (is.atomic(x)) print(utils::head(x, 40))",
			"      else utils::str(x, max.level = 1, list.len = 12, give.attr = FALSE)),",
			"      error = function(e) conditionMessage(e))",
			"    out <- substr(out, 1, 110); if (length(out) > 15) out <- c(out[1:15], '...')",
			"    for (l in out) cat('@@P@@', l, '\\n', sep = '') }",
			"  onError <- function(e) {",
			"    if (!armed) return(invisible())",
			"    armed <<- FALSE",
			"    calls <- sys.calls(); n <- length(calls); k <- 0",
			"    for (i in rev(seq_len(n))) if (ours(attr(calls[[i]], 'srcref')) && !grepl('^\\\\.bio7(dbg|keep)', deparse(calls[[i]])[1])) { k <- i; break }",
			"    line <- if (k > 0) paste0(basename(srcfile(attr(calls[[k]], 'srcref'))), '#', attr(calls[[k]], 'srcref')[1]) else NA",
			"    cat('@@E@@', line, '\\t', one(conditionMessage(e)), '\\n', sep = '')",
			"    frame <- if (k > 1) sys.frame(k - 1) else globalenv()",
			"    eval(quote(browser()), envir = frame) }",
			"  export <- function(x, file) {",
			"    if (is.data.frame(x) || (is.matrix(x) && !is.numeric(x))) {",
			"      m <- as.matrix(format(x)); m <- rbind(colnames(m), m); m <- cbind(c('', rownames(x)), m)",
			"      utils::write.table(m, file, sep = '\\t', quote = FALSE, row.names = FALSE, col.names = FALSE, fileEncoding = 'UTF-8'); 'table'",
			"    } else if (is.matrix(x) && is.numeric(x)) {",
			"      con <- file(file, 'wb'); on.exit(close(con)); writeBin(as.integer(dim(x)), con, size = 4, endian = 'big')",
			"      writeBin(as.numeric(t(x)), con, size = 4, endian = 'big'); 'image'",
			"    } else if (is.numeric(x) || is.logical(x)) {",
			"      con <- file(file, 'wb'); on.exit(close(con)); writeBin(as.integer(length(x)), con, size = 4, endian = 'big')",
			"      writeBin(as.numeric(x), con, size = 4, endian = 'big'); 'plot'",
			"    } else {",
			"      writeLines(utils::capture.output(print(x)), file, useBytes = TRUE); 'text'",
			"    } }",
			/* Always a table (the hover of the editor): a matrix as it is, a vector as one column */
			"  exportTable <- function(x, file) {",
			"    if (is.factor(x)) x <- as.character(x)",
			"    if (is.atomic(x) && is.null(dim(x)) && length(x) > 0) x <- data.frame(value = x, row.names = if (is.null(names(x))) NULL else make.unique(names(x)))",
			"    if (is.atomic(x) && length(dim(x)) == 2L) { x <- as.data.frame(x, stringsAsFactors = FALSE) }",
			"    if (is.data.frame(x)) {",
			"      m <- as.matrix(format(x)); m <- rbind(colnames(m), m); m <- cbind(c('', rownames(x)), m)",
			"      utils::write.table(m, file, sep = '\\t', quote = FALSE, row.names = FALSE, col.names = FALSE, fileEncoding = 'UTF-8'); 'table'",
			"    } else export(x, file) }",
			/* A numeric (or logical) matrix as an image, else 'none' */
			"  exportImage <- function(x, file) if ((is.numeric(x) || is.logical(x)) && length(dim(x)) == 2L) export(x * 1, file) else 'none'",
			"  environment()",
			"})"));
}
