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

package com.eco.bio7.rffm;

/**
 * The console of an embedded R session: receives the output of R and the
 * prompts. Called in the thread of R: hand the text to the UI asynchronously.
 */
public interface RConsole {

	/**
	 * Output of R.
	 *
	 * @param error true for the error stream of R (messages, e.g., of loading
	 *              packages; errors and warnings of the console come through
	 *              writeError and writeWarning)
	 */
	void write(String text, boolean error);

	/** An error of an evaluation in the console (by default the error stream). */
	default void writeError(String text) {
		write(text, true);
	}

	/** The warnings of an evaluation in the console (by default the error stream). */
	default void writeWarning(String text) {
		write(text, true);
	}

	/** R waits for console input (e.g., "> " or "+ "). */
	default void prompt(String prompt) {
		write(prompt, false);
	}

	/** R is busy (evaluating) or idle. */
	default void busy(boolean busy) {
	}

	/** A message of R for the user (e.g., a warning dialog in other front-ends). */
	default void message(String message) {
		write(message + "\n", true);
	}

	/** A console which discards the output. */
	RConsole NONE = (text, error) -> {
	};
}
