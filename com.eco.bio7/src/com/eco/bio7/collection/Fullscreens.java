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

package com.eco.bio7.collection;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.spatial.SpatialView;
import com.eco.bio7.util.FullscreenMonitors;
import com.eco.bio7.worldwind.WorldWindView;

import ij.ImagePlus;
import ij.WindowManager;

/**
 * A small API to display the views of Bio7 in fullscreen mode on different
 * monitors, e.g., in a script:
 *
 * <pre>
 * Fullscreens.rPlot(1);              // the R plot on the secondary monitor
 * Fullscreens.view("My View", 0);    // a custom view on the primary monitor
 * Fullscreens.image(2);              // the current ImageJ image on the third monitor
 * Fullscreens.exitAll();
 * </pre>
 *
 * The monitors are numbered with the primary monitor first (0: primary, 1:
 * secondary, 2: tertiary monitor, etc.), as the keys F2, F3, F4. Custom views
 * (also the R and Python plots) and images can be displayed at the same time,
 * one per monitor: a view or image displayed on a monitor which already shows
 * another view (or image) replaces it. All methods can be called from any
 * thread. They return false if the view, image or monitor is not available.
 */
public final class Fullscreens {

	/** The title of the view of the R plots (the first device). */
	public static final String R_PLOT = "R Plot";
	/** The title of the view of the Python plots. */
	public static final String PYTHON_PLOT = "Python Plot";

	private static final String CUSTOM_VIEW_ID = "com.eco.bio7.custom_controls";

	private Fullscreens() {
	}

	/** Returns the number of monitors. */
	public static int getMonitorCount() {
		return sync(() -> FullscreenMonitors.getMonitors(Display.getDefault()).length, 0);
	}

	/**
	 * Returns the bounds of the monitors (primary monitor first) in the
	 * coordinates of SWT (x, y, width, height).
	 */
	public static int[][] getMonitorBounds() {
		return sync(() -> {
			Monitor[] monitors = FullscreenMonitors.getMonitors(Display.getDefault());
			int[][] b = new int[monitors.length][];
			for (int i = 0; i < monitors.length; i++) {
				Rectangle r = monitors[i].getBounds();
				b[i] = new int[] { r.x, r.y, r.width, r.height };
			}
			return b;
		}, new int[0][]);
	}

	/* Custom views (also the R and Python plots) */

	/**
	 * Displays the custom view with the title (the name of its tab, e.g., "R Plot",
	 * "R Plot 2", "Python Plot") in fullscreen mode on the monitor or moves it to
	 * the monitor.
	 */
	public static boolean view(String title, int monitor) {
		return sync(() -> {
			Monitor m = monitor(monitor);
			CustomView v = customView(title);
			if (m == null || v == null) {
				return false;
			}
			CustomViewFullscreen.show(v, m);
			return CustomViewFullscreen.isFullscreen(v);
		}, false);
	}

	/** Displays the R plot (the first plot device) in fullscreen mode on the monitor. */
	public static boolean rPlot(int monitor) {
		return view(R_PLOT, monitor);
	}

	/** Displays the R plot of the device number (1, 2, ...) in fullscreen mode on the monitor. */
	public static boolean rPlot(int device, int monitor) {
		return view(device <= 1 ? R_PLOT : R_PLOT + " " + device, monitor);
	}

	/** Displays the Python plot in fullscreen mode on the monitor. */
	public static boolean pythonPlot(int monitor) {
		return view(PYTHON_PLOT, monitor);
	}

	/** Closes the fullscreen mode of the custom view with the title. */
	public static boolean exitView(String title) {
		return sync(() -> {
			CustomView v = customView(title);
			if (v == null || !CustomViewFullscreen.isFullscreen(v)) {
				return false;
			}
			CustomViewFullscreen.exit(v);
			return true;
		}, false);
	}

	/** Returns true if the custom view with the title is displayed in fullscreen mode. */
	public static boolean isViewFullscreen(String title) {
		return sync(() -> {
			CustomView v = customView(title);
			return v != null && CustomViewFullscreen.isFullscreen(v);
		}, false);
	}

	/* ImageJ images */

	/** Displays the current ImageJ image in fullscreen mode on the monitor. */
	public static boolean image(int monitor) {
		return image(WindowManager.getCurrentImage(), monitor);
	}

	/** Displays the ImageJ image with the title in fullscreen mode on the monitor. */
	public static boolean image(String title, int monitor) {
		return image(WindowManager.getImage(title), monitor);
	}

	/** Displays the ImageJ image in fullscreen mode on the monitor or moves it to the monitor. */
	public static boolean image(ImagePlus imp, int monitor) {
		if (imp == null) {
			return false;
		}
		return sync(() -> {
			Monitor m = monitor(monitor);
			if (m == null) {
				return false;
			}
			if (com.eco.bio7.image.ui.Fullscreen.isFullscreen(imp)) {
				com.eco.bio7.image.ui.Fullscreen.exit(imp);
			}
			com.eco.bio7.image.ui.Fullscreen.enter(imp, m);
			return com.eco.bio7.image.ui.Fullscreen.isFullscreen(imp);
		}, false);
	}

	/** Closes the fullscreen mode of the ImageJ image with the title. */
	public static boolean exitImage(String title) {
		ImagePlus imp = WindowManager.getImage(title);
		return imp != null && sync(() -> {
			boolean was = com.eco.bio7.image.ui.Fullscreen.isFullscreen(imp);
			com.eco.bio7.image.ui.Fullscreen.exit(imp);
			return was;
		}, false);
	}

	/* The OpenGL views (displayed in fullscreen mode by the window of NEWT) */

	/** Displays the globe of WorldWind in fullscreen mode on the monitor. */
	public static boolean worldWind(int monitor) {
		WorldWindView v = WorldWindView.getInstance();
		if (v == null || !validMonitor(monitor)) {
			return false;
		}
		v.createFullscreen(monitor);
		return true;
	}

	/** Displays the Space (3D) view in fullscreen mode on the monitor. */
	public static boolean spatial(int monitor) {
		SpatialView v = spatialView();
		if (v == null || !validMonitor(monitor)) {
			return false;
		}
		v.createFullscreen(monitor);
		return true;
	}

	/** Closes the fullscreen mode of the globe of WorldWind. */
	public static void exitWorldWind() {
		WorldWindView v = WorldWindView.getInstance();
		if (v != null && v.isFullscreen()) {
			v.fullscreenKey(FullscreenMonitors.KEY_PRIMARY);
		}
	}

	/** Closes the fullscreen mode of the Space (3D) view. */
	public static void exitSpatial() {
		SpatialView v = spatialView();
		if (v != null && v.isFullscreen()) {
			v.fullscreenKey(FullscreenMonitors.KEY_PRIMARY);
		}
	}

	/** Closes all fullscreen modes (custom views, plots, images, WorldWind, Space view). */
	public static void exitAll() {
		sync(() -> {
			CustomViewFullscreen.exit();
			com.eco.bio7.image.ui.Fullscreen.exit();
			return true;
		}, true);
		exitWorldWind();
		exitSpatial();
	}

	/* The helpers */

	private static Monitor monitor(int index) {
		Monitor[] monitors = FullscreenMonitors.getMonitors(Display.getDefault());
		return index >= 0 && index < monitors.length ? monitors[index] : null;
	}

	private static boolean validMonitor(int index) {
		return index >= 0 && index < getMonitorCount();
	}

	/* The custom view with the title (the name of the tab or the secondary id), in the UI thread */
	private static CustomView customView(String title) {
		if (title == null) {
			return null;
		}
		for (IViewReference ref : viewReferences(CUSTOM_VIEW_ID)) {
			if (title.equals(ref.getPartName()) || title.equals(ref.getSecondaryId())) {
				IViewPart part = ref.getView(true);
				if (part instanceof CustomView cv) {
					return cv;
				}
			}
		}
		return null;
	}

	private static SpatialView spatialView() {
		return sync(() -> {
			for (IViewReference ref : viewReferences(SpatialView.ID)) {
				if (ref.getView(false) instanceof SpatialView sv) {
					return sv;
				}
			}
			return null;
		}, null);
	}

	private static List<IViewReference> viewReferences(String id) {
		List<IViewReference> refs = new ArrayList<>();
		if (!PlatformUI.isWorkbenchRunning()) {
			return refs;
		}
		for (IWorkbenchWindow w : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : w.getPages()) {
				for (IViewReference ref : page.getViewReferences()) {
					if (id.equals(ref.getId())) {
						refs.add(ref);
					}
				}
			}
		}
		return refs;
	}

	/* Runs in the SWT UI thread (also if called from a script thread) */
	private static <T> T sync(java.util.function.Supplier<T> action, T fallback) {
		Display display = Display.getDefault();
		if (display.isDisposed()) {
			return fallback;
		}
		if (display.getThread() == Thread.currentThread()) {
			return action.get();
		}
		List<T> result = new ArrayList<>(1);
		display.syncExec(() -> result.add(action.get()));
		return result.isEmpty() ? fallback : result.get(0);
	}
}
