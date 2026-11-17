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

import java.util.Random;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.rbridge.backend.RBackend;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.util.Util;
import com.eco.bio7.rffm.RException;

public class FromSpreadAction extends Action implements IMenuCreator {

	private Menu fMenu;

	protected String[][] sheetdata;

	private String[] firstChar = { ".", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };

	protected String[] head;

	protected String[] colnames;

	protected String[][] data;

	private Grid grid;

	protected String[] dataVector;

	protected String input;

	private String text;

	public FromSpreadAction() {
		setId("FromSpread");
		setToolTipText("R Menu");
		setText("R <-");

		setMenuCreator(this);
	}

	/*
	 * Assigns texts as vector of the detected type: numeric (doubles, full
	 * precision), logical (TRUE, FALSE, T, F), Date (ISO dates) or character;
	 * empty cells and "NA": NA
	 */
	private static void assignTyped(RBackend c, String name, String[] values) throws Exception {
		String type = TableParts.type(values);
		if (type.equals("numeric") && RNumbers.assign(c, name, values)) {
			return;
		}
		c.assign(name, values);
		c.eval(name + "[" + name + " == \"NA\"] <- NA");
		switch (type) {
		case "logical" -> c.eval(name + " <- as.logical(" + name + ")");
		case "date" -> c.eval(name + " <- as.Date(" + name + ")");
		default -> {
		}
		}
	}

	/*
	 * Without head: if the first selected row looks like names (texts above
	 * numbers), the question whether it are the names (a data frame, variables)
	 */
	public static boolean askHead(Grid g, String kind, boolean head) {
		if (head || kind.equals("matrix") || kind.equals("array") || !TableParts.looksLikeHeader(g)) {
			return head;
		}
		return org.eclipse.jface.dialogs.MessageDialog.openQuestion(Util.getShell(), "Names of the Columns",
				"The first selected row looks like the names of the columns (texts above numbers).\n\n"
						+ "Use it as names? (No: the first row is data, the columns with it become texts.)");
	}

	/*
	 * The selected block: a matrix (numeric if all cells are numbers, else
	 * character), a data frame (typed columns) or a variable for each column
	 * (the names of the columns or the first row with head)
	 */
	private void transferBlock(String kind, boolean head) {
		Grid g = RTable.getGrid();
		if (g == null || RTable.getTabFolder().getItemCount() == 0) {
			Bio7Dialog.message("No opened grid available!");
			return;
		}
		if (!RTools.isAliveDialog()) {
			return;
		}
		if (RState.isBusy()) {
			Bio7Dialog.message("R is busy!");
			return;
		}
		head = askHead(g, kind, head);
		TableParts.Block block = TableParts.block(g, head);
		if (block.columns().length == 0 || block.rows() == 0) {
			Bio7Dialog.message("No cells selected!");
			return;
		}
		String name = null;
		java.util.List<String> variables = new java.util.ArrayList<>();
		if (kind.equals("variables")) {
			for (String n : block.names()) {
				String v = TableParts.rName(n);
				while (variables.contains(v)) {
					v = v + "_";
				}
				variables.add(v);
			}
			if (!org.eclipse.jface.dialogs.MessageDialog.openConfirm(Util.getShell(), "To R",
					"Creates " + variables.size() + " variables (existing objects are replaced):\n" + String.join(", ", variables))) {
				return;
			}
		} else {
			InputDialog inp = new InputDialog(Util.getShell(), "To R", "Create a name for the " + (kind.equals("matrix") ? "matrix" : "data frame") + " ("
					+ block.rows() + " x " + block.columns().length + ")!", kind.equals("matrix") ? "m" : "df", null);
			if (inp.open() != Dialog.OK) {
				return;
			}
			name = replaceWrongWord(inp.getValue());
		}
		String target = name;
		RState.setBusy(true);
		Job job = new Job("Transfer to R") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
				RBackend c = RBridge.getBackend();
				String[][] cols = block.columns();
				try {
					switch (kind) {
					case "matrix" -> {
						String[] all = new String[cols.length * block.rows()];
						for (int j = 0; j < cols.length; j++) {
							System.arraycopy(cols[j], 0, all, j * block.rows(), block.rows());
						}
						if (!RNumbers.assign(c, ".bio7_m", all)) {
							c.assign(".bio7_m", all);
							c.eval(".bio7_m[.bio7_m == \"NA\"] <- NA");
						}
						c.eval(target + " <- matrix(.bio7_m, nrow = " + block.rows() + ", ncol = " + cols.length + "); rm(.bio7_m)");
					}
					case "frame" -> {
						StringBuilder frame = new StringBuilder(), temps = new StringBuilder();
						for (int j = 0; j < cols.length; j++) {
							String tmp = ".bio7_col" + j;
							assignTyped(c, tmp, cols[j]);
							frame.append(j > 0 ? ", " : "").append('`').append(block.names()[j].replace("`", "")).append("` = ").append(tmp);
							temps.append(j > 0 ? ", " : "").append('"').append(tmp).append('"');
						}
						c.eval(target + " <- data.frame(" + frame + ", check.names = FALSE, stringsAsFactors = FALSE)");
						c.eval("rm(list = c(" + temps + "))");
					}
					default -> {
						for (int j = 0; j < cols.length; j++) {
							assignTyped(c, variables.get(j), cols[j]);
						}
					}
					}
				} catch (Exception ex) {
					Bio7Dialog.message("Transfer to R failed:\n" + ex.getMessage());
				} finally {
					RState.setBusy(false);
					monitor.done();
				}
				REval.listRObjects();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	/*
	 * Each selected column (or row) as an element of a named list: a numeric,
	 * logical, Date or character vector (see assignTyped)
	 */
	private void transferList(boolean byColumns, boolean head) {
		Grid g = RTable.getGrid();
		if (g == null || RTable.getTabFolder().getItemCount() == 0) {
			Bio7Dialog.message("No opened grid available!");
			return;
		}
		if (!RTools.isAliveDialog()) {
			return;
		}
		if (RState.isBusy()) {
			Bio7Dialog.message("R is busy!");
			return;
		}
		java.util.List<TableParts.Part> parts = TableParts.parts(g, byColumns, head);
		if (parts.isEmpty()) {
			Bio7Dialog.message("No cells selected!");
			return;
		}
		InputDialog inp = new InputDialog(Util.getShell(), "To R",
				"Create a name for the list (" + parts.size() + (byColumns ? " columns" : " rows") + ": " + TableParts.names(parts) + ")!",
				byColumns ? "columns" : "rows", null);
		if (inp.open() != Dialog.OK) {
			return;
		}
		String name = replaceWrongWord(inp.getValue());
		RState.setBusy(true);
		Job job = new Job("Transfer to R") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
				RBackend c = RBridge.getBackend();
				StringBuilder list = new StringBuilder(), temps = new StringBuilder();
				try {
					for (int i = 0; i < parts.size(); i++) {
						TableParts.Part p = parts.get(i);
						String tmp = ".bio7_part" + i;
						assignTyped(c, tmp, p.values());
						list.append(i > 0 ? ", " : "").append('`').append(p.name().replace("`", "")).append("` = ").append(tmp);
						temps.append(i > 0 ? ", " : "").append('"').append(tmp).append('"');
					}
					c.eval(name + " <- list(" + list + ")");
					c.eval("rm(list = c(" + temps + "))");
				} catch (Exception ex) {
					Bio7Dialog.message("Transfer to R failed:\n" + ex.getMessage());
				} finally {
					RState.setBusy(false);
					monitor.done();
				}
				REval.listRObjects();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		MenuItem menuItem0 = new MenuItem(fMenu, SWT.PUSH);

		menuItem0.setText("R<-Table");
		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);

		menuItem.setText("R<- Table+head");

		new MenuItem(fMenu, SWT.SEPARATOR);
		//
		MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);

		menuItem1.setText("R<-Selection(numeric)");

		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);

		menuItem2.setText("R<-Selection(character)");

		MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);

		menuItem3.setText("R<-Selection(factor)");

		menuItem0.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				if (RTable.getTabFolder().getItemCount() > 0) {
					if (RTools.isAliveDialog()) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							Job job = new Job("Transfer to R") {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);

									transferDataframe();

									monitor.done();
									return Status.OK_STATUS;
								}

							};
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

										RState.setBusy(false);
										REval.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("R is busy!");
						}
					}
				} else {
					Bio7Dialog.message("No opened grid available!");
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				if (RTable.getTabFolder().getItemCount() > 0) {
					if (RTools.isAliveDialog()) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							Job job = new Job("Transfer to R") {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);

									transferDataframeWithHead();

									monitor.done();
									return Status.OK_STATUS;
								}

							};
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

										RState.setBusy(false);
										REval.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("R is busy!");
						}
					}
				} else {
					Bio7Dialog.message("No opened grid available!");
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem1.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				if (RTable.getTabFolder().getItemCount() > 0) {
					if (RTools.isAliveDialog()) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							Job job = new Job("Transfer to R") {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									monitor.beginTask("Transfer Vector Data ...", IProgressMonitor.UNKNOWN);

									transferAsVector();

									monitor.done();
									return Status.OK_STATUS;
								}

							};
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

										RState.setBusy(false);
										REval.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("R is busy!");
						}
					}
				} else {
					Bio7Dialog.message("No opened grid available!");
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				if (RTable.getTabFolder().getItemCount() > 0) {
					if (RTools.isAliveDialog()) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							Job job = new Job("Transfer to R") {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									monitor.beginTask("Transfer Vector Data ...", IProgressMonitor.UNKNOWN);

									transferCharacterVector();

									monitor.done();
									return Status.OK_STATUS;
								}

							};
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

										RState.setBusy(false);
										REval.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("R is busy!");
						}
					}
				} else {
					Bio7Dialog.message("No opened grid available!");
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem3.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				if (RTable.getTabFolder().getItemCount() > 0) {
					if (RTools.isAliveDialog()) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							Job job = new Job("Transfer to R") {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									monitor.beginTask("Transfer vector data as factor ...", IProgressMonitor.UNKNOWN);

									transferAsFactor();

									monitor.done();
									return Status.OK_STATUS;
								}

							};
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

										RState.setBusy(false);
										REval.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("R is busy!");
						}
					}
				} else {
					Bio7Dialog.message("No opened grid available!");
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		new MenuItem(fMenu, SWT.SEPARATOR);
		/* Each selected column (row) an element of a list: the name of the column (row) or the first selected cell (+head) */
		for (Object[] t : new Object[][] { { "R<-Selection Columns as List", true, false }, { "R<-Selection Columns as List+head", true, true },
				{ "R<-Selection Rows as List", false, false }, { "R<-Selection Rows as List+head", false, true } }) {
			MenuItem item = new MenuItem(fMenu, SWT.PUSH);
			item.setText((String) t[0]);
			item.addListener(SWT.Selection, _ -> transferList((Boolean) t[1], (Boolean) t[2]));
		}
		new MenuItem(fMenu, SWT.SEPARATOR);
		/* The selected block: a matrix, a data frame (typed columns) or a variable for each column */
		MenuItem matrix = new MenuItem(fMenu, SWT.PUSH);
		matrix.setText("R<-Selection as Matrix");
		matrix.addListener(SWT.Selection, _ -> transferBlock("matrix", false));
		for (Object[] t : new Object[][] { { "R<-Selection as Data Frame", "frame", false }, { "R<-Selection as Data Frame+head", "frame", true },
				{ "R<-Selection Columns as Variables", "variables", false }, { "R<-Selection Columns as Variables+head", "variables", true } }) {
			MenuItem item = new MenuItem(fMenu, SWT.PUSH);
			item.setText((String) t[0]);
			item.addListener(SWT.Selection, _ -> transferBlock((String) t[1], (Boolean) t[2]));
		}
		return fMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	private void transferDataframeWithHead() {

		grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					CTabItem item = RTable.getTabFolder().getSelection();
					/* We care about wrong chars as the name for the dataframe! */
					text = replaceWrongWord(item.getText());

					data = new String[grid.getItemCount()][grid.getColumnCount()];

					sheetdata = new String[data[0].length][data.length - 1];

					// sheetdata = new String[data[0].length][data.length -
					// 1];
					colnames = new String[data[0].length];
					for (int i = 0; i < grid.getItemCount(); i++) {

						for (int j = 0; j < grid.getColumnCount(); j++) {

							data[i][j] = grid.getItem(i).getText(j);

						}
					}
				}
			});

			for (int c = 0; c < data[0].length; c++) {

				if (data[0][c].equals("") || data[0][c] == null) {
					colnames[c] = "X" + (c + 1);

				} else {

					colnames[c] = data[0][c];

					/* Replace wrong header chars! */
					String regEx = "[^a-zA-Z0-9_.]";
					if (colnames[c].substring(0, 1).matches("[0-9]")) {
						colnames[c] = colnames[c].replaceFirst("[0-9]", "X" + colnames[c].charAt(0));
					}

					else if (colnames[c].substring(0, 1).matches(regEx)) {

						colnames[c] = colnames[c].replaceFirst(regEx, "X.");
					}
					colnames[c] = colnames[c].replaceAll(regEx, ".");

				}
			}

			for (int i = 1; i < data.length; i++) {
				for (int j = 0; j < data[0].length; j++) {
					int count = i - 1;
					String number = "NA";

					if (data[i][j] == null || data[i][j].toString().equals("") || data[i][j].toString().equals(" ")) {
						number = "NA";

					}

					else {
						number = data[i][j].toString();

					}

					sheetdata[j][count] = number;

				}

			}

			if (RTools.isRrunning()) {
				RBackend connection = RBridge.getBackend();

				try {
					connection.eval("try(" + text + "<-data.frame(1:" + sheetdata[0].length + "))");// construct
					// a
					// dataframe
					// with
					// first
					// col.

					String stringBuild = "." + generateRandomString();
					String stringRowNames = "." + generateRandomString();
					// System.out.println(stringBuild);

					for (int i = 0; i < sheetdata.length; i++) {
						/*
						 * We do not use a matrix in this case because Strings and numbers are converted for the whole matrix. We use a dataframe because a dataframe can have numeric and character
						 * columns. We therefore have to create a dataframe with one col (for the row dimension) and then add different cols (numeric or character) to the dataframe.
						 * 
						 * Random temporary col name will be used to avoid a deletion of existing variables!!
						 */
						String num = stringBuild + (i + 1);
						// System.out.println(num);

						/* Numbers as doubles (full precision), else R decides (texts) */
						if (!RNumbers.assign(connection, num, sheetdata[i])) {
							try {
								connection.assign(num, sheetdata[i]);
							} catch (RException e1) {
								e1.printStackTrace();
							}
							connection.eval("try(if(sum(is.na(as.numeric(" + num + "[" + num + "!=\"NA\"])))==0){" + num + "<-as.numeric(" + num + ")}else{" + num + "<-as.character(" + num + ")})");
						}
						connection.eval("try(" + text + "<-data.frame(" + text + "," + num + "))");
						connection.eval("try(remove(" + num + "))");
					}

					try {
						/* Transfer the edited col names! */

						connection.assign(stringRowNames, colnames);
					} catch (RException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					/*
					 * Delete the first col which was only created to use the dataframe!
					 */
					connection.eval("try(" + text + "[1]<-NULL)");
					/* We rename the cols here! */
					connection.eval("try(colnames(" + text + ") <-" + stringRowNames + ")");
					/* Remove the vector with the colnames! */
					connection.eval("try(remove(" + stringRowNames + "))");
				} catch (RException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}

		}
		sheetdata = null;
		data = null;
		colnames = null;
	}

	private String generateRandomString() {
		String choose = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rnd = new Random();

		StringBuilder stringBuild = new StringBuilder(30);
		for (int i = 0; i < 30; i++) {
			stringBuild.append(choose.charAt(rnd.nextInt(choose.length())));
		}

		return stringBuild.toString();
	}

	private void transferDataframe() {
		if (RTools.isAliveDialog()) {

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				@Override
				public void run() {
					CTabItem item = RTable.getTabFolder().getSelection();
					/* We care about wrong chars as the name for the dataframe! */
					text = replaceWrongWord(item.getText());

					grid = RTable.getGrid();
					if (grid != null) {
						String[][] data = new String[grid.getItemCount()][grid.getColumnCount()];
						sheetdata = new String[data[0].length][data.length];
						for (int i = 0; i < grid.getItemCount(); i++) {

							for (int j = 0; j < grid.getColumnCount(); j++) {

								data[i][j] = grid.getItem(i).getText(j);

								if (data[i][j] == null || data[i][j].equals(" ") || data[i][j].equals("")) {
									sheetdata[j][i] = "NA";

								}

								else {
									sheetdata[j][i] = data[i][j];

								}

							}
						}
					}

				}

			});

			if (RTools.isAliveDialog()) {

				String stringBuild = "." + generateRandomString();
				String stringRowNames = "." + generateRandomString();

				RBackend connection = RBridge.getBackend();
				try {
					connection.eval("try(" + text + "<-data.frame(1:" + sheetdata[0].length + "))");

					String c;
					String name = null;
					boolean number = true;
					StringBuffer str = new StringBuffer();
					for (int i = 0; i < sheetdata.length; i++) {

						name = stringBuild + (i + 1);
						str.append("X" + (i + 1) + ",");
						/* Numbers as doubles (full precision), else R decides (texts) */
						if (!RNumbers.assign(connection, name, sheetdata[i])) {
							try {
								connection.assign(name, sheetdata[i]);
							} catch (RException e1) {

								e1.printStackTrace();
							}

							/*
							 * If no String is present convert to integer ->NA values will also be converted!
							 */
							connection.eval("try(if(sum(is.na(as.numeric(" + name + "[" + name + "!=\"NA\"])))==0){" + name + "<-as.numeric(" + name + ")}else{" + name + "<-as.character(" + name + ")})");
						}

						connection.eval("try(" + text + "<-data.frame(" + text + "," + name + "))");

						connection.eval("try(remove(" + name + "))");

					}

					/* Transfer the edited col names! */
					String s = str.toString();

					try {
						connection.assign(stringRowNames, s.split(","));
					} catch (RException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					/*
					 * Delete the first col which was only created to use the dataframe!
					 */
					connection.eval("try(" + text + "[1]<-NULL)");
					/* We rename the cols here! */
					connection.eval("colnames(" + text + ") <-" + stringRowNames + "");
					/* Remove the vector with the colnames! */
					connection.eval("remove(" + stringRowNames + ")");

					str = null;

				} catch (RException e2) {

					e2.printStackTrace();
				}

			}
		}
		sheetdata = null;
		data = null;
		colnames = null;
	}

	private void transferAsVector() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (RTools.isAliveDialog()) {

			grid = RTable.getGrid();
			if (grid != null) {

				display.syncExec(new Runnable() {

					@Override
					public void run() {
						Point[] sel = grid.getCellSelection();
						int tempX = -1;
						int tempY = -1;
						dataVector = new String[sel.length];

						for (int i = 0; i < sel.length; i++) {

							dataVector[i] = grid.getItem(sel[i].y).getText(sel[i].x);
							if (dataVector[i] == "") {
								dataVector[i] = "NA";
							}
							tempX = sel[i].x;
							tempY = sel[i].y;

						}

					}
				});

				display.syncExec(new Runnable() {

					@Override
					public void run() {
						InputDialog inp = new InputDialog(Util.getShell(), "To R", "Create a name for the vector!", "vector", null);
						input = "vector";
						if (inp.open() == Dialog.OK) {

							input = replaceWrongWord(inp.getValue());

						}
					}
				});

				RBackend c = RBridge.getBackend();
				try {
					/* Numbers as doubles (full precision), else R converts the texts */
					if (!RNumbers.assign(c, input, dataVector)) {
						c.assign(input, dataVector);
						try {
							c.eval("" + input + "<-as.numeric(" + input + ")");
						} catch (RException e1) {
							e1.printStackTrace();
						}
					}
				} catch (RException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		}
		dataVector = null;
	}

	private void transferAsFactor() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (RTools.isAliveDialog()) {
			grid = RTable.getGrid();
			if (grid != null) {

				display.syncExec(new Runnable() {

					@Override
					public void run() {
						Point[] sel = grid.getCellSelection();

						dataVector = new String[sel.length];
						for (int i = 0; i < sel.length; i++) {
							dataVector[i] = grid.getItem(sel[i].y).getText(sel[i].x);
							if (dataVector[i] == "") {
								dataVector[i] = "NA";
							}

						}
					}
				});

				display.syncExec(new Runnable() {

					@Override
					public void run() {
						InputDialog inp = new InputDialog(Util.getShell(), "To R", "Create a name for the vector!", "vector", null);
						input = "vector";
						if (inp.open() == Dialog.OK) {
							input = replaceWrongWord(inp.getValue());

						}
					}
				});
				RBackend c = RBridge.getBackend();
				try {
					c.assign(input, dataVector);
					try {
						c.eval("" + input + "<-as.factor(" + input + ")");
					} catch (RException e1) {

						e1.printStackTrace();
					}
				} catch (RException e1) {

					e1.printStackTrace();
				}
			}
		}
		dataVector = null;
	}

	private void transferCharacterVector() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (RTools.isAliveDialog()) {
			grid = RTable.getGrid();
			if (grid != null) {
				display.syncExec(new Runnable() {

					@Override
					public void run() {
						Point[] sel = grid.getCellSelection();

						dataVector = new String[sel.length];
						for (int i = 0; i < sel.length; i++) {
							dataVector[i] = grid.getItem(sel[i].y).getText(sel[i].x);
							if (dataVector[i] == "") {
								dataVector[i] = "NA";
							}

						}
					}
				});
				display.syncExec(new Runnable() {

					@Override
					public void run() {
						InputDialog inp = new InputDialog(Util.getShell(), "To R", "Create a name for the vector!", "vector", null);
						input = "vector";
						if (inp.open() == Dialog.OK) {
							input = replaceWrongWord(inp.getValue());

						}
					}
				});

				RBackend c = RBridge.getBackend();
				try {
					c.assign(input, dataVector);

				} catch (RException e1) {

					e1.printStackTrace();
				}
			}
		}
		dataVector = null;
	}

	public String replaceWrongWord(String namesInput) {
		String names;
		/* Replace wrong header chars! */
		String regEx = "[^a-zA-Z0-9_.]";
		if (namesInput.substring(0, 1).matches("[0-9]")) {
			namesInput = namesInput.replaceFirst("[0-9]", "X" + namesInput.charAt(0));
		} else if (namesInput.substring(0, 1).matches(regEx)) {
			namesInput = namesInput.replaceFirst(regEx, "X.");
		}

		names = namesInput.replaceAll(regEx, ".");

		return names;
	}

}