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
import java.util.Stack;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.JavaScriptInterpreter;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.jobs.ImageMacroWorkspaceJob;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.util.Util;

public class ScriptAction extends Action implements IMenuCreator {

	private Menu menu;

	protected int storecount;

	private IPreferenceStore store;

	protected MenuItem menuItem;

	protected File[] fil;

	protected Stack<Menu> menuStack;

	

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

			@Override
			public void menuShown(MenuEvent e) {
				MenuItem[] menuItems = menu.getItems();
				// Only delete the extra script menu items!
				for (int i = 6; i < menuItems.length; i++) {
					if (menuItems[i] != null) {
						menuItems[i].dispose();
					}
				}
				/*Here we create the submenus with actions for the scripts recursively!*/
				menuStack = new Stack<Menu>();
				menuStack.push(menu);
				String loc = store.getString(PreferenceConstants.D_GRID_SCRIPTS);
				createSubMenus(loc);

			}
		});

		return menu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}
	
	 /*List files and folders recursively!*/
		public void createSubMenus(String directoryName) {
			File directory = new File(directoryName);
			File[] fList = directory.listFiles();
			for (File file : fList) {
				if (file.isFile()) {
					createScriptSubmenus(file);
				} else if (file.isDirectory()) {
					Menu men = menuStack.peek();
					MenuItem mntmRScripts = new MenuItem(men, SWT.CASCADE);
					mntmRScripts.setText(file.getName());
					Menu menuScripts = new Menu(mntmRScripts);
					mntmRScripts.setMenu(menuScripts);
					menuStack.push(menuScripts);
					createSubMenus(file.getAbsolutePath());
				}

			}
	       /*Leave the menu!*/
			menuStack.pop();

		}
	    /*Create a menu item and action for the different files!*/
		public void createScriptSubmenus(File file) {
			
			Menu submenu = menuStack.peek();

			MenuItem item = new MenuItem(submenu, SWT.NONE);

			item.setText(file.getName().substring(0, file.getName().lastIndexOf(".")));

			item.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {

					/*if (text.equals("Empty")) {
						System.out.println("No script available!");
					}*/

					 if (file.getName().endsWith(".R") || file.getName().endsWith(".r")) {
						if (RServe.isAliveDialog()) {
							if (RState.isBusy() == false) {
								RState.setBusy(true);
								final RInterpreterJob Do = new RInterpreterJob(null, true, file.toString());
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

								Bio7Dialog.message("Rserve is busy!");
							}

						}
					}
					 else if (file.getName().endsWith(".ijm")) {
						 ImageMacroWorkspaceJob job = new ImageMacroWorkspaceJob(file);

							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

									}
								}
							});

							job.schedule();
					 }

					else if (file.getName().endsWith(".bsh")) {

						BeanShellInterpreter.interpretJob(null, file.toString());

					} else if (file.getName().endsWith(".groovy")) {

						GroovyInterpreter.interpretJob(null, file.toString());

					} else if (file.getName().endsWith(".py")) {

						PythonInterpreter.interpretJob(null, file.toString());
					} else if (file.getName().endsWith(".js")) {

						JavaScriptInterpreter.interpretJob(null, file.toString());

					} else if (file.getName().endsWith(".java")) {

						Job job = new Job("Compile Java") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Compile Java...", IProgressMonitor.UNKNOWN);
								String name = file.getName().replaceFirst("[.][^.]+$", "");
								// IWorkspace workspace =
								// ResourcesPlugin.getWorkspace();
								IPath location = Path.fromOSString(file.getAbsolutePath());

								// IFile ifile =
								// workspace.getRoot().getFileForLocation(location);
								CompileClassAndMultipleClasses cp = new CompileClassAndMultipleClasses();
								try {
									cp.compileAndLoad(new File(location.toOSString()), new File(location.toOSString()).getParent(), name, null, true);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									// Bio7Dialog.message(e.getMessage());
								}

								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

								} else {

								}
							}
						});
						// job.setSystem(true);
						job.schedule();

					}
					
					else if (file.getName().endsWith(".txt")) {
						File fileToOpen = file;
						 
						if (fileToOpen.exists() && fileToOpen.isFile()) {
						    IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
						    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						 
						    try {
						        IDE.openEditorOnFileStore( page, fileStore );
						    } catch ( PartInitException ex ) {
						        //Put your exception handler here if you wish to
						    }
						} else {
						    //Do something if the file does not exist
						}
					}

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});

		}

	

}