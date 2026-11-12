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

import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.rbridge.backend.Backends;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.rbridge.backend.RserveBackend;
import com.eco.bio7.reditors.REditor;

/**
 * The connection of Bio7 to R, independent of how R is connected: embedded R
 * (default, in the process of Bio7) or Rserve. Use RServe.getConnection()
 * only for code which needs Rserve itself (e.g., a remote connection).
 */
public final class RBridge {

	/* The backend of the current Rserve connection (created once per connection) */
	private static RBackend rserveBackend;

	static {
		/* The R editor and WorldWind (other bundles) evaluate with the same connection */
		REditor.setEvaluatorSupplier(RBridge::getBackend);
		com.eco.bio7.worldwind.WorldWindView.setEvaluatorSupplier(RBridge::getBackend);
	}

	private RBridge() {
	}

	/**
	 * Returns the connection to R: embedded R if it runs, else the Rserve
	 * connection.
	 *
	 * @return the backend or null if R is not connected.
	 */
	public static RBackend getBackend() {
		RBackend embedded = Backends.embedded();
		if (embedded != null) {
			return embedded;
		}
		RConnection c = RServe.getConnection();
		if (c == null) {
			return null;
		}
		RBackend b = rserveBackend;
		if (b == null || b.rserveConnection() != c) {
			b = new RserveBackend(c);
			rserveBackend = b;
		}
		return b;
	}

	/** Returns true if R is connected (embedded R or Rserve). */
	public static boolean isConnected() {
		return getBackend() != null;
	}

	/** Returns true if embedded R is used. */
	public static boolean isEmbedded() {
		return Backends.isEmbeddedRunning();
	}
}
