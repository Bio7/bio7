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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.layout.grouplayout.GroupLayout;
import org.eclipse.wb.swt.layout.grouplayout.LayoutStyle;

public class SizedSpreadDialog extends Dialog {

	private Spinner spinner_1;
	private Spinner spinner;
	private Grid grid;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public SizedSpreadDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		spinner = new Spinner(container, SWT.BORDER);
		spinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		spinner.setSelection(100);
		spinner.setMinimum(1);
		spinner.setMaximum(10000000);

		spinner_1 = new Spinner(container, SWT.BORDER);
		spinner_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		spinner_1.setSelection(100);
		spinner_1.setMinimum(1);
		spinner_1.setMaximum(10000000);

		Label widthLabel;
		widthLabel = new Label(container, SWT.NONE);
		widthLabel.setText("Width");

		Label heightLabel;
		heightLabel = new Label(container, SWT.NONE);
		heightLabel.setText("Height");
		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(spinner, GroupLayout.PREFERRED_SIZE, 102, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED)
						.add(widthLabel, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED)
						.add(spinner_1, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED)
						.add(heightLabel, GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout
						.createSequentialGroup()
						.addContainerGap()
						.add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(spinner_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(heightLabel)
								.add(widthLabel).add(spinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addContainerGap(47, Short.MAX_VALUE)));
		groupLayout.linkSize(new org.eclipse.swt.widgets.Control[] { spinner, spinner_1 }, GroupLayout.HORIZONTAL);
		container.setLayout(groupLayout);

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		final Button button_1 = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		final Button button = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog
	 */

	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();

			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {

			grid = RTable.getGrid();
			final int cols = spinner.getSelection();
			final int rows = spinner_1.getSelection();
			final CTabFolder ctab = RTable.getTabFolder();
			final int tabItems = ctab.getItemCount();

			if (grid != null) {

				Display display = PlatformUI.getWorkbench().getDisplay();
				display.asyncExec(new Runnable() {

					public void run() {
						grid = new Spread().spread(ctab, cols, rows, "Sheet " + (tabItems + 1));
						RTable.setGrid(grid);
					}
				});

			}
			this.okPressed();
			this.close();
			return;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(319, 141);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Size");
	}

}
