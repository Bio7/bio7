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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rbridge.views.PackageInstallView;

public class RemoveRLibrarysJob extends WorkspaceJob {

	//protected String[] items;
	protected TableItem[] tableItems;

	public RemoveRLibrarysJob() {
		super("Remove...");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Remove Librarys..", IProgressMonitor.UNKNOWN);
		if (RServe.isAlive()) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					tableItems = PackageInstallView.getAllInstalledPackagesList().getSelection();

					for (int i = 0; i < tableItems.length; i++) {
						RConnection c = RServe.getConnection();

						try {
							c.eval("try(remove.packages(\"" + tableItems[i].getText(0) + "\"))");
						} catch (RserveException e) {

							e.printStackTrace();
						}
						System.out.println("Removed library " + tableItems[i].getText(0));

					}
				}
			});

		}

		return Status.OK_STATUS;
	}

}
