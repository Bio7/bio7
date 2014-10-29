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

public class Measure {
	double AREA,MEAN,STD_DEV,MODE,MIN,MAX,
	CENTROIDX,CENTROIDY,CENTER_OF_MASSX,CENTER_OF_MASSY,PERIMETER,ROIX,ROIY,ROI_WIDTH,ROI_HEIGHT,ELLIPSE_MAJOR,ELLIPSE_MINOR,ELLIPSE_ANGLE,CIRCULARITY, FERET,
	INTEGRATED_DENSITY,RAW_INTEGRATED_DENSITY,MEDIAN,SKEWNESS,KURTOSIS,AREA_FRACTION,SLICE,
	LIMIT,LABELS,INVERT_Y,ASPECT_RATIO,ROUNDNESS,SOLIDITY,FERETX,FERETY,FERET_ANGLE,MIN_FERET,CHANNEL,FRAME,LENGTH,ANGLE;
	

	private String LABEL;
	
	public double getAREA() {
		return AREA;
	}

	public void setAREA(double area) {
		AREA = area;
	}

	public double getAREA_FRACTION() {
		return AREA_FRACTION;
	}

	public void setAREA_FRACTION(double area_fraction) {
		AREA_FRACTION = area_fraction;
	}

	
	public double getCENTER_OF_MASSX() {
		return CENTER_OF_MASSX;
	}

	public void setCENTER_OF_MASSX(double center_of_massx) {
		CENTER_OF_MASSX = center_of_massx;
	}

	public double getCENTER_OF_MASSY() {
		return CENTER_OF_MASSY;
	}

	public void setCENTER_OF_MASSY(double center_of_massy) {
		CENTER_OF_MASSY = center_of_massy;
	}

	public double getCENTROIDX() {
		return CENTROIDX;
	}

	public void setCENTROIDX(double centroidx) {
		CENTROIDX = centroidx;
	}

	public double getCENTROIDY() {
		return CENTROIDY;
	}

	public void setCENTROIDY(double centroidy) {
		CENTROIDY = centroidy;
	}

	public double getROI_HEIGHT() {
		return ROI_HEIGHT;
	}

	public void setROI_HEIGHT(double rect_height) {
		ROI_HEIGHT = rect_height;
	}

	public double getROI_WIDTH() {
		return ROI_WIDTH;
	}

	public void setROI_WIDTH(double rect_width) {
		ROI_WIDTH = rect_width;
	}

	public double getROIX() {
		return ROIX;
	}

	public void setROIX(double rectx) {
		ROIX = rectx;
	}

	public double getROIY() {
		return ROIY;
	}

	public void setROIY(double recty) {
		ROIY = recty;
	}

	public double getCIRCULARITY() {
		return CIRCULARITY;
	}

	public void setCIRCULARITY(double circularity) {
		CIRCULARITY = circularity;
	}

	

	public double getELLIPSE_ANGLE() {
		return ELLIPSE_ANGLE;
	}

	public void setELLIPSE_ANGLE(double ellipse_angle) {
		ELLIPSE_ANGLE = ellipse_angle;
	}

	public double getELLIPSE_MAJOR() {
		return ELLIPSE_MAJOR;
	}

	public void setELLIPSE_MAJOR(double ellipse_major) {
		ELLIPSE_MAJOR = ellipse_major;
	}

	public double getELLIPSE_MINOR() {
		return ELLIPSE_MINOR;
	}

	public void setELLIPSE_MINOR(double ellipse_minor) {
		ELLIPSE_MINOR = ellipse_minor;
	}

	public double getFERET() {
		return FERET;
	}

	public void setFERET(double feret) {
		FERET = feret;
	}

	public double getINTEGRATED_DENSITY() {
		return INTEGRATED_DENSITY;
	}

	public void setINTEGRATED_DENSITY(double integrated_density) {
		INTEGRATED_DENSITY = integrated_density;
	}
	
	public double getRAW_INTEGRATED_DENSITY() {
		return RAW_INTEGRATED_DENSITY;
	}

	public void setRAW_INTEGRATED_DENSITY(double rAW_INTEGRATED_DENSITY) {
		RAW_INTEGRATED_DENSITY = rAW_INTEGRATED_DENSITY;
	}

	public double getINVERT_Y() {
		return INVERT_Y;
	}

	public void setINVERT_Y(double invert_y) {
		INVERT_Y = invert_y;
	}

	public double getKURTOSIS() {
		return KURTOSIS;
	}

	public void setKURTOSIS(double kurtosis) {
		KURTOSIS = kurtosis;
	}

	public double getLABELS() {
		return LABELS;
	}

	public void setLABELS(double labels) {
		LABELS = labels;
	}

	public double getLIMIT() {
		return LIMIT;
	}

	public void setLIMIT(double limit) {
		LIMIT = limit;
	}

	public double getMEAN() {
		return MEAN;
	}

	public void setMEAN(double mean) {
		MEAN = mean;
	}

	public double getMEDIAN() {
		return MEDIAN;
	}

	public void setMEDIAN(double median) {
		MEDIAN = median;
	}

	

	public double getMODE() {
		return MODE;
	}

	public void setMODE(double mode) {
		MODE = mode;
	}

	public double getPERIMETER() {
		return PERIMETER;
	}

	public void setPERIMETER(double perimeter) {
		PERIMETER = perimeter;
	}

	

	public double getSKEWNESS() {
		return SKEWNESS;
	}

	public void setSKEWNESS(double skewness) {
		SKEWNESS = skewness;
	}

	public double getSLICE() {
		return SLICE;
	}

	public void setSLICE(double slice) {
		SLICE = slice;
	}

	public double getSTD_DEV() {
		return STD_DEV;
	}

	public void setSTD_DEV(double std_dev) {
		STD_DEV = std_dev;
	}

	public Measure(){
		
		
	}

	public double getMAX() {
		return MAX;
	}

	public void setMAX(double max) {
		MAX = max;
	}

	public double getMIN() {
		return MIN;
	}

	public void setMIN(double min) {
		MIN = min;
	}

	public void setASPECT_RATIO(double d) {
		ASPECT_RATIO=d;
		
	}
	public double getASPECT_RATIO() {
		return ASPECT_RATIO;
		
	}

	public void setROUNDNESS(double d) {
		ROUNDNESS=d;
		
	}
	public double getROUNDNESS() {
		return ROUNDNESS;
		
	}

	public void setSOLIDITY(double d) {
		SOLIDITY=d;
		
	}
	public double getSOLIDITY() {
		return SOLIDITY;
		
	}

	public void setFERETX(double feretX) {
		FERETX=feretX;
		
	}
	public double getFERETX() {
		return FERETX;
		
	}
	

	public void setFERETY(double feretY) {
	 FERETY=feretY;
		
	}
	public double getFERETY() {
		 return FERETY;
			
		}

	public void setFERET_ANGLE(double feretAngle) {
		FERET_ANGLE=feretAngle;
		
	}
	public double getFERET_ANGLE() {
		 return FERET_ANGLE;
		
	}

	public void setMIN_FERET(double minFeret) {
		MIN_FERET=minFeret;
		
	}
	public double getMIN_FERET() {
		return MIN_FERET;
		
	}

	public void setCHANNEL(int channel) {
		CHANNEL=channel;
		
	}
	public double getCHANNEL() {
		return CHANNEL;
		
	}

	public void setFRAME(int frame) {
		FRAME=frame;
		
	}
	
	public double getFRAME() {
		return FRAME;
		
	}

	public void setLENGTH(double length) {
		LENGTH=length;
		
	}
	
	public double getLENGTH() {
		return LENGTH;
		
	}

	public void setANGLE(double angle) {
		ANGLE=angle;
		
	}
	public double getANGLE() {
		return ANGLE;
		
	}

	public void setLABEL(String fileName) {
		LABEL=fileName;
		
	}
	
	
	public String getLABEL() {
		return LABEL;
		
	}
	

}
