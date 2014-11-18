/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

		menuItem2.setText("Remove Selected Row(s)");

		MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);

		menuItem3.setText("Remove Selected Column(s)");
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
				new SizedSpreadDialog(new Shell()).open();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuItem.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				Grid grid = RTable.getGrid();
				int sel = grid.getSelectionIndex();
				if (grid != null) {
					new GridItem(grid, SWT.NONE, sel);
					
					grid.layout();
					
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
				int sel = grid.getSelectionIndex();
				if (grid != null) {
					new GridItem(grid, SWT.NONE, sel + 1);
					
					grid.layout();
					
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
				GridColumn column = null;
				if (grid != null) {
					Point[] sel = grid.getCellSelection();

					if (sel.length == 0) {

						column = new GridColumn(grid, SWT.NONE, grid.getColumnCount());
						column.setWidth(50);

					} else {
						// System.out.println(sel[0].x);
						column = new GridColumn(grid, SWT.NONE, sel[0].x);
						// column.setText("C " + (sel[0].x + 1));
						column.setWidth(50);

					}

					renameColumnsSorted(grid);

					// column.pack();

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
				GridColumn column = null;
				if (grid != null) {
					Point[] sel = grid.getCellSelection();

					if (sel.length == 0) {

						column = new GridColumn(grid, SWT.NONE, grid.getColumnCount());
						column.setWidth(50);

					} else {

						column = new GridColumn(grid, SWT.NONE, sel[0].x + 1);
						// column.setText("C " + (sel[0].x + 1));
						column.setWidth(50);

					}

					renameColumnsSorted(grid);

					// column.pack();

				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					// grid.remove(1);
					grid.remove(grid.getSelectionIndices());
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem3.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				Grid grid = RTable.getGrid();

				if (grid != null) {
					if(grid.getCellSelection()!=null){
					Point[] sel = grid.getCellSelection();
                    if (sel.length>0){
					/*
					 * If the header is selected we must avoid to get all
					 * selections in a column. We can calculate the selected x
					 * coordinates (columns) from each unique y-coordinate (the
					 * first available!)
					 */
					int colNrCurrent = sel[0].y;
					// System.out.println(colNrCurrent);
					for (int i = 0; i < sel.length; i++) {
						int colNrTemp = sel[i].y;

						if (colNrCurrent == colNrTemp) {
							grid.getColumn(sel[i].x).dispose();
							// System.out.println(sel[i].x);
							colNrTemp = colNrCurrent;
						}
					}
					renameColumnsSorted(grid);
					}
					}
				}
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
					InputDialog dialog = new InputDialog(new Shell(), "Rename Tab", "Please enter a title!", item.getText(), new IInputValidator() {

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

	private void renameColumnsSorted(Grid grid) {
		for (int i = 0; i < grid.getColumnCount(); i++) {
			GridColumn c = grid.getColumn(i);
			c.setText("C " + (i + 1));
		}
	}

}