/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.util;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * Removes the launch submenus of the Eclipse debug plugin (Run As, Debug As,
 * Profile As) from the context menu of a Bio7 editor. They are contributed for
 * the file of the editor, but there are no launch configurations for R,
 * Python, ImageJ macro, Markdown or script files (the editors have their own
 * actions to run and debug the code).
 */
public final class LaunchMenus {

	/* The ids of the submenus (org.eclipse.debug.ui: contextualLaunch.run.submenu, .debug.submenu, .profile.submenu) */
	private static final String PREFIX = "org.eclipse.debug.ui.contextualLaunch.";

	private LaunchMenus() {
	}

	/**
	 * Removes the launch submenus from the context menu of the control (e.g., the
	 * text widget of an editor). Call it after the context menu of the editor has
	 * been created and registered (after createPartControl of the editor), so
	 * that the submenus are removed after they have been contributed.
	 */
	public static void removeFrom(Control control) {
		if (control == null || control.isDisposed()) {
			return;
		}
		Menu menu = control.getMenu();
		if (menu != null && menu.getData(MenuManager.MANAGER_KEY) instanceof MenuManager manager) {
			manager.addMenuListener(LaunchMenus::remove);
		}
	}

	private static void remove(IMenuManager manager) {
		for (IContributionItem item : manager.getItems()) {
			String id = item.getId();
			if (id != null && id.startsWith(PREFIX)) {
				manager.remove(item);
			}
		}
	}
}
