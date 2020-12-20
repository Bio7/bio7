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

import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;


public class DynamicTexture {

	private ImageJ ij;
	private ImagePlus imp;
	private BufferedImage buff;
	private WritableRaster raster;
	private static boolean imagejEnabled;

	public DynamicTexture() {

	}

	public void render(int w, int h, Graphics2D g2) {
		
			if (WindowManager.getImageCount() > 0) {

				imp = WindowManager.getCurrentWindow().getImagePlus();
				
				if (imp != null) {
					Image img = imp.getImage();
					g2.drawImage(img, 0, 0, imp.getWidth(), imp.getHeight(), null);
				}
				
				if(buff!=null){
					g2.drawImage(buff, 0, 0, imp.getWidth(), imp.getHeight(), null);
				}
			}
			
			
		

	}

	public static boolean isImagejEnabled() {
		return imagejEnabled;
	}

	public static void setImagejEnabled(boolean imagejEnabled) {
		DynamicTexture.imagejEnabled = imagejEnabled;
	}
}
