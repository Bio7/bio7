package com.eco.bio7.image;

import org.eclipse.osgi.util.NLS;

public class MessagesThemeIDsSettings extends NLS {
	private static final String BUNDLE_NAME = "com.eco.bio7.image.messages"; //$NON-NLS-1$
	public static String Theme_Black_1;
	public static String Theme_Black_2;
	public static String Theme_Black_3;
	public static String Theme_Black_4;
	public static String Theme_Black_5;
	public static String Theme_Font_Color;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, MessagesThemeIDsSettings.class);
	}

	private MessagesThemeIDsSettings() {
	}
}
