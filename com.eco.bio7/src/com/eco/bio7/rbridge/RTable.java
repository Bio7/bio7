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
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.util.PlaceholderLabel;
import com.eco.bio7.util.Util;

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
	private Action undoAction;
	private Action redoAction;
	/* Updates the undo/redo actions when a history changes! */
	private final Runnable historyListener = () -> {
		Display display = Display.getDefault();
		if (Display.getCurrent() == display) {
			updateUndoRedo();
		} else {
			display.asyncExec(this::updateUndoRedo);
		}
	};
	private static RTable instance;
	private CTabItem item;

	public static Grid getGrid() {
		return grid;
	}

	public static void setGrid(Grid grid) {
		RTable.grid = grid;
		if (instance != null) {
			instance.historyListener.run();
		}
	}

	private static CTabFolder tabFolder;

	public static CTabFolder getTabFolder() {
		return tabFolder;
	}

	public static RTable getInstance() {
		return instance;
	}

	/** Undoes the last change of the selected sheet (see GridHistory). */
	public static void undo() {
		GridHistory history = prepareHistory();
		if (history != null) {
			history.undo();
		}
	}

	/** Redoes the last undone change of the selected sheet (see GridHistory). */
	public static void redo() {
		GridHistory history = prepareHistory();
		if (history != null) {
			history.redo();
		}
	}

	/* A cell which is edited is committed first (the editor is a Text in the grid)! */
	private static GridHistory prepareHistory() {
		Grid g = getGrid();
		if (g == null || g.isDisposed()) {
			return null;
		}
		Control focus = g.getDisplay().getFocusControl();
		if (focus instanceof Text && focus.getParent() == g) {
			g.setFocus();
		}
		return GridHistory.of(g);
	}

	private void updateUndoRedo() {
		if (undoAction == null) {
			return;
		}
		GridHistory history = GridHistory.of(grid);
		boolean canUndo = history != null && history.canUndo();
		boolean canRedo = history != null && history.canRedo();
		undoAction.setEnabled(canUndo);
		redoAction.setEnabled(canRedo);
		undoAction.setToolTipText(canUndo ? "Undo " + history.getUndoLabel() : "Undo");
		redoAction.setToolTipText(canRedo ? "Redo " + history.getRedoLabel() : "Redo");
		undoAction.setText(canUndo ? "Undo " + history.getUndoLabel() : "Undo");
		redoAction.setText(canRedo ? "Redo " + history.getRedoLabel() : "Redo");
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

				item = (CTabItem) event.item;// tabFolder.getSelection();
				/* Garbage collector can work!!!! */
				grid = (Grid) event.item.getData();

				/* Dispose swt colors and images explicitly! */
				for (int i = 0; i < grid.getItemCount(); i++) {
					for (int j = 0; j < grid.getColumnCount(); j++) {
						// Color foreg = grid.getItem(i).getForeground(j);
						// Color backg = grid.getItem(i).getBackground(j);
						Image im = grid.getItem(i).getImage(j);
						/*
						 * if (foreg != null) { foreg.dispose(); }
						 */
						/*
						 * if (backg != null) { backg.dispose(); }
						 */
						if (im != null) {
							im.dispose();
						}

					}
				}

				grid.disposeAllItems();

				item.dispose();

				/* Necessary to set the grid from the now active item! */
				if (tabFolder.getSelection() != null) {
					grid = (Grid) tabFolder.getSelection().getData();
				}
				updateUndoRedo();

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
				updateUndoRedo();
			}

			public void widgetSelected(SelectionEvent e) {
				grid = (Grid) e.item.getData();
				updateUndoRedo();
			}

		});
		tabFolder.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent mouseevent)

			{
				if (mouseevent.count == 2) {

					CTabItem item = tabFolder.getSelection();
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
		/* Edit > Undo/Redo and the shortcuts of the workbench (e.g., Ctrl/Cmd+Z) undo the sheet! */
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
		getViewSite().getActionBars().updateActionBars();
		GridHistory.addListener(historyListener);
		updateUndoRedo();

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
					grid = new Spread().spread(RTable.getTabFolder(), 1, 1, "Sheet " + (RTable.getTabFolder().getItemCount() + 1));
					RTable.setGrid(grid);
				}

			}
		};
		ImageDescriptor desc1 = ImageDescriptor.createFromImage(Bio7Plugin.getImageDescriptor("/icons/views/addspreadaction.png").createImage());
		addSpread.setImageDescriptor(desc1);

		addColumn = new Action("Add Column") {

			public void run() {
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
		};
		ImageDescriptor desc2 = ImageDescriptor.createFromImage(Bio7Plugin.getImageDescriptor("/icons/views/addcolumnaction.png").createImage());
		addColumn.setImageDescriptor(desc2);

		addRow = new Action("Add Row") {

			public void run() {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					int sel = grid.getSelectionIndex();
					GridHistory.of(grid).recordGrid("Add Row", () -> new GridItem(grid, SWT.NONE, sel));
				}

			}
		};
		ImageDescriptor desc3 = ImageDescriptor.createFromImage(Bio7Plugin.getImageDescriptor("/icons/views/addrowaction.png").createImage());
		addRow.setImageDescriptor(desc3);

		ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
		undoAction = new Action("Undo") {
			public void run() {
				undo();
			}
		};
		undoAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_UNDO));
		undoAction.setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_UNDO_DISABLED));
		redoAction = new Action("Redo") {
			public void run() {
				redo();
			}
		};
		redoAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
		redoAction.setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_REDO_DISABLED));

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
		toolbarManager.add(fi);
		toolbarManager.add(edit);
		toolbarManager.add(add);
		toolbarManager.add(fromSpread);
		toolbarManager.add(scriptAction);
		toolbarManager.add(new Separator());
		toolbarManager.add(undoAction);
		toolbarManager.add(redoAction);
		toolbarManager.add(new Separator());
		toolbarManager.add(addSpread);
		toolbarManager.add(addColumn);
		toolbarManager.add(addRow);

		//toolbarManager.add(new PlaceholderLabel().getPlaceholderLabel());

	}

	public void dispose() {
		GridHistory.removeListener(historyListener);
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
