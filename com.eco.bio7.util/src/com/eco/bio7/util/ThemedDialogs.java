/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.util;

import org.eclipse.core.runtime.Platform;

/**
 * The preferences (node com.eco.bio7, default true) of the Bio7 dialogs, which
 * are styled by the theme on every OS: the color dialog, the font dialog and the
 * message box. False: the native dialog of the OS (on Windows always light).
 */
public final class ThemedDialogs {

	/** The color dialog ({@link ThemedColorDialog}). */
	public static final String COLOR = "BIO7_COLOR_DIALOG";
	/** The font dialog ({@link ThemedFontDialog}). */
	public static final String FONT = "BIO7_FONT_DIALOG";
	/** The message box ({@link ThemedMessageBox}). */
	public static final String MESSAGE = "BIO7_MESSAGE_BOX";

	private ThemedDialogs() {
	}

	/** True if the Bio7 dialog of the preference is used (default true). */
	public static boolean isEnabled(String preference) {
		try {
			return Platform.getPreferencesService().getBoolean("com.eco.bio7", preference, true, null);
		} catch (RuntimeException e) {
			/* Without the preferences (e.g., in a test): the Bio7 dialog */
			return true;
		}
	}
}
