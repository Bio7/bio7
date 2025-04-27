/*******************************************************************************
 * Copyright (c) 2007-2015 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.image;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;

/**
 * This class provides some static methods for the creation of custom views
 * inside the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class CustomDetachedImageJView extends ViewPart {//implements ISaveablePart2

	protected int insertMark = -1;

	private static final String ID = "com.eco.bio7.custom_controls";

	public Composite customViewParent;

	private Panel awtCurrent;

	private String secId;

	public ImagePlus plus;

	public ImageWindow win;

	public JPanel viewPanel;

	public IViewReference ref2;

	private CustomDetachedImageJView customView;

	protected ImageJPartListener2 palist;

	protected FXSwtAwtCustom swt;

	private boolean isDetached;

	protected IViewPart activated;

	public boolean isDetached() {
		return isDetached;
	}

	public CustomDetachedImageJView getCustomView() {
		return customView;
	}

	public CustomDetachedImageJView() {
		// customView=this;
	}

	public void setData(ImagePlus plu, ImageWindow win) {
		this.plus = plu;
		this.win = win;

	}

	/* Implement to overwrite function to set name! */
	public void setPartName(String partName) {
		super.setPartName(partName);
	}

	public void createPartControl(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagej");

		this.customViewParent = parent;
		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				Object data = customViewParent.getData();
				if (data instanceof Vector) {
					Vector<?> ve = (Vector<?>) data;
					closeTabPanels(ve);
				}

			}
		});

		parent.addControlListener(new ControlAdapter() {
			@Override
			public synchronized void controlResized(final ControlEvent e) {
				/*
				 * Here we write the values in the com.eco.bio7 plugin preferences with the help
				 * of scoped preferences!
				 */
				updateDetached();
				Rectangle rec = parent.getClientArea();

				IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");
				if (store != null) {
					String selection = store.getString("PLOT_DEVICE_SELECTION");
					String pathTo = store.getString("pathTempR");
					int correction = 0;
					if (CanvasView.tabFolder.isDisposed() == false && CanvasView.tabFolder != null) {
						/* Height correction for the plot! */
						correction = CanvasView.tabFolder.getTabHeight();
					}

					if (selection.equals("PLOT_IMAGEJ_DISPLAYSIZE_CAIRO")) {

						store.setValue("DEVICE_DEFINITION", ".bio7Device <- function(filename = \"" + pathTo
								+ "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = " + rec.width + ", height = "
								+ (rec.height - correction) + ", type=\"cairo\")}; options(device=\".bio7Device\")");
					} else if (selection.equals("PLOT_IMAGEJ_DISPLAYSIZE")) {
						store.setValue("DEVICE_DEFINITION",
								".bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff"
										+ "\") { tiff(filename,width =  " + rec.width + ", height = "
										+ (rec.height - correction)
										+ ", units = \"px\")}; options(device=\".bio7Device\")");

					}
				}
				/* Here we resize the ImageJ plot window! */
				ImageWindow currentPlotWindow = WindowManager.getCurrentWindow();

				if (currentPlotWindow != null) {
					//if (currentPlotWindow instanceof PlotWindow) {

					CanvasView view = CanvasView.getCanvas_view();
					view.layoutParent(parent);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							view.resizePlotWindow(parent, currentPlotWindow);
						}
					});
					//} else {
					//	parent.layout();
					//}
				} else {
					parent.layout();
				}
			}

		});

		updateDetached();

	}

	private void updateDetached() {
		isDetached = customViewParent.getShell().getText().length() == 0;
	}

	public void setFocus() {

	}

	class ImageJPartListener2 implements IPartListener2 {
		public void partActivated(IWorkbenchPartReference ref) {

			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			if (page != null) {

				ref2 = page.findViewReference("com.eco.bio7.image.detachedImage", secId);

				if (ref.equals(ref2)) {

					// Wrap to avoid deadlock of awt frame access!
					java.awt.EventQueue.invokeLater(new Runnable() {
						public void run() {
							WindowManager.setTempCurrentImage(plus);
							WindowManager.setCurrentWindow(win);
							if (Util.getOS().equals("Mac")) {
								if (swt != null) {
									Frame frameSwtAwt = swt.getFrame();
									if (frameSwtAwt != null) {

										if (frameSwtAwt != null)
											frameSwtAwt.dispatchEvent(
													new WindowEvent(frameSwtAwt, WindowEvent.WINDOW_ACTIVATED));
										// frameSwtAwt.dispatchEvent(new WindowEvent(frameSwtAwt,
										// WindowEvent.WINDOW_GAINED_FOCUS));

									}
								}

							}
						}
					});

					CanvasView.setCurrent(viewPanel);
					//ImageJ.setCustomView(customView);
					/*To show the status message!*/
					if (win != null) {
						win.repaint();
					}
				}
			}
			/*
			 * Workaround a bug on Mac which causes empty views when a perspective has been
			 * changed! Deactivated since it steals key focus of the detached view!
			 */
			if (Util.getOS().equals("Mac")) {
				if (swt != null) {
					/*Composite top = swt.getTop();
					if (top != null && top.isDisposed() == false) {
						top.setVisible(false);
						top.setVisible(true);
					}*/
				}

			}

		}

		public void partBroughtToTop(IWorkbenchPartReference ref) {

		}

		public void partClosed(IWorkbenchPartReference ref) {
			/* Important to control the references! */
			IWorkbenchPage page = ref.getPage();
			if (page != null) {
				//page.getActivePart();

				if (ref.equals(ref2)) {
					/*Job job = new Job("Close Detached Images") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							monitor.beginTask("Close Detached Images ...", IProgressMonitor.UNKNOWN);
							if (WindowManager.getImageCount() > 0) {
								if (win != null) {
									if (win.getImagePlus() != null) {
										win.bio7TabClose();
									}
								}
							}
							monitor.done();
							return Status.OK_STATUS;
						}
					
					};
					
					// job.setSystem(true);
					job.schedule();*/
					if (Util.getOS().equals("Mac")) {
						SwingUtilities.invokeLater(new Runnable() {
							// !!
							public void run() {
								if (WindowManager.getImageCount() > 0) {
									if (win != null) {
										if (win.getImagePlus() != null) {
											win.bio7TabClose(true);
										}
									}
								}
							}
						});
					} 
					else {
						if (WindowManager.getImageCount() > 0) {
							if (win != null) {
								if (win.getImagePlus() != null) {
									win.bio7TabClose(false);
								}
							}
						}
					}
					
					/*Remove the part listener?*/
					page.removePartListener(palist);

					/*
					 * ArrayList arrL = CanvasView.getCanvas_view().getDetachedSecViewIDs();
					 * System.out.println("prim: " + secId); for (int i = 0; i < arrL.size(); i++) {
					 * if (arrL.get(i).equals(secId)) { System.out.println("removed: " +
					 * arrL.get(i)); arrL.remove(i); } }
					 */

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

	/**
	 * Creates a given JPanel tab inside a custom view.
	 * 
	 * @param jpanel a JPanel
	 * @param title  the title of the tab.
	 */
	public void setPanel(final JPanel jpanel, final String id, final String name) {
		secId = id;

		viewPanel = jpanel;

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				IViewPart activated = null;
				try {

					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					activated = page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_CREATE);
					//IWorkbenchPartSite site = activated.getSite();

					palist = new ImageJPartListener2();
					page.addPartListener(palist);

				} catch (PartInitException e) {

					e.printStackTrace();
				}
				if (activated instanceof CustomDetachedImageJView) {
					customView = (CustomDetachedImageJView) activated;
					customView.setPartName(name);
					display.update();
					swt = new FXSwtAwtCustom(viewPanel, customView);
					swt.addTab(id);
					//ImageJ.setCustomView(customView);
					Composite top = swt.getTop();
					top.setParent(customView.getCustomViewParent());

				}

			}
		});

	}

	/**
	 * Creates a given JPanel tab inside a custom view.
	 * 
	 * @param jpanel a JPanel
	 * @param rec
	 * @param title  the title of the tab.
	 * @param rec    a Rectangle for the size and coordinates.
	 */
	public void setPanelFloatingDetached(final JPanel jpanel, final String id, final String name, Rectangle rec) {
		secId = id;
		viewPanel = jpanel;
		try {

			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			activated = page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_CREATE);
			IWorkbenchPartSite site = activated.getSite();
			//activated = page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_ACTIVATE);
			EModelService s = (EModelService) site.getService(EModelService.class);
			MPartSashContainerElement p = (MPart) site.getService(MPart.class);

			if (p.getCurSharedRef() != null)
				p = p.getCurSharedRef();
			s.detach(p, rec.x, rec.y, rec.width, rec.height);
			palist = new ImageJPartListener2();
			page.addPartListener(palist);

		} catch (PartInitException e) {

			e.printStackTrace();
		}
		if (activated instanceof CustomDetachedImageJView) {
			customView = (CustomDetachedImageJView) activated;
			customView.setPartName(name);
			//display.update();
			swt = new FXSwtAwtCustom(viewPanel, customView);
			swt.addTab(id);
			//ImageJ.setCustomView(customView);
			Composite top = swt.getTop();
			top.setParent(customView.getCustomViewParent());

		}
	}

	public void dispose() {
		customViewParent.dispose();
		super.dispose();
	}

	/**
	 * Creates a given Panel tab inside a custom view.
	 * 
	 * @param panel a Panel
	 * @param title the title of the tab.
	 */

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

	/*public void doSave(IProgressMonitor monitor) {
	
		Object data = customViewParent.getData();
		if (data instanceof Vector) {
			Vector<?> ve = (Vector<?>) data;
			closeTabPanels(ve);
		}
	
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
	}*/

	/*
	 * Close tab items and dispose different GUI references. Called if the view or a
	 * tab item is closed!
	 */
	private void closeTabPanels(Vector ve) {
		if (ve != null) {

			if ((ve.get(0)) instanceof javax.swing.JPanel) {
				JPanel panel = (JPanel) ve.get(0);
				panel = null;

			}

		}
	}

	public void setstatusline(String message) {
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage(message);

	}

}