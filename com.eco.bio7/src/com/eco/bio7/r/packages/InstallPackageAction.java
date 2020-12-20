package com.eco.bio7.r.packages;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class InstallPackageAction implements IObjectActionDelegate {

	public InstallPackageAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {

		new ExecuteDevtoolsCommand("install()");

	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
