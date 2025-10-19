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

import ij.WindowManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.eclipse.swt.custom.StyledText;

public class BatchProcessImages {

	private Shell sShell = null;

	private Text text = null;

	private Button button = null;

	private Text text1 = null;

	private Button button1 = null;

	private static Label label5 = null;

	private Button button2 = null;

	private Label label6 = null;

	private Label label4 = null;

	private Label label7 = null;

	private static String outputDirectory;
	
	protected static String inputDirectory;

	private String fileName;

	

	/**
	 * This method initializes sShell
	 * 
	 * @wbp.parser.entryPoint
	 */
	public void createSShell(Display display) {
		sShell = new Shell(SWT.SHELL_TRIM | SWT.TOOL | SWT.ON_TOP);
		sShell.setText("Batch Dialog"); //$NON-NLS-1$
		sShell.setSize(new Point(376, 199));
		sShell.setLayout(null);
		text = new Text(sShell, SWT.BORDER);
		text.setBounds(5, 25, 246, 19);
		button = new Button(sShell, SWT.NONE);
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
		text1 = new Text(sShell, SWT.BORDER);
		text1.setBounds(5, 53, 246, 19);
		button2 = new Button(sShell, SWT.NONE);
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
		label6 = new Label(sShell, SWT.NONE);
		label6.setBounds(238, 174, 0, 13);
		label6.setText(""); //$NON-NLS-1$
		label4 = new Label(sShell, SWT.NONE);
		label4.setBounds(5, 194, 0, 13);
		label4.setText(""); //$NON-NLS-1$
		button1 = new Button(sShell, SWT.NONE);
		button1.setBounds(37, 106, 186, 23);
		button1.setText("Start Conversion"); //$NON-NLS-1$
		label7 = new Label(sShell, SWT.NONE);
		label7.setBounds(5, 240, 0, 13);
		label7.setText("");
		button1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

			}

		});
		sShell.open();
	}
	
	public static String[]getIODirectory(){
		String[]io=new String[2];
		io[0]=inputDirectory;
		io[1]=outputDirectory;
		
		return io;
	}

	public static Label getLabel5() {
		return label5;
	}

	public static void setLabel5(Label label5) {
		BatchProcessImages.label5 = label5;
	}

	public Shell getSShell() {
		return sShell;
	}

	public void setSShell(Shell shell) {
		sShell = shell;
	}
}
