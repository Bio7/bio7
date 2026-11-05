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
package com.eco.bio7.themes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * The separators (labels with the style SWT.SEPARATOR) on Windows: Windows draws
 * the etched line with the system colors (a white line, also in a dark theme).
 * With a dark background the separator is painted over with a grey line (a
 * blend of the text and the background color). With a light background the
 * native line is kept.
 */
final class SeparatorColors {

	private static final boolean WINDOWS = "win32".equals(SWT.getPlatform());
	/* A separator with the paint listener */
	private static final String PAINTED = "bio7.themes.separatorPainted";
	/* The share of the text color in the line (the rest is the background) */
	private static final double LINE_SHARE = 0.35;

	private static boolean installed;

	private SeparatorColors() {
	}

	/* The separators of the open views and the separators created later (e.g., in a view opened later) */
	static void install() {
		if (installed || !WINDOWS) {
			return;
		}
		installed = true;
		Display display = Display.getDefault();
		display.addFilter(SWT.Skin, event -> {
			if (event.widget instanceof Label label) {
				hook(label);
			}
		});
		for (Shell shell : display.getShells()) {
			hookAll(shell);
		}
	}

	private static void hookAll(Control control) {
		if (control.isDisposed()) {
			return;
		}
		if (control instanceof Label label) {
			hook(label);
		} else if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				hookAll(child);
			}
		}
	}

	private static void hook(Label label) {
		if (label.isDisposed() || (label.getStyle() & SWT.SEPARATOR) == 0 || (label.getStyle() & SWT.SHADOW_NONE) != 0
				|| label.getData(PAINTED) != null) {
			return;
		}
		label.setData(PAINTED, Boolean.TRUE);
		label.addListener(SWT.Paint, SeparatorColors::paint);
		label.redraw();
	}

	/* After the native line: the line in grey (only with a dark background) */
	private static void paint(Event event) {
		Label label = (Label) event.widget;
		Color background = label.getBackground();
		RGB bg = background.getRGB();
		if (luminance(bg) >= 128) {
			return;
		}
		RGB fg = label.getForeground().getRGB();
		/* In the coordinates of the label (not getBounds: the position in the parent) */
		org.eclipse.swt.graphics.Point size = label.getSize();
		Rectangle area = new Rectangle(0, 0, size.x, size.y);
		GC gc = event.gc;
		Color line = new Color(blend(fg.red, bg.red), blend(fg.green, bg.green), blend(fg.blue, bg.blue));
		try {
			gc.setBackground(background);
			gc.fillRectangle(area);
			gc.setForeground(line);
			gc.setLineWidth(1);
			if ((label.getStyle() & SWT.VERTICAL) != 0) {
				int x = area.x + area.width / 2;
				gc.drawLine(x, area.y, x, area.y + area.height);
			} else {
				int y = area.y + area.height / 2;
				gc.drawLine(area.x, y, area.x + area.width, y);
			}
		} finally {
			line.dispose();
		}
	}

	private static int blend(int fg, int bg) {
		return (int) Math.round(fg * LINE_SHARE + bg * (1 - LINE_SHARE));
	}

	private static double luminance(RGB rgb) {
		return 0.299 * rgb.red + 0.587 * rgb.green + 0.114 * rgb.blue;
	}
}
