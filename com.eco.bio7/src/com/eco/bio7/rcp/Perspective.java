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
		IFolderLayout left = layout.createFolder("MainLeft", IPageLayout.LEFT, (float) 0.25, editorArea);//$NON-NLS-1$
		IFolderLayout Right = layout.createFolder("MainRight", IPageLayout.RIGHT, (float) 0.75, editorArea);
		
		IFolderLayout subLeftTop = layout.createFolder("SubLeftTop", IPageLayout.TOP, (float) 0.3, "MainLeft");//$NON-NLS-1$
		IFolderLayout subLeftBottom = layout.createFolder("SubLeftBottom", IPageLayout.BOTTOM, (float) 0.75, "MainLeft");//$NON-NLS-1$
		
		
		IFolderLayout subRightTop = layout.createFolder("SubRightTop", IPageLayout.TOP, (float) 0.3, "MainRight");//$NON-NLS-1$
		
		IFolderLayout subRightBottom = layout.createFolder("SubRightBottom", IPageLayout.BOTTOM, (float) 0.75, "MainRight");
		
		IFolderLayout subRightBottomLeft = layout.createFolder("SubRightBottomLeft", IPageLayout.LEFT, (float) 0.4, "SubRightBottom");
		
		IFolderLayout subRightBottomRight = layout.createFolder("SubRightBottomRight", IPageLayout.RIGHT, (float) 0.6, "SubRightBottom");
		
		IFolderLayout subRightBottomLeftTop = layout.createFolder("subRightBottomLeftTop", IPageLayout.TOP, (float) 0.5, "SubRightBottomLeft");
		
		IFolderLayout subRightBottomLeftBottom = layout.createFolder("subRightBottomLeftTop", IPageLayout.BOTTOM, (float) 0.5, "SubRightBottomLeft");
		
		
		
		
		subLeftTop.addView(StateButtonView.ID);
		//topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		subLeftBottom.addView(InfoView.ID);
		
		subRightTop.addView(Spreadview.ID);
		
		subRightBottomRight.addView(Quadview.ID);

		subRightBottomLeftTop.addView(LineChartView.ID);
		
		subRightBottomLeftBottom.addView(PieChartView.ID);
		
		

	}

}
