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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ImageJImageAction extends Action implements IMenuCreator {

	private Menu fMenu;
	String[] thetype = { "8-bit", "16-bit", "32-bit", "8-bit Color",
			"RGB Color", "RGB Stack", "HSB Stack" };
	String[] theadjust = { "Brightness/Contrast...", "Window/Level...",
			"Color Balance...", "Threshold...", "Size...", "Canvas Size...","Line Width... "};
	String[] thecolour = { "Stack to RGB", "Make Composite",
			"Split Channels", "Merge Channels...","Channels Tool...", "Show LUT","Display LUTs", "Edit LUT...",
			"Color Picker..." };
	String[] thestacks = { "Add Slice", "Delete Slice", "Next Slice [>]",
			"Previous Slice [<]", "Set Slice...", "Images to Stack","Stack to Images",
			
			"Make Montage...", "Reslice [/]...","Orthogonal Views",
			"Z Project...", "3D Project...", "Plot Z-axis Profile",
			"Label..."};
	
	String[] thestacktools = {"Combine...","Concatenate...","Reduce...","Reverse","Insert...","Montage to Stack...",
			"Make Substack...","Grouped Z Project...","Set Label...","Remove Slice Labels","Start Animation [\\]", "Stop Animation", "Animation Options..."};

	String[] thehyperstacks = {"New Hyperstack...","Stack to Hyperstack...","Hyperstack to Stack","Reduce Dimensionality...","Channels Tool..."};

	String[] thezoom = { "In [+]", "Out [-]", "View 100%", "Original Scale","Set... ",
			"To Selection" };
	String[] thelookuptables = { "Fire", "Grays", "Ice", "Spectrum",
			"3-3-2 RGB", "Red", "Green", "Blue", "Cyan", "Magenta", "Yellow",
			"Red/Green", "Invert LUT", "Apply LUT", };
	String[] thetransform = {"Flip Horizontally","Flip Vertically","Flip Z","Rotate 90 Degrees Right","Rotate 90 Degrees Left","Rotate... ","Translate...","Bin...","Image to Results","Results to Image"};
    
	String[] theoverlay = { "Add Selection...", "Add Image...",
			"Hide Overlay", "Show Overlay","From ROI Manager", "To ROI Manager","Remove Overlay",
			"Flatten","Labels...","Overlay Options...","List Elements"};
	
	MenuItem[] adjust_ = new MenuItem[theadjust.length];
	MenuItem[] type_ = new MenuItem[thetype.length];
	MenuItem[] colour_ = new MenuItem[thecolour.length];
	MenuItem[] stacks_ = new MenuItem[thestacks.length];
	MenuItem[] stackstools_ = new MenuItem[thestacktools.length];
	MenuItem[] hyperstacks_ = new MenuItem[thehyperstacks.length];
	MenuItem[] zoom_ = new MenuItem[thezoom.length];
	MenuItem[] lookuptables_ = new MenuItem[thelookuptables.length];
	MenuItem[] thetransform_ = new MenuItem[thetransform.length];
	MenuItem[] theoverlay_ = new MenuItem[theoverlay.length];

	public ImageJImageAction() {
		setId("Image");
		setToolTipText("ImageJ");
		setText("Image");
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
		menuItem_type.setText("Type");
		for (int i = 0; i < thetype.length; i++) {
			final int count = i;
			type_[i] = new MenuItem(fMenu2, SWT.CHECK);
			type_[i].setText(thetype[i]);

			type_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {

					IJ.getInstance().doCommand(thetype[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenu3 = new Menu(fMenu);
		MenuItem menuItem_adjust = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_adjust.setMenu(fMenu3);
		menuItem_adjust.setText("Adjust");

		for (int i = 0; i < theadjust.length; i++) {
			final int count = i;
			adjust_[i] = new MenuItem(fMenu3, SWT.PUSH);
			adjust_[i].setText(theadjust[i]);
			adjust_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(theadjust[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}


		Menu fMenuColour = new Menu(fMenu);
		MenuItem menuItem_colour = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_colour.setMenu(fMenuColour);
		menuItem_colour.setText("Colour");

		for (int i = 0; i < thecolour.length; i++) {
			final int count = i;
			colour_[i] = new MenuItem(fMenuColour, SWT.PUSH);
			colour_[i].setText(thecolour[i]);
			colour_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					
					IJ.getInstance().doCommand(thecolour[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenuStacks = new Menu(fMenu);
		MenuItem menuItem_stacks = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_stacks.setMenu(fMenuStacks);
		menuItem_stacks.setText("Stacks");

		for (int i = 0; i < thestacks.length; i++) {
			final int count = i;
			stacks_[i] = new MenuItem(fMenuStacks, SWT.PUSH);
			stacks_[i].setText(thestacks[i]);
			stacks_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thestacks[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}
		
		Menu fMenuStackTools = new Menu(fMenuStacks);
		MenuItem menuItem_stacktools = new MenuItem(fMenuStacks, SWT.CASCADE);
		menuItem_stacktools.setMenu(fMenuStackTools);
		menuItem_stacktools.setText("Tools");

		for (int i = 0; i < thestacktools.length; i++) {
			final int count = i;
			stackstools_[i] = new MenuItem(fMenuStackTools, SWT.PUSH);
			stackstools_[i].setText(thestacktools[i]);
			stackstools_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thestacktools[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenuHyperStacks = new Menu(fMenu);
		MenuItem menuItem_hyperstacks = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_hyperstacks.setMenu(fMenuHyperStacks);
		menuItem_hyperstacks.setText("HyperStacks");

		for (int i = 0; i < hyperstacks_.length; i++) {
			final int count = i;
			hyperstacks_[i] = new MenuItem(fMenuHyperStacks, SWT.PUSH);
			hyperstacks_[i].setText(thehyperstacks[i]);
			hyperstacks_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thehyperstacks[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenu7 = new Menu(fMenu);
		MenuItem menuItem_zoom = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_zoom.setMenu(fMenu7);
		menuItem_zoom.setText("Zoom");

		for (int i = 0; i < thezoom.length; i++) {
			final int count = i;
			zoom_[i] = new MenuItem(fMenu7, SWT.PUSH);
			zoom_[i].setText(thezoom[i]);
			zoom_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thezoom[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}
		Menu fMenu8 = new Menu(fMenu);
		MenuItem menuItem_lookuptables = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_lookuptables.setMenu(fMenu8);
		menuItem_lookuptables.setText("Lookup Tables");

		for (int i = 0; i < thelookuptables.length; i++) {
			final int count = i;
			lookuptables_[i] = new MenuItem(fMenu8, SWT.PUSH);
			lookuptables_[i].setText(thelookuptables[i]);
			lookuptables_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thelookuptables[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}
		Menu fMenu9 = new Menu(fMenu);
		MenuItem menuItem_thetransform = new MenuItem(fMenu, SWT.CASCADE);
		 menuItem_thetransform.setMenu(fMenu9);
		 menuItem_thetransform.setText("Transform");

		for (int i = 0; i < thetransform.length; i++) {
			final int count = i;
			thetransform_[i] = new MenuItem(fMenu9, SWT.PUSH);
			thetransform_[i].setText(thetransform[i]);
			thetransform_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(thetransform[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}
		Menu fMenu10 = new Menu(fMenu);
		MenuItem menuItem_theoverlay = new MenuItem(fMenu, SWT.CASCADE);
		menuItem_theoverlay.setMenu(fMenu10);
		menuItem_theoverlay.setText("Overlay");

		for (int i = 0; i < theoverlay.length; i++) {
			final int count = i;
			theoverlay_[i] = new MenuItem(fMenu10, SWT.PUSH);
			theoverlay_[i].setText(theoverlay[i]);
			theoverlay_[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					IJ.getInstance().doCommand(theoverlay[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);
		menuItem1.setText("Show info");

		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);
		menuItem2.setText("Properties");
		MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);
		menuItem3.setText("Crop");
		MenuItem menuItem4 = new MenuItem(fMenu, SWT.PUSH);
		menuItem4.setText("Duplicate");
		MenuItem menuItem5 = new MenuItem(fMenu, SWT.PUSH);
		menuItem5.setText("Rename");

		MenuItem menuItem6 = new MenuItem(fMenu, SWT.PUSH);

		menuItem6.setText("Scale");

		menuItem1.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Show Info...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		

		menuItem2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Properties...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem3.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Crop");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem4.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Duplicate...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem5.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Rename...");
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem6.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Scale...");

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