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

package com.eco.bio7.rbridge.plot;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.rffm.RSession;
import com.eco.bio7.rffm.graphics.RDeviceFactory;
import com.eco.bio7.rffm.graphics.RecordingGraphicsDevice;

/**
 * An R graphics device drawn with SWT as vector graphics in a Bio7 custom view
 * (experimental, embedded R only). R records the drawing operations
 * (RecordingGraphicsDevice, in the thread of R), the UI draws them with the
 * SWT GC: sharp at any size and zoom. Resizing the view draws the plot again in
 * the new size; until then the current plot is scaled.
 */
public final class RPlotVectorDevice {

	private static final String VIEW_ID = "com.eco.bio7.custom_controls";
	private static final String TITLE = "R Plot";
	/* lwd 1 is one pixel (point), as the screen devices of R */
	private static final double DPI = 96;
	/* The smallest size R draws in (plots with several panels), a smaller view shows the plot scaled */
	private static final int MIN_PLOT_WIDTH = 480, MIN_PLOT_HEIGHT = 420;
	/* A smaller canvas is a minimized view: keep the size */
	private static final int MIN_CANVAS = 60;
	private static final double[] STOP = new double[0];

	/* The numbers of the open views (each device has its own view) */
	private static final Set<Integer> openViews = ConcurrentHashMap.newKeySet();
	/* The canvas size of the last view (the next device starts in it) */
	private static volatile int[] lastCanvasSize;

	private final RSession session;
	private final RecordingGraphicsDevice device;
	private final int number;
	private final String viewName;

	/* The latest page from R and if a redraw is scheduled */
	private volatile RecordingGraphicsDevice.Page pending;
	private final AtomicBoolean redrawScheduled = new AtomicBoolean();
	/* UI thread */
	private RecordingGraphicsDevice.Page shown;
	private double[] mapping = { 1, 0, 0 };
	private Composite root;
	private Canvas canvas;
	private Label historyLabel, statusLabel;
	private volatile boolean closedByR;
	private volatile int[] requestedSize;
	/* The page count of the device when the view was last checked (UI thread) */
	private long checkedPages;

	private final LinkedBlockingQueue<double[]> clicks = new LinkedBlockingQueue<>();
	private volatile boolean locating;

	private RPlotVectorDevice(RSession session, int width, int height) {
		this.session = session;
		this.number = nextNumber();
		this.viewName = number == 1 ? TITLE : TITLE + " " + number;
		this.device = new RecordingGraphicsDevice(width, height, DPI);
		device.setFrameInterval(100);
		device.setPageListener(this::page);
		device.setLocatorHandler(this::waitForClick);
		device.setCloseListener(this::closedInR);
	}

	/** The factory for RSession.registerDevice (called in the thread of R). */
	public static RDeviceFactory factory(RSession session) {
		return (widthInches, heightInches, pointsize) -> {
			int[] size = preferredSize(widthInches, heightInches);
			RPlotVectorDevice plot = new RPlotVectorDevice(session, size[0], size[1]);
			Display.getDefault().asyncExec(plot::open);
			return new RDeviceFactory.DeviceSpec(plot.device, DPI, pointsize, RPlotCanvasDevice.historySize());
		};
	}

	private static synchronized int nextNumber() {
		int n = 1;
		while (openViews.contains(n)) {
			n++;
		}
		openViews.add(n);
		return n;
	}

	/* The canvas of the last view for the default size (7 x 7 inches), else the requested size */
	private static int[] preferredSize(double widthInches, double heightInches) {
		if (widthInches != 7 || heightInches != 7) {
			return new int[] { (int) Math.round(widthInches * DPI), (int) Math.round(heightInches * DPI) };
		}
		int[] last = lastCanvasSize;
		return plotSize(last != null ? last[0] : 640, last != null ? last[1] : 480);
	}

	private static int[] plotSize(int width, int height) {
		return new int[] { Math.max(width, MIN_PLOT_WIDTH), Math.max(height, MIN_PLOT_HEIGHT) };
	}

	/* ---------------- The view (UI thread) ---------------- */

	private void open() {
		if (closedByR) {
			openViews.remove(number);
			return;
		}
		Composite parent;
		try {
			parent = new CustomView().getComposite(viewName, icon());
		} catch (RuntimeException e) {
			openViews.remove(number);
			return;
		}
		if (parent == null || parent.isDisposed()) {
			openViews.remove(number);
			return;
		}
		/* A new plot view: in the folder of the ImageJ-Canvas view (if the perspective has one) */
		PlotViews.dockBesideImageJCanvas(VIEW_ID, viewName);
		root = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		root.setLayout(layout);

		canvas = new Canvas(root, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		canvas.addListener(SWT.Paint, e -> {
			Rectangle area = canvas.getClientArea();
			mapping = SwtPlotRenderer.paint(e.gc, shown, area.width, area.height);
		});
		canvas.addListener(SWT.Resize, _ -> canvasResized());
		installClicks();

		Composite bar = new Composite(root, SWT.NONE);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout barLayout = new GridLayout(6, false);
		barLayout.marginHeight = 2;
		bar.setLayout(barLayout);
		button(bar, "◀", "Previous plot", () -> moveInHistory(-1));
		button(bar, "▶", "Next plot", () -> moveInHistory(1));
		historyLabel = new Label(bar, SWT.NONE);
		historyLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button(bar, "Save...", "Save the plot (PNG, PDF or SVG; R draws it again)", this::save);
		button(bar, "Copy", "Copy the plot as image", this::copy);
		statusLabel = new Label(bar, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		root.addListener(SWT.Dispose, _ -> {
			openViews.remove(number);
			clicks.offer(STOP);
			closedByUser();
		});
		parent.layout(true, true);
		shown = pending != null ? pending : device.snapshot();
		canvas.redraw();
	}

	/* The R icon of the view tab (shared by all plot views, created once) */
	private static Image icon;

	private static Image icon() {
		if (icon == null || icon.isDisposed()) {
			var descriptor = com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/views/rview.svg");
			icon = descriptor != null ? descriptor.createImage(false) : null;
		}
		return icon;
	}

	private static void button(Composite parent, String text, String tip, Runnable action) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		b.setToolTipText(tip);
		b.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		b.addListener(SWT.Selection, _ -> action.run());
	}

	private boolean isOpen() {
		return canvas != null && !canvas.isDisposed();
	}

	/* The view was resized: R draws the plot again in the new size (debounced in RSession) */
	private void canvasResized() {
		Rectangle area = canvas.getClientArea();
		if (area.width < MIN_CANVAS || area.height < MIN_CANVAS) {
			return;
		}
		lastCanvasSize = new int[] { area.width, area.height };
		int[] size = plotSize(area.width, area.height);
		int[] requested = requestedSize;
		if (requested != null && requested[0] == size[0] && requested[1] == size[1]) {
			return;
		}
		requestedSize = size;
		if (size[0] == (int) device.width() && size[1] == (int) device.height()) {
			return;
		}
		session.resizeDevice(device, size[0], size[1]);
	}

	/* A page from R (thread of R): one redraw for the latest page */
	private void page(RecordingGraphicsDevice.Page page) {
		pending = page;
		if (redrawScheduled.compareAndSet(false, true)) {
			Display.getDefault().asyncExec(() -> {
				redrawScheduled.set(false);
				if (isOpen()) {
					shown = pending;
					canvas.redraw();
					/* A new plot (also in another perspective): the view is shown in the current perspective */
					long pages = device.pageCount();
					if (pages != checkedPages) {
						checkedPages = pages;
						PlotViews.showInPerspective(VIEW_ID, viewName);
					}
				}
			});
		}
	}

	/* ---------------- Actions ---------------- */

	private void moveInHistory(int step) {
		Thread t = new Thread(() -> {
			try {
				int[] state = session.history(device, step);
				Display.getDefault().asyncExec(() -> {
					if (historyLabel != null && !historyLabel.isDisposed()) {
						historyLabel.setText(state[0] == 0 ? "" : state[0] + "/" + state[1]);
						historyLabel.getParent().layout(true);
					}
				});
			} catch (Exception e) {
				/* R ended */
			}
		}, "R plot history");
		t.setDaemon(true);
		t.start();
	}

	/* Saves the plot: R draws it again into a file device (vector formats stay vectors) */
	private void save() {
		FileDialog dialog = new FileDialog(canvas.getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "*.png", "*.pdf", "*.svg" });
		dialog.setFilterNames(new String[] { "PNG image", "PDF document", "SVG graphics" });
		dialog.setFileName("plot.png");
		dialog.setOverwrite(true);
		String file = dialog.open();
		if (file == null) {
			return;
		}
		String lower = file.toLowerCase(Locale.ROOT);
		int w = (int) device.width(), h = (int) device.height();
		String path = file.replace("\\", "/").replace("'", "\\'");
		String copy;
		if (lower.endsWith(".pdf")) {
			copy = "pdf, file = '" + path + "', width = " + (w / DPI) + ", height = " + (h / DPI);
		} else if (lower.endsWith(".svg")) {
			copy = "svg, filename = '" + path + "', width = " + (w / DPI) + ", height = " + (h / DPI);
		} else {
			copy = "png, filename = '" + path + (lower.endsWith(".png") ? "" : ".png") + "', width = " + w + ", height = " + h + ", res = " + (int) DPI;
		}
		runInR(n -> "local({ cur <- dev.cur(); dev.set(" + n + "); on.exit(if (cur %in% dev.list()) dev.set(cur)); dev.copy(" + copy + "); dev.off() })");
	}

	/* Copies the plot as an image (drawn at the size of the device) */
	private void copy() {
		RecordingGraphicsDevice.Page page = shown;
		if (page == null) {
			return;
		}
		Image image = SwtPlotRenderer.toImage(canvas.getDisplay(), page);
		Clipboard clipboard = new Clipboard(canvas.getDisplay());
		try {
			clipboard.setContents(new Object[] { image.getImageData() }, new Transfer[] { ImageTransfer.getInstance() });
		} finally {
			clipboard.dispose();
			image.dispose();
		}
	}

	/* Evaluates code for this device in R (not in the UI thread: R may be busy) */
	private void runInR(IntFunction<String> code) {
		Thread t = new Thread(() -> {
			try {
				int n = session.deviceNumber(device);
				if (n > 1) {
					session.voidEval(code.apply(n));
				}
			} catch (Exception e) {
				String message = e.getMessage();
				Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(), TITLE, message));
			}
		}, "R plot action");
		t.setDaemon(true);
		t.start();
	}

	/* ---------------- locator() and identify() ---------------- */

	private void installClicks() {
		canvas.addListener(SWT.MouseDown, e -> {
			if (!locating) {
				return;
			}
			if (e.button == 1) {
				double[] m = mapping;
				clicks.offer(new double[] { (e.x - m[1]) / m[0], (e.y - m[2]) / m[0] });
			} else {
				clicks.offer(STOP);
			}
		});
		canvas.addListener(SWT.KeyDown, e -> {
			if (locating && e.keyCode == SWT.ESC) {
				clicks.offer(STOP);
			}
		});
	}

	/* In the thread of R: waits for a click (or the end, an interrupt, the closed plot) */
	private double[] waitForClick() {
		clicks.clear();
		locating = true;
		Display display = Display.getDefault();
		display.asyncExec(() -> locatorMode(true));
		try {
			while (true) {
				double[] p = clicks.poll(100, TimeUnit.MILLISECONDS);
				if (p != null) {
					return p == STOP ? null : p;
				}
				if (session.isInterruptRequested() || device.isClosed() || closedByR) {
					return null;
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} finally {
			locating = false;
			display.asyncExec(() -> locatorMode(false));
		}
	}

	private void locatorMode(boolean on) {
		if (!isOpen()) {
			return;
		}
		canvas.setCursor(on ? canvas.getDisplay().getSystemCursor(SWT.CURSOR_CROSS) : null);
		if (on) {
			canvas.setFocus();
		}
		statusLabel.setText(on ? "Left click a point, right click or Esc to stop" : "");
		statusLabel.getParent().layout(true);
	}

	/* ---------------- Closing ---------------- */

	/* dev.off() in R: close the view */
	private void closedInR() {
		closedByR = true;
		Display.getDefault().asyncExec(() -> {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null || window.getActivePage() == null) {
				return;
			}
			IWorkbenchPage page = window.getActivePage();
			IViewReference ref = page.findViewReference(VIEW_ID, viewName);
			if (ref != null && isOpen()) {
				page.hideView(ref);
			}
		});
	}

	/* The user closed the view: close the device in R (not in the UI thread: R may be busy) */
	private void closedByUser() {
		if (closedByR || !session.isAlive()) {
			return;
		}
		Thread t = new Thread(() -> {
			try {
				int n = session.deviceNumber(device);
				if (n > 1) {
					session.voidEval("if (" + n + " %in% dev.list()) dev.off(" + n + ")");
				}
			} catch (Exception e) {
				/* R ended */
			}
		}, "Close R device");
		t.setDaemon(true);
		t.start();
	}
}
