package com.eco.bio7.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;

public class Util {
	
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
	
	public  File[] ListFilesDirectory(File filedirectory, final String[] extensions) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of the file or directory inside Bio7.
				//String filename = children[i];
			}
		}

		// Filter the extension of the file.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(extensions[0]) || name.endsWith(extensions[1])|| name.endsWith(extensions[2])|| name.endsWith(extensions[3])|| name.endsWith(extensions[4])|| name.endsWith(extensions[5]));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}
	
	public  File[] ListFileDirectory(File filedirectory, final String extension) {
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
	public  String fileToString(String path) {// this function returns the
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

}

