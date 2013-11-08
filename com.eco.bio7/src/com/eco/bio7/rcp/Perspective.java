package com.eco.bio7.rcp;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.database.Spreadview;
import com.eco.bio7.discrete.StateButtonView;
import com.eco.bio7.discrete.Quadview;
import com.eco.bio7.info.InfoView;
import com.eco.bio7.plot.LineChartView;
import com.eco.bio7.plot.PieChartView;

public class Perspective implements IPerspectiveFactory {

	private IWorkbenchWindow window;

	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		IFolderLayout topLeft = layout.createFolder(
				"topLeft", IPageLayout.LEFT, (float) 0.24, editorArea);//$NON-NLS-1$
		topLeft.addView(StateButtonView.ID);
		topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);

		IFolderLayout bottomLeft = layout.createFolder("bottomLeft",
				IPageLayout.BOTTOM, (float) 0.30, "topLeft");
		bottomLeft.addView(InfoView.ID);
		

		layout.addStandaloneView(Spreadview.ID, true, IPageLayout.TOP, 0.3f,
				editorArea);

		layout.addStandaloneView(Quadview.ID, true, IPageLayout.RIGHT, 0.4f,
				editorArea);
		layout.addStandaloneView(LineChartView.ID, true, IPageLayout.BOTTOM,
				0.5f, editorArea);
		layout.addStandaloneView(PieChartView.ID, true, IPageLayout.TOP, 0.5f,
				editorArea);

	}

}
