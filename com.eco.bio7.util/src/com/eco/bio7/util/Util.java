package com.eco.bio7.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

public class Util {
	private static Color col;
	private static org.eclipse.swt.graphics.Color colSwt;

	/**
	 * A method to detetct the Operating System.
	 * 
	 * @return the name of the Operating System (Windows, Linux, Mac)
	 */
	public static String getOS() {
		String OS = null;
		String osname = System.getProperty("os.name"); //$NON-NLS-1$
		if (osname.startsWith("Windows")) { //$NON-NLS-1$
			OS = "Windows"; //$NON-NLS-1$
		} else if (osname.equals("Linux")) { //$NON-NLS-1$
			OS = "Linux"; //$NON-NLS-1$
		} else if (osname.startsWith("Mac")) { //$NON-NLS-1$
			OS = "Mac"; //$NON-NLS-1$
		}
		return OS;
	}

	/*
	 * From:
	 * http://stackoverflow.com/questions/4748673/how-can-i-check-the-bitness-of
	 * -my-os-using-java-j2se-not-os-arch/5940770#5940770 Author: ChrisH:
	 * http://stackoverflow.com/users/71109/chrish
	 */
	/**
	 * A method to detect the architecture of the Operating System (32-bit, 64-bit).
	 * 
	 * @return the architecture (32, 64)
	 */
	public static String getArch() {
		String arch = System.getenv("PROCESSOR_ARCHITECTURE"); //$NON-NLS-1$
		String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432"); //$NON-NLS-1$

		String realArch = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? "64" : "32"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return realArch;
	}

	public File[] ListFilesDirectory(File filedirectory, final String[] extensions) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of the file or directory inside Bio7.
				// String filename = children[i];
			}
		}

		// Filter the extension of the file.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(extensions[0]) || name.endsWith(extensions[1]) || name.endsWith(extensions[2]) || name.endsWith(extensions[3]) || name.endsWith(extensions[4])
						|| name.endsWith(extensions[5]));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}

	public File[] ListFileDirectory(File filedirectory, final String extension) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of the file or directory inside Bio7.
				String filename = children[i];
			}
		}

		// Filter the extension of the file.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(extension));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}

	/**
	 * Returns a string representation of the file.
	 * 
	 * @param path
	 * @return
	 */
	public static String fileToString(String path) {// this function returns the
		// File as a String
		FileInputStream fileinput = null;
		try {
			fileinput = new FileInputStream(path);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		int filetmp = 0;
		try {
			filetmp = fileinput.available();
		} catch (IOException e) {

			e.printStackTrace();
		}
		byte bitstream[] = new byte[filetmp];
		try {
			fileinput.read(bitstream);
		} catch (IOException e) {

			e.printStackTrace();
		}
		String content = new String(bitstream);
		return content;
	}

	/**
	 * Returns a platform shell for dialogs, etc.
	 * 
	 * @return a shell
	 */
	public static Shell getShell() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows.length > 0) {
				return windows[0].getShell();
			}
		} else {
			return window.getShell();
		}
		return null;
	}

	/**
	 * Returns a default display.
	 * 
	 * @return a display
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null)
			display = Display.getDefault();
		return display;
	}

	/**
	 * A method to return the default color as an AWT color.
	 * 
	 * @return a AWT color object
	 */
	public static Color getSWTBackgroundToAWT() {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				org.eclipse.swt.graphics.Color colswt = getShell().getBackground();
				int r = colswt.getRed();
				int g = colswt.getGreen();
				int b = colswt.getBlue();
				col = new Color(r, g, b);

			}
		});
		return col;

	}

	/**
	 * A method to return the default foregound color as an AWT color.
	 * 
	 * @return a AWT color object
	 */
	public static Color getSWTForegroundToAWT() {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				org.eclipse.swt.graphics.Color colswt = getShell().getForeground();
				int r = colswt.getRed();
				int g = colswt.getGreen();
				int b = colswt.getBlue();
				col = new Color(r, g, b);

			}
		});
		return col;

	}

	/**
	 * A method to return the default color as an SWT color.
	 * 
	 * @return
	 * 
	 * @return a SWT color object
	 */
	public static org.eclipse.swt.graphics.Color getSWTBackgroundColor() {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				colSwt = getShell().getBackground();

			}
		});
		return colSwt;

	}

	/**
	 * A method to return the default color as an SWT color.
	 * 
	 * @return
	 * 
	 * @return a SWT color object
	 */
	public static org.eclipse.swt.graphics.Color getSWTForegroundColor() {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				colSwt = getShell().getForeground();

			}
		});
		return colSwt;

	}

	/**
	 * A method to activate the current editor.
	 * 
	 * @param editor
	 */
	public static void activateEditorPage(final IEditorPart editor) {
		IEditorSite site = editor.getEditorSite();
		final IWorkbenchPage page = site.getPage();
		Display display = site.getShell().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				page.activate(editor);
			}
		});
		if (editor != page.getActiveEditor())
			throw new RuntimeException("Editor couldn't activated"); //$NON-NLS-1$
	}

	/**
	 * A method to detect if the dark theme is active.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isThemeBlack() {
		boolean themeBlack = false;
		IWorkbench workbench = PlatformUI.getWorkbench();
		MApplication application = workbench.getService(MApplication.class);
		IEclipseContext context = application.getContext();
		IThemeEngine engine = context.get(IThemeEngine.class);
		ITheme theme = engine.getActiveTheme();
		// System.out.println(theme.getLabel());

		// IPreferenceStore workbenchStore =
		// IDEWorkbenchPlugin.getDefault().getPreferenceStore();
		// System.out.print(ThemeHelper.getEngine().getActiveTheme().getId());
		if (theme != null) {
			String activeTheme = theme.getId();
			/*
			 * We use a black style if the CSS is the dark theme or the darkest dark theme!
			 */
			if (activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_1) || activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_2)
					|| activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_3) || activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_4)
					|| activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_5)) {

				themeBlack = true;

			}
		}
		return themeBlack;
	}
	// from http://www.java2s.com/Tutorial/Java/0280__SWT/ConvertbetweenSWTImageandAWTBufferedImage.htm

	public static BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
			BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					pixelArray[0] = rgb.red;
					pixelArray[1] = rgb.green;
					pixelArray[2] = rgb.blue;
					raster.setPixels(x, y, 1, 1, pixelArray);
				}
			}
			return bufferedImage;
		} else {
			RGB[] rgbs = palette.getRGBs();
			byte[] red = new byte[rgbs.length];
			byte[] green = new byte[rgbs.length];
			byte[] blue = new byte[rgbs.length];
			for (int i = 0; i < rgbs.length; i++) {
				RGB rgb = rgbs[i];
				red[i] = (byte) rgb.red;
				green[i] = (byte) rgb.green;
				blue[i] = (byte) rgb.blue;
			}
			if (data.transparentPixel != -1) {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
			} else {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
			}
			BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					pixelArray[0] = pixel;
					raster.setPixel(x, y, pixelArray);
				}
			}
			return bufferedImage;
		}
	}

	/**
	 * A method to get the plugin path.
	 * 
	 * @return the default path of a plugin
	 */
	public static String getPluginPathString(String id) {

		Bundle bundle = Platform.getBundle(id);

		URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationUrl);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String path = new File(fileUrl.getFile()).toString();
		return path;
	}

}
