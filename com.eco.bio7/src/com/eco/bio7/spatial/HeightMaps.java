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

	public static boolean isInverse() {
		return inverse;
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
		// Recompute normals if detail changed
		if (vertexNormals == null || lastDetailUsed != detail) {
			computeVertexNormals(text, gl);
		}

		text.enable(gl);
		text.bind(gl);

		int cx = -OriginX;
		int cy = -OriginY;
		float mX = text.getWidth();
		float mY = text.getHeight();

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
		text.disable(gl);
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
		text.enable(gl);
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

		text.disable(gl);

	}

	public void renderTexture(Texture text, GL2 gl, GLAutoDrawable drawable) {
		float mX = text.getWidth();
		float mY = text.getHeight();
		text.bind(gl);
		text.enable(gl);
		OriginX = -(int) (mX / 2);
		OriginY = -(int) (mX / 2);
		TextureCoords tc = text.getImageTexCoords();
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
		OriginY = -(int) (mX / 2);
		TextureCoords tc = text.getImageTexCoords();
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
					int type = imp.getType();
					Calibration cal = imp.getCalibration();

					int[] v;
					switch (type) {
					case ImagePlus.GRAY32:
						v = imp.getPixel(x, y);
						/* We get the calibration value from ImageJ! */
						double cValue = Float.intBitsToFloat(v[0]);
						if (cValue == v[0]) {
							h = v[0];

						} else {
							h = (float) cValue;
						}
						break;
					case ImagePlus.GRAY8:
					case ImagePlus.GRAY16:
						v = imp.getPixel(x, y);
						/* We get the calibration value from ImageJ! */
						double c2Value = cal.getCValue(v[0]);
						if (c2Value == v[0]) {
							h = v[0];

						} else {
							h = (float) c2Value;
						}
						break;
					default:
						h = 0;
					}

				}
			}
		}
		return h; // Index Into Our Height
		// Array And Return The
		// Height
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

					for (int y = 0; y < imageHeight; y++) {
						for (int x = 0; x < imageWidth; x++) {

							int[] v;
							switch (type) {
							case ImagePlus.GRAY32:
								v = imp.getPixel(x, y);
								/* We get the calibration value from ImageJ! */
								double cValue = Float.intBitsToFloat(v[0]);
								if (cValue == v[0]) {
									heights[y][x] = v[0];

								} else {
									heights[y][x] = (float) cValue;
								}
								break;
							case ImagePlus.GRAY8:
							case ImagePlus.GRAY16:

								v = imp.getPixel(x, y);
								/* We get the calibration value from ImageJ! */
								double c2Value = cal.getCValue(v[0]);
								if (c2Value == v[0]) {
									heights[y][x] = v[0];

								} else {
									heights[y][x] = (float) c2Value;
								}
								break;
							}
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