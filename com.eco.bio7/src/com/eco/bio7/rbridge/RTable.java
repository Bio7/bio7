/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
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
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Listener;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.util.PlaceholderLabel;

public class RTable extends ViewPart {

	private Action saveFileAction;
	public static final String ID = "com.eco.bio7.rbridge.RTable"; //$NON-NLS-1$
	private static Grid grid;
	private Action getValues;
	private FileAction fi = new FileAction();
	private AddRemoveAction add = new AddRemoveAction();
	private EditAction edit = new EditAction();
	private FromSpreadAction fromSpread = new FromSpreadAction();
	private ScriptAction scriptAction = new ScriptAction();
	protected String[][] sheetdata;
	protected boolean withHead = true;
	public Text txt;
	private Action addSpread;
	private Action addColumn;
	private Action addRow;
	private static RTable instance;

	public static Grid getGrid() {
		return grid;
	}

	public static void setGrid(Grid grid) {
		RTable.grid = grid;
	}

	private static CTabFolder tabFolder;

	public static CTabFolder getTabFolder() {
		return tabFolder;
	}

	public static RTable getInstance() {
		return instance;
	}

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		instance = this;
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());
		tabFolder = new CTabFolder(container, SWT.BOTTOM);

		grid = new Spread().spread(tabFolder, 30, 30, "Sheet 1");
		tabFolder.addCTabFolder2Listener(new CTabFolder2Listener() {

			public void close(final CTabFolderEvent event) {
				final CTabItem item = (CTabItem) event.item;// tabFolder.getSelection();
				/* Garbage collector can work!!!! */
				grid = (Grid) event.item.getData();
				
				/*Dispose swt colors and images explicitly! */
				for (int i = 0; i < grid.getItemCount(); i++) {
					for (int j = 0; j < grid.getColumnCount(); j++) {
						Color foreg = grid.getItem(i).getForeground(j);
						Color backg = grid.getItem(i).getBackground(j);
						Image im = grid.getItem(i).getImage(j);
						if (foreg != null) {
							foreg.dispose();
						}
						if (backg != null) {
							backg.dispose();
						}
						if (im != null) {
							im.dispose();
						}

					}
				}
				grid.removeAll();
				item.dispose();
				
				/* Necessary to set the grid from the now active item! */
				if (tabFolder.getSelection() != null) {
					grid = (Grid) tabFolder.getSelection().getData();
				}

			}

			@Override
			public void maximize(CTabFolderEvent event) {

			}

			@Override
			public void minimize(CTabFolderEvent event) {

			}

			@Override
			public void restore(CTabFolderEvent event) {

			}

			@Override
			public void showList(CTabFolderEvent event) {

			}
		});

		tabFolder.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				grid = (Grid) e.item.getData();
			}

			public void widgetSelected(SelectionEvent e) {
				grid = (Grid) e.item.getData();

			}

		});
		tabFolder.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent mouseevent)

			{
				if (mouseevent.count == 2) {

					CTabItem item = tabFolder.getSelection();
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
			}

		});
		grid.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {

			}
		});

		//
		createActions();
		initializeToolBar();
		initializeMenu();

	}

	/**
	 * Create the actions
	 */
	private void createActions() {

		saveFileAction = new Action("Save File") {
			public void run() {
				Bio7Dialog.saveFile();
			}
		};

		addSpread = new Action("Add Spread") {

			public void run() {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					grid = new Spread().spread(RTable.getTabFolder(), 50, 20, "Sheet " + (RTable.getTabFolder().getItemCount() + 1));
					RTable.setGrid(grid);
				}

			}
		};
		ImageDescriptor desc1 = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/newspread.gif")));
		addSpread.setImageDescriptor(desc1);

		addColumn = new Action("Add Column") {

			public void run() {
				Grid grid = RTable.getGrid();
				GridColumn column = null;
				if (grid != null) {
					Point[] sel = grid.getCellSelection();

					if (sel.length == 0) {

						column = new GridColumn(grid, SWT.NONE, grid.getColumnCount());
						column.setWidth(50);

					} else {
						column = new GridColumn(RTable.getGrid(), SWT.NONE, sel[0].x);

						column.setWidth(50);

					}

					renameColumnsSorted(grid);

				}
			}
		};
		ImageDescriptor desc2 = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/newcolumn.gif")));
		addColumn.setImageDescriptor(desc2);

		addRow = new Action("Add Row") {

			public void run() {
				Grid grid = RTable.getGrid();
				int sel = grid.getSelectionIndex();
				if (grid != null) {
					new GridItem(grid, SWT.NONE, sel);
				}

			}
		};
		ImageDescriptor desc3 = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/newrow.gif")));
		addRow.setImageDescriptor(desc3);

	}

	private void renameColumnsSorted(Grid grid) {
		for (int i = 0; i < grid.getColumnCount(); i++) {
			GridColumn c = grid.getColumn(i);
			c.setText("C " + (i + 1));
		}
	}

	/**
	 * Initialize the toolbar
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		toolbarManager.add(addRow);
		toolbarManager.add(addColumn);
		toolbarManager.add(addSpread);
		toolbarManager.add(scriptAction);
		toolbarManager.add(fromSpread);
		toolbarManager.add(add);
		toolbarManager.add(edit);
		toolbarManager.add(fi);		
		toolbarManager.add(new PlaceholderLabel().getPlaceholderLabel());

	}

	public void dispose() {
		instance = null;
		super.dispose();
	}

	/**
	 * Initialize the menu
	 */
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		// Set the focus
	}

	class TextItem extends ControlContribution {

		TextItem() {
			super("text");
		}

		protected Control createControl(Composite parent) {
			txt = new Text(parent, SWT.SINGLE | SWT.BORDER);
			txt.setSize(200, 5);
			txt.addListener(SWT.DefaultSelection, new Listener() {
				public void handleEvent(Event e) {

				}
			});

			return txt;
		}
	}

}
