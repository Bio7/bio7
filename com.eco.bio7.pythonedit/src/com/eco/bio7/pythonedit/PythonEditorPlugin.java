package com.eco.bio7.pythonedit;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.eco.bio7.editors.python.PyPartitionScanner;
import com.eco.bio7.editors.python.PythonScriptColorProvider;
import com.eco.bio7.pythoneditors.ScriptCodeScanner;


/**
 * The activator class controls the plug-in life cycle
 */
public class PythonEditorPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.eco.bio7.pythonedit"; //$NON-NLS-1$

	// The shared instance
	public static PythonEditorPlugin plugin;
	
	public final static String SCRIPT_PARTITIONING = "__script_example_partitioning"; //$NON-NLS-1$

	private PythonScriptColorProvider fColorProvider;
	private ScriptCodeScanner fCodeScanner;

	private PyPartitionScanner PyPartitionScanner;
	
	/**
	 * The constructor
	 */
	public PythonEditorPlugin() {
		plugin=this;
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
	public static PythonEditorPlugin getDefault() {
		return plugin;
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
	public PyPartitionScanner getScriptPartitionScanner() {
		if (PyPartitionScanner == null)
			PyPartitionScanner = new PyPartitionScanner();
		return PyPartitionScanner;
	}

	public RuleBasedScanner getScriptCodeScanner() {
		if (fCodeScanner == null)
			fCodeScanner = new ScriptCodeScanner(getScriptColorProvider());
		return fCodeScanner;
	}

	/**
	 * Returns the singleton Script color provider.
	 * 
	 * @return the singleton Script color provider
	 */
	public PythonScriptColorProvider getScriptColorProvider() {
		if (fColorProvider == null)
			fColorProvider = new PythonScriptColorProvider();
		return fColorProvider;
	}
}
