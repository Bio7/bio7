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


import com.eco.bio7.rbridge.backend.Backends;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.reditors.REditor;

/**
 * The connection of Bio7 to R: embedded R in the process of Bio7. The values are
 * RValue of the plugin com.eco.bio7.rffm, e.g.,
 * {@code RBridge.getBackend().eval("mean(x)").firstDouble()}.
 */
public final class RBridge {

	static {
		/* The R editor and WorldWind (other bundles) evaluate with the same connection */
		REditor.setEvaluatorSupplier(RBridge::getBackend);
		com.eco.bio7.worldwind.WorldWindView.setEvaluatorSupplier(RBridge::getBackend);
	}

	private RBridge() {
	}

	/**
	 * Returns the connection to R (embedded R).
	 *
	 * @return the backend or null if R is not started.
	 */
	public static RBackend getBackend() {
		return Backends.embedded();
	}

	/** Returns true if R is started. */
	public static boolean isConnected() {
		return getBackend() != null;
	}

	/** Returns true if R is started (R is always embedded). */
	public static boolean isEmbedded() {
		return Backends.isEmbeddedRunning();
	}
}
