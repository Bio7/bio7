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
package com.eco.bio7.themes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

/**
 * macOS 26 and later: the native lists (NSTableView) have rows of 24 points for
 * every font, a large distance between the items of a list with a small font
 * (e.g., the variables of the R-Shell and the Python-Shell). The rows get the
 * height of the font again (as on earlier macOS versions). Checked when a list
 * is painted, so it follows a change of the font. The Cocoa classes are called
 * by reflection (Bio7 compiles for all platforms).
 */
final class CompactLists {

	private static final String HOOKED = "bio7.themes.compactList";
	/* The space above and below the text */
	private static final int PADDING = 2;
	private static boolean installed;
	private static boolean failed;

	private CompactLists() {
	}

	static void install() {
		if (installed || !"cocoa".equals(SWT.getPlatform()) || macosMajor() < 26) {
			return;
		}
		installed = true;
		Display display = Display.getDefault();
		display.addFilter(SWT.Skin, com.eco.bio7.util.SafeEvents.guard("CompactLists", event -> {
			if (event.widget instanceof List list) {
				hook(list);
			}
		}));
		for (Shell shell : display.getShells()) {
			hookAll(shell);
		}
	}

	private static int macosMajor() {
		try {
			return Integer.parseInt(System.getProperty("os.version", "0").split("\\.")[0]);
		} catch (NumberFormatException e) {
			return 0;
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
		if (failed || list.isDisposed() || list.getData(HOOKED) != null) {
			return;
		}
		list.setData(HOOKED, Boolean.TRUE);
		list.addListener(SWT.Paint, com.eco.bio7.util.SafeEvents.guard("CompactLists painting", CompactLists::check));
		apply(list);
	}

	private static void check(Event event) {
		if (event.widget instanceof List list) {
			apply(list);
		}
	}

	/* The row height of the font (only if it differs: no repaint loop) */
	private static void apply(List list) {
		if (failed || list.isDisposed()) {
			return;
		}
		GC gc = new GC(list);
		int height;
		try {
			gc.setFont(list.getFont());
			height = gc.getFontMetrics().getHeight() + PADDING;
		} finally {
			gc.dispose();
		}
		if (list.getItemHeight() == height) {
			return;
		}
		try {
			Object view = Control.class.getDeclaredField("view").get(list);
			Class<?> os = Class.forName("org.eclipse.swt.internal.cocoa.OS");
			long id = view.getClass().getField("id").getLong(view);
			long selector = (long) os.getMethod("sel_registerName", String.class).invoke(null, "setRowSizeStyle:");
			/* NSTableViewRowSizeStyleCustom: the row height of the table view applies */
			os.getMethod("objc_msgSend", long.class, long.class, long.class).invoke(null, id, selector, 0L);
			view.getClass().getMethod("setRowHeight", double.class).invoke(view, (double) height);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			/* Another SWT: the native height stays (not tried again) */
			failed = true;
		}
	}
}
