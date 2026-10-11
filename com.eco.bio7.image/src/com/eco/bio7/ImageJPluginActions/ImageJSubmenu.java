/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.ImageJPluginActions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.eco.bio7.image.ui.IJTabs;

import ij.Menus;

/**
 * Builds the Bio7 ImageJ menus as a copy of the SWT menu bar of the (hidden)
 * ImageJ shell. A selection of a copied menu item is forwarded to the original
 * ImageJ menu item so that ImageJ executes the command.
 *
 * @author M. Austenfeld
 */
public class ImageJSubmenu {

	/**
	 * Adds the items of the ImageJ menu with the given label to the given menu.
	 *
	 * @param mainMenu the target menu.
	 * @param label    the label of the ImageJ main menu (e.g., "File").
	 */
	public void addSubMenus(Menu mainMenu, String label) {
		Menu menuBar = Menus.getMenuBar();
		if (menuBar == null || menuBar.isDisposed()) {
			return;
		}
		for (MenuItem item : menuBar.getItems()) {
			if (stripMnemonic(item.getText()).equals(label) && item.getMenu() != null) {
				copyMenu(item.getMenu(), mainMenu, label);
			}
		}
	}

	private static String stripMnemonic(String text) {
		return text.replace("&", "");
	}

	private void copyMenu(Menu source, Menu target, String mainMenuLabel) {
		for (MenuItem sourceItem : source.getItems()) {
			int style = sourceItem.getStyle();
			String text = sourceItem.getText();
			String label = stripMnemonic(text).split("\t")[0];
			if ((style & SWT.SEPARATOR) != 0) {
				new MenuItem(target, SWT.SEPARATOR);
			} else if ((style & SWT.CASCADE) != 0) {
				MenuItem cascade = new MenuItem(target, SWT.CASCADE);
				cascade.setText(text);
				Menu subMenu = new Menu(target);
				cascade.setMenu(subMenu);
				if (sourceItem.getMenu() != null) {
					copyMenu(sourceItem.getMenu(), subMenu, mainMenuLabel);
				}
			} else if (isExcluded(label)) {
				/* Not useful in Bio7 (the main window is hidden)! */
			} else if (label.equals("Close All") && mainMenuLabel.equals("File")) {
				MenuItem it = new MenuItem(target, SWT.PUSH);
				it.setText(text);
				it.addListener(SWT.Selection, e -> IJTabs.deleteAllTabs());
			} else {
				int copyStyle = (style & SWT.CHECK) != 0 ? SWT.CHECK : (style & SWT.RADIO) != 0 ? SWT.RADIO : SWT.PUSH;
				MenuItem it = new MenuItem(target, copyStyle);
				it.setText(text);
				it.setEnabled(sourceItem.getEnabled());
				if (copyStyle != SWT.PUSH) {
					it.setSelection(sourceItem.getSelection());
				}
				it.addListener(SWT.Selection, e -> forward(sourceItem, it, e));
			}
		}
	}

	/* Executes the command of the original ImageJ menu item! */
	private static void forward(MenuItem sourceItem, MenuItem copy, Event e) {
		if (sourceItem.isDisposed()) {
			return;
		}
		if ((copy.getStyle() & (SWT.CHECK | SWT.RADIO)) != 0) {
			sourceItem.setSelection(copy.getSelection());
		}
		Event event = new Event();
		event.stateMask = e.stateMask;
		sourceItem.notifyListeners(SWT.Selection, event);
	}

	private static boolean isExcluded(String label) {
		return label.equals("Quit") || label.equals("Update ImageJ...") || label.equals("Show All") || label.startsWith("Main Window [") || label.startsWith("Put Behind [tab]") || label.startsWith("Cascade")
				|| label.startsWith("Tile");
	}
}
