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
package com.eco.bio7.compile;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

/**
 * The actions of the context menu of the Java editor: "Profile Java" (id
 * ...profile) and "Clear Profile Marks" (id ...clearProfileMarks).
 */
public class JavaProfileAction implements IEditorActionDelegate {

	@Override
	public void run(IAction action) {
		if (action.getId() != null && action.getId().endsWith("clearProfileMarks")) {
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			IFile file = editor == null ? null : editor.getEditorInput().getAdapter(IFile.class);
			if (file != null) {
				JavaProfileMarks.clear(file);
			}
			return;
		}
		JavaProfiler.profileActiveEditor();
	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
