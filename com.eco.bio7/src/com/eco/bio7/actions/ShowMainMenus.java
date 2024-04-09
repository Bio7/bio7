package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;

public class ShowMainMenus extends Action {

	protected final IWorkbenchWindow window;

	public ShowMainMenus(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.show.main.menus");
		setActionDefinitionId("com.eco.bio7.show.main.menus");

	}

	public void run() {
		WorkbenchWindow workbenchWin = (WorkbenchWindow) PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		MenuManager menuManager = workbenchWin.getMenuManager();
		IContributionItem[] items = menuManager.getItems();

		for (IContributionItem item : items) {

			item.setVisible(true);

		}
		menuManager.update(true);

	}

}