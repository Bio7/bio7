package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;
import com.eco.bio7.preferences.PreferencePageShell;
import com.eco.bio7.preferences.RPackagesPreferencePage;
import com.eco.bio7.preferences.RServePrefs;
import com.eco.bio7.rpreferences.TemplatesPreferencePage;
import com.eco.bio7.rpreferences.WorkbenchPreferenceR;

/**
 * Open the preferences dialog
 */
public class OpenRservePlotPreferencesAction extends Action implements ActionFactory.IWorkbenchAction {

	private IWorkbenchWindow workbenchWindow;

	public OpenRservePlotPreferencesAction() {
		super("Rserve");
		setId("com.eco.bio7.r.rserve");
		setText("Preferences");

	}

	public void run() {

		configureWorkspaceSettings();

	}

	protected void configureWorkspaceSettings() {

		RServePrefs pref = new RServePrefs();
		IPreferencePage page = pref;
		page.setTitle("Rserve preferences");
		// page.setImageDescriptor(image);
		
		

		showPreferencePage("com.eco.bio7.RServe",page);
	}

	protected void showPreferencePage(String id, IPreferencePage page) {
		final IPreferenceNode targetNode = new PreferenceNode(id, page);
		
		
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		
		final PreferenceDialog dialog = new WorkbenchPreferenceDialog(new Shell(), manager);
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				dialog.open();
			}
		});
	}

	public void dispose() {
		workbenchWindow = null;
	}

}