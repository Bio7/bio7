package com.eco.bio7.popup.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.utils.ScanClassPath;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

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
				if (project != null) {
					try {
						if (project.hasNature(JavaCore.NATURE_ID)) {
							javafx.application.Platform.runLater(new Runnable() {
								@Override
								public void run() {
									recalculateClasspath(project);
								}
							});

						} else {
							Bio7Dialog.message("Please select a Java Project!");
						}
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					Bio7Dialog.message("Please select a Java Project!");
				}
			}
		}

	}

	private void recalculateClasspath(IProject project) {
		IJavaProject javaProject = JavaCore.create(project);

		IFolder sourceFolder = project.getFolder("src");
		IPackageFragmentRoot fragRoot = javaProject.getPackageFragmentRoot(sourceFolder);

		List<IClasspathEntry> entriesJre = new ArrayList<IClasspathEntry>();

		IVMInstallType installType = JavaRuntime.getVMInstallType("org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");

		VMStandin vmStandin = new VMStandin(installType, "Bio7 Bundled OpenJDK");
		vmStandin.setName("Bio7 Bundled OpenJDK");

		String path = Platform.getInstallLocation().getURL().getPath();
		/* Extra path for the different MacOSX installation paths! */
		String OS = ApplicationWorkbenchWindowAdvisor.getOS();
		if (OS.equals("Mac")) {
			vmStandin.setInstallLocation(new File(path + "../MacOS/jdk/Contents/Home/"));

		} else {
			vmStandin.setInstallLocation(new File(path + "/jdk"));
		}

		IVMInstall vmInstall = vmStandin.convertToRealVM();

		// IVMInstall vmInstall =
		// JavaRuntime.getDefaultVMInstall();

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
			// e.printStackTrace();
			System.out.println("Minor error! Please check the classpath of the project and if necessary calculate again!");
		}
		Bio7Dialog.message("Java Bio7 Project Libraries Recalculated!");
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
