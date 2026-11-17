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
package com.eco.bio7.pybridge;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;

import com.eco.bio7.collection.CustomView;
import com.eco.bio7.pyffm.PyPlotListener;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.rbridge.plot.PlotViews;
import com.eco.bio7.rbridge.plot.TriangleButton;


/**
 * The plot device of the embedded Python: the matplotlib figures in a view
 * beside the console. The figures are drawn by matplotlib in the size of the
 * view (sharp on high-resolution screens); after a resize of the view they are
 * drawn again in the new size (until then the current image is scaled).
 */
public final class PythonPlotView {

	private static final String VIEW_ID = "com.eco.bio7.custom_controls";
	private static final String VIEW_NAME = "Python Plot";
	private static final int MIN_SIZE = 50;
	/* The delay of the new drawing after a resize (milliseconds) */
	private static final int RESIZE_DELAY = 250;

	/* The last image of each figure (pixels, size, scale) */
	private record Frame(int figure, byte[] rgba, int width, int height, double scale) {
	}

	private static final Map<Integer, Frame> frames = new ConcurrentHashMap<>();
	private static volatile int[] open = new int[0];
	private static volatile int current;

	private static Composite root;
	private static Canvas canvas;
	private static Label figureLabel;
	private static Image image;
	private static Frame imageFrame;
	private static int[] requested = new int[] { 0, 0 };
	private static Runnable resizeTask;
	private static Image icon;

	private PythonPlotView() {
	}

	/** The listener of the session: the images of matplotlib. */
	static PyPlotListener listener() {
		return new PyPlotListener() {
			@Override
			public void figure(int figure, byte[] rgba, int width, int height, double scale) {
				frames.put(figure, new Frame(figure, rgba, width, height, scale));
				current = figure;
				Display.getDefault().asyncExec(PythonPlotView::show);
			}

			@Override
			public void figuresChanged(int[] figures) {
				open = figures;
				frames.keySet().removeIf(k -> Arrays.stream(figures).noneMatch(f -> f == k));
				if (Arrays.stream(figures).noneMatch(f -> f == current)) {
					current = figures.length > 0 ? figures[figures.length - 1] : 0;
				}
				Display.getDefault().asyncExec(PythonPlotView::update);
			}
		};
	}

	/* ---------------- The preferences (Preferences Python Plot) ---------------- */

	static final String FIT = "PY_PLOT_FIT";
	static final String HIDPI = "PY_PLOT_HIDPI";
	static final String STYLE = "PY_PLOT_STYLE";
	static final String FONT_SIZE = "PY_PLOT_FONT_SIZE";
	static final String SAVE_DPI = "PY_PLOT_SAVE_DPI";
	private static boolean listening;

	private static org.eclipse.jface.preference.IPreferenceStore store() {
		return com.eco.bio7.Bio7Plugin.getDefault().getPreferenceStore();
	}

	/**
	 * Sends the preferences of the plot view to Python (at the start and after a
	 * change: the open figures are drawn again, the style applies to new figures).
	 */
	static void applyOptions(PySession session) {
		if (!listening) {
			listening = true;
			store().addPropertyChangeListener(e -> {
				if (e.getProperty().startsWith("PY_PLOT_") && !e.getProperty().equals(SAVE_DPI)) {
					PySession s = EmbeddedPython.session();
					if (s != null) {
						Thread t = new Thread(() -> applyOptions(s), "Python plot preferences");
						t.setDaemon(true);
						t.start();
					}
					Display.getDefault().asyncExec(PythonPlotView::update);
				}
			});
		}
		org.eclipse.jface.preference.IPreferenceStore s = store();
		String style = s.getString(STYLE).strip().replace("\\", "\\\\").replace("'", "\\'");
		String code = "import sys\nsys.modules['_bio7']._set_plot_options({'fit': " + (s.getBoolean(FIT) ? "True" : "False") + ", 'hidpi': "
				+ (s.getBoolean(HIDPI) ? "True" : "False") + ", 'style': '" + style + "', 'font_size': " + Math.max(0, s.getInt(FONT_SIZE)) + "})";
		try {
			session.exec(code);
		} catch (Exception e) {
			EmbeddedPython.print("The plot preferences could not be applied: " + e.getMessage() + "\n", true);
		}
	}

	/** The size for the first plots (UI thread): the size of an open view, else a default. */
	static void initSize(PySession session) {
		double scale = scale();
		int w = 640, h = 480;
		if (isOpen()) {
			Rectangle area = canvas.getClientArea();
			w = Math.max(area.width, MIN_SIZE);
			h = Math.max(area.height, MIN_SIZE);
		}
		requested = new int[] { w, h };
		session.resizePlot(0, w, h, scale);
	}

	/* The device scale of the screen (pixels per point) */
	private static double scale() {
		Display display = Display.getDefault();
		int zoom = display.getPrimaryMonitor() != null ? display.getPrimaryMonitor().getZoom() : 100;
		return Math.max(1.0, zoom / 100.0);
	}

	private static boolean isOpen() {
		return canvas != null && !canvas.isDisposed();
	}

	/* A new image (UI thread): the view is opened if necessary */
	private static void show() {
		if (!isOpen()) {
			open();
		}
		update();
		showInPerspective();
	}

	/* The ImageJ-Canvas view (its folder takes the plot in the Image Analysis perspective) */
	private static final String IMAGEJ_CANVAS = "com.eco.bio7.imagej";

	/*
	 * The place of the view (new in a perspective): a tab in the folder of the
	 * ImageJ-Canvas view, else of the table, else beside the console
	 */
	private static void place() {
		String id = VIEW_ID + ":" + VIEW_NAME;
		if (!PythonViews.dockInto(id, IMAGEJ_CANVAS) && !PythonViews.dockInto(id, com.eco.bio7.rbridge.RTable.ID)) {
			PlotViews.dock(VIEW_ID, VIEW_NAME);
		}
	}

	/* Shows the view in the current perspective (placed if it is new there) */
	private static void showInPerspective() {
		try {
			org.eclipse.ui.IWorkbenchPage page = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page == null) {
				return;
			}
			org.eclipse.ui.IViewReference ref = page.findViewReference(VIEW_ID, VIEW_NAME);
			org.eclipse.ui.IViewPart view = ref != null ? ref.getView(false) : null;
			if (view != null && page.isPartVisible(view)) {
				return;
			}
			page.showView(VIEW_ID, VIEW_NAME, org.eclipse.ui.IWorkbenchPage.VIEW_VISIBLE);
			if (ref == null) {
				place();
			}
		} catch (Exception e) {
			/* Not shown */
		}
	}

	/* Creates the content of the view */
	private static void open() {
		Composite parent;
		try {
			parent = new CustomView().getComposite(VIEW_NAME, icon());
		} catch (RuntimeException e) {
			return;
		}
		if (parent == null || parent.isDisposed()) {
			return;
		}
		place();
		root = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		root.setLayout(layout);
		canvas = new Canvas(root, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		canvas.addListener(SWT.Paint, e -> paint(e.gc));
		canvas.addListener(SWT.Resize, _ -> resized());
		/*
		 * The keys of the fullscreen mode (F2 to F5, Esc): on macOS a canvas only takes
		 * the keyboard focus (a click) if it has a key listener
		 */
		canvas.addListener(SWT.KeyDown, _ -> {
		});
		canvas.addListener(SWT.MouseDown, _ -> canvas.setFocus());

		Composite bar = new Composite(root, SWT.NONE);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout barLayout = new GridLayout(8, false);
		barLayout.marginHeight = 2;
		bar.setLayout(barLayout);
		TriangleButton.create(bar, true, "Previous figure", () -> step(-1)).setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		TriangleButton.create(bar, false, "Next figure", () -> step(1)).setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		figureLabel = new Label(bar, SWT.NONE);
		figureLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button(bar, "Save...", "Save the figure (PNG, SVG or PDF; matplotlib draws it again)", PythonPlotView::save);
		button(bar, "Copy", "Copy the figure as image", PythonPlotView::copy);
		button(bar, "ImageJ", "Open the figure as ImageJ image with a chosen resolution (e.g., 300 dpi for a publication)", PythonPlotView::toImageJ);
		button(bar, "Close", "Close the figure (plt.close)", PythonPlotView::closeFigure);
		new Label(bar, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		root.addListener(SWT.Dispose, _ -> {
			if (image != null) {
				image.dispose();
				image = null;
				imageFrame = null;
			}
		});
		parent.layout(true, true);
	}

	private static Image icon() {
		if (icon == null || icon.isDisposed()) {
			var descriptor = com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/views/pythonaction.svg");
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

	/* Shows the image of the current figure and the label */
	private static void update() {
		if (!isOpen()) {
			return;
		}
		Frame f = frames.get(current);
		if (f != imageFrame) {
			if (image != null) {
				image.dispose();
				image = null;
			}
			imageFrame = f;
			if (f != null) {
				image = toImage(f);
			}
		}
		int[] figs = open;
		int at = -1;
		for (int i = 0; i < figs.length; i++) {
			if (figs[i] == current) {
				at = i;
			}
		}
		figureLabel.setText(at < 0 ? "" : "Figure " + current + "  (" + (at + 1) + "/" + figs.length + ")");
		figureLabel.getParent().layout(true);
		canvas.redraw();
	}

	/* An SWT image of the pixels: drawn sharp at the zoom of the screen */
	private static Image toImage(Frame f) {
		ImageData data = new ImageData(f.width, f.height, 24, new PaletteData(0xff0000, 0x00ff00, 0x0000ff));
		byte[] alpha = new byte[f.width * f.height];
		int[] line = new int[f.width];
		for (int y = 0; y < f.height; y++) {
			int o = y * f.width * 4;
			for (int x = 0; x < f.width; x++) {
				int p = o + 4 * x;
				line[x] = (f.rgba[p] & 0xff) << 16 | (f.rgba[p + 1] & 0xff) << 8 | (f.rgba[p + 2] & 0xff);
				alpha[y * f.width + x] = f.rgba[p + 3];
			}
			data.setPixels(0, y, f.width, line, 0);
		}
		data.alphaData = alpha;
		int baseZoom = (int) Math.round(f.scale * 100);
		ImageDataProvider provider = zoom -> {
			if (zoom == baseZoom) {
				return data;
			}
			int w = Math.max(1, f.width * zoom / baseZoom), h = Math.max(1, f.height * zoom / baseZoom);
			return data.scaledTo(w, h);
		};
		return new Image(Display.getDefault(), provider);
	}

	private static void paint(GC gc) {
		Rectangle area = canvas.getClientArea();
		gc.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(area);
		if (image == null || image.isDisposed()) {
			return;
		}
		Rectangle b = image.getBounds();
		if (!store().getBoolean(FIT)) {
			/* The size of the figure (figsize): centered, made smaller if it doesn't fit */
			double f = Math.min(1.0, Math.min((double) area.width / b.width, (double) area.height / b.height));
			int w = (int) Math.round(b.width * f), h = (int) Math.round(b.height * f);
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(image, 0, 0, b.width, b.height, (area.width - w) / 2, (area.height - h) / 2, w, h);
		} else if (b.width == area.width && b.height == area.height) {
			gc.drawImage(image, 0, 0);
		} else {
			/* Until matplotlib has drawn the figure in the new size */
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(image, 0, 0, b.width, b.height, 0, 0, area.width, area.height);
		}
	}

	/* The view was resized: matplotlib draws the figure again (after a short delay) */
	private static void resized() {
		Rectangle area = canvas.getClientArea();
		if (area.width < MIN_SIZE || area.height < MIN_SIZE) {
			return;
		}
		if (requested[0] == area.width && requested[1] == area.height) {
			return;
		}
		Display display = canvas.getDisplay();
		if (resizeTask != null) {
			display.timerExec(-1, resizeTask);
		}
		resizeTask = () -> {
			resizeTask = null;
			if (!isOpen()) {
				return;
			}
			Rectangle a = canvas.getClientArea();
			PySession s = EmbeddedPython.session();
			if (s == null || a.width < MIN_SIZE || a.height < MIN_SIZE) {
				return;
			}
			requested = new int[] { a.width, a.height };
			s.resizePlot(current, a.width, a.height, scale());
		};
		display.timerExec(RESIZE_DELAY, resizeTask);
	}

	/* ---------------- Actions ---------------- */

	private static void step(int direction) {
		int[] figs = open;
		PySession s = EmbeddedPython.session();
		if (figs.length == 0 || s == null) {
			return;
		}
		int at = 0;
		for (int i = 0; i < figs.length; i++) {
			if (figs[i] == current) {
				at = i;
			}
		}
		int next = figs[Math.floorMod(at + direction, figs.length)];
		current = next;
		Frame f = frames.get(next);
		Rectangle area = canvas.getClientArea();
		if (f != null && (int) Math.round(f.width / f.scale) == area.width && (int) Math.round(f.height / f.scale) == area.height) {
			update();
		} else {
			/* Drawn again in the current size */
			s.resizePlot(next, Math.max(area.width, MIN_SIZE), Math.max(area.height, MIN_SIZE), scale());
		}
	}

	private static void save() {
		PySession s = EmbeddedPython.session();
		if (s == null || current == 0) {
			return;
		}
		FileDialog dialog = new FileDialog(canvas.getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "*.png", "*.svg", "*.pdf" });
		dialog.setFilterNames(new String[] { "PNG image", "SVG graphics", "PDF document" });
		dialog.setFileName("figure" + current + ".png");
		dialog.setOverwrite(true);
		String file = dialog.open();
		if (file == null) {
			return;
		}
		String lower = file.toLowerCase(Locale.ROOT);
		String format = lower.endsWith(".svg") ? "svg" : lower.endsWith(".pdf") ? "pdf" : "png";
		String path = format.equals("png") && !lower.endsWith(".png") ? file + ".png" : file;
		int figure = current;
		Thread t = new Thread(() -> {
			try {
				/* The resolution of the preferences (0: the resolution of the figure) */
				s.saveFigure(figure, Path.of(path), format, format.equals("png") ? Math.max(0, store().getInt(SAVE_DPI)) : 0);
			} catch (Exception e) {
				EmbeddedPython.print("The figure could not be saved: " + e.getMessage() + "\n", true);
			}
		}, "Python save figure");
		t.setDaemon(true);
		t.start();
	}

	private static void copy() {
		if (image == null || image.isDisposed()) {
			return;
		}
		Clipboard clipboard = new Clipboard(canvas.getDisplay());
		try {
			clipboard.setContents(new Object[] { image.getImageData(100) }, new Transfer[] { ImageTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	/*
	 * The figure in ImageJ with a chosen resolution: matplotlib draws it again
	 * (savefig with the dpi, the same size in inches), ImageJ opens it calibrated
	 */
	private static void toImageJ() {
		Frame f = imageFrame;
		PySession s = EmbeddedPython.session();
		if (f == null || s == null) {
			return;
		}
		int w = (int) Math.round(f.width / f.scale), h = (int) Math.round(f.height / f.scale);
		int dpi = com.eco.bio7.rbridge.plot.PlotResolutionDialog.ask(canvas.getShell(), w, h);
		if (dpi <= 0) {
			return;
		}
		int figure = f.figure;
		Thread t = new Thread(() -> {
			try {
				java.io.File file = java.io.File.createTempFile("bio7figure", ".png");
				s.saveFigure(figure, file.toPath(), "png", dpi);
				com.eco.bio7.rbridge.plot.PlotResolutionDialog.openInImageJ(file, "Python Figure " + figure, dpi);
			} catch (Exception e) {
				EmbeddedPython.print("The figure could not be transferred: " + e.getMessage() + "\n", true);
			}
		}, "Python figure to ImageJ");
		t.setDaemon(true);
		t.start();
	}

	private static void closeFigure() {
		PySession s = EmbeddedPython.session();
		if (s != null && current != 0) {
			s.closeFigure(current);
		}
	}
}
