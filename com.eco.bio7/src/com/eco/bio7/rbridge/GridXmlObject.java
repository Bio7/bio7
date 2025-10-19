/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.rbridge;

import java.awt.Color;
import java.util.ArrayList;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * This class is a template for the xml file generated with XStream for the discrete simulation grids of Bio7.
 * @author M. Austenfeld.
 *
 */
public class GridXmlObject {
	
	private String[][] gridValues;
	private GridXMLFontObject fontObject[][];
	private int[][] cellWidth;
	private int[][] cellHeight;
	private Color cellColor[][];
	private Color gridLineColor;

	
	
	public Color getGridLineColor() {
		return gridLineColor;
	}

	public void setGridLineColor(Color gridLineColor) {
		this.gridLineColor = gridLineColor;
	}

	public int[][] getCellWidth() {
		return cellWidth;
	}

	public void setCellWidth(int[][] cellWidth) {
		this.cellWidth = cellWidth;
	}

	public int[][] getCellHeight() {
		return cellHeight;
	}

	public void setCellHeight(int[][] cellHeight) {
		this.cellHeight = cellHeight;
	}

	public Color[][] getCellColor() {
		return cellColor;
	}

	public void setCellColor(Color[][] cellColor) {
		this.cellColor = cellColor;
	}
	
	public String[][] getGridValues() {
		return gridValues;
	}
	
	public void setGridValues(String[][] gridValues) {
		this.gridValues = gridValues;
	}

	public GridXMLFontObject[][] getFontObject() {
		return fontObject;
	}

	public void setFontObject(GridXMLFontObject[][] font) {
		this.fontObject = font;
	}
	

}
