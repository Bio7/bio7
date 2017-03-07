package com.eco.bio7.r.packages;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class ReleasePackageAction implements IObjectActionDelegate {

	public ReleasePackageAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {

		new ExecuteDevtoolsCommand("release()");
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
