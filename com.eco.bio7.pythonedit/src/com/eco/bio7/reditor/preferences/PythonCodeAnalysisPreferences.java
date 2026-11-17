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

/** The code analysis of the Python editor (ruff or the own checks; as "Preferences Editor Code Analysis" of R). */
public class PythonCodeAnalysisPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PythonCodeAnalysisPreferences() {
		super(GRID);
		setPreferenceStore(PythonPreferences.store());
		setDescription("Python Editor Code Analysis (applied with the next change of the code)");
	}

	@Override
	public void createFieldEditors() {
		addField(new LabelFieldEditor("Code Analysis:", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PythonPreferences.ANALYSIS_WARNINGS, "Show warnings (else only the errors of the compiler)", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PythonPreferences.ANALYSIS_MISSING_MODULES, "Check for installed modules (imports)", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("ruff (if installed, else the own checks: undefined names, unused imports and variables):", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PythonPreferences.ANALYSIS_RUFF, "Use ruff", getFieldEditorParent()));
		addField(new StringFieldEditor(PythonPreferences.ANALYSIS_SELECT, "Rules (e.g. 'E,F,W,B'; empty: the default or the project)", -1,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new StringFieldEditor(PythonPreferences.ANALYSIS_IGNORE, "Ignored rules (e.g. 'E501,F401')", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE,
				getFieldEditorParent()));
		IntegerFieldEditor length = new IntegerFieldEditor(PythonPreferences.ANALYSIS_LINE_LENGTH, "Line length (E501; 0: the default or the project)",
				getFieldEditorParent());
		length.setValidRange(0, 400);
		addField(length);
		addField(new LabelFieldEditor("The rules of ruff: https://docs.astral.sh/ruff/rules/", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
