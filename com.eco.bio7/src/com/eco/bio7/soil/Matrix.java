package com.eco.bio7.soil;

import java.awt.Color;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import com.eco.bio7.discrete.Field;

/**
 * This class provides a static method for the creation of an image matrix in
 * ImageJ.
 * 
 * @author Bio7
 * 
 */
public class Matrix {

	/**
	 * This method creates a random initialized ImageJ (float) matrix.
	 * 
	 * @param name
	 *            a name for the matrix which can be used e.g. for soil
	 *            properties etc.
	 * @param value
	 *            a random start value (0.0 - value) for a randomized grid.
	 */
	public static void create(String name, int value) {

		/* If no image is present we create one with random pixels! */
		ImageProcessor ip = new FloatProcessor(Field.getWidth(), Field.getHeight());

		ip.setColor(Color.white);
		ip.fill();

		int w = ip.getWidth();
		int h = ip.getHeight();

		for (int u = 0; u < h; u++) {
			for (int v = 0; v < w; v++) {
				float b = (float) (Math.random() * value);

				ip.putPixelValue(v, u, b);

			}
		}
		/* We have to calibrate the float matrix (image)! */
		ip.resetMinAndMax();

		ImagePlus imp = new ImagePlus(name, ip);
		imp.updateAndDraw();
		imp.show();

	}

}
