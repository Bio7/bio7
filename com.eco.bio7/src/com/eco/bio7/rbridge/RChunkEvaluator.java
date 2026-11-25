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
package com.eco.bio7.rbridge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.eco.bio7.markdownedit.chunks.RChunks;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.rbridge.debug.DebugSession;

/**
 * Evaluates an R chunk of the markdown editor in the workspace of R (as typed
 * in the console): the printed values, messages, warnings and errors are
 * collected, the plots are drawn on a PNG device (each page a file, at twice
 * the size for HiDPI). The current plot device of R is restored afterwards.
 */
public final class RChunkEvaluator {

	/*
	 * %1$s: the file of the code, %2$s: the folder of the output, %3$d/%4$d: the
	 * size of a plot (pixels), %5$d: the resolution
	 */
	private static final String CODE = String.join("\n", //
			"local({", //
			"  .old <- grDevices::dev.cur()", //
			"  .type <- if (isTRUE(capabilities('cairo'))) 'cairo' else getOption('bitmapType')", //
			"  grDevices::png(file.path(%2$s, 'plot%%03d.png'), width = %3$d, height = %4$d, res = %5$d, type = .type)", //
			"  .dev <- grDevices::dev.cur()", //
			"  .out <- character()", //
			"  .con <- textConnection('.out', 'w', local = TRUE)", //
			"  sink(.con)", //
			"  tryCatch({", //
			"    for (.e in parse(%1$s, keep.source = FALSE, encoding = 'UTF-8')) {", //
			"      .ok <- withCallingHandlers(tryCatch({", //
			"        .r <- withVisible(eval(.e, envir = globalenv()))", //
			"        if (.r$visible) print(.r$value)", //
			"        TRUE", //
			"      }, error = function(e) { cat('Error: ', conditionMessage(e), '\\n', sep = ''); FALSE }),", //
			"      warning = function(w) { cat('Warning: ', conditionMessage(w), '\\n', sep = ''); invokeRestart('muffleWarning') },", //
			"      message = function(m) { cat(conditionMessage(m)); invokeRestart('muffleMessage') })", //
			"      if (!.ok) { cat('@@CHUNKERROR@@\\n'); break }", //
			"    }", //
			"  }, error = function(e) cat('Error: ', conditionMessage(e), '\\n@@CHUNKERROR@@\\n', sep = ''),", //
			"  finally = {", //
			"    sink()", //
			"    close(.con)", //
			"    if (.dev %%in%% grDevices::dev.list()) grDevices::dev.off(.dev)", //
			"    if (.old > 1 && .old %%in%% grDevices::dev.list()) grDevices::dev.set(.old)", //
			"  })", //
			"  writeLines(.out, file.path(%2$s, 'out.txt'), useBytes = TRUE)", //
			"  invisible(NULL)", //
			"})");

	private static final String ERROR_MARK = "@@CHUNKERROR@@";

	private RChunkEvaluator() {
	}

	/** Installs the evaluator in the markdown editor. */
	public static void install() {
		RChunks.install(RChunkEvaluator::evaluate);
	}

	static RChunks.Output evaluate(String code, int widthPoints, int heightPoints) {
		RBackend backend = RBridge.getBackend();
		/* Started when it is used (preferences) */
		if (backend == null && com.eco.bio7.rcp.AutoStart.startROnUse()) {
			backend = RBridge.getBackend();
		}
		if (backend == null) {
			return new RChunks.Output(com.eco.bio7.rcp.AutoStart.NOT_RUNNING_R.replace("\n", " "), List.of(), true);
		}
		Path dir = null;
		try {
			dir = Files.createTempDirectory("bio7chunk");
			Path src = dir.resolve("chunk.R");
			Files.writeString(src, code, StandardCharsets.UTF_8);
			String folder = DebugSession.rString(dir.toString().replace("\\", "/"));
			String file = DebugSession.rString(src.toString().replace("\\", "/"));
			backend.voidEval(String.format(java.util.Locale.ROOT, CODE, file, folder, widthPoints * 2, heightPoints * 2, 192));
			Path outFile = dir.resolve("out.txt");
			String text = Files.exists(outFile) ? Files.readString(outFile, StandardCharsets.UTF_8) : "";
			boolean error = text.contains(ERROR_MARK);
			text = text.replace(ERROR_MARK + "\n", "").replace(ERROR_MARK, "");
			/* A syntax error names the temporary file */
			text = text.replace(src.toString().replace("\\", "/") + ":", "line ").replace(src.toString() + ":", "line ");
			List<Path> plots = new ArrayList<>();
			try (Stream<Path> files = Files.list(dir)) {
				files.filter(p -> p.getFileName().toString().matches("plot\\d+\\.png")).sorted().forEach(plots::add);
			}
			for (Path p : plots) {
				p.toFile().deleteOnExit();
			}
			outFile.toFile().deleteOnExit();
			src.toFile().deleteOnExit();
			dir.toFile().deleteOnExit();
			/* The new variables in the R-Shell */
			REval.listRObjects();
			return new RChunks.Output(text, plots, error);
		} catch (IOException e) {
			return new RChunks.Output("The chunk could not be evaluated: " + e.getMessage(), List.of(), true);
		} catch (Exception e) {
			/* E.g., R is busy or waits in a debug session */
			String msg = e.getMessage() == null ? e.toString() : e.getMessage();
			return new RChunks.Output("R could not evaluate the chunk: " + msg, List.of(), true);
		}
	}
}
