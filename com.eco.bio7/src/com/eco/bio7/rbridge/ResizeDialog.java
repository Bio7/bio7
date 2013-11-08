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

		
		spinner = new Spinner(container, SWT.BORDER);
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

		
		spinner_1 = new Spinner(container, SWT.BORDER);
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

		Label widthLabel;
		widthLabel = new Label(container, SWT.NONE);
		widthLabel.setText("Width");

		Label heightLabel;
		heightLabel = new Label(container, SWT.NONE);
		heightLabel.setText("Height");
		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(GroupLayout.LEADING)
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(spinner, GroupLayout.PREFERRED_SIZE, 102, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.RELATED)
					.add(widthLabel, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.RELATED)
					.add(spinner_1, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.RELATED)
					.add(heightLabel, GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(GroupLayout.LEADING)
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(spinner_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(heightLabel)
						.add(widthLabel)
						.add(spinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(47, Short.MAX_VALUE))
		);
		groupLayout.linkSize(new org.eclipse.swt.widgets.Control[] {spinner, spinner_1}, GroupLayout.HORIZONTAL);
		container.setLayout(groupLayout);

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
		return new Point(319, 91);
	}
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Resize");
	}

}
