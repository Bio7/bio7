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
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

/**
 * The selected items of the lists (SWT List) on Windows: the native list box
 * draws the selection with the system colors (e.g., a white bar with a white
 * text in a dark theme). With a dark background the selected items are painted
 * over (after the native painting) with a grey selection (a blend of the text
 * and the background color) and the text. With a light background the native
 * selection is kept.
 */
final class ListSelectionColors {

	private static final boolean WINDOWS = "win32".equals(SWT.getPlatform());
	/* A list with the paint listener */
	private static final String PAINTED = "bio7.themes.listPainted";
	/* The share of the text color in the selection with and without the focus (the rest is the background) */
	private static final double FOCUS_SHARE = 0.35;
	private static final double SHARE = 0.22;
	/* The left margin of the text of the list box */
	private static final int TEXT_MARGIN = 2;

	private static boolean installed;

	private ListSelectionColors() {
	}

	/* The lists of the open views and the lists created later (e.g., in a view opened later) */
	static void install() {
		if (installed || !WINDOWS) {
			return;
		}
		installed = true;
		Display display = Display.getDefault();
		display.addFilter(SWT.Skin, event -> {
			if (event.widget instanceof List list) {
				hook(list);
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
		if (control instanceof List list) {
			hook(list);
		} else if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				hookAll(child);
			}
		}
	}

	private static void hook(List list) {
		if (list.isDisposed() || list.getData(PAINTED) != null) {
			return;
		}
		list.setData(PAINTED, Boolean.TRUE);
		list.addListener(SWT.Paint, ListSelectionColors::paint);
		/* The selection has another color without the focus */
		list.addListener(SWT.FocusIn, _ -> list.redraw());
		list.addListener(SWT.FocusOut, _ -> list.redraw());
		/*
		 * The list box draws a new selection directly (mouse, keys), not in a paint:
		 * the list is painted again after a change of the selection (also a selection
		 * of the program without an event, e.g., setSelection, at the next mouse or
		 * key release)
		 */
		list.addListener(SWT.Selection, _ -> list.redraw());
		list.addListener(SWT.DefaultSelection, _ -> list.redraw());
		list.addListener(SWT.MouseUp, _ -> list.redraw());
		list.addListener(SWT.KeyUp, _ -> list.redraw());
		list.redraw();
	}

	/* After the native painting: the selected items in grey (only with a dark background) */
	private static void paint(Event event) {
		List list = (List) event.widget;
		RGB bg = list.getBackground().getRGB();
		if (luminance(bg) >= 128) {
			return;
		}
		int[] selection = list.getSelectionIndices();
		if (selection.length == 0) {
			return;
		}
		RGB fg = list.getForeground().getRGB();
		double share = list.isFocusControl() ? FOCUS_SHARE : SHARE;
		Color fill = new Color(blend(fg.red, bg.red, share), blend(fg.green, bg.green, share), blend(fg.blue, bg.blue, share));
		GC gc = event.gc;
		try {
			Rectangle area = list.getClientArea();
			int itemHeight = Math.max(1, list.getItemHeight());
			int top = list.getTopIndex();
			gc.setFont(list.getFont());
			gc.setForeground(list.getForeground());
			int textHeight = gc.getFontMetrics().getHeight();
			for (int index : selection) {
				int y = area.y + (index - top) * itemHeight;
				if (y + itemHeight < area.y || y > area.y + area.height) {
					continue;
				}
				gc.setBackground(fill);
				gc.fillRectangle(area.x, y, area.width, itemHeight);
				gc.drawText(list.getItem(index), area.x + TEXT_MARGIN, y + (itemHeight - textHeight) / 2, true);
			}
		} finally {
			fill.dispose();
		}
	}

	private static int blend(int fg, int bg, double share) {
		return (int) Math.round(fg * share + bg * (1 - share));
	}

	private static double luminance(RGB rgb) {
		return 0.299 * rgb.red + 0.587 * rgb.green + 0.114 * rgb.blue;
	}
}
