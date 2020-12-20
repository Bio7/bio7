package com.eco.bio7.actions.theme;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.editor.BeanshellEditorPlugin;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.pythonedit.PythonEditorPlugin;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.util.Bio7Dialog;
import com.eco.bio7.util.MessagesThemeIDsSettings;

public class ThemeSwitchHandler extends Action {
	private static final String DEFAULT_THEME = "org.eclipse.e4.ui.css.theme.e4_default";

	public ThemeSwitchHandler(String text) {
		super(text);
		setId("com.eco.bio7.action.switch.theme");
	}

	public void switchTheme(IThemeEngine engine) {

		String id = engine.getActiveTheme().getId();
		/*If we have a black theme!*/
		if (id.startsWith(MessagesThemeIDsSettings.Theme_Black_1) || id.startsWith(MessagesThemeIDsSettings.Theme_Black_2) || id.startsWith(MessagesThemeIDsSettings.Theme_Black_3) || id.startsWith(MessagesThemeIDsSettings.Theme_Black_4)
				|| id.startsWith(MessagesThemeIDsSettings.Theme_Black_5)) {
			// second argument defines that change is
			// persisted and restored on restart
			engine.setTheme(DEFAULT_THEME, true);
			lightFontColors();

		} else {
			/*We switch to a black theme. The first key is the default black theme!*/
			engine.setTheme(MessagesThemeIDsSettings.Theme_Black_1, true);
			if (MessagesThemeIDsSettings.Theme_Font_Color.equals("dark")) {
				IPreferenceStore storeR = Bio7REditorPlugin.getDefault().getPreferenceStore();
				PreferenceConverter.setValue(storeR, "colourkey", new RGB(167, 236, 33));
				PreferenceConverter.setValue(storeR, "colourkey1", new RGB(177, 102, 218));
				PreferenceConverter.setValue(storeR, "colourkey2", new RGB(23, 198, 163));
				PreferenceConverter.setValue(storeR, "colourkey3", new RGB(128, 128, 128));
				PreferenceConverter.setValue(storeR, "colourkey4", new RGB(219, 176, 78));
				PreferenceConverter.setValue(storeR, "colourkey5", new RGB(230, 230, 250));
				PreferenceConverter.setValue(storeR, "colourkey6", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeR, "colourkey7", new RGB(104, 151, 187));
				PreferenceConverter.setValue(storeR, "colourkey8", new RGB(250, 243, 243));

				IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
				PreferenceConverter.setValue(store, "colourkey", new RGB(167, 236, 33));
				PreferenceConverter.setValue(store, "colourkey1", new RGB(177, 102, 218));
				PreferenceConverter.setValue(store, "colourkey2", new RGB(23, 198, 163));
				PreferenceConverter.setValue(store, "colourkey3", new RGB(128, 128, 128));
				PreferenceConverter.setValue(store, "colourkey4", new RGB(219, 176, 78));
				PreferenceConverter.setValue(store, "colourkey5", new RGB(230, 230, 250));
				PreferenceConverter.setValue(store, "colourkey6", new RGB(250, 243, 243));
				PreferenceConverter.setValue(store, "colourkey7", new RGB(104, 151, 187));
				PreferenceConverter.setValue(store, "colourkey8", new RGB(250, 243, 243));

				IPreferenceStore storeBsh = BeanshellEditorPlugin.getDefault().getPreferenceStore();
				PreferenceConverter.setValue(storeBsh, "colourkey", new RGB(167, 236, 33));
				PreferenceConverter.setValue(storeBsh, "colourkey1", new RGB(177, 102, 218));
				PreferenceConverter.setValue(storeBsh, "colourkey2", new RGB(23, 198, 163));
				PreferenceConverter.setValue(storeBsh, "colourkey3", new RGB(128, 128, 128));
				PreferenceConverter.setValue(storeBsh, "colourkey4", new RGB(219, 176, 78));
				PreferenceConverter.setValue(storeBsh, "colourkey5", new RGB(230, 230, 250));
				PreferenceConverter.setValue(storeBsh, "colourkey6", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeBsh, "colourkey7", new RGB(104, 151, 187));

				IPreferenceStore storeMarkdown = Activator.getDefault().getPreferenceStore();
				PreferenceConverter.setValue(storeMarkdown, "colourkey", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storeMarkdown, "colourkey1", new RGB(167, 236, 33));
				PreferenceConverter.setValue(storeMarkdown, "colourkey2", new RGB(250, 243, 243));

				IPreferenceStore storePython = PythonEditorPlugin.getDefault().getPreferenceStore();
				PreferenceConverter.setValue(storePython, "colourkey", new RGB(167, 236, 33));
				PreferenceConverter.setValue(storePython, "colourkey1", new RGB(177, 102, 218));
				PreferenceConverter.setValue(storePython, "colourkey2", new RGB(23, 198, 163));
				PreferenceConverter.setValue(storePython, "colourkey3", new RGB(128, 128, 128));
				PreferenceConverter.setValue(storePython, "colourkey4", new RGB(219, 176, 78));
				PreferenceConverter.setValue(storePython, "colourkey5", new RGB(230, 230, 250));
				PreferenceConverter.setValue(storePython, "colourkey7", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storePython, "colourkey8", new RGB(104, 151, 187));
				PreferenceConverter.setValue(storePython, "colourkey9", new RGB(250, 243, 243));
				PreferenceConverter.setValue(storePython, "colourkey10", new RGB(0, 0, 0));
				PreferenceConverter.setValue(storePython, "colourkey12", new RGB(0, 0, 0));
			} else {
				lightFontColors();
			}
		}
	}

	private void lightFontColors() {
		IPreferenceStore storeR = Bio7REditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setValue(storeR, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setValue(storeR, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setValue(storeR, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setValue(storeR, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setValue(storeR, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storeR, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storeR, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storeR, "colourkey7", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storeR, "colourkey8", new RGB(0, 0, 0));

		IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setValue(store, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setValue(store, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setValue(store, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setValue(store, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setValue(store, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setValue(store, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setValue(store, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setValue(store, "colourkey7", new RGB(0, 0, 0));
		PreferenceConverter.setValue(store, "colourkey8", new RGB(63, 127, 95));

		IPreferenceStore storeBsh = BeanshellEditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setValue(storeBsh, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setValue(storeBsh, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setValue(storeBsh, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setValue(storeBsh, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setValue(storeBsh, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storeBsh, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storeBsh, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storeBsh, "colourkey7", new RGB(0, 0, 0));

		IPreferenceStore storeMarkdown = Activator.getDefault().getPreferenceStore();
		PreferenceConverter.setValue(storeMarkdown, "colourkey", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storeMarkdown, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setValue(storeMarkdown, "colourkey2", new RGB(0, 0, 0));

		IPreferenceStore storePython = PythonEditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setValue(storePython, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setValue(storePython, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setValue(storePython, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setValue(storePython, "colourkey3", new RGB(63, 127, 95));
		PreferenceConverter.setValue(storePython, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storePython, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storePython, "colourkey7", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storePython, "colourkey8", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storePython, "colourkey9", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storePython, "colourkey10", new RGB(0, 0, 0));
		PreferenceConverter.setValue(storePython, "colourkey12", new RGB(0, 0, 0));
	}

	public void run() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		MApplication application = workbench.getService(MApplication.class);
		IEclipseContext context = application.getContext();
		IThemeEngine engine = context.get(IThemeEngine.class);
		switchTheme(engine);
		Bio7Dialog.message("A restart is required for the theme change to take full effect.");

	}

}