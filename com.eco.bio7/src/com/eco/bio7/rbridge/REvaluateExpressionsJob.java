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
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rcp.StartBio7Utils;

public class REvaluateExpressionsJob extends WorkspaceJob {

	private String[] expressionArray;

	/* Evaluates R expressions from the console of Bio7 ! */
	public REvaluateExpressionsJob(String[] toprint) {
		super("Job");
		this.expressionArray = toprint;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Evaluate Expression", IProgressMonitor.UNKNOWN);

		for (int i = 0; i < expressionArray.length; i++) {
			if (expressionArray[i].isEmpty() == false) {
				evaluate(i);
				System.out.println();
			}
		}

		return Status.OK_STATUS;
	}

	private void evaluate(int i) {
		String trybegin = "try(";
		String tryend = ")";

		String out = null;
		try {
			out = RServe.getConnection().eval("paste(capture.output(print(" + trybegin + trybegin + "(" + expressionArray[i] + ")" + tryend + tryend + ")),collapse=\"\\n\")").asString();

		} catch (REXPMismatchException e) {

			e.printStackTrace();
		} catch (RserveException e) {
			System.out.println("Error : " + e.getMessage());
		}
		RServe.setRout(out);

		//

		/* Avoid the printing of an empty object - 'NULL'! */
		if (out != null && out.equals("NULL") == false) {
			StartBio7Utils.getConsoleInstance().cons.println(out);
			/* Send also the output to the R console view! */
			if (RShellView.isConsoleExpanded()) {

				RShellView.setTextConsole(out);

			}
		}

		out = null;
	}

}
