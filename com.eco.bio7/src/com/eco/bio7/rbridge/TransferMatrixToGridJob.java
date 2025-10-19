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
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.batch.Bio7Dialog;

public class TransferMatrixToGridJob extends UIJob {

	private String name;
	private REXPLogical isNumeric;

	public TransferMatrixToGridJob(String name) {
		super("Transfer Matrix");
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

		transferMatrixToGrid();
		monitor.done();
		return Status.OK_STATUS;
	}

	private void transferMatrixToGrid() {
		RConnection c = RServe.getConnection();
		try {
			isNumeric = (REXPLogical) c.eval("try(is.numeric(" + name + "))");
		} catch (RserveException e) {

			e.printStackTrace();
		}

		boolean[] bolNumeric = isNumeric.isTRUE();

		if (bolNumeric[0]) {// Must be numeric!
			double[][] matrix = null;
			int lengthY = 0;
			int lengthX = 0;

			try {
				lengthY = c.eval("try(length(" + name + "[,1]))").asInteger();
				lengthX = c.eval("try(length(" + name + "[1,]))").asInteger();

			} catch (REXPMismatchException e) {

				e.printStackTrace();
			} catch (RserveException e) {

				e.printStackTrace();
			}
			Grid grid = RTable.getGrid();
			grid = new Spread().spread(RTable.getTabFolder(), lengthX, lengthY, name);
			RTable.setGrid(grid);
			if (grid != null) {

				int colCount = grid.getColumnCount();
				int rowCount = grid.getItemCount();

				try {
					matrix = c.eval("try(" + name + ")").asDoubleMatrix();
				} catch (REXPMismatchException eve) {

					eve.printStackTrace();
				} catch (RserveException ex) {
				}

				if (colCount < matrix[0].length) {
					int diff = matrix[0].length - colCount;
					for (int i = 0; i < diff; i++) {
						GridColumn column = new GridColumn(grid, SWT.NONE);
						column.setText("C " + (i + 1 + colCount));
						column.setWidth(50);
					}
				}
				if (rowCount < matrix.length) {
					int diff = matrix.length - rowCount;
					for (int i = 0; i < diff; i++) {
						new GridItem(grid, SWT.NONE);

					}
				}

				for (int j = 0; j < matrix.length; j++) {

					for (int k = 0; k < matrix[0].length; k++) {

						String value = String.valueOf(matrix[j][k]);
						grid.getItem(j).setText(k, value);

					}
				}
			}
		} else {
			Bio7Dialog.message("Only numeric values allowed!");
		}

	}

}
