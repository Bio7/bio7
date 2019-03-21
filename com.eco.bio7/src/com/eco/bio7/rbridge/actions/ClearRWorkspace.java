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
package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class ClearRWorkspace extends Action {

	private final IWorkbenchWindow window;

	public ClearRWorkspace(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setActionDefinitionId("com.eco.bio7.clear_r_workspace");
		setId("com.eco.bio7.clear_r_workspace");

	}

	public void run() {

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean rPipe = store.getBoolean("r_pipe");
		String dev = store.getString("DEVICE_DEFINITION");
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			dev = dev.replace("\\", "/");
		}

		if (rPipe == true) {
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			if (selectionConsole.equals("R")) {
				MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				message.setMessage("Do you really want to remove all visible objects?");
				message.setText("Remove objects");
				int response = message.open();
				if (response == SWT.YES) {
					String clear = "rm(list=ls(all=TRUE))";
					ConsolePageParticipant.pipeInputToConsole(clear, true, true);
				}

			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		} else {

			RConnection d = RServe.getConnection();

			if (d != null) {
				if (RState.isBusy() == false) {
					MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					message.setMessage("Do you really want to remove all visible objects?");
					message.setText("Remove objects");
					int response = message.open();
					if (response == SWT.YES) {
						String clear = "rm(list=ls())";
						// String clear = "rm(list=ls(all=TRUE))";

						try {
							RConnection con = RServe.getConnection();
							con.eval(clear);
							/* Reestablish default device! */
							/*
							 * boolean customDevice = store .getBoolean("USE_CUSTOM_DEVICE"); if
							 * (customDevice) { con.eval(dev); }
							 */

						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						RServeUtil.listRObjects();
					}
				} else {
					Bio7Dialog.message("Rserve is busy!");
				}

			} else {

				MessageBox messageBox = new MessageBox(new Shell(),

						SWT.ICON_WARNING);
				messageBox.setMessage("RServer connection failed - Server is not running !");
				messageBox.open();

			}
		}
	}

}