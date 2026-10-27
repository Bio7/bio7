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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A slider for a frame rate on a logarithmic scale made of even steps: a
 * decade (e.g., 1 - 10 fps) has {@value #STEPS_PER_DECADE} steps, so that each
 * step changes the speed by the same factor (about 8%). Each step has a minor
 * tick, every {@value #MAJOR_STEPS} steps a major tick, the decades (1, 10,
 * 100, ...) and the ends are labelled. The value can be changed with the mouse
 * (click, drag, wheel) and the keys: Left/Right (Down/Up) one step,
 * Shift+Left/Right and PageDown/PageUp {@value #MAJOR_STEPS} steps, Home/End
 * to the minimum/maximum. A Selection event is sent when the value is changed
 * by the user.
 */
public class SpeedSlider extends Canvas {

	/** The steps in a decade (10^(1/30) = 1.08 per step). */
	public static final int STEPS_PER_DECADE = 30;

	/** The steps between two major ticks (and of Shift+Left/Right). */
	public static final int MAJOR_STEPS = 10;

	/* The first and the last step (step 0 is 1 fps)! */
	private final int first;
	private final int last;
	private double value;
	private boolean dragging;
	/* Without an own background the slider is transparent (e.g., in a group) */
	private boolean backgroundSet;

	/**
	 * @param min the lowest frame rate (rounded up to a step)
	 * @param max the highest frame rate (rounded down to a step)
	 */
	public SpeedSlider(Composite parent, int style, double min, double max) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.TRANSPARENT);
		first = (int) Math.ceil(STEPS_PER_DECADE * Math.log10(min) - 1e-6);
		last = (int) Math.floor(STEPS_PER_DECADE * Math.log10(max) + 1e-6);
		value = valueOf(first);
		addListener(SWT.Paint, e -> paint(e.gc));
		addListener(SWT.Resize, e -> redraw());
		addListener(SWT.FocusIn, e -> redraw());
		addListener(SWT.FocusOut, e -> redraw());
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
				setStep(stepAt(e.x));
			}
		});
		addListener(SWT.MouseMove, e -> {
			if (dragging) {
				setStep(stepAt(e.x));
			}
		});
		addListener(SWT.MouseUp, e -> dragging = false);
		addListener(SWT.MouseVerticalWheel, e -> {
			setStep(step() + (e.count > 0 ? 1 : -1));
			e.doit = false;
		});
		getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
			@Override
			public void getRole(AccessibleControlEvent e) {
				e.detail = ACC.ROLE_SLIDER;
			}

			@Override
			public void getValue(AccessibleControlEvent e) {
				e.result = format(value) + " fps";
			}
		});
	}

	@Override
	public void setBackground(Color color) {
		super.setBackground(color);
		backgroundSet = color != null;
	}

	/** The frame rate of a step (step 0 is 1 fps, step 30 is 10 fps). */
	public static double valueOf(int step) {
		/* Rounded to 6 digits, so that, e.g., step 30 is exactly 10! */
		return Double.parseDouble(String.format(Locale.ROOT, "%.6g", Math.pow(10, step / (double) STEPS_PER_DECADE)));
	}

	/* The (fractional) step of a frame rate! */
	private static double stepOf(double v) {
		return STEPS_PER_DECADE * Math.log10(v);
	}

	/* The current step (the nearest one if the value was set between two steps)! */
	private int step() {
		return (int) Math.round(stepOf(value));
	}

	/** The value (frames per second). */
	public double getValue() {
		return value;
	}

	/** Sets the value without a Selection event (also between two steps). */
	public void setValue(double v) {
		double c = Math.max(valueOf(first), Math.min(valueOf(last), v));
		if (c != value) {
			value = c;
			redraw();
		}
	}

	private void setStep(int step) {
		double v = valueOf(Math.max(first, Math.min(last, step)));
		if (v != value) {
			value = v;
			redraw();
			notifyListeners(SWT.Selection, new Event());
		}
	}

	private void keyDown(Event e) {
		int n = (e.stateMask & SWT.SHIFT) != 0 ? MAJOR_STEPS : 1;
		switch (e.keyCode) {
		case SWT.ARROW_RIGHT:
		case SWT.ARROW_UP:
			setStep(step() + n);
			break;
		case SWT.ARROW_LEFT:
		case SWT.ARROW_DOWN:
			setStep(step() - n);
			break;
		case SWT.PAGE_UP:
			setStep(step() + MAJOR_STEPS);
			break;
		case SWT.PAGE_DOWN:
			setStep(step() - MAJOR_STEPS);
			break;
		case SWT.HOME:
			setStep(first);
			break;
		case SWT.END:
			setStep(last);
			break;
		default:
			return;
		}
		e.doit = false;
	}

	/** The label of a value, e.g., "0.5", "12", "1000". */
	public static String format(double v) {
		if (v >= 100) {
			return String.format(Locale.ROOT, "%.0f", v);
		}
		/* Small values with two significant digits, e.g., 0.0032 or 0.000001 */
		int decimals = v >= 10 ? 1 : v >= 0.1 || v <= 0 ? 2 : (int) Math.ceil(-Math.log10(v)) + 1;
		String s = String.format(Locale.ROOT, "%." + decimals + "f", v);
		/* No trailing zeros: 2.50 -> 2.5, 10.0 -> 10! */
		return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
	}

	/* ---- layout and painting ---- */

	private static int thumbHeight(GC gc) {
		return Math.max(14, gc.getFontMetrics().getHeight());
	}

	/* The track: x of the first step and the (whole) pixels per step, so that all ticks are evenly spaced! */
	private int[] track(GC gc) {
		int labels = Math.max(gc.textExtent(format(valueOf(first))).x, gc.textExtent(format(valueOf(last))).x) / 2;
		int m = Math.max(thumbHeight(gc) / 2 + 3, labels + 2);
		int n = Math.max(1, last - first);
		int available = Math.max(n, getClientArea().width - 2 * m);
		/* Whole pixels per step if there is enough room, else fractional! */
		double spacing = available / (double) n >= 3 ? Math.floor(available / (double) n) : available / (double) n;
		int w = (int) Math.round(spacing * n);
		return new int[] { (getClientArea().width - w) / 2, w };
	}

	private static int xOf(int[] t, double fraction) {
		return t[0] + (int) Math.round(fraction * t[1]);
	}

	private double fraction(double step) {
		return (step - first) / (double) Math.max(1, last - first);
	}

	private int stepAt(int x) {
		GC gc = new GC(this);
		int[] t = track(gc);
		gc.dispose();
		return first + (int) Math.round(Math.max(0, Math.min(1, (x - t[0]) / (double) Math.max(1, t[1]))) * (last - first));
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
			int x = xOf(t, fraction(stepOf(value)));
			/* Ticks below the track (sharp lines): each step, every 5 steps if the steps are too close! */
			gc.setAntialias(SWT.OFF);
			int ty = 2 + th + 2;
			int minorEvery = w / (double) Math.max(1, last - first) >= 3 ? 1 : 5;
			for (int s = first; s <= last; s++) {
				boolean major = s % MAJOR_STEPS == 0 || s == first || s == last;
				if (!major && s % minorEvery != 0) {
					continue;
				}
				int tx = xOf(t, fraction(s));
				gc.setForeground(major ? majorColor : minorColor);
				gc.drawLine(tx, ty, tx, ty + (major ? 6 : 3));
			}
			gc.setAntialias(SWT.ON);
			/* Track, the part up to the value in the accent color! */
			gc.setBackground(trackColor);
			gc.fillRoundRectangle(m, cy - 2, w, 4, 4, 4);
			gc.setBackground(accent);
			gc.fillRoundRectangle(m, cy - 2, Math.max(4, x - m), 4, 4, 4);
			/* Labels: the ends and the decades (1, 10, 100, ...) if there is room! */
			gc.setForeground(fg);
			List<int[]> used = new ArrayList<int[]>();
			List<Integer> labelled = new ArrayList<Integer>();
			labelled.add(first);
			labelled.add(last);
			for (int s = first; s <= last; s++) {
				if (s % STEPS_PER_DECADE == 0 && s != first && s != last) {
					labelled.add(s);
				}
			}
			for (int s : labelled) {
				String text = format(valueOf(s));
				Point e = gc.textExtent(text);
				int lx = xOf(t, fraction(s)) - e.x / 2;
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
