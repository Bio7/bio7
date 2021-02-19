/*******************************************************************************
 * Copyright (c) 2007-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class UpdateREnvironmentTableJob extends WorkspaceJob {

	private String packageItem;
	private Table table;
	String[] packageLsList = null;
	String[] packageLsfList = null;
	String[] packageDataList = null;
	String[] packageDataListDescription = null;
	private boolean func;

	// protected String[] itemsSpatial;

	public UpdateREnvironmentTableJob(String packageItem, Table table, boolean func) {
		super("Update packages...");
		this.packageItem = packageItem;
		this.table = table;
		this.func = func;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Update Environment Table..", IProgressMonitor.UNKNOWN);
		if (packageItem.equals("Attached:")) {
			return Status.OK_STATUS;
		}
		if (RServe.isAlive()) {
			RConnection c = RServe.getConnection();

			try {
				if (func) {

					
					packageLsfList = c.eval("try(lsf.str(\"package:" + packageItem + "\"))").asStrings();
					//packageLsList = c.eval("try(gsub(\" \\nNULL\", \"\",as.character(lapply(as.list(lsf.str(\"package:base\")),args))))").asStrings();
				} else {

					packageDataList = c.eval("try(data(package=\"" + packageItem + "\")$results[,\"Item\"])")
							.asStrings();
					packageDataListDescription = c
							.eval("try(data(package=\"" + packageItem + "\")$results[,\"Title\"])").asStrings();
				}
			} catch (RserveException | REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					table.setVisible(false);
					if (table != null) {
						if (table.isDisposed() == false) {
							table.removeAll();
							if (func) {
								TableColumn colTitle = table.getColumn(0);
								colTitle.setText("Function");
								TableColumn colDescr = table.getColumn(1);
								colDescr.setText("");
								for (int i = 0; i < packageLsfList.length; i++) {
									TableItem it = new TableItem(table, SWT.NONE);
									it.setText(0, packageLsfList[i]);
									it.setText(1, "Function");
								}

							} else {
								TableColumn colTitle = table.getColumn(0);
								colTitle.setText("Dataset");
								TableColumn colDescr = table.getColumn(1);
								colDescr.setText("Dataset Description");
								for (int i = 0; i < packageDataList.length; i++) {
									TableItem it = new TableItem(table, SWT.NONE);
									it.setText(0, packageDataList[i]);
									it.setText(1, packageDataListDescription[i]);
								}
							}
						}

					}
					table.setVisible(true);
				}
			});

		}
		return Status.OK_STATUS;
	}

}
