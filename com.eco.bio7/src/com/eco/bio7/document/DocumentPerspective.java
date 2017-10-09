package com.eco.bio7.document;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IFolderLayout;

public class DocumentPerspective implements IPerspectiveFactory {

	/**
	 * Creates the initial layout for a page.
	 */
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		addFastViews(layout);
		addViewShortcuts(layout);
		addPerspectiveShortcuts(layout);
		layout.addPlaceholder("com.eco.bio7.custom_controls", IPageLayout.RIGHT, 0.55f, IPageLayout.ID_EDITOR_AREA);
		layout.addView("org.eclipse.ui.console.ConsoleView", IPageLayout.BOTTOM, 0.72f, IPageLayout.ID_EDITOR_AREA);
		layout.addView("org.eclipse.ui.views.ContentOutline", IPageLayout.LEFT, 0.23f, "org.eclipse.ui.console.ConsoleView");
		{
			IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.LEFT, 0.23f, IPageLayout.ID_EDITOR_AREA);
			folderLayout.addView("org.eclipse.ui.navigator.ProjectExplorer");
		}
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
