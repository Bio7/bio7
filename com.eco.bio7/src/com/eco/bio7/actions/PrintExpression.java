package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPage;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.rbridge.RCompletionShell;
import com.eco.bio7.rbridge.RServe;

public class PrintExpression extends Action {

	private final IWorkbenchWindow window;

	private RCompletionShell shell = null;

	public PrintExpression(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.print_expression");
		setActionDefinitionId("com.eco.bio7.print_expression");
	}

	public void run() {

		RConnection d = RServe.getConnection();

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IViewReference ref = page.showView("com.eco.bio7.RShell").getSite().getPage().findViewReference("com.eco.bio7.RShell");

		//	((WorkbenchPage) page.showView("com.eco.bio7.RShell").getSite().getPage()).getActivePerspective().getPresentation().detachPart(ref);

			ref.getView(false).getViewSite().getShell().setSize(380, 450);
			ref.getView(false).getViewSite().getShell().setLocation(300, 50);
			 

		} catch (PartInitException e1) {

			e1.printStackTrace();
		}

	}

	public Shell getShell() {
		return shell;
	}

}