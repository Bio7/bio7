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

import com.opencsv.CSVReader;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

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
	private Label lblSeperator;
	private Label label_1;

	/**
	 * This method initializes combo
	 * 
	 */
	private void createCombo() {
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
		sShell = new Shell(SWT.CLOSE | SWT.RESIZE | SWT.TITLE);
		sShell.setText("CSV Reader");
		sShell.setSize(new Point(518, 667));
		sShell.setLayout(new GridLayout(3, true));
		
		text3 = new Text(sShell, SWT.BORDER);
		GridData gd_text3 = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_text3.heightHint = 25;
		text3.setLayoutData(gd_text3);
		button = new Button(sShell, SWT.NONE);
		GridData gd_button = new GridData(SWT.FILL, SWT.FILL, true, false, 1,
				1);
		gd_button.heightHint = 35;
		button.setLayoutData(gd_button);
		button.setText("File");
		button.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				String f = openFile(sShell.getDisplay());
				if (f != null) {
					text3.setText(f);
				}

			}
		});
		table = new Grid(sShell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 5);
		gd_table.widthHint = 411;
		table.setLayoutData(gd_table);
		table.setHeaderVisible(true);
		table.setRowHeaderVisible(true);
		table.setLinesVisible(true);
		for (int i = 0; i < 20; i++) {
			GridColumn column = new GridColumn(table, SWT.NONE);

			column.setText("C " + (i + 1));
			column.setWidth(50);

		}
		for (int i = 0; i < 20; i++) {
			new GridItem(table, SWT.NONE);

		}
										button1 = new Button(sShell, SWT.NONE);
										GridData gd_button1 = new GridData(SWT.FILL, SWT.FILL, false, false, 3,
												1);
										gd_button1.heightHint = 35;
										button1.setLayoutData(gd_button1);
										button1.setText("Preview");
										button1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
											public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
												modified();
											}

										});
										
										lblSeperator = new Label(sShell, SWT.SEPARATOR | SWT.HORIZONTAL);
										lblSeperator.setText("Seperator");
										lblSeperator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
										seperatorLabel = new Label(sShell, SWT.NONE);
										seperatorLabel.setText("Seperator:");
										new Label(sShell, SWT.NONE);
										new Label(sShell, SWT.NONE);
								
										final Button tabButton = new Button(sShell, SWT.CHECK);
										GridData gd_tabButton = new GridData(SWT.FILL, SWT.FILL, false, false,
												1, 1);
										gd_tabButton.heightHint = 30;
										tabButton.setLayoutData(gd_tabButton);
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
		
				semicolonButton = new Button(sShell, SWT.CHECK);
				GridData gd_semicolonButton = new GridData(SWT.FILL, SWT.FILL, false,
						false, 1, 1);
				gd_semicolonButton.heightHint = 30;
				semicolonButton.setLayoutData(gd_semicolonButton);
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
		new Label(sShell, SWT.NONE);
		
				kommaButton = new Button(sShell, SWT.CHECK);
				GridData gd_kommaButton = new GridData(SWT.FILL, SWT.FILL, false,
						false, 1, 1);
				gd_kommaButton.heightHint = 30;
				kommaButton.setLayoutData(gd_kommaButton);
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
		
				otherButton = new Button(sShell, SWT.CHECK);
				GridData gd_otherButton = new GridData(SWT.FILL, SWT.FILL, false,
						false, 1, 1);
				gd_otherButton.heightHint = 30;
				otherButton.setLayoutData(gd_otherButton);
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
		new Label(sShell, SWT.NONE);
		
				text = new Text(sShell, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
				text.setEnabled(false);
				text.setTextLimit(1);
				text.setText("\t");
				text.addModifyListener(new org.eclipse.swt.events.ModifyListener() {
					public void modifyText(org.eclipse.swt.events.ModifyEvent e) {

					}
				});
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		
		label_1 = new Label(sShell, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		combo = new Combo(sShell, SWT.NONE);
		GridData gd_combo = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_combo.heightHint = 25;
		combo.setLayoutData(gd_combo);
		combo.setItems(new String[] { " ", "\"", "'" });
		combo.select(0);
		label1 = new Label(sShell, SWT.NONE);
		GridData gd_label1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1,
				1);
		gd_label1.heightHint = 35;
		label1.setLayoutData(gd_label1);
		label1.setText("Text delimiter");
		new Label(sShell, SWT.NONE);
		spinner = new Spinner(sShell, SWT.NONE);
		GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false, 1,
				1);
		gd_spinner.heightHint = 25;
		spinner.setLayoutData(gd_spinner);
		spinner.setMaximum(1000000);
		label2 = new Label(sShell, SWT.NONE);
		GridData gd_label2 = new GridData(SWT.FILL, SWT.FILL, false, false, 1,
				1);
		gd_label2.heightHint = 35;
		label2.setLayoutData(gd_label2);
		label2.setText("At line");
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);
		
				button2 = new Button(sShell, SWT.NONE);
				GridData gd_button2 = new GridData(SWT.FILL, SWT.FILL, false, false, 1,
						1);
				gd_button2.heightHint = 35;
				button2.setLayoutData(gd_button2);
				button2.setText("OK");
				button2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						fileToRead = text3.getText();
						name = new File(fileToRead).getName();
						c = text.getText().charAt(0);
						c2 = combo.getItem(combo.getSelectionIndex()).charAt(0);
						sel = spinner.getSelection();
						LoadCsvJob job = new LoadCsvJob(fileToRead, c, c2, sel, name);
						// job.setSystem(true);
						job.schedule();

						sShell.dispose();

					}
				});
		new Label(sShell, SWT.NONE);
		button3 = new Button(sShell, SWT.NONE);
		GridData gd_button3 = new GridData(SWT.FILL, SWT.FILL, false, false, 1,
				1);
		gd_button3.heightHint = 35;
		button3.setLayoutData(gd_button3);
		button3.setText("Cancel");
		button3.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				sShell.dispose();

			}
		});
		createCombo();

	}

	private void modified() {
		table.clearAll(true);
		CSVReader reader = null;
		if (text.getText().length() > 0
				&& combo.getItem(combo.getSelectionIndex()).length() > 0) {
			FileReader fi = null;
			try {
				fi = new FileReader(text3.getText());

				reader = new CSVReader(fi, text.getText().charAt(0), combo
						.getItem(combo.getSelectionIndex()).charAt(0),
						spinner.getSelection());

				String[] nextLine;
				int count = 0;
				try {
					while ((nextLine = reader.readNext()) != null) {
						if (count < table.getItemCount()) {

							for (int i = 0; i < 20; i++) {
								if (i < nextLine.length) {
									table.getItem(count)
											.setText(i, nextLine[i]);
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
