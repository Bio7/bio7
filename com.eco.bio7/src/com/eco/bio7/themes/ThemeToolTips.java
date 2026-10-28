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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * The tool tips of the controls with the colors of a custom theme: the tool tips
 * of SWT are native (they can't be colored). With a custom theme the tool tip
 * text of a control is taken when the mouse enters the control and shown in an
 * own tool tip (the hover colors of the theme). Without a custom theme the
 * controls get their tool tip text back (the native tool tips). The tool tips of
 * tool items and tab items are native.
 */
final class ThemeToolTips {

	/* The tool tip text of a control taken from the native tool tip */
	private static final String TEXT = "bio7.themes.toolTip";
	private static final int MAX_WIDTH = 420;
	private static final int PADDING = 6;

	private static Color background;
	private static Color foreground;
	private static Color border;
	private static boolean installed;
	private static Shell tip;
	private static Control owner;

	private ThemeToolTips() {
	}

	/** Sets the colors of the tool tips (null for the native tool tips). */
	static void apply(RGB back, RGB text, RGB line) {
		Display display = Display.getDefault();
		install(display);
		hide();
		if (back == null || text == null) {
			background = foreground = border = null;
		} else {
			background = new Color(display, back);
			foreground = new Color(display, text);
			border = new Color(display, line != null ? line : text);
		}
	}

	private static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		Listener listener = ThemeToolTips::handle;
		for (int type : new int[] { SWT.MouseEnter, SWT.MouseHover, SWT.MouseExit, SWT.MouseDown, SWT.MouseWheel, SWT.KeyDown,
				SWT.Deactivate }) {
			display.addFilter(type, listener);
		}
	}

	private static void handle(Event event) {
		if (!(event.widget instanceof Control control) || control.isDisposed()) {
			return;
		}
		switch (event.type) {
		case SWT.MouseEnter:
			take(control);
			break;
		case SWT.MouseHover:
			if (background != null && control.getData(TEXT) instanceof String text && control.getToolTipText() == null) {
				show(control, text);
			}
			break;
		case SWT.MouseExit:
			if (control == owner) {
				hide();
			}
			break;
		default:
			hide();
			break;
		}
	}

	/* The tool tip text of the control (a custom theme) or the native tool tip again */
	private static void take(Control control) {
		String current = control.getToolTipText();
		Object taken = control.getData(TEXT);
		if (background != null) {
			if (current != null) {
				/* Also a new tool tip text set by the application */
				control.setData(TEXT, current);
				control.setToolTipText(null);
			}
		} else if (taken instanceof String text) {
			control.setData(TEXT, null);
			if (current == null) {
				control.setToolTipText(text);
			}
		}
	}

	private static void show(Control control, String text) {
		hide();
		Display display = control.getDisplay();
		Shell shell = new Shell(control.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS | SWT.NO_TRIM);
		GC gc = new GC(shell);
		gc.setFont(control.getFont());
		List<String> lines = wrap(gc, text);
		int width = 0;
		int lineHeight = gc.getFontMetrics().getHeight();
		for (String line : lines) {
			width = Math.max(width, gc.textExtent(line).x);
		}
		gc.dispose();
		Point size = new Point(width + 2 * PADDING, lines.size() * lineHeight + 2 * PADDING);
		shell.setSize(size);
		shell.addListener(SWT.Paint, e -> {
			Rectangle r = shell.getClientArea();
			e.gc.setFont(control.isDisposed() ? null : control.getFont());
			e.gc.setBackground(background);
			e.gc.fillRectangle(r);
			e.gc.setForeground(border);
			e.gc.drawRectangle(0, 0, r.width - 1, r.height - 1);
			e.gc.setForeground(foreground);
			for (int i = 0; i < lines.size(); i++) {
				e.gc.drawText(lines.get(i), PADDING, PADDING + i * lineHeight, true);
			}
		});
		/* Below the cursor, inside the monitor */
		Point cursor = display.getCursorLocation();
		Rectangle monitor = control.getMonitor().getClientArea();
		int x = Math.max(monitor.x, Math.min(cursor.x + 8, monitor.x + monitor.width - size.x));
		int y = cursor.y + 20;
		if (y + size.y > monitor.y + monitor.height) {
			y = cursor.y - size.y - 4;
		}
		shell.setLocation(x, y);
		shell.setVisible(true);
		tip = shell;
		owner = control;
	}

	private static void hide() {
		if (tip != null && !tip.isDisposed()) {
			tip.dispose();
		}
		tip = null;
		owner = null;
	}

	/* The lines of the text (the line breaks of the text and wrapped at the maximal width) */
	private static List<String> wrap(GC gc, String text) {
		List<String> lines = new ArrayList<>();
		for (String paragraph : text.split("\r?\n", -1)) {
			StringBuilder line = new StringBuilder();
			for (String word : paragraph.split(" ", -1)) {
				String next = line.length() == 0 ? word : line + " " + word;
				if (line.length() > 0 && gc.textExtent(next).x > MAX_WIDTH) {
					lines.add(line.toString());
					line = new StringBuilder(word);
				} else {
					line = new StringBuilder(next);
				}
			}
			lines.add(line.toString());
		}
		return lines;
	}
}
