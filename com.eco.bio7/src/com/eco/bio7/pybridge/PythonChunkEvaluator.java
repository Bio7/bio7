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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.eco.bio7.markdownedit.chunks.RChunks;
import com.eco.bio7.pyffm.PyException;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;

/**
 * Evaluates a Python chunk (```{python}) of the markdown editor in the embedded
 * Python (in __main__, as typed in the console): the output and errors are
 * collected, the matplotlib figures of the chunk are saved as PNG files (at
 * twice the size for HiDPI). Python is started if necessary.
 */
public final class PythonChunkEvaluator {

	private PythonChunkEvaluator() {
	}

	/** Installs the evaluator in the markdown editor. */
	public static void install() {
		if (EmbeddedPython.isAvailable()) {
			RChunks.installPython(PythonChunkEvaluator::evaluate);
		}
	}

	static RChunks.Output evaluate(String code, int widthPoints, int heightPoints) {
		return evaluate(code, widthPoints, heightPoints, 192);
	}

	/** As above, the plots with a resolution (dpi; the size in points = 1/96 inch). */
	static RChunks.Output evaluate(String code, int widthPoints, int heightPoints, int dpi) {
		return evaluate(code, widthPoints, heightPoints, dpi, false);
	}

	/**
	 * As above; embed: the interactive charts as files chartNNN.html in the plots
	 * (a rendered document), else shown in the browser view.
	 */
	static RChunks.Output evaluate(String code, int widthPoints, int heightPoints, int dpi, boolean embed) {
		if (!EmbeddedPython.isRunning()) {
			String error = EmbeddedPython.start();
			if (error != null) {
				return new RChunks.Output("Python could not be started: " + error, List.of(), true);
			}
		}
		PySession py = EmbeddedPython.session();
		Path dir = null;
		try {
			load(py);
			dir = Files.createTempDirectory("bio7pychunk");
			py.assign("_bio7_args", new PyValue.PyList(List.of(PyValue.of(code), PyValue.of(dir.toString()))));
			PyValue status = py.eval(String.format(java.util.Locale.ROOT, "__import__('_bio7chunk').run(*_bio7_args, %d, %d, %d, %s)", widthPoints, heightPoints, dpi,
					embed ? "True" : "False"));
			py.exec("del _bio7_args");
			boolean error = status instanceof PyValue.Str s && "error".equals(s.asString());
			Path outFile = dir.resolve("out.txt");
			String text = Files.exists(outFile) ? Files.readString(outFile, StandardCharsets.UTF_8) : "";
			List<Path> plots = new ArrayList<>();
			try (Stream<Path> files = Files.list(dir)) {
				files.filter(p -> p.getFileName().toString().matches("plot\\d+\\.png")).sorted().forEach(plots::add);
			}
			if (embed) {
				try (Stream<Path> files = Files.list(dir)) {
					files.filter(p -> p.getFileName().toString().matches("chart\\d+\\.html")).sorted().forEach(plots::add);
				}
			}
			for (Path p : plots) {
				p.toFile().deleteOnExit();
			}
			outFile.toFile().deleteOnExit();
			dir.toFile().deleteOnExit();
			/* The new variables in the Python-Shell */
			PythonShellView.refreshOpenViews();
			return new RChunks.Output(text, plots, error);
		} catch (IOException e) {
			return new RChunks.Output("The chunk could not be evaluated: " + e.getMessage(), List.of(), true);
		} catch (PyException e) {
			/* E.g., Python is busy */
			return new RChunks.Output("Python could not evaluate the chunk: " + e.getMessage(), List.of(), true);
		}
	}

	/* The module of the chunks (once) */
	private static void load(PySession py) throws PyException, IOException {
		PyValue loaded = py.eval("'_bio7chunk' in __import__('sys').modules");
		if (loaded instanceof PyValue.Array a && a.getDouble(0) != 0) {
			return;
		}
		String source;
		try (InputStream in = PythonChunkEvaluator.class.getResourceAsStream("python_chunk.py")) {
			source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		py.assign("_bio7_src", PyValue.of(source));
		py.exec("import types as _t, sys as _s\n_m = _t.ModuleType('_bio7chunk')\nexec(compile(_bio7_src, '<_bio7chunk>', 'exec'), _m.__dict__)\n"
				+ "_s.modules['_bio7chunk'] = _m\ndel _t, _s, _m, _bio7_src");
	}
}
