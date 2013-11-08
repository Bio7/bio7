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

package com.eco.bio7.libreoffice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class LibreOfficeConnectionJob extends WorkspaceJob {

	private IPreferenceStore store;

	private boolean canceled = false;

	private boolean openrun = false;

	public LibreOfficeConnectionJob() {
		super("Connecting To LibreOffice");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Connecting To LibreOffice", 20);
		canceled = false;
		store = Bio7Plugin.getDefault().getPreferenceStore();

		try {
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				
				List<String> args = new ArrayList<String>();
				args.add (store.getString(PreferenceConstants.PATH_LIBREOFFICE) + "\\soffice"); 	
				args.add ("-nologo"); 
				args.add ("-nodefault"); 
				args.add ("-accept=socket,host=0,port=2002;urp;"); 
				ProcessBuilder pb = new ProcessBuilder (args);
				pb.start();
				//String []lo={store.getString(PreferenceConstants.PATH_LIBREOFFICE) + "\\soffice" ,"-nologo" ,"-nodefault" ,"-accept=socket,host=0,port=2002;urp;"};
				//Runtime.getRuntime().exec(lo);
				//Runtime.getRuntime().exec(store.getString(PreferenceConstants.PATH_LIBREOFFICE) + "\\soffice -nologo -nodefault -accept=socket,host=0,port=2002;urp;");
			} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
				Runtime.getRuntime().exec(store.getString(PreferenceConstants.PATH_LIBREOFFICE) + "/soffice -nologo -nodefault -accept=socket,host=0,port=2002;urp;");
			}
			else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
				Runtime.getRuntime().exec(store.getString(PreferenceConstants.PATH_LIBREOFFICE) + "/soffice -nologo -nodefault -accept=socket,host=0,port=2002;urp;");
			}
		} catch (IOException e1) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					MessageDialog.openWarning(new Shell(), "LibreOffice", "LibreOffice version >4.0 executable not available !" +
							"\nPlease adjust the path in the Bio7 preferences.\n" +
							"Example Windows: C:\\Program Files\\LibreOffice x.x\\program\n"+
							"Example Linux (MacOSX): /usr/lib/libreoffice/program");
					canceled = true;
				}
			});
		}

		for (int i = 0; i < 21; i++) {
			if (monitor.isCanceled())
				try {
					throw new InterruptedException();
				} catch (InterruptedException e) {
					canceled = true;

				}
			if (canceled == false) {
				if (LibreOffice.getXRemoteServiceManager() == null) {

					doconnect();
					monitor.worked(1);
					monitor.setTaskName("Connect to LibreOffice: " + (i) + " from 20");

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {

					}
					if (i == 20) {
						monitor.worked(20);
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.syncExec(new Runnable() {

							public void run() {

								MessageDialog.openWarning(new Shell(), "LibreOffice", "LibreOffice connection could not be established !" + "\n"
										+ "Probably the firewall is active !" + "\n" + "Please deactivate the firewall and try again !");

							}
						});

					}

				}

				else {
					if (LibreOffice.getXRemoteServiceManager() != null) {
						openrun = true;
						monitor.setTaskName("Connected");
						monitor.worked(20);

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {

							e.printStackTrace();
						}

					} else {
						monitor.worked(20);
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.syncExec(new Runnable() {

							public void run() {
								MessageDialog.openWarning(new Shell(), "LibreOffice", "LibreOffice connection could not be established->Please try again !");

							}
						});
					}

					i = 21;
				}

			}
			monitor.worked(1);

		}

		return Status.OK_STATUS;
	}

	private void doconnect() {

		try {
			LibreOffice.setXRemoteServiceManager(this.getRemoteServiceManager("uno:socket,host=localhost,port=2002;urp;StarOffice.ServiceManager"));
		} catch (Exception e) {

		}

	}

	private XMultiComponentFactory getRemoteServiceManager(String unoUrl) throws java.lang.Exception {
		if (LibreOffice.getXRemoteContext() == null) {

			XComponentContext xLocalContext = com.sun.star.comp.helper.Bootstrap.createInitialComponentContext(null);

			XMultiComponentFactory xLocalServiceManager = xLocalContext.getServiceManager();

			Object urlResolver = xLocalServiceManager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", xLocalContext);

			XUnoUrlResolver xUnoUrlResolver = (XUnoUrlResolver) UnoRuntime.queryInterface(XUnoUrlResolver.class, urlResolver);

			Object initialObject = xUnoUrlResolver.resolve(unoUrl);
			XPropertySet xPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, initialObject);
			Object context = xPropertySet.getPropertyValue("DefaultContext");
			LibreOffice.setXRemoteContext((XComponentContext) UnoRuntime.queryInterface(XComponentContext.class, context));
		}
		return LibreOffice.getXRemoteContext().getServiceManager();
	}

}
