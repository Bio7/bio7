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
package com.eco.bio7.markdownedit.chunks;

import java.nio.file.Path;
import java.util.List;

/**
 * The evaluation of an R or Python chunk of the markdown editor (the output is shown
 * inline below the chunk). Bio7 installs the evaluator (the R bridge is in another bundle).
 */
public final class RChunks {

	/**
	 * The output of a chunk: the text (printed values, messages, warnings, errors)
	 * and the plots (PNG files, rendered at twice the size for sharp HiDPI views).
	 */
	public record Output(String text, List<Path> plots, boolean error) {
	}

	/** Evaluates the code of a chunk in R (a background thread). */
	@FunctionalInterface
	public interface Evaluator {
		/**
		 * @param widthPoints  the width of a plot in the editor
		 * @param heightPoints the height of a plot in the editor
		 */
		Output evaluate(String code, int widthPoints, int heightPoints);
	}

	private static volatile Evaluator evaluator;
	private static volatile Evaluator pythonEvaluator;

	private RChunks() {
	}

	/** Installs the evaluator (Bio7). */
	public static void install(Evaluator e) {
		evaluator = e;
	}

	/** Installs the evaluator of the Python chunks (```{python}, Bio7). */
	public static void installPython(Evaluator e) {
		pythonEvaluator = e;
	}

	static Output evaluate(String code, boolean python, int widthPoints, int heightPoints) {
		Evaluator e = python ? pythonEvaluator : evaluator;
		if (e == null) {
			return new Output(python ? "Python is not available." : "R is not available.", List.of(), true);
		}
		try {
			return e.evaluate(code, widthPoints, heightPoints);
		} catch (RuntimeException ex) {
			return new Output("The chunk could not be evaluated: " + ex.getMessage(), List.of(), true);
		}
	}
}
