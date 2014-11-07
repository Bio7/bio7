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
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;

import uk.co.mmscomputing.device.scanner.Scanner;
import uk.co.mmscomputing.device.scanner.ScannerDevice;
import uk.co.mmscomputing.device.scanner.ScannerIOException;
import uk.co.mmscomputing.device.scanner.ScannerIOMetadata;
import uk.co.mmscomputing.device.scanner.ScannerListener;

import com.eco.bio7.image.CanvasView;
import com.eco.bio7.image.IJTabs;

public class ImageJFileAction extends Action implements IMenuCreator, ScannerListener {

	private Menu fMenu;

	String[] newImage = { "Image...", "Text Window", "Internal Clipboard", "System Clipboard" };

	String[] imp = { "Image Sequence...", "Raw...", "LUT... ", "Text Image... ", "Text File... ", "Results... ", "URL...", "Stack From List...", "TIFF Virtual Stack...", "AVI...",
			"XY Coordinates... " };

	String[] saveas = { "Tiff...", "Gif...", "Jpeg...", "BMP...", "PNG...", "PGM...", "Text Image...", "ZIP...", "Raw Data...", "Image Sequence... ", "AVI... ", "FITS...", "LUT...", "Selection...",
			"XY Coordinates...", "Results...", "Text..." };

	String[] url = { "particles.gif", "blobs.gif", "mri-stack.zip", "leaf.jpg", "Tree_Rings.jpg", "Cell_Colony.jpg", "FluorescentCells.jpg", "baboon.jpg", "bat-cochlea-volume.zip", "embryos.jpg",
			"flybrain.zip", "Rat_Hippocampal_Neuron.zip", "Spindly-GFP.zip", "organ-of-corti.zip", "confocal-series.zip" };

	MenuItem[] save_as = new MenuItem[saveas.length];

	MenuItem[] import_as = new MenuItem[imp.length];

	MenuItem[] samples = new MenuItem[url.length];

	Scanner scanner;

	public ImageJFileAction() {
		setId("File");
		setToolTipText("ImageJ");
		setText("File");
		setMenuCreator(this);
		// uk.co.mmscomputing.util.JarLib.load(jtwain.class,"D:/eclipse-3.41/com.eco.bio7.libs/libs//uk/co/mmscomputing/device/twain/win32/jtwain");
		try {
			 scanner = Scanner.getDevice();
			 scanner.addListener(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		Menu fMenu1 = new Menu(fMenu);
		MenuItem menuItem = new MenuItem(fMenu, SWT.CASCADE);
		menuItem.setMenu(fMenu1);
		menuItem.setText("New");
		for (int i = 0; i < newImage.length; i++) {
			final int count = i;
			import_as[i] = new MenuItem(fMenu1, SWT.PUSH);
			import_as[i].setText(newImage[i]);
			import_as[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {

					IJ.getInstance().doCommand(newImage[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);
		menuItem2.setText("Open");
		MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);
		menuItem3.setText("Open Next");

		/*
		 * MenuItem menuItem5 = new MenuItem(fMenu, SWT.PUSH);
		 * menuItem5.setText("Open Recent");
		 */
		Menu fMenu2 = new Menu(fMenu);

		MenuItem menuItem21 = new MenuItem(fMenu, SWT.CASCADE);
		menuItem21.setMenu(fMenu2);
		menuItem21.setText("Import");
		for (int i = 0; i < imp.length; i++) {
			final int count = i;
			import_as[i] = new MenuItem(fMenu2, SWT.PUSH);
			import_as[i].setText(imp[i]);
			import_as[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {

					IJ.getInstance().doCommand(imp[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		MenuItem menuItem7 = new MenuItem(fMenu, SWT.PUSH);
		menuItem7.setText("Save");

		Menu fMenu3 = new Menu(fMenu);
		MenuItem menuItem8 = new MenuItem(fMenu, SWT.CASCADE);
		menuItem8.setMenu(fMenu3);
		menuItem8.setText("Saveas");

		for (int i = 0; i < saveas.length; i++) {
			final int count = i;
			save_as[i] = new MenuItem(fMenu3, SWT.PUSH);
			save_as[i].setText(saveas[i]);
			save_as[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {

					IJ.getInstance().doCommand(saveas[count]);

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		Menu fMenu4 = new Menu(fMenu);
		MenuItem menuItem4 = new MenuItem(fMenu, SWT.CASCADE);
		menuItem4.setMenu(fMenu4);

		menuItem4.setText("Open Samples");
		for (int i = 0; i < url.length; i++) {
			final int count = i;
			samples[i] = new MenuItem(fMenu4, SWT.PUSH);
			samples[i].setText(url[i]);
			samples[i].addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					Job job = new Job("Open...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							monitor.beginTask("Opening: " + url, IProgressMonitor.UNKNOWN);
							final ImagePlus imp;

							IJ.showStatus("Opening: " + url);

							imp = IJ.openImage("http://rsb.info.nih.gov/ij/images/" + url[count]);
							/*
							 * ImagePlus imp = new
							 * ImagePlus("http://rsb.info.nih.gov/ij/images/" +
							 * url[count]); WindowManager.checkForDuplicateName
							 * = true;
							 */
							SwingUtilities.invokeLater(new Runnable() {
								// !!
								public void run() {

									imp.show();
									IJ.showStatus("");
								}
							});

							monitor.done();
							return Status.OK_STATUS;
						}

					};

					// job.setSystem(true);
					job.schedule();

				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});
		}

		MenuItem menuItem9 = new MenuItem(fMenu, SWT.PUSH);
		menuItem9.setText("Revert");
		MenuItem menuItem10 = new MenuItem(fMenu, SWT.PUSH);
		menuItem10.setText("Page Setup");
		MenuItem menuItem11 = new MenuItem(fMenu, SWT.PUSH);
		menuItem11.setText("Print");
		MenuItem menuItem13 = new MenuItem(fMenu, SWT.PUSH);
		menuItem13.setText("Select Device");
		MenuItem menuItem14 = new MenuItem(fMenu, SWT.PUSH);
		menuItem14.setText("Aquire");
		MenuItem menuItem6 = new MenuItem(fMenu, SWT.PUSH);
		menuItem6.setText("Close All Tabs And Views");

		menuItem2.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {

						IJ.getInstance().doCommand("Open...");
					}
				});

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem3.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Open...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		/*
		 * menuItem5.addSelectionListener(new SelectionListener() {
		 * 
		 * public void selectionChanged(SelectionChangedEvent event) { }
		 * 
		 * public void widgetSelected(SelectionEvent e) {
		 * 
		 * IJ.getInstance().doCommand("OpenRecent"); }
		 * 
		 * public void widgetDefaultSelected(SelectionEvent e) { } });
		 */
		menuItem6.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {
                /*Close detached views! Not reliable for many perspectives!*/
				/*IWorkbenchPage wbp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				CanvasView canv = CanvasView.getCanvas_view();
				ArrayList<String> detArr = canv.getDetachedSecViewIDs();

				for (int i = 0; i < detArr.size(); i++) {
					wbp.hideView(wbp.findViewReference("com.eco.bio7.image.detachedImage", detArr.get(i)));
				}

				detArr.clear();*/
                /*Close the tabs!*/
				IJTabs.deleteAllTabs();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem7.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Save");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem9.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Revert");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem10.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Page Setup...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem11.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IJ.getInstance().doCommand("Print...");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuItem13.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				if (scanner.isBusy() == false) {
					try {

						scanner.select();

					} catch (ScannerIOException e1) {
						// TODO Auto-generated catch block
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.asyncExec(new Runnable() {
							public void run() {
								MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES);
								message.setMessage("Access denied! Close all Twain dialogs!");
								message.setText("Bio7");
								int response = message.open();
								if (response == SWT.YES) {
								}
							}
						});
						// e1.printStackTrace();
					}
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		menuItem14.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				try {
					scanner.acquire();
				} catch (ScannerIOException e1) {
					// TODO Auto-generated catch block
					// e1.printStackTrace();
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.asyncExec(new Runnable() {
						public void run() {
							MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES);
							message.setMessage("Access denied! \nTwain dialog maybe already opened!");
							message.setText("Bio7");
							int response = message.open();
							if (response == SWT.YES) {
							}
						}
					});
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		/*
		 * menuItem.addSelectionListener(new SelectionListener() {
		 * 
		 * public void selectionChanged(SelectionChangedEvent event) { }
		 * 
		 * public void widgetSelected(SelectionEvent e) {
		 * 
		 * IJ.getInstance().doCommand("Image..."); }
		 * 
		 * public void widgetDefaultSelected(SelectionEvent e) { } });
		 */
		return fMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	@Override
	public void update(ScannerIOMetadata.Type type, ScannerIOMetadata metadata) {

		if (type.equals(ScannerIOMetadata.ACQUIRED)) {
			BufferedImage image = metadata.getImage();
			ImagePlus imp = new ImagePlus("Scan", image);
			imp.show();
			image = null;
			metadata.setImage(null);
			try {
				new uk.co.mmscomputing.concurrent.Semaphore(0, true).tryAcquire(2000, null);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}

		} else if (type.equals(ScannerIOMetadata.NEGOTIATE)) {
			ScannerDevice device = metadata.getDevice();
			try {
				device.setResolution(100);
			} catch (ScannerIOException e) {

				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			/*
			 * try{ device.setShowUserInterface(true);
			 * device.setShowProgressBar(true);
			 * device.setRegionOfInterest(0,0,210.0,300.0);
			 * device.setResolution(100); }catch(Exception e){
			 * e.printStackTrace(); }
			 */
		} else if (type.equals(ScannerIOMetadata.STATECHANGE)) {

			// System.err.println(metadata.getStateStr());
		} else if (type.equals(ScannerIOMetadata.EXCEPTION)) {
			// metadata.getException().printStackTrace();

		}
	}

}