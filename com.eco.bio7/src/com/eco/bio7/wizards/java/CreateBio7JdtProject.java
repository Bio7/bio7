package com.eco.bio7.wizards.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.VMStandin;

import com.eco.bio7.compile.utils.ScanClassPath;

public class CreateBio7JdtProject {

	private IFolder sourceFolder;

	public CreateBio7JdtProject(String containerName, IProgressMonitor monitor) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(containerName);
		try {
			project.create(monitor);

			project.open(monitor);

			IProjectDescription description = project.getDescription();
			description.setNatureIds(new String[] { JavaCore.NATURE_ID });
			project.setDescription(description, monitor);

			IJavaProject javaProject = JavaCore.create(project);

			IFolder binFolder = project.getFolder("bin");
			binFolder.create(false, true, monitor);
			javaProject.setOutputLocation(binFolder.getFullPath(), monitor);

			sourceFolder = project.getFolder("src");
			sourceFolder.create(false, true, monitor);

			List<IClasspathEntry> entriesJre = new ArrayList<IClasspathEntry>();

			IVMInstallType installType = JavaRuntime
					.getVMInstallType("org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");

			VMStandin vmStandin = new VMStandin(installType, "Bio7 Bundled JRE");
			vmStandin.setName("Bio7 Bundled JRE");

			String path = Platform.getInstallLocation().getURL().getPath();
			vmStandin.setInstallLocation(new File(path + "/jre"));
			

			IVMInstall vmInstall = vmStandin.convertToRealVM();

			// for (LibraryLocation libraryLocation : libs) {
			// System.out.println(libraryLocation);
			// }


			// IVMInstall ivmInstall = JavaRuntime.getDefaultVMInstall();

			// IVMInstall vmInstall= JavaRuntime.getVMInstall(new
			// Path("C:/Program Files/Java/jdk1.7.0_25/jre"));

			// IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();

			// IPath containerPath=new Path(JavaRuntime.JRE_CONTAINER);
			// IPath vmPath = containerPath.append(new
			// Path("C:/Program Files/Java/jdk1.7.0_25/jre"));
			// JavaCore.newContainerEntry(vmPath);

			LibraryLocation[] locations = JavaRuntime
					.getLibraryLocations(vmInstall);
			for (LibraryLocation element : locations) {
				// System.out.println("location: "+locations);
				entriesJre.add(JavaCore.newLibraryEntry(
						element.getSystemLibraryPath(), null, null));
			}
			// javaProject.setRawClasspath(entries2.toArray(new
			// IClasspathEntry[entries2.size()]), null);

			IPackageFragmentRoot root11 = javaProject
					.getPackageFragmentRoot(sourceFolder);
			IClasspathEntry[] oldEntries = entriesJre
					.toArray(new IClasspathEntry[entriesJre.size()]);
			IClasspathEntry[] newEntries = new ScanClassPath().scanForJDT();
			System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
			newEntries[oldEntries.length] = JavaCore.newSourceEntry(root11
					.getPath());
			javaProject.setRawClasspath(newEntries, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public IContainer getSourceFolder() {
		// TODO Auto-generated method stub
		return sourceFolder;
	}

}
