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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.preferences.PreferenceConstants;

public class UpdateRPackagesJob extends WorkspaceJob {

	// protected String[] itemsSpatial;

	public UpdateRPackagesJob() {
		super("Update packages...");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Update Packages..", IProgressMonitor.UNKNOWN);
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
				out = c.eval(".pkgListToUpdate <- installed.packages(priority='NA')[,'Package'];" + ".pkgListToUpdate<-.pkgListToUpdate[-which(.pkgListToUpdate==\"Rserve\",arr.ind =TRUE)];"
						+ "try(paste(capture.output(update.packages(repos =\"" + server + "\",checkBuilt=TRUE, ask=FALSE,oldPkgs=.pkgListToUpdate)),collapse=\"\\n\"))").asString();
				c.eval("remove(.pkgListToUpdate)");
			} catch (REXPMismatchException e) {

				e.printStackTrace();
			} catch (RserveException e) {

				e.printStackTrace();
			}

			System.out.println(out);

			// }
			/*
			 * For the packages on Linux and Mac we try the default path if no custom path
			 * is given!
			 */
			/*
			 * else {
			 * 
			 * if (destdir.isEmpty() == false) {
			 * 
			 * String out = null; try { out =
			 * c.eval("try(paste(capture.output(update.packages(repos =\"" + server +
			 * "\")),collapse=\"\\n\"))").asString();
			 * 
			 * } catch (REXPMismatchException e) {
			 * 
			 * e.printStackTrace(); } catch (RserveException e) {
			 * 
			 * e.printStackTrace(); }
			 * 
			 * System.out.println(out);
			 * 
			 * } else {
			 * 
			 * String out = null; try { out =
			 * c.eval("try(paste(capture.output(install.packages(\"" + items[i] +
			 * "\",dependencies=TRUE,repos =\"" + server +
			 * "\")),collapse=\"\\n\"))").asString(); } catch (REXPMismatchException e) {
			 * 
			 * e.printStackTrace(); } catch (RserveException e) {
			 * 
			 * e.printStackTrace(); }
			 * 
			 * System.out.println(out);
			 * 
			 * }
			 * 
			 * }
			 */

			/* If thematic (spatial) packages are selected! */
			/*
			 * for (int i = 0; i < itemsSpatial.length; i++) {
			 * 
			 * String out = null; try { out = c.eval(
			 * "try(paste(capture.output(install.packages(\"" + itemsSpatial[i] +
			 * "\",dependencies=TRUE,repos =\"" + server + "\",lib=\"" + destdir +
			 * "\",destdir=\"" + destdir + "\")),collapse=\"\\n\"))").asString(); } catch
			 * (REXPMismatchException e) {
			 * 
			 * e.printStackTrace(); } catch (RserveException e) {
			 * 
			 * e.printStackTrace(); }
			 * 
			 * System.out.println(out);
			 * 
			 * }
			 */

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
