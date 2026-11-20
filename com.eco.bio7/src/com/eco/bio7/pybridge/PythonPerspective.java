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
package com.eco.bio7.pybridge;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.eco.bio7.rbridge.RTable;

/**
 * The Python perspective (as the R perspective): the Python-Shell and the
 * Python packages, the console, the editor, the table (where the Python plots
 * open) and the outline.
 */
public class PythonPerspective implements IPerspectiveFactory {

	public static final String ID = "com.eco.bio7.pybridge.PythonPerspective";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.30f, editorArea);
		IFolderLayout bottomLeft = layout.createFolder("viewBottomLeft", IPageLayout.BOTTOM, 0.55f, "topLeft");
		IFolderLayout viewRight = layout.createFolder("viewBottomRight", IPageLayout.BOTTOM, 0.55f, editorArea);

		topLeft.addView(PythonShellView.ID);
		topLeft.addView(PythonPackagesView.ID);
		topLeft.addView("org.eclipse.ui.navigator.ProjectExplorer");

		bottomLeft.addView("org.eclipse.ui.console.ConsoleView");

		viewRight.addView(RTable.ID);
		/* The Python plot view opens here (a tab beside the table) */
		viewRight.addPlaceholder("com.eco.bio7.custom_controls:Python Plot");
		viewRight.addView("org.eclipse.wst.common.snippets.internal.ui.SnippetsView");
		layout.addView("org.eclipse.ui.views.ContentOutline", IPageLayout.RIGHT, 0.735f, editorArea);
	}
}
