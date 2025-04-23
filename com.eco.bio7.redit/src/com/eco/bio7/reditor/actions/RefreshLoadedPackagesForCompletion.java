/*******************************************************************************
 * Copyright (c) 2005-2018 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditor.actions;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
import com.eco.bio7.util.Util;

public class RefreshLoadedPackagesForCompletion extends Action implements IObjectActionDelegate {

	private REditor rEditor;

	public RefreshLoadedPackagesForCompletion(String text, REditor rEditor) {
		super(text);
		setActionDefinitionId("com.eco.bio7.reditor.rserve.refresh.packages");
		setId("com.eco.bio7.reditor.refresh.packages");
		this.rEditor = rEditor;

	}

	public void run() {

		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			if (RState.isBusy() == false) {

				RState.setBusy(true);
				Job job = new Job("Reload") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Reload package information...", IProgressMonitor.UNKNOWN);

						RConnection c = REditor.getRserveConnection();
						if (c != null) {

							Map<String, String> map = rEditor.getParser().getCurrentPackageImports();
							for (Map.Entry<String, String> entry : map.entrySet()) {
								// System.out.println("library("+entry.getValue()+")");
								try {
									c.eval("try(library(" + entry.getValue() + "))");
								} catch (RserveException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							try {
								/*
								 * Writes the available functions to a file!
								 */
								c.eval(".bio7WriteFunctionDef();");
							} catch (RserveException e) {

								e.printStackTrace();
							}

						}

						monitor.done();
						return Status.OK_STATUS;
					}

				};
				job.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							/*
							 * Reload the code proposals (not the templates) for the R editor!
							 */
							CalculateRProposals.setStartupTemplate(false);
							CalculateRProposals.loadRCodePackageTemplates();
							CalculateRProposals.updateCompletions();

							/* Reparse the document! */
							if (rEditor != null) {

								REditor.getUpdateCompletion().trigger();
								Parse parse = rEditor.getParser();
								parse.parse();
							}

							RState.setBusy(false);
						} else {

						}
					}
				});
				// job.setSystem(true);
				job.schedule();
			} else {
				System.out.println("Rserve is busy!");
			}
		} else {
			message("Rserve is not alive!");
		}

	}

	/**
	 * Opens a message Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 */
	public void message(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(Util.getShell(),

						SWT.ICON_WARNING);
				messageBox.setText("Info!");
				messageBox.setMessage(text);
				messageBox.open();
			}
		});
	}

	@Override
	public void run(IAction action) {
		run();

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub

	}

}
