/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.compile.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Util;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.eco.bio7.javaeditor.Bio7EditorPlugin;

public class ScanClassPath {

	private String pathBundle;
	String[] bundles = new String[] { "com.eco.bio7", "com.eco.bio7.libs", "com.eco.bio7.javaedit",
			"com.eco.bio7.image", "com.eco.bio7.WorldWind", "com.eco.bio7.scenebuilder", "com.eco.bio7.browser",
			"Bundled_R", "com.eco.bio7.javacv", "com.eco.bio7.deepcv" };// "org.eclipse.ui.workbench","org.eclipse.core.commands"

	String[] bundlesEclipse;
	private String OS;

	private void assignBundleLibs() {
		OS = getOS();
		String swtLibraryOS = "";

		if (OS.equals("Windows")) {

			swtLibraryOS = "org.eclipse.swt.win32.win32.x86_64";

		} else if (OS.equals("Mac")) {

			if (isaarch64()) {

				swtLibraryOS = "org.eclipse.swt.cocoa.macosx.aarch64";

			} else {
				swtLibraryOS = "org.eclipse.swt.cocoa.macosx.x86_64";

			}
		}

		else if (OS.equals("Linux")) {

			swtLibraryOS = "org.eclipse.swt.gtk.linux.x86_64";

		}
		/* Add all Eclipse necessary libraries */
		bundlesEclipse = new String[] { "org.eclipse.core.commands", "org.eclipse.ui.workbench", "org.eclipse.ui",
				"org.eclipse.swt", swtLibraryOS, "org.eclipse.draw2d", "org.eclipse.equinox.registry",
				"org.eclipse.equinox.common", "org.eclipse.core.runtime", "org.eclipse.core.jobs",
				"org.eclipse.jface" };
	}

	public String scan() {
		assignBundleLibs();
		/*
		 * Scan all necessary plugins for libs and calculate the paths to the libs
		 * decoupled from JDT because we still need this for the custom Java compiler,
		 * Flow editor and Bio7 model import!
		 */
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();

		ArrayList<String> bundlePaths = new ArrayList<String>();
		StringBuffer buf = new StringBuffer();

		String pathseparator = File.pathSeparator;
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = Platform.getBundle(bundles[i]);
			if (bundle != null) {
				URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
				URL fileUrl = null;
				try {
					fileUrl = FileLocator.toFileURL(locationUrl);
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				pathBundle = fileUrl.getFile();
				bundlePaths.add(pathseparator + pathBundle);

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
						buf.append(pathseparator + bundlePaths.get(i) + elements[u].getValue());

					}
				}
			}
		}

		// String platformPath =
		// Platform.getInstallLocation().getURL().getPath().replace("\\", "/");
		String modulePath = store.getString("JAVA_MODULES_PATH");
		modulePath = modulePath.replace(";", "");
		if (Util.isMac()) {
			buf.append(":" + pathseparator + modulePath + "/javafx.base.jar:");
			buf.append(modulePath + "/javafx.controls.jar:");
			buf.append(modulePath + "/javafx.fxml.jar:");
			buf.append(modulePath + "/javafx.graphics.jar:");
			buf.append(modulePath + "/javafx.media.jar:");
			buf.append(modulePath + "/javafx.swing.jar:");
			buf.append(modulePath + "/javafx.web.jar:");
			buf.append(modulePath + "/javafx-swt.jar:");
		} else if (Util.isWindows()) {
			buf.append(";" + pathseparator + modulePath + "/javafx.base.jar;");
			buf.append(pathseparator + modulePath + "/javafx.controls.jar;");
			buf.append(pathseparator + modulePath + "/javafx.fxml.jar;");
			buf.append(pathseparator + modulePath + "/javafx.graphics.jar;");
			buf.append(pathseparator + modulePath + "/javafx.media.jar;");
			buf.append(pathseparator + modulePath + "/javafx.swing.jar;");
			buf.append(pathseparator + modulePath + "/javafx.web.jar;");
			buf.append(pathseparator + modulePath + "/javafx-swt.jar;");
		} else {
			buf.append(":" + pathseparator + modulePath + "/javafx.base.jar:");
			buf.append(pathseparator + modulePath + "/javafx.controls.jar:");
			buf.append(pathseparator + modulePath + "/javafx.fxml.jar:");
			buf.append(pathseparator + modulePath + "/javafx.graphics.jar:");
			buf.append(pathseparator + modulePath + "/javafx.media.jar:");
			buf.append(pathseparator + modulePath + "/javafx.swing.jar:");
			buf.append(pathseparator + modulePath + "/javafx.web.jar:");
			buf.append(pathseparator + modulePath + "/javafx-swt.jar:");

		}

		/*
		 * Get the *.jar list from the Bio7 Java preferences and add them to the
		 * classpath!
		 */

		String libs = store.getString("JAVA_LIBS");
		String[] conv = convert(libs);
		for (int j = 0; j < conv.length; j++) {

			String addedExtLibs = conv[j];
			if (OS.equals("Windows") == false) {
				addedExtLibs = addedExtLibs.replace("::", "");
				addedExtLibs = addedExtLibs.replace(":", "/");
			}

			addedExtLibs = addedExtLibs.replace("\\", "/");
			buf.append(pathseparator + addedExtLibs);

		}

		/* Here we add the required Eclipse *.jars! */
		for (int i = 0; i < bundlesEclipse.length; i++) {
			Bundle bundle = Platform.getBundle(bundlesEclipse[i]);
			if (OS.equals("Windows")) {
				String loc = Platform.getInstallLocation().getURL().getFile() + "/plugins/";
				String[] bundleName = bundle.toString().split(" ");
				String locat = (loc + bundleName[0] + ".jar");
				locat = locat.replace("file", "");

				// java.nio.file.Path path = Paths.get(loc);

				// System.out.println("compile path:" + locat);
				// path.toAbsolutePath().toString());
				buf.append(pathseparator + locat + pathseparator);
			} else if (OS.equals("Mac")) {
				String loc = Platform.getInstallLocation().getURL().getFile() + "/plugins/";
				String[] bundleName = bundle.toString().split(" ");
				String locat = (loc + bundleName[0] + ".jar");
				locat = locat.replace("file", "");

				// java.nio.file.Path path = Paths.get(loc);

				// System.out.println("path:" + File.pathSeparator +
				// path.toAbsolutePath().toString());
				buf.append(pathseparator + locat + pathseparator);
			}

			else if (OS.equals("Linux")) {

				String loc = Platform.getInstallLocation().getURL().getFile() + "/plugins/";
				String[] bundleName = bundle.toString().split(" ");
				String locat = (loc + bundleName[0] + ".jar");
				locat = locat.replace("file", "");

				// java.nio.file.Path path = Paths.get(loc);

				// System.out.println("compile path:" + locat);
				// path.toAbsolutePath().toString());
				buf.append(pathseparator + locat + pathseparator);

				/*
				 * String loc = bundle.getLocation().substring(23); //
				 * System.out.println("loc: " + loc);
				 * 
				 * Eclipse PDE and exported RCP paths are different (absolute vs. relative)!
				 * 
				 * 
				 * 
				 * if (loc.startsWith("/")) { loc = loc.substring(1); }
				 * 
				 * loc = loc.replace("::", ""); loc = loc.replace(":", "/"); java.nio.file.Path
				 * path;
				 * 
				 * Calculate an absolute path to the resource. Exported RCP has a relative path!
				 * 
				 * 
				 * path = Paths.get(loc);
				 * 
				 * // System.out.println("path:" + File.pathSeparator + //
				 * path.toAbsolutePath().toString()); buf.append(pathseparator +
				 * path.toAbsolutePath().toString());
				 */
			}

			// System.out.println(File.pathSeparator +loc);

		}

		buf.append(pathseparator + bundlePaths.get(0) + "/bin");
		buf.append(pathseparator + bundlePaths.get(2) + "/bin");
		buf.append(pathseparator + bundlePaths.get(3) + "/bin");
		buf.append(pathseparator + bundlePaths.get(4) + "/bin");
		buf.append(pathseparator + bundlePaths.get(7) + "/bin");

		// buf.append(File.pathSeparator+Platform.getInstallLocation().getURL().getPath()+"plugins/org.eclipse.ui.workbench_3.7.0.I20110519-0100.jar");
		// buf.append(File.pathSeparator+Platform.getInstallLocation().getURL().getPath()+"/plugins/org.eclipse.core.commands_3.6.0.I20110111-0800.jar");
		// System.out.println(buf.toString());

		String classpaths = buf.toString();
		/* We have to format the classpath for the dynamic compilation! */
		classpaths = classpaths.replace(";/", "");
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

	public IClasspathEntry[] scanForJDT() {
		assignBundleLibs();
		/*
		 * Scan all necessary plugins for libs and calculate the paths to the libs!
		 */
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();

		ArrayList<String> bundlePaths = new ArrayList<String>();
		ArrayList<String> buf = new ArrayList<String>();
		ArrayList<IClasspathEntry> classPathEntry = new ArrayList<IClasspathEntry>();

		String pathseparator = File.pathSeparator;
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = Platform.getBundle(bundles[i]);
			if (bundle != null) {

				URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
				URL fileUrl = null;
				try {
					fileUrl = FileLocator.toFileURL(locationUrl);
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				pathBundle = fileUrl.getFile();
				bundlePaths.add(pathseparator + pathBundle);
				// System.out.println("2:" + File.pathSeparator + pathBundle);
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
					/*
					 * We only parse the plugins with the important *. jar libraries. See array
					 * 'bundles'!
					 */
					if (i == 0 | i == 1 || i == 6 || i == 8 || i == 9) {
						for (int u = 0; u < elements.length; u++) {

							/*
							 * We do not need the external referenced jfxswt.jar listed here (browser plugin
							 * with external reference is not listed!)!
							 */
							if (i == 0 || i == 6) {

								String lib = pathseparator + bundlePaths.get(i) + elements[u].getValue();
								// System.out.println(lib);
								String external = "external";
								if (lib.toLowerCase().contains(external.toLowerCase()) == false) {
									// System.out.println(lib);
									buf.add(lib);
								}

							}

							else {
								// System.out.println(pathseparator + bundlePaths.get(i) +
								// elements[u].getValue());
								buf.add(pathseparator + bundlePaths.get(i) + elements[u].getValue());
							}

						}
					}
				}
			}
		}

		/*
		 * Get the *.jar list from the Bio7 Java preferences and add them to the
		 * classpath!
		 */
		String libs = store.getString("JAVA_LIBS");
		String[] conv = convert(libs);

		for (int j = 0; j < conv.length; j++) {

			String string = conv[j];

			String addedExtLibs = pathseparator + string;
			if (OS.equals("Windows") == false) {
				addedExtLibs = addedExtLibs.replace("::", "");
				addedExtLibs = addedExtLibs.replace(":", "/");
			}
			addedExtLibs = addedExtLibs.replace("\\", "/");
			buf.add(addedExtLibs);
			String en = addedExtLibs;

			classPathEntry.add(JavaCore.newLibraryEntry(new Path(en), null, // no
					// source
					null, // no source
					false)); // not exported

		}

		/* Here we add the required Eclipse *.jars! */
		for (int i = 0; i < bundlesEclipse.length; i++) {
			Bundle bundle = Platform.getBundle(bundlesEclipse[i]);
			if (OS.equals("Windows")) {
				String loc = Platform.getInstallLocation().getURL().getFile() + "/plugins/";
				String[] bundleName = bundle.toString().split(" ");
				String locat = (loc + bundleName[0] + ".jar");
				locat = locat.replace("file", "");

				// java.nio.file.Path path = Paths.get(loc);

				// System.out.println("path:" + File.pathSeparator +
				// path.toAbsolutePath().toString());
				buf.add(pathseparator + locat + pathseparator);
			}

			else if (OS.equals("Mac")) {
				String loc = Platform.getInstallLocation().getURL().getFile() + "/plugins/";
				String[] bundleName = bundle.toString().split(" ");
				String locat = (loc + bundleName[0] + ".jar");
				locat = locat.replace("file", "");

				// java.nio.file.Path path = Paths.get(loc);

				// System.out.println("path:" + File.pathSeparator +
				// path.toAbsolutePath().toString());
				buf.add(pathseparator + locat + pathseparator);
			}

			else if (OS.equals("Linux")) {
				String loc = Platform.getInstallLocation().getURL().getFile() + "/plugins/";
				String[] bundleName = bundle.toString().split(" ");
				String locat = (loc + bundleName[0] + ".jar");
				locat = locat.replace("file", "");

				// java.nio.file.Path path = Paths.get(loc);

				// System.out.println("path:" + File.pathSeparator +
				// path.toAbsolutePath().toString());
				buf.add(pathseparator + locat + pathseparator);
			}

			// System.out.println(File.pathSeparator +loc);

		}
		/* We don't need the *.jar libs for this plugins! */
		buf.add(pathseparator + bundlePaths.get(0) + "/bin");
		int temp = buf.size() - 1;// We need to store the current index of the ArrayList to relate the src folders
		// (see below)!
		buf.add(pathseparator + bundlePaths.get(2) + "/bin");
		buf.add(pathseparator + bundlePaths.get(3) + "/bin");
		buf.add(pathseparator + bundlePaths.get(4) + "/bin");
		buf.add(pathseparator + bundlePaths.get(7) + "/bin");
		String modulePath = store.getString("JAVA_MODULES_PATH");
		modulePath = modulePath.replace(";", "");
		// String platformPath =
		// Platform.getInstallLocation().getURL().getPath().replace("\\", "/");
		if (Util.isWindows()) {
			buf.add(";" + pathseparator + modulePath + "/javafx.base.jar;");
			buf.add(pathseparator + modulePath + "/javafx.controls.jar;");
			buf.add(pathseparator + modulePath + "/javafx.fxml.jar;");
			buf.add(pathseparator + modulePath + "/javafx.graphics.jar;");
			buf.add(pathseparator + modulePath + "/javafx.media.jar;");
			buf.add(pathseparator + modulePath + "/javafx.swing.jar;");
			buf.add(pathseparator + modulePath + "/javafx.web.jar;");
			buf.add(pathseparator + modulePath + "/javafx-swt.jar;");

		} else if (Util.isMac()) {
			buf.add(":" + pathseparator + modulePath + "/javafx.base.jar:");
			buf.add(modulePath + "/javafx.controls.jar:");
			buf.add(modulePath + "/javafx.fxml.jar:");
			buf.add(modulePath + "/javafx.graphics.jar:");
			buf.add(modulePath + "/javafx.media.jar:");
			buf.add(modulePath + "/javafx.swing.jar:");
			buf.add(modulePath + "/javafx.web.jar:");
			buf.add(modulePath + "/javafx-swt.jar:");
		} else {// Linux!
			buf.add(":" + pathseparator + modulePath + "/javafx.base.jar:");
			buf.add(pathseparator + modulePath + "/javafx.controls.jar:");
			buf.add(pathseparator + modulePath + "/javafx.fxml.jar:");
			buf.add(pathseparator + modulePath + "/javafx.graphics.jar:");
			buf.add(pathseparator + modulePath + "/javafx.media.jar:");
			buf.add(pathseparator + modulePath + "/javafx.swing.jar:");
			buf.add(pathseparator + modulePath + "/javafx.web.jar:");
			buf.add(pathseparator + modulePath + "/javafx-swt.jar:");

		}
		/*
		 * Here we add the results to the classpath. Src entries are created, too for
		 * necessary plugins!
		 */
		IClasspathEntry[] entries = new IClasspathEntry[buf.size()];
		if (OS.equals("Windows")) {
			for (int k = 0; k < buf.size(); k++) {
				String rep = buf.get(k).replace(";", "");
				/*
				 * We add the source! the variable temp has stored the position of the /bin
				 * folders in the buffer so we can relate the src paths to the classpath (the
				 * src indexes are from the bundlePaths array)!
				 **/
				if (k == temp) {

					String pathSr = pathseparator + bundlePaths.get(0) + "/src";
					String pathSrc = pathSr.replace(";", "");

					entries[k] = JavaCore.newLibraryEntry(new Path(rep), new Path(pathSrc), null, false);
					/* With ImageJ plugin source! */
				} else if (k == (temp + 2)) {

					String pathSr = pathseparator + bundlePaths.get(3) + "/src";
					String pathSrc = pathSr.replace(";", "");

					entries[k] = JavaCore.newLibraryEntry(new Path(rep), new Path(pathSrc), null, false);
					/* With ImageJ plugin source! */
				}

				else if (k == (temp + 3)) {
					String pathSr = pathseparator + bundlePaths.get(4) + "/src";
					String pathSrc = pathSr.replace(";", "");

					entries[k] = JavaCore.newLibraryEntry(new Path(rep), new Path(pathSrc), null, false);

				}
				/* With WorldWind plugin source! */
				else if (k == (temp + 4)) {
					String pathSr = pathseparator + bundlePaths.get(7) + "/src";
					String pathSrc = pathSr.replace(";", "");

					entries[k] = JavaCore.newLibraryEntry(new Path(rep), new Path(pathSrc), null, false);

				}

				else {
					entries[k] = JavaCore.newLibraryEntry(new Path(rep), null, // no
							// source
							null, // no source
							false); // not exported

				}
			}
		} else {
			for (int k = 0; k < buf.size(); k++) {
				String rep = buf.get(k).replace("::", "");
				rep = rep.replace(":", "/");
				rep = rep.replace(";/", "");
				/* We add the source! */
				if (k == temp) {

					String pathSr = pathseparator + bundlePaths.get(0) + "/src";
					String pathSrc = pathSr.replace("::", "");

					entries[k] = JavaCore.newLibraryEntry(new Path(rep), new Path(pathSrc), null, false);
					/* With ImageJ plugin source! */
				} else if (k == (temp + 2)) {

					String pathSr = pathseparator + bundlePaths.get(3) + "/src";
					String pathSrc = pathSr.replace("::", "");

					entries[k] = JavaCore.newLibraryEntry(new Path(rep), new Path(pathSrc), null, false);
					/* With ImageJ plugin source! */
				}

				else if (k == (temp + 3)) {
					String pathSr = pathseparator + bundlePaths.get(4) + "/src";
					String pathSrc = pathSr.replace("::", "");

					entries[k] = JavaCore.newLibraryEntry(new Path(rep), new Path(pathSrc), null, false);

				}
				/* With WorldWind plugin source! */
				else if (k == (temp + 4)) {
					String pathSr = pathseparator + bundlePaths.get(7) + "/src";
					String pathSrc = pathSr.replace("::", "");

					entries[k] = JavaCore.newLibraryEntry(new Path(rep), new Path(pathSrc), null, false);

				}

				else {
					entries[k] = JavaCore.newLibraryEntry(new Path(rep), null, // no
							// source
							null, // no source
							false); // not exported

				}
			}
		}

		return entries;
	}

	public boolean isaarch64() {
		boolean isaarch;

		String osname = System.getProperty("os.arch");
		if (osname.startsWith("aarch64")) {
			isaarch = true;
		} else {
			isaarch = false;
		}
		return isaarch;
	}

	public String getOS() {
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

}
