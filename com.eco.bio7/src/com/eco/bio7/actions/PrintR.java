package com.eco.bio7.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPage;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.rbridge.RServe;

public class PrintR implements IEditorActionDelegate {

	public void dispose() {

	}

	public void init(IWorkbenchWindow window) {

	}

	public void run(IAction action) {

		RConnection d = RServe.getConnection();

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IViewReference ref = page.showView("com.eco.bio7.RShell").getSite().getPage().findViewReference("com.eco.bio7.RShell");

			//((WorkbenchPage) page.showView("com.eco.bio7.RShell").getSite().getPage()).getActivePerspective().getPresentation().detachPart(ref);

			ref.getView(false).getViewSite().getShell().setSize(380, 450);
			ref.getView(false).getViewSite().getShell().setLocation(300, 50);
			 

		} catch (PartInitException e1) {

			e1.printStackTrace();
		}

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {

	}

	public void selectionChanged(IAction action, ISelection selection) {

	}
}
