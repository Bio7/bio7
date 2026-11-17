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

/** The code context and the code completion of the Python editor (as "Preferences Editor Code" of R). */
public class PythonCodePreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PythonCodePreferences() {
		super(GRID);
		setPreferenceStore(PythonPreferences.store());
		setDescription("Python Editor Code (close and reopen the editor to apply the code completion settings)");
	}

	@Override
	public void createFieldEditors() {
		addField(new LabelFieldEditor("Code context:", getFieldEditorParent()));
		addField(new BooleanFieldEditor(com.eco.bio7.pythonedit.analysis.PythonEditorSupport.HOVERS,
				"Enable Hovers (all, also in the context menu of the editor)", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Code completion/templates:", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PythonPreferences.TYPED_COMPLETION, "Open code completion when typing", getFieldEditorParent()));
		addField(new StringFieldEditor(PythonPreferences.ACTIVATION_CHARS, "Activation chars (e.g. '.' or '.abc...xyz')", -1,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		IntegerFieldEditor delay = new IntegerFieldEditor(PythonPreferences.ACTIVATION_DELAY, "Delay of the code completion (ms)", getFieldEditorParent());
		delay.setValidRange(0, 5000);
		addField(delay);
		addField(new BooleanFieldEditor(PythonPreferences.LIVE_COMPLETION,
				"Propose the variables of the running Python (e.g., the columns of a DataFrame)", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
