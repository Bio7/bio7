package com.eco.bio7.actions;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.rbridge.RCompletionShell;

public class PrintExpression extends Action {

	private RCompletionShell shell = null;
	

	public PrintExpression(String text, IWorkbenchWindow window) {
		super(text);
		setId("com.eco.bio7.print_expression");
		setActionDefinitionId("com.eco.bio7.print_expression");
	}

	public void run() {

		

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		IViewReference ref = null;
		IWorkbenchPartSite part = null;
		try {
			part = page.showView("com.eco.bio7.RShell").getSite();
			ref = part.getPage().findViewReference("com.eco.bio7.RShell");
		} catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		EModelService s = (EModelService) part.getService(EModelService.class);

		MPartSashContainerElement p = (MPart) part.getService(MPart.class);
		if (p.getCurSharedRef() != null) {
			p = p.getCurSharedRef();

			s.detach(p, 400, 100, 420, 490);
		}

	}

	public Shell getShell() {
		return shell;
	}

}