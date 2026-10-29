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
package com.eco.bio7.themes;

import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * The tab renderer of the tab folders in the views (e.g., the tabs of the 3D
 * options) of the dark theme: like the renderer of the view tabs but with the
 * minimum characters of a tab folder. The renderer of the view tabs shortens the
 * texts of all tabs to one character (the preference "show full text for view
 * tabs"), the light theme shows the full texts and the chevron with the number
 * of the hidden tabs.
 */
public class InnerTabRendering extends CTabRendering {

	private final CTabFolder folder;
	private final int minimumCharacters;

	public InnerTabRendering(CTabFolder parent) {
		/* The minimum characters of the tab folder (before the renderer changes them) */
		int characters = parent.getMinimumCharacters();
		super(parent);
		folder = parent;
		minimumCharacters = characters;
		parent.setMinimumCharacters(characters);
	}

	@Override
	protected void draw(int part, int state, Rectangle bounds, GC gc) {
		/* Again after a change of the preference (the renderer sets its minimum characters) */
		if (folder.getMinimumCharacters() != minimumCharacters) {
			int characters = minimumCharacters;
			folder.getDisplay().asyncExec(() -> {
				if (!folder.isDisposed()) {
					folder.setMinimumCharacters(characters);
				}
			});
		}
		super.draw(part, state, bounds, gc);
	}
}
