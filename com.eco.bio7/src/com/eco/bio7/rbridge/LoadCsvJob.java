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
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.util.Util;

/**
 * Loads CSV data (a file or the clipboard) with the options of the CSV import
 * dialog into a new sheet of the table.
 */
public class LoadCsvJob extends WorkspaceJob {

	private final String fileToRead;
	private final boolean fromClip;
	/* The text of the clipboard (read in the UI thread) */
	private final String clipboardText;
	private final CsvReading.Options options;

	/**
	 * Must be called in the UI thread (the options are read from the dialog).
	 */
	public LoadCsvJob(CSVImport csvImportref, String fileToRead) {
		super("Csv");
		this.fileToRead = fileToRead;
		this.fromClip = csvImportref.isClipboard();
		this.options = csvImportref.getOptions();
		this.clipboardText = fromClip ? csvImportref.getClipboardText() : null;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Load Csv", IProgressMonitor.UNKNOWN);
		final String name;
		final List<String[]> rows;
		try {
			String text;
			if (fromClip) {
				text = clipboardText;
				name = "clipboard";
			} else {
				text = CsvReading.readText(fileToRead, options.charset, 0);
				name = new File(fileToRead).getName();
			}
			if (text == null || text.isEmpty()) {
				Bio7Dialog.message("No CSV data available!");
				return Status.OK_STATUS;
			}
			rows = CsvReading.parse(text, options, 0);
		} catch (Exception e) {
			e.printStackTrace();
			Bio7Dialog.message("The CSV data could not be read:\n" + e.getMessage());
			return Status.OK_STATUS;
		}
		if (rows.isEmpty()) {
			Bio7Dialog.message("The CSV data contains no rows (with the selected options)!");
			return Status.OK_STATUS;
		}
		int cols = 0;
		for (String[] row : rows) {
			cols = Math.max(cols, row.length);
		}
		final int sizex = cols;
		Display display = Util.getDisplay();
		/* Finally load the data into the grid GUI! */
		display.syncExec(() -> {
			Grid grid = new Spread().spread(RTable.getTabFolder(), sizex, rows.size(), name);
			RTable.setGrid(grid);
			grid.setRedraw(false);
			try {
				for (int r = 0; r < rows.size(); r++) {
					String[] row = rows.get(r);
					for (int c = 0; c < row.length; c++) {
						grid.getItem(r).setText(c, row[c]);
					}
				}
				if (options.titlesFromFirstRow) {
					String[] header = rows.get(0);
					for (int c = 0; c < header.length; c++) {
						if (!header[c].trim().isEmpty()) {
							grid.getColumn(c).setText(header[c].trim());
						}
					}
				}
			} finally {
				grid.setRedraw(true);
			}
			grid.forceFocus();
		});
		monitor.done();
		return Status.OK_STATUS;
	}

}
