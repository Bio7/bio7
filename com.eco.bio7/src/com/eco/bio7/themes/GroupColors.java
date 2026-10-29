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
	/* The data key of the text color of a painted group (e.g., for the labels of the sliders) */
	static final String TEXT = "bio7.themes.groupText";
	private static final int CORNER_RADIUS = 10;
	/* The original inner margin of a painted group (see padding) and the additional space */
	private static final String MARGIN = "bio7.themes.groupMargin";
	private static final int PADDING = 4;

	private static Color fill;
	private static Color border;
	private static Color text;
	/* The groups are painted: with the colors of a custom theme or (fill null) with the color of the parent (default themes) */
	private static boolean active;
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
			if (active && event.widget instanceof Group group) {
				style(group);
			}
		});
	}

	/** Sets the colors of all groups (null for the native boxes). */
	static void apply(RGB fillColor, RGB borderColor, RGB textColor) {
		apply(fillColor, borderColor, textColor, fillColor != null);
	}

	/*
	 * The groups of the default themes: the boxes with the color of the parent
	 * (else the native box has an own tint and the widgets of the group the color of
	 * the view, e.g., in the dark theme)!
	 */
	static void applyDefault() {
		apply(null, null, null, true);
	}

	private static void apply(RGB fillColor, RGB borderColor, RGB textColor, boolean paint) {
		if (!COCOA) {
			return;
		}
		install();
		active = paint;
		Display display = Display.getDefault();
		fill = fillColor == null ? null : new Color(display, fillColor);
		border = fillColor == null ? null : new Color(display, borderColor != null ? borderColor : fillColor);
		text = fillColor == null || textColor == null ? null : new Color(display, textColor);
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
		Composite parent = group.getParent();
		group.setData(FILL, !active ? null : fill != null ? fill : parent != null ? parent.getBackground() : null);
		group.setData(TEXT, !active ? null : text != null ? text : parent != null ? parent.getForeground() : null);
		/*
		 * Painted by the parent: the paint of the group is only the area of its content
		 * (below the title of the native box), the transparent group shows the parent!
		 */
		if (active && parent != null && parent.getData(PAINTED) == null) {
			parent.setData(PAINTED, Boolean.TRUE);
			parent.addListener(SWT.Paint, GroupColors::paintGroups);
		}
		if (active && group.getData(PAINTED) == null) {
			group.setData(PAINTED, Boolean.TRUE);
			/* The parent paints the box again at the new bounds */
			group.addListener(SWT.Resize, _ -> redrawParent(group));
			group.addListener(SWT.Move, _ -> redrawParent(group));
			/* A background set later (e.g., by CSS after a theme change) would cover the box of the parent */
			group.addListener(SWT.Paint, _ -> {
				if (active && hasOwnBackground(group)) {
					group.getDisplay().asyncExec(() -> clearBackground(group));
				}
			});
		}
		if (active) {
			clearBackground(group);
			/* After the CSS of a new group (the CSS engine styles it after this filter) */
			group.getDisplay().asyncExec(() -> clearBackground(group));
		}
		try {
			/* The native box draws nothing (also not the title) but keeps its layout */
			Object view = Control.class.getField("view").get(group);
			long id = view.getClass().getField("id").getLong(view);
			Class<?> os = Class.forName("org.eclipse.swt.internal.cocoa.OS", true, Control.class.getClassLoader());
			Method sel = os.getMethod("sel_registerName", String.class);
			Method send = os.getMethod("objc_msgSend", long.class, long.class, long.class);
			send.invoke(null, id, sel.invoke(null, "setTransparent:"), active ? 1L : 0L);
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another SWT version: the native box */
		}
		padding(group);
		/* Also the children (e.g., the sliders which paint the fill of the group) and the parent (the box) */
		redrawAll(group);
		redrawParent(group);
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
			if (active && original == null) {
				int value = margin.getInt(group);
				group.setData(MARGIN, value);
				margin.setInt(group, value + PADDING);
				group.requestLayout();
			} else if (!active && original instanceof Integer value) {
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

	/* The group shows the box painted by its parent (macOS: the background of the group fills its content) */
	private static void clearBackground(Group group) {
		if (!group.isDisposed() && active && hasOwnBackground(group)) {
			group.setBackground(null);
			redrawParent(group);
		}
	}

	/* The group has an own background color (not inherited from the parent) */
	private static boolean hasOwnBackground(Group group) {
		try {
			java.lang.reflect.Field field = Control.class.getDeclaredField("background");
			field.setAccessible(true);
			return field.get(group) != null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}

	private static void redrawParent(Group group) {
		Composite parent = group.getParent();
		if (parent != null && !parent.isDisposed()) {
			Rectangle b = group.getBounds();
			parent.redraw(b.x - 2, b.y - 8, b.width + 4, b.height + 10, false);
		}
	}

	/*
	 * The boxes of the painted groups of the parent (in the coordinates of the
	 * parent): the box is the area of the content of the native box (below the
	 * title strip), the title in the text color of the theme above the box.
	 */
	private static void paintGroups(Event event) {
		if (!active) {
			return;
		}
		Composite parent = (Composite) event.widget;
		GC gc = event.gc;
		/* The default themes: the color of the parent and a border between the background and the text */
		Color background = fill != null ? fill : parent.getBackground();
		Color line = border;
		if (line == null) {
			RGB b = parent.getBackground().getRGB(), f = parent.getForeground().getRGB();
			line = new Color(parent.getDisplay(), (b.red * 4 + f.red) / 5, (b.green * 4 + f.green) / 5, (b.blue * 4 + f.blue) / 5);
		}
		for (Control child : parent.getChildren()) {
			if (!(child instanceof Group group) || !group.isVisible() || group.getData(FILL) == null) {
				continue;
			}
			/* A background set later (e.g., by the CSS of a theme change) covers the box */
			if (hasOwnBackground(group)) {
				group.getDisplay().asyncExec(() -> clearBackground(group));
			}
			Rectangle r = group.getBounds();
			int strip = contentTop(group);
			if (strip < 0) {
				strip = 0;
			}
			int top = r.y + strip;
			gc.setAdvanced(true);
			gc.setAntialias(SWT.ON);
			gc.setBackground(background);
			gc.fillRoundRectangle(r.x, top, r.width - 1, r.y + r.height - top - 1, CORNER_RADIUS, CORNER_RADIUS);
			gc.setForeground(line);
			gc.drawRoundRectangle(r.x, top, r.width - 1, r.y + r.height - top - 1, CORNER_RADIUS, CORNER_RADIUS);
			String title = group.getText();
			if (!title.isEmpty()) {
				gc.setFont(group.getFont());
				org.eclipse.swt.graphics.FontMetrics metrics = gc.getFontMetrics();
				/* The baseline of the title just above the box */
				gc.setForeground(text != null ? text : parent.getForeground());
				gc.drawText(title, r.x + 6, top - 2 - metrics.getLeading() - metrics.getAscent(), true);
			}
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
