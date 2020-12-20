package com.eco.bio7.r.packages;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class DocumentPackageAction implements IObjectActionDelegate {

	public String packageName;
	public String[] includeRVariables;
	public boolean builtFromVariables;
	public boolean cancelCreation = false;

	public DocumentPackageAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {

		new ExecuteDevtoolsCommand("document()");
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
