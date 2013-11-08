package com.eco.bio7.util;

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

}
