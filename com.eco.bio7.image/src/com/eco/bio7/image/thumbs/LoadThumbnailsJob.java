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

import java.io.File;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

class LoadThumbnailsJob extends WorkspaceJob {

	File[] files;

	public LoadThumbnailsJob(File[] filesin) {
		super("Load Thumbs");

		this.files = filesin;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {

		monitor.beginTask("Load Images in preview", files.length);
		ThumbnailsView.getThumb().setCanceled(false);
		ThumbnailsView.getThumb().setImages(files, monitor);
        
		return Status.OK_STATUS;
	}

}
