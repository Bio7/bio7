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
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new BooleanFieldEditor("UNUSED_FUNCTION", "Detect Unused Functions", getFieldEditorParent()));
		addField(new BooleanFieldEditor("MISSING_FUNCTION", "Detect if function is available", getFieldEditorParent()));
		addField(new BooleanFieldEditor("UNUSED_VAR", "Detect if variable is used", getFieldEditorParent()));
		addField(new BooleanFieldEditor("MISSING_VAR", "Detect if variable is available", getFieldEditorParent()));	
		addField(new BooleanFieldEditor("CHECK_CONSTANTS", "Check for wrong constants (na, true, false, null)", getFieldEditorParent()));	
		addField(new BooleanFieldEditor("FUNCTION_ALREADY_DEFINED", "Check if a function is already defined", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CHECK_MISSING_FUNCTION_CALL_ARGS", "Check if a function call argument is missing", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CHECK_EXCESSIVE_FUNCTION_CALL_ARGS", "Check if a function call has to many arguments", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CHECK_VARIABLE_ASSIGNMENT_FUNCTION_CALL_ARGS", "Check if the name of a variable assignment in a function call is wrong", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CHECK_FOR_EMPTY_ARG_FUNCTION", "Check function call with parameter on a parameterless function", getFieldEditorParent()));

		
		
		
		
	}
	public void init(IWorkbench workbench) {

		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		store.setDefault("UNUSED_FUNCTION", true);
		store.setDefault("MISSING_FUNCTION", true);
		store.setDefault("UNUSED_VAR", true);
		store.setDefault("MISSING_VAR", true);
		store.setDefault("CHECK_CONSTANTS", true);
		store.setDefault("FUNCTION_ALREADY_DEFINED", true);
		store.setDefault("CHECK_MISSING_FUNCTION_CALL_ARGS", true);
		store.setDefault("CHECK_EXCESSIVE_FUNCTION_CALL_ARGS", true);
		store.setDefault("CHECK_VARIABLE_ASSIGNMENT_FUNCTION_CALL_ARGS", true);
		store.setDefault("CHECK_FOR_EMPTY_ARG_FUNCTION", true);
		
		

	}

	public boolean performOk() {

		return super.performOk();
	}

	public void propertyChange(PropertyChangeEvent evt) {

	}

}
