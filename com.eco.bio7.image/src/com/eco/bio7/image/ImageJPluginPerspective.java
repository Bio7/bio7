package com.eco.bio7.image;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import com.eco.bio7.image.thumbs.ThumbnailsView;

public class ImageJPluginPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		IFolderLayout Left = layout.createFolder(
				"Left", IPageLayout.LEFT, (float) 0.7, editorArea);//$NON-NLS-1$
		Left.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		Left.addView(CanvasView.ID);
		
		IFolderLayout Right = layout.createFolder(
				"Right", IPageLayout.RIGHT, (float) 0.3, editorArea);//$NON-NLS-1$
		
		Right.addView(ThumbnailsView.ID);
		
		
		

	}

}
