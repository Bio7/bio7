/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.util.Util;
import com.opencsv.CSVReader;

public class LoadCsvJob extends WorkspaceJob {
	private String[] readLineNext;
	private int count;
	private int sizey;
	private int sizex;
	private Reader fi;
	private CSVReader reader;
	private String fileToRead;
	protected char sep;
	protected char quot;
	protected int sel;
	protected String name;
	protected Grid grid;
	private File fil;
	private boolean strictQuotes;
	private boolean ignoreWs;
	private boolean keepCr;
	private char esc;
	private boolean fromClip;
	private String plainText;
	private Clipboard clipboard;

	public LoadCsvJob(CSVImport csvImportref, String fileToRead) {
		super("Csv");
		this.fileToRead = fileToRead;
		this.fromClip = csvImportref.isClipboard();
		this.sel = csvImportref.getSelectedRange();
		this.sep = csvImportref.getSep();
		this.quot = csvImportref.getQuot();
		this.esc = csvImportref.getEsc();
		this.strictQuotes = csvImportref.getStrictQuotes();
		this.ignoreWs = csvImportref.getIgnoreWs();
		this.keepCr = csvImportref.getKeepCr();

	}
   /*A method to calculate the width and height. Reader can't be reset so we load the CSV here, too!*/
	private void readWidthHeightForTable() {
		/* Read from clipboard! */
		if (fromClip == false) {
			fil = new File(fileToRead);
			name = fil.getName();

			try {
				fi = new FileReader(fileToRead);
			} catch (FileNotFoundException e2) {

				e2.printStackTrace();
			}
		}
		/* Read from file! */
		else {
			Display display = Util.getDisplay();
			display.syncExec(new Runnable() {

				public void run() {

					clipboard = new Clipboard(display);
					plainText = (String) clipboard.getContents(TextTransfer.getInstance());
					if (plainText != null && plainText.isEmpty() == false) {
						fi = new StringReader(plainText);
						name = "clipboard";
					}
				}
			});

		}
		/* Calculate the dimensions for the grid! */
		reader = new CSVReader(fi, sep, quot, esc, sel, strictQuotes, ignoreWs, keepCr);

		sizey = 0;
		sizex = 0;
		String[] nextLine;

		try {
			while ((nextLine = reader.readNext()) != null) {

				for (int i = 0; i < nextLine.length; i++) {
					if (nextLine.length > sizex) {
						sizex = nextLine.length;
					}

				}

				sizey++;

			}
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Load Csv", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}
		
		/* Open Reader and calculate size! */
		readWidthHeightForTable();
		
		/* Now Load the File definitely. We have to reread the reader! */
		if (fromClip) {
			if (plainText != null && plainText.isEmpty() == false) {
				fi = new StringReader(plainText);
			}
		} else {
			try {
				fi = new FileReader(fileToRead);
			} catch (FileNotFoundException e2) {

				e2.printStackTrace();
			}
		}
		/*Finally load the data into the grid GUI!*/
		reader = new CSVReader(fi, sep, quot, sel);
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				grid = new Spread().spread(RTable.getTabFolder(), sizex, sizey, name);
				RTable.setGrid(grid);

				count = 0;
				try {
					while ((readLineNext = reader.readNext()) != null) {

						if (count < grid.getItemCount()) {

							for (int i = 0; i < readLineNext.length; i++) {

								if (i < readLineNext.length) {

									grid.getItem(count).setText(i, readLineNext[i]);
								}
							}

							count++;

						}

					}
				} catch (IOException e1) {

					e1.printStackTrace();
				}
			}
		});

		monitor.done();

		return Status.OK_STATUS;

	}

}
