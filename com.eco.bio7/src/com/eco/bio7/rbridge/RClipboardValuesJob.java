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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.rosuda.REngine.Rserve.RserveException;


public class RClipboardValuesJob extends WorkspaceJob {

	
	public RClipboardValuesJob() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Transfer Values from Clipboard", IProgressMonitor.UNKNOWN);
		/* Get Data from OpenOffice, Excel, etc. from the clipboard if available ! */

		if (RServe.isAliveDialog()) {
			
				try {
					RServe.getConnection().eval("try(clip<-read.delim(\"clipboard\"))");
				} catch (RserveException e) {
					
					e.printStackTrace();
				}
			
			System.out.println("Data in dataframe \"clip\":");
			
			RServe.print("clip");
		} 
		return Status.OK_STATUS;
	}

}
