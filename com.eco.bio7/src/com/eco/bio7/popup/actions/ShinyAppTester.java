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
package com.eco.bio7.popup.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IFileEditorInput;

/**
 * The property com.eco.bio7.shiny.isApp of an editor input: the file is a Shiny
 * app (the toolbar button "Start Shiny App" is shown).
 */
public class ShinyAppTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		return "isApp".equals(property) && receiver instanceof IFileEditorInput fi && ShinyApps.isShinyApp(fi.getFile());
	}
}
