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
package com.eco.bio7.ijmacro.editor.toolbar.debug;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

import ij.macro.Interpreter;

public class DebugVariablesView extends ViewPart {

	public static final String ID = "com.eco.bio7.rbridge.debug.ImageJMacroDebugVariablesView"; //$NON-NLS-1$
	private static Table grid;
	private TableColumn column;
	private TableColumn column1;
	private TableColumn column2;
	private TableColumn column3;
	protected int index;
	protected Point pt;
	protected TableEditor editor;
	private DebugStopAction debugStopAction;
	private DebugIjMacroScript startMacroDebugging;
	private DebugNextAction debugNextAction;
	private DebugRunToInsertionAction debugRunToInsertionPointAction;
	private DebugTraceAction debugTraceAction;
	private DebugMarkerAction debugMarkerAction;
	private DebugStepFinishAction debugStepFinishAction;
	private static DebugVariablesView instance;

	public static DebugVariablesView getInstance() {
		return instance;
	}

	public DebugIjMacroScript getStartMacroDebugging() {
		return startMacroDebugging;
	}

	public DebugVariablesView() {
		setPartName("Variables (x)");
		instance=this;
	}

	public static Table getDebugVariablesGrid() {

		return grid;
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {

		grid = new Table(parent,
				SWT.BORDER | SWT.V_SCROLL | SWT.SCROLL_LINE | SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);

		{
			column1 = new TableColumn(grid, SWT.CENTER);
			column1.setText("Name");
			column1.setWidth(100);
		}
		{
			column2 = new TableColumn(grid, SWT.CENTER);
			column2.setText("*");
			column2.setWidth(100);
		}
		{
			column3 = new TableColumn(grid, SWT.CENTER);
			column3.setText("Value");
			column3.setWidth(100);
		}

		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);

		// Show row header

		final TableEditor editor = new TableEditor(grid);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		createActions();
		initializeToolBar();
		initializeMenu();

		/* Resize column width if shell changes! */
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (grid.isDisposed() == false) {
					Rectangle area = parent.getClientArea();
					Point preferredSize = grid.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					int width = area.width - 2 * grid.getBorderWidth();
					if (preferredSize.y > area.height + grid.getHeaderHeight()) {
						// Subtract the scrollbar width from the total column width
						// if a vertical scrollbar will be required
						Point vBarSize = grid.getVerticalBar().getSize();
						width -= vBarSize.x;
					}
					Point oldSize = grid.getSize();
					if (oldSize.x > area.width) {
						// table is getting smaller so make the columns
						// smaller first and then resize the table to
						// match the client area width
						column1.setWidth(width / 3);
						column2.setWidth(width / 3);
						column3.setWidth(width / 3);

						grid.setSize(area.width, area.height);
					} else {
						// table is getting bigger so make the table
						// bigger first and then make the columns wider
						// to match the client area width
						grid.setSize(area.width, area.height);
						column1.setWidth(width / 3);
						column2.setWidth(width / 3);
						column3.setWidth(width / 3);

					}
				}
				parent.layout();
			}
		});

		grid.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {

				int selection = grid.getSelectionIndex();

				Interpreter interp = Interpreter.getInstance();
				if (interp != null)
					//interp.setVariable("aa", 2);
					interp.showArrayInspector(selection);

			}

		});

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
		startMacroDebugging = new DebugIjMacroScript(this);
		toolbarManager.add(startMacroDebugging);
		debugStopAction = new DebugStopAction(this);
		toolbarManager.add(debugStopAction);
		debugStopAction.setEnabled(false);
		debugNextAction = new DebugNextAction(this);
		toolbarManager.add(debugNextAction);
		debugRunToInsertionPointAction = new DebugRunToInsertionAction(this);
		toolbarManager.add(debugRunToInsertionPointAction);
		debugTraceAction = new DebugTraceAction(this);
		toolbarManager.add(debugTraceAction);
		debugMarkerAction = new DebugMarkerAction(this);
		toolbarManager.add(debugMarkerAction);
		debugStepFinishAction = new DebugStepFinishAction(this);
		toolbarManager.add(debugStepFinishAction);
		toolbarManager.add(new DebugSetVariableAction());

	}

	public DebugStopAction getDebugStopAction() {
		return debugStopAction;
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
