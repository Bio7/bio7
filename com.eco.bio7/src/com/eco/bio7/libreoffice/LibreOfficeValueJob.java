package com.eco.bio7.libreoffice;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.sun.star.frame.XModel;
import com.sun.star.uno.UnoRuntime;

public class LibreOfficeValueJob extends WorkspaceJob {

	private IPreferenceStore store;

	private boolean canceled = false;

	private boolean openrun = false;

	public LibreOfficeValueJob() {
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
				aArguments[0].Value = "Please select a cell range (e.g. C4:E6)without Title";
				aArguments[1] = new com.sun.star.beans.PropertyValue();

				aArguments[1].Value = new Boolean(true);
				xRngSel.startRangeSelection(aArguments);
				synchronized (aListener) {
					aListener.wait(); // wait until the selection is done
				}
				xRngSel.removeRangeSelectionListener(aListener);
				if (aListener.aResult != null && aListener.aResult.length() != 0) {
					com.sun.star.view.XSelectionSupplier xSel = (com.sun.star.view.XSelectionSupplier) UnoRuntime.queryInterface(com.sun.star.view.XSelectionSupplier.class,
							xController);
					com.sun.star.sheet.XSpreadsheetView xView = (com.sun.star.sheet.XSpreadsheetView) UnoRuntime.queryInterface(com.sun.star.sheet.XSpreadsheetView.class,
							xController);
					com.sun.star.sheet.XSpreadsheet xSheet = xView.getActiveSheet();
					com.sun.star.table.XCellRange xResultRange = xSheet.getCellRangeByName(aListener.aResult);

					xSel.select(xResultRange);
					com.sun.star.sheet.XCellRangeData xData = (com.sun.star.sheet.XCellRangeData) UnoRuntime.queryInterface(com.sun.star.sheet.XCellRangeData.class, xResultRange);
					Object[][] data = xData.getDataArray();
					LibreOffice.setSheetdata(new String[data[0].length][data.length]);

					for (int i = 0; i < data.length; i++) {
						for (int j = 0; j < data[0].length; j++) {

							LibreOffice.getSheetdata()[j][i] = data[i][j].toString();

						}
					}

					for (int i = 0; i < LibreOffice.getSheetdata().length; i++) {
						System.out.println("");
						for (int j = 0; j < LibreOffice.getSheetdata()[0].length; j++) {
							System.out.print(LibreOffice.getSheetdata()[i][j]);
						}

					}
					BeanShellInterpreter.doInterpret("String [][]sheetdata=LibreOffice.getSheetdata();", null);
					UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xResultRange);

				}
			}
		}
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
