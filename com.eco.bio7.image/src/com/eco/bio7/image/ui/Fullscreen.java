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
import org.eclipse.swt.widgets.Shell;

import ij.ImagePlus;
import ij.WindowManager;

/**
 * Displays the current image in a fullscreen shell (toggled with F2, closed
 * with ESC). The holder composite of the image is moved into the shell and back
 * to the tab or view when the fullscreen mode is closed.
 */
public class Fullscreen {

	private static Shell shell;
	private static Composite holder;
	private static Composite oldParent;
	private static CTabItem oldItem;

	/* Installed once by Bio7ImageJ! */
	static final Listener KEY_FILTER = new Listener() {
		public void handleEvent(Event e) {
			if (e.keyCode == SWT.F2) {
				if (isActive()) {
					exit();
					e.doit = false;
				} else if (e.widget instanceof Control && isImageControl((Control) e.widget)) {
					ImagePlus imp = WindowManager.getCurrentImage();
					if (imp != null) {
						enter(imp);
						e.doit = false;
					}
				}
			} else if (e.keyCode == SWT.ESC && isActive()) {
				/* The ESC key is also forwarded to ImageJ (abort macro or plugin)! */
				Display.getCurrent().asyncExec(Fullscreen::exit);
			}
		}
	};

	/* Returns true if the control is part of an embedded image window! */
	private static boolean isImageControl(Control control) {
		for (Control c = control; c != null; c = c.getParent()) {
			if (c instanceof ij.gui.ImageCanvas) {
				return true;
			}
		}
		return false;
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
		if (isActive()) {
			return;
		}
		holder = Bio7ImageJ.getHolder(imp);
		if (holder == null) {
			return;
		}
		oldParent = holder.getParent();
		oldItem = Bio7ImageJ.getTab(imp);
		shell = new Shell(holder.getDisplay(), SWT.NO_TRIM | SWT.ON_TOP);
		shell.setLayout(new FillLayout());
		shell.setBackground(holder.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		shell.addListener(SWT.Close, _ -> restore());
		if (oldItem != null) {
			oldItem.setControl(null);
		}
		holder.setParent(shell);
		holder.setVisible(true);
		shell.setBounds(holder.getMonitor().getBounds());
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
		oldParent = null;
		oldItem = null;
		shell = null;
	}
}
