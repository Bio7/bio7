/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.jobs;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import ij.IJ;

public class ImageJMacroWorkspaceJob extends WorkspaceJob implements IJobChangeListener {

	private String content;

	public ImageJMacroWorkspaceJob(String content) {
		super("ImageJ macro in progress....");
		this.content = content;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("ImageJ macro is running.....", IProgressMonitor.UNKNOWN);

		IJ.runMacro(content);
		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
			return Status.CANCEL_STATUS;
		}

		return Status.OK_STATUS;
	}

	public void aboutToRun(IJobChangeEvent event) {

	}

	public void awake(IJobChangeEvent event) {

	}

	public void done(IJobChangeEvent event) {

	}

	public void running(IJobChangeEvent event) {

	}

	public void scheduled(IJobChangeEvent event) {

	}

	public void sleeping(IJobChangeEvent event) {

	}

}
