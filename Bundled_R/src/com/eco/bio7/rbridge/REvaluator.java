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

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * The evaluation in R which the bundles of Bio7 (e.g., the R editor) use: an
 * Rserve connection or embedded R (com.eco.bio7.rbridge.backend.RBackend). The
 * methods and exceptions are those of RConnection.
 */
public interface REvaluator {

	REXP eval(String code) throws RserveException;

	void voidEval(String code) throws RserveException;

	void assign(String name, String value) throws RserveException;

	void assign(String name, REXP value) throws RserveException;

	void assign(String name, String[] values) throws REngineException;

	void assign(String name, double[] values) throws REngineException;

	void assign(String name, int[] values) throws REngineException;

	/** Returns true if R is embedded in the process of Bio7 (not Rserve). */
	default boolean isEmbedded() {
		return false;
	}

	/**
	 * The time an editor feature (hover, code completion, help popup) waits for R
	 * at most (embedded R), in milliseconds.
	 */
	long EDITOR_TIMEOUT_MS = 10_000;

	/**
	 * An evaluator whose evaluations wait at most timeoutMillis for R (embedded R;
	 * Rserve: this evaluator, unchanged).
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
	 * accepts calls from any thread, the UI isn't blocked), Rserve in the UI thread
	 * as before. An exception of r is printed, not thrown (the caller resets
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

	/** An evaluator for an Rserve connection. */
	static REvaluator of(RConnection c) {
		if (c == null) {
			return null;
		}
		return new REvaluator() {
			public REXP eval(String code) throws RserveException {
				return c.eval(code);
			}

			public void voidEval(String code) throws RserveException {
				c.voidEval(code);
			}

			public void assign(String name, String value) throws RserveException {
				c.assign(name, value);
			}

			public void assign(String name, REXP value) throws RserveException {
				c.assign(name, value);
			}

			public void assign(String name, String[] values) throws REngineException {
				c.assign(name, values);
			}

			public void assign(String name, double[] values) throws REngineException {
				c.assign(name, values);
			}

			public void assign(String name, int[] values) throws REngineException {
				c.assign(name, values);
			}
		};
	}
}
