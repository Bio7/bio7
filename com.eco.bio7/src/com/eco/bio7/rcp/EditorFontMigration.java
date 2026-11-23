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

package com.eco.bio7.rcp;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.eco.bio7.util.EditorFonts;

/**
 * Once per workspace (before the workbench starts): the stored font
 * preferences of the syntax colors of the Bio7 editors (colourkeyfont...) are
 * removed. They were copies of an editor font of an earlier time (e.g., another
 * size after a zoom) and are no setting of their own anymore: they follow the
 * editor font (see EditorFonts). The editor fonts chosen by the user (Colors and
 * Fonts) are kept.
 */
final class EditorFontMigration {

	private static final String DONE = "EDITOR_SYNTAX_FONTS_MIGRATED";
	private static final String[] EDITORS = { "com.eco.bio7.redit", "com.eco.bio7.pythonedit", "com.eco.bio7.ijmacro.editor", "com.eco.bio7.edit",
			"com.eco.bio7.markdownedit" };

	private EditorFontMigration() {
	}

	static void run() {
		try {
			IEclipsePreferences bio7 = InstanceScope.INSTANCE.getNode("com.eco.bio7");
			if (bio7.getBoolean(DONE, false)) {
				return;
			}
			for (String editor : EDITORS) {
				IEclipsePreferences node = InstanceScope.INSTANCE.getNode(editor);
				boolean changed = false;
				for (String key : node.keys()) {
					if (key.startsWith(EditorFonts.SYNTAX_FONT)) {
						node.remove(key);
						changed = true;
					}
				}
				if (changed) {
					node.flush();
				}
			}
			bio7.putBoolean(DONE, true);
			bio7.flush();
		} catch (BackingStoreException | RuntimeException e) {
			/* Tried again at the next start */
		}
	}
}
