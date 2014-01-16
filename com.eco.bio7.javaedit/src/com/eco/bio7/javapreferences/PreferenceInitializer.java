package com.eco.bio7.javapreferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

import com.eco.bio7.compile.Activator;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;



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

		IPreferenceStore storeJava = Bio7EditorPlugin.getDefault().getPreferenceStore();
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
		
		PreferenceConverter.setDefault(storeJava, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeJava, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeJava, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(storeJava, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setDefault(storeJava, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeJava, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeJava, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeJava, "colourkey7", new RGB(0, 0, 0));
		// PreferenceConverter.setDefault(storeJava, "colourkey8", new RGB(0,
		// 150, 150));

		PreferenceConverter.setDefault(storeJava, "colourkeyfont", new FontData(font, fsize, 1));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont1", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont2", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont3", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont4", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont5", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont6", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont7", new FontData(font, fsize, 0));

		
	}

}
