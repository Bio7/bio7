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

/** Create Shiny App (WindowBuilder Shiny): the Shiny app of a GUI class at its location. */
public class CreateShinyAppAction extends WindowBuilderAppAction {

	public CreateShinyAppAction() {
		super(OutputLocation.Kind.SHINY, false);
	}
}
