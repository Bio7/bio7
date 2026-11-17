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

	/* The lines of the debugged copy with an injected line (1-based, sorted) */
	private static int[] injected = new int[0];
	/* The source line of the first stop (the first breakpoint) */
	private static int firstBreakpoint;

	/** The preference of the watch expressions (one per line). */
	public static final String WATCH_PREFERENCE = "R_DEBUG_WATCH_EXPRESSIONS";

	/**
	 * Writes the debugged copy of the script: the breakpoints (source line ->
	 * expression or null) injected. Returns the path of the copy.
	 */
	public static Path writeDebugCopy(Path source, Map<Integer, RBreakpoint> breakpoints) throws IOException {
		List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
		List<String> out = new ArrayList<>();
		List<Integer> inj = new ArrayList<>();
		/* The wrapper; the counters of the hit counts are removed at the start (same line) */
		out.add(".bio7debugfunc <- function() { rm(list = ls(pattern = '^\\\\.bio7hit_', all.names = TRUE, envir = .GlobalEnv), envir = .GlobalEnv)");
		firstBreakpoint = 0;
		for (int i = 0; i < lines.size(); i++) {
			int sourceLine = i + 1;
			if (breakpoints.containsKey(sourceLine)) {
				out.add(injection(sourceLine, breakpoints.get(sourceLine)));
				inj.add(out.size());
				if (firstBreakpoint == 0) {
					firstBreakpoint = sourceLine;
				}
			}
			out.add(lines.get(i));
		}
		out.add("}");
		injected = inj.stream().mapToInt(Integer::intValue).toArray();
		Path copy = Files.createTempFile("bio7debug", ".R");
		copy.toFile().deleteOnExit();
		Files.write(copy, out, StandardCharsets.UTF_8);
		return copy;
	}

	/*
	 * The line inserted before a breakpoint: browser(), a condition, a hit count
	 * (stop once, at the n-th time; with a condition: the n-th time it is true) and
	 * a log point (print the value instead of stopping), also combined
	 */
	static String injection(int sourceLine, RBreakpoint bp) {
		/* The breakpoint stores its line: browser() itself doesn't tell R's position */
		String stop = "{ assign('.bio7bp', " + sourceLine + "L, envir = .GlobalEnv); browser() }";
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
			action = "cat(\"\\u25B6 line " + sourceLine + ": " + label + " = \", tryCatch(paste(format(" + x
					+ "), collapse = ' '), error = function(e) conditionMessage(e)), \"\\n\", sep = \"\")";
		}
		String cond = bp.condition().isEmpty() ? null : "isTRUE(" + bp.condition() + ")";
		if (bp.hitCount() > 1) {
			/* Counted only when the condition is true (&&) */
			String v = ".bio7hit_" + sourceLine;
			String count = "(assign('" + v + "', get0('" + v + "', envir = .GlobalEnv, ifnotfound = 0) + 1, envir = .GlobalEnv) == " + bp.hitCount() + ")";
			return "if (" + (cond == null ? "" : cond + " && ") + count + ") " + action;
		}
		return cond == null ? action : "if (" + cond + ") " + action;
	}

	/**
	 * The line of the editor of a line of the debugged copy (the wrapper and the
	 * injected lines removed); an injected line maps to the line of its breakpoint.
	 */
	public static int toSourceLine(int copyLine) {
		int line = copyLine - 1;
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
			"  dims <- function(x) { d <- dim(x); if (!is.null(d)) paste(d, collapse = ' x ') else as.character(length(x)) }",
			"  short <- function(x) one(tryCatch(",
			"    if (is.atomic(x) && is.null(dim(x)) && length(x) <= 10) paste(format(x), collapse = ' ')",
			"    else paste(utils::capture.output(utils::str(x, max.level = 0, vec.len = 3, give.attr = FALSE)), collapse = ' '),",
			"    error = function(e) '<unreadable>'))",
			"  vars <- function(env) for (v in sort(ls(envir = env))) {",
			"    x <- tryCatch(get(v, envir = env), error = function(e) NULL)",
			"    cat('@@V@@', v, '\\t', class(x)[1], '\\t', dims(x), '\\t', size(x), '\\t', short(x), '\\n', sep = '') }",
			"  watch <- function(exprs, env) for (i in seq_along(exprs)) {",
			"    v <- tryCatch(list(value = eval(parse(text = exprs[i]), envir = new.env(parent = env))), error = function(e) e)",
			"    val <- if (inherits(v, 'error')) one(paste('Error:', conditionMessage(v))) else short(v$value)",
			"    cat('@@W@@', i, '\\t', val, '\\n', sep = '') }",
			"  stack <- function(calls) for (i in seq_along(calls)) {",
			"    cl <- calls[[i]]; sr <- attr(cl, 'srcref'); txt <- one(paste(deparse(cl, width.cutoff = 200L)[1], collapse = ''))",
			"    if (grepl('^(\\\\.bio7dbg|withCallingHandlers|eval\\\\(quote\\\\(browser|h\\\\(simpleError|\\\\.handleSimpleError|\\\\(function \\\\(expr\\\\)|withVisible\\\\(eval\\\\(expr|eval\\\\(expr, globalenv)', txt)) next",
			"    cat('@@S@@', i, '\\t', if (ours(sr)) sr[1] else NA, '\\t', txt, '\\n', sep = '') }",
			"  state <- function(env, exprs, frame = 0) {",
			"    armed <<- FALSE",
			"    calls <- sys.calls(); calls <- calls[-length(calls)]",
			"    cat('@@FRAME@@', frame, '\\n', sep = '')",
			"    bp <- get0('.bio7bp', envir = .GlobalEnv, ifnotfound = NA); assign('.bio7bp', NA, envir = .GlobalEnv)",
			"    cat('@@BP@@', bp, '\\n', sep = '')",
			"    vars(env); watch(exprs, env); stack(calls); invisible() }",
			"  arm <- function() { armed <<- TRUE; invisible() }",
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
			"    for (i in rev(seq_len(n))) if (ours(attr(calls[[i]], 'srcref'))) { k <- i; break }",
			"    line <- if (k > 0) attr(calls[[k]], 'srcref')[1] else NA",
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
