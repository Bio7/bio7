package com.eco.bio7.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

/**
 * A utility class for the ImageJ plugin.
 * 
 * @author M. Austenfeld
 *
 */
public class Util {

	private static Font awtFont;
	private static Color col = null;

	/**
	 * A method to get the ImageJ path.
	 * 
	 * @return the default path of the ImageJ plugin
	 */
	public static String getImageJPath() {

		Bundle bundle = Platform.getBundle("com.eco.bio7.image");

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

	/**
	 * A method to get the default OS font from SWT to AWT.
	 * 
	 * @return the OS font as an AWT font.
	 */
	public static java.awt.Font getOSFontToAwt() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int fontSizeCorrection = store.getInt("FONT_SIZE_CORRECTION");

		Display dis = getDisplay();

		dis.syncExec(new Runnable() {

			public void run() {

				FontData fontData = dis.getSystemFont().getFontData()[0];

				// int resolution = Toolkit.getDefaultToolkit().getScreenResolution();
				int awtFontSize = (int) Math.round((double) fontData.getHeight() * dis.getDPI().y / 72.0);

				/* Font size correction! */

				awtFont = new java.awt.Font(fontData.getName(), fontData.getStyle(), awtFontSize + fontSizeCorrection);
			}
		});

		return awtFont;
	}

	public static Color getSWTBackgroundToAWT() {
		col = new Color(0, 0, 0);
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				IViewSite viewSite = CanvasView.getCanvas_view().getViewSite();
				IWorkbenchWindow workbenchWindow = viewSite.getWorkbenchWindow();
				org.eclipse.swt.graphics.Color colswt = workbenchWindow.getShell().getBackground();
				int r = colswt.getRed();
				int g = colswt.getGreen();
				int b = colswt.getBlue();
				col = new Color(r, g, b);

			}
		});
		return col;

	}

	public static Color getSWTForegroundToAWT() {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				IViewSite viewSite = CanvasView.getCanvas_view().getViewSite();
				IWorkbenchWindow workbenchWindow = viewSite.getWorkbenchWindow();
				org.eclipse.swt.graphics.Color colswt = workbenchWindow.getShell().getForeground();
				int r = colswt.getRed();
				int g = colswt.getGreen();
				int b = colswt.getBlue();
				col = new Color(r, g, b);

			}
		});
		return col;

	}

	public static Color getSWTBackgroundToAWT(Composite parent) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				org.eclipse.swt.graphics.Color colswt = parent.getParent().getBackground();
				int r = colswt.getRed();
				int g = colswt.getGreen();
				int b = colswt.getBlue();
				col = new Color(r, g, b);

			}
		});
		return col;

	}

	public static Color getSWTForegroundToAWT(Composite parent) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				org.eclipse.swt.graphics.Color colswt = parent.getParent().getForeground();
				int r = colswt.getRed();
				int g = colswt.getGreen();
				int b = colswt.getBlue();
				col = new Color(r, g, b);

			}
		});
		return col;

	}

	/**
	 * A method to detect the current OS.
	 * 
	 * @return the OS name as a string
	 */
	public static String getOS() {
		String OS = null;
		String osname = System.getProperty("os.name");
		if (osname.startsWith("Windows")) {
			OS = "Windows";
		} else if (osname.equals("Linux")) {
			OS = "Linux";
		} else if (osname.startsWith("Mac")) {
			OS = "Mac";
		}
		return OS;
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
	public String fileToString(String path) {// this function returns the
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
	 * A method to get the dpi of the display.
	 * 
	 * @return the dpi as type Point.
	 */
	public static Point getDpi() {
		Display dis = getDisplay();
		return dis.getDPI();
	}

	/**
	 * A method to return the primary monitor zoom.
	 * 
	 * @return the zoom value as integer.
	 */
	public static int getZoom() {
		Display dis = getDisplay();
		Monitor primary = dis.getPrimaryMonitor();
		return primary.getZoom();
	}

	/**
	 * A method to return the scale factor (1.0, 2.0).
	 * 
	 * @return the scale factor as type double
	 */
	public static double getScale() {

		int deviceZoom = DPIUtil.getDeviceZoom();
		double scale = deviceZoom / 100.0;
		return scale;

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

	// The source for the following method from:
	// https://stackoverflow.com/questions/20767708/how-do-you-detect-a-retina-display-in-java#20767802
	public static boolean isMacRetinaDisplay() {
		if (getZoom() == 200) {
			return true;
		}
		return false;
	}

	/**
	 * Opens the view with the specified id.
	 * 
	 * @param id the id as a string value.
	 */
	public static void openView(final String id) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					page.showView(id);
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

	}

	/**
	 * Activates the view with the specified id.
	 * 
	 * @param id the id as a string value.
	 */
	public static void activateView(final String id) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IViewPart viewPart = page.findView(id);
				if (viewPart != null) {
					page.activate(viewPart);
				}

			}
		});

	}

	// Method source from: https://github.com/archimatetool/archi/

	/**
	 * Compare two version numbers with the format 1, 1.1, or 1.1.1
	 * 
	 * From
	 * http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
	 * 
	 * @param newer The version string considered to be newer
	 * @param older The version string considered to be older
	 * @return -1 if newer < older <br/>
	 *         0 if newer == older <br/>
	 *         1 if newer > older
	 */
	public static int compareVersionNumbers(String newer, String older) {
		return Integer.compare(versionNumberAsInt(newer), versionNumberAsInt(older));
	}

	/**
	 * Convert a version number to an integer
	 * 
	 * @param version
	 * @return integer
	 */

	// Method source from: https://github.com/archimatetool/archi/
	public static int versionNumberAsInt(String version) {
		String[] vals = version.split("\\.");

		if (vals.length == 1) {
			return Integer.parseInt(vals[0]);
		}
		if (vals.length == 2) {
			return (Integer.parseInt(vals[0]) << 16) + (Integer.parseInt(vals[1]) << 8);
		}
		if (vals.length == 3) {
			return (Integer.parseInt(vals[0]) << 16) + (Integer.parseInt(vals[1]) << 8) + Integer.parseInt(vals[2]);
		}

		return 0;
	}

	// Method source from: https://github.com/archimatetool/archi/

	/**
	 * Compare given version to current OS version and see if the current OS version
	 * is greater than the given version
	 * 
	 * @param version The version string to compare to system OS version
	 * @return -1 if newer < older <br/>
	 *         0 if newer == older <br/>
	 *         1 if newer > older
	 */
	public static int compareOSVersion(String version) {
		String current = System.getProperty("os.version"); //$NON-NLS-1$
		return compareVersionNumbers(current, version);
	}

}
