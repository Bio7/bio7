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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;

import com.eco.bio7.rffm.graphics.GraphicsContext;
import com.eco.bio7.rffm.graphics.RecordingGraphicsDevice;
import com.eco.bio7.rffm.graphics.RecordingGraphicsDevice.Op;

/**
 * Draws a recorded page of R (RecordingGraphicsDevice) with SWT: vector
 * graphics (float paths, sharp at any size and on HiDPI displays). Only in the
 * UI thread.
 */
public final class SwtPlotRenderer {

	private final Device device;
	private final Map<String, Font> fonts = new HashMap<>();
	/* The base transform: the page fitted into the area */
	private Transform base;

	private SwtPlotRenderer(Device device) {
		this.device = device;
	}

	/**
	 * Draws the page into an area: in its size or, while the size changes,
	 * scaled to fit (uniformly, centered) until R has drawn it again.
	 *
	 * @return the scale and the offset (x, y) of the page in the area
	 */
	public static double[] paint(GC gc, RecordingGraphicsDevice.Page page, int areaWidth, int areaHeight) {
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, areaWidth, areaHeight);
		if (page == null || page.width() <= 0 || page.height() <= 0) {
			return new double[] { 1, 0, 0 };
		}
		double scale = Math.min((double) areaWidth / page.width(), (double) areaHeight / page.height());
		if (Math.abs(scale - 1) < 0.005) {
			scale = 1;
		}
		double ox = (areaWidth - page.width() * scale) / 2, oy = (areaHeight - page.height() * scale) / 2;
		if (scale == 1) {
			ox = Math.max(0, ox);
			oy = Math.max(0, oy);
		}
		SwtPlotRenderer r = new SwtPlotRenderer(gc.getDevice());
		try {
			r.draw(gc, page, (float) scale, (float) ox, (float) oy);
		} finally {
			r.dispose();
		}
		return new double[] { scale, ox, oy };
	}

	/** Draws the page into an image of its size (e.g., to copy it). */
	public static Image toImage(Device device, RecordingGraphicsDevice.Page page) {
		Image image = new Image(device, Math.max(1, page.width()), Math.max(1, page.height()));
		GC gc = new GC(image);
		try {
			paint(gc, page, page.width(), page.height());
		} finally {
			gc.dispose();
		}
		return image;
	}

	private void dispose() {
		fonts.values().forEach(Font::dispose);
		if (base != null) {
			base.dispose();
		}
	}

	private void draw(GC gc, RecordingGraphicsDevice.Page page, float scale, float ox, float oy) {
		gc.setAdvanced(true);
		gc.setAntialias(SWT.ON);
		gc.setTextAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		base = new Transform(device);
		base.translate(ox, oy);
		base.scale(scale, scale);
		gc.setTransform(base);
		/* The page area: the clipping of R is inside of it */
		Path pageClip = new Path(device);
		pageClip.addRectangle(0, 0, page.width(), page.height());
		gc.setClipping(pageClip);
		pageClip.dispose();
		for (Op op : page.ops()) {
			try {
				drawOp(gc, op, page);
			} catch (RuntimeException e) {
				/* One wrong operation must not stop the drawing */
			}
		}
		gc.setClipping((org.eclipse.swt.graphics.Rectangle) null);
		gc.setTransform(null);
		gc.setAlpha(255);
	}

	private void drawOp(GC gc, Op op, RecordingGraphicsDevice.Page page) {
		switch (op) {
		case RecordingGraphicsDevice.NewPage p -> {
			gc.setAlpha(255);
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
			gc.fillRectangle(0, 0, page.width(), page.height());
			fill(gc, p.gc().fill(), path -> path.addRectangle(0, 0, page.width(), page.height()), SWT.FILL_WINDING);
		}
		case RecordingGraphicsDevice.Line l -> stroke(gc, l.gc(), path -> {
			path.moveTo((float) l.x1(), (float) l.y1());
			path.lineTo((float) l.x2(), (float) l.y2());
		});
		case RecordingGraphicsDevice.Polyline l -> stroke(gc, l.gc(), path -> lines(path, l.x(), l.y(), false));
		case RecordingGraphicsDevice.Polygon p -> {
			fill(gc, p.gc().fill(), path -> lines(path, p.x(), p.y(), true), SWT.FILL_WINDING);
			stroke(gc, p.gc(), path -> lines(path, p.x(), p.y(), true));
		}
		case RecordingGraphicsDevice.Rect r -> {
			float x = (float) Math.min(r.x0(), r.x1()), y = (float) Math.min(r.y0(), r.y1());
			float w = (float) Math.abs(r.x1() - r.x0()), h = (float) Math.abs(r.y1() - r.y0());
			fill(gc, r.gc().fill(), path -> path.addRectangle(x, y, w, h), SWT.FILL_WINDING);
			stroke(gc, r.gc(), path -> path.addRectangle(x, y, w, h));
		}
		case RecordingGraphicsDevice.Circle c -> {
			float d = (float) (2 * c.r());
			float x = (float) (c.x() - c.r()), y = (float) (c.y() - c.r());
			fill(gc, c.gc().fill(), path -> path.addArc(x, y, d, d, 0, 360), SWT.FILL_WINDING);
			stroke(gc, c.gc(), path -> path.addArc(x, y, d, d, 0, 360));
		}
		case RecordingGraphicsDevice.Path p -> {
			java.util.function.Consumer<Path> shape = path -> {
				int k = 0;
				for (int n : p.nper()) {
					for (int i = 0; i < n; i++, k++) {
						if (i == 0) {
							path.moveTo((float) p.x()[k], (float) p.y()[k]);
						} else {
							path.lineTo((float) p.x()[k], (float) p.y()[k]);
						}
					}
					path.close();
				}
			};
			fill(gc, p.gc().fill(), shape, p.winding() ? SWT.FILL_WINDING : SWT.FILL_EVEN_ODD);
			stroke(gc, p.gc(), shape);
		}
		case RecordingGraphicsDevice.Text t -> text(gc, t, page.dpi());
		case RecordingGraphicsDevice.Raster r -> raster(gc, r);
		case RecordingGraphicsDevice.Clip c -> {
			Path clip = new Path(device);
			clip.addRectangle((float) Math.min(c.x0(), c.x1()), (float) Math.min(c.y0(), c.y1()), (float) Math.abs(c.x1() - c.x0()),
					(float) Math.abs(c.y1() - c.y0()));
			gc.setClipping(clip);
			clip.dispose();
		}
		}
	}

	private static void lines(Path path, double[] x, double[] y, boolean close) {
		for (int i = 0; i < x.length; i++) {
			if (i == 0) {
				path.moveTo((float) x[i], (float) y[i]);
			} else {
				path.lineTo((float) x[i], (float) y[i]);
			}
		}
		if (close) {
			path.close();
		}
	}

	private void fill(GC gc, int rcolor, java.util.function.Consumer<Path> shape, int rule) {
		if (GraphicsContext.isTransparent(rcolor)) {
			return;
		}
		Path path = new Path(device);
		try {
			shape.accept(path);
			gc.setBackground(color(rcolor));
			gc.setAlpha(GraphicsContext.alpha(rcolor));
			gc.setFillRule(rule);
			gc.fillPath(path);
		} finally {
			path.dispose();
		}
	}

	private void stroke(GC gc, GraphicsContext g, java.util.function.Consumer<Path> shape) {
		if (g.lty() == GraphicsContext.LTY_BLANK || GraphicsContext.isTransparent(g.col())) {
			return;
		}
		Path path = new Path(device);
		try {
			shape.accept(path);
			gc.setForeground(color(g.col()));
			gc.setAlpha(GraphicsContext.alpha(g.col()));
			gc.setLineAttributes(lineAttributes(g));
			gc.drawPath(path);
		} finally {
			path.dispose();
		}
	}

	/* The line of R: the width (lwd 1 = 1/96 inch, the device has 96 dpi), the dash pattern, the ends and joins */
	private static LineAttributes lineAttributes(GraphicsContext g) {
		float lw = (float) Math.max(0.01, g.lwd());
		int cap = switch (g.lend()) {
		case 2 -> SWT.CAP_FLAT;
		case 3 -> SWT.CAP_SQUARE;
		default -> SWT.CAP_ROUND;
		};
		int join = switch (g.ljoin()) {
		case 2 -> SWT.JOIN_MITER;
		case 3 -> SWT.JOIN_BEVEL;
		default -> SWT.JOIN_ROUND;
		};
		float[] dash = dash(g.lty(), lw);
		return new LineAttributes(lw, cap, join, dash == null ? SWT.LINE_SOLID : SWT.LINE_CUSTOM, dash, 0, (float) Math.max(1, g.lmitre()));
	}

	/* 4-bit lengths (dash, gap, ...) in units of the line width */
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

	/* A text at its baseline, rotated, scaled to the width which R measured (the layout of R fits) */
	private void text(GC gc, RecordingGraphicsDevice.Text t, double dpi) {
		GraphicsContext g = t.gc();
		if (GraphicsContext.isTransparent(g.col())) {
			return;
		}
		gc.setFont(font(g, dpi));
		Point extent = gc.textExtent(t.text(), SWT.DRAW_TRANSPARENT);
		FontMetrics fm = gc.getFontMetrics();
		double sx = extent.x > 0 && t.width() > 0 ? t.width() / extent.x : 1;
		/* Only a correction of the font (another font of SWT) */
		sx = Math.max(0.7, Math.min(1.4, sx));
		Transform tr = new Transform(device);
		try {
			float[] elements = new float[6];
			base.getElements(elements);
			tr.setElements(elements[0], elements[1], elements[2], elements[3], elements[4], elements[5]);
			tr.translate((float) t.x(), (float) t.y());
			if (t.rot() != 0) {
				tr.rotate((float) -t.rot());
			}
			tr.scale((float) sx, 1f);
			gc.setTransform(tr);
			gc.setForeground(color(g.col()));
			gc.setAlpha(GraphicsContext.alpha(g.col()));
			int x = (int) Math.round(-extent.x * t.hadj());
			int y = -(fm.getAscent() + fm.getLeading());
			gc.drawString(t.text(), x, y, true);
		} finally {
			gc.setTransform(base);
			tr.dispose();
		}
	}

	/* The SWT font for the font of R (the size in pixels of the device -> points of the display) */
	private Font font(GraphicsContext g, double dpi) {
		double pixels = Math.max(1, g.fontSize() * dpi / 72.0);
		int dpiY = Math.max(1, device.getDPI().y);
		float points = (float) (pixels * 72.0 / dpiY);
		String family = switch (g.fontfamily() == null ? "" : g.fontfamily()) {
		case "", "sans", "Helvetica", "Arial" -> sansName();
		case "serif", "Times" -> isMac() ? "Times" : "Times New Roman";
		case "mono", "Courier" -> isMac() ? "Menlo" : "Courier New";
		default -> g.fontfamily();
		};
		int style = switch (g.fontface()) {
		case 2 -> SWT.BOLD;
		case 3 -> SWT.ITALIC;
		case 4 -> SWT.BOLD | SWT.ITALIC;
		default -> SWT.NORMAL;
		};
		if (g.fontface() == 5) {
			family = isMac() ? "Times" : "Times New Roman";
		}
		String name = family;
		String key = name + "|" + style + "|" + Math.round(points * 10);
		return fonts.computeIfAbsent(key, _ -> {
			org.eclipse.swt.graphics.FontData fd = new org.eclipse.swt.graphics.FontData(name, 10, style);
			fd.height = points;
			return new Font(device, fd);
		});
	}

	private String sansName() {
		org.eclipse.swt.graphics.FontData[] system = device.getSystemFont().getFontData();
		return system.length > 0 ? system[0].getName() : "Arial";
	}

	private static boolean isMac() {
		return System.getProperty("os.name", "").toLowerCase().contains("mac");
	}

	/* An image as the cairo device of R: (x, y) is the bottom left corner, the height is negative if y points down */
	private void raster(GC gc, RecordingGraphicsDevice.Raster r) {
		if (r.w() <= 0 || r.h() <= 0) {
			return;
		}
		ImageData data = new ImageData(r.w(), r.h(), 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		byte[] alpha = new byte[r.w() * r.h()];
		int[] rgb = new int[r.w() * r.h()];
		for (int i = 0; i < rgb.length; i++) {
			int argb = GraphicsContext.toARGB(r.abgr()[i]);
			rgb[i] = argb & 0xFFFFFF;
			alpha[i] = (byte) (argb >>> 24);
		}
		data.setPixels(0, 0, rgb.length, rgb, 0);
		data.alphaData = alpha;
		float[] elements = new float[6];
		base.getElements(elements);
		if (!r.interpolate()) {
			/*
			 * Sharp pixels: enlarged without interpolation (nearest neighbour) to the size on the
			 * display (twice for HiDPI), only the small rest is scaled by the GC
			 */
			double s = Math.hypot(elements[0], elements[1]) * 2;
			int sw = (int) Math.min(4096, Math.max(r.w(), Math.round(Math.abs(r.width()) * s)));
			int sh = (int) Math.min(4096, Math.max(r.h(), Math.round(Math.abs(r.height()) * s)));
			if (sw != r.w() || sh != r.h()) {
				data = data.scaledTo(sw, sh);
			}
		}
		int iw = data.width, ih = data.height;
		Image image = new Image(device, data);
		Transform tr = new Transform(device);
		try {
			tr.setElements(elements[0], elements[1], elements[2], elements[3], elements[4], elements[5]);
			tr.translate((float) r.x(), (float) r.y());
			tr.rotate((float) -r.rot());
			tr.scale((float) (r.width() / iw), (float) (r.height() / ih));
			tr.translate(0, ih / 2f);
			tr.scale(1, -1);
			tr.translate(0, -ih / 2f);
			gc.setTransform(tr);
			gc.setAlpha(255);
			gc.setInterpolation(r.interpolate() ? SWT.HIGH : SWT.NONE);
			gc.drawImage(image, 0, 0);
		} finally {
			gc.setInterpolation(SWT.HIGH);
			gc.setTransform(base);
			tr.dispose();
			image.dispose();
		}
	}

	private static Color color(int rcolor) {
		return new Color(rcolor & 0xFF, (rcolor >>> 8) & 0xFF, (rcolor >>> 16) & 0xFF);
	}
}
