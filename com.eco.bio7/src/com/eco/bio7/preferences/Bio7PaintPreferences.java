package com.eco.bio7.preferences;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class Bio7PaintPreferences extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private MultiLineTextFieldEditor mult;

	public Bio7PaintPreferences() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		setDescription("Startup of Bio7: R, Python, startup scripts and commands");

	}

	public void createFieldEditors() {
		/* Started one after the other in a job (the user interface stays usable) */
		addField(new LabelFieldEditor("Start at the startup of Bio7 (in the background):", getFieldEditorParent()));
		addField(new BooleanFieldEditor(com.eco.bio7.rcp.AutoStart.START_R, "Start R", getFieldEditorParent()));
		addField(new BooleanFieldEditor(com.eco.bio7.rcp.AutoStart.START_PYTHON, "Start Python", getFieldEditorParent()));
		addField(new org.eclipse.jface.preference.RadioGroupFieldEditor(com.eco.bio7.rcp.AutoStart.CONSOLE, "The console shows (if both are started):", 2,
				new String[][] { { "R", "R" }, { "Python", "Python" } }, getFieldEditorParent(), false));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		/* Else only with the toolbar actions (or at the startup above) */
		addField(new LabelFieldEditor("Start when used (a script, a chunk, the API):", getFieldEditorParent()));
		addField(new BooleanFieldEditor(com.eco.bio7.rcp.AutoStart.R_ON_USE, "Start R when it is used", getFieldEditorParent()));
		addField(new BooleanFieldEditor(com.eco.bio7.rcp.AutoStart.PYTHON_ON_USE, "Start Python when it is used", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new BooleanFieldEditor("STARTUP_SCRIPTS", "Enable startup scripts", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		mult=new MultiLineTextFieldEditor("BIO7_STARTUP_COMMANDS", "Startup Commands (Groovy Script)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		addField(mult);
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("JDT:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("SHOW_JDT_GUI", "Enable JDT Gui elements", getFieldEditorParent()));
		//addField(new SpacerFieldEditor(getFieldEditorParent()));
		/*addField(new LabelFieldEditor("Additional Default Perspectives:", getFieldEditorParent()));
		addField(new StringFieldEditor("DEFAULT_CUSTOM_PERSPECTIVE1", "Additional perspective at startup (ID)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new StringFieldEditor("DEFAULT_CUSTOM_PERSPECTIVE2", "Additional perspective at startup (ID)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new StringFieldEditor("DEFAULT_CUSTOM_PERSPECTIVE3", "Additional perspective at startup (ID)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		*/
		
	}

	public void init(IWorkbench workbench) {
		

		

	}

}
