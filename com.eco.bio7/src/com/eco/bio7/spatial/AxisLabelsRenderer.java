/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
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

import java.awt.Font;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Renders coordinate axis labels with ruler-style ticks and rotated text.
 * Integrated with Bio7's Options3d and SpatialStructure for Cartesian/Octant
 * modes.
 * 
 * Features: Detects Cartesian vs Octant mode from SpatialStructure Ruler axes
 * (XY plane) only in Octant mode Billboard-style rotated text (always readable)
 * Displays positive AND negative numbers (Cartesian) or 0 to max (Octant) Major
 * and minor tick marks Automatic scaling with SpatialStructure dimensions Full
 * configuration API
 */
public class AxisLabelsRenderer {

	private TextRenderer renderer;
	private Font font;

	// Configuration for each axis
	private AxisConfig xAxisConfig;
	private AxisConfig yAxisConfig;
	private AxisConfig zAxisConfig;

	// Global settings
	private boolean enabled = false;
	private boolean showLabels = true;
	private boolean showMajorTicks = true;
	private boolean showMinorTicks = true;
	private boolean showRulerAxes = true; // Draw physical ruler axes (octant mode only)
	private float majorTickLength = 10.0f;
	private float minorTickLength = 5.0f;
	private float labelOffset = 35.0f;
	private float[] tickColor = { 0.0f, 0.0f, 0.0f, 1.0f }; // Black
	private float[] labelColor = { 0.0f, 0.0f, 0.0f, 1.0f }; // Black
	private float[] axisColor = { 0.2f, 0.2f, 0.2f, 1.0f }; // Dark gray for axes

	// Tracking for dynamic updates
	private float lastSizeX = -1;
	private float lastSizeY = -1;
	private float lastSizeZ = -1;
	private boolean lastOctantMode = false;

	/**
	 * Configuration class for each axis
	 */
	public static class AxisConfig {
		public float minValue;
		public float maxValue;
		public float labelInterval;
		public int minorTicksPerMajor;
		public int decimalPlaces;
		public boolean enabled;
		public String axisName;

		public AxisConfig(String name, float min, float max, float labelInterval, int minorTicks, int decimals) {
			this.axisName = name;
			this.minValue = min;
			this.maxValue = max;
			this.labelInterval = Math.abs(labelInterval);
			this.minorTicksPerMajor = Math.max(1, minorTicks);
			this.decimalPlaces = Math.max(0, decimals);
			this.enabled = true;
		}

		public float getMinorTickInterval() {
			return labelInterval / (float) minorTicksPerMajor;
		}

		public String formatValue(float value) {
			if (decimalPlaces == 0) {
				return String.format("%.0f", value);
			} else {
				String format = String.format("%%.%df", decimalPlaces);
				return String.format(format, value);
			}
		}

		public void updateRange(float min, float max) {
			this.minValue = min;
			this.maxValue = max;
		}
	}

	/**
	 * Default constructor
	 */
	public AxisLabelsRenderer() {
		this(0, 1000, 0, 1000, 0, 1000);
	}

	/**
	 * Constructor with specified ranges
	 */
	public AxisLabelsRenderer(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
		font = new Font("Monospaced", Font.PLAIN, 11);
		renderer = new TextRenderer(font, true, false);

		this.xAxisConfig = new AxisConfig("X", minX, maxX, 100.0f, 10, 0);
		this.yAxisConfig = new AxisConfig("Y", minY, maxY, 100.0f, 10, 0);
		this.zAxisConfig = new AxisConfig("Z", minZ, maxZ, 100.0f, 10, 0);
	}

	/**
	 * Render axis labels, ticks, and ruler axes Detects Cartesian vs Octant mode
	 * automatically from HeightMaps.isCartesian()
	 */
	public void renderAxisLabels(GL2 gl, SpatialStructure spatial) {
		if (!enabled) {
			return;
		}

		if (!showLabels && !showMajorTicks && !showMinorTicks && !showRulerAxes) {
			return;
		}

		// Get current mode from HeightMaps (which is managed by SpatialStructure)
		boolean isOctantMode = !HeightMaps.isCartesian();
		updateAxisRangesFromSpatialStructure(spatial, isOctantMode);

		// In octant mode, draw ruler axes first
		if (isOctantMode && showRulerAxes) {
			drawRulerAxes(gl, spatial);
		}

		// Draw axis labels and ticks
		if (xAxisConfig.enabled) {
			renderAxis(gl, xAxisConfig, true, false, false, isOctantMode);
		}
		if (yAxisConfig.enabled) {
			renderAxis(gl, yAxisConfig, false, true, false, isOctantMode);
		}
		if (zAxisConfig.enabled) {
			renderAxis(gl, zAxisConfig, false, false, true, isOctantMode);
		}
	}

	/**
	 * Draw ruler axes (X and Y) in octant mode Creates two perpendicular lines
	 * forming the corner of the coordinate system Positioned at the origin (0, 0,
	 * 0) in octant space
	 */
	private void drawRulerAxes(GL2 gl, SpatialStructure spatial) {
		float sizeX = spatial.getSizeX();
		float sizeY = spatial.getSizeY();

		gl.glDisable(GL2.GL_LIGHTING);
		gl.glColor4f(axisColor[0], axisColor[1], axisColor[2], axisColor[3]);
		gl.glLineWidth(2.0f);

		// Draw X-axis ruler (from origin to max X)
		gl.glBegin(GL2.GL_LINES);
		gl.glVertex3f(0, 0, 0);
		gl.glVertex3f(sizeX, 0, 0);
		gl.glEnd();

		// Draw Y-axis ruler (from origin to max Y)
		gl.glBegin(GL2.GL_LINES);
		gl.glVertex3f(0, 0, 0);
		gl.glVertex3f(0, sizeY, 0);
		gl.glEnd();

		gl.glLineWidth(1.0f);
		gl.glEnable(GL2.GL_LIGHTING);
	}

	/**
	 * Automatically update axis ranges based on SpatialStructure size and mode
	 * Called before rendering to sync with current spatial extents
	 */
	private void updateAxisRangesFromSpatialStructure(SpatialStructure spatial, boolean isOctantMode) {
		float sizeX = spatial.getSizeX();
		float sizeY = spatial.getSizeY();
		float sizeZ = spatial.getSizeZ();

		// Update X axis range
		if (lastOctantMode != isOctantMode || Math.abs(lastSizeX - sizeX) > 1.0f) {
			if (isOctantMode) {
				// Octant mode: 0 to max
				xAxisConfig.updateRange(0, sizeX);
			} else {
				// Cartesian mode: -max to +max
				xAxisConfig.updateRange(-sizeX, sizeX);
			}
			lastSizeX = sizeX;
		}

		// Update Y axis range
		if (lastOctantMode != isOctantMode || Math.abs(lastSizeY - sizeY) > 1.0f) {
			if (isOctantMode) {
				yAxisConfig.updateRange(0, sizeY);
			} else {
				yAxisConfig.updateRange(-sizeY, sizeY);
			}
			lastSizeY = sizeY;
		}

		// Update Z axis range
		if (lastOctantMode != isOctantMode || Math.abs(lastSizeZ - sizeZ) > 1.0f) {
			if (isOctantMode) {
				zAxisConfig.updateRange(0, sizeZ);
			} else {
				zAxisConfig.updateRange(-sizeZ, sizeZ);
			}
			lastSizeZ = sizeZ;
		}

		lastOctantMode = isOctantMode;
	}

	/**
	 * Render a single axis with ticks and rotated labels
	 */
	private void renderAxis(GL2 gl, AxisConfig config, boolean isXAxis, boolean isYAxis, boolean isZAxis,
			boolean isOctantMode) {
		float minorInterval = config.getMinorTickInterval();
		float startValue = (float) Math.ceil(config.minValue / minorInterval) * minorInterval;

		for (float value = startValue; value <= config.maxValue + 0.001f; value += minorInterval) {
			if (value < config.minValue - 0.001f || value > config.maxValue + 0.001f) {
				continue;
			}

			// Determine if this is a major tick (at label interval)
			boolean isMajorTick = Math.abs(value % config.labelInterval) < minorInterval * 0.1f;

			// In Cartesian mode, always show 0 as a major tick
			if (!isOctantMode && Math.abs(value) < minorInterval * 0.1f) {
				isMajorTick = true;
			}

			if (isMajorTick && showMajorTicks) {
				drawTick(gl, value, isXAxis, isYAxis, isZAxis, majorTickLength, tickColor, isOctantMode);
			} else if (!isMajorTick && showMinorTicks) {
				drawTick(gl, value, isXAxis, isYAxis, isZAxis, minorTickLength, tickColor, isOctantMode);
			}

			if (isMajorTick && showLabels) {
				drawRotatedLabel(gl, value, config, isXAxis, isYAxis, isZAxis, isOctantMode);
			}
		}
	}

	/**
	 * Draw a single tick mark Position varies based on axis and mode (Cartesian vs
	 * Octant)
	 */
	private void drawTick(GL2 gl, float value, boolean isXAxis, boolean isYAxis, boolean isZAxis, float length,
			float[] color, boolean isOctantMode) {
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glColor4f(color[0], color[1], color[2], color[3]);
		gl.glLineWidth(1.0f);
		gl.glBegin(GL2.GL_LINES);

		if (isXAxis) {
			if (isOctantMode) {
				// Octant: ticks point down from X axis
				gl.glVertex3f(value, -length, 0);
				gl.glVertex3f(value, 0, 0);
			} else {
				// Cartesian: ticks in YZ plane
				gl.glVertex3f(value, -length, -length);
				gl.glVertex3f(value, length, length);
			}
		} else if (isYAxis) {
			if (isOctantMode) {
				// Octant: ticks point left from Y axis
				gl.glVertex3f(-length, value, 0);
				gl.glVertex3f(0, value, 0);
			} else {
				// Cartesian: ticks in XZ plane
				gl.glVertex3f(-length, value, -length);
				gl.glVertex3f(length, value, length);
			}
		} else if (isZAxis) {
			if (isOctantMode) {
				// Octant: ticks point down from Z axis
				gl.glVertex3f(-length, -length, value);
				gl.glVertex3f(0, 0, value);
			} else {
				// Cartesian: ticks in XY plane
				gl.glVertex3f(-length, -length, value);
				gl.glVertex3f(length, length, value);
			}
		}

		gl.glEnd();
		gl.glEnable(GL2.GL_LIGHTING);
	}

	/**
	 * Draw a label rotated to face the camera (billboard style) Uses matrix
	 * manipulation for screen-aligned text rendering
	 */
	private void drawRotatedLabel(GL2 gl, float value, AxisConfig config, boolean isXAxis, boolean isYAxis,
			boolean isZAxis, boolean isOctantMode) {
		String label = config.formatValue(value);

		// Save the current matrix state
		gl.glPushMatrix();

		// Calculate label position based on axis
		float labelX, labelY, labelZ;

		if (isXAxis) {
			labelX = value;
			labelY = -labelOffset;
			labelZ = -labelOffset;
		} else if (isYAxis) {
			labelX = -labelOffset;
			labelY = value;
			labelZ = -labelOffset;
		} else {
			labelX = -labelOffset;
			labelY = -labelOffset;
			labelZ = value;
		}

		// Translate to label position
		gl.glTranslatef(labelX, labelY, labelZ);

		// Billboard effect: Get the current model-view matrix and zero out the rotation
		// This makes the text always face the camera
		float[] mv = new float[16];
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mv, 0);

		// Reset rotation to identity (keep translation)
		mv[0] = 1;
		mv[4] = 0;
		mv[8] = 0;
		mv[1] = 0;
		mv[5] = 1;
		mv[9] = 0;
		mv[2] = 0;
		mv[6] = 0;
		mv[10] = 1;

		gl.glLoadMatrixf(mv, 0);

		// Render the text
		renderer.begin3DRendering();
		renderer.setColor(labelColor[0], labelColor[1], labelColor[2], labelColor[3]);

		// Center text on the label position
		float textWidth = renderer.getCharWidth('0') * label.length();
		renderer.draw3D(label, -textWidth / 2f, -6, 0, 0.8f);

		renderer.end3DRendering();

		// Restore the matrix
		gl.glPopMatrix();
	}

	// ==================== Configuration Setters ====================

	public void setEnabled(boolean enable) {
		this.enabled = enable;
	}

	public void setShowRulerAxes(boolean show) {
		this.showRulerAxes = show;
	}

	public void setXAxisConfig(float interval, int minorTicks, int decimalPlaces) {
		xAxisConfig.labelInterval = Math.abs(interval);
		xAxisConfig.minorTicksPerMajor = Math.max(1, minorTicks);
		xAxisConfig.decimalPlaces = Math.max(0, decimalPlaces);
	}

	public void setYAxisConfig(float interval, int minorTicks, int decimalPlaces) {
		yAxisConfig.labelInterval = Math.abs(interval);
		yAxisConfig.minorTicksPerMajor = Math.max(1, minorTicks);
		yAxisConfig.decimalPlaces = Math.max(0, decimalPlaces);
	}

	public void setZAxisConfig(float interval, int minorTicks, int decimalPlaces) {
		zAxisConfig.labelInterval = Math.abs(interval);
		zAxisConfig.minorTicksPerMajor = Math.max(1, minorTicks);
		zAxisConfig.decimalPlaces = Math.max(0, decimalPlaces);
	}

	public void setAllAxesConfig(float interval, int minorTicks, int decimalPlaces) {
		setXAxisConfig(interval, minorTicks, decimalPlaces);
		setYAxisConfig(interval, minorTicks, decimalPlaces);
		setZAxisConfig(interval, minorTicks, decimalPlaces);
	}

	public void setXLabelInterval(float interval) {
		xAxisConfig.labelInterval = Math.abs(interval);
	}

	public void setYLabelInterval(float interval) {
		yAxisConfig.labelInterval = Math.abs(interval);
	}

	public void setZLabelInterval(float interval) {
		zAxisConfig.labelInterval = Math.abs(interval);
	}

	public void setXMinorTicks(int count) {
		xAxisConfig.minorTicksPerMajor = Math.max(1, count);
	}

	public void setYMinorTicks(int count) {
		yAxisConfig.minorTicksPerMajor = Math.max(1, count);
	}

	public void setZMinorTicks(int count) {
		zAxisConfig.minorTicksPerMajor = Math.max(1, count);
	}

	public void setXDecimalPlaces(int places) {
		xAxisConfig.decimalPlaces = Math.max(0, places);
	}

	public void setYDecimalPlaces(int places) {
		yAxisConfig.decimalPlaces = Math.max(0, places);
	}

	public void setZDecimalPlaces(int places) {
		zAxisConfig.decimalPlaces = Math.max(0, places);
	}

	public void setShowLabels(boolean show) {
		this.showLabels = show;
	}

	public void setShowMajorTicks(boolean show) {
		this.showMajorTicks = show;
	}

	public void setShowMinorTicks(boolean show) {
		this.showMinorTicks = show;
	}

	public void setMajorTickLength(float length) {
		this.majorTickLength = Math.abs(length);
	}

	public void setMinorTickLength(float length) {
		this.minorTickLength = Math.abs(length);
	}

	public void setLabelOffset(float offset) {
		this.labelOffset = Math.abs(offset);
	}

	public void setTickColor(float r, float g, float b, float a) {
		this.tickColor = new float[] { r, g, b, a };
	}

	public void setLabelColor(float r, float g, float b, float a) {
		this.labelColor = new float[] { r, g, b, a };
	}

	public void setAxisColor(float r, float g, float b, float a) {
		this.axisColor = new float[] { r, g, b, a };
	}

	public void setAxisEnabled(char axis, boolean enabled) {
		switch (Character.toUpperCase(axis)) {
		case 'X':
			xAxisConfig.enabled = enabled;
			break;
		case 'Y':
			yAxisConfig.enabled = enabled;
			break;
		case 'Z':
			zAxisConfig.enabled = enabled;
			break;
		}
	}

	// ==================== Configuration Getters ====================

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isShowRulerAxes() {
		return showRulerAxes;
	}

	public AxisConfig getXAxisConfig() {
		return xAxisConfig;
	}

	public AxisConfig getYAxisConfig() {
		return yAxisConfig;
	}

	public AxisConfig getZAxisConfig() {
		return zAxisConfig;
	}

	public float getXMin() {
		return xAxisConfig.minValue;
	}

	public float getXMax() {
		return xAxisConfig.maxValue;
	}

	public float getYMin() {
		return yAxisConfig.minValue;
	}

	public float getYMax() {
		return yAxisConfig.maxValue;
	}

	public float getZMin() {
		return zAxisConfig.minValue;
	}

	public float getZMax() {
		return zAxisConfig.maxValue;
	}

	public boolean isShowLabels() {
		return showLabels;
	}

	public boolean isShowMajorTicks() {
		return showMajorTicks;
	}

	public boolean isShowMinorTicks() {
		return showMinorTicks;
	}

	public float getMajorTickLength() {
		return majorTickLength;
	}

	public float getMinorTickLength() {
		return minorTickLength;
	}

	public float getLabelOffset() {
		return labelOffset;
	}

	public float[] getTickColor() {
		return tickColor;
	}

	public float[] getLabelColor() {
		return labelColor;
	}

	public float[] getAxisColor() {
		return axisColor;
	}

	public boolean isAxisEnabled(char axis) {
		switch (Character.toUpperCase(axis)) {
		case 'X':
			return xAxisConfig.enabled;
		case 'Y':
			return yAxisConfig.enabled;
		case 'Z':
			return zAxisConfig.enabled;
		default:
			return false;
		}
	}

	public void dispose() {
		if (renderer != null) {
			renderer.dispose();
		}
	}
}