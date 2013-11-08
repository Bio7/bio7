package com.eco.bio7.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.Bio7Plugin;

public class OpenOfficePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Create the preference page
	 */
	public OpenOfficePreferencePage() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		setDescription("");
	}

	/**
	 * Create contents of the preference page
	 */
	@Override
	protected void createFieldEditors() {

		
		addField(new BooleanFieldEditor(PreferenceConstants.D_ENABLE_HEAD_REPLACE, "Enable replacement for LibreOffice and the Bio7 Table component", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.D_OPENOFFICE_HEAD, "Replace wrong chars", getFieldEditorParent()));
		
		
	}

	/**
	 * Initialize the preference page
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

}
