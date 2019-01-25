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
			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1],
					colorCubeLines[2], colorCubeLines[3]);

			renderer.draw3D("X", sizeX + 10, 0, 0, 1.0f);
			renderer.end3DRendering();

			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1],
					colorCubeLines[2], colorCubeLines[3]);

			renderer.draw3D("Y", 0, sizeY + 10, 0, 1.0f);
			renderer.end3DRendering();

			renderer.begin3DRendering();
			renderer.setColor(colorCubeLines[0], colorCubeLines[1],
					colorCubeLines[2], colorCubeLines[3]);

			renderer.draw3D("Z", 0, 0, sizeZ + 10, 1.0f);

			renderer.end3DRendering();
		}
	}

	private void setInfoText() {

		renderer.draw("Y", 5, 25);
		renderer.draw("|_X", 10, 15);
		renderer.draw("/", 7, 3);
		renderer.draw("Z", 0, 0);

		renderer.endRendering();

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
			gl.glColor3f(colorCubeLines[0], colorCubeLines[1],
					colorCubeLines[2]);
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
			 * gl.glBegin(GL.GL_LINES); gl.glColor3ui(0, 0, 0); Horizontal
			 * lines. for (float i=-sizeY; i<=sizeY; i=i+gridSize) {
			 * gl.glVertex2f(-sizeX, i); gl.glVertex2f(sizeX, i); } Vertical
			 * lines. for (float i=-sizeX; i<=sizeX; i=i+gridSize) {
			 * gl.glVertex2f(i, -sizeY); gl.glVertex2f(i, sizeY); }
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
			gl.glColor3f(colorCubeLines[0], colorCubeLines[1],
					colorCubeLines[2]);
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
