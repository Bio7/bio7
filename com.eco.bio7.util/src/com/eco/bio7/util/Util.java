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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
				return (name.endsWith(extensions[0]) || name.endsWith(extensions[1]) || name.endsWith(extensions[2])
						|| name.endsWith(extensions[3]) || name.endsWith(extensions[4])
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
	 * Returns true if the dark theme is active or will be activated at startup.
	 * Unlike {@link #isThemeBlack()} this method can be called before the
	 * workbench has been created (e.g., in a preference initializer). Like the
	 * Eclipse theme engine the theme of the preferences is used and without a
	 * selected theme the dark theme if the operating system uses a dark theme.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isDarkTheme() {
		String id = Platform.getPreferencesService().getString("org.eclipse.e4.ui.css.swt.theme", "themeid", null, null);
		if (id == null || id.isEmpty()) {
			return Display.isSystemDarkTheme() && !"true".equalsIgnoreCase(System.getProperty("org.eclipse.e4.ui.css.theme.disableOSDarkThemeInherit"));
		}
		return isDarkThemeId(id);
	}

	/**
	 * Calls the listener (in the UI thread) when the theme is changed between a
	 * light and a dark theme, e.g. to update the colors of the editors. The
	 * argument is true for a dark theme.
	 * 
	 * @param listener the listener.
	 */
	public static void addThemeListener(java.util.function.Consumer<Boolean> listener) {
		org.osgi.framework.BundleContext context = org.osgi.framework.FrameworkUtil.getBundle(Util.class).getBundleContext();
		if (context == null) {
			return;
		}
		final boolean[] last = { isDarkTheme() };
		java.util.Dictionary<String, Object> properties = new java.util.Hashtable<String, Object>();
		properties.put(org.osgi.service.event.EventConstants.EVENT_TOPIC, IThemeEngine.Events.THEME_CHANGED);
		org.osgi.service.event.EventHandler handler = event -> {
			Object theme = event.getProperty(IThemeEngine.Events.THEME);
			if (!(theme instanceof ITheme)) {
				return;
			}
			boolean dark = isDarkThemeId(((ITheme) theme).getId());
			Runnable update = () -> {
				if (dark != last[0]) {
					last[0] = dark;
					listener.accept(dark);
				}
			};
			if (Display.getCurrent() != null) {
				update.run();
			} else {
				Display.getDefault().asyncExec(update);
			}
		};
		context.registerService(org.osgi.service.event.EventHandler.class, handler, properties);
	}

	/**
	 * Removes the stored colors which are equal to the default color of the light
	 * or the dark theme, so that these colors follow the theme. E.g., older Bio7
	 * versions stored the colors of the light theme, which then were also used in
	 * the dark theme. Colors which have been changed by the user are kept.
	 * 
	 * @param store    the preference store of an editor.
	 * @param defaults sets the default colors of the light (false) or the dark
	 *                 (true) theme in the given store.
	 */
	public static void followThemeColors(org.eclipse.jface.preference.IPreferenceStore store, java.util.function.BiConsumer<org.eclipse.jface.preference.IPreferenceStore, Boolean> defaults) {
		java.util.Map<String, String> light = themeDefaults(defaults, false);
		java.util.Map<String, String> dark = themeDefaults(defaults, true);
		java.util.Set<String> names = new java.util.HashSet<String>(light.keySet());
		names.addAll(dark.keySet());
		for (String name : names) {
			if (store.contains(name) && !store.isDefault(name)) {
				String value = store.getString(name);
				if (value.equals(light.get(name)) || value.equals(dark.get(name))) {
					store.setToDefault(name);
				}
			}
		}
	}

	/* The default values of a theme (recorded with a temporary store)! */
	private static java.util.Map<String, String> themeDefaults(java.util.function.BiConsumer<org.eclipse.jface.preference.IPreferenceStore, Boolean> defaults, boolean dark) {
		java.util.Map<String, String> values = new java.util.HashMap<String, String>();
		defaults.accept(new org.eclipse.jface.preference.PreferenceStore() {
			@Override
			public void setDefault(String name, String value) {
				values.put(name, value);
			}
		}, dark);
		return values;
	}

	/**
	 * Returns the open (restored) editors of the given type in all workbench
	 * windows.
	 * 
	 * @param type the class of the editors.
	 * @return a list of editors.
	 */
	public static <T> java.util.List<T> getOpenEditors(Class<T> type) {
		java.util.List<T> editors = new java.util.ArrayList<T>();
		if (!PlatformUI.isWorkbenchRunning()) {
			return editors;
		}
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (org.eclipse.ui.IEditorReference ref : page.getEditorReferences()) {
					IEditorPart editor = ref.getEditor(false);
					if (type.isInstance(editor)) {
						editors.add(type.cast(editor));
					}
				}
			}
		}
		return editors;
	}

	/* True if the theme id is one of the dark themes! */
	private static boolean isDarkThemeId(String activeTheme) {
		return activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_1) || activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_2)
				|| activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_3) || activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_4)
				|| activeTheme.startsWith(MessagesThemeIDsSettings.Theme_Black_5);
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
			if (isDarkThemeId(activeTheme)) {

				themeBlack = true;

			}
		}
		return themeBlack;
	}
	// from
	// http://www.java2s.com/Tutorial/Java/0280__SWT/ConvertbetweenSWTImageandAWTBufferedImage.htm

	public static BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
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
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
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

	/**
	 * A method to adjust the height of a text widget.
	 * 
	 * @param text the text widget
	 * @param newFont a new font object
	 * @param parent a parent composite for a temporary text to calculate a default height.
	 */
	public static void updateTextHeight(Text text, Font newFont, Composite parent) {
		
		Text temp = new Text(parent, SWT.BORDER);
		int defaultHeight = temp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		temp.dispose();
		
		text.setFont(newFont);

		// Calculate new preferred size based on the new font
		int preferredHeight = text.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

		// Ensure it doesn't go below the original system default
		int finalHeight = Math.max(preferredHeight, defaultHeight);

		// Update LayoutData (assuming GridLayout is used)
		GridData data = (GridData) text.getLayoutData();
		data.heightHint = finalHeight;

		// Force the parent to re-evaluate the layout
		text.getParent().layout(true);
	}

}
