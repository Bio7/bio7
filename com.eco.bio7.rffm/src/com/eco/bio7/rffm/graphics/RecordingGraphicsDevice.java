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

package com.eco.bio7.rffm.graphics;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An R graphics device which records the drawing operations as a display list
 * (vector graphics): a renderer of any toolkit (e.g., SWT or Swing) draws the
 * list in its own UI thread, sharp at any size. The device itself only needs
 * the JDK: the texts are measured with Java2D (in the thread of R).
 * <p>
 * The pages are handed to a listener while R draws (at most every
 * {@link #setFrameInterval(long) interval}) and when R stops drawing.
 */
public class RecordingGraphicsDevice implements ResizableGraphicsDevice {

	/** A drawing operation (coordinates in device pixels: x from the left, y from the top). */
	public sealed interface Op permits NewPage, Line, Polyline, Polygon, Rect, Circle, Path, Text, Raster, Clip {
	}

	/** A new page filled with gc.fill (white if transparent). */
	public record NewPage(GraphicsContext gc) implements Op {
	}

	public record Line(double x1, double y1, double x2, double y2, GraphicsContext gc) implements Op {
	}

	public record Polyline(double[] x, double[] y, GraphicsContext gc) implements Op {
	}

	/** Filled with gc.fill, outlined with gc.col. */
	public record Polygon(double[] x, double[] y, GraphicsContext gc) implements Op {
	}

	public record Rect(double x0, double y0, double x1, double y1, GraphicsContext gc) implements Op {
	}

	public record Circle(double x, double y, double r, GraphicsContext gc) implements Op {
	}

	/** Several polygons (nper points each), winding: non-zero rule, else even-odd. */
	public record Path(double[] x, double[] y, int[] nper, boolean winding, GraphicsContext gc) implements Op {
	}

	/**
	 * A text at its baseline.
	 *
	 * @param width the width measured for R (a renderer can scale its text to it,
	 *              so the layout of R fits)
	 */
	public record Text(double x, double y, String text, double rot, double hadj, double width, GraphicsContext gc) implements Op {
	}

	/** An image (R's ABGR pixels from the top; x, y: the bottom left corner; height negative if y points down). */
	public record Raster(int[] abgr, int w, int h, double x, double y, double width, double height, double rot, boolean interpolate) implements Op {
	}

	/** The clipping rectangle for the following operations. */
	public record Clip(double x0, double x1, double y0, double y1) implements Op {
	}

	/**
	 * A page: the operations since the last new page.
	 *
	 * @param version increases with each published page
	 */
	public record Page(List<Op> ops, int width, int height, double dpi, long version) {
	}

	private final double dpi;
	private final FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
	private final List<Op> ops = new ArrayList<>();
	private volatile Consumer<Page> pageListener;
	private volatile Runnable closeListener;
	private volatile Supplier<double[]> locatorHandler;
	private long frameInterval = 100;
	private int width;
	private int height;
	private boolean dirty;
	private int hold;
	private long lastPage;
	private long version;
	private volatile boolean closed;

	/**
	 * @param width  the width in pixels
	 * @param height the height in pixels
	 * @param dpi    the resolution (e.g., 96: lwd 1 is one pixel)
	 */
	public RecordingGraphicsDevice(int width, int height, double dpi) {
		this.width = Math.max(1, width);
		this.height = Math.max(1, height);
		this.dpi = dpi;
	}

	/** Receives the pages (in the thread of R: hand them to the UI asynchronously). */
	public void setPageListener(Consumer<Page> listener) {
		this.pageListener = listener;
	}

	/** Called when R closes the device (dev.off()). */
	public void setCloseListener(Runnable listener) {
		this.closeListener = listener;
	}

	/** Enables locator() and identify() (see Java2DGraphicsDevice.setLocatorHandler); set it before the device is opened. */
	public void setLocatorHandler(Supplier<double[]> handler) {
		this.locatorHandler = handler;
	}

	/** The minimal time between two pages while R draws (milliseconds). */
	public void setFrameInterval(long millis) {
		this.frameInterval = Math.max(0, millis);
	}

	public double dpi() {
		return dpi;
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public synchronized void resize(int width, int height) {
		this.width = Math.max(1, width);
		this.height = Math.max(1, height);
	}

	@Override
	public synchronized double width() {
		return width;
	}

	@Override
	public synchronized double height() {
		return height;
	}

	/** The current page (a copy of the list). */
	public synchronized Page snapshot() {
		return new Page(List.copyOf(ops), width, height, dpi, version);
	}

	/* ---------------- Recording ---------------- */

	private synchronized void add(Op op) {
		ops.add(op);
		dirty = true;
	}

	@Override
	public void newPage(GraphicsContext gc) {
		synchronized (this) {
			ops.clear();
		}
		add(new NewPage(gc));
		changed();
	}

	@Override
	public void line(double x1, double y1, double x2, double y2, GraphicsContext gc) {
		add(new Line(x1, y1, x2, y2, gc));
		changed();
	}

	@Override
	public void polyline(double[] x, double[] y, GraphicsContext gc) {
		add(new Polyline(x, y, gc));
		changed();
	}

	@Override
	public void polygon(double[] x, double[] y, GraphicsContext gc) {
		add(new Polygon(x, y, gc));
		changed();
	}

	@Override
	public void rect(double x0, double y0, double x1, double y1, GraphicsContext gc) {
		add(new Rect(x0, y0, x1, y1, gc));
		changed();
	}

	@Override
	public void circle(double x, double y, double r, GraphicsContext gc) {
		add(new Circle(x, y, r, gc));
		changed();
	}

	@Override
	public void path(double[] x, double[] y, int[] nper, boolean winding, GraphicsContext gc) {
		add(new Path(x, y, nper, winding, gc));
		changed();
	}

	@Override
	public void text(double x, double y, String text, double rot, double hadj, GraphicsContext gc) {
		if (text == null || text.isEmpty()) {
			return;
		}
		add(new Text(x, y, text, rot, hadj, strWidth(text, gc), gc));
		changed();
	}

	@Override
	public void raster(int[] abgr, int w, int h, double x, double y, double width, double height, double rot, boolean interpolate,
			GraphicsContext gc) {
		add(new Raster(abgr, w, h, x, y, width, height, rot, interpolate));
		changed();
	}

	@Override
	public void clip(double x0, double x1, double y0, double y1) {
		add(new Clip(x0, x1, y0, y1));
	}

	/* ---------------- Text metrics (Java2D, any thread) ---------------- */

	@Override
	public double strWidth(String text, GraphicsContext gc) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		return font(gc, dpi).createGlyphVector(frc, text).getLogicalBounds().getWidth();
	}

	@Override
	public double[] metricInfo(int codePoint, GraphicsContext gc) {
		Font font = font(gc, dpi);
		String s = new String(Character.toChars(codePoint == 0 ? 'M' : codePoint));
		GlyphVector gv = font.createGlyphVector(frc, s);
		Rectangle2D bounds = gv.getVisualBounds();
		double ascent = Math.max(0, -bounds.getY());
		double descent = Math.max(0, bounds.getY() + bounds.getHeight());
		double w = gv.getGlyphMetrics(0).getAdvance();
		if (codePoint == 0) {
			var lm = font.getLineMetrics(s, frc);
			ascent = Math.max(ascent, lm.getAscent());
			descent = Math.max(descent, lm.getDescent());
		}
		return new double[] { ascent, descent, w };
	}

	/**
	 * The Java2D font of a graphics context (the same as for the metrics); the
	 * size in pixels (points * dpi / 72).
	 */
	public static Font font(GraphicsContext gc, double dpi) {
		String family = switch (gc.fontfamily() == null ? "" : gc.fontfamily()) {
		case "", "sans", "Helvetica", "Arial" -> Font.SANS_SERIF;
		case "serif", "Times" -> Font.SERIF;
		case "mono", "Courier" -> Font.MONOSPACED;
		default -> gc.fontfamily();
		};
		int style = switch (gc.fontface()) {
		case 2 -> Font.BOLD;
		case 3 -> Font.ITALIC;
		case 4 -> Font.BOLD | Font.ITALIC;
		default -> Font.PLAIN;
		};
		if (gc.fontface() == 5) {
			family = Font.SERIF;
		}
		float size = (float) Math.max(1, gc.fontSize() * dpi / 72.0);
		return new Font(family, style, 1).deriveFont(size);
	}

	/* ---------------- Pages ---------------- */

	@Override
	public void mode(int mode) {
		if (mode == 0) {
			flush();
		}
	}

	@Override
	public int holdflush(int level) {
		int current;
		synchronized (this) {
			hold = Math.max(0, hold + level);
			current = hold;
		}
		if (current == 0) {
			flush();
		}
		return current;
	}

	@Override
	public boolean supportsLocator() {
		return locatorHandler != null;
	}

	@Override
	public double[] locator() {
		var handler = locatorHandler;
		if (handler == null) {
			return null;
		}
		/* The user sees the current plot while R waits for the click */
		flush();
		return handler.get();
	}

	@Override
	public void close() {
		closed = true;
		flush();
		Runnable l = closeListener;
		if (l != null) {
			l.run();
		}
	}

	private void changed() {
		boolean publish;
		synchronized (this) {
			publish = hold == 0 && System.currentTimeMillis() - lastPage >= frameInterval;
		}
		if (publish) {
			emit();
		}
	}

	/** Hands the page to the listener (if changed and not held). */
	public void flush() {
		synchronized (this) {
			if (!dirty || hold > 0 && !closed) {
				return;
			}
		}
		emit();
	}

	private void emit() {
		Consumer<Page> l = pageListener;
		Page page;
		synchronized (this) {
			dirty = false;
			lastPage = System.currentTimeMillis();
			version++;
			if (l == null) {
				return;
			}
			page = snapshot();
		}
		l.accept(page);
	}
}
