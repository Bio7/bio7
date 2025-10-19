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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class SaveTextJob extends WorkspaceJob {

	protected String file;
	protected Grid grid;

	public SaveTextJob(String file, Grid grid) {
		super("Save Text");
		this.file = file;
		this.grid = grid;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Save Text", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				saveText(file, grid);
			}
		});

		monitor.done();
		return Status.OK_STATUS;
	}

	private void saveText(final String save, final Grid grid) {
		if (grid != null) {

			GridItem[] items = grid.getItems();
			final int cols = grid.getColumnCount();

			try {
				final BufferedWriter bw = new BufferedWriter(new FileWriter(save));

				for (int i = 0; i < items.length; i++) {
					final GridItem item = items[i];
					for (int j = 0; j < cols; j++) {
						String str = item.getText(j);
						bw.write(str);
						if (j < cols - 1) {
							bw.write(",");
						}
					}
					bw.newLine();
				}

				bw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

}
