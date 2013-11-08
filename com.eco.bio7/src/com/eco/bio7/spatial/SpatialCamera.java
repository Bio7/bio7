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


package com.eco.bio7.spatial;

/**
 * This class offers methods to adjust a custom camera and methods to get camera
 * coordinates (walk-through camera).
 * 
 * @author Bio7
 * 
 */
public class SpatialCamera {

	/**
	 * A method to adjust the custom camera coordinates in the Spatial view.
	 * 
	 * @param xpos
	 *            the x-position of the camera.
	 * @param ypos
	 *            the y-position of the camera.
	 * @param zpos
	 *            the z-position of the camera.
	 * @param xlook
	 *            the x look at coordinate.
	 * @param ylook
	 *            the y look at coordinate.
	 * @param zlook
	 *            the z look at coordinate.
	 */
	public static void setCustomCamera(double xpos, double ypos, double zpos, double xlook, double ylook, double zlook) {

		SpatialStructure.setCustomCamera(xpos, ypos, zpos, xlook, ylook, zlook);

	}

	/**
	 * This method returns the x-position of the camera.
	 * 
	 * @return the x-position as a double.
	 */
	public static double getXCamPos() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc.getXCamPos();
	}

	/**
	 * This method returns the y-position of the camera.
	 * 
	 * @return the y-position as a double.
	 */
	public static double getYCamPos() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc.getYCamPos();
	}

	/**
	 * This method returns the z-position of the camera.
	 * 
	 * @return the z-position as a double.
	 */
	public static double getZCamPos() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc.getZCamPos();
	}

}
