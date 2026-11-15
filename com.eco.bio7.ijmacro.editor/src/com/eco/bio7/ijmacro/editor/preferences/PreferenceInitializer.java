package com.eco.bio7.ijmacro.editor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {

		IPreferenceStore storeMacroEditor = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		FontData[] f = JFaceResources.getFontRegistry().get("com.eco.bio7.ijmacro.editor.textfont").getFontData();

		/* The color schemes of the light and the dark theme */
		storeMacroEditor.setDefault(com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE, com.eco.bio7.util.ColorSchemes.defaultName(false));
		storeMacroEditor.setDefault(com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE, com.eco.bio7.util.ColorSchemes.defaultName(true));
		setColorDefaults(storeMacroEditor, com.eco.bio7.util.Util.isDarkTheme());
		/* Stored colors of a theme (not changed by the user) follow the theme! */
		com.eco.bio7.util.Util.followThemeColors(storeMacroEditor, PreferenceInitializer::setColorDefaults);
		/* The colors are changed when the theme is changed between light and dark! */
		com.eco.bio7.util.Util.addThemeListener(dark -> {
			setColorDefaults(storeMacroEditor, dark);
			com.eco.bio7.util.Util.followThemeColors(storeMacroEditor, PreferenceInitializer::setColorDefaults);
			WorkbenchPreferenceijmacro.updateEditorColors();
		});

		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont", f);
		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont1", f);
		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont2", f);
		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont3", f);
		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont4", f);
		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont5", f);
		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont6", f);
		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont7", f);
		PreferenceConverter.setDefault(storeMacroEditor, "colourkeyfont8", f);

		storeMacroEditor.setDefault("EVALUATE_EXTERNAL", false);
		storeMacroEditor.setDefault("SHOW_INFOPOPUP", true);
		storeMacroEditor.setDefault("MARK_WORDS", true);
		storeMacroEditor.setDefault("IJ_COMPLETION_CONTAINS", false);
		storeMacroEditor.setDefault("TYPED_CODE_COMPLETION", false);
		storeMacroEditor.setDefault("ACTIVATION_CHARS", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.");

		storeMacroEditor.setDefault("CLOSE_BRACES", true);
		storeMacroEditor.setDefault("CLOSE_BRACES_LINEBREAK", true);
		storeMacroEditor.setDefault("CLOSE_PARENTHESES", true);
		storeMacroEditor.setDefault("CLOSE_BRACKETS", true);
		storeMacroEditor.setDefault("CLOSE_DOUBLE_QUOTE", true);
		storeMacroEditor.setDefault("CLOSE_SINGLEQUOTE", true);
		storeMacroEditor.setDefault("IJMACRO_EDITOR_EDIT_INDENT", true);

		storeMacroEditor.setDefault("IJMACRO_EDITOR_FORMAT_OPTIONS", "0,4, ,true,false,0,false,false,true,false");

	}

	/**
	 * Sets the default colors of the ImageJ macro editor for the light or the dark theme.
	 * 
	 * @param store  the preference store.
	 * @param isDark true for the dark theme.
	 */
	public static void setColorDefaults(IPreferenceStore store, boolean isDark) {
		/* The colors of the selected color scheme of the theme (see ColorSchemes) */
		IPreferenceStore own = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		String name = own.getString(isDark ? com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE : com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE);
		com.eco.bio7.util.ColorSchemes.Scheme scheme = com.eco.bio7.util.ColorSchemes.get(name, isDark);
		for (int i = 0; i < COLOR_ROLES.length; i++) {
			com.eco.bio7.util.ColorSchemes.Style style = scheme.style(COLOR_ROLES[i]);
			String key = i == 0 ? "colourkey" : "colourkey" + i;
			PreferenceConverter.setDefault(store, key, style.color());
			store.setDefault(i == 0 ? "BOLD_COLOURKEY" : "BOLD_COLOURKEY" + i, style.bold());
		}
		/* The classic dark scheme keeps the default color of the macro editor */
		if (scheme.name().equals("Bio7 Dark")) {
			PreferenceConverter.setDefault(store, "colourkey4", new RGB(204, 108, 29));
		}
	}

	/** The roles of the color preferences colourkey, colourkey1, ... colourkey10 of the macro editor. */
	public static final com.eco.bio7.util.ColorSchemes.Role[] COLOR_ROLES = { com.eco.bio7.util.ColorSchemes.Role.KEYWORD,
			com.eco.bio7.util.ColorSchemes.Role.CONSTANT, com.eco.bio7.util.ColorSchemes.Role.STRING, com.eco.bio7.util.ColorSchemes.Role.COMMENT,
			com.eco.bio7.util.ColorSchemes.Role.DEFAULT, com.eco.bio7.util.ColorSchemes.Role.OPERATOR, com.eco.bio7.util.ColorSchemes.Role.BRACE,
			com.eco.bio7.util.ColorSchemes.Role.NUMBER, com.eco.bio7.util.ColorSchemes.Role.MULTILINE_COMMENT,
			com.eco.bio7.util.ColorSchemes.Role.FUNCTION_CALL, com.eco.bio7.util.ColorSchemes.Role.FUNCTION_DEFINITION };


}
