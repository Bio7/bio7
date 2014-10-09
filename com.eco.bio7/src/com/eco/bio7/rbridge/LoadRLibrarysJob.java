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
/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rpreferences.template.RCompletionProcessor;


public class LoadRLibrarysJob extends WorkspaceJob {

	protected String[] items;

	public LoadRLibrarysJob() {
		super("Load...");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Load Libraries..", IProgressMonitor.UNKNOWN);
		if (RServe.isAlive()) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					items = RLibraryList.getAllPackagesList().getSelection();
				}
			});

			for (int i = 0; i < items.length; i++) {
				RConnection c = RServe.getConnection();
				
					try {
						c.eval("try(library(" + items[i] + "))");
						
						/*Function loaded at Rserve startup!*/
						c.eval("writeFunctionDef();");
					} catch (RserveException e) {
						
						e.printStackTrace();
					}
					System.out.println("Loaded library "+items[i]);
				
			}		
			/*Rewrite the code templates for the R editor!*/
			
			
			
			
			RCompletionProcessor.loadRCodePackageTemplates(false);
		}

		return Status.OK_STATUS;
	}
	

}
