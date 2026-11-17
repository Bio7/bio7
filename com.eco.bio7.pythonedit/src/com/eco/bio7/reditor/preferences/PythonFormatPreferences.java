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
package com.eco.bio7.reditor.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.pythonedit.analysis.PythonPreferences;

/** The formatting of the Python editor (ruff format or black; as "Preferences Editor Formatting" of R). */
public class PythonFormatPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PythonFormatPreferences() {
		super(GRID);
		setPreferenceStore(PythonPreferences.store());
		setDescription("Python Editor Formatting (Source > Format)");
	}

	@Override
	public void createFieldEditors() {
		addField(new ComboFieldEditor(PythonPreferences.FORMATTER, "Formatter:", new String[][] { { "Automatic (ruff, else black)", "auto" },
				{ "ruff format", "ruff" }, { "black", "black" } }, getFieldEditorParent()));
		IntegerFieldEditor length = new IntegerFieldEditor(PythonPreferences.FORMAT_LINE_LENGTH, "Line length (0: 88 or the project)", getFieldEditorParent());
		length.setValidRange(0, 400);
		addField(length);
		addField(new ComboFieldEditor(PythonPreferences.FORMAT_QUOTES, "Quotes of strings:", new String[][] { { "Double quotes", "double" },
				{ "Single quotes (ruff)", "single" }, { "Keep the quotes", "preserve" } }, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PythonPreferences.FORMAT_TABS, "Indent with tabs (ruff)", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PythonPreferences.FORMAT_SORT_IMPORTS, "Sort the imports (ruff)", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("A configuration of the project (pyproject.toml, ruff.toml) is used for the other settings.", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
