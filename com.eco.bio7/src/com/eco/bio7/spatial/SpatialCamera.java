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
	 * This method returns the x-position of the camera (the walkthrough camera).
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


	/**
	 * Displays the custom camera in the main view. With the split view the small
	 * view then shows the normal camera (rotated with the mouse). Else (the
	 * default) the custom camera is displayed in the small view of the split view.
	 * Without the split view the custom camera is always displayed in the view.
	 * 
	 * @param mainView true for the main view
	 */
	public static void setCustomCameraInMainView(boolean mainView) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setCustomCameraInMainView(mainView);
		}
	}

	/**
	 * Returns true if the custom camera is displayed in the main view (see
	 * setCustomCameraInMainView).
	 * 
	 * @return true for the main view
	 */
	public static boolean isCustomCameraInMainView() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null && struc.isCustomCameraInMainView();
	}

	/**
	 * Returns the position and the look at of the custom camera.
	 * 
	 * @return x, y, z of the position and x, y, z of the look at
	 */
	public static double[] getCustomCamera() {
		return SpatialStructure.getCustomCamera();
	}

	/**
	 * The coordinates of the custom camera are in the space of the walkthrough:
	 * the scene is shown like in the walkthrough (without the rotation of the
	 * mouse and without the centering of the data). Set by copyWalkthroughToCustomCamera.
	 * 
	 * @param walkthroughCoordinates true for the coordinates of the walkthrough
	 */
	public static void setCustomCameraWalkthroughCoordinates(boolean walkthroughCoordinates) {
		SpatialStructure.setCustomCameraWalkFrame(walkthroughCoordinates);
	}

	/**
	 * Returns true if the coordinates of the custom camera are in the space of the
	 * walkthrough.
	 * 
	 * @return true for the coordinates of the walkthrough
	 */
	public static boolean isCustomCameraWalkthroughCoordinates() {
		return SpatialStructure.isCustomCameraWalkFrame();
	}

	/**
	 * Sets the camera of the split view (the small view if the custom camera is
	 * not used there).
	 * 
	 * @param xpos  the x-position of the camera
	 * @param ypos  the y-position of the camera
	 * @param zpos  the z-position of the camera
	 * @param xlook the x look at coordinate
	 * @param ylook the y look at coordinate
	 * @param zlook the z look at coordinate
	 */
	public static void setSplitCamera(double xpos, double ypos, double zpos, double xlook, double ylook, double zlook) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.xCamSplit = xpos;
			struc.yCamSplit = ypos;
			struc.zCamSplit = zpos;
			struc.xSplitLookAt = xlook;
			struc.ySplitLookAt = ylook;
			struc.zSplitLookAt = zlook;
		}
	}

	/**
	 * Returns the position and the look at of the camera of the split view.
	 * 
	 * @return x, y, z of the position and x, y, z of the look at (null if the 3D
	 *         view is not opened)
	 */
	public static double[] getSplitCamera() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc == null ? null
				: new double[] { struc.xCamSplit, struc.yCamSplit, struc.zCamSplit, struc.xSplitLookAt, struc.ySplitLookAt,
						struc.zSplitLookAt };
	}

	/**
	 * The coordinates of the split view camera are in the space of the
	 * walkthrough (see setCustomCameraWalkthroughCoordinates).
	 * 
	 * @param walkthroughCoordinates true for the coordinates of the walkthrough
	 */
	public static void setSplitCameraWalkthroughCoordinates(boolean walkthroughCoordinates) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setSplitCameraWalkFrame(walkthroughCoordinates);
		}
	}

	/**
	 * Returns true if the coordinates of the split view camera are in the space of
	 * the walkthrough.
	 * 
	 * @return true for the coordinates of the walkthrough
	 */
	public static boolean isSplitCameraWalkthroughCoordinates() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null && struc.isSplitCameraWalkFrame();
	}

	/**
	 * Returns the position and the look at of the walkthrough (and flythrough)
	 * camera.
	 * 
	 * @return x, y, z of the position and x, y, z of the look at (null if the 3D
	 *         view is not opened)
	 */
	public static double[] getWalkthroughCamera() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc == null ? null : struc.getWalkthroughCamera();
	}

	/**
	 * Copies the position and the look at of the walkthrough (or flythrough) camera
	 * to the custom camera, with the coordinates of the walkthrough (the same
	 * view, also when the walkthrough is disabled).
	 */
	public static void copyWalkthroughToCustomCamera() {
		double[] c = getWalkthroughCamera();
		if (c != null) {
			setCustomCamera(c[0], c[1], c[2], c[3], c[4], c[5]);
			setCustomCameraWalkthroughCoordinates(true);
		}
	}

	/**
	 * Copies the position and the look at of the walkthrough (or flythrough) camera
	 * to the camera of the split view, with the coordinates of the walkthrough.
	 */
	public static void copyWalkthroughToSplitCamera() {
		double[] c = getWalkthroughCamera();
		if (c != null) {
			setSplitCamera(c[0], c[1], c[2], c[3], c[4], c[5]);
			setSplitCameraWalkthroughCoordinates(true);
		}
	}


	/**
	 * Uses the camera of the main view (see setMainCamera) instead of the camera
	 * rotated with the mouse: another perspective without a rotation of the world
	 * (e.g., with the custom camera in the small view of the split view). The
	 * custom camera has priority if it is displayed in the main view.
	 * 
	 * @param enabled true to use the main view camera
	 */
	public static void setMainCameraEnabled(boolean enabled) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setMainCameraEnabled(enabled);
		}
	}

	/**
	 * Returns true if the main view camera is used.
	 * 
	 * @return true if the main view camera is enabled
	 */
	public static boolean isMainCameraEnabled() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null && struc.isMainCameraEnabled();
	}

	/**
	 * Sets the camera of the main view (see setMainCameraEnabled).
	 * 
	 * @param xpos  the x-position of the camera
	 * @param ypos  the y-position of the camera
	 * @param zpos  the z-position of the camera
	 * @param xlook the x look at coordinate
	 * @param ylook the y look at coordinate
	 * @param zlook the z look at coordinate
	 */
	public static void setMainCamera(double xpos, double ypos, double zpos, double xlook, double ylook, double zlook) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setMainCamera(xpos, ypos, zpos, xlook, ylook, zlook);
		}
	}

	/**
	 * Returns the position and the look at of the main view camera.
	 * 
	 * @return x, y, z of the position and x, y, z of the look at (null if the 3D
	 *         view is not opened)
	 */
	public static double[] getMainCamera() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc == null ? null : struc.getMainCamera();
	}

	/**
	 * The coordinates of the main view camera are in the space of the walkthrough
	 * (see setCustomCameraWalkthroughCoordinates).
	 * 
	 * @param walkthroughCoordinates true for the coordinates of the walkthrough
	 */
	public static void setMainCameraWalkthroughCoordinates(boolean walkthroughCoordinates) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setMainCameraWalkFrame(walkthroughCoordinates);
		}
	}

	/**
	 * Returns the position and the look at of the camera rotated with the mouse
	 * (the normal camera, looking at the center).
	 * 
	 * @return x, y, z of the position and x, y, z of the look at (null if the 3D
	 *         view is not opened)
	 */
	public static double[] getViewCamera() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc == null ? null : struc.getViewCamera();
	}

	/**
	 * Copies the camera rotated with the mouse to the main view camera (the same
	 * view, then the position and the look at can be changed).
	 */
	public static void copyViewToMainCamera() {
		double[] c = getViewCamera();
		if (c != null) {
			setMainCamera(c[0], c[1], c[2], c[3], c[4], c[5]);
			setMainCameraWalkthroughCoordinates(false);
		}
	}

	/**
	 * Copies the position and the look at of the walkthrough (or flythrough) camera
	 * to the main view camera, with the coordinates of the walkthrough.
	 */
	public static void copyWalkthroughToMainCamera() {
		double[] c = getWalkthroughCamera();
		if (c != null) {
			setMainCamera(c[0], c[1], c[2], c[3], c[4], c[5]);
			setMainCameraWalkthroughCoordinates(true);
		}
	}

}
