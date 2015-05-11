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

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import com.eco.bio7.rbridge.RTable;

public class RPerspective implements IPerspectiveFactory {

	/**
	 * Creates the initial layout for a page.
	 */
	public void createInitialLayout(IPageLayout layout) {
		/*
		 * Old! String editorArea = layout.getEditorArea(); layout.setEditorAreaVisible(false); layout.addView("com.eco.bio7.RShell", IPageLayout.LEFT, 0.5f, editorArea); layout.addView(RTable.ID, IPageLayout.RIGHT, 0.39f, "com.eco.bio7.RShell");
		 */

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, (float) 0.30, editorArea);
		
		
		IFolderLayout bottomLeft = layout.createFolder("viewBottomLeft", IPageLayout.BOTTOM, (float) 0.55,"topLeft");
		

		IFolderLayout viewRight = layout.createFolder("viewBottomRight", IPageLayout.BOTTOM, (float) 0.55, editorArea);
		
		topLeft.addView("com.eco.bio7.RShell");
		topLeft.addView("org.eclipse.ui.views.ResourceNavigator");
		
		
		bottomLeft.addView("org.eclipse.ui.console.ConsoleView");
		bottomLeft.addView("com.eco.bio7.rbridge.debug.DebugTextView");
		bottomLeft.addView("com.eco.bio7.rbridge.debug.DebugVariablesView");
		viewRight.addView(RTable.ID);		
		viewRight.addView("org.eclipse.wst.common.snippets.internal.ui.SnippetsView");
		layout.addView("org.eclipse.ui.views.ContentOutline", IPageLayout.RIGHT, 0.66f, editorArea);
	}

}
