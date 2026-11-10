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

/** The Rserve backend: delegates to the RConnection. */
public final class RserveBackend implements RBackend {

	private final RConnection c;

	public RserveBackend(RConnection connection) {
		this.c = connection;
	}

	@Override
	public REXP eval(String code) throws RserveException {
		return c.eval(code);
	}

	@Override
	public void voidEval(String code) throws RserveException {
		c.voidEval(code);
	}

	@Override
	public void assign(String name, String value) throws RserveException {
		c.assign(name, value);
	}

	@Override
	public void assign(String name, REXP value) throws RserveException {
		c.assign(name, value);
	}

	@Override
	public void assign(String name, double[] values) throws REngineException {
		c.assign(name, values);
	}

	@Override
	public void assign(String name, int[] values) throws REngineException {
		c.assign(name, values);
	}

	@Override
	public void assign(String name, String[] values) throws REngineException {
		c.assign(name, values);
	}

	@Override
	public void assign(String name, byte[] values) throws REngineException {
		c.assign(name, values);
	}

	@Override
	public void assign(String name, REXP value, REXP env) throws REngineException {
		c.assign(name, value, env);
	}

	@Override
	public boolean close() {
		return c.close();
	}

	@Override
	public void shutdown() throws RserveException {
		c.shutdown();
	}

	@Override
	public boolean isConnected() {
		return c.isConnected();
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}

	@Override
	public RConnection rserveConnection() {
		return c;
	}
}
