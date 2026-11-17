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
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.ui.progress.UIJob;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rffm.RValue;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

public class TransferVectorToGridJob extends UIJob {

	private String name;
	private RValue isNumeric;
	private boolean row;

	public TransferVectorToGridJob(String name, boolean row) {
		super("Transfer Vector");
		this.name = name;
		this.row = row;
	}

	public IStatus runInUIThread(IProgressMonitor monitor) {
		monitor.beginTask("Transfer Vector", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}

		transferVectorToGrid();
		monitor.done();
		return Status.OK_STATUS;
	}

	private void transferVectorToGrid() {
		RBackend c = RBridge.getBackend();

		int length = 0;

		try {
			length = c.eval("try(length(" + name + "))").firstInt();

		} catch (RTypeException e) {

			e.printStackTrace();
		} catch (RException e) {

			e.printStackTrace();
		}
		Grid grid = RTable.getGrid();
		int tabs = RTable.getTabFolder().getItemCount();

		if (tabs > 0) {
			/* The vector is written into the selected sheet: not undoable, the history is cleared! */
			GridHistory.clear(grid);

			int colCount = grid.getColumnCount();
			int rowCount = grid.getItemCount();

			int plusC = Spread.getSelectedColumnMin();
			int plusR = Spread.getSelectedRowMin();

			try {
				isNumeric = c.eval("try(is.numeric(" + name + "))");
			} catch (RException e) {

				e.printStackTrace();
			}

			boolean[] bolNumeric = isNumeric.booleans();

			if (bolNumeric[0]) {// Must be numeric!

				double[] vector = null;
				try {
					vector = (double[]) c.eval("try(" + name + ")").doubles();
				} catch (RTypeException eve) {

					eve.printStackTrace();
				} catch (RException ex) {
				}

				if (row) {

					for (int j = 0; j < vector.length; j++) {

						if (j < rowCount - plusR) {
							String value = String.valueOf(vector[j]);
							grid.getItem(j + plusR).setText(0 + plusC, value);
						}

						else {
							/* Add rows if we exceed the size! */
							Bio7Grid.createRow(j + plusR, grid.getItemHeight());
							String value = String.valueOf(vector[j]);
							grid.getItem(j + plusR).setText(0 + plusC, value);
						}

					}
				} else {
					for (int j = 0; j < vector.length; j++) {

						if (j < colCount - plusC) {
							String value = String.valueOf(vector[j]);
							grid.getItem(0 + plusR).setText(j + plusC, value);
						}

						else {
							/* Add columns if we exceed the size! */
							Bio7Grid.createColumn(j, 50, Bio7Grid.columnName(j + plusC));
							String value = String.valueOf(vector[j]);
							grid.getItem(0 + plusR).setText(j + plusC, value);
						}

					}

				}
			}
			/* Character or factor! */
			else {

				String[] vector = null;
				try {
					vector = (String[]) c.eval("try(" + name + ")").strings();
				} catch (RTypeException eve) {

					eve.printStackTrace();
				} catch (RException ex) {
				}

				if (row) {

					for (int j = 0; j < vector.length; j++) {

						if (j < rowCount - plusR) {
							String value = String.valueOf(vector[j]);
							grid.getItem(j + plusR).setText(0 + plusC, value);
						}

						else {
							/* Add rows if we exceed the size! */
							Bio7Grid.createRow(j + plusR, grid.getItemHeight());
							String value = String.valueOf(vector[j]);
							grid.getItem(j + plusR).setText(0 + plusC, value);

						}

					}
				} else {
					for (int j = 0; j < vector.length; j++) {

						if (j < colCount - plusC) {
							String value = String.valueOf(vector[j]);
							grid.getItem(0 + plusR).setText(j + plusC, value);
						}
						/* Add columns if we exceed the size! */
						else {
							Bio7Grid.createColumn(j, 50, Bio7Grid.columnName(j + plusC));
							String value = String.valueOf(vector[j]);
							grid.getItem(0 + plusR).setText(j + plusC, value);
						}

					}

				}

			}
		} else {
			Bio7Dialog.message("No opened Sheet available!");
		}
	}

}
