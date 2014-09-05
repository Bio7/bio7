package com.eco.bio7.popup.actions;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.r.RState;
import com.eco.bio7.rbridge.RServe;


public class RPackageSkeleton implements IObjectActionDelegate {

	private String filePath;
	private String dirPath;
	public String packageName;
	public String[] includeRVariables;
	public boolean builtFromVariables;
	public boolean cancelCreation = false;

	public RPackageSkeleton() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {

		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;

			Object selectedObj = strucSelection.getFirstElement();
			IResource resource = (IResource) strucSelection.getFirstElement();
			final IProject activeProject = resource.getProject();
			
			if (selectedObj instanceof IFile) {
				IFile selectedFile = (IFile) selectedObj;

				Iterator<?> iter = strucSelection.iterator();

				String fi = selectedFile.getRawLocation().toString();
				dirPath = new File(fi).getParentFile().getPath().replace("\\", "/");
				// System.out.println(dirPath);

				Vector<String> v = new Vector<String>();
				while (iter.hasNext()) {

					IFile file = (IFile) iter.next();

					filePath = file.getLocation().toString();

					file.getName();
					//System.out.println(filePath);
					v.add(filePath);

				}
				String[] stringsToR = v.toArray(new String[v.size()]);
				if (RServe.isAliveDialog()) {
					RConnection c = RServe.getConnection();
					try {
						c.assign("tempPackageFiles", stringsToR);

					} catch (REngineException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					RSkeletonInputDialog diag = new RSkeletonInputDialog(new Shell(), this);
					diag.open();
					if (cancelCreation == false) {

						if (RState.isBusy() == false) {
							RState.setBusy(true);
							Job job = new Job("R Package Skeleton") {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									monitor.beginTask("Create R Package Skeleton ...", IProgressMonitor.UNKNOWN);

									RConnection c = RServe.getConnection();
									try {
										c.assign("listOfVariables", includeRVariables);
									} catch (REngineException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}

									try {

										if (builtFromVariables) {
											c.eval("try(package.skeleton(name=\"" + packageName + "\",path=\"" + dirPath + "\",listOfVariables))");
										} else {
											c.eval("try(package.skeleton(name=\"" + packageName + "\",path=\"" + dirPath + "\",code_files =tempPackageFiles))");
										}
										/* Remove the temporary variable! */
										c.eval("try(remove(tempPackageFiles))");
										c.eval("try(remove(listOfVariables))");
									} catch (RserveException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
									IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
									IProject proj = root.getProject(activeProject.getName());
									try {
										proj.refreshLocal(IResource.DEPTH_INFINITE, null);
									} catch (CoreException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

									monitor.done();
									return Status.OK_STATUS;
								}

							};
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

										RState.setBusy(false);
									} else {

										RState.setBusy(false);
									}
								}
							});
							// job.setSystem(true);
							job.schedule();
						} else {
							Bio7Dialog.message("RServer is busy!");
						}
					}
				}
				

			}
		}
		

	}

	public static String getFileName(String path) {

		String fileName = null;
		String separator = File.separator;

		int pos = path.lastIndexOf(separator);
		int pos2 = path.lastIndexOf(".");

		if (pos2 > -1)
			fileName = path.substring(pos + 1, pos2);
		else
			fileName = path.substring(pos + 1);

		return fileName;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
