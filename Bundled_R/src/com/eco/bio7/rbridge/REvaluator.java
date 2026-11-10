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
