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

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import static com.eco.bio7.discrete.Field.*;
import com.eco.bio7.methods.CurrentStates;
//import com.eco.bio7.methods.State;

/**
 * This class implements a few common landscape metrics for the use in spatially
 * explicit simulations. These metrics are a java subset implementation of the
 * Fragstat algorithms.
 * 
 * @author Bio7
 * 
 * 
 * 
 */
public class LandscapeMetrics {

	private ArrayList<Patches> list_states;

	private LinkedList<Point> q;

	private int[][] temp = new int[getHeight()][getWidth()];;

	public LandscapeMetrics() {

		list_states = new ArrayList<Patches>();
		q = new LinkedList<Point>();

	}

	/**
	 * Resets the landscape measurements.
	 */
	public void reset() {
		list_states.clear();
		q.clear();

		for (int y = 0; y < temp.length; y++) {
			for (int x = 0; x < temp[0].length; x++) {
				temp[y][x] = getState(x, y);

			}
		}

	}

	/**
	 * Returns the total amount of individual cells of the given type.
	 * 
	 * @param type
	 *            state (type) to be counted.
	 * @return the amount as an integer value.
	 */
	public int getTotalAmount(int type) {
		int count = 0;
		for (int y = 0; y < Field.getHeight(); y++) {
			for (int x = 0; x < Field.getWidth(); x++) {

				if (getState(x, y) == type) {

					count++;

				}

			}
		}
		return count;

	}

	/**
	 * Returns the total area of the field(number of cells).
	 * 
	 * @return the area as an integer value.
	 */
	public int getTotalArea() {
		int area = 0;
		area = getWidth() * getHeight();
		return area;
	}

	/**
	 * Returns the percentage of the field of the given type(percent of
	 * Landscape).
	 * 
	 * @param type
	 *            the state(type).
	 * @return the percentage as a double value.
	 */
	public double getPercentofField(int type) {
		double percent = 0;
		double amount = getTotalAmount(type);
		double area = getTotalArea();
		percent = (amount / area) * 100;

		return percent;

	}

	/**
	 * Returns the amount of patches of type i
	 * 
	 * @param type
	 *            the type(state).
	 * @return the amount as an integer value.
	 */
	public int getAmountPatchType(int type) {
		Patches patches = list_states.get(type);
		int amount = patches.get_amount();
		return amount;
	}

	/**
	 * Returns the sizes of each patch j of type i (pij) available in the
	 * discrete grid.
	 * 
	 * @param type
	 *            the type(state).
	 * 
	 * @return an integer array with the patch size.
	 */
	public int[] getPatchesSize(int type) {
		Patches patches = list_states.get(type);
		int[] sizes = patches.get_patches_size();
		return sizes;
	}

	/**
	 * Returns the patch density of type (state) n.
	 * 
	 * @param type
	 *            the type(state).
	 * @return a double value of the patch density.
	 */
	public double getPatchDensity(int type) {
		double density;
		double amount = getAmountPatchType(type);
		double area = getTotalArea();
		density = amount / area;
		return density;
	}

	/**
	 * Returns the average patch perimeter-area ratio of the given type(state).
	 * 
	 * @param type
	 *            the type(state).
	 * @return a double value of the ratio.
	 */
	public double getAveragePerimeterAreaRatio(int type) {
		double ratio;
		int[] psize = getPatchesSize(type);
		double[] perimeter = getPatchesPerimeter(type);
		double sum = 0;
		double temp;
		double size = psize.length;
		for (int i = 0; i < psize.length; i++) {
			temp = psize[i];
			sum = sum + perimeter[i] / temp;

		}
		ratio = sum / size;

		return ratio;
	}

	/**
	 * 
	 * Returns the patch perimeter-area ratio of the given type(state).
	 * 
	 * @param type
	 *            the type(state).
	 * @return a double array with the ratios.
	 */
	public double[] getPerimeterAreaRatio(int type) {

		int[] psize = getPatchesSize(type);
		double[] perimeter = getPatchesPerimeter(type);
		double[] ratio = new double[psize.length];
		double temp;
		for (int i = 0; i < psize.length; i++) {
			temp = psize[i];
			ratio[i] = perimeter[i] / temp;

		}

		return ratio;
	}

	/**
	 * Returns the fractal dimension of each patch j of type i (pij).
	 * 
	 * @param type
	 *            the type(state).
	 * 
	 * @return a double array with the fractal dimensions.
	 */
	public double[] getFractalDimension(int type) {
		double frac[] = null;
		Patches patches = list_states.get(type);
		frac = new double[patches.get_amount()];
		int[] sizes = patches.get_patches_size();
		double[] peri = getPatchesPerimeter(type);

		for (int i = 0; i < frac.length; i++) {
			frac[i] = 2 * Math.log(0.25 * peri[i]) / Math.log(sizes[i]);
			if (sizes[i] == 1 || frac[i] < 1) {
				frac[i] = 1;
			}

		}

		return frac;
	}

	/**
	 * 
	 * Returns the distances to the nearest neighbour of each patch. It uses a
	 * very slow algorithm and shouldn't be used in a random field >300*300 !
	 * The function will return an array of size 1 with value 0 if the field
	 * consists only of one patch of the given type.
	 * 
	 * @param type
	 *            the type(state).
	 * @return a double array with the distances.
	 */
	public double[] getPatchesDistance(int type) {

		int min;
		int distanceto;

		Patches patches = list_states.get(type);
		ArrayList patcheslist = patches.get_patchlist();

		double[] dist = new double[patcheslist.size()];
		Patch p1;
		Patch p2;
		if (patcheslist.size() > 1) {
			for (int j = 0; j < patcheslist.size(); j++) {
				p1 = (Patch) patcheslist.get(j);
				min = 1000000000;

				for (int k = 0; k < patcheslist.size(); k++) {
					p2 = (Patch) patcheslist.get(k);
					if (j != k) {

						distanceto = distance(p1, p2);
						if (distanceto < min) {
							min = distanceto;
						}

					}

				}

				dist[j] = (Math.sqrt(min));

			}
		} else if (patcheslist.size() == 1) {

			dist[0] = 0;

		}

		return dist;
	}

	private int distance(Patch pj, Patch pk) {
		int dist = -1;
		int min = 1000000000;
		int ydiff;
		int xdiff;
		Point p;
		Point p2;
		for (int i = 0; i < pj.getSize(); i++) {
			p = pj.getPoint(i);
			for (int j = 0; j < pk.getSize(); j++) {
				p2 = pk.getPoint(j);
				ydiff = p.y - p2.y;
				xdiff = p.x - p2.x;
				dist = xdiff * xdiff + ydiff * ydiff;
				min = min < dist ? min : dist;

			}

		}

		return min;
	}

	/**
	 * Returns the perimeter of all patches of type n.
	 * 
	 * @param type
	 *            the type(state).
	 * @return a double array with the perimeters.
	 */
	public double[] getPatchesPerimeter(int type) {
		Patches patches = list_states.get(type);
		double[] peri = patches.get_patch_perimeter(type);
		return peri;
	}

	/**
	 * Returns the amount of edges from type n to type n with the neighbourhood
	 * rule.
	 * 
	 * @param type
	 *            the type(state).
	 * @param toType
	 *            the type(state).
	 * @param n
	 *            an integer value for the neighbourhood rule.
	 * @return a double array with the amounts.
	 */
	public double[] getEdge(int type, int toType, int n) {
		Patches patches = list_states.get(type);
		double[] peri = patches.get_edge(type, toType, n);
		return peri;
	}

	/**
	 * This is the main method to calculate Landscape Metrics. This method
	 * expects the rule of neighbourhood (4,8,12).
	 * 
	 * @param neighbourhood
	 *            an integer value for the neighbourhood rule.
	 */
	public void doPatchStatistics(int neighbourhood) {
		int thestate;
		for (int i = 0; i < CurrentStates.getStateList().size(); i++) {

			list_states.add(new Patches());

		}

		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				thestate = Field.getState(x, y);
				/* We do the statistics for all states which available ! */
				if (thestate == temp[y][x]) {

					floodFill(x, y, thestate, neighbourhood);
				}
			}

		}

	}

	/* An java implementation of the floodfill algorithm ! */
	private void floodFill(int x, int y, int state, int neighbourhood) {

		Patch p = new Patch();

		q.addFirst(new Point(x, y));
		while (!q.isEmpty()) {

			Point n = q.removeLast();

			if ((n.x >= 0) && (n.x < getWidth()) && (n.y >= 0) && (n.y < getHeight()) && (temp[n.y][n.x] == state)) {

				/*
				 * mark the temp array with a number which is not a state for
				 * the visited cells !
				 */
				temp[n.y][n.x] = -1;
				/* add one point to to the Patch list */

				p.setPoint(n.y, n.x);

				// patch.setPoint(n.y, n.x);
				/* add the neighbours to the list ! */
				if (neighbourhood == 4) {
					q.addFirst(new Point(n.x + 1, n.y));
					q.addFirst(new Point(n.x, n.y + 1));
					q.addFirst(new Point(n.x, n.y - 1));
					q.addFirst(new Point(n.x - 1, n.y));
				}

				else if (neighbourhood == 8) {

					q.addFirst(new Point(n.x + 1, n.y));
					q.addFirst(new Point(n.x, n.y + 1));
					q.addFirst(new Point(n.x, n.y - 1));
					q.addFirst(new Point(n.x - 1, n.y));

					q.addFirst(new Point(n.x - 1, n.y - 1));
					q.addFirst(new Point(n.x - 1, n.y + 1));
					q.addFirst(new Point(n.x + 1, n.y - 1));
					q.addFirst(new Point(n.x + 1, n.y + 1));

				}

				else if (neighbourhood == 12) {
					q.addFirst(new Point(n.x + 1, n.y));
					q.addFirst(new Point(n.x, n.y + 1));
					q.addFirst(new Point(n.x, n.y - 1));
					q.addFirst(new Point(n.x - 1, n.y));

					q.addFirst(new Point(n.x - 1, n.y - 1));
					q.addFirst(new Point(n.x - 1, n.y + 1));
					q.addFirst(new Point(n.x + 1, n.y - 1));
					q.addFirst(new Point(n.x + 1, n.y + 1));

					q.addFirst(new Point(n.x, n.y - 2));
					q.addFirst(new Point(n.x, n.y + 2));
					q.addFirst(new Point(n.x - 2, n.y));
					q.addFirst(new Point(n.x + 2, n.y));
				}

			}
		}

		Patches patches = list_states.get(state);
		patches.set_patch(p);

		q.clear();// reset the list !
		// patch.delete();
	}

	class Patch {
		ArrayList<Point> patch = new ArrayList<Point>();

		public Point getPoint(int number) {
			Point p = patch.get(number);
			return p;
		}

		public void setPoint(int y, int x) {
			patch.add(new Point(x, y));
		}

		public int getSize() {

			return patch.size();
		}

		public void delete() {
			patch.clear();
		}

	}

	class Patches {

		ArrayList<Patch> patches = new ArrayList<Patch>();

		private Patch[] get_all_patches() {
			Patch[] p = new Patch[patches.size()];
			for (int i = 0; i < patches.size(); i++) {
				p[i] = patches.get(i);

			}
			return p;
		}

		private ArrayList get_patchlist() {

			return patches;
		}

		private void set_patch(Patch patch) {

			patches.add(patch);

		}

		private int[] get_patches_size() {
			int[] sizes = new int[patches.size()];

			for (int i = 0; i < patches.size(); i++) {

				Patch p = patches.get(i);
				sizes[i] = p.getSize();

			}
			return sizes;
		}

		private int get_amount() {

			return patches.size();

		}

		private void delete() {
			patches.clear();
		}

		private double[] get_patch_perimeter(int state) {

			double[] peri = new double[patches.size()];
			int[] yy = { 0, 1, -1, 0 };
			int[] xx = { 1, 0, 0, -1 };
			int y;
			int x;
			int count = 0;
			Patch p;
			for (int i = 0; i < patches.size(); i++) {

				p = patches.get(i);

				for (int j = 0; j < p.getSize(); j++) {

					Point n = p.getPoint(j);
					y = n.y;
					x = n.x;

					for (int k = 0; k < 4; k++) {
						if (y + yy[k] >= 0 && y + yy[k] < getHeight() && x + xx[k] >= 0 && x + xx[k] < getWidth()) {
							if (getState(x + xx[k], y + yy[k]) != state) {
								count++;

							}
						} else {
							count++;
						}
					}

				}
				peri[i] = count;
				count = 0;
			}

			return peri;

		}

		private double[] get_edge(int state, int tostate, int rule) {
			int nrule;
			if (rule == 4) {
				nrule = 4;
			} else {
				nrule = 8;
			}

			double[] peri = new double[patches.size()];

			int[] yy = { 0, 1, -1, 0 };
			int[] xx = { 1, 0, 0, -1 };

			int[] yy8 = { 0, 1, -1, 0, -1, 1, -1, 1 };
			int[] xx8 = { 1, 0, 0, -1, -1, -1, 1, 1 };

			int y;
			int x;
			int count = 0;
			Patch p;
			for (int i = 0; i < patches.size(); i++) {

				p = patches.get(i);

				for (int j = 0; j < p.getSize(); j++) {

					Point n = p.getPoint(j);
					y = n.y;
					x = n.x;

					for (int k = 0; k < nrule; k++) {
						if (nrule == 4) {

							if (y + yy[k] >= 0 && y + yy[k] < getHeight() && x + xx[k] >= 0 && x + xx[k] < getWidth()) {

								if (getState(x + xx[k], y + yy[k]) == tostate) {
									count++;

								}

							} else {
								count++;
							}
						}

						else {
							if (y + yy8[k] >= 0 && y + yy8[k] < Field.getHeight() && x + xx8[k] >= 0 && x + xx8[k] < Field.getWidth()) {

								if (Field.getState(x + xx8[k], y + yy8[k]) == tostate) {
									count++;

								}

							} else {
								count++;
							}

						}
					}

				}
				peri[i] = count;
				count = 0;
			}

			return peri;

		}

	}

}
