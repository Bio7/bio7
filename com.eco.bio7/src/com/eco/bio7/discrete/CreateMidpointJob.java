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

package com.eco.bio7.discrete;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import com.eco.bio7.database.Bio7State;
//import com.eco.bio7.database.Planttemplate;
//import com.eco.bio7.methods.Plant;

public class CreateMidpointJob extends WorkspaceJob {

	private int po;
	private double sigma;
	private double fractal;
	private int selected;
	private int seed;
	private int scale;
	private String[] t;
	private boolean createImage;

	/* Evaluates R expressions from the console of Bio7 ! */
	public CreateMidpointJob(int po, double sigma, double fractal, int selected, int seed, int scale, String[] t, boolean createImage) {
		super("Job");
		this.po = po;
		this.sigma = sigma;
		this.fractal = fractal;
		this.selected = selected;
		this.seed = seed;
		this.scale = scale;
		this.t = t;
		this.createImage = createImage;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Evaluate Line Selection", IProgressMonitor.UNKNOWN);
		if (createImage == false) {

			midpoint(po, sigma, fractal, selected, seed, scale, t, createImage);
		} else {

			createFractalImage(po, sigma, fractal, selected, seed, scale, t, createImage);
		}
		return Status.OK_STATUS;
	}

	public void midpoint(int po, double sigma, double fractal, int selected, int seed, int scale, String[] t, boolean createImage) {
		boolean addition = true;

		if (selected == 0) {
			addition = true;
		} else {
			addition = false;
		}

		Midpoint mp1 = new Midpoint(po, sigma, fractal, addition, seed);

		double[] prob = new double[t.length];
		for (int i = 0; i < t.length; i++) {
			prob[i] = Double.parseDouble(t[i]);
			// System.out.println(prob[i]);
		}

		// Tools.array2dOut(mp1.getIntField(prob));

		int[][] val = mp1.getIntField(prob);

		for (int y = 0; y < Field.getHeight(); y++) {

			for (int x = 0; x < Field.getWidth(); x++) {

				int zuff = val[x][y];

				if (zuff > 0) {
					/* Zero is the value of the soil ! */

					/* We fill the state array ! */
					Field.setState(x, y, zuff);
					Field.setTempState(x, y, zuff);

					/* Create a plant object from an activated template ! */

					//Planttemplate p = Bio7State.getPlantTemplate(zuff);
					// Get a plant template by chance!

					/*
					 * Plant templates are all activated plants in the
					 * spreadsheet of Bio7. If only one plant is activated, this
					 * template has the index 1 !
					 */

					/* We fill the plant array */
					/*Field.setPlant(x, y, new Plant(x, y, p));
					Field.setTempPlant(x, y, new Plant(x, y, p));*/
				}

				else {
					/* We fill the arrays with the soil state ! */
					/*Field.setPlant(x, y, Field.getSoil(x, y));
					Field.setTempPlant(x, y, Field.getSoil(x, y));*/

					Field.setState(x, y, zuff);
					Field.setTempState(x, y, zuff);

				}

			}
		}

		Field.doPaint();
		val = null;
		prob = null;

	}

	protected void createFractalImage(int po, double sigma, double fractal, int selected, int seed, int scale, String[] t, boolean createImage) {

		boolean addition = true;

		if (selected == 0) {
			addition = true;
		} else {
			addition = false;
		}

		Midpoint mp1 = new Midpoint(po, sigma, fractal, addition, seed);
		double[][] val = mp1.getDiscreteDoubleField();
		FloatProcessor ip = new FloatProcessor(val.length - 1, val[0].length - 1);
		for (int i = 0; i < val.length - 1; i++) {
			for (int u = 0; u < val[0].length - 1; u++) {
				double value = (val[i][u]) * scale;
				ip.putPixelValue(i, u, value);

			}

		}
		ip.resetMinAndMax();
		ImagePlus imp = new ImagePlus("FractalImage", ip);
		imp.show();
	}

}
