/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class TerminateRserve {

	public static Process process;

	public static void killProcessLinux() {

		// Runtime rt = Runtime.getRuntime();
		// rt.exec("killall Rserve-bin.so");
		List<String> args = new ArrayList<String>();
		args.add("killall");
		args.add("Rserve");
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectErrorStream();
		try {
			process = pb.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void killProcessMac() {

		// Runtime rt = Runtime.getRuntime();
		// rt.exec("killall Rserve-bin.so");
		List<String> args = new ArrayList<String>();
		args.add("killall");
		// args.add ("Rserve-bin.so");
		args.add("Rserve");
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectErrorStream();
		try {
			process = pb.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void killProcessWindows() {
		String osname = System.getProperty("os.name");
		if (osname.startsWith("Windows")) {

			Bundle bundlenew = Platform.getBundle("com.eco.bio7");

			URL locationUrl = FileLocator.find(bundlenew, new Path("/process/Process.exe"), null);
			URL url = null;
			try {
				url = FileLocator.toFileURL(locationUrl);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			File file = new File(url.getFile());

			// Runtime runtime = Runtime.getRuntime();
			// String arg = " -k Rserve.exe";
			String line;

			// process = runtime.exec("\""+file.getCanonicalPath()+"\"" + arg);
			List<String> args = new ArrayList<String>();
			try {
				args.add(file.getCanonicalPath());
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			args.add("-k");
			args.add("Rserve.exe");
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream();
			try {
				process = pb.start();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			try {
				while ((line = input.readLine()) != null) {

				}
			} catch (IOException e) {

				e.printStackTrace();
			}
			try {
				input.close();
			} catch (IOException e) {

				e.printStackTrace();
			}

			process.destroy();
		}
	}

	public static void killProcessRtermLinux() {

		// Runtime rt = Runtime.getRuntime();
		// rt.exec("killall Rserve-bin.so");
		List<String> args = new ArrayList<String>();
		args.add("killall");
		args.add("R");
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectErrorStream();
		try {
			process = pb.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void killProcessRtermMac() {

		// Runtime rt = Runtime.getRuntime();
		// rt.exec("killall Rserve-bin.so");
		List<String> args = new ArrayList<String>();
		args.add("killall");
		// args.add ("Rserve-bin.so");
		args.add("R");
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectErrorStream();
		try {
			process = pb.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void killProcessRtermWindows() {
		String osname = System.getProperty("os.name");
		if (osname.startsWith("Windows")) {

			Bundle bundlenew = Platform.getBundle("com.eco.bio7");

			URL locationUrl = FileLocator.find(bundlenew, new Path("/process/Process.exe"), null);
			URL url = null;
			try {
				url = FileLocator.toFileURL(locationUrl);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			File file = new File(url.getFile());

			// Runtime runtime = Runtime.getRuntime();
			// String arg = " -k Rserve.exe";
			String line;

			// process = runtime.exec("\""+file.getCanonicalPath()+"\"" + arg);
			List<String> args = new ArrayList<String>();
			try {
				args.add(file.getCanonicalPath());
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			args.add("-k");
			args.add("Rterm.exe");
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream();
			try {
				process = pb.start();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			try {
				while ((line = input.readLine()) != null) {

				}
			} catch (IOException e) {

				e.printStackTrace();
			}
			try {
				input.close();
			} catch (IOException e) {

				e.printStackTrace();
			}

			process.destroy();
		}
	}

}
