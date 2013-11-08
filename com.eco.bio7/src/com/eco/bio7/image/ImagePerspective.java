package com.eco.bio7.image;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import com.eco.bio7.image.thumbs.ThumbnailsView;

public class ImagePerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);

		IFolderLayout Right = layout.createFolder(
				"Right", IPageLayout.RIGHT, (float) 0.5, editorArea);//$NON-NLS-1$
		Right.addView(PointPanelView.ID);

		Right.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		// Bottom left.
		IFolderLayout Left = layout.createFolder(
				"Left", IPageLayout.LEFT, (float) 0.5, editorArea);//$NON-NLS-1$
		Left.addView(CanvasView.ID);

		IFolderLayout bottomLeft = layout.createFolder(
				"bottomLeft", IPageLayout.BOTTOM, (float) 0.50,//$NON-NLS-1$
				"Right");
		bottomLeft.addView(ThumbnailsView.ID);

	}

}
