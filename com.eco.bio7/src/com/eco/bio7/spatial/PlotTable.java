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
		display.syncExec(() -> {
			Grid grid = RTable.getGrid();
			if (grid == null || grid.isDisposed()) {
				return;
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
		});
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
	 * Opens a dialog to select the columns of the active sheet of the Bio7 table
	 * for the roles of a plot. A role with a '?' at the end is optional (e.g.,
	 * "Group?"). The dialog has the settings of the plot, too: the title, the
	 * titles of the axes (with the names of the first three columns, not for less
	 * than three roles, e.g., a pie chart), logarithmic axes, the color map and
	 * the export action (see Selection.apply).
	 *
	 * @param title       the title of the dialog (e.g., the plot)
	 * @param description a description (or null)
	 * @param roles       the roles (e.g., "X", "Y", "Z", "Group?")
	 * @return the selection or null (canceled or no table data)
	 */
	public static Selection select(String title, String description, String... roles) {
		PlotTable table = fromActiveSheet();
		/* The selected cells (if more than one cell is selected) */
		PlotTable selection = fromSelection();
		if (selection != null && selection.values.length * selection.columns() < 2) {
			selection = null;
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (table == null || table.columns() == 0 || table.values.length == 0) {
			display.syncExec(() -> org.eclipse.jface.dialogs.MessageDialog.openInformation(display.getActiveShell(), title,
					"The Bio7 table has no data. Open the table (Window - Show View - Bio7 Table) and load or paste the data into a sheet."));
			return null;
		}
		Selection[] result = new Selection[1];
		PlotTable selected = selection;
		display.syncExec(() -> result[0] = open(display, table, selected, title, description, roles));
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

	private static Selection open(Display display, PlotTable sheet, PlotTable selection, String title, String description, String[] roles) {
		/* The data: the selected cells (if available) or the sheet */
		PlotTable[] data = { selection != null ? selection : sheet };
		Shell parent = display.getActiveShell();
		Shell shell = parent != null ? new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE)
				: new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		shell.setText(title);
		shell.setData("org.eclipse.e4.ui.css.CssClassName", com.eco.bio7.themes.CustomTheme.DIALOG_CLASS);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 12;
		layout.marginHeight = 12;
		layout.verticalSpacing = 8;
		shell.setLayout(layout);
		Label info = new Label(shell, SWT.WRAP);
		info.setText((description != null ? description + "\n" : "") + "Sheet: " + sheet.sheet() + " (" + sheet.columns() + " columns)");
		GridData infoData = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		infoData.widthHint = 380;
		info.setLayoutData(infoData);
		Button useSelection = new Button(shell, SWT.CHECK);
		if (selection != null) {
			java.util.List<int[]> ranges = com.eco.bio7.rbridge.TableSelection.ranges(com.eco.bio7.rbridge.TableSelection.cells());
			useSelection.setText("Only the selected cells (" + ranges.size() + (ranges.size() == 1 ? " range, " : " ranges, ") + selection.columns()
					+ (selection.columns() == 1 ? " column)" : " columns)"));
			useSelection.setSelection(true);
		} else {
			useSelection.setText("Only the selected cells (select ranges in the table, more with Ctrl/Cmd or Shift)");
			useSelection.setEnabled(false);
		}
		useSelection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		Button names = new Button(shell, SWT.CHECK);
		names.setText("The first row contains the column names");
		names.setSelection(data[0].isFirstRowNames());
		names.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		Combo[] combos = new Combo[roles.length];
		boolean[] optional = new boolean[roles.length];
		for (int i = 0; i < roles.length; i++) {
			optional[i] = roles[i].endsWith("?");
			Label label = new Label(shell, SWT.NONE);
			label.setText(optional[i] ? roles[i].substring(0, roles[i].length() - 1) + " (optional):" : roles[i] + ":");
			combos[i] = new Combo(shell, SWT.READ_ONLY);
			combos[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		Label rows = new Label(shell, SWT.NONE);
		rows.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		/* The settings of the plot */
		boolean axes = roles.length >= 3;
		org.eclipse.swt.widgets.Group plotGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
		plotGroup.setText("Plot");
		plotGroup.setLayout(new GridLayout(2, false));
		plotGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		Label titleLabel = new Label(plotGroup, SWT.NONE);
		titleLabel.setText("Title:");
		titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		org.eclipse.swt.widgets.Text titleText = new org.eclipse.swt.widgets.Text(plotGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData titleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		titleData.heightHint = titleText.getLineHeight() * 2 + 6;
		titleText.setLayoutData(titleData);
		titleText.setText(title + "\n" + "Sheet: " + data[0].sheet());
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
				/* A title typed by the user is kept if the columns are changed */
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
		/* The axis titles: the names of the columns of the first three roles (if not edited) */
		Runnable axisDefaults = () -> {
			if (!axes) {
				return;
			}
			updating[0] = true;
			for (int k = 0; k < 3; k++) {
				if (!edited[k]) {
					String name = combos[k].getText();
					axisTexts[k].setText(name.equals("(none)") ? "" : name);
				}
			}
			updating[0] = false;
		};
		for (Combo combo : combos) {
			combo.addListener(SWT.Selection, _ -> axisDefaults.run());
		}
		boolean[] reset = { false };
		Runnable fill = () -> {
			PlotTable table = data[0];
			table.setFirstRowNames(names.getSelection());
			String[] columnNames = table.columnNames();
			int[] defaults = defaultColumns(table, roles, optional);
			for (int i = 0; i < roles.length; i++) {
				/* Other columns (the selection or the sheet): the defaults again */
				int previous = reset[0] ? -1 : combos[i].getSelectionIndex();
				combos[i].removeAll();
				if (optional[i]) {
					combos[i].add("(none)");
				}
				for (int c = 0; c < columnNames.length; c++) {
					combos[i].add(columnNames[c]);
				}
				int index = previous >= 0 ? previous : defaults[i] + (optional[i] ? 1 : 0);
				combos[i].select(Math.max(0, Math.min(index, combos[i].getItemCount() - 1)));
			}
			rows.setText(table.rows() + " data rows");
			reset[0] = false;
		};
		fill.run();
		axisDefaults.run();
		names.addListener(SWT.Selection, _ -> {
			fill.run();
			axisDefaults.run();
		});
		useSelection.addListener(SWT.Selection, _ -> {
			data[0] = useSelection.getSelection() && selection != null ? selection : sheet;
			names.setSelection(data[0].isFirstRowNames());
			reset[0] = true;
			fill.run();
			axisDefaults.run();
			/* The title with the name of the data (the sheet or the selection) if not edited */
			if (titleText.getText().equals(title + "\n" + "Sheet: " + sheet.sheet()) || titleText.getText().equals(title + "\n" + "Sheet: " + (selection != null ? selection.sheet() : ""))) {
				titleText.setText(title + "\n" + "Sheet: " + data[0].sheet());
			}
			shell.layout(true, true);
		});
		org.eclipse.swt.widgets.Composite buttons = new org.eclipse.swt.widgets.Composite(shell, SWT.NONE);
		buttons.setLayout(new GridLayout(2, true));
		buttons.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 2, 1));
		Button ok = new Button(buttons, SWT.PUSH);
		ok.setText("Plot");
		ok.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button cancel = new Button(buttons, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		shell.setDefaultButton(ok);
		Selection[] result = new Selection[1];
		Combo[] mapCombo = { colorMap };
		ok.addListener(SWT.Selection, _ -> {
			int[] columns = new int[roles.length];
			for (int i = 0; i < roles.length; i++) {
				columns[i] = combos[i].getSelectionIndex() - (optional[i] ? 1 : 0);
			}
			String plotTitle = titleText.getText().replace("\r\n", "\n").replace('\r', '\n').strip();
			String[] axisTitles = null;
			boolean[] logAxes = null;
			if (axes) {
				axisTitles = new String[] { axisTexts[0].getText().strip(), axisTexts[1].getText().strip(), axisTexts[2].getText().strip() };
				logAxes = new boolean[] { logs[0].getSelection(), logs[1].getSelection(), logs[2].getSelection() };
			}
			int map = mapCombo[0] != null ? Math.max(0, mapCombo[0].getSelectionIndex()) : 0;
			result[0] = new Selection(data[0], roles.clone(), columns,
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
}
