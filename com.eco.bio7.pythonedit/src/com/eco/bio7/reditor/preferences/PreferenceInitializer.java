package com.eco.bio7.reditor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import com.eco.bio7.pythonedit.PythonEditorPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {

		IPreferenceStore storePython = PythonEditorPlugin.getDefault().getPreferenceStore();
		FontData[] f = JFaceResources.getFontRegistry().get("com.eco.bio7.pythoneditor.textfont").getFontData();

		setColorDefaults(storePython, com.eco.bio7.util.Util.isDarkTheme());
		/* Stored colors of a theme (not changed by the user) follow the theme! */
		com.eco.bio7.util.Util.followThemeColors(storePython, PreferenceInitializer::setColorDefaults);
		/* The colors are changed when the theme is changed between light and dark! */
		com.eco.bio7.util.Util.addThemeListener(dark -> {
			setColorDefaults(storePython, dark);
			com.eco.bio7.util.Util.followThemeColors(storePython, PreferenceInitializer::setColorDefaults);
			WorkbenchPreferencePython.updateEditorColors();
		});

		PreferenceConverter.setDefault(storePython, "colourkeyfont", f);
		PreferenceConverter.setDefault(storePython, "colourkeyfont1", f);
		PreferenceConverter.setDefault(storePython, "colourkeyfont2", f);
		PreferenceConverter.setDefault(storePython, "colourkeyfont3", f);
		PreferenceConverter.setDefault(storePython, "colourkeyfont4", f);
		PreferenceConverter.setDefault(storePython, "colourkeyfont5", f);
		// PreferenceConverter.setDefault(storePython, "colourkeyfont6", new
		// FontData("Courier New", 10, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont7", f);
		PreferenceConverter.setDefault(storePython, "colourkeyfont8", f);
		PreferenceConverter.setDefault(storePython, "colourkeyfont9", f);
		PreferenceConverter.setDefault(storePython, "colourkeyfont10", f);
		// PreferenceConverter.setDefault(storePython, "colourkeyfon11", new
		// FontData("Courier New", 10, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont12", f);

	}

	/**
	 * Sets the default colors of the Python editor for the light or the dark theme.
	 * 
	 * @param store  the preference store.
	 * @param isDark true for the dark theme.
	 */
	public static void setColorDefaults(IPreferenceStore store, boolean isDark) {
		if (isDark) {

			PreferenceConverter.setDefault(store, "colourkey", new RGB(167, 236, 33));
			PreferenceConverter.setDefault(store, "colourkey1", new RGB(177, 102, 218));
			PreferenceConverter.setDefault(store, "colourkey2", new RGB(23, 198, 163));
			PreferenceConverter.setDefault(store, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(store, "colourkey4", new RGB(219, 176, 78));
			PreferenceConverter.setDefault(store, "colourkey5", new RGB(230, 230, 250));
			PreferenceConverter.setDefault(store, "colourkey7", new RGB(250, 243, 243));
			PreferenceConverter.setDefault(store, "colourkey8", new RGB(104, 151, 187));
			PreferenceConverter.setDefault(store, "colourkey9", new RGB(250, 243, 243));
			PreferenceConverter.setDefault(store, "colourkey10", new RGB(255, 198, 109));
			PreferenceConverter.setDefault(store, "colourkey12", new RGB(230, 230, 250));

		} else {
			PreferenceConverter.setDefault(store, "colourkey", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(store, "colourkey1", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(store, "colourkey2", new RGB(42, 0, 255));
			PreferenceConverter.setDefault(store, "colourkey3", new RGB(63, 127, 95));
			PreferenceConverter.setDefault(store, "colourkey4", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey5", new RGB(0, 0, 0));
			// PreferenceConverter.setDefault(store, "colourkey6", new RGB(0,
			// 0, 0));
			PreferenceConverter.setDefault(store, "colourkey7", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey8", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey9", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey10", new RGB(0, 0, 0));
			// PreferenceConverter.setDefault(store, "colourkey11", new RGB(0,
			// 0, 0));
			PreferenceConverter.setDefault(store, "colourkey12", new RGB(0, 0, 0));
		}
	}

}
