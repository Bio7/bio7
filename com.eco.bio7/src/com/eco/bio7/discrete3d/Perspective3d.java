package com.eco.bio7.discrete3d;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import com.eco.bio7.spatial.SpatialView;

public class Perspective3d implements IPerspectiveFactory, IPerspectiveListener {

	public void createInitialLayout(IPageLayout layout) {

		//String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		{
			IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.LEFT, 0.30f, IPageLayout.ID_EDITOR_AREA);
			folderLayout.addView("com.eco.bio7.discrete3d.Options3d");
			folderLayout.addView("org.eclipse.ui.console.ConsoleView");
		}
		layout.addView("org.eclipse.ui.navigator.ProjectExplorer", IPageLayout.TOP, 0.5f, "folder");
		IFolderLayout Right = layout.createFolder("Right", IPageLayout.BOTTOM, 0.5f, IPageLayout.ID_EDITOR_AREA);//$NON-NLS-1$
		
				Right.addView(SpatialView.ID);
				

	}

	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		//Quad3dview.getAnimator().stop();

	}

	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		//Quad3dview.getAnimator().stop();

	}

}
