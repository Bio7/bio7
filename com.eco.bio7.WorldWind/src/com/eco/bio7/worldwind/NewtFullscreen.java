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
package com.eco.bio7.worldwind;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import com.eco.bio7.util.FullscreenMonitors;
import com.eco.bio7.util.Util;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;

/**
 * The fullscreen mode of a NEWT window (the 3D view and the globe) with the
 * same keys and monitors as all fullscreen modes of Bio7 (see
 * {@link FullscreenMonitors}): F2 the primary monitor (exits in fullscreen
 * mode), F3 the secondary, F4 the tertiary monitor, F5 a selected monitor and
 * ESC exits. The monitors of SWT (primary monitor first) are mapped to the
 * monitor devices of NEWT by their bounds.
 */
public final class NewtFullscreen {

	private NewtFullscreen() {
	}

	/**
	 * Returns 2 to 5 for the NEWT keys F2 to F5, else 0.
	 */
	public static int functionKey(short newtKeyCode) {
		switch (newtKeyCode) {
		case com.jogamp.newt.event.KeyEvent.VK_F2:
			return 2;
		case com.jogamp.newt.event.KeyEvent.VK_F3:
			return 3;
		case com.jogamp.newt.event.KeyEvent.VK_F4:
			return 4;
		case com.jogamp.newt.event.KeyEvent.VK_F5:
			return 5;
		default:
			return 0;
		}
	}

	/**
	 * The action of a fullscreen key (2 to 5, see {@link #functionKey(short)}) for
	 * the window. Can be called in any thread (the dialog of F5 is opened in the
	 * SWT UI thread).
	 *
	 * @param exit        exits the fullscreen mode of the view
	 * @param beforeEnter called before the fullscreen mode is entered or null
	 */
	public static void handleKey(GLWindow window, int key, Runnable exit, Runnable beforeEnter) {
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			if (key == FullscreenMonitors.KEY_PRIMARY && window.isFullscreen()) {
				exit.run();
				return;
			}
			Monitor[] monitors = FullscreenMonitors.getMonitors(display);
			Monitor current = window.isFullscreen() ? swtMonitor(window, monitors) : null;
			Monitor target = FullscreenMonitors.monitorForKey(key, display, null, current);
			if (target == null) {
				return;
			}
			if (window.isFullscreen() && FullscreenMonitors.same(target, current)) {
				exit.run();
				return;
			}
			enter(window, target.getBounds(), FullscreenMonitors.indexOf(monitors, target), beforeEnter);
		});
	}

	/**
	 * Displays the window in fullscreen mode on monitor i (0: the primary, 1: the
	 * secondary monitor, etc.). Can be called in any thread.
	 *
	 * @param beforeEnter called before the fullscreen mode is entered or null
	 */
	public static void enter(GLWindow window, int i, Runnable beforeEnter) {
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			Monitor[] monitors = FullscreenMonitors.getMonitors(display);
			if (i >= 0 && i < monitors.length) {
				enter(window, monitors[i].getBounds(), i, beforeEnter);
			} else {
				display.beep();
			}
		});
	}

	/* In a job which waits for the monitors (available after the window was created) */
	private static void enter(GLWindow window, Rectangle bounds, int index, Runnable beforeEnter) {
		if (beforeEnter != null) {
			beforeEnter.run();
		}
		Job job = new Job("Fullscreen") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Fullscreen mode ...", 20);
				Screen screen = window.getScreen();
				while (screen.getMonitorDevices() == null && !monitor.isCanceled()) {
					monitor.worked(1);
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return Status.CANCEL_STATUS;
					}
				}
				MonitorDevice dev = device(screen, bounds, index);
				if (dev != null) {
					List<MonitorDevice> monitorDevices = new ArrayList<>();
					monitorDevices.add(dev);
					/* Another monitor: the window leaves the fullscreen mode first */
					if (window.isFullscreen()) {
						window.setFullscreen(false);
					}
					/* Workaround for windows to correctly set fullscreen coordinates! */
					if (Util.getOS().equals("Windows")) {
						window.setVisible(false);
						window.setFullscreen(monitorDevices);
						window.setFullscreen(false);
						window.setFullscreen(monitorDevices);
						window.setVisible(true);
					} else {
						window.setFullscreen(monitorDevices);
					}
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/* The NEWT monitor of the SWT monitor bounds (or of the index if the bounds don't match, e.g., other units) */
	private static MonitorDevice device(Screen screen, Rectangle bounds, int index) {
		List<MonitorDevice> devices = screen.getMonitorDevices();
		if (devices == null || devices.isEmpty()) {
			return null;
		}
		for (MonitorDevice d : devices) {
			if (equal(d.getViewportInWindowUnits(), bounds)) {
				return d;
			}
		}
		for (MonitorDevice d : devices) {
			if (equal(d.getViewport(), bounds)) {
				return d;
			}
		}
		List<MonitorDevice> ordered = ordered(screen, devices);
		return index >= 0 && index < ordered.size() ? ordered.get(index) : null;
	}

	/* The SWT monitor of the fullscreen window or null */
	private static Monitor swtMonitor(GLWindow window, Monitor[] monitors) {
		MonitorDevice main = window.getMainMonitor();
		if (main == null) {
			return null;
		}
		for (Monitor m : monitors) {
			if (equal(main.getViewportInWindowUnits(), m.getBounds()) || equal(main.getViewport(), m.getBounds())) {
				return m;
			}
		}
		List<MonitorDevice> devices = window.getScreen().getMonitorDevices();
		if (devices == null) {
			return null;
		}
		int index = ordered(window.getScreen(), devices).indexOf(main);
		return index >= 0 && index < monitors.length ? monitors[index] : null;
	}

	/* The monitors with the primary monitor first (like the SWT monitors) */
	private static List<MonitorDevice> ordered(Screen screen, List<MonitorDevice> devices) {
		List<MonitorDevice> ordered = new ArrayList<>();
		MonitorDevice primary = screen.getPrimaryMonitor();
		if (primary != null) {
			ordered.add(primary);
		}
		for (MonitorDevice d : devices) {
			if (d != primary) {
				ordered.add(d);
			}
		}
		return ordered;
	}

	private static boolean equal(RectangleImmutable r, Rectangle b) {
		return r != null && r.getX() == b.x && r.getY() == b.y && r.getWidth() == b.width && r.getHeight() == b.height;
	}
}
