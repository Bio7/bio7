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

import org.eclipse.jface.preference.IPreferenceStore;

import com.eco.bio7.Bio7Plugin;

/**
 * The selection of the R backend (preference R_BACKEND: RSERVE or EMBEDDED) and
 * the embedded backend once it runs. This class doesn't load the classes of
 * embedded R (the bundle com.eco.bio7.rffm is optional).
 */
public final class Backends {

	public static final String PREFERENCE = "R_BACKEND";
	public static final String RSERVE = "RSERVE";
	public static final String EMBEDDED = "EMBEDDED";

	private static volatile RBackend embedded;

	/** The message when the user wants to terminate embedded R. */
	public static final String CANNOT_TERMINATE = "Embedded R runs in the process of Bio7 and can't be terminated.\n\n"
			+ "Interrupt a computation with Ctrl+C in the console, or restart Bio7 to start R again.";

	private Backends() {
	}

	/** Returns true if embedded R is selected in the preferences (experimental). */
	public static boolean isEmbeddedSelected() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		return EMBEDDED.equals(store.getString(PREFERENCE));
	}

	/** The embedded backend if embedded R runs, else null. */
	public static RBackend embedded() {
		return embedded;
	}

	static void setEmbedded(RBackend backend) {
		embedded = backend;
	}

	/** Returns true if embedded R runs. */
	public static boolean isEmbeddedRunning() {
		return embedded != null;
	}

	/**
	 * Returns why embedded R can't be used, or null if it can: the bundle, Java 22+
	 * and the native access of the JVM.
	 */
	public static String unavailableReason() {
		if (Runtime.version().feature() < 22) {
			return "Embedded R needs Java 22 or later (this is Java " + Runtime.version().feature() + ").";
		}
		try {
			Class.forName("com.eco.bio7.rffm.RSession", false, Backends.class.getClassLoader());
		} catch (ClassNotFoundException | LinkageError e) {
			return "The plugin com.eco.bio7.rffm (feature 'Bio7 Embedded R') is not installed.";
		}
		return null;
	}

	/**
	 * Starts embedded R (in the console of Bio7) if it is selected and not running.
	 *
	 * @return null if it runs, else the error (Rserve is used then)
	 */
	public static String startEmbedded() {
		if (embedded != null) {
			return null;
		}
		String reason = unavailableReason();
		if (reason != null) {
			return reason;
		}
		try {
			return EmbeddedR.start();
		} catch (LinkageError e) {
			return "Embedded R could not be loaded: " + e;
		}
	}
}
