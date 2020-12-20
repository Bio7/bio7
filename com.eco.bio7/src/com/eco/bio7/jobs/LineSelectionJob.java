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

package com.eco.bio7.jobs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RConnectionJob;
import com.eco.bio7.rbridge.RServe;

public class LineSelectionJob extends WorkspaceJob {

	/* Evaluates R expressions from the console of Bio7 ! */
	public LineSelectionJob() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Evaluate Line Selection", IProgressMonitor.UNKNOWN);
		if (RServe.getConnection() != null) {
			final RConnection cscript = RServe.getConnection();
			/* If the R expression is evaluated with the line selection! */
			/*if (Bio7Plugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.R_START_SHELL)) {
				
					try {
						cscript.eval("try(f <- file(\"clipboard\", open=\"r\"))");
					
					cscript.eval("try(source(f, echo=T))");
					cscript.eval("try(close(f))");
					cscript.eval("try(rm(f))");
					} catch (RserveException e) {
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
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			//}

		} else {

			RServe.isAliveDialog();

		}

		return Status.OK_STATUS;
	}

	public static void writeOutput() {
		writeToConsole();
		
			try {
				RServe.getConnection().voidEval("try(readLines(con = stdin()))");
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		writeToConsole();

	}

	public static void writeToConsole() {
		new Thread() {
			public void run() {
				Process p = RConnectionJob.getProc();
				// Write to the output!
				final OutputStream os = p.getOutputStream();
				final OutputStreamWriter osw = new OutputStreamWriter(os);
				final BufferedWriter bw = new BufferedWriter(osw, 100);

				try {
					bw.write("\n");

					bw.close();
				} catch (IOException e) {
					System.err.println("");
				}

			}
		}.start();

	}

}
