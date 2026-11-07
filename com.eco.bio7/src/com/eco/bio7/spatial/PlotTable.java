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

package com.eco.bio7.spatial;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.rbridge.RTable;

/**
 * The data of the active sheet of the Bio7 table for plots (see Plot3D): the
 * columns as numbers or texts and a dialog to select the columns of the roles
 * of a plot (e.g., X, Y, Z and an optional group). If cells are selected in
 * the table (also several ranges with Ctrl/Cmd or Shift) the dialog uses only
 * the selected cells (an option of the dialog, see fromSelection).
 *
 * <pre>
 * PlotTable.Selection s = PlotTable.select("Scatter Plot", "Select the columns", "X", "Y", "Z", "Group?");
 * if (s != null) {
 * 	double[] x = s.numbers(0), y = s.numbers(1), z = s.numbers(2);
 * 	String[] group = s.has(3) ? s.texts(3) : null;
 * }
 * </pre>
 */
public final class PlotTable {

	private final String sheet;
	private final String[] header;
	private final String[][] values;
	private boolean firstRowNames;

	private PlotTable(String sheet, String[] header, String[][] values) {
		this.sheet = sheet;
		this.header = header;
		this.values = values;
		this.firstRowNames = detectNames();
	}

	/**
	 * Reads the active sheet of the Bio7 table (in the user interface thread).
	 *
	 * @return the data or null if no table is available
	 */
	public static PlotTable fromActiveSheet() {
		PlotTable[] result = new PlotTable[1];
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> result[0] = fromGrid(RTable.getGrid()));
		return result[0];
	}

	/* Reads a sheet of the Bio7 table (in the user interface thread) or null */
	static PlotTable fromGrid(Grid grid) {
		PlotTable[] result = new PlotTable[1];
		{
			if (grid == null || grid.isDisposed()) {
				return null;
			}
			int columns = grid.getColumnCount(), rows = grid.getItemCount();
			String[] header = new String[columns];
			for (int c = 0; c < columns; c++) {
				String text = grid.getColumn(c).getText();
				header[c] = text != null && !text.isBlank() ? text : "Column " + (c + 1);
			}
			/* The rows with values (empty rows at the end are ignored) */
			List<String[]> list = new ArrayList<>();
			int last = -1;
			for (int r = 0; r < rows; r++) {
				String[] row = new String[columns];
				boolean empty = true;
				for (int c = 0; c < columns; c++) {
					row[c] = grid.getItem(r).getText(c);
					empty &= row[c] == null || row[c].isBlank();
				}
				list.add(row);
				if (!empty) {
					last = r;
				}
			}
			String name = "";
			if (RTable.getTabFolder() != null && RTable.getTabFolder().getSelection() != null) {
				name = RTable.getTabFolder().getSelection().getText();
			}
			result[0] = new PlotTable(name, header, list.subList(0, last + 1).toArray(new String[0][]));
		}
		return result[0];
	}

	/**
	 * Reads the selected cells of the active sheet of the Bio7 table (also
	 * several separate ranges, selected with Ctrl/Cmd or Shift): the columns with
	 * a selected cell and the rows with a selected cell (not selected cells are
	 * empty).
	 * 
	 * @return the data or null if no table or no cell is selected
	 */
	public static PlotTable fromSelection() {
		PlotTable[] result = new PlotTable[1];
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			Grid grid = RTable.getGrid();
			if (grid == null || grid.isDisposed()) {
				return;
			}
			org.eclipse.swt.graphics.Point[] cells = grid.getCellSelection();
			if (cells.length == 0) {
				return;
			}
			java.util.TreeSet<Integer> columns = new java.util.TreeSet<>(), rows = new java.util.TreeSet<>();
			java.util.Set<Long> selected = new java.util.HashSet<>();
			for (org.eclipse.swt.graphics.Point p : cells) {
				columns.add(p.x);
				rows.add(p.y);
				selected.add(((long) p.y << 20) ^ p.x);
			}
			Integer[] cs = columns.toArray(new Integer[0]), rs = rows.toArray(new Integer[0]);
			String[] header = new String[cs.length];
			for (int c = 0; c < cs.length; c++) {
				String text = grid.getColumn(cs[c]).getText();
				header[c] = text != null && !text.isBlank() ? text : "Column " + (cs[c] + 1);
			}
			String[][] values = new String[rs.length][cs.length];
			for (int r = 0; r < rs.length; r++) {
				for (int c = 0; c < cs.length; c++) {
					values[r][c] = selected.contains(((long) rs[r] << 20) ^ cs[c]) ? grid.getItem(rs[r]).getText(cs[c]) : "";
				}
			}
			String name = "";
			if (RTable.getTabFolder() != null && RTable.getTabFolder().getSelection() != null) {
				name = RTable.getTabFolder().getSelection().getText();
			}
			result[0] = new PlotTable(name + " (selection)", header, values);
		});
		return result[0];
	}

	/* The first row contains names if a column has a text in the first row and numbers below */
	private boolean detectNames() {
		if (values.length < 2) {
			return false;
		}
		for (int c = 0; c < header.length; c++) {
			if (!isNumber(values[0][c]) && values[0][c] != null && !values[0][c].isBlank() && isNumber(values[1][c])) {
				return true;
			}
		}
		return false;
	}

	private static boolean isNumber(String s) {
		return Double.isFinite(parse(s));
	}

	/* A number (also with a decimal comma) or NaN */
	static double parse(String s) {
		if (s == null) {
			return Double.NaN;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return Double.NaN;
		}
		try {
			return Double.parseDouble(t);
		} catch (NumberFormatException e) {
			try {
				return Double.parseDouble(t.replace(',', '.'));
			} catch (NumberFormatException e2) {
				return Double.NaN;
			}
		}
	}

	/** The name of the sheet. */
	public String sheet() {
		return sheet;
	}

	/** True if the first row contains the names of the columns (detected, see setFirstRowNames). */
	public boolean isFirstRowNames() {
		return firstRowNames;
	}

	/** Sets if the first row contains the names of the columns. */
	public void setFirstRowNames(boolean firstRowNames) {
		this.firstRowNames = firstRowNames;
	}

	/** The number of the columns. */
	public int columns() {
		return header.length;
	}

	/** The number of the data rows (without a row of names). */
	public int rows() {
		return Math.max(0, values.length - (firstRowNames ? 1 : 0));
	}

	/** The names of the columns (the first row or the titles of the columns). */
	public String[] columnNames() {
		String[] names = new String[header.length];
		for (int c = 0; c < header.length; c++) {
			String first = firstRowNames && values.length > 0 ? values[0][c] : null;
			names[c] = first != null && !first.isBlank() ? first.trim() : header[c];
		}
		return names;
	}

	/** The values of a column as numbers (NaN for empty cells and texts). */
	public double[] numbers(int column) {
		int start = firstRowNames ? 1 : 0;
		double[] v = new double[rows()];
		for (int r = 0; r < v.length; r++) {
			v[r] = parse(values[r + start][column]);
		}
		return v;
	}

	/** The values of a column as texts. */
	public String[] texts(int column) {
		int start = firstRowNames ? 1 : 0;
		String[] v = new String[rows()];
		for (int r = 0; r < v.length; r++) {
			String s = values[r + start][column];
			v[r] = s != null ? s.trim() : "";
		}
		return v;
	}

	/** The share of the numbers in a column (0 - 1). */
	public double numericShare(int column) {
		double[] v = numbers(column);
		int n = 0;
		for (double d : v) {
			if (Double.isFinite(d)) {
				n++;
			}
		}
		return v.length > 0 ? n / (double) v.length : 0;
	}

	/* ----------------------------- The selection ----------------------------- */

	/** The selected columns of the roles of a plot. */
	public record Selection(PlotTable table, String[] roles, int[] columns, Options options) {

		/**
		 * Applies the settings of the dialog to a plot: the title, the axis titles,
		 * the logarithmic axes and the export action (the color map see colorMap).
		 * 
		 * @param plot the plot
		 * @return the plot
		 */
		public Plot3D apply(Plot3D plot) {
			if (options.title() != null && !options.title().isBlank()) {
				plot.title(options.title());
			}
			if (options.axisTitles() != null) {
				String[] t = options.axisTitles();
				plot.axisTitles(blank(t[0]), blank(t[1]), blank(t[2]));
				boolean[] log = options.logAxes();
				plot.logAxes(log[0], log[1], log[2]);
			}
			plot.exportAction(options.exportAction());
			return plot;
		}

		/** The color map of the dialog (e.g., PlotLegend.VIRIDIS). */
		public int[] colorMap() {
			return options.colorMap();
		}

		/** The title of the dialog (lines separated with \n). */
		public String title() {
			return options.title();
		}

		private static String blank(String s) {
			return s == null || s.isBlank() ? null : s;
		}

		/** True if a column is selected for the role (an optional role can be empty). */
		public boolean has(int role) {
			return columns[role] >= 0;
		}

		/** The numbers of the column of the role. */
		public double[] numbers(int role) {
			return table.numbers(columns[role]);
		}

		/** The texts of the column of the role. */
		public String[] texts(int role) {
			return table.texts(columns[role]);
		}

		/** The name of the column of the role (e.g., for the title of an axis). */
		public String name(int role) {
			return has(role) ? table.columnNames()[columns[role]] : null;
		}
	}

	/**
	 * The settings of the plot of the dialog: the title, the axis titles and the
	 * logarithmic axes (null for a plot without axes, e.g., a pie chart), the
	 * color map and the export action.
	 */
	public record Options(String title, String[] axisTitles, boolean[] logAxes, int[] colorMap, boolean exportAction) {
	}

	/* The color maps of the dialog */
	private static final String[] COLOR_MAP_NAMES = { "Viridis", "Rainbow", "Diverging", "Terrain" };
	private static final int[][] COLOR_MAPS = { PlotLegend.VIRIDIS, PlotLegend.RAINBOW, PlotLegend.DIVERGING, PlotLegend.TERRAIN };

	/**
	 * Opens a dialog to select the data of the roles of a plot in the active sheet
	 * of the Bio7 table, like the data ranges of a chart in LibreOffice: each role
	 * has a range of cells (e.g., B2:B51, several ranges separated by ';'), typed,
	 * chosen as a column or selected in the table with the button "Select" (the
	 * dialog is reduced to a bar and the selection of the table is taken over).
	 * The dialog doesn't block the table. A role with a '?' at the end is optional
	 * (e.g., "Group?"). The dialog has the settings of the plot, too: the title,
	 * the titles of the axes (with the names of the first three roles, not for
	 * less than three roles, e.g., a pie chart), logarithmic axes, the color map
	 * and the export action (see Selection.apply). If cells are selected in the
	 * table when the dialog is opened, the selected columns are the default ranges.
	 *
	 * @param title       the title of the dialog (e.g., the plot)
	 * @param description a description (or null)
	 * @param roles       the roles (e.g., "X", "Y", "Z", "Group?")
	 * @return the selection or null (canceled or no table data)
	 */
	public static Selection select(String title, String description, String... roles) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		Selection[] result = new Selection[1];
		display.syncExec(() -> {
			Grid grid = RTable.getGrid();
			PlotTable table = fromGrid(grid);
			if (table == null || table.columns() == 0 || table.values.length == 0) {
				org.eclipse.jface.dialogs.MessageDialog.openInformation(display.getActiveShell(), title,
						"The Bio7 table has no data. Open the table (Window - Show View - Bio7 Table) and load or paste the data into a sheet.");
				return;
			}
			result[0] = open(display, grid, table, title, description, roles);
		});
		return result[0];
	}

	/*
	 * The default columns of the roles: a role of texts (e.g., "Group", "Labels",
	 * "Name") the next unused text column, the other roles the next unused numeric
	 * column (else the next unused column), an optional role none (-1).
	 */
	static int[] defaultColumns(PlotTable table, String[] roles, boolean[] optional) {
		int n = table.columns();
		boolean[] numeric = new boolean[n], used = new boolean[n];
		for (int c = 0; c < n; c++) {
			numeric[c] = table.numericShare(c) > 0.5;
		}
		int[] result = new int[roles.length];
		for (int i = 0; i < roles.length; i++) {
			if (optional[i]) {
				result[i] = -1;
				continue;
			}
			boolean text = roles[i].toLowerCase().matches(".*(label|group|name|categor|class|species|text).*");
			int pick = -1;
			for (int c = 0; c < n && pick < 0; c++) {
				if (!used[c] && numeric[c] != text) {
					pick = c;
				}
			}
			for (int c = 0; c < n && pick < 0; c++) {
				if (!used[c]) {
					pick = c;
				}
			}
			result[i] = Math.max(0, pick);
			if (pick >= 0) {
				used[pick] = true;
			}
		}
		return result;
	}

	/* ----------------------------- Ranges of cells ----------------------------- */

	/** The name of a column like a spreadsheet (0 = A, 25 = Z, 26 = AA). */
	public static String columnLetters(int column) {
		StringBuilder b = new StringBuilder();
		int c = column + 1;
		while (c > 0) {
			int r = (c - 1) % 26;
			b.insert(0, (char) ('A' + r));
			c = (c - 1) / 26;
		}
		return b.toString();
	}

	/**
	 * Formats ranges of cells {fromColumn, fromRow, toColumn, toRow} (0 based) like
	 * a spreadsheet, e.g., "B2:B51; D2:D51".
	 */
	public static String formatRanges(List<int[]> ranges) {
		StringBuilder b = new StringBuilder();
		for (int[] r : ranges) {
			if (b.length() > 0) {
				b.append("; ");
			}
			b.append(columnLetters(r[0])).append(r[1] + 1);
			if (r[0] != r[2] || r[1] != r[3]) {
				b.append(':').append(columnLetters(r[2])).append(r[3] + 1);
			}
		}
		return b.toString();
	}

	/**
	 * Parses ranges of cells like a spreadsheet: "B2:B51", "B2", "B:B" or "B" (the
	 * whole column), several separated by ';' (a sheet name before '.' or '!' and
	 * '$' are ignored).
	 *
	 * @param text the ranges
	 * @param rows the number of the rows of the sheet (for whole columns)
	 * @return the ranges {fromColumn, fromRow, toColumn, toRow} (0 based)
	 * @throws IllegalArgumentException if the text is not a range
	 */
	public static List<int[]> parseRanges(String text, int rows) {
		List<int[]> list = new ArrayList<>();
		for (String part : text.split(";")) {
			String t = part.trim().replace("$", "").toUpperCase(java.util.Locale.ROOT);
			if (t.isEmpty()) {
				continue;
			}
			int sheet = Math.max(t.lastIndexOf('.'), t.lastIndexOf('!'));
			if (sheet >= 0) {
				t = t.substring(sheet + 1);
			}
			String[] ends = t.split(":", -1);
			if (ends.length > 2) {
				throw new IllegalArgumentException("Not a range: " + part.trim());
			}
			int[] a = cell(ends[0], 0, part), b = ends.length == 2 ? cell(ends[1], Math.max(0, rows - 1), part) : null;
			if (b == null) {
				/* One cell or a whole column */
				b = new int[] { a[0], a[2] == 1 ? a[1] : Math.max(0, rows - 1) };
			}
			list.add(new int[] { Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[0], b[0]), Math.max(a[1], b[1]) });
		}
		return list;
	}

	/* A cell "B12" or a column "B": {column, row, has a row} */
	private static int[] cell(String text, int defaultRow, String part) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("([A-Z]+)(\\d*)").matcher(text.trim());
		if (!m.matches()) {
			throw new IllegalArgumentException("Not a range: " + part.trim());
		}
		int column = 0;
		for (char ch : m.group(1).toCharArray()) {
			column = column * 26 + (ch - 'A' + 1);
		}
		boolean row = !m.group(2).isEmpty();
		int r = row ? Integer.parseInt(m.group(2)) - 1 : defaultRow;
		if (r < 0) {
			throw new IllegalArgumentException("Not a range: " + part.trim());
		}
		return new int[] { column - 1, r, row ? 1 : 0 };
	}

	/* The texts of the cells of ranges (column by column, row by row), without the row of the names */
	private String[] cells(List<int[]> ranges, boolean namesRow) {
		List<String> out = new ArrayList<>();
		for (int[] r : ranges) {
			for (int c = r[0]; c <= r[2]; c++) {
				for (int row = r[1]; row <= r[3]; row++) {
					if (namesRow && row == 0) {
						continue;
					}
					out.add(row < values.length && c < header.length && values[row][c] != null ? values[row][c].trim() : "");
				}
			}
		}
		return out.toArray(new String[0]);
	}

	/* The name of the data of ranges: the first row of the first column (names) or the title of the column */
	private String rangeName(List<int[]> ranges, boolean namesRow) {
		if (ranges.isEmpty() || ranges.get(0)[0] >= header.length) {
			return null;
		}
		int c = ranges.get(0)[0];
		if (namesRow && values.length > 0 && values[0][c] != null && !values[0][c].isBlank()) {
			return values[0][c].trim();
		}
		return header[c];
	}

	/* The data range of a column (without the row of the names) */
	private String columnRange(int column, boolean namesRow) {
		int first = namesRow ? 1 : 0, last = Math.max(first, values.length - 1);
		return formatRanges(List.of(new int[] { column, first, column, last }));
	}

	private static Selection open(Display display, Grid grid, PlotTable sheet, String title, String description, String[] roles) {
		/*
		 * Not modal: the cells can be selected in the table while the dialog is open
		 * (the dialog stays in front of the workbench)
		 */
		Shell parent = display.getActiveShell();
		int style = SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MODELESS;
		Shell shell = parent != null ? new Shell(parent, style) : new Shell(display, style);
		shell.setText(title);
		shell.setData("org.eclipse.e4.ui.css.CssClassName", com.eco.bio7.themes.CustomTheme.DIALOG_CLASS);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 12;
		layout.marginHeight = 12;
		layout.verticalSpacing = 8;
		shell.setLayout(layout);
		Label info = new Label(shell, SWT.WRAP);
		info.setText((description != null ? description + "\n" : "") + "Sheet: " + sheet.sheet() + " (" + sheet.columns()
				+ " columns). The data of each role is a range of cells: type it (e.g., B2:B51; the columns A, B, C, ... are the columns C 1, C 2, C 3, ... of the table), choose a column or select the cells in the table (Select).");
		GridData infoData = new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1);
		infoData.widthHint = 460;
		info.setLayoutData(infoData);
		Button names = new Button(shell, SWT.CHECK);
		names.setText("The first row contains the column names");
		names.setSelection(sheet.isFirstRowNames());
		names.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		Combo[] fields = new Combo[roles.length];
		Label[] counts = new Label[roles.length];
		Button[] selects = new Button[roles.length];
		boolean[] optional = new boolean[roles.length];
		String[] roleLabels = new String[roles.length];
		for (int i = 0; i < roles.length; i++) {
			optional[i] = roles[i].endsWith("?");
			roleLabels[i] = optional[i] ? roles[i].substring(0, roles[i].length() - 1) : roles[i];
			Label label = new Label(shell, SWT.NONE);
			label.setText(optional[i] ? roleLabels[i] + " (optional):" : roleLabels[i] + ":");
			fields[i] = new Combo(shell, SWT.DROP_DOWN);
			GridData fieldData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			fieldData.widthHint = 170;
			fields[i].setLayoutData(fieldData);
			fields[i].setToolTipText("A range of cells, e.g., B2:B51 (several ranges separated by ';'), or a column of the list"
					+ (optional[i] ? "; empty: none" : ""));
			selects[i] = new Button(shell, SWT.PUSH);
			selects[i].setText("Select");
			selects[i].setToolTipText("Select the cells in the table: the dialog is reduced, the selection of the table is taken over (Enter or Done: back)");
			counts[i] = new Label(shell, SWT.NONE);
			GridData countData = new GridData(SWT.FILL, SWT.CENTER, false, false);
			countData.widthHint = 190;
			counts[i].setLayoutData(countData);
		}
		Label status = new Label(shell, SWT.WRAP);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		/* The settings of the plot */
		boolean axes = roles.length >= 3;
		org.eclipse.swt.widgets.Group plotGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
		plotGroup.setText("Plot");
		plotGroup.setLayout(new GridLayout(2, false));
		plotGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		Label titleLabel = new Label(plotGroup, SWT.NONE);
		titleLabel.setText("Title:");
		titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		org.eclipse.swt.widgets.Text titleText = new org.eclipse.swt.widgets.Text(plotGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData titleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		titleData.heightHint = titleText.getLineHeight() * 2 + 6;
		titleText.setLayoutData(titleData);
		titleText.setText(title + "\n" + "Sheet: " + sheet.sheet());
		titleText.setToolTipText("The title (two lines), the math markup is possible (e.g., x^2, N_0)");
		org.eclipse.swt.widgets.Text[] axisTexts = new org.eclipse.swt.widgets.Text[3];
		boolean[] edited = new boolean[3];
		boolean[] updating = { false };
		Button[] logs = new Button[3];
		Combo colorMap = null;
		if (axes) {
			String[] axisNames = { "X axis:", "Y axis:", "Z axis:" };
			for (int k = 0; k < 3; k++) {
				Label l = new Label(plotGroup, SWT.NONE);
				l.setText(axisNames[k]);
				axisTexts[k] = new org.eclipse.swt.widgets.Text(plotGroup, SWT.BORDER);
				axisTexts[k].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				int axis = k;
				/* A title typed by the user is kept if the data are changed */
				axisTexts[k].addListener(SWT.Modify, _ -> {
					if (!updating[0]) {
						edited[axis] = true;
					}
				});
			}
			Label logLabel = new Label(plotGroup, SWT.NONE);
			logLabel.setText("Logarithmic:");
			org.eclipse.swt.widgets.Composite logBox = new org.eclipse.swt.widgets.Composite(plotGroup, SWT.NONE);
			GridLayout logLayout = new GridLayout(3, false);
			logLayout.marginWidth = 0;
			logLayout.marginHeight = 0;
			logBox.setLayout(logLayout);
			String[] logNames = { "X", "Y", "Z" };
			for (int k = 0; k < 3; k++) {
				logs[k] = new Button(logBox, SWT.CHECK);
				logs[k].setText(logNames[k]);
			}
			Label mapLabel = new Label(plotGroup, SWT.NONE);
			mapLabel.setText("Color map:");
			colorMap = new Combo(plotGroup, SWT.READ_ONLY);
			colorMap.setItems(COLOR_MAP_NAMES);
			colorMap.select(0);
			colorMap.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		Button exportAction = new Button(plotGroup, SWT.CHECK);
		exportAction.setText("Show the export action of the 3D view (WebGL, Blender)");
		exportAction.setSelection(true);
		exportAction.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		org.eclipse.swt.widgets.Composite buttons = new org.eclipse.swt.widgets.Composite(shell, SWT.NONE);
		buttons.setLayout(new GridLayout(2, true));
		buttons.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 4, 1));
		Button ok = new Button(buttons, SWT.PUSH);
		ok.setText("Plot");
		ok.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button cancel = new Button(buttons, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		shell.setDefaultButton(ok);

		/* The data of the sheet (read again after a change of the table, e.g., a selection) */
		PlotTable[] data = { sheet };
		/* The ranges, the values and the names of the roles (null: an invalid or empty range) */
		String[][] roleValues = new String[roles.length][];
		String[] roleNames = new String[roles.length];
		Runnable axisDefaults = () -> {
			if (!axes) {
				return;
			}
			updating[0] = true;
			for (int k = 0; k < 3; k++) {
				if (!edited[k]) {
					axisTexts[k].setText(roleNames[k] != null ? roleNames[k] : "");
				}
			}
			updating[0] = false;
		};
		/* Evaluates the range of a role: the number of the values, the name and the state of the Plot button */
		Runnable[] validate = new Runnable[1];
		validate[0] = () -> {
			boolean namesRow = names.getSelection();
			boolean valid = true;
			int length = -1;
			boolean sameLength = true;
			for (int i = 0; i < roles.length; i++) {
				String text = fields[i].getText().trim();
				roleValues[i] = null;
				roleNames[i] = null;
				if (text.isEmpty()) {
					counts[i].setText(optional[i] ? "none" : "a range is needed");
					valid &= optional[i];
					continue;
				}
				try {
					List<int[]> ranges = parseRanges(text, data[0].values.length);
					String[] v = data[0].cells(ranges, namesRow);
					roleValues[i] = v;
					roleNames[i] = data[0].rangeName(ranges, namesRow);
					int numbers = 0;
					for (String x : v) {
						if (isNumber(x)) {
							numbers++;
						}
					}
					/* The column of the table (its title, e.g., "C 2", and the name) */
					int c = ranges.get(0)[0];
					String column = c < data[0].header.length ? data[0].header[c] : PlotTable.columnLetters(c);
					String name = roleNames[i] != null && !roleNames[i].equals(column) ? " " + roleNames[i] : "";
					String kind = numbers == v.length ? v.length + " numbers" : numbers == 0 ? v.length + " texts" : v.length + " values, " + numbers + " numbers";
					counts[i].setText(kind + " (" + column + name + (ranges.size() > 1 || ranges.get(0)[0] != ranges.get(0)[2] ? ", ..." : "") + ")");
					counts[i].setToolTipText(kind + " in " + formatRanges(ranges));
					if (length >= 0 && length != v.length) {
						sameLength = false;
					}
					length = v.length;
				} catch (IllegalArgumentException ex) {
					counts[i].setText("invalid range");
					valid = false;
				}
			}
			status.setText(!sameLength ? "The roles have different numbers of values (missing values are empty)." : "");
			ok.setEnabled(valid);
			axisDefaults.run();
			shell.layout(true, true);
		};
		/* The columns as items of the fields (a column: its data range) and the default ranges */
		String[][] columnRanges = new String[1][];
		Runnable fillItems = () -> {
			boolean namesRow = names.getSelection();
			data[0].setFirstRowNames(namesRow);
			String[] columnNames = data[0].columnNames();
			columnRanges[0] = new String[columnNames.length];
			for (int i = 0; i < roles.length; i++) {
				String text = fields[i].getText();
				fields[i].removeAll();
				if (optional[i]) {
					fields[i].add("(none)");
				}
				for (int c = 0; c < columnNames.length; c++) {
					columnRanges[0][c] = data[0].columnRange(c, namesRow);
					fields[i].add(columnNames[c] + "  (" + columnRanges[0][c] + ")");
				}
				fields[i].setText(text);
			}
		};
		fillItems.run();
		/* The defaults: the selected columns of the table (if more than one cell is selected), else the default columns */
		List<int[]> selected = com.eco.bio7.rbridge.TableSelection.ranges(grid.getCellSelection());
		int[] defaults = defaultColumns(sheet, roles, optional);
		boolean useSelection = selected.size() > 0 && grid.getCellSelection().length > 1;
		/* One range text for each selected column (several ranges of a column are joined with ';') */
		List<String> selectedColumns = new ArrayList<>();
		if (useSelection) {
			java.util.TreeMap<Integer, List<int[]>> byColumn = new java.util.TreeMap<>();
			for (int[] r : selected) {
				for (int c = r[0]; c <= r[2]; c++) {
					byColumn.computeIfAbsent(c, _ -> new ArrayList<>()).add(new int[] { c, r[1], c, r[3] });
				}
			}
			for (List<int[]> l : byColumn.values()) {
				l.sort((x, y) -> Integer.compare(x[1], y[1]));
				selectedColumns.add(formatRanges(l));
			}
		}
		for (int i = 0, k = 0; i < roles.length; i++) {
			if (useSelection) {
				if (k < selectedColumns.size()) {
					fields[i].setText(selectedColumns.get(k++));
				}
			} else if (defaults[i] >= 0) {
				fields[i].setText(sheet.columnRange(defaults[i], names.getSelection()));
			}
		}
		validate[0].run();

		for (int i = 0; i < roles.length; i++) {
			int role = i;
			/* A column of the list: its data range */
			fields[i].addListener(SWT.Selection, _ -> {
				int index = fields[role].getSelectionIndex() - (optional[role] ? 1 : 0);
				if (index < 0) {
					fields[role].setText("");
				} else if (columnRanges[0] != null && index < columnRanges[0].length) {
					fields[role].setText(columnRanges[0][index]);
				}
				validate[0].run();
			});
			fields[i].addListener(SWT.Modify, _ -> validate[0].run());
			selects[i].addListener(SWT.Selection, _ -> {
				shell.setVisible(false);
				selectInTable(display, shell, grid, roleLabels[role], fields[role].getText(), range -> {
					if (range != null) {
						fields[role].setText(range);
					}
				}, () -> {
					if (!shell.isDisposed()) {
						/* The data of the table again (changed values) */
						PlotTable now = fromGrid(grid);
						if (now != null) {
							now.setFirstRowNames(names.getSelection());
							data[0] = now;
						}
						shell.setVisible(true);
						shell.setActive();
						fields[role].setFocus();
						validate[0].run();
					}
				});
			});
		}
		names.addListener(SWT.Selection, _ -> {
			/* The default ranges of whole columns start below the names */
			boolean namesRow = names.getSelection();
			for (int i = 0; i < roles.length; i++) {
				String text = fields[i].getText().trim();
				for (int c = 0; columnRanges[0] != null && c < columnRanges[0].length; c++) {
					if (text.equals(columnRanges[0][c])) {
						fields[i].setText(data[0].columnRange(c, namesRow));
					}
				}
			}
			fillItems.run();
			validate[0].run();
		});
		Selection[] result = new Selection[1];
		Combo[] mapCombo = { colorMap };
		ok.addListener(SWT.Selection, _ -> {
			/* The values of the roles as the columns of a table (one column for each role) */
			PlotTable now = fromGrid(grid);
			if (now != null) {
				now.setFirstRowNames(names.getSelection());
				data[0] = now;
			}
			validate[0].run();
			if (!ok.isEnabled()) {
				return;
			}
			int rowsCount = 0;
			for (String[] v : roleValues) {
				rowsCount = Math.max(rowsCount, v != null ? v.length : 0);
			}
			String[] header = new String[roles.length];
			String[][] values = new String[rowsCount][roles.length];
			int[] columns = new int[roles.length];
			for (int i = 0; i < roles.length; i++) {
				header[i] = roleNames[i] != null ? roleNames[i] : roleLabels[i];
				columns[i] = roleValues[i] != null ? i : -1;
				for (int r = 0; r < rowsCount; r++) {
					values[r][i] = roleValues[i] != null && r < roleValues[i].length ? roleValues[i][r] : "";
				}
			}
			PlotTable table = new PlotTable(sheet.sheet(), header, values);
			table.setFirstRowNames(false);
			String plotTitle = titleText.getText().replace("\r\n", "\n").replace('\r', '\n').strip();
			String[] axisTitles = null;
			boolean[] logAxes = null;
			if (axes) {
				axisTitles = new String[] { axisTexts[0].getText().strip(), axisTexts[1].getText().strip(), axisTexts[2].getText().strip() };
				logAxes = new boolean[] { logs[0].getSelection(), logs[1].getSelection(), logs[2].getSelection() };
			}
			int map = mapCombo[0] != null ? Math.max(0, mapCombo[0].getSelectionIndex()) : 0;
			result[0] = new Selection(table, roles.clone(), columns,
					new Options(plotTitle, axisTitles, logAxes, COLOR_MAPS[map].clone(), exportAction.getSelection()));
			shell.close();
		});
		cancel.addListener(SWT.Selection, _ -> shell.close());
		shell.pack();
		if (parent != null) {
			org.eclipse.swt.graphics.Rectangle p = parent.getBounds();
			org.eclipse.swt.graphics.Point s = shell.getSize();
			shell.setLocation(p.x + (p.width - s.x) / 2, p.y + (p.height - s.y) / 3);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result[0];
	}

	/*
	 * The reduced dialog (like LibreOffice): a bar with the range of the selected
	 * cells of the table (updated with each selection, also editable). Enter, Done
	 * or closing the bar returns to the dialog.
	 */
	private static void selectInTable(Display display, Shell dialog, Grid grid, String role, String current,
			java.util.function.Consumer<String> range, Runnable done) {
		Shell bar = new Shell(dialog.getParent() instanceof Shell ? (Shell) dialog.getParent() : dialog.getShell(),
				SWT.DIALOG_TRIM | SWT.MODELESS | SWT.ON_TOP);
		bar.setText(dialog.getText() + ": " + role);
		bar.setData("org.eclipse.e4.ui.css.CssClassName", com.eco.bio7.themes.CustomTheme.DIALOG_CLASS);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 10;
		layout.marginHeight = 8;
		bar.setLayout(layout);
		Label label = new Label(bar, SWT.NONE);
		label.setText("Select the cells of " + role + " in the table:");
		org.eclipse.swt.widgets.Text text = new org.eclipse.swt.widgets.Text(bar, SWT.BORDER | SWT.SINGLE);
		GridData textData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textData.widthHint = 220;
		text.setLayoutData(textData);
		text.setText(current != null ? current : "");
		Button finish = new Button(bar, SWT.PUSH);
		finish.setText("Done");
		bar.setDefaultButton(finish);
		/* The selection of the table: the range (also several ranges with Ctrl/Cmd or Shift) */
		org.eclipse.swt.widgets.Listener follow = _ -> {
			if (bar.isDisposed() || grid.isDisposed()) {
				return;
			}
			/* After the selection of the grid has been changed (the events of the mouse and the keys) */
			display.asyncExec(() -> {
				if (bar.isDisposed() || grid.isDisposed()) {
					return;
				}
				org.eclipse.swt.graphics.Point[] cells = grid.getCellSelection();
				if (cells.length > 0) {
					text.setText(formatRanges(com.eco.bio7.rbridge.TableSelection.ranges(cells)));
				}
			});
		};
		if (!grid.isDisposed()) {
			grid.addListener(SWT.Selection, follow);
			grid.addListener(SWT.MouseUp, follow);
			grid.addListener(SWT.KeyUp, follow);
		}
		boolean[] closed = { false };
		Runnable close = () -> {
			if (closed[0]) {
				return;
			}
			closed[0] = true;
			if (!grid.isDisposed()) {
				grid.removeListener(SWT.Selection, follow);
				grid.removeListener(SWT.MouseUp, follow);
				grid.removeListener(SWT.KeyUp, follow);
			}
			range.accept(text.isDisposed() ? null : text.getText().trim());
			if (!bar.isDisposed()) {
				bar.dispose();
			}
			done.run();
		};
		finish.addListener(SWT.Selection, _ -> close.run());
		bar.addListener(SWT.Close, e -> {
			e.doit = false;
			close.run();
		});
		/* The dialog is closed (e.g., the workbench): the bar, too */
		dialog.addListener(SWT.Dispose, _ -> {
			if (!bar.isDisposed()) {
				closed[0] = true;
				bar.dispose();
			}
		});
		bar.pack();
		org.eclipse.swt.graphics.Point location = dialog.getLocation();
		bar.setLocation(location.x, location.y);
		bar.open();
		if (!grid.isDisposed()) {
			grid.setFocus();
		}
	}
}
