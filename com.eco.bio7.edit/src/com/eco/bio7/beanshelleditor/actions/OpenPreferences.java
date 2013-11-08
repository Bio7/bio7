package com.eco.bio7.beanshelleditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class OpenPreferences extends Action {

	

	

	public OpenPreferences() {
		super("Preferences");
		setId("com.eco.bio7.script_preferences");

		setText("BeanShell/Groovy Preferences");

	}

	public void run() {
		PreferenceDialog dialog =
				PreferencesUtil.createPreferenceDialogOn(null,"prefbeanshell",null,null);
				dialog.open(); 
		
	}

}