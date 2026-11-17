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

import java.util.List;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;

/**
 * Shows cells (rows of strings) in a new sheet of the Bio7 table: the
 * transfers of the R-Shell and the Python-Shell.
 */
public final class TableTransfer {

	private TableTransfer() {
	}

	/**
	 * Shows the rows in a new sheet (UI thread).
	 *
	 * @return null or an error message
	 */
	public static String show(String name, List<String[]> rows) {
		if (rows == null || rows.isEmpty()) {
			return name + ": no data for the table";
		}
		int cols = rows.stream().mapToInt(r -> r.length).max().orElse(1);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(RTable.ID);
		} catch (Exception e) {
			return "The table could not be opened: " + e.getMessage();
		}
		if (RTable.getTabFolder() == null) {
			return "The table could not be opened";
		}
		Grid grid = new Spread().spread(RTable.getTabFolder(), cols, rows.size(), name);
		RTable.setGrid(grid);
		if (grid == null) {
			return "The sheet could not be created";
		}
		for (int i = grid.getColumnCount(); i < cols; i++) {
			GridColumn column = new GridColumn(grid, SWT.NONE);
			column.setText(Bio7Grid.columnName(i));
			column.setWidth(50);
		}
		for (int i = grid.getItemCount(); i < rows.size(); i++) {
			new GridItem(grid, SWT.NONE);
		}
		for (int r = 0; r < rows.size(); r++) {
			String[] row = rows.get(r);
			for (int c = 0; c < row.length; c++) {
				grid.getItem(r).setText(c, row[c] == null ? "" : row[c]);
			}
		}
		return null;
	}

	/**
	 * The R code for several objects (mode columns, rows: one column or row each)
	 * or the elements of a list, data frame or matrix (list_columns, list_rows):
	 * a character vector with the number of rows and columns, then the cells
	 * (column by column), the names as header; "ERROR" and the message for an error.
	 */
	public static String rCode(String[] names, String mode) {
		String objs;
		if (mode.startsWith("list")) {
			objs = "local({ x <- " + quote(names[0]) + "\n"
					+ "  if (is.data.frame(x) || is.list(x)) as.list(x)\n"
					+ "  else if (is.matrix(x)) { l <- lapply(seq_len(ncol(x)), function(j) x[, j]); names(l) <- colnames(x); l }\n"
					+ "  else stop(\"" + names[0].replace("\\", "").replace("\"", "") + " is not a list, data frame or matrix\") })";
		} else {
			StringBuilder b = new StringBuilder("list(");
			for (int i = 0; i < names.length; i++) {
				b.append(i > 0 ? ", " : "").append(quote(names[i])).append(" = ").append(quote(names[i]));
			}
			objs = b.append(")").toString();
		}
		return "local({\n"
				+ "  cv <- function(v) {\n"
				+ "    if (is.null(v)) return(character(0))\n"
				+ "    if (is.data.frame(v)) v <- unlist(lapply(v, function(col) if (is.factor(col)) as.character(col) else col), use.names = FALSE)\n"
				+ "    if (is.factor(v)) v <- as.character(v)\n"
				+ "    if (is.list(v)) v <- unlist(v, use.names = FALSE)\n"
				+ "    s <- as.character(as.vector(v))\n"
				+ "    s[is.na(s)] <- \"NA\"\n"
				+ "    s\n"
				+ "  }\n"
				+ "  m <- tryCatch({\n"
				+ "    objs <- " + objs + "\n"
				+ "    nm <- names(objs)\n"
				+ "    if (is.null(nm)) nm <- rep(\"\", length(objs))\n"
				+ "    nm[is.na(nm) | nm == \"\"] <- paste0(\"V\", which(is.na(nm) | nm == \"\"))\n"
				+ "    vals <- lapply(objs, cv)\n"
				+ "    n <- max(c(0L, lengths(vals)))\n"
				+ "    cells <- if (n == 0) matrix(\"\", 0, length(vals)) else matrix(vapply(vals, function(v) c(v, rep(\"\", n - length(v))), character(n)), nrow = n)\n"
				+ "    " + (mode.endsWith("rows") ? "cbind(nm, t(cells))" : "rbind(nm, cells)") + "\n"
				+ "  }, error = function(e) e)\n"
				+ "  if (inherits(m, \"error\")) c(\"ERROR\", conditionMessage(m)) else c(as.character(dim(m)), as.vector(m))\n"
				+ "})";
	}

	/** The cells of the result of {@link #rCode} (rows of strings); an error: an exception with the message of R. */
	public static List<String[]> rRows(String[] result) {
		if (result == null || result.length < 2) {
			throw new IllegalStateException("No data");
		}
		if (result[0].equals("ERROR")) {
			throw new IllegalStateException(result[1]);
		}
		int nrow = Integer.parseInt(result[0]), ncol = Integer.parseInt(result[1]);
		List<String[]> rows = new java.util.ArrayList<>();
		for (int r = 0; r < nrow; r++) {
			String[] row = new String[ncol];
			for (int c = 0; c < ncol; c++) {
				row[c] = result[2 + c * nrow + r];
			}
			rows.add(row);
		}
		return rows;
	}

	/* A name of R (with backticks if it is not syntactic) */
	private static String quote(String name) {
		return name.matches("[A-Za-z.][A-Za-z0-9._]*") && !name.matches("\\.\\d.*") ? name : "`" + name.replace("`", "\\`") + "`";
	}

	/** The name of a sheet for several objects (e.g., "x, y, z"). */
	public static String sheetName(List<String> names) {
		String s = String.join(", ", names);
		return s.length() > 40 ? s.substring(0, 37) + "..." : s;
	}
}
