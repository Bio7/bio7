package com.eco.bio7.rpreferences;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.osgi.framework.Bundle;

import com.eco.bio7.reditor.Bio7REditorPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {

		/*String osname = System.getProperty("os.name");
		String OS = "Windows";
		if (osname.startsWith("Windows")) {
			OS = "Windows";
		} else if (osname.equals("Linux")) {
			OS = "Linux";
		} else if (osname.startsWith("Mac")) {
			OS = "Mac";
		}
		
		
		String font = null;
		int fsize = 10;
		
		if (OS.equals("Windows")) {
		
			font = "Consolas";
			fsize = 10;
		} else if (OS.equals("Linux")) {
		
			font = "Monospace";
			fsize = 10;
		
		} else if (OS.equals("Mac")) {
		
			font = "Helvetica Neue";
			fsize = 14;
		
		}*/
		IPreferenceStore storeR = Bio7REditorPlugin.getDefault().getPreferenceStore();
		FontData[] f = JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont").getFontData();

		
		/* The color schemes of the light and the dark theme */
		storeR.setDefault(com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE, com.eco.bio7.util.ColorSchemes.defaultName(false));
		storeR.setDefault(com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE, com.eco.bio7.util.ColorSchemes.defaultName(true));
		setColorDefaults(storeR, com.eco.bio7.util.Util.isDarkTheme());
		/* Stored colors of a theme (not changed by the user) follow the theme! */
		com.eco.bio7.util.Util.followThemeColors(storeR, PreferenceInitializer::setColorDefaults);
		/* The colors are changed when the theme is changed between light and dark! */
		com.eco.bio7.util.Util.addThemeListener(dark -> {
			setColorDefaults(storeR, dark);
			com.eco.bio7.util.Util.followThemeColors(storeR, PreferenceInitializer::setColorDefaults);
			WorkbenchPreferenceR.updateEditorColors();
		});

		PreferenceConverter.setDefault(storeR, "colourkeyfont", f);
		PreferenceConverter.setDefault(storeR, "colourkeyfont1", f);
		PreferenceConverter.setDefault(storeR, "colourkeyfont2", f);
		PreferenceConverter.setDefault(storeR, "colourkeyfont3", f);
		PreferenceConverter.setDefault(storeR, "colourkeyfont4", f);
		PreferenceConverter.setDefault(storeR, "colourkeyfont5", f);
		PreferenceConverter.setDefault(storeR, "colourkeyfont6", f);
		PreferenceConverter.setDefault(storeR, "colourkeyfont7", f);
		PreferenceConverter.setDefault(storeR, "colourkeyfont8", f);

		storeR.setDefault("FUNCTIONS_FOLDING", true);
		storeR.setDefault("FOR_LOOP_FOLDING", true);
		storeR.setDefault("WHILE_LOOP_FOLDING", true);
		storeR.setDefault("REPEAT_LOOP_FOLDING", true);
		storeR.setDefault("IF_CONDITION_FOLDING", true);

		storeR.setDefault("SHOW_INFOPOPUP", true);
		storeR.setDefault("R_EDITOR_HOVERS", true);
		storeR.setDefault("SHOW_HOVERPOPUP", true);
		storeR.setDefault("SHOW_HOVERPOPUP_STR", true);
		storeR.setDefault("MARK_WORDS", false);
		storeR.setDefault("TYPED_CODE_COMPLETION", true);
		storeR.setDefault("ACTIVATION_AMOUNT_CHAR_COMPLETION",2);
		storeR.setDefault("EDITOR_TO_OUTLINE", true);
		storeR.setDefault("ACTIVATION_CHARS", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.@$:");

		storeR.setDefault("UNUSED_FUNCTION", true);
		storeR.setDefault("MISSING_FUNCTION", true);
		storeR.setDefault("UNUSED_VAR", true);
		storeR.setDefault("MISSING_VAR", true);
		storeR.setDefault("CHECK_CONSTANTS", true);
		storeR.setDefault("FUNCTION_ALREADY_DEFINED", true);
		storeR.setDefault("VARIABLE_ALREADY_DEFINED", false);
		storeR.setDefault("CHECK_MISSING_FUNCTION_CALL_ARGS", true);
		storeR.setDefault("WRONG_FUNCTION_CALL_OPERATOR_ASSIGNMENT", false);
		storeR.setDefault("CHECK_EXCESSIVE_FUNCTION_CALL_ARGS", true);
		storeR.setDefault("CHECK_VARIABLE_ASSIGNMENT_FUNCTION_CALL_ARGS", true);
		storeR.setDefault("CHECK_FOR_EMPTY_ARG_FUNCTION", true);
		storeR.setDefault("CHECK_VARIABLES_IN_FUNCTION_CALLS", false);
		storeR.setDefault("CHECK_INSTALLED_LIBRARY", true);
		storeR.setDefault("WRONG_NULL_COMPARISON", true);

		storeR.setDefault("CLOSE_BRACES", true);
		storeR.setDefault("CLOSE_BRACES_LINEBREAK", true);
		storeR.setDefault("CLOSE_PARENTHESES", true);
		storeR.setDefault("CLOSE_BRACKETS", true);
		storeR.setDefault("CLOSE_DOUBLE_QUOTE", true);
		storeR.setDefault("CLOSE_SINGLEQUOTE", true);
		storeR.setDefault("REDITOR_EDIT_INDENT", true);

		/* The built-in formatter (default) and its options; formatR and styler are optional */
		com.eco.bio7.reditor.format.RFormatterSupport.initializeDefaults(storeR);
		storeR.setDefault("STYLER_PACKAGE", false);
		/* The packages of an opened script are added to the code completion (default: attached with library()) */
		storeR.setDefault(com.eco.bio7.reditor.actions.PackageCompletionOnOpen.MODE, com.eco.bio7.reditor.actions.PackageCompletionOnOpen.ATTACH);
		storeR.setDefault("FORMAT_COMMENTS", true);
		storeR.setDefault("FORMAT_BLANK_LINES", true);
		storeR.setDefault("FORMAT_REPLACE_ASSIGNMENT", false);
		storeR.setDefault("FORMAT_BRACES_NEWLINE", false);
		storeR.setDefault("FORMAT_NUMBER_SPACES", 4);
		storeR.setDefault("FORMAT_MINIMUM_LINE", 70);
		storeR.setDefault("CHECK_SPELLING_REDITOR", false);
		
		storeR.setDefault("LOCATION_DATABASE_XML_CONFIG", getProfilesFile().toString());

	}
	
	private File getProfilesFile() {
		Bundle bundle = Platform.getBundle("com.eco.bio7.redit");
        
		// String path = bundle.getLocation().split(":")[2];
		URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationUrl);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String path = fileUrl.getFile();

		// System.out.println(path);
		File fileStartupScripts = new File(path + "/store/database_profiles.xml");
		return fileStartupScripts;
	}

	/**
	 * Sets the default colors of the R editor for the light or the dark theme.
	 * 
	 * @param store  the preference store.
	 * @param isDark true for the dark theme.
	 */
	public static void setColorDefaults(IPreferenceStore store, boolean isDark) {
		/* The colors of the selected color scheme of the theme (see ColorSchemes) */
		IPreferenceStore own = Bio7REditorPlugin.getDefault().getPreferenceStore();
		String name = own.getString(isDark ? com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE : com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE);
		com.eco.bio7.util.ColorSchemes.Scheme scheme = com.eco.bio7.util.ColorSchemes.get(name, isDark);
		for (int i = 0; i < COLOR_ROLES.length; i++) {
			com.eco.bio7.util.ColorSchemes.Style style = scheme.style(COLOR_ROLES[i]);
			String key = i == 0 ? "colourkey" : "colourkey" + i;
			PreferenceConverter.setDefault(store, key, style.color());
			store.setDefault(i == 0 ? "BOLD_COLOURKEY" : "BOLD_COLOURKEY" + i, style.bold());
		}
	}

	/** The roles of the color preferences colourkey, colourkey1, ... colourkey11 of the R editor. */
	public static final com.eco.bio7.util.ColorSchemes.Role[] COLOR_ROLES = { com.eco.bio7.util.ColorSchemes.Role.KEYWORD,
			com.eco.bio7.util.ColorSchemes.Role.CONSTANT, com.eco.bio7.util.ColorSchemes.Role.STRING, com.eco.bio7.util.ColorSchemes.Role.COMMENT,
			com.eco.bio7.util.ColorSchemes.Role.DEFAULT, com.eco.bio7.util.ColorSchemes.Role.OPERATOR, com.eco.bio7.util.ColorSchemes.Role.BRACE,
			com.eco.bio7.util.ColorSchemes.Role.NUMBER, com.eco.bio7.util.ColorSchemes.Role.ASSIGNMENT, com.eco.bio7.util.ColorSchemes.Role.FUNCTION_CALL,
			com.eco.bio7.util.ColorSchemes.Role.FUNCTION_DEFINITION, com.eco.bio7.util.ColorSchemes.Role.NAMESPACE };


}
