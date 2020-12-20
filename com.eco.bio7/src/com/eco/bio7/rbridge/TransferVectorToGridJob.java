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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.ui.progress.UIJob;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.batch.Bio7Dialog;

public class TransferVectorToGridJob extends UIJob {

	private String name;
	private REXPLogical isNumeric;
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
		RConnection c = RServe.getConnection();

		int length = 0;

		try {
			length = c.eval("try(length(" + name + "))").asInteger();

		} catch (REXPMismatchException e) {

			e.printStackTrace();
		} catch (RserveException e) {

			e.printStackTrace();
		}
		Grid grid = RTable.getGrid();
		int tabs = RTable.getTabFolder().getItemCount();

		if (tabs > 0) {

			int colCount = grid.getColumnCount();
			int rowCount = grid.getItemCount();

			int plusC = Spread.getSelectedColumnMin();
			int plusR = Spread.getSelectedRowMin();

			try {
				isNumeric = (REXPLogical) c.eval("try(is.numeric(" + name + "))");
			} catch (RserveException e) {

				e.printStackTrace();
			}

			boolean[] bolNumeric = isNumeric.isTRUE();

			if (bolNumeric[0]) {// Must be numeric!

				double[] vector = null;
				try {
					vector = (double[]) c.eval("try(" + name + ")").asDoubles();
				} catch (REXPMismatchException eve) {

					eve.printStackTrace();
				} catch (RserveException ex) {
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
							Bio7Grid.createColumn(j, 50, "C" + (j + plusC + 1));
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
					vector = (String[]) c.eval("try(" + name + ")").asStrings();
				} catch (REXPMismatchException eve) {

					eve.printStackTrace();
				} catch (RserveException ex) {
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
							Bio7Grid.createColumn(j, 50, "C" + (j + plusC + 1));
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
