package com.eco.bio7.markdownedit.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import com.eco.bio7.markdownedit.Activator;


public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public PreferenceInitializer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initializeDefaultPreferences() {

		
		IPreferenceStore storeR = Activator.getDefault().getPreferenceStore();
		FontData []f=JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.markdown.textfont").getFontData();
		//Font f= JFaceResources.getFont(JFaceResources.TEXT_FONT);
		PreferenceConverter.setDefault(storeR, "colourkey", new RGB(0,0,0));
		PreferenceConverter.setDefault(storeR, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeR, "colourkey2", new RGB(42, 0, 255));
		//PreferenceConverter.setDefault(storeR, "colourkey3", new RGB(128, 128, 128));
		
		

        PreferenceConverter.setDefault(storeR, "colourkeyfont", f);
        PreferenceConverter.setDefault(storeR, "colourkeyfont1", f);
        PreferenceConverter.setDefault(storeR, "colourkeyfont2", f);
        //PreferenceConverter.setDefault(storeR, "colourkeyfont3", f);
        storeR.setDefault("OPEN_WORD_IN_VIEW",false);
        storeR.setDefault("RECONCILE_MARKDOWN",false);
        storeR.setDefault("RECONCILE_MARKDOWN_TIME", 1000);
       


	}

}
