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

import java.util.Random;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;
import com.sun.star.frame.XModel;
import com.sun.star.uno.UnoRuntime;

public class LibreOfficeValueToRJob extends WorkspaceJob {

	private IPreferenceStore store;

	private boolean canceled = false;

	private static String sheetdata[][];

	public LibreOfficeValueToRJob() {
		super("Get Spreadsheet values");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Get Spreadsheet values", 10);
		canceled = false;
		store = Bio7Plugin.getDefault().getPreferenceStore();
		if (LibreOffice.getXSpreadsheetModel() != null) {
			try {
				doSampleFunction(LibreOffice.getXSpreadsheetModel());
			} catch (Exception e1) {
				Bio7Dialog.message("No spreadsheet opened !");
			}
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

		if (monitor.isCanceled())
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {
				canceled = true;

			}

		monitor.worked(10);

		return Status.OK_STATUS;
	}

	public void doSampleFunction(XModel xModel) throws Exception {
		if (xModel != null) {
			if (xModel.getCurrentController() != null) {
				com.sun.star.frame.XController xController = xModel.getCurrentController();

				com.sun.star.sheet.XRangeSelection xRngSel = (com.sun.star.sheet.XRangeSelection) UnoRuntime.queryInterface(com.sun.star.sheet.XRangeSelection.class, xController);
				ExampleRangeListener aListener = new ExampleRangeListener();
				xRngSel.addRangeSelectionListener(aListener);
				com.sun.star.beans.PropertyValue[] aArguments = new com.sun.star.beans.PropertyValue[2];
				aArguments[0] = new com.sun.star.beans.PropertyValue();
				aArguments[0].Name = "Title";
				aArguments[0].Value = "Please select a cell range (e.g. C4:E6) without Title";
				aArguments[1] = new com.sun.star.beans.PropertyValue();

				aArguments[1].Value = new Boolean(true);
				xRngSel.startRangeSelection(aArguments);
				synchronized (aListener) {
					aListener.wait(); // wait until the selection is done
				}
				xRngSel.removeRangeSelectionListener(aListener);
				if (aListener.aResult != null && aListener.aResult.length() != 0) {
					com.sun.star.view.XSelectionSupplier xSel = (com.sun.star.view.XSelectionSupplier) UnoRuntime.queryInterface(com.sun.star.view.XSelectionSupplier.class, xController);
					com.sun.star.sheet.XSpreadsheetView xView = (com.sun.star.sheet.XSpreadsheetView) UnoRuntime.queryInterface(com.sun.star.sheet.XSpreadsheetView.class, xController);
					com.sun.star.sheet.XSpreadsheet xSheet = xView.getActiveSheet();
					com.sun.star.table.XCellRange xResultRange = xSheet.getCellRangeByName(aListener.aResult);

					xSel.select(xResultRange);
					com.sun.star.sheet.XCellRangeData xData = (com.sun.star.sheet.XCellRangeData) UnoRuntime.queryInterface(com.sun.star.sheet.XCellRangeData.class, xResultRange);
					Object[][] data = xData.getDataArray();
					// Reverse the array!
					sheetdata = new String[data[0].length][data.length];

					for (int i = 0; i < data.length; i++) {
						for (int j = 0; j < data[0].length; j++) {
							try {
								if (data[i][j] == null || data[i][j].toString().equals("")) {
									sheetdata[j][i] = "NA";

								}

								else {
									sheetdata[j][i] = data[i][j].toString();

								}
							} catch (NumberFormatException e) {

								sheetdata[j][i] = "NA";
							}

						}
					}

					if (RServe.isRrunning()) {
						RConnection connection = RServe.getConnection();
						String stringBuild = "."+generateRandomString();
						String stringRowNames = "."+generateRandomString();
						connection.eval("try(LibreOffice<-data.frame(1:" + sheetdata[0].length + "))");

						StringBuffer str = new StringBuffer();
						for (int i = 0; i < sheetdata.length; i++) {
							str.append("X" + (i + 1) + ",");
							// name = "X" + (i+1);
							String name = stringBuild + (i + 1);
							connection.assign(name, sheetdata[i]);

							/*
							 * If no String is present convert to integer ->NA
							 * values will also be converted!
							 */
							connection.eval("try(if(sum(is.na(as.numeric(" + name + "[" + name + "!=\"NA\"])))==0){" + name + "<-as.numeric(" + name + ")}else{" + name + "<-as.character(" + name
									+ ")})");

							connection.eval("try(LibreOffice<-data.frame(LibreOffice," + name + "))");
							connection.eval("try(remove(" + name + "))");
						}
						/* Transfer the edited col names! */
						String s = str.toString();

						try {
							connection.assign(stringRowNames, s.split(","));
						} catch (REngineException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						/*
						 * Delete the first col which was only created to use
						 * the dataframe!
						 */
						connection.eval("try(LibreOffice[1]<-NULL)");
						/* We rename the cols here! */
						connection.eval("colnames(LibreOffice) <-"+stringRowNames+"");
						/* Remove the vector with the colnames! */
						connection.eval("remove("+stringRowNames+")");

						str = null;

					}

					UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xResultRange);

					System.out.println("Selected: " + aListener.aResult);

				}
			}
		}
	}
	private String generateRandomString() {
		String choose = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rnd = new Random();

		StringBuilder stringBuild = new StringBuilder(30);
		for (int i = 0; i < 30; i++){
			stringBuild.append(choose.charAt(rnd.nextInt(choose.length())));
		}
		
		return stringBuild.toString();
	}

	private class ExampleRangeListener implements com.sun.star.sheet.XRangeSelectionListener {
		public String aResult;

		public void done(com.sun.star.sheet.RangeSelectionEvent aEvent) {
			aResult = aEvent.RangeDescriptor;
			synchronized (this) {
				notify();
			}
		}

		public void aborted(com.sun.star.sheet.RangeSelectionEvent aEvent) {
			synchronized (this) {
				notify();
			}
		}

		public void disposing(com.sun.star.lang.EventObject aObj) {
		}
	}

}
