package com.eco.bio7.reditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class OpenPlotPreferences extends Action {

	

	

	public OpenPlotPreferences(String text) {
		super(text);
		setActionDefinitionId("com.eco.bio7.reditor.rserve.plot.preferences");
		setId("com.eco.bio7.reditor.plot.preferences");

		//setText("R Plot Preferences");

	}

	public void run() {
		PreferenceDialog dialog =
				PreferencesUtil.createPreferenceDialogOn(null,"com.eco.bio7.RServePlot",null,null);
				dialog.open(); 
		
	}

}