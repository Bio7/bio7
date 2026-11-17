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
package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rbridge.debug.RProfiler;

/** Profiles the R script with the profiler of R (Rprof): a report in the browser and marks in the editor. */
public class ProfileScriptAction implements IObjectActionDelegate, IEditorActionDelegate {

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		action.setActionDefinitionId("com.eco.bio7.r_profile_script");
		if (targetEditor != null) {
			targetEditor.getSite().getKeyBindingService().registerAction(action);
		}
	}

	@Override
	public void run(IAction action) {
		if (RBridge.getBackend() == null) {
			/* The message: R is not running */
			RTools.isAliveDialog();
			return;
		}
		RProfiler.profileActiveEditor(false);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}
}
