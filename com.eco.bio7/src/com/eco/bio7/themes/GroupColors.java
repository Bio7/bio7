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

import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

/**
 * The groups of a custom theme on macOS: a group is a native box (NSBox) which
 * draws its own tinted fill over the background color. With a custom theme the
 * native box is transparent (it keeps its layout with the title above the box)
 * and the group paints the box with the fill and the border color of the theme
 * and the title with the text color. Without a custom theme the native box is
 * drawn again. The internal Cocoa classes of SWT are called by reflection (the
 * plugin is compiled and used on all platforms; the other platforms paint the
 * group with the CSS color).
 */
final class GroupColors {

	private static final boolean COCOA = "cocoa".equals(SWT.getPlatform());
	/* A group with the paint listener of the theme */
	private static final String PAINTED = "bio7.themes.groupPainted";
	/**
	 * The data key of the fill color of a painted group (e.g., for the sliders which
	 * fill with the color of their parent).
	 */
	static final String FILL = "bio7.themes.groupFill";
	private static final int CORNER_RADIUS = 10;
	/* The original inner margin of a painted group (see padding) and the additional space */
	private static final String MARGIN = "bio7.themes.groupMargin";
	private static final int PADDING = 4;

	private static Color fill;
	private static Color border;
	private static boolean installed;

	private GroupColors() {
	}

	/* The groups created later (e.g., in a view opened later) get the colors, too */
	static void install() {
		if (installed || !COCOA) {
			return;
		}
		installed = true;
		Display.getDefault().addFilter(SWT.Skin, event -> {
			if (fill != null && event.widget instanceof Group group) {
				style(group);
			}
		});
	}

	/** Sets the colors of all groups (null for the native boxes). */
	static void apply(RGB fillColor, RGB borderColor) {
		if (!COCOA) {
			return;
		}
		install();
		Display display = Display.getDefault();
		fill = fillColor == null ? null : new Color(display, fillColor);
		border = fillColor == null ? null : new Color(display, borderColor != null ? borderColor : fillColor);
		for (Shell shell : display.getShells()) {
			styleAll(shell);
		}
	}

	private static void styleAll(Control control) {
		if (control.isDisposed()) {
			return;
		}
		if (control instanceof Group group) {
			style(group);
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				styleAll(child);
			}
		}
	}

	private static void style(Group group) {
		if (group.isDisposed()) {
			return;
		}
		group.setData(FILL, fill);
		if (fill != null && group.getData(PAINTED) == null) {
			group.setData(PAINTED, Boolean.TRUE);
			group.addListener(SWT.Paint, GroupColors::paint);
		}
		try {
			/* The native box draws nothing (also not the title) but keeps its layout */
			Object view = Control.class.getField("view").get(group);
			long id = view.getClass().getField("id").getLong(view);
			Class<?> os = Class.forName("org.eclipse.swt.internal.cocoa.OS", true, Control.class.getClassLoader());
			Method sel = os.getMethod("sel_registerName", String.class);
			Method send = os.getMethod("objc_msgSend", long.class, long.class, long.class);
			send.invoke(null, id, sel.invoke(null, "setTransparent:"), fill != null ? 1L : 0L);
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another SWT version: the native box */
		}
		padding(group);
		/* Also the children (e.g., the sliders which paint the fill of the group) */
		redrawAll(group);
	}

	/*
	 * The content of a painted box has more space at the top and the bottom (the
	 * inner margin of SWT, used for the layout and the client area): else the first
	 * row (e.g., check boxes, sliders) touches the edge of the box!
	 */
	private static void padding(Group group) {
		try {
			java.lang.reflect.Field margin = Group.class.getDeclaredField("vMargin");
			margin.setAccessible(true);
			Object original = group.getData(MARGIN);
			if (fill != null && original == null) {
				int value = margin.getInt(group);
				group.setData(MARGIN, value);
				margin.setInt(group, value + PADDING);
				group.requestLayout();
			} else if (fill == null && original instanceof Integer value) {
				margin.setInt(group, value);
				group.setData(MARGIN, null);
				group.requestLayout();
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another SWT version: the native margin */
		}
	}

	private static void redrawAll(Control control) {
		control.redraw();
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				redrawAll(child);
			}
		}
	}

	/* The box (the area of the content of the native box) and the title above */
	private static void paint(Event event) {
		if (fill == null) {
			return;
		}
		Group group = (Group) event.widget;
		GC gc = event.gc;
		Rectangle r = group.getBounds();
		String text = group.getText();
		gc.setFont(group.getFont());
		org.eclipse.swt.graphics.FontMetrics metrics = gc.getFontMetrics();
		/* The top of the content (the native layout with the space of the title) */
		int top = contentTop(group);
		if (top < 0) {
			top = text.isEmpty() ? 0 : metrics.getHeight() + 1;
		}
		gc.setAdvanced(true);
		gc.setAntialias(SWT.ON);
		gc.setBackground(fill);
		gc.fillRoundRectangle(0, top, r.width - 1, r.height - top - 1, CORNER_RADIUS, CORNER_RADIUS);
		gc.setForeground(border);
		gc.drawRoundRectangle(0, top, r.width - 1, r.height - top - 1, CORNER_RADIUS, CORNER_RADIUS);
		if (!text.isEmpty()) {
			/* The baseline of the title just above the box */
			gc.setForeground(group.getForeground());
			gc.drawText(text, 6, top - 2 - metrics.getLeading() - metrics.getAscent(), true);
		}
	}

	/* The distance of the content view from the top of the native box (not flipped) or -1 */
	private static int contentTop(Group group) {
		try {
			java.lang.reflect.Field field = Group.class.getDeclaredField("contentView");
			field.setAccessible(true);
			Object content = field.get(group);
			Object view = Control.class.getField("view").get(group);
			Object contentFrame = content.getClass().getMethod("frame").invoke(content);
			Object frame = view.getClass().getMethod("frame").invoke(view);
			Class<?> rect = contentFrame.getClass();
			double height = rect.getField("height").getDouble(frame);
			double y = rect.getField("y").getDouble(contentFrame);
			double contentHeight = rect.getField("height").getDouble(contentFrame);
			return (int) Math.round(height - (y + contentHeight));
		} catch (ReflectiveOperationException | RuntimeException e) {
			return -1;
		}
	}
}
