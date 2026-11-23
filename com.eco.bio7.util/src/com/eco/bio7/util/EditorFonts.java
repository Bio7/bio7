/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.util;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * The fonts of the Bio7 editors. All editors (R, Python, Markdown, ImageJ
 * Macro, BeanShell/Groovy) use their font definition, which defaults to the
 * Bio7 text font ({@link #TEXT_FONT}), which defaults to the text font of
 * Eclipse (the same font as the Eclipse editors on a new installation). The
 * syntax colors have no own font (only bold and italic),
 * their font preferences (colourkeyfont...) follow the editor font.
 */
public final class EditorFonts {

	/** The text font of all Bio7 editors and consoles (font definition of com.eco.bio7). */
	public static final String TEXT_FONT = "com.eco.bio7.textfont";
	/** The prefix of the font preferences of the syntax colors (colourkeyfont, colourkeyfont1, ...). */
	public static final String SYNTAX_FONT = "colourkeyfont";

	private static final int MIN_HEIGHT = 4;

	private EditorFonts() {
	}

	/** The font of the font definition (e.g., an editor font) as shown now. */
	public static FontData font(String fontId) {
		FontData[] data = JFaceResources.getFontRegistry().getFontData(fontId);
		if (data == null || data.length == 0) {
			data = JFaceResources.getFontRegistry().getFontData(JFaceResources.TEXT_FONT);
		}
		return data[0];
	}

	/**
	 * Zooms the editor font by delta points (from the font shown now, not from a
	 * stored size): the font definition gets the new size (stored like a font of
	 * the Colors and Fonts preferences) and the syntax fonts of the store the same
	 * font (which notifies the open editors). Returns false if the font would get
	 * too small.
	 */
	public static boolean zoom(String fontId, IPreferenceStore editorStore, int delta) {
		FontData base = font(fontId);
		int height = base.getHeight() + delta;
		if (height < MIN_HEIGHT) {
			return false;
		}
		FontData zoomed = new FontData(base.getName(), height, base.getStyle());
		ScopedPreferenceStore workbench = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.workbench");
		workbench.setValue(fontId, PreferenceConverter.getStoredRepresentation(new FontData[] { zoomed }));
		/* The font registry follows the preference (the Colors and Fonts page writes it the same way) */
		JFaceResources.getFontRegistry().put(fontId, new FontData[] { zoomed });
		syncSyntaxFonts(editorStore, zoomed);
		return true;
	}

	/**
	 * The syntax fonts of the store (colourkeyfont, colourkeyfont1 to
	 * colourkeyfont20, if defined) get the font, each with its own style.
	 */
	public static void syncSyntaxFonts(IPreferenceStore store, FontData font) {
		for (int i = 0; i <= 20; i++) {
			String key = i == 0 ? SYNTAX_FONT : SYNTAX_FONT + i;
			if (!store.contains(key)) {
				continue;
			}
			FontData old = PreferenceConverter.getFontData(store, key);
			int style = old != null ? old.getStyle() : font.getStyle();
			PreferenceConverter.setValue(store, key, new FontData(font.getName(), font.getHeight(), style));
		}
	}
}
