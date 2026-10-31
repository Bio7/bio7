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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;

import com.eco.bio7.util.FullscreenMonitors;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;

/**
 * Displays the current image in a fullscreen shell (the same keys as all
 * fullscreen modes of Bio7, see {@link FullscreenMonitors}: F2 the primary
 * monitor (exits in fullscreen mode), F3 the secondary, F4 the tertiary monitor,
 * F5 a selected monitor, closed with ESC). The holder composite of the image is
 * moved into the shell and back to the tab or view when the fullscreen mode is
 * closed.
 */
public class Fullscreen {

	private static Shell shell;
	private static Composite holder;
	private static Composite oldParent;
	private static CTabItem oldItem;
	/* The image in fullscreen mode! */
	private static ImagePlus image;
	/* True if the fullscreen image was activated last (by the user)! */
	private static boolean activatedLast;

	/*
	 * If another shell (e.g., the Bio7 window to open a menu) is activated, the
	 * focus is restored to the canvas of the selected tab, which makes this image
	 * the current image. So the fullscreen image is made the current image again,
	 * unless another image or menu item was clicked!
	 */
	private static final Listener ACTIVATION_FILTER = new Listener() {
		public void handleEvent(Event e) {
			if (!isActive()) {
				return;
			}
			if (e.type == SWT.MouseDown) {
				if (e.widget instanceof Control) {
					Control control = (Control) e.widget;
					if (control.getShell() == shell) {
						/* A click in the fullscreen shell activates its image (even without an activation event)! */
						activatedLast = true;
						activateImage();
					} else if (!(control instanceof ToolBar) && imageFor(control) != null) {
						/*
						 * Only a click on another image (canvas, tab or detached view) selects another
						 * image. A click on a toolbar (e.g., the Window menu in the top right of the
						 * tab folder) keeps the fullscreen image!
						 */
						activatedLast = false;
					}
				}
			} else if (e.type == SWT.Show) {
				/*
				 * A menu is opened (e.g., the Window menu) before an asynchronous runnable is
				 * executed, so the image is activated before the menu items are created!
				 */
				if (e.widget instanceof Menu && activatedLast) {
					activateImage();
				}
			} else if (e.type == SWT.Selection) {
				if (e.widget instanceof MenuItem) {
					activatedLast = false;
				}
			} else if (e.type == SWT.Activate) {
				if (e.widget instanceof Shell && e.widget != shell && activatedLast) {
					/* After the focus has been restored (a click is processed before)! */
					e.display.asyncExec(() -> {
						if (activatedLast && isActive()) {
							activateImage();
						}
					});
				}
			}
		}
	};

	/* Installed once by Bio7ImageJ! */
	static final Listener KEY_FILTER = new Listener() {
		public void handleEvent(Event e) {
			int key = FullscreenMonitors.functionKey(e.keyCode);
			if (key == FullscreenMonitors.KEY_PRIMARY && isActive()) {
				/* F2 exits the fullscreen mode (like the other fullscreen modes)! */
				exit();
				e.doit = false;
			} else if (key > 0) {
				/* F2 to F4: the primary, secondary and tertiary monitor, F5: a selected monitor! */
				ImagePlus imp = isActive() ? image : e.widget instanceof Control ? imageFor((Control) e.widget) : null;
				if (imp != null) {
					e.doit = false;
					/* Not in the filter (F5 opens a dialog)! */
					Display.getCurrent().asyncExec(() -> toMonitor(imp, key));
				}
			} else if (e.keyCode == SWT.ESC && isActive()) {
				/* The ESC key is also forwarded to ImageJ (abort macro or plugin)! */
				Display.getCurrent().asyncExec(Fullscreen::exit);
			}
		}
	};

	/*
	 * Displays the image in fullscreen mode on the monitor of the key (see
	 * FullscreenMonitors). If the image is already displayed on this monitor, the
	 * fullscreen mode is closed.
	 */
	private static void toMonitor(ImagePlus imp, int key) {
		Display display = Display.getDefault();
		Monitor current = isActive() ? shell.getMonitor() : null;
		Monitor monitor = FullscreenMonitors.monitorForKey(key, display, isActive() ? shell : null, current);
		if (monitor == null) {
			return;
		}
		if (isActive()) {
			boolean same = FullscreenMonitors.same(monitor, current);
			exit();
			if (same) {
				return;
			}
		}
		enter(imp, monitor);
	}

	/** Returns the monitors with the primary monitor first. */
	public static Monitor[] getMonitors(Display display) {
		return FullscreenMonitors.getMonitors(display);
	}

	/*
	 * The image of the control with the focus or null: the control is part of an
	 * embedded image window, of the ImageJ-Canvas view (e.g., the tab folder) or of
	 * a detached image view. On Windows the focus is not always on the canvas!
	 */
	private static ImagePlus imageFor(Control control) {
		for (Control c = control; c != null; c = c.getParent()) {
			if (c instanceof ij.gui.ImageCanvas) {
				ImagePlus imp = ((ij.gui.ImageCanvas) c).getImage();
				return imp != null ? imp : WindowManager.getCurrentImage();
			}
			if (c == CanvasView.tabFolder) {
				return IJTabs.getSelectedImagePlus();
			}
			ImagePlus detached = detachedImage(c);
			if (detached != null) {
				return detached;
			}
		}
		return null;
	}

	/* The image of the detached view whose content is the composite or null! */
	private static ImagePlus detachedImage(Control c) {
		IWorkbenchPage page = Bio7ImageJ.getPage();
		if (page == null || !(c instanceof Composite)) {
			return null;
		}
		for (IViewReference ref : page.getViewReferences()) {
			if (CustomDetachedImageJView.ID.equals(ref.getId()) && ref.getView(false) instanceof CustomDetachedImageJView) {
				CustomDetachedImageJView view = (CustomDetachedImageJView) ref.getView(false);
				if (view.customViewParent == c) {
					return view.plus;
				}
			}
		}
		return null;
	}

	public static boolean isActive() {
		return shell != null && !shell.isDisposed();
	}

	/** Returns true if the given holder is displayed in fullscreen mode. */
	static boolean isFullscreen(Composite imageHolder) {
		return isActive() && holder == imageHolder;
	}

	/**
	 * Displays the image in fullscreen mode. Must be called in the SWT UI thread.
	 */
	public static void enter(ImagePlus imp) {
		enter(imp, null);
	}

	/**
	 * Displays the image in fullscreen mode on the given monitor (the monitor of
	 * the image if null). Must be called in the SWT UI thread.
	 */
	public static void enter(ImagePlus imp, Monitor monitor) {
		if (isActive()) {
			return;
		}
		holder = Bio7ImageJ.getHolder(imp);
		if (holder == null) {
			return;
		}
		image = imp;
		oldParent = holder.getParent();
		oldItem = Bio7ImageJ.getTab(imp);
		shell = new Shell(holder.getDisplay(), SWT.NO_TRIM | SWT.ON_TOP);
		shell.setLayout(new FillLayout());
		shell.setBackground(holder.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		shell.addListener(SWT.Close, _ -> restore());
		/* Clicking the fullscreen shell makes the image the current image (like a tab)! */
		shell.addListener(SWT.Activate, _ -> {
			activatedLast = true;
			activateImage();
		});
		Display display = holder.getDisplay();
		display.addFilter(SWT.MouseDown, ACTIVATION_FILTER);
		display.addFilter(SWT.Selection, ACTIVATION_FILTER);
		display.addFilter(SWT.Activate, ACTIVATION_FILTER);
		display.addFilter(SWT.Show, ACTIVATION_FILTER);
		if (oldItem != null) {
			oldItem.setControl(null);
		}
		holder.setParent(shell);
		holder.setVisible(true);
		/* On the target monitor before the fullscreen mode is set! */
		shell.setBounds((monitor != null ? monitor : holder.getMonitor()).getBounds());
		shell.setFullScreen(true);
		shell.open();
		/*
		 * macOS (e.g., with a notch): the shell is moved below the menu bar area with
		 * the height of the monitor, so the bottom (e.g., the time slider of a
		 * hyperstack) is not visible!
		 */
		shell.addListener(SWT.Move, _ -> fitToMonitor());
		shell.addListener(SWT.Resize, _ -> fitToMonitor());
		fitToMonitor();
		shell.getDisplay().timerExec(300, Fullscreen::fitToMonitor);
		shell.layout(true, true);
		if (imp.getCanvas() != null) {
			imp.getCanvas().setFocus();
		}
		activateImage();
	}

	/* Makes the fullscreen image the current ImageJ image (selected in the window menu)! */
	private static void activateImage() {
		if (image == null) {
			return;
		}
		ImageWindow win = image.getWindow();
		if (win != null && !win.isClosed()) {
			WindowManager.setTempCurrentImage(image);
			WindowManager.setCurrentWindow(win);
		}
	}

	/* The shell ends at the bottom of the monitor! */
	private static void fitToMonitor() {
		if (!isActive()) {
			return;
		}
		Rectangle monitor = shell.getMonitor().getBounds();
		Rectangle bounds = shell.getBounds();
		int bottom = monitor.y + monitor.height;
		if (bounds.y + bounds.height > bottom && bottom - bounds.y > 0) {
			shell.setSize(bounds.width, bottom - bounds.y);
			shell.layout(true, true);
		}
	}

	/**
	 * Closes the fullscreen mode.
	 */
	public static void exit() {
		if (isActive()) {
			shell.close();
		}
	}

	/* Moves the holder back to the tab or view! */
	private static void restore() {
		Display display = Display.getCurrent();
		if (display != null) {
			display.removeFilter(SWT.MouseDown, ACTIVATION_FILTER);
			display.removeFilter(SWT.Selection, ACTIVATION_FILTER);
			display.removeFilter(SWT.Activate, ACTIVATION_FILTER);
			display.removeFilter(SWT.Show, ACTIVATION_FILTER);
		}
		activatedLast = false;
		if (holder != null && !holder.isDisposed()) {
			if (oldParent != null && !oldParent.isDisposed()) {
				holder.setParent(oldParent);
				if (oldItem != null && !oldItem.isDisposed()) {
					oldItem.setControl(holder);
					oldItem.getParent().setSelection(oldItem);
				}
				oldParent.layout(true, true);
			} else {
				/* The tab or view has been closed. Disposing closes the image! */
				holder.dispose();
			}
		}
		holder = null;
		image = null;
		oldParent = null;
		oldItem = null;
		shell = null;
	}
}
