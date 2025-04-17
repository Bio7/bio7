package com.eco.bio7.collection;

import java.awt.Panel;
import java.util.Vector;
import javax.swing.JPanel;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartPanel;
import com.eco.bio7.compile.Model;
import com.eco.bio7.methods.Compiled;
import com.jogamp.opengl.util.Animator;


/**
 * This class provides some static methods for the creation of custom views
 * inside the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class CustomView extends ViewPart {// implements ISaveablePart2

	private ChartPanel panelchart = null;

	protected int insertMark = -1;

	private static final String ID = "com.eco.bio7.custom_controls";

	private Composite customViewParent;

	private Panel awtCurrent;

	private String secId;

	protected IViewPart activated;

	protected boolean singleView = false;

	public CustomView() {

	}

	public void createPartControl(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagej");

		/*
		 * Add a nested composite to use addData for a reference to the different
		 * panels. The parent composite seems to set addDatasince Eclipse 4.4M6!
		 */
		this.customViewParent = new Composite(parent, SWT.NONE);
		customViewParent.setLayout(new FillLayout());
		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				Vector<?> ve = (Vector<?>) customViewParent.getData();
				closeTabPanels(ve);

				/* Call the close method of the Model class! */
				Model model = Compiled.getModel();
				/* Check if compiled! */
				if (model != null) {
					try {
						model.close();
					} catch (Exception ev) {

						ev.printStackTrace();
					}
				}

			}
		});

	}

	public void setFocus() {

	}

	/**
	 * Creates a given JPanel tab inside a custom view.
	 * 
	 * @param jpanel a JPanel
	 * @param id     the id and name of the tab.
	 */
	public void setPanel(final JPanel jpanel, final String id) {
		setPanel(jpanel, id, null);
	}

	/**
	 * Creates a given JPanel tab inside a custom view.
	 * 
	 * @param jpanel a JPanel
	 * @param id     the id and name of the tab.
	 * @param image  the image of the tab.
	 */
	public void setPanel(final JPanel jpanel, final String id, Image image) {
		secId = id;

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					if (singleView) {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						activated = page.showView("com.eco.bio7.custom_controls");
					} else {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("com.eco.bio7.custom_controls", secId, IWorkbenchPage.VIEW_CREATE);
						activated = page.showView("com.eco.bio7.custom_controls", secId, IWorkbenchPage.VIEW_ACTIVATE);
					}

				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (activated instanceof CustomView) {
					CustomView view = (CustomView) activated;
					view.setPartName(id);
					if (image != null) {
						view.setTitleImage(image);
					}
					display.update();
					Control c[] = view.getCustomViewParent().getChildren();
					for (int i = 0; i < c.length; i++) {
						c[i].dispose();
					}
					SwtAwtCustom swt = new SwtAwtCustom(jpanel, view);
					swt.addTab(id);
				}

			}
		});

	}

	/**
	 * Creates a given Panel tab inside a custom view.
	 * 
	 * @param panel a Panel
	 * @param id    the id and name of the tab.
	 */
	public void setPanel(final Panel panel, final String id) {
		setPanel(panel, id, null);
	}

	/**
	 * Creates a given Panel tab inside a custom view.
	 * 
	 * @param panel a Panel
	 * @param id    the id and name of the tab.
	 * @param image the image of the tab.
	 */
	public void setPanel(final Panel panel, final String id, Image image) {
		secId = id;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					if (singleView) {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						activated = page.showView("com.eco.bio7.custom_controls");
					} else {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("com.eco.bio7.custom_controls", secId, IWorkbenchPage.VIEW_CREATE);
						activated = page.showView("com.eco.bio7.custom_controls", secId, IWorkbenchPage.VIEW_ACTIVATE);
					}

				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (activated instanceof CustomView) {
					CustomView view = (CustomView) activated;
					view.setPartName(id);
					if (image != null) {
						view.setTitleImage(image);
					}
					display.update();
					Control c[] = view.getCustomViewParent().getChildren();
					Vector<?> ve = (Vector<?>) view.getCustomViewParent().getData();
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
	 * Returns a SWT composite and creates a custom view with a tab.
	 * 
	 * @param id the id and name of the tab.
	 * @return a swt composite.
	 * 
	 */
	public Composite getComposite(String id) {
		return getComposite(id, null);
	}

	/**
	 * Returns a SWT composite and creates a custom view with a tab.
	 * 
	 * @param id the id and name of the tab.
	 * @return a swt composite.
	 * @param image the image of the tab.
	 */
	public Composite getComposite(String id, Image image) {
		secId = id;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					if (singleView) {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						activated = page.showView("com.eco.bio7.custom_controls");
					} else {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("com.eco.bio7.custom_controls", secId, IWorkbenchPage.VIEW_CREATE);
						activated = page.showView("com.eco.bio7.custom_controls", secId, IWorkbenchPage.VIEW_ACTIVATE);
					}

				} catch (PartInitException e) {

					e.printStackTrace();
				}

			}
		});

		CustomView viewReturn = null;
		if (activated instanceof CustomView) {
			final CustomView view = (CustomView) activated;
			view.setPartName(id);
			if (image != null) {
				view.setTitleImage(image);
			}
			display.update();
			display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					Control c[] = view.getCustomViewParent().getChildren();
					for (int i = 0; i < c.length; i++) {
						c[i].dispose();
					}
				}
			});

			viewReturn = view;
		}
		return viewReturn.getCustomViewParent();
	}

	/**
	 * Returns a Draw2d LightweightSystem and creates a custom view with a tab.
	 * 
	 * @param id the id and name of the tab.
	 * @return a LightweightSystem component.
	 */
	public LightweightSystem getDraw2d(String id) {
		return getDraw2d(id, null);
	}

	/**
	 * Returns a Draw2d LightweightSystem and creates a custom view with a tab.
	 * 
	 * @param id the id and name of the tab.
	 * @return a LightweightSystem component.
	 * @param image the image of the tab.
	 */
	public LightweightSystem getDraw2d(String id, Image image) {
		secId = id;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					if (singleView) {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						activated = page.showView("com.eco.bio7.custom_controls");
					} else {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("com.eco.bio7.custom_controls", secId, IWorkbenchPage.VIEW_CREATE);
						activated = page.showView("com.eco.bio7.custom_controls", secId, IWorkbenchPage.VIEW_ACTIVATE);
					}

				} catch (PartInitException e) {

					e.printStackTrace();
				}

			}
		});
		LightweightSystem light = null;
		if (activated instanceof CustomView) {
			final CustomView view = (CustomView) activated;
			view.setPartName(id);
			if (image != null) {
				view.setTitleImage(image);
			}
			display.update();
			display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					Control c[] = view.getCustomViewParent().getChildren();
					for (int i = 0; i < c.length; i++) {
						c[i].dispose();
					}
				}
			});
			Draw2dCustom swt = new Draw2dCustom(view);
			light = swt.addTab(id);
		}
		return light;
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

	protected ChartPanel getPanelchart() {
		return panelchart;
	}

	protected void setPanelchart(ChartPanel panelchart) {
		this.panelchart = panelchart;
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
	 * IPartListener is not working to detect the view close because the method
	 * "closedView" is called after the view has been closed! However we can use the
	 * editor ISaveablePart2 interface which mimics this behaviour for views. The
	 * views are marked like a non saved editor with a '*' char! In the close event
	 * (do save) we clean up all data and close the tab items!
	 */
	/*
	 * public void doSave(IProgressMonitor monitor) {
	 * 
	 * 
	 * CTabItem[] items = tabFolder.getItems();
	 * 
	 * if (items.length > 0) { for (int i = 0; i < items.length; i++) { Vector ve =
	 * (Vector) items[i].getData();
	 * 
	 * closeTabPanels(ve); items[i].dispose(); }
	 * 
	 * }
	 * 
	 * Vector<?> ve = (Vector<?>) customViewParent.getData(); closeTabPanels(ve);
	 * 
	 * Call the close method of the Model class! Model model=Compiled.getModel();
	 * Check if compiled! if (model != null) { try { model.close(); } catch
	 * (Exception e) { // TODO Auto-generated catch block e.printStackTrace(); } }
	 * 
	 * 
	 * }
	 * 
	 * @Override public void doSaveAs() {
	 * 
	 * }
	 * 
	 * @Override public boolean isDirty() { // Needed to save on close! return true;
	 * }
	 * 
	 * @Override public boolean isSaveAsAllowed() {
	 * 
	 * return false; }
	 * 
	 * @Override public boolean isSaveOnCloseNeeded() { // Needed to save on close!
	 * return true; }
	 * 
	 * @Override public int promptToSaveOnClose() {
	 * 
	 * return YES; }
	 */

	/*
	 * Close tab items and dispose different GUI references. Called if the view or a
	 * tab item is closed!
	 */
	private void closeTabPanels(Vector ve) {
		if (ve != null) {

			if ((ve.get(0)) instanceof Animator) {
				Animator anim = (Animator) ve.get(0);
				anim.stop();
			} else if ((ve.get(0)) instanceof javax.swing.JPanel) {
				JPanel panel = (JPanel) ve.get(0);
				panel = null;
			}

		}
	}

	public void setSingleView(boolean singleView) {
		this.singleView = singleView;
	}

}