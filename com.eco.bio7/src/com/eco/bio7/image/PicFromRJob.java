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
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;

class PicFromRJob extends WorkspaceJob {

	private ImagePlus imp;
	private double[][] matrix = null;
	private int type;
	private ImageProcessor ip;
	private String name;
	private int transferDataType;
	private REXPLogical bolExists;
	private REXPLogical bolXExists;
	private REXPLogical bolYExists;
	private REXPLogical bolRawExists;
	private REXPLogical bolDoubleExists;
	private RConnection c;
	private REXPLogical isNumeric;

	public PicFromRJob(int type, String matrixName, int tranferDoubleType) {
		super("Transfer image from R");
		this.type = type;
		this.name = matrixName;
		this.transferDataType = tranferDoubleType;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {

		monitor.beginTask("Transfer image from R", IProgressMonitor.UNKNOWN);
		c = RServe.getConnection();
		picFromR();

		return Status.OK_STATUS;
	}
	/*
	   This method creates an ImageJ image from the given named data in R.
       Image type: 0=ColourProcessor, 1=ByteProcessor, 2=FloatProcessor,3=ShortProcessor
       Transfer typ: 0=Double, 1=Integer, 2=Byte.
    */

	private void picFromR() {
		if (RServe.isAliveDialog()) {
			if (name.isEmpty() == false && name != null) {
				try {
					bolExists = (REXPLogical) c.eval("try(exists(\"" + name + "\"))");

				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				boolean[] bolE = bolExists.isTrue();
				/*
				 * if(bolE[0]){
				 */
				if (bolE[0] && name.equals("image") == false) {
					try {
						isNumeric = (REXPLogical) c.eval("try(is.numeric(" + name + ")||is.raw(" + name + "))");
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					boolean[] bolNumeric = isNumeric.isTrue();

					if (bolNumeric[0]) {

						/*****************************************************************************************/
						/* Transfer doubles! */
						if (transferDataType == 0) {

							REXPLogical bol = null;
							try {
								bol = (REXPLogical) c.eval("try(is.matrix(" + name + "))");
							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bo = bol.isTrue();
							/************************ as matrix! **************************/
							if (bo[0]) {
								try {
									matrix = c.eval("try(" + name + ")").asDoubleMatrix();
								} catch (REXPMismatchException e) {

									e.printStackTrace();
								} catch (RserveException e) {

									e.printStackTrace();
								}

								if (matrix != null) {
									if (type == 0) {
										ip = new ColorProcessor(matrix.length, matrix[0].length);
										for (int i = 0; i < matrix.length; i++) {
											for (int u = 0; u < matrix[0].length; u++) {
												int value = (int) (matrix[i][u]);
												ip.putPixel(i, u, value);

											}

										}
									} else if (type == 1) {
										ip = new ByteProcessor(matrix.length, matrix[0].length);
										for (int i = 0; i < matrix.length; i++) {
											for (int u = 0; u < matrix[0].length; u++) {
												int value = (int) (matrix[i][u]);
												ip.putPixel(i, u, value);

											}

										}
									} else if (type == 2) {
										ip = new FloatProcessor(matrix.length, matrix[0].length);
										for (int i = 0; i < matrix.length; i++) {
											for (int u = 0; u < matrix[0].length; u++) {
												double value = (matrix[i][u]);
												ip.putPixelValue(i, u, value);

											}

										}
										ip.resetMinAndMax();
									} else {
										ip = new ShortProcessor(matrix.length, matrix[0].length);
										for (int i = 0; i < matrix.length; i++) {
											for (int u = 0; u < matrix[0].length; u++) {
												double value = (matrix[i][u]);
												ip.putPixelValue(i, u, value);

											}

										}
										ip.resetMinAndMax();
									}

									imp = new ImagePlus(name, ip);
									imp.show();
									matrix = null;
								}
							}
							/************************ as list! **************************/
							else {
								try {
									bolXExists = (REXPLogical) c.eval("try(exists(\"imageSizeX\"))");
									bolYExists = (REXPLogical) c.eval("try(exists(\"imageSizeY\"))");
								} catch (RserveException e1) {

									e1.printStackTrace();
								}
								boolean[] bolX = bolXExists.isTrue();
								boolean[] bolY = bolYExists.isTrue();
								if (bolX[0] == true && bolY[0] == true) {
									double[] imageData = null;
									int imagesSizeY;
									try {
										imagesSizeY = (int) c.eval("try(imageSizeY)").asDouble();

										int imagesSizeX = (int) c.eval("try(imageSizeX)").asDouble();
										/*
										 * We can proof for integer as transfer
										 * data type so we don't have to proof
										 * for byte, too! integers can be
										 * transfered as doubles!
										 */
										bolDoubleExists = (REXPLogical) c.eval("try(is.double(" + name + "))");
										boolean[] bolInteger = bolDoubleExists.isTrue();
										if (bolInteger[0]) {
											imageData = c.eval("try(" + name + ")").asDoubles();
										} else {
											/*
											 * Again we only need to convert the
											 * data to integers!
											 */
											imageData = c.eval("try(as.integer(" + name + "))").asDoubles();
										}

										if (imageData != null) {
											if (type == 0) {
												ip = new ColorProcessor(imagesSizeX, imagesSizeY);

												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														double value = (imageData[i * imagesSizeX + u]);
														ip.putPixel(u, i, (int) value);

													}

												}
											} else if (type == 1) {
												ip = new ByteProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														double value = (imageData[i * imagesSizeX + u]);

														ip.putPixelValue(u, i, value);

													}

												}
											} else if (type == 2) {
												ip = new FloatProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														double value = (imageData[i * imagesSizeX + u]);
														ip.putPixelValue(u, i, value);

													}

												}
												ip.resetMinAndMax();
											} else {
												ip = new ShortProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														double value = (imageData[i * imagesSizeX + u]);
														ip.putPixelValue(u, i, value);

													}

												}
												ip.resetMinAndMax();
											}

											imp = new ImagePlus(name, ip);
											imp.show();

										}

									} catch (REXPMismatchException e) {

										e.printStackTrace();
									} catch (RserveException e) {

										e.printStackTrace();
									}
									imageData = null;

								} else {
									Bio7Dialog.message("The size variables (imageSizeX, imageSizeY)\n are not available!");
								}
							}
							/************************************* Transfer integers! *******************************************/

						} else if (transferDataType == 1) {

							try {
								bolXExists = (REXPLogical) c.eval("try(exists(\"imageSizeX\"))");
								bolYExists = (REXPLogical) c.eval("try(exists(\"imageSizeY\"))");
							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bolX = bolXExists.isTrue();
							boolean[] bolY = bolYExists.isTrue();
							if (bolX[0] == true && bolY[0] == true) {
								int[] imageData = null;
								int imagesSizeY;
								try {
									imagesSizeY = (int) c.eval("try(imageSizeY)").asDouble();

									int imagesSizeX = (int) c.eval("try(imageSizeX)").asDouble();
									bolDoubleExists = (REXPLogical) c.eval("try(is.integer(" + name + "))");
									boolean[] bolInteger = bolDoubleExists.isTrue();
									if (bolInteger[0]) {
										imageData = c.eval("try(" + name + ")").asIntegers();
									} else {
										imageData = c.eval("try(as.integer(" + name + "))").asIntegers();
									}

									if (imageData != null) {
										if (type == 0) {
											ip = new ColorProcessor(imagesSizeX, imagesSizeY);

											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);
													ip.putPixel(u, i, value);

												}

											}
										} else if (type == 1) {
											ip = new ByteProcessor(imagesSizeX, imagesSizeY);
											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);

													ip.putPixel(u, i, value);

												}

											}
										} else if (type == 2) {
											ip = new FloatProcessor(imagesSizeX, imagesSizeY);
											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);
													ip.putPixel(u, i, value);

												}

											}
											ip.resetMinAndMax();
										} else {
											ip = new ShortProcessor(imagesSizeX, imagesSizeY);
											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);
													ip.putPixel(u, i, value);

												}

											}
											ip.resetMinAndMax();
										}

										imp = new ImagePlus(name, ip);
										imp.show();

									}

								} catch (REXPMismatchException e) {

									e.printStackTrace();
								} catch (RserveException e) {

									e.printStackTrace();
								}
								imageData = null;
							} else {
								Bio7Dialog.message("The size variables (imageSizeX, imageSizeY)\n are not available!");
							}
						}
						/************************************** Transfer Bytes! **************************************************************/

						else if (transferDataType == 2) {

							byte[] imageData;
							int imagesSizeY;
							try {
								bolRawExists = (REXPLogical) c.eval("try(is.raw(" + name + "))");
								bolXExists = (REXPLogical) c.eval("try(exists(\"imageSizeX\"))");
								bolYExists = (REXPLogical) c.eval("try(exists(\"imageSizeY\"))");

							} catch (RserveException e1) {
								e1.printStackTrace();
							}
							boolean[] bolRaw = bolRawExists.isTrue();
							boolean[] bolX = bolXExists.isTrue();
							boolean[] bolY = bolYExists.isTrue();
							if (bolRaw[0] == true) {
								if (bolX[0] == true && bolY[0] == true) {
									try {
										imagesSizeY = (int) c.eval("try(imageSizeY)").asDouble();

										int imagesSizeX = (int) c.eval("try(imageSizeX)").asDouble();
										imageData = c.eval("try(" + name + ")").asBytes();

										if (imageData != null) {
											if (type == 0) {
												ip = new ColorProcessor(imagesSizeX, imagesSizeY);

												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														int value = (imageData[i * imagesSizeX + u]);
														ip.putPixel(u, i, value & 0xff);

													}

												}
											} else if (type == 1) {
												ip = new ByteProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														byte value = (imageData[i * imagesSizeX + u]);

														ip.putPixel(u, i, value & 0xff);

													}

												}
											} else if (type == 2) {
												ip = new FloatProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														int value = (imageData[i * imagesSizeX + u]);
														ip.putPixel(u, i, value & 0xff);

													}

												}
												ip.resetMinAndMax();
											} else {
												ip = new ShortProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														int value = (imageData[i * imagesSizeX + u]);
														ip.putPixel(u, i, value & 0xff);

													}

												}
												ip.resetMinAndMax();
											}

											imp = new ImagePlus(name, ip);
											imp.show();

										}
									} catch (REXPMismatchException e) {

										Bio7Dialog.message("An transfer error occured!\n" + "Please select the correct image type!\n" + "For a byte transfer a list has to be present!\n");
									} catch (RserveException e) {

										e.printStackTrace();
									}
									imageData = null;

								} else {
									Bio7Dialog.message("The size variables (imageSizeX, imageSizeY)\n are not available!");
								}
							} else {

								Bio7Dialog.message("Raw data (as.raw(yourData)) is required for the byte transfer!");
							}

						}

					} else {
						Bio7Dialog.message("Specified image data is not numeric!");
					}
				} else {
					Bio7Dialog.message("Specified image data not existent\n" + "in the R workspace!\n (The word image is also forbidden for\n a transfer to ImageJ!)");
				}
			} else {
				Bio7Dialog.message("No image data name specified!");
			}
		}
	}

}
