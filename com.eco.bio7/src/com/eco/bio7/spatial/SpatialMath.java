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


package com.eco.bio7.spatial;

/**
 * This class offers some utility math methods.
 * @author Bio7
 *
 */
public class SpatialMath {
	/**
	 * This method calculates the normal from three vertices.
	 * 
	 * @param v1
	 *            vector1 as a float array
	 * @param v2
	 *            vector 2 as a float array
	 * @param v3
	 *            vector 3 as a float array
	 * @return a float array representing the normal
	 */
	public static float[] calculateNormal(float[] v1, float[] v2, float[] v3) {
		float[] e1 = new float[3];
		float[] e2 = new float[3];
		float[] normal = new float[3];
		// Edge 1
		e1[0] = v2[0] - v1[0];
		e1[1] = v2[1] - v1[1];
		e1[2] = v2[2] - v1[2];
		// Edge 2
		e2[0] = v3[0] - v1[0];
		e2[1] = v3[1] - v1[1];
		e2[2] = v3[2] - v1[2];
		// Calculate the cross product for the Edges
		normal[0] = e1[1] * e2[2] - e1[2] * e2[1];
		normal[1] = e1[2] * e2[0] - e1[0] * e2[2];
		normal[2] = e1[0] * e2[1] - e1[1] * e2[0];
		// Normalize
		return normal;
	}

	/**
	 * This method normalizes a vector.
	 * 
	 * @param normal
	 *            a float array
	 * @return the normalized vector.
	 */
	public static float[] normalize(float[] normal) {
		float t = normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2];
		if (t != 0 && t != 1)
			t = (float) (1 / Math.sqrt(t));
		normal[0] *= t;
		normal[1] *= t;
		normal[2] *= t;
		return normal;
	}

	/**
	 * Calculates the euclidean distance between two points.
	 * 
	 * @param vector1
	 *            a float array with the coordinates.
	 * @param vector2
	 *            a float array with the coordinates.
	 * @return the distance of two points.
	 */
	public static float euclideanDistance(float[] vector1, float[] vector2) {
		double d = (Math.pow(vector1[0] - vector2[0], 2) + Math.pow(vector1[1] - vector2[1], 2) + Math.pow(vector1[2] - vector2[2], 2));
		float distance = (float) Math.sqrt(d);

		return distance;
	}
	
	

}
