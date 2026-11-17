package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PreferencesUtil;
import com.eco.bio7.util.Util;

/**
 * Open the preferences dialog
 */
public class OpenRPreferencesAction extends Action implements ActionFactory.IWorkbenchAction {

	private IWorkbenchWindow workbenchWindow;

	public OpenRPreferencesAction() {
		super("R Preferences");
		setActionDefinitionId("com.eco.bio7.r_openPreferences");
		setId("com.eco.bio7.r.preferences");
		setText("Preferences");

	}

	public void run() {

		/* Open the R preferences in the general Bio7 Preferences! */
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(Util.getShell(), "com.eco.bio7.R", null, null);
		// diag.getTreeViewer().expandAll();
		/*
		 * Here we also expand the nodes of the preferences! To access all nodes
		 * we have to expand the Tree!
		 */
		dialog.getTreeViewer().expandAll();
		Object[] obj = dialog.getTreeViewer().getExpandedElements();
		for (int i = 0; i < obj.length; i++) {
			if (obj[i] instanceof PreferenceNode) {
				PreferenceNode it = (PreferenceNode) obj[i];

				if (it.getId().equals("com.eco.bio7.preferences.Bio7PreferencePage")) {
					dialog.getTreeViewer().setExpandedState(it, true);
				} else if (it.getId().equals("com.eco.bio7.R")) {
					dialog.getTreeViewer().setExpandedState(it, true);
				} else {
					/* Close the nodes which we don't use here! */
					dialog.getTreeViewer().setExpandedState(it, false);
				}
			}

		}

		dialog.open();

	}

	/*
	 * protected void configureWorkspaceSettings() {
	 * 
	 * RPreferences pref = new RPreferences(); IPreferencePage page = pref;
	 * page.setTitle("R preferences"); // page.setImageDescriptor(image);
	 * 
	 * RPackagesPreferencePage pref1=new RPackagesPreferencePage();
	 * IPreferencePage page1 = pref1;
	 * page1.setTitle("R Preferences Built Tools");
	 * 
	 * WorkbenchPreferenceR pref2 = new WorkbenchPreferenceR(); IPreferencePage
	 * page2 = pref2; page2.setTitle("R editor preferences"); //
	 * page.setImageDescriptor(image);
	 * 
	 * TemplatesPreferencePage pref3 = new TemplatesPreferencePage();
	 * IPreferencePage page3 = pref3; page3.setTitle("R editor templates");
	 * 
	 * PreferencePageShell pref4 = new PreferencePageShell(); IPreferencePage
	 * page4 = pref4; page4.setTitle("Preferences Native");
	 * 
	 * RPlotPreferences pref5 = new RPlotPreferences(); IPreferencePage page5 =
	 * pref5; page5.setTitle("R Plot preferences");
	 * 
	 * RPrefDebug pref6 = new RPrefDebug(); IPreferencePage page6 = pref6;
	 * page6.setTitle("R Debug preferences");
	 * 
	 * RCodePreferences pref7 = new RCodePreferences(); IPreferencePage page7 =
	 * pref7; page7.setTitle("R code preferences");
	 * 
	 * RCodeAnalysisPreferences pref8 = new RCodeAnalysisPreferences();
	 * IPreferencePage page8 = pref8;
	 * page8.setTitle("R code analysis preferences");
	 * 
	 * showPreferencePage("com.eco.bio7.RTools",
	 * page,"com.eco.bio7.RPlot",page5,"com.eco.bio7.rpackages", page1,
	 * "com.eco.bio7.RTools.editor", page2,
	 * "com.eco.bio7.RTools.editor.templates",
	 * page3,"com.eco.bio7.native",page4,"com.eco.bio7.debug",page6,
	 * "com.eco.bio7.rcode",page7,"com.eco.bio7.rcode.analysis",page8); }
	 * 
	 * protected void showPreferencePage(String id, IPreferencePage page,String
	 * id5, IPreferencePage page5,String id1, IPreferencePage page1, String id2,
	 * IPreferencePage page2, String id3, IPreferencePage page3,String id4,
	 * IPreferencePage page4,String id6,IPreferencePage page6,String
	 * id7,IPreferencePage page7,String id8,IPreferencePage page8) { final
	 * IPreferenceNode targetNode = new PreferenceNode(id, page);
	 * 
	 * final IPreferenceNode targetNode1 = new PreferenceNode(id1, page1);
	 * 
	 * final IPreferenceNode targetNode2 = new PreferenceNode(id2, page2);
	 * 
	 * final IPreferenceNode targetNode3 = new PreferenceNode(id3, page3);
	 * 
	 * final IPreferenceNode targetNode4 = new PreferenceNode(id4, page4);
	 * 
	 * final IPreferenceNode targetNode5 = new PreferenceNode(id5, page5);
	 * 
	 * final IPreferenceNode targetNode6 = new PreferenceNode(id6, page6);
	 * 
	 * final IPreferenceNode targetNode7 = new PreferenceNode(id7, page7);
	 * 
	 * final IPreferenceNode targetNode8 = new PreferenceNode(id8, page8);
	 * 
	 * PreferenceManager manager = new PreferenceManager();
	 * manager.addToRoot(targetNode); manager.addToRoot(targetNode5);
	 * manager.addToRoot(targetNode1); manager.addToRoot(targetNode2);
	 * manager.addToRoot(targetNode3); manager.addToRoot(targetNode4);
	 * manager.addToRoot(targetNode6); manager.addToRoot(targetNode7);
	 * manager.addToRoot(targetNode8); final PreferenceDialog dialog = new
	 * WorkbenchPreferenceDialog(Util.getShell(), manager);
	 * BusyIndicator.showWhile(Display.getCurrent(), new Runnable() { public
	 * void run() { dialog.create();
	 * dialog.setMessage(targetNode.getLabelText()); dialog.open(); } }); }
	 */

	public void dispose() {
		workbenchWindow = null;
	}

}