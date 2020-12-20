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

import javax.swing.SwingUtilities;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.loader3d.OBJModel;
import com.jogamp.opengl.util.awt.Overlay;

/**
 * A class which offers some custom methods for the Spatial view.
 * 
 * @author Bio7
 * 
 */
public class SpatialUtil {

	/**
	 * Get the x-rotation value.
	 * 
	 * @return a float value.
	 */
	public static float getRotationX() {
		float rotx = 0;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			rotx = struc.getRotatx();
		}
		return rotx;
	}

	/**
	 * Sets the rotation in x-direction.
	 * 
	 * @param rotatx a float value.
	 */
	public static void setRotationX(float rotatx) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setRotatx(rotatx);
		}
	}

	/**
	 * Get the y-rotation value.
	 * 
	 * @return a float value.
	 */
	public static float getRotationY() {
		float roty = 0;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			roty = struc.getRotaty();
		}
		return roty;
	}

	/**
	 * Sets the rotation in y-direction.
	 * 
	 * @param rotaty a float value.
	 */
	public static void setRotationY(float rotaty) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setRotaty(rotaty);
		}
	}

	/**
	 * Get the z-rotation value.
	 * 
	 * @return a float value.
	 */
	public static float getRotationZ() {
		float rotz = 0;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			rotz = struc.getRotatz();
		}
		return rotz;
	}

	/**
	 * Sets the rotation in z-direction.
	 * 
	 * @param rotatz a float value.
	 */
	public static void setRotationZ(float rotatz) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setRotatz(rotatz);
		}
	}

	/**
	 * Resets the rotation of the rotation matrix.
	 */
	public static void resetRotation() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.getLastRot().setIdentity(); // Reset Rotation
			struc.getThisRot().setIdentity(); // Reset Rotation
		}

	}

	/**
	 * Returns if the movement has been stopped.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isStopMovement() {
		boolean stop = false;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {

			stop = struc.isStopMovement();
		}
		return stop;
	}

	/**
	 * A method which stops the movement in the Spatial panel for selection and
	 * custom purposes.
	 * 
	 * @param stop a boolean value.
	 */
	public static void setStopMovement(boolean stop) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setStopMovement(stop);
		}
	}

	/**
	 * Returns the x-size of the cube in the Spatial view.
	 * 
	 * @return the x-size as a float.
	 */
	public float getCubeSizeX() {
		float sizex = 0;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			sizex = struc.getSizeX();
		}
		return sizex;
	}

	/**
	 * Returns the y-size of the cube in the Spatial view.
	 * 
	 * @return the y-size as a float.
	 */
	public float getCubeSizeY() {
		float sizey = 0;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			sizey = struc.getSizeY();
		}
		return sizey;
	}

	/**
	 * Returns the z-size of the cube in the Spatial view.
	 * 
	 * @return the z-size as a float.
	 */
	public float getCubeSizeZ() {
		float sizez = 0;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			sizez = struc.getSizeZ();
		}
		return sizez;
	}

	/**
	 * Returns if a split view is active.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isSplitView() {
		boolean split = false;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			split = struc.isSplitView();
		}
		return split;
	}

	/**
	 * Returns if the split panel is drawn.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isSplitPanelDrawing() {
		boolean splitPanel = false;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			splitPanel = struc.isSplitPanelDrawing();
		}
		return splitPanel;

	}

	/**
	 * This methods triggers the switch from split view to normal view.
	 * 
	 * @param splitView a boolean value.
	 */
	public static void setSplitView(boolean splitView) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setSplitView(splitView);
		}
	}

	/**
	 * Sets the light position of the first lamp.
	 * 
	 * @param x the x-position as a float.
	 * @param y the y-position as a float.
	 * @param z the z-position as a float.
	 */
	public static void setLightPos1(float x, float y, float z) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.lightPos1[0] = x;
			struc.lightPos1[1] = y;
			struc.lightPos1[2] = z;
		}
	}

	/**
	 * Sets the light position of the second lamp.
	 * 
	 * @param x the x-position as a float.
	 * @param y the y-position as a float.
	 * @param z the z-position as a float.
	 */
	public static void setLightPos2(float x, float y, float z) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.lightPos2[0] = x;
			struc.lightPos2[1] = y;
			struc.lightPos2[2] = z;
		}

	}

	/**
	 * Sets the light position of the third lamp.
	 * 
	 * @param x the x-position as a float.
	 * @param y the y-position as a float.
	 * @param z the z-position as a float.
	 */
	public static void setLightPos3(float x, float y, float z) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.lightPos3[0] = x;
			struc.lightPos3[1] = y;
			struc.lightPos3[2] = z;
		}
	}

	/**
	 * Sets the light position of the fourth lamp.
	 * 
	 * @param x the x-position as a float.
	 * @param y the y-position as a float.
	 * @param z the z-position as a float.
	 */
	public static void setLightPos4(float x, float y, float z) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.lightPos4[0] = x;
			struc.lightPos4[1] = y;
			struc.lightPos4[2] = z;
		}
	}

	/**
	 * Returns the light position of the first lamp.
	 * 
	 * @return a float array.
	 */
	public static float[] getLightPos1() {
		float[] pos1 = null;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			pos1 = struc.getLightPos1();
		}
		return pos1;
	}

	/**
	 * Returns the light position of the second lamp.
	 * 
	 * @return a float array.
	 */
	public static float[] getLightPos2() {
		float[] pos2 = null;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			pos2 = struc.getLightPos2();
		}
		return pos2;
	}

	/**
	 * Returns the light position of the third lamp.
	 * 
	 * @return a float array.
	 */
	public static float[] getLightPos3() {
		float[] pos3 = null;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			pos3 = struc.getLightPos3();
		}
		return pos3;
	}

	/**
	 * Returns the light position of the fourth lamp.
	 * 
	 * @return a float array.
	 */
	public float[] getLightPos4() {
		float[] pos4 = null;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			pos4 = struc.getLightPos4();
		}
		return pos4;

	}

	/**
	 * Triggers the fullscreen mode in the 3D view.
	 * 
	 */
	public static void setFullscreen() {

		final SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {

			struc.getView().createFullscreen(0);

		}

	}
	
	/**
	 * Triggers the fullscreen mode and opens the 3D view in monitor i.
	 * 
	 */
	public static void setFullscreen(int i) {

		final SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {

			struc.getView().createFullscreen(i);

		}

	}

	/**
	 * A method to set the splitscreen coordinates and size.
	 * 
	 * @param x      the x-origin.
	 * @param y      the y-origin.
	 * @param width  the width.
	 * @param height the height.
	 */
	public static void setSplitScreenSizeLocation(int x, int y, int width, int height) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setSplitScreenSize(x, y, width, height);
		}
	}

	/**
	 * This method returns the width of the visible screen.
	 * 
	 * @return the width as an integer value.
	 */
	public static int getScreenWidth() {
		int width = 0;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			width = struc.getWidth();
		}
		return width;
	}

	/**
	 * This method returns the height of the visible screen.
	 * 
	 * @return the height as an integer value.
	 */
	public static int getScreenHeight() {
		int height = 0;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			height = struc.getHeight();
		}
		return height;
	}

	/**
	 * This method creates an overlay object.
	 * 
	 * @return an Overlay instance.
	 */
	public static Overlay createOverlay() {
		Overlay overlay = null;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			overlay = new Overlay(struc.getDrawable());
		}
		return overlay;
	}

	/**
	 * This method enables or disables the visualization of the grid orientation.
	 * 
	 * @param value a boolean value.
	 */
	public static void showGrid(boolean value) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.showGrid = value;

		}

	}

	/**
	 * This method enables or disables the visualization of the quad orientation.
	 * 
	 * @param value a boolean value.
	 */
	public static void showQuad(boolean value) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.showQuad = value;
		}

	}

	/**
	 * This method enables or disables the visualization of the axes orientation.
	 * 
	 * @param value a boolean value.
	 */
	public static void showAxes(boolean value) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.showAxes = value;

		}

	}

	/**
	 * Sets the background color.
	 * 
	 * @param r the red value as an integer (0-255).
	 * @param g the green value as an integer (0-255).
	 * @param b the blue value as an integer (0-255).
	 */
	public static void setBackgroundColor(int r, int g, int b) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();

		float red = (float) (r) / 255;
		float green = (float) (g) / 255;
		float blue = (float) (b) / 255;
		if (struc != null) {
			struc.setColorBackground(new float[] { red, green, blue, 1.0f });
			struc.backgroundColour = true;
		}
	}

	/**
	 * Sets the color of the cube, axes and grid.
	 * 
	 * @param r the red value as an integer (0-255).
	 * @param g the green value as an integer (0-255).
	 * @param b the blue value as an integer (0-255).
	 */
	public static void setLinesColor(int r, int g, int b) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();

		float red = (float) (r) / 255;
		float green = (float) (g) / 255;
		float blue = (float) (b) / 255;
		if (struc != null) {
			struc.getScene().setColorCubeLines(new float[] { red, green, blue, 1.0f });
			struc.backgroundColour = true;
		}

	}

	/**
	 * A method which returns a reference to the UI loaded *.obj model.
	 * 
	 * @return an object of type OBJModel.
	 */
	public static OBJModel getObjModel() {
		OBJModel model = null;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();

		if (struc != null) {
			model = struc.model;
		}
		return model;
	}

	/**
	 * This method returns a boolean which is triggered by the UI slider to adjust a
	 * frame rate.
	 * 
	 * @return a boolean value.
	 */
	public static boolean canStep() {
		boolean can = true;

		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();

		if (struc != null) {
			can = struc.canStep();
		}
		return can;
	}

	/**
	 * This method invokes the setup method of the compiled OpenGL code if
	 * available.
	 */
	public static void setup() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		if (grid != null) {
			grid.setup = true;
		}
	}

	/**
	 * This method invokes the "run" method triggered by the timer.
	 */
	public static void startStop() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		if (grid != null) {
			if (grid.isFromOpenGl()) {
				grid.setFromOpenGl(false);
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						Options3d.getPlayPauseButton().setSelection(false);
					}
				});

			} else {
				grid.setFromOpenGl(true);
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						Options3d.getPlayPauseButton().setSelection(true);
					}
				});
			}
		}

	}

	/**
	 * This method returns the boolean value "true" if the OpenGL run method is
	 * triggered!.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isStarted() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();

		return grid.isFromOpenGl();

	}

}
