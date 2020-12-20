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

import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import java.awt.Graphics2D;
import java.awt.Image;
import com.eco.bio7.discrete.Quad2d;

public class DynamicTexture {

	private ImageJ ij;
	private ImagePlus imp;
	private static boolean imagejEnabled;

	public DynamicTexture() {

	}

	public void render(int w, int h, Graphics2D g2) {
		if (imagejEnabled == false) {
			if (Quad2d.getQuad2dInstance() != null) {

				Quad2d.getQuad2dInstance().malen(g2);
			}
		} else {
			if (WindowManager.getImageCount() > 0) {

				try {
					imp = WindowManager.getCurrentWindow().getImagePlus();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (imp != null) {
					Image img = imp.getImage();
					g2.drawImage(img, 0, 0, imp.getWidth(), imp.getHeight(), null);
				}
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
