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
package com.eco.bio7.loader3d;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * A model of a mesh file (STL, PLY, OBJ with materials and textures, OFF, see
 * MeshReader) for the 3D view. The model is centered at the origin and scaled
 * to a size (the largest dimension), the original coordinates are kept (see
 * getMeshData and getWeldedMesh, e.g., for the measurements).
 */
public class MeshModel {

	private static final float[] DEFAULT_DIFFUSE = { 0.8f, 0.8f, 0.8f };

	private final MeshData mesh;
	private MeshData welded;
	private MeshData.Report report;
	private final ModelDimensions modelDims = new ModelDimensions();
	private final float maxSize;
	/* The display (centered and scaled) */
	private final FloatBuffer positions;
	private final FloatBuffer normals;
	private final FloatBuffer colors;
	private final FloatBuffer texCoords;
	private final IntBuffer indices;
	/* The textures of the materials (created in the OpenGL thread of a context) */
	private final Map<String, Texture> textures = new HashMap<>();
	private GLContext textureContext;

	/**
	 * Creates a model of a mesh.
	 *
	 * @param mesh the mesh (see MeshReader)
	 * @param size the size of the largest dimension in the 3D view
	 */
	public MeshModel(MeshData mesh, float size) {
		this.mesh = mesh;
		this.maxSize = size;
		float[] b = mesh.getBounds();
		modelDims.set(new Tuple3(b[0], b[1], b[2]));
		modelDims.update(new Tuple3(b[3], b[4], b[5]));
		/* Centered and scaled (like the former OBJ loader) */
		float largest = Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2]));
		float scale = largest > 0 ? size / largest : 1;
		float cx = (b[0] + b[3]) / 2, cy = (b[1] + b[4]) / 2, cz = (b[2] + b[5]) / 2;
		float[] p = mesh.positions;
		float[] display = new float[p.length];
		for (int i = 0; i < p.length; i += 3) {
			display[i] = (p[i] - cx) * scale;
			display[i + 1] = (p[i + 1] - cy) * scale;
			display[i + 2] = (p[i + 2] - cz) * scale;
		}
		positions = Buffers.newDirectFloatBuffer(display);
		/* The normals of the file or smooth normals (a triangle soup like STL: flat) */
		MeshData normalsMesh = new MeshData();
		normalsMesh.positions = display;
		normalsMesh.triangles = mesh.triangles;
		normalsMesh.normals = mesh.normals;
		normalsMesh.ensureNormals();
		normals = Buffers.newDirectFloatBuffer(normalsMesh.normals);
		colors = mesh.colors != null ? Buffers.newDirectFloatBuffer(mesh.colors) : null;
		texCoords = mesh.texCoords != null ? Buffers.newDirectFloatBuffer(mesh.texCoords) : null;
		indices = Buffers.newDirectIntBuffer(mesh.triangles);
	}

	/**
	 * Loads a mesh file (STL, PLY, OBJ or OFF, see MeshReader).
	 *
	 * @param file the file
	 * @param size the size of the largest dimension in the 3D view
	 * @return the model
	 * @throws IOException if the file can't be read
	 */
	public static MeshModel load(File file, float size) throws IOException {
		MeshData mesh = MeshReader.read(file);
		return mesh.format.equals("OBJ") ? new OBJModel(mesh, size) : new MeshModel(mesh, size);
	}

	/**
	 * Draws the model (centered at the origin, with the materials, textures and
	 * vertex colors of the file).
	 */
	public void draw(GL2 gl) {
		GLContext context = GLContext.getCurrent();
		if (textureContext != context) {
			/* A new context: the textures are created again */
			textures.clear();
			textureContext = context;
		}
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_LIGHTING_BIT | GL2.GL_CURRENT_BIT | GL2.GL_COLOR_BUFFER_BIT
				| GL2.GL_TEXTURE_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glPushClientAttrib(GL2.GL_CLIENT_VERTEX_ARRAY_BIT);
		gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
		gl.glVertexPointer(3, GL.GL_FLOAT, 0, positions.rewind());
		gl.glNormalPointer(GL.GL_FLOAT, 0, normals.rewind());
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
		if (colors != null) {
			/* The vertex colors are the ambient and diffuse color */
			gl.glEnableClientState(GLPointerFunc.GL_COLOR_ARRAY);
			gl.glColorPointer(3, GL.GL_FLOAT, 0, colors.rewind());
			gl.glEnable(GLLightingFunc.GL_COLOR_MATERIAL);
			gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_AMBIENT_AND_DIFFUSE);
		}
		if (texCoords != null) {
			gl.glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, texCoords.rewind());
		}
		if (mesh.groups.isEmpty()) {
			applyMaterial(gl, null);
			drawTriangles(gl, 0, mesh.triangles.length / 3);
		} else {
			for (MeshData.Group group : mesh.groups) {
				MeshData.Material material = group.material != null ? mesh.materials.get(group.material) : null;
				applyMaterial(gl, material);
				drawTriangles(gl, group.start, group.count);
			}
		}
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopClientAttrib();
		gl.glPopAttrib();
	}

	private void drawTriangles(GL2 gl, int start, int count) {
		indices.position(start * 3);
		gl.glDrawElements(GL.GL_TRIANGLES, count * 3, GL.GL_UNSIGNED_INT, indices.slice());
		indices.rewind();
	}

	/* The material (OBJ) or the default (white with vertex colors, else gray) */
	private void applyMaterial(GL2 gl, MeshData.Material material) {
		float alpha = material != null ? material.alpha : 1;
		float[] diffuse = material != null ? material.diffuse : (colors != null ? new float[] { 1, 1, 1 } : DEFAULT_DIFFUSE);
		float[] ambient = material != null ? material.ambient : new float[] { 0.2f, 0.2f, 0.2f };
		float[] specular = material != null ? material.specular : new float[] { 0.1f, 0.1f, 0.1f };
		float shininess = material != null ? Math.min(128, material.shininess * 128f / 1000f) : 20;
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_AMBIENT, rgba(ambient, alpha), 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_DIFFUSE, rgba(diffuse, alpha), 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_SPECULAR, rgba(specular, alpha), 0);
		gl.glMaterialf(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_SHININESS, shininess);
		/* Without lighting: the diffuse color */
		gl.glColor4f(diffuse[0], diffuse[1], diffuse[2], alpha);
		if (alpha < 1) {
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			gl.glDisable(GL.GL_BLEND);
		}
		Texture texture = material != null && material.texture != null && texCoords != null
				? texture(gl, material.texture)
				: null;
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glLoadIdentity();
		if (texture != null) {
			texture.enable(gl);
			texture.bind(gl);
			gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
			if (texture.getMustFlipVertically()) {
				gl.glTranslatef(0, 1, 0);
				gl.glScalef(1, -1, 1);
			}
		} else {
			gl.glDisable(GL.GL_TEXTURE_2D);
		}
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	private static float[] rgba(float[] rgb, float alpha) {
		return new float[] { rgb[0], rgb[1], rgb[2], alpha };
	}

	/* The texture of a file (null if it can't be loaded) */
	private Texture texture(GL2 gl, String file) {
		if (textures.containsKey(file)) {
			return textures.get(file);
		}
		Texture texture = null;
		try {
			texture = TextureIO.newTexture(new File(file), false);
			texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
			texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
		} catch (IOException | RuntimeException e) {
			System.out.println("The texture can't be loaded: " + file + " (" + e.getMessage() + ")");
		}
		textures.put(file, texture);
		return texture;
	}

	/** Returns the name of the model (the file name without the extension). */
	public String getName() {
		return mesh.name;
	}

	/** Returns the format of the file (STL, PLY, OBJ or OFF). */
	public String getFormat() {
		return mesh.format;
	}

	/** Returns the number of vertices (duplicate vertices merged). */
	public int getNumVertices() {
		return getWeldedMesh().getVertexCount();
	}

	/** Returns the number of triangles. */
	public int getNumFaces() {
		return mesh.getTriangleCount();
	}

	/** Returns the size of the model in the coordinates of the file. */
	public ModelDimensions getModelDims() {
		return modelDims;
	}

	/** Returns the size of the largest dimension in the 3D view. */
	public float getMaxSize() {
		return maxSize;
	}

	/** Returns the mesh of the file (the coordinates of the file). */
	public MeshData getMeshData() {
		return mesh;
	}

	/**
	 * Returns the mesh with merged duplicate vertices (the coordinates of the
	 * file), e.g., for the measurements.
	 */
	public synchronized MeshData getWeldedMesh() {
		if (welded == null) {
			welded = mesh.weld();
		}
		return welded;
	}

	/** Returns the checks of the mesh (open edges, watertight, ...). */
	public synchronized MeshData.Report getReport() {
		if (report == null) {
			report = getWeldedMesh().check();
		}
		return report;
	}

	/** Returns the display positions (centered and scaled). */
	protected FloatBuffer getDisplayPositions() {
		return positions;
	}
}
