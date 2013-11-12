package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import java.awt.Panel;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * This class provides some static methods for the creation of custom views inside the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class CustomView extends ViewPart implements ISaveablePart2 {

	protected int insertMark = -1;

	private static final String ID = "com.eco.bio7.custom_controls";

	private Composite customViewParent;

	private Panel awtCurrent;

	private String secId;

	protected IViewPart activated;

	protected boolean singleView = false;

	private ImagePlus plus;

	private ImageWindow win;

	private JPanel viewPanel;

	public IViewReference ref2;

	public CustomView() {

	}

	public void setData(ImagePlus plu, ImageWindow win) {
		this.plus = plu;
		this.win = win;

	}

	public void createPartControl(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagej");

		this.customViewParent = parent;
		// getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);

		// secondaryId = getViewSite().getSecondaryId();

	}

	public void setFocus() {

	}

	class ImageJPartListener2 implements IPartListener2 {
		public void partActivated(IWorkbenchPartReference ref) {

			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			if (page != null) {

				ref2 = page.findViewReference("com.eco.bio7.image.detachedImage", secId);

				if (ref.equals(ref2)) {

					WindowManager.setTempCurrentImage(plus);
					WindowManager.setCurrentWindow(win);

					CanvasView.setCurrent(viewPanel);
					System.out.println(" view activated!");
				}
			}
		}

		public void partBroughtToTop(IWorkbenchPartReference ref) {

		}

		public void partClosed(IWorkbenchPartReference ref) {

			IWorkbenchPage page = ref.getPage();
			if (page != null) {
				page.getActivePart();
				System.out.println("page not null");

				if (ref.equals(ref2)) {
					SwingUtilities.invokeLater(new Runnable() {
						// !!
						public void run() {
							win.bio7TabClose();
						}
					});

					System.out.println("part closed!");

				}
			}

		}

		public void partDeactivated(IWorkbenchPartReference ref) {

		}

		public void partOpened(IWorkbenchPartReference ref) {

		}

		public void partHidden(IWorkbenchPartReference ref) {

		}

		public void partVisible(IWorkbenchPartReference ref) {

		}

		public void partInputChanged(IWorkbenchPartReference ref) {

		}
	}

	/*
	 * private IPartListener2 partListener = new IPartListener2() {
	 * 
	 * @Override public void partActivated(IWorkbenchPartReference partRef) { //
	 * 
	 * 
	 * 
	 * }
	 * 
	 * public void partBroughtToTop(IWorkbenchPartReference partRef) { // TODO // Auto-generated // method // stub
	 * 
	 * }
	 * 
	 * public void partClosed(IWorkbenchPartReference partRef) { // TODO
	 * 
	 * }
	 * 
	 * public void partDeactivated(IWorkbenchPartReference partRef) { // TODO //
	 * 
	 * }
	 * 
	 * public void partOpened(IWorkbenchPartReference partRef) {
	 * 
	 * if (partRef.getId().equals("com.eco.bio7.javaeditors.TemplateEditor")) {
	 * 
	 * }
	 * 
	 * 
	 * //System.out.println("view opened!");
	 * 
	 * 
	 * }
	 * 
	 * public void partHidden(IWorkbenchPartReference partRef) { // TODO
	 * 
	 * }
	 * 
	 * public void partVisible(IWorkbenchPartReference partRef) { // TODO
	 * 
	 * }
	 * 
	 * public void partInputChanged(IWorkbenchPartReference partRef) { // TODO // Auto-generated // method // stub
	 * 
	 * }
	 * 
	 * };
	 */

	/**
	 * Creates a given JPanel tab inside a custom view.
	 * 
	 * @param jpanel
	 *            a JPanel
	 * @param title
	 *            the title of the tab.
	 */
	public void setPanel(final JPanel jpanel, final String id) {
		secId = id;
		viewPanel = jpanel;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					if (singleView) {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						activated = page.showView("com.eco.bio7.image.detachedImage");
					} else {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_CREATE);
						activated = page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_ACTIVATE);

						ImageJPartListener2 palist = new ImageJPartListener2();
						page.addPartListener(palist);
					}

				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (activated instanceof CustomView) {
					CustomView view = (CustomView) activated;
					/*
					 * Control c[] = view.getCustomViewParent().getChildren(); for (int i = 0; i < c.length; i++) { c[i].dispose(); }
					 */

					SwtAwtCustom swt = new SwtAwtCustom(viewPanel, view);
					swt.addTab(id);

				}

			}
		});

	}

	/**
	 * Creates a given Panel tab inside a custom view.
	 * 
	 * @param panel
	 *            a Panel
	 * @param title
	 *            the title of the tab.
	 */
	public void setPanel(final Panel panel, final String id) {

		secId = id;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					if (singleView) {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						activated = page.showView("com.eco.bio7.image.detachedImage");
					} else {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_CREATE);
						activated = page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_ACTIVATE);
					}

				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (activated instanceof CustomView) {
					CustomView view = (CustomView) activated;
					Control c[] = view.getCustomViewParent().getChildren();
					Vector ve = (Vector) view.getCustomViewParent().getData();
					if (ve != null && ve.size() > 0) {
						closeTabPanels(ve);
					}
					for (int i = 0; i < c.length; i++) {
						// System.out.println(c[i]);
						c[i].dispose();
					}
					SwtAwtCustom swt = new SwtAwtCustom(panel, view);
					swt.addAWTTab(id);
				}

			}
		});

	}

	/**
	 * Returns the display of the view.
	 * 
	 * @return the display of the view.
	 */
	public static Display getDisplay() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		return display;

	}

	protected void setAwtCurrent(Panel current) {
		this.awtCurrent = current;
	}

	protected Composite getCustomViewParent() {
		return customViewParent;
	}

	protected void setCustomViewParent(Composite parent2) {
		this.customViewParent = parent2;
	}

	/*
	 * IPartListener is not working to detect the view close because the method "closedView" is called after the view has been closed! However we can use the editor ISaveablePart2 interface which mimics this behaviour for views. The views are marked like a non saved editor with a '*' char! In the
	 * close event (do save) we clean up all data and close the tab items!
	 */
	public void doSave(IProgressMonitor monitor) {

		/*
		 * CTabItem[] items = tabFolder.getItems();
		 * 
		 * if (items.length > 0) { for (int i = 0; i < items.length; i++) { Vector ve = (Vector) items[i].getData();
		 * 
		 * closeTabPanels(ve); items[i].dispose(); }
		 * 
		 * }
		 */
		Vector<?> ve = (Vector<?>) customViewParent.getData();
		closeTabPanels(ve);

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public boolean isDirty() {
		// Needed to save on close!
		return true;
	}

	@Override
	public boolean isSaveAsAllowed() {

		return false;
	}

	@Override
	public boolean isSaveOnCloseNeeded() {
		// Needed to save on close!
		return true;
	}

	@Override
	public int promptToSaveOnClose() {

		return 0;
	}

	/*
	 * Close tab items and dispose different GUI references. Called if the view or a tab item is closed!
	 */
	private void closeTabPanels(Vector ve) {
		if (ve != null) {

			if ((ve.get(0)) instanceof javax.swing.JPanel) {
				JPanel panel = (JPanel) ve.get(0);
				panel = null;

			}

		}
	}

	public void setSingleView(boolean singleView) {
		this.singleView = singleView;
	}

}