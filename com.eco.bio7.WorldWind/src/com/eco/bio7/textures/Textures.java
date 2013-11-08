/*
Nasa WorldWind plugin
Copyright (C) 2009  M. Austenfeld

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.*/

package com.eco.bio7.textures;

import static javax.media.opengl.GL.GL_LINEAR;
import static javax.media.opengl.GL.GL_TEXTURE_2D;
import static javax.media.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static javax.media.opengl.GL.GL_TEXTURE_MIN_FILTER;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import org.eclipse.jface.preference.IPreferenceStore;
import com.jogamp.opengl.util.awt.TextureRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;



public class Textures {
	public DynamicTexture dynamicTexture;
	public static TextureRenderer animRenderer;
	public Texture tex;
	private boolean renderImage = false;
	private ImagePlus imp;
	private BufferedImage buff;
	public int texture = 1;
	private ImageProcessor ip;	
	public boolean createNewTexture = true;
	public boolean showTexture;
	private IPreferenceStore store;
	
	private boolean staticImageImageJ = false;

	public Textures() {

		
		

	}

	public void createImageTexture(GL2 gl) {
		if (tex != null) {
			//tex.dispose();
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
			//tex.dispose();

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
				
				t.setTexParameteri(gl,GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
				t.setTexParameteri(gl,GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
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
