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

import java.awt.CheckboxMenuItem;
import java.awt.MenuBar;
import java.util.Stack;

import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.eco.bio7.image.IJTabs;

import ij.IJ;
import ij.ImagePlus;
import ij.Menus;
import ij.Prefs;
import ij.WindowManager;
import ij.util.Tools;

/**
 * @author M. Austenfeld A class to built the ImageJ plugin SWT menus.
 */
public class ImageJSubmenu {

	private Stack<Menu> mainMenuStack = new Stack<Menu>();// Just for variable
	private MenuItem menuBit;

	public void addSubMenus(Menu mainMenu, String label) {
		mainMenuStack.push(mainMenu);
		MenuBar menuBar = Menus.getMenuBar();
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			java.awt.Menu menu = menuBar.getMenu(i);

			if (menu.getLabel().equals(label))

				recurseSubMenu(menu, mainMenu, label);
		}
	}

	private void recurseSubMenu(java.awt.Menu menu, Menu mainMenu, String mainMenuLabel) {
		int items = menu.getItemCount();
		if (items == 0)
			return;
		for (int i = 0; i < items; i++) {
			java.awt.MenuItem mItem = menu.getItem(i);
			String label = mItem.getActionCommand();

			if (mItem instanceof java.awt.Menu) {
				Menu menuSub = new Menu(mainMenu);
				MenuItem menuItem_stacks = new MenuItem(mainMenu, SWT.CASCADE);
				menuItem_stacks.setMenu(menuSub);
				menuItem_stacks.setText(mItem.getLabel());

				/* Push Menu on the stack! */
				mainMenuStack.push(menuSub);

				recurseSubMenu((java.awt.Menu) mItem, menuSub, mainMenuLabel);

				mainMenuStack.pop();

			} else if (mItem instanceof java.awt.MenuItem) {

				Menu currentSubMenu = mainMenuStack.peek();

				if (mItem instanceof CheckboxMenuItem) {
					if (mainMenuLabel.equals("Window")) {
						/* Don't add images to the menu. We have already a list in the tabs menu! */
						String idString = mItem.getActionCommand();
						int id = (int) Tools.parseDouble(idString, 0);
						ImagePlus imp = WindowManager.getImage(id);
						if (imp != null) {
							break;
						}
					}

					MenuItem it = new MenuItem(currentSubMenu, SWT.CHECK);
					it.setSelection(false);
					if ("Autorun Examples".equals(mItem.getLabel())) {
						if (Prefs.autoRunExamples) {

							it.setSelection(true);

						} else {

							it.setSelection(false);
						}
					}

					String lab = mItem.getLabel();

					it.setText(lab);
					it.addSelectionListener(new SelectionListener() {

						public void widgetSelected(SelectionEvent e) {
							String cmd = mItem.getActionCommand();

							if ("Autorun Examples".equals(cmd)) { // Examples>Autorun Examples

								if (Prefs.autoRunExamples) {
									Prefs.autoRunExamples = false;
									it.setSelection(false);

								} else {
									Prefs.autoRunExamples = true;
									it.setSelection(true);
								}
								

							}

							else if (mainMenuLabel.equals("Window")) {

								/* Add the opened dialogs to the menu! */
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										WindowManager.activateWindow(cmd, mItem);
									}
								});

							}

							else {

								IJ.doCommand(cmd);
							}

						}

						public void widgetDefaultSelected(SelectionEvent e) {

						}
					});
					if (label.equals("8-bit") || label.equals("16-bit") || label.equals("32-bit") || label.equals("8-bit Color") ||
							label.equals("RGB Color") || label.equals("RGB Stack") || label.equals("HSB Stack") || label.equals("Lab Stack")) {
						ImagePlus imp = WindowManager.getCurrentImage();
						if (imp != null) {

							if (IJ.getImage().getType() == 0 && label.equals("8-bit")) {

								it.setSelection(true);
								menuBit = it;

							} else if (IJ.getImage().getType() == 1 && label.equals("16-bit")) {
								it.setSelection(true);
								menuBit = it;
							} else if (IJ.getImage().getType() == 2 && label.equals("32-bit")) {
								it.setSelection(true);
								menuBit = it;
							} else if (IJ.getImage().getType() == 3 && label.equals("8-bit Color")) {
								it.setSelection(true);
								menuBit = it;
							}

							else if (IJ.getImage().getType() == 4 && label.equals("RGB Color")) {
								it.setSelection(true);
								menuBit = it;
							} else if (imp.getStack().isRGB() && label.equals("RGB Stack")) {
								it.setSelection(true);
								menuBit.setSelection(false);
							} else if (imp.getStack().isHSB() && label.equals("HSB Stack")) {
								it.setSelection(true);
								menuBit.setSelection(false);
							} else if (imp.getStack().isLab() && label.equals("Lab Stack")) {
								it.setSelection(true);
								menuBit.setSelection(false);
							}

							else {
								it.setSelection(false);
							}
						}
					}

				}

				else {
					/* If we have a menu seperator! */
					if (label.equals("-")) {
						new MenuItem(currentSubMenu, SWT.SEPARATOR);
						/* If we have a menu item! */
					} else if (!(label.equals("-"))) {

						/* Don't show unused actions! */
						String labelMenuItem = mItem.getLabel();

						/*
						 * if (labelMenuItem.equals("Quit") || labelMenuItem.equals("Update ImageJ..."))
						 * {
						 * 
						 * MenuItem it = new MenuItem(currentSubMenu, SWT.NONE); String lab =
						 * mItem.getLabel();
						 * 
						 * it.setText(lab);
						 * 
						 * it.addSelectionListener(new SelectionListener() {
						 * 
						 * public void widgetSelected(SelectionEvent e) {
						 * IJ.showMessage("Disabled for the Eclipse ImageJ plugin!");
						 * 
						 * }
						 * 
						 * public void widgetDefaultSelected(SelectionEvent e) {
						 * 
						 * } });
						 * 
						 * }
						 */
						/* Exclude the following menu items! */
						if (labelMenuItem.equals("Quit") || labelMenuItem.equals("Update ImageJ...") || labelMenuItem.equals("Show All") || labelMenuItem.startsWith("Main Window [") || labelMenuItem.startsWith("Put Behind [tab]") || labelMenuItem.startsWith("Cascade")
								|| labelMenuItem.startsWith("Tile")) {

						}

						else if (labelMenuItem.equals("Close All")) {
							MenuItem it = new MenuItem(currentSubMenu, SWT.NONE);
							String lab = mItem.getLabel();

							it.setText(lab);

							it.addSelectionListener(new SelectionListener() {

								public void widgetSelected(SelectionEvent e) {
									/* Close the tabs! */
									IJTabs.deleteAllTabs();

								}

								public void widgetDefaultSelected(SelectionEvent e) {

								}
							});

						}
						/*
						 * else if(labelMenuItem.equals("Open...")){ MenuItem it = new
						 * MenuItem(currentSubMenu, SWT.PUSH); String lab = mItem.getLabel(); if
						 * (mItem.getShortcut() != null) { //System.out.println(mItem.getShortcut() +
						 * "  :  " + mItem.getShortcut().getKey());
						 * it.setAccelerator(mItem.getShortcut().getKey());
						 * it.setText(lab+"\t"+mItem.getShortcut()); } else{ it.setText(lab); }
						 * it.addSelectionListener(new SelectionListener() {
						 * 
						 * 
						 * 
						 * public void widgetSelected(SelectionEvent e) { SwingUtilities.invokeLater(new
						 * Runnable() { public void run() {
						 * 
						 * IJ.getInstance().doCommand("Open..."); } });
						 * 
						 * }
						 * 
						 * public void widgetDefaultSelected(SelectionEvent e) {
						 * 
						 * } }); }
						 */

						else {

							MenuItem it = new MenuItem(currentSubMenu, SWT.NONE);
							String lab = mItem.getLabel();
							/* Create shortcuts if available! */
							if (mItem.getShortcut() != null) {
								// System.out.println(mItem.getShortcut() + " : " +
								// mItem.getShortcut().getKey());
								it.setAccelerator(mItem.getShortcut().getKey());
								it.setText(lab + "\t" + mItem.getShortcut());
							} else {
								it.setText(lab);
							}

							it.addSelectionListener(new SelectionListener() {

								public void widgetSelected(SelectionEvent e) {
									String cmd = mItem.getActionCommand();
									IJ.doCommand(cmd);

								}

								public void widgetDefaultSelected(SelectionEvent e) {

								}
							});
						}
					}

				}

			}

		}

	}

}
