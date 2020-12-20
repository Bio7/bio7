/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.spatial;

import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.discrete.Field;
import com.jogamp.opengl.util.awt.TextureRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import static com.jogamp.opengl.GL2.*; // GL2 constants



public class Textures {
	public DynamicTexture dynamicTexture;
	public static TextureRenderer animRenderer;
	public Texture tex;
	private boolean renderImage = false;
	private ImagePlus imp;
	private BufferedImage buff;
	public int texture = 1;
	private ImageProcessor ip;
	private HeightMaps heightMap;
	public boolean createNewTexture = true;
	public boolean showTexture;
	private IPreferenceStore store;
	
	private boolean staticImageImageJ = false;

	public Textures(HeightMaps heightMap) {

		this.heightMap = heightMap;
		store = Bio7Plugin.getDefault().getPreferenceStore();

	}

	public void createImageTexture(GL2 gl) {
		if (tex != null) {
			//tex.destroy(gl);
		}
		createNewTexture = true;
		createTexture(gl);

	}

	void createTexture(GL2 gl) {
		if (renderImage) {
			imageTexture(gl);

		} else {
			renderPaint();
		}
	}

	
	private void imageTexture(GL2 gl) {
		if (staticImageImageJ) {
			if (WindowManager.getImageCount() > 0) {

				imp = WindowManager.getCurrentWindow().getImagePlus();

				if (imp != null) {
					Image img = imp.getImage();
					heightMap.imageWidth = imp.getWidth();
					heightMap.imageHeight = imp.getHeight();
					ip = imp.getProcessor();

					buff = createBufferedImage(img);

					tex = AWTTextureIO.newTexture(GLProfile.getDefault(),buff, true);

					tex.setTexParameteri(gl,GL_TEXTURE_MIN_FILTER, GL_LINEAR);
					tex.setTexParameteri(gl,GL_TEXTURE_MAG_FILTER, GL_LINEAR);
					gl.glBindTexture(GL_TEXTURE_2D, texture);
					// 
				}
			}
		} else {

			initTextures(gl);
		}
	}

	public void renderPaint() {

		if (DynamicTexture.isImagejEnabled()) {
			if (WindowManager.getImageCount() > 0) {
				ImagePlus imp = WindowManager.getCurrentWindow().getImagePlus();
				if (imp != null) {

					animRenderer = new TextureRenderer(imp.getWidth(), imp.getHeight(), false);
					dynamicTexture = new DynamicTexture();
					tex = animRenderer.getTexture();
				}
			}

		} else {

			animRenderer = new TextureRenderer(Field.getWidth() * Field.getQuadSize(), Field.getHeight() * Field.getQuadSize(), false);
			dynamicTexture = new DynamicTexture();

			tex = animRenderer.getTexture();

		}

	}

	public void updateAnimRenderer() {
		int w = animRenderer.getWidth();
		int h = animRenderer.getHeight();
		Graphics2D g2d = animRenderer.createGraphics();

		dynamicTexture.render(w, h, g2d);
		g2d.dispose();
		animRenderer.markDirty(0, 0, w, h);
	}

	public void initTextures(GL2 gl) {
		if (tex != null)
			//tex.destroy(gl);

		tex = createTexture(store.getString("spatialImage"),gl);

	}

	private Texture createTexture(String filename,GL2 gl) {
		Texture t = null;

		try {
			BufferedImage image = ImageIO.read(new File(filename));
			try {
				if (image != null) {
					t = AWTTextureIO.newTexture(GLProfile.getDefault(),image, false);
				}
			} catch (GLException e) {

				e.printStackTrace();
			}
			if (t != null) {
				heightMap.imageHeight = image.getHeight();
				heightMap.imageWidth = image.getWidth();
				t.setTexParameteri(gl,GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				t.setTexParameteri(gl,GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			}
		} catch (IOException e) {
			System.err.println("Image not available! " + filename);
		}
		return t;
	}

	private BufferedImage createBufferedImage(java.awt.Image image) {

		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bi;
	}

	public void setRenderFromFile(boolean renderImage) {
		this.renderImage = renderImage;
	}

	public boolean isRenderImage() {
		return renderImage;
	}

	public boolean isShowTexture() {
		return showTexture;
	}

	public void setShowTexture(boolean showTexture) {
		this.showTexture = showTexture;
	}

	public boolean isStaticImageImageJ() {
		return staticImageImageJ;
	}

	public void setStaticImageImageJ(boolean staticImageImageJ) {
		this.staticImageImageJ = staticImageImageJ;
	}

}
