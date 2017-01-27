package com.eco.bio7.document;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

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
		layout.addView("org.eclipse.ui.views.ContentOutline", IPageLayout.LEFT, 0.22f, "org.eclipse.ui.console.ConsoleView");
		layout.addView("org.eclipse.ui.views.ResourceNavigator", IPageLayout.LEFT, 0.23f, IPageLayout.ID_EDITOR_AREA);
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
