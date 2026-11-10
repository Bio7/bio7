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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * An R graphics device which draws with Java2D into an image (no UI toolkit:
 * usable from SWT, Swing or without a display). The frames are handed to a
 * listener while R draws (at most every {@link #setFrameInterval(long)
 * interval}) and when R stops drawing, like a screen device of R.
 * <p>
 * The device size is in logical pixels; the image has the size times the
 * scale (e.g., 2 for a HiDPI display) for a crisp display.
 */
public class Java2DGraphicsDevice implements ResizableGraphicsDevice {

	/**
	 * A drawn frame (a copy).
	 *
	 * @param argb    the pixels (ARGB, opaque), width * height
	 * @param width   the width of the image in pixels (device width * scale)
	 * @param height  the height of the image
	 * @param scale   the scale of the image (image pixels per device pixel)
	 * @param version increases with each frame
	 */
	public record Frame(int[] argb, int width, int height, double scale, long version) {

		/** The frame as an image. */
		public BufferedImage toImage() {
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			img.setRGB(0, 0, width, height, argb, 0, width);
			return img;
		}
	}

	private final double dpi;
	private final double scale;
	private volatile Consumer<Frame> frameListener;
	private volatile Runnable closeListener;
	private volatile java.util.function.Supplier<double[]> locatorHandler;
	private long frameInterval = 100;

	private int width;
	private int height;
	private BufferedImage image;
	private Graphics2D g;
	private boolean dirty;
	private int hold;
	private long lastFrame;
	private long version;
	private volatile boolean closed;

	/**
	 * @param width  the width in pixels
	 * @param height the height in pixels
	 * @param dpi    the resolution (e.g., 96: lwd 1 is one pixel)
	 * @param scale  the image pixels per device pixel (1, or 2 for HiDPI)
	 */
	public Java2DGraphicsDevice(int width, int height, double dpi, double scale) {
		this.dpi = dpi;
		this.scale = scale <= 0 ? 1 : scale;
		allocate(width, height);
	}

	/** Receives the frames (in the thread of R: copy or hand them on quickly). */
	public void setFrameListener(Consumer<Frame> listener) {
		this.frameListener = listener;
	}

	/** Called when R closes the device (dev.off()). */
	public void setCloseListener(Runnable listener) {
		this.closeListener = listener;
	}

	/**
	 * Enables locator() and identify(): the handler waits for a click (in the
	 * thread of R) and returns the position in device units, or null to stop. Set
	 * it before the device is opened in R.
	 */
	public void setLocatorHandler(java.util.function.Supplier<double[]> handler) {
		this.locatorHandler = handler;
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
		flush(true);
		return handler.get();
	}

	/** The minimal time between two frames while R draws (milliseconds). */
	public void setFrameInterval(long millis) {
		this.frameInterval = Math.max(0, millis);
	}

	public double dpi() {
		return dpi;
	}

	public double scale() {
		return scale;
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * Changes the size (in the thread of R, see RSession.resizeDevice which also
	 * replays the plot).
	 */
	public synchronized void resize(int width, int height) {
		if (width == this.width && height == this.height) {
			return;
		}
		allocate(width, height);
	}

	private void allocate(int w, int h) {
		width = Math.max(1, w);
		height = Math.max(1, h);
		if (g != null) {
			g.dispose();
		}
		image = new BufferedImage((int) Math.ceil(width * scale), (int) Math.ceil(height * scale), BufferedImage.TYPE_INT_RGB);
		g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.scale(scale, scale);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		dirty = true;
	}

	@Override
	public synchronized double width() {
		return width;
	}

	@Override
	public synchronized double height() {
		return height;
	}

	/** The current image as a frame (a copy). */
	public synchronized Frame snapshot() {
		int w = image.getWidth();
		int h = image.getHeight();
		/* The pixels of the image directly (TYPE_INT_RGB): one array copy */
		int[] data = ((java.awt.image.DataBufferInt) image.getRaster().getDataBuffer()).getData();
		return new Frame(data.clone(), w, h, scale, version);
	}

	/* ---------------- Drawing ---------------- */

	@Override
	public synchronized void newPage(GraphicsContext gc) {
		g.setClip(null);
		g.setComposite(AlphaComposite.Src);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.setComposite(AlphaComposite.SrcOver);
		if (!GraphicsContext.isTransparent(gc.fill())) {
			g.setColor(color(gc.fill()));
			g.fillRect(0, 0, width, height);
		}
		changed();
	}

	@Override
	public synchronized void line(double x1, double y1, double x2, double y2, GraphicsContext gc) {
		stroke(new Line2D.Double(x1, y1, x2, y2), gc);
		changed();
	}

	@Override
	public synchronized void polyline(double[] x, double[] y, GraphicsContext gc) {
		stroke(path(x, y, false), gc);
		changed();
	}

	@Override
	public synchronized void polygon(double[] x, double[] y, GraphicsContext gc) {
		Shape s = path(x, y, true);
		fill(s, gc);
		stroke(s, gc);
		changed();
	}

	@Override
	public synchronized void rect(double x0, double y0, double x1, double y1, GraphicsContext gc) {
		Shape s = new Rectangle2D.Double(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0));
		fill(s, gc);
		stroke(s, gc);
		changed();
	}

	@Override
	public synchronized void circle(double x, double y, double r, GraphicsContext gc) {
		Shape s = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
		fill(s, gc);
		stroke(s, gc);
		changed();
	}

	@Override
	public synchronized void path(double[] x, double[] y, int[] nper, boolean winding, GraphicsContext gc) {
		Path2D.Double p = new Path2D.Double(winding ? Path2D.WIND_NON_ZERO : Path2D.WIND_EVEN_ODD);
		int k = 0;
		for (int n : nper) {
			for (int i = 0; i < n; i++, k++) {
				if (i == 0) {
					p.moveTo(x[k], y[k]);
				} else {
					p.lineTo(x[k], y[k]);
				}
			}
			p.closePath();
		}
		fill(p, gc);
		stroke(p, gc);
		changed();
	}

	@Override
	public synchronized void text(double x, double y, String text, double rot, double hadj, GraphicsContext gc) {
		if (text == null || text.isEmpty() || GraphicsContext.isTransparent(gc.col())) {
			return;
		}
		Font font = font(gc);
		AffineTransform saved = g.getTransform();
		g.translate(x, y);
		if (rot != 0) {
			g.rotate(-Math.toRadians(rot));
		}
		GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), text);
		double w = gv.getLogicalBounds().getWidth();
		g.setColor(color(gc.col()));
		g.drawGlyphVector(gv, (float) (-w * hadj), 0f);
		g.setTransform(saved);
		changed();
	}

	@Override
	public synchronized double strWidth(String text, GraphicsContext gc) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		return font(gc).createGlyphVector(frc(), text).getLogicalBounds().getWidth();
	}

	@Override
	public synchronized double[] metricInfo(int codePoint, GraphicsContext gc) {
		Font font = font(gc);
		String s = new String(Character.toChars(codePoint == 0 ? 'M' : codePoint));
		GlyphVector gv = font.createGlyphVector(frc(), s);
		Rectangle2D bounds = gv.getVisualBounds();
		double ascent = Math.max(0, -bounds.getY());
		double descent = Math.max(0, bounds.getY() + bounds.getHeight());
		double w = gv.getGlyphMetrics(0).getAdvance();
		if (codePoint == 0) {
			var lm = font.getLineMetrics(s, frc());
			ascent = Math.max(ascent, lm.getAscent());
			descent = Math.max(descent, lm.getDescent());
		}
		return new double[] { ascent, descent, w };
	}

	@Override
	public synchronized void raster(int[] abgr, int w, int h, double x, double y, double width, double height, double rot, boolean interpolate,
			GraphicsContext gc) {
		if (w <= 0 || h <= 0) {
			return;
		}
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] argb = new int[w * h];
		for (int i = 0; i < argb.length; i++) {
			argb[i] = GraphicsContext.toARGB(abgr[i]);
		}
		img.setRGB(0, 0, w, h, argb, 0, w);
		AffineTransform saved = g.getTransform();
		Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				interpolate ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		/* As the cairo device of R: (x, y) is the bottom left corner, the height is negative if y points down */
		g.translate(x, y);
		g.rotate(-Math.toRadians(rot));
		g.scale(width / w, height / h);
		g.translate(0, h / 2.0);
		g.scale(1, -1);
		g.translate(0, -h / 2.0);
		g.drawImage(img, 0, 0, null);
		g.setTransform(saved);
		if (hint != null) {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
		}
		changed();
	}

	@Override
	public synchronized void clip(double x0, double x1, double y0, double y1) {
		g.setClip(new Rectangle2D.Double(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0)));
	}

	@Override
	public void mode(int mode) {
		if (mode == 0) {
			flush(true);
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
			flush(true);
		}
		return current;
	}

	@Override
	public void close() {
		closed = true;
		flush(true);
		Runnable l = closeListener;
		if (l != null) {
			l.run();
		}
	}

	/* ---------------- Helpers ---------------- */

	private void changed() {
		dirty = true;
		if (hold == 0 && System.currentTimeMillis() - lastFrame >= frameInterval) {
			emit();
		}
	}

	/** Hands the image to the listener (if changed and not held). */
	public void flush(boolean force) {
		synchronized (this) {
			if (!dirty || hold > 0 && !closed) {
				return;
			}
		}
		emit();
	}

	private void emit() {
		Consumer<Frame> l = frameListener;
		Frame f;
		synchronized (this) {
			dirty = false;
			lastFrame = System.currentTimeMillis();
			version++;
			if (l == null) {
				return;
			}
			f = snapshot();
		}
		l.accept(f);
	}

	private FontRenderContext frc() {
		return g.getFontRenderContext();
	}

	private Font font(GraphicsContext gc) {
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
		/* Points to pixels */
		float size = (float) Math.max(1, gc.fontSize() * dpi / 72.0);
		return new Font(family, style, 1).deriveFont(size);
	}

	private static Color color(int rcolor) {
		return new Color(GraphicsContext.toARGB(rcolor), true);
	}

	private void fill(Shape s, GraphicsContext gc) {
		if (!GraphicsContext.isTransparent(gc.fill())) {
			g.setColor(color(gc.fill()));
			g.fill(s);
		}
	}

	private void stroke(Shape s, GraphicsContext gc) {
		if (gc.lty() == GraphicsContext.LTY_BLANK || GraphicsContext.isTransparent(gc.col())) {
			return;
		}
		g.setColor(color(gc.col()));
		g.setStroke(stroke(gc));
		g.draw(s);
	}

	private BasicStroke stroke(GraphicsContext gc) {
		float lw = (float) Math.max(0.01, gc.lwd() * dpi / 96.0);
		int cap = switch (gc.lend()) {
		case 2 -> BasicStroke.CAP_BUTT;
		case 3 -> BasicStroke.CAP_SQUARE;
		default -> BasicStroke.CAP_ROUND;
		};
		int join = switch (gc.ljoin()) {
		case 2 -> BasicStroke.JOIN_MITER;
		case 3 -> BasicStroke.JOIN_BEVEL;
		default -> BasicStroke.JOIN_ROUND;
		};
		float mitre = (float) Math.max(1, gc.lmitre());
		float[] dash = dash(gc.lty(), lw);
		return dash == null ? new BasicStroke(lw, cap, join, mitre) : new BasicStroke(lw, cap, join, mitre, dash, 0f);
	}

	/* The dash pattern of R: 4-bit lengths (dash, gap, ...) in units of the line width */
	private static float[] dash(int lty, float lw) {
		if (lty == 0) {
			return null;
		}
		float[] d = new float[8];
		int n = 0;
		for (int i = 0; i < 8; i++) {
			int len = (lty >>> (4 * i)) & 0xF;
			if (len == 0) {
				break;
			}
			d[n++] = len * Math.max(1f, lw);
		}
		if (n == 0) {
			return null;
		}
		float[] r = new float[n % 2 == 0 ? n : n * 2];
		for (int i = 0; i < r.length; i++) {
			r[i] = d[i % n];
		}
		return r;
	}

	private static Path2D.Double path(double[] x, double[] y, boolean close) {
		Path2D.Double p = new Path2D.Double();
		for (int i = 0; i < x.length; i++) {
			if (i == 0) {
				p.moveTo(x[i], y[i]);
			} else {
				p.lineTo(x[i], y[i]);
			}
		}
		if (close) {
			p.closePath();
		}
		return p;
	}
}
