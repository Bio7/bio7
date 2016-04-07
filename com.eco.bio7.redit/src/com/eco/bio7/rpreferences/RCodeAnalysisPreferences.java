package com.eco.bio7.rpreferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.reditor.Bio7REditorPlugin;

public class RCodeAnalysisPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	public RCodeAnalysisPreferences() {
		super(GRID);

		setPreferenceStore(Bio7REditorPlugin.getDefault().getPreferenceStore());
		//setDescription("R Code Preferences");
	}

	public void createFieldEditors() {

		
		addField(new LabelFieldEditor("Code Analysis:", getFieldEditorParent()));
		
		addField(new BooleanFieldEditor("UNUSED_FUNCTIONS", "Detect Unused Functions", getFieldEditorParent()));
		addField(new BooleanFieldEditor("FUNCTION_AVAILABLE", "Detect if function is available", getFieldEditorParent()));
		addField(new BooleanFieldEditor("UNUSED_VAR", "Detect if variable is used", getFieldEditorParent()));
		addField(new BooleanFieldEditor("MISSING_VAR", "Detect if variable is available", getFieldEditorParent()));		
		
		
		
	}
	public void init(IWorkbench workbench) {

		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		store.setDefault("UNUSED_FUNCTIONS", true);
		store.setDefault("FUNCTION_AVAILABLE", true);
		store.setDefault("UNUSED_VAR", true);
		store.setDefault("REPEAT_LOOP_FOLDING", true);
		store.setDefault("MISSING_VAR", true);
		
		

	}

	public boolean performOk() {

		return super.performOk();
	}

	public void propertyChange(PropertyChangeEvent evt) {

	}

}
