/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;

/**
 * Imports a spreadsheet of LibreOffice (*.ods): each sheet of the file is a new
 * sheet of the Bio7 table (see OdsFile).
 */
public class LoadOdsJob extends WorkspaceJob {

	private final String file;

	public LoadOdsJob(String file) {
		super("Import LibreOffice (ODS)");
		this.file = file;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Import LibreOffice (ODS)", IProgressMonitor.UNKNOWN);
		try {
			File f = new File(file);
			if (!f.exists()) {
				Bio7Dialog.message("File not found!");
				return Status.OK_STATUS;
			}
			List<OdsFile.Sheet> sheets;
			try {
				sheets = OdsFile.read(f);
			} catch (IOException e) {
				Bio7Dialog.message("The file can't be read as a LibreOffice spreadsheet (*.ods):\n" + e.getMessage());
				return Status.OK_STATUS;
			}
			Display display = PlatformUI.getWorkbench().getDisplay();
			for (OdsFile.Sheet sheet : sheets) {
				if (monitor.isCanceled()) {
					break;
				}
				display.syncExec(() -> {
					if (RTable.getTabFolder() == null) {
						return;
					}
					String[][] cells = sheet.cells();
					int rows = Math.max(1, cells.length), columns = Math.max(1, cells.length > 0 ? cells[0].length : 0);
					Grid grid = new Spread().spread(RTable.getTabFolder(), columns, rows, sheet.name());
					RTable.setGrid(grid);
					for (int i = 0; i < cells.length; i++) {
						for (int j = 0; j < cells[i].length; j++) {
							grid.getItem(i).setText(j, cells[i][j]);
						}
					}
				});
			}
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}
