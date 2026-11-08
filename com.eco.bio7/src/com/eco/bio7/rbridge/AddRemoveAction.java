/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import com.eco.bio7.util.Util;

public class AddRemoveAction extends Action implements IMenuCreator {

	private Menu fMenu;

	private Shell shell;

	public AddRemoveAction() {
		setId("RAddRemove");
		setToolTipText("R Menu");
		setText("Sheet");

		setMenuCreator(this);
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		MenuItem menuItem0 = new MenuItem(fMenu, SWT.PUSH);
		menuItem0.setText("Add Sheet");

		MenuItem menuItem01 = new MenuItem(fMenu, SWT.PUSH);
		menuItem01.setText("Add Sized Sheet");
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);

		menuItem.setText("Add Row (Before)");

		MenuItem menuItem_ = new MenuItem(fMenu, SWT.PUSH);

		menuItem_.setText("Add Row (After)");
		new MenuItem(fMenu, SWT.SEPARATOR);
		//
		MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);

		menuItem1.setText("Add Column (Before)");

		MenuItem menuItem1_ = new MenuItem(fMenu, SWT.PUSH);

		menuItem1_.setText("Add Column (After)");
		new MenuItem(fMenu, SWT.SEPARATOR);

		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);

		menuItem2.setText("Delete Rows");

		MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);

		menuItem3.setText("Delete Columns");
		new MenuItem(fMenu, SWT.SEPARATOR);

		MenuItem menuItem4 = new MenuItem(fMenu, SWT.PUSH);

		menuItem4.setText("Rename Sheet");

		menuItem0.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					grid = new Spread().spread(RTable.getTabFolder(), 1, 1, "Sheet " + (RTable.getTabFolder().getItemCount() + 1));
					RTable.setGrid(grid);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem01.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				new SizedSpreadDialog(Util.getShell()).open();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuItem.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					int sel = grid.getSelectionIndex();
					GridHistory.of(grid).recordGrid("Add Row", () -> {
						new GridItem(grid, SWT.NONE, sel);
						grid.layout();
					});
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem_.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					int sel = grid.getSelectionIndex();
					GridHistory.of(grid).recordGrid("Add Row", () -> {
						new GridItem(grid, SWT.NONE, sel + 1);
						grid.layout();
					});
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem1.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					Point[] sel = grid.getCellSelection();
					int index = sel.length == 0 ? grid.getColumnCount() : sel[0].x;
					GridHistory.of(grid).recordGrid("Add Column", () -> {
						new GridColumn(grid, SWT.NONE, index).setWidth(50);
						renameColumnsSorted(grid);
					});
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem1_.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					Point[] sel = grid.getCellSelection();
					int index = sel.length == 0 ? grid.getColumnCount() : sel[0].x + 1;
					GridHistory.of(grid).recordGrid("Add Column", () -> {
						new GridColumn(grid, SWT.NONE, index).setWidth(50);
						renameColumnsSorted(grid);
					});
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				deleteRows(RTable.getGrid());
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem3.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				deleteColumns(RTable.getGrid());
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem4.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				CTabItem item = RTable.getTabFolder().getSelection();
				if (item != null) {
					InputDialog dialog = new InputDialog(Util.getShell(), "Rename Tab", "Please enter a title!", item.getText(), new IInputValidator() {

						public String isValid(String text) {
							if (text.length() == 0) {
								return "item";
							}
							return null;
						}

					});

					if (dialog.open() == Window.OK) {
						item.setText(dialog.getValue());
					}
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		return fMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	/**
	 * Deletes the rows of the selected cells of a sheet (undoable). In a sheet of
	 * measurements (e.g., "3D Points") the measurements of the rows are removed
	 * (see RTable.ROWS_REMOVED).
	 * 
	 * @param grid the sheet
	 */
	public static void deleteRows(Grid grid) {
		if (grid == null || grid.isDisposed()) {
			return;
		}
		int[] rows = grid.getSelectionIndices();
		if (rows == null || rows.length == 0) {
			return;
		}
		/* A sheet of measurements: the measurements of the rows are removed (the sheet is updated) */
		if (grid.getData(RTable.ROWS_REMOVED) instanceof java.util.function.Consumer<?> owner) {
			@SuppressWarnings("unchecked")
			java.util.function.Consumer<int[]> remove = (java.util.function.Consumer<int[]>) owner;
			remove.accept(rows);
			return;
		}
		GridHistory.of(grid).recordGrid("Delete Rows", () -> grid.remove(rows));
	}

	/**
	 * Deletes the columns of the selected cells of a sheet (undoable). The columns
	 * of a sheet of measurements are not deleted (the columns of the values).
	 * 
	 * @param grid the sheet
	 */
	public static void deleteColumns(Grid grid) {
		if (grid == null || grid.isDisposed() || grid.getData(RTable.ROWS_REMOVED) != null) {
			return;
		}
		Point[] sel = grid.getCellSelection();
		if (sel == null || sel.length == 0) {
			return;
		}
		/* The selected columns (all rows), removed from the right so that the indices stay valid! */
		java.util.TreeSet<Integer> columns = new java.util.TreeSet<Integer>(java.util.Collections.reverseOrder());
		for (Point p : sel) {
			columns.add(p.x);
		}
		GridHistory.of(grid).recordGrid("Delete Columns", () -> {
			for (int column : columns) {
				if (column < grid.getColumnCount()) {
					grid.getColumn(column).dispose();
				}
			}
			renameColumns(grid);
		});
	}

	/* The titles of the columns again (A, B, C, ...) */
	private static void renameColumns(Grid grid) {
		for (int i = 0; i < grid.getColumnCount(); i++) {
			grid.getColumn(i).setText(Bio7Grid.columnName(i));
		}
	}

	private void renameColumnsSorted(Grid grid) {
		for (int i = 0; i < grid.getColumnCount(); i++) {
			GridColumn c = grid.getColumn(i);
			c.setText(Bio7Grid.columnName(i));
		}
	}

}