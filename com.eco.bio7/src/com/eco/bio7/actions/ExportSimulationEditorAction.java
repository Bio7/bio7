/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

/**
 * "Export Simulation as HTML..." in the context menu of the Java editor and of
 * the R editor: exports the simulation model of the editor (see
 * {@link ExportSimulation}).
 */
public class ExportSimulationEditorAction implements IEditorActionDelegate {

	private IEditorPart editor;

	@Override
	public void run(IAction action) {
		if (editor != null) {
			editor.getSite().getPage().activate(editor);
			ExportSimulation.run(editor.getSite().getShell());
		}
	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		editor = targetEditor;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
