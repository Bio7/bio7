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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;
import javax.swing.JPanel;
import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.loader3d.OBJModel;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import static javax.media.opengl.GL2.*; // GL2 constants


/**
 * This is a utility class for Jogl (OpenGL) to load and create models and
 * textures.
 * 
 * @author Bio7
 * 
 */
public class SpatialLoader {

	private static GL2 gl;

	protected String objectName;// Name of the model!
	protected String objectPath;// Path to the model!

	public SpatialLoader(GL2 gl_) {
		gl = gl_;

	}

	/**
	 * This method loads an object model from the given path.
	 * 
	 * @param path
	 *            the path to the model.
	 * @param size
	 *            the initial size.
	 * @return an OBJModel instance.
	 */
	public static OBJModel loadModel(String path, float size) {
		OBJModel model = null;
		if (Bio7Dialog.getOS().equals("Windows")) {
			path = path.replace("/", "\\\\");
		}

		File file = new File(path);
		if (file.exists()) {
			String objectPath = file.getParent() + "/";
			String objectName = getFileName(path);
			if (objectName != null) {

				try {
					model = new OBJModel(objectName, objectPath, size, gl, false);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					model = null;

				}

			}
		} else {
			System.out.println("Can't locate the file!");
		}
		return model;
	}
	/**
	 * This method loads an object model from the given path.
	 * 
	 * @param gl
	 *            the GL reference.
	 *            
	 * @param path
	 *            the path to the model.
	 * @param size
	 *            the initial size.
	 * @return an OBJModel instance.
	 */
	public static OBJModel loadModel(GL2 gl,String path, float size) {
		OBJModel model = null;
		if (Bio7Dialog.getOS().equals("Windows")) {
			path = path.replace("/", "\\\\");
		}

		File file = new File(path);
		if (file.exists()) {
			String objectPath = file.getParent() + "/";
			String objectName = getFileName(path);
			if (objectName != null) {

				try {
					model = new OBJModel(objectName, objectPath, size, gl, false);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					model = null;

				}

			}
		} else {
			System.out.println("Can't locate the file!");
		}
		return model;
	}

	protected void loadObjModel() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		grid.model = null;
		// Get the filepath from the pref. store!
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String op = store.getString("objectFile");

		File file = new File(op);
		if (file.exists()) {
			objectPath = file.getParent() + "/";
			objectName = getFileName(op);

			try {
				grid.model = new OBJModel(objectName, objectPath, 1000.0f, gl, false);
			} catch (RuntimeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				grid.model = null;
			}

		}

	}

	/**
	 * This method creates a texture from a given path.
	 * 
	 * @param path
	 *            the path to the texture.
	 * 
	 * @return a Texture instance.
	 */
	public static Texture createTexture(String path, GL2 gl) {
		Texture t = null;
		File file = new File(path);
		if (file.exists()) {
			try {
				BufferedImage image = ImageIO.read(file);
				t = AWTTextureIO.newTexture(GLProfile.getDefault(),image, true);
				t.setTexParameteri(gl,GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				t.setTexParameteri(gl,GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			} catch (IOException e) {
				System.err.println("Image not available! " + path);
			}
		}
		return t;
	}

	/**
	 * This method returns the filename of a given file without the extension.
	 * 
	 * @param path
	 *            the path to the file.
	 * @return returns the filename as a String value.
	 */
	public static String getFileName(String path) {

		String fileName = null;
		String separator = File.separator;

		int pos = path.lastIndexOf(separator);
		int pos2 = path.lastIndexOf(".");

		if (pos2 > -1)
			fileName = path.substring(pos + 1, pos2);
		else
			fileName = path.substring(pos + 1);

		return fileName;
	}

}
