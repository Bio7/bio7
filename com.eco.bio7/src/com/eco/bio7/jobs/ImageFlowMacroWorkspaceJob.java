package com.eco.bio7.jobs;

import java.io.File;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import com.eco.bio7.actions.Bio7Action;

public class ImageFlowMacroWorkspaceJob extends WorkspaceJob implements IJobChangeListener {

	private File file;

	public ImageFlowMacroWorkspaceJob(File file) {
		super("ImageJ macro in progress....");
		this.file = file;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("ImageJ macro" + file.getName() + " is running.....", IProgressMonitor.UNKNOWN);
		
		ij.IJ.runMacroFile(file.getAbsolutePath());
		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {
				Bio7Action.stopFlow();
				System.out.println("Flow canceled!");

			}
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
