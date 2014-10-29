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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class MediumThumbAction extends Action {

	public MediumThumbAction() {
		super("Medium", AS_CHECK_BOX);

		setId("medium");
		setText("Medium");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(
				Display.getCurrent(), getClass().getResourceAsStream(
						"/images/bild.gif")));

	}

	public void run() {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());

		ThumbnailsView.getThumb().setSize("medium");
		ThumbnailsView.getThumb().getLarge().setChecked(false);
		ThumbnailsView.getThumb().getMedium().setChecked(true);
		ThumbnailsView.getThumb().getSmall().setChecked(false);
		prefs.put("SIZE", "medium");
	}

}