/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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

package com.eco.bio7.rbridge.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import com.eco.bio7.rbridge.RTable;

public class RImagePerspective implements IPerspectiveFactory {

	/**
	 * Creates the initial layout for a page.
	 */
	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, (float) 0.30, editorArea);

		IFolderLayout viewRightFull = layout.createFolder("viewRightFull", IPageLayout.RIGHT, (float) 0.735, editorArea);
		layout.addView("com.eco.bio7.image_methods", IPageLayout.BOTTOM, 0.437f, "viewRightFull");
		
		IFolderLayout bottomLeft = layout.createFolder("viewBottomLeft", IPageLayout.BOTTOM, (float) 0.55, "topLeft");

		IFolderLayout viewRight = layout.createFolder("viewBottomRight", IPageLayout.BOTTOM, (float) 0.55, editorArea);
		topLeft.addView("org.eclipse.ui.navigator.ProjectExplorer");
		topLeft.addView("com.eco.bio7.RShell");
		topLeft.addView("com.eco.bio7.rbridge.PackageInstallView");
		topLeft.addView("com.eco.bio7.reditor.database.view.DatabaseView");

		bottomLeft.addView("org.eclipse.ui.console.ConsoleView");
		bottomLeft.addView("com.eco.bio7.rbridge.RPlotView");
		bottomLeft.addView("com.eco.bio7.rbridge.views.RTemplatesView");

		viewRight.addView("com.eco.bio7.imagej");
		viewRight.addView(RTable.ID);
		viewRight.addView("org.eclipse.wst.common.snippets.internal.ui.SnippetsView");

		viewRightFull.addView("com.eco.bio7.image.TransferGeometryView");
		viewRightFull.addView("org.eclipse.ui.views.ContentOutline");

		layout.addView("com.eco.bio7.ijtoolbar", IPageLayout.BOTTOM, 0.80f, editorArea);
	}

}
