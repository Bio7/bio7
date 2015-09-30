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

import java.awt.Panel;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;

/**
 * This class provides some static methods for the creation of custom views
 * inside the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class CustomDetachedImageJView extends ViewPart implements ISaveablePart2 {

	protected int insertMark = -1;

	private static final String ID = "com.eco.bio7.custom_controls";

	private Composite customViewParent;

	private Panel awtCurrent;

	private String secId;

	protected IViewPart activated;

	private ImagePlus plus;

	private ImageWindow win;

	private JPanel viewPanel;

	public IViewReference ref2;

	private CustomDetachedImageJView customView;

	protected ImageJPartListener2 palist;

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

	public void createPartControl(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagej");

		this.customViewParent = parent;

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
					ImageJ.setCustomView(customView);
				}
			}
		}

		public void partBroughtToTop(IWorkbenchPartReference ref) {

		}

		public void partClosed(IWorkbenchPartReference ref) {
			/* Important to control the references! */
			IWorkbenchPage page = ref.getPage();
			if (page != null) {
				page.getActivePart();

				if (ref.equals(ref2)) {
					SwingUtilities.invokeLater(new Runnable() {
						// !!
						public void run() {
							if (WindowManager.getImageCount() > 0) {
								win.bio7TabClose();
							}
						}
					});

					/*
					 * ArrayList arrL =
					 * CanvasView.getCanvas_view().getDetachedSecViewIDs();
					 * System.out.println("prim: " + secId); for (int i = 0; i <
					 * arrL.size(); i++) { if (arrL.get(i).equals(secId)) {
					 * System.out.println("removed: " + arrL.get(i));
					 * arrL.remove(i); } }
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

					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_CREATE);
					activated = page.showView("com.eco.bio7.image.detachedImage", secId, IWorkbenchPage.VIEW_ACTIVATE);

					palist = new ImageJPartListener2();
					page.addPartListener(palist);

				} catch (PartInitException e) {

					e.printStackTrace();
				}
				if (activated instanceof CustomDetachedImageJView) {
					customView = (CustomDetachedImageJView) activated;

					SwtAwtCustom swt = new SwtAwtCustom(viewPanel, customView);
					swt.addTab(id);
					ImageJ.setCustomView(customView);

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

	public void doSave(IProgressMonitor monitor) {

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
	}

	/*
	 * Close tab items and dispose different GUI references. Called if the view
	 * or a tab item is closed!
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