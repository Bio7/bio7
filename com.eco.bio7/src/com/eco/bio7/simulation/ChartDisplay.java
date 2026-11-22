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

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.eco.bio7.collection.CustomView;

/**
 * The live charts of a simulation in Bio7 (the panel "Charts" below the display
 * of the model in the Custom perspective): the line charts of chart(...), the
 * XY charts of chartXY(...) and the bar charts of chartBars(...).
 */
final class ChartDisplay {

	/** The panel of the charts in the Custom perspective (below the display on the right). */
	static final String PANEL = "subRightBottom";

	private static final RGB[] CATEGORY = { new RGB(0x1f, 0x77, 0xb4), new RGB(0xff, 0x7f, 0x0e), new RGB(0x2c, 0xa0, 0x2c), new RGB(0xd6, 0x27, 0x28), new RGB(0x94, 0x67, 0xbd),
			new RGB(0x8c, 0x56, 0x4b), new RGB(0xe3, 0x77, 0xc2), new RGB(0x7f, 0x7f, 0x7f), new RGB(0xbc, 0xbd, 0x22), new RGB(0x17, 0xbe, 0xcf) };

	private static Canvas canvas;
	private static volatile List<ChartData> charts = List.of();
	private static volatile long last;
	private static volatile boolean pending;

	private ChartDisplay() {
	}

	/** Shows the charts of the model (at most about 7 times per second; nothing without charts). */
	static void update(Simulation model) {
		if (!Simulation.hasDisplay()) {
			return;
		}
		List<ChartData> c = model.getCharts();
		/* A model with charts: the view is shown at the start (no change of the layout with the first values) */
		boolean wanted = !c.isEmpty() || model.usesCharts();
		if (!wanted && charts.isEmpty() && (canvas == null || canvas.isDisposed())) {
			return;
		}
		charts = c;
		long now = System.currentTimeMillis();
		if (pending || now - last < 150 && !c.isEmpty() && model.t > 0) {
			return;
		}
		last = now;
		pending = true;
		Simulation.ui(() -> {
			pending = false;
			show(model);
		});
	}

	private static void show(Simulation model) {
		if (charts.isEmpty() && !model.usesCharts()) {
			if (canvas != null && !canvas.isDisposed()) {
				canvas.redraw();
			}
			return;
		}
		if (canvas == null || canvas.isDisposed()) {
			Composite parent = new CustomView().getComposite(model.arrangeViews ? PANEL : "Charts");
			if (parent == null || parent.isDisposed()) {
				return;
			}
			if (model.arrangeViews) {
				com.eco.bio7.collection.Work.renamePanelTab(parent, "Charts");
			}
			parent.setLayout(new FillLayout());
			canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
			canvas.addListener(SWT.Paint, e -> paint(e.gc, canvas.getSize()));
			canvas.addListener(SWT.Resize, e -> canvas.redraw());
			parent.layout(true, true);
			/* Beside the display (below the editor) */
			if (model.arrangeViews) {
				SimulationLayout.chartsBeside();
			}
		}
		canvas.redraw();
	}

	/* All charts in a grid (two columns for more than one chart) */
	private static void paint(GC gc, Point size) {
		Color bg = canvas.getBackground(), fg = canvas.getForeground();
		gc.setBackground(bg);
		gc.fillRectangle(0, 0, size.x, size.y);
		List<ChartData> list = charts;
		if (list.isEmpty()) {
			gc.setForeground(canvas.getForeground());
			gc.drawText("The charts of the model are shown when the simulation runs.", 12, 10, true);
			return;
		}
		gc.setAntialias(SWT.ON);
		gc.setTextAntialias(SWT.ON);
		int cols = list.size() > 1 && size.x > 520 ? 2 : 1, rows = (list.size() + cols - 1) / cols;
		int cw = size.x / cols, ch = size.y / rows;
		for (int i = 0; i < list.size(); i++) {
			chart(gc, list.get(i), (i % cols) * cw, (i / cols) * ch, cw, ch, fg);
		}
	}

	private static void chart(GC gc, ChartData c, int x0, int y0, int w, int h, Color fg) {
		Color muted = new Color(blend(fg.getRGB(), canvas.getBackground().getRGB(), 0.55)), grid = new Color(blend(fg.getRGB(), canvas.getBackground().getRGB(), 0.85));
		int left = x0 + 54, right = x0 + w - 130, top = y0 + 22, bottom = y0 + h - 22;
		if (right - left < 40 || bottom - top < 30) {
			return;
		}
		gc.setForeground(fg);
		String title = c.title.isEmpty() ? String.join(", ", c.series.keySet()) : c.title;
		gc.drawText(title, x0 + 10, y0 + 4, true);
		gc.setForeground(grid);
		gc.drawRectangle(left, top, right - left, bottom - top);
		if (ChartData.BARS.equals(c.kind)) {
			bars(gc, c, left, top, right, bottom, fg, muted, grid);
			return;
		}
		double xmin = Double.MAX_VALUE, xmax = -Double.MAX_VALUE, ymin = Double.MAX_VALUE, ymax = -Double.MAX_VALUE;
		for (List<double[]> s : c.series.values()) {
			for (double[] q : s) {
				xmin = Math.min(xmin, q[0]);
				xmax = Math.max(xmax, q[0]);
				ymin = Math.min(ymin, q[1]);
				ymax = Math.max(ymax, q[1]);
			}
		}
		if (xmin == Double.MAX_VALUE) {
			return;
		}
		if (xmax == xmin) {
			xmax = xmin + 1;
		}
		if (ymax == ymin) {
			ymax += 1;
			ymin -= 1;
		}
		double pad = (ymax - ymin) * 0.05;
		ymin -= pad;
		ymax += pad;
		axes(gc, left, top, right, bottom, xmin, xmax, ymin, ymax, muted, grid);
		int k = 0;
		for (Map.Entry<String, List<double[]>> e : c.series.entrySet()) {
			Color col = new Color(CATEGORY[k % CATEGORY.length]);
			gc.setForeground(col);
			gc.setLineWidth(2);
			List<double[]> s = e.getValue();
			int step = Math.max(1, s.size() / 1500);
			int[] pts = new int[(s.size() + step - 1) / step * 2];
			int n = 0;
			for (int i = 0; i < s.size(); i += step) {
				pts[n++] = (int) (left + (s.get(i)[0] - xmin) / (xmax - xmin) * (right - left));
				pts[n++] = (int) (bottom - (s.get(i)[1] - ymin) / (ymax - ymin) * (bottom - top));
			}
			if (n >= 4) {
				gc.drawPolyline(java.util.Arrays.copyOf(pts, n));
			}
			if (ChartData.XY.equals(c.kind) && n >= 2) {
				gc.setBackground(col);
				gc.fillOval(pts[n - 2] - 3, pts[n - 1] - 3, 7, 7);
			}
			gc.setLineWidth(1);
			gc.setBackground(col);
			gc.fillRectangle(right + 10, top + k * 18 + 2, 11, 9);
			gc.setForeground(fg);
			double lastValue = s.isEmpty() ? 0 : s.get(s.size() - 1)[1];
			gc.drawText(e.getKey() + ": " + SimulationSettings.format(lastValue, Math.abs(lastValue) >= 100 ? 1 : 0.001, false), right + 25, top + k * 18 - 2, true);
			col.dispose();
			k++;
		}
		muted.dispose();
		grid.dispose();
	}

	private static void bars(GC gc, ChartData c, int left, int top, int right, int bottom, Color fg, Color muted, Color grid) {
		int n = Math.min(c.labels.length, c.values.length);
		if (n == 0) {
			return;
		}
		double ymax = 0, ymin = 0;
		for (int i = 0; i < n; i++) {
			ymax = Math.max(ymax, c.values[i]);
			ymin = Math.min(ymin, c.values[i]);
		}
		if (ymax == ymin) {
			ymax = ymin + 1;
		}
		axes(gc, left, top, right, bottom, Double.NaN, Double.NaN, ymin, ymax * 1.05, muted, grid);
		double bw = (right - left) / (double) n;
		Color col = new Color(CATEGORY[0]);
		gc.setBackground(col);
		int zero = (int) (bottom - (0 - ymin) / (ymax * 1.05 - ymin) * (bottom - top));
		/* Every k-th label if they do not fit */
		int widest = 0;
		for (int i = 0; i < n; i++) {
			widest = Math.max(widest, gc.textExtent(c.labels[i]).x);
		}
		int every = (int) Math.max(1, Math.ceil(widest * 1.15 / bw));
		for (int i = 0; i < n; i++) {
			int y = (int) (bottom - (c.values[i] - ymin) / (ymax * 1.05 - ymin) * (bottom - top));
			int x = (int) (left + i * bw + bw * 0.15);
			gc.fillRectangle(x, Math.min(y, zero), (int) Math.max(1, bw * 0.7), Math.abs(zero - y));
			gc.setForeground(muted);
			Point e = gc.textExtent(c.labels[i]);
			if (i % every == 0) {
				gc.drawText(c.labels[i], (int) (left + i * bw + bw / 2 - e.x / 2), bottom + 3, true);
			}
		}
		col.dispose();
	}

	private static void axes(GC gc, int left, int top, int right, int bottom, double xmin, double xmax, double ymin, double ymax, Color muted, Color grid) {
		double ys = nice(ymax - ymin);
		for (double v = Math.ceil(ymin / ys) * ys; v <= ymax; v += ys) {
			int y = (int) (bottom - (v - ymin) / (ymax - ymin) * (bottom - top));
			gc.setForeground(grid);
			gc.drawLine(left, y, right, y);
			gc.setForeground(muted);
			String t = label(v);
			gc.drawText(t, left - 6 - gc.textExtent(t).x, y - 8, true);
		}
		if (!Double.isNaN(xmin)) {
			double xs = nice(xmax - xmin);
			for (double v = Math.ceil(xmin / xs) * xs; v <= xmax; v += xs) {
				int x = (int) (left + (v - xmin) / (xmax - xmin) * (right - left));
				String t = label(v);
				gc.setForeground(muted);
				gc.drawText(t, x - gc.textExtent(t).x / 2, bottom + 3, true);
			}
		}
	}

	private static double nice(double span) {
		double p = Math.pow(10, Math.floor(Math.log10(span / 5))), f = span / 5 / p;
		return (f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10) * p;
	}

	private static String label(double v) {
		if (v != 0 && (Math.abs(v) >= 1e5 || Math.abs(v) < 1e-3)) {
			return String.format(java.util.Locale.ROOT, "%.1e", v);
		}
		String s = String.format(java.util.Locale.ROOT, "%.6f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
		return s.equals("-0") ? "0" : s;
	}

	private static RGB blend(RGB a, RGB b, double t) {
		return new RGB((int) (a.red * (1 - t) + b.red * t), (int) (a.green * (1 - t) + b.green * t), (int) (a.blue * (1 - t) + b.blue * t));
	}
}
