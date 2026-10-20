/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;

import com.eco.bio7.collection.Work;

/**
 * Shows or hides the main toolbar of the Bio7 window (see
 * {@link Work#toggleMainToolbar()}).
 */
public class ShowHideMainToolbarAction extends Action {

	public ShowHideMainToolbarAction(String text) {
		super(text);
		setId("com.eco.bio7.show_hide_main_toolbar");
		setToolTipText("Shows or hides the main toolbar");
	}

	public void run() {
		Work.toggleMainToolbar();
	}
}
