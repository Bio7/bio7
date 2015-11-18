package com.eco.bio7.rpreferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import com.eco.bio7.reditor.Bio7REditorPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {

		String osname = System.getProperty("os.name");
		String OS = "Windows";
		if (osname.startsWith("Windows")) {
			OS = "Windows";
		} else if (osname.equals("Linux")) {
			OS = "Linux";
		} else if (osname.startsWith("Mac")) {
			OS = "Mac";
		}

		// TODO Auto-generated method stub

		IPreferenceStore storeR = Bio7REditorPlugin.getDefault().getPreferenceStore();
		String font = null;
		int fsize = 10;

		if (OS.equals("Windows")) {

			font = "Courier New";
			fsize = 10;
		} else if (OS.equals("Linux")) {

			font = "Courier New";
			fsize = 10;

		} else if (OS.equals("Mac")) {

			font = "Helvetica Neue";
			fsize = 14;

		}

		PreferenceConverter.setDefault(storeR, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeR, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeR, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(storeR, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setDefault(storeR, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeR, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeR, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeR, "colourkey7", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeR, "colourkey8", new RGB(0, 0, 0));

		PreferenceConverter.setDefault(storeR, "colourkeyfont", new FontData(font, fsize, 1));
		PreferenceConverter.setDefault(storeR, "colourkeyfont1", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont2", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont3", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont4", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont5", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont6", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont7", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont8", new FontData(font, fsize, 0));
		
		storeR.setDefault("FUNCTIONS_FOLDING", true);
		storeR.setDefault("FOR_LOOP_FOLDING", true);
		storeR.setDefault("WHILE_LOOP_FOLDING", true);
		storeR.setDefault("REPEAT_LOOP_FOLDING", true);
		storeR.setDefault("IF_CONDITION_FOLDING", true);
		
		storeR.setDefault("SHOW_INFOPOPUP", true);
		storeR.setDefault("MARK_WORDS", false);
		storeR.setDefault("TYPED_CODE_COMPLETION", true);
		storeR.setDefault("EDITOR_TO_OUTLINE", false);
		storeR.setDefault("ACTIVATION_CHARS", "(");

	}

}
