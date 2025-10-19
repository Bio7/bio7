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
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.util.Util;

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
					if (RServe.isAliveDialog()) {
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
										RServeUtil.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("Rserve is busy!");
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
					if (RServe.isAliveDialog()) {
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
										RServeUtil.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("Rserve is busy!");
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
					if (RServe.isAliveDialog()) {
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
										RServeUtil.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("Rserve is busy!");
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
					if (RServe.isAliveDialog()) {
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
										RServeUtil.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("Rserve is busy!");
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
					if (RServe.isAliveDialog()) {
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
										RServeUtil.listRObjects();
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("Rserve is busy!");
						}
					}
				} else {
					Bio7Dialog.message("No opened grid available!");
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

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

			if (RServe.isRrunning()) {
				RConnection connection = RServe.getConnection();

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

						try {
							connection.assign(num, sheetdata[i]);
						} catch (REngineException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						connection.eval("try(if(sum(is.na(as.numeric(" + num + "[" + num + "!=\"NA\"])))==0){" + num + "<-as.numeric(" + num + ")}else{" + num + "<-as.character(" + num + ")})");
						connection.eval("try(" + text + "<-data.frame(" + text + "," + num + "))");
						connection.eval("try(remove(" + num + "))");
					}

					try {
						/* Transfer the edited col names! */

						connection.assign(stringRowNames, colnames);
					} catch (REngineException e) {
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
				} catch (RserveException e1) {
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
		if (RServe.isAliveDialog()) {

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

			if (RServe.isAliveDialog()) {

				String stringBuild = "." + generateRandomString();
				String stringRowNames = "." + generateRandomString();

				RConnection connection = RServe.getConnection();
				try {
					connection.eval("try(" + text + "<-data.frame(1:" + sheetdata[0].length + "))");

					String c;
					String name = null;
					boolean number = true;
					StringBuffer str = new StringBuffer();
					for (int i = 0; i < sheetdata.length; i++) {

						name = stringBuild + (i + 1);
						str.append("X" + (i + 1) + ",");
						try {
							connection.assign(name, sheetdata[i]);
						} catch (REngineException e1) {

							e1.printStackTrace();
						}

						/*
						 * If no String is present convert to integer ->NA values will also be converted!
						 */
						connection.eval("try(if(sum(is.na(as.numeric(" + name + "[" + name + "!=\"NA\"])))==0){" + name + "<-as.numeric(" + name + ")}else{" + name + "<-as.character(" + name + ")})");

						connection.eval("try(" + text + "<-data.frame(" + text + "," + name + "))");

						connection.eval("try(remove(" + name + "))");

					}

					/* Transfer the edited col names! */
					String s = str.toString();

					try {
						connection.assign(stringRowNames, s.split(","));
					} catch (REngineException e) {
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

				} catch (RserveException e2) {

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
		if (RServe.isAliveDialog()) {

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

				RConnection c = RServe.getConnection();
				try {
					c.assign(input, dataVector);
					try {
						c.eval("" + input + "<-as.numeric(" + input + ")");
					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (REngineException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		}
		dataVector = null;
	}

	private void transferAsFactor() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (RServe.isAliveDialog()) {
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
				RConnection c = RServe.getConnection();
				try {
					c.assign(input, dataVector);
					try {
						c.eval("" + input + "<-as.factor(" + input + ")");
					} catch (RserveException e1) {

						e1.printStackTrace();
					}
				} catch (REngineException e1) {

					e1.printStackTrace();
				}
			}
		}
		dataVector = null;
	}

	private void transferCharacterVector() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (RServe.isAliveDialog()) {
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

				RConnection c = RServe.getConnection();
				try {
					c.assign(input, dataVector);

				} catch (REngineException e1) {

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