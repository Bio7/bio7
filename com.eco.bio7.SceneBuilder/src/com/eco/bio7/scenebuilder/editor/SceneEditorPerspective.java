package com.eco.bio7.scenebuilder.editor;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class SceneEditorPerspective implements IPerspectiveFactory {

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
		

		
		topLeft.addView("com.eco.bio7.scenebuilder.library");
		topLeft.addView("org.eclipse.ui.views.ResourceNavigator");	
		viewRight.addView("com.eco.bio7.scenebuilder.hierachy");		
		viewRight.addView("org.eclipse.ui.views.ContentOutline");		
		topRigtht.addView("org.eclipse.ui.console.ConsoleView");
		topRigtht.addView("org.eclipse.ui.views.PropertySheet");
		topRigtht.addView("org.eclipse.wst.common.snippets.internal.ui.SnippetsView");	
		layout.addView("com.eco.bio7.scenebuilder.inspector", IPageLayout.RIGHT, 0.66f, editorArea);

		

	}

}
