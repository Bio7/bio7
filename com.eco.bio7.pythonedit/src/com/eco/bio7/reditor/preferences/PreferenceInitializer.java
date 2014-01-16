package com.eco.bio7.reditor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import com.eco.bio7.pythonedit.PythonEditorPlugin;

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

		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();
		String font = null;
		int fsize = 10;

		if (OS.equals("Windows")) {

			font = "Courier New";
			fsize = 10;
		} else if (OS.equals("Linux")) {

			font = "Courier New";
			fsize = 10;

		} else if (OS.equals("Mac")) {

			font = "Monaco";
			fsize = 11;

		}

		IPreferenceStore storePython = PythonEditorPlugin.getDefault().getPreferenceStore();
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

		PreferenceConverter.setDefault(storePython, "colourkeyfont", new FontData(font, fsize, 1));
		PreferenceConverter.setDefault(storePython, "colourkeyfont1", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont2", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont3", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont4", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont5", new FontData(font, fsize, 0));
		// PreferenceConverter.setDefault(storePython, "colourkeyfont6", new
		// FontData("Courier New", 10, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont7", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont8", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont9", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont10", new FontData(font, fsize, 0));
		// PreferenceConverter.setDefault(storePython, "colourkeyfon11", new
		// FontData("Courier New", 10, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont12", new FontData(font, fsize, 0));

	}

}
