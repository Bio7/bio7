/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
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
package com.eco.bio7.pybridge;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * The Python packages as dialog (e.g., from the Python preferences): the same
 * panel as the Python Packages view.
 */
public class PythonPackagesDialog extends Dialog {

	public PythonPackagesDialog(Shell parent) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	/** Opens the dialog for the Python of Bio7. */
	public static void open(Shell parent) {
		new PythonPackagesDialog(parent).open();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Python Packages");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(760, 720);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	@Override
	protected void buttonPressed(int id) {
		if (id == IDialogConstants.CLOSE_ID) {
			close();
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		PythonPackagesPanel panel = new PythonPackagesPanel(area);
		panel.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return area;
	}
}
