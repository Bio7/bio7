package com.eco.bio7.markdownedit.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

import com.eco.bio7.markdownedit.Activator;


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
		IPreferenceStore storeR = Activator.getDefault().getPreferenceStore();
		FontData []f=JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.markdown.textfont").getFontData();
		//Font f= JFaceResources.getFont(JFaceResources.TEXT_FONT);
		PreferenceConverter.setDefault(storeR, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeR, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeR, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(storeR, "colourkey3", new RGB(128, 128, 128));
		
		

        PreferenceConverter.setDefault(storeR, "colourkeyfont", f);
        PreferenceConverter.setDefault(storeR, "colourkeyfont1", f);
        PreferenceConverter.setDefault(storeR, "colourkeyfont2", f);
        PreferenceConverter.setDefault(storeR, "colourkeyfont3", f);
       


	}

}
