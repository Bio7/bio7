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
package com.eco.bio7.windowbuilder;

/** Change Shiny App Location: chooses the location of the Shiny app of a GUI class again and creates it there. */
public class ShinyAppLocationAction extends WindowBuilderAppAction {

	public ShinyAppLocationAction() {
		super(OutputLocation.Kind.SHINY, true);
	}
}
