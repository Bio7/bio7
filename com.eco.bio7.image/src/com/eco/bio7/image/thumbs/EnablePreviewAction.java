/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image.thumbs;

import java.util.prefs.Preferences;

import org.eclipse.jface.action.Action;

public class EnablePreviewAction extends Action {

	public EnablePreviewAction() {
		super("Preview", AS_CHECK_BOX);

		setId("preview");
		setText("Mag.");

		/*
		 * ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(
		 * Display.getCurrent(), getClass().getResourceAsStream(
		 * "/images/bild.gif")));
		 */

	}

	public void run() {

		ThumbnailsView view = ThumbnailsView.getThumb();
		if (view.thumbnail.isVisible()) {
			view.thumbnail.setVisible(false);
			view.sashForm.setWeights(new int[] { 1, 0});
		} else {
			view.thumbnail.setVisible(true);
			view.sashForm.setWeights(new int[] { 3, 1});
		}

		/*
		 * ThumbnailsView.getThumb().setInarow(15); Preferences prefs =
		 * Preferences.userNodeForPackage(this.getClass());
		 * 
		 * prefs.put("PREVIEW", "TRUE");
		 */
	}

}