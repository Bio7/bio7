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

package com.eco.bio7.rbridge;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.preferences.PreferenceConstants;

public class ScriptAction extends Action implements IMenuCreator {

	private Menu menu;

	protected int storecount;

	private IPreferenceStore store;

	protected MenuItem menuItem;

	protected File[] fil;

	

	public ScriptAction() {
		setId("GridScripts");
		setToolTipText("Scripts");
		setText("Scripts");

		setMenuCreator(this);

		store = Bio7Plugin.getDefault().getPreferenceStore();
	}

	public Menu getMenu(Control parent) {
		if (menu != null) {
			menu.dispose();
		}

		menu = new Menu(parent);
		menu.addMenuListener(new MenuListener() {
			public void menuHidden(MenuEvent e) {
				
			}

			public void menuShown(MenuEvent e) {
				MenuItem[] menuItems = menu.getItems();
				for (int i = 0; i < menuItems.length; i++) {
					if(menuItems[i]!=null){
					menuItems[i].dispose();
					}
				}
				File files = new File(store.getString(PreferenceConstants.D_GRID_SCRIPTS));
				fil = ListFilesDirectory(files, new String[] { ".r",".R",".bsh", ".groovy",".py" });
				MenuItem[] it=new MenuItem[fil.length];
				if (fil.length > 0) {
                  
					for (int i = 0; i < fil.length; i++) {
						/*Necessary final variable for selection listener!*/
						final int count = i;
						it[count] = new MenuItem(menu, SWT.CASCADE);
						it[count].setText(fil[count].getName().substring(0, fil[count].getName().lastIndexOf(".")));
						it[count].addSelectionListener(new SelectionListener() {

							public void widgetSelected(SelectionEvent e) {
								
								if (fil[count].getName().endsWith(".R") || fil[count].getName().endsWith(".r")) {
									if (RServe.isAliveDialog()) {
										if (RState.isBusy() == false) {
											RState.setBusy(true);
											final RInterpreterJob Do = new RInterpreterJob(null, true, fil[count].toString());
											Do.addJobChangeListener(new JobChangeAdapter() {
												public void done(IJobChangeEvent event) {
													if (event.getResult().isOK()) {
														int countDev = RServe.getDisplayNumber();
														RState.setBusy(false);
														if (countDev > 0) {
															RServe.closeAndDisplay();
														}
													}
												}
											});
											Do.setUser(true);
											Do.schedule();
										} else {

											Bio7Dialog.message("RServer is busy!");
										}

									}
								}
								
								else if (fil[count].getName().endsWith(".bsh")) {

									BeanShellInterpreter.interpretJob(null, fil[count].toString());

								} else if (fil[count].getName().endsWith(".groovy")) {

									GroovyInterpreter.interpretJob(null, fil[count].toString());

								}
								else if (fil[count].getName().endsWith(".py")) {

									PythonInterpreter.interpretJob(null, fil[count].toString());

								}

							}

							public void widgetDefaultSelected(SelectionEvent e) {

							}
						});

					}

				}

				else {
					new MenuItem(menu, SWT.NONE).setText("Empty");

				}

			}
		});

		return menu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	public static File[] ListFilesDirectory(File filedirectory, final String[] extensions) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of the file or directory inside Bio7.
				String filename = children[i];
			}
		}

		// Filter the extension of the file.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(extensions[0]) || name.endsWith(extensions[1])|| name.endsWith(extensions[2])|| name.endsWith(extensions[3])|| name.endsWith(extensions[4]));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}

}