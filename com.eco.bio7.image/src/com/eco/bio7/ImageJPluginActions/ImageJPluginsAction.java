/*******************************************************************************
 * Copyright (c) 2004-2019 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.ImageJPluginActions;

import java.util.Hashtable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.image.Util;

public class ImageJPluginsAction extends Action implements IMenuCreator {

	private static Menu pluginsMenu;

	private static Hashtable<String, Menu> pluginsMenuTable = new Hashtable<String, Menu>();

	MenuItem[] plugins_;

	protected String currentMenu;

	protected String s;

	protected String na;

	protected String sTemp;

	private Util util;

	public ImageJPluginsAction() {
		setId("Plugins");
		setToolTipText("ImageJ");
		setText("Plugins");
		setMenuCreator(this);
		util = new Util();

	}

	public Menu getMenu(Control parent) {
		if (pluginsMenu != null) {
			pluginsMenu.dispose();
		}
		pluginsMenu = new Menu(parent);
		// Add main Menu to stack!

		pluginsMenu.addMenuListener(new MenuListener() {
			public void menuHidden(MenuEvent e) {

			}

			@Override
			public void menuShown(MenuEvent e) {

				MenuItem[] menuItems = pluginsMenu.getItems();
				// Only delete the plugins menu items and menus!
				for (int i = 0; i < menuItems.length; i++) {
					if (menuItems[i] != null) {
						menuItems[i].dispose();
					}
				}

				MenuItem menuItem = new MenuItem(pluginsMenu, SWT.PUSH);

				menuItem.setText("Compile Java (Eclipse)");

				menuItem.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
						if (editor == null || editor instanceof CompilationUnitEditor == false) {
							message("No ImageJ Java plugin file in Eclipse opened!");
							return;
						}

						IEditorInput editorInput = editor.getEditorInput();

						if (editorInput instanceof IFileEditorInput) {
							IFile ifile = ((IFileEditorInput) editorInput).getFile();

							String path = ifile.getRawLocation().toString();
							Display display = PlatformUI.getWorkbench().getDisplay();
							display.syncExec(new Runnable() {
								public void run() {
									new ij.plugin.Compiler().run(path);
								}
							});

						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});

				MenuItem menuItemPython = new MenuItem(pluginsMenu, SWT.PUSH);

				menuItemPython.setText("Interpret Script (Eclipse)");

				menuItemPython.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
						if (editor == null) {
							message("No file in Eclipse opened!");
							return;
						}

						IEditorInput editorInput = editor.getEditorInput();

						if (editorInput instanceof IFileEditorInput) {
							IFile ifile = ((IFileEditorInput) editorInput).getFile();
							String ext = ifile.getFileExtension();
							if (ext.equals("js") || ext.equals("py") || ext.equals("bsh") || ext.equals("ijm")||ext.equals("txt")) {
								String path = ifile.getRawLocation().toString();

								ImageJMacroRunnerWorkspaceJob job = new ImageJMacroRunnerWorkspaceJob(path);
								job.setName("IJMacro_Interpret_Job");
								job.addJobChangeListener(new JobChangeAdapter() {
									public void done(IJobChangeEvent event) {
										if (event.getResult().isOK()) {

										}
									}
								});

								job.schedule();

							}

							else {
								message("Not a script file!");
							}
						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});

				/* Add all plugins submenus! */

				new ImageJSubmenu().addSubMenus(pluginsMenu, "Plugins");

			}
		});

		return pluginsMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	/**
	 * Opens a message Dialog with the given text.
	 * 
	 * @param text the text for the dialog.
	 */
	public static void message(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(com.eco.bio7.image.Util.getShell(),

						SWT.ICON_WARNING);
				messageBox.setText("Info!");
				messageBox.setMessage(text);
				messageBox.open();
			}
		});
	}

}