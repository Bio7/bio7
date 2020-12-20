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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.discrete.CounterModel;
import com.eco.bio7.methods.CurrentStates;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.table.XCell;
import com.sun.star.uno.UnoRuntime;

public class LibreOfficeSendValueJob extends WorkspaceJob {

	private boolean canceled = false;

	private int xrow = 0;

	private int ycolumn = 1;

	private XCell xCell;

	private boolean openrun = false;

	public LibreOfficeSendValueJob() {
		super("Connecting To LibreOffice");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Connecting To LibreOffice", 10);
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

				if (LibreOffice.getXSpreadsheets() != null) {

					try {
						LibreOffice.getXSpreadsheets().insertNewByName("Bio7" + "_" + LibreOffice.getPopnumber(), (short) 0);
					} catch (RuntimeException e) {
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.syncExec(new Runnable() {

							public void run() {
								MessageBox messageBox = new MessageBox(new Shell(),

								SWT.ICON_INFORMATION);
								messageBox.setMessage("No Spreadsheet opened !");
								messageBox.open();
							}
						});

					}

					Object sheet = LibreOffice.getXSpreadsheets().getByName("Bio7" + "_" + LibreOffice.getPopnumber());

					XSpreadsheet xSpreadsheet = (XSpreadsheet) UnoRuntime.queryInterface(XSpreadsheet.class, sheet);

					for (int i = 0; i < CurrentStates.getStateList().size(); i++) {
						xCell = xSpreadsheet.getCellByPosition(i, 0);
						xCell.setFormula(CurrentStates.getStateName(i));

						CounterModel zahl = (CounterModel) Quad2d.getQuad2dInstance().zaehlerlist.get(i);

						int z[] = zahl.getCounterListasArray();

						for (int j = 0; j < z.length; j++) {

							xCell = xSpreadsheet.getCellByPosition(xrow, ycolumn);
							xCell.setValue(z[j]);

							ycolumn++;
						}

						ycolumn = 1;
						xrow++;

					}

					xrow = 0;

					LibreOffice.setXSpreadsheetModel((XModel) UnoRuntime.queryInterface(XModel.class, LibreOffice.getXSpreadsheetComponent()));
					LibreOffice.setXSpreadsheetController(LibreOffice.getXSpreadsheetModel().getCurrentController());
					LibreOffice.setXSpreadsheetView((XSpreadsheetView) UnoRuntime.queryInterface(XSpreadsheetView.class, LibreOffice.getXSpreadsheetController()));
					LibreOffice.getXSpreadsheetView().setActiveSheet(xSpreadsheet);
					LibreOffice.setPopnumber(LibreOffice.getPopnumber() + 1);
				} else {
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_INFORMATION);
							messageBox.setMessage("No Spreadsheet opened !");
							messageBox.open();
						}
					});

				}
			}

			else {
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						MessageBox messageBox = new MessageBox(new Shell(),

						SWT.ICON_INFORMATION);
						messageBox.setMessage("LibreOffice connection failed  !");
						messageBox.open();
					}
				});
			}
		} catch (com.sun.star.lang.DisposedException e) {

			throw e;
		}

	}

}
