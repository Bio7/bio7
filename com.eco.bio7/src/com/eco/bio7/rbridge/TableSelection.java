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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * The selection of the active sheet of the Bio7 table: the selected ranges
 * (also several separate ranges, selected with Ctrl/Cmd or Shift like in a
 * spreadsheet) and methods to select ranges. The columns and rows are 0
 * based. The selection is displayed with a border (see TableSelectionOverlay).
 */
public final class TableSelection {

	private TableSelection() {
	}

	/* Runs in the user interface thread with the active sheet (the default if no sheet is available) */
	private static <T> T withGrid(java.util.function.Function<Grid, T> action, Supplier<T> fallback) {
		Object[] result = new Object[1];
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			Grid grid = RTable.getGrid();
			result[0] = grid != null && !grid.isDisposed() ? action.apply(grid) : fallback.get();
		});
		@SuppressWarnings("unchecked")
		T t = (T) result[0];
		return t;
	}

	/**
	 * Returns the selected cells of the active sheet.
	 *
	 * @return the cells (x = column, y = row)
	 */
	public static Point[] cells() {
		return withGrid(grid -> grid.getCellSelection(), () -> new Point[0]);
	}

	/** Returns the number of the selected cells. */
	public static int count() {
		return cells().length;
	}

	/**
	 * Returns the selected ranges of the active sheet as rectangles of cells:
	 * fromColumn, fromRow, toColumn, toRow (inclusive). A selection of another
	 * shape is divided into rectangles.
	 *
	 * @return the ranges
	 */
	public static List<int[]> ranges() {
		return ranges(cells());
	}

	/** Divides cells into rectangles (rows of runs which are extended downwards). */
	public static List<int[]> ranges(Point[] cells) {
		Set<Long> selected = new HashSet<>();
		for (Point p : cells) {
			selected.add(key(p.x, p.y));
		}
		Point[] sorted = cells.clone();
		Arrays.sort(sorted, (a, b) -> a.y != b.y ? Integer.compare(a.y, b.y) : Integer.compare(a.x, b.x));
		Set<Long> used = new HashSet<>();
		List<int[]> ranges = new ArrayList<>();
		for (Point p : sorted) {
			if (used.contains(key(p.x, p.y))) {
				continue;
			}
			/* The run to the right */
			int x1 = p.x;
			while (selected.contains(key(x1 + 1, p.y)) && !used.contains(key(x1 + 1, p.y))) {
				x1++;
			}
			/* Downwards while the whole run is selected */
			int y1 = p.y;
			boolean full = true;
			while (full) {
				for (int x = p.x; x <= x1; x++) {
					if (!selected.contains(key(x, y1 + 1)) || used.contains(key(x, y1 + 1))) {
						full = false;
						break;
					}
				}
				if (full) {
					y1++;
				}
			}
			for (int y = p.y; y <= y1; y++) {
				for (int x = p.x; x <= x1; x++) {
					used.add(key(x, y));
				}
			}
			ranges.add(new int[] { p.x, p.y, x1, y1 });
		}
		return ranges;
	}

	/** Selects a range (the former selection is removed). */
	public static void select(int fromColumn, int fromRow, int toColumn, int toRow) {
		range(fromColumn, fromRow, toColumn, toRow, false);
	}

	/** Adds a range to the selection (like a selection with Ctrl/Cmd). */
	public static void add(int fromColumn, int fromRow, int toColumn, int toRow) {
		range(fromColumn, fromRow, toColumn, toRow, true);
	}

	/** Selects whole columns (all rows with values). */
	public static void selectColumns(int... columns) {
		withGrid(grid -> {
			int rows = lastRow(grid) + 1;
			List<Point> cells = new ArrayList<>();
			for (int c : columns) {
				if (c >= 0 && c < grid.getColumnCount()) {
					for (int r = 0; r < rows; r++) {
						cells.add(new Point(c, r));
					}
				}
			}
			grid.setCellSelection(cells.toArray(new Point[0]));
			grid.redraw();
			return null;
		}, () -> null);
	}

	/** Removes the selection. */
	public static void clear() {
		withGrid(grid -> {
			grid.deselectAllCells();
			grid.redraw();
			return null;
		}, () -> null);
	}

	private static void range(int fromColumn, int fromRow, int toColumn, int toRow, boolean add) {
		withGrid(grid -> {
			int c0 = Math.max(0, Math.min(fromColumn, toColumn)), c1 = Math.min(grid.getColumnCount() - 1, Math.max(fromColumn, toColumn));
			int r0 = Math.max(0, Math.min(fromRow, toRow)), r1 = Math.min(grid.getItemCount() - 1, Math.max(fromRow, toRow));
			List<Point> cells = new ArrayList<>();
			if (add) {
				cells.addAll(Arrays.asList(grid.getCellSelection()));
			}
			for (int r = r0; r <= r1; r++) {
				for (int c = c0; c <= c1; c++) {
					cells.add(new Point(c, r));
				}
			}
			grid.setCellSelection(cells.toArray(new Point[0]));
			grid.redraw();
			return null;
		}, () -> null);
	}

	/* The last row with a value */
	private static int lastRow(Grid grid) {
		for (int r = grid.getItemCount() - 1; r >= 0; r--) {
			for (int c = 0; c < grid.getColumnCount(); c++) {
				String t = grid.getItem(r).getText(c);
				if (t != null && !t.isBlank()) {
					return r;
				}
			}
		}
		return -1;
	}

	private static long key(int column, int row) {
		return ((long) row << 20) ^ (column & 0xFFFFF);
	}
}
