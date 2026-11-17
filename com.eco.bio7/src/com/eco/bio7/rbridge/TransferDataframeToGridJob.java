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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.ui.progress.UIJob;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

public class TransferDataframeToGridJob extends UIJob {

	private boolean head;
	private String name;

	public TransferDataframeToGridJob(boolean head, String name) {
		super("Transfer Dataframe");
		this.head = head;
		this.name = name;

	}

	public IStatus runInUIThread(IProgressMonitor monitor) {
		monitor.beginTask("Transfer Dataframe", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}

		transferWithHead(head, name);
		monitor.done();
		return Status.OK_STATUS;
	}

	public static void transferWithHead(boolean head, String name) {
		RBackend c = RBridge.getBackend();
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean format = store.getBoolean("TRANSFER_METHOD");
		int digits = store.getInt("DEFAULT_DIGITS");

		String[] matrix = null;
		boolean withHead = head;
		int lengthY = 0;
		int lengthX = 0;

		try {
			lengthY = c.eval("try(length(" + name + "[,1]))").firstInt();
			lengthX = c.eval("try(length(" + name + "[1,]))").firstInt();

		} catch (RTypeException e) {

			e.printStackTrace();
		} catch (RException e) {

			e.printStackTrace();
		}
		Grid grid = RTable.getGrid();

		grid = new Spread().spread(RTable.getTabFolder(), lengthX, lengthY, name);
		RTable.setGrid(grid);
		if (grid != null) {

			int colCount = grid.getColumnCount();
			int rowCount = grid.getItemCount();

			if (colCount < lengthX) {
				int diff = lengthX - colCount;
				for (int i = 0; i < diff; i++) {
					GridColumn column = new GridColumn(grid, SWT.NONE);
					column.setText(Bio7Grid.columnName(i + colCount));
					column.setWidth(50);
				}
			}

			if (rowCount < lengthY) {
				int diff = lengthY - rowCount;
				for (int i = 0; i < diff; i++) {
					new GridItem(grid, SWT.NONE);

				}
			}
			if (withHead == false) {
				for (int j = 0; j < lengthX; j++) {
					try {
						// Get the columns!
						if (format) {
							matrix = c.eval("try(format(" + name + "[," + (j + 1) + "],trim = TRUE,digits=" + digits + "))").strings();
						} else {
							/* A numeric column with full precision (as.character writes 15 digits) */
							matrix = RNumbers.texts(c, name + "[," + (j + 1) + "]");
							if (matrix == null) {
								matrix = c.eval("try(as.character(" + name + "[," + (j + 1) + "]))").strings();
							}
						}
					} catch (RTypeException e) {

						e.printStackTrace();
					} catch (RException e) {

						e.printStackTrace();
					}
					String value;
					for (int k = 0; k < lengthY; k++) {
						if (matrix[k] == null) {
							value = "NA";
						} else {
							value = matrix[k].toString();
						}

						grid.getItem(k).setText(j, value);

					}
				}
			} else {
				String[] names = null;
				try {
					names = c.eval("try(names(" + name + "))").strings();
				} catch (RTypeException e1) {

					e1.printStackTrace();
				} catch (RException e1) {

					e1.printStackTrace();
				}
				// Add additional item for the head!
				new GridItem(grid, SWT.NONE);

				for (int j = 0; j < lengthX; j++) {

					try {
						// Get the columns!
						if (format) {
							matrix = c.eval("try(format(" + name + "[," + (j + 1) + "],trim = TRUE,digits=" + digits + "))").strings();
						} else {
							/* A numeric column with full precision (as.character writes 15 digits) */
							matrix = RNumbers.texts(c, name + "[," + (j + 1) + "]");
							if (matrix == null) {
								matrix = c.eval("try(as.character(" + name + "[," + (j + 1) + "]))").strings();
							}
						}
					} catch (RTypeException e) {

						e.printStackTrace();
					} catch (RException e) {

						e.printStackTrace();
					}
					String value;
					for (int k = 0; k < lengthY + 1; k++) {
						if (k > 0) {

							if (matrix[k-1] == null) {
								value = "NA";
							} else {

								value = matrix[k - 1].toString();
							}

							grid.getItem(k).setText(j, value);
						} else {
							grid.getItem(k).setText(j, names[j]);
						}

					}
				}

			}
		}
	}

}
