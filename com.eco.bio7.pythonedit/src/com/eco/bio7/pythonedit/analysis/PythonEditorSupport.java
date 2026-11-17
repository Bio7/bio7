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
package com.eco.bio7.pythonedit.analysis;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The connection of the Python editor to Bio7 (set by the main plugin): the
 * selected Python, the variables of the running embedded Python and the
 * installation of packages. All may be null (no Bio7, Python not running).
 */
public final class PythonEditorSupport {

	/** The Python executable for the analysis (null: none found). */
	public static volatile Supplier<String> executable;

	/** The names of the variables of the running Python (no warning for them). */
	public static volatile Supplier<Collection<String>> liveNames;

	/**
	 * The attributes of a variable of the running Python for the completion
	 * ("df" -> the names of df): name and kind; null if not available.
	 */
	public static volatile Function<String, List<String[]>> liveCompletion;

	/** A short description of a variable of the running Python (type, shape, value) or null. */
	public static volatile Function<String, String> liveHover;

	/**
	 * The debugger is stopped: type, size and value of an expression in the frame
	 * of the stop (several lines); null if not stopped or not defined. Called in
	 * the thread of the hover.
	 */
	public static volatile Function<String, String> debugHover;

	/** The actions of the debug hover ("workspace", "table", "imagej") with the expression. */
	public static volatile BiConsumer<String, String> debugAction;

	/** The preference: all hovers of the Python editor on or off. */
	public static final String HOVERS = "PYTHON_EDITOR_HOVERS";

	/** True if the hovers are on (also if the default of the preference is missing). */
	public static boolean hoversEnabled() {
		org.eclipse.jface.preference.IPreferenceStore store = com.eco.bio7.pythonedit.PythonEditorPlugin.getDefault().getPreferenceStore();
		return !store.contains(HOVERS) || store.getBoolean(HOVERS);
	}

	/** Profiles the script of the active editor (true: only the selection) in the embedded Python. */
	public static volatile Consumer<Boolean> profileScript;

	/** Debugs the script of the active editor (the debugger of the embedded Python). */
	public static volatile Runnable debugScript;

	/** Opens the installation of a package (e.g., the Python Packages view). */
	public static volatile Consumer<String> installPackage;

	private PythonEditorSupport() {
	}

	static String executable() {
		Supplier<String> s = executable;
		return s == null ? null : s.get();
	}

	static Collection<String> liveNames() {
		Supplier<Collection<String>> s = liveNames;
		try {
			Collection<String> c = s == null ? null : s.get();
			return c == null ? Set.of() : c;
		} catch (RuntimeException e) {
			return Set.of();
		}
	}
}
