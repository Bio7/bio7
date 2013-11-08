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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;

public class ListRPackagesJob extends WorkspaceJob {

	private String st;

	public ListRPackagesJob() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Retrieve package list..", IProgressMonitor.UNKNOWN);
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String server = store.getDefaultString(PreferenceConstants.PACKAGE_R_SERVER);
		if (RServe.isAlive()) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					PackagesList.getAllList().removeAll();
				}
			});
			RConnection c = RServe.getConnection();

			try {
				c.eval("pkg<-available.packages(contriburl=contrib.url(\"" + server + "\"))");

				c.eval("names<-pkg[,1]");
				int b = 0;
				try {
					b = (int) c.eval("length(names)").asInteger();
				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				for (int i = 1; i < b; i++) {

					st = null;
					try {
						st = (String) c.eval("names[" + i + "]").asString();
					} catch (REXPMismatchException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					display.syncExec(new Runnable() {

						public void run() {
							if (PackagesList.getAllList().isDisposed() == false) {
								PackagesList.getAllList().add(st);
							}
						}
					});

				}
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return Status.OK_STATUS;
	}

}
