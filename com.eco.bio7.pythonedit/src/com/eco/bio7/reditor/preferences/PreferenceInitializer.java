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

		/* The color schemes of the light and the dark theme */
		storePython.setDefault(com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE, com.eco.bio7.util.ColorSchemes.defaultName(false));
		storePython.setDefault(com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE, com.eco.bio7.util.ColorSchemes.defaultName(true));
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
		/* The colors of the selected color scheme of the theme (see ColorSchemes) */
		com.eco.bio7.util.ColorSchemes.Scheme scheme = com.eco.bio7.util.ColorSchemes.selected(PythonEditorPlugin.getDefault().getPreferenceStore(), isDark);
		com.eco.bio7.util.ColorSchemes.setDefaults(store, scheme, COLOR_ROLES);
		if (com.eco.bio7.util.ColorSchemes.isClassic(scheme)) {
			/* The classic schemes: the former colors of the editor (the new keys of the scheme) */
			classicDefaults(store, isDark);
			for (int i = 0; i <= 12; i++) {
				store.setDefault(i == 0 ? "BOLD_COLOURKEY" : "BOLD_COLOURKEY" + i, false);
			}
		}
	}

	private static void classicDefaults(IPreferenceStore store, boolean isDark) {
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
	/** The roles of the color preferences colourkey, colourkey1, ... of the editor. */
	public static final com.eco.bio7.util.ColorSchemes.Role[] COLOR_ROLES = { com.eco.bio7.util.ColorSchemes.Role.KEYWORD, com.eco.bio7.util.ColorSchemes.Role.BUILTIN, com.eco.bio7.util.ColorSchemes.Role.STRING, com.eco.bio7.util.ColorSchemes.Role.COMMENT, com.eco.bio7.util.ColorSchemes.Role.DEFAULT, com.eco.bio7.util.ColorSchemes.Role.OPERATOR, null, com.eco.bio7.util.ColorSchemes.Role.NUMBER, com.eco.bio7.util.ColorSchemes.Role.NAMESPACE, com.eco.bio7.util.ColorSchemes.Role.CONSTANT, com.eco.bio7.util.ColorSchemes.Role.FUNCTION_DEFINITION, null, com.eco.bio7.util.ColorSchemes.Role.BRACE, com.eco.bio7.util.ColorSchemes.Role.FUNCTION_CALL };


}
