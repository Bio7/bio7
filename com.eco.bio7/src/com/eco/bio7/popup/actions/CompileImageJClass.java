package com.eco.bio7.popup.actions;

import java.io.IOException;
import java.io.StringReader;

import ij.IJ;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.jdk.SimpleCompiler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.compile.Model;
import com.eco.bio7.methods.Compiled;

public class CompileImageJClass implements IObjectActionDelegate {

	public CompileImageJClass() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {

		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;
			if (strucSelection.size() == 0) {

			} else if (strucSelection.size() == 1) {
				Object selectedObj = strucSelection.getFirstElement();
				if (selectedObj instanceof IFile) {
					IFile selectedFile = (IFile) selectedObj;

					final String loc = selectedFile.getLocation().toString();
					
					Job job = new Job("Compile Java To Classfile") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							monitor.beginTask("Compile Java...", IProgressMonitor.UNKNOWN);

							IJ.runPlugIn("ij.plugin.Compiler", loc);

							monitor.done();
							return Status.OK_STATUS;
						}

					};
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {

							} else {

							}
						}
					});
					// job.setSystem(true);
					job.schedule();

					

				}

			} else {

			}
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
