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
package com.eco.bio7.simulation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntUnaryOperator;

import ij.ImagePlus;
import ij.process.ColorProcessor;

/**
 * The display of a grid in an ImageJ image (in the ImageJ-Canvas view): each
 * cell is a square of pixels with the color of the cell. At most one update
 * waits for the user interface (the last state is always displayed).
 */
final class GridDisplay {

	private final String title;
	private ImagePlus image;
	private final AtomicBoolean pending = new AtomicBoolean();
	private volatile Runnable again;
	/* The grid size and the scale of the shown image (the scale is kept for the same grid) */
	private int shownW, shownH, shownScale;

	/* The image shown last for a title (of an earlier model, e.g., before a script was run again): replaced (thread of the user interface) */
	private static final java.util.Map<String, ImagePlus> SHOWN = new java.util.HashMap<>();

	GridDisplay(String title) {
		this.title = title;
	}

	/** Closes the images of the simulations (e.g., of the model before a new model; in the thread of the user interface). */
	static void closeAll() {
		for (ImagePlus imp : new java.util.ArrayList<>(SHOWN.values())) {
			if (imp.getWindow() != null && !imp.getWindow().isClosed()) {
				imp.changes = false;
				imp.getWindow().close();
			}
		}
		SHOWN.clear();
	}

	/** Opens the ImageJ-Canvas view (in the thread of the user interface). */
	static void prepare() {
		if (Simulation.hasDisplay()) {
			com.eco.bio7.collection.Work.openView("com.eco.bio7.imagej");
		}
	}

	/**
	 * Displays the grid.
	 *
	 * @param w     the width (cells)
	 * @param h     the height (cells)
	 * @param color the RGB color of the cell with the index y * w + x
	 * @param redo  the call of the display (repeated if an update is running)
	 */
	void show(int w, int h, IntUnaryOperator color, Runnable redo) {
		show(w, h, color, redo, null);
	}

	/** Displays the grid and the shapes of the model (drawLine, drawImage, ... in cells). */
	void show(int w, int h, IntUnaryOperator color, Runnable redo, Simulation model) {
		if (w <= 0 || h <= 0) {
			return;
		}
		if (model != null) {
			inputModel = model;
		}
		java.util.List<Shape> drawing = model == null ? java.util.List.of() : model.getDrawing();
		if (!pending.compareAndSet(false, true)) {
			again = redo;
			return;
		}
		again = null;
		/* With shapes and images: a finer image (at least about 800 pixels) */
		int wanted = drawing.isEmpty() ? Math.max(1, Math.min(16, 480 / Math.max(w, h))) : Math.max(1, Math.min(24, (int) Math.ceil(800.0 / Math.max(w, h))));
		/*
		 * The scale is not reduced for the same grid (e.g., a step without shapes):
		 * else the image gets another size and its window is replaced (a new tab
		 * flashes), the shapes are drawn in some steps only
		 */
		if (w == shownW && h == shownH && shownScale > wanted) {
			wanted = shownScale;
		}
		shownW = w;
		shownH = h;
		shownScale = wanted;
		final int scale = wanted;
		final int iw = w * scale, ih = h * scale;
		final int[] pixels = new int[iw * ih];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int rgb = color.applyAsInt(y * w + x);
				for (int yy = 0; yy < scale; yy++) {
					int row = (y * scale + yy) * iw + x * scale;
					for (int xx = 0; xx < scale; xx++) {
						pixels[row + xx] = rgb;
					}
				}
			}
		}
		if (!drawing.isEmpty()) {
			java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(iw, ih, java.awt.image.BufferedImage.TYPE_INT_RGB);
			bi.setRGB(0, 0, iw, ih, pixels, 0, iw);
			java.awt.Graphics2D g = bi.createGraphics();
			Overlay.paint(g, drawing, scale, model.getImageFiles());
			g.dispose();
			bi.getRGB(0, 0, iw, ih, pixels, 0, iw);
		}
		Simulation.ui(() -> {
			try {
				boolean closed = image == null || (image.getWindow() != null && image.getWindow().isClosed());
				if (closed || image.getWidth() != iw || image.getHeight() != ih) {
					if (image != null && !closed && image.getWindow() != null) {
						image.getWindow().close();
					}
					image = new ImagePlus(title, new ColorProcessor(iw, ih, pixels));
					/* The image of an earlier model with the same name (a script run again, a class compiled again) */
					ImagePlus earlier = SHOWN.put(title, image);
					if (earlier != null && earlier != image && earlier.getWindow() != null && !earlier.getWindow().isClosed()) {
						earlier.getWindow().close();
					}
					image.show();
				} else {
					image.setProcessor(new ColorProcessor(iw, ih, pixels));
					image.updateAndDraw();
				}
				listen(image);
			} finally {
				pending.set(false);
				Runnable r = again;
				again = null;
				if (r != null) {
					/*
					 * Not in the thread of the user interface (it stays free, e.g., for the speed
					 * slider). With the lock of the model (as its step and its actions): the
					 * display doesn't read a step which is computed at the same time (half
					 * changed values and cleared shapes: a flickering image)
					 */
					Simulation.redisplay(model == null ? r : () -> {
						synchronized (model) {
							r.run();
						}
					});
				}
			}
		});
	}

	/** The RGB colors of a ramp (e.g., "#000000", "#ffffff"; an alpha of #rrggbbaa is ignored in ImageJ). */
	static int[][] ramp(String[] colors) {
		int[][] ramp = new int[colors.length][];
		for (int i = 0; i < colors.length; i++) {
			int c = Simulation.rgb(colors[i]);
			ramp[i] = new int[] { (c >> 16) & 255, (c >> 8) & 255, c & 255 };
		}
		return ramp;
	}

	/** The color of the value on the ramp from lo to lo + span (the same colors as the HTML export). */
	static int rampColor(int[][] ramp, double value, double lo, double span) {
		int n = ramp.length;
		double t = (value - lo) / span;
		if (!(t >= 0)) {
			t = 0;
		}
		if (t > 1) {
			t = 1;
		}
		double p = t * (n - 1);
		int a = Math.min(n - 2, (int) Math.floor(p));
		double w = p - a;
		int r = (int) (ramp[a][0] + w * (ramp[a + 1][0] - ramp[a][0]));
		int g = (int) (ramp[a][1] + w * (ramp[a + 1][1] - ramp[a][1]));
		int b = (int) (ramp[a][2] + w * (ramp[a + 1][2] - ramp[a][2]));
		return (r << 16) | (g << 8) | b;
	}

	ImagePlus getImage() {
		return image;
	}

	/* ----- The input of the image: clicks, drags and keys for the model ----- */

	/* The model which gets the input */
	private volatile Simulation inputModel;
	/* The canvas with the listeners, its key listeners of ImageJ (removed while the model gets the keys) */
	private org.eclipse.swt.widgets.Control listened;
	private org.eclipse.swt.widgets.Listener[] imageJKeys;
	private boolean imageJMouseOff;

	/* The listeners on the canvas of the image (a new image: a new canvas; in the thread of the user interface) */
	private void listen(ImagePlus imp) {
		ij.gui.ImageCanvas canvas = imp.getCanvas();
		Simulation model = inputModel;
		if (canvas == null || canvas.isDisposed() || model == null) {
			return;
		}
		if (canvas != listened) {
			listened = canvas;
			imageJKeys = null;
			imageJMouseOff = false;
			Input in = new Input(canvas);
			canvas.addListener(org.eclipse.swt.SWT.MouseDown, in);
			canvas.addListener(org.eclipse.swt.SWT.MouseMove, in);
			canvas.addListener(org.eclipse.swt.SWT.MouseUp, in);
			canvas.addListener(org.eclipse.swt.SWT.KeyDown, in);
		}
		/*
		 * A model with click() or drag(): the mouse is not used by the tools of ImageJ
		 * in this image (no selections while painting; the wheel still zooms)
		 */
		if (!imageJMouseOff && (model.handles("click") || model.handles("drag"))) {
			imageJMouseOff = true;
			for (int type : new int[] { org.eclipse.swt.SWT.MouseDown, org.eclipse.swt.SWT.MouseUp, org.eclipse.swt.SWT.MouseMove, org.eclipse.swt.SWT.MouseDoubleClick }) {
				for (org.eclipse.swt.widgets.Listener l : canvas.getListeners(type)) {
					if (!(l instanceof Input)) {
						canvas.removeListener(type, l);
					}
				}
			}
		}
		/* A model with key(): the keys are not shortcuts of ImageJ in this image */
		if (imageJKeys == null && model.handles("key")) {
			imageJKeys = canvas.getListeners(org.eclipse.swt.SWT.KeyDown);
			for (org.eclipse.swt.widgets.Listener k : imageJKeys) {
				if (!(k instanceof Input)) {
					canvas.removeListener(org.eclipse.swt.SWT.KeyDown, k);
				}
			}
		}
	}

	/* The position of the mouse in cells of the grid (null: outside) */
	private double[] cell(ij.gui.ImageCanvas canvas, int mx, int my) {
		ImagePlus imp = image;
		int w = shownW, h = shownH;
		if (imp == null || w <= 0 || h <= 0) {
			return null;
		}
		double scale = imp.getWidth() / (double) w;
		double x = canvas.offScreenXD(mx) / scale, y = canvas.offScreenYD(my) / scale;
		return x >= 0 && y >= 0 && x < w && y < h ? new double[] { x, y } : null;
	}

	/* Clicks (without a drag), drags with the left button (each new cell), keys (in the thread of the user interface) */
	private final class Input implements org.eclipse.swt.widgets.Listener {
		private final ij.gui.ImageCanvas canvas;
		private double[] pressed;
		private boolean dragging;
		private int lastX, lastY, pressX, pressY;

		Input(ij.gui.ImageCanvas canvas) {
			this.canvas = canvas;
		}

		@Override
		public void handleEvent(org.eclipse.swt.widgets.Event e) {
			Simulation m = inputModel;
			if (m == null) {
				return;
			}
			switch (e.type) {
			case org.eclipse.swt.SWT.MouseDown -> {
				pressed = e.button == 1 ? cell(canvas, e.x, e.y) : null;
				pressX = e.x;
				pressY = e.y;
				dragging = false;
			}
			case org.eclipse.swt.SWT.MouseMove -> {
				if (pressed == null || (e.stateMask & org.eclipse.swt.SWT.BUTTON1) == 0 || !m.handles("drag")) {
					return;
				}
				if (!dragging && Math.abs(e.x - pressX) <= 2 && Math.abs(e.y - pressY) <= 2) {
					return;
				}
				double[] c = cell(canvas, e.x, e.y);
				if (!dragging) {
					/* The first cell: where the drag started */
					dragging = true;
					lastX = (int) pressed[0];
					lastY = (int) pressed[1];
					m.input("drag", pressed[0], pressed[1], null, false);
				}
				if (c != null && ((int) c[0] != lastX || (int) c[1] != lastY)) {
					lastX = (int) c[0];
					lastY = (int) c[1];
					m.input("drag", c[0], c[1], null, false);
				}
			}
			case org.eclipse.swt.SWT.MouseUp -> {
				if (pressed != null && !dragging && e.button == 1) {
					double[] c = cell(canvas, e.x, e.y);
					if (c != null) {
						m.input("click", c[0], c[1], null, false);
					}
				}
				pressed = null;
				dragging = false;
			}
			case org.eclipse.swt.SWT.KeyDown -> {
				if ((e.stateMask & (org.eclipse.swt.SWT.CTRL | org.eclipse.swt.SWT.COMMAND)) != 0 || !m.handles("key")) {
					return;
				}
				String key = Simulation.keyName(e.character, special(e.keyCode));
				if (key != null) {
					m.input("key", 0, 0, key, false);
					e.doit = false;
				}
			}
			default -> {
			}
			}
		}
	}

	/* The names of the special keys (as JavaScript), "" for modifier keys */
	private static String special(int code) {
		return switch (code) {
		case org.eclipse.swt.SWT.ARROW_LEFT -> "ArrowLeft";
		case org.eclipse.swt.SWT.ARROW_RIGHT -> "ArrowRight";
		case org.eclipse.swt.SWT.ARROW_UP -> "ArrowUp";
		case org.eclipse.swt.SWT.ARROW_DOWN -> "ArrowDown";
		case org.eclipse.swt.SWT.CR, org.eclipse.swt.SWT.KEYPAD_CR -> "Enter";
		case org.eclipse.swt.SWT.ESC -> "Escape";
		case org.eclipse.swt.SWT.BS -> "Backspace";
		case org.eclipse.swt.SWT.DEL -> "Delete";
		case org.eclipse.swt.SWT.TAB -> "Tab";
		case org.eclipse.swt.SWT.SHIFT, org.eclipse.swt.SWT.CTRL, org.eclipse.swt.SWT.ALT, org.eclipse.swt.SWT.COMMAND, org.eclipse.swt.SWT.CAPS_LOCK -> "";
		default -> null;
		};
	}
}
