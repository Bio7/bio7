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
package com.eco.bio7.pybridge;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.rbridge.RTable;
import com.eco.bio7.util.Util;

/**
 * The Python menu of the table (as the R menu): the table as pandas DataFrame
 * (with or without the first row as header), the selected cells as numpy array
 * (numbers), list (text) or pandas Categorical (factor). The data goes to the
 * embedded Python (console and scripts).
 */
public class PythonFromSpreadAction extends Action implements IMenuCreator {

	private Menu fMenu;

	public PythonFromSpreadAction() {
		setId("Python_Menu");
		setToolTipText("Python Menu");
		setText("Python <-");
		setMenuCreator(this);
	}

	@Override
	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		item("Python<-Table", () -> transferTable(false));
		item("Python<-Table+head", () -> transferTable(true));
		new MenuItem(fMenu, SWT.SEPARATOR);
		item("Python<-Selection(numeric)", () -> transferSelection("numeric"));
		item("Python<-Selection(character)", () -> transferSelection("character"));
		item("Python<-Selection(categorical)", () -> transferSelection("categorical"));
		return fMenu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
		}
	}

	private void item(String text, Runnable action) {
		MenuItem item = new MenuItem(fMenu, SWT.PUSH);
		item.setText(text);
		item.addListener(SWT.Selection, _ -> action.run());
	}

	/* ---------------- The transfers (UI thread: the data, a job: Python) ---------------- */

	/* The table as DataFrame named after the tab (the first row as header if selected) */
	private void transferTable(boolean head) {
		Grid grid = RTable.getGrid();
		if (grid == null || RTable.getTabFolder().getItemCount() == 0 || !PythonImageTransfer.ensurePython()) {
			return;
		}
		CTabItem tab = RTable.getTabFolder().getSelection();
		String name = pythonName(tab == null ? "table" : tab.getText());
		int rows = grid.getItemCount(), cols = grid.getColumnCount();
		if (rows == 0 || cols == 0 || head && rows < 2) {
			Bio7Dialog.message("The table has no data!");
			return;
		}
		String[][] cells = new String[cols][rows];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				cells[c][r] = grid.getItem(r).getText(c);
			}
		}
		Map<String, PyValue> columns = new LinkedHashMap<>();
		for (int c = 0; c < cols; c++) {
			String header = head ? cells[c][0].strip() : "";
			String colName = header.isEmpty() ? "X" + (c + 1) : header;
			while (columns.containsKey(colName)) {
				colName = colName + "_" + (c + 1);
			}
			String[] values = new String[head ? rows - 1 : rows];
			System.arraycopy(cells[c], head ? 1 : 0, values, 0, values.length);
			columns.put(colName, column(values));
		}
		assign(name, new PyValue.Frame(columns, null), null);
	}

	/* The selected cells: numbers (numpy, NaN for text and empty cells), strings or categories */
	private void transferSelection(String type) {
		Grid grid = RTable.getGrid();
		if (grid == null || !PythonImageTransfer.ensurePython()) {
			return;
		}
		Point[] sel = grid.getCellSelection();
		if (sel.length == 0) {
			Bio7Dialog.message("No cells selected!");
			return;
		}
		String[] values = new String[sel.length];
		for (int i = 0; i < sel.length; i++) {
			values[i] = grid.getItem(sel[i].y).getText(sel[i].x);
		}
		InputDialog inp = new InputDialog(Util.getShell(), "To Python", "Create a name for the data!", "vector",
				s -> s.matches("[A-Za-z_][A-Za-z0-9_]*") ? null : "Not a valid Python name");
		if (inp.open() != Window.OK) {
			return;
		}
		String name = inp.getValue();
		switch (type) {
		case "numeric" -> {
			double[] d = new double[values.length];
			for (int i = 0; i < d.length; i++) {
				d[i] = number(values[i]);
			}
			assign(name, PyValue.of(d), null);
		}
		case "character" -> assign(name, strings(values), null);
		default -> assign(name, strings(values),
				/* A pandas Categorical (a list without pandas) */
				"try:\n    import pandas as _pd\n    " + name + " = _pd.Categorical(" + name + ")\n    del _pd\nexcept ImportError:\n    pass");
		}
	}

	/* A column: numbers (float64, NaN for empty cells) if all filled cells are numbers, else strings */
	private static PyValue column(String[] values) {
		double[] d = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			String v = values[i] == null ? "" : values[i].strip();
			if (v.isEmpty() || v.equals("NA")) {
				d[i] = Double.NaN;
				continue;
			}
			d[i] = number(v);
			if (Double.isNaN(d[i]) && !v.equalsIgnoreCase("nan")) {
				return strings(values);
			}
		}
		return PyValue.of(d);
	}

	/* Strings (None for empty cells and NA) */
	private static PyValue.Str strings(String[] values) {
		String[] s = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			String v = values[i] == null ? "" : values[i];
			s[i] = v.isBlank() || v.strip().equals("NA") ? null : v;
		}
		/* One value would be a str in Python: a list is expected */
		return new PyValue.Str(s);
	}

	private static double number(String v) {
		if (v == null) {
			return Double.NaN;
		}
		try {
			return Double.parseDouble(v.strip());
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	/* A valid Python name from the name of the tab */
	static String pythonName(String text) {
		String n = text.strip().replaceAll("[^A-Za-z0-9_]", "_");
		if (n.isEmpty() || Character.isDigit(n.charAt(0))) {
			n = "_" + n;
		}
		return n;
	}

	/* Assigns in a job (Python may be busy), then optional code; the result in the console */
	private static void assign(String name, PyValue value, String after) {
		Job job = new Job("Transfer to Python") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
				try {
					PySession py = EmbeddedPython.session();
					py.assign(name, value);
					if (value instanceof PyValue.Str s && s.length() == 1) {
						/* Always a list (one cell) */
						py.exec(name + " = [" + name + "]");
					}
					if (after != null) {
						py.exec(after);
					}
					String type = py.eval("type(" + name + ").__name__") instanceof PyValue.Str t ? t.asString() : "";
					EmbeddedPython.print("\nTransferred to Python: " + name + " (" + type + ", " + value.length() + (value instanceof PyValue.Frame ? " rows" : " values") + ")\n", false);
					EmbeddedPython.prompt();
					PythonShellView.refreshOpenViews();
				} catch (Exception e) {
					EmbeddedPython.print("\nTransfer to Python: " + e.getMessage() + "\n", true);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}
