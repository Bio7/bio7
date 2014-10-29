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

package com.eco.bio7.image.measure;

import java.util.ArrayList;

/**
 * This class provides static methods for the access of values as an array from
 * a particle analysis of ImageJ. This class supports the transfer to the R
 * application by means of the Rserve application.
 * 
 * @author Bio7
 * 
 */
public class Bio7ImageJAnalyse {

	public static ArrayList measurement = new ArrayList();

	private static double[] AREA, MEAN, STD_DEV, MODE, MIN, MAX, CENTROIDX, CENTROIDY, CENTER_OF_MASSX, CENTER_OF_MASSY, PERIMETER, ROIX, ROIY, ROI_WIDTH, ROI_HEIGHT, ELLIPSE_MAJOR, ELLIPSE_MINOR,
			ELLIPSE_ANGLE, CIRCULARITY, FERET, INTEGRATED_DENSITY, RAW_INTEGRATED_DENSITY, MEDIAN, SKEWNESS, KURTOSIS, AREA_FRACTION, SLICE, LIMIT, LABELS, INVERT_Y, ASPECT_RATIO, ROUNDNESS,
			SOLIDITY, FERETX, FERETY, FERET_ANGLE, MIN_FERET, CHANNEL, FRAME, LENGTH, ANGLE;

	private static String[] LABEL;

	/**
	 * Clears the measurements of a particle analysis.
	 */
	public static void clearList() {

		measurement.clear();
		AREA = null;
		MEAN = null;
		STD_DEV = null;
		MODE = null;
		MIN = null;
		MAX = null;
		CENTROIDX = null;
		CENTROIDY = null;
		CENTER_OF_MASSX = null;
		CENTER_OF_MASSY = null;
		PERIMETER = null;
		ROIX = null;
		ROIY = null;
		ROI_WIDTH = null;
		ROI_HEIGHT = null;
		ELLIPSE_MAJOR = null;
		ELLIPSE_MINOR = null;
		ELLIPSE_ANGLE = null;
		CIRCULARITY = null;
		FERET = null;
		INTEGRATED_DENSITY = null;
		RAW_INTEGRATED_DENSITY = null;
		MEDIAN = null;
		SKEWNESS = null;
		KURTOSIS = null;
		AREA_FRACTION = null;
		SLICE = null;
		LIMIT = null;
		LABELS = null;
		INVERT_Y = null;
		ASPECT_RATIO = null;
		ROUNDNESS = null;
		SOLIDITY = null;
		FERETX = null;
		FERETY = null;
		FERET_ANGLE = null;
		MIN_FERET = null;
		CHANNEL = null;
		FRAME = null;
		LENGTH = null;
		ANGLE = null;
		LABEL = null;
	}

	/**
	 * Fills arrays with the values from a particle analysis.
	 */
	public static void fillArrays() {
		int size = measurement.size();
		AREA = new double[size];
		MEAN = new double[size];
		STD_DEV = new double[size];
		MODE = new double[size];
		MIN = new double[size];
		MAX = new double[size];
		CENTROIDX = new double[size];
		CENTROIDY = new double[size];
		CENTER_OF_MASSX = new double[size];
		CENTER_OF_MASSY = new double[size];
		PERIMETER = new double[size];
		ROIX = new double[size];
		ROIY = new double[size];
		ROI_WIDTH = new double[size];
		ROI_HEIGHT = new double[size];
		ELLIPSE_MAJOR = new double[size];
		ELLIPSE_MINOR = new double[size];
		ELLIPSE_ANGLE = new double[size];
		CIRCULARITY = new double[size];
		FERET = new double[size];
		INTEGRATED_DENSITY = new double[size];
		RAW_INTEGRATED_DENSITY = new double[size];
		MEDIAN = new double[size];
		SKEWNESS = new double[size];
		KURTOSIS = new double[size];
		AREA_FRACTION = new double[size];
		SLICE = new double[size];

		LIMIT = new double[size];
		LABELS = new double[size];
		INVERT_Y = new double[size];

		ASPECT_RATIO = new double[size];
		ROUNDNESS = new double[size];
		SOLIDITY = new double[size];
		FERETX = new double[size];
		FERETY = new double[size];
		FERET_ANGLE = new double[size];
		MIN_FERET = new double[size];
		CHANNEL = new double[size];
		FRAME = new double[size];
		LENGTH = new double[size];
		ANGLE = new double[size];
		LABEL = new String[size];

		for (int i = 0; i < size; i++) {
			Measure measure = (Measure) measurement.get(i);

			if (i < AREA.length) {
				AREA[i] = measure.getAREA();
			}
			if (i < MEAN.length) {
				MEAN[i] = measure.getMEAN();
			}
			if (i < STD_DEV.length) {
				STD_DEV[i] = measure.getSTD_DEV();
			}
			if (i < MODE.length) {
				MODE[i] = measure.getMODE();
			}
			if (i < MIN.length) {
				MIN[i] = measure.getMIN();
			}
			if (i < MAX.length) {
				MAX[i] = measure.getMAX();
			}
			if (i < CENTROIDX.length) {
				CENTROIDX[i] = measure.getCENTROIDX();
			}
			if (i < CENTROIDY.length) {
				CENTROIDY[i] = measure.getCENTROIDY();
			}

			if (i < CENTER_OF_MASSX.length) {
				CENTER_OF_MASSX[i] = measure.getCENTER_OF_MASSX();
			}
			if (i < CENTER_OF_MASSY.length) {
				CENTER_OF_MASSY[i] = measure.getCENTER_OF_MASSY();
			}

			if (i < PERIMETER.length) {
				PERIMETER[i] = measure.getPERIMETER();
			}

			if (i < ROIX.length) {
				ROIX[i] = measure.getROIX();
			}
			if (i < ROIY.length) {
				ROIY[i] = measure.getROIY();
			}
			if (i < ROI_WIDTH.length) {
				ROI_WIDTH[i] = measure.getROI_WIDTH();
			}
			if (i < ROI_HEIGHT.length) {
				ROI_HEIGHT[i] = measure.getROI_HEIGHT();
			}

			if (i < ELLIPSE_MAJOR.length) {
				ELLIPSE_MAJOR[i] = measure.getELLIPSE_MAJOR();
			}
			if (i < ELLIPSE_MINOR.length) {
				ELLIPSE_MINOR[i] = measure.getELLIPSE_MINOR();
			}
			if (i < ELLIPSE_ANGLE.length) {
				ELLIPSE_ANGLE[i] = measure.getELLIPSE_ANGLE();
			}

			if (i < CIRCULARITY.length) {
				CIRCULARITY[i] = measure.getCIRCULARITY();
			}
			if (i < FERET.length) {
				FERET[i] = measure.getFERET();
			}

			if (i < INTEGRATED_DENSITY.length) {
				INTEGRATED_DENSITY[i] = measure.getINTEGRATED_DENSITY();
			}
			if (i < RAW_INTEGRATED_DENSITY.length) {
				RAW_INTEGRATED_DENSITY[i] = measure.getRAW_INTEGRATED_DENSITY();
			}
			if (i < MEDIAN.length) {
				MEDIAN[i] = measure.getMEDIAN();
			}
			if (i < SKEWNESS.length) {
				SKEWNESS[i] = measure.getSKEWNESS();
			}
			if (i < KURTOSIS.length) {
				KURTOSIS[i] = measure.getKURTOSIS();
			}
			if (i < AREA_FRACTION.length) {
				AREA_FRACTION[i] = measure.getAREA_FRACTION();
			}
			if (i < SLICE.length) {
				SLICE[i] = measure.getSLICE();
			}

			if (i < LIMIT.length) {
				LIMIT[i] = measure.getLIMIT();
			}
			if (i < LABELS.length) {
				LABELS[i] = measure.getLABELS();
			}
			if (i < INVERT_Y.length) {
				INVERT_Y[i] = measure.getINVERT_Y();
			}
			if (i < ASPECT_RATIO.length) {
				ASPECT_RATIO[i] = measure.getASPECT_RATIO();
			}
			if (i < ROUNDNESS.length) {
				ROUNDNESS[i] = measure.getROUNDNESS();
			}

			if (i < SOLIDITY.length) {
				SOLIDITY[i] = measure.getSOLIDITY();
			}
			if (i < FERETX.length) {

				FERETX[i] = measure.getFERETX();
			}
			if (i < FERETY.length) {
				FERETY[i] = measure.getFERETY();
			}
			if (i < FERET_ANGLE.length) {
				FERET_ANGLE[i] = measure.getFERET_ANGLE();
			}
			if (i < MIN_FERET.length) {
				MIN_FERET[i] = measure.getMIN_FERET();
			}
			if (i < CHANNEL.length) {
				CHANNEL[i] = measure.getCHANNEL();
			}
			if (i < FRAME.length) {
				FRAME[i] = measure.getFRAME();
			}
			if (i < LENGTH.length) {
				LENGTH[i] = measure.getLENGTH();
			}
			if (i < ANGLE.length) {
				ANGLE[i] = measure.getANGLE();
			}
			if (i < LABEL.length) {
				LABEL[i] = measure.getLABEL();
			}

		}
	}

	/**
	 * Returns the area values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getAREA() {
		return AREA;
	}

	/**
	 * Returns the area fraction values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getAREA_FRACTION() {
		return AREA_FRACTION;
	}

	/**
	 * Returns the center of mass x-coordinate values of the ImageJ measurement
	 * as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getCENTER_OF_MASSX() {
		return CENTER_OF_MASSX;
	}

	/**
	 * Returns the center of mass y-coordinate values of the ImageJ measurement
	 * as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getCENTER_OF_MASSY() {
		return CENTER_OF_MASSY;
	}

	/**
	 * Returns the centroid x-coordinate values of the ImageJ measurement as an
	 * array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getCENTROIDX() {
		return CENTROIDX;
	}

	/**
	 * Returns the centroid y-coordinate values of the ImageJ measurement as an
	 * array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getCENTROIDY() {
		return CENTROIDY;
	}

	/**
	 * Returns the circularity values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getCIRCULARITY() {
		return CIRCULARITY;
	}

	/**
	 * Returns the ellipse angle values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getELLIPSE_ANGLE() {
		return ELLIPSE_ANGLE;
	}

	/**
	 * Returns the ellipse major values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getELLIPSE_MAJOR() {
		return ELLIPSE_MAJOR;
	}

	/**
	 * Returns the ellipse minor values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getELLIPSE_MINOR() {
		return ELLIPSE_MINOR;
	}

	/**
	 * Returns ferret's diameter values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getFERET() {
		return FERET;
	}

	/**
	 * Returns the integrated density values of the ImageJ measurement as an
	 * array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getINTEGRATED_DENSITY() {
		return INTEGRATED_DENSITY;
	}

	/**
	 * Returns the raw integrated density values of the ImageJ measurement as an
	 * array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getRAW_INTEGRATED_DENSITY() {
		return INTEGRATED_DENSITY;
	}

	/**
	 * Returns the inverted y values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getINVERT_Y() {
		return INVERT_Y;
	}

	/**
	 * Returns the kurtosis values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getKURTOSIS() {
		return KURTOSIS;
	}

	/**
	 * Returns the labels of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getLABELS() {
		return LABELS;
	}

	/**
	 * Returns the limits of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getLIMIT() {
		return LIMIT;
	}

	/**
	 * Returns the max values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getMAX() {
		return MAX;
	}

	/**
	 * Returns the mean values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getMEAN() {
		return MEAN;
	}

	/**
	 * Returns the list of the measurements.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static ArrayList getMeasurement() {
		return measurement;
	}

	/**
	 * Returns the median values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getMEDIAN() {
		return MEDIAN;
	}

	/**
	 * Returns the min values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getMIN() {
		return MIN;
	}

	/**
	 * Returns the mode values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getMODE() {
		return MODE;
	}

	/**
	 * Returns the perimeter values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getPERIMETER() {
		return PERIMETER;
	}

	/**
	 * Returns the bounding rectangle height values of the ImageJ measurement as
	 * an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getROI_HEIGHT() {
		return ROI_HEIGHT;
	}

	/**
	 * Returns the bounding rectangle width values of the ImageJ measurement as
	 * an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getROI_WIDTH() {
		return ROI_WIDTH;
	}

	/**
	 * Returns the bounding rectangle x-coordinate values of the ImageJ
	 * measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getROIX() {
		return ROIX;
	}

	/**
	 * Returns the bounding rectangle y-coordinate values of the ImageJ
	 * measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getROIY() {
		return ROIY;
	}

	/**
	 * Returns the skewness values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getSKEWNESS() {
		return SKEWNESS;
	}

	/**
	 * Returns the slice values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getSLICE() {
		return SLICE;
	}

	/**
	 * Returns the standard deviation values of the ImageJ measurement as an
	 * array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getSTD_DEV() {
		return STD_DEV;
	}

	/**
	 * Returns the aspect ratio values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */

	public static double[] getASPECT_RATIO() {
		return ASPECT_RATIO;
	}

	/**
	 * Returns the roundness values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getROUNDNESS() {
		return ROUNDNESS;
	}

	/**
	 * Returns the solidity values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getSOLIDITY() {
		return SOLIDITY;
	}

	/**
	 * Returns the feret x values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getFERETX() {
		return FERETX;
	}

	/**
	 * Returns the feret y values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getFERETY() {
		return FERETY;
	}

	/**
	 * Returns the feret angle values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getFERET_ANGLE() {
		return FERET_ANGLE;
	}

	/**
	 * Returns the feret minimum values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getMIN_FERET() {
		return MIN_FERET;
	}

	/**
	 * Returns the channel value of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getCHANNEL() {
		return CHANNEL;
	}

	/**
	 * Returns the frame values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getFRAME() {
		return FRAME;
	}

	/**
	 * Returns the length values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getLENGTH() {
		return LENGTH;
	}

	/**
	 * Returns the angle values of the ImageJ measurement as an array.
	 * 
	 * @return a double array of the measured feature.
	 */
	public static double[] getANGLE() {
		return ANGLE;
	}

	/**
	 * Returns the label values of the ImageJ measurement as an array.
	 * 
	 * @return a String array of the measured feature.
	 */
	public static String[] getLABEL() {
		return LABEL;
	}

}
