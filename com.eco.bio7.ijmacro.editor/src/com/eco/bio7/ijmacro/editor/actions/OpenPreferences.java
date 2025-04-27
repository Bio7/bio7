package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class OpenPreferences extends Action {

	

	

	public OpenPreferences() {
		super("Preferences");
		setActionDefinitionId("com.eco.bio7.ijmacro.editor.preferences");

		setText("ImageJ Macro Preferences");

	}

	public void run() {
		PreferenceDialog dialog =
				PreferencesUtil.createPreferenceDialogOn(null,"prefijmacroeditor",null,null);
				dialog.open(); 
		
	}

}