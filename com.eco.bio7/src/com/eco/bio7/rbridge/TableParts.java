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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.graphics.Point;

/**
 * The selected cells of the table split into its columns or rows: the parts of
 * a list (R) or a dict (Python), each with a name.
 */
public final class TableParts {

	/** A part of the selection: its name and the texts of its cells (empty cells: "NA"). */
	public record Part(String name, String[] values) {
	}

	private TableParts() {
	}

	/**
	 * The selected cells by column (each selected column a part) or by row (UI
	 * thread).
	 *
	 * @param grid      the table
	 * @param byColumns true: the columns, false: the rows
	 * @param head      true: the first selected cell of a column (row) is the name
	 *                  of the part; false: the name of the column (A, B, ...) or the
	 *                  number of the row
	 */
	public static List<Part> parts(Grid grid, boolean byColumns, boolean head) {
		Map<Integer, TreeMap<Integer, String>> groups = new TreeMap<>();
		for (Point p : grid.getCellSelection()) {
			int key = byColumns ? p.x : p.y, pos = byColumns ? p.y : p.x;
			groups.computeIfAbsent(key, k -> new TreeMap<>()).put(pos, grid.getItem(p.y).getText(p.x));
		}
		List<Part> parts = new ArrayList<>();
		Set<String> used = new HashSet<>();
		for (Map.Entry<Integer, TreeMap<Integer, String>> g : groups.entrySet()) {
			List<String> cells = new ArrayList<>(g.getValue().values());
			String name = "";
			if (head && !cells.isEmpty()) {
				name = cells.remove(0).strip();
			}
			if (name.isEmpty()) {
				name = byColumns ? columnName(grid, g.getKey()) : String.valueOf(g.getKey() + 1);
			}
			String unique = name;
			for (int i = 2; !used.add(unique); i++) {
				unique = name + "_" + i;
			}
			String[] values = new String[cells.size()];
			for (int i = 0; i < values.length; i++) {
				String v = cells.get(i) == null ? "" : cells.get(i).strip();
				values[i] = v.isEmpty() ? "NA" : v;
			}
			parts.add(new Part(unique, values));
		}
		return parts;
	}

	/** A rectangular block of the selection: the names and the cells of its columns. */
	public record Block(String[] names, String[][] columns) {
		public int rows() {
			return columns.length == 0 ? 0 : columns[0].length;
		}
	}

	/**
	 * The selected cells as a block: only the columns and rows with selected
	 * cells (e.g., every second column: the columns side by side); a cell of
	 * these columns and rows which is not selected is "NA" (UI thread).
	 *
	 * @param head true: the first selected row are the names of the columns
	 */
	public static Block block(Grid grid, boolean head) {
		Point[] sel = grid.getCellSelection();
		if (sel.length == 0) {
			return new Block(new String[0], new String[0][]);
		}
		java.util.TreeSet<Integer> colSet = new java.util.TreeSet<>(), rowSet = new java.util.TreeSet<>();
		Set<Long> selected = new HashSet<>();
		for (Point p : sel) {
			colSet.add(p.x);
			rowSet.add(p.y);
			selected.add(key(p.x, p.y));
		}
		List<Integer> cols = new ArrayList<>(colSet), rows = new ArrayList<>(rowSet);
		int headRow = rows.get(0);
		if (head) {
			rows.remove(0);
		}
		String[] names = new String[cols.size()];
		String[][] columns = new String[cols.size()][rows.size()];
		Set<String> used = new HashSet<>();
		for (int c = 0; c < cols.size(); c++) {
			int col = cols.get(c);
			String name = head && selected.contains(key(col, headRow)) ? grid.getItem(headRow).getText(col).strip() : "";
			if (name.isEmpty()) {
				name = columnName(grid, col);
			}
			String unique = name;
			for (int i = 2; !used.add(unique); i++) {
				unique = name + "_" + i;
			}
			names[c] = unique;
			for (int r = 0; r < rows.size(); r++) {
				int row = rows.get(r);
				String v = selected.contains(key(col, row)) ? grid.getItem(row).getText(col) : "";
				v = v == null ? "" : v.strip();
				columns[c][r] = v.isEmpty() ? "NA" : v;
			}
		}
		return new Block(names, columns);
	}

	/**
	 * True if the first selected row looks like the names of the columns: in
	 * each column a text (not a number, logical value or date), and below it at
	 * least one column of numbers, logical values or dates (UI thread).
	 */
	public static boolean looksLikeHeader(Grid grid) {
		Block b = block(grid, false);
		if (b.rows() < 2) {
			return false;
		}
		boolean typedBelow = false;
		for (String[] col : b.columns()) {
			String first = col[0];
			if (first.equals("NA") || !type(new String[] { first }).equals("character")) {
				return false;
			}
			String[] rest = java.util.Arrays.copyOfRange(col, 1, col.length);
			typedBelow |= !type(rest).equals("character");
		}
		return typedBelow;
	}

	private static long key(int column, int row) {
		return ((long) row << 20) ^ column;
	}

	/**
	 * The type of the texts of a column (NA ignored): "numeric" (also all NA),
	 * "logical" (TRUE, FALSE, T, F, true, false), "date" (ISO dates
	 * 2026-11-17) or "character".
	 */
	public static String type(String[] values) {
		if (RNumbers.parse(values) != null) {
			return "numeric";
		}
		boolean logical = true, date = true;
		for (String v : values) {
			if (v == null || v.equals("NA")) {
				continue;
			}
			logical &= v.matches("TRUE|FALSE|T|F|true|false|True|False");
			date &= isDate(v);
		}
		return logical ? "logical" : date ? "date" : "character";
	}

	private static boolean isDate(String v) {
		if (!v.matches("\\d{4}-\\d{2}-\\d{2}")) {
			return false;
		}
		try {
			java.time.LocalDate.parse(v);
			return true;
		} catch (java.time.format.DateTimeParseException e) {
			return false;
		}
	}

	/** A name of R (make.names: invalid characters as '.', an 'X' before a digit). */
	public static String rName(String name) {
		String n = name.strip().replaceAll("[^A-Za-z0-9._]", ".");
		if (n.isEmpty() || !n.matches("[A-Za-z.].*") || n.matches("\\.\\d.*")) {
			n = "X" + n;
		}
		return n;
	}

	/** The names of the parts for a dialog (shortened). */
	public static String names(List<Part> parts) {
		StringBuilder b = new StringBuilder();
		for (Part p : parts) {
			if (b.length() > 40) {
				return b.append(", ...").toString();
			}
			b.append(b.isEmpty() ? "" : ", ").append(p.name());
		}
		return b.toString();
	}

	private static String columnName(Grid grid, int column) {
		String t = column < grid.getColumnCount() ? grid.getColumn(column).getText() : "";
		return t == null || t.isBlank() ? Bio7Grid.columnName(column) : t.strip();
	}
}
