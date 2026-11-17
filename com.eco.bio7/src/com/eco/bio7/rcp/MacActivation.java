/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rcp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * macOS: a click on the icon of Bio7 in the Dock makes Bio7 the active
 * application, but its window stayed behind the windows of the other
 * applications and the activation was lost again (a second click was
 * necessary). SWT reports the activation of the application with an Activate
 * event without a widget: then the active window (else the main window) is
 * brought to the front and activated, so that the activation stays.
 */
public final class MacActivation {

	private static boolean installed;

	private MacActivation() {
	}

	/**
	 * Installs the activation handling (UI thread, macOS only, once).
	 *
	 * @param main the main window (if no window of Bio7 is active)
	 */
	public static void install(Display display, Shell main) {
		if (installed || display == null || !"Mac".equals(com.eco.bio7.util.Util.getOS())) {
			return;
		}
		installed = true;
		display.addFilter(SWT.Activate, e -> {
			if (e.widget != null) {
				/* The activation of a window of Bio7 (not of the application) */
				return;
			}
			/* At once (not in the next pass of the event loop): no delay if the user interface thread is busy */
			Shell shell = display.getActiveShell();
			if (shell == null || !shell.isVisible()) {
				shell = main;
			}
			if (shell != null && !shell.isDisposed() && shell.isVisible() && !shell.getMinimized()) {
				/* activateIgnoringOtherApps and makeKeyAndOrderFront: in front, the activation stays */
				shell.forceActive();
			}
		});
	}
}
