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
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * The script which the debugger starts for the active R file. A file which
 * only defines classes or functions (e.g., a class of a project with one class
 * per file) does nothing on its own, so:
 * <ol>
 * <li>the script of the last debug session is started again if it loaded the
 * file with source(),</li>
 * <li>else the one R file of the folder (or of the project) which loads the
 * file with source() is started,</li>
 * <li>else the file itself is debugged, with a hint if the session ends
 * without a stop.</li>
 * </ol>
 */
final class DebugTarget {

	/** The script to debug, a note for the Debug Text view and a hint for the end of a session without a stop. */
	record Choice(IFile script, String note, String hint) {
	}

	/* The script of the last debug session */
	private static volatile IFile lastScript;

	private DebugTarget() {
	}

	/** Remembers the script of a debug session. */
	static void started(IFile script) {
		lastScript = script;
	}

	/** The script to debug for the active file. */
	static Choice choose(IFile active) {
		if (active == null || active.getLocation() == null) {
			return new Choice(active, null, null);
		}
		/* 1: the script of the last session has loaded the file */
		IFile last = lastScript;
		if (last != null && last.exists() && !last.equals(active) && loadedByLastSession(active)) {
			return new Choice(last, "▶ Debugging " + last.getName() + " (the script of the last session, it loads " + active.getName() + ")", null);
		}
		if (!definitionsOnly(active)) {
			return new Choice(active, null, null);
		}
		/* 2: the one R file of the folder (else of the project) which loads the file */
		List<IFile> callers = callers(active, active.getParent(), IResource.DEPTH_ONE);
		if (callers.isEmpty() && active.getProject() != null) {
			callers = callers(active, active.getProject(), IResource.DEPTH_INFINITE);
		}
		if (callers.size() == 1) {
			IFile script = callers.get(0);
			return new Choice(script, "▶ Debugging " + script.getName() + " (it loads " + active.getName() + ", which only defines classes and functions)", null);
		}
		/* 3: the file itself, with a hint */
		String from = callers.isEmpty() ? "the script which loads it with source()"
				: callers.stream().map(IFile::getName).collect(Collectors.joining(", ")) + " (the script which loads it)";
		return new Choice(active, null, active.getName() + " only defines classes and functions, nothing calls them.\nStart the debugger from " + from + ".");
	}

	/* The file was loaded with source() in the last debug session (R knows the files) */
	private static boolean loadedByLastSession(IFile file) {
		String[] sourced = DebugSession.rStringsNow("if (exists('.bio7dbg', envir = globalenv()) && !is.null(.bio7dbg$sourced)) .bio7dbg$sourced else character(0)");
		if (sourced == null) {
			return false;
		}
		Path p = real(file.getLocation().toFile().toPath());
		for (String s : sourced) {
			if (!s.isBlank() && real(Path.of(s)).equals(p)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * The file only defines classes, functions and values (assignments of
	 * functions, classes or constants, library(), setClass(), ...): checked by the
	 * parser of R; false if R is not available.
	 */
	private static boolean definitionsOnly(IFile file) {
		String path = DebugSession.rString(file.getLocation().toString().replace("\\", "/"));
		String defining = "c('setRefClass', 'methods::setRefClass', 'R6Class', 'R6::R6Class', 'setClass', 'methods::setClass', 'new_class', 'S7::new_class',"
				+ " 'new_generic', 'S7::new_generic', 'setGeneric', 'setMethod', 'setValidity', 'function')";
		String code = "local({ e <- tryCatch(parse(" + path + ", keep.source = FALSE), error = function(err) NULL);"
				+ " ok <- function(x) { if (!is.call(x)) return(FALSE); f <- deparse(x[[1]])[1];"
				+ " if (f %in% c('library', 'require', 'requireNamespace', 'suppressPackageStartupMessages', 'options', 'setClass', 'setGeneric', 'setMethod', 'setValidity')) return(TRUE);"
				+ " if (f %in% c('<-', '=', '<<-') && length(x) == 3L) { r <- x[[3]]; return(!is.call(r) || deparse(r[[1]])[1] %in% " + defining + ") };"
				+ " FALSE };"
				+ " as.character(!is.null(e) && length(e) > 0L && all(vapply(e, ok, logical(1)))) })";
		String[] out = DebugSession.rStringsNow(code);
		return out != null && out.length == 1 && "TRUE".equals(out[0]);
	}

	/* The R files of a container which load the file with source() (a call of source and the name of the file as a string) */
	private static List<IFile> callers(IFile file, IContainer container, int depth) {
		List<IFile> out = new ArrayList<>();
		if (container == null) {
			return out;
		}
		Pattern name = Pattern.compile("[\"']([^\"'\\n]*[/\\\\])?" + Pattern.quote(file.getName()) + "[\"']");
		try {
			container.accept(r -> {
				if (r.getType() == IResource.FILE && !r.equals(file) && r.getLocation() != null && r.getName().toLowerCase(Locale.ROOT).endsWith(".r")) {
					try {
						String text = Files.readString(r.getLocation().toFile().toPath(), StandardCharsets.UTF_8).replaceAll("(?m)#.*$", "");
						if (text.contains("source(") && name.matcher(text).find()) {
							out.add((IFile) r);
						}
					} catch (IOException | RuntimeException e) {
						/* Not readable */
					}
				}
				return true;
			}, depth, false);
		} catch (CoreException e) {
			/* The files found so far */
		}
		return out;
	}

	private static Path real(Path p) {
		try {
			return p.toRealPath();
		} catch (IOException | RuntimeException e) {
			return p.toAbsolutePath().normalize();
		}
	}
}
