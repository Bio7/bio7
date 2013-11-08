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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.apache.poi.xssf.usermodel.*;

public class SaveXlsxExcelJob extends WorkspaceJob {

	protected String file;
	protected Grid grid;
	XSSFRow rowa;
	XSSFCell cella;
	private XSSFSheet sheet;

	public SaveXlsxExcelJob(String file, Grid grid) {
		super("Save as Excel 2007 (*.xlsx)");
		this.file = file;
		this.grid = grid;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Save Excel", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}

		saveExcel(file);
		monitor.done();
		return Status.OK_STATUS;

	}

	private void saveExcel(String save) {
		XSSFWorkbook wb = new XSSFWorkbook(); // or new HSSFWorkbook();
		sheet = wb.createSheet("Sheet1");

		if (grid != null) {

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					for (int j = 0; j < grid.getItemCount(); j++) {
						rowa = sheet.createRow(j);
						
						for (int k = 0; k < grid.getColumnCount(); k++) {

							cella = rowa.createCell(k);
							String s = grid.getItem(j).getText(k);
							//Font f=grid.getItem(j).getFont();
							
							try {
								double d = Double.parseDouble(s);
								cella.setCellType(XSSFCell.CELL_TYPE_NUMERIC);
								cella.setCellValue(d);

							} catch (NumberFormatException e1) {

								cella.setCellType(XSSFCell.CELL_TYPE_STRING);
								cella.setCellValue(s);

							}

						}
					}
				}
			});

		}
		FileOutputStream fileOut;

		try {
			fileOut = new FileOutputStream(save);
			try {
				wb.write(fileOut);
				fileOut.close();
			} catch (IOException e1) {

				e1.printStackTrace();
			}

		} catch (FileNotFoundException e1) {

			e1.printStackTrace();
		}
		wb = null;
		sheet = null;

	}

}
