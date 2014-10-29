package com.eco.bio7.image;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ScanClassPath {
	
	private String pathBundle;

	public String scan(){
		/*
		 * Scan all necessary plugins for libs and calculate the paths to the
		 * libs!
		 */
		String[] bundles = new String[] { "com.eco.bio7", "com.eco.bio7.libs", "com.eco.bio7.javaedit", "com.eco.bio7.image", "com.eco.bio7.WorldWind", "com.eco.bio7.physics",
				"org.eclipse.ui.workbench", "org.eclipse.core.commands" };// "org.eclipse.ui.workbench","org.eclipse.core.commands"
		ArrayList<String> bundlePaths = new ArrayList<String>();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = Platform.getBundle(bundles[i]);

			URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
			URL fileUrl = null;
			try {
				fileUrl = FileLocator.toFileURL(locationUrl);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			pathBundle = fileUrl.getFile();
			bundlePaths.add(File.pathSeparator + pathBundle);

			ManifestElement[] elements = null;
			String requires = (String) bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
			// String
			// requireBundles=(String)bundle.getHeaders().get(Constants.REQUIRE_BUNDLE);
			// System.out.println(requires);

			try {
				elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, requires);
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (elements != null) {
				for (int u = 0; u < elements.length; u++) {
					// System.out.println(File.pathSeparator
					// +bundlePaths.get(i)+elements[u].getValue());
					buf.append(File.pathSeparator + bundlePaths.get(i) + elements[u].getValue());

				}
			}
			/*
			 * try { elements =
			 * ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE,
			 * requireBundles); } catch (BundleException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 * if(elements!=null){ for (int u = 0; u < elements.length; u++) {
			 * //System.out.println( File.pathSeparator
			 * +bundlePaths.get(i)+elements[u].getValue());
			 * buf.append(File.pathSeparator
			 * +bundlePaths.get(i)+elements[u].getValue());
			 * 
			 * } }
			 */

		}

		// System.out.println(Platform.getInstanceLocation().getURL().getPath());
		// System.out.println(Platform.getInstallLocation().getURL().getPath());
		buf.append(File.pathSeparator + bundlePaths.get(0) + "/bin");
		buf.append(File.pathSeparator + bundlePaths.get(2) + "/bin");
		buf.append(File.pathSeparator + bundlePaths.get(3) + "/bin");
		buf.append(File.pathSeparator + bundlePaths.get(4) + "/bin");
		// buf.append(File.pathSeparator+Platform.getInstallLocation().getURL().getPath()+"plugins/org.eclipse.ui.workbench_3.7.0.I20110519-0100.jar");
		// buf.append(File.pathSeparator+Platform.getInstallLocation().getURL().getPath()+"/plugins/org.eclipse.core.commands_3.6.0.I20110111-0800.jar");
		// System.out.println(buf.toString());
		String classpaths = buf.toString();
		return classpaths;
	}
	
	

}
