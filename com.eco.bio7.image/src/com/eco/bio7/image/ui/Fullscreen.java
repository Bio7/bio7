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
 * Displays images in fullscreen shells (the same keys as all fullscreen modes
 * of Bio7, see {@link FullscreenMonitors}: F2 the primary monitor (exits in
 * fullscreen mode), F3 the secondary, F4 the tertiary monitor, F5 a selected
 * monitor, closed with ESC). Several images can be displayed at the same time,
 * one per monitor: an image displayed on a monitor which already shows another
 * image replaces it. The holder composite of an image is moved into its shell
 * and back to the tab or view when the fullscreen mode is closed.
 */
public class Fullscreen {

	/* The fullscreen mode of one image */
	private static final class Session {
		Shell shell;
		Composite holder;
		Composite oldParent;
		CTabItem oldItem;
		ImagePlus image;
	}

	/* The images in fullscreen mode (in the order they were entered) */
	private static final List<Session> sessions = new ArrayList<>();
	/* The fullscreen image activated last (by the user) or null! */
	private static Session activatedLast;
	private static boolean filtersInstalled;

	/*
	 * If another shell (e.g., the Bio7 window to open a menu) is activated, the
	 * focus is restored to the canvas of the selected tab, which makes this image
	 * the current image. So the fullscreen image activated last is made the
	 * current image again, unless another image or menu item was clicked!
	 */
	private static final Listener ACTIVATION_FILTER = new Listener() {
		public void handleEvent(Event e) {
			if (!isActive()) {
				return;
			}
			if (e.type == SWT.MouseDown) {
				if (e.widget instanceof Control) {
					Control control = (Control) e.widget;
					Session s = sessionOf(control.getShell());
					if (s != null) {
						/* A click in a fullscreen shell activates its image (even without an activation event)! */
						activatedLast = s;
						activateImage(s);
					} else if (!(control instanceof ToolBar) && imageFor(control) != null) {
						/*
						 * Only a click on another image (canvas, tab or detached view) selects another
						 * image. A click on a toolbar (e.g., the Window menu in the top right of the
						 * tab folder) keeps the fullscreen image!
						 */
						activatedLast = null;
					}
				}
			} else if (e.type == SWT.Show) {
				/*
				 * A menu is opened (e.g., the Window menu) before an asynchronous runnable is
				 * executed, so the image is activated before the menu items are created!
				 */
				if (e.widget instanceof Menu && activatedLast != null) {
					activateImage(activatedLast);
				}
			} else if (e.type == SWT.Selection) {
				if (e.widget instanceof MenuItem) {
					activatedLast = null;
				}
			} else if (e.type == SWT.Activate) {
				if (e.widget instanceof Shell && sessionOf((Shell) e.widget) == null && activatedLast != null) {
					/* After the focus has been restored (a click is processed before)! */
					Session last = activatedLast;
					e.display.asyncExec(() -> {
						if (activatedLast == last && sessions.contains(last)) {
							activateImage(last);
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
			Control control = e.widget instanceof Control ? (Control) e.widget : null;
			/*
			 * The keys belong to a fullscreen image only if its shell has the focus: else
			 * another image or view (e.g., a custom view or plot) can be displayed in
			 * fullscreen mode on another monitor at the same time.
			 */
			Session s = control != null ? sessionOf(control.getShell()) : null;
			if (key == FullscreenMonitors.KEY_PRIMARY && s != null) {
				/* F2 exits the fullscreen mode (like the other fullscreen modes)! */
				close(s);
				e.doit = false;
			} else if (key > 0) {
				/* F2 to F4: the primary, secondary and tertiary monitor, F5: a selected monitor! */
				ImagePlus imp = s != null ? s.image : control != null ? imageFor(control) : null;
				if (imp != null) {
					e.doit = false;
					/* Not in the filter (F5 opens a dialog)! */
					Display.getCurrent().asyncExec(() -> toMonitor(imp, key));
				}
			} else if (e.keyCode == SWT.ESC && s != null) {
				/* The ESC key is also forwarded to ImageJ (abort macro or plugin)! */
				Display.getCurrent().asyncExec(() -> close(s));
			}
		}
	};

	/*
	 * Displays the image in fullscreen mode on the monitor of the key (see
	 * FullscreenMonitors). If the image is already displayed on this monitor, the
	 * fullscreen mode is closed, on another monitor it is moved.
	 */
	private static void toMonitor(ImagePlus imp, int key) {
		Display display = Display.getDefault();
		Session s = sessionOf(imp);
		Monitor current = s != null ? s.shell.getMonitor() : null;
		Monitor monitor = FullscreenMonitors.monitorForKey(key, display, s != null ? s.shell : null, current);
		if (monitor == null) {
			return;
		}
		if (s != null) {
			boolean same = FullscreenMonitors.same(monitor, current);
			close(s);
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

	private static Session sessionOf(Shell shell) {
		for (Session s : sessions) {
			if (s.shell == shell) {
				return s;
			}
		}
		return null;
	}

	private static Session sessionOf(ImagePlus imp) {
		for (Session s : sessions) {
			if (s.image == imp) {
				return s;
			}
		}
		return null;
	}

	/** Returns true if an image is displayed in fullscreen mode. */
	public static boolean isActive() {
		return !sessions.isEmpty();
	}

	/** Returns true if the given image is displayed in fullscreen mode. */
	public static boolean isFullscreen(ImagePlus imp) {
		return imp != null && sessionOf(imp) != null;
	}

	/** Returns true if the given holder is displayed in fullscreen mode. */
	static boolean isFullscreen(Composite imageHolder) {
		for (Session s : sessions) {
			if (s.holder == imageHolder) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Displays the image in fullscreen mode. Must be called in the SWT UI thread.
	 */
	public static void enter(ImagePlus imp) {
		enter(imp, null);
	}

	/**
	 * Displays the image in fullscreen mode on the given monitor (the monitor of
	 * the image if null). Another image in fullscreen mode on this monitor is
	 * closed. Must be called in the SWT UI thread.
	 */
	public static void enter(ImagePlus imp, Monitor monitor) {
		if (imp == null || sessionOf(imp) != null) {
			return;
		}
		Composite holder = Bio7ImageJ.getHolder(imp);
		if (holder == null) {
			return;
		}
		Monitor target = monitor != null ? monitor : holder.getMonitor();
		/* One image per monitor */
		for (Session other : new ArrayList<>(sessions)) {
			if (FullscreenMonitors.same(other.shell.getMonitor(), target)) {
				close(other);
			}
		}
		Session s = new Session();
		s.image = imp;
		s.holder = holder;
		s.oldParent = holder.getParent();
		s.oldItem = Bio7ImageJ.getTab(imp);
		Display display = holder.getDisplay();
		Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		s.shell = shell;
		sessions.add(s);
		shell.setLayout(new FillLayout());
		shell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		shell.addListener(SWT.Close, _ -> restore(s));
		/* Clicking the fullscreen shell makes the image the current image (like a tab)! */
		shell.addListener(SWT.Activate, _ -> {
			activatedLast = s;
			activateImage(s);
		});
		if (!filtersInstalled) {
			filtersInstalled = true;
			display.addFilter(SWT.MouseDown, ACTIVATION_FILTER);
			display.addFilter(SWT.Selection, ACTIVATION_FILTER);
			display.addFilter(SWT.Activate, ACTIVATION_FILTER);
			display.addFilter(SWT.Show, ACTIVATION_FILTER);
		}
		if (s.oldItem != null) {
			s.oldItem.setControl(null);
		}
		holder.setParent(shell);
		holder.setVisible(true);
		/* On the target monitor before the fullscreen mode is set! */
		shell.setBounds(target.getBounds());
		shell.setFullScreen(true);
		shell.open();
		/*
		 * macOS (e.g., with a notch): the shell is moved below the menu bar area with
		 * the height of the monitor, so the bottom (e.g., the time slider of a
		 * hyperstack) is not visible!
		 */
		shell.addListener(SWT.Move, _ -> fitToMonitor(s));
		shell.addListener(SWT.Resize, _ -> fitToMonitor(s));
		fitToMonitor(s);
		display.timerExec(300, () -> fitToMonitor(s));
		shell.layout(true, true);
		if (imp.getCanvas() != null) {
			imp.getCanvas().setFocus();
		}
		activatedLast = s;
		activateImage(s);
	}

	/* Makes the fullscreen image the current ImageJ image (selected in the window menu)! */
	private static void activateImage(Session s) {
		if (s == null || s.image == null) {
			return;
		}
		ImageWindow win = s.image.getWindow();
		if (win != null && !win.isClosed()) {
			WindowManager.setTempCurrentImage(s.image);
			WindowManager.setCurrentWindow(win);
		}
	}

	/* The shell ends at the bottom of the monitor! */
	private static void fitToMonitor(Session s) {
		Shell shell = s.shell;
		if (shell == null || shell.isDisposed() || !sessions.contains(s)) {
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
	 * Closes the fullscreen mode of all images.
	 */
	public static void exit() {
		for (Session s : new ArrayList<>(sessions)) {
			close(s);
		}
	}

	/** Closes the fullscreen mode of the image. */
	public static void exit(ImagePlus imp) {
		Session s = sessionOf(imp);
		if (s != null) {
			close(s);
		}
	}

	/** Closes the fullscreen mode of the holder (e.g., the image is closed). */
	static void exit(Composite imageHolder) {
		for (Session s : new ArrayList<>(sessions)) {
			if (s.holder == imageHolder) {
				close(s);
			}
		}
	}

	private static void close(Session s) {
		if (s.shell != null && !s.shell.isDisposed()) {
			s.shell.close();
		} else {
			restore(s);
		}
	}

	/* Moves the holder back to the tab or view! */
	private static void restore(Session s) {
		if (!sessions.remove(s)) {
			return;
		}
		if (activatedLast == s) {
			activatedLast = null;
		}
		Display display = Display.getCurrent();
		if (sessions.isEmpty() && filtersInstalled && display != null) {
			filtersInstalled = false;
			display.removeFilter(SWT.MouseDown, ACTIVATION_FILTER);
			display.removeFilter(SWT.Selection, ACTIVATION_FILTER);
			display.removeFilter(SWT.Activate, ACTIVATION_FILTER);
			display.removeFilter(SWT.Show, ACTIVATION_FILTER);
		}
		if (s.holder != null && !s.holder.isDisposed()) {
			if (s.oldParent != null && !s.oldParent.isDisposed()) {
				s.holder.setParent(s.oldParent);
				if (s.oldItem != null && !s.oldItem.isDisposed()) {
					s.oldItem.setControl(s.holder);
					s.oldItem.getParent().setSelection(s.oldItem);
				}
				s.oldParent.layout(true, true);
			} else {
				/* The tab or view has been closed. Disposing closes the image! */
				s.holder.dispose();
			}
		}
		s.holder = null;
		s.image = null;
		s.oldParent = null;
		s.oldItem = null;
	}
}
