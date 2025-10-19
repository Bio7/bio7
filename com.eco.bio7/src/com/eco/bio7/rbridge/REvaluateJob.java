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
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.StartBio7Utils;

public class REvaluateJob extends WorkspaceJob {

	private String toprint;

	/* Evaluates R expressions from the console of Bio7 ! */
	public REvaluateJob(String toprint) {
		super("Job");
		this.toprint = toprint;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Evaluate Expression", IProgressMonitor.UNKNOWN);
		String trybegin = "tryCatch({";
		String tryend = "},error = function(e) {message(paste(\"Error: \",e$message))})";

		String out = null;
		try {
			out = RServe.getConnection().eval("paste(capture.output(print(" + trybegin  + "(" + toprint + ")"  + tryend + ")),collapse=\"\\n\")").asString();

		} catch (REXPMismatchException e) {

			e.printStackTrace();
		} catch (RserveException e) {
			System.out.println("Error : " + e.getMessage());
		}
		RServe.setRout(out);

		/* Avoid the printing of an empty object - 'NULL'! */
		if (out != null && out.equals("NULL") == false) {
			StartBio7Utils.getConsoleInstance().cons.println(out);
			/* Send also the output to the R console view! */
			if (RShellView.isConsoleExpanded()) {

				RShellView.setTextConsole(out);

			}
		}

		out = null;
		return Status.OK_STATUS;
	}

}
