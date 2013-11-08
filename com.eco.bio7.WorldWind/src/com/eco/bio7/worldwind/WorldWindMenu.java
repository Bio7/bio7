/*
Nasa WorldWind plugin
Copyright (C) 2008  M. Austenfeld

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.*/
package com.eco.bio7.worldwind;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class WorldWindMenu extends Shell {

	/**
	 * Launch the application
	 * @param args
	 */
	public static void main(String args[]) {
		try {
			Display display = Display.getDefault();
			WorldWindMenu shell = new WorldWindMenu(display, SWT.SHELL_TRIM);
			shell.open();
			shell.layout();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the shell
	 * @param display
	 * @param style
	 */
	public WorldWindMenu(Display display, int style) {
		super(display, style);
		createContents();
	}

	/**
	 * Create contents of the window
	 */
	protected void createContents() {
		setText("SWT Application");
		setSize(500, 375);

		final Menu menu = new Menu(this, SWT.BAR);
		setMenuBar(menu);

		final MenuItem newSubmenuMenuItem = new MenuItem(menu, SWT.CASCADE);
		newSubmenuMenuItem.setText("New SubMenu");

		final Menu menu_1 = new Menu(newSubmenuMenuItem);
		newSubmenuMenuItem.setMenu(menu_1);

		final MenuItem newItemMenuItem = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem.setText("New Item");

		final MenuItem newSubmenuMenuItem_1 = new MenuItem(menu_1, SWT.CASCADE);
		newSubmenuMenuItem_1.setText("New SubMenu");

		final Menu menu_2 = new Menu(newSubmenuMenuItem_1);
		newSubmenuMenuItem_1.setMenu(menu_2);

		final MenuItem newItemMenuItem_6 = new MenuItem(menu_2, SWT.NONE);
		newItemMenuItem_6.setText("New Item");

		final MenuItem newItemMenuItem_7 = new MenuItem(menu_2, SWT.NONE);
		newItemMenuItem_7.setText("New Item");

		final MenuItem newItemMenuItem_2 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_2.setText("New Item");

		final MenuItem newItemMenuItem_1 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_1.setText("New Item");

		final MenuItem newItemMenuItem_3 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_3.setText("New Item");

		final MenuItem newItemMenuItem_4 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_4.setText("New Item");

		new MenuItem(menu_1, SWT.SEPARATOR);

		final MenuItem newItemMenuItem_5 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_5.setText("New Item");
		//
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}
