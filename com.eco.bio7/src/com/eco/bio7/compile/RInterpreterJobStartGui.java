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

package com.eco.bio7.compile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class RInterpreterJobStartGui extends WorkspaceJob {

	private String tointerpret;

	public RInterpreterJobStartGui(String tointerpret) {
		super("Job");

		this.tointerpret = tointerpret;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Interpret RScript.....", IProgressMonitor.UNKNOWN);

		interpret_r_job(tointerpret);

		return Status.OK_STATUS;
	}

	public void interpret_r_job(String selected) {

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			String path;
			if (ApplicationWorkbenchWindowAdvisor.is64BitVM()) {
				path = store.getString(PreferenceConstants.PATH_R)
						+ "/bin/x64/Rgui";
			} else {
				path = store.getString(PreferenceConstants.PATH_R)
						+ "/bin/i386/Rgui";
			}

			//String path = store.getString(PreferenceConstants.PATH_R) + "/bin/i386/Rgui";

			// Runtime.getRuntime().exec("\"" + ex + "\"" + " " + "\""+selected+"\"");

			List<String> args = new ArrayList<String>();
			args.add(path);
			args.add(selected);
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream();
			try {
				pb.start();
			} catch (IOException e) {
				Bio7Dialog.message("R execution failed !");
			}

		} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
			String shellOption = ShellMessages.getString("LinuxShell.1");
			String path = store.getString(PreferenceConstants.PATH_R) + "/bin/R";
			int shell = Integer.parseInt(shellOption);
			if (shell == 1) {

				//Runtime.getRuntime().exec("xterm -hold -e " + path); //$NON-NLS-1$ //$NON-NLS-2$
				List<String> args = new ArrayList<String>();
				args.add("xterm");
				args.add("-hold");
				args.add("-e");
				args.add(path);
				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectErrorStream();
				try {
					pb.start();

				} catch (IOException e) {
					Bio7Dialog.message("R execution failed !");

				}
			} else {

				//Runtime.getRuntime().exec("gnome-terminal -x " + path); //$NON-NLS-1$ //$NON-NLS-2$
				List<String> args = new ArrayList<String>();
				args.add("gnome-terminal");
				args.add("-x");
				args.add(path);
				ProcessBuilder pb = new ProcessBuilder(args);
				pb.redirectErrorStream();
				try {
					pb.start();

				} catch (IOException e) {
					Bio7Dialog.message("R execution failed !");

				}
			}

		} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
			String path = store.getString(PreferenceConstants.PATH_R) + "/R";

			//Runtime.getRuntime().exec("/usr/x11/bin/xterm -hold -e " + path); //$NON-NLS-1$ //$NON-NLS-2$
			List<String> args = new ArrayList<String>();
			args.add("/usr/x11/bin/xterm");
			args.add("-hold");
			args.add("-e");
			args.add(path);
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream();
			try {
				pb.start();

			} catch (IOException e) {
				Bio7Dialog.message("R execution failed !");

			}
		}

	}

}
