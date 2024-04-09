package com.eco.bio7.actions;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.WorkbenchWindow;

import com.eco.bio7.util.Util;

public class HideMainMenusAndFullscreen extends Action {

	protected final IWorkbenchWindow window;
	protected boolean hiddenMenu = false;

	public HideMainMenusAndFullscreen(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.hide.main.menus.fullscreen");
		setActionDefinitionId("com.eco.bio7.hide.main.menus.fullscreen");

	}

	public void run() {

		String fullscreenId = "org.eclipse.ui.window.fullscreenmode";
		String hideMainMenuId = "com.eco.bio7.hide.main.menus";
		String showMainMenuId = "com.eco.bio7.show.main.menus";
		IHandlerService handlerService = (IHandlerService) (IHandlerService) PlatformUI.getWorkbench()
				.getService(IHandlerService.class);
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					handlerService.executeCommand(fullscreenId, null);
					/* Exclude MacOSX which has automatically hides the menu! */
					if (Util.getOS().equals("Mac") == false) {
						if (hiddenMenu == false) {
							WorkbenchWindow workbenchWin = (WorkbenchWindow) PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow();
							MenuManager menuManager = workbenchWin.getMenuManager();
							IContributionItem[] items = menuManager.getItems();

							for (IContributionItem item : items) {

								item.setVisible(false);

							}
							menuManager.update(true);
							hiddenMenu = true;
						} else {
							WorkbenchWindow workbenchWin = (WorkbenchWindow) PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow();
							MenuManager menuManager = workbenchWin.getMenuManager();
							IContributionItem[] items = menuManager.getItems();

							for (IContributionItem item : items) {

								item.setVisible(true);

							}
							menuManager.update(true);
							hiddenMenu = false;
						}
					}
				} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

}