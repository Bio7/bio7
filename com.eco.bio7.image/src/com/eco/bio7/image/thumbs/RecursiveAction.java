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

import org.eclipse.jface.action.Action;

public class RecursiveAction extends Action {

	public RecursiveAction() {
		super("Recursive", AS_CHECK_BOX);

		setId("recursive");
		setText("Rec.");

	}

	public void run() {

		if (ThumbnailAction.isRecursive()) {
			ThumbnailAction.setRecursive(false);
		} else {
			ThumbnailAction.setRecursive(true);
		}

	}

}