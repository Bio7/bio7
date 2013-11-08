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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.apache.poi.xssf.usermodel.*;
import com.eco.bio7.batch.Bio7Dialog;

public class LoadXlsxExcelJob extends WorkspaceJob {

	protected String file;
	protected Grid grid;
	
	private int s;
	private boolean canRead;
	private XSSFWorkbook wb;
	

	public LoadXlsxExcelJob(String files, Grid grids) {
		super("Load Excel");
		this.file = files;
		this.grid = grids;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Load Excel", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}
		loadExcel(file);

		monitor.done();
		return Status.OK_STATUS;
	}

	private void loadExcel(final String file) {
		final File fil = new File(file);
		if (fil.exists()) {
			canRead = true;
			if (grid != null) {

				try {
					InputStream inp = new FileInputStream(file);

					
						  try {
							wb = new XSSFWorkbook(inp);
						} catch (Exception e) {
							Bio7Dialog.message("Wrong format!\nOnly Excel *.xlsx (2007-2010) is supported!");
							canRead = false;
							e.printStackTrace();
						}

						//wb = new HSSFWorkbook(inp);
					
						
					
				} catch (IOException ex) {

					ex.printStackTrace();
				}
				if (canRead) {
					for (s = 0; s < wb.getNumberOfSheets(); s++) {
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.syncExec(new Runnable() {

							public void run() {
								String name = fil.getName();
								grid = new Spread().spread(RTable.getTabFolder(), 0, 0, name);
								RTable.setGrid(grid);

								 XSSFSheet sheet  = wb.getSheetAt(s);

								int colCount = grid.getColumnCount();
								int rowCount = grid.getItemCount();
								int exelRow = endOfRow(sheet);
								int exelColumn = endOfColumn(sheet);
								// System.out.println(exelRow + " " + exelColumn
								// + "---" + sheet.getPhysicalNumberOfRows() +
								// " " +
								// sheet.getRow(0).getPhysicalNumberOfCells());
								if (colCount < exelColumn) {
									int diff = exelColumn - colCount;
									for (int i = 0; i < diff; i++) {
										GridColumn column = new GridColumn(grid, SWT.NONE);
										column.setText("C " + (i + 1 + colCount));
										column.setWidth(50);
									}
								}
								if (rowCount < exelRow) {
									int diff = exelRow - rowCount;
									for (int i = 0; i < diff; i++) {
										new GridItem(grid, SWT.NONE).setHeight(16);

									}
								}

								// Iterate over each row in the sheet
								int rows = sheet.getPhysicalNumberOfRows();

								for (int i = 0; i < exelRow; i++) {
									 XSSFRow  row = sheet.getRow(i);

									if (row == null) {
										for (int u = 0; u < exelColumn; u++) {
											grid.getItem(i).setText(u, " ");
										}

									} else {
										for (int u = 0; u < exelColumn; u++) {

											 XSSFCell cell = row.getCell(u);

											if (cell != null) {

												switch (cell.getCellType()) {
												case XSSFCell.CELL_TYPE_NUMERIC:

													String val = String.valueOf(cell.getNumericCellValue());

													grid.getItem(i).setText(u, val);
													break;
												case XSSFCell.CELL_TYPE_STRING:
													XSSFRichTextString st = cell.getRichStringCellValue();
													String val2 = st.getString();
													grid.getItem(i).setText(u, val2);
													break;
												case XSSFCell.CELL_TYPE_FORMULA:

													try {
														String val3 = String.valueOf(cell.getRawValue());
														grid.getItem(i).setText(u, val3);
													} catch (Exception e) {
														//System.out.println(e.getMessage());
														String s2 = cell.getCellFormula();
														grid.getItem(i).setText(u, s2);
													}

													break;
												case XSSFCell.CELL_TYPE_BLANK:

													grid.getItem(i).setText(u, " ");
													break;
												case XSSFCell.CELL_TYPE_BOOLEAN:
													boolean s4 = cell.getBooleanCellValue();
													if (s4) {
														grid.getItem(i).setText(u, "TRUE");
													} else {
														grid.getItem(i).setText(u, "FALSE");
													}
													break;
												default:

													break;
												}

											} else {
												grid.getItem(i).setText(u, " ");
											}

										}

									}
								}
							}
						});

					}

					wb = null;

				}
			}
		} else {
			Bio7Dialog.message("File not found!");
		}
	}

	public static int endOfRow( XSSFSheet  sheet) {

		int lastRowNum = sheet.getLastRowNum();

		if (lastRowNum > 0) {
			return (lastRowNum + 1);
		} else {
			return sheet.getPhysicalNumberOfRows() > 0 ? 1 : 0;
		}

	}

	public static int endOfColumn( XSSFSheet sheet) {
		int rowCount = endOfRow(sheet);
		int maxCellNum = 0;
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			 XSSFRow row = sheet.getRow(rowIndex);
			if (row != null) {
				maxCellNum = Math.max(maxCellNum, row.getLastCellNum());
			}

		}

		return maxCellNum;
	}

}
