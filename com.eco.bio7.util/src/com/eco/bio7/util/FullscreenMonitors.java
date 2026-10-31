/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

/**
 * The monitors and the keys of the fullscreen mode of all Bio7 views (ImageJ,
 * the custom views, the 3D view and the globe), so that all views behave the
 * same:
 * <ul>
 * <li>F2: fullscreen on the primary monitor (exits if in fullscreen mode)</li>
 * <li>F3: fullscreen on the secondary monitor</li>
 * <li>F4: fullscreen on the tertiary monitor</li>
 * <li>F5: select the monitor in a dialog</li>
 * <li>ESC: exit</li>
 * </ul>
 * F3, F4 and F5 move the fullscreen mode to another monitor and exit if the
 * view is already displayed on the monitor. The monitors are numbered with the
 * primary monitor first. All methods must be called in the SWT UI thread!
 */
public final class FullscreenMonitors {

	/** The key for the primary monitor (exits the fullscreen mode, too). */
	public static final int KEY_PRIMARY = 2;
	/** The key for the dialog to select the monitor. */
	public static final int KEY_SELECT = 5;

	private FullscreenMonitors() {
	}

	/**
	 * Returns 2 to 5 for the SWT keys F2 to F5, else 0.
	 */
	public static int functionKey(int swtKeyCode) {
		switch (swtKeyCode) {
		case SWT.F2:
			return 2;
		case SWT.F3:
			return 3;
		case SWT.F4:
			return 4;
		case SWT.F5:
			return 5;
		default:
			return 0;
		}
	}

	/**
	 * Returns the monitors with the primary monitor first and the other monitors
	 * in the order of the system.
	 */
	public static Monitor[] getMonitors(Display display) {
		List<Monitor> list = new ArrayList<Monitor>();
		Monitor primary = display.getPrimaryMonitor();
		if (primary != null) {
			list.add(primary);
		}
		for (Monitor m : display.getMonitors()) {
			if (!same(m, primary)) {
				list.add(m);
			}
		}
		return list.toArray(new Monitor[0]);
	}

	/**
	 * True if the monitors are the same. The handles of the monitors can change
	 * (e.g., on macOS), so the bounds are compared.
	 */
	public static boolean same(Monitor a, Monitor b) {
		return a != null && b != null && a.getBounds().equals(b.getBounds());
	}

	/** The index of the monitor (primary monitor first) or -1. */
	public static int indexOf(Monitor[] monitors, Monitor monitor) {
		for (int i = 0; i < monitors.length; i++) {
			if (same(monitors[i], monitor)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * The monitor of a fullscreen key (2 to 4: the primary, secondary or tertiary
	 * monitor, 5: selected in a dialog). Returns null if the monitor is not
	 * available (with a beep) or the dialog was canceled.
	 *
	 * @param key     the key (see {@link #functionKey(int)})
	 * @param parent  the parent of the dialog (e.g., the fullscreen shell) or null
	 * @param current the monitor of the fullscreen mode (marked in the dialog) or
	 *                null
	 */
	public static Monitor monitorForKey(int key, Display display, Shell parent, Monitor current) {
		if (key == KEY_SELECT) {
			return selectMonitor(display, parent, current);
		}
		Monitor[] monitors = getMonitors(display);
		int index = key - KEY_PRIMARY;
		if (index >= 0 && index < monitors.length) {
			return monitors[index];
		}
		/* No secondary or tertiary monitor! */
		display.beep();
		return null;
	}

	/**
	 * Lets the user select a monitor (null if canceled).
	 *
	 * @param parent  the parent of the dialog or null (the active shell)
	 * @param current the monitor of the fullscreen mode or null
	 */
	public static Monitor selectMonitor(Display display, Shell parent, Monitor current) {
		Monitor[] monitors = getMonitors(display);
		ListDialog dialog = new ListDialog(parent != null ? parent : display.getActiveShell());
		dialog.setTitle("Fullscreen");
		dialog.setMessage("Select the monitor for the fullscreen mode:");
		dialog.setContentProvider(ArrayContentProvider.getInstance());
		dialog.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				Monitor m = (Monitor) element;
				int index = Arrays.asList(monitors).indexOf(m);
				Rectangle b = m.getBounds();
				String name = "Monitor " + (index + 1) + (index == 0 ? " (primary)" : "");
				return name + ": " + b.width + " x " + b.height + " at " + b.x + ", " + b.y + (same(m, current) ? " - current" : "");
			}
		});
		dialog.setInput(monitors);
		Monitor selected = monitors[0];
		for (Monitor m : monitors) {
			if (same(m, current)) {
				selected = m;
			}
		}
		dialog.setInitialSelections(new Object[] { selected });
		dialog.setHeightInChars(Math.max(3, monitors.length));
		if (dialog.open() != Window.OK || dialog.getResult() == null || dialog.getResult().length == 0) {
			return null;
		}
		return (Monitor) dialog.getResult()[0];
	}
}
