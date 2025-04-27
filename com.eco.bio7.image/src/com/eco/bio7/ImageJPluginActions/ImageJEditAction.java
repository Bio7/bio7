/*******************************************************************************
 * Copyright (c) 2005-2016 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.ImageJPluginActions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import ij.gui.Toolbar;

public class ImageJEditAction extends Action implements IMenuCreator {

	private Menu fMenu;

	public ImageJEditAction() {
		setId("Edit");
		setToolTipText("ImageJ");
		setText("Edit");
		setMenuCreator(this);
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		fMenu.addMenuListener(new MenuListener() {
			public void menuHidden(MenuEvent e) {

			}

			@Override
			public void menuShown(MenuEvent e) {

				MenuItem[] menuItems = fMenu.getItems();
				// Only delete the plugins menu items and menus!
				for (int i = 0; i < menuItems.length; i++) {
					if (menuItems[i] != null) {
						menuItems[i].dispose();
					}
				}
				new ImageJSubmenu().addSubMenus(fMenu, "Edit");

				new MenuItem(fMenu, SWT.SEPARATOR);

				MenuItem menuItemColFor = new MenuItem(fMenu, SWT.PUSH);

				menuItemColFor.setText("Colour Foreground");

				menuItemColFor.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						Shell shell = new Shell();
						ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);
						// dialog.setRGB(rgb);
						dialog.setText("Foreground Colour");
						RGB color = dialog.open();
						if (color != null) {

							Toolbar.setForegroundColor(new java.awt.Color(color.red, color.green, color.blue));

						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});

				MenuItem menuItemColBack = new MenuItem(fMenu, SWT.PUSH);

				menuItemColBack.setText("Colour Background");
				menuItemColBack.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						Shell shell = new Shell();
						ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);
						// dialog.setRGB(rgb);
						dialog.setText("Background Colour");
						RGB color = dialog.open();
						if (color != null) {

							Toolbar.setBackgroundColor(new java.awt.Color(color.red, color.green, color.blue));

						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});

			}
		});

		return fMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

}