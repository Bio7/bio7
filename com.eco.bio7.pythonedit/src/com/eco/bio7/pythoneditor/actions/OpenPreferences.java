package com.eco.bio7.pythoneditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class OpenPreferences extends Action {

	

	

	public OpenPreferences() {
		super("Preferences");
		setId("com.eco.bio7.python_editor_preferences");

		setText("Python Editor Preferences");

	}

	public void run() {
		PreferenceDialog dialog =
				PreferencesUtil.createPreferenceDialogOn(null,"prefpython",null,null);
				dialog.open(); 
		
	}

}