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
package com.eco.bio7.gdal.ui;

import java.nio.file.Path;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * The preferences of GDAL (in the instance scope of the plugin
 * com.eco.bio7.gdal): GDAL is only loaded if it is enabled (a library which
 * doesn't work can't affect the application while GDAL is not used).
 */
public final class GdalPreferences {

	public static final String PLUGIN_ID = "com.eco.bio7.gdal";
	public static final String PAGE_ID = "com.eco.bio7.gdal.preferences";

	/** GDAL can be used (loaded when it is needed). */
	public static final String ENABLED = "GDAL_ENABLED";
	/** GDAL is loaded at the start of the application (in the background). */
	public static final String LOAD_AT_STARTUP = "GDAL_LOAD_AT_STARTUP";
	/** The library (a file or a folder); empty: the automatic search. */
	public static final String LIBRARY = "GDAL_LIBRARY";
	/** The library which passed the test in a separate process (loaded without a new test). */
	public static final String TESTED = "GDAL_TESTED_LIBRARY";

	private static ScopedPreferenceStore store;

	private GdalPreferences() {
	}

	public static synchronized IPreferenceStore store() {
		if (store == null) {
			store = new ScopedPreferenceStore(InstanceScope.INSTANCE, PLUGIN_ID);
			store.setDefault(ENABLED, false);
			store.setDefault(LOAD_AT_STARTUP, false);
			store.setDefault(LIBRARY, "");
			store.setDefault(TESTED, "");
		}
		return store;
	}

	public static boolean isEnabled() {
		return store().getBoolean(ENABLED);
	}

	/** The library of the preferences (null: the automatic search). */
	public static Path library() {
		String s = store().getString(LIBRARY).strip();
		return s.isEmpty() ? null : Path.of(s);
	}

	static void save() {
		try {
			((ScopedPreferenceStore) store()).save();
		} catch (java.io.IOException e) {
			/* Kept in memory */
		}
	}
}
