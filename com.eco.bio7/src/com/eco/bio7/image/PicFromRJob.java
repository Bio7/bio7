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
import ij.process.DoubleProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import com.eco.bio7.rbridge.backend.RBackend;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rffm.RValue;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

class PicFromRJob extends WorkspaceJob {

	private ImagePlus imp;
	private double[][] matrix = null;
	private int type;
	private ImageProcessor ip;
	private String name;
	private int transferDataType;
	private RValue bolExists;
	private RValue bolXExists;
	private RValue bolYExists;
	private RValue bolRawExists;
	private RValue bolDoubleExists;
	private RBackend c;
	private RValue isNumeric;

	public PicFromRJob(int type, String matrixName, int tranferDoubleType) {
		super("Transfer image from R");
		this.type = type;
		this.name = matrixName;
		this.transferDataType = tranferDoubleType;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {

		monitor.beginTask("Transfer image from R", IProgressMonitor.UNKNOWN);
		c = RBridge.getBackend();
		picFromR();

		return Status.OK_STATUS;
	}
	/*
	   This method creates an ImageJ image from the given named data in R.
       Image type: 0=ColourProcessor, 1=ByteProcessor, 2=FloatProcessor,3=ShortProcessor, 4=DoubleProcessor
       Transfer typ: 0=Double, 1=Integer, 2=Byte.
    */

	private void picFromR() {
		if (RTools.isAliveDialog()) {
			if (name.isEmpty() == false && name != null) {
				try {
					bolExists = c.eval("try(exists(\"" + name + "\"))");

				} catch (RException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				boolean[] bolE = bolExists.booleans();
				/*
				 * if(bolE[0]){
				 */
				if (bolE[0] && name.equals("image") == false) {
					try {
						isNumeric = c.eval("try(is.numeric(" + name + ")||is.raw(" + name + "))");
					} catch (RException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					boolean[] bolNumeric = isNumeric.booleans();

					if (bolNumeric[0]) {

						/*****************************************************************************************/
						/* Transfer doubles! */
						if (transferDataType == 0) {

							RValue bol = null;
							try {
								bol = c.eval("try(is.matrix(" + name + "))");
							} catch (RException e1) {

								e1.printStackTrace();
							}
							boolean[] bo = bol.booleans();
							/************************ as matrix! **************************/
							if (bo[0]) {
								try {
									matrix = c.eval("try(" + name + ")").matrix();
								} catch (RTypeException e) {

									e.printStackTrace();
								} catch (RException e) {

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
									} else if (type == 4) {
										ip = new DoubleProcessor(matrix.length, matrix[0].length);
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
									bolXExists = c.eval("try(exists(\"imageSizeX\"))");
									bolYExists = c.eval("try(exists(\"imageSizeY\"))");
								} catch (RException e1) {

									e1.printStackTrace();
								}
								boolean[] bolX = bolXExists.booleans();
								boolean[] bolY = bolYExists.booleans();
								if (bolX[0] == true && bolY[0] == true) {
									double[] imageData = null;
									int imagesSizeY;
									try {
										imagesSizeY = (int) c.eval("try(imageSizeY)").firstDouble();

										int imagesSizeX = (int) c.eval("try(imageSizeX)").firstDouble();
										/*
										 * We can proof for integer as transfer
										 * data type so we don't have to proof
										 * for byte, too! integers can be
										 * transfered as doubles!
										 */
										bolDoubleExists = c.eval("try(is.double(" + name + "))");
										boolean[] bolInteger = bolDoubleExists.booleans();
										if (bolInteger[0]) {
											imageData = c.eval("try(" + name + ")").doubles();
										} else {
											/*
											 * Again we only need to convert the
											 * data to integers!
											 */
											imageData = c.eval("try(as.integer(" + name + "))").doubles();
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
											} else if (type == 4) {
												ip = new DoubleProcessor(imagesSizeX, imagesSizeY);
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

									} catch (RTypeException e) {

										e.printStackTrace();
									} catch (RException e) {

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
								bolXExists = c.eval("try(exists(\"imageSizeX\"))");
								bolYExists = c.eval("try(exists(\"imageSizeY\"))");
							} catch (RException e1) {

								e1.printStackTrace();
							}
							boolean[] bolX = bolXExists.booleans();
							boolean[] bolY = bolYExists.booleans();
							if (bolX[0] == true && bolY[0] == true) {
								int[] imageData = null;
								int imagesSizeY;
								try {
									imagesSizeY = (int) c.eval("try(imageSizeY)").firstDouble();

									int imagesSizeX = (int) c.eval("try(imageSizeX)").firstDouble();
									bolDoubleExists = c.eval("try(is.integer(" + name + "))");
									boolean[] bolInteger = bolDoubleExists.booleans();
									if (bolInteger[0]) {
										imageData = c.eval("try(" + name + ")").ints();
									} else {
										imageData = c.eval("try(as.integer(" + name + "))").ints();
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
													ip.putPixelValue(u, i, value);

												}

											}
											ip.resetMinAndMax();
										} else if (type == 4) {
											ip = new DoubleProcessor(imagesSizeX, imagesSizeY);
											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);
													ip.putPixelValue(u, i, value);

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

								} catch (RTypeException e) {

									e.printStackTrace();
								} catch (RException e) {

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
								bolRawExists = c.eval("try(is.raw(" + name + "))");
								bolXExists = c.eval("try(exists(\"imageSizeX\"))");
								bolYExists = c.eval("try(exists(\"imageSizeY\"))");

							} catch (RException e1) {
								e1.printStackTrace();
							}
							boolean[] bolRaw = bolRawExists.booleans();
							boolean[] bolX = bolXExists.booleans();
							boolean[] bolY = bolYExists.booleans();
							if (bolRaw[0] == true) {
								if (bolX[0] == true && bolY[0] == true) {
									try {
										imagesSizeY = (int) c.eval("try(imageSizeY)").firstDouble();

										int imagesSizeX = (int) c.eval("try(imageSizeX)").firstDouble();
										imageData = c.eval("try(" + name + ")").bytes();

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
														ip.putPixelValue(u, i, value & 0xff);

													}

												}
												ip.resetMinAndMax();
											} else if (type == 4) {
												ip = new DoubleProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														int value = (imageData[i * imagesSizeX + u]);
														ip.putPixelValue(u, i, value & 0xff);

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
									} catch (RTypeException e) {

										Bio7Dialog.message("An transfer error occured!\n" + "Please select the correct image type!\n" + "For a byte transfer a list has to be present!\n");
									} catch (RException e) {

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
