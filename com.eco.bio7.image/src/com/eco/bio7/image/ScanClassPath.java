package com.eco.bio7.image;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.eco.bio7.javaeditor.Bio7EditorPlugin;

public class ScanClassPath {
	
	private String pathBundle;

	
	String[] bundles = new String[] { "com.eco.bio7", "com.eco.bio7.libs", "com.eco.bio7.javaedit", "com.eco.bio7.image", "com.eco.bio7.WorldWind",
			"com.eco.bio7.scenebuilder", "com.eco.bio7.browser","Bundled_R"  };// "org.eclipse.ui.workbench","org.eclipse.core.commands"

	
	
	

	public String scan() {
		/*
		 * Scan all necessary plugins for libs and calculate the paths to the
		 * libs decoupled from JDT because we still need this for the custom Java compiler, Flow editor and Bio7 model import!
		 */
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		
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

			/*
			 * Get the *.jar list from the Bio7 Java preferences and add them to
			 * the classpath!
			 */
			String libs = store.getString("javaLibs");
			String[] conv = convert(libs);
			for (int j = 0; j < conv.length; j++) {

				buf.append(File.pathSeparator + conv[j].replace("\\", "/"));

			}

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
			

		}
		
		/* Here we add the required Eclipse *.jars! 
		for (int i = 0; i < bundlesEclipse.length; i++) {
			Bundle bundle = Platform.getBundle(bundlesEclipse[i]);
			String loc = bundle.getLocation().substring(15);
			//System.out.println("loc: " + loc);
			Eclipse PDE and exported RCP paths are different (absolute vs. relative)!
			if (loc.startsWith("/")) {
				loc = loc.substring(1);
			}
			java.nio.file.Path path;
            Calculate an absolute path to the resource. Exported RCP has a relative path!
			path = Paths.get(loc);

			//System.out.println("path:" + File.pathSeparator + path.toAbsolutePath().toString());
			buf.append(File.pathSeparator + path.toAbsolutePath().toString());

			// System.out.println(File.pathSeparator +loc);

		}
*/
		
		buf.append(File.pathSeparator + bundlePaths.get(0) + "/bin");
		buf.append(File.pathSeparator + bundlePaths.get(2) + "/bin");
		buf.append(File.pathSeparator + bundlePaths.get(3) + "/bin");
		buf.append(File.pathSeparator + bundlePaths.get(4) + "/bin");
		buf.append(File.pathSeparator + bundlePaths.get(7) + "/bin");
		
		// buf.append(File.pathSeparator+Platform.getInstallLocation().getURL().getPath()+"plugins/org.eclipse.ui.workbench_3.7.0.I20110519-0100.jar");
		// buf.append(File.pathSeparator+Platform.getInstallLocation().getURL().getPath()+"/plugins/org.eclipse.core.commands_3.6.0.I20110111-0800.jar");
		// System.out.println(buf.toString());

		String classpaths = buf.toString();

		return classpaths;
	}
	
	/* Convert the string from the preferences! */
	private String[] convert(String preferenceValue) {
		StringTokenizer tokenizer = new StringTokenizer(preferenceValue, ";");
		int tokenCount = tokenizer.countTokens();
		String[] elements = new String[tokenCount];

		for (int i = 0; i < tokenCount; i++) {
			elements[i] = tokenizer.nextToken();
		}

		return elements;
	}
	
	

}
