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

package com.eco.bio7.rbridge;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import com.eco.bio7.themes.CustomTheme;
import com.eco.bio7.themes.ThemeStore;

/**
 * Draws the selection of a sheet of the Bio7 table like a spreadsheet (e.g.,
 * LibreOffice): a border around the selected cells (also several separate
 * ranges, selected with Ctrl/Cmd or Shift), a light fill and a handle at the
 * lower right corner of each range. The color is the accent color of the
 * active custom theme (the icon color) or a blue for the light and the dark
 * theme.
 */
public final class TableSelectionOverlay {

	private static final String INSTALLED = "bio7.table.selectionOverlay";
	private static Color color;
	private static RGB colorRgb;

	private TableSelectionOverlay() {
	}

	/** Installs the overlay on a sheet (once). */
	public static void install(Grid grid) {
		if (grid == null || grid.isDisposed() || grid.getData(INSTALLED) != null) {
			return;
		}
		grid.setData(INSTALLED, Boolean.TRUE);
		/* Called after the painting of the grid (the listener of the grid is first) */
		grid.addListener(SWT.Paint, e -> paint(grid, e));
		/* The whole border is drawn again if the selection changes */
		grid.addListener(SWT.Selection, _ -> grid.redraw());
		grid.addListener(SWT.MouseUp, _ -> grid.redraw());
		grid.addListener(SWT.KeyUp, _ -> grid.redraw());
		grid.addListener(SWT.FocusIn, _ -> grid.redraw());
		grid.addListener(SWT.FocusOut, _ -> grid.redraw());
	}

	/* The accent color of the theme (the icon color of a custom theme) or of the system */
	public static Color color(Display display) {
		RGB rgb = null;
		boolean dark = false;
		try {
			org.eclipse.e4.ui.css.swt.theme.IThemeEngine engine = ThemeStore.engine();
			org.eclipse.e4.ui.css.swt.theme.ITheme active = engine != null ? engine.getActiveTheme() : null;
			dark = active != null && active.getId().contains("dark");
			if (active != null && CustomTheme.isCustomThemeId(active.getId())) {
				CustomTheme theme = ThemeStore.find(CustomTheme.folderOf(active.getId()));
				if (theme != null) {
					rgb = theme.getColor("iconColor");
				}
			}
		} catch (RuntimeException e) {
			/* The system color */
		}
		if (rgb == null) {
			/* A strong blue (the selection colors of the systems are too light for a border) */
			rgb = dark ? new RGB(0x5A, 0xA0, 0xFF) : new RGB(0x1E, 0x66, 0xD0);
		}
		if (color == null || !rgb.equals(colorRgb)) {
			color = new Color(display, rgb);
			colorRgb = rgb;
		}
		return color;
	}

	private static void paint(Grid grid, Event e) {
		Point[] cells = grid.getCellSelection();
		if (cells == null || cells.length == 0) {
			return;
		}
		/* Only the visible rows are drawn (e.g., a selected column with many rows) */
		int top = grid.getTopIndex(), itemHeight = Math.max(1, grid.getItemHeight());
		int bottom = top + grid.getClientArea().height / itemHeight + 2;
		Set<Long> selected = new HashSet<>(cells.length * 2);
		for (Point p : cells) {
			selected.add(key(p.x, p.y));
		}
		GC gc = e.gc;
		Rectangle area = grid.getClientArea();
		int headerHeight = grid.getHeaderVisible() ? grid.getHeaderHeight() : 0;
		int rowHeaderWidth = grid.isRowHeaderVisible() ? grid.getItemHeaderWidth() : 0;
		/* Only in the area of the cells (not over the headers) */
		gc.setClipping(new Rectangle(area.x + rowHeaderWidth, area.y + headerHeight, area.width - rowHeaderWidth, area.height - headerHeight));
		Color c = color(grid.getDisplay());
		boolean focus = grid.isFocusControl();
		gc.setAntialias(SWT.OFF);
		/* The fill (light) */
		gc.setBackground(c);
		gc.setAlpha(focus ? 38 : 22);
		for (Point p : cells) {
			if (p.y < top - 1 || p.y > bottom) {
				continue;
			}
			Rectangle r = bounds(grid, p);
			if (r != null && r.intersects(e.x, e.y, e.width, e.height)) {
				gc.fillRectangle(r);
			}
		}
		/* The border: the edges of the cells without a selected neighbor */
		gc.setAlpha(focus ? 255 : 200);
		gc.setForeground(c);
		gc.setLineWidth(2);
		for (Point p : cells) {
			if (p.y < top - 1 || p.y > bottom) {
				continue;
			}
			Rectangle r = bounds(grid, p);
			if (r == null || !r.intersects(e.x - 4, e.y - 4, e.width + 8, e.height + 8)) {
				continue;
			}
			int x0 = r.x, y0 = r.y, x1 = r.x + r.width - 1, y1 = r.y + r.height - 1;
			if (!selected.contains(key(p.x, p.y - 1))) {
				gc.drawLine(x0, y0 + 1, x1, y0 + 1);
			}
			if (!selected.contains(key(p.x, p.y + 1))) {
				gc.drawLine(x0, y1, x1, y1);
			}
			if (!selected.contains(key(p.x - 1, p.y))) {
				gc.drawLine(x0 + 1, y0, x0 + 1, y1);
			}
			if (!selected.contains(key(p.x + 1, p.y))) {
				gc.drawLine(x1, y0, x1, y1);
			}
			/* The handle at the lower right corner of a range */
			if (!selected.contains(key(p.x + 1, p.y)) && !selected.contains(key(p.x, p.y + 1)) && !selected.contains(key(p.x + 1, p.y + 1))) {
				/* A square with a border of the background color */
				gc.setBackground(grid.getBackground());
				gc.fillRectangle(x1 - 4, y1 - 4, 9, 9);
				gc.setBackground(c);
				gc.fillRectangle(x1 - 3, y1 - 3, 7, 7);
			}
		}
		gc.setAlpha(255);
		gc.setLineWidth(1);
		gc.setClipping((Rectangle) null);
	}

	/* The bounds of a cell (null if the row is not visible) */
	private static Rectangle bounds(Grid grid, Point p) {
		if (p.y < 0 || p.y >= grid.getItemCount() || p.x < 0 || p.x >= grid.getColumnCount()) {
			return null;
		}
		GridItem item = grid.getItem(p.y);
		if (!item.isVisible()) {
			return null;
		}
		Rectangle r = item.getBounds(p.x);
		return r != null && r.width > 0 && r.height > 0 ? r : null;
	}

	private static long key(int column, int row) {
		return ((long) row << 20) ^ (column & 0xFFFFF);
	}
}
