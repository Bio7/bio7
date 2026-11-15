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
package com.eco.bio7.pybridge;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * The Python Packages view (as the R packages view): install, update and remove
 * the packages of the Python of Bio7, the imported modules of the embedded
 * Python.
 */
public class PythonPackagesView extends ViewPart {

	public static final String ID = "com.eco.bio7.pybridge.PythonPackagesView";
	private static final String R_PACKAGES = "com.eco.bio7.rbridge.PackageInstallView";
	private PythonPackagesPanel panel;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new org.eclipse.swt.layout.FillLayout());
		panel = new PythonPackagesPanel(parent);
	}

	@Override
	public void setFocus() {
		panel.getControl().setFocus();
	}

	/** Shows the view with a package in the tab "Install Packages" (UI thread). */
	public static void prepareInstall(String pkg) {
		show();
		try {
			var page = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page.findView(ID) instanceof PythonPackagesView v && v.panel != null) {
				v.panel.prepareInstall(pkg);
			}
		} catch (RuntimeException e) {
			/* No window */
		}
	}

	/** Shows the view as a tab beside the R packages view (if shown), else where the workbench places it. */
	public static void show() {
		PythonViews.showBeside(ID, R_PACKAGES);
	}
}
