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

import javax.swing.SwingUtilities;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.loader3d.MeshModel;
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
	 * Returns the iso surface of the 3d view (e.g., to create an iso surface of
	 * the data of a model with IsoSurface.setData).
	 * 
	 * @return the iso surface or null if the 3d view is not opened
	 */
	public static IsoSurface getIsoSurface() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null ? struc.getIsoSurface() : null;
	}

	/**
	 * Returns the clipping (box, cutting plane and slice) of the volume and the iso
	 * surface of the 3d view.
	 * 
	 * @return the clipping or null if the 3d view is not opened
	 */
	public static Clipping getClipping() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null ? struc.getClipping() : null;
	}

	/**
	 * Returns the measurements of the iso surface and the volume of the 3d view
	 * (objects, region statistics and picked points, displayed in the Bio7
	 * table).
	 * 
	 * @return the measurements or null if the 3d view is not available
	 */
	public static Measurement3d getMeasurement() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null ? struc.getMeasurement() : null;
	}

	/**
	 * Returns the volume renderer of the 3d view (e.g., to load a stack with
	 * VolumeRenderer.load).
	 * 
	 * @return the volume renderer or null if the 3d view is not opened
	 */
	public static VolumeRenderer getVolumeRenderer() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null ? struc.getVolumeRenderer() : null;
	}

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
	 * Sets the bounds of the data of a model (e.g., spheres of a script) for the
	 * box, the grids, the plot view with the axis numbers and the camera (if no
	 * texture, volume or iso surface is displayed). The data is moved into the
	 * center of the view.
	 * 
	 * @param x0 the minimum x
	 * @param x1 the maximum x
	 * @param y0 the minimum y
	 * @param y1 the maximum y
	 * @param z0 the minimum z
	 * @param z1 the maximum z
	 */
	public static void setDataBounds(double x0, double x1, double y0, double y1, double z0, double z1) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setDataBounds(new double[] { x0, x1, y0, y1, z0, z1 });
		}
	}

	/**
	 * Removes the bounds of the data of a model (the box and the grids have the
	 * size of the space again).
	 */
	public static void clearDataBounds() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setDataBounds(null);
		}
	}

	/**
	 * Selects the grid planes of the plot view (behind the data): the floor (XY,
	 * off by default because a height map covers it) and the walls (XZ, YZ).
	 * 
	 * @param xy the XY grid (floor)
	 * @param xz the XZ grid
	 * @param yz the YZ grid
	 */
	public static void setPlotGrids(boolean xy, boolean xz, boolean yz) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotGridXY = xy;
			struc.plotGridXZ = xz;
			struc.plotGridYZ = yz;
			Options3d.updatePlotGrids();
		}
	}

	/**
	 * Enables or disables the plot view (the grids behind the data and the axes
	 * with the numbers at the edges of the box, like a 3D plot). The axis numbers
	 * are enabled with the plot view.
	 * 
	 * @param plotView true for the plot view
	 */
	public static void setPlotView(boolean plotView) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotView = plotView;
			if (plotView && struc.getAxisLabelsRenderer() != null) {
				struc.getAxisLabelsRenderer().setEnabled(true);
			}
		}
	}

	/**
	 * Sets the title of the plot view (displayed at the top of the 3D view).
	 * <p>
	 * The texts of the plot view (the title, the axis titles and the legends)
	 * can contain a simple math markup: x^2, x^-0.5 or x^{n+1} for superscripts,
	 * N_0 or N_{max} for subscripts (\\^ and \\_ for the characters). Greek
	 * letters and symbols can be written as Unicode characters (e.g., \u03B1 for
	 * alpha, \u00B7 for a dot).
	 * 
	 * @param title the title (lines separated with \n) or null (no title)
	 */
	public static void setPlotTitle(String title) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotTitle = title;
		}
	}

	/**
	 * Sets the titles of the axes of the plot view (e.g., the names and the units
	 * of the data).
	 * 
	 * @param x the title of the x axis (null = X)
	 * @param y the title of the y axis (null = Y)
	 * @param z the title of the z axis (null = Z)
	 */
	public static void setAxisTitles(String x, String y, String z) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotAxisTitles = new String[] { x, y, z };
		}
	}

	/**
	 * Selects logarithmic axes of the plot view. The data of a logarithmic axis
	 * are drawn as log10 values (e.g., Math.log10(value)) and the numbers of the
	 * axis are the values (10^v, e.g., 100, 1e5, 1.34e10) with minor ticks and
	 * grid lines at 2, 3, ..., 9 times the powers of ten.
	 * 
	 * @param x true for a logarithmic x axis
	 * @param y true for a logarithmic y axis
	 * @param z true for a logarithmic z axis
	 */
	public static void setLogAxes(boolean x, boolean y, boolean z) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotLogAxes = new boolean[] { x, y, z };
		}
	}

	/**
	 * Sets the formats of the numbers of the axes of the plot view (see
	 * java.util.Formatter, e.g., "%.2e" for 1.34e+10, "%.1f" or "%,.0f"). By
	 * default (null) the numbers are formatted automatically: very large or small
	 * numbers in a scientific notation (e.g., 1.34e10, 2.5e-6). The format "10^"
	 * displays the numbers as powers of ten with superscripts (e.g., 10^3,
	 * 1.34x10^10), e.g., for logarithmic axes.
	 * 
	 * @param x the format of the x axis (null = automatic)
	 * @param y the format of the y axis (null = automatic)
	 * @param z the format of the z axis (null = automatic)
	 */
	public static void setAxisNumberFormats(String x, String y, String z) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotNumberFormats = new String[] { x, y, z };
		}
	}

	/**
	 * Sets the values of the axis numbers of the plot view at the data bounds
	 * (see setDataBounds). The three axes of the 3D view have the same scale. For
	 * data with different units and ranges (e.g., the time in hours and cell
	 * counts up to 1e10) the data is drawn in the coordinates of the box (e.g.,
	 * 0 - 10 for all axes) and the axis numbers display the values of the data
	 * (a linear mapping of the bounds to the values). For a logarithmic axis the
	 * values are log10 values.
	 * 
	 * @param x0 the value at the minimum x of the bounds
	 * @param x1 the value at the maximum x of the bounds
	 * @param y0 the value at the minimum y of the bounds
	 * @param y1 the value at the maximum y of the bounds
	 * @param z0 the value at the minimum z of the bounds
	 * @param z1 the value at the maximum z of the bounds
	 */
	public static void setAxisRanges(double x0, double x1, double y0, double y1, double z0, double z1) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotAxisRanges = new double[] { x0, x1, y0, y1, z0, z1 };
		}
	}

	/**
	 * Displays a legend of categories in the top left corner of the 3D view: a
	 * box with the color and the label of each category (e.g., the groups of a
	 * scatter plot).
	 * 
	 * @param title  the title of the legend (null = no title)
	 * @param labels the labels of the categories
	 * @param colors the colors of the categories (0xRRGGBB, e.g., 0xFF8000 or
	 *               java.awt.Color.getRGB())
	 */
	public static void setLegend(String title, String[] labels, int[] colors) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotLegend = labels != null ? PlotLegend.categories(title, labels, colors) : null;
		}
	}

	/**
	 * Displays a color bar at the right side of the 3D view: a color ramp with
	 * the numbers of the values (e.g., the heights of a surface). The colors are
	 * evenly distributed from the minimum to the maximum (see
	 * PlotLegend.rampColor to draw the data with the same colors and the ramps
	 * PlotLegend.VIRIDIS, RAINBOW, DIVERGING, TERRAIN).
	 * 
	 * @param title  the title of the color bar (e.g., the name and the unit)
	 * @param min    the value of the first color
	 * @param max    the value of the last color
	 * @param colors the colors of the ramp (0xRRGGBB)
	 */
	public static void setColorBar(String title, double min, double max, int[] colors) {
		setColorBar(title, min, max, colors, false, null);
	}

	/**
	 * Displays a color bar at the right side of the 3D view (see
	 * setColorBar(String, double, double, int[])) with logarithmic values or a
	 * format of the numbers.
	 * 
	 * @param title  the title of the color bar
	 * @param min    the value of the first color (a log10 value if log)
	 * @param max    the value of the last color (a log10 value if log)
	 * @param colors the colors of the ramp (0xRRGGBB)
	 * @param log    true if min and max are log10 values (the numbers are 10^v)
	 * @param format the format of the numbers (java.util.Formatter, e.g., "%.1f",
	 *               "10^" for powers of ten, null = automatic, e.g., 1.34e10)
	 */
	public static void setColorBar(String title, double min, double max, int[] colors, boolean log, String format) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotColorBar = colors != null ? PlotLegend.colorBar(title, min, max, colors, log, format) : null;
		}
	}

	/**
	 * Sets the font sizes of the plot view as factors of the default sizes (1 =
	 * default, 2 = twice as large, 0.5 = half the size).
	 * 
	 * @param title      the title (default 16 points)
	 * @param axisTitles the titles of the axes
	 * @param numbers    the numbers of the axes
	 * @param legends    the legend and the color bar (default 12 points)
	 */
	public static void setPlotFontSizes(float title, float axisTitles, float numbers, float legends) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotFontScales = new float[] { positive(title), positive(axisTitles), positive(numbers), positive(legends) };
		}
	}

	/**
	 * Sets all font sizes of the plot view (the title, the axis titles and
	 * numbers and the legends) as a factor of the default sizes (1 = default).
	 * 
	 * @param scale the factor of all font sizes
	 */
	public static void setPlotFontSize(float scale) {
		setPlotFontSizes(scale, scale, scale, scale);
	}

	private static float positive(float scale) {
		return scale > 0 && Float.isFinite(scale) ? scale : 1;
	}

	/**
	 * Shows or hides the action "Export Plot (WebGL, Blender)" in the toolbar of the 3D view
	 * (hidden by default). The action exports the plot of the plot API (see
	 * Plot3D) to an HTML page with WebGL.
	 * 
	 * @param visible true to show the action
	 */
	public static void setPlotExportAction(boolean visible) {
		SpatialView.setExportActionVisible(visible);
	}

	/**
	 * Sets the colors of the texts of the plot view (0xRRGGBB, -1 = the color of
	 * the lines of the 3D view): the title, the numbers of the axes, the titles of
	 * the axes and the legend with the color bar.
	 * 
	 * @param title      the color of the title
	 * @param numbers    the color of the axis numbers
	 * @param axisTitles the color of the axis titles
	 * @param legend     the color of the texts of the legend and the color bar
	 */
	public static void setPlotTextColors(int title, int numbers, int axisTitles, int legend) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotTextColors = new int[] { title, numbers, axisTitles, legend };
		}
	}

	/**
	 * Sets the font families of the texts of the plot view (e.g., "Arial",
	 * "Georgia", null = Verdana): the title, the numbers of the axes, the titles
	 * of the axes and the legend with the color bar.
	 * 
	 * @param title      the font of the title
	 * @param numbers    the font of the axis numbers
	 * @param axisTitles the font of the axis titles
	 * @param legend     the font of the legend and the color bar
	 */
	public static void setPlotFonts(String title, String numbers, String axisTitles, String legend) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotFonts = new String[] { title, numbers, axisTitles, legend };
		}
	}

	/**
	 * Removes the legend and the color bar of the 3D view.
	 */
	public static void clearLegend() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.plotLegend = null;
			struc.plotColorBar = null;
		}
	}

	/**
	 * Removes the title, the axis titles, the logarithmic axes, the number
	 * formats, the axis ranges, the legend and the color bar of the plot view.
	 */
	public static void resetPlotLabels() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.resetPlotLabels();
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
	 * Returns true while the first view of a frame is drawn: the main view (false
	 * for the small view of the split view and for the second eye of the stereo
	 * view). Something which should be done once per frame (e.g., an overlay) can
	 * be guarded with it. A step of a model is guarded already with canStep (true
	 * only in the first view of a frame).
	 * 
	 * @return true while the first view of a frame is drawn
	 */
	public static boolean isMainViewDrawing() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc == null || struc.isFirstViewPass();
	}

	/**
	 * Returns true while the small view of the split view is drawn (false without
	 * the split view). Something which should be drawn only in the main view
	 * (e.g., an overlay) can be skipped with it.
	 * 
	 * @return true while the small view is drawn
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
	 * Sets the custom camera (the position and the look at) and enables it (see
	 * the Camera tab, the check box is updated).
	 * 
	 * @param xpos  the x-position of the camera
	 * @param ypos  the y-position of the camera
	 * @param zpos  the z-position of the camera
	 * @param xlook the x look at coordinate
	 * @param ylook the y look at coordinate
	 * @param zlook the z look at coordinate
	 */
	public static void setCustomCamera(double xpos, double ypos, double zpos, double xlook, double ylook,
			double zlook) {
		SpatialStructure.setCustomCamera(xpos, ypos, zpos, xlook, ylook, zlook);
		setCustomCamera(true);
	}

	/**
	 * Enables or disables the custom camera (the check box in the Camera tab is
	 * updated).
	 * 
	 * @param enabled true to use the custom camera
	 */
	public static void setCustomCamera(boolean enabled) {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.setCustomCameraEnabled(enabled);
		}
	}

	/**
	 * Displays the custom camera in the main view: with the split view the small
	 * view shows the normal camera (see SpatialCamera for the other camera
	 * settings, e.g., SpatialCamera.copyWalkthroughToCustomCamera).
	 * 
	 * @param mainView true for the main view, false for the small view of the
	 *                 split view (the default)
	 */
	public static void setCustomCameraInMainView(boolean mainView) {
		SpatialCamera.setCustomCameraInMainView(mainView);
	}

	/**
	 * Uses the camera of the main view (see SpatialCamera.setMainCamera) instead
	 * of the camera rotated with the mouse.
	 * 
	 * @param enabled true to use the main view camera
	 */
	public static void setMainCamera(boolean enabled) {
		SpatialCamera.setMainCameraEnabled(enabled);
	}

	/**
	 * Returns true if the custom camera is used.
	 * 
	 * @return true if the custom camera is enabled
	 */
	public static boolean isCustomCamera() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null && struc.isCustomCamera();
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
	 * Triggers the fullscreen mode in the 3D view (on the primary monitor like F2).
	 * 
	 */
	public static void setFullscreen() {

		final SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {

			struc.getView().createFullscreen(0);

		}

	}
	
	/**
	 * Triggers the fullscreen mode and opens the 3D view in monitor i (0: the
	 * primary, 1: the secondary monitor, etc., like F2, F3 and F4).
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
		MeshModel model = getModel();
		return model instanceof OBJModel ? (OBJModel) model : null;
	}

	/**
	 * Returns the model of the Model tab (a mesh file: OBJ, STL, PLY or OFF).
	 * 
	 * @return the model or null
	 */
	public static MeshModel getModel() {
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		return struc != null ? struc.model : null;
	}

	/**
	 * This method returns a boolean which is triggered by the UI slider to adjust a
	 * frame rate. It is true only in the first view of a frame (not again in the
	 * small view of the split view or for the second eye of the stereo view), so a
	 * step of a model guarded with it is computed once per frame.
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
						setPlayPauseSelection(false);
					}
				});

			} else {
				grid.setFromOpenGl(true);
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						setPlayPauseSelection(true);
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

	/* The play/pause button of the Options3d view (it can be disposed, e.g., if it was moved into a custom view)! */
	private static void setPlayPauseSelection(boolean selected) {
		org.eclipse.swt.widgets.Button button = Options3d.getPlayPauseButton();
		if (button != null && !button.isDisposed()) {
			button.setSelection(selected);
		}
	}
}
