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

public class RDebugPerspective implements IPerspectiveFactory {

	/**
	 * Creates the initial layout for a page.
	 */
	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, (float) 0.30, editorArea);

		//IFolderLayout viewRightFull = layout.createFolder("viewRightFull", IPageLayout.RIGHT, (float) 0.735, editorArea);

		IFolderLayout bottomLeft = layout.createFolder("viewBottomLeft", IPageLayout.BOTTOM, (float) 0.55, "topLeft");

		IFolderLayout viewRight = layout.createFolder("viewBottomRight", IPageLayout.BOTTOM, (float) 0.55, editorArea);
		topLeft.addView("org.eclipse.ui.navigator.ProjectExplorer");
		
		bottomLeft.addView("com.eco.bio7.rbridge.debug.DebugVariablesView");
		

		viewRight.addView("org.eclipse.ui.console.ConsoleView");
		layout.addView("com.eco.bio7.rbridge.debug.DebugTextView", IPageLayout.RIGHT, 0.64f, IPageLayout.ID_EDITOR_AREA);
		
		

		
		

		
	}

}
