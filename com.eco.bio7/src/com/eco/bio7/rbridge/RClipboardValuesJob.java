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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.util.Util;

public class RClipboardValuesJob extends WorkspaceJob {

	public RClipboardValuesJob() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Transfer Values from Clipboard", IProgressMonitor.UNKNOWN);
		/*
		 * Get Data from OpenOffice, Excel, etc. from the clipboard if available
		 * !
		 */

		if (RServe.isAliveDialog()) {
			RConnection con = RServe.getConnection();
			try {

				if (Util.getOS().equals("Mac")) {
					
					con.eval("try(clip <- read.table(pipe(\"pbpaste\"), sep=\"\t\", header=T))");
				} else {
					con.eval("try(clip<-read.delim(\"clipboard\"))");
				}

			} catch (RserveException e) {

				e.printStackTrace();
			}

			System.out.println("Data in dataframe \"clip\":");

			RServe.print("clip");
		}
		return Status.OK_STATUS;
	}

}
