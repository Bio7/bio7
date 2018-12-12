/*******************************************************************************
 * Copyright (c) 2004-2018 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.util.Util;

public class SaveExcelJob extends WorkspaceJob {

	protected String file;
	private HSSFRow rowa;
	private HSSFCell cella;
	private HSSFSheet sheet;
	private HSSFWorkbook wb;
	private HashMap<Integer, String> hash;

	public SaveExcelJob(String file) {
		super("Save Excel");
		this.file = file;
		hash = new HashMap<Integer, String>();
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
		wb = new HSSFWorkbook(); // or new HSSFWorkbook();

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				CTabFolder tabFolder = RTable.getTabFolder();
				int tabCount = tabFolder.getItemCount();
				for (int i = 0; i < tabCount; i++) {
					String tabName = tabFolder.getItem(i).getText();
					if (hash.containsValue(tabName)) {
						String id = UUID.randomUUID().toString();
						sheet = wb.createSheet(tabName + "_" + id);

					} else {
						sheet = wb.createSheet(tabName);
						hash.put(i, tabName);
					}

					final CTabItem[] items = tabFolder.getItems();
					Composite comp = (Composite) items[i].getControl();
					Grid grid = (Grid) comp.getChildren()[0];

					if (grid != null) {
						for (int j = 0; j < grid.getItemCount(); j++) {
							rowa = sheet.createRow(j);

							for (int k = 0; k < grid.getColumnCount(); k++) {

								cella = rowa.createCell(k);
								String s = grid.getItem(j).getText(k);
								Image im = grid.getItem(j).getImage(k);
								if (im != null) {

									BufferedImage buf = Util.convertToAWT(im.getImageData());
									ByteArrayOutputStream baos = new ByteArrayOutputStream();
									try {
										ImageIO.write(buf, "png", baos);
									} catch (IOException e) {
										e.printStackTrace();
									}
									byte[] bytes = baos.toByteArray();

									// Adds a image to the workbook
									int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);

									CreationHelper helper = wb.getCreationHelper();

									Drawing<?> drawing = sheet.createDrawingPatriarch();

									ClientAnchor anchor = helper.createClientAnchor();

									anchor.setCol1(k); // Column B
									anchor.setRow1(j); // Row 3
									anchor.setCol2(k + 1); // Column C
									anchor.setRow2(j + 1); // Row 4

									// Creates an image
									drawing.createPicture(anchor, pictureIdx);
									sheet.setColumnWidth(k, buf.getWidth() * 36);
									cella.getRow().setHeight((short) (buf.getHeight() * 15));

								}

								try {
									double d = Double.parseDouble(s);
									cella.setCellType(CellType.NUMERIC);
									cella.setCellValue(d);

								} catch (NumberFormatException e1) {

									cella.setCellType(CellType.STRING);
									cella.setCellValue(s);

								}

							}

						}
					}

				}

			}
		});

		FileOutputStream fileOut;

		try {
			fileOut = new FileOutputStream(save);
			try {
				wb.write(fileOut);
				fileOut.close();
				wb.close();
			} catch (IOException e1) {

				e1.printStackTrace();
			}

		} catch (FileNotFoundException e1) {

			Bio7Dialog.message("Cannot write file. Probably\n opened by another application!");

			// e1.printStackTrace();
		}

		sheet = null;
        hash.clear();
		wb = null;

	}
}
