package com.eco.bio7.image.preferences;

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;

import com.eco.bio7.image.ui.Activator;
import com.eco.bio7.image.ui.Util;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String path = Util.getImageJPath();// Get the installation directory of the ImageJ plugin!
		String pluginsPath = path + File.separator + "plugins";// Default plugins directory!
		String macrosPath = path + File.separator + "macros";// Default macros directory!
		store.setDefault("PLUGINS_PATH", pluginsPath);
		store.setDefault("MACROS_PATH", macrosPath);

		/* Dialog Sizes for ImageJ dialogs! */
		store.setDefault("IMAGE_METHODS_SIZE_X", 360);// 260
		store.setDefault("IMAGE_METHODS_SIZE_Y", 450);// 790
		store.setDefault("IMAGEJ_TOOLBAR_SIZE_X", 750);// 580
		store.setDefault("IMAGEJ_TOOLBAR_SIZE_Y", 150);// 130
		
		store.setDefault("OPEN_IMAGES_AS_VIEWS", false);
		store.setDefault("ENABLE_DETACHED_VIEW_WINDOWS",true);
		store.setDefault("DETACHED_IMAGE_POSITION_X", 100);
		store.setDefault("DETACHED_IMAGE_POSITION_Y", 100);
		store.setDefault("DETACHED_IMAGE_WIDTH", 300);
		store.setDefault("DETACHED_IMAGE_HEIGHT", 300);
		store.setDefault("DETACHED_IMAGE_SPACING_X", 50);
		store.setDefault("DETACHED_IMAGE_SPACING_Y", 50);
		store.setDefault("DETACHED_IMAGE_SPACING_X_MAX", 300);
		store.setDefault("DETACHED_IMAGE_SPACING_Y_MAX", 0);

	}

}
