package com.eco.bio7.rbridge;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

/*A class for the configuration of some variables and configurations needed for R embedded in Bio7!*/
public class RConfig {

	public static void config(RConnection con) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		/*
		 * Set the max print option to avoid a kind of deadlock in print situations!
		 */
		try {
			Bundle bundle = Platform.getBundle("com.eco.bio7");
			URL fileURL = bundle.getEntry("rcompletion/calculateRCompletion.R");
			// URL scriptPath = fileURL.toExternalForm();
			File file = null;
			try {
				file = new File(FileLocator.resolve(fileURL).toURI());
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			String scriptPath = file.getAbsolutePath();
			String scriptFolder = file.getParent()+"/";
			
			String tempPath = store.getString(PreferenceConstants.P_TEMP_R);
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

				tempPath = tempPath.replace("\\", "/");
				scriptPath = scriptPath.replace("\\", "/");
				scriptFolder= scriptFolder.replace("\\", "/");
			}
			// System.out.println(tempPath);
			con.eval("try(options(max.print=5000))");
			/* Make the default completion function in R available! */
			con.eval("try(source('" + scriptPath + "'))");
			/* Define a new R environment! */
			con.eval("try(.bio7TempEnvPath<- new.env())");
			/* Set Bio7 scriptFolder path in R! */
			con.eval("try(assign(\"scriptFolder\", \"" + scriptFolder + "\", env=.bio7TempEnvPath))");
			/* Set Bio7 temp path in R! */
			con.eval("try(assign(\"pathTemp\", \"" + tempPath + "\", env=.bio7TempEnvPath))");
			

			// con.eval("try(setHook(packageEvent(\"spatstat\",\"onLoad\"),\"writeFunctionDef\"))");
			/*
			 * Set the default install location for the add on packages!
			 */

			String rPackages = store.getString("InstallLocation");
			String dev = store.getString("DEVICE_DEFINITION");
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

				rPackages = rPackages.replace("\\", "/");
				dev = dev.replace("\\", "/");
				con.eval("try(.libPaths(\"" + rPackages + "\"))");
			}
			/* For Linux and Mac if there is no path we will take the default defined! */
			else {
				if (rPackages.isEmpty() == false) {
					con.eval("try(.libPaths(\"" + rPackages + "\"))");
				}
			}

			/* Set the default device! */
			boolean customDevice = store.getBoolean("USE_CUSTOM_DEVICE");
			if (customDevice) {
				con.eval(dev);
			}
			/* Set the custom startup preferences options! */
			String rStartupArgs = store.getString("R_STARTUP_ARGS");
			rStartupArgs = rStartupArgs.replace('\r', ' ');// Replace
			con.eval("" + rStartupArgs + "");

		} catch (RserveException e1) {

			// e1.printStackTrace();
			System.out.println("Error occured!");
		}
	}

}
