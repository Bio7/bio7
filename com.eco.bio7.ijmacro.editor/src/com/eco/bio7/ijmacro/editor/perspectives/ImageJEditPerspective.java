package com.eco.bio7.ijmacro.editor.perspectives;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IFolderLayout;

public class ImageJEditPerspective implements IPerspectiveFactory {

	/**
	 * Creates the initial layout for a page.
	 */
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		addFastViews(layout);
		addViewShortcuts(layout);
		addPerspectiveShortcuts(layout);
		layout.addView("org.eclipse.ui.navigator.ProjectExplorer", IPageLayout.LEFT, 0.15f, IPageLayout.ID_EDITOR_AREA);
		{
			IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.BOTTOM, 0.5f, "org.eclipse.ui.navigator.ProjectExplorer");
			folderLayout.addView("org.eclipse.ui.views.ContentOutline");
			folderLayout.addView("org.eclipse.ui.console.ConsoleView");
			folderLayout.addView("com.eco.bio7.ijmacro.editor.toolbar.debug");
			
		}
		layout.addView("com.eco.bio7.imagej", IPageLayout.RIGHT, 0.5f, IPageLayout.ID_EDITOR_AREA);
		layout.addPlaceholder("com.eco.bio7.image.detachedImage", IPageLayout.BOTTOM, 0.5f, "com.eco.bio7.imagej");
		layout.addPlaceholder("com.eco.bio7.thumbnails", IPageLayout.BOTTOM, 0.5f, IPageLayout.ID_EDITOR_AREA);
	}

	/**
	 * Add fast views to the perspective.
	 */
	private void addFastViews(IPageLayout layout) {
	}

	/**
	 * Add view shortcuts to the perspective.
	 */
	private void addViewShortcuts(IPageLayout layout) {
	}

	/**
	 * Add perspective shortcuts to the perspective.
	 */
	private void addPerspectiveShortcuts(IPageLayout layout) {
	}

}
