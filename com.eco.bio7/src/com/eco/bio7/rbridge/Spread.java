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

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridEditor;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import com.eco.bio7.util.Util;

class Spread {
	private Grid grid;
	protected boolean down = false;
	protected int index;
	protected Point pt;
	protected Rectangle clientArea;
	protected int column;
	private Clipboard cb;
	private int initialColumns = 1;
	private int initialRows = 1;
	protected boolean pressedMouse;
	protected boolean controllPressed;
	private CutCopyPaste ccp;
	private static int selectedColumnMin = 0;
	private static int selectedRowMax = 0;
	private static int selectedColumnMax = 0;
	private static int selectedRowMin = 0;

	public static int getSelectedColumnMin() {
		return selectedColumnMin;
	}

	public static int getSelectedRowMax() {
		return selectedRowMax;
	}

	public static int getSelectedColumnMax() {
		return selectedColumnMax;
	}

	public static int getSelectedRowMin() {
		return selectedRowMin;
	}

	/* The column of the last click on a column header (for Shift+click)! */
	private int lastHeaderColumn = -1;

	private void selectColumns(int col, int stateMask) {
		if (grid.getItemCount() == 0) {
			return;
		}
		boolean add = (stateMask & SWT.MOD1) != 0;
		boolean range = (stateMask & SWT.SHIFT) != 0 && lastHeaderColumn >= 0 && lastHeaderColumn < grid.getColumnCount();
		if (!add) {
			grid.deselectAllCells();
		}
		int from = range ? Math.min(lastHeaderColumn, col) : col;
		int to = range ? Math.max(lastHeaderColumn, col) : col;
		for (int c = from; c <= to; c++) {
			grid.selectColumn(c);
		}
		if (!range) {
			lastHeaderColumn = col;
		}
		/* A paste starts at the top of the (first) selected column! */
		selectedColumnMin = from;
		selectedRowMin = 0;
		grid.redraw();
	}

	public Grid spread(CTabFolder tabFolder, int cols, int rows, String name) {
		ccp = new CutCopyPaste();
		initialColumns = cols;
		initialRows = rows;

		cb = new Clipboard(Display.getDefault());
		CTabItem tab1TabItem = new CTabItem(tabFolder, SWT.NONE);
		tab1TabItem.setShowClose(true);
		tab1TabItem.setText(name);
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new FillLayout());
		tab1TabItem.setControl(composite);

		grid = new Grid(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		grid.setBackground(Util.getSWTBackgroundColor());
		//grid.setRowHeaderRenderer(new CustomRowHeaderRenderer());
		final Menu menu = new Menu(grid);
		grid.setMenu(menu);
		Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				GridColumn column = (GridColumn) e.widget;
			}
		};
		for (int i = 0; i < initialColumns; i++) {
			GridColumn column = new GridColumn(grid, SWT.NONE);
			//column.setHeaderRenderer(new CustomColumnHeaderRenderer());
			column.addListener(SWT.Selection, sortListener);
			column.setText("C " + (i + 1));
			column.setWidth(50);

		}
		for (int i = 0; i < initialRows; i++) {
			new GridItem(grid, SWT.NONE);
			
		}
		grid.addKeyListener(new KeyListener() {
			String selectedItem = "";

			public void keyPressed(KeyEvent e) {

				if ((e.keyCode == 'c') && (e.stateMask == SWT.CTRL)) {
					ccp.copy();
				} else if ((e.keyCode == 'v') && (e.stateMask == SWT.CTRL)) {
					ccp.paste();
				} else if ((e.keyCode == 't') && (e.stateMask == SWT.CTRL)) {
					ccp.cut();
				} else if ((e.keyCode == 'i') && (e.stateMask == SWT.CTRL)) {
					ccp.copyImageToClipboard();
				} else if ((e.keyCode == 'p') && (e.stateMask == SWT.CTRL)) {
					ccp.pasteImageFromClipboard();
				} else if ((e.keyCode == SWT.DEL)) {
					ccp.delete();
				} else if (e.keyCode == SWT.CTRL) {
					controllPressed = true;

				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				controllPressed = false;

			}

		});

		final MenuItem newItemMenuItem_11 = new MenuItem(menu, SWT.NONE);
		newItemMenuItem_11.setText("Cut");
		newItemMenuItem_11.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ccp.cut();

			}
		});
		final MenuItem newItemMenuItem_1 = new MenuItem(menu, SWT.NONE);
		newItemMenuItem_1.setText("Copy");
		newItemMenuItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				ccp.copy();

			}
		});

		final MenuItem newItemMenuItem_2 = new MenuItem(menu, SWT.NONE);
		newItemMenuItem_2.setText("Paste");
		newItemMenuItem_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ccp.paste();
			}

		});

		final MenuItem newItemMenuItem_3 = new MenuItem(menu, SWT.NONE);
		newItemMenuItem_3.setText("Copy Image To Clipboard");
		newItemMenuItem_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				ccp.copyImageToClipboard();

			}
		});
		final MenuItem newItemMenuItem_4 = new MenuItem(menu, SWT.NONE);
		newItemMenuItem_4.setText("Paste Image from Clipboard");
		newItemMenuItem_4.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				ccp.pasteImageFromClipboard();

			}
		});

		final MenuItem newItemMenuItem = new MenuItem(menu, SWT.NONE);
		newItemMenuItem.setText("Delete");
		newItemMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				ccp.delete();

			}
		});

		grid.setHeaderVisible(true);
		// Show row header
		grid.setRowHeaderVisible(true);
		// Enable cell selection
		grid.setRowsResizeable(true);
		grid.setCellSelectionEnabled(true);
		tab1TabItem.setData(grid);

		/*
		 * A click on a column header (without moving or resizing the column) selects the column
		 * like a click on a row header selects the row: Shift selects the range from the last
		 * clicked column, Ctrl/Cmd adds the column to the selection!
		 */
		final int[] headerDown = { -1, 0, 0 };
		grid.addListener(SWT.MouseDown, e -> {
			headerDown[0] = -1;
			if (e.button == 1 && grid.getHeaderVisible() && e.y >= 0 && e.y < grid.getHeaderHeight()) {
				GridColumn c = grid.getColumn(new Point(e.x, e.y));
				if (c != null) {
					headerDown[0] = grid.indexOf(c);
					headerDown[1] = e.x;
					headerDown[2] = c.getWidth();
				}
			}
		});
		grid.addListener(SWT.MouseUp, e -> {
			int col = headerDown[0];
			headerDown[0] = -1;
			if (col < 0 || col >= grid.getColumnCount() || e.button != 1 || Math.abs(e.x - headerDown[1]) > 3 || grid.getColumn(col).getWidth() != headerDown[2]) {
				return;
			}
			selectColumns(col, e.stateMask);
		});

		final GridEditor editor = new GridEditor(grid);

		grid.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				if (down == false) {
					Rectangle clientArea = grid.getClientArea();
					pt = new Point(event.x, event.y);
					index = grid.getTopIndex();

					while (index < grid.getItemCount()) {
						boolean visible = false;
						final GridItem item = grid.getItem(index);
						for (int i = 0; i < grid.getColumnCount(); i++) {
							Rectangle rect = item.getBounds(i);
							if (rect.contains(pt)) {
								column = i;
								/* Recursive call! */
								recursiveEdit(editor, item);

								return;
							}
							if (!visible && rect.intersects(clientArea)) {
								visible = true;
							}
						}
						if (!visible)
							return;
						index++;
					}
				}
			}

			/*
			 * A recursive method to move to the next row to e.g. ease the input
			 * of data!
			 */
			/* Sets the text of a cell as an undoable step (if the text was changed)! */
			private void setCellText(GridItem item, int col, String value) {
				int row = grid.indexOf(item);
				if (row < 0 || value.equals(item.getText(col))) {
					return;
				}
				GridHistory.of(grid).recordCells("Edit", java.util.Collections.singletonList(new Point(col, row)), () -> item.setText(col, value));
			}

			private void recursiveEdit(final GridEditor editor, final GridItem item) {

				final Text text = new Text(grid, SWT.NONE);
				Listener textListener = new Listener() {
					public void handleEvent(final Event e) {
						switch (e.type) {
						case SWT.FocusOut:
							if (item.isDisposed() == false) {
								setCellText(item, column, text.getText());
								text.dispose();

							}
							break;
						case SWT.Traverse:
							switch (e.detail) {
							case SWT.TRAVERSE_RETURN:
								setCellText(item, column, text.getText());
								index++;
								if (index < grid.getItemCount()) {

									GridItem gi = grid.getItem(index);
									grid.showItem(gi);

									/*
									 * This snippet can be used to deselect the
									 * previous cell and select the next cell!
									 * Point pt = new Point(column, index);
									 * grid.selectCell(pt);Point pt2 = new
									 * Point(column, index-1);
									 * grid.deselectCell(pt2);
									 */
									recursiveEdit(editor, gi);
									 /*Dynamically resize row amount!*/
								} else if (index == grid.getItemCount()) {
									GridHistory.of(grid).recordGrid("Add Row", () -> Bio7Grid.createRow(index, item.getHeight()));

									GridItem gi = grid.getItem(index);
									grid.showItem(gi);
									recursiveEdit(editor, gi);
								}

								else {
									text.dispose();
									e.doit = false;
								}
								break;
							/* Other keys don't work here! */
							case SWT.TRAVERSE_TAB_NEXT:
								setCellText(item, column, text.getText());
								index--;
								if (index >= 0) {
									GridItem gi = grid.getItem(index);
									grid.showItem(gi);
									recursiveEdit(editor, gi);
								} else {
									text.dispose();
									e.doit = false;
								}
								break;
							case SWT.TRAVERSE_ARROW_NEXT:
								setCellText(item, column, text.getText());
								column++;
								if (column < grid.getColumnCount()) {
									GridItem gi = grid.getItem(index);
									GridColumn col = grid.getColumn(column);
									grid.showColumn(col);
									recursiveEdit(editor, gi);
								}
                                /*Dynamically resize column amount!*/
								else if (column == grid.getColumnCount()) {
									GridHistory.of(grid).recordGrid("Add Column", () -> Bio7Grid.createColumn(cols, grid.getColumn(column - 1).getWidth(), "C " + (grid.getColumnCount() + 1)));

									GridItem gi = grid.getItem(index);
									GridColumn col = grid.getColumn(column);
									grid.showColumn(col);
									recursiveEdit(editor, gi);
								}

								else {
									text.dispose();
									e.doit = false;
								}
								break;
							case SWT.TRAVERSE_ARROW_PREVIOUS:
								setCellText(item, column, text.getText());
								column--;
								if (column >= 0) {
									GridItem gi = grid.getItem(index);
									GridColumn col = grid.getColumn(column);
									grid.showColumn(col);
									recursiveEdit(editor, gi);
								} else {
									text.dispose();
									e.doit = false;
								}
								break;
							case SWT.TRAVERSE_ESCAPE:
								text.dispose();
								e.doit = false;
							}
							break;

						}
					}
				};
				text.addListener(SWT.FocusOut, textListener);
				text.addListener(SWT.Traverse, textListener);
				editor.setEditor(text, item, column);
				text.setText(item.getText(column));
				text.selectAll();
				text.setFocus();
			}

		});

		grid.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {

				pressedMouse = true;
				if (down) {
					clientArea = grid.getClientArea();
					pt = new Point(event.x, event.y);
					index = grid.getTopIndex();

				}

				if (event.y < grid.getHeaderHeight()) {

					Point[] sel = grid.getCellSelection();
					int colTempX = 0;

					for (Point selection : sel) {
						if (selection.x > colTempX) {
							// Store index of current column!
							colTempX = selection.x;

						}

					}
				}

				if (event.button == 1) {
					Rectangle clientArea = grid.getClientArea();
					Point pt = new Point(event.x, event.y);
					int index = grid.getTopIndex();
					while (index < grid.getItemCount()) {
						boolean visible = false;
						GridItem item = grid.getItem(index);
						for (int i = 0; i < grid.getColumnCount(); i++) {
							Rectangle rect = item.getBounds(i);
							if (rect.contains(pt)) {

								selectedColumnMin = i;
								selectedRowMin = index;

							}
							if (!visible && rect.intersects(clientArea)) {
								visible = true;
							}
						}
						if (!visible)
							return;
						index++;
					}
				}
			}

		});
		grid.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				pressedMouse = false;

			}
		});

		/* Global key listener for control key! */
		grid.getDisplay().addFilter(SWT.KeyDown, new Listener() {

			public void handleEvent(Event event) {
				if (event.keyCode == SWT.CTRL) {
					controllPressed = true;

				}

			}

		});
		/* Global key listener for control key! */
		grid.getDisplay().addFilter(SWT.KeyUp, new Listener() {

			public void handleEvent(Event event) {
				if (event.keyCode == SWT.CTRL) {
					controllPressed = false;

				}

			}

		});

		grid.addListener(SWT.MouseMove, new Listener() {
			public void handleEvent(Event event) {
				/*
				 * if
				 * (event.type==SWT.MouseMove&&pressedMouse==true&&controllPressed
				 * ==true) { if(grid.getSelectionCount()>=0){
				 * grid.deselectAll();
				 * //System.out.println(grid.getSelectionCount()); } }
				 */
			}
		});

		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		tabFolder.showItem(tab1TabItem);

		if (tabFolder.getItemCount() == 0) {
			tabFolder.setSelection(0);
		} else {
			tabFolder.setSelection(tabFolder.getItemCount() - 1);
		}
		return grid;
	}

}
