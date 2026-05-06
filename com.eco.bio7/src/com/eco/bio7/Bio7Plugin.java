package com.eco.bio7;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import com.eco.bio7.compile.Bio7FragmentLoader;

public class Bio7Plugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.eco.bio7";
    
    private static Bio7Plugin plugin;
    private static BundleContext context;
    private static Bio7FragmentLoader fragmentLoader;
 // Resource bundle.
 	private ResourceBundle resourceBundle;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        plugin = this;
        context = bundleContext;
        
        // Initialize fragment loader
        fragmentLoader = new Bio7FragmentLoader(bundleContext);
        fragmentLoader.initialize();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        if (fragmentLoader != null) {
            fragmentLoader.shutdown();  // Changed from unloadAll()
            fragmentLoader = null;
        }
        
        plugin = null;
        context = null;
        super.stop(bundleContext);
    }

    public static Bio7Plugin getDefault() {
        return plugin;
    }
    
    public static BundleContext getContext() {
        return context;
    }
    
    public static Bio7FragmentLoader getFragmentLoader() {
        return fragmentLoader;
    }
 // get the root of the plugin
 	public static IWorkspace getWorkspace() {
 		return ResourcesPlugin.getWorkspace();
 	}

    /**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = Bio7Plugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * The standard to returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle("com.eco.bio7.Bio7PluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}

    /**
	 * Returns an image descriptor for the image file at the given plug-in relative
	 * path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.eco.bio7", path);
	}
}