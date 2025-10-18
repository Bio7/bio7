/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/


package com.eco.bio7.libreoffice;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.UnoRuntime;


public class LibreOpenJob extends WorkspaceJob implements IJobChangeListener {

	private boolean canceled = false;

	public LibreOpenJob() {
		super("Open a spread");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Open a spread", 10);
		canceled = false;

		try {

			useConnection();

		} catch (Exception e1) {

			e1.printStackTrace();
		}

		if (monitor.isCanceled())
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {
				canceled = true;

			}

		monitor.worked(10);

		return Status.OK_STATUS;
	}

	public void useConnection() throws java.lang.Exception {
		try {
			if (LibreOffice.getXRemoteServiceManager() != null) {
				LibreOffice.setDesktop(LibreOffice.getXRemoteServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", LibreOffice.getXRemoteContext()));
				LoTerminateListener terminateListener = new LoTerminateListener();
				/* Add a listener if the Oo application has been terminated ! */
				XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, LibreOffice.getDesktop());

				xDesktop.addTerminateListener(terminateListener);

				LibreOffice.setXComponentLoader((XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, LibreOffice.getDesktop()));

				PropertyValue[] loadProps = new PropertyValue[0];
				LibreOffice.setXSpreadsheetComponent(LibreOffice.getXComponentLoader().loadComponentFromURL("private:factory/scalc", "_blank", 0, loadProps));

				LibreOffice.setXSpreadsheetDocument((XSpreadsheetDocument) UnoRuntime.queryInterface(XSpreadsheetDocument.class, LibreOffice.getXSpreadsheetComponent()));

				LibreOffice.setXSpreadsheets(LibreOffice.getXSpreadsheetDocument().getSheets());

				com.sun.star.uno.Type elemType = LibreOffice.getXSpreadsheets().getElementType();

				LibreOffice.setXSpreadsheetModel((XModel) UnoRuntime.queryInterface(XModel.class, LibreOffice.getXSpreadsheetComponent()));
				LibreOffice.setXSpreadsheetController(LibreOffice.getXSpreadsheetModel().getCurrentController());
				LibreOffice.setXSpreadsheetView((XSpreadsheetView) UnoRuntime.queryInterface(XSpreadsheetView.class, LibreOffice.getXSpreadsheetController()));

				LibreOffice.setPopnumber(1);
			} else {
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						MessageBox messageBox = new MessageBox(new Shell(),

						SWT.ICON_INFORMATION);
						messageBox.setMessage("Open a connection to OpenOffice!");
						messageBox.open();
					}
				});
			}
		} catch (com.sun.star.lang.DisposedException e) { // works from Patch

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_INFORMATION);
					messageBox.setMessage("Open a connection to OpenOffice!");
					messageBox.open();
				}
			});
			LibreOffice.setXRemoteContext(null);
			LibreOffice.setXRemoteServiceManager(null);
			//throw e;
			

		}

	}

	public void aboutToRun(IJobChangeEvent event) {

	}

	public void awake(IJobChangeEvent event) {

	}

	public void done(IJobChangeEvent event) {
		LibreOffice.setSpreadopened(true);

	}

	public void running(IJobChangeEvent event) {
		LibreOffice.setSpreadopened(false);

	}

	public void scheduled(IJobChangeEvent event) {

	}

	public void sleeping(IJobChangeEvent event) {

	}

}
