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

package com.eco.bio7.collection;

import java.awt.Polygon;
import java.awt.Rectangle;
import org.eclipse.jface.action.Action;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.rcp.ApplicationActionBarAdvisor;
import com.eco.bio7.time.Time;

public class ResizeArray {

	/*
	 * Arrays are fast, so no Vector are used for a dynamic resizing - instead
	 * the Arrays will be recalculated !
	 */

	public static void update(int y, int x) {

		if (Time.isPause() == false) {
			Time.setPause(true);
			Action ac = ApplicationActionBarAdvisor.getStart();
			ac.setChecked(false);
		}
		resizequad(y, x);
		Quad2d qu = Quad2d.getQuad2dInstance();
		qu.resize_scrollpane_quad2d();

		
		Hexagon hex = Hexagon.getHexagonInstance();
		if (hex != null) {

			resizehexagon(y, x);
			hex.resize_scrollpane_hex2d();
		}

	}

	private static void resizehexagon(int y, int x) {
		Hexagon hex = Hexagon.getHexagonInstance();
		hex.poly = resizequad2d(hex.poly, y, x);

		hex.drawHex();

	}

	private static void resizequad(int y, int x) {
		Quad2d qu = Quad2d.getQuad2dInstance();
		Field.setHeight(y);
		Field.setWidth(x);
		Field.setStateArray(resize2d(Field.getStateArray(), y, x));
		Field.setTempArray(resize2d(Field.getTempArray(), y, x));
		qu.quad = resizequad2d(Quad2d.quad, y, x);
		qu.drawQuad();
		
	}

	private static Object resizeArray(Object oldArray, int newSize) {

		int oldSize = java.lang.reflect.Array.getLength(oldArray);
		Class elementType = oldArray.getClass().getComponentType();
		Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
		int preserveLength = Math.min(oldSize, newSize);
		if (preserveLength > 0) {

			System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
		}
		return newArray;
	}

	private static int[][] resize2d(int[][] thearray, int feldgroessey, int feldgroessex) {

		thearray = (int[][]) resizeArray(thearray, feldgroessey);

		for (int i = 0; i < thearray.length; i++) {

			if (thearray[i] == null) {
				thearray[i] = new int[feldgroessex];
			}

			else {

				thearray[i] = (int[]) resizeArray(thearray[i], feldgroessex);

			}
		}

		return thearray;
	}

	private static Rectangle[][] resizequad2d(Rectangle[][] thearray, int feldgroessey, int feldgroessex) {

		thearray = (Rectangle[][]) resizeArray(thearray, feldgroessey);

		for (int i = 0; i < thearray.length; i++) {

			if (thearray[i] == null) {
				thearray[i] = new Rectangle[feldgroessex];
			}

			else {

				thearray[i] = (Rectangle[]) resizeArray(thearray[i], feldgroessex);

			}
		}

		return thearray;
	}

	private static Polygon[][] resizequad2d(Polygon[][] thearray, int feldgroessey, int feldgroessex) {

		thearray = (Polygon[][]) resizeArray(thearray, feldgroessey);

		for (int i = 0; i < thearray.length; i++) {

			if (thearray[i] == null) {
				thearray[i] = new Polygon[feldgroessex];
			}

			else {

				thearray[i] = (Polygon[]) resizeArray(thearray[i], feldgroessex);

			}
		}

		return thearray;
	}
}
