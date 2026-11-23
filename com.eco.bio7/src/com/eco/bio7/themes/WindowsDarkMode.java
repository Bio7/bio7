package com.eco.bio7.themes;

import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;

/**
 * The drop-down lists of the combos on Windows with a dark theme.
 *
 * The dark mode of SWT (OS.setTheme, switched on by the ThemeEngine) gives the
 * drop-down list of a Combo the dark Explorer theme each time the list is
 * dropped down (Combo.updateDropDownTheme for CBN_DROPDOWN): the list then shows
 * the item under the mouse (the selection) white with the white text of the
 * theme - not readable.
 *
 * After each drop-down (a mouse click or a key in a Combo) the dark mode of the
 * list window is switched off again: the list keeps the dark colors of the theme
 * (WM_CTLCOLORLISTBOX) and shows the selection with the selection color of
 * Windows (blue, white text). Tested with SWT 3.135 (Eclipse 4.41).
 *
 * The classes of SWT for Windows are used by reflection, so this compiles on all
 * OS.
 */
public final class WindowsDarkMode {

	private static boolean installed;

	private WindowsDarkMode() {
	}

	/** True on Windows if the active theme is dark (a theme id with "dark", as the Eclipse dark theme). */
	public static boolean isActive() {
		if (!"win32".equals(SWT.getPlatform())) {
			return false;
		}
		try {
			IThemeEngine engine = ThemeStore.engine();
			ITheme active = engine == null ? null : engine.getActiveTheme();
			return active != null && active.getId() != null && active.getId().contains("dark");
		} catch (RuntimeException | LinkageError e) {
			return false;
		}
	}

	/** Installs the fix of the drop-down lists (once, only on Windows; in the UI thread). */
	public static void install(Display display) {
		if (installed || !"win32".equals(SWT.getPlatform()) || display == null || display.isDisposed()) {
			return;
		}
		installed = true;
		Listener dropDown = e -> {
			if (e.widget instanceof Combo combo && !combo.isDisposed() && isActive()) {
				/* After the drop-down (SWT has set the theme of the list in the meantime) */
				display.asyncExec(() -> {
					if (!combo.isDisposed()) {
						undarkList(combo);
					}
				});
			}
		};
		display.addFilter(SWT.MouseDown, dropDown);
		display.addFilter(SWT.KeyDown, dropDown);
	}

	/* The drop-down list without the dark Explorer theme (the colors of the theme stay) */
	private static void undarkList(Combo combo) {
		try {
			ClassLoader loader = Display.class.getClassLoader();
			Class<?> os = Class.forName("org.eclipse.swt.internal.win32.OS", true, loader);
			Class<?> infoClass = Class.forName("org.eclipse.swt.internal.win32.COMBOBOXINFO", true, loader);
			Object info = infoClass.getConstructor().newInstance();
			infoClass.getField("cbSize").setInt(info, infoClass.getField("sizeof").getInt(null));
			long handle = Combo.class.getField("handle").getLong(combo);
			if (!(Boolean) os.getMethod("GetComboBoxInfo", long.class, infoClass).invoke(null, handle, info)) {
				return;
			}
			long list = infoClass.getField("hwndList").getLong(info);
			if (list == 0) {
				return;
			}
			os.getMethod("AllowDarkModeForWindow", long.class, boolean.class).invoke(null, list, false);
			os.getMethod("SetWindowTheme", long.class, char[].class, char[].class).invoke(null, list, "Explorer\0".toCharArray(), null);
			Class<?> rect = Class.forName("org.eclipse.swt.internal.win32.RECT", true, loader);
			os.getMethod("InvalidateRect", long.class, rect, boolean.class).invoke(null, list, null, true);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			/* Another version of SWT: the list stays as it is */
		}
	}
}
