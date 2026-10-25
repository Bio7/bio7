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
import java.io.UncheckedIOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.jogamp.opengl.GL2;

/**
 * A model of an OBJ file (with the materials and textures of the MTL file),
 * centered and scaled to a size. The file is read with the MeshReader (also
 * polygons, negative indices and vertex colors), see MeshModel for the other
 * formats.
 */
public class OBJModel extends MeshModel {

	/**
	 * Loads the OBJ file path + nm + ".obj".
	 *
	 * @param nm          the name of the file without the extension
	 * @param path        the directory (with a separator at the end)
	 * @param sz          the size of the largest dimension in the 3D view
	 * @param gl          the OpenGL context (not used, the model is drawn with
	 *                    draw)
	 * @param showDetails prints the checks of the mesh
	 * @throws UncheckedIOException if the file can't be read
	 */
	public OBJModel(String nm, String path, float sz, GL2 gl, boolean showDetails) {
		this(read(new File(path, nm + ".obj")), sz);
		if (showDetails) {
			System.out.println(getName() + ": " + getReport());
		}
	}

	/**
	 * Creates the model of a read OBJ file (see MeshReader).
	 *
	 * @param mesh the mesh
	 * @param size the size of the largest dimension in the 3D view
	 */
	public OBJModel(MeshData mesh, float size) {
		super(mesh, size);
	}

	private static MeshData read(File file) {
		try {
			return MeshReader.read(file);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Returns the vertices of the file (centered and scaled like the display).
	 */
	public ArrayList<Tuple3> getVerts() {
		MeshData mesh = getMeshData();
		FloatBuffer display = getDisplayPositions();
		int[] index = mesh.positionIndex;
		int count = 0;
		if (index != null) {
			for (int i : index) {
				count = Math.max(count, i + 1);
			}
		} else {
			count = mesh.getVertexCount();
		}
		Tuple3[] verts = new Tuple3[count];
		for (int i = 0; i < mesh.getVertexCount(); i++) {
			int p = index != null ? index[i] : i;
			if (verts[p] == null) {
				verts[p] = new Tuple3(display.get(i * 3), display.get(i * 3 + 1), display.get(i * 3 + 2));
			}
		}
		ArrayList<Tuple3> list = new ArrayList<>();
		for (Tuple3 t : verts) {
			if (t != null) {
				list.add(t);
			}
		}
		return list;
	}

	/** Returns the normals of the vertices (empty if the file has no normals). */
	public ArrayList<Tuple3> getNormals() {
		ArrayList<Tuple3> list = new ArrayList<>();
		float[] n = getMeshData().normals;
		if (n != null) {
			for (int i = 0; i < n.length; i += 3) {
				list.add(new Tuple3(n[i], n[i + 1], n[i + 2]));
			}
		}
		return list;
	}

	/**
	 * Returns the texture coordinates of the vertices (empty if the file has
	 * none).
	 */
	public ArrayList<Tuple3> getTexCoords() {
		ArrayList<Tuple3> list = new ArrayList<>();
		float[] t = getMeshData().texCoords;
		if (t != null) {
			for (int i = 0; i < t.length; i += 2) {
				list.add(new Tuple3(t[i], t[i + 1], 0));
			}
		}
		return list;
	}
}
