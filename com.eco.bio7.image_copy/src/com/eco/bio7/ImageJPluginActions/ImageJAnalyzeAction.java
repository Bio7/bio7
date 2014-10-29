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
import java.awt.EventQueue;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ImageJAnalyzeAction extends Action implements IMenuCreator {

	private Menu fMenu;
	String[] thegels = { "Select First Lane", "Select Next Lane", "Plot Lanes",
			"Re-plot Lanes","Reset", "Label Peaks", "Gel Analyzer Options..."};
	String[] thetools = { "Save XY Coordinates...", "Fractal Box Count...",
			"Analyze Line Graph", "Curve Fitting...", "ROI Manager...",
			"Scale Bar...", "Calibration Bar..."};

	MenuItem[] gels_ = new MenuItem[thegels.length];
	MenuItem[] tools_ = new MenuItem[thetools.length];

	public ImageJAnalyzeAction() {
		setId("Analyze");
		setToolTipText("ImageJ");
		setText("Analyze");

		setMenuCreator(this);
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		Menu fMenu2 = new Menu(fMenu);
		MenuItem menuItem_type = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_type.setMenu(fMenu2);
		menuItem_type.setText("Gels");
		for (int i = 0; i < thegels.length; i++) {
			final int count = i;
			gels_[i] = new MenuItem(fMenu2, SWT.PUSH);
			gels_[i].setText(thegels[i]);
			gels_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thegels[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenu3 = new Menu(fMenu);
		MenuItem menuItem_adjust = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_adjust.setMenu(fMenu3);
		menuItem_adjust.setText("Tools");

		for (int i = 0; i < thetools.length; i++) {
			final int count = i;
			tools_[i] = new MenuItem(fMenu3, SWT.PUSH);
			tools_[i].setText(thetools[i]);
			tools_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thetools[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);
		menuItem2.setText("Measure");
		MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);
		menuItem3.setText("Analyze Particles...");
		MenuItem menuItem4 = new MenuItem(fMenu, SWT.PUSH);
		menuItem4.setText("Summarize");
		MenuItem menuItem5 = new MenuItem(fMenu, SWT.PUSH);
		menuItem5.setText("Distribution");

		MenuItem menuItem6 = new MenuItem(fMenu, SWT.PUSH);

		menuItem6.setText("Label");

		MenuItem menuItem7 = new MenuItem(fMenu, SWT.PUSH);
		menuItem7.setText("Clear Results");
		MenuItem menuItem8 = new MenuItem(fMenu, SWT.PUSH);
		menuItem8.setText("Set Measurements...");
		MenuItem menuItem9 = new MenuItem(fMenu, SWT.PUSH);
		menuItem9.setText("Set Scale...");
		MenuItem menuItem10 = new MenuItem(fMenu, SWT.PUSH);
		menuItem10.setText("Calibrate...");
		MenuItem menuItem11 = new MenuItem(fMenu, SWT.PUSH);
		menuItem11.setText("Histogram");
		MenuItem menuItem12 = new MenuItem(fMenu, SWT.PUSH);
		menuItem12.setText("Plot Profile");
		MenuItem menuItem13 = new MenuItem(fMenu, SWT.PUSH);
		menuItem13.setText("Surface Plot...");

		menuItem2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Measure");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem3.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				EventQueue.invokeLater(new Runnable() {
					public void run() {
						IJ.getInstance().doCommand("Analyze Particles...");
					}
				});
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem4.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Summarize");
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem5.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Distribution...");
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem6.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Label");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem7.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Clear Results");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuItem8.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Set Measurements...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem9.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Set Scale...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem10.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Calibrate...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem11.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Histogram");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem12.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Plot Profile");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem13.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Surface Plot...");

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