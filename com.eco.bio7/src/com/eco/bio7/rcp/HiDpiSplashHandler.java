/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.rcp;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageFileNameProvider;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.osgi.framework.Bundle;

/**
 * The splash handler of Bio7. Like the default Eclipse splash handler it shows
 * the progress bar (product properties startupProgressRect, startupMessageRect,
 * startupForegroundColor). In addition the splash image is displayed sharp on
 * high resolution displays: the workbench loads splash.bmp with 100% only, so
 * the background is replaced by an image with splash.bmp (100%) and
 * splash@2x.png (200%).
 */
public class HiDpiSplashHandler extends BasicSplashHandler {

	private static final String SPLASH = "splash.bmp";
	private static final String SPLASH_2X = "splash@2x.png";

	@Override
	public void init(Shell splash) {
		super.init(splash);
		IProduct product = Platform.getProduct();
		String progress = product == null ? null : product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
		String message = product == null ? null : product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
		String foreground = product == null ? null : product.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR);
		setProgressRect(StringConverter.asRectangle(progress, new Rectangle(10, 10, 300, 15)));
		setMessageRect(StringConverter.asRectangle(message, new Rectangle(10, 35, 300, 15)));
		int rgb;
		try {
			rgb = Integer.parseInt(foreground, 16);
		} catch (Exception e) {
			rgb = 0x000000;
		}
		setForeground(new RGB((rgb & 0xFF0000) >> 16, (rgb & 0xFF00) >> 8, rgb & 0xFF));
		setHiDpiBackground(splash);
		/* Creates the progress bar now! */
		getContent();
	}

	/* Replaces the background image by an image with a 200% version (if available)! */
	private static void setHiDpiBackground(Shell splash) {
		Bundle bundle = Platform.getBundle("com.eco.bio7");
		String path1x = filePath(bundle, SPLASH);
		String path2x = filePath(bundle, SPLASH_2X);
		if (path1x == null || path2x == null) {
			return;
		}
		try {
			/* Only 100% and 200%: for other zoom levels (e.g., 150% on Windows) SWT scales one of them! */
			Image image = new Image(splash.getDisplay(), (ImageFileNameProvider) zoom -> zoom == 100 ? path1x : zoom == 200 ? path2x : null);
			splash.setBackgroundImage(image);
			splash.addDisposeListener(e -> image.dispose());
		} catch (RuntimeException e) {
			/* The image of the workbench stays! */
			e.printStackTrace();
		}
	}

	private static String filePath(Bundle bundle, String name) {
		if (bundle == null) {
			return null;
		}
		URL url = bundle.getEntry(name);
		if (url == null) {
			return null;
		}
		try {
			File file = new File(FileLocator.toFileURL(url).getPath());
			return file.exists() ? file.getAbsolutePath() : null;
		} catch (IOException e) {
			return null;
		}
	}
}
