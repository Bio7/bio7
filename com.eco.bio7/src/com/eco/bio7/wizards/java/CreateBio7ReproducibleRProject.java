package com.eco.bio7.wizards.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import com.eco.bio7.util.Util;

public class CreateBio7ReproducibleRProject {

	String author;
	String title;
	Combo combo;
	String format = "";
	String selectedFormat;

	public CreateBio7ReproducibleRProject(String containerName, IProgressMonitor monitor, Bio7ReproducibleRProjectWizardPage page) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(containerName);
		try {
			project.create(monitor);
			project.open(monitor);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Util.getDisplay().syncExec(new Runnable() {
			public void run() {
				try {

					if (page.getBtnCreateDataFolder().getSelection()) {
						IFolder dataFolder = project.getFolder("data");
						dataFolder.create(false, true, monitor);
					}
					// javaProject.setOutputLocation(dataFolder.getFullPath(), monitor);
					if (page.getBtnCreateDocFolder().getSelection()) {
						IFolder docFolder = project.getFolder("doc");
						docFolder.create(false, true, monitor);
					}
					if (page.getBtnCreateFigsFolder().getSelection()) {
						IFolder figsFolder = project.getFolder("figs");
						figsFolder.create(false, true, monitor);
					}
					if (page.getBtnCreateOutputFolder().getSelection()) {
						IFolder outputFolder = project.getFolder("output");
						outputFolder.create(false, true, monitor);
					}
					if (page.getBtnCreateRFolder().getSelection()) {
						IFolder rFolder = project.getFolder("R");
						rFolder.create(false, true, monitor);
					}
					if (page.getBtnCreateReportsFolder().getSelection()) {
						IFolder reportsFolder = project.getFolder("reports");
						reportsFolder.create(false, true, monitor);
					}
					if (page.getBtnCreateReferencesFolder().getSelection()) {
						IFolder referencesFolder = project.getFolder("references");
						referencesFolder.create(false, true, monitor);
					}
					if (page.getBtnCreateModelFolder().getSelection()) {
						IFolder modelFolder = project.getFolder("model");
						modelFolder.create(false, true, monitor);
					}

					// IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IResource resource = root.findMember(new Path(containerName));
					if (!resource.exists() || !(resource instanceof IContainer)) {
						throwCoreException("Container \"" + containerName + "\" does not exist.");
					}
					IContainer container = (IContainer) resource;

					monitor.worked(1);
					if (page.getBtnCreateRFile().getSelection()) {
						final IFile fileMd = container.getFile(new Path("01_template.R"));
						String anR = "template";
						int indexR = fileMd.getName().lastIndexOf('.');
						if (indexR > 0 && indexR <= fileMd.getName().length() - 2) {
							anR = fileMd.getName().substring(0, indexR);
						}

						try {
							InputStream stream = openContentRStream(anR);
							if (fileMd.exists()) {
								fileMd.setContents(stream, true, true, monitor);
							} else {
								fileMd.create(stream, true, monitor);
							}
							stream.close();
						} catch (IOException e) {
						}
						monitor.worked(1);
					}
					if (page.getBtnCreateReadme().getSelection()) {
						final IFile file = container.getFile(new Path("README.md"));
						String an = "template";
						int index = file.getName().lastIndexOf('.');
						if (index > 0 && index <= file.getName().length() - 2) {
							an = file.getName().substring(0, index);
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
						monitor.setTaskName("Opening file for editing...");
						Util.getDisplay().asyncExec(new Runnable() {
							public void run() {
								IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
								try {
									IDE.openEditor(page, file, true);
									// IDE.openEditor(page, file,
									// "org.eclipse.ui.DefaultTextEditor");
								} catch (PartInitException e) {
								}
							}
						});
						monitor.worked(1);
					}
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	private InputStream openContentStream(String filename) {

		String contents = "# Readme";

		return new ByteArrayInputStream(contents.getBytes());
	}
	private InputStream openContentRStream(String filename) {

		String contents = "";

		return new ByteArrayInputStream(contents.getBytes());
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "com.eco.bio7", IStatus.OK, message, null);
		throw new CoreException(status);
	}

}
