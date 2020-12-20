package com.eco.bio7.batch;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;

public class Bio7JobFlow extends WorkspaceJob implements IJobChangeListener {

	public Bio7JobFlow() {
		super("Job");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Flow is running.....", IProgressMonitor.UNKNOWN);

		BatchModel.batchStart();

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
