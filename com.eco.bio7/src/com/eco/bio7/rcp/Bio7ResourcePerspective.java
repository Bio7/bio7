package com.eco.bio7.rcp;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class Bio7ResourcePerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);

		IFolderLayout topLeft = layout.createFolder(
				"topLeft", IPageLayout.LEFT, (float) 0.23, editorArea);
		IFolderLayout topRigtht = layout.createFolder(
				"topRight", IPageLayout.BOTTOM, (float) 0.6, editorArea);

		IFolderLayout viewRight = layout.createFolder(
				"viewRight", IPageLayout.BOTTOM, (float) 0.60,
				"topLeft");
		topLeft.addView("org.eclipse.ui.navigator.ProjectExplorer");
		viewRight.addView("org.eclipse.ui.views.ContentOutline");
		viewRight.addPlaceholder("com.eco.bio7.rbridge.debug.DebugVariablesView"); //Dynamic. Not Shown at startup!
		topRigtht.addView("org.eclipse.ui.console.ConsoleView");
		topRigtht.addView("org.eclipse.ui.views.PropertySheet");
		topRigtht.addView("org.eclipse.wst.common.snippets.internal.ui.SnippetsView");
		

	}

}
