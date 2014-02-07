package com.eco.bio7.wizards.java;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

import java.io.*;

import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

import com.eco.bio7.compile.utils.ScanClassPath;

public class JavaClassPluginWizard extends Wizard implements INewWizard {
	private JavaClassWizardPage page;
	private ISelection selection;

	public JavaClassPluginWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new JavaClassWizardPage(selection);
		addPage(page);
	}

	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException
					.getMessage());
			return false;
		}
		return true;
	}

	private void doFinish(String containerName, String fileName,
			IProgressMonitor monitor) throws CoreException {
		
		
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(containerName);
		project.create(monitor);
		project.open(monitor);
		
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, monitor);
		
		IJavaProject javaProject = JavaCore.create(project); 
		
		IFolder binFolder = project.getFolder("bin");
		binFolder.create(false, true, monitor);
		javaProject.setOutputLocation(binFolder.getFullPath(), monitor);
		
		IFolder sourceFolder = project.getFolder("src");
		sourceFolder.create(false, true, monitor);
		
		List<IClasspathEntry> entriesJre = new ArrayList<IClasspathEntry>();
		
		
		//IVMInstall vmInstall= JavaRuntime.getVMInstall(new Path("C:/Program Files/Java/jdk1.7.0_25/jre"));
		
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		
		//IPath containerPath=new Path(JavaRuntime.JRE_CONTAINER);
		//IPath vmPath = containerPath.append(new Path("C:/Program Files/Java/jdk1.7.0_25/jre"));
		//JavaCore.newContainerEntry(vmPath);
		
		
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation element : locations) {
			//System.out.println("location: "+locations);
		 entriesJre.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null));
		}
		//javaProject.setRawClasspath(entries2.toArray(new IClasspathEntry[entries2.size()]), null);

		
		IPackageFragmentRoot root11 = javaProject.getPackageFragmentRoot(sourceFolder);
		IClasspathEntry[] oldEntries = entriesJre.toArray(new IClasspathEntry[entriesJre.size()]);
		IClasspathEntry[] newEntries = new ScanClassPath().scanForJDT();
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newSourceEntry(root11.getPath());
		javaProject.setRawClasspath(newEntries, null);
		
		

		monitor.beginTask("Creating " + fileName, 2);
		
		
		//IWorkspaceRoot root2 = ResourcesPlugin.getWorkspace().getRoot();
		/*IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName
					+ "\" does not exist.");
		}*/
		
		
		IContainer container = sourceFolder;
		final IFile file = container.getFile(new Path(fileName));
		String an = "My_Class";
		int index = file.getName().lastIndexOf('.');
	      if (index>0&& index <= file.getName().length() - 2 ) {
	      an=file.getName().substring(0, index);
	      }  
	   
		try {
			InputStream stream = openContentStream(an);
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		
		
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, "org.eclipse.jdt.ui.CompilationUnitEditor",true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}

	private InputStream openContentStream(String filename) {
		String linefeed = "\r\n";
		String head = "public class "+filename+" {"+linefeed+linefeed+
		"	public static void main(String[] args) {"+linefeed+
		"	}"+linefeed+
		"}";
		

		String contents = head;

		return new ByteArrayInputStream(contents.getBytes());
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "com.eco.bio7", IStatus.OK,
				message, null);
		throw new CoreException(status);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}