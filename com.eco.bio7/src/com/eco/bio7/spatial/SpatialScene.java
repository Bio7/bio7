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

import java.awt.Font;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class SpatialScene {
	private TextRenderer renderer;
	private Font font;
	private float colorCubeLines[] = { 0, 0, 0, 1 };// default background
													// colours

	public SpatialScene() {
		font = new Font("Verdana", Font.BOLD, 22);
		renderer = new TextRenderer(font, true, false);
	}

	public void showAxes(GL2 gl, SpatialStructure grid) {
		/* The x and y axes are as long as a displayed texture! */
		float sizeX = grid.getAxisSizeX();
		float sizeY = grid.getAxisSizeY();
		float sizeZ = grid.getAxisSizeZ();
		if (grid.showAxes) {
			setAxesLines(gl, grid);
			/* The letters are scaled to a displayed texture like the axis numbers! */
			float letterScale = AxisLabelsRenderer.getSizeFactor(grid);

			// Save original matrix
			float[] originalMV = new float[16];
			gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, originalMV, 0);

			// X Axis Label
			gl.glPushMatrix();
			gl.glTranslatef(sizeX + 10 * letterScale, 0, 0);
			applyBillboardRotation(gl, originalMV);
			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], colorCubeLines[3]);
			renderer.draw3D("X", 0, 0, 0, letterScale);
			renderer.end3DRendering();
			gl.glPopMatrix();

			// Y Axis Label
			gl.glPushMatrix();
			gl.glTranslatef(0, sizeY + 10 * letterScale, 0);
			applyBillboardRotation(gl, originalMV);
			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], colorCubeLines[3]);
			renderer.draw3D("Y", 0, 0, 0, letterScale);
			renderer.end3DRendering();
			gl.glPopMatrix();

			// Z Axis Label
			gl.glPushMatrix();
			gl.glTranslatef(0, 0, sizeZ + 10 * letterScale);
			applyBillboardRotation(gl, originalMV);
			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], colorCubeLines[3]);
			renderer.draw3D("Z", 0, 0, 0, letterScale);
			renderer.end3DRendering();
			gl.glPopMatrix();
		}
	}

	/**
	 * Applies a billboard rotation that makes text face the camera. Extracts the
	 * camera's rotation and applies the inverse.
	 * 
	 * @param gl         the OpenGL context
	 * @param originalMV the original modelview matrix before translation
	 */
	private void applyBillboardRotation(GL2 gl, float[] originalMV) {
		// Extract scale from the original matrix
		float scaleX = (float) Math
				.sqrt(originalMV[0] * originalMV[0] + originalMV[1] * originalMV[1] + originalMV[2] * originalMV[2]);
		float scaleY = (float) Math
				.sqrt(originalMV[4] * originalMV[4] + originalMV[5] * originalMV[5] + originalMV[6] * originalMV[6]);
		float scaleZ = (float) Math
				.sqrt(originalMV[8] * originalMV[8] + originalMV[9] * originalMV[9] + originalMV[10] * originalMV[10]);

		// Create billboard rotation matrix (transpose of normalized rotation)
		float[] billboardRot = new float[16];
		billboardRot[0] = originalMV[0] / scaleX;
		billboardRot[1] = originalMV[4] / scaleY;
		billboardRot[2] = originalMV[8] / scaleZ;
		billboardRot[3] = 0;

		billboardRot[4] = originalMV[1] / scaleX;
		billboardRot[5] = originalMV[5] / scaleY;
		billboardRot[6] = originalMV[9] / scaleZ;
		billboardRot[7] = 0;

		billboardRot[8] = originalMV[2] / scaleX;
		billboardRot[9] = originalMV[6] / scaleY;
		billboardRot[10] = originalMV[10] / scaleZ;
		billboardRot[11] = 0;

		billboardRot[12] = 0;
		billboardRot[13] = 0;
		billboardRot[14] = 0;
		billboardRot[15] = 1;

		gl.glMultMatrixf(billboardRot, 0);
	}

	public void setAxesLines(GL2 gl, SpatialStructure grid) {
		float sizeX = grid.getAxisSizeX();
		float sizeY = grid.getAxisSizeY();
		float sizeZ = grid.getAxisSizeZ();
		/*
		 * Cartesian: the axes from -size to +size (like the axis numbers, see
		 * AxisLabelsRenderer), octant: from the origin to +size!
		 */
		float start = HeightMaps.isCartesian() ? -1 : 0;

		gl.glBegin(GL.GL_LINES);

		// x
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(start * sizeX, 0, 0);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(sizeX, 0, 0);
		/* y */
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, start * sizeY, 0);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, sizeY, 0);
		/* z */
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, 0, start * sizeZ);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, 0, sizeZ);

		gl.glEnd();

	}

	public void drawGrid(GL2 gl, SpatialStructure grid) {
		if (grid.showGrid) {
			/* The XY extent is as large as a displayed texture! */
			float sizeX = grid.getAxisSizeX();
			float sizeY = grid.getAxisSizeY();
			float sizeZ = grid.getAxisSizeZ();
			float gridSize = grid.getGridSize();
			gl.glBegin(GL.GL_LINES);
			gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
			/* Horizontal lines. */
			if (grid.layerXYGrid) {
				for (float i = 0; i <= sizeY; i = i + gridSize) {
					gl.glVertex2f(0, i);
					gl.glVertex2f(sizeX, i);
				}
				/* Vertical lines. */
				for (float i = 0; i <= sizeX; i = i + gridSize) {
					gl.glVertex2f(i, 0);
					gl.glVertex2f(i, sizeY);
				}

				for (float i = 0; i >= -sizeY; i = i - gridSize) {
					gl.glVertex2f(0, i);
					gl.glVertex2f(sizeX, i);
				}

				for (float i = 0; i <= sizeX; i = i + gridSize) {
					gl.glVertex2f(i, 0);
					gl.glVertex2f(i, -sizeY);
				}
				/* back-left */
				for (float i = 0; i >= -sizeY; i = i - gridSize) {
					gl.glVertex2f(0, i);
					gl.glVertex2f(-sizeX, i);
				}
				/* Vertical lines. */
				for (float i = 0; i >= -sizeX; i = i - gridSize) {
					gl.glVertex2f(i, 0);
					gl.glVertex2f(i, -sizeY);
				}
				/* front-left */
				for (float i = 0; i <= sizeY; i = i + gridSize) {
					gl.glVertex2f(0, i);
					gl.glVertex2f(-sizeX, i);
				}
				/* Vertical lines. */
				for (float i = 0; i >= -sizeX; i = i - gridSize) {
					gl.glVertex2f(i, 0);
					gl.glVertex2f(i, sizeY);
				}

			}

			/** **************************************************** */
			if (grid.layerXZGrid) {
				for (float i = 0; i <= sizeZ; i = i + gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(sizeX, 0, i);
				}
				/* Vertical lines. */
				for (float i = 0; i <= sizeX; i = i + gridSize) {
					gl.glVertex3f(i, 0, 0);
					gl.glVertex3f(i, 0, sizeZ);
				}

				for (float i = 0; i >= -sizeZ; i = i - gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(sizeX, 0, i);
				}

				for (float i = 0; i <= sizeX; i = i + gridSize) {
					gl.glVertex3f(i, 0, 0);
					gl.glVertex3f(i, 0, -sizeZ);
				}
				/* back-left */
				for (float i = 0; i >= -sizeZ; i = i - gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(-sizeX, 0, i);
				}
				/* Vertical lines. */
				for (float i = 0; i >= -sizeX; i = i - gridSize) {
					gl.glVertex3f(i, 0, 0);
					gl.glVertex3f(i, 0, -sizeZ);
				}
				/* front-left */
				for (float i = 0; i <= sizeZ; i = i + gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(-sizeX, 0, i);
				}
				/* Vertical lines. */
				for (float i = 0; i >= -sizeX; i = i - gridSize) {
					gl.glVertex3f(i, 0, 0);
					gl.glVertex3f(i, 0, sizeZ);
				}
			}
			/** ************************************************** */
			if (grid.layerYZGrid) {
				for (float i = 0; i <= sizeZ; i = i + gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(0, sizeY, i);
				}
				/* Vertical lines. */
				for (float i = 0; i <= sizeY; i = i + gridSize) {
					gl.glVertex3f(0, i, 0);
					gl.glVertex3f(0, i, sizeZ);
				}

				for (float i = 0; i >= -sizeZ; i = i - gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(0, sizeY, i);
				}
				/* Vertical lines. */
				for (float i = 0; i <= sizeY; i = i + gridSize) {
					gl.glVertex3f(0, i, 0);
					gl.glVertex3f(0, i, -sizeZ);
				}

				for (float i = 0; i <= sizeZ; i = i + gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(0, -sizeY, i);
				}
				/* Vertical lines. */
				for (float i = 0; i >= -sizeY; i = i - gridSize) {
					gl.glVertex3f(0, i, 0);
					gl.glVertex3f(0, i, sizeZ);
				}

				for (float i = 0; i >= -sizeZ; i = i - gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(0, -sizeY, i);
				}
				/* Vertical lines. */
				for (float i = 0; i >= -sizeY; i = i - gridSize) {
					gl.glVertex3f(0, i, 0);
					gl.glVertex3f(0, i, -sizeZ);
				}

			}

			/*
			 * gl.glBegin(GL.GL_LINES); gl.glColor3ui(0, 0, 0); Horizontal lines. for (float
			 * i=-sizeY; i<=sizeY; i=i+gridSize) { gl.glVertex2f(-sizeX, i);
			 * gl.glVertex2f(sizeX, i); } Vertical lines. for (float i=-sizeX; i<=sizeX;
			 * i=i+gridSize) { gl.glVertex2f(i, -sizeY); gl.glVertex2f(i, sizeY); }
			 */

			gl.glEnd();
		}
	}

	public void drawGrid2(GL2 gl, SpatialStructure grid) {
		/* The XY extent is as large as a displayed texture! */
		float sizeX = grid.getAxisSizeX();
		float sizeY = grid.getAxisSizeY();
		float sizeZ = grid.getAxisSizeZ();
		float gridSize = grid.getGridSize();
		if (grid.showGrid) {
			gl.glBegin(GL.GL_LINES);
			/* Horizontal lines. */
			gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
			if (grid.layerXYGrid) {
				for (float i = 0; i <= sizeY; i = i + gridSize) {
					gl.glVertex2f(0, i);
					gl.glVertex2f(sizeX, i);
				}
				/* Vertical lines. */
				for (float i = 0; i <= sizeX; i = i + gridSize) {
					gl.glVertex2f(i, 0);
					gl.glVertex2f(i, sizeY);
				}
			}
			if (grid.layerXZGrid) {
				for (float i = 0; i <= sizeZ; i = i + gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(sizeX, 0, i);
				}
				/* Vertical lines. */
				for (float i = 0; i <= sizeX; i = i + gridSize) {
					gl.glVertex3f(i, 0, 0);
					gl.glVertex3f(i, 0, sizeZ);
				}
			}
			/* Inside of glBegin (and only if the grid is shown) like the other grids! */
			if (grid.layerYZGrid) {
				for (float i = 0; i <= sizeZ; i = i + gridSize) {
					gl.glVertex3f(0, 0, i);
					gl.glVertex3f(0, sizeY, i);
				}
				/* Vertical lines. */
				for (float i = 0; i <= sizeY; i = i + gridSize) {
					gl.glVertex3f(0, i, 0);
					gl.glVertex3f(0, i, sizeZ);
				}
			}
			gl.glEnd();
		}

	}

	/* The box fitted to the displayed rendering (see getSceneBounds)! */
	void drawQuadLines(GL2 gl, SpatialStructure grid) {
		double[] b = grid.getSceneBounds();
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		for (int k = 0; k < 2; k++) {
			double z = b[4 + k];
			gl.glBegin(GL.GL_LINE_LOOP);
			gl.glVertex3d(b[0], b[2], z);
			gl.glVertex3d(b[1], b[2], z);
			gl.glVertex3d(b[1], b[3], z);
			gl.glVertex3d(b[0], b[3], z);
			gl.glEnd();
		}
		gl.glBegin(GL.GL_LINES);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				gl.glVertex3d(b[i], b[2 + j], b[4]);
				gl.glVertex3d(b[i], b[2 + j], b[5]);
			}
		}
		gl.glEnd();
	}

	/**
	 * Draws the grids at the far sides of the box (XY at z max, XZ at y max, YZ at
	 * x max). Not in a plot view (see drawPlotView).
	 */
	public void drawGridCage(GL2 gl, SpatialStructure grid) {
		if (grid.plotView || !grid.showGrid
				|| !(grid.layerXYGridMax || grid.layerXZGridMax || grid.layerYZGridMax)) {
			return;
		}
		double[] b = grid.getSceneBounds();
		float step = grid.getGridSize();
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glBegin(GL.GL_LINES);
		/* The fixed axis: 0 = x (YZ grid), 1 = y (XZ grid), 2 = z (XY grid) */
		if (grid.layerYZGridMax) {
			drawFaceGrid(gl, 0, b[1], b, step);
		}
		if (grid.layerXZGridMax) {
			drawFaceGrid(gl, 1, b[3], b, step);
		}
		if (grid.layerXYGridMax) {
			drawFaceGrid(gl, 2, b[5], b, step);
		}
		gl.glEnd();
	}

	/**
	 * Draws a plot view of the box fitted to the displayed rendering (like a 3D
	 * plot): the grids at the three sides of the box behind the data (seen from the
	 * camera, e.g., left, back and bottom) and the axes with ticks and numbers at
	 * the front edges of the floor (the z axis at the front left edge).
	 * 
	 * @param numbers true to draw the ticks and numbers of the axes
	 */
	public void drawPlotView(GL2 gl, SpatialStructure grid, boolean numbers) {
		double[] b = grid.getSceneBounds();
		float[] mv = new float[16];
		float[] pr = new float[16];
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mv, 0);
		gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, pr, 0);
		double[] eye = eyePosition(mv);
		/* The sides behind the data: the sides of the box farther from the camera */
		double[] back = new double[3];
		double[] front = new double[3];
		for (int k = 0; k < 3; k++) {
			boolean minIsBack = eye == null || eye[k] > (b[2 * k] + b[2 * k + 1]) / 2;
			back[k] = minIsBack ? b[2 * k] : b[2 * k + 1];
			front[k] = minIsBack ? b[2 * k + 1] : b[2 * k];
		}
		/* The positions of the ticks: the grid lines are at the numbers of the axes */
		double[][] ticks = { tickPositions(grid, 0, b), tickPositions(grid, 1, b), tickPositions(grid, 2, b) };
		if (grid.showGrid) {
			gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
			gl.glBegin(GL.GL_LINES);
			if (grid.plotGridYZ) {
				drawFaceLines(gl, 0, back[0], b, ticks);
			}
			if (grid.plotGridXZ) {
				drawFaceLines(gl, 1, back[1], b, ticks);
			}
			if (grid.plotGridXY) {
				drawFaceLines(gl, 2, back[2], b, ticks);
			}
			gl.glEnd();
			/* The minor lines of the logarithmic axes (2, 3, ..., 9 times the powers of ten), lighter */
			double[][] minor = { minorLogPositions(grid, 0, b), minorLogPositions(grid, 1, b), minorLogPositions(grid, 2, b) };
			if (minor[0].length + minor[1].length + minor[2].length > 0) {
				gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_CURRENT_BIT);
				gl.glEnable(GL.GL_BLEND);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				gl.glColor4f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], 0.25f);
				gl.glBegin(GL.GL_LINES);
				if (grid.plotGridYZ) {
					drawMinorLines(gl, 0, back[0], b, minor);
				}
				if (grid.plotGridXZ) {
					drawMinorLines(gl, 1, back[1], b, minor);
				}
				if (grid.plotGridXY) {
					drawMinorLines(gl, 2, back[2], b, minor);
				}
				gl.glEnd();
				gl.glPopAttrib();
			}
		}
		/* The z axis at the vertical front edge which is left on the screen */
		double[] zA = { front[0], back[1] };
		double[] zB = { back[0], front[1] };
		double[] zEdge = screenX(mv, pr, zA[0], zA[1], back[2]) <= screenX(mv, pr, zB[0], zB[1], back[2]) ? zA : zB;
		/* The axes: x at the front y, y at the front x (on the floor), z at the front left edge */
		double[][] start = { { b[0], front[1], back[2] }, { front[0], b[2], back[2] }, { zEdge[0], zEdge[1], b[4] } };
		double[][] end = { { b[1], front[1], back[2] }, { front[0], b[3], back[2] }, { zEdge[0], zEdge[1], b[5] } };
		/* The direction of the ticks and numbers: away from the box */
		double size = Math.max(b[1] - b[0], Math.max(b[3] - b[2], b[5] - b[4]));
		double tick = size * 0.02;
		double[][] out = { { 0, Math.signum(front[1] - back[1]), 0 }, { Math.signum(front[0] - back[0]), 0, 0 },
				{ Math.signum(zEdge[0] - (b[0] + b[1]) / 2), Math.signum(zEdge[1] - (b[2] + b[3]) / 2), 0 } };
		gl.glLineWidth(1.5f);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glBegin(GL.GL_LINES);
		for (int k = 0; k < 3; k++) {
			gl.glVertex3d(start[k][0], start[k][1], start[k][2]);
			gl.glVertex3d(end[k][0], end[k][1], end[k][2]);
		}
		gl.glEnd();
		gl.glLineWidth(1);
		float sizeFactor = AxisLabelsRenderer.getSizeFactor(grid);
		String[] names = { "X", "Y", "Z" };
		String[] titles = grid.plotAxisTitles;
		String[] formats = grid.plotNumberFormats;
		/* The font sizes (factors) of the axis titles and the numbers */
		float[] fontScales = grid.plotFontScales;
		float titleScaleFactor = fontScales != null ? fontScales[1] : 1, numberScale = fontScales != null ? fontScales[2] : 1;
		for (int k = 0; k < 3; k++) {
			double min = b[2 * k], max = b[2 * k + 1];
			if (max <= min) {
				continue;
			}
			double labelDistance = 4;
			if (numbers) {
				double valueA = plotValue(grid, k, min, b), valueB = plotValue(grid, k, max, b);
				double step = tickStep(grid, k, Math.abs(valueB - valueA));
				boolean log = isLogAxis(grid, k);
				double[] minor = minorLogPositions(grid, k, b);
				gl.glBegin(GL.GL_LINES);
				for (double position : ticks[k]) {
					double[] p = start[k].clone();
					p[k] = position;
					gl.glVertex3d(p[0], p[1], p[2]);
					gl.glVertex3d(p[0] + out[k][0] * tick, p[1] + out[k][1] * tick, p[2] + out[k][2] * tick);
				}
				/* The minor ticks of a logarithmic axis (half as long) */
				for (double position : minor) {
					double[] p = start[k].clone();
					p[k] = position;
					gl.glVertex3d(p[0], p[1], p[2]);
					gl.glVertex3d(p[0] + out[k][0] * tick * 0.5, p[1] + out[k][1] * tick * 0.5, p[2] + out[k][2] * tick * 0.5);
				}
				gl.glEnd();
				String format = formats != null && k < formats.length ? formats[k] : null;
				int longest = 0;
				for (double position : ticks[k]) {
					double[] p = start[k].clone();
					p[k] = position;
					String[] categories = categories(grid, k);
					String text;
					if (categories != null) {
						int c = (int) Math.round(plotValue(grid, k, position, b));
						text = c >= 0 && c < categories.length ? categories[c] : "";
					} else {
						double v = Math.round(plotValue(grid, k, position, b) / step) * step;
						text = log ? formatLogValue(v, step, format) : formatPlotValue(v, step, format);
					}
					longest = Math.max(longest, text.length());
					drawPlotText(gl, text, p[0] + out[k][0] * tick * 4 * numberScale, p[1] + out[k][1] * tick * 4 * numberScale,
							p[2] + out[k][2] * tick * 4 * numberScale, 0.5f * sizeFactor * numberScale, textColor(grid.plotTextColors, 1),
							family(grid.plotFonts, 1));
				}
				/* Long numbers (e.g., 1.34e10): the title of the axis farther away */
				labelDistance = (10 + Math.max(0, longest - 4) * 0.8) * numberScale + 4 * (titleScaleFactor - 1);
			}
			if (grid.showAxes) {
				double[] p = start[k].clone();
				p[k] = (min + max) / 2;
				double d = tick * (numbers ? labelDistance : 3);
				/* null: X, Y, Z, an empty title: no title */
				String name = titles != null && k < titles.length && titles[k] != null ? titles[k] : names[k];
				if (name.isBlank()) {
					continue;
				}
				/* A long title is smaller */
				float titleScale = name.length() > 3 ? 0.62f : 0.7f;
				drawPlotText(gl, name, p[0] + out[k][0] * d, p[1] + out[k][1] * d, p[2] + out[k][2] * d,
						titleScale * sizeFactor * titleScaleFactor, textColor(grid.plotTextColors, 2), family(grid.plotFonts, 2));
			}
		}
	}

	/* The categories of an axis (only for the data of a model) or null */
	static String[] categories(SpatialStructure grid, int axis) {
		String[][] c = grid.plotAxisCategories;
		return c != null && axis < c.length && c[axis] != null && grid.getTextureExtent() == null ? c[axis] : null;
	}

	/* True if the axis is logarithmic (only for the data of a model, not for images) */
	private static boolean isLogAxis(SpatialStructure grid, int axis) {
		boolean[] log = grid.plotLogAxes;
		return log != null && axis < log.length && log[axis] && grid.getTextureExtent() == null;
	}

	/*
	 * The step of the ticks of an axis: 1, 2 or 5 times a power of ten (a
	 * logarithmic axis with more than one decade: whole decades)
	 */
	private static double tickStep(SpatialStructure grid, int axis, double span) {
		double step = niceStep(span / 6);
		if (isLogAxis(grid, axis) && span >= 1) {
			step = Math.max(1, Math.round(step));
		}
		return step;
	}

	/*
	 * The positions of the minor ticks of a logarithmic axis: log10(k * 10^n), k =
	 * 2 ... 9 (if the ticks are whole decades and there are not too many decades)
	 */
	private static double[] minorLogPositions(SpatialStructure grid, int axis, double[] b) {
		double min = b[2 * axis], max = b[2 * axis + 1];
		if (!isLogAxis(grid, axis) || max <= min) {
			return new double[0];
		}
		/* The log10 values at the bounds */
		double valueA = plotValue(grid, axis, min, b), valueB = plotValue(grid, axis, max, b);
		double from = Math.min(valueA, valueB), to = Math.max(valueA, valueB);
		if (to - from > 12 || tickStep(grid, axis, to - from) != 1) {
			return new double[0];
		}
		java.util.List<Double> positions = new java.util.ArrayList<>();
		for (int n = (int) Math.floor(from); n < to; n++) {
			for (int k = 2; k < 10; k++) {
				double v = n + Math.log10(k);
				if (v > from && v < to) {
					positions.add(min + (v - valueA) / (valueB - valueA) * (max - min));
				}
			}
		}
		return positions.stream().mapToDouble(Double::doubleValue).toArray();
	}

	/* The minor grid lines (see minorLogPositions) of a side of the box! */
	private static void drawMinorLines(GL2 gl, int axis, double value, double[] b, double[][] minor) {
		int u = (axis + 1) % 3, v = (axis + 2) % 3;
		double[] p = new double[3];
		p[axis] = value;
		for (int k = 0; k < 2; k++) {
			int along = k == 0 ? u : v, across = k == 0 ? v : u;
			for (double s : minor[along]) {
				p[along] = s;
				p[across] = b[2 * across];
				gl.glVertex3d(p[0], p[1], p[2]);
				p[across] = b[2 * across + 1];
				gl.glVertex3d(p[0], p[1], p[2]);
			}
		}
	}

	/* The positions of the ticks (the round numbers) of an axis of the plot view! */
	private static double[] tickPositions(SpatialStructure grid, int axis, double[] b) {
		double min = b[2 * axis], max = b[2 * axis + 1];
		if (max <= min) {
			return new double[0];
		}
		double valueA = plotValue(grid, axis, min, b), valueB = plotValue(grid, axis, max, b);
		if (valueA == valueB) {
			return new double[0];
		}
		double step = tickStep(grid, axis, Math.abs(valueB - valueA));
		double from = Math.min(valueA, valueB), to = Math.max(valueA, valueB);
		java.util.List<Double> positions = new java.util.ArrayList<>();
		/* The categories at 0, 1, 2, ... */
		String[] categories = categories(grid, axis);
		if (categories != null) {
			for (int i = 0; i < categories.length; i++) {
				if (i >= from - 1e-9 && i <= to + 1e-9) {
					positions.add(min + (i - valueA) / (valueB - valueA) * (max - min));
				}
			}
			return positions.stream().mapToDouble(Double::doubleValue).toArray();
		}
		for (double v = Math.ceil(from / step) * step; v <= to + step * 1e-6 && positions.size() < 1000; v += step) {
			positions.add(min + (v - valueA) / (valueB - valueA) * (max - min));
		}
		return positions.stream().mapToDouble(Double::doubleValue).toArray();
	}

	/* The grid lines of a side of the box at the ticks of the two other axes and the borders! */
	private static void drawFaceLines(GL2 gl, int axis, double value, double[] b, double[][] ticks) {
		int u = (axis + 1) % 3, v = (axis + 2) % 3;
		double[] p = new double[3];
		p[axis] = value;
		for (int k = 0; k < 2; k++) {
			int along = k == 0 ? u : v, across = k == 0 ? v : u;
			double[] lines = java.util.stream.DoubleStream
					.concat(java.util.stream.DoubleStream.of(b[2 * along], b[2 * along + 1]), java.util.Arrays.stream(ticks[along]))
					.toArray();
			for (double s : lines) {
				p[along] = s;
				p[across] = b[2 * across];
				gl.glVertex3d(p[0], p[1], p[2]);
				p[across] = b[2 * across + 1];
				gl.glVertex3d(p[0], p[1], p[2]);
			}
		}
	}

	/*
	 * The displayed value of a position of an axis: the pixel coordinates of a
	 * texture or stack (y from the top like in ImageJ) or the coordinates of the
	 * calibrated ImageJ image of the texture or stack (origin, pixel size, inverted
	 * y, the z of the slices, see AxisLabelsRenderer.textureCalibration) and the
	 * heights of a height map, else the position!
	 */
	private static double plotValue(SpatialStructure grid, int axis, double position, double[] b) {
		float[] extent = grid.getTextureExtent();
		boolean image = extent != null;
		/* The values of the data at the bounds (see SpatialUtil.setAxisRanges) */
		double[] ranges = grid.plotAxisRanges;
		if (!image && ranges != null && b[2 * axis + 1] > b[2 * axis]) {
			double t = (position - b[2 * axis]) / (b[2 * axis + 1] - b[2 * axis]);
			return ranges[2 * axis] + t * (ranges[2 * axis + 1] - ranges[2 * axis]);
		}
		if (axis == 0) {
			if (!image) {
				return position;
			}
			ij.measure.Calibration cal = AxisLabelsRenderer.textureCalibration(grid);
			return cal != null ? cal.getX(position - b[0]) : position - b[0];
		}
		if (axis == 1) {
			if (!image) {
				return position;
			}
			double row = HeightMaps.isFlipVertically() ? position - b[2] : b[3] - position;
			ij.measure.Calibration cal = AxisLabelsRenderer.textureCalibration(grid);
			return cal != null ? cal.getY(row, Math.round(extent[1])) : row;
		}
		boolean heightMap = grid.heightMap && grid.textures != null && grid.textures.showTexture
				&& grid.textures.tex != null;
		if (heightMap && SpatialStructure.hMap != null && SpatialStructure.hMap.getScale() != 0) {
			return position / SpatialStructure.hMap.getScale() * (HeightMaps.isInverse() ? -1 : 1);
		}
		if (image) {
			/* A stack (volume, iso surface): the calibrated z of the slices (z in units of the pixel width) */
			ij.measure.Calibration cal = grid.getStackCalibration() != null ? AxisLabelsRenderer.textureCalibration(grid) : null;
			if (cal != null && cal.pixelWidth > 0 && cal.pixelDepth > 0) {
				return cal.getZ((position - b[4]) * cal.pixelWidth / cal.pixelDepth);
			}
			return position - b[4];
		}
		return position;
	}

	/* A step of 1, 2 or 5 times a power of ten! */
	static double niceStep(double value) {
		if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
			return 1;
		}
		double power = Math.pow(10, Math.floor(Math.log10(value)));
		double f = value / power;
		return (f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10) * power;
	}

	/*
	 * A number of an axis with the step of the ticks: very large or small numbers
	 * in a scientific notation (e.g., 1.34e10, 2.5e-6), else decimal. A format
	 * (java.util.Formatter) is used if available.
	 */
	static String formatPlotValue(double value, double step, String format) {
		if (Math.abs(value) < step * 1e-6) {
			value = 0;
		}
		String formatted = customFormat(value, format);
		if (formatted != null) {
			return formatted;
		}
		double largest = Math.max(Math.abs(value), step);
		if (value != 0 && (largest >= 1e5 || step < 1e-3)) {
			/* The decimals of the mantissa which distinguish the ticks */
			int exponent = (int) Math.floor(Math.log10(Math.abs(value)));
			int decimals = Math.max(0, Math.min(6, exponent - (int) Math.floor(Math.log10(step) + 1e-9)));
			return scientific(value, decimals);
		}
		int decimals = step >= 1 ? 0 : (int) Math.ceil(-Math.log10(step) - 1e-9);
		return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
	}

	/*
	 * A number of a logarithmic axis (the log10 value v): the value 10^v, e.g.,
	 * 0.01, 1, 1000, 1e5, 1.34e10.
	 */
	static String formatLogValue(double v, double step, String format) {
		double value = Math.pow(10, v);
		/* Beyond the range of a double (e.g., 1e400): the exponent */
		if (Double.isInfinite(value) || value == 0) {
			String exponent = Math.abs(v - Math.rint(v)) < 1e-9 ? String.valueOf((long) Math.rint(v))
					: String.format(java.util.Locale.ROOT, "%.2f", v);
			return POWER_OF_TEN.equals(format) ? "10^{" + exponent + "}" : "1e" + exponent;
		}
		String formatted = customFormat(value, format);
		if (formatted != null) {
			return formatted;
		}
		/* The significant digits: the decades exactly, else enough to distinguish the ticks */
		int digits = step >= 1 && Math.abs(v - Math.rint(v)) < 1e-9 ? 1 : Math.max(2, Math.min(6, 1 + (int) Math.ceil(-Math.log10(step * 2.3) + 1e-9)));
		if (value >= 1e5 || value < 1e-3) {
			return scientific(value, digits - 1);
		}
		java.math.BigDecimal rounded = new java.math.BigDecimal(value).round(new java.math.MathContext(digits));
		return rounded.stripTrailingZeros().toPlainString();
	}

	/* The scientific notation with a number of decimals (trailing zeros removed), e.g., 1.34e10 */
	static String scientific(double value, int decimals) {
		if (value == 0) {
			return "0";
		}
		int exponent = (int) Math.floor(Math.log10(Math.abs(value)));
		double mantissa = value / Math.pow(10, exponent);
		String m = String.format(java.util.Locale.ROOT, "%." + decimals + "f", mantissa);
		/* Rounded to 10 (e.g., 9.996): the next exponent */
		if (Math.abs(Double.parseDouble(m)) >= 10) {
			exponent++;
			m = String.format(java.util.Locale.ROOT, "%." + decimals + "f", mantissa / 10);
		}
		if (m.contains(".")) {
			m = m.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return m + "e" + exponent;
	}

	/* The format of the numbers as powers of ten with superscripts, e.g., 1.3x10^{10} */
	static final String POWER_OF_TEN = "10^";

	/* A number as a power of ten (with the markup of a superscript), e.g., 10^{3}, 1.34x10^{10} */
	static String powerOfTen(double value) {
		if (value == 0 || Double.isNaN(value) || Double.isInfinite(value)) {
			return value == 0 ? "0" : String.valueOf(value);
		}
		String s = scientific(value, 2);
		int e = s.indexOf('e');
		String mantissa = s.substring(0, e), exponent = s.substring(e + 1);
		if (mantissa.equals("1") || mantissa.equals("-1")) {
			return (mantissa.startsWith("-") ? "-" : "") + "10^{" + exponent + "}";
		}
		return mantissa + "\u00D710^{" + exponent + "}";
	}

	/* The number with a format of java.util.Formatter or null (no or an invalid format) */
	private static String customFormat(double value, String format) {
		if (format == null || format.isBlank()) {
			return null;
		}
		if (format.equals(POWER_OF_TEN)) {
			return powerOfTen(value);
		}
		try {
			return String.format(java.util.Locale.ROOT, format, value);
		} catch (java.util.IllegalFormatException e) {
			return null;
		}
	}

	/* The fonts of the legends (created with the scale of the view) */
	private TextRenderer legendRenderer, legendTitleRenderer;
	private float legendScale;
	private String legendFamily;

	/**
	 * Draws the legend of categories (top left) and the color bar (right side
	 * below the axis orientation overlay) on a panel with the background color.
	 */
	public void drawLegends(GL2 gl, PlotLegend legend, PlotLegend colorBar, int width, int height, float pixelScale, float fontScale) {
		/* The size of the fonts and the layout: the scale of the screen and the font size */
		float screenScale = pixelScale;
		pixelScale *= fontScale;
		String legendFont = family(fonts, 3);
		if (legendRenderer == null || legendScale != pixelScale || !legendFont.equals(legendFamily)) {
			if (legendRenderer != null) {
				legendRenderer.dispose();
				legendTitleRenderer.dispose();
			}
			legendScale = pixelScale;
			legendFamily = legendFont;
			legendRenderer = new TextRenderer(new Font(legendFont, Font.PLAIN, Math.max(8, Math.round(12 * pixelScale))), true, false);
			legendTitleRenderer = new TextRenderer(new Font(legendFont, Font.BOLD, Math.max(8, Math.round(12 * pixelScale))), true, false);
		}
		float[] background = new float[4];
		gl.glGetFloatv(GL.GL_COLOR_CLEAR_VALUE, background, 0);
		gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(0, width, 0, height, -1, 1);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glViewport(0, 0, width, height);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_CULL_FACE);
		for (int plane = 0; plane < 6; plane++) {
			gl.glDisable(GL2.GL_CLIP_PLANE0 + plane);
		}
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glShadeModel(GL2.GL_SMOOTH);
		float s = pixelScale;
		double lineHeight = legendRenderer.getBounds("Ag").getHeight();
		int margin = Math.round(10 * screenScale), pad = Math.round(8 * s), gap = Math.round(6 * s);
		if (legend != null) {
			/* The size of the panel */
			int swatch = (int) Math.round(lineHeight);
			double textWidth = legend.title != null ? mathWidth(legendTitleRenderer, parseMath(legend.title)) : 0;
			for (String label : legend.labels) {
				textWidth = Math.max(textWidth, swatch + gap + mathWidth(legendRenderer, parseMath(label)));
			}
			double rowHeight = lineHeight * 1.45;
			int rows = legend.labels.length + (legend.title != null ? 1 : 0);
			int w = (int) Math.ceil(textWidth) + 2 * pad, h = (int) Math.ceil(rows * rowHeight) + 2 * pad - (int) (rowHeight - lineHeight);
			/* Below the title of the plot view */
			int x0 = margin, y1 = height - margin - titleHeight;
			panel(gl, x0, y1 - h, w, h, background);
			/* The color boxes */
			double y = y1 - pad - (legend.title != null ? rowHeight : 0);
			for (int i = 0; i < legend.labels.length; i++) {
				PlotLegend.setColor(gl, legend.colors[i]);
				int yb = (int) Math.round(y - lineHeight);
				gl.glRecti(x0 + pad, yb, x0 + pad + swatch, yb + swatch);
				gl.glColor4f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], 0.6f);
				rectangle(gl, x0 + pad, yb, swatch, swatch);
				y -= rowHeight;
			}
			/* The texts */
			y = y1 - pad;
			if (legend.title != null) {
				text(legendTitleRenderer, legend.title, x0 + pad, (int) Math.round(y - lineHeight * 0.8), width, height);
				y -= rowHeight;
			}
			for (String label : legend.labels) {
				text(legendRenderer, label, x0 + pad + swatch + gap, (int) Math.round(y - lineHeight * 0.8), width, height);
				y -= rowHeight;
			}
		}
		if (colorBar != null) {
			int barWidth = Math.round(16 * s);
			int barHeight = (int) Math.max(lineHeight * 4, Math.min(height * 0.4, height - 200 * screenScale));
			/* The numbers of the bar (like an axis) */
			double step = niceStep((colorBar.max - colorBar.min) / 5);
			if (colorBar.log && colorBar.max - colorBar.min >= 1) {
				step = Math.max(1, Math.round(step));
			}
			java.util.List<double[]> numbers = new java.util.ArrayList<>();
			java.util.List<String> texts = new java.util.ArrayList<>();
			double numberWidth = 0;
			for (double v = Math.ceil(colorBar.min / step - 1e-9) * step; v <= colorBar.max + step * 1e-6 && texts.size() < 50; v += step) {
				double value = Math.round(v / step) * step;
				String text = colorBar.log ? formatLogValue(value, step, colorBar.format) : formatPlotValue(value, step, colorBar.format);
				numbers.add(new double[] { (value - colorBar.min) / (colorBar.max - colorBar.min) });
				texts.add(text);
				numberWidth = Math.max(numberWidth, mathWidth(legendRenderer, parseMath(text)));
			}
			double titleWidth = colorBar.title != null ? mathWidth(legendTitleRenderer, parseMath(colorBar.title)) : 0;
			int w = (int) Math.ceil(Math.max(titleWidth, barWidth + gap + numberWidth)) + 2 * pad;
			int titleHeight = colorBar.title != null ? (int) Math.round(lineHeight * 1.6) : 0;
			int h = barHeight + titleHeight + 2 * pad + (int) Math.round(lineHeight);
			int x0 = width - margin - w;
			int y1 = height - Math.round(AXIS_GIZMO_SPACE * screenScale);
			panel(gl, x0, y1 - h, w, h, background);
			int bx = x0 + pad, by1 = y1 - pad - titleHeight - (int) Math.round(lineHeight / 2), by0 = by1 - barHeight;
			/* The ramp (the colors from the bottom to the top) */
			int n = colorBar.colors.length;
			gl.glBegin(GL2.GL_QUAD_STRIP);
			for (int i = 0; i < n; i++) {
				PlotLegend.setColor(gl, colorBar.colors[i]);
				double y = by0 + barHeight * i / (double) (n - 1);
				gl.glVertex2d(bx, y);
				gl.glVertex2d(bx + barWidth, y);
			}
			gl.glEnd();
			gl.glColor4f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], 1);
			rectangle(gl, bx, by0, barWidth, barHeight);
			/* The ticks */
			gl.glBegin(GL.GL_LINES);
			for (double[] t : numbers) {
				double y = by0 + t[0] * barHeight;
				gl.glVertex2d(bx + barWidth, y);
				gl.glVertex2d(bx + barWidth + 4 * s, y);
			}
			gl.glEnd();
			if (colorBar.title != null) {
				text(legendTitleRenderer, colorBar.title, x0 + pad, y1 - pad - (int) Math.round(lineHeight * 0.9), width, height);
			}
			for (int i = 0; i < texts.size(); i++) {
				int y = (int) Math.round(by0 + numbers.get(i)[0] * barHeight - lineHeight * 0.35);
				text(legendRenderer, texts.get(i), bx + barWidth + gap, y, width, height);
			}
		}
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	/* The space of the axis orientation overlay at the top right (in points) */
	private static final int AXIS_GIZMO_SPACE = 126;

	/* A panel with the background color and a border */
	private void panel(GL2 gl, int x, int y, int w, int h, float[] background) {
		gl.glColor4f(background[0], background[1], background[2], 0.85f);
		gl.glRecti(x, y, x + w, y + h);
		gl.glColor4f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], 0.5f);
		rectangle(gl, x, y, w, h);
	}

	private static void rectangle(GL2 gl, int x, int y, int w, int h) {
		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glVertex2d(x + 0.5, y + 0.5);
		gl.glVertex2d(x + w - 0.5, y + 0.5);
		gl.glVertex2d(x + w - 0.5, y + h - 0.5);
		gl.glVertex2d(x + 0.5, y + h - 0.5);
		gl.glEnd();
	}

	/* A text in window coordinates (the baseline at y) */
	private void text(TextRenderer r, String text, int x, int y, int width, int height) {
		r.beginRendering(width, height);
		/* The color of the legend */
		float[] c = textColor(textColors, 3);
		r.setColor(c[0], c[1], c[2], c[3]);
		drawMath(r, parseMath(text), x, y, 1, r.getBounds("A").getHeight());
		r.endRendering();
	}

	/* A part of a text: level 1 = superscript, -1 = subscript, 0 = normal */
	record Segment(String text, int level) {
	}

	/* The size of the superscripts and subscripts */
	private static final float SMALL = 0.68f;

	/*
	 * The parts of a text with a simple math markup: x^2, x^-0.5 or x^{n+1} for a
	 * superscript, N_0 or N_{max} for a subscript (\^ and \_ for the characters).
	 */
	static java.util.List<Segment> parseMath(String text) {
		java.util.List<Segment> out = new java.util.ArrayList<>();
		if (text.indexOf('^') < 0 && text.indexOf('_') < 0) {
			out.add(new Segment(text, 0));
			return out;
		}
		StringBuilder b = new StringBuilder();
		int i = 0, n = text.length();
		while (i < n) {
			char c = text.charAt(i);
			if (c == '\\' && i + 1 < n && (text.charAt(i + 1) == '^' || text.charAt(i + 1) == '_' || text.charAt(i + 1) == '\\')) {
				b.append(text.charAt(i + 1));
				i += 2;
				continue;
			}
			if ((c == '^' || c == '_') && i + 1 < n) {
				String part;
				int j;
				if (text.charAt(i + 1) == '{') {
					/* The matching brace (nested braces, e.g., e^{-(x^{2})}) */
					int close = -1;
					for (int k = i + 2, depth = 1; k < n; k++) {
						char ch = text.charAt(k);
						if (ch == '{') {
							depth++;
						} else if (ch == '}' && --depth == 0) {
							close = k;
							break;
						}
					}
					if (close < 0) {
						b.append(c);
						i++;
						continue;
					}
					part = text.substring(i + 2, close);
					j = close + 1;
				} else {
					/* A number (e.g., -0.5) or a word */
					j = i + 1;
					if (text.charAt(j) == '-' || text.charAt(j) == '+') {
						j++;
					}
					if (j < n && (Character.isDigit(text.charAt(j)) || text.charAt(j) == '.')) {
						while (j < n && (Character.isDigit(text.charAt(j)) || text.charAt(j) == '.')) {
							j++;
						}
						/* A point at the end of a sentence */
						if (text.charAt(j - 1) == '.') {
							j--;
						}
					} else {
						while (j < n && Character.isLetterOrDigit(text.charAt(j))) {
							j++;
						}
					}
					part = text.substring(i + 1, j);
					if (part.isEmpty() || part.equals("-") || part.equals("+")) {
						b.append(c);
						i++;
						continue;
					}
				}
				if (b.length() > 0) {
					out.add(new Segment(b.toString(), 0));
					b.setLength(0);
				}
				/* The minus sign of an exponent */
				out.add(new Segment(part.replace('-', '\u2212'), c == '^' ? 1 : -1));
				i = j;
				continue;
			}
			b.append(c);
			i++;
		}
		if (b.length() > 0 || out.isEmpty()) {
			out.add(new Segment(b.toString(), 0));
		}
		return out;
	}

	/* The width of the parts of a text (in the units of the renderer) */
	private static double mathWidth(TextRenderer r, java.util.List<Segment> segments) {
		double w = 0;
		for (Segment segment : segments) {
			w += advance(r, segment.text()) * (segment.level() == 0 ? 1 : SMALL);
		}
		return w;
	}

	/* The width of a text with the spaces (getBounds ignores spaces at the end) */
	private static double advance(TextRenderer r, String text) {
		return r.getBounds(text + "|").getWidth() - r.getBounds("|").getWidth();
	}

	/* Draws the parts of a text (the baseline at x, y; ascent: the height of the capital letters) */
	private static void drawMath(TextRenderer r, java.util.List<Segment> segments, float x, float y, float scale, double ascent) {
		for (Segment segment : segments) {
			float size = segment.level() == 0 ? scale : scale * SMALL;
			float dy = segment.level() > 0 ? (float) (ascent * 0.48 * scale) : segment.level() < 0 ? (float) (-ascent * 0.22 * scale) : 0;
			r.draw3D(segment.text(), x, y + dy, 0, size);
			x += advance(r, segment.text()) * size;
		}
	}

	/* The font of the title of the plot view (created with the scale of the view) */
	private TextRenderer titleRenderer;
	private float titleScale;
	private String titleFamily;
	/* The height of the title drawn in the current frame (the legend is drawn below) */
	private int titleHeight;

	/** Called if no title is drawn in the frame (the legend at the top). */
	public void noPlotTitle() {
		titleHeight = 0;
	}

	/**
	 * Draws the title of the plot view centered at the top of the view (lines
	 * separated with \n).
	 */
	public void drawPlotTitle(GL2 gl, String title, int width, int height, float pixelScale, float fontScale) {
		float screenScale = pixelScale;
		pixelScale *= fontScale;
		String titleFont = family(fonts, 0);
		if (titleRenderer == null || titleScale != pixelScale || !titleFont.equals(titleFamily)) {
			if (titleRenderer != null) {
				titleRenderer.dispose();
			}
			titleScale = pixelScale;
			titleFamily = titleFont;
			titleRenderer = new TextRenderer(new Font(titleFont, Font.BOLD, Math.max(10, Math.round(16 * pixelScale))), true, false);
		}
		String[] lines = title.split("\n");
		titleRenderer.beginRendering(width, height);
		float[] titleColor = textColor(textColors, 0);
		titleRenderer.setColor(titleColor[0], titleColor[1], titleColor[2], titleColor[3]);
		double lineHeight = titleRenderer.getBounds("Ag").getHeight() * 1.3;
		double ascent = titleRenderer.getBounds("A").getHeight();
		for (int i = 0; i < lines.length; i++) {
			java.util.List<Segment> segments = parseMath(lines[i]);
			int x = (int) Math.round((width - mathWidth(titleRenderer, segments)) / 2);
			int y = (int) Math.round(height - 12 * screenScale - (i + 1) * lineHeight);
			drawMath(titleRenderer, segments, x, y, 1, ascent);
		}
		titleRenderer.endRendering();
		titleHeight = (int) Math.round(12 * screenScale + lines.length * lineHeight);
	}

	/* The colors of the texts of the plot view (title, numbers, axis titles, legend; -1 = the color of the lines) */
	private int[] textColors;

	void setTextColors(int[] colors) {
		this.textColors = colors;
	}

	/* The color of a text (the slot of the colors or the color of the lines) */
	float[] textColor(int[] colors, int slot) {
		if (colors != null && slot < colors.length && colors[slot] >= 0) {
			int rgb = colors[slot];
			return new float[] { ((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f, 1 };
		}
		return colorCubeLines.clone();
	}

	/* A text at a position facing the camera (centered) with a color! */
	void drawPlotText(GL2 gl, String text, double x, double y, double z, float scale, float[] color) {
		drawPlotText(gl, text, x, y, z, scale, color, null);
	}

	/* A text at a position facing the camera with a color and a font family (null = the default font) */
	void drawPlotText(GL2 gl, String text, double x, double y, double z, float scale, float[] color, String family) {
		float[] previous = drawColor;
		String previousFont = drawFont;
		drawColor = color;
		drawFont = family;
		drawPlotText(gl, text, x, y, z, scale);
		drawColor = previous;
		drawFont = previousFont;
	}

	/* The font family of drawPlotText (null = the default font) */
	private String drawFont;
	/* The renderers of the 3D texts with other font families (the same size and style as the default) */
	private final java.util.Map<String, TextRenderer> plotRenderers = new java.util.HashMap<>();

	private TextRenderer plotRenderer(String family) {
		if (family == null || family.equals(DEFAULT_FONT)) {
			return renderer;
		}
		return plotRenderers.computeIfAbsent(family, f -> new TextRenderer(new Font(f, Font.BOLD, 22), true, false));
	}

	/* The default font of the plot texts */
	static final String DEFAULT_FONT = "Verdana";

	/* The font families of the plot texts (title, numbers, axis titles, legend; null = the default) */
	private String[] fonts;

	void setFonts(String[] fonts) {
		this.fonts = fonts;
	}

	/* The font family of a slot (the default if none is set) */
	static String family(String[] fonts, int slot) {
		return fonts != null && slot < fonts.length && fonts[slot] != null && !fonts[slot].isBlank() ? fonts[slot] : DEFAULT_FONT;
	}

	/* The color of drawPlotText (null = the color of the lines) */
	private float[] drawColor;

	/* A text at a position facing the camera (centered)! */
	void drawPlotText(GL2 gl, String text, double x, double y, double z, float scale) {
		gl.glPushMatrix();
		gl.glTranslated(x, y, z);
		float[] mv = new float[16];
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mv, 0);
		/* Facing the camera with the scale of the view (e.g., the scale of the data) */
		float scaleView = (float) Math.sqrt(mv[0] * mv[0] + mv[1] * mv[1] + mv[2] * mv[2]);
		mv[0] = scaleView;
		mv[1] = 0;
		mv[2] = 0;
		mv[4] = 0;
		mv[5] = scaleView;
		mv[6] = 0;
		mv[8] = 0;
		mv[9] = 0;
		mv[10] = scaleView;
		gl.glLoadMatrixf(mv, 0);
		TextRenderer r = plotRenderer(drawFont);
		r.begin3DRendering();
		float[] c = drawColor != null ? drawColor : colorCubeLines;
		r.setColor(c[0], c[1], c[2], c[3]);
		/* A text with superscripts and subscripts (see parseMath) */
		java.util.List<Segment> segments = parseMath(text);
		double ascent = r.getBounds("A").getHeight();
		double textWidth = mathWidth(r, segments);
		drawMath(r, segments, (float) (-textWidth / 2 * scale), (float) (-r.getBounds("Ag").getHeight() / 3 * scale), scale, ascent);
		r.end3DRendering();
		gl.glPopMatrix();
	}

	/* The camera position in the coordinates of the modelview matrix or null! */
	private static double[] eyePosition(float[] m) {
		/* Column major: the 3x3 part a and the translation t (eye = -a^-1 * t) */
		double a = m[0], b = m[4], c = m[8], d = m[1], e = m[5], f = m[9], g = m[2], h = m[6], i = m[10];
		double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
		if (Math.abs(det) < 1e-12) {
			return null;
		}
		double tx = -m[12], ty = -m[13], tz = -m[14];
		double x = ((e * i - f * h) * tx + (c * h - b * i) * ty + (b * f - c * e) * tz) / det;
		double y = ((f * g - d * i) * tx + (a * i - c * g) * ty + (c * d - a * f) * tz) / det;
		double z = ((d * h - e * g) * tx + (b * g - a * h) * ty + (a * e - b * d) * tz) / det;
		return new double[] { x, y, z };
	}

	/* The x coordinate on the screen (normalized) of a position! */
	private static double screenX(float[] mv, float[] pr, double x, double y, double z) {
		double[] eye = new double[4];
		for (int r = 0; r < 4; r++) {
			eye[r] = mv[r] * x + mv[4 + r] * y + mv[8 + r] * z + mv[12 + r];
		}
		double cx = pr[0] * eye[0] + pr[4] * eye[1] + pr[8] * eye[2] + pr[12] * eye[3];
		double cw = pr[3] * eye[0] + pr[7] * eye[1] + pr[11] * eye[2] + pr[15] * eye[3];
		return Math.abs(cw) > 1e-12 ? cx / cw : cx;
	}

	/* The grid lines (multiples of the grid size and the borders) of a side of the box! */
	private static void drawFaceGrid(GL2 gl, int axis, double value, double[] b, float step) {
		int u = (axis + 1) % 3, v = (axis + 2) % 3;
		double[] p = new double[3];
		p[axis] = value;
		for (double s : gridValues(b[2 * u], b[2 * u + 1], step)) {
			p[u] = s;
			p[v] = b[2 * v];
			gl.glVertex3d(p[0], p[1], p[2]);
			p[v] = b[2 * v + 1];
			gl.glVertex3d(p[0], p[1], p[2]);
		}
		for (double s : gridValues(b[2 * v], b[2 * v + 1], step)) {
			p[v] = s;
			p[u] = b[2 * u];
			gl.glVertex3d(p[0], p[1], p[2]);
			p[u] = b[2 * u + 1];
			gl.glVertex3d(p[0], p[1], p[2]);
		}
	}

	/* The borders and the multiples of the step between (at most 1000 lines)! */
	private static double[] gridValues(double min, double max, float step) {
		if (max <= min) {
			return new double[] { min };
		}
		int n = step > 0 ? (int) Math.min(1000, Math.floor(max / step) - Math.ceil(min / step) + 1) : 0;
		double first = step > 0 ? Math.ceil(min / step) * step : min;
		double[] values = new double[Math.max(0, n) + 2];
		int count = 0;
		values[count++] = min;
		for (int i = 0; i < n; i++) {
			double s = first + i * step;
			if (s > min && s < max) {
				values[count++] = s;
			}
		}
		values[count++] = max;
		return java.util.Arrays.copyOf(values, count);
	}

	public float[] getColorCubeLines() {
		return colorCubeLines;
	}

	public void setColorCubeLines(float[] colorCubeLines) {
		this.colorCubeLines = colorCubeLines;
	}

}