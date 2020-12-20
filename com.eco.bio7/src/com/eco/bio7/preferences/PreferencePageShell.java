package com.eco.bio7.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.Bio7Plugin;

public class PreferencePageShell extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Create the preference page
	 */
	public PreferencePageShell() {
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		setDescription("");
	}

	/**
	 * Create contents of the preference page
	 */
	@Override
	protected void createFieldEditors() {
		{
			LabelFieldEditor labelFieldEditor = new LabelFieldEditor("Shell:", getFieldEditorParent());
			labelFieldEditor.setLabelText("Shell:");
			addField(labelFieldEditor);
		}
		addField(new StringFieldEditor("shell_arguments", "Command After Startup", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		
		
		
		SpacerFieldEditor spacer = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer);
		
		addField(new LabelFieldEditor("R:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("r_pipe", "Execute Editor Source in R", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new DirectoryFieldEditor("r_pipe_path", "Path R		", getFieldEditorParent()));
				

	}

	/**
	 * Initialize the preference page
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

}
