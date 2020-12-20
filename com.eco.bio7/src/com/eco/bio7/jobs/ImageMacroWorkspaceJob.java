package com.eco.bio7.jobs;

import java.io.File;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import com.eco.bio7.actions.Bio7Action;

public class ImageMacroWorkspaceJob extends WorkspaceJob {

	private File file;

	public ImageMacroWorkspaceJob(File file) {
		super("ImageJ Macro");
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

}
