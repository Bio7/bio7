/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.util.Util;
import com.eco.bio7.worldwind.WorldWindView;

public class RConnectionJob extends WorkspaceJob {

	private static Process proc = null;

	private static IPreferenceStore store;

	private static volatile boolean canceled = false;

	public RConnectionJob() {
		super("Connecting To RServer");
		store = Bio7Plugin.getDefault().getPreferenceStore();

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Connecting To RServer", IProgressMonitor.UNKNOWN);
		canceled = false;
		RServe.setRrunning(true);
		boolean remote = store.getBoolean("REMOTE");
		if (remote == false) {
			connectLocal(monitor);
		} else {
			connectRemote(monitor);
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	/*
	 * R is started in the ConsolePageParticipant class as a native application.
	 * Here Rserve is started in this R process and the connection is established.
	 */
	private void connectLocal(IProgressMonitor monitor) {
		int timeout = store.getInt("RSERVE_CONNECT_TIMEOUT");
		RserveLauncher.Result result = RserveLauncher.start(monitor, store.getString("RSERVE_ARGS"), store.getInt("RSERVE_CLIENT_CONNECTION_PORT"),
				timeout > 0 ? timeout : 20);
		if (!result.notes().isEmpty()) {
			System.out.print(result.notes());
		}
		if (result.isConnected()) {
			/* Setup all used Bio7 R connections (also for WorldWind and the REditor class)! */
			RServe.setConnection(result.connection());
			configureRAfterSuccess(monitor);
			return;
		}
		RServe.setRrunning(false);
		RServe.setConnection(null);
		if (result.status() == RserveLauncher.Status.CANCELED) {
			canceled = true;
			return;
		}
		showFailure(result.message(), result.offersSetup());
	}

	/* A warning, with a button for the setup of R and Rserve if it can help */
	private static void showFailure(String message, boolean offerSetup) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(() -> {
			if (!offerSetup) {
				MessageDialog.openWarning(Util.getShell(), "Rserve", message);
				return;
			}
			MessageDialog dialog = new MessageDialog(Util.getShell(), "Rserve", null, message, MessageDialog.WARNING, 0, "R and Rserve Setup...", "Close");
			if (dialog.open() == 0) {
				RSetupDialog.openSetup();
			}
		});
	}

	private void connectRemote(IProgressMonitor monitor) {
		String lastError = null;
		for (int i = 0; i < 10; i++) {
			if (monitor.isCanceled() || canceled) {
				canceled = true;
				RServe.setRrunning(false);
				return;
			}
			monitor.subTask("Connect to Rserve: " + (i + 1) + " of 10");
			lastError = doConnectRemote();
			if (RServe.getConnection() != null && RServe.getConnection().isConnected()) {
				configureRAfterSuccess(monitor);
				return;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		RServe.setRrunning(false);
		RServe.setConnection(null);
		showFailure("The connection to the remote Rserve (" + store.getString("HOST") + ":" + store.getInt("TCP") + ") could not be established."
				+ (lastError != null ? "\nLast error: " + lastError : "") + "\n\nPlease check the server, the login and the firewall.", false);
	}

	private void configureRAfterSuccess(IProgressMonitor monitor) {
		RServe.setRrunning(true);
		monitor.setTaskName("Connected");
		RConnection con = RServe.getConnection();
		RConfig.config(con);
		RState.setBusy(false);

		System.out.println("Rserve connection established !");
	}

	/* Connects to the remote server and returns the error or null */
	private String doConnectRemote() {

		RConnection c;
		String host = store.getString("HOST");
		int tcp = store.getInt("TCP");

		try {
			c = new RConnection(host, tcp);

			RServe.setConnection(c);
			WorldWindView.setRConnection(c);
		} catch (RserveException e1) {
			return e1.getMessage();
		}
		String name = store.getString("USERNAME");
		String pass = store.getString("PASSWORD");
		/* Login for remote connection! */
		try {
			c.login(name, pass);
		} catch (RserveException e2) {
			c.close();
			RServe.setConnection(null);
			return "Login failed: " + e2.getMessage();
		}
		return null;
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

	public static IPreferenceStore getStore() {
		return store;
	}

	public static void setStore(IPreferenceStore store) {
		RConnectionJob.store = store;
	}

}
