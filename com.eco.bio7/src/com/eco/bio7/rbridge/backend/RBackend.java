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

import com.eco.bio7.rffm.RException;

/**
 * The connection of Bio7 to R: embedded R, in the process of Bio7.
 * Evaluations return an RValue (plugin com.eco.bio7.rffm), errors of R are an
 * RException.
 *
 * @see com.eco.bio7.rbridge.RBridge#getBackend()
 */
public interface RBackend extends com.eco.bio7.rbridge.REvaluator {

	/** Closes the connection (embedded R keeps running). */
	boolean close();

	/** Does nothing for embedded R (it runs until Bio7 ends). */
	void shutdown() throws RException;

	boolean isConnected();

	/**
	 * A counter which changes after every evaluation of R, or -1 if not
	 * available.
	 */
	default long activity() {
		return -1;
	}
}
