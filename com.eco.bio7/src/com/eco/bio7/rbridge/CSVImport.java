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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import au.com.bytecode.opencsv.CSVReader;

public class CSVImport {

	public Shell sShell = null; 
	private Grid table = null;
	private Text text = null;
	private Text text3 = null;
	private Button button = null;
	private Button button1 = null;
	private Button button2 = null;
	private Button button3 = null;
	protected static String file; 
	private Label seperatorLabel = null;
	private Label label1 = null;
	private Label label2 = null;
	private Spinner spinner = null;
	protected Grid grid;
	private Combo combo = null;
	private Button kommaButton;
	private Button semicolonButton;
	private Button otherButton;
	private String[] nextLine2;
	private int count;
	private int sizey;
	private int sizex;
	private FileReader fi;
	private CSVReader reader;
	private String fileToRead;
	protected char c;
	protected char c2;
	protected int sel;
	protected String name;

	/**
	 * This method initializes combo
	 * 
	 */
	private void createCombo() {
		combo = new Combo(sShell, SWT.NONE);
		combo.setBounds(new Rectangle(317, 177, 46, 21));
		combo.setItems(new String[] { " ", "\"", "'" });
		combo.select(0);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Display display = Display.getDefault();
		CSVImport thisClass = new CSVImport();
		thisClass.createSShell();
		thisClass.sShell.open();

		while (!thisClass.sShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	public static String openFile(Display display) {

		display.syncExec(new Runnable() {

			public void run() {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load");

				String[] filterExt = { "*.*" };
				fd.setFilterExtensions(filterExt);
				file = fd.open();
			}
		});
		return file;
	}

	/**
	 * This method initializes sShell
	 */
	void createSShell() {
		sShell = new Shell(SWT.CLOSE | SWT.TITLE | SWT.MIN | SWT.MAX);
		sShell.setText("CSV Reader");
		sShell.setSize(new Point(506, 393));
		sShell.setLayout(null);
		table = new Grid(sShell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		table.setHeaderVisible(true);
		table.setRowHeaderVisible(true);
		table.setLinesVisible(true);
		table.setBounds(new Rectangle(4, 68, 307, 254));
		for (int i = 0; i < 20; i++) {
			GridColumn column = new GridColumn(table, SWT.NONE);

			column.setText("C " + (i + 1));
			column.setWidth(50);

		}
		for (int i = 0; i < 20; i++) {
			new GridItem(table, SWT.NONE);

		}

		text = new Text(sShell, SWT.BORDER);
		text.setEnabled(false);
		text.setBounds(new Rectangle(317, 152, 46, 19));
		text.setTextLimit(1);
		text.setText("\t");
		text.addModifyListener(new org.eclipse.swt.events.ModifyListener() {
			public void modifyText(org.eclipse.swt.events.ModifyEvent e) {

			}
		});
		text3 = new Text(sShell, SWT.BORDER);
		text3.setBounds(new Rectangle(5, 18, 301, 28));
		button = new Button(sShell, SWT.NONE);
		button.setBounds(new Rectangle(317, 18, 66, 28));
		button.setText("File");
		button.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				String f = openFile(sShell.getDisplay());
				if (f != null) {
					text3.setText(f);
				}

			}
		});
		button1 = new Button(sShell, SWT.NONE);
		button1.setBounds(new Rectangle(317, 232, 66, 28));
		button1.setText("Preview");
		button1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				modified();
			}

		});

		button2 = new Button(sShell, SWT.NONE);
		button2.setBounds(new Rectangle(4, 328, 71, 28));
		button2.setText("OK");
		button2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				fileToRead = text3.getText();
				name = new File(fileToRead).getName();
				c = text.getText().charAt(0);
				c2 = combo.getItem(combo.getSelectionIndex()).charAt(0);
				sel = spinner.getSelection();
				LoadCsvJob job = new LoadCsvJob(fileToRead, c, c2, sel, name);
				//job.setSystem(true);
				job.schedule();

				sShell.dispose();

			}
		});
		button3 = new Button(sShell, SWT.NONE);
		button3.setBounds(new Rectangle(239, 328, 72, 28));
		button3.setText("Cancel");
		seperatorLabel = new Label(sShell, SWT.NONE);
		seperatorLabel.setBounds(new Rectangle(317, 65, 55, 20));
		seperatorLabel.setText("Seperator:");
		label1 = new Label(sShell, SWT.NONE);
		label1.setBounds(new Rectangle(369, 178, 71, 20));
		label1.setText("Text delimiter");
		label2 = new Label(sShell, SWT.NONE);
		label2.setBounds(new Rectangle(369, 204, 71, 20));
		label2.setText("At line");
		spinner = new Spinner(sShell, SWT.NONE);
		spinner.setMaximum(1000000);
		spinner.setBounds(new Rectangle(317, 206, 46, 20));
		createCombo();
		button3.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				sShell.dispose();

			}
		});

		final Button tabButton = new Button(sShell, SWT.CHECK);
		tabButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (tabButton.getSelection()) {
					text.setEnabled(false);
					text.setText("\t");
					kommaButton.setSelection(false);
					semicolonButton.setSelection(false);
					otherButton.setSelection(false);
					// tabButton.setSelection(false);
				} else {
					text.setText("");
				}
			}
		});
		tabButton.setText("Tab");
		tabButton.setBounds(317, 91, 66, 16);

		kommaButton = new Button(sShell, SWT.CHECK);
		kommaButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (kommaButton.getSelection()) {
					text.setEnabled(false);
					text.setText(",");
					semicolonButton.setSelection(false);
					otherButton.setSelection(false);
					tabButton.setSelection(false);
				} else {
					text.setText("");
				}
			}
		});
		kommaButton.setText("Komma");
		kommaButton.setBounds(317, 117, 66, 16);

		semicolonButton = new Button(sShell, SWT.CHECK);
		semicolonButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (semicolonButton.getSelection()) {
					text.setEnabled(false);
					text.setText(";");
					kommaButton.setSelection(false);
					otherButton.setSelection(false);
					tabButton.setSelection(false);
				} else {
					text.setText("");
				}
			}
		});
		semicolonButton.setText("Semicolon");
		semicolonButton.setBounds(389, 91, 93, 16);

		otherButton = new Button(sShell, SWT.CHECK);
		otherButton.setToolTipText("Default = Whitespace");
		otherButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (otherButton.getSelection()) {
					text.setEnabled(true);
					text.setText(" ");
					kommaButton.setSelection(false);
					semicolonButton.setSelection(false);
					tabButton.setSelection(false);
				} else {
					text.setEnabled(false);
				}
			}
		});
		otherButton.setText("Other");
		otherButton.setBounds(389, 117, 55, 16);

	}

	private void modified() {
		table.clearAll(true);
		CSVReader reader = null;
		if (text.getText().length() > 0 && combo.getItem(combo.getSelectionIndex()).length() > 0) {
			FileReader fi = null;
			try {
				fi = new FileReader(text3.getText());

				reader = new CSVReader(fi, text.getText().charAt(0), combo.getItem(combo.getSelectionIndex()).charAt(0), spinner.getSelection());

				String[] nextLine;
				int count = 0;
				try {
					while ((nextLine = reader.readNext()) != null) {
						if (count < table.getItemCount()) {
							
							for (int i = 0; i < 20; i++) {
								if (i < nextLine.length) {
									table.getItem(count).setText(i, nextLine[i]);
								}
							}

							count++;

						}
					}
				} catch (IOException e1) {

					e1.printStackTrace();
				}

			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}

		}
	}

	private void readTable() {

		try {
			fi = new FileReader(fileToRead);
		} catch (FileNotFoundException e2) {

			e2.printStackTrace();
		}

		reader = new CSVReader(fi, c, c2, sel);

		sizey = 0;
		sizex = 0;
		String[] nextLine;

		try {
			while ((nextLine = reader.readNext()) != null) {

				for (int i = 0; i < nextLine.length; i++) {
					if (nextLine.length > sizex) {
						sizex = nextLine.length;
					}

				}

				sizey++;

			}
		} catch (IOException e) {
			
			e.printStackTrace();
		}

		/* Now Load the File definitely ! */
		try {
			fi = new FileReader(fileToRead);
		} catch (FileNotFoundException e2) {
			
			e2.printStackTrace();
		}

	}

}
