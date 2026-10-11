/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.image.ui;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;

/**
 * A view which displays a single image detached from the ImageJ-Canvas view.
 * The secondary id of the view is the ImageJ id of the image. Closing the view
 * closes the image.
 *
 * @author Bio7
 *
 */
public class CustomDetachedImageJView extends ViewPart {

	public static final String ID = "com.eco.bio7.image.detachedImage";

	public Composite customViewParent;

	public ImagePlus plus;

	public ImageWindow win;

	private boolean isDetached;

	public boolean isDetached() {
		return isDetached;
	}

	public CustomDetachedImageJView() {
	}

	/* Implement to overwrite function to set name! */
	public void setPartName(String partName) {
		super.setPartName(partName);
	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagej");
		this.customViewParent = parent;
		parent.setLayout(new FillLayout());
		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				updateDetached();
				CanvasView.updatePlotDeviceSize(parent.getClientArea(), 0);
			}
		});
		updateDetached();
	}

	private void updateDetached() {
		isDetached = customViewParent.getShell().getText().length() == 0;
	}

	public void setFocus() {
		if (plus != null) {
			win = plus.getWindow();
			if (win != null && !win.isClosed()) {
				WindowManager.setTempCurrentImage(plus);
				WindowManager.setCurrentWindow(win);
				setstatusline(win.createSubtitle());
			}
		}
		customViewParent.setFocus();
	}

	/**
	 * Lays out the view and the image.
	 */
	public void layout() {
		if (customViewParent != null && !customViewParent.isDisposed()) {
			customViewParent.layout(true, true);
		}
	}

	/**
	 * Returns the detached view which displays the image or null. Must be called
	 * in the SWT UI thread.
	 */
	public static CustomDetachedImageJView find(ImagePlus imp) {
		if (imp == null) {
			return null;
		}
		IWorkbenchPage page = Bio7ImageJ.getPage();
		if (page == null) {
			return null;
		}
		IViewPart view = findBySecondaryId(page, Integer.toString(imp.getID()));
		if (view instanceof CustomDetachedImageJView) {
			return (CustomDetachedImageJView) view;
		}
		return null;
	}

	private static IViewPart findBySecondaryId(IWorkbenchPage page, String secId) {
		org.eclipse.ui.IViewReference ref = page.findViewReference(ID, secId);
		return ref != null ? ref.getView(false) : null;
	}

	/**
	 * Moves the image from its tab into a new view.
	 *
	 * @param imp the image displayed in a tab of the ImageJ-Canvas view.
	 * @param rec location and size of a floating (detached) view or null.
	 * @return the view or null.
	 */
	public static CustomDetachedImageJView detach(ImagePlus imp, Rectangle rec) {
		CustomDetachedImageJView[] result = new CustomDetachedImageJView[1];
		Util.getDisplay().syncExec(() -> {
			CTabItem item = Bio7ImageJ.getTab(imp);
			Composite holder = Bio7ImageJ.getHolder(imp);
			IWorkbenchPage page = Bio7ImageJ.getPage();
			if (item == null || holder == null || page == null) {
				return;
			}
			try {
				IViewPart activated = page.showView(ID, Integer.toString(imp.getID()), IWorkbenchPage.VIEW_ACTIVATE);
				if (!(activated instanceof CustomDetachedImageJView)) {
					return;
				}
				CustomDetachedImageJView view = (CustomDetachedImageJView) activated;
				if (rec != null) {
					IWorkbenchPartSite site = activated.getSite();
					EModelService s = site.getService(EModelService.class);
					MPartSashContainerElement p = site.getService(MPart.class);
					if (p.getCurSharedRef() != null)
						p = p.getCurSharedRef();
					s.detach(p, rec.x, rec.y, rec.width, rec.height);
				}
				view.setImage(imp, item, holder);
				result[0] = view;
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
		return result[0];
	}

	/* Moves the holder (with the ImageJ window) from the tab into this view! */
	private void setImage(ImagePlus imp, CTabItem item, Composite holder) {
		this.plus = imp;
		this.win = imp.getWindow();
		setPartName(imp.getTitle());
		item.setControl(null);
		holder.setParent(customViewParent);
		item.dispose();
		holder.setVisible(true);
		customViewParent.layout(true, true);
		setFocus();
	}

	public void dispose() {
		/* Disposing the holder closes the image (see Bio7ImageJ)! */
		if (customViewParent != null && !customViewParent.isDisposed()) {
			for (Control child : customViewParent.getChildren()) {
				child.dispose();
			}
		}
		super.dispose();
	}

	public void setstatusline(String message) {
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage(message);
	}
}
