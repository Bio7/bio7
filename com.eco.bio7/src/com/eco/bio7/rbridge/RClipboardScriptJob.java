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
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.console.Console;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.util.Util;

public class RClipboardScriptJob extends WorkspaceJob {

	/* Evaluates R expressions from the clipboard! */
	public RClipboardScriptJob() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Evaluate Clipboard", IProgressMonitor.UNKNOWN);
		RConnection con = RServe.getConnection();
		if (con != null) {

			try {

				if (Util.getOS().equals("Mac")) {

					con.eval("try(f <- paste(scan(pipe(\"pbpaste\"),what=character())))");
				} else {
					con.eval("try(f <- file(\"clipboard\", open=\"r\"))");
				}


				String rout = null;
				try {
					rout = con.eval("try(paste(capture.output(source(f,echo=T)),collapse=\"\\n\"))").asString();
				} catch (REXPMismatchException e) {

					e.printStackTrace();
				}
				Console console = StartBio7Utils.getConsoleInstance().cons;
				console.println(rout);
				if (RShellView.isConsoleExpanded()) {
					RShellView.setTextConsole(rout);
				}

				con.eval("try(close(f))");
				con.eval("try(rm(f))");
			} catch (RserveException e) {

				e.printStackTrace();
			}

		} else {

			RServe.isAliveDialog();

		}

		return Status.OK_STATUS;
	}

}
