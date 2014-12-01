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
		//addField(new DirectoryFieldEditor("r_pipe_path", "Path R		", getFieldEditorParent()));
		SpacerFieldEditor spacer1 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer1);
		
		{
			LabelFieldEditor labelFieldEditor = new LabelFieldEditor("Python/Blender:", getFieldEditorParent());
			labelFieldEditor.setLabelText("Python/Blender:");
			addField(labelFieldEditor);
		}
		addField(new BooleanFieldEditor("python_pipe", "Execute Editor (Floweditor) Source in Python/Blender", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new RadioGroupFieldEditor("python_select", "Select Interpreter", 1, new String[][] { { "Python", "Python" }, { "Blender", "Blender" } }, getFieldEditorParent(), false));
		addField(new DirectoryFieldEditor("python_pipe_path", "Path Python", getFieldEditorParent()));
		addField(new LabelFieldEditor("Blender:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor("path_blender", "Path Blender", getFieldEditorParent()));

		RadioGroupFieldEditor radioGroupFieldEditor = new RadioGroupFieldEditor("blender_options", "Blender Default Options", 1, new String[][] { { "None (-)", "pnone" },{ "Interactive Shell (--python-console)", "interactive" },
				{ "Python Script (-P)", "pscript" } }, getFieldEditorParent(), false);
		
		addField(radioGroupFieldEditor);
		addField(new StringFieldEditor("blender_args", "Blender Arguments", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));

		addField(new MultiLineTextFieldEditor("before_script_blender", "Before Script Execution (Interactive Shell)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new MultiLineTextFieldEditor("after_script_blender", "After Script Execution (Interactive Shell)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		SpacerFieldEditor spacer2 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer2);
		

	}

	/**
	 * Initialize the preference page
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

}
