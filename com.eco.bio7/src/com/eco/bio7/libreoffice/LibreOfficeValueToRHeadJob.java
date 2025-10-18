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
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RServe;
import com.sun.star.frame.XModel;
import com.sun.star.uno.UnoRuntime;

public class LibreOfficeValueToRHeadJob extends WorkspaceJob {

	private IPreferenceStore store;

	private boolean canceled = false;

	private static String sheetdata[][];

	private String[] colnames;

	private String[] firstChar = { ".", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };

	public LibreOfficeValueToRHeadJob() {
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
				Bio7Dialog.message("No spreadsheet opened or an exception occured!");
			}
		} else {

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_INFORMATION);
					messageBox.setMessage("No Spreadsheet opened !"); //$NON-NLS-1$
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
				aArguments[0].Value = "Please select a cell range (e.g. C4:E6) with Title";
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
					sheetdata = new String[data[0].length][data.length - 1];
					colnames = new String[data[0].length];

					for (int c = 0; c < data[0].length; c++) {
						if (data[0][c].toString().equals("") || data[0][c].toString() == null) {
							colnames[c] = "X" + c;

						} else {
							if (data[0][c] instanceof Double || data[0][c] instanceof Integer) {
								colnames[c] = "X" + String.valueOf(data[0][c].toString());
							} else {
								colnames[c] = data[0][c].toString();
								/*Replace wrong header chars!*/
								
								String regEx = "[^a-zA-Z0-9_.]";
								if (colnames[c].substring(0, 1).matches("[0-9]")) {
									colnames[c] = colnames[c].replaceFirst("[0-9]", "X" + colnames[c].charAt(0));
								}

								else if (colnames[c].substring(0, 1).matches(regEx)) {

									colnames[c] = colnames[c].replaceFirst(regEx, "X.");
								}
								colnames[c] = colnames[c].replaceAll(regEx, ".");
									/*String[] a = st.split(",");

									
									 * Replace the comma since it is the split
									 * argument!
									 
									colnames[c] = colnames[c].replace(",", ".");
									for (int i = 0; i < a.length; i++) {

										colnames[c] = colnames[c].replace(a[i], ".");
										
										 * Replace 'numbers and .' if they are
										 * the first character (numbers replaced
										 * by X+number)!
										 
										for (int j = 0; j < firstChar.length; j++) {

											if (colnames[c].startsWith(firstChar[j])) {
												colnames[c] = colnames[c].replaceFirst(firstChar[j], "X" + firstChar[j]);
											}
										}
									}*/
								//}
							}
						}
					}

					for (int i = 1; i < data.length; i++) {
						for (int j = 0; j < data[0].length; j++) {
							int count = i - 1;
							String number = "NA";

							try {
								if (data[i][j] == null || data[i][j].toString().equals("")) {
									number = "NA";

								}

								else {
									number = data[i][j].toString();

								}

							} catch (NumberFormatException e) {

								number = "NA";
							}

							sheetdata[j][count] = number;

						}
					}

					if (RServe.isRrunning()) {
						RConnection connection = RServe.getConnection();
						String stringBuild = "."+generateRandomString();
						String stringRowNames = "."+generateRandomString();
						connection.eval("try(LibreOffice<-data.frame(1:" + sheetdata[0].length + "))");
						for (int i = 0; i < sheetdata.length; i++) {

							String num = stringBuild + (i + 1);

							connection.assign("" + num, sheetdata[i]);
							connection.eval("try(if(sum(is.na(as.numeric(" + num + "[" + num + "!=\"NA\"])))==0){" + num + "<-as.numeric(" + num + ")}else{" + num + "<-as.character(" + num + ")})");
							connection.eval("try(LibreOffice<-data.frame(LibreOffice," + num + "))");
							connection.eval("try(remove(" + num + "))");
						}

						try {
							/* Transfer the edited col names! */
							connection.assign(stringRowNames, colnames);
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
