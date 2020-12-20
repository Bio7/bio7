package com.eco.bio7.reditor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.pythonedit.PythonEditorPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {

		IPreferenceStore storePython = PythonEditorPlugin.getDefault().getPreferenceStore();
		FontData[] f = JFaceResources.getFontRegistry().get("com.eco.bio7.pythoneditor.textfont").getFontData();

		boolean isDark = Display.isSystemDarkTheme();

		if (isDark) {

			PreferenceConverter.setDefault(storePython, "colourkey", new RGB(167, 236, 33));
			PreferenceConverter.setDefault(storePython, "colourkey1", new RGB(177, 102, 218));
			PreferenceConverter.setDefault(storePython, "colourkey2", new RGB(23, 198, 163));
			PreferenceConverter.setDefault(storePython, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(storePython, "colourkey4", new RGB(219, 176, 78));
			PreferenceConverter.setDefault(storePython, "colourkey5", new RGB(230, 230, 250));
			PreferenceConverter.setDefault(storePython, "colourkey7", new RGB(250, 243, 243));
			PreferenceConverter.setDefault(storePython, "colourkey8", new RGB(104, 151, 187));
			PreferenceConverter.setDefault(storePython, "colourkey9", new RGB(250, 243, 243));
			PreferenceConverter.setDefault(storePython, "colourkey10", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storePython, "colourkey12", new RGB(0, 0, 0));

		} else {
			PreferenceConverter.setDefault(storePython, "colourkey", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(storePython, "colourkey1", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(storePython, "colourkey2", new RGB(42, 0, 255));
			PreferenceConverter.setDefault(storePython, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(storePython, "colourkey4", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storePython, "colourkey5", new RGB(0, 0, 0));
			// PreferenceConverter.setDefault(storePython, "colourkey6", new RGB(0,
			// 0, 0));
			PreferenceConverter.setDefault(storePython, "colourkey7", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storePython, "colourkey8", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storePython, "colourkey9", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(storePython, "colourkey10", new RGB(0, 0, 0));
			// PreferenceConverter.setDefault(storePython, "colourkey11", new RGB(0,
			// 0, 0));
			PreferenceConverter.setDefault(storePython, "colourkey12", new RGB(0, 0, 0));
		}

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

}
