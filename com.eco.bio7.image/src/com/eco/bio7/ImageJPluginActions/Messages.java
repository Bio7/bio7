package com.eco.bio7.ImageJPluginActions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.eco.bio7.ImageJPluginActions.messages"; //$NON-NLS-1$
	public static String LinuxContextMenuDelay;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
