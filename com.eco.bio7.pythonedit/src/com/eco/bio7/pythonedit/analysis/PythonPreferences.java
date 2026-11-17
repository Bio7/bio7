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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import com.eco.bio7.pythonedit.PythonEditorPlugin;

/**
 * The preferences of the Python editor for the code completion, the code
 * analysis and the formatting (the pages "Preferences Python Editor Code",
 * "... Code Analysis" and "... Formatting") and the options which are sent to
 * the analysis (bio7_analysis.py).
 */
public final class PythonPreferences {

	/* Code completion */
	public static final String TYPED_COMPLETION = "PY_TYPED_CODE_COMPLETION";
	public static final String ACTIVATION_CHARS = "PY_ACTIVATION_CHARS";
	public static final String ACTIVATION_DELAY = "PY_ACTIVATION_DELAY";
	public static final String LIVE_COMPLETION = "PY_LIVE_COMPLETION";

	/* Code analysis */
	public static final String ANALYSIS_WARNINGS = "PY_ANALYSIS_WARNINGS";
	public static final String ANALYSIS_RUFF = "PY_ANALYSIS_RUFF";
	public static final String ANALYSIS_SELECT = "PY_ANALYSIS_SELECT";
	public static final String ANALYSIS_IGNORE = "PY_ANALYSIS_IGNORE";
	public static final String ANALYSIS_LINE_LENGTH = "PY_ANALYSIS_LINE_LENGTH";
	public static final String ANALYSIS_MISSING_MODULES = "PY_ANALYSIS_MISSING_MODULES";

	/* Formatting */
	public static final String FORMATTER = "PY_FORMATTER";
	public static final String FORMAT_LINE_LENGTH = "PY_FORMAT_LINE_LENGTH";
	public static final String FORMAT_QUOTES = "PY_FORMAT_QUOTES";
	public static final String FORMAT_TABS = "PY_FORMAT_TABS";
	public static final String FORMAT_SORT_IMPORTS = "PY_FORMAT_SORT_IMPORTS";

	private PythonPreferences() {
	}

	/** The defaults (called by the preference initializer). */
	public static void setDefaults(IPreferenceStore s) {
		s.setDefault(TYPED_COMPLETION, true);
		s.setDefault(ACTIVATION_CHARS, ".");
		s.setDefault(ACTIVATION_DELAY, 300);
		s.setDefault(LIVE_COMPLETION, true);
		s.setDefault(ANALYSIS_WARNINGS, true);
		s.setDefault(ANALYSIS_RUFF, true);
		s.setDefault(ANALYSIS_SELECT, "");
		s.setDefault(ANALYSIS_IGNORE, "");
		s.setDefault(ANALYSIS_LINE_LENGTH, 0);
		s.setDefault(ANALYSIS_MISSING_MODULES, true);
		s.setDefault(FORMATTER, "auto");
		s.setDefault(FORMAT_LINE_LENGTH, 0);
		s.setDefault(FORMAT_QUOTES, "double");
		s.setDefault(FORMAT_TABS, false);
		s.setDefault(FORMAT_SORT_IMPORTS, false);
	}

	public static IPreferenceStore store() {
		return PythonEditorPlugin.getDefault().getPreferenceStore();
	}

	/** The options of the code analysis (line length 0: the default of ruff or the project). */
	public static Map<String, Object> analysisOptions() {
		IPreferenceStore s = store();
		Map<String, Object> o = new LinkedHashMap<>();
		o.put("warnings", s.getBoolean(ANALYSIS_WARNINGS));
		o.put("ruff", s.getBoolean(ANALYSIS_RUFF));
		o.put("select", s.getString(ANALYSIS_SELECT).strip());
		o.put("ignore", s.getString(ANALYSIS_IGNORE).strip());
		o.put("line_length", s.getInt(ANALYSIS_LINE_LENGTH));
		o.put("missing_modules", s.getBoolean(ANALYSIS_MISSING_MODULES));
		return o;
	}

	/** The options of the formatting (line length 0: the default of the formatter or the project). */
	public static Map<String, Object> formatOptions() {
		IPreferenceStore s = store();
		Map<String, Object> o = new LinkedHashMap<>();
		o.put("formatter", s.getString(FORMATTER));
		o.put("line_length", s.getInt(FORMAT_LINE_LENGTH));
		o.put("quote_style", s.getString(FORMAT_QUOTES));
		o.put("indent_tabs", s.getBoolean(FORMAT_TABS));
		o.put("sort_imports", s.getBoolean(FORMAT_SORT_IMPORTS));
		return o;
	}
}
