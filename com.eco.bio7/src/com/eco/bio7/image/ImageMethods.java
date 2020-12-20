package com.eco.bio7.image;

import java.awt.Point;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Display;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.util.Util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class ImageMethods {

	protected static boolean createMatrix;
	private static String l;
	private static String[] firstChar = { ".", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };

	/**
	 * This methods transfers image data from ImageJ to R by means of Rserve. Matrix
	 * or vector data is created and variables for the width, height and the name of
	 * the image will be created.
	 * 
	 * 
	 * @param name             the name for the data.
	 * @param matrix           a boolean, if true automatically a data matrix will
	 *                         be created (only for double values - in case of an
	 *                         RGB transfer an integer matrix will be created!).
	 * @param transferDataType an integer value for the data type which will be
	 *                         transfered. (0=double, 1=integer, 2=byte, 3=RGB as
	 *                         single byte vectors or integer matrix)
	 * @param impPlus          an optional ImagePlus object as the default image
	 *                         source.
	 * 
	 */

	public static void imageToR(String name, boolean matrix, int transferDataType, ImagePlus impPlus) {
		double[] pDouble = null;
		int[] pInt = null;
		byte[] pByte = null;

		byte[] pRByte = null;
		byte[] pGByte = null;
		byte[] pBByte = null;
		int y = 0;
		int x = 0;
		ImagePlus imp = null;
		if (RServe.isAlive()) {
			RConnection c = RServe.getConnection();
			/* Get the active image ! */

			if (impPlus == null) {
				imp = WindowManager.getCurrentImage();
			} else {
				imp = impPlus;
			}
			if (imp != null) {

				// if (transferDataType == 0 || transferDataType == 3) {
				createMatrix = matrix;
				// } else {
				// createMatrix = false;
				// }

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
					/* We transfer the values to R! */

					try {
						c.assign(name, pDouble);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				/* Byte transfer type! */
				else if (transferDataType == 2) {
					// pByte = (byte[])ip.getPixels();

					/*
					 * The above method does not work and causes an error when a float image is
					 * transfered! This does not happen if every single value is converted to a
					 * byte!
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
				/* R,G,B Byte transfer type! */
				else if (transferDataType == 3) {
					// pByte = (byte[])ip.getPixels();

					/*
					 * The above method does not work and causes an error when a float image is
					 * transfered! This does not happen if every single value is converted to a
					 * byte!
					 */
					pRByte = new byte[w * h];
					pGByte = new byte[w * h];
					pBByte = new byte[w * h];
					for (int z = 0; z < h * w; z++) {

						if (x > (w - 1)) {
							y++;
							x = 0;
						}
						int RGB = ip.getPixel(x, y);

						pRByte[z] = (byte) ((RGB >> 16) & 0xff);
						pGByte[z] = (byte) ((RGB >> 8) & 0xff);
						pBByte[z] = (byte) (RGB & 0xff);

						if (x < w) {
							x++;
						}
					}

					/* We transfer the values to R! */

					try {
						c.assign(name + "_R", pRByte);
						c.assign(name + "_G", pGByte);
						c.assign(name + "_B", pBByte);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				/*
				 * We create a matrix with the width and height from the array or list. Only in
				 * double mode!
				 */

				if (createMatrix) {
					if (transferDataType == 3) {// Not used at the moment!
						try {

							c.eval("try(" + name + "<-cbind(as.integer(" + name + "_R" + ")))");
							c.eval("try(remove(" + name + "_R" + "))");
							c.eval("try(" + name + "<-cbind(" + name + ",as.integer(" + name + "_G" + ")))");
							c.eval("try(remove(" + name + "_G" + "))");
							c.eval("try(" + name + "<-cbind(" + name + ",as.integer(" + name + "_B" + ")))");
							c.eval("try(remove(" + name + "_B" + "))");

						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} else {

						try {
							c.eval("try(" + name + "<-matrix(" + name + "," + w + "," + h + "))");

							// c.eval("try(remove(imageData))");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			} else {
				System.out.println("No image available!");
			}
			pDouble = null;
			pInt = null;
			pByte = null;
			pRByte = null;
			pGByte = null;
			pBByte = null;

		}
	}

	/**
	 * This method creates an ImageJ image from the given named data (matrix or
	 * vector) in R. This method expects the variables imageSizeX, imageSizeY to be
	 * present in the R workspace if vector data is transferred.
	 * 
	 * @param type             an integer which represents an ImageJ image
	 *                         type.(0=ColourProcessor, 1=ByteProcessor,
	 *                         2=FloatProcessor, 3=ShortProcessor)
	 * @param name             a string identifier for the R data.
	 * 
	 * @param transferDataType the data type as transfer type (0=double, 1=integer,
	 *                         2=byte).
	 * 
	 */

	public static void imageFromR(int type, String name, int transferDataType) {
		ImagePlus imp;
		double[][] matrix = null;
		REXPLogical bolXExists = null;
		REXPLogical bolYExists = null;
		REXPLogical bolDoubleExists = null;
		REXPLogical bolExists = null;
		REXPLogical bolRawExists = null;
		REXPLogical isNumeric = null;
		ImageProcessor ip;
		RConnection c = RServe.getConnection();
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
										 * We can proof for integer as transfer data type so we don't have to proof for
										 * byte, too! integers can be transfered as doubles!
										 */
										bolDoubleExists = (REXPLogical) c.eval("try(is.double(" + name + "))");
										boolean[] bolInteger = bolDoubleExists.isTrue();
										if (bolInteger[0]) {
											imageData = c.eval("try(" + name + ")").asDoubles();
										} else {
											/*
											 * Again we only need to convert the data to integers if it raw data e.g.!
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
							/*************************************
							 * Transfer integers!
							 *******************************************/

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
						/**************************************
						 * Transfer Bytes!
						 **************************************************************/

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

										System.out.println("An transfer error occured!\n" + "Please select the correct image type!\n" + "For a byte transfer a list has to be present!\n");
									} catch (RserveException e) {

										e.printStackTrace();
									}
									imageData = null;

								} else {
									System.out.println("The size variables (imageSizeX, imageSizeY)\n are not available!");
								}
							} else {

								System.out.println("Raw data (as.raw(yourData)) is required for the byte transfer!");
							}

						}

					} else {
						System.out.println("Specified image data is not numeric!");
					}
				} else {
					System.out.println("Specified image data not existent\n" + "in the R workspace!\n (The word image is also forbidden for\n a transfer to ImageJ!)");
				}
			} else {
				System.out.println("No image data name specified!");
			}
		} else {
			System.out.println("Rserve is not alive!");
		}
	}

	/*
	 * Method for cluster analysis and PCR with fixed imageData as name!
	 * 0=double,1=integer, 2=byte
	 */
	protected static void imagePlusToR(ImagePlus imp, String name, boolean matrix, int transferDataType) {

		double[] pDouble = null;
		int[] pInt = null;
		byte[] pByte = null;
		int y = 0;
		int x = 0;
		if (RServe.isAlive()) {
			RConnection c = RServe.getConnection();
			/* Get the active image ! */

			if (imp != null) {

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
					/* We transfer the values to R! */

					try {
						c.assign("imageData", pDouble);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					pDouble = null;
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
						c.assign("imageData", pInt);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					pInt = null;
				}
				/* Byte transfer type! */
				else if (transferDataType == 2) {
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
						c.assign("imageData", pByte);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					pByte = null;
				}

			} else {

				System.out.println("No image available!");

			}

		} else {
			System.out.println("RServer connection failed - Server is not running !");
		}
	}

	/**
	 * A method to transfer byte information directly to an image as one container.
	 * If no image is present a new one will be created. This method expects the
	 * variables imageSizeX, imageSizeY to be present in the R workspace.
	 * 
	 * @param dataName the name of the R vector.
	 * @param type     the type of the image (1 = byte, 2 = float).
	 */
	public static void transferImageInPlace(String dataName, int type) {

		ImagePlus imp = WindowManager.getCurrentImage();
		ByteProcessor ip;
		FloatProcessor ipFloat;
		int imagesSizeY = 0;
		int imagesSizeX = 0;

		try {
			imagesSizeY = (int) RServe.getConnection().eval("try(imageSizeY)").asDouble();
			imagesSizeX = (int) RServe.getConnection().eval("try(imageSizeX)").asDouble();
		} catch (REXPMismatchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RserveException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (type == 1) {
			if (imp == null) {

				/* If no image is present we create one with random pixels! */
				ip = new ByteProcessor(imagesSizeX, imagesSizeY);
				ip.setColor(java.awt.Color.white);

				ip.fill();

				byte[] imageData = null;

				try {

					imageData = RServe.getConnection().eval("try(" + dataName + ")").asBytes();

				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				ip = new ByteProcessor(imagesSizeX, imagesSizeY);
				for (int i = 0; i < imagesSizeY; i++) {

					for (int u = 0; u < imagesSizeX; u++) {
						byte value = (imageData[i * imagesSizeX + u]);
						ip.putPixel(u, i, value & 0xff);

					}

				}
				imp = new ImagePlus(dataName, ip);
				imp.show();
			} else {
				ip = (ByteProcessor) imp.getProcessor();
				byte[] imageData = null;
				if (ip.getWidth() == imagesSizeX && ip.getHeight() == imagesSizeY) {

					try {
						imageData = RServe.getConnection().eval("try(" + dataName + ")").asBytes();
					} catch (REXPMismatchException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					for (int i = 0; i < imagesSizeY; i++) {

						for (int u = 0; u < imagesSizeX; u++) {
							byte value = (imageData[i * imagesSizeX + u]);
							ip.putPixel(u, i, value & 0xff);

						}

					}

				}
				imp.updateAndDraw();
			}
		} else {
			if (imp == null) {

				/* If no image is present we create one with random pixels! */
				ipFloat = new FloatProcessor(imagesSizeX, imagesSizeY);
				ipFloat.setColor(java.awt.Color.white);

				ipFloat.fill();

				double[] imageDataFloat = null;

				try {

					imageDataFloat = RServe.getConnection().eval("try(" + dataName + ")").asDoubles();

				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				ipFloat = new FloatProcessor(imagesSizeX, imagesSizeY);
				for (int i = 0; i < imagesSizeY; i++) {

					for (int u = 0; u < imagesSizeX; u++) {
						double value = (imageDataFloat[i * imagesSizeX + u]);
						ipFloat.putPixelValue(u, i, value);
					}

				}
				ipFloat.resetMinAndMax();
				imp = new ImagePlus(dataName, ipFloat);
				imp.show();
			} else {
				ipFloat = (FloatProcessor) imp.getProcessor();
				double[] imageDataFloat = null;
				if (ipFloat.getWidth() == imagesSizeX && ipFloat.getHeight() == imagesSizeY) {

					try {
						imageDataFloat = RServe.getConnection().eval("try(" + dataName + ")").asDoubles();
					} catch (REXPMismatchException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					for (int i = 0; i < imagesSizeY; i++) {

						for (int u = 0; u < imagesSizeX; u++) {
							double value = (imageDataFloat[i * imagesSizeX + u]);
							ipFloat.putPixelValue(u, i, value);

						}

					}
					ipFloat.resetMinAndMax();

				}
				imp.updateAndDraw();
			}

		}
	}

	/**
	 * A method to transfer Color information (as datatype raw) directly to an image
	 * as one container. If no image is present a new one will be created. This
	 * method expects the variables imageSizeX, imageSizeY to be present in the R
	 * workspace.
	 * 
	 * @param dataName the name of the raw R vector.
	 * @param type     the type of the image.
	 */
	public static void transferRGBImageInPlace(String dataName) {

		ImagePlus imp = WindowManager.getCurrentImage();
		ColorProcessor ip;
		int imagesSizeY = 0;
		int imagesSizeX = 0;

		try {
			imagesSizeY = (int) RServe.getConnection().eval("try(imageSizeY)").asDouble();
			imagesSizeX = (int) RServe.getConnection().eval("try(imageSizeX)").asDouble();
		} catch (REXPMismatchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RserveException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (imp == null) {

			/* If no image is present we create one with random pixels! */
			ip = new ColorProcessor(imagesSizeX, imagesSizeY);
			ip.setColor(java.awt.Color.white);

			ip.fill();

			byte[] imageData = null;

			try {

				imageData = RServe.getConnection().eval("try(" + dataName + ")").asBytes();

			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ip = new ColorProcessor(imagesSizeX, imagesSizeY);
			for (int i = 0; i < imagesSizeY; i++) {

				for (int u = 0; u < imagesSizeX; u++) {
					int value = (imageData[i * imagesSizeX + u]);
					// value is a bit-packed RGB value

					/*
					 * int red = value & 0xff; int green = (value >> 8) & 0xff; int blue = (value
					 * >>16) & 0xff; int result= red<<16|green<<8|blue;
					 */

					ip.putPixel(u, i, value);

				}

			}
			imp = new ImagePlus(dataName, ip);
			imp.show();
		} else {
			ip = (ColorProcessor) imp.getProcessor();
			byte[] imageData = null;
			if (ip.getWidth() == imagesSizeX && ip.getHeight() == imagesSizeY) {

				try {
					imageData = RServe.getConnection().eval("try(" + dataName + ")").asBytes();
				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				for (int i = 0; i < imagesSizeY; i++) {

					for (int u = 0; u < imagesSizeX; u++) {
						int value = (imageData[i * imagesSizeX + u]);
						ip.putPixel(u, i, value);

					}

				}

			}
			imp.updateAndDraw();
		}
	}

	/**
	 * This methods transfers image stack data to R by means of Rserve. A feature
	 * matrix from the slices will be created and variables for the width, height
	 * and the name of the image stack will be created in the R workspace, too.
	 * 
	 * 
	 * @param name             the name for the data matrix.
	 * 
	 * @param transferDataType an integer value for the data type which will be
	 *                         transfered. (0=double, 1=integer, 2=byte
	 * 
	 * @param impPlus          an optional ImagePlus object as the default image
	 *                         source.
	 * 
	 */

	public static void imageFeatureStackToR(String name, int transferDataType, ImagePlus impPlus) {
		double[] pDouble = null;
		int[] pInt = null;
		byte[] pByte = null;

		int slice = 1;
		ImagePlus imp = null;
		if (RServe.isAlive()) {
			RConnection c = RServe.getConnection();
			/* Get the active image ! */

			if (impPlus == null) {
				imp = WindowManager.getCurrentImage();
			} else {
				imp = impPlus;
			}
			if (imp != null) {
				int size = imp.getStackSize();

				/* Get the image processor of the image ! */
				//ImageProcessor ipImageStack = imp.getProcessor();
				int w = imp.getWidth();
				int h = imp.getHeight();

				try {
					c.eval("imageSizeY<-" + h);

					c.eval("imageSizeX<-" + w);
					//c.eval("imageDataName<-'" + name + "'");
				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					c.eval("try(" + name + "<-NULL)");
				} catch (RserveException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				while (slice <= size) {
					int y = 0;
					int x = 0;
					ImageProcessor ip = imp.getImageStack().getProcessor(slice);
					/* We transfer the values to R! */
					l = "layer" + slice;

					// if (transferDataType == 0 || transferDataType == 3) {

					// } else {
					// createMatrix = false;
					// }

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

						try {
							c.assign(l, pDouble);
						} catch (REngineException e) {
							// TODO Auto-generated catch block
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
							c.assign(l, pInt);
						} catch (REngineException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					/* Byte transfer type! */
					else if (transferDataType == 2) {
						// pByte = (byte[])ip.getPixels();

						/*
						 * The above method does not work and causes an error when a float image is
						 * transfered! This does not happen if every single value is converted to a
						 * byte!
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
							c.assign(l, pByte);
						} catch (REngineException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					try {
						c.eval("try(" + name + "<-cbind(" + name + "," + l + "))");
						c.eval("remove(" + l + ")");
					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					/*
					 * We create a matrix with the width and height from the array or list. Only in
					 * double mode!
					 */

					slice++;
				}

			} else {
				System.out.println("No image available!");
			}
			pDouble = null;
			pInt = null;
			pByte = null;

		}
	}

	/**
	 * This methods transfers image stack data of ROI selections (ROI Manager) to R
	 * by means of Rserve. A feature matrix from the slices will be created and
	 * variables for the width, height and the name of the image ROI data will be
	 * created in the R workspace, too. Signatures can be added, too (from the group number
	 * or extracted from the ROI names number at the end seperated by an underscore.
	 * If the group argument is false and if the name of the ROI has no underscore followed
	 * by a signature number no signatures will be added to the R matrix data!
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
	public static void imageFeatureStackSelectionToR(ImagePlus imp, int transferType, int minStackSlice, int maxStackSlice, boolean signatureFromGroup) {

		if (transferType == 3) {
			System.out.println("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the(slice) selection in e.g. byte mode!");
			return;
		}

		RConnection connection = RServe.getConnection();

		if (RoiManager.getInstance() != null) {
			Roi[] r = RoiManager.getInstance().getSelectedRoisAsArray();
			if (r.length < 1) {
				System.out.println("NO ROI's available in ROI Manager!");
				return;
			}
			if (RServe.isAlive()) {

				if ((imp.getStackSize()) <= 1) {
					System.out.println("Please select a stack with more than one image!");
					return;
				}

				/* Get the image processor of the image ! */
				ImageProcessor ipSize = imp.getProcessor();
				int w = ipSize.getWidth();
				int h = ipSize.getHeight();

				try {
					connection.eval("imageSizeY<-" + h);

					connection.eval("imageSizeX<-" + w);

				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				/*
				 * Only transfer selected ROI's from the ROI Manager or all
				 * if no ROI is selected!
				 */
				//Roi[] r = RoiManager.getInstance().getSelectedRoisAsArray();
				//items = CanvasView.getCanvas_view().tabFolder.getItems();

				/*Initialize all ROI's in R!*/
				for (int ro = 0; ro < r.length; ro++) {

					String roiName = r[ro].getName();
					roiName = correctChars(roiName);
					try {
						connection.eval("try(" + roiName + "<-NULL)");

					} catch (RserveException e2) {
						// TODO Auto-generated catch block
						System.out.println("Please select a proper name for the ROI's in the ROI Manager\n" + "Some special chars are not supported for the transfer to R!");
						return;
					}
				}

				/*Here we iterate through all slices and add the ROI's data (layer) for each slice!*/
				for (int i = minStackSlice; i <= maxStackSlice; i++) {
					//count = i;

					ImageStack stack = imp.getImageStack();
					/* Get the image processor of the image ! */
					ImageProcessor ip = stack.getProcessor(i);
					/*Here we iterate over all ROI's in the ROI Manager!*/
					for (int ro = 0; ro < r.length; ro++) {

						Roi roiDefault = r[ro];
						String roiName = r[ro].getName();
						roiName = correctChars(roiName);
						ip.setRoi(roiDefault);
						/*if (roiDefault != null && !roiDefault.isArea()) {
							Bio7Dialog.message("The command require\n" + "an area selection, or no selection.");
							return;
						}*/

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
							} catch (REngineException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							valuesDouble.clear();
							valD = null;
							try {
								connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
								connection.eval("remove(" + l + ")");
							} catch (RserveException e1) {
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
							} catch (REngineException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							valuesInt.clear();
							valI = null;
							try {
								connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
								connection.eval("remove(" + l + ")");
							} catch (RserveException e1) {
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
							} catch (REngineException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							valuesByte.clear();
							valB = null;
							try {
								connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
								connection.eval("remove(" + l + ")");
							} catch (RserveException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
						/*Here (if enabled in the GUI) we add a class column as the first column in the matrix after the first stack slice for each ROI in R from the ROI Manager!*/
						if (i == minStackSlice) {

							if (transferType == 0) {
								try {

									if (signatureFromGroup) {

										int sigNumber = roiDefault.getGroup();
										if (sigNumber > 0) {
											connection.eval("" + roiName + "<-cbind(rep(c(" + sigNumber + "),length(" + roiName + "[,1]))," + roiName + ")");
											connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
										}
									} else {
										int index = roiName.lastIndexOf("_");
										if (index > -1) {
											String sigNumber = roiName.substring(index + 1, roiName.length());
											if (sigNumber.isEmpty() == false) {
												connection.eval("" + roiName + "<-cbind(rep(c(" + sigNumber + "),length(" + roiName + "[,1]))," + roiName + ")");
												connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
											}
										}
									}

								} catch (RserveException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							} else if (transferType == 1) {
								try {

									if (signatureFromGroup) {
										int sigNumber = roiDefault.getGroup();
										if (sigNumber > 0) {
											connection.eval("" + roiName + "<-cbind(rep(c(as.integer(" + sigNumber + ")),length(" + roiName + "[,1]))," + roiName + ")");
											connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
										}

									} else {

										int index = roiName.lastIndexOf("_");
										if (index > -1) {
											String sigNumber = roiName.substring(index + 1, roiName.length());
											if (sigNumber.isEmpty() == false) {
												connection.eval("" + roiName + "<-cbind(rep(c(as.integer(" + sigNumber + ")),length(" + roiName + "[,1]))," + roiName + ")");
												connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
											}
										}
									}

								} catch (RserveException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							} else if (transferType == 2) {
								try {

									if (signatureFromGroup) {

										int sigNumber = roiDefault.getGroup();
										if (sigNumber > 0) {
											connection.eval("" + roiName + "<-cbind(rep(c(as.raw(" + sigNumber + ")),length(" + roiName + "[,1]))," + roiName + ")");
											connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
										}

									} else {

										int index = roiName.lastIndexOf("_");
										if (index > -1) {
											String sigNumber = roiName.substring(index + 1, roiName.length());
											if (sigNumber.isEmpty() == false) {
												connection.eval("" + roiName + "<-cbind(rep(c(as.raw(" + sigNumber + ")),length(" + roiName + "[,1]))," + roiName + ")");
												connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
											}
										}
									}

								} catch (RserveException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							}

						}

					}

				}

				imp = null;

			} else {

				System.out.println("Rserve is not alive!");
			}
		} else {
			/* If ROI Manager isn't active! */

			System.out.println("Please add selections to the ROI Manager!");

		}

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
