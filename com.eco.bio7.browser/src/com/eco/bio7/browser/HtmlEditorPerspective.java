package com.eco.bio7.browser;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class HtmlEditorPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, (float) 0.25, editorArea);

		IFolderLayout viewRight = layout.createFolder("viewRight", IPageLayout.BOTTOM, (float) 0.40, editorArea);

		topLeft.addView("org.eclipse.ui.views.ResourceNavigator");
		viewRight.addView("com.eco.bio7.browser.Browser");

	}

}
