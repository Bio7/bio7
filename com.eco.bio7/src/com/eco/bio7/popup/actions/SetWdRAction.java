package com.eco.bio7.popup.actions;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.rbridge.RServe;

public class SetWdRAction extends Action implements IObjectActionDelegate {

	public SetWdRAction() {
		super();
		setId("com.eco.bio7.setwd");
		setActionDefinitionId("com.eco.bio7.setWdAction");
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {

		if (RServe.isAliveDialog()) {

			ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
					.getSelection();
			IStructuredSelection strucSelection = null;
			if (selection instanceof IStructuredSelection) {
				strucSelection = (IStructuredSelection) selection;

				Object selectedObj = strucSelection.getFirstElement();

				if (selectedObj instanceof IFolder || selectedObj instanceof IProject) {
					IFolder selectedFolder = null;
					String loc;
					if (selectedObj instanceof IProject) {
						IProject proj = (IProject) selectedObj;
						loc = proj.getLocation().toOSString();

					} else {
						selectedFolder = (IFolder) selectedObj;
						loc = selectedFolder.getLocation().toString();

					}
					String cleanedPath=loc.replace("\\","/");
					RServe.printJob("try(setwd(\"" + cleanedPath + "\"))");

				}

			}
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
