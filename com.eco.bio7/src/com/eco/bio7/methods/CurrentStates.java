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

package com.eco.bio7.methods;

import java.util.ArrayList;
import java.util.HashMap;

public class CurrentStates {

	private static ArrayList<Integer> r = new ArrayList<Integer>();

	private static ArrayList<Integer> g = new ArrayList<Integer>();

	private static ArrayList<Integer> b = new ArrayList<Integer>();

	private static ArrayList<String> stateName = new ArrayList<String>();

	private static ArrayList<String> stateDescriptions = new ArrayList<String>();

	public static void setStateDescriptions(ArrayList<String> stateDescriptions) {
		CurrentStates.stateDescriptions = stateDescriptions;
	}

	private static HashMap<String, Integer> hash = new HashMap<String, Integer>();

	private static String name;

	public static void addState(String stat) {

		stateName.add(new String(stat));

		r.add(Integer.valueOf((int) (Math.random() * 255)));
		g.add(Integer.valueOf((int) (Math.random() * 255)));
		b.add(Integer.valueOf((int) (Math.random() * 255)));

		hash.put(new String(stat), stateName.size() - 1);
	}

	public static void removeState(String species) {

		int occ = getIndexStateName(species); // Get the index of the species!

		stateName.remove(occ); // remove the String!

		r.remove(occ); // remove Colours!
		g.remove(occ);
		b.remove(occ);
		hash.remove(species);

	}

	public static ArrayList<String> getStateDescriptions() {
		return stateDescriptions;
	}

	public static int[] getRGB(int v) {
		Integer R = (Integer) r.get(v);
		int r = R.intValue();

		Integer G = (Integer) g.get(v);
		int g = G.intValue();

		Integer B = (Integer) b.get(v);
		int b = B.intValue();
		int rgb[] = { r, g, b };

		return rgb; // Returns an array with the colour values!!

	}

	public static ArrayList<Integer> getB() {
		return b;
	}

	public static ArrayList<String> getStateList() {
		return stateName;
	}

	public static ArrayList<String> getStateName() {
		return stateName;
	}

	public static void setSpecies_name(ArrayList<String> species_name) {
		CurrentStates.stateName = species_name;
	}

	public static void setR(ArrayList<Integer> r) {
		CurrentStates.r = r;
	}

	public static void setG(ArrayList<Integer> g) {
		CurrentStates.g = g;
	}

	public static void setB(ArrayList<Integer> b) {
		CurrentStates.b = b;
	}

	public static ArrayList<Integer> getG() {
		return g;
	}

	public static ArrayList<Integer> getR() {
		return r;
	}

	public static float[] getRGBGL(int v) {// Conversion for OpenGL!
		Integer R = (Integer) r.get(v);
		float r = (float) (R.intValue()) / 255;

		Integer G = (Integer) g.get(v);
		float g = (float) (G.intValue()) / 255;

		Integer B = (Integer) b.get(v);
		float b = (float) (B.intValue()) / 255;
		float rgbgl[] = { r, g, b };
		return rgbgl; // Returns an array with the colour values!

	}

	public static String getStateName(int b) {
		String be = (String) stateName.get(b);
		return be;
	}

	public static String getStateDescription(int b) {
		String be = (String) stateDescriptions.get(b);
		return be;
	}

	public static int getIndexStateName(String be) {

		int ind = -1;
		String b = be;
		for (int i = 0; i < stateName.size(); i++) {
			name = (String) stateName.get(i);

			if (name.equals(b)) {

				ind = i;

			}

		}

		return ind;

	}

	public static boolean stateExistent(String spec) {
		boolean exist;
		exist = hash.containsKey(spec);
		return exist;
	}

}
