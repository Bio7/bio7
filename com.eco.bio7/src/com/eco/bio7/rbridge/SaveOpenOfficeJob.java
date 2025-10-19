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

/*package com.eco.bio7.rbridge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

public class SaveOpenOfficeJob extends WorkspaceJob {

	protected String file;
	protected Grid grid;
	protected Object[][] data;
	protected Object[][] data2;

	public SaveOpenOfficeJob(String file, Grid grid) {
		super("Save OpenOffice");
		this.file = file;
		this.grid = grid;

	}
	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Save OpenOffice", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}
		saveOpenOffice(file);

		monitor.done();
		return Status.OK_STATUS;
		
	}


	public IStatus runInUIThread(IProgressMonitor monitor) {
		return null;
	}

	private void saveOpenOffice(final String file) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
		 data = new Object[grid.getItemCount()][grid.getColumnCount()];
		
		 * The next array is only the data without the header which we have to
		 * copy and then store!
		 
		data2 = new Object[grid.getItemCount() - 1][grid.getColumnCount()];
		 Don't include the header! 
		for (int i = 0; i < grid.getItemCount(); i++) {
			for (int j = 0; j < grid.getColumnCount(); j++) {
				String value = grid.getItem(i).getText(j);
				data[i][j] = value;
				if (i > 0) {
					data2[i - 1][j] = data[i][j];
				}
			}
		}
			}
		});
		 The header separately! 
		Object[] head = data[0];

		TableModel model = new DefaultTableModel(data2, head);
		SpreadSheet sh;
		try {
			 sh = SpreadSheet.createEmpty(model);

			sh.saveAs(new File(file));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		model=null;
		sh=null;
		head=null;
		data = null;
		data2 = null;
	}

	
}
*/