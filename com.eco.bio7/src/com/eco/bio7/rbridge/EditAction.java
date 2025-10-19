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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.util.Util;

public class EditAction extends Action implements IMenuCreator {

	private Menu fMenu;

	private Clipboard cb;

	protected int storecount;

	private CutCopyPaste ccp;

	public EditAction() {
		setId("RFile");
		setToolTipText("R Menu");
		setText("Edit");

		setMenuCreator(this);
		cb = new Clipboard(Display.getDefault());
		ccp = new CutCopyPaste();
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		MenuItem menuCut = new MenuItem(fMenu, SWT.PUSH);

		menuCut.setText("&Cut\tCtrl+ t");
		menuCut.setAccelerator(SWT.CTRL + 't');

		MenuItem menuCopy = new MenuItem(fMenu, SWT.PUSH);

		menuCopy.setText("&Copy\tCtrl+ c");
		menuCut.setAccelerator(SWT.CTRL + 'c');
		MenuItem menuPaste = new MenuItem(fMenu, SWT.PUSH);

		menuPaste.setText("&Paste\tCtrl+ v");
		menuCut.setAccelerator(SWT.CTRL + 'v');
		MenuItem menuDelete = new MenuItem(fMenu, SWT.PUSH);

		menuDelete.setText("&Delete\tDel");
		menuCut.setAccelerator(SWT.DEL);
		new MenuItem(fMenu, SWT.SEPARATOR);

		MenuItem menuCopyImage = new MenuItem(fMenu, SWT.PUSH);

		menuCopyImage.setText("&Copy Image To Clipboard\tCtrl+ i");

		menuCopyImage.setAccelerator(SWT.CTRL + 'i');

		MenuItem menuPasteImage = new MenuItem(fMenu, SWT.PUSH);

		menuPasteImage.setText("&Paste Image From Clipboard\tCtrl+ p");

		menuPasteImage.setAccelerator(SWT.CTRL + 'p');

		MenuItem menuFont = new MenuItem(fMenu, SWT.PUSH);

		menuFont.setText("Font");

		new MenuItem(fMenu, SWT.SEPARATOR);

		MenuItem menuBackgroundCol = new MenuItem(fMenu, SWT.PUSH);

		menuBackgroundCol.setText("Background Colour");

		MenuItem menuGridlineCol = new MenuItem(fMenu, SWT.PUSH);

		menuGridlineCol.setText("Gridline Colour");

		new MenuItem(fMenu, SWT.SEPARATOR);

		MenuItem menuResizeSelCell = new MenuItem(fMenu, SWT.PUSH);

		menuResizeSelCell.setText("Resize Selected Cells");

		menuCut.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				ccp.cut();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuCopy.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				ccp.copy();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuPaste.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				ccp.paste();

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuCopyImage.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				ccp.copyImageToClipboard();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuPasteImage.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				ccp.pasteImageFromClipboard();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuDelete.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				ccp.delete();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuFont.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				FontRegistry fontRegistry;
				fontRegistry = new FontRegistry(Display.getCurrent());
				FontDialog fd = new FontDialog(new Shell(), SWT.NONE);
				fd.setText("Select Font");
				fd.setRGB(new RGB(0, 0, 0));
				// FontData defaultFont = new FontData("Courier", 10, SWT.BOLD);
				// fd.setFontData(defaultFont);

				FontData newFont = fd.open();
				if (newFont == null) {
					return;
				}

				Grid grid = RTable.getGrid();

				if (grid != null) {

					Point[] sel = grid.getCellSelection();
					int temp = 0;
					for (Point selection : sel) {
						fontRegistry.put("font", new FontData[] { new FontData(newFont.getName(), newFont.getHeight(), newFont.getStyle()) });
						grid.getItem(selection.y).setFont(selection.x, fontRegistry.get("font"));
						grid.getItem(selection.y).setForeground(selection.x, new Color(Display.getCurrent(), fd.getRGB()));
					}

				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuBackgroundCol.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				ColorDialog dialog = new ColorDialog(new Shell(), SWT.APPLICATION_MODAL);

				RGB color = dialog.open();

				/*
				 * HSSFColor cf=getColor(new java.awt.Color(color.red,color.green,color.blue)); short[] s= cf.getTriplet(); System.out.println(" "+s[0]+" "+s[1]+" "+s[2]);
				 */
				if (color == null) {
					return;
				}
				ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
				String colorAsString = color.toString();
				Color col = colorRegistry.get(colorAsString);
				if (col == null) {
					colorRegistry.put(colorAsString, color);
				}

				Grid grid = RTable.getGrid();

				if (grid != null) {

					Point[] sel = grid.getCellSelection();
					int temp = 0;

					for (Point selection : sel) {

						if (color != null) {

							grid.getItem(selection.y).setBackground(selection.x, colorRegistry.get(colorAsString));

						}
					}
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuGridlineCol.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				ColorDialog dialog = new ColorDialog(new Shell(), SWT.APPLICATION_MODAL);

				RGB color = dialog.open();
				if (color == null) {
					return;
				}
				ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
				String colorAsString = color.toString();
				Color col = colorRegistry.get(colorAsString);
				if (col == null) {
					colorRegistry.put(colorAsString, color);
				}

				Grid grid = RTable.getGrid();

				if (grid != null) {

					Point[] sel = grid.getCellSelection();
					int temp = 0;
					for (Point selection : sel) {

						if (color != null) {
							grid.setLineColor(colorRegistry.get(colorAsString));

						}
					}
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuResizeSelCell.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				new ResizeDialog(Util.getShell()).open();

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
	/*
	 * private HSSFPalette palette = new HSSFPalette(new PaletteRecord()) { };
	 * 
	 * protected final HSSFColor getColor(final java.awt.Color col) { HSSFColor c; c = palette.findColor((byte) col.getRed(), (byte) col.getGreen(), (byte) col.getBlue()); if (c == null) { try { c =
	 * palette.addColor((byte) col.getRed(), (byte) col.getGreen(), (byte) col.getBlue()); } catch (RuntimeException re) { c = palette.findSimilarColor((byte) col.getRed(), (byte) col.getGreen(),
	 * (byte) col.getBlue()); } } return c; }
	 */

}