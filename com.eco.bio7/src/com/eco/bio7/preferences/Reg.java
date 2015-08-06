package com.eco.bio7.preferences;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;
import com.eco.bio7.Bio7Plugin;

public class Reg {
		
	private static String[] s = Messages.getString("LibreOffice.0").split(",");
	private static String[] OOVERSION = s;
	private static String roo;//Temporary variable for the path!
	/*
	 * If the plugin is not available determine the location from the registry.
	 * If the plugin is present set the path to the plugin as the default!
	 */
	public static String setPrefReg(String path) {
		if (path.equals("r")) {
			if (Platform.getBundle("Bundled_R") == null) {
				System.out.println("No bundle available! Please adjust the path to R manually!");

			} else {
				Bundle bundlenew = Platform.getBundle("Bundled_R");

				URL locationUrl = FileLocator.find(bundlenew, new Path("/R"), null);
				URL fileUrl = null;
				try {
					fileUrl = FileLocator.toFileURL(locationUrl);
				} catch (IOException e2) {

					e2.printStackTrace();
				}

				File file = new File(fileUrl.getFile());
				path = file.getAbsolutePath();

				installRegPath(path);

			}

		} else if (path.equals("libreoffice")) {
			// Path determined from the registry with reflection!
			roo = null;
			for (int i = 0; i < OOVERSION.length; i++) {
				try {
					roo = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\LibreOffice\\LibreOffice\\" + OOVERSION[i], "Path");

				} catch (IllegalArgumentException e) {

					e.printStackTrace();
				} catch (IllegalAccessException e) {

					e.printStackTrace();
				} catch (InvocationTargetException e) {

					e.printStackTrace();
				}

				if (roo != null) {
					roo = roo.replace("soffice.exe", "");
					path = roo;
				}

			}
			if (path == null||path.equals("libreoffice")) {
				path = "C:\\";
			}
		}
		return path;
	}

	private static void installRegPath(String path_to_r) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean bool = store.getBoolean(PreferenceConstants.P_BOOLEAN);

		if (bool == true) {
			try {
				WinRegistry.createKey(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\R-core\\R");
			} catch (IllegalArgumentException e) {

				e.printStackTrace();
			} catch (IllegalAccessException e) {

				e.printStackTrace();
			} catch (InvocationTargetException e) {

				e.printStackTrace();
			}
			try {
				WinRegistry.writeStringValue(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\R-core\\R", "Current Version", "3.2.1");
				WinRegistry.writeStringValue(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\R-core\\R", "InstallPath", path_to_r);
				WinRegistry.writeStringValue(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\R-core\\R\\3.2.1", "InstallPath", path_to_r);
			} catch (IllegalArgumentException e) {

				e.printStackTrace();
			} catch (IllegalAccessException e) {

				e.printStackTrace();
			} catch (InvocationTargetException e) {

				e.printStackTrace();
			}

		}

	}

}
