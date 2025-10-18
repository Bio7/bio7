/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;

class TransferPicJob extends WorkspaceJob {

	private boolean createMatrix;
	private String name;
	private int transferDataType;

	public TransferPicJob(String matrixName, int transferInt) {

		super("Transfer image to R");
		this.name = matrixName;
		this.transferDataType = transferInt;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {

		monitor.beginTask("Transfer image", IProgressMonitor.UNKNOWN);
		createMatrix = false;
		if (name == null || name.equals("")) {
			Bio7Dialog.message("You've defined no name for the image!");

		} else {
			picToR();
		}

		return Status.OK_STATUS;
	}

	/* Transfer type: 0=double, 1=integer, 2=byte */
	private void picToR() {
		double[] pDouble = null;
		int[] pInt = null;
		byte[] pByte = null;
		int y = 0;
		int x = 0;
		if (RServe.isAliveDialog()) {
			RConnection c = RServe.getConnection();
			/* Get the active image ! */
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp != null) {
				if (transferDataType == 0) {
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {
						public void run() {
							MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
							message.setMessage("Should a matrix be created from the image?");
							message.setText("Matrix?");
							int response = message.open();
							if (response == SWT.YES) {

								createMatrix = true;
							}
						}
					});
				} else {
					createMatrix = false;
				}
				/* Get the image processor of the image ! */
				ImageProcessor ip = imp.getProcessor();
				int w = ip.getWidth();
				int h = ip.getHeight();

				try {
					c.eval("imageSizeY<-" + h);

					c.eval("imageSizeX<-" + w);
					c.eval("imageDataName<-'" + name + "'");
				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				/* Double transfer type */
				if (transferDataType == 0) {
					/* From Float images we transfer the scaled values! */
					if (ip instanceof FloatProcessor) {

						pDouble = new double[w * h];

						for (int z = 0; z < h * w; z++) {

							if (x > (w - 1)) {
								y++;
								x = 0;
							}
							int v = ip.getPixel(x, y);
							pDouble[z] = Float.intBitsToFloat(v);

							if (x < w) {
								x++;
							}
						}

					}
					/* Normal transfer of double values! */
					else {
						pDouble = new double[w * h];

						for (int z = 0; z < h * w; z++) {

							if (x > (w - 1)) {
								y++;
								x = 0;
							}
							pDouble[z] = (double) ip.getPixel(x, y);

							if (x < w) {
								x++;
							}
						}
					}
					/*else {
						pDouble = new double[w * h];

						for (int z = 0; z < h * w; z++) {

							if (y > (h - 1)) {
								x++;
								y = 0;
							}
							pDouble[z] = (double) ip.getPixel(x, y);

							if (y < h) {
								y++;
							}
						}
					}*/
					/* We transfer the values to R! */

					try {
						c.assign(name, pDouble);
					} catch (REngineException e) {

						e.printStackTrace();
					}

				}
				/* Integer transfer type! */
				else if (transferDataType == 1) {
					pInt = new int[w * h];

					for (int z = 0; z < h * w; z++) {

						if (x > (w - 1)) {
							y++;
							x = 0;
						}
						pInt[z] = ip.getPixel(x, y);

						if (x < w) {
							x++;
						}
					}

					/* We transfer the values to R! */

					try {
						c.assign(name, pInt);
					} catch (REngineException e) {

						e.printStackTrace();
					}

				}
				/* Byte transfer type! */
				else if (transferDataType == 2) {
					// pByte = (byte[])ip.getPixels();

					/*
					 * The above method does not work and causes an error when a
					 * float image is transfered! This does not happen if every
					 * single value is converted to a byte!
					 */
					pByte = new byte[w * h];
					for (int z = 0; z < h * w; z++) {

						if (x > (w - 1)) {
							y++;
							x = 0;
						}
						pByte[z] = (byte) ip.getPixel(x, y);

						if (x < w) {
							x++;
						}
					}

					/* We transfer the values to R! */

					try {
						c.assign(name, pByte);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				
				else if(transferDataType == 3){
					
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {
						public void run() {

							MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
							message.setMessage("Do you want to create a integer matrix?");
							message.setText("Bio7");
							int response = message.open();
							if (response == SWT.YES) {
								ImageMethods.imageToR(name,true,3,null);

							} else {
								ImageMethods.imageToR(name,false,3,null);

							}

						}
					});
					
					
					
					
					
				}

				/*
				 * We create a matrix with the width and height from the array
				 * or list!
				 */

				if (createMatrix) {
					try {
						c.eval("try(" + name + "<-matrix(" + name + "," + w + "," + h + "))");
						
						//or the transpose c.eval("try(" + name + "<-matrix(" + name + "," + h + "," + w + ", byrow = TRUE))");

					} catch (RserveException e) {

						e.printStackTrace();
					}
				}

			} else {
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES);
						message.setMessage("No image available!");
						message.setText("Bio7");
						int response = message.open();
						if (response == SWT.YES) {
						}
					}
				});
			}
			pDouble = null;
			pInt = null;
			pByte = null;
		}
	}
}
