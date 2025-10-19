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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;
import com.eco.bio7.worldwind.WorldWindView;

public class RConnectionJob extends WorkspaceJob {

	private static Runtime rt;

	private static Process proc = null;

	private static IPreferenceStore store;

	private static boolean canceled = false;

	private BufferedReader input;

	public RConnectionJob() {
		super("Connecting To RServer");
		store = Bio7Plugin.getDefault().getPreferenceStore();

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Connecting To RServer", 10);
		canceled = false;
		RServe.setRrunning(true);
		// If remote!
		boolean remote = store.getBoolean("REMOTE");
		String rserveArgs = store.getString("RSERVE_ARGS");
		/*
		 * R started in the ConsolePageParticipants class as a native
		 * application. Here we call now Rserve!
		 */
		if (remote == false) {
			/* Start up the process of R and Rserve! */

			//if (store.getBoolean("RSERVE_NATIVE_START")) {
				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

				if (selectionConsole.equals("R")) {

					ConsolePageParticipant.pipeInputToConsole("library(Rserve)", true, false);
					ConsolePageParticipant.pipeInputToConsole("run.Rserve(" + rserveArgs + ")", true, false);

				} else {
					Bio7Dialog.message("Please start the native R connection in the Bio7 Console!");
				}

			/*} else {
				 Start Rserve without shell (deprecated)! 
				startExec();
			}*/

		} // Remote connection!
		for (int i = 0; i < 11; i++) {
			if (monitor.isCanceled())
				try {
					throw new InterruptedException();
				} catch (InterruptedException e) {
					canceled = true;
					RServe.setRrunning(false);

				}
			if (canceled == false) {
				if (RServe.getConnection() == null) {

					if (remote == false) {

						doConnectLocal();

					} else {

						doConnectRemote();

					}

					monitor.setTaskName("Connect to Rserver: " + (i) + " from 10");

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {

					}
					if (i == 10) {
						RServe.setRrunning(false);

						monitor.worked(10);
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.syncExec(new Runnable() {

							public void run() {

								MessageDialog.openWarning(Util.getShell(), "R", "R-Server connection could not be established" + "\n" + "probably the firewall is active->Please try again !");
								RServe.setConnection(null);
							}
						});

					}

				}

				else {
					if (RServe.getConnection().isConnected()) {

						configureRAfterSuccess(monitor);

					} else {
						RServe.setRrunning(false);
						monitor.worked(10);
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.syncExec(new Runnable() {

							public void run() {
								MessageDialog.openWarning(Util.getShell(), "R", "R-Server connection could not be established" + "->Please try again !");
								RServe.setConnection(null);

							}
						});
					}

					i = 11;
				}

			}
			monitor.worked(1);

		}

		return Status.OK_STATUS;
	}

	private void configureRAfterSuccess(IProgressMonitor monitor) {
		RServe.setRrunning(true);
		monitor.setTaskName("Connected");
		monitor.worked(10);
		RConnection con = RServe.getConnection();
		RConfig.config(con);
		RState.setBusy(false);
		

		System.out.println("Rserve connection established !");

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
	}

	

	/* Print the input stream for errors etc.! */
	private void consoleOutput() {
		new Thread() {

			public void run() {
				setPriority(Thread.MAX_PRIORITY);
				final InputStream inp = proc.getInputStream();
				InputStreamReader inr = new InputStreamReader(inp);
				int ch;
				try {
					while ((ch = inr.read()) != -1) {
						System.out.print((char) ch);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void doConnectRemote() {

		RConnection c;
		String host = store.getString("HOST");
		int tcp = store.getInt("TCP");

		try {
			c = new RConnection(host, tcp);

			RServe.setConnection(c);
			WorldWindView.setRConnection(c);
		} catch (RserveException e1) {
			System.out.println(e1.toString());

		}
		if (RServe.getConnection() != null) {
			String name = store.getString("USERNAME");
			String pass = store.getString("PASSWORD");
			// System.out.println(name + " " + pass);
			/* Login for remote connection! */

			try {
				RServe.getConnection().login(name, pass);
			} catch (RserveException e2) {

				System.out.println(e2.toString());
				RServe.setConnection(null);
			}
		}

	}

	private void doConnectLocal() {
       int port=store.getInt("RSERVE_CLIENT_CONNECTION_PORT");
		RConnection c;

		try {
			c = new RConnection("127.0.0.1", port);
            /*Setup all used Bio7 R connections (also for WorldWind and the REditor class)!*/
			RServe.setConnection(c);

		} catch (RserveException e1) {

		}

	}

	public static boolean isCanceled() {
		return canceled;
	}

	public static void setCanceled(boolean canceled) {
		RConnectionJob.canceled = canceled;
	}

	public static Process getProc() {
		return proc;
	}

	public static Runtime getRt() {
		return rt;
	}

	public static IPreferenceStore getStore() {
		return store;
	}

	public static void setStore(IPreferenceStore store) {
		RConnectionJob.store = store;
	}

}
