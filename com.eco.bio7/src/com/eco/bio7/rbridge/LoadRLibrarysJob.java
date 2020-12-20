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

package com.eco.bio7.rbridge;

/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rbridge.views.PackageInstallView;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;

public class LoadRLibrarysJob extends WorkspaceJob {

	//protected String[] items;
	private IEditorPart editor;
	protected TableItem[] tableItems;

	public LoadRLibrarysJob(IEditorPart editor) {
		super("Load...");
		this.editor = editor;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Load Libraries..", IProgressMonitor.UNKNOWN);
		if (RServe.isAlive()) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					tableItems = PackageInstallView.getAllInstalledPackagesList().getSelection();

					for (int i = 0; i < tableItems.length; i++) {
						RConnection c = RServe.getConnection();

						try {
							c.eval("try(library(" + tableItems[i].getText(0) + "))");

							/*
							 * Function loaded at Rserve startup. Writes the available
							 * functions to a file!
							 */
							c.eval(".bio7WriteFunctionDef();");
						} catch (RserveException e) {

							e.printStackTrace();
						}
						System.out.println("Loaded library " + tableItems[i].getText(0));

					}
				}
			});

			/*
			 * Reload the code proposals (not the templates) for the R editor!
			 */
			CalculateRProposals.setStartupTemplate(false);
			CalculateRProposals.loadRCodePackageTemplates();
			CalculateRProposals.updateCompletions();
			if (editor instanceof REditor) {
				REditor rEditor = (REditor) editor;
				Parse parse = rEditor.getParser();
				parse.parse();
			}

		}

		return Status.OK_STATUS;
	}

}
