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

package com.eco.bio7.environment;

import java.util.Date;
import cern.jet.random.tfloat.FloatUniform;
import cern.jet.random.tfloat.engine.FloatMersenneTwister;
import com.eco.bio7.discrete.Field;

/**
 * This class provides some static methods for the creation of custom formulas
 * and disturbance functions inside the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class Environment {
	/*  */
	private static FloatUniform uni = new FloatUniform(0.0f, 1000.0f, new FloatMersenneTwister(new Date()));

	private static int random = 0;

	/**
	 * A simple clumbed disturbance function.
	 * 
	 * @param xd
	 *            the extension in x direction.
	 * @param yd
	 *            the extension in y direction.
	 * 
	 */
	public void clumbedDisturbance(int yd, int xd) {
		int a = yd;
		int b = xd;
		int ax = (int) (Math.random() * (Field.getWidth()));
		int ay = (int) (Math.random() * (Field.getHeight()));

		for (int x = ax; x < a + ax; x++) {
			for (int y = ay; y < b + ay; y++) {
				Field.setTempState((x + Field.getWidth()) % Field.getWidth(), (y + Field.getHeight()) % Field.getHeight(), 0);
				Field.setState((x + Field.getWidth()) % Field.getWidth(), (y + Field.getHeight()) % Field.getHeight(), 0);

			}
		}

	}

	/**
	 * A random disturbance function which affects all states but not the
	 * defined state (per mille - slider value 1 = 0.1%).
	 * 
	 * @param x
	 *            x-position
	 * @param y
	 *            y-position
	 * @param value
	 *            an integer value representing the disturbance
	 * @param state
	 *            a state not affected by the disturbance function
	 */
	public static void randomDisturbance(int x, int y, int value, int state) {

		random = uni.nextInt();

		if (random < value) {

			if (Field.getState(x, y) != state) {
				Field.setTempState(x, y, 0);

				Field.setState(x, y, 0);

			}

		}

	}

}
