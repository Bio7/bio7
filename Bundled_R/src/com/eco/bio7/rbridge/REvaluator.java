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

package com.eco.bio7.rbridge;

import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RValue;

/**
 * The evaluation in R which the bundles of Bio7 (e.g., the R editor) use
 * (com.eco.bio7.rbridge.backend.RBackend). The values are RValue (plugin
 * com.eco.bio7.rffm), an error of R is an RException.
 */
public interface REvaluator {

	/** Evaluates R code and returns the result. */
	RValue eval(String code) throws RException;

	/** Evaluates R code without transferring the result. */
	void voidEval(String code) throws RException;

	/** Assigns a value to a variable of the global environment. */
	void assign(String name, RValue value) throws RException;

	/** Assigns a text (a character vector of length 1). */
	default void assign(String name, String value) throws RException {
		assign(name, RValue.of(value));
	}

	default void assign(String name, String[] values) throws RException {
		assign(name, RValue.of(values));
	}

	default void assign(String name, double[] values) throws RException {
		assign(name, RValue.of(values));
	}

	default void assign(String name, int[] values) throws RException {
		assign(name, RValue.of(values));
	}

	default void assign(String name, byte[] values) throws RException {
		assign(name, RValue.of(values));
	}

	/** Returns true if R is embedded in the process of Bio7. */
	default boolean isEmbedded() {
		return false;
	}

	/**
	 * The time an editor feature (hover, code completion, help popup) waits for R
	 * at most (embedded R), in milliseconds.
	 */
	long EDITOR_TIMEOUT_MS = 10_000;

	/**
	 * An evaluator whose evaluations wait at most timeoutMillis for R (default:
	 * this evaluator, unchanged).
	 */
	default REvaluator withTimeout(long timeoutMillis) {
		return this;
	}

	/**
	 * The evaluator for an editor feature (hover, code completion, help popup):
	 * time-limited for embedded R; null if c is null.
	 */
	static REvaluator forEditor(REvaluator c) {
		return c == null ? null : c.withTimeout(EDITOR_TIMEOUT_MS);
	}

	/**
	 * Runs the R calls of an editor feature: embedded R in the current thread (it
	 * accepts calls from any thread, the UI isn't blocked), without R in the UI
	 * thread. An exception of r is printed, not thrown (the caller resets
	 * RState.setBusy afterwards; e.g., a result missing after a timeout).
	 */
	static void run(REvaluator c, Runnable r) {
		try {
			if (c != null && c.isEmbedded()) {
				r.run();
			} else {
				org.eclipse.swt.widgets.Display.getDefault().syncExec(r);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
}
