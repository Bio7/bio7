package com.eco.bio7.ijmacro.editor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {

		IPreferenceStore storeMacroEditor = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		FontData[] f = JFaceResources.getFontRegistry().get("com.eco.bio7.ijmacro.editor.textfont").getFontData();

		boolean isDark = Display.isSystemDarkTheme();

		if (isDark) {

			PreferenceConverter.setDefault(storeMacroEditor, "colourkey", new RGB(167, 236, 33));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey1", new RGB(177, 102, 218));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey2", new RGB(23, 198, 163));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey4", new RGB(219, 176, 78));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey5", new RGB(230, 230, 250));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey6", new RGB(250, 243, 243));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey7", new RGB(104, 151, 187));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey8", new RGB(250, 243, 243));

		} else {
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey1", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey2", new RGB(42, 0, 255));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey4", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey5", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey6", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey7", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storeMacroEditor, "colourkey8", new RGB(63, 127, 95));
			// PreferenceConverter.setDefault(storeBsh, "colourkey8", new RGB(50,
			// 150, 150));
		}

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

}
