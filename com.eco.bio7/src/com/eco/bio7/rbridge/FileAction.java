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

import java.awt.Color;
import java.io.File;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
//import org.jopendocument.dom.spreadsheet.Sheet;

import com.eco.bio7.batch.Bio7Dialog;

public class FileAction extends Action implements IMenuCreator {

	private Menu fMenu;

	protected Grid grid;

	//protected Sheet sheet = null;

	protected Color colo;

	protected String co;

	public FileAction() {
		setId("RFile");
		setToolTipText("R Menu");
		setText("File");

		setMenuCreator(this);
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		
		MenuItem menuItem51 = new MenuItem(fMenu, SWT.PUSH);

		menuItem51.setText("Load Bio7 XML");
		
		new MenuItem(fMenu, SWT.SEPARATOR);

		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);

		menuItem.setText("Import Excel (97-2003)");

		MenuItem menuItem01 = new MenuItem(fMenu, SWT.PUSH);

		menuItem01.setText("Import Excel (2007-2013)");

		new MenuItem(fMenu, SWT.SEPARATOR);

		/*MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);

		menuItem3.setText("Import OpenOffice");
		
		new MenuItem(fMenu, SWT.SEPARATOR);*/

		MenuItem menuItem5 = new MenuItem(fMenu, SWT.PUSH);

		menuItem5.setText("Import CSV");
		
		new MenuItem(fMenu, SWT.SEPARATOR);
		
		MenuItem menuItem6 = new MenuItem(fMenu, SWT.PUSH);

		menuItem6.setText("Save as Bio7 XML");
		
		new MenuItem(fMenu, SWT.SEPARATOR);

		MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);

		menuItem1.setText("Export as Excel (97-2003)");

		MenuItem menuItem11 = new MenuItem(fMenu, SWT.PUSH);

		menuItem11.setText("Export as Excel (2007-2010)");

		new MenuItem(fMenu, SWT.SEPARATOR);

		/*MenuItem menuItem4 = new MenuItem(fMenu, SWT.PUSH);

		menuItem4.setText("Export as OpenOffice");
		
		new MenuItem(fMenu, SWT.SEPARATOR);*/

		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);

		menuItem2.setText("Export as Text");

		

		menuItem6.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				final String file = Bio7Dialog.saveFile("*.bgjar");
				new File(file).getAbsolutePath();
				grid = RTable.getGrid();
				if (file != null) {
					SaveGridXMLJob job = new SaveGridXMLJob(file, new File(file).getParent(), grid);
					// job.setSystem(true);
					job.schedule();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuItem5.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				CSVImport thisClass = new CSVImport();
				thisClass.createSShell();
				thisClass.sShell.open();

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem51.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				final String file = Bio7Dialog.openFile(new String[] { "*.bgjar", "*." });
				grid = RTable.getGrid();
				if (file != null) {
					LoadGridXMLJob job = new LoadGridXMLJob(file);
					// job.setSystem(true);
					job.schedule();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		/*menuItem3.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				String file = null;
				file = Bio7Dialog.openFile(new String[] { "*.ods", "*." });
				File fil = null;
				if (file != null) {

					fil = new File(file);

					if (fil.exists()) {
						LoadOpenOfficeJob job = new LoadOpenOfficeJob(fil);
						job.setSystem(true);
						job.schedule();
					} else {
						Bio7Dialog.message("File not found!");
					}
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});*/

		menuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				final String file = Bio7Dialog.openFile(new String[] { "*.xls", "*." });
				grid = RTable.getGrid();
				if (file != null) {
					LoadExcelJob job = new LoadExcelJob(file, grid);
					// job.setSystem(true);
					job.schedule();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem01.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				final String file = Bio7Dialog.openFile(new String[] { "*.xlsx", "*." });
				grid = RTable.getGrid();
				if (file != null) {
					LoadXlsxExcelJob job = new LoadXlsxExcelJob(file, grid);
					// job.setSystem(true);
					job.schedule();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem1.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				String save = Bio7Dialog.saveFile("*.xls");
				Grid grid = RTable.getGrid();
				if (save != null) {
					SaveExcelJob job = new SaveExcelJob(save, grid);
					// job.setSystem(true);
					job.schedule();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		/*menuItem4.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				final String file = Bio7Dialog.saveFile("*.ods");
				grid = RTable.getGrid();
				if (file != null) {
					SaveOpenOfficeJob job = new SaveOpenOfficeJob(file, grid);
					 job.setSystem(true);
					job.schedule();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});*/
		menuItem11.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				String save = Bio7Dialog.saveFile("*.xlsx");
				Grid grid = RTable.getGrid();
				if (save != null) {
					SaveXlsxExcelJob job = new SaveXlsxExcelJob(save, grid);
					// job.setSystem(true);
					job.schedule();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuItem2.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				final String save = Bio7Dialog.saveFile("*.txt");
				final Grid grid = RTable.getGrid();
				if (save != null) {
					SaveTextJob job = new SaveTextJob(save, grid);
					// job.setSystem(true);
					job.schedule();
				}

			}

			@Override
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

}