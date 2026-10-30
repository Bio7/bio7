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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * GTK: the tab row of an empty tab folder (e.g., the editor area without an
 * editor) has the height of the font, the tool bar with the minimize and
 * maximize buttons is higher and overlaps the content below the tab row. The
 * tab row of an empty tab folder gets the height of its tool bar, a folder with
 * tabs has the height of the tabs again (the tabs are not cut).
 */
final class EmptyFolderHeight {

	private static final boolean GTK = "gtk".equals(SWT.getPlatform());
	/* A tab folder with the height of its tool bar (see adjust) */
	private static final String FIXED = "bio7.themes.emptyFolderHeight";
	private static final String LISTENER = "bio7.themes.emptyFolderListener";
	private static boolean installed;

	private EmptyFolderHeight() {
	}

	static void install() {
		if (installed || !GTK) {
			return;
		}
		installed = true;
		Display display = Display.getDefault();
		display.addFilter(SWT.Resize, event -> {
			if (event.widget instanceof CTabFolder folder) {
				display.asyncExec(() -> adjust(folder));
			}
		});
	}

	private static void adjust(CTabFolder folder) {
		if (folder.isDisposed()) {
			return;
		}
		boolean fixed = folder.getData(FIXED) != null;
		if (folder.getItemCount() > 0) {
			/* The tabs: the height of the tabs again */
			if (fixed) {
				folder.setData(FIXED, null);
				folder.setTabHeight(SWT.DEFAULT);
			}
			return;
		}
		Control top = folder.getTopRight();
		if (top == null || top.isDisposed() || !top.isVisible()) {
			return;
		}
		int height = top.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		if (height > folder.getTabHeight()) {
			if (folder.getData(LISTENER) == null) {
				folder.setData(LISTENER, Boolean.TRUE);
				/* A new tab (e.g., an opened editor) without a resize of the folder */
				folder.addListener(SWT.Paint, _ -> {
					if (folder.getData(FIXED) != null && folder.getItemCount() > 0) {
						folder.getDisplay().asyncExec(() -> adjust(folder));
					}
				});
			}
			folder.setData(FIXED, Boolean.TRUE);
			folder.setTabHeight(height);
		}
	}
}
