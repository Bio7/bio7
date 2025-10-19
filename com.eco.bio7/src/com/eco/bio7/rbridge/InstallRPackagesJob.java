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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.views.PackageInstallView;

public class InstallRPackagesJob extends WorkspaceJob {

	protected String[] items;
	//protected String[] itemsSpatial;

	public InstallRPackagesJob() {
		super("Install packages...");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Retrieve package list..", IProgressMonitor.UNKNOWN);
		if (RServe.isAlive()) {
			RConnection c = RServe.getConnection();
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					items = PackageInstallView.getAllList().getSelection();
					//itemsSpatial = PackageInstallView.getSpatialList().getSelection();
			}
			});
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			String destdir = store.getString("InstallLocation");
			String server = store.getString("R_PACKAGE_SERVER");
			if (Bio7Dialog.getOS().equals("Windows")) {
				/* If a location is given! */

				destdir = destdir.replace("\\", "\\\\");
				for (int i = 0; i < items.length; i++) {

					String out = null;
					try {
						out = c.eval(
								"try(paste(capture.output(install.packages(\"" + items[i] + "\",dependencies=TRUE,repos =\"" + server + "\",lib=\"" + destdir + "\",destdir=\"" + destdir
										+ "\")),collapse=\"\\n\"))").asString();
					} catch (REXPMismatchException e) {

						e.printStackTrace();
					} catch (RserveException e) {

						e.printStackTrace();
					}

					System.out.println(out);

				}

			}
			/*For the packages on Linux and Mac we try the default path if no custom path is given!*/
			else{
				
				if (destdir.isEmpty() == false) {
					for (int i = 0; i < items.length; i++) {

						String out = null;
						try {
							out = c.eval(
									"try(paste(capture.output(install.packages(\"" + items[i] + "\",dependencies=TRUE,repos =\"" + server + "\",lib=\"" + destdir + "\",destdir=\"" + destdir
											+ "\")),collapse=\"\\n\"))").asString();
						} catch (REXPMismatchException e) {

							e.printStackTrace();
						} catch (RserveException e) {

							e.printStackTrace();
						}

						System.out.println(out);

					}
					
				}
				else{
					for (int i = 0; i < items.length; i++) {

						String out = null;
						try {
							out = c.eval(
									"try(paste(capture.output(install.packages(\"" + items[i] + "\",dependencies=TRUE,repos =\"" + server + "\")),collapse=\"\\n\"))").asString();
						} catch (REXPMismatchException e) {

							e.printStackTrace();
						} catch (RserveException e) {

							e.printStackTrace();
						}

						System.out.println(out);

					}
					
				}
				
				
			
			}

			
			/* If thematic (spatial) packages are selected! */
			/*for (int i = 0; i < itemsSpatial.length; i++) {

				String out = null;
				try {
					out = c.eval(
							"try(paste(capture.output(install.packages(\"" + itemsSpatial[i] + "\",dependencies=TRUE,repos =\"" + server + "\",lib=\"" + destdir + "\",destdir=\"" + destdir
									+ "\")),collapse=\"\\n\"))").asString();
				} catch (REXPMismatchException e) {

					e.printStackTrace();
				} catch (RserveException e) {

					e.printStackTrace();
				}

				System.out.println(out);

			}*/

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
