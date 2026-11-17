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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

public class UpdateRPackagesJob extends WorkspaceJob {

	// protected String[] itemsSpatial;

	public UpdateRPackagesJob() {
		super("Update packages...");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Update Packages..", IProgressMonitor.UNKNOWN);
		if (RTools.isAlive()) {
			RBackend c = RBridge.getBackend();

			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			// String destdir = store.getString("InstallLocation");
			String server = store.getString("R_PACKAGE_SERVER");
			// if (Bio7Dialog.getOS().equals("Windows")) {
			/* If a location is given! */

			// destdir = destdir.replace("\\", "\\\\");
			String out = null;
			try {
				out = c.eval(".pkgListToUpdate <- installed.packages(priority='NA')[,'Package'];"
						+ "try(paste(capture.output(update.packages(repos =\"" + server + "\",checkBuilt=TRUE, ask=FALSE,oldPkgs=.pkgListToUpdate)),collapse=\"\\n\"))").firstString();
				c.eval("remove(.pkgListToUpdate)");
			} catch (RTypeException e) {

				e.printStackTrace();
			} catch (RException e) {

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
			 * "\")),collapse=\"\\n\"))").firstString();
			 * 
			 * } catch (RTypeException e) {
			 * 
			 * e.printStackTrace(); } catch (RException e) {
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
			 * "\")),collapse=\"\\n\"))").firstString(); } catch (RTypeException e) {
			 * 
			 * e.printStackTrace(); } catch (RException e) {
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
			 * "\",destdir=\"" + destdir + "\")),collapse=\"\\n\"))").firstString(); } catch
			 * (RTypeException e) {
			 * 
			 * e.printStackTrace(); } catch (RException e) {
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

		try {
			RBridge.getBackend().voidEval("try(readLines(con = stdin()))");
		} catch (RException e) {

			e.printStackTrace();
		}


	}

}
