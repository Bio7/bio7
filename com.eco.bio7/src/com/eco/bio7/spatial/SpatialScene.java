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
		float sizeX = grid.getSizeX();
		float sizeY = grid.getSizeY();
		float sizeZ = grid.getSizeZ();
		if (grid.showAxes) {
			setAxesLines(gl, grid);

			// Save original matrix
			float[] originalMV = new float[16];
			gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, originalMV, 0);

			// X Axis Label
			gl.glPushMatrix();
			gl.glTranslatef(sizeX + 10, 0, 0);
			applyBillboardRotation(gl, originalMV);
			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], colorCubeLines[3]);
			renderer.draw3D("X", 0, 0, 0, 1.0f);
			renderer.end3DRendering();
			gl.glPopMatrix();

			// Y Axis Label
			gl.glPushMatrix();
			gl.glTranslatef(0, sizeY + 10, 0);
			applyBillboardRotation(gl, originalMV);
			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], colorCubeLines[3]);
			renderer.draw3D("Y", 0, 0, 0, 1.0f);
			renderer.end3DRendering();
			gl.glPopMatrix();

			// Z Axis Label
			gl.glPushMatrix();
			gl.glTranslatef(0, 0, sizeZ + 10);
			applyBillboardRotation(gl, originalMV);
			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2], colorCubeLines[3]);
			renderer.draw3D("Z", 0, 0, 0, 1.0f);
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
		float sizeX = grid.getSizeX();
		float sizeY = grid.getSizeY();
		float sizeZ = grid.getSizeZ();

		gl.glBegin(GL.GL_LINES);

		// x
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, 0, 0);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(sizeX, 0, 0);
		/* y */
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, 0, 0);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, sizeY, 0);
		/* z */
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, 0, 0);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3f(0, 0, sizeZ);

		gl.glEnd();

	}

	public void drawGrid(GL2 gl, SpatialStructure grid) {
		if (grid.showGrid) {
			float sizeX = grid.getSizeX();
			float sizeY = grid.getSizeY();
			float sizeZ = grid.getSizeZ();
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
		float sizeX = grid.getSizeX();
		float sizeY = grid.getSizeY();
		float sizeZ = grid.getSizeZ();
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
		}
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

	void drawQuadLines(GL2 gl, SpatialStructure grid) {

		float sizeX = grid.getSizeX();
		float sizeY = grid.getSizeY();
		float sizeZ = grid.getSizeZ();
		float x = sizeX;
		float y = sizeY;
		float z = sizeZ;
		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(-x, -y, -z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(x, -y, -z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(x, y, -z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(-x, y, -z);
		gl.glEnd();

		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(-x, -y, z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(x, -y, z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(x, y, z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(-x, y, z);
		gl.glEnd();

		gl.glBegin(GL.GL_LINES);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(-x, -y, -z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(-x, -y, z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(x, -y, -z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(x, -y, z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(x, y, -z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(x, y, z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(-x, y, -z);
		gl.glColor3f(colorCubeLines[0], colorCubeLines[1], colorCubeLines[2]);
		gl.glVertex3d(-x, y, z);
		gl.glEnd();

	}

	public float[] getColorCubeLines() {
		return colorCubeLines;
	}

	public void setColorCubeLines(float[] colorCubeLines) {
		this.colorCubeLines = colorCubeLines;
	}

}