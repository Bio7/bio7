package com.eco.bio7.compile;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class CompilerMessages {
	private static final String BUNDLE_NAME = "imports.compiler";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private CompilerMessages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
