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

package com.eco.bio7.rbridge.backend;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * The connection of Bio7 to R: embedded R (default) or Rserve.
 * The methods and exceptions are those of RConnection which Bio7 uses, so the
 * code works with both.
 *
 * @see com.eco.bio7.rbridge.RServe#getBackend()
 */
public interface RBackend extends com.eco.bio7.rbridge.REvaluator {

	/** Evaluates R code and returns the result. */
	REXP eval(String code) throws RserveException;

	/** Evaluates R code without transferring the result. */
	void voidEval(String code) throws RserveException;

	/** Assigns a string (a character vector of length 1). */
	void assign(String name, String value) throws RserveException;

	void assign(String name, REXP value) throws RserveException;

	void assign(String name, double[] values) throws REngineException;

	void assign(String name, int[] values) throws REngineException;

	void assign(String name, String[] values) throws REngineException;

	void assign(String name, byte[] values) throws REngineException;

	/** Assigns a value in an environment (null: the global environment). */
	void assign(String name, REXP value, REXP env) throws REngineException;

	/** Closes the connection (embedded R keeps running). */
	boolean close();

	/** Stops the server (Rserve) or does nothing (embedded R runs until Bio7 ends). */
	void shutdown() throws RserveException;

	boolean isConnected();

	/** Returns true for embedded R. */
	boolean isEmbedded();

	/**
	 * A counter which changes after every evaluation of R (embedded R), or -1 if
	 * not available (Rserve).
	 */
	default long activity() {
		return -1;
	}

	/** The Rserve connection or null (embedded R). */
	RConnection rserveConnection();

	/** A backend for an Rserve connection (for code which still has an RConnection). */
	static RBackend of(RConnection connection) {
		return connection == null ? null : new RserveBackend(connection);
	}
}
