package com.eco.bio7.actions;

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
import com.eco.bio7.preferences.RPrefDebug;
import com.eco.bio7.preferences.RServePlotPrefs;
import com.eco.bio7.preferences.RServePrefs;
import com.eco.bio7.rpreferences.TemplatesPreferencePage;
import com.eco.bio7.rpreferences.WorkbenchPreferenceR;

/**
 * Open the preferences dialog
 */
public class OpenRservePreferencesAction extends Action implements ActionFactory.IWorkbenchAction {

	private IWorkbenchWindow workbenchWindow;

	public OpenRservePreferencesAction() {
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
		
		RPackagesPreferencePage pref1=new RPackagesPreferencePage();
		IPreferencePage page1 = pref1;
		page1.setTitle("R Preferences Built Tools");

		WorkbenchPreferenceR pref2 = new WorkbenchPreferenceR();
		IPreferencePage page2 = pref2;
		page2.setTitle("Rserve editor preferences");
		// page.setImageDescriptor(image);

		TemplatesPreferencePage pref3 = new TemplatesPreferencePage();
		IPreferencePage page3 = pref3;
		page3.setTitle("Rserve editor templates");
		
		PreferencePageShell pref4 = new PreferencePageShell();
		IPreferencePage page4 = pref4;
		page4.setTitle("Preferences Native");
		
		RServePlotPrefs pref5 = new RServePlotPrefs();
		IPreferencePage page5 = pref5;
		page5.setTitle("Rserve Plot preferences");
		
		RPrefDebug pref6 = new RPrefDebug();
		IPreferencePage page6 = pref6;
		page6.setTitle("R Debug preferences");

		showPreferencePage("com.eco.bio7.RServe", page,"com.eco.bio7.RServePlot",page5,"com.eco.bio7.rpackages", page1, "com.eco.bio7.RServe.editor", page2, "com.eco.bio7.RServe.editor.templates", page3,"com.eco.bio7.native",page4,"com.eco.bio7.debug",page6);
	}

	protected void showPreferencePage(String id, IPreferencePage page,String id5, IPreferencePage page5,String id1, IPreferencePage page1, String id2, IPreferencePage page2, String id3, IPreferencePage page3,String id4, IPreferencePage page4,String id6,IPreferencePage page6) {
		final IPreferenceNode targetNode = new PreferenceNode(id, page);
		
		final IPreferenceNode targetNode1 = new PreferenceNode(id1, page1);

		final IPreferenceNode targetNode2 = new PreferenceNode(id2, page2);

		final IPreferenceNode targetNode3 = new PreferenceNode(id3, page3);
		
		final IPreferenceNode targetNode4 = new PreferenceNode(id4, page4);
		
		final IPreferenceNode targetNode5 = new PreferenceNode(id5, page5);
		
		final IPreferenceNode targetNode6 = new PreferenceNode(id6, page6);

		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		manager.addToRoot(targetNode5);
		manager.addToRoot(targetNode1);
		manager.addToRoot(targetNode2);
		manager.addToRoot(targetNode3);
		manager.addToRoot(targetNode4);
		manager.addToRoot(targetNode6);
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