package com.eco.bio7.collection;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class CustomPerspective implements IPerspectiveFactory {

	/**
	 * Creates the initial layout for a page.
	 */
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		addFastViews(layout);
		addViewShortcuts(layout);
		addPerspectiveShortcuts(layout);

		layout.setEditorAreaVisible(false);
		IFolderLayout left = layout.createFolder("MainLeft", IPageLayout.LEFT, (float) 0.25, editorArea);//$NON-NLS-1$
		IFolderLayout Right = layout.createFolder("MainRight", IPageLayout.RIGHT, (float) 0.75, editorArea);

		IFolderLayout subLeftTop = layout.createFolder("SubLeftTop", IPageLayout.TOP, (float) 0.5, "MainLeft");//$NON-NLS-1$
		IFolderLayout subLeftBottom = layout.createFolder("SubLeftBottom", IPageLayout.BOTTOM, (float) 0.5, "MainLeft");//$NON-NLS-1$

		IFolderLayout subRightTop = layout.createFolder("SubRightTop", IPageLayout.TOP, (float) 0.5, "MainRight");//$NON-NLS-1$
		IFolderLayout subRightBottom = layout.createFolder("SubRightBottom", IPageLayout.BOTTOM, (float) 0.5, "MainRight");

		IFolderLayout subRightBottomLeft = layout.createFolder("SubRightBottomLeft", IPageLayout.LEFT, (float) 0.5, "SubRightBottom");
		IFolderLayout subRightBottomRight = layout.createFolder("SubRightBottomRight", IPageLayout.RIGHT, (float) 0.5, "SubRightBottom");

		IFolderLayout subRightBottomLeftTop = layout.createFolder("subRightBottomLeftTop", IPageLayout.TOP, (float) 0.5, "SubRightBottomLeft");
		IFolderLayout subRightBottomLeftBottom = layout.createFolder("subRightBottomLeftTop", IPageLayout.BOTTOM, (float) 0.5, "SubRightBottomLeft");

		// add placeholders for the views
		left.addPlaceholder("com.eco.bio7.custom_controls:left");
		Right.addPlaceholder("com.eco.bio7.custom_controls:right");

		subLeftTop.addPlaceholder("com.eco.bio7.custom_controls:subLeftTop");
		subLeftBottom.addPlaceholder("com.eco.bio7.custom_controls:subLeftBottom");

		subRightTop.addPlaceholder("com.eco.bio7.custom_controls:subRightTop");
		subRightBottom.addPlaceholder("com.eco.bio7.custom_controls:subRightBottom");

		subRightBottomLeft.addPlaceholder("com.eco.bio7.custom_controls:subRightBottomLeft");
		subRightBottomRight.addPlaceholder("com.eco.bio7.custom_controls:subRightBottomRight");

		subRightBottomLeftTop.addPlaceholder("com.eco.bio7.custom_controls:subRightBottomLeftTop");
		subRightBottomLeftBottom.addPlaceholder("com.eco.bio7.custom_controls:subRightBottomLeftBottom");

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
