/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rbridge.debug;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridEditor;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.console.ConsolePageParticipant;

public class DebugVariablesView extends ViewPart {

	public static final String ID = "com.eco.bio7.rbridge.debug.DebugVariablesView"; //$NON-NLS-1$
	private static Grid grid;

	public static Grid getDebugVariablesGrid() {
		return grid;
	}

	protected int index;
	protected Point pt;
	protected int column;
	protected GridEditor editor;

	public DebugVariablesView() {
		setPartName("Variables (x)");
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout(SWT.HORIZONTAL));

		grid = new Grid(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);

		{
			GridColumn gridColumn = new GridColumn(grid, SWT.NONE);
			gridColumn.setText("Name");
			gridColumn.setWidth(150);
		}
		{
			GridColumn gridColumn = new GridColumn(grid, SWT.NONE);
			gridColumn.setText("Type");
			gridColumn.setWidth(150);
		}
		{
			GridColumn gridColumn = new GridColumn(grid, SWT.NONE);
			gridColumn.setText("Value");
			gridColumn.setWidth(150);
		}

		grid.setHeaderVisible(true);
		// Show row header
		grid.setRowHeaderVisible(true);
		// Enable cell selection
		grid.setRowsResizeable(true);
		grid.setCellSelectionEnabled(true);
		final GridEditor editor = new GridEditor(grid);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		grid.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Rectangle clientArea = grid.getClientArea();
				Point pt = new Point(event.x, event.y);
				int index = grid.getTopIndex();
				while (index < grid.getItemCount()) {
					boolean visible = false;
					final GridItem item = grid.getItem(index);
					for (int i = 0; i < grid.getColumnCount(); i++) {
						if (i > 1) {
							Rectangle rect = item.getBounds(i);
							if (rect.contains(pt)) {
								final int column = i;
								final Text text = new Text(grid, SWT.NONE);
								Listener textListener = new Listener() {
									public void handleEvent(final Event e) {
										switch (e.type) {
										case SWT.FocusOut:
											item.setText(column, text.getText());
											text.dispose();
											break;
										case SWT.Traverse:
											switch (e.detail) {
											case SWT.TRAVERSE_RETURN:
												item.setText(column, text.getText());
												String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
												if (selectionConsole.equals("R")) {
													ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();

													con.pipeToRConsole(item.getText(0)+"<-"+text.getText());
													System.out.println("Set debug expression to: "+item.getText(0)+"<-"+text.getText());
												}
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
								editor.setEditor(text, item, i);
								text.setText(item.getText(i));
								text.selectAll();
								text.setFocus();
								return;
							}
							if (!visible && rect.intersects(clientArea)) {
								visible = true;
							}
						}
					}
					if (!visible)
						return;
					index++;
				}
			}
		});
		createActions();
		initializeToolBar();
		initializeMenu();
	}

	public void createItems() {

	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		// Create the actions
	}

	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		// Set the focus
	}
}
