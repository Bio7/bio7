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
package com.eco.bio7.pybridge.debug;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.eco.bio7.rbridge.RTable;

/**
 * The perspective of the Python debugger (as R Debug, not opened at the start):
 * the Python Debug view, the editor, the console, the outline.
 */
public class PythonDebugPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.30f, editorArea);
		IFolderLayout bottomLeft = layout.createFolder("viewBottomLeft", IPageLayout.BOTTOM, 0.55f, "topLeft");
		IFolderLayout viewRight = layout.createFolder("viewBottomRight", IPageLayout.BOTTOM, 0.55f, editorArea);
		topLeft.addView("org.eclipse.ui.navigator.ProjectExplorer");
		topLeft.addView("com.eco.bio7.PythonShell");
		bottomLeft.addView(PythonDebugView.ID);
		viewRight.addView("org.eclipse.ui.console.ConsoleView");
		viewRight.addView(RTable.ID);
		viewRight.addPlaceholder("com.eco.bio7.custom_controls:Python Plot");
		layout.addView("org.eclipse.ui.views.ContentOutline", IPageLayout.RIGHT, 0.735f, editorArea);
	}
}
