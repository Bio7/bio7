package com.eco.bio7.actions;

/*import org.eclipse.jface.action.Action;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPage;

public class NutrientAction extends Action {

	private final IWorkbenchWindow window;

	public NutrientAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.soil_action");

		setImageDescriptor(com.eco.bio7.Bio7Plugin
				.getImageDescriptor("/icons/bio7new.png"));
	}

	public void run() {
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			IViewReference ref = page.showView("com.eco.bio7.soil_parameter")
					.getSite().getPage().findViewReference(
							"com.eco.bio7.soil_parameter");

			((WorkbenchPage) page.showView("com.eco.bio7.soil_parameter")
					.getSite().getPage()).getActivePerspective()
					.getPresentation().detachPart(ref);

			ref.getView(false).getViewSite().getShell().setSize(205, 240);
			ref.getView(false).getViewSite().getShell().setLocation(300, 200);
			// 

		} catch (PartInitException e1) {

			e1.printStackTrace();
		}

	}

}*/