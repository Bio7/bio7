package com.eco.bio7.image.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.eco.bio7.image.Activator;
import com.eco.bio7.util.Util;

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
		store.setDefault("ROI_MANAGER", false);
		store.setDefault("RESULTS_TABLE", false);
		store.setDefault("MACRO_RECORDER", false);

		if (Util.getOS().equals("Windows")) {
			store.setDefault("FONT_SIZE_CORRECTION", 0);
			store.setDefault("FONT_ANTIALIASED",false);
		} else if (Util.getOS().equals("Linux")) {
			store.setDefault("FONT_SIZE_CORRECTION", 2);
			store.setDefault("FONT_ANTIALIASED",true);
		} else if (Util.getOS().equals("Mac")) {
			store.setDefault("FONT_SIZE_CORRECTION", 0);
			store.setDefault("FONT_ANTIALIASED",false);
		}

	}

}
