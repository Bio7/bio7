/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.compile;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.Console;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.StartBio7Utils;

public class RInterpreterJob extends WorkspaceJob {

	private String tointerpret;

	private String script;

	private String location;

	private static boolean exception = false;

	private static boolean plot = true;
	
	private static String rCommand = "" + "try(paste(capture.output(source(.bio7TempRScriptFile,echo=T)),collapse=\"\\n\"))";

	public RInterpreterJob(String tointerpret, boolean doPlot, String loc) {
		super("Interpret RScript");

		this.tointerpret = tointerpret;
		this.plot = doPlot;
		this.location = loc;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Interpret RScript.....", IProgressMonitor.UNKNOWN);

		interpretRJob(tointerpret, plot, location);
		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {
				Bio7Action.stopFlow();
				System.out.println("Flow canceled!");

			}
		}

		return Status.OK_STATUS;
	}

	public void interpretRJob(String thesource, final boolean plot, String loc) {
		String temp = thesource;

		final RConnection cscript = RServe.getConnection();

		if (RServe.isRrunning()) {
			if (cscript != null) {
				try {
					//boolean startShell = Bio7Plugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.R_START_SHELL);
					
					if (Bio7Dialog.getOS().equals("Windows")) {
						/* If a location is given! */
						if (loc != null) {
							loc = loc.replace("/", "\\");

							cscript.assign(".bio7TempRScriptFile", loc);
							/*if (startShell) {
								cscript.voidEval("try(source(.bio7TempRScriptFile,echo=T))");

							} else {*/

								String rout = null;
								try {
									
									rout = cscript.eval(rCommand).asString();
									
									
									
									
								} catch (REXPMismatchException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								Console cons = StartBio7Utils.getConsoleInstance().cons;
								cons.println(rout);
								/* Send also the output to the R console view! */
								if (RShellView.isConsoleExpanded()) {
									
									RShellView.setTextConsole(rout);
									
								}
							//}

						} else {

							/*
							 * The classical way for Windows. Important for
							 * loading files and the drag and drop support!
							 */
							script = temp.replace('\r', ' ');// Replace
							// LineFeed.
							cscript.voidEval(script);

						}
					}
					/* If Linux is the OS! */
					else {

						if (loc != null) {
							cscript.assign(".bio7TempRScriptFile", loc);
							/*if (startShell) {
								cscript.voidEval("try(source(.bio7TempRScriptFile))");

							} else {*/
								String rout = null;
								try {
									rout = cscript.eval(rCommand).asString();
								} catch (REXPMismatchException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								Console cons = StartBio7Utils.getConsoleInstance().cons;
								cons.println(rout);
								/* Send also the output to the R console view! */
								if (RShellView.isConsoleExpanded()) {
									RShellView.setTextConsole(rout);
								}
							//}

						} else {

							/*
							 * The standard evaluation for Linux.Important for
							 * loading files and the drag and drop support!
							 */
							script = temp.replace('\r', ' ');// Replace
							// LineFeed.
							cscript.voidEval(script);
						}
					}

				} catch (RserveException e) {
					exception = true;
					System.out.println("Rserve exception!");
				}

			}
			if (plot == true) {
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {

						if (exception == false) {
							RScript.getmarkers();// Try to plot if markers
													// available !
						}

						else {
							Bio7Dialog.message("An exception occured! \n" + "If necessary restart the RServe application!");
						}

					}
				});
			}
		}
		temp = null;
		script = null;
		exception = false;
	}

	/*public  void writeToConsole() {
		new Thread() {
			public void run() {
				Process p = RConnectionJob.getProc();
				// Write to the output!
				final OutputStream os = p.getOutputStream();
				final OutputStreamWriter osw = new OutputStreamWriter(os);
				final BufferedWriter bw = new BufferedWriter(osw, 100);

				try {
					bw.write("1");
					bw.newLine();
					os.flush();
					bw.flush();
					bw.close();
				} catch (IOException e) {
					System.err.println("");
				}

			}
		}.start();

	}*/

}
