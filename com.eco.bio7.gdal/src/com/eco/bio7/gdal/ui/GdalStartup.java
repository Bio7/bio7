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
package com.eco.bio7.gdal.ui;

import org.eclipse.ui.IStartup;

/** Loads GDAL at the startup of the workbench (if enabled in the preferences, in a background job). */
public class GdalStartup implements IStartup {

	@Override
	public void earlyStartup() {
		GdalActivation.loadAtStartup();
	}
}
