package com.eco.bio7.markdownedit;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import com.eco.bio7.markdownedit.editors.MarkdownColorProvider;
import com.eco.bio7.markdownedit.editors.MarkdownScanner;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.eco.bio7.markdownedit"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private MarkdownColorProvider fColorProvider;

	private MarkdownScanner fCodeScanner;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	public RuleBasedScanner getMarkdownScanner() {
		if (fCodeScanner == null)
			fCodeScanner = new MarkdownScanner(getRColorProvider());
		return fCodeScanner;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	public MarkdownColorProvider getRColorProvider() {
		if (fColorProvider == null)
			fColorProvider = new MarkdownColorProvider();
		return fColorProvider;
	}
	
}
