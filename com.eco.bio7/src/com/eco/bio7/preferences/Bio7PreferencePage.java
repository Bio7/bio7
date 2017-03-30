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
		addField(new DirectoryFieldEditor(PreferenceConstants.D_STRING, "&Path to Startup Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_IMPORT, "&Path to Import Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_EXPORT, "&Path to Export Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_SCRIPT_GENERAL, "&Path to General Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_SCRIPT_SPATIAL, "&Path to Spatial Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_SCRIPT_IMAGE, "&Path to Image Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_SCRIPT_R, "&Path to R Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_RSHELL_SCRIPTS, "&Path to R Shell Scripts:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.D_GRID_SCRIPTS, "&Path to Grid Scripts:", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		

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