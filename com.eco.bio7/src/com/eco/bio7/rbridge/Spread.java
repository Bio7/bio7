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
		final Menu menu = new Menu(grid);
		grid.setMenu(menu);
		Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				GridColumn column = (GridColumn) e.widget;
			}
		};
		for (int i = 0; i < initialColumns; i++) {
			GridColumn column = new GridColumn(grid, SWT.NONE);
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
			private void recursiveEdit(final GridEditor editor, final GridItem item) {

				final Text text = new Text(grid, SWT.NONE);
				Listener textListener = new Listener() {
					public void handleEvent(final Event e) {
						switch (e.type) {
						case SWT.FocusOut:
							if (item.isDisposed() == false) {
								item.setText(column, text.getText());
								text.dispose();

							}
							break;
						case SWT.Traverse:
							switch (e.detail) {
							case SWT.TRAVERSE_RETURN:
								item.setText(column, text.getText());
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
									Bio7Grid.createRow(index, item.getHeight());

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
								item.setText(column, text.getText());
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
								item.setText(column, text.getText());
								column++;
								if (column < grid.getColumnCount()) {
									GridItem gi = grid.getItem(index);
									GridColumn col = grid.getColumn(column);
									grid.showColumn(col);
									recursiveEdit(editor, gi);
								}
                                /*Dynamically resize column amount!*/
								else if (column == grid.getColumnCount()) {
									Bio7Grid.createColumn(cols, grid.getColumn(column - 1).getWidth(), "C " + (grid.getColumnCount() + 1));

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
								item.setText(column, text.getText());
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
