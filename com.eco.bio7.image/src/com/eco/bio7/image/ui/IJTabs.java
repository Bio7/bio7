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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;

/**
 * A class for the control of the ImageJ tabs.
 *
 * @author Bio7
 *
 */
public class IJTabs {

	private static Display getDisplay() {
		return Util.getDisplay();
	}

	private static CTabItem[] getItems() {
		if (CanvasView.tabFolder == null || CanvasView.tabFolder.isDisposed()) {
			return new CTabItem[0];
		}
		return CanvasView.tabFolder.getItems();
	}

	/**
	 * Returns the image displayed in the given tab.
	 *
	 * @param item a tab of the ImageJ view.
	 * @return the image or null.
	 */
	public static ImagePlus getImagePlus(CTabItem item) {
		if (item != null && !item.isDisposed() && item.getData() instanceof ImagePlus) {
			return (ImagePlus) item.getData();
		}
		return null;
	}

	/**
	 * Returns the image window displayed in the given tab.
	 *
	 * @param item a tab of the ImageJ view.
	 * @return the image window or null.
	 */
	public static ImageWindow getImageWindow(CTabItem item) {
		ImagePlus imp = getImagePlus(item);
		return imp != null ? imp.getWindow() : null;
	}

	/**
	 * Returns the image of the selected tab.
	 *
	 * @return the image or null.
	 */
	public static ImagePlus getSelectedImagePlus() {
		AtomicReference<ImagePlus> imp = new AtomicReference<ImagePlus>();
		getDisplay().syncExec(() -> {
			if (CanvasView.tabFolder != null && !CanvasView.tabFolder.isDisposed()) {
				imp.set(getImagePlus(CanvasView.tabFolder.getSelection()));
			}
		});
		return imp.get();
	}

	/**
	 * Returns the images of all tabs.
	 *
	 * @return a list of images.
	 */
	public static List<ImagePlus> getImagePlusList() {
		List<ImagePlus> list = new ArrayList<ImagePlus>();
		getDisplay().syncExec(() -> {
			for (CTabItem item : getItems()) {
				ImagePlus imp = getImagePlus(item);
				if (imp != null) {
					list.add(imp);
				}
			}
		});
		return list;
	}

	/**
	 * Activates the tab with the specified number.
	 *
	 * @param number the number of the tab.
	 */
	public static void setActive(final int number) {
		getDisplay().syncExec(() -> {
			CTabItem[] items = getItems();
			if (number >= 0 && number < items.length) {
				selectTab(items[number]);
			}
		});
	}

	private static void selectTab(CTabItem item) {
		CanvasView.tabFolder.showItem(item);
		CanvasView.tabFolder.setSelection(item);
		CanvasView view = CanvasView.getCanvas_view();
		if (view != null) {
			view.tabSelected(item);
		}
	}

	/**
	 * Returns the amount of opened tabs in the ImageJ view.
	 *
	 * @return the amount of tabs as an integer value.
	 */
	public static int getAmountTabs() {
		AtomicReference<Integer> count = new AtomicReference<Integer>(0);
		getDisplay().syncExec(() -> count.set(getItems().length));
		return count.get();
	}

	/**
	 * Detaches the view with the given secondary id (the image id) at the given
	 * location and size.
	 */
	public static void setSecondaryViewShellLocAndSize(String sec, Rectangle rec) {
		getDisplay().syncExec(() -> {
			IWorkbenchPage page = Bio7ImageJ.getPage();
			if (page == null) {
				return;
			}
			IViewReference ref = page.findViewReference(CustomDetachedImageJView.ID, sec);
			if (ref == null) {
				return;
			}
			try {
				IViewPart activated = page.showView(CustomDetachedImageJView.ID, sec, IWorkbenchPage.VIEW_ACTIVATE);
				if (rec != null && activated != null) {
					IWorkbenchPartSite site = activated.getSite();
					EModelService s = site.getService(EModelService.class);
					MPartSashContainerElement p = site.getService(MPart.class);
					if (p.getCurSharedRef() != null)
						p = p.getCurSharedRef();
					s.detach(p, rec.x, rec.y, rec.width, rec.height);
				}
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Closes all tabs in the ImageJ view (and all detached image views).
	 */
	public static void deleteAllTabs() {
		getDisplay().syncExec(() -> {
			IWorkbenchPage page = Bio7ImageJ.getPage();
			if (page != null) {
				for (IViewReference ref : page.getViewReferences()) {
					if (ref.getId().equals(CustomDetachedImageJView.ID)) {
						page.hideView(ref);
					}
				}
			}
			for (CTabItem item : getItems()) {
				closeTab(item);
			}
		});
	}

	/* Disposing the holder closes the image (see Bio7ImageJ)! */
	private static void closeTab(CTabItem item) {
		Control holder = item.getControl();
		if (holder != null && !holder.isDisposed()) {
			holder.dispose();
		}
		if (!item.isDisposed()) {
			item.dispose();
		}
	}

	/**
	 * Closes the tab (and the image) with the specified number in the ImageJ view.
	 *
	 * @param number the number of the tab as an integer value.
	 */
	public static void deleteTab(int number) {
		getDisplay().syncExec(() -> {
			CTabItem[] items = getItems();
			if (number >= 0 && number < items.length) {
				closeTab(items[number]);
			}
		});
	}

	/**
	 * Closes the active tab in the ImageJ view.
	 *
	 * @deprecated Since the SWT port tabs can't be hidden without closing the
	 *             image. Same as {@link #deleteActiveTab()}.
	 */
	@Deprecated
	public static void hideTab() {
		deleteActiveTab();
	}

	/**
	 * Closes the tab with the specified number in the ImageJ view.
	 *
	 * @param number the tab number.
	 * @deprecated Since the SWT port tabs can't be hidden without closing the
	 *             image. Same as {@link #deleteTab(int)}.
	 */
	@Deprecated
	public static void hideTabNumber(int number) {
		deleteTab(number);
	}

	/**
	 * Closes the tab which displays the given image.
	 *
	 * @param imagePlus the image.
	 * @deprecated Since the SWT port tabs can't be hidden without closing the
	 *             image. Use {@link ImagePlus#close()}.
	 */
	@Deprecated
	public static void hideTab(ImagePlus imagePlus) {
		getDisplay().syncExec(() -> {
			CTabItem item = Bio7ImageJ.getTab(imagePlus);
			if (item != null) {
				closeTab(item);
			}
		});
	}

	/**
	 * Deletes the active tab in the ImageJ view.
	 */
	public static void deleteActiveTab() {
		getDisplay().syncExec(() -> {
			if (CanvasView.tabFolder != null && !CanvasView.tabFolder.isDisposed()) {
				CTabItem item = CanvasView.tabFolder.getSelection();
				if (item != null) {
					closeTab(item);
				}
			}
		});
	}

	/**
	 * Selects the tab which displays the image with the given title.
	 */
	public static void setActiveTab(String title) {
		getDisplay().syncExec(() -> {
			for (CTabItem item : getItems()) {
				ImagePlus imp = getImagePlus(item);
				if (imp != null && imp.getTitle().equals(title)) {
					selectTab(item);
					return;
				}
			}
		});
	}

	/**
	 * Selects the tab which displays the image with the given ImageJ id (or index
	 * if the value is positive).
	 */
	public static void setActiveTabID(int theId) {
		int id = theId > 0 ? WindowManager.getNthImageID(theId) : theId;
		ImagePlus plus = WindowManager.getImage(id);
		if (plus != null) {
			getDisplay().syncExec(() -> {
				CTabItem item = Bio7ImageJ.getTab(plus);
				if (item != null) {
					selectTab(item);
				}
			});
		}
	}

	/**
	 * Selects the tab which displays the given image window.
	 */
	public static void setActiveTabWindow(ImageWindow win) {
		if (win == null || win.getImagePlus() == null) {
			return;
		}
		getDisplay().syncExec(() -> {
			CTabItem item = Bio7ImageJ.getTab(win.getImagePlus());
			if (item != null) {
				selectTab(item);
			}
		});
	}

	/**
	 * Lays out the detached image views (e.g. for orthogonal stack views)!
	 */
	public static void doSecondaryViewLayout() {
		getDisplay().syncExec(() -> {
			IWorkbenchPage page = Bio7ImageJ.getPage();
			if (page == null) {
				return;
			}
			for (IViewReference ref : page.getViewReferences()) {
				if (ref.getId().equals(CustomDetachedImageJView.ID)) {
					IViewPart view = ref.getView(false);
					if (view instanceof CustomDetachedImageJView) {
						((CustomDetachedImageJView) view).layout();
					}
				}
			}
		});
	}

	/**
	 * Closes the detached view which displays the given window.
	 *
	 * @return true if a detached view has been closed.
	 */
	public static boolean closeDetachedWindowView(ImageWindow refWin) {
		AtomicBoolean result = new AtomicBoolean(false);
		if (refWin == null) {
			return false;
		}
		getDisplay().syncExec(() -> {
			CustomDetachedImageJView view = CustomDetachedImageJView.find(refWin.getImagePlus());
			if (view != null) {
				view.getSite().getPage().hideView(view);
				result.set(true);
			}
		});
		return result.get();
	}
}
