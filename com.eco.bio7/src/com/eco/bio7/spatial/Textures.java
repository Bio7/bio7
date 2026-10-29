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
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.Bio7Plugin;
import com.jogamp.opengl.util.awt.TextureRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import static com.jogamp.opengl.GL2.*; // GL2 constants

public class Textures {
	public DynamicTexture dynamicTexture;
	public static TextureRenderer animRenderer;

	private static final int EMPTY_TEXTURE_SIZE = 1000;
	public Texture tex;
	private boolean renderImage = false;
	private ImagePlus imp;
	private BufferedImage buff;
	public int texture = 1;
	/* The context of the texture of an image (see newTexture) */
	private com.jogamp.opengl.GLContext texContext;
	private ImageProcessor ip;
	private HeightMaps heightMap;
	public boolean createNewTexture = true;
	public boolean showTexture;
	private IPreferenceStore store;

	private boolean staticImageImageJ = false;
	/* The title of the ImageJ image or the name of the file of a static texture! */
	private String sourceName = "";
	private Runnable updateListener;

	public Textures(HeightMaps heightMap) {

		this.heightMap = heightMap;
		store = Bio7Plugin.getDefault().getPreferenceStore();

	}

	public void createImageTexture(GL2 gl, GLAutoDrawable drawable) {
		if (tex != null) {
			// tex.destroy(gl);
		}
		createNewTexture = true;
		createTexture(gl, drawable);

	}

	void createTexture(GL2 gl, GLAutoDrawable drawable) {
		if (renderImage) {
			imageTexture(gl, drawable);

		} else {
			renderPaint();
		}
	}

	private void imageTexture(GL2 gl, GLAutoDrawable drawable) {
		if (staticImageImageJ) {
			if (WindowManager.getImageCount() > 0) {

				imp = WindowManager.getCurrentWindow().getImagePlus();

				if (imp != null) {
					heightMap.imageWidth = imp.getWidth();
					heightMap.imageHeight = imp.getHeight();
					ip = imp.getProcessor();
					sourceName = imp.getTitle();

					buff = createImageWithLut(imp);
					newTexture(gl, buff);
				}
			}
		} else {

			initTextures(gl, drawable);
		}
	}

	/*
	 * The texture of the image in the current context (called in the display or
	 * the init of the 3D view: the context is current). Not later with
	 * GLAutoDrawable.invoke(false, ...): the texture was not created when its
	 * parameters were set (on Windows no texture or the texture of another
	 * context). The former texture is released.
	 */
	private boolean newTexture(GL2 gl, BufferedImage image) {
		try {
			Texture created = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);
			created.setTexParameteri(gl, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			created.setTexParameteri(gl, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			Texture old = tex;
			com.jogamp.opengl.GLContext oldContext = texContext;
			tex = created;
			texContext = gl.getContext();
			/* Only a texture of this context (after a new context the name can be another texture) */
			if (old != null && oldContext == texContext && (animRenderer == null || old != animRenderer.getTexture())) {
				old.destroy(gl);
			}
			notifyListener();
			return true;
		} catch (RuntimeException e) {
			System.err.println("The texture can't be created (" + image.getWidth() + " x " + image.getHeight() + "): " + e);
			e.printStackTrace();
			return false;
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

			/* No dynamic source: an empty texture (the size of the former default discrete field)! */
			animRenderer = new TextureRenderer(EMPTY_TEXTURE_SIZE, EMPTY_TEXTURE_SIZE, false);
			dynamicTexture = new DynamicTexture();

			tex = animRenderer.getTexture();

		}
		notifyListener();

	}

	public void updateAnimRenderer() {
		int w = animRenderer.getWidth();
		int h = animRenderer.getHeight();
		Graphics2D g2d = animRenderer.createGraphics();

		dynamicTexture.render(w, h, g2d);
		g2d.dispose();
		animRenderer.markDirty(0, 0, w, h);
	}

	public void initTextures(GL2 gl, GLAutoDrawable drawable) {
		String path = null;
		// if (tex != null)
		// tex.destroy(gl);
		path = store.getString("spatialImage");
		if (path != null) {
			tex = createTexture(store.getString("spatialImage"), gl, drawable);
		}

	}

	private Texture createTexture(String filename, GL2 gl, GLAutoDrawable drawable) {

		BufferedImage image;
		try {
			image = ImageIO.read(new File(filename));
			if (image == null) {
				/* Not a supported image file! */
				System.err.println("The texture file can't be read: " + filename);
				return tex;
			}
			sourceName = new File(filename).getName();
			if (newTexture(gl, image)) {
				heightMap.imageHeight = image.getHeight();
				heightMap.imageWidth = image.getWidth();
			}
		} catch (IOException e) {
			System.err.println(e);
		}
		return tex;
	}

	/**
	 * Creates an image of the ImageJ image with the current LUT and display range.
	 * The AWT image of ImagePlus.getImage() is cached and not updated if a LUT is
	 * applied to a displayed image!
	 * 
	 * @param imp the ImageJ image
	 * @return an ARGB image like displayed in ImageJ
	 */
	public static BufferedImage createImageWithLut(ImagePlus imp) {
		ImageProcessor ip = imp.getProcessor();
		if (ip == null || imp.isComposite()) {
			return createBufferedImage(imp.getImage());
		}
		int w = ip.getWidth();
		int h = ip.getHeight();
		int[] argb = new int[w * h];
		if (ip instanceof ColorProcessor) {
			int[] pixels = (int[]) ip.getPixels();
			for (int i = 0; i < argb.length; i++) {
				argb[i] = 0xff000000 | pixels[i];
			}
		} else {
			byte[] pixels;
			ColorModel cm;
			if (ip instanceof ByteProcessor) {
				/* The current color model contains the display range of 8-bit images! */
				pixels = (byte[]) ip.getPixels();
				cm = ip.getCurrentColorModel();
			} else {
				/* 16-bit and 32-bit images are scaled to the display range! */
				pixels = (byte[]) ip.convertToByteProcessor(true).getPixels();
				cm = ip.getColorModel();
			}
			int[] lut = new int[256];
			for (int i = 0; i < 256; i++) {
				lut[i] = 0xff000000 | cm.getRGB(i);
			}
			for (int i = 0; i < argb.length; i++) {
				argb[i] = lut[pixels[i] & 0xff];
			}
		}
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		bi.setRGB(0, 0, w, h, argb, 0, w);
		return bi;
	}

	private static BufferedImage createBufferedImage(java.awt.Image image) {

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

	/**
	 * Returns a description of the displayed texture (source and size).
	 * 
	 * @return the description
	 */
	public String getInfo() {
		if (!showTexture || tex == null) {
			return "No texture displayed";
		}
		String source;
		if (!renderImage) {
			source = DynamicTexture.isImagejEnabled() ? "Dynamic texture (ImageJ)" : "Empty dynamic texture";
		} else if (staticImageImageJ) {
			source = "Static texture (ImageJ): " + sourceName;
		} else {
			source = "Static texture (file): " + sourceName;
		}
		return source + ", " + tex.getWidth() + " x " + tex.getHeight();
	}

	/**
	 * A listener which is called (in the OpenGL thread) when a texture has been
	 * created.
	 * 
	 * @param listener the listener
	 */
	public void setUpdateListener(Runnable listener) {
		this.updateListener = listener;
	}

	private void notifyListener() {
		Runnable listener = updateListener;
		if (listener != null) {
			listener.run();
		}
	}

	public boolean isStaticImageImageJ() {
		return staticImageImageJ;
	}

	public void setStaticImageImageJ(boolean staticImageImageJ) {
		this.staticImageImageJ = staticImageImageJ;
	}

}
