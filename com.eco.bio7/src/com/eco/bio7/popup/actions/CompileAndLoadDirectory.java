package com.eco.bio7.popup.actions;

import java.io.File;
import org.codehaus.commons.compiler.jdk.JavaSourceClassLoader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.rcp.StartBio7Utils;

public class CompileAndLoadDirectory implements IObjectActionDelegate {

	private File fi;
	private String name;
	private String dir;
	private IWorkbenchPage pag;
	private IResource res;
	private IFile selectedFile;

	public CompileAndLoadDirectory() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	IResource extractSelection(ISelection sel) {
		if (!(sel instanceof IStructuredSelection))
			return null;
		IStructuredSelection ss = (IStructuredSelection) sel;
		Object element = ss.getFirstElement();
		if (element instanceof IResource)
			return (IResource) element;
		if (!(element instanceof IAdaptable))
			return null;
		IAdaptable adaptable = (IAdaptable) element;
		Object adapter = adaptable.getAdapter(IResource.class);
		return (IResource) adapter;
	}

	

	public void run(IAction action) {
		compileClasses();

	}

	private void compileClasses() {
		pag = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		
		 res = extractSelection(selection);

		JavaSourceClassLoader.resource = res;

		IPath pa = res.getProject().getLocation();
		fi = pa.toFile();

		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;

			Object selectedObj = strucSelection.getFirstElement();
			if (selectedObj instanceof IFile) {
		    selectedFile = (IFile) selectedObj;
				/* Get the file location as String! */
				String selFile = selectedFile.getRawLocation().toString();

				/* Get the filename without extension! */
				name = selectedFile.getName().replaceFirst("[.][^.]+$", "");

				/* Get the parent directory! */
				dir = new File(selFile).getParentFile().getPath().replace("\\", "/");

			}
		}

		StartBio7Utils.getConsoleInstance().cons.clear();

		Job job = new Job("Compile And Run") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Compile And Run...", IProgressMonitor.UNKNOWN);
				
				
				CompileClassAndMultipleClasses cp = new CompileClassAndMultipleClasses();
				cp.compileClasses(res, selectedFile,pag);
				
				
				
				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

				} else {
					//JavaSourceClassLoader.directCall = false;
				}
			}
		});

		job.schedule();
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
