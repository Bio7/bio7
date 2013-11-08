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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import au.com.bytecode.opencsv.CSVReader;
import com.eco.bio7.batch.Bio7Dialog;

public class LoadCsvJob extends WorkspaceJob {
	private String[] readLineNext;
	private int count;
	private int sizey;
	private int sizex;
	private FileReader fi;
	private CSVReader reader;
	private String fileToRead;
	protected char c;
	protected char c2;
	protected int sel;
	protected String name;
	protected Grid grid;
	private File fil;

	public LoadCsvJob(String fileToRead_, char c_, char c_2, int sel_, String name_) {
		super("Csv");
		this.fileToRead = fileToRead_;
		this.sel = sel_;
		this.c = c_;
		this.c2 = c_2;
		this.name = name_;

	}

	private void readTable() {
		fil = new File(fileToRead);
		if (fil.exists()) {
			try {
				fi = new FileReader(fileToRead);
			} catch (FileNotFoundException e2) {

				e2.printStackTrace();
			}

			reader = new CSVReader(fi, c, c2, sel);

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

			/* Now Load the File definetely ! */
			try {
				fi = new FileReader(fileToRead);
			} catch (FileNotFoundException e2) {

				e2.printStackTrace();
			}
		} else {
			Bio7Dialog.message("File not found!");
		}

	}

	private void loadData() {

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

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Load Csv", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}
		readTable();
		if (fil.exists()) {
			reader = new CSVReader(fi, c, c2, sel);
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					grid = new Spread().spread(RTable.getTabFolder(), sizex, sizey, name);
					RTable.setGrid(grid);

					loadData();
				}
			});

			monitor.done();
		}
		return Status.OK_STATUS;

	}

}
