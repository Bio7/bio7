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

import com.eco.bio7.rbridge.REvaluator;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RSession;
import com.eco.bio7.rffm.RValue;

/**
 * The embedded backend (default): R in the process of Bio7 (plugin
 * com.eco.bio7.rffm). The values of the session are passed on unchanged.
 */
public final class EmbeddedRBackend implements RBackend {

	private final RSession session;

	public EmbeddedRBackend(RSession session) {
		this.session = session;
	}

	public RSession session() {
		return session;
	}

	@Override
	public long activity() {
		return session.getActivity();
	}

	/*
	 * The UI thread must not wait for R while it evaluates console input (a long
	 * computation would freeze Bio7): the call fails at once
	 */
	private void checkUiThread() throws RException {
		if (session.isBusy() && !session.isRThread() && org.eclipse.swt.widgets.Display.getCurrent() != null) {
			throw new RException("R is busy (console evaluation running)");
		}
	}

	@Override
	public RValue eval(String code) throws RException {
		return eval(code, 0);
	}

	private RValue eval(String code, long timeoutMillis) throws RException {
		checkUiThread();
		return session.eval(code, timeoutMillis);
	}

	@Override
	public void voidEval(String code) throws RException {
		voidEval(code, 0);
	}

	private void voidEval(String code, long timeoutMillis) throws RException {
		checkUiThread();
		session.voidEval(code, timeoutMillis);
	}

	/* Evaluations wait at most timeoutMillis; assign as this backend */
	@Override
	public REvaluator withTimeout(long timeoutMillis) {
		EmbeddedRBackend b = this;
		return new REvaluator() {
			public RValue eval(String code) throws RException {
				return b.eval(code, timeoutMillis);
			}

			public void voidEval(String code) throws RException {
				b.voidEval(code, timeoutMillis);
			}

			public void assign(String name, RValue value) throws RException {
				b.assign(name, value);
			}

			public boolean isEmbedded() {
				return true;
			}
		};
	}

	@Override
	public void assign(String name, RValue value) throws RException {
		session.assign(name, value);
	}

	@Override
	public boolean close() {
		/* Embedded R runs until Bio7 ends */
		return true;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public boolean isConnected() {
		return session.isAlive();
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}
}
