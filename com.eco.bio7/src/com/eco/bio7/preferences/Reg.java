package com.eco.bio7.preferences;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;

public class Reg {
	/*
	 * A class to set paths from the plugin location (R) or the registry
	 * (LibreOffice if available!).
	 *
	 */

	public static String setPrefReg(String progDescr) {
		String returnPath = null;
		if (progDescr.equals("r")) {
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
				returnPath = file.getAbsolutePath();

			}

		} else if (progDescr.equals("libreoffice")) {

			boolean keyExists = false;

			keyExists = Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE,
					"SOFTWARE\\LibreOffice\\UNO\\InstallPath");

			if (keyExists) {

				try {
					returnPath = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE,
							"SOFTWARE\\LibreOffice\\UNO\\InstallPath", null);// null to get (default) reg value!
				} catch (Win32Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				returnPath = "C:\\";

			}
		}
		return returnPath;
	}

}
