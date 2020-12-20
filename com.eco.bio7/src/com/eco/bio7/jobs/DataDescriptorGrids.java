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


package com.eco.bio7.jobs;

import java.util.ArrayList;

/**
 * This class is a template for the xml file generated with XStream for the discrete simulation grids of Bio7.
 * @author M. Austenfeld.
 *
 */
public class DataDescriptorGrids {
	private int fieldSizeX;
	private int fieldSizeY;
	private ArrayList<String> stateNames;
	private ArrayList<Integer> colorR;
	private ArrayList<Integer> colorG;
	private ArrayList<Integer> colorB;
	private ArrayList<String> descriptions;
	private int[][] states;
	private String source;
	private boolean fullClass;
	private String fileName;

	/**
	 * @return the width
	 */
	public int getFieldSizeX() {
		return fieldSizeX;
	}

	/**
	 * @param fieldSizeX the height
	 */
	public void setFieldSizeX(int fieldSizeX) {
		this.fieldSizeX = fieldSizeX;
	}

	/**
	 * @return the height
	 */
	public int getFieldSizeY() {
		return fieldSizeY;
	}

	/**
	 * @param fieldSizeY the height
	 */
	public void setFieldSizeY(int fieldSizeY) {
		this.fieldSizeY = fieldSizeY;
	}

	/**
	 * @return the names of the states
	 */
	public ArrayList<String> getStateNames() {
		return stateNames;
	}

	/**
	 * @param stateNames the names of the states
	 */
	public void setStateNames(ArrayList<String> stateNames) {
		this.stateNames = stateNames;
	}

	/**
	 * @return the red component of the color
	 */
	public ArrayList<Integer> getColorR() {
		return colorR;
	}

	/**
	 * @param colorR the red component of the color as an integer
	 */
	public void setColorR(ArrayList<Integer> colorR) {
		this.colorR = colorR;
	}

	/**
	 * @return the green component of the color
	 */
	public ArrayList<Integer> getColorG() {
		return colorG;
	}

	/**
	 * @param colorG the green component of the color as an integer
	 */
	public void setColorG(ArrayList<Integer> colorG) {
		this.colorG = colorG;
	}

	/**
	 * @return the blue component of the color
	 */
	public ArrayList<Integer> getColorB() {
		return colorB;
	}

	/**
	 * @param colorB the blue component of the color as an integer
	 */
	public void setColorB(ArrayList<Integer> colorB) {
		this.colorB = colorB;
	}

	/**
	 * @return the descriptions as a string
	 */
	public ArrayList<String> getDescriptions() {
		return descriptions;
	}

	/**
	 * @param descriptions the descriptions as a string
	 */
	public void setDescriptions(ArrayList<String> descriptions) {
		this.descriptions = descriptions;
	}

	/**
	 * @return the state values as an integer array (2d)
	 */
	public int[][] getStates() {
		return states;
	}

	/**
	 * @param states a 2d integer array with the states.
	 */
	public void setStates(int[][] states) {
		this.states = states;
	}
	
	/**
	 * @param set the source for automatic compilation!.
	 */
	public void setSource(String source) {
		this.source = source;
	}
	/**
	 * @return the state values as an integer array (2d)
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @return a boolean value if a full class was stored.
	 */
	public boolean isClassBody() {
		return fullClass;
	}

	/**
	 * @param fullClass sets a boolean to indicate if the source is a full class.
	 */
	public void setClassBody(boolean fullClass) {
		this.fullClass = fullClass;
	}

	/**
	 * @return the name of the stored file for a full Java class.
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName sets the file name for the full Java class.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	

}
