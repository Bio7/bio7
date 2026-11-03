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

import static com.eco.bio7.rbridge.RServeUtil.listRObjects;

import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.themes.CustomTheme;
import com.eco.bio7.util.Util;

public class CSVImport {

	public Shell sShell = null;
	private Grid table = null;
	private Text textSeperator = null;
	private Text textFile = null;
	private Button button = null;
	private Button button2 = null;
	private Button button3 = null;
	protected static String file;
	private Label seperatorLabel = null;
	private Label label1 = null;
	private Label label2 = null;
	private Spinner spinner = null;
	protected Grid grid;
	private Text textQuotedChar;
	private Button kommaButton;
	private Button semicolonButton;
	private Button otherButton;
	protected char c;
	protected char c2;
	protected int sel;
	protected String name;
	private Label lblSeperator;
	private Label label_1;
	private Button strictQuotes;
	private Label lblExtra;
	private Button ignoreWs;
	private Button keepCr;
	private Text textEscape;
	private Label lblNewLabel;
	private Label labelError;
	private Label lblMessage;
	private Button btnClipboardData;
	private boolean noProblemsDetected;
	private Button ignoreQuotations;
	private Button trimValues;
	private Button skipEmptyRows;
	private Button titlesFromFirstRow;
	private Text textComment;
	private Combo comboEncoding;
	/* The encodings of the CSV files */
	private static final String[] ENCODINGS = { "UTF-8", "ISO-8859-1", "windows-1252", "UTF-16", "x-MacRoman", "US-ASCII" };

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
		/* On Linux GTK this flag cares about the right background! */
		sShell.setBackgroundMode(SWT.INHERIT_DEFAULT);
		sShell.setText("CSV Reader");
		/* Styled with the custom themes like the modal dialogs */
		sShell.setData("org.eclipse.e4.ui.css.CssClassName", CustomTheme.DIALOG_CLASS);
		sShell.setSize(new Point(560, 900));
		GridLayout layout = new GridLayout(3, true);
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		layout.verticalSpacing = 8;
		sShell.setLayout(layout);

		textFile = new Text(sShell, SWT.BORDER);
		/* The text fields have their normal height */
		GridData gd_text3 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		textFile.setLayoutData(gd_text3);
		button = new Button(sShell, SWT.NONE);
		/* The buttons have their normal height */
		GridData gd_button = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		button.setLayoutData(gd_button);
		button.setText("File");
		button.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				String f = openFile(sShell.getDisplay());
				if (f != null) {
					textFile.setText(f);
				}
				modified();

			}
		});

		btnClipboardData = new Button(sShell, SWT.CHECK);
		btnClipboardData.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				textFile.setText("");
				modified();
			}
		});
		GridData gd_btnClipboardData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
		btnClipboardData.setLayoutData(gd_btnClipboardData);
		btnClipboardData.setText("From clipboard");
		new Label(sShell, SWT.NONE);
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

		lblSeperator = new Label(sShell, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSeperator.setText("Seperator");
		lblSeperator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		seperatorLabel = new Label(sShell, SWT.NONE);
		GridData gd_seperatorLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_seperatorLabel.verticalIndent = 6;
		seperatorLabel.setLayoutData(gd_seperatorLabel);
		seperatorLabel.setText("Separator character:");
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);

		final Button tabButton = new Button(sShell, SWT.CHECK);
		GridData gd_tabButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		tabButton.setLayoutData(gd_tabButton);
		tabButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (tabButton.getSelection()) {
					textSeperator.setEnabled(false);
					textSeperator.setText("\t");
					kommaButton.setSelection(false);
					semicolonButton.setSelection(false);
					otherButton.setSelection(false);
					// tabButton.setSelection(false);
				} else {
					textSeperator.setText("");
				}
				modified();
			}
		});
		tabButton.setText("Tab");

		semicolonButton = new Button(sShell, SWT.CHECK);
		GridData gd_semicolonButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		semicolonButton.setLayoutData(gd_semicolonButton);
		semicolonButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (semicolonButton.getSelection()) {
					textSeperator.setEnabled(false);
					textSeperator.setText(";");
					kommaButton.setSelection(false);
					otherButton.setSelection(false);
					tabButton.setSelection(false);
				} else {
					textSeperator.setText("");
				}
				modified();
			}
		});
		semicolonButton.setText("Semicolon");
		new Label(sShell, SWT.NONE);

		kommaButton = new Button(sShell, SWT.CHECK);
		kommaButton.setSelection(true);
		GridData gd_kommaButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		kommaButton.setLayoutData(gd_kommaButton);
		kommaButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (kommaButton.getSelection()) {
					textSeperator.setEnabled(false);
					textSeperator.setText(",");
					semicolonButton.setSelection(false);
					otherButton.setSelection(false);
					tabButton.setSelection(false);
				} else {
					textSeperator.setText("");
				}
				modified();
			}
		});
		kommaButton.setText("Comma");

		otherButton = new Button(sShell, SWT.CHECK);
		GridData gd_otherButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		otherButton.setLayoutData(gd_otherButton);
		otherButton.setToolTipText("Default = Whitespace");
		otherButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (otherButton.getSelection()) {
					textSeperator.setEnabled(true);
					// textSeperator.setText(" ");
					kommaButton.setSelection(false);
					semicolonButton.setSelection(false);
					tabButton.setSelection(false);
				} else {
					textSeperator.setEnabled(false);
				}
				modified();
			}
		});
		otherButton.setText("Other");
		new Label(sShell, SWT.NONE);

		textSeperator = new Text(sShell, SWT.BORDER);
		GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		textSeperator.setLayoutData(gd_text);
		textSeperator.setEnabled(false);
		textSeperator.setTextLimit(1);
		textSeperator.setText(",");
		textSeperator.addModifyListener(new org.eclipse.swt.events.ModifyListener() {
			public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
				modified();
			}
		});
		Button detectButton = new Button(sShell, SWT.NONE);
		detectButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		detectButton.setText("Detect");
		detectButton.setToolTipText("Detect the separator (comma, semicolon, tab or pipe) from the first lines");
		detectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				detectSeparator(tabButton);
			}
		});
		new Label(sShell, SWT.NONE);

		lblExtra = new Label(sShell, SWT.NONE);
		GridData gd_lblExtra = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblExtra.verticalIndent = 6;
		lblExtra.setLayoutData(gd_lblExtra);
		lblExtra.setText("Extra:");
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);

		strictQuotes = new Button(sShell, SWT.CHECK);
		GridData gd_strictQuotes = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		strictQuotes.setLayoutData(gd_strictQuotes);
		strictQuotes.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				modified();
			}
		});
		strictQuotes.setText("Strict Quotes");

		ignoreWs = new Button(sShell, SWT.CHECK);
		GridData gd_ignoreWs = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		ignoreWs.setLayoutData(gd_ignoreWs);
		ignoreWs.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				modified();
			}
		});
		ignoreWs.setToolTipText("Ignore Leading Whitespace");
		ignoreWs.setText("Ignore Leading WS");

		keepCr = new Button(sShell, SWT.CHECK);
		GridData gd_keepCr = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		keepCr.setLayoutData(gd_keepCr);
		keepCr.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				modified();
			}
		});
		keepCr.setToolTipText("Keep Carriage Returns");
		keepCr.setText("Keep CR");

		ignoreQuotations = createOption("Ignore Quotations", "Quote characters are read as normal characters");
		trimValues = createOption("Trim Values", "Remove the whitespace around the values");
		skipEmptyRows = createOption("Skip Empty Rows", "Rows without values are not loaded");
		titlesFromFirstRow = createOption("Column Titles", "The first row is also used for the column titles of the sheet (the row stays in the sheet, e.g., as a header for R)");
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);

		label_1 = new Label(sShell, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		label_1.setLayoutData(gd_label_1);

		lblNewLabel = new Label(sShell, SWT.NONE);
		GridData gd_lblNewLabel = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel.verticalIndent = 6;
		lblNewLabel.setLayoutData(gd_lblNewLabel);
		lblNewLabel.setText("Escape character:");
		label1 = new Label(sShell, SWT.NONE);
		GridData gd_label1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_label1.verticalIndent = 6;
		label1.setLayoutData(gd_label1);
		label1.setText("Quote character:");
		label2 = new Label(sShell, SWT.NONE);
		GridData gd_label2 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_label2.verticalIndent = 6;
		label2.setLayoutData(gd_label2);
		label2.setText("Start at line:");

		textEscape = new Text(sShell, SWT.BORDER);
		textEscape.setText("\\");
		GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		textEscape.setTextLimit(1);
		textEscape.setLayoutData(gd_text_1);
		textEscape.addModifyListener(new org.eclipse.swt.events.ModifyListener() {
			public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
				modified();
			}
		});
		textQuotedChar = new Text(sShell, SWT.BORDER);
		textQuotedChar.setText("\"");
		textQuotedChar.setTextLimit(1);
		textQuotedChar.addModifyListener(new org.eclipse.swt.events.ModifyListener() {
			public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
				modified();
			}
		});
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		textQuotedChar.setLayoutData(gd_combo);
		spinner = new Spinner(sShell, SWT.BORDER);
		spinner.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				modified();
			}
		});
		GridData gd_spinner = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		spinner.setLayoutData(gd_spinner);
		spinner.setMaximum(1000000);

		Label lblComment = new Label(sShell, SWT.NONE);
		lblComment.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblComment.setText("Comment character:");
		Label lblEncoding = new Label(sShell, SWT.NONE);
		lblEncoding.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblEncoding.setText("Encoding:");
		new Label(sShell, SWT.NONE);

		textComment = new Text(sShell, SWT.BORDER);
		textComment.setTextLimit(1);
		textComment.setToolTipText("Lines starting with this character (e.g., #) are skipped. Empty = no comments");
		textComment.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textComment.addModifyListener(e -> modified());
		comboEncoding = new Combo(sShell, SWT.READ_ONLY);
		comboEncoding.setItems(ENCODINGS);
		comboEncoding.select(0);
		comboEncoding.setToolTipText("The character encoding of the file");
		comboEncoding.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboEncoding.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				modified();
			}
		});
		new Label(sShell, SWT.NONE);

		lblMessage = new Label(sShell, SWT.NONE);
		GridData gd_lblMessage = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblMessage.verticalIndent = 6;
		lblMessage.setLayoutData(gd_lblMessage);
		lblMessage.setText("Help message:");
		new Label(sShell, SWT.NONE);
		new Label(sShell, SWT.NONE);

		labelError = new Label(sShell, SWT.NONE);
		labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		GridData gd_labelError = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		labelError.setLayoutData(gd_labelError);
		labelError.setText("Please select a file or enable the clipboard!");
		noProblemsDetected = false;

		button2 = new Button(sShell, SWT.NONE);
		GridData gd_button2 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		button2.setLayoutData(gd_button2);
		button2.setText("OK");
		button2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				if (noProblemsDetected) {
					String fileToRead = textFile.getText();
					LoadCsvJob job = new LoadCsvJob(CSVImport.this, fileToRead);
					// job.setSystem(true);
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								Display.getDefault().syncExec(new Runnable() {

									public void run() {
										table.dispose();
										sShell.dispose();
									}
								});

							} else {
								Display.getDefault().syncExec(new Runnable() {

									public void run() {
										table.dispose();
										sShell.dispose();
									}
								});
							}
						}
					});
					job.schedule();

				} else {
					Bio7Dialog.message("Problems detected! Please read the parser message!");
				}

			}
		});
		new Label(sShell, SWT.NONE);
		button3 = new Button(sShell, SWT.NONE);
		GridData gd_button3 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		button3.setLayoutData(gd_button3);
		button3.setText("Cancel");
		button3.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				table.dispose();
				sShell.dispose();

			}
		});

	}

	private void modified() {
		table.setRedraw(false);
		for (int i = 0; i < table.getItemCount(); i++) {
			for (int j = 0; j < table.getColumnCount(); j++) {
				table.getItem(i).setText(j, "");
			}
		}
		table.setRedraw(true);

		/* Detect if char is given! */

		if (textSeperator.getText().length() <= 0 || textQuotedChar.getText().length() <= 0
				|| textEscape.getText().length() <= 0) {
			labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
			labelError.setText("The separator, quote, and escape characters could'nt be empty!");
			noProblemsDetected = false;
			return;
		}

		else {
			labelError.setForeground(lblMessage.getForeground());
			labelError.setText("No problems detected!");
			noProblemsDetected = true;
		}

		char seperator = textSeperator.getText().charAt(0);
		char quoteChar = textQuotedChar.getText().charAt(0);
		char escape = textEscape.getText().charAt(0);

		if (anyCharactersAreTheSame(seperator, quoteChar, escape)) {
			labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
			labelError.setText("The separator, quote, and escape characters must be different!");

			// Bio7Dialog.message("The separator, quote, and escape
			// characters must be different!");
			noProblemsDetected = false;
			return;
		} else {
			labelError.setForeground(lblMessage.getForeground());
			labelError.setText("No problems detected!");
			noProblemsDetected = true;
		}

		char comment = getComment();
		if (comment != '\0' && (comment == seperator || comment == quoteChar || comment == escape)) {
			labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
			labelError.setText("The comment character must differ from the other characters!");
			noProblemsDetected = false;
			return;
		}

		String text;
		if (btnClipboardData.getSelection()) {
			text = getClipboardText();
			if (text == null || text.isEmpty()) {
				labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				labelError.setText("The clipboard is empty or contains no text!");
				noProblemsDetected = false;
				return;
			}
		} else {
			String fil = textFile.getText();
			if (fil == null || fil.isEmpty()) {
				labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				labelError.setText("Please select a file or the clipboard!");
				noProblemsDetected = false;
				return;
			}
			try {
				text = CsvReading.readText(fil, getCharset(), CsvReading.PREVIEW_CHARS);
			} catch (Exception e) {
				labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				labelError.setText("The file could not be read: " + e.getMessage());
				noProblemsDetected = false;
				return;
			}
		}

		/* The preview with the same options as the import */
		List<String[]> rows;
		try {
			rows = CsvReading.parse(text, getOptions(), table.getItemCount());
		} catch (Exception e) {
			labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
			labelError.setText("Parser problem: " + e.getMessage());
			noProblemsDetected = false;
			return;
		}
		table.setRedraw(false);
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumn(i).setText("C " + (i + 1));
		}
		for (int r = 0; r < rows.size() && r < table.getItemCount(); r++) {
			String[] row = rows.get(r);
			for (int i = 0; i < row.length && i < table.getColumnCount(); i++) {
				table.getItem(r).setText(i, row[i]);
				if (r == 0 && titlesFromFirstRow.getSelection() && !row[i].trim().isEmpty()) {
					table.getColumn(i).setText(row[i].trim());
				}
			}
		}
		table.setRedraw(true);
		labelError.setForeground(lblMessage.getForeground());
		labelError.setText(rows.isEmpty() ? "No rows found with the selected options!" : "No problems detected!");
	}

	/* A check box option of the parser */
	private Button createOption(String text, String tooltip) {
		Button b = new Button(sShell, SWT.CHECK);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		b.setLayoutData(gd);
		b.setText(text);
		b.setToolTipText(tooltip);
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				modified();
			}
		});
		return b;
	}

	/* Detects the separator from the first lines of the file or clipboard */
	private void detectSeparator(Button tabButton) {
		String text = null;
		if (btnClipboardData.getSelection()) {
			text = getClipboardText();
		} else if (!textFile.getText().isEmpty()) {
			try {
				text = CsvReading.readText(textFile.getText(), getCharset(), 64 * 1024);
			} catch (Exception e) {
				text = null;
			}
		}
		if (text == null || text.isEmpty()) {
			labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
			labelError.setText("Please select a file or the clipboard!");
			return;
		}
		char quote = textQuotedChar.getText().isEmpty() ? '"' : textQuotedChar.getText().charAt(0);
		char sep = CsvReading.detectSeparator(text, quote, getComment(), spinner.getSelection());
		if (sep == '\0') {
			labelError.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
			labelError.setText("No separator detected (comma, semicolon, tab or pipe)!");
			return;
		}
		tabButton.setSelection(sep == '\t');
		semicolonButton.setSelection(sep == ';');
		kommaButton.setSelection(sep == ',');
		otherButton.setSelection(sep == '|');
		textSeperator.setEnabled(sep == '|');
		/* Calls modified() */
		textSeperator.setText(String.valueOf(sep));
	}

	/*
	 * The following methods were copied from the CSV source:
	 * 
	 * Copyright 2005 Bytecode Pty Ltd.
	 * 
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
	 * use this file except in compliance with the License. You may obtain a copy of
	 * the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations under
	 * the License.
	 */

	public Button getBtnClipboardData() {
		return btnClipboardData;
	}

	public void setBtnClipboardData(Button btnClipboardData) {
		this.btnClipboardData = btnClipboardData;
	}

	/**
	 * Checks to see if any two of the three characters are the same. This is
	 * because in opencsv the separator, quote, and escape characters must the
	 * different.
	 *
	 * @param separator The defined separator character
	 * @param quotechar The defined quotation cahracter
	 * @param escape    The defined escape character
	 * @return True if any two of the three are the same.
	 */
	private boolean anyCharactersAreTheSame(char separator, char quotechar, char escape) {
		return isSameCharacter(separator, quotechar) || isSameCharacter(separator, escape)
				|| isSameCharacter(quotechar, escape);
	}

	/**
	 * Checks that the two characters are the same and are not the defined
	 * NULL_CHARACTER.
	 * 
	 * @param c1 First character
	 * @param c2 Second character
	 * @return True if both characters are the same and are not the defined
	 *         NULL_CHARACTER
	 */
	private boolean isSameCharacter(char c1, char c2) {
		return c1 != '\0' && c1 == c2;
	}

	public boolean isClipboard() {
		return btnClipboardData.getSelection();
	}

	public char getSep() {
		return textSeperator.getText().charAt(0);
	}

	public char getQuot() {

		return textQuotedChar.getText().charAt(0);

	}

	public char getEsc() {
		return textEscape.getText().charAt(0);

	}

	public int getSelectedRange() {
		return spinner.getSelection();

	}

	public boolean getStrictQuotes() {
		return strictQuotes.getSelection();

	}

	public boolean getIgnoreWs() {
		return ignoreWs.getSelection();
	}

	public boolean getKeepCr() {
		return keepCr.getSelection();
	}

	/** The comment character or '\0'. */
	public char getComment() {
		return textComment.getText().isEmpty() ? '\0' : textComment.getText().charAt(0);
	}

	public Charset getCharset() {
		try {
			return Charset.forName(comboEncoding.getText());
		} catch (Exception e) {
			return Charset.forName("UTF-8");
		}
	}

	/** The text of the clipboard or null (must be called in the UI thread). */
	public String getClipboardText() {
		Clipboard clipboard = new Clipboard(Util.getDisplay());
		try {
			return (String) clipboard.getContents(TextTransfer.getInstance());
		} finally {
			clipboard.dispose();
		}
	}

	/** The options of the dialog (must be called in the UI thread). */
	public CsvReading.Options getOptions() {
		CsvReading.Options o = new CsvReading.Options();
		o.separator = getSep();
		o.quote = getQuot();
		o.escape = getEsc();
		o.comment = getComment();
		o.skipLines = getSelectedRange();
		o.strictQuotes = getStrictQuotes();
		o.ignoreLeadingWhiteSpace = getIgnoreWs();
		o.keepCarriageReturn = getKeepCr();
		o.ignoreQuotations = ignoreQuotations.getSelection();
		o.trimValues = trimValues.getSelection();
		o.skipEmptyRows = skipEmptyRows.getSelection();
		o.titlesFromFirstRow = titlesFromFirstRow.getSelection();
		o.charset = getCharset();
		return o;
	}

}
