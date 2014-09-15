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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XController;
import com.sun.star.frame.XModel;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.XCell;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * This is an easy to use class for the transfer of calculation values from or
 * to Bio7 from or to the LibreOffice calculator tool.
 * 
 * @author Bio7
 */
public class LibreOffice {

	private static XComponentContext xRemoteContext = null;

	private static XMultiComponentFactory xRemoteServiceManager = null;

	private static XComponent xSpreadsheetComponent = null;

	private static XModel xSpreadsheetModel = null;

	private static XController xSpreadsheetController = null;

	private static XSpreadsheetView xSpreadsheetView = null;

	private static XSpreadsheetDocument xSpreadsheetDocument;

	private static XSpreadsheets xSpreadsheets;

	private static XComponentLoader xComponentLoader;

	private static Object desktop;

	private static int popnumber = 1;

	private static int xrow = 0;

	private static int ycolumn = 1;

	private static XCell xCell;

	private static String[][] sheetdata;

	private static String currentsheet;

	private static boolean spreadopened = false;

	/**
	 * Opens a new LibreOffice spreadsheet.
	 */
	public static void openSpread() {

		try {
			if (LibreOffice.xRemoteServiceManager != null) {
				try {
					LibreOffice.desktop = xRemoteServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", xRemoteContext);
				} catch (Exception e) {

					e.printStackTrace();
				}
				LibreOffice.xComponentLoader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, LibreOffice.desktop);

				PropertyValue[] loadProps = new PropertyValue[0];
				try {
					LibreOffice.xSpreadsheetComponent = xComponentLoader.loadComponentFromURL("private:factory/scalc", "_blank", 0, loadProps);
				} catch (IOException e) {

					e.printStackTrace();
				} catch (IllegalArgumentException e) {

					e.printStackTrace();
				}

				LibreOffice.xSpreadsheetDocument = (XSpreadsheetDocument) UnoRuntime.queryInterface(XSpreadsheetDocument.class, LibreOffice.xSpreadsheetComponent);

				LibreOffice.xSpreadsheets = LibreOffice.xSpreadsheetDocument.getSheets();

				com.sun.star.uno.Type elemType = LibreOffice.xSpreadsheets.getElementType();

				LibreOffice.xSpreadsheetModel = (XModel) UnoRuntime.queryInterface(XModel.class, LibreOffice.xSpreadsheetComponent);
				LibreOffice.xSpreadsheetController = LibreOffice.xSpreadsheetModel.getCurrentController();
				LibreOffice.xSpreadsheetView = (XSpreadsheetView) UnoRuntime.queryInterface(XSpreadsheetView.class, LibreOffice.xSpreadsheetController);

				LibreOffice.popnumber = 1;

			} else {
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						MessageBox messageBox = new MessageBox(new Shell(),

						SWT.ICON_INFORMATION);
						messageBox.setMessage("Open a connection to LibreOffice!");
						messageBox.open();
					}
				});
			}
		} catch (com.sun.star.lang.DisposedException e) { // works from Patch
			LibreOffice.setXRemoteContext(null);
			LibreOffice.setXRemoteServiceManager(null);
			// throw e;
		}

	}

	/**
	 * Opens a selection dialog for the transfer of spreadsheet values to R.
	 */
	public static void selectionToR() {
		if (RState.isBusy() == false) {
			if (xSpreadsheetModel != null) {
				LibreOfficeValueToRJob to = new LibreOfficeValueToRJob();

				try {
					to.doSampleFunction(xSpreadsheetModel);
				} catch (java.lang.Exception e) {

					e.printStackTrace();
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
		} else {
			Bio7Dialog.message("RServer is busy!");
		}
	}

	/**
	 * Opens a selection dialog for the transfer of spreadsheet values to
	 * BeanShell.
	 */
	public static void selectionToBeanshell() {
		if (xSpreadsheetModel != null) {
			LibreOfficeValueJob to = new LibreOfficeValueJob();

			try {
				to.doSampleFunction(xSpreadsheetModel);
			} catch (java.lang.Exception e) {

				e.printStackTrace();
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

	}

	/**
	 * Returns a list of available sheets in the opened LibreOffice spreadsheet.
	 * 
	 * @return the available sheets as a string array.
	 */
	public static String[] getSheets() {

		return LibreOffice.xSpreadsheets.getElementNames();

	}

	/**
	 * Returns sheet values from the specified range.
	 * 
	 * @param range
	 *            a string representation of the range.
	 */
	public static void getSheetValues(String range) {

		com.sun.star.frame.XController xController = xSpreadsheetModel.getCurrentController();
		com.sun.star.view.XSelectionSupplier xSel = (com.sun.star.view.XSelectionSupplier) UnoRuntime.queryInterface(com.sun.star.view.XSelectionSupplier.class, xController);
		com.sun.star.sheet.XSpreadsheetView xView = (com.sun.star.sheet.XSpreadsheetView) UnoRuntime.queryInterface(com.sun.star.sheet.XSpreadsheetView.class, xController);
		com.sun.star.sheet.XSpreadsheet xSheet = xView.getActiveSheet();
		com.sun.star.table.XCellRange xResultRange = xSheet.getCellRangeByName(range);

		try {
			xSel.select(range);
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		}
		com.sun.star.sheet.XCellRangeData xData = (com.sun.star.sheet.XCellRangeData) UnoRuntime.queryInterface(com.sun.star.sheet.XCellRangeData.class, xResultRange);
		Object[][] data = xData.getDataArray();
		sheetdata = new String[data.length][data[0].length];
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				try {
					if (data[i][j] == null || data[i][j].toString().equals("")) {
						sheetdata[i][j] = "NA";

					}

					else {
						sheetdata[i][j] = data[i][j].toString();

					}
				} catch (NumberFormatException e) {

					sheetdata[i][j] = "NA";
				}

			}
		}

		UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xResultRange);

	}

	/**
	 * Inserts values in the LibreOffice spreadsheet.
	 * 
	 * @param data
	 *            the data as a double array.
	 * @param head
	 *            a string array for the head of the data.
	 */
	public static void sheetInsertValues(double[][] data, String[] head) {
		if (LibreOffice.xRemoteServiceManager != null) {

			Object sheet = null;
			try {
				sheet = LibreOffice.xSpreadsheets.getByName(currentsheet);
			} catch (NoSuchElementException e) {

				e.printStackTrace();
			} catch (WrappedTargetException e) {

				e.printStackTrace();
			}// Hier

			XSpreadsheet xSpreadsheet = (XSpreadsheet) UnoRuntime.queryInterface(XSpreadsheet.class, sheet);

			for (int i = 0; i < data.length; i++) {
				try {
					xCell = xSpreadsheet.getCellByPosition(i, 0);

				} catch (IndexOutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				xCell.setFormula(head[i]);

				for (int j = 0; j < data[0].length; j++) {

					try {
						xCell = xSpreadsheet.getCellByPosition(xrow, ycolumn);

					} catch (IndexOutOfBoundsException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					xCell.setValue(data[i][j]);

					ycolumn++;
				}

				ycolumn = 1;
				xrow++;

			}

			xrow = 0;

			xSpreadsheetModel = (XModel) UnoRuntime.queryInterface(XModel.class, LibreOffice.xSpreadsheetComponent);
			xSpreadsheetController = LibreOffice.xSpreadsheetModel.getCurrentController();
			xSpreadsheetView = (XSpreadsheetView) UnoRuntime.queryInterface(XSpreadsheetView.class, LibreOffice.xSpreadsheetController);
			xSpreadsheetView.setActiveSheet(xSpreadsheet);
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

	}

	/**
	 * Inserts values in the opened spreadsheet and marks them at the given
	 * coordinates.
	 * 
	 * @param data
	 *            the data as a double array.
	 * @param head
	 *            a string array for the head of the data.
	 * @param sheety
	 *            the y-coordinates for the marks as an integer array.
	 * @param sheetx
	 *            the x-coordinates for the marks as an integer array.
	 */
	public static void sheetInsertValuesMark(double[][] data, String[] head, int[] sheetx, int[] sheety) {
		if (LibreOffice.xRemoteServiceManager != null) {

			Object sheet = null;
			try {
				sheet = LibreOffice.xSpreadsheets.getByName(currentsheet);
			} catch (NoSuchElementException e) {

				e.printStackTrace();
			} catch (WrappedTargetException e) {

				e.printStackTrace();
			}

			XSpreadsheet xSpreadsheet = (XSpreadsheet) UnoRuntime.queryInterface(XSpreadsheet.class, sheet);

			for (int y = 0; y < data.length; y++) {
				try {
					xCell = xSpreadsheet.getCellByPosition(y, 0);

				} catch (IndexOutOfBoundsException e) {

					e.printStackTrace();
				}
				xCell.setFormula(head[y]);

				for (int x = 0; x < data[0].length; x++) {

					try {
						xCell = xSpreadsheet.getCellByPosition(xrow, ycolumn);

					} catch (IndexOutOfBoundsException e) {

						e.printStackTrace();
					}
					for (int i = 0; i < sheetx.length; i++) {

						if (sheetx[i] == y && sheety[i] == x) {
							com.sun.star.beans.XPropertySet xPropSet = null;
							xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xCell);
							try {
								xPropSet.setPropertyValue("CharColor", new Integer(0xff0000));
							} catch (UnknownPropertyException e) {

								e.printStackTrace();
							} catch (PropertyVetoException e) {

								e.printStackTrace();
							} catch (IllegalArgumentException e) {

								e.printStackTrace();
							} catch (WrappedTargetException e) {

								e.printStackTrace();
							}

						}

					}

					xCell.setValue(data[y][x]);

					ycolumn++;
				}

				ycolumn = 1;
				xrow++;

			}

			xrow = 0;

			xSpreadsheetModel = (XModel) UnoRuntime.queryInterface(XModel.class, LibreOffice.xSpreadsheetComponent);
			xSpreadsheetController = LibreOffice.xSpreadsheetModel.getCurrentController();
			xSpreadsheetView = (XSpreadsheetView) UnoRuntime.queryInterface(XSpreadsheetView.class, LibreOffice.xSpreadsheetController);
			xSpreadsheetView.setActiveSheet(xSpreadsheet);

		} else {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_INFORMATION);
					messageBox.setMessage("No Connection to LibreOffice !");
					messageBox.open();
				}
			});

		}

	}

	/**
	 * Inserts text in the opened spreadsheet at the given coordinates.
	 * 
	 * @param text
	 *            the text as a string.
	 * @param y
	 *            the y-coordinate of the text.
	 * @param x
	 *            the x-coordinate of the text.
	 */
	public static void insertText(String text, int x, int y) {
		if (xRemoteServiceManager != null) {

			Object sheet = null;
			try {
				sheet = xSpreadsheets.getByName(currentsheet);
			} catch (NoSuchElementException e) {

				e.printStackTrace();
			} catch (WrappedTargetException e) {

				e.printStackTrace();
			}

			XSpreadsheet xSpreadsheet = (XSpreadsheet) UnoRuntime.queryInterface(XSpreadsheet.class, sheet);

			XCell xCell = null;
			try {
				xCell = xSpreadsheet.getCellByPosition(x, y);
			} catch (IndexOutOfBoundsException e) {

				e.printStackTrace();
			}

			xCell.setFormula(text);

			xSpreadsheetModel = (XModel) UnoRuntime.queryInterface(XModel.class, LibreOffice.xSpreadsheetComponent);
			xSpreadsheetController = LibreOffice.xSpreadsheetModel.getCurrentController();
			xSpreadsheetView = (XSpreadsheetView) UnoRuntime.queryInterface(XSpreadsheetView.class, LibreOffice.xSpreadsheetController);
			xSpreadsheetView.setActiveSheet(xSpreadsheet);

		} else {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_INFORMATION);
					messageBox.setMessage("No Connection to LibreOffice !");
					messageBox.open();
				}
			});

		}

	}

	/**
	 * Activates the sheet with the given name.
	 * 
	 * @param name
	 *            the name of the sheet as a string.
	 */
	public static void activateSheet(String name) {
		setCurrentSheet(name);
		Object sheet = null;
		try {
			sheet = LibreOffice.xSpreadsheets.getByName(name);
		} catch (NoSuchElementException e) {

			e.printStackTrace();
		} catch (WrappedTargetException e) {

			e.printStackTrace();
		}

		XSpreadsheet xSpreadsheet = (XSpreadsheet) UnoRuntime.queryInterface(XSpreadsheet.class, sheet);

		xSpreadsheetModel = (XModel) UnoRuntime.queryInterface(XModel.class, LibreOffice.xSpreadsheetComponent);
		xSpreadsheetController = LibreOffice.xSpreadsheetModel.getCurrentController();
		xSpreadsheetView = (XSpreadsheetView) UnoRuntime.queryInterface(XSpreadsheetView.class, LibreOffice.xSpreadsheetController);
		xSpreadsheetView.setActiveSheet(xSpreadsheet);

	}

	protected static void setCurrentSheet(String name) {
		currentsheet = name;
	}

	/**
	 * Creates a new sheet in the activated spreadsheet with the given name.
	 * 
	 * @param name
	 *            the name of the new sheet as a string.
	 */
	public static void newSheet(String name) {
		if (xRemoteServiceManager != null) {

			try {
				xSpreadsheets.insertNewByName(name, (short) 0);// Name
				activateSheet(name);
			} catch (RuntimeException e) {
				if (xSpreadsheets != null) {
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_INFORMATION);
							messageBox.setMessage("Spreadsheet with name exists! Any values will be added!");
							messageBox.open();
						}
					});

				} else {
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_INFORMATION);
							messageBox.setMessage("No spreadsheet is opened !");
							messageBox.open();
						}
					});

				}
			}

		} else {
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
	}

	protected static XComponentContext getXRemoteContext() {
		return xRemoteContext;
	}

	protected static XMultiComponentFactory getXRemoteServiceManager() {
		return xRemoteServiceManager;
	}

	protected static void setXRemoteServiceManager(XMultiComponentFactory remoteServiceManager) {
		xRemoteServiceManager = remoteServiceManager;
	}

	protected static void setXRemoteContext(XComponentContext remoteContext) {
		xRemoteContext = remoteContext;
	}

	protected static int getPopnumber() {
		return popnumber;
	}

	protected static void setPopnumber(int popnumber) {
		LibreOffice.popnumber = popnumber;
	}

	protected static XSpreadsheets getXSpreadsheets() {
		return xSpreadsheets;
	}

	protected static void setXSpreadsheets(XSpreadsheets spreadsheets) {
		xSpreadsheets = spreadsheets;
	}

	protected static XSpreadsheetView getXSpreadsheetView() {
		return xSpreadsheetView;
	}

	protected static void setXSpreadsheetView(XSpreadsheetView spreadsheetView) {
		xSpreadsheetView = spreadsheetView;
	}

	protected static XModel getXSpreadsheetModel() {
		return xSpreadsheetModel;
	}

	protected static void setXSpreadsheetModel(XModel spreadsheetModel) {
		xSpreadsheetModel = spreadsheetModel;
	}

	protected static XComponent getXSpreadsheetComponent() {
		return xSpreadsheetComponent;
	}

	protected static void setXSpreadsheetComponent(XComponent spreadsheetComponent) {
		xSpreadsheetComponent = spreadsheetComponent;
	}

	protected static XController getXSpreadsheetController() {
		return xSpreadsheetController;
	}

	protected static void setXSpreadsheetController(XController spreadsheetController) {
		xSpreadsheetController = spreadsheetController;
	}

	/**
	 * Returns the sheet data of the spreadsheet.
	 * 
	 * @return the sheet data as a double array.
	 */
	public static String[][] getSheetdata() {
		return sheetdata;
	}

	protected static void setSheetdata(String[][] sheetdata) {
		LibreOffice.sheetdata = sheetdata;
	}

	protected static Object getDesktop() {
		return desktop;
	}

	protected static void setDesktop(Object desktop) {
		LibreOffice.desktop = desktop;
	}

	protected static XComponentLoader getXComponentLoader() {
		return xComponentLoader;
	}

	protected static void setXComponentLoader(XComponentLoader componentLoader) {
		xComponentLoader = componentLoader;
	}

	protected static XSpreadsheetDocument getXSpreadsheetDocument() {
		return xSpreadsheetDocument;
	}

	protected static void setXSpreadsheetDocument(XSpreadsheetDocument spreadsheetDocument) {
		xSpreadsheetDocument = spreadsheetDocument;
	}

	/**
	 * Returns if the spread has been opened.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isSpreadopened() {
		return spreadopened;
	}

	/**
	 * Sets the value to true.
	 * 
	 * @param spreadopened
	 *            a boolean value.
	 */
	protected static void setSpreadopened(boolean spreadopened) {
		LibreOffice.spreadopened = spreadopened;
	}

	/**
	 * Returns if Bio7 is connected to LibreOffice.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isConnected() {

		if (xRemoteServiceManager == null) {
			return false;
		} else {
			return true;
		}

	}

}
