package com.eco.bio7.util;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

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

	/*
	 * From:
	 * http://stackoverflow.com/questions/4748673/how-can-i-check-the-bitness-of
	 * -my-os-using-java-j2se-not-os-arch/5940770#5940770 Author: ChrisH:
	 * http://stackoverflow.com/users/71109/chrish
	 */
	/**
	 * A method to detect the architecture of the Operating System (32-bit,
	 * 64-bit).
	 * 
	 * @return the architecture (32, 64)
	 */
	public static String getArch() {
		String arch = System.getenv("PROCESSOR_ARCHITECTURE");
		String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");

		String realArch = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? "64" : "32";
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
	 * A method to return the default color as an SWT color.
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

}
