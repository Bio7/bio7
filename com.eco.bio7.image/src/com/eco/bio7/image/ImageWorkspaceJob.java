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

package com.eco.bio7.image;

import java.io.File;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ImageWorkspaceJob extends WorkspaceJob {

	private File file;
	private IProgressMonitor monitor;
	private int total;
	private int bytecount;

	public ImageWorkspaceJob(int total, int bytecount) {
		super("Job");
		this.file = file;
		this.total = total;
		this.bytecount = bytecount;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		this.monitor = monitor;
		monitor.beginTask("Load Image", total);

		while (total < bytecount) {
			monitor.worked(1);
		}

		return Status.OK_STATUS;
	}

}
