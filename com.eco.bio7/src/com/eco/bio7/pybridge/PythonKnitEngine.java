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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eco.bio7.markdownedit.chunks.RChunks;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RSession;
import com.eco.bio7.rffm.RValue;

/**
 * The Python chunks (```{python}) of a document rendered with knitr/rmarkdown
 * in the embedded R run in the embedded Python of Bio7 (as in the markdown
 * editor, without the R package reticulate): the output and the matplotlib
 * figures in the document, the module bio7 is available.
 * <p>
 * The engine is set when knitr is loaded (a hook of R). The chunk option
 * engine.bio7 = FALSE uses the former engine (reticulate).
 */
public final class PythonKnitEngine {

	/*
	 * The knitr engine: the code, the figure size (inch -> points: 96 per inch)
	 * and the resolution of knitr (with fig.retina) to Bio7; the figures are
	 * copied to the figure path of knitr and included with its plot hook
	 */
	private static final String ENGINE = String.join("\n", //
			"local({", //
			"  former <- NULL", //
			"  set <- function(...) {", //
			"    cur <- knitr::knit_engines$get('python')", //
			"    if (!isTRUE(attr(cur, 'bio7'))) former <<- cur", //
			"    f <- function(options) {", //
			"      if (isFALSE(options$engine.bio7) && !is.null(former)) return(former(options))", //
			"      if (!isTRUE(options$eval)) return(knitr::engine_output(options, options$code, NULL))", //
			"      num <- function(x, default) if (is.numeric(x) && length(x) > 0) x[1] else default", //
			"      w <- num(options$fig.width, 7); h <- num(options$fig.height, 5); dpi <- num(options$dpi, 96)", //
			"      r <- .bio7PythonChunk(paste(options$code, collapse = '\\n'), round(w * 96), round(h * 96), round(dpi))", //
			"      if (isTRUE(r$error) && !isTRUE(options$error))", //
			"        stop('Python chunk ', options$label, ':\\n', r$text, call. = FALSE)", //
			"      out <- sub('\\n+$', '', r$text)", //
			"      res <- knitr::engine_output(options, options$code, if (nzchar(out)) out else NULL)", //
			"      charts <- r$plots[grepl('\\\\.html$', r$plots)]", //
			"      r$plots <- r$plots[!grepl('\\\\.html$', r$plots)]", //
			"      if (length(r$plots) > 0 && !identical(options$fig.show, 'hide')) {", //
			"        hook <- knitr::knit_hooks$get('plot')", //
			"        options$fig.num <- length(r$plots)", //
			"        ret <- num(options$fig.retina, 1)", //
			"        auto <- is.null(options$out.width) || isTRUE(all.equal(options$out.width, w * dpi / ret))", //
			"        for (i in seq_along(r$plots)) {", //
			"          f <- knitr::fig_path('.png', options, number = i)", //
			"          dir.create(dirname(f), recursive = TRUE, showWarnings = FALSE)", //
			"          file.copy(r$plots[[i]], f, overwrite = TRUE)", //
			"          options$fig.cur <- i", //
			"          if (auto) {", //
			"            # The width of a figure (also with its own figsize) in the document", //
			"            con <- file(f, 'rb'); readBin(con, 'raw', 16)", //
			"            px <- readBin(con, 'integer', 1, size = 4, endian = 'big'); close(con)", //
			"            options$out.width <- round(px / ret)", //
			"          }", //
			"          res <- paste0(res, hook(f, options))", //
			"        }", //
			"      }", //
			"      # Interactive charts (plotly, altair, bokeh, folium, holoviews): embedded in HTML output", //
			"      for (ch in charts) {", //
			"        if (knitr::is_html_output()) {", //
			"          html <- paste(readLines(ch, warn = FALSE, encoding = 'UTF-8'), collapse = '\n')", //
			"          html <- gsub('&', '&amp;', html, fixed = TRUE)", //
			"          html <- gsub('\"', '&quot;', html, fixed = TRUE)", //
			"          height <- if (!is.null(options$out.height)) options$out.height else paste0(round(h * 96 + 40), 'px')", //
			"          res <- paste0(res, '\n\n```{=html}\n<iframe srcdoc=\"', html, '\" style=\"width:100%;height:', height,", //
			"                        ';border:none;\"></iframe>\n```\n\n')", //
			"        } else {", //
			"          res <- paste0(res, '\n\n*(An interactive chart: only in HTML output)*\n\n')", //
			"        }", //
			"      }", //
			"      res", //
			"    }", //
			"    attr(f, 'bio7') <- TRUE", //
			"    knitr::knit_engines$set(python = f)", //
			"  }", //
			"  if (isNamespaceLoaded('knitr')) set()", //
			"  setHook(packageEvent('knitr', 'onLoad'), set)", //
			"})");

	private static volatile RSession installed;

	private PythonKnitEngine() {
	}

	/** Installs the engine in the embedded R (once per session). */
	public static synchronized void install(RSession session) {
		if (session == null || session == installed || !EmbeddedPython.isAvailable()) {
			return;
		}
		try {
			session.registerFunction(".bio7PythonChunk", PythonKnitEngine::chunk);
			session.voidEval(ENGINE);
			installed = session;
		} catch (RException e) {
			System.err.println("The Python engine for knitr could not be installed: " + e.getMessage());
		}
	}

	/* .bio7PythonChunk(code, widthPoints, heightPoints, dpi): list(text, plots, error) */
	private static RValue chunk(List<RValue> args) throws Exception {
		String code = ((RValue.Str) args.get(0)).values()[0];
		int w = (int) number(args.get(1));
		int h = (int) number(args.get(2));
		int dpi = (int) number(args.get(3));
		RChunks.Output out = PythonChunkEvaluator.evaluate(code, w, h, dpi, true);
		Map<String, RValue> result = new LinkedHashMap<>();
		result.put("text", new RValue.Str(out.text() == null ? "" : out.text()));
		result.put("plots", new RValue.Str(out.plots().stream().map(Path::toString).map(p -> p.replace('\\', '/')).toArray(String[]::new)));
		result.put("error", new RValue.Logical(out.error()));
		return RValue.List.named(result);
	}

	private static double number(RValue v) {
		return switch (v) {
		case RValue.Num n -> n.values()[0];
		case RValue.Int i -> i.values()[0];
		default -> throw new IllegalArgumentException("A number is expected");
		};
	}
}
