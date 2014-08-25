package com.eco.bio7.popup.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.commons.compiler.jdk.JavaSourceClassLoader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.compile.utils.ScanClassPath;
import com.eco.bio7.rcp.StartBio7Utils;

public class RecalculateClasspath implements IObjectActionDelegate {

	public RecalculateClasspath() {
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
			if (selectedObj instanceof IAdaptable) {
				IProject project = (IProject) ((IAdaptable) selectedObj).getAdapter(IProject.class);
				try {
					if (project.hasNature(JavaCore.NATURE_ID)) {

						IJavaProject javaProject = JavaCore.create(project);

						IFolder sourceFolder = project.getFolder("src");
						IPackageFragmentRoot fragRoot = javaProject.getPackageFragmentRoot(sourceFolder);

						List<IClasspathEntry> entriesJre = new ArrayList<IClasspathEntry>();


						IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();

						LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
						for (LibraryLocation element : locations) {
							// System.out.println("location: "+locations);
							entriesJre.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null));
						}
						IClasspathEntry[] newEntries = new ScanClassPath().scanForJDT();

						IClasspathEntry[] oldEntries = entriesJre.toArray(new IClasspathEntry[entriesJre.size()]);

						System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
						newEntries[oldEntries.length] = JavaCore.newSourceEntry(fragRoot.getPath());

						try {
							javaProject.setRawClasspath(newEntries, null);
						} catch (JavaModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Bio7Dialog.message("Java Bio7 Project Libraries Recalculated!");

					} else {
						Bio7Dialog.message("Please select a Java Project!");
					}
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
