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

import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import static com.jogamp.opengl.GL2.*;
import java.nio.FloatBuffer;

public class HeightMaps {
	public SpatialStructure grid;
	public int OriginX = 0;
	public int OriginY = 0;
	public int imageWidth;
	public int imageHeight;
	public int detail = 16;
	public static boolean cartesian = true;
	private boolean bRender = true;
	float v1[] = new float[3];
	float v2[] = new float[3];
	float v3[] = new float[3];
	float v4[] = new float[4];
	private float[] result;
	private float[] result2;
	public static boolean improveQualtity = false;
	private float scale = 1;
	private static boolean graphicsCardSwitch = false;
	float[][] heights;
	private static boolean inverse = false;

	// Smooth shading variables
	private float[][][] vertexNormals;
	private int lastDetailUsed = -1;
	// The texture and height values the vertex normals were computed for
	private Texture lastNormalsTexture;
	private float[][] lastNormalsHeights;

	public static boolean isInverse() {
		return inverse;
	}

	/*
	 * false: the image is displayed like in ImageJ (0,0 at the top left), true:
	 * flipped vertically (the first row of the image at the bottom).
	 */
	private static boolean flipVertically = false;

	/** Returns true if the texture and height map are displayed vertically flipped. */
	public static boolean isFlipVertically() {
		return flipVertically;
	}

	/**
	 * Displays the texture and height map vertically flipped (true) or like the
	 * image in ImageJ with 0,0 at the top left (false, the default).
	 */
	public static void setFlipVertically(boolean flip) {
		HeightMaps.flipVertically = flip;
	}

	/*
	 * With a single diffuse light the faces turned away from the light are almost
	 * black and the colors of the texture (e.g., the LUT of the image) are lost.
	 * More ambient light keeps the colors visible in the shadow. The lighting
	 * state is restored in endHeightMapLighting!
	 */
	private static void beginHeightMapLighting(GL2 gl) {
		gl.glPushAttrib(GL_LIGHTING_BIT);
		gl.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, new float[] { 0.4f, 0.4f, 0.4f, 1.0f }, 0);
		gl.glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT, new float[] { 1.0f, 1.0f, 1.0f, 1.0f }, 0);
		gl.glMaterialfv(GL_FRONT_AND_BACK, GL_DIFFUSE, new float[] { 0.6f, 0.6f, 0.6f, 1.0f }, 0);
	}

	private static void endHeightMapLighting(GL2 gl) {
		gl.glPopAttrib();
	}

	/*
	 * The rows of the image are drawn from the bottom (first row) to the top. The
	 * surface (texture and heights together) is mirrored around its center, so
	 * that the first row is at the top like in ImageJ. Returns true if the matrix
	 * has been pushed (see endOrientation)!
	 */
	private static boolean beginOrientation(GL2 gl, float centerY) {
		if (flipVertically) {
			return false;
		}
		gl.glPushMatrix();
		gl.glTranslatef(0, centerY, 0);
		gl.glScalef(1, -1, 1);
		gl.glTranslatef(0, -centerY, 0);
		return true;
	}

	private static void endOrientation(GL2 gl, boolean pushed) {
		if (pushed) {
			gl.glPopMatrix();
		}
	}

	public static void setInverse(boolean inverse) {
		HeightMaps.inverse = inverse;
	}

	private static boolean createMap = false;
	private static boolean dynamicMap = true;

	public HeightMaps() {
		this.grid = SpatialStructure.getSpatialStructureInstance();
	}

	/**
	 * Compute vertex normals for smooth shading by averaging normals from all
	 * surrounding triangles
	 */
	public void computeVertexNormals(Texture text, GL2 gl) {
		int cx = -OriginX;
		int cy = -OriginY;
		float mX = text.getWidth();
		float mY = text.getHeight();

		// Initialize vertex normals - grid-based indexing
		int gridWidth = (int) ((mX / detail) + 1);
		int gridHeight = (int) ((mY / detail) + 1);
		vertexNormals = new float[gridHeight][gridWidth][3];

		// Initialize all normals to zero
		for (int y = 0; y < gridHeight; y++) {
			for (int x = 0; x < gridWidth; x++) {
				vertexNormals[y][x][0] = 0;
				vertexNormals[y][x][1] = 0;
				vertexNormals[y][x][2] = 0;
			}
		}

		float[] vA = new float[3];
		float[] vB = new float[3];
		float[] vC = new float[3];
		float[] vD = new float[3];

		int gridX = 0, gridY = 0;

		// Accumulate normals from all triangles
		for (int Y = OriginY; Y < (text.getHeight() - detail) + OriginY; Y += detail) {
			gridX = 0;
			for (int X = OriginX; X < (text.getWidth() - detail) + OriginX; X += detail) {

				// Get quad vertices
				vA[0] = X;
				vA[1] = Y;
				vA[2] = Height(X + cx, Y + cy) * scale;

				vB[0] = X;
				vB[1] = Y + detail;
				vB[2] = Height(X + cx, Y + detail + cy) * scale;

				vC[0] = X + detail;
				vC[1] = Y + detail;
				vC[2] = Height(X + detail + cx, Y + detail + cy) * scale;

				vD[0] = X + detail;
				vD[1] = Y;
				vD[2] = Height(X + detail + cx, Y + cy) * scale;

				float[] n1 = calculateNormals(vA, vB, vC);
				float[] n2 = calculateNormals(vA, vC, vD);

				// Add normals to grid vertices
				if (gridY < gridHeight && gridX < gridWidth) {
					vertexNormals[gridY][gridX][0] += n1[0] + n2[0];
					vertexNormals[gridY][gridX][1] += n1[1] + n2[1];
					vertexNormals[gridY][gridX][2] += n1[2] + n2[2];
				}
				if (gridY + 1 < gridHeight && gridX < gridWidth) {
					vertexNormals[gridY + 1][gridX][0] += n1[0];
					vertexNormals[gridY + 1][gridX][1] += n1[1];
					vertexNormals[gridY + 1][gridX][2] += n1[2];
				}
				if (gridY + 1 < gridHeight && gridX + 1 < gridWidth) {
					vertexNormals[gridY + 1][gridX + 1][0] += n1[0] + n2[0];
					vertexNormals[gridY + 1][gridX + 1][1] += n1[1] + n2[1];
					vertexNormals[gridY + 1][gridX + 1][2] += n1[2] + n2[2];
				}
				if (gridY < gridHeight && gridX + 1 < gridWidth) {
					vertexNormals[gridY][gridX + 1][0] += n2[0];
					vertexNormals[gridY][gridX + 1][1] += n2[1];
					vertexNormals[gridY][gridX + 1][2] += n2[2];
				}

				gridX++;
			}
			gridY++;
		}

		// Normalize all vertex normals
		for (int y = 0; y < gridHeight; y++) {
			for (int x = 0; x < gridWidth; x++) {
				normalize(vertexNormals[y][x]);
			}
		}

		lastDetailUsed = detail;
	}

	private float[] getGridNormal(int gridY, int gridX) {
		if (vertexNormals != null && gridY >= 0 && gridY < vertexNormals.length && gridX >= 0
				&& gridX < vertexNormals[0].length) {
			return vertexNormals[gridY][gridX];
		}
		return new float[] { 0, 0, 1 };
	}

	/**
	 * Faster VBO-based rendering method with smooth shading (preferred)
	 * @param drawable 
	 */
	public void RenderHeightMap(Texture text, GL2 gl, GLAutoDrawable drawable) {
		/*
		 * Recompute the normals if the detail, the texture or the heights changed.
		 * Dynamic heights are read from the active image and can change any time!
		 */
		if (vertexNormals == null || lastDetailUsed != detail || dynamicMap || lastNormalsTexture != text
				|| lastNormalsHeights != heights) {
			computeVertexNormals(text, gl);
			lastNormalsTexture = text;
			lastNormalsHeights = heights;
		}

		/* Without the texture for the wireframe (see drawWireframe) */
		if (!wireframePass) {
			text.enable(gl);
		}
		text.bind(gl);

		int cx = -OriginX;
		int cy = -OriginY;
		float mX = text.getWidth();
		float mY = text.getHeight();
		boolean mirrored = beginOrientation(gl, OriginY + mY / 2);
		beginHeightMapLighting(gl);

		float s = detail;

		float tileX, tileY, tilestepX, tilestepY;
		if (graphicsCardSwitch == false) {
			tileX = (mX / s);
			tileY = (mY / s);
			tilestepX = ((mX / s) / s) * mX;
			tilestepY = ((mY / s) / s) * mY;
		} else {
			tileX = (mX / s) / mX;
			tileY = (mY / s) / mY;
			tilestepX = (mX / s) / s;
			tilestepY = (mY / s) / s;
		}

		// Calculate buffer sizes
		int quadCount = (int) ((mX / s) * (mY / s));
		int vertexCount = quadCount * 6; // 6 vertices per quad (2 triangles)

		FloatBuffer vertexBuffer = FloatBuffer.allocate(vertexCount * 3);
		FloatBuffer normalBuffer = FloatBuffer.allocate(vertexCount * 3);
		FloatBuffer texCoordBuffer = FloatBuffer.allocate(vertexCount * 2);

		int gridX = 0, gridY = 0;
		float tilex = 0, tiley = 0;
		float i = 0, u = 0;

		for (int Y = OriginY; Y <= (text.getHeight() - detail) + OriginY; Y += detail) {
			tiley = (tileY * u) / tilestepY;
			gridX = 0;
			for (int X = OriginX; X <= (text.getWidth() - detail) + OriginX; X += detail) {
				tilex = (tileX * i) / tilestepX;

				float z1 = Height(X + cx, Y + cy) * scale;
				float z2 = Height(X + cx, Y + detail + cy) * scale;
				float z3 = Height(X + detail + cx, Y + detail + cy) * scale;
				float z4 = Height(X + detail + cx, Y + cy) * scale;

				// Apply inverse flag for geo mode (negative z-coordinates)
				if (inverse) {
					z1 = -z1;
					z2 = -z2;
					z3 = -z3;
					z4 = -z4;
				}

				// Triangle 1: v1, v2, v3
				vertexBuffer.put(X).put(Y).put(z1);
				vertexBuffer.put(X).put(Y + detail).put(z2);
				vertexBuffer.put(X + detail).put(Y + detail).put(z3);

				// Triangle 2: v1, v3, v4
				vertexBuffer.put(X).put(Y).put(z1);
				vertexBuffer.put(X + detail).put(Y + detail).put(z3);
				vertexBuffer.put(X + detail).put(Y).put(z4);

				// Normals for Triangle 1
				float[] n1 = getGridNormal(gridY, gridX);
				normalBuffer.put(-n1[0]).put(-n1[1]).put(-n1[2]);

				float[] n2 = getGridNormal(gridY + 1, gridX);
				normalBuffer.put(-n2[0]).put(-n2[1]).put(-n2[2]);

				float[] n3 = getGridNormal(gridY + 1, gridX + 1);
				normalBuffer.put(-n3[0]).put(-n3[1]).put(-n3[2]);

				// Normals for Triangle 2
				normalBuffer.put(-n1[0]).put(-n1[1]).put(-n1[2]);
				normalBuffer.put(-n3[0]).put(-n3[1]).put(-n3[2]);

				float[] n4 = getGridNormal(gridY, gridX + 1);
				normalBuffer.put(-n4[0]).put(-n4[1]).put(-n4[2]);

				// Texture coordinates
				texCoordBuffer.put(tilex).put(tiley);
				texCoordBuffer.put(tilex).put(tiley + tileY / tilestepY);
				texCoordBuffer.put(tilex + tileX / tilestepX).put(tiley + tileY / tilestepY);
				texCoordBuffer.put(tilex).put(tiley);
				texCoordBuffer.put(tilex + tileX / tilestepX).put(tiley + tileY / tilestepY);
				texCoordBuffer.put(tilex + tileX / tilestepX).put(tiley);

				i++;
				gridX++;
			}
			i = 0;
			u++;
			gridY++;
		}

		vertexBuffer.rewind();
		normalBuffer.rewind();
		texCoordBuffer.rewind();

		int[] vbos = new int[3];
		gl.glGenBuffers(3, vbos, 0);

		// Vertex VBO
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbos[0]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GL2.GL_STATIC_DRAW);
		gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);

		// Normal VBO
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbos[1]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, normalBuffer.capacity() * 4, normalBuffer, GL2.GL_STATIC_DRAW);
		gl.glNormalPointer(GL2.GL_FLOAT, 0, 0);

		// TexCoord VBO
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbos[2]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, texCoordBuffer.capacity() * 4, texCoordBuffer, GL2.GL_STATIC_DRAW);
		gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, 0);

		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		if (bRender) {
			gl.glDrawArrays(GL2.GL_TRIANGLES, 0, vertexCount);
		}

		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		gl.glDeleteBuffers(3, vbos, 0);
		endHeightMapLighting(gl);
		endOrientation(gl, mirrored);
		text.disable(gl);
		drawWireframe(gl, () -> RenderHeightMap(text, gl, drawable));
	}

	/* The triangles of the height map as lines over the surface (see setWireframe) */
	private static boolean wireframe;
	private boolean wireframePass;

	/**
	 * Returns true if the triangles of the height map are drawn as lines over the
	 * surface.
	 * 
	 * @return true if the wireframe is shown
	 */
	public static boolean isWireframe() {
		return wireframe;
	}

	/**
	 * Shows the triangles of the height map as lines over the surface (e.g., to see
	 * the level of detail).
	 * 
	 * @param show true to show the wireframe
	 */
	public static void setWireframe(boolean show) {
		wireframe = show;
	}

	/* Renders the height map again as dark lines in front of the surface! */
	private void drawWireframe(GL2 gl, Runnable render) {
		if (!wireframe || wireframePass) {
			return;
		}
		wireframePass = true;
		gl.glPushAttrib(GL_ENABLE_BIT | GL_POLYGON_BIT | GL_CURRENT_BIT | GL_LINE_BIT | GL_LIGHTING_BIT);
		try {
			gl.glDisable(GL_LIGHTING);
			gl.glDisable(GL_TEXTURE_2D);
			gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
			gl.glEnable(GL_POLYGON_OFFSET_LINE);
			gl.glPolygonOffset(-1, -2);
			gl.glLineWidth(1);
			gl.glColor4f(0.1f, 0.1f, 0.1f, 0.8f);
			render.run();
		} finally {
			gl.glPopAttrib();
			wireframePass = false;
		}
	}

	public void renderDynamicHeightMap(GL2 gl, GLAutoDrawable drawable) {
		/* Get a reference to the textures!!! */
		grid.textures.updateAnimRenderer();
		int x, y;
		float z = 0;

		Texture text = Textures.animRenderer.getTexture();

		// Render image right-side up
		float a = 1.0f;

		int cx = -OriginX;// Cartesian correction for the height!
		int cy = -OriginY;
		float mX = text.getWidth();
		float mY = text.getHeight();

		float s = detail;// How fine is the resolution of the quads.
		boolean mirrored = beginOrientation(gl, OriginY + mY / 2);
		beginHeightMapLighting(gl);
		float tileX;
		float tileY;
		/* Graphics Card specific? */
		float tilestepX;
		float tilestepY;
		if (graphicsCardSwitch == false) {
			tileX = (mX / s);
			tileY = (mY / s);
			tilestepX = ((mX / s) / s) * mX;
			tilestepY = ((mY / s) / s) * mY;
		} else {
			tileX = (mX / s);
			tileY = (mY / s);
			tilestepX = (mX / s) / s;
			tilestepY = (mY / s) / s;
		}

		float tilex = 0;
		float tiley = 0;
		float i = 0;// Counter for the steps and multiplier for coordinates.
		float u = 0;
		int heightCount = 0;
		text.bind(gl);
		/* Without the texture for the wireframe (see drawWireframe) */
		if (!wireframePass) {
			text.enable(gl);
		}
		float triangleLength = (mX * mY) / s;

		int triangleCounter = 0;

		if (bRender) {
			gl.glBegin(GL_TRIANGLES);
		} else {
			gl.glBegin(GL_LINES);
		}

		for (int Y = OriginY; Y <= (text.getHeight() - detail) + OriginY; Y += detail) {
			tiley = (tileY * u) / tilestepY;
			heightCount = 0;
			for (int X = OriginX; X <= (text.getWidth() - detail) + OriginX; X += detail) {
				tilex = (tileX * i) / tilestepX;
				heightCount++;
				// Get The (X, Y, Z) Value For The Bottom Left Vertex
				x = X;
				y = Y;
				if (cartesian) {
					z = Height(X + cx, Y + cy);
				} else {
					z = Height(X, Y);
				}

				/* Store the calculated coordinates */
				v1[0] = x;
				v1[1] = y;
				v1[2] = z * scale;
				// Get The (X, Y, Z) Value For The Top Left Vertex
				x = X;
				y = Y + detail;
				if (cartesian) {
					z = Height(X + cx, Y + detail + cy);

				} else {
					z = Height(X, Y + detail);
				}

				/* Store the calculated coordinates */
				v2[0] = x;
				v2[1] = y;
				v2[2] = z * scale;
				// Get The (X, Y, Z) Value For The Top Right Vertex
				x = X + detail;
				if (cartesian) {
					z = Height(X + detail + cx, Y + detail + cy);
				} else {
					z = Height(X + detail, Y + detail);
				}
				y = Y + detail;

				/* Store the calculated coordinates */
				v3[0] = x;
				v3[1] = y;
				v3[2] = z * scale;
				// Get The (X, Y, Z) Value For The Bottom Right Vertex
				x = X + detail;
				if (cartesian) {
					z = Height(X + detail + cx, Y + cy);
				} else {
					z = Height(X + detail, Y);
				}
				y = Y;

				v4[0] = x;
				v4[1] = y;
				v4[2] = z * scale;

				/*
				 * We calculate the normals for the part of the plane and need only 3 edges for
				 * a plane and the normal!
				 */
				result2 = calculateNormals(v1, v3, v4);
				result = calculateNormals(v1, v2, v3);
				if (inverse == false) {
					gl.glTexCoord2f(tilex, tiley);
					gl.glNormal3f(-result2[0], -result2[1], -result2[2]);
					gl.glVertex3f(v1[0], v1[1], v1[2]);

					gl.glTexCoord2f(tilex + tileX / tilestepX, tiley);
					gl.glNormal3f(-result2[0], -result2[1], -result2[2]);
					gl.glVertex3f(v4[0], v4[1], v4[2]);

					gl.glTexCoord2f(tilex + tileX / tilestepX, tiley + tileY / tilestepY);
					gl.glNormal3f(-result2[0], -result2[1], -result[2]);
					gl.glVertex3f(v3[0], v3[1], v3[2]);

					gl.glTexCoord2f(tilex + tileX / tilestepX, tiley + tileY / tilestepY);
					gl.glNormal3f(-result[0], -result[1], -result[2]);
					gl.glVertex3f(v3[0], v3[1], v3[2]);

					gl.glTexCoord2f(tilex, tiley + tileY / tilestepY);
					gl.glNormal3f(-result[0], -result[1], -result[2]);
					gl.glVertex3f(v2[0], v2[1], v2[2]);

					gl.glTexCoord2f(tilex, tiley);
					gl.glNormal3f(-result[0], -result[1], -result[2]);
					gl.glVertex3f(v1[0], v1[1], v1[2]);
				}
				/*
				 * Inverse the z-coordinate for a correct geographic view of the heightmap!
				 */
				else {
					gl.glTexCoord2f(tilex, tiley);
					gl.glNormal3f(-result2[0], -result2[1], -result2[2]);
					gl.glVertex3f(v1[0], v1[1], -v1[2]);

					gl.glTexCoord2f(tilex + tileX / tilestepX, tiley);
					gl.glNormal3f(-result2[0], -result2[1], -result2[2]);
					gl.glVertex3f(v4[0], v4[1], -v4[2]);

					gl.glTexCoord2f(tilex + tileX / tilestepX, tiley + tileY / tilestepY);
					gl.glNormal3f(-result2[0], -result2[1], -result[2]);
					gl.glVertex3f(v3[0], v3[1], -v3[2]);

					gl.glTexCoord2f(tilex + tileX / tilestepX, tiley + tileY / tilestepY);
					gl.glNormal3f(-result[0], -result[1], -result[2]);
					gl.glVertex3f(v3[0], v3[1], -v3[2]);

					gl.glTexCoord2f(tilex, tiley + tileY / tilestepY);
					gl.glNormal3f(-result[0], -result[1], -result[2]);
					gl.glVertex3f(v2[0], v2[1], -v2[2]);

					gl.glTexCoord2f(tilex, tiley);
					gl.glNormal3f(-result[0], -result[1], -result[2]);
					gl.glVertex3f(v1[0], v1[1], -v1[2]);

				}
				i++;

			}

			i = 0;

			u++;
		}

		u = 0;

		gl.glEnd();
		endHeightMapLighting(gl);
		endOrientation(gl, mirrored);

		text.disable(gl);
		drawWireframe(gl, () -> renderDynamicHeightMap(gl, drawable));
	}

	public void renderTexture(Texture text, GL2 gl, GLAutoDrawable drawable) {
		float mX = text.getWidth();
		float mY = text.getHeight();
		text.bind(gl);
		text.enable(gl);
		OriginX = -(int) (mX / 2);
		OriginY = -(int) (mY / 2);
		TextureCoords tc = text.getImageTexCoords();
		boolean mirrored = beginOrientation(gl, cartesian ? 0 : -OriginY);
		if (cartesian == false) {
			gl.glBegin(GL_QUADS);
			gl.glTexCoord2f(tc.left(), tc.top());
			gl.glVertex3f(-mX / 2 - OriginX, -mY / 2 - OriginY, 0);
			gl.glTexCoord2f(tc.right(), tc.top());
			gl.glVertex3f(+mX / 2 - OriginX, -mY / 2 - OriginY, 0);
			gl.glTexCoord2f(tc.right(), tc.bottom());
			gl.glVertex3f(+mX / 2 - OriginX, mY / 2 - OriginY, 0);
			gl.glTexCoord2f(tc.left(), tc.bottom());
			gl.glVertex3f(-mX / 2 - OriginX, mY / 2 - OriginY, 0);

			gl.glEnd();
		} else {

			gl.glBegin(GL_QUADS);
			gl.glTexCoord2f(tc.left(), tc.top());
			gl.glVertex3f(-mX / 2, -mY / 2, 0);
			gl.glTexCoord2f(tc.right(), tc.top());
			gl.glVertex3f(+mX / 2, -mY / 2, 0);
			gl.glTexCoord2f(tc.right(), tc.bottom());
			gl.glVertex3f(+mX / 2, mY / 2, 0);
			gl.glTexCoord2f(tc.left(), tc.bottom());
			gl.glVertex3f(-mX / 2, mY / 2, 0);

			gl.glEnd();

		}
		endOrientation(gl, mirrored);

		text.disable(gl);
	}

	public void renderDynamicTexture(GL2 gl, GLAutoDrawable drawable) {
		grid.textures.updateAnimRenderer();
		Texture text = Textures.animRenderer.getTexture();
		float mX = text.getWidth();
		float mY = text.getHeight();
		text.bind(gl);
		text.enable(gl);
		OriginX = -(int) (mX / 2);
		OriginY = -(int) (mY / 2);
		TextureCoords tc = text.getImageTexCoords();
		boolean mirrored = beginOrientation(gl, cartesian ? 0 : -OriginY);
		if (cartesian == false) {
			gl.glBegin(GL_QUADS);
			gl.glTexCoord2f(tc.left(), tc.top());
			gl.glVertex3f(-mX / 2 - OriginX, -mY / 2 - OriginY, 0);
			gl.glTexCoord2f(tc.right(), tc.top());
			gl.glVertex3f(+mX / 2 - OriginX, -mY / 2 - OriginY, 0);
			gl.glTexCoord2f(tc.right(), tc.bottom());
			gl.glVertex3f(+mX / 2 - OriginX, mY / 2 - OriginY, 0);
			gl.glTexCoord2f(tc.left(), tc.bottom());
			gl.glVertex3f(-mX / 2 - OriginX, mY / 2 - OriginY, 0);

			gl.glEnd();
		} else {

			gl.glBegin(GL_QUADS);
			gl.glTexCoord2f(tc.left(), tc.top());
			gl.glVertex3f(-mX / 2, -mY / 2, 0);
			gl.glTexCoord2f(tc.right(), tc.top());
			gl.glVertex3f(+mX / 2, -mY / 2, 0);
			gl.glTexCoord2f(tc.right(), tc.bottom());
			gl.glVertex3f(+mX / 2, mY / 2, 0);
			gl.glTexCoord2f(tc.left(), tc.bottom());
			gl.glVertex3f(-mX / 2, mY / 2, 0);

			gl.glEnd();

		}
		endOrientation(gl, mirrored);

		text.disable(gl);
	}

	public float[] calculateNormals(float[] v1, float[] v2, float[] v3) {
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
		return normalize(normal);
	}

	/* Normalize a vector! */
	private float[] normalize(float[] normal) {
		float t = normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2];
		if (t != 0 && t != 1)
			t = (float) (1 / Math.sqrt(t));
		normal[0] *= t;
		normal[1] *= t;
		normal[2] *= t;
		return normal;
	}

	public float Height(int x, int y) {
		float value = 0;
		/*
		 * The default access to the ImageJ image values directly from the image!
		 */
		if (dynamicMap) {
			value = HeightDynamic(x, y);

		} else {
			/*
			 * We need this extra method to have a container for the height values if we
			 * want to render a height map to ImageJ!
			 */

			if (createMap) {

				HeightStatic();
				createMap = false;
			}

			if (heights != null) {
				/*
				 * Important not to access values beyond the image width, height!
				 */
				if (y < heights.length && x < heights[0].length && x >= 0 && y >= 0) {
					value = heights[y][x];
				}

			}
		}
		return value;
	}

	float HeightDynamic(int x, int y) { // This Returns The Height
		// From A Height Map Index
		float h = 0;
		if (grid.heightMap) {
			if (WindowManager.getImageCount() > 0) {

				ImagePlus imp = WindowManager.getCurrentWindow().getImagePlus();

				if (imp != null) {

					imageHeight = imp.getHeight();
					imageWidth = imp.getWidth();
					h = heightValue(imp.getProcessor(), imp.getType(), imp.getCalibration(), x, y);

				}
			}
		}
		return h; // Index Into Our Height
		// Array And Return The
		// Height
	}

	/**
	 * Returns the height value of a pixel: the calibrated value for 8-bit and
	 * 16-bit images, the float value for 32-bit images and the luminance for RGB
	 * images. Pixels outside of the image and NaN values return 0.
	 */
	private static float heightValue(ImageProcessor ip, int type, Calibration cal, int x, int y) {
		if (ip == null || x < 0 || y < 0 || x >= ip.getWidth() || y >= ip.getHeight()) {
			return 0;
		}
		float value;
		switch (type) {
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16:
		case ImagePlus.COLOR_256:
			/* We get the calibration value from ImageJ! */
			value = (float) cal.getCValue(ip.getPixel(x, y));
			break;
		case ImagePlus.GRAY32:
		case ImagePlus.COLOR_RGB:
			/* The float value or the luminance of the RGB color! */
			value = ip.getPixelValue(x, y);
			break;
		default:
			value = 0;
		}
		return Float.isNaN(value) ? 0 : value;
	}

	public void HeightStatic() { // This method creates a height array from
		// an active image in ImageJ!

		if (grid.heightMap) {
			if (WindowManager.getImageCount() > 0) {

				ImagePlus imp = WindowManager.getCurrentWindow().getImagePlus();

				if (imp != null) {
					imageHeight = imp.getHeight();
					imageWidth = imp.getWidth();
					heights = new float[imageHeight][imageWidth];

					int type = imp.getType();
					Calibration cal = imp.getCalibration();
					ImageProcessor ip = imp.getProcessor();

					for (int y = 0; y < imageHeight; y++) {
						for (int x = 0; x < imageWidth; x++) {
							heights[y][x] = heightValue(ip, type, cal, x, y);
						}

					}
				}
			}
		}

	}

	public int getOriginX() {
		return OriginX;
	}

	public void setOriginX(int originX) {
		OriginX = originX;
	}

	public int getOriginY() {
		return OriginY;
	}

	public void setOriginY(int originY) {
		OriginY = originY;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}

	public int getDetail() {
		return detail;
	}

	public void setDetail(int detail) {
		this.detail = detail;
	}

	public static boolean isCartesian() {
		return cartesian;
	}

	public void setCartesian(boolean cartesian) {
		this.cartesian = cartesian;
	}

	public float getScale() {
		return scale;
	}

	/** The maximum number of vertices of an exported height map (see export). */
	public static final int MAX_EXPORT_VERTICES = 4_000_000;

	/**
	 * Exports the height map of an ImageJ image as a triangle mesh with the colors
	 * of the image (with the LUT) as vertex colors, e.g., for Blender (*.glb or
	 * *.ply keep the colors). The format is selected by the file extension (obj,
	 * stl, ply, glb or vtk). x and y are the pixel coordinates (centered, the first
	 * row at the top like in the 3D view), z is the height multiplied by the
	 * height factor (negative with Geo).
	 * 
	 * @param imp  the image
	 * @param file the file
	 * @param step    the step in pixels (1 = every pixel, increased for very large
	 *                images, see MAX_EXPORT_VERTICES)
	 * @param texture true to export the image (with the LUT) as a texture: OBJ
	 *                (a *.png file next to the *.obj file) and glTF binary (*.glb,
	 *                embedded)
	 * @return a description of the exported mesh
	 * @throws IOException if the file can't be written
	 */
	public String export(ImagePlus imp, java.io.File file, int step, boolean texture) throws java.io.IOException {
		ImageProcessor ip = imp.getProcessor();
		int w = ip.getWidth(), h = ip.getHeight();
		step = Math.max(1, step);
		while (((long) (w - 1) / step + 1) * ((h - 1) / step + 1) > MAX_EXPORT_VERTICES) {
			step++;
		}
		/* The columns and rows (the last pixel included) */
		int[] cols = samples(w, step), rows = samples(h, step);
		int nx = cols.length, ny = rows.length;
		if (nx < 2 || ny < 2) {
			throw new java.io.IOException("The image is too small for a height map!");
		}
		int type = imp.getType();
		Calibration cal = imp.getCalibration();
		java.awt.image.BufferedImage colors = Textures.createImageWithLut(imp);
		boolean flip = flipVertically;
		float[] positions = new float[nx * ny * 3];
		float[] vertexColors = new float[nx * ny * 3];
		/* The texture coordinates: the centers of the pixels (v from the top) */
		float[] texCoords = new float[nx * ny * 2];
		for (int j = 0; j < ny; j++) {
			for (int i = 0; i < nx; i++) {
				int v = (j * nx + i) * 3;
				float z = heightValue(ip, type, cal, cols[i], rows[j]) * scale;
				positions[v] = cols[i] - (w - 1) / 2f;
				/* The first row at the top (y up) like in the 3D view (or flipped) */
				positions[v + 1] = flip ? rows[j] - (h - 1) / 2f : (h - 1) / 2f - rows[j];
				positions[v + 2] = inverse ? -z : z;
				int rgb = colors != null ? colors.getRGB(cols[i], rows[j]) : 0xffcccccc;
				vertexColors[v] = ((rgb >> 16) & 0xff) / 255f;
				vertexColors[v + 1] = ((rgb >> 8) & 0xff) / 255f;
				vertexColors[v + 2] = (rgb & 0xff) / 255f;
				int uv = (j * nx + i) * 2;
				texCoords[uv] = (cols[i] + 0.5f) / w;
				texCoords[uv + 1] = (rows[j] + 0.5f) / h;
			}
		}
		int[] indices = new int[(nx - 1) * (ny - 1) * 6];
		int t = 0;
		for (int j = 0; j < ny - 1; j++) {
			for (int i = 0; i < nx - 1; i++) {
				int a = j * nx + i, b = a + 1, d = a + nx, e = d + 1;
				/* Counterclockwise seen from above (the normals point up) */
				if (flip) {
					indices[t++] = a;
					indices[t++] = b;
					indices[t++] = d;
					indices[t++] = b;
					indices[t++] = e;
					indices[t++] = d;
				} else {
					indices[t++] = a;
					indices[t++] = d;
					indices[t++] = b;
					indices[t++] = b;
					indices[t++] = d;
					indices[t++] = e;
				}
			}
		}
		boolean textured = texture && colors != null;
		IsoSurface.exportMesh(file, positions, indices, vertexColors, textured ? texCoords : null,
				textured ? colors : null, "height map", imp.getTitle(), "pixel");
		String name = file.getName().toLowerCase();
		boolean withTexture = textured && (name.endsWith(".obj") || name.endsWith(".glb"));
		return nx * ny + " vertices, " + indices.length / 3 + " triangles (step " + step + " pixel"
				+ (withTexture ? ", with the texture" : "") + ")";
	}

	/* The positions 0, step, 2 * step, ... and the last position! */
	private static int[] samples(int size, int step) {
		int n = (size - 1) / step + 1;
		boolean last = (size - 1) % step != 0;
		int[] values = new int[n + (last ? 1 : 0)];
		for (int i = 0; i < n; i++) {
			values[i] = i * step;
		}
		if (last) {
			values[n] = size - 1;
		}
		return values;
	}

	/* The cached height range of the active image (see getHeightRange) */
	private float[] heightRange;
	private Object heightRangePixels;
	private long heightRangeTime;

	/**
	 * Returns the range of the heights (min, max: scaled, negated if inverse) of
	 * the active ImageJ image or null if no height map is displayed. The range is
	 * computed again if the image changed (at most every 0.5 s).
	 * 
	 * @return the min and max height or null
	 */
	public float[] getHeightRange() {
		if (grid == null || !grid.heightMap || WindowManager.getImageCount() == 0
				|| WindowManager.getCurrentWindow() == null) {
			return null;
		}
		ImagePlus imp = WindowManager.getCurrentWindow().getImagePlus();
		if (imp == null || imp.getProcessor() == null) {
			return null;
		}
		ImageProcessor ip = imp.getProcessor();
		long now = System.currentTimeMillis();
		if (heightRange == null || heightRangePixels != ip.getPixels() || now - heightRangeTime > 500) {
			int type = imp.getType();
			Calibration cal = imp.getCalibration();
			float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
			for (int y = 0; y < ip.getHeight(); y++) {
				for (int x = 0; x < ip.getWidth(); x++) {
					float v = heightValue(ip, type, cal, x, y);
					min = Math.min(min, v);
					max = Math.max(max, v);
				}
			}
			heightRange = new float[] { min, max };
			heightRangePixels = ip.getPixels();
			heightRangeTime = now;
		}
		float a = heightRange[0] * scale, b = heightRange[1] * scale;
		if (inverse) {
			a = -a;
			b = -b;
		}
		return new float[] { Math.min(a, b), Math.max(a, b) };
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public static boolean isDynamicMap() {
		return dynamicMap;
	}

	public static void setDynamicMap(boolean value) {
		dynamicMap = value;
	}

	public static boolean isCreateMap() {
		return createMap;
	}

	public static void setCreateMap(boolean createMap) {
		HeightMaps.createMap = createMap;
	}

	public static boolean isGraphicsCardSwitch() {
		return graphicsCardSwitch;
	}

	public static void setGraphicsCardSwitch(boolean graphicsCardSwitch) {
		HeightMaps.graphicsCardSwitch = graphicsCardSwitch;
	}

}