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
import com.eco.bio7.r.RState;
import com.swtdesigner.SWTResourceManager;

public class RLibraryList extends Shell {

	private static List allPackagesList;

	public RLibraryList(Display display, int style) {
		super(display, style);
		createContents();
		setImage(SWTResourceManager.getImage(RLibraryList.class, "/pics/logo.gif"));
		setLayout(new FormLayout());

	}

	/**
	 * Create contents of the window
	 */
	protected void createContents() {
		setText("Libraries");
		setSize(221, 586);

		allPackagesList = new List(this, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		final FormData fd_allPackagesList = new FormData();
		fd_allPackagesList.right = new FormAttachment(100, 0);
		fd_allPackagesList.bottom = new FormAttachment(100, -32);
		fd_allPackagesList.top = new FormAttachment(0, 0);
		fd_allPackagesList.left = new FormAttachment(0, 0);
		allPackagesList.setLayoutData(fd_allPackagesList);
		RConnection c = RServe.getConnection();
		int b = 0;
		if (c != null) {

			try {
				c.eval("try(pkg <- list(sort(.packages(all.available = TRUE))))");

				c.eval("names<-pkg[[1]]");
				try {
					b = (int) c.eval("length(names)").asInteger();
				} catch (REXPMismatchException e1) {

					e1.printStackTrace();
				}
			} catch (RserveException e2) {

				e2.printStackTrace();
			}

			for (int i = 1; i < b; i++) {

				String st = null;

				try {
					st = (String) c.eval("names[" + i + "]").asString();
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

						Bio7Dialog.message("RServer is busy!");

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

						Bio7Dialog.message("RServer is busy!");

					}
				} else {
					System.out.println("No Rserve connection available !");
				}

			}
		});
		final FormData fd_uninstallButton = new FormData();
		fd_uninstallButton.left = new FormAttachment(0, 0);
		fd_uninstallButton.right = new FormAttachment(100, 0);
		fd_uninstallButton.bottom = new FormAttachment(allPackagesList, 33, SWT.BOTTOM);
		fd_uninstallButton.top = new FormAttachment(allPackagesList, 5, SWT.BOTTOM);
		uninstallButton.setLayoutData(fd_uninstallButton);
		uninstallButton.setText("Remove");

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
