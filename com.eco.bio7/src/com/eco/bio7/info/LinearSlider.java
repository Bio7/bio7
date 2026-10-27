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
package com.eco.bio7.info;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A slider for an integer value on a linear scale with the look of the
 * SpeedSlider: minor and major ticks (1, 2 or 5 times a power of ten) and
 * labelled major ticks. The labels can show the value multiplied with a factor
 * (see setLabelFactor). The value can be changed with the mouse (click, drag,
 * wheel) and the keys: Left/Right (Down/Up) one increment, Shift+Left/Right and
 * PageDown/PageUp one page increment (a major tick by default), Home/End to the
 * minimum/maximum. A Selection event is sent when the value is changed by the
 * user.
 */
public class LinearSlider extends Canvas {

	private int minimum = 0;
	private int maximum = 100;
	private int selection = 0;
	private int increment = 1;
	private int pageIncrement = 0;
	private double labelFactor = 1;
	private boolean dragging;
	/* Without an own background the slider is transparent (e.g., in a group) */
	private boolean backgroundSet;

	public LinearSlider(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.TRANSPARENT);
		addListener(SWT.Paint, e -> paint(e.gc));
		addListener(SWT.Resize, _ -> redraw());
		addListener(SWT.FocusIn, _ -> redraw());
		addListener(SWT.FocusOut, _ -> redraw());
		addListener(SWT.KeyDown, this::keyDown);
		addListener(SWT.Traverse, e -> {
			/* Tab moves the focus, the arrow keys change the value! */
			if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
				e.doit = true;
			}
		});
		addListener(SWT.MouseDown, e -> {
			if (e.button == 1) {
				setFocus();
				dragging = true;
				setValueByUser(valueAt(e.x));
			}
		});
		addListener(SWT.MouseMove, e -> {
			if (dragging) {
				setValueByUser(valueAt(e.x));
			}
		});
		addListener(SWT.MouseUp, _ -> dragging = false);
		addListener(SWT.MouseVerticalWheel, e -> {
			setValueByUser(selection + (e.count > 0 ? increment : -increment));
			e.doit = false;
		});
		getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getRole(AccessibleControlEvent e) {
				e.detail = ACC.ROLE_SLIDER;
			}

			@Override
			public void getValue(AccessibleControlEvent e) {
				e.result = format(selection * labelFactor);
			}
		});
	}

	@Override
	public void setBackground(Color color) {
		super.setBackground(color);
		backgroundSet = color != null;
	}

	public int getSelection() {
		return selection;
	}

	/** Sets the value without a Selection event. */
	public void setSelection(int value) {
		int v = Math.max(minimum, Math.min(maximum, value));
		if (v != selection) {
			selection = v;
			redraw();
		}
	}

	public int getMinimum() {
		return minimum;
	}

	public void setMinimum(int minimum) {
		this.minimum = minimum;
		if (maximum < minimum) {
			maximum = minimum;
		}
		setSelection(selection);
		redraw();
	}

	public int getMaximum() {
		return maximum;
	}

	public void setMaximum(int maximum) {
		this.maximum = Math.max(minimum, maximum);
		setSelection(selection);
		redraw();
	}

	/** The change of the arrow keys and the mouse wheel. */
	public void setIncrement(int increment) {
		this.increment = Math.max(1, increment);
	}

	/** The change of PageUp/PageDown (0 = a major tick). */
	public void setPageIncrement(int pageIncrement) {
		this.pageIncrement = Math.max(0, pageIncrement);
	}

	/** The labels of the ticks show the value multiplied with the factor. */
	public void setLabelFactor(double labelFactor) {
		this.labelFactor = labelFactor;
		redraw();
	}

	public void addSelectionListener(SelectionListener listener) {
		addListener(SWT.Selection, e -> listener.widgetSelected(new SelectionEvent(e)));
	}

	private void setValueByUser(int value) {
		int v = Math.max(minimum, Math.min(maximum, value));
		if (v != selection) {
			selection = v;
			redraw();
			notifyListeners(SWT.Selection, new Event());
		}
	}

	private void keyDown(Event e) {
		int page = pageIncrement > 0 ? pageIncrement : (int) Math.max(1, Math.round(ticks()[0]));
		int n = (e.stateMask & SWT.SHIFT) != 0 ? page : increment;
		switch (e.keyCode) {
		case SWT.ARROW_RIGHT:
		case SWT.ARROW_UP:
			setValueByUser(selection + n);
			break;
		case SWT.ARROW_LEFT:
		case SWT.ARROW_DOWN:
			setValueByUser(selection - n);
			break;
		case SWT.PAGE_UP:
			setValueByUser(selection + page);
			break;
		case SWT.PAGE_DOWN:
			setValueByUser(selection - page);
			break;
		case SWT.HOME:
			setValueByUser(minimum);
			break;
		case SWT.END:
			setValueByUser(maximum);
			break;
		default:
			return;
		}
		e.doit = false;
	}

	/* The major and the minor tick distance in values (1, 2 or 5 times a power of ten of the labels) */
	private double[] ticks() {
		double range = Math.max(1e-9, (maximum - minimum) * Math.abs(labelFactor));
		double raw = range / 5;
		double power = Math.pow(10, Math.floor(Math.log10(raw)));
		double f = raw / power;
		double nice = f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10;
		double major = nice * power;
		double minor = major / (nice == 2 ? 4 : 5);
		double factor = Math.abs(labelFactor) > 0 ? Math.abs(labelFactor) : 1;
		return new double[] { major / factor, minor / factor };
	}

	/** The label of a value, e.g., "0.5", "12", "1000". */
	public static String format(double v) {
		if (Math.abs(v) >= 100 || v == Math.rint(v)) {
			return String.format(Locale.ROOT, "%.0f", v);
		}
		String s = String.format(Locale.ROOT, Math.abs(v) >= 10 ? "%.1f" : "%.2f", v);
		return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
	}

	/* ---- layout and painting ---- */

	private static int thumbHeight(GC gc) {
		return Math.max(14, gc.getFontMetrics().getHeight());
	}

	/* The track: x of the minimum and the width */
	private int[] track(GC gc) {
		int labels = Math.max(gc.textExtent(format(minimum * labelFactor)).x,
				gc.textExtent(format(maximum * labelFactor)).x) / 2;
		int m = Math.max(thumbHeight(gc) / 2 + 3, labels + 2);
		int w = Math.max(1, getClientArea().width - 2 * m);
		return new int[] { m, w };
	}

	private int xOf(int[] t, double value) {
		double fraction = (value - minimum) / Math.max(1e-9, (double) (maximum - minimum));
		return t[0] + (int) Math.round(fraction * t[1]);
	}

	private int valueAt(int x) {
		GC gc = new GC(this);
		int[] t = track(gc);
		gc.dispose();
		double fraction = Math.max(0, Math.min(1, (x - t[0]) / (double) Math.max(1, t[1])));
		int v = minimum + (int) Math.round(fraction * (maximum - minimum));
		/* Snapped to the increment */
		return minimum + Math.round((v - minimum) / (float) increment) * increment;
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		GC gc = new GC(this);
		int fh = gc.getFontMetrics().getHeight();
		int h = 2 + thumbHeight(gc) + 2 + 7 + 2 + fh + 2;
		gc.dispose();
		return new Point(wHint == SWT.DEFAULT ? 200 : wHint, hHint == SWT.DEFAULT ? h : hHint);
	}

	private Color blend(Color a, Color b, double f) {
		RGB x = a.getRGB(), y = b.getRGB();
		return new Color(getDisplay(), (int) (x.red * f + y.red * (1 - f)), (int) (x.green * f + y.green * (1 - f)),
				(int) (x.blue * f + y.blue * (1 - f)));
	}

	private void paint(GC gc) {
		Rectangle r = getClientArea();
		Color fg = getForeground();
		Color bg = getBackground();
		Color trackColor = blend(fg, bg, 0.3);
		Color minorColor = blend(fg, bg, 0.4);
		Color majorColor = blend(fg, bg, 0.7);
		Color accent = isEnabled() ? getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND) : minorColor;
		/* The thumb is lighter than the background (light theme) or a light grey (dark theme)! */
		Color knob = blend(getDisplay().getSystemColor(SWT.COLOR_WHITE), bg, 0.8);
		try {
			gc.setAdvanced(true);
			if (backgroundSet) {
				gc.setBackground(bg);
				gc.fillRectangle(r);
			}
			int[] t = track(gc);
			int m = t[0];
			int w = t[1];
			int th = thumbHeight(gc);
			int cy = 2 + th / 2;
			int x = xOf(t, selection);
			double[] ticks = ticks();
			double major = ticks[0], minor = ticks[1];
			/* Ticks below the track: the minor ticks if they are not too close */
			gc.setAntialias(SWT.OFF);
			int ty = 2 + th + 2;
			double pixelsPerMinor = minor / Math.max(1e-9, maximum - minimum) * w;
			if (pixelsPerMinor >= 3) {
				gc.setForeground(minorColor);
				for (long i = (long) Math.ceil(minimum / minor - 1e-9); i * minor <= maximum + 1e-9; i++) {
					int tx = xOf(t, i * minor);
					gc.drawLine(tx, ty, tx, ty + 3);
				}
			}
			List<Double> majors = new ArrayList<>();
			majors.add((double) minimum);
			for (long i = (long) Math.ceil(minimum / major - 1e-9); i * major <= maximum + 1e-9; i++) {
				double v = i * major;
				if (v > minimum && v < maximum) {
					majors.add(v);
				}
			}
			majors.add((double) maximum);
			gc.setForeground(majorColor);
			for (double v : majors) {
				int tx = xOf(t, v);
				gc.drawLine(tx, ty, tx, ty + 6);
			}
			gc.setAntialias(SWT.ON);
			/* Track, the part up to the value in the accent color! */
			gc.setBackground(trackColor);
			gc.fillRoundRectangle(m, cy - 2, w, 4, 4, 4);
			gc.setBackground(accent);
			gc.fillRoundRectangle(m, cy - 2, Math.max(4, x - m), 4, 4, 4);
			/* Labels: the ends first, then the major ticks if there is room! */
			gc.setForeground(fg);
			List<int[]> used = new ArrayList<>();
			List<Double> labelled = new ArrayList<>();
			labelled.add((double) minimum);
			labelled.add((double) maximum);
			labelled.addAll(majors.subList(1, majors.size() - 1));
			for (double v : labelled) {
				String text = format(v * labelFactor);
				Point e = gc.textExtent(text);
				int lx = xOf(t, v) - e.x / 2;
				int x0 = lx - 4, x1 = lx + e.x + 4;
				boolean free = lx >= 0 && lx + e.x <= r.width;
				for (int[] u : used) {
					if (x0 < u[1] && x1 > u[0]) {
						free = false;
					}
				}
				if (free) {
					gc.drawText(text, lx, ty + 8, true);
					used.add(new int[] { x0, x1 });
				}
			}
			/* Round thumb! */
			int d = th;
			gc.setBackground(knob);
			gc.fillOval(x - d / 2, 2, d, d);
			gc.setForeground(isEnabled() ? accent : majorColor);
			gc.setLineWidth(2);
			gc.drawOval(x - d / 2 + 1, 3, d - 2, d - 2);
			gc.setLineWidth(1);
			if (isFocusControl()) {
				gc.setForeground(fg);
				gc.setLineStyle(SWT.LINE_DOT);
				gc.drawOval(x - d / 2 - 2, 0, d + 4, d + 4);
				gc.setLineStyle(SWT.LINE_SOLID);
			}
		} finally {
			trackColor.dispose();
			minorColor.dispose();
			majorColor.dispose();
			knob.dispose();
		}
	}
}
