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

public class ImageJProcessAction extends Action implements IMenuCreator {

	private Menu fMenu;
	String []thenoise={"Add Noise","Add Specified Noise...","Salt and Pepper","Despeckle","Remove Outliers...","Remove NaNs..."};
	String []theshadows={"North","Northeast","East","Southeast","South","Southwest","West","Northwest","Shadows Demo"};
	String []thebinarys={"Make Binary","Convert to Mask","Erode","Dilate","Open","Close-","Outline","Fill Holes","Skeletonize","Distance Map","Ultimate Points","Watershed","Voronoi","Options..."};
	String []themath={"Add...","Subtract...","Multiply...","Divide...","AND...","OR...","XOR...","Min...","Max...","Gamma...","Set...","Log","Exp","Square","Square Root","Reciprocal","NaN Background","Abs","Macro..."};
	String []thefft={"FFT","Inverse FFT","Redisplay Power Spectrum","FFT Options...","Bandpass Filter...","Custom Filter...","FD Math...","Swap Quadrants"};
	String []thefilters={"Convolve...","Gaussian Blur...","Median...","Mean...","Minimum...","Maximum...","Unsharp Mask...","Variance...","Gaussian Blur 3D...","Median 3D...","Mean 3D...","Minimum 3D...","Maximum 3D...","Variance 3D...","Show Circular Masks..."};
	String []thebatch={"Measure...","Convert...","Macro... ","Virtual Stack..."};
	
	MenuItem[]noise_=new MenuItem[thenoise.length];
	MenuItem[]shadows_=new MenuItem[theshadows.length];
	MenuItem[]binarys_=new MenuItem[thebinarys.length];
	MenuItem[]math_=new MenuItem[themath.length];
	MenuItem[]fft_=new MenuItem[thefft.length];
	MenuItem[]filters_=new MenuItem[thefilters.length];
	MenuItem[]batch_=new MenuItem[thebatch.length];
	
	
	
	
	public ImageJProcessAction ()
	{
	setId("Process");
	setToolTipText("ImageJ");
	setText("Process");		
	setMenuCreator(this);
	}

	
	public Menu getMenu(Control parent) {
	if (fMenu != null) {
	fMenu.dispose();
	}
	fMenu= new Menu(parent);
	
	
	
	
	
	
	
    Menu fMenu2= new Menu(fMenu);
	MenuItem menuItem_type = new MenuItem (fMenu, SWT.CASCADE);
	menuItem_type.setMenu(fMenu2) ;
	menuItem_type.setText ("Noise");
	for (int i = 0; i < thenoise.length; i++) {
		final int count=i;
		noise_[i] = new MenuItem (fMenu2, SWT.PUSH);
		noise_[i].setText (thenoise[i]);
		noise_[i].addSelectionListener(new SelectionListener() {
			
			

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand(thenoise[count]);
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
	}
	
	 Menu fMenu3= new Menu(fMenu);
		MenuItem menuItem_adjust = new MenuItem (fMenu, SWT.CASCADE);
		menuItem_adjust.setMenu(fMenu3) ;
		menuItem_adjust.setText ("Shadows");
	
	for (int i = 0; i < theshadows.length; i++) {
		final int count=i;
		shadows_[i] = new MenuItem (fMenu3, SWT.PUSH);
		shadows_[i].setText (theshadows[i]);
		shadows_[i].addSelectionListener(new SelectionListener() {
			 
			

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand(theshadows[count]);
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
	}
	
	Menu fMenu4= new Menu(fMenu);
	MenuItem menuItem_rotate = new MenuItem (fMenu, SWT.CASCADE);
	menuItem_rotate.setMenu(fMenu4) ;
	menuItem_rotate.setText ("Binary");
	
	for (int i = 0; i < thebinarys.length; i++) {
		final int count=i;
		binarys_[i] = new MenuItem (fMenu4, SWT.PUSH);
		binarys_[i].setText (thebinarys[i]);
		binarys_[i].addSelectionListener(new SelectionListener() {
			
			

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand(thebinarys[count]);
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
	}
	
	Menu fMenu5= new Menu(fMenu);
	MenuItem menuItem_colour = new MenuItem (fMenu, SWT.CASCADE);
	menuItem_colour.setMenu(fMenu5) ;
	menuItem_colour.setText ("Math");
	
	for (int i = 0; i < themath.length; i++) {
		final int count=i;
		math_[i] = new MenuItem (fMenu5, SWT.PUSH);
		math_[i].setText (themath[i]);
		math_[i].addSelectionListener(new SelectionListener() {
			

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand(themath[count]);
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
	}
	
	Menu fMenu6= new Menu(fMenu);
	MenuItem menuItem_stacks = new MenuItem (fMenu, SWT.CASCADE);
	menuItem_stacks.setMenu(fMenu6) ;
	menuItem_stacks.setText ("FFT");
	
	for (int i = 0; i < thefft.length; i++) {
		final int count=i;
		fft_[i] = new MenuItem (fMenu6, SWT.PUSH);
		fft_[i].setText (thefft[i]);
		fft_[i].addSelectionListener(new SelectionListener() {
		

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand(thefft[count]);
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
	}
	
	Menu fMenu7= new Menu(fMenu);
	MenuItem menuItem_zoom = new MenuItem (fMenu, SWT.CASCADE);
	menuItem_zoom.setMenu(fMenu7) ;
	menuItem_zoom.setText ("Filters");
	
	for (int i = 0; i < thefilters.length; i++) {
		final int count=i;
		filters_[i] = new MenuItem (fMenu7, SWT.PUSH);
		filters_[i].setText (thefilters[i]);
		filters_[i].addSelectionListener(new SelectionListener() {
			
			

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand(thefilters[count]);
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
	}
	Menu fMenu8= new Menu(fMenu);
	MenuItem menuItem_batch = new MenuItem (fMenu, SWT.CASCADE);
	menuItem_batch.setMenu(fMenu8) ;
	menuItem_batch.setText ("Batch");
	
	for (int i = 0; i < thebatch.length; i++) {
		final int count=i;
		batch_[i] = new MenuItem (fMenu8, SWT.PUSH);
		batch_[i].setText (thebatch[i]);
		batch_[i].addSelectionListener(new SelectionListener() {
			
			

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand(thebatch[count]);
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
	}
	
	
	
	
	
	MenuItem menuItem1 = new MenuItem (fMenu, SWT.PUSH);
	menuItem1.setText ("Smooth");
	
	MenuItem menuItem2 = new MenuItem (fMenu, SWT.PUSH);
	menuItem2.setText ("Sharpen");
	MenuItem menuItem3 = new MenuItem (fMenu, SWT.PUSH);
	menuItem3.setText ("Find Edges");
	MenuItem menuItem4 = new MenuItem (fMenu, SWT.PUSH);
	menuItem4.setText ("Enhance Contrast");
	MenuItem menuItem5 = new MenuItem (fMenu, SWT.PUSH);
	menuItem5.setText ("Image Calculator");
	
	
	MenuItem menuItem6 = new MenuItem (fMenu, SWT.PUSH);
	
	menuItem6.setText ("Substract Background");
	
	
	MenuItem menuItem7 = new MenuItem (fMenu, SWT.PUSH);
	menuItem7.setText ("Repeat Command");
	MenuItem menuItem8 = new MenuItem (fMenu, SWT.PUSH);
	menuItem8.setText ("Find Maxima...");
	
	
	
	 menuItem1.addSelectionListener(new SelectionListener() {
			
			public void selectionChanged(SelectionChangedEvent event) {
				
				
				
			}

			public void widgetSelected(SelectionEvent e) {
				IJ.getInstance().doCommand("Smooth");
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
	
	
	
	
	
	
	
	
	
    menuItem2.addSelectionListener(new SelectionListener() {
		
		public void selectionChanged(SelectionChangedEvent event) {
			
			
			
		}

		public void widgetSelected(SelectionEvent e) {
			IJ.getInstance().doCommand("Sharpen");
			
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
	});
    menuItem3.addSelectionListener(new SelectionListener() {
		
		public void selectionChanged(SelectionChangedEvent event) {
			
			
			
		}

		public void widgetSelected(SelectionEvent e) {
			IJ.getInstance().doCommand("Find Edges");
			
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
	});
    menuItem4.addSelectionListener(new SelectionListener() {
		
		public void selectionChanged(SelectionChangedEvent event) {
			
			
			
		}

		public void widgetSelected(SelectionEvent e) {
			IJ.getInstance().doCommand("Enhance Contrast...");
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
	});
    menuItem5.addSelectionListener(new SelectionListener() {
		
		public void selectionChanged(SelectionChangedEvent event) {
			
			
			
		}

		public void widgetSelected(SelectionEvent e) {
			
			IJ.getInstance().doCommand("Image Calculator...");
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
	});
    menuItem6.addSelectionListener(new SelectionListener() {
		
		public void selectionChanged(SelectionChangedEvent event) {
			
			
			
		}

		public void widgetSelected(SelectionEvent e) {
			
			IJ.getInstance().doCommand("Subtract Background...");
			
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
	});
    menuItem7.addSelectionListener(new SelectionListener() {
		
		public void selectionChanged(SelectionChangedEvent event) {
			
			
			
		}

		public void widgetSelected(SelectionEvent e) {
			
			IJ.getInstance().doCommand("Repeat Command");
			
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			
		}
	});
    menuItem8.addSelectionListener(new SelectionListener() {
		
		public void selectionChanged(SelectionChangedEvent event) {
			
			
			
		}

		public void widgetSelected(SelectionEvent e) {
			IJ.getInstance().doCommand("Find Maxima...");
			
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