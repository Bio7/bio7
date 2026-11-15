package com.eco.bio7.beanshell.editor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import com.eco.bio7.editor.BeanshellEditorPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {

		IPreferenceStore storeBsh = BeanshellEditorPlugin.getDefault().getPreferenceStore();
		FontData[] f = JFaceResources.getFontRegistry().get("com.eco.bio7.beanshelleditor.textfont").getFontData();

		/* The color schemes of the light and the dark theme */
		storeBsh.setDefault(com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE, com.eco.bio7.util.ColorSchemes.defaultName(false));
		storeBsh.setDefault(com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE, com.eco.bio7.util.ColorSchemes.defaultName(true));
		setColorDefaults(storeBsh, com.eco.bio7.util.Util.isDarkTheme());
		/* Stored colors of a theme (not changed by the user) follow the theme! */
		com.eco.bio7.util.Util.followThemeColors(storeBsh, PreferenceInitializer::setColorDefaults);
		/* The colors are changed when the theme is changed between light and dark! */
		com.eco.bio7.util.Util.addThemeListener(dark -> {
			setColorDefaults(storeBsh, dark);
			com.eco.bio7.util.Util.followThemeColors(storeBsh, PreferenceInitializer::setColorDefaults);
			WorkbenchPreferencebsh.updateEditorColors();
		});
		// PreferenceConverter.setDefault(storeBsh, "colourkey8", new RGB(50,
		// 150, 150));

		PreferenceConverter.setDefault(storeBsh, "colourkeyfont", f);
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont1", f);
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont2", f);
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont3", f);
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont4", f);
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont5", f);
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont6", f);
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont7", f);

	}

	/**
	 * Sets the default colors of the BeanShell/Groovy editor for the light or the dark theme.
	 * 
	 * @param store  the preference store.
	 * @param isDark true for the dark theme.
	 */
	public static void setColorDefaults(IPreferenceStore store, boolean isDark) {
		/* The colors of the selected color scheme of the theme (see ColorSchemes) */
		com.eco.bio7.util.ColorSchemes.Scheme scheme = com.eco.bio7.util.ColorSchemes.selected(BeanshellEditorPlugin.getDefault().getPreferenceStore(), isDark);
		com.eco.bio7.util.ColorSchemes.setDefaults(store, scheme, COLOR_ROLES);
	}

	/** The roles of the color preferences colourkey, colourkey1, ... of the editor. */
	public static final com.eco.bio7.util.ColorSchemes.Role[] COLOR_ROLES = { com.eco.bio7.util.ColorSchemes.Role.KEYWORD, com.eco.bio7.util.ColorSchemes.Role.CONSTANT, com.eco.bio7.util.ColorSchemes.Role.STRING, com.eco.bio7.util.ColorSchemes.Role.COMMENT, com.eco.bio7.util.ColorSchemes.Role.DEFAULT, com.eco.bio7.util.ColorSchemes.Role.OPERATOR, com.eco.bio7.util.ColorSchemes.Role.BRACE, com.eco.bio7.util.ColorSchemes.Role.NUMBER, com.eco.bio7.util.ColorSchemes.Role.FUNCTION_CALL, com.eco.bio7.util.ColorSchemes.Role.FUNCTION_DEFINITION };


}
