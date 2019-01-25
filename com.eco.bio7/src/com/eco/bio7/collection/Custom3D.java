/*package com.eco.bio7.collection;

import java.util.Vector;
import com.jogamp.opengl.GLCanvas;
import javax.swing.JPanel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderAdapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
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
import com.eco.bio7.image.CanvasView;

*//**
 * This class provides some static methods for the creation of
 * a custom 3d view inside the Bio7 application.
 * 
 * @author Bio7
 *
 *//*
public class Custom3D extends ViewPart {

	private static int insertMark = -1;

	private static final String ID = "com.eco.bio7.custom_3d";

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
				if (part instanceof Custom3D) {
					CTabItem[] items = tabFolder.getItems();

					if (items.length > 0) {
						for (int i = 0; i < items.length; i++) {
							Vector ve = (Vector) items[i].getData();
							if ((ve.get(0)) instanceof com.sun.opengl.util.Animator) {
								com.sun.opengl.util.Animator anim = (com.sun.opengl.util.Animator) ve
										.get(0);
								anim.stop();

							} else {
								JPanel panel = (JPanel) ve.get(0);
								panel = null;

							}
						}
					}
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

				Vector ve = (Vector) event.item.getData();
				if ((ve.get(0)) instanceof com.sun.opengl.util.Animator) {
					com.sun.opengl.util.Animator anim = (com.sun.opengl.util.Animator) ve
							.get(0);
					anim.stop();
				} else {
					JPanel panel = (JPanel) ve.get(0);
					panel = null;
				}

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

	*//**
	 * Creates a given JOGL GLCanvas tab inside a custom 3d view.
	 * 
	 * @param canvas a GLCanvas
	 * @param title the title of the tab.
	 * @param anim the animator if available.
	 *//*
	public static void setPanel(GLCanvas canvas, String title,
			com.sun.opengl.util.Animator anim) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchPage page = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					page.showView("com.eco.bio7.custom_3d");
				} catch (PartInitException e) {

					e.printStackTrace();
				}

			}
		});

		SwtAwtCustom swt = new SwtAwtCustom(canvas, anim);
		swt.addCanvasTab(title);
	}

	protected static Composite getParent2() {
		return parent2;
	}

	protected static void setParent2(Composite parent2) {
		Custom3D.parent2 = parent2;
	}

	protected static CTabFolder getTabFolder() {
		return tabFolder;
	}

	protected static void setTabFolder(CTabFolder tabFolder) {
		Custom3D.tabFolder = tabFolder;
	}

	protected static JPanel getCurrent() {
		return current;
	}

	protected static void setCurrent(JPanel current) {
		Custom3D.current = current;
	}

	protected static int getInsertMark() {
		return insertMark;
	}

	protected static void setInsertMark(int insertMark) {
		Custom3D.insertMark = insertMark;
	}

}*/