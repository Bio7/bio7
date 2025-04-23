package com.eco.bio7.rpreferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.reditor.Bio7REditorPlugin;

public class DatabasePreference extends  FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Create the preference page.
	 */
	public DatabasePreference() {
		super(GRID);
		setPreferenceStore(Bio7REditorPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * Create contents of the preference page.
	 */
	@Override
	protected void createFieldEditors() {
		addField(new FileFieldEditor("LOCATION_DATABASE_XML_CONFIG", "Database Configuration File", getFieldEditorParent()));
	}

	/**
	 * Initialize the preference page.
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

}
