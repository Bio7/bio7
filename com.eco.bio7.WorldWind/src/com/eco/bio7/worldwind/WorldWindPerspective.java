/*******************************************************************************
 * Copyright (c) 2004-2019 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.worldwind;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbenchPage;

public class WorldWindPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {

		//String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		{
			IFolderLayout folderLayout = layout.createFolder("folder_1", IPageLayout.LEFT, 0.31f, IPageLayout.ID_EDITOR_AREA);
			folderLayout.addView("org.eclipse.ui.console.ConsoleView");
			folderLayout.addView("com.eco.bio7.worldwind.WorldWindOptionsView");
			
		}
		{
			IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.TOP, 0.5f, WorldWindOptionsView.ID);
			folderLayout.addView("org.eclipse.ui.navigator.ProjectExplorer");
		}
		IFolderLayout Right = layout.createFolder("Right", IPageLayout.BOTTOM, 0.5f, IPageLayout.ID_EDITOR_AREA);//$NON-NLS-1$
		
				Right.addView(WorldWindView.ID);

	}

	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {

	}

	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {

	}

}
