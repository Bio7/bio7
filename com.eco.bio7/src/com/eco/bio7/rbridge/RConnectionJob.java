/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RState;
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

			if (store.getBoolean("RSERVE_NATIVE_START")) {
				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

				if (selectionConsole.equals("R")) {

					ConsolePageParticipant.pipeInputToConsole("library(Rserve)", true, false);
					ConsolePageParticipant.pipeInputToConsole("run.Rserve(" + rserveArgs + ")", true, false);

				} else {
					Bio7Dialog.message("Please start the native R connection in the Bio7 Console!");
				}

			} else {
				/* Start Rserve without shell (deprecated)! */
				startExec();
			}

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
		/*
		 * Set the max print option to avoid a kind of deadlock in print
		 * situations!
		 */
		try {
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			String tempPath = store.getString(PreferenceConstants.P_TEMP_R);
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

				tempPath = tempPath.replace("\\", "/");
			}
			// System.out.println(tempPath);
			con.eval("try(options(max.print=5000))");
			/* Define a new R environment! */
			con.eval("try(.bio7TempEnvPath<- new.env())");
			/* Set Bio7 temp path in R! */
			con.eval("try(assign(\"pathTemp\", \"" + tempPath + "\", env=.bio7TempEnvPath))");
			/* Make the default completion function in R available! */
			con.eval("try(source(paste(.bio7TempEnvPath$pathTemp,'calculateRCompletion.R',sep='')))");
			// con.eval("try(setHook(packageEvent(\"spatstat\",\"onLoad\"),\"writeFunctionDef\"))");
			/*
			 * Set the default install location for the add on packages!
			 */

			String rPackages = store.getString("InstallLocation");
			String dev = store.getString("DEVICE_DEFINITION");
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

				rPackages = rPackages.replace("\\", "/");
				dev = dev.replace("\\", "/");
				con.eval("try(.libPaths(\"" + rPackages + "\"))");
			}
			/*For Linux and Mac if there is no path we will take the default defined!*/
			else{
				if (rPackages.isEmpty() == false) {
				con.eval("try(.libPaths(\"" + rPackages + "\"))");
				}
			}
			
			/* Set the default device! */
			boolean customDevice = store.getBoolean("USE_CUSTOM_DEVICE");
			if (customDevice) {
				con.eval(dev);
			}
			/* Set the custom startup preferences options! */
			String rStartupArgs = store.getString("R_STARTUP_ARGS");
			rStartupArgs = rStartupArgs.replace('\r', ' ');// Replace
			con.eval("" + rStartupArgs + "");
			/*
			 * If Rserve is unavailable reset the variable which indicates if
			 * Rserve is busy!
			 */
			RState.setBusy(false);
		} catch (RserveException e1) {

			// e1.printStackTrace();
			System.out.println("Error occured!");
		}

		System.out.println("Rserve connection established !");

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
	}

	/* Deprecated startup without native shell! */
	private void startExec() {

		rt = Runtime.getRuntime();

		boolean startRShell = store.getBoolean(PreferenceConstants.R_START_SHELL);
		String rserveArgs = store.getString("RSERVE_ARGS");
		String rArgs = store.getString("R_STARTUP_ARGS");

		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			String pathR;
			if (ApplicationWorkbenchWindowAdvisor.is64BitVM()) {
				pathR = store.getString(PreferenceConstants.PATH_R) + "/bin/x64/r";
			} else {
				pathR = store.getString(PreferenceConstants.PATH_R) + "/bin/i386/r";
			}

			if (startRShell == false) {

				List<String> args = new ArrayList<String>();
				args.add(pathR);
				args.add(rArgs);
				args.add("-e");
				args.add("library(Rserve);Rserve(args='" + rserveArgs + "');");

				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectErrorStream();
				try {
					proc = pb.start();

				} catch (IOException e) {

					e.printStackTrace();
				}
			} else {
				List<String> args = new ArrayList<String>();
				args.add("CMD");
				args.add("/c");

				args.add("start");
				args.add("\"Rserve\"");
				args.add(pathR);
				args.add("-e");
				args.add("library(Rserve);Rserve();");

				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectErrorStream();
				try {
					proc = pb.start();

				} catch (IOException e) {

					MessageDialog.openWarning(Util.getShell(), "R", "Rserve executable not available !");
					RServe.setConnection(null);
				}
			}

			consoleOutput();

		} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
			String path;
			if (ApplicationWorkbenchWindowAdvisor.is64BitVM()) {
				path = store.getString(PreferenceConstants.PATH_R) + "/bin/R";
			} else {
				path = store.getString(PreferenceConstants.PATH_R) + "/bin/R";
			}
			if (startRShell == true) {
				String shellType = store.getString("LINUX_SHELL");
				// Runtime runtime = Runtime.getRuntime();
				// proc = runtime.exec("xterm -hold -e " + pathR +
				// "/bin/R -e library(Rserve);Rserve();");
				List<String> args = new ArrayList<String>();
				if (shellType.equals("XTERM")) {

					args.add("xterm");
					args.add("-hold");
					args.add("-e");
					if (path.isEmpty() == false) {
						args.add(path);
					} else {
						args.add("R");
					}
					args.add("-e");
					args.add("library(Rserve);Rserve();");
				} else if (shellType.equals("GNOME")) {
					args.add("gnome-terminal");

					args.add("-e");
					if (path.isEmpty() == false) {
						args.add("sh -c \"" + path + " -e 'library(Rserve);Rserve();'; exec bash\"");
					}

					else {
						args.add("sh -c \"" + "R" + " -e 'library(Rserve);Rserve();'; exec bash\"");
					}

				}

				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectErrorStream();
				try {
					proc = pb.start();

				} catch (IOException e) {

					MessageDialog.openWarning(Util.getShell(), "R", "Rserve executable not available !");
					RServe.setConnection(null);
				}

			} else {

				List<String> args = new ArrayList<String>();
				args.add("/bin/sh");
				args.add("-c");
				if (path.isEmpty() == false) {
					args.add("echo 'library(Rserve);Rserve(args=\"" + rserveArgs + "\")'|" + path + " " + rArgs);
				}

				else {
					args.add("echo 'library(Rserve);Rserve(args=\"" + rserveArgs + "\")'|" + "R" + " " + rArgs);
				}

				// Runtime runtime = Runtime.getRuntime();
				// proc = runtime.exec("xterm -e " + pathR +
				// "/bin/R -e library(Rserve);Rserve(args='" + rserveArgs +
				// "');");
				/*
				 * List<String> args = new ArrayList<String>();
				 * args.add("xterm"); args.add("-e"); args.add(path);
				 * args.add(rArgs); args.add("-e");
				 * args.add("library(Rserve);Rserve(args='" + rserveArgs +
				 * "');");
				 */

				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectErrorStream();
				try {
					proc = pb.start();

				} catch (IOException e) {

					MessageDialog.openWarning(Util.getShell(), "R", "Rserve executable not available !");
					RServe.setConnection(null);
				}

			}
			consoleOutput();
		} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
			// String
			// path="/Library/Frameworks/R.framework/Versions/2.10.0/Resources/bin/R";
			String path;

			path = store.getString(PreferenceConstants.PATH_R) + "/bin/R";

			if (startRShell == true) {

				List<String> args = new ArrayList<String>();
				args.add("/usr/x11/bin/xterm");
				args.add("-hold");
				args.add("-e");
				if (path.isEmpty() == false) {
					args.add(path);
				} else {
					args.add("R");
				}
				args.add("-e");
				args.add("library(Rserve);Rserve();");

				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectErrorStream();
				try {
					proc = pb.start();

				} catch (IOException e) {

					MessageDialog.openWarning(Util.getShell(), "R", "Rserve executable not available !");
					RServe.setConnection(null);
				}

			} else {

				List<String> args = new ArrayList<String>();
				args.add("/usr/x11/bin/xterm");
				args.add("-e");
				if (path.isEmpty() == false) {
					args.add(path);
				} else {
					args.add("R");
				}
				args.add(rArgs);
				args.add("-e");
				args.add("library(Rserve);Rserve(args='" + rserveArgs + "');");

				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectErrorStream();
				try {
					proc = pb.start();

				} catch (IOException e) {

					MessageDialog.openWarning(Util.getShell(), "R", "Rserve executable not available !");
					RServe.setConnection(null);
				}
			}
			consoleOutput();
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

		RConnection c;

		try {
			c = new RConnection("127.0.0.1", 6311);

			RServe.setConnection(c);
			WorldWindView.setRConnection(c);
			REditor.setConnection(c);

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
