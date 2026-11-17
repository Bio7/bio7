/********************************************************************************
 * Copyright (c) 2020 Marcel Austenfeld
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ********************************************************************************/

package _util; 

import java.awt.Point;
import java.util.ArrayList;

import com.eco.bio7.rbridge.backend.RBackend;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rbridge.RBridge;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import com.eco.bio7.rffm.RValue;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

public class ImageToRTransfer {

	protected static boolean createMatrix;
	private static String l;
	private static String[] firstChar = { ".", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };

	/**
	 * This methods transfers image stack data of ROI selections (ROI Manager) to R
	 * (embedded R). A feature matrix from the slices will be created and
	 * variables for the width, height and the name of the image ROI data will be
	 * created in the R workspace, too. Signatures can be added, too (from the group
	 * number or extracted from the ROI names number at the end seperated by an
	 * underscore. If the group argument is false and if the name of the ROI has no
	 * underscore followed by a signature number no signatures will be added to the
	 * R matrix data!
	 * 
	 * @param imp                A given ImagePlus instance.
	 * @param transferType       The data type as transfer type (0=double,
	 *                           1=integer, 2=byte).
	 * @param minStackSlice      The first stack slice (transfer starts with this
	 *                           slice!).
	 * @param maxStackSlice      The last stack slice. (transfer stops with this
	 *                           slice!).
	 * 
	 * @param signatureFromGroup A boolean if the group number is added as
	 *                           signature. If false the underscore is used!
	 */
	public static void imageFeatureStackSelectionToR(ImagePlus imp, int transferType, int minStackSlice,
			int maxStackSlice, boolean signatureFromGroup) {

		if (transferType == 3) {
			System.out.println(
					"RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the(slice) selection in e.g. byte mode!");
			return;
		}

		/* embedded R (the backend of Bio7) */
		RBackend connection = RBridge.getBackend();

		if (RoiManager.getInstance() != null) {
			Roi[] r = RoiManager.getInstance().getSelectedRoisAsArray();
			if (r.length < 1) {
				System.out.println("NO ROI's available in ROI Manager!");
				return;
			}
			if (RTools.isAlive()) {

				/*
				 * if ((imp.getStackSize()) <= 1) {
				 * System.out.println("Please select a stack with more than one image!");
				 * return; }
				 */

				/* Get the image processor of the image ! */
				ImageProcessor ipSize = imp.getProcessor();
				int w = ipSize.getWidth();
				int h = ipSize.getHeight();

				try {
					connection.eval("imageSizeY<-" + h);

					connection.eval("imageSizeX<-" + w);

				} catch (RException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				/*
				 * Only transfer selected ROI's from the ROI Manager or all if no ROI is
				 * selected!
				 */
				// Roi[] r = RoiManager.getInstance().getSelectedRoisAsArray();
				// items = CanvasView.getCanvas_view().tabFolder.getItems();

				/* Initialize all ROI's in R! */
				for (int ro = 0; ro < r.length; ro++) {

					String roiName = r[ro].getName();
					roiName = correctChars(roiName);
					try {
						connection.eval("try(" + roiName + "<-NULL)");

					} catch (RException e2) {
						// TODO Auto-generated catch block
						System.out.println("Please select a proper name for the ROI's in the ROI Manager\n"
								+ "Some special chars are not supported for the transfer to R!");
						return;
					}
				}

				/*
				 * Here we iterate through all slices and add the ROI's data (layer) for each
				 * slice!
				 */
				for (int i = minStackSlice; i <= maxStackSlice; i++) {
					// count = i;

					ImageStack stack = imp.getImageStack();
					/* Get the image processor of the image ! */
					ImageProcessor ip = stack.getProcessor(i);
					/* Here we iterate over all ROI's in the ROI Manager! */
					for (int ro = 0; ro < r.length; ro++) {

						Roi roiDefault = r[ro];
						String roiName = r[ro].getName();
						roiName = correctChars(roiName);
						ip.setRoi(roiDefault);
						/*
						 * if (roiDefault != null && !roiDefault.isArea()) {
						 * Bio7Dialog.message("The command require\n" +
						 * "an area selection, or no selection."); return; }
						 */

						if (transferType == 0) {
							ArrayList<Double> valuesDouble = getROIPixelsDouble(ip, roiDefault);
							// System.out.println(valuesDouble.size());
							double[] valD = new double[valuesDouble.size()];
							for (int u = 0; u < valD.length; u++) {
								valD[u] = valuesDouble.get(u);
							}
							l = "layer" + i;
							try {
								connection.assign(l, valD);
							} catch (RException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							valuesDouble.clear();
							valD = null;
							try {
								connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
								connection.eval("remove(" + l + ")");
							} catch (RException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						} else if (transferType == 1) {
							ArrayList<Integer> valuesInt = getROIPixelsInteger(ip, roiDefault);
							int[] valI = new int[valuesInt.size()];
							for (int u = 0; u < valI.length; u++) {
								valI[u] = valuesInt.get(u);
							}
							l = "layer" + i;
							try {
								connection.assign(l, valI);
							} catch (RException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							valuesInt.clear();
							valI = null;
							try {
								connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
								connection.eval("remove(" + l + ")");
							} catch (RException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						} else if (transferType == 2) {
							ArrayList<Byte> valuesByte = getROIPixelsByte(ip, roiDefault);
							byte[] valB = new byte[valuesByte.size()];
							for (int u = 0; u < valB.length; u++) {
								valB[u] = valuesByte.get(u);
							}
							l = "layer" + i;
							try {
								connection.assign(l, valB);
							} catch (RException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							valuesByte.clear();
							valB = null;
							try {
								connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
								connection.eval("remove(" + l + ")");
							} catch (RException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
						/*
						 * Here (if enabled in the GUI) we add a class column as the first column in the
						 * matrix after the first stack slice for each ROI in R from the ROI Manager!
						 */
						if (i == minStackSlice) {

							if (transferType == 0) {
								try {

									if (signatureFromGroup) {

										int sigNumber = roiDefault.getGroup();
										if (sigNumber > 0) {
											connection.eval("" + roiName + "<-cbind(rep(c(" + sigNumber + "),length("
													+ roiName + "[,1]))," + roiName + ")");
											connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
										}
									} else {
										int index = roiName.lastIndexOf("_");
										if (index > -1) {
											String sigNumber = roiName.substring(index + 1, roiName.length());
											if (sigNumber.isEmpty() == false) {
												connection.eval("" + roiName + "<-cbind(rep(c(" + sigNumber
														+ "),length(" + roiName + "[,1]))," + roiName + ")");
												connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
											}
										}
									}

								} catch (RException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							} else if (transferType == 1) {
								try {

									if (signatureFromGroup) {
										int sigNumber = roiDefault.getGroup();
										if (sigNumber > 0) {
											connection.eval("" + roiName + "<-cbind(rep(c(as.integer(" + sigNumber
													+ ")),length(" + roiName + "[,1]))," + roiName + ")");
											connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
										}

									} else {

										int index = roiName.lastIndexOf("_");
										if (index > -1) {
											String sigNumber = roiName.substring(index + 1, roiName.length());
											if (sigNumber.isEmpty() == false) {
												connection.eval("" + roiName + "<-cbind(rep(c(as.integer(" + sigNumber
														+ ")),length(" + roiName + "[,1]))," + roiName + ")");
												connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
											}
										}
									}

								} catch (RException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							} else if (transferType == 2) {
								try {

									if (signatureFromGroup) {

										int sigNumber = roiDefault.getGroup();
										if (sigNumber > 0) {
											connection.eval("" + roiName + "<-cbind(rep(c(as.raw(" + sigNumber
													+ ")),length(" + roiName + "[,1]))," + roiName + ")");
											connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
										}

									} else {

										int index = roiName.lastIndexOf("_");
										if (index > -1) {
											String sigNumber = roiName.substring(index + 1, roiName.length());
											if (sigNumber.isEmpty() == false) {
												connection.eval("" + roiName + "<-cbind(rep(c(as.raw(" + sigNumber
														+ ")),length(" + roiName + "[,1]))," + roiName + ")");
												connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
											}
										}
									}

								} catch (RException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							}

						}

					}

				}

				imp = null;

			} else {

				System.out.println("R is not running!\nPlease start R (toolbar action \"Start R\").");
			}
		} else {
			/* If ROI Manager isn't active! */

			System.out.println("Please add selections to the ROI Manager!");

		}

	}

	/**
	 * A method to transfer a integer vector from R to ImageJ as a short image.
	 * 
	 * @param name of the data
	 * @return an ImagePlus object
	 */
	public static ImagePlus imageFromR(String name) {
		ImagePlus imp = null;
		RValue bolXExists = null;
		RValue bolYExists = null;
		RValue bolDoubleExists = null;
		RValue bolExists = null;
		RValue isNumeric = null;
		RValue isImageMatrixNull = null;
		ImageProcessor ip;
		RBackend c = RBridge.getBackend();
		if (RTools.isAlive()) {
			/* Test if the imageMatrix R object is null! */
			try {
				isImageMatrixNull = (RValue) c.eval("try(is.null("+name+"))");
			} catch (RException e) {
				e.printStackTrace();
			}
			boolean[] boolIsImageMatrixNull = isImageMatrixNull.booleans();
			/* Test if the imageMatrix object is null and return null! */
			if (boolIsImageMatrixNull[0]) {
				return null;
			}
			if (name.isEmpty() == false && name != null) {
				try {
					bolExists = (RValue) c.eval("try(exists(\"" + name + "\"))");
				} catch (RException e1) {
					e1.printStackTrace();
				}
				boolean[] bolE = bolExists.booleans();
				if (bolE[0] && name.equals("image") == false) {

					try {
						isNumeric = (RValue) c.eval("try(is.numeric(" + name + ")||is.raw(" + name + "))");
					} catch (RException e) {
						e.printStackTrace();
					}
					boolean[] bolNumeric = isNumeric.booleans();
					if (bolNumeric[0]) {
						try {
							bolXExists = (RValue) c.eval("try(exists(\"imageSizeX\"))");
							bolYExists = (RValue) c.eval("try(exists(\"imageSizeY\"))");
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
								bolDoubleExists = (RValue) c.eval("try(is.integer(" + name + "))");
								boolean[] bolInteger = bolDoubleExists.booleans();
								if (bolInteger[0]) {
									imageData = c.eval("try(" + name + ")").ints();
								} else {
									imageData = c.eval("try(as.integer(" + name + "))").ints();
								}

								if (imageData != null) {

									ip = new ShortProcessor(imagesSizeX, imagesSizeY);
									for (int i = 0; i < imagesSizeY; i++) {

										for (int u = 0; u < imagesSizeX; u++) {
											int value = (imageData[i * imagesSizeX + u]);
											ip.putPixel(u, i, value);

										}

									}
									ip.resetMinAndMax();

									imp = new ImagePlus(name, ip);

								}

							} catch (RTypeException e) {

								e.printStackTrace();
							} catch (RException e) {

								e.printStackTrace();
							}
							imageData = null;
						} else {
							System.out.println("The size variables (imageSizeX, imageSizeY)\n are not available!");
						}

					} else {
						System.out.println("Specified image data is not numeric!");
					}
				} else {
					System.out.println("Specified image data not existent\n"
							+ "in the R workspace!\n (The word image is also forbidden for\n a transfer to ImageJ!)");
				}
			} else {
				System.out.println("No image data name specified!");
			}
		} else {
			System.out.println("R is not running!\nPlease start R (toolbar action \"Start R\").");
		}
		return imp;
	}

	private static ArrayList<Double> getROIPixelsDouble(ImageProcessor ip, Roi roi) {

		ArrayList<Double> values = new ArrayList<Double>();
		for (Point p : roi.getContainedPoints()) {

			values.add(Double.valueOf(ip.getPixelValue(p.x, p.y)));
		}
		return values;
	}

	private static ArrayList<Integer> getROIPixelsInteger(ImageProcessor ip, Roi roi) {

		ArrayList<Integer> values = new ArrayList<Integer>();
		for (Point p : roi.getContainedPoints()) {

			values.add(Integer.valueOf(ip.getPixel(p.x, p.y)));
		}
		return values;
	}

	private static ArrayList<Byte> getROIPixelsByte(ImageProcessor ip, Roi roi) {
		ArrayList<Byte> values = new ArrayList<Byte>();
		for (Point p : roi.getContainedPoints()) {

			values.add(Byte.valueOf((byte) (ip.getPixel(p.x, p.y))));
		}
		return values;
	}

	private static String correctChars(String name) {
		/*
		 * Replace the comma since it is the split argument!
		 */
		name = name.replace(",", ".");

		name = name.replace("-", "_");

		for (int j = 0; j < firstChar.length; j++) {

			if (name.startsWith(firstChar[j])) {
				name = name.replaceFirst(firstChar[j], "X" + firstChar[j]);
			}
		}

		return name;

	}

}
