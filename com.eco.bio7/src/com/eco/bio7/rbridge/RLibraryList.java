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

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RState;
import com.swtdesigner.SWTResourceManager;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;

public class RLibraryList extends Shell {

	private static List allPackagesList;

	public RLibraryList(Display display, int style) {
		super(display, style);
		
		setImage(SWTResourceManager.getImage(RLibraryList.class, "/pics/logo.gif"));
				
						allPackagesList = new List(this, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
						allPackagesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 4));
						
								allPackagesList.addMouseListener(new MouseAdapter() {
									public void mouseDoubleClick(final MouseEvent e) {
										if (RServe.isAlive()) {
											if (RState.isBusy() == false) {
												RState.setBusy(true);
												LoadRLibrarysJob Do = new LoadRLibrarysJob();
												Do.addJobChangeListener(new JobChangeAdapter() {
													public void done(IJobChangeEvent event) {
														if (event.getResult().isOK()) {
															RState.setBusy(false);
														} else {
															RState.setBusy(false);
														}
													}
												});
												Do.setUser(true);
												Do.schedule();
											} else {
						
												Bio7Dialog.message("Rserve is busy!");
						
											}
										} else {
											System.out.println("No Rserve connection available !");
										}
						
									}
								});
								allPackagesList.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(final SelectionEvent e) {

									}
								});
		
				final Button uninstallButton = new Button(this, SWT.NONE);
				GridData gd_uninstallButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
				gd_uninstallButton.heightHint = 35;
				uninstallButton.setLayoutData(gd_uninstallButton);
				uninstallButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						if (RServe.isAlive()) {
							if (RState.isBusy() == false) {
								RState.setBusy(true);
								RemoveRLibrarysJob Do = new RemoveRLibrarysJob();
								Do.addJobChangeListener(new JobChangeAdapter() {
									public void done(IJobChangeEvent event) {
										if (event.getResult().isOK()) {
											RState.setBusy(false);
										} else {
											RState.setBusy(false);
										}
									}
								});
								Do.setUser(true);
								Do.schedule();
							} else {

								Bio7Dialog.message("Rserve is busy!");

							}
						} else {
							System.out.println("No Rserve connection available !");
						}

					}
				});
				uninstallButton.setText("Remove");
				createContents();
	}

	/**
	 * Create contents of the window
	 */
	protected void createContents() {
		setText("Libraries");
		setSize(323, 743);
		setLayout(new GridLayout(2, true));
		RConnection c = RServe.getConnection();
		int b = 0;
		if (c != null) {

			try {
				c.eval("try(.bio7ListOfWebPackages <- list(sort(.packages(all.available = TRUE))))");

				c.eval(".bio7ListOfWebPackagesNames<-.bio7ListOfWebPackages[[1]]");
				try {
					b = (int) c.eval("length(.bio7ListOfWebPackagesNames)").asInteger();
				} catch (REXPMismatchException e1) {

					e1.printStackTrace();
				}
			} catch (RserveException e2) {

				e2.printStackTrace();
			}

			for (int i = 1; i < b; i++) {

				String st = null;

				try {
					st = (String) c.eval(".bio7ListOfWebPackagesNames[" + i + "]").asString();
				} catch (REXPMismatchException e1) {

					e1.printStackTrace();
				} catch (RserveException e1) {

					e1.printStackTrace();
				}

				allPackagesList.add(st);

			}

		} else {
			System.out.println("No Rserve connection available !");
		}

		//
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public static List getAllList() {
		return allPackagesList;
	}

	public static List getAllPackagesList() {
		return allPackagesList;
	}

}
