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
		if (grid.showGrid) {
			float step = grid.getGridSize();
			gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
			gl.glBegin(GL.GL_LINES);
			if (grid.plotGridYZ) {
				drawFaceGrid(gl, 0, back[0], b, step);
			}
			if (grid.plotGridXZ) {
				drawFaceGrid(gl, 1, back[1], b, step);
			}
			if (grid.plotGridXY) {
				drawFaceGrid(gl, 2, back[2], b, step);
			}
			gl.glEnd();
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
		for (int k = 0; k < 3; k++) {
			double min = b[2 * k], max = b[2 * k + 1];
			if (max <= min) {
				continue;
			}
			if (numbers) {
				double valueA = plotValue(grid, k, min, b), valueB = plotValue(grid, k, max, b);
				double step = niceStep(Math.abs(valueB - valueA) / 6);
				double from = Math.min(valueA, valueB), to = Math.max(valueA, valueB);
				gl.glBegin(GL.GL_LINES);
				for (double v = Math.ceil(from / step) * step; v <= to + step * 1e-6; v += step) {
					double[] p = start[k].clone();
					p[k] = min + (v - valueA) / (valueB - valueA) * (max - min);
					gl.glVertex3d(p[0], p[1], p[2]);
					gl.glVertex3d(p[0] + out[k][0] * tick, p[1] + out[k][1] * tick, p[2] + out[k][2] * tick);
				}
				gl.glEnd();
				for (double v = Math.ceil(from / step) * step; v <= to + step * 1e-6; v += step) {
					double[] p = start[k].clone();
					p[k] = min + (v - valueA) / (valueB - valueA) * (max - min);
					drawPlotText(gl, formatPlotValue(v, step), p[0] + out[k][0] * tick * 4,
							p[1] + out[k][1] * tick * 4, p[2] + out[k][2] * tick * 4, 0.5f * sizeFactor);
				}
			}
			if (grid.showAxes) {
				double[] p = start[k].clone();
				p[k] = (min + max) / 2;
				double d = tick * (numbers ? 9 : 3);
				drawPlotText(gl, names[k], p[0] + out[k][0] * d, p[1] + out[k][1] * d, p[2] + out[k][2] * d,
						0.7f * sizeFactor);
			}
		}
	}

	/*
	 * The displayed value of a position of an axis: the pixel coordinates of a
	 * texture or stack (y from the top like in ImageJ) and the heights of a height
	 * map, else the position!
	 */
	private static double plotValue(SpatialStructure grid, int axis, double position, double[] b) {
		boolean image = grid.getTextureExtent() != null;
		if (axis == 0) {
			return image ? position - b[0] : position;
		}
		if (axis == 1) {
			if (!image) {
				return position;
			}
			return HeightMaps.isFlipVertically() ? position - b[2] : b[3] - position;
		}
		boolean heightMap = grid.heightMap && grid.textures != null && grid.textures.showTexture
				&& grid.textures.tex != null;
		if (heightMap && SpatialStructure.hMap != null && SpatialStructure.hMap.getScale() != 0) {
			return position / SpatialStructure.hMap.getScale() * (HeightMaps.isInverse() ? -1 : 1);
		}
		return image ? position - b[4] : position;
	}

	/* A step of 1, 2 or 5 times a power of ten! */
	private static double niceStep(double value) {
		if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
			return 1;
		}
		double power = Math.pow(10, Math.floor(Math.log10(value)));
		double f = value / power;
		return (f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10) * power;
	}

	private static String formatPlotValue(double value, double step) {
		if (Math.abs(value) < step * 1e-6) {
			value = 0;
		}
		int decimals = step >= 1 ? 0 : (int) Math.ceil(-Math.log10(step));
		return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
	}

	/* A text at a position facing the camera (centered)! */
	private void drawPlotText(GL2 gl, String text, double x, double y, double z, float scale) {
		gl.glPushMatrix();
		gl.glTranslated(x, y, z);
		float[] mv = new float[16];
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mv, 0);
		mv[0] = 1;
		mv[1] = 0;
		mv[2] = 0;
		mv[4] = 0;
		mv[5] = 1;
		mv[6] = 0;
		mv[8] = 0;
		mv[9] = 0;
		mv[10] = 1;
		gl.glLoadMatrixf(mv, 0);
		renderer.begin3DRendering();
		renderer.setColor(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], colorCubeLines[3]);
		java.awt.geom.Rectangle2D bounds = renderer.getBounds(text);
		renderer.draw3D(text, (float) (-bounds.getWidth() / 2 * scale), (float) (-bounds.getHeight() / 3 * scale), 0,
				scale);
		renderer.end3DRendering();
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