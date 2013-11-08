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

/*package com.eco.bio7.rbridge;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.jopendocument.dom.spreadsheet.CellStyle;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import com.eco.bio7.batch.Bio7Dialog;

public class LoadOpenOfficeJob extends WorkspaceJob {

	protected File file;
	protected Grid grid;
	private Sheet sheet;
	private Color co;
	private Color colo;
	private SpreadSheet sh;
	private String name;

	public LoadOpenOfficeJob(File file) {
		super("Load OpenOffice");
		this.file = file;

	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Load OpenOffice", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}

		openOO(file);
		monitor.done();
		return Status.OK_STATUS;
	}

	private void openOO(File fil) {

		name = fil.getName();

		try {
			sh = SpreadSheet.createFromFile(fil);
		} catch (IOException e2) {
			Bio7Dialog.message("Wrong format!");
			e2.printStackTrace();
		}
		int count = sh.getSheetCount();
		for (int u = 0; u < count; u++) {

			try {
				sheet = sh.createFromFile(fil).getSheet(u);

				name = "Sheet " + (u + 1);

			} catch (IOException e) {

				e.printStackTrace();
			}

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {

					grid = new Spread().spread(RTable.getTabFolder(), sheet.getColumnCount(), sheet.getRowCount(), name);
					RTable.setGrid(grid);

					for (int i = 0; i < sheet.getColumnCount(); i++) {

						for (int j = 0; j < sheet.getRowCount(); j++) {

							String val = sheet.getCellAt(i, j).getValue().toString();
							CellStyle st = sheet.getCellAt(i, j).getStyle();
							if (st != null) {
								co = st.getBackgroundColor();
								if (co != null) {
									
										colo = co;
									

								}
							}
							// PaletteData palette = new PaletteData();
							if (colo != null) {
								grid.getItem(j).setBackground(i, new org.eclipse.swt.graphics.Color(Display.getCurrent(), new RGB(colo.getRed(), colo.getGreen(), colo.getBlue())));
							}
							grid.getItem(j).setText(i, val);
							co = null;
							colo = null;

						}
					}
					sheet = null;
				}
			});
		}
	}

}
*/