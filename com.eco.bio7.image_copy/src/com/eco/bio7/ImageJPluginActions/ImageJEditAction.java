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

package com.eco.bio7.ImageJPluginActions;

import ij.IJ;
import ij.gui.Toolbar;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class ImageJEditAction extends Action implements IMenuCreator {

	private Menu fMenu;
	String[] select = { "Select All", "Select None", "Restore Selection",
			"Fit Spline", "Fit Ellipse", "Convex Hull","Make Inverse","Create Selection","Create Mask",
			 "Properties... ","Scale... ","Rotate...", "Enlarge...", "Make Band...",
			"Specify...","Straighten...","To Bounding Box","Line to Area","Area to Line","Image to Selection...","Interpolate","Add to Manager"};
	String[] options = { "Line Width...", "Input/Output...", "Fonts...",
			"Profile Plot Options...", "Point Tool...","Rounded Rect Tool...", "Colors...",
			"Appearance...", "Conversions...", "Memory & Threads...","Proxy Settings...","Compiler...","DICOM...","Misc..." };
	MenuItem[] option_ = new MenuItem[options.length];
	MenuItem[] select_ = new MenuItem[select.length];

	public ImageJEditAction() {
		setId("Edit");
		setToolTipText("ImageJ");
		setText("Edit");
		setMenuCreator(this);
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		
		MenuItem menuItem1_1 = new MenuItem(fMenu, SWT.PUSH);

		menuItem1_1.setText("Colour Foreground");

		MenuItem menuItem1_2 = new MenuItem(fMenu, SWT.PUSH);

		menuItem1_2.setText("Colour Background");
		
		MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);
		menuItem1.setText("Undo");

		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);
		menuItem2.setText("Cut");
		MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);
		menuItem3.setText("Copy");
		MenuItem menuItem31 = new MenuItem(fMenu, SWT.PUSH);
		menuItem31.setText("Copy to System");
		MenuItem menuItem4 = new MenuItem(fMenu, SWT.PUSH);
		menuItem4.setText("Paste");
		MenuItem menuItem5 = new MenuItem(fMenu, SWT.PUSH);
		menuItem5.setText("Paste Control...");
		Menu fMenu2 = new Menu(fMenu);
		MenuItem menuItem6 = new MenuItem(fMenu, SWT.PUSH);

		menuItem6.setText("Clear");

		MenuItem menuItem7 = new MenuItem(fMenu, SWT.PUSH);
		menuItem7.setText("Clear Outside");
		MenuItem menuItem9 = new MenuItem(fMenu, SWT.PUSH);
		menuItem9.setText("Fill");
		MenuItem menuItem10 = new MenuItem(fMenu, SWT.PUSH);
		menuItem10.setText("Draw");
		MenuItem menuItem11 = new MenuItem(fMenu, SWT.PUSH);
		menuItem11.setText("Invert");

		MenuItem menuItem21 = new MenuItem(fMenu, SWT.CASCADE);
		menuItem21.setMenu(fMenu2);
		menuItem21.setText("Selection");
		for (int i = 0; i < select.length; i++) {
			final int count = i;
			select_[i] = new MenuItem(fMenu2, SWT.PUSH);
			select_[i].setText(select[i]);
			select_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(select[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenu3 = new Menu(fMenu);
		MenuItem menuItem8 = new MenuItem(fMenu, SWT.CASCADE);
		menuItem8.setMenu(fMenu3);
		menuItem8.setText("Options");

		for (int i = 0; i < options.length; i++) {
			final int count = i;
			option_[i] = new MenuItem(fMenu3, SWT.PUSH);
			option_[i].setText(options[i]);
			option_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(options[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		menuItem2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Cut");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem3.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Copy");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem31.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Copy to System");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem4.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Paste");
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem5.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Paste Control...");
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		
		menuItem6.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Clear");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem7.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Clear Outside");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem9.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Fill");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem10.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Draw");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem11.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Invert");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem1.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Undo");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem1_1.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				Shell shell = new Shell();
				ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);
				// dialog.setRGB(rgb);
				dialog.setText("Foreground Colour");
				RGB color = dialog.open();
				if (color != null) {

					Toolbar.setForegroundColor(new java.awt.Color(color.red, color.green, color.blue));

				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem1_2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				Shell shell = new Shell();
				ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);
				// dialog.setRGB(rgb);
				dialog.setText("Background Colour");
				RGB color = dialog.open();
				if (color != null) {

					Toolbar.setBackgroundColor(new java.awt.Color(color.red, color.green, color.blue));

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

}