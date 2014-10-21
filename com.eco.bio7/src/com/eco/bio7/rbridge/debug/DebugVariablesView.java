package com.eco.bio7.rbridge.debug;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.nebula.widgets.grid.GridColumn;

public class DebugVariablesView extends ViewPart {

	public static final String ID = "com.eco.bio7.rbridge.debug.DebugVariablesView"; //$NON-NLS-1$
	private static Grid debugVariablesGrid;

	public static Grid getDebugVariablesGrid() {
		return debugVariablesGrid;
	}

	public DebugVariablesView() {
		setPartName("Variables (x)");
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		 debugVariablesGrid = new Grid(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		 {
		 	GridColumn gridColumn = new GridColumn(debugVariablesGrid, SWT.NONE);
		 	gridColumn.setText("Name");
		 	gridColumn.setWidth(150);
		 }
		 {
		 	GridColumn gridColumn = new GridColumn(debugVariablesGrid, SWT.NONE);
		 	gridColumn.setText("Value");
		 	gridColumn.setWidth(150);
		 }
		 {
		 	GridColumn gridColumn = new GridColumn(debugVariablesGrid, SWT.NONE);
		 	gridColumn.setText("Value");
		 	gridColumn.setWidth(150);
		 }
		 {
		 	GridColumn gridColumn = new GridColumn(debugVariablesGrid, SWT.NONE);
		 	gridColumn.setText("Traceback");
		 	gridColumn.setWidth(150);
		 }

		createActions();
		initializeToolBar();
		initializeMenu();
	}
	
	public void createItems(){
		
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
