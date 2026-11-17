/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.jobs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rffm.RException;

public class LineSelectionJob extends WorkspaceJob {

	/* Evaluates R expressions from the console of Bio7 ! */
	public LineSelectionJob() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Evaluate Line Selection", IProgressMonitor.UNKNOWN);
		if (RBridge.getBackend() != null) {
			final RBackend cscript = RBridge.getBackend();
			/* If the R expression is evaluated with the line selection! */
			/*if (Bio7Plugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.R_START_SHELL)) {
				
					try {
						cscript.eval("try(f <- file(\"clipboard\", open=\"r\"))");
					
					cscript.eval("try(source(f, echo=T))");
					cscript.eval("try(close(f))");
					cscript.eval("try(rm(f))");
					} catch (RException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				

			} else {*/

				
					try {
						cscript.eval("try(f <- file(\"clipboard\", open=\"r\"))");
					
					cscript.eval("try(source(f, echo=T))");
					cscript.eval("try(close(f))");
					cscript.eval("try(rm(f))");
					//writeToConsole();
					//cscript.voidEval("try(readLines(con = stdin()))");
					} catch (RException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			//}

		} else {

			RTools.isAliveDialog();

		}

		return Status.OK_STATUS;
	}

	public static void writeOutput() {
		
			try {
				RBridge.getBackend().voidEval("try(readLines(con = stdin()))");
			} catch (RException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		

	}

}
