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

import java.awt.Dimension;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.image.ui.CanvasView;
import com.eco.bio7.rffm.RSession;
import com.eco.bio7.rffm.graphics.Java2DGraphicsDevice;
import com.eco.bio7.rffm.graphics.RDeviceFactory;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * An R graphics device shown in the plot canvas of SWT ImageJ (
 * embedded R only): R draws with Java2D (in the thread of R); the frames are
 * shown while R draws (like a screen device of R). Resizing the plot window
 * draws the plot again in the new size (the PlotCanvas calls setSize).
 * <p>
 * This is a Plot (for the PlotWindow and the PlotCanvas of ImageJ) whose image
 * comes from R; the data functions of ImageJ plots (List, Data) don't apply.
 */
public class RPlotCanvasDevice extends Plot {

	/* The resolution: lwd 1 is one pixel, as the screen devices of R */
	private static final double DPI = 96;
	/** The number of plots of the history (back/forward), see the plot preferences (0 = off). */
	static int historySize() {
		try {
			return Math.max(0, com.eco.bio7.Bio7Plugin.getDefault().getPreferenceStore().getInt("R_PLOT_HISTORY"));
		} catch (RuntimeException e) {
			return 30;
		}
	}
	/* The end of locator() (a right click or Esc) */
	private static final double[] STOP = new double[0];
	private final java.util.concurrent.LinkedBlockingQueue<double[]> clicks = new java.util.concurrent.LinkedBlockingQueue<>();
	private volatile boolean locating;
	private org.eclipse.swt.widgets.Label historyLabel;
	private org.eclipse.swt.widgets.Control statusControl;

	private final RSession session;
	private final Java2DGraphicsDevice device;
	private final double scale;
	private volatile ColorProcessor ip;
	private volatile int contentVersion;
	private ImagePlus imagePlus;
	private volatile boolean closedByR;
	/* The latest frame for the UI (pixels at the device size) and if an update is pending */
	private volatile int[] pendingPixels;
	private volatile ColorProcessor pendingHiRes;
	/* The high resolution image of the shown frame (UI thread) */
	private ColorProcessor shownHiRes;
	private volatile int pendingWidth, pendingHeight;
	/* The number of the latest frame and of the shown one */
	private long frameVersion, appliedVersion;
	private final java.util.concurrent.atomic.AtomicBoolean updateScheduled = new java.util.concurrent.atomic.AtomicBoolean();

	private RPlotCanvasDevice(RSession session, String title, int width, int height, double scale) {
		super(title, "", "");
		this.session = session;
		this.scale = scale;
		this.device = new Java2DGraphicsDevice(width, height, DPI, scale);
		this.ip = blank(width, height);
		device.setFrameListener(this::frame);
		device.setLocatorHandler(this::waitForClick);
		disableImageJRanges();
		device.setCloseListener(this::closedInR);
	}

	/** The factory for RSession.registerDevice (called in the thread of R). */
	public static RDeviceFactory factory(RSession session) {
		return (widthInches, heightInches, pointsize) -> {
			int[] size = preferredSize(widthInches, heightInches);
			RPlotCanvasDevice plot = new RPlotCanvasDevice(session, "R Plot", size[0], size[1], displayScale());
			Display.getDefault().asyncExec(plot::open);
			return new RDeviceFactory.DeviceSpec(plot.device, DPI, pointsize, historySize());
		};
	}

	/* The size of the ImageJ view for the default size (7 x 7 inches), else the requested size */
	private static int[] preferredSize(double widthInches, double heightInches) {
		if (widthInches != 7 || heightInches != 7) {
			return new int[] { (int) Math.round(widthInches * DPI), (int) Math.round(heightInches * DPI) };
		}
		int[] size = { 640, 480 };
		int[] fitted = lastFittedSize;
		Rectangle area = viewArea;
		if (fitted != null) {
			/* The space of the last plot window (the next one has the same layout) */
			size[0] = fitted[0];
			size[1] = fitted[1];
		} else if (area != null && area.width > 100 && area.height > 100) {
			size[0] = area.width - 20;
			size[1] = area.height - 90;
		}
		return plotSize(size[0], size[1]);
	}

	/* The client area of the ImageJ view (updated in the UI thread, read in the thread of R) */
	private static volatile Rectangle viewArea;
	private static volatile double viewScale = 1;
	/* The size which fitted into the last plot window */
	private static volatile int[] lastFittedSize;

	/** Remembers the size of the ImageJ view and the zoom of the display (call in the UI thread). */
	public static void updateViewSize() {
		CanvasView view = CanvasView.getCanvas_view();
		if (view != null && view.getParent2() != null && !view.getParent2().isDisposed()) {
			viewArea = view.getParent2().getClientArea();
		}
		Display display = Display.getDefault();
		if (display.getPrimaryMonitor() != null) {
			viewScale = display.getPrimaryMonitor().getZoom() / 100.0;
		}
	}

	/* As the PlotCanvas: at least 2x for smooth lines and text (see PlotCanvas.getDisplaySwtImage) */
	private static double displayScale() {
		return Math.max(2.0, viewScale);
	}

	/* Shows the plot in the ImageJ view (UI thread) */
	private void open() {
		show();
		/* The device gets the size of the plot area of the window (when the layout is done) */
		Display display = Display.getDefault();
		display.asyncExec(this::installActions);
		display.asyncExec(this::fitToWindow);
		display.timerExec(300, this::fitToWindow);
		ImagePlus.addImageListener(new ImageListener() {
			@Override
			public void imageOpened(ImagePlus imp) {
			}

			@Override
			public void imageUpdated(ImagePlus imp) {
			}

			@Override
			public void imageClosed(ImagePlus imp) {
				if (imp == imagePlus) {
					ImagePlus.removeImageListener(this);
					closedByUser();
				}
			}
		});
	}

	/*
	 * The buttons of ImageJ plots (List, Data, More, Live) work with the data of an
	 * ImageJ plot, which an R plot doesn't have: they are replaced with actions for
	 * the R plot.
	 */
	private void installActions() {
		ImagePlus imp = imagePlus;
		if (imp == null || !(imp.getWindow() instanceof ij.gui.PlotWindow window) || window.bottomPanel == null || window.bottomPanel.isDisposed()) {
			return;
		}
		org.eclipse.swt.widgets.Composite panel = window.bottomPanel;
		org.eclipse.swt.widgets.Control status = null;
		for (var child : panel.getChildren()) {
			if (child instanceof org.eclipse.swt.widgets.Button) {
				child.setVisible(false);
				if (child.getLayoutData() instanceof org.eclipse.swt.layout.GridData gd) {
					gd.exclude = true;
				}
			} else if (status == null) {
				status = child;
			}
		}
		statusControl = status;
		/* No coordinate readout of ImageJ (the data coordinates of an ImageJ plot) */
		window.showStatus("");
		org.eclipse.swt.widgets.Button back = button(panel, "\u25C0", "The previous plot (history)", () -> moveInHistory(-1));
		org.eclipse.swt.widgets.Button forward = button(panel, "\u25B6", "The next plot (history)", () -> moveInHistory(1));
		historyLabel = new org.eclipse.swt.widgets.Label(panel, org.eclipse.swt.SWT.NONE);
		historyLabel.setText("      ");
		historyLabel.setToolTipText("The position in the history of the plots");
		org.eclipse.swt.widgets.Button save = button(panel, "Save...", "Save the plot as PNG, PDF or SVG (drawn again by R)", this::save);
		org.eclipse.swt.widgets.Button copy = button(panel, "Copy", "Copy the plot image to the clipboard", this::copy);
		org.eclipse.swt.widgets.Button toImageJ = button(panel, "To ImageJ", "Open the plot as an ImageJ image (for the ImageJ tools)", this::toImageJ);
		installClicks(imp);
		if (status != null) {
			back.moveAbove(status);
			forward.moveAbove(status);
			historyLabel.moveAbove(status);
			save.moveAbove(status);
			copy.moveAbove(status);
			toImageJ.moveAbove(status);
		}
		/* All in one row: the hidden buttons don't count, the status label takes the remaining space */
		if (panel.getLayout() instanceof org.eclipse.swt.layout.GridLayout grid) {
			int visible = 0;
			for (var child : panel.getChildren()) {
				if (!(child.getLayoutData() instanceof org.eclipse.swt.layout.GridData gd && gd.exclude)) {
					visible++;
				}
			}
			grid.numColumns = Math.max(1, visible);
		}
		if (status != null && status.getLayoutData() instanceof org.eclipse.swt.layout.GridData sd) {
			sd.horizontalSpan = 1;
			sd.grabExcessHorizontalSpace = true;
		}
		panel.layout(true, true);
		window.parentComposite.layout(true, true);
	}

	/*
	 * The range arrows, the R/F icons and the wheel zoom of ImageJ plots change the
	 * axis ranges of an ImageJ plot, which an R plot doesn't have. The PlotWindow
	 * shows them when the mouse is outside the frame of the plot: the frame (package
	 * fields of Plot) is set so that it never is (and the wheel is ignored).
	 */
	private void disableImageJRanges() {
		setPlotField("leftMargin", 0);
		setPlotField("topMargin", 0);
		setPlotField("frameWidth", -1);
		setPlotField("frameHeight", 1 << 28);
	}

	private void setPlotField(String name, int value) {
		try {
			java.lang.reflect.Field f = Plot.class.getDeclaredField(name);
			f.setAccessible(true);
			f.setInt(this, value);
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another version of ImageJ: the arrows stay visible (without function) */
		}
	}

	private static org.eclipse.swt.widgets.Button button(org.eclipse.swt.widgets.Composite parent, String text, String tip, Runnable action) {
		var b = new org.eclipse.swt.widgets.Button(parent, org.eclipse.swt.SWT.PUSH);
		b.setText(text);
		b.setToolTipText(tip);
		b.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.CENTER, false, false));
		b.addListener(org.eclipse.swt.SWT.Selection, e -> action.run());
		return b;
	}

	/* Saves the plot: R draws it again into a file device of the format (vector formats stay vectors) */
	private void save() {
		var dialog = new org.eclipse.swt.widgets.FileDialog(Display.getDefault().getActiveShell(), org.eclipse.swt.SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "*.png", "*.pdf", "*.svg" });
		dialog.setFilterNames(new String[] { "PNG image", "PDF document", "SVG graphics" });
		dialog.setFileName("plot.png");
		dialog.setOverwrite(true);
		String file = dialog.open();
		if (file == null) {
			return;
		}
		String lower = file.toLowerCase(java.util.Locale.ROOT);
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
		String devCopy = copy;
		runInR(n -> "local({ cur <- dev.cur(); dev.set(" + n + "); on.exit(if (cur %in% dev.list()) dev.set(cur)); dev.copy(" + devCopy + "); dev.off() })");
	}

	/* Shows the previous or next plot of the history (R draws it) */
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

	/* The clicks for locator() and identify(): left click a point, right click or Esc stops */
	private void installClicks(ImagePlus imp) {
		var canvas = imp.getCanvas();
		if (canvas == null) {
			return;
		}
		canvas.addListener(org.eclipse.swt.SWT.MouseDown, e -> {
			if (!locating) {
				return;
			}
			if (e.button == 1) {
				clicks.offer(new double[] { canvas.offScreenXD(e.x), canvas.offScreenYD(e.y) });
			} else {
				clicks.offer(STOP);
			}
		});
		canvas.addListener(org.eclipse.swt.SWT.KeyDown, e -> {
			if (locating && e.keyCode == org.eclipse.swt.SWT.ESC) {
				clicks.offer(STOP);
			}
		});
	}

	/* locator() in the thread of R: waits for a click (or the end, an interrupt, the closed plot) */
	private double[] waitForClick() {
		if (session.isLimitedThreadWaiting()) {
			/* The UI thread waits for R (the code runs from the UI): no click could arrive */
			System.err.println("locator(): not available while the UI waits for R (run the code in the console or a job)");
			return null;
		}
		clicks.clear();
		locating = true;
		Display display = Display.getDefault();
		display.asyncExec(() -> locatorMode(true));
		try {
			while (true) {
				double[] p = clicks.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
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

	/* The crosshair cursor and a hint while R waits for clicks */
	private void locatorMode(boolean on) {
		ImagePlus imp = imagePlus;
		if (imp == null || isDisposed(imp)) {
			return;
		}
		var canvas = imp.getCanvas();
		canvas.setCursor(on ? Display.getDefault().getSystemCursor(org.eclipse.swt.SWT.CURSOR_CROSS) : null);
		if (on) {
			canvas.setFocus();
		}
		if (imp.getWindow() instanceof ij.gui.PlotWindow window) {
			window.showStatus(on ? "R waits for clicks: left click a point, right click or Esc to stop" : "");
		}
	}

	/* Copies the image of the plot to the clipboard */
	private void copy() {
		ImagePlus imp = imagePlus;
		if (imp == null) {
			return;
		}
		org.eclipse.swt.graphics.Image image = imp.getSwtImage();
		if (image == null) {
			return;
		}
		var clipboard = new org.eclipse.swt.dnd.Clipboard(Display.getDefault());
		try {
			clipboard.setContents(new Object[] { image.getImageData() }, new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.ImageTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	/* Opens a copy of the plot as a normal ImageJ image */
	private void toImageJ() {
		ImagePlus imp = imagePlus;
		if (imp != null) {
			new ImagePlus(getTitle() + " (image)", imp.getProcessor().duplicate()).show();
		}
	}

	/* Evaluates code for this device in R (not in the UI thread: R may be busy) */
	private void runInR(java.util.function.IntFunction<String> code) {
		Thread t = new Thread(() -> {
			try {
				int n = session.deviceNumber(device);
				if (n > 1) {
					session.voidEval(code.apply(n));
				}
			} catch (Exception e) {
				String message = e.getMessage();
				Display.getDefault().asyncExec(() -> org.eclipse.jface.dialogs.MessageDialog.openError(Display.getDefault().getActiveShell(), "R Plot", message));
			}
		}, "R plot action");
		t.setDaemon(true);
		t.start();
	}

	/*
	 * Resizes the device to the space of the canvas in the plot window: the area of
	 * the window without its other rows (the buttons below the plot).
	 */
	private void fitToWindow() {
		ImagePlus imp = imagePlus;
		if (imp == null || isDisposed(imp) || imp.getWindow() == null) {
			return;
		}
		var window = imp.getWindow();
		if (window.parentComposite == null || window.parentComposite.isDisposed()) {
			return;
		}
		Rectangle area = window.getSwtParentBounds();
		int width = area.width;
		int height = area.height;
		/* Only the rows below the canvas (the buttons of the plot window) */
		var canvas = imp.getCanvas();
		if (canvas != null) {
			int canvasTop = canvas.getBounds().y;
			for (var child : window.parentComposite.getChildren()) {
				if (child != canvas && child.isVisible() && child.getBounds().y > canvasTop) {
					height -= child.getSize().y;
				}
			}
		}
		if (width > MIN_WIDTH && height > MIN_HEIGHT) {
			int[] size = plotSize(width, height);
			lastFittedSize = size;
			setSize(size[0], size[1]);
		}
	}

	/*
	 * A frame of R (thread of R): the pixels for ImageJ and the high resolution
	 * image for the display. One update of the UI is pending at most (R can draw
	 * faster than the UI shows).
	 */
	private void frame(Java2DGraphicsDevice.Frame f) {
		ColorProcessor full = new ColorProcessor(f.width(), f.height(), f.argb());
		int w = (int) Math.round(f.width() / f.scale());
		int h = (int) Math.round(f.height() / f.scale());
		int[] pixels;
		if (f.scale() == 1) {
			pixels = f.argb().clone();
		} else if (f.width() == 2 * w && f.height() == 2 * h) {
			pixels = halve(f.argb(), f.width(), w, h);
		} else {
			full.setInterpolationMethod(ImageProcessor.BILINEAR);
			pixels = (int[]) full.resize(w, h, true).getPixels();
		}
		synchronized (this) {
			pendingHiRes = full;
			pendingWidth = w;
			pendingHeight = h;
			pendingPixels = pixels;
			frameVersion++;
		}
		if (updateScheduled.compareAndSet(false, true)) {
			Display.getDefault().asyncExec(this::updateImage);
		}
	}

	private void closedInR() {
		closedByR = true;
		Display.getDefault().asyncExec(() -> {
			if (imagePlus != null && imagePlus.getWindow() != null) {
				imagePlus.close();
			}
		});
	}

	/* The user closed the plot: close the device in R (not in the UI thread: R may be busy) */
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

	private static ColorProcessor blank(int w, int h) {
		ColorProcessor cp = new ColorProcessor(Math.max(1, w), Math.max(1, h));
		cp.setColor(java.awt.Color.WHITE);
		cp.fill();
		return cp;
	}

	/* ---------------- Plot (for the PlotWindow and the PlotCanvas) ---------------- */

	/* The smallest size of the device: smaller sizes (e.g., a minimized view) keep the current plot */
	private static final int MIN_WIDTH = 320, MIN_HEIGHT = 240;
	/*
	 * The smallest size R draws in: plots with several panels (e.g., par(mfrow = c(2, 3)))
	 * need it ("figure margins too large"); a smaller view shows the plot zoomed out
	 */
	private static final int MIN_PLOT_WIDTH = 480, MIN_PLOT_HEIGHT = 420;

	private static int[] plotSize(int width, int height) {
		return new int[] { Math.max(width, MIN_PLOT_WIDTH), Math.max(height, MIN_PLOT_HEIGHT) };
	}

	/** The PlotCanvas was resized: R draws the plot again in the new size. */
	@Override
	public void setSize(int width, int height) {
		/* The PlotCanvas passes at least the minimum size: that is a too small view (minimized), not a plot size */
		if (width <= MIN_WIDTH || height <= MIN_HEIGHT) {
			return;
		}
		int[] size = plotSize(width, height);
		int[] requested = requestedSize;
		/* The same size again (each resize step sends it): no new timer, no new drawing */
		if (requested != null && requested[0] == size[0] && requested[1] == size[1]) {
			return;
		}
		requestedSize = size;
		if (size[0] == (int) device.width() && size[1] == (int) device.height()) {
			return;
		}
		session.resizeDevice(device, size[0], size[1]);
	}

	/* The last size sent to R */
	private volatile int[] requestedSize;

	/* The frame size of ImageJ plots (with their margins): not used, R draws the whole size */
	@Override
	public void setFrameSize(int width, int height) {
	}

	@Override
	public void setWindowSize(int width, int height) {
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(MIN_WIDTH, MIN_HEIGHT);
	}

	@Override
	public Dimension getSize() {
		return new Dimension(ip.getWidth(), ip.getHeight());
	}

	@Override
	public ImageProcessor getProcessor() {
		return ip;
	}

	@Override
	public ImagePlus getImagePlus() {
		if (imagePlus == null) {
			imagePlus = new ImagePlus(getTitle(), ip);
		}
		return imagePlus;
	}

	@Override
	public void setImagePlus(ImagePlus imp) {
		this.imagePlus = imp;
	}

	/* The image comes from R: nothing to draw */
	@Override
	public void draw() {
	}

	@Override
	public void update() {
		updateImage();
	}

	/**
	 * Shows the latest frame (UI thread): the pixels are copied into the processor
	 * of the image; a new processor (which changes the window layout) only if the
	 * size changed.
	 */
	@Override
	public void updateImage() {
		updateScheduled.set(false);
		ImagePlus imp = imagePlus;
		int[] pixels;
		int w, h;
		ColorProcessor hi;
		long version;
		synchronized (this) {
			pixels = pendingPixels;
			w = pendingWidth;
			h = pendingHeight;
			hi = pendingHiRes;
			version = frameVersion;
		}
		/* Nothing new (e.g., an update of ImageJ while resizing): the display keeps its cached image */
		if (imp == null || pixels == null || isDisposed(imp) || version == appliedVersion) {
			return;
		}
		appliedVersion = version;
		showOnNewPlot(imp);
		/* The high resolution image belongs to the shown frame (the PlotCanvas draws it with the size of the image) */
		shownHiRes = hi;
		contentVersion++;
		ColorProcessor current = ip;
		if (current.getWidth() == w && current.getHeight() == h && imp.getProcessor() == current) {
			System.arraycopy(pixels, 0, (int[]) current.getPixels(), 0, Math.min(pixels.length, w * h));
			imp.updateAndDraw();
		} else {
			ColorProcessor cp = new ColorProcessor(w, h, pixels);
			ip = cp;
			imp.setProcessor(cp);
		}
	}

	/* The page count of the device when the view was last checked (UI thread) */
	private long checkedPages;

	/*
	 * A new plot (also in another perspective): the view of the plot (the
	 * ImageJ-Canvas view or a detached view) is shown in the current perspective
	 * and its tab is selected
	 */
	private void showOnNewPlot(ImagePlus imp) {
		long pages = device.pageCount();
		if (pages == checkedPages) {
			return;
		}
		checkedPages = pages;
		var detached = com.eco.bio7.image.ui.CustomDetachedImageJView.find(imp);
		boolean shown = detached != null
				? PlotViews.showInPerspective(detached.getViewSite().getId(), detached.getViewSite().getSecondaryId())
				: PlotViews.showInPerspective(CanvasView.ID, null);
		if (shown && imp.getWindow() != null) {
			imp.getWindow().toFront();
		}
	}

	/* The plot window was closed (its canvas is disposed) */
	private static boolean isDisposed(ImagePlus imp) {
		var canvas = imp.getCanvas();
		return canvas != null && canvas.isDisposed();
	}

	@Override
	public boolean isFrozen() {
		return false;
	}

	@Override
	public int getContentVersion() {
		return contentVersion;
	}

	/**
	 * The rendering at the scale of the display (used by the PlotCanvas for HiDPI),
	 * always exactly the size of the shown image times the scale: the canvas draws
	 * a source rectangle of that size.
	 */
	@Override
	public ImagePlus makeHighResolution(String title, float requestedScale, boolean antialiasedText, boolean showIt) {
		ColorProcessor hi = shownHiRes;
		ImageProcessor shown = imagePlus != null ? imagePlus.getProcessor() : ip;
		if (hi == null || shown == null) {
			return null;
		}
		int w = (int) Math.round(shown.getWidth() * requestedScale);
		int h = (int) Math.round(shown.getHeight() * requestedScale);
		ImageProcessor p = hi;
		if (hi.getWidth() != w || hi.getHeight() != h) {
			hi.setInterpolationMethod(ImageProcessor.BILINEAR);
			p = hi.resize(w, h, true);
		}
		return new ImagePlus(title == null ? getTitle() : title, p);
	}

	/* Half the size: the average of 2 x 2 pixels (fast for a scale of 2) */
	private static int[] halve(int[] src, int srcWidth, int w, int h) {
		int[] dst = new int[w * h];
		for (int y = 0; y < h; y++) {
			int r0 = 2 * y * srcWidth, r1 = r0 + srcWidth;
			for (int x = 0; x < w; x++) {
				int a = src[r0 + 2 * x], b = src[r0 + 2 * x + 1], c = src[r1 + 2 * x], d = src[r1 + 2 * x + 1];
				int r = (((a >> 16) & 255) + ((b >> 16) & 255) + ((c >> 16) & 255) + ((d >> 16) & 255) + 2) >> 2;
				int g = (((a >> 8) & 255) + ((b >> 8) & 255) + ((c >> 8) & 255) + ((d >> 8) & 255) + 2) >> 2;
				int bl = ((a & 255) + (b & 255) + (c & 255) + (d & 255) + 2) >> 2;
				dst[y * w + x] = 0xFF000000 | (r << 16) | (g << 8) | bl;
			}
		}
		return dst;
	}
}
