package com.eco.bio7.discrete3d;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;

//import com.eco.bio7.soil.NitrateView;
import com.eco.bio7.spatial.Options3d;
import com.eco.bio7.spatial.SpatialView;

public class Perspective3d implements IPerspectiveFactory, IPerspectiveListener {

	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		IFolderLayout Right = layout.createFolder("Right", IPageLayout.RIGHT, (float) 0.75, editorArea);//$NON-NLS-1$

		Right.addView(SpatialView.ID);
		//Right.addView(Quad3dview.ID);
		layout.addView(Options3d.ID, IPageLayout.LEFT, 0.30f, "Right");

	}

	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		//Quad3dview.getAnimator().stop();

	}

	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		//Quad3dview.getAnimator().stop();

	}

}
