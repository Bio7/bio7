package com.eco.bio7.preferences;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.wb.swt.DoubleFieldEditor;
import org.osgi.framework.Bundle;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;


public class Bio7PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public Bio7PreferencePage() {
		super(GRID);

		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		setDescription("");
	}

	public void createFieldEditors() {
		
		addField(new DirectoryFieldEditor(PreferenceConstants.PATH_LIBREOFFICE, "&Path to LibreOffice:", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Script location", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_STRING, "&Path to startup Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_SCRIPT_GENERAL, "&Path to main menu scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_RSHELL_SCRIPTS, "&Path to 'R-Shell' context menu scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_GRID_SCRIPTS, "&Path to 'Table' menu scripts:", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new BooleanFieldEditor("SAVE_ALL_EDITORS", "Save all editors on close", getFieldEditorParent()));
		addField(new BooleanFieldEditor("TOUCH_BAR_MAC", "Enable Touch Bar for MacOSX", getFieldEditorParent()));
		addField(new BooleanFieldEditor("ENABLE_SCENE_BUILDER_LINUX", "Enable SceneBuilder for Linux", getFieldEditorParent()));
		
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Bio7 Console", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BIO7_CONSOLE_INTERPRET_ASCII", "Interpret ASCII control characters (Restart to apply)", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BIO7_CONSOLE_INTERPRET_CARRIAGE_RETURN", "Interpret Carriage Return (\\r) as control character (Restart to apply)", getFieldEditorParent()));
		
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Java Swing", getFieldEditorParent()));
		addField(new DoubleFieldEditor("JAVA2D_UI_SCALE", "Set Scale Swing Components", getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

		try {
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				store.setDefault(PreferenceConstants.PATH_R, Reg.setPrefReg(PreferenceConstants.PATH_R));
				store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, Reg.setPrefReg(PreferenceConstants.PATH_LIBREOFFICE));
			}
		} catch (RuntimeException e) {

		}

	}

}