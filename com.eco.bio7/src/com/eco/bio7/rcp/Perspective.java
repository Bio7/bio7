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
		layout.setEditorAreaVisible(true);
		IFolderLayout left = layout.createFolder("MainLeft", IPageLayout.LEFT, 0.2f, editorArea);//$NON-NLS-1$
		left.addView("org.eclipse.ui.navigator.ProjectExplorer");
		IFolderLayout Right = layout.createFolder("MainRight", IPageLayout.BOTTOM, 0.45f, IPageLayout.ID_EDITOR_AREA);
		Right.addView("com.eco.bio7.quadgrid");
		
		
		IFolderLayout subRightTop = layout.createFolder("SubRightTop", IPageLayout.RIGHT, 0.6f, IPageLayout.ID_EDITOR_AREA);
		
		subRightTop.addView(Spreadview.ID);
		
		IFolderLayout subLeftTop = layout.createFolder("SubLeftTop", IPageLayout.TOP, 0.5f, "SubRightTop");//$NON-NLS-1$
		
		
		
		
		subLeftTop.addView(StateButtonView.ID);
		{
			IFolderLayout folderLayout = layout.createFolder("folder", IPageLayout.BOTTOM, 0.45f, "org.eclipse.ui.navigator.ProjectExplorer");
			folderLayout.addView("com.eco.bio7.control");
			folderLayout.addView("org.eclipse.ui.console.ConsoleView");
		}
		layout.addView("com.eco.bio7.piechart", IPageLayout.RIGHT, 0.6f, "MainRight");
		layout.addView("com.eco.bio7.linechart", IPageLayout.BOTTOM, 0.5f, "com.eco.bio7.piechart");
		
		

	}

}
