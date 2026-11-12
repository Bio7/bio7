package com.eco.bio7.rbridge;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;
import com.eco.bio7.rbridge.backend.RBackend;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

/*A class for the configuration of some variables and configurations needed for R embedded in Bio7!*/
public class RConfig {

	/** The configuration for an Rserve connection (see config(RBackend)). */
	public static void config(RConnection con) {
		config(RBackend.of(con));
	}

	/* Defines the functions of an R file of Bio7 in R (the code is sent, R needs no access to the file) */
	private static void loadRCode(RBackend con, String file) {
		try {
			String code = java.nio.file.Files.readString(java.nio.file.Path.of(file), java.nio.charset.StandardCharsets.UTF_8);
			con.assign(".bio7RCode", code);
			con.voidEval("try(eval(parse(text = .bio7RCode), envir = globalenv())); try(rm(.bio7RCode, envir = globalenv()))");
		} catch (IOException | RserveException | RuntimeException e) {
			System.err.println("Bio7: the R functions of " + file + " could not be loaded: " + e.getMessage());
		}
	}

	public static void config(RBackend con) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		
		try {
			Bundle bundle = Platform.getBundle("com.eco.bio7");
			URL url = FileLocator.find(bundle, new Path("rcompletion/calculateRCompletion.R"), null);		
			File file = null;			
			try {
				URL fileUrl = FileLocator.toFileURL(url);
				
				file = new File(fileUrl.getPath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			String scriptPath = file.getAbsolutePath();
			String scriptFolder = file.getParent() + "/";

			String tempPath = store.getString(PreferenceConstants.P_TEMP_R);
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

				tempPath = tempPath.replace("\\", "/");
				scriptPath = scriptPath.replace("\\", "/");
				scriptFolder = scriptFolder.replace("\\", "/");
			}
			/*
			 * Set the max print option to avoid a kind of deadlock in print situations!
			 */
			con.eval("try(options(max.print=5000))");
			/* Make the default completion function in R available! */
			con.eval("try(source('" + scriptPath + "'))");
			/* The package lists of the package views (cached repository index), also for a remote Rserve! */
			loadRCode(con, scriptFolder + "bio7Packages.R");
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
		/*
		 * R runs and the completion helper is defined: the packages of the R scripts
		 * opened before R was started are loaded now (in a job, when R is free)
		 */
		try {
			com.eco.bio7.reditor.actions.PackageCompletionOnOpen.rStarted();
		} catch (RuntimeException | LinkageError e) {
			System.err.println("Bio7: the packages of the opened R scripts could not be loaded: " + e.getMessage());
		}
	}

}
