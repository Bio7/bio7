/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.views.PackageInstallView;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

public class ListRPackagesJob extends WorkspaceJob {

	private String[] packageList;

	public ListRPackagesJob() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Retrieve package list..", IProgressMonitor.UNKNOWN);
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String server = store.getString("R_PACKAGE_SERVER");
		if (RTools.isAlive()) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					PackageInstallView.getAllList().removeAll();
				}
			});
			RBackend c = RBridge.getBackend();

			try {
				/*
				 * The source index of the repository (cached on disk for a day, see
				 * rcompletion/bio7Packages.R); the matrix stays in R (not transferred)
				 */
				c.voidEval("try(.bio7ListOfWebPackages <- if (exists(\".bio7AvailablePackages\")) .bio7AvailablePackages(\"" + server
						+ "\", type = \"source\") else available.packages(contriburl = contrib.url(\"" + server + "\")))");

				try {
					packageList = c.eval("try(.bio7ListOfWebPackages[,1])").strings();
				} catch (RTypeException e) {
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

				
			} catch (RException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return Status.OK_STATUS;
	}

}
