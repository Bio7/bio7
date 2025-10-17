/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.batch;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class BatchIODialog extends Dialog {
	private Text text = null;

	private Button button = null;

	private Text text1 = null;

	protected Button button1 = null;

	protected static Label label5 = null;

	private Button button2 = null;

	private Label label6 = null;

	private Label label4 = null;

	private Label label7 = null;

	private static String outputDirectory;
	
	protected static String inputDirectory;

	protected String fileName;

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public BatchIODialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);
		text = new Text(container, SWT.BORDER);
		text.setBounds(9, 11, 76, 19);
		text.setBounds(5, 25, 246, 19);
		button = new Button(container, SWT.NONE);
		button.setBounds(9, 37, 45, 23);
		button.setBounds(272, 21, 66, 23);
		button.setText("Source"); //$NON-NLS-1$
		button.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load"); //$NON-NLS-1$

				String[] filterExt = { "*.avi", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterExtensions(filterExt);
				text.setText(fd.open());
				inputDirectory = fd.getFilterPath();
			}
		});
		text1 = new Text(container, SWT.BORDER);
		text1.setBounds(9, 67, 76, 19);
		text1.setBounds(5, 53, 246, 19);
		button2 = new Button(container, SWT.NONE);
		button2.setBounds(9, 93, 66, 23);
		button2.setBounds(272, 49, 66, 23);
		button2.setText("Destination"); //$NON-NLS-1$
		button2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.SAVE);
				fd.setText("Save"); //$NON-NLS-1$

				String[] filterExt = { "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterExtensions(filterExt);
				text1.setText(fd.open());
				outputDirectory = fd.getFilterPath();
				fileName = fd.getFileName();
			}
		});
		label6 = new Label(container, SWT.NONE);
		label6.setBounds(9, 123, 0, 13);
		label6.setBounds(238, 174, 0, 13);
		label6.setText(""); //$NON-NLS-1$
		label4 = new Label(container, SWT.NONE);
		label4.setBounds(9, 143, 0, 13);
		label4.setBounds(5, 194, 0, 13);
		label4.setText("");
		label7 = new Label(container, SWT.NONE);
		label7.setBounds(9, 193, 0, 13);
		label7.setBounds(5, 240, 0, 13);
		label7.setText("");

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
	public static String[]getIODirectory(){
		String[]io=new String[2];
		io[0]=inputDirectory;
		io[1]=outputDirectory;
		
		return io;
	}

}
