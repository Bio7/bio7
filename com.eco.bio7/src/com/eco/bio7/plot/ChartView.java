package com.eco.bio7.plot;

import javax.swing.JPanel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderAdapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartPanel;

import com.eco.bio7.image.CanvasView;

/**
 * This class provides static methods to create a custom chart panel inside of Bio7.
 * 
 * @author Bio7
 *
 */
public class ChartView extends ViewPart {

	private static ChartPanel panelchart = null;

	public static int insertMark = -1;

	private static final String ID = "com.eco.bio7.custom_chart";

	private static Composite parent2;

	private static JPanel current;

	private static CTabFolder tabFolder;

	

	public void createPartControl(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
				"com.eco.bio7.imagej");
		getViewSite().getPage().addPartListener(new IPartListener() {
			public void partActivated(IWorkbenchPart part) {
				if (part instanceof CanvasView) {

				}
			}

			public void partBroughtToTop(IWorkbenchPart part) {

			}

			public void partClosed(IWorkbenchPart part) {
				if (part instanceof CanvasView) {

				}
			}

			public void partDeactivated(IWorkbenchPart part) {
				if (part instanceof CanvasView) {

				}
			}

			public void partOpened(IWorkbenchPart part) {
				if (part instanceof CanvasView) {

				}
			}
		});
		Display display = Display.getDefault();
		this.parent2 = parent;

		tabFolder = new CTabFolder(parent, SWT.TOP);
		tabFolder.setBorderVisible(true);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
		tabFolder.setSimple(false);

		tabFolder.setSelectionBackground(new Color[] {
				display.getSystemColor(SWT.COLOR_DARK_GREEN),
				display.getSystemColor(SWT.COLOR_DARK_GREEN) },
				new int[] { 90 }, true);
		tabFolder.setSelectionForeground(display
				.getSystemColor(SWT.COLOR_WHITE));

		tabFolder.addCTabFolderListener(new CTabFolderAdapter() {
			public void itemClosed(CTabFolderEvent event) {

			}

		});
		tabFolder.addSelectionListener(new SelectionListener() {
			public void itemClosed(CTabFolderEvent event) {

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

			public void widgetSelected(SelectionEvent e) {

			}

		});

	}

	public void setFocus() {
		tabFolder.setFocus();

	}

	/**
	 * Creates a JFreeChart tab from the given chart panel.
	 * 
	 * @param chartpanel a JFreeChart chart panel.
	 * @param title the title of the tab.
	 */
	public static void setChart(ChartPanel chartpanel, String title) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchPage page = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					page.showView("com.eco.bio7.custom_chart");
				} catch (PartInitException e) {

					e.printStackTrace();
				}

			}
		});

		SwtAwt swt = new SwtAwt(chartpanel);
		swt.addTab(title);
	}

	protected static JPanel getCurrent() {
		return current;
	}

	protected static void setCurrent(JPanel current) {
		ChartView.current = current;
	}

	protected static Composite getParent2() {
		return parent2;
	}

	protected static CTabFolder getTabFolder() {
		return tabFolder;
	}

}
