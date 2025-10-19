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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.wb.swt.layout.grouplayout.GroupLayout;
import org.eclipse.wb.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class ResizeDialog extends Dialog {

	private Spinner spinner_1;
	private Spinner spinner;

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public ResizeDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(4, false));

		
		spinner = new Spinner(container, SWT.BORDER);
		GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner.heightHint = 30;
		spinner.setLayoutData(gd_spinner);
		spinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Grid grid = RTable.getGrid();
				if (grid != null) {

					Point[] sel = grid.getCellSelection();
					int temp = 0;
					for (Point selection : sel) {

						

						grid.getItem(selection.y).setHeight(spinner_1.getSelection());
						grid.getColumn(selection.x).setWidth(spinner.getSelection());
					}

				}
				
			
			}
		});
		spinner.setSelection(50);
		spinner.setMinimum(1);
		spinner.setMaximum(1000);

		Label widthLabel;
		widthLabel = new Label(container, SWT.NONE);
		GridData gd_widthLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_widthLabel.heightHint = 30;
		widthLabel.setLayoutData(gd_widthLabel);
		widthLabel.setText("Width");

		Label heightLabel;
		
				
				spinner_1 = new Spinner(container, SWT.BORDER);
				GridData gd_spinner_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
				gd_spinner_1.heightHint = 30;
				spinner_1.setLayoutData(gd_spinner_1);
				spinner_1.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						Grid grid = RTable.getGrid();
						if (grid != null) {

							Point[] sel = grid.getCellSelection();
							int temp = 0;
							for (Point selection : sel) {

								

								grid.getItem(selection.y).setHeight(spinner_1.getSelection());
								grid.getColumn(selection.x).setWidth(spinner.getSelection());
							}

						}
						
					
					}
				});
				spinner_1.setSelection(20);
				spinner_1.setMinimum(1);
				spinner_1.setMaximum(1000);
		heightLabel = new Label(container, SWT.NONE);
		GridData gd_heightLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_heightLabel.heightHint = 30;
		heightLabel.setLayoutData(gd_heightLabel);
		heightLabel.setText("Height");

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(492, 178);
	}
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Resize");
	}

}
