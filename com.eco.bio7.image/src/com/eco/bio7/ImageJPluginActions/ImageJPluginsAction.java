/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.ImageJPluginActions;

import ij.IJ;
import ij.Menus;
import java.util.Hashtable;
import javax.swing.SwingUtilities;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ImageJPluginsAction extends Action implements IMenuCreator {

	private static Menu pluginsMenu;

	String[] themacros = { "Install...", "Run...", "Startup Macros...", "Record..."};

	String[] theshortcuts = { "List Shortcuts", "Create Shortcut", "Add Shortcut by Name... ", "Remove Shortcut..." };

	String[] theutilities = { "Control Panel...", "ImageJ Properties", "Find Commands...", "Threads", "Benchmark", "Reset...", "Monitor Memory...","Monitor Events...", "Search...", "Capture Screen",
			 };//"Capture Image"

	String[] thenew = { "Macro","Macro Tool", "JavaScript", "Plugin", "Plugin Filter", "Plugin Frame","Plugin Tool", "Text Window...", "Table..." };

	String[] plugins = Menus.getPlugins();

	MenuItem[] macros_ = new MenuItem[themacros.length];

	MenuItem[] shortcuts_ = new MenuItem[theshortcuts.length];

	MenuItem[] utilities_ = new MenuItem[theutilities.length];

	MenuItem[] new_ = new MenuItem[thenew.length];

	/* Stores all created menus! If menu is not on the list it will be created! */

	private static Hashtable<String, Menu> pluginsMenuTable = new Hashtable<String, Menu>();

	MenuItem[] plugins_;

	private Menu fMenu9;

	protected String currentMenu;

	protected String s;

	protected String na;

	protected String sTemp;

	public ImageJPluginsAction() {
		setId("Plugins");
		setToolTipText("ImageJ");
		setText("Plugins");

		setMenuCreator(this);

	}

	public static Menu getPluginsMenu() {
		return pluginsMenu;
	}

	public static Hashtable<String, Menu> getPluginsMenuTable() {
		return pluginsMenuTable;
	}

	public static void setPluginsMenuTable(Hashtable<String, Menu> pluginsMenuTable) {
		ImageJPluginsAction.pluginsMenuTable = pluginsMenuTable;
	}

	public Menu getMenu(Control parent) {
		if (pluginsMenu != null) {
			pluginsMenu.dispose();
		}
		pluginsMenu = new Menu(parent);

		Menu fMenu6 = new Menu(pluginsMenu);
		MenuItem menuItem_stacks = new MenuItem(pluginsMenu, SWT.CASCADE);
		menuItem_stacks.setMenu(fMenu6);
		menuItem_stacks.setText("Macros");

		for (int i = 0; i < themacros.length; i++) {
			final int count = i;
			macros_[i] = new MenuItem(fMenu6, SWT.PUSH);
			macros_[i].setText(themacros[i]);
			macros_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(themacros[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenu7 = new Menu(pluginsMenu);
		MenuItem menuItem_zoom = new MenuItem(pluginsMenu, SWT.CASCADE);
		menuItem_zoom.setMenu(fMenu7);
		menuItem_zoom.setText("Shortcuts");

		for (int i = 0; i < theshortcuts.length; i++) {
			final int count = i;
			shortcuts_[i] = new MenuItem(fMenu7, SWT.PUSH);
			shortcuts_[i].setText(theshortcuts[i]);
			shortcuts_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(theshortcuts[count]);
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
		}

		Menu fMenu8 = new Menu(pluginsMenu);
		MenuItem menuItem_lookuptables = new MenuItem(pluginsMenu, SWT.CASCADE);
		menuItem_lookuptables.setMenu(fMenu8);
		menuItem_lookuptables.setText("Utilities");

		for (int i = 0; i < theutilities.length; i++) {
			final int count = i;
			utilities_[i] = new MenuItem(fMenu8, SWT.PUSH);
			utilities_[i].setText(theutilities[i]);
			utilities_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(theutilities[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenu10 = new Menu(pluginsMenu);
		MenuItem menuItem_new = new MenuItem(pluginsMenu, SWT.CASCADE);
		menuItem_new.setMenu(fMenu10);
		menuItem_new.setText("New");

		for (int i = 0; i < thenew.length; i++) {
			final int count = i;
			new_[i] = new MenuItem(fMenu10, SWT.PUSH);
			new_[i].setText(thenew[i]);
			new_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thenew[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		MenuItem menuItem2 = new MenuItem(pluginsMenu, SWT.PUSH);
		menuItem2.setText("Edit...");

		MenuItem menuItem21 = new MenuItem(pluginsMenu, SWT.PUSH);
		menuItem21.setText("Compile and Run...");

		menuItem2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Edit...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem21.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Compile and Run...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		pluginsMenu.addMenuListener(new MenuListener() {
			public void menuHidden(MenuEvent e) {

			}

			@Override
			public void menuShown(MenuEvent e) {
				pluginsMenuTable.clear();// name of the submenus
				plugins_ = new MenuItem[Menus.getPlugins().length];
				MenuItem[] menuItems = pluginsMenu.getItems();
				// Only delete the plugins menu items and menus!
				for (int i = 6; i < menuItems.length; i++) {
					if (menuItems[i] != null) {
						menuItems[i].dispose();
					}
				}
				/* Add plugins from *.class files! */
				classPlugins();
				/* Add plugins from *.jar files! */
				addJarPlugins();

			}
		});

		return pluginsMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	private void classPlugins() {
		/* Add the *.class files to the menu! */
		for (int i = 0; i < plugins.length; i++) {

			int slashIndex = plugins[i].indexOf('/');
			String menuName = slashIndex < 0 ? "Plugins" : "Plugins>" + plugins[i].substring(0, slashIndex).replace('/', '>');

			String command = plugins[i];
			if (slashIndex > 0) {
				command = plugins[i].substring(slashIndex + 1);
			}
			command = command.replace('_', ' ');
			command.trim();

			final String co = command;

			plugins_[i] = new MenuItem(pluginsMenu, SWT.PUSH);

			plugins_[i].setText(co);

			plugins_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {

							IJ.doCommand(co);
						}
					});
				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});

		}
	}

	private void addJarPlugins() {
		/* Add the *.jars to the menu */

		for (int j = 0; j < Menus.bio7JarAllCommand.size(); j++) {
			final int count = j;
			s = Menus.bio7JarAllCommand.get(j);
           //System.out.println(s);
			currentMenu = null;
			if (s.startsWith("Plugins>")) {
				int firstComma = s.indexOf(',');
				if (firstComma == -1 || firstComma <= 8) {

				}

				else {
					String name = s.substring(8, firstComma);

					createSubmenuEntry(name);
					currentMenu = name;
				}
			}

			else if (s.startsWith("File>")) {

				int firstComma = s.indexOf(',');
				if (firstComma == -1 || firstComma <= 5) {

				}

				else {
					String name = s.substring(5, firstComma);

					createSubmenuEntry(name);
					currentMenu = name;
				}
			} else if (s.startsWith("Edit>")) {

				int firstComma = s.indexOf(',');
				if (firstComma == -1 || firstComma <= 5) {

				}

				else {
					String name = s.substring(5, firstComma);

					createSubmenuEntry(name);
					currentMenu = name;
				}
			} else if (s.startsWith("Image>")) {

				int firstComma = s.indexOf(',');
				if (firstComma == -1 || firstComma <= 6) {

				}

				else {
					String name = s.substring(6, firstComma);

					createSubmenuEntry(name);
					currentMenu = name;
				}
			} else if (s.startsWith("Process>")) {// if with submenu e.g.

				int firstComma = s.indexOf(',');
				if (firstComma == -1 || firstComma <= 8) {

				}

				else {
					String name = s.substring(8, firstComma);

					createSubmenuEntry(name);
					currentMenu = name;
				}
			}

			else if (s.startsWith("Analyze>")) {

				int firstComma = s.indexOf(',');
				if (firstComma == -1 || firstComma <= 8) {

				}

				else {
					String name = s.substring(8, firstComma);

					createSubmenuEntry(name);
					currentMenu = name;
				}
			} else if (s.startsWith("\"") || s.startsWith("Plugins")) {

				currentMenu = null;
			}

			else {
				int firstQuote = s.indexOf('"');
				String name = firstQuote < 0 ? s : s.substring(0, firstQuote).trim();
				currentMenu = name;
				int comma = name.indexOf(',');
				if (comma >= 0)
					name = name.substring(0, comma);
				currentMenu = name;

				if (name.startsWith("Help>")) {

					name = "Help>About Plugins";

					if (pluginsMenuTable.containsKey(name) == false) {
						Menu newMenu = new Menu(pluginsMenu);

						MenuItem it = new MenuItem(pluginsMenu, SWT.CASCADE);
						it.setMenu(newMenu);
						it.setText(name);
						pluginsMenuTable.put(name, newMenu);
						currentMenu = name;

						pluginsMenuTable.put("Help>About Plugins", newMenu);
					}

				} else {
					currentMenu = name;

				}

			}
			int firstQuote = s.indexOf('"');
			if (firstQuote == -1)
				return;
			s = s.substring(firstQuote, s.length()); // remove menu

			String menuEntry = s;

			if (s.startsWith("\"")) {

				int quote = s.indexOf('"', 1);
				menuEntry = quote < 0 ? s.substring(1) : s.substring(1, quote);
				final String ent = menuEntry;
				if (currentMenu != null) {

					/*
					 * Get the specified submenu from the hashtable and add menu
					 * items!
					 */
					Menu men = (Menu) pluginsMenuTable.get(currentMenu);
					if (men != null) {

						MenuItem it = new MenuItem(men, SWT.CASCADE);
						it.setText(menuEntry);
						it.addSelectionListener(new SelectionListener() {

							public void widgetSelected(SelectionEvent e) {

								IJ.doCommand(ent);
							}

							public void widgetDefaultSelected(SelectionEvent e) {

							}
						});
					}

				} else {

					/* The plugins general menu! */
					MenuItem it = new MenuItem(pluginsMenu, SWT.PUSH);
					it.setText(menuEntry);

					it.addSelectionListener(new SelectionListener() {

						public void widgetSelected(SelectionEvent e) {

							IJ.doCommand(ent);

						}

						public void widgetDefaultSelected(SelectionEvent e) {

						}
					});

				}

			} else {
				int comma = s.indexOf(',');
				if (comma > 0)
					menuEntry = s.substring(0, comma);

			}
		}
	}

	private void createSubmenuEntry(String name) {
		if (pluginsMenuTable.containsKey(name) == false) {
			Menu newMenu = new Menu(pluginsMenu);

			MenuItem it = new MenuItem(pluginsMenu, SWT.CASCADE);
			it.setMenu(newMenu);
			it.setText(name);
			currentMenu = name;
			pluginsMenuTable.put(name, newMenu);
		}
	}

}