package com.eco.bio7.discrete3d;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.eco.bio7.spatial.SpatialView;

public class Perspective3d implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);
		IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.LEFT, 0.30f, IPageLayout.ID_EDITOR_AREA);
		folderLayout.addView("com.eco.bio7.discrete3d.Options3d");
		folderLayout.addView("org.eclipse.ui.console.ConsoleView");

		// Put Project Explorer and the custom controls placeholder in the same tab folder
		IFolderLayout explorerFolder = layout.createFolder("explorerFolder", IPageLayout.TOP, 0.5f, "folder");
		explorerFolder.addView("org.eclipse.ui.navigator.ProjectExplorer");
		explorerFolder.addPlaceholder("com.eco.bio7.custom_controls:*");

		IFolderLayout Right = layout.createFolder("Right", IPageLayout.BOTTOM, 0.5f, IPageLayout.ID_EDITOR_AREA);
		Right.addView(SpatialView.ID);
	}

}
