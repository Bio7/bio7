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


package com.eco.bio7.discrete;

import java.awt.Polygon;
import java.awt.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import cern.jet.random.tfloat.FloatUniform;
import cern.jet.random.tfloat.engine.FloatMersenneTwister;
import com.eco.bio7.collection.ResizeArray;
import com.eco.bio7.info.InfoView;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

/**
 * This class provides some static methods for the creation and variation of
 * discrete grid models. The quadgrid and the hexgrid are affected by this
 * methods.
 * 
 * @author Bio7
 * 
 */

public class Field {

	private static int sizey = 50;
	// The y-field size!

	private static int sizex = 50;
	// The x-field size!

	private static int quadsize = 20;
	// the quad size!

	private static int[][] xystate = new int[sizey][sizex];
	// The numeric state array!

	private static int[][] xytempstate = new int[sizey][sizex];
	// The temporary state array!

	private static int random_int;

	private static FloatUniform uni;

	/**
	 * A random function for the discrete grid. Plants and states are dispersed
	 * over the grid.
	 */
	public static void chance() {// Random method!!
		uni = new FloatUniform(0.0f, CurrentStates.getStateList().size() - 1, new FloatMersenneTwister(new java.util.Date()));
		for (int i = 0; i < sizey; i++) {

			for (int u = 0; u < sizex; u++) {

				random_int = uni.nextInt();

				if (random_int > 0) {// Zero is the soil value!

					xystate[i][u] = random_int;
					xytempstate[i][u] = random_int;

				}

				else if (random_int < 1) {
					xystate[i][u] = 0;// Only the soil is set!
					xytempstate[i][u] = 0;

				}

			}
		}

	}

	/**
	 * A method which will set the field values to 0 (xyold,xynew) and to null
	 * (plantold,plantnew)
	 */
	public static void resetField() {
		for (int i = 0; i < Field.sizey; i++) {
			for (int u = 0; u < Field.sizex; u++) {
				xystate[i][u] = 0;
				xytempstate[i][u] = 0;

			}
		}
	}

	/**
	 * A method for adjusting the size of the discrete field.
	 * 
	 * @param x
	 *            the width of the field.
	 * @param y
	 *            the height of the field.
	 */
	public static void setSize(final int x, final int y) {

		ResizeArray.update(y, x);
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				InfoView.getScale_1().setSelection(y);
				InfoView.getLblFieldsizeY().setText("Fieldsize Y -> " + InfoView.getScale_1().getSelection());
				InfoView.getScale_2().setSelection(x);
				InfoView.getLblFieldsizeX().setText("Fieldsize X -> " + InfoView.getScale_2().getSelection());
			}
		});

	}

	/* The modulo operator to remove the border! */

	/**
	 * A method which returns the coordinates of the neighbourhood of the given
	 * distance.
	 * 
	 * @param x
	 *            the x-coordinate.
	 * @param y
	 *            the y-coordinate
	 * @param dx
	 *            the distance from the x-coordinate.
	 * @param dy
	 *            the distance from the y-coordinate.
	 * @return an integer array with the coordinates of the given distance.
	 */
	public static int[] torus(int x, int y, int dx, int dy) {

		int coords[] = new int[2];
		coords[0] = ((y + dy + sizey) % (sizey));
		coords[1] = ((x + dx + sizex) % (sizex));

		return coords;
	}

	/**
	 * A method which calculates the sum of the cells in the neighbourhood n*8.
	 * 
	 * @param n
	 *            an integer value for the neighbourhood.
	 * @param x
	 *            the x-coordinate of the center cell.
	 * @param y
	 *            the y-coordinate of the center cell.
	 * 
	 * @return the sum of the neighbourhood.
	 */
	public static int torusMooreSum(int n, int x, int y) {

		int count = 0;
		for (int v = -n; v <= n; v++) {

			for (int w = -n; w <= n; w++) {
				int mody = ((y + v + sizey) % (sizey));
				int modx = ((x + w + sizex) % (sizex));
				if (v != 0 || w != 0) {
					count = count + xystate[mody][modx];
				}
			}
		}

		return count;
	}

	/**
	 * Copies the values of the temporary plant and temporary state array to the
	 * plant and state array.
	 */
	public static void copyOldNew() {

		for (int i = 0; i < sizey; i++) {
			for (int u = 0; u < sizex; u++) {

				/* Copy the numeric values! */
				xystate[i][u] = xytempstate[i][u];

			}

		}

	}

	/**
	 * Repaints the discrete quad and if available the discrete hexagon panel.
	 */
	public static void doPaint() {
		Quad2d.getQuad2dInstance().repaint();
		Hexagon hex = Hexagon.getHexagonInstance();
		if (hex != null) {
			hex.repaint();

		}

	}

	/**
	 * Method for the access of the quad array.
	 * 
	 * @return the rectangle array of the discrete quad field.
	 * 
	 */
	public static Rectangle[][] getQuads() {

		return Quad2d.getQuad2dInstance().quad;

	}

	/**
	 * Method for the access of the Polygon (Hexagon) array.
	 * 
	 * @return the polygon array of the discrete hexagon field.
	 * 
	 */

	public static Polygon[][] getHexagon() {

		return Hexagon.getHexagonInstance().poly;

	}

	/**
	 * Returns an object for the barrier state with the same attributes as a
	 * plant object.
	 * 
	 * @param x
	 *            the x-coordinate
	 * 
	 * @param y
	 *            the y-coordinate
	 * 
	 * @return a plant object as the soil state.
	 */

	/**
	 * Returns the size of the quads.
	 * 
	 * @return the size of the quads in the quadgrid as an integer value.
	 */
	public static int getQuadSize() {
		return quadsize;
	}

	/**
	 * Sets the size of the quads.
	 * 
	 * @param size
	 *            the size of the quads in the quadgrid as an integer value.
	 */
	public static void setQuadSize(int size) {
		Field.quadsize = size;
	}

	/**
	 * Set the state at the specified x,y coordinates.
	 * 
	 * @param x
	 *            the x-coordinate.
	 * @param y
	 *            the y-coordinate.
	 * @param state
	 *            the integer value of an available state.
	 */
	public static void setState(int x, int y, int state) {
		if (state >= 0 && state < CurrentStates.getStateList().size()) {
			xystate[y][x] = state;
		}

	}

	/**
	 * Set the temporary state at the specified x,y coordinates.
	 * 
	 * @param x
	 *            the x-coordinate.
	 * @param y
	 *            the y-coordinate.
	 * @param state
	 *            the integer value of an available state.
	 */
	public static void setTempState(int x, int y, int state) {
		if (state >= 0 && state < CurrentStates.getStateList().size()) {
			xytempstate[y][x] = state;
		}
	}

	/**
	 * Get the state at the specified x,y coordinates.
	 * 
	 * @param x
	 *            the x-coordinate.
	 * @param y
	 *            the y-coordinate.
	 * @return the integer value of an available state.
	 */
	public static int getState(int x, int y) {
		return xystate[y][x];
	}

	/**
	 * Get the temporary state at the specified x,y coordinates.
	 * 
	 * @param x
	 *            the x-coordinate.
	 * @param y
	 *            the y-coordinate.
	 * @return the integer value of an available state.
	 */
	public static int getTempState(int x, int y) {
		return xytempstate[y][x];
	}

	/**
	 * A method to return the name of the selected state number.
	 * 
	 * @param number
	 *            the state number.
	 * @return the name of the state.
	 */
	public static String getStateName(int number) {
		return CurrentStates.getStateName(number);
	}

	/**
	 * A method which returns a boolean true value if the given state with name
	 * exists at the given coordinates.
	 * 
	 * @param x
	 *            the x-coordinate of the grid.
	 * @param y
	 *            the y-coordinate of the grid.
	 * @param name
	 *            the name of the state.
	 * @return a boolean value.
	 */
	public static boolean isState(int x, int y, String name) {
		boolean stat;
		String state = getStateName(getState(x, y));
		if (name.equals(state)) {
			stat = true;
		} else {
			stat = false;
		}
		return stat;
	}

	/**
	 * A method to return the description of the selected state number.
	 * 
	 * @param number
	 *            the state number.
	 * @return the description of the state.
	 */
	public static String getStateDescription(int number) {
		return CurrentStates.getStateDescription(number);
	}

	/**
	 * Get the width of the field.
	 * 
	 * @return an integer value for the width.
	 */
	public static int getWidth() {
		return sizex;
	}

	/**
	 * Set the width of the field.
	 * 
	 * @param sizex
	 *            an integer value for the width.
	 */
	public static void setWidth(int sizex) {
		Field.sizex = sizex;
	}

	/**
	 * Get the height of the field.
	 * 
	 * @return an integer value for the height.
	 */
	public static int getHeight() {
		return sizey;
	}

	/**
	 * Sets the height of the Field.
	 * 
	 * @param sizey
	 *            an integer value for the height.
	 */
	public static void setHeight(int sizey) {
		Field.sizey = sizey;
	}

	/**
	 * Returns the temporary state array.
	 * 
	 * @return an integer array.
	 */
	public static int[][] getTempArray() {
		return xytempstate;
	}

	/**
	 * Sets the temporary state array.
	 * 
	 * @param xynew
	 *            an integer array.
	 */
	public static void setTempArray(int[][] xynew) {
		Field.xytempstate = xynew;
	}

	/**
	 * Returns the state array.
	 * 
	 * @return an integer array.
	 */
	public static int[][] getStateArray() {
		return xystate;
	}

	/**
	 * Sets the state array.
	 * 
	 * @param xyold
	 *            an integer array.
	 */
	public static void setStateArray(int[][] xyold) {
		Field.xystate = xyold;
	}

	/**
	 * Transfers the Quadgrid pattern to the R workspace!
	 */
	public static void transferPatternToR() {
		if (RServe.isAlive()) {
			if (RState.isBusy() == false) {
				RConnection c = RServe.getConnection();
				int h = Field.getHeight();
				int w = Field.getWidth();
				String name = "quadgrid";

				int[] pInt = new int[w * h];
				int y = 0;
				int x = 0;
				for (int z = 0; z < h * w; z++) {

					if (x > (w - 1)) {
						y++;
						x = 0;
					}
					pInt[z] = Field.getState(x, y);

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

				try {
					c.eval("try(" + name + "<-matrix(" + name + "," + w + "," + h + "))");

				} catch (RserveException e) {

					e.printStackTrace();
				}
				pInt = null;
			} else {
				System.out.println("Rserve is running!");
			}
		} else {
			System.out.println("No Rserve connection available!");
		}
	}

}
