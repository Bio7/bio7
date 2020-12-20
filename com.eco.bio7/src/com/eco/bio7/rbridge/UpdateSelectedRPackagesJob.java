/*******************************************************************************
 * Copyright (c) 2004-2019 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;

public class UpdateSelectedRPackagesJob extends WorkspaceJob {

	private String[] selectedPackages;

	// protected String[] itemsSpatial;

	public UpdateSelectedRPackagesJob(String [] selectedPackages) {
		super("Update packages...");
		this.selectedPackages=selectedPackages;
		
		
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Update Selected Packages..", IProgressMonitor.UNKNOWN);
		
		if (RServe.isAlive()) {
			RConnection c = RServe.getConnection();

			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			// String destdir = store.getString("InstallLocation");
			String server = store.getString("R_PACKAGE_SERVER");
			// if (Bio7Dialog.getOS().equals("Windows")) {
			/* If a location is given! */

			// destdir = destdir.replace("\\", "\\\\");
            /*Here we update the packages but exclude Rserve!*/
			String out = null;
			
			try {
				try {
					c.assign(".pkgSelectedToUpdate", selectedPackages);
				} catch (REngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				out = c.eval("try(paste(capture.output(update.packages(repos =\"" + server + "\",checkBuilt=TRUE, ask=FALSE,oldPkgs=.pkgSelectedToUpdate)),collapse=\"\\n\"))").asString();
				c.eval("remove(.pkgSelectedToUpdate)");
			} catch (REXPMismatchException e) {

				e.printStackTrace();
			} catch (RserveException e) {

				e.printStackTrace();
			}

			System.out.println(out);

		}
		return Status.OK_STATUS;
	}

	public static void writeOutput() {
		writeToConsole();

		try {
			RServe.getConnection().voidEval("try(readLines(con = stdin()))");
		} catch (RserveException e) {

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
