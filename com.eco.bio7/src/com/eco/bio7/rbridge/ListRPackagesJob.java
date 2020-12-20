/*******************************************************************************
 * Copyright (c) 2007-2017 M. Austenfeld
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.views.PackageInstallView;

public class ListRPackagesJob extends WorkspaceJob {

	private String[] packageList;

	public ListRPackagesJob() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Retrieve package list..", IProgressMonitor.UNKNOWN);
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String server = store.getString("R_PACKAGE_SERVER");
		if (RServe.isAlive()) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					PackageInstallView.getAllList().removeAll();
				}
			});
			RConnection c = RServe.getConnection();

			try {
				c.eval("try(.bio7ListOfWebPackages<-available.packages(contriburl=contrib.url(\"" + server + "\")))");

				try {
					packageList = c.eval("try(.bio7ListOfWebPackages[,1])").asStrings();
				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				
					display.syncExec(new Runnable() {

						public void run() {
							if (PackageInstallView.getAllList().isDisposed() == false) {
								PackageInstallView.getAllList().setItems(packageList);
							}
						}
					});

				
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return Status.OK_STATUS;
	}

}
