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
 * ranges, selected with Ctrl/Cmd or Shift) and a light fill (no fill handle:
 * the table has no function to fill cells by dragging). The color is the accent color of the
 * active custom theme (the icon color), a blue for the light theme or a grey
 * for the dark theme.
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
		/*
		 * The grid paints the changed cells of a selection (and the border beside
		 * them). The color of the whole selection changes with the focus.
		 */
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
			/* The theme of the preferences if the engine has no active theme yet (e.g., at the start) */
			dark = active != null ? active.getId().contains("dark") : com.eco.bio7.util.Util.isDarkTheme();
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
			/*
			 * The dark theme: a light grey (like the grey selection of the cells), the
			 * light theme: a strong blue (the selection colors of the system are too light
			 * for a border)
			 */
			rgb = dark ? new RGB(0xA8, 0xAC, 0xB2) : new RGB(0x1E, 0x66, 0xD0);
		}
		if (color == null || !rgb.equals(colorRgb)) {
			color = new Color(display, rgb);
			colorRgb = rgb;
		}
		return color;
	}

	private static void paint(Grid grid, Event e) {
		if (grid.getCellSelectionCount() == 0) {
			return;
		}
		GC gc = e.gc;
		Rectangle area = grid.getClientArea();
		int headerHeight = grid.getHeaderVisible() ? grid.getHeaderHeight() : 0;
		int rowHeaderWidth = grid.isRowHeaderVisible() ? grid.getItemHeaderWidth() : 0;
		/* Only in the area of the cells (not over the headers) and in the painted region */
		Rectangle clip = new Rectangle(area.x + rowHeaderWidth, area.y + headerHeight, area.width - rowHeaderWidth, area.height - headerHeight).intersection(new Rectangle(e.x, e.y, e.width, e.height));
		if (clip.isEmpty()) {
			return;
		}
		/* The visible rows (with their positions) */
		int top = grid.getTopIndex(), count = grid.getItemCount();
		java.util.List<GridItem> rows = new java.util.ArrayList<>();
		java.util.List<Integer> rowY = new java.util.ArrayList<>();
		int y = area.y + headerHeight;
		for (int i = Math.max(0, top); i < count && y < area.y + area.height; i++) {
			GridItem item = grid.getItem(i);
			if (item.isVisible()) {
				rows.add(item);
				rowY.add(y);
				y += item.getHeight() + 1;
			}
		}
		/* The visible columns (with their positions) */
		int columnCount = grid.getColumnCount();
		int[] order = grid.getColumnOrder();
		int x = rows.isEmpty() ? 0 : rows.get(0).getBounds(order.length > 0 ? order[0] : 0).x;
		java.util.List<Integer> cols = new java.util.ArrayList<>(), colX = new java.util.ArrayList<>(), colW = new java.util.ArrayList<>();
		for (int index : order) {
			if (index < 0 || index >= columnCount || !grid.getColumn(index).isVisible()) {
				continue;
			}
			int w = grid.getColumn(index).getWidth();
			if (x + w > area.x + rowHeaderWidth && x < area.x + area.width) {
				cols.add(index);
				colX.add(x);
				colW.add(w);
			}
			x += w;
			if (x >= area.x + area.width) {
				break;
			}
		}
		int nr = rows.size(), nc = cols.size();
		if (nr == 0 || nc == 0) {
			return;
		}
		/* The selection of the visible cells and of the cells around them (the edges) */
		boolean[][] sel = new boolean[nr + 2][nc + 2];
		Point p = new Point(0, 0);
		for (int r = -1; r <= nr; r++) {
			int rowIndex = r < 0 ? rows.get(0).getRowIndex() - 1 : r == nr ? rows.get(nr - 1).getRowIndex() + 1 : rows.get(r).getRowIndex();
			if (rowIndex < 0 || rowIndex >= count) {
				continue;
			}
			p.y = rowIndex;
			for (int c = -1; c <= nc; c++) {
				int col = c < 0 ? previous(order, cols.get(0)) : c == nc ? next(order, cols.get(nc - 1)) : cols.get(c);
				if (col < 0) {
					continue;
				}
				p.x = col;
				sel[r + 1][c + 1] = grid.isCellSelected(p);
			}
		}
		Color color = color(grid.getDisplay());
		boolean focus = grid.isFocusControl();
		gc.setClipping(clip);
		gc.setAntialias(SWT.OFF);
		gc.setBackground(color);
		/*
		 * A cell owns its grid lines at the right and at the bottom: the fill and the
		 * border are drawn over the grid lines between the selected cells (no gaps)
		 */
		gc.setAlpha(focus ? 38 : 22);
		for (int r = 0; r < nr; r++) {
			int y0 = rowY.get(r), h = rows.get(r).getHeight() + 1;
			for (int c = 0; c < nc; c++) {
				if (sel[r + 1][c + 1]) {
					/* The cells in a row are filled together */
					int c1 = c;
					while (c1 + 1 < nc && sel[r + 1][c1 + 2]) {
						c1++;
					}
					gc.fillRectangle(colX.get(c), y0, colX.get(c1) + colW.get(c1) - colX.get(c), h);
					c = c1;
				}
			}
		}
		/* The border: the edges of the cells without a selected neighbor (2 pixels) */
		gc.setAlpha(focus ? 255 : 200);
		for (int r = 0; r < nr; r++) {
			int y0 = rowY.get(r), h = rows.get(r).getHeight() + 1;
			for (int c = 0; c < nc; c++) {
				if (!sel[r + 1][c + 1]) {
					continue;
				}
				int x0 = colX.get(c), w = colW.get(c);
				boolean up = sel[r][c + 1], down = sel[r + 2][c + 1], left = sel[r + 1][c], right = sel[r + 1][c + 2];
				if (!up) {
					gc.fillRectangle(x0, y0, w, 2);
				}
				if (!down) {
					gc.fillRectangle(x0, y0 + h - 2, w, 2);
				}
				if (!left) {
					gc.fillRectangle(x0, y0, 2, h);
				}
				if (!right) {
					gc.fillRectangle(x0 + w - 2, y0, 2, h);
				}
				/* The inner corners (a selected neighbor without the diagonal cell) */
				if (up && left && !sel[r][c]) {
					gc.fillRectangle(x0, y0, 2, 2);
				}
				if (up && right && !sel[r][c + 2]) {
					gc.fillRectangle(x0 + w - 2, y0, 2, 2);
				}
				if (down && left && !sel[r + 2][c]) {
					gc.fillRectangle(x0, y0 + h - 2, 2, 2);
				}
				if (down && right && !sel[r + 2][c + 2]) {
					gc.fillRectangle(x0 + w - 2, y0 + h - 2, 2, 2);
				}
			}
		}
		gc.setAlpha(255);
		gc.setClipping(new Rectangle(e.x, e.y, e.width, e.height));
	}

	/* The column before and after a column in the display order (-1 if none) */
	private static int previous(int[] order, int column) {
		for (int i = 0; i < order.length; i++) {
			if (order[i] == column) {
				return i > 0 ? order[i - 1] : -1;
			}
		}
		return -1;
	}

	private static int next(int[] order, int column) {
		for (int i = 0; i < order.length; i++) {
			if (order[i] == column) {
				return i + 1 < order.length ? order[i + 1] : -1;
			}
		}
		return -1;
	}
}
