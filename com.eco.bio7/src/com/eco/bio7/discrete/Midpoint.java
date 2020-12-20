/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andreas Ermler for Bio7 as a University project!
 *******************************************************************************/

package com.eco.bio7.discrete;

import java.util.Arrays;
import java.util.Random;

class Midpoint {

	private double sigma, h, delta;

	private boolean addition;

	private int maxlevel, n, dim, seed, dd, d, x, y, stage;

	private double[][] dField;

	private int[][] iField, discIField;

	private double[] oneDArray, borders;

	private Random r;

	Midpoint() {
		maxlevel = 8; // maximal number of recursions (n = 2 ^ maxlevel)
		sigma = 1.0; // initial standard deviation
		h = 0.8; // parameter that determines fractal dimension (dd = 3 - h)
		addition = true; // turns on/off random additions
		seed = -3333; // seed value for random number generator

		update();
	}

	Midpoint(int maxlevel, double sigma, double h, boolean addition, int seed) {
		this.maxlevel = maxlevel; // maximal number of recursions (n = 2 ^ maxlevel)
		this.sigma = sigma; // initial standard deviation
		this.h = h; // parameter that determines fractal dimension (dd = 3 - h)
		this.addition = addition; // turns on/off random additions
		this.seed = seed; // seed value for random number generator

		update();
	}
	
	
	private double f3(double delta, double x0, double x1, double x2) {
		Random rf3 = new Random();
		return ((x0 + x1 + x2) / 3 + delta * rf3.nextGaussian());
	}
	
	
	private double f4(double delta, double x0, double x1, double x2, double x3) {
		Random rf4 = new Random();
		return ((x0 + x1 + x2 + x3) / 4 + delta * rf4.nextGaussian());

	}
	
	
	//returns the 2d-array, double-values were casted to int-values
	public int[][] getDiscreteIntField() {
		discIField = new int[dim][dim];

		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				discIField[i][j] = (int) dField[i][j];
			}
		}
		return discIField;
	}
	
	
	// returns the 2d-array
	public double[][] getDiscreteDoubleField() {
		return dField;
	}
	
	
	// returns the 2d-array divided into (# of probabilities) classes
	public int[][] getIntField(double[] probabilities) {

		iField = new int[dim][dim];

		// Copy 2d-Field into 1d-Array
		oneDArray = new double[dim * dim];
		int index = 0;
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				oneDArray[index++] = dField[i][j];
			}
		}

		// Sort 1d-Array
		Arrays.sort(oneDArray);

		// Get borders of the classes
		borders = new double[probabilities.length - 1];
		index = 0;
		for (int i = 0; i < probabilities.length - 1; i++) {
			index = index
					+ (int) Math.round(probabilities[i] * oneDArray.length);
			borders[i] = oneDArray[index - 1];
		}

		// assign the values of the field to the different classes
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				if (dField[i][j] <= borders[0]) {
					iField[i][j] = 0;
				} else {
					for (int k = 1; k < borders.length; k++) {
						if (dField[i][j] > borders[k - 1]
								&& dField[i][j] <= borders[k]) {
							iField[i][j] = k;
						}
					}
					if (dField[i][j] > borders[borders.length - 1]) {
						iField[i][j] = borders.length;
					}
				}

			}
		}
		return iField;
	}

	// Implementation of the algorithm MidPointFM2D
	// (from: The Science of Fractal Images,
	// Barnsley, Devaney, Mandelbrot, Peitgen, Saupe, Voss
	// 1988, Springer Verlag
	// p.100)
	public double[][] update() {

		r = new Random(seed);

		// size of Field (N+1,N+1)
		n = (int) Math.pow(2, maxlevel);
		dField = new double[n + 1][n + 1];
		dim = n + 1;

		// set the initial random corners
		delta = sigma;
		dField[0][0] = delta * r.nextGaussian();
		dField[0][n] = delta * r.nextGaussian();
		dField[n][0] = delta * r.nextGaussian();
		dField[n][n] = delta * r.nextGaussian();

		dd = n;
		d = n / 2;

		for (stage = 1; stage < maxlevel + 1; stage++) {
			// going from grid type I to type II
			delta = delta * Math.pow(0.5, (0.5 * h));

			// interpolate and offset points
			for (x = d; x <= n - d; x = x + dd) {
				for (y = d; y <= n - d; y = y + dd) {
					dField[x][y] = f4(delta, dField[x + d][y + d],
							dField[x + d][y - d], dField[x - d][y + d],
							dField[x - d][y - d]);
				}

			}

			// displace other points also if needed
			if (addition) {
				for (x = 0; x <= n; x = x + dd) {
					for (y = 0; y <= n; y = y + dd) {
						dField[x][y] = dField[x][y] + delta * r.nextGaussian();
					}
				}
			}

			// going from grid type II to type I
			delta = delta * Math.pow(0.5, (0.5 * h));
			// interpolate and offset boundary grid points
			for (x = d; x <= n - d; x = x + dd) {
				dField[x][0] = f3(delta, dField[x + d][0], dField[x - d][0],
						dField[x][d]);
				dField[x][n] = f3(delta, dField[x + d][n], dField[x - d][n],
						dField[x][n - d]);
				dField[0][x] = f3(delta, dField[0][x + d], dField[0][x - d],
						dField[d][x]);
				dField[n][x] = f3(delta, dField[n][x + d], dField[n][x - d],
						dField[n - d][x]);
			}

			// interpolate and offset interior grid points
			for (x = d; x <= n - d; x = x + dd) {
				for (y = dd; y <= n - d; y = y + dd) {
					dField[x][y] = f4(delta, dField[x][y + d],
							dField[x][y - d], dField[x + d][y],
							dField[x - d][y]);
				}
			}

			for (x = dd; x <= n - d; x = x + dd) {
				for (y = d; y <= n - d; y = y + dd) {
					dField[x][y] = f4(delta, dField[x][y + d],
							dField[x][y - d], dField[x + d][y],
							dField[x - d][y]);
				}
			}

			// displace other points also if needed
			if (addition) {
				for (x = 0; x <= n; x += dd) {
					for (y = 0; y <= n; y += dd) {
						dField[x][y] = dField[x][y] + delta * r.nextGaussian();
					}
				}
				for (x = d; x <= n - d; x += dd) {
					for (y = d; y <= n - d; y += dd) {
						dField[x][y] = dField[x][y] + delta * r.nextGaussian();
					}
				}
			}

			dd = dd / 2;
			d = d / 2;

		}

		return dField;
	}
	
	
	
	// Getters and setters
	public boolean isAddition() {
		return addition;
	}

	public void setAddition(boolean addition) {
		this.addition = addition;
		update();
	}

	public double getH() {
		return h;
	}

	public void setH(double h) {
		this.h = h;
		update();
	}

	public int getMaxlevel() {
		return maxlevel;
	}

	public void setMaxlevel(int maxlevel) {
		this.maxlevel = maxlevel;
		update();
	}

	public int getSeed() {
		return seed;
	}

	public void setSeed(int seed) {
		this.seed = seed;
		update();
	}

	public double getSigma() {
		return sigma;
	}

	public void setSigma(double sigma) {
		this.sigma = sigma;
		update();
	}

	public int getDim() {
		return dim;
	}

	public void setDim(int dim) {
		this.dim = dim;
		update();
	}

}
