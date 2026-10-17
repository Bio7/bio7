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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * The undo/redo history of a grid (a sheet of the Table view). Each grid has
 * its own history, which is disposed with the grid.
 * <ul>
 * <li>Changes of cells (text, font, colors, image) store only the changed
 * cells ({@link #recordCells(String, Collection, Runnable)}).</li>
 * <li>Changes of the structure (rows, columns, sizes) store a snapshot of the
 * whole sheet before and after ({@link #recordGrid(String, Runnable)}).</li>
 * </ul>
 * Changes which are not recorded (e.g., the transfer of data from R or the
 * import of a file into the sheet) clear the history ({@link #clear(Grid)}),
 * else an undo would restore an outdated state. The history is limited to
 * {@value #MAX_STEPS} steps and about {@value #MAX_CELLS} stored cells. All
 * methods must be called in the UI thread.
 */
public final class GridHistory {

	private static final String KEY = "com.eco.bio7.rbridge.GridHistory";
	private static final int MAX_STEPS = 100;
	private static final long MAX_CELLS = 2_000_000L;

	private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<Runnable>();

	private final Grid grid;
	private final Deque<Change> undo = new ArrayDeque<Change>();
	private final Deque<Change> redo = new ArrayDeque<Change>();
	private long storedCells;
	/* > 0 while an operation is recorded: the operation may use Bio7Grid methods which would clear the history! */
	private int recording;

	private GridHistory(Grid grid) {
		this.grid = grid;
		grid.addListener(SWT.Dispose, e -> {
			undo.clear();
			redo.clear();
			changed();
		});
	}

	/** The history of the grid (created on demand), null for a null or disposed grid. */
	public static GridHistory of(Grid grid) {
		if (grid == null || grid.isDisposed()) {
			return null;
		}
		Object history = grid.getData(KEY);
		if (history instanceof GridHistory) {
			return (GridHistory) history;
		}
		GridHistory created = new GridHistory(grid);
		grid.setData(KEY, created);
		return created;
	}

	/**
	 * Clears the history of the grid, e.g., after a change which is not recorded.
	 * Can be called from any thread.
	 */
	public static void clear(Grid grid) {
		if (grid == null) {
			return;
		}
		Display display = grid.getDisplay();
		Runnable clear = () -> {
			if (!grid.isDisposed() && grid.getData(KEY) instanceof GridHistory) {
				GridHistory history = (GridHistory) grid.getData(KEY);
				if (history.recording > 0) {
					return;
				}
				history.undo.clear();
				history.redo.clear();
				history.storedCells = 0;
				changed();
			}
		};
		if (Display.getCurrent() == display) {
			clear.run();
		} else if (!display.isDisposed()) {
			display.syncExec(clear);
		}
	}

	/** A listener which is notified when a history changes (e.g., to update the undo/redo actions). */
	public static void addListener(Runnable listener) {
		LISTENERS.add(listener);
	}

	public static void removeListener(Runnable listener) {
		LISTENERS.remove(listener);
	}

	private static void changed() {
		for (Runnable listener : LISTENERS) {
			listener.run();
		}
	}

	public boolean canUndo() {
		return !undo.isEmpty();
	}

	public boolean canRedo() {
		return !redo.isEmpty();
	}

	/** The label of the next undo step (e.g., "Paste"), null if there is none. */
	public String getUndoLabel() {
		return undo.isEmpty() ? null : undo.peek().label;
	}

	public String getRedoLabel() {
		return redo.isEmpty() ? null : redo.peek().label;
	}

	public void undo() {
		if (undo.isEmpty() || grid.isDisposed()) {
			return;
		}
		Change change = undo.pop();
		change.apply(grid, true);
		redo.push(change);
		changed();
	}

	public void redo() {
		if (redo.isEmpty() || grid.isDisposed()) {
			return;
		}
		Change change = redo.pop();
		change.apply(grid, false);
		undo.push(change);
		changed();
	}

	/**
	 * Runs an operation which changes the given cells (x = column, y = row) and
	 * records it as one step. Cells outside of the sheet are ignored.
	 */
	public void recordCells(String label, Collection<Point> cells, Runnable operation) {
		List<Point> positions = new ArrayList<Point>(new LinkedHashSet<Point>(cells));
		List<CellState> before = captureCells(positions);
		run(operation);
		if (grid.isDisposed()) {
			return;
		}
		List<CellState> after = captureCells(positions);
		CellsChange change = new CellsChange(label, positions, before, after);
		if (!change.isEmpty()) {
			add(change);
		}
	}

	/** Runs an operation which changes the structure of the sheet and records it as one step. */
	public void recordGrid(String label, Runnable operation) {
		GridState before = new GridState(grid);
		run(operation);
		if (grid.isDisposed()) {
			return;
		}
		GridState after = new GridState(grid);
		if (!before.equals(after)) {
			add(new GridChange(label, before, after));
		}
	}

	private void run(Runnable operation) {
		recording++;
		try {
			operation.run();
		} finally {
			recording--;
		}
	}

	private List<CellState> captureCells(List<Point> positions) {
		List<CellState> states = new ArrayList<CellState>(positions.size());
		for (Point p : positions) {
			states.add(p.y < grid.getItemCount() && p.x < grid.getColumnCount() ? new CellState(grid.getItem(p.y), p.x) : null);
		}
		return states;
	}

	private void add(Change change) {
		redo.clear();
		undo.push(change);
		storedCells = 0;
		for (Change c : undo) {
			storedCells += c.size();
		}
		/* Removes the oldest steps if the history is too large! */
		while (undo.size() > MAX_STEPS || (storedCells > MAX_CELLS && undo.size() > 1)) {
			storedCells -= undo.removeLast().size();
		}
		if (storedCells > MAX_CELLS) {
			/* A single step larger than the limit is not kept! */
			undo.clear();
			storedCells = 0;
		}
		changed();
	}

	/* ---- the recorded changes ---- */

	private abstract static class Change {
		final String label;

		Change(String label) {
			this.label = label;
		}

		abstract void apply(Grid grid, boolean undo);

		abstract long size();
	}

	private static final class CellsChange extends Change {
		private final List<Point> positions;
		private final List<CellState> before;
		private final List<CellState> after;

		CellsChange(String label, List<Point> positions, List<CellState> before, List<CellState> after) {
			super(label);
			/* Only the changed cells are kept! */
			this.positions = new ArrayList<Point>();
			this.before = new ArrayList<CellState>();
			this.after = new ArrayList<CellState>();
			for (int i = 0; i < positions.size(); i++) {
				if (!Objects.equals(before.get(i), after.get(i))) {
					this.positions.add(positions.get(i));
					this.before.add(before.get(i));
					this.after.add(after.get(i));
				}
			}
		}

		boolean isEmpty() {
			return positions.isEmpty();
		}

		@Override
		void apply(Grid grid, boolean undo) {
			List<CellState> states = undo ? before : after;
			for (int i = 0; i < positions.size(); i++) {
				Point p = positions.get(i);
				CellState state = states.get(i);
				if (state != null && p.y < grid.getItemCount() && p.x < grid.getColumnCount()) {
					state.apply(grid.getItem(p.y), p.x);
				}
			}
			grid.redraw();
		}

		@Override
		long size() {
			return positions.size() * 2L;
		}
	}

	private static final class GridChange extends Change {
		private final GridState before;
		private final GridState after;

		GridChange(String label, GridState before, GridState after) {
			super(label);
			this.before = before;
			this.after = after;
		}

		@Override
		void apply(Grid grid, boolean undo) {
			(undo ? before : after).apply(grid);
		}

		@Override
		long size() {
			return before.size() + after.size();
		}
	}

	/* The content and format of a cell. Unset formats are null (the default of the grid)! */
	private static final class CellState {
		final String text;
		final Font font;
		final Color foreground;
		final Color background;
		final Image image;

		CellState(GridItem item, int column) {
			text = item.getText(column);
			font = item.getFont(column);
			foreground = item.getForeground(column);
			background = item.getBackground(column);
			image = item.getImage(column);
		}

		boolean isEmpty() {
			return (text == null || text.isEmpty()) && font == null && foreground == null && background == null && image == null;
		}

		/*
		 * Only the properties which differ are set: e.g., the first setImage call (even with
		 * null) of a grid computes the height of all rows again!
		 */
		void apply(GridItem item, int column) {
			String t = text == null ? "" : text;
			if (!t.equals(item.getText(column))) {
				item.setText(column, t);
			}
			Font f = font == null || font.isDisposed() ? null : font;
			if (item.getFont(column) != f) {
				item.setFont(column, f);
			}
			Color fg = foreground == null || foreground.isDisposed() ? null : foreground;
			if (item.getForeground(column) != fg) {
				item.setForeground(column, fg);
			}
			Color bg = background == null || background.isDisposed() ? null : background;
			if (item.getBackground(column) != bg) {
				item.setBackground(column, bg);
			}
			Image im = image == null || image.isDisposed() ? null : image;
			if (item.getImage(column) != im) {
				item.setImage(column, im);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof CellState)) {
				return false;
			}
			CellState c = (CellState) o;
			return Objects.equals(text, c.text) && font == c.font && foreground == c.foreground && background == c.background && image == c.image;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(text);
		}
	}

	/* A snapshot of a whole sheet: columns (title, width), rows (height) and cells (empty cells are null)! */
	private static final class GridState {
		final String[] titles;
		final int[] widths;
		final int[] heights;
		final CellState[][] cells;

		GridState(Grid grid) {
			int columns = grid.getColumnCount();
			int rows = grid.getItemCount();
			titles = new String[columns];
			widths = new int[columns];
			for (int c = 0; c < columns; c++) {
				GridColumn column = grid.getColumn(c);
				titles[c] = column.getText();
				widths[c] = column.getWidth();
			}
			heights = new int[rows];
			cells = new CellState[rows][];
			for (int r = 0; r < rows; r++) {
				GridItem item = grid.getItem(r);
				heights[r] = item.getHeight();
				CellState[] row = null;
				for (int c = 0; c < columns; c++) {
					CellState cell = new CellState(item, c);
					if (!cell.isEmpty()) {
						if (row == null) {
							row = new CellState[columns];
						}
						row[c] = cell;
					}
				}
				cells[r] = row;
			}
		}

		long size() {
			long n = titles.length + heights.length;
			for (CellState[] row : cells) {
				if (row != null) {
					for (CellState cell : row) {
						if (cell != null) {
							n++;
						}
					}
				}
			}
			return n;
		}

		void apply(Grid grid) {
			grid.setRedraw(false);
			try {
				while (grid.getColumnCount() > titles.length) {
					grid.getColumn(grid.getColumnCount() - 1).dispose();
				}
				while (grid.getColumnCount() < titles.length) {
					new GridColumn(grid, SWT.NONE);
				}
				for (int c = 0; c < titles.length; c++) {
					GridColumn column = grid.getColumn(c);
					column.setText(titles[c] == null ? "" : titles[c]);
					column.setWidth(widths[c]);
				}
				while (grid.getItemCount() > heights.length) {
					grid.remove(grid.getItemCount() - 1);
				}
				while (grid.getItemCount() < heights.length) {
					new GridItem(grid, SWT.NONE);
				}
				for (int r = 0; r < heights.length; r++) {
					GridItem item = grid.getItem(r);
					if (item.getHeight() != heights[r]) {
						item.setHeight(heights[r]);
					}
					CellState[] row = cells[r];
					for (int c = 0; c < titles.length; c++) {
						CellState cell = row == null ? null : row[c];
						if (cell != null) {
							cell.apply(item, c);
						} else {
							clearCell(item, c);
						}
					}
				}
			} finally {
				grid.setRedraw(true);
			}
			grid.layout();
			grid.redraw();
		}

		private static void clearCell(GridItem item, int column) {
			if (!item.getText(column).isEmpty()) {
				item.setText(column, "");
			}
			if (item.getFont(column) != null) {
				item.setFont(column, null);
			}
			if (item.getForeground(column) != null) {
				item.setForeground(column, null);
			}
			if (item.getBackground(column) != null) {
				item.setBackground(column, null);
			}
			if (item.getImage(column) != null) {
				item.setImage(column, null);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof GridState)) {
				return false;
			}
			GridState s = (GridState) o;
			if (!java.util.Arrays.equals(titles, s.titles) || !java.util.Arrays.equals(widths, s.widths) || !java.util.Arrays.equals(heights, s.heights)) {
				return false;
			}
			for (int r = 0; r < cells.length; r++) {
				if (!java.util.Arrays.equals(cells[r], s.cells[r])) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			return java.util.Arrays.hashCode(heights);
		}
	}
}
