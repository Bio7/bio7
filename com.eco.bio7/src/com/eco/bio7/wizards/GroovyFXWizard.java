package com.eco.bio7.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

public class GroovyFXWizard extends Wizard implements INewWizard {
	private GroovyFXWizardPage page;
	private ISelection selection;

	public GroovyFXWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new GroovyFXWizardPage(selection);
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

		monitor.beginTask("Creating " + fileName, 2);
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName
					+ "\" does not exist.");
		}
		
		//System.out.println(resource.getProject().getFullPath());
		
		IContainer container = (IContainer) resource;
		
		final IFile file = container.getFile(new Path(fileName));
		
		
		String an = "My_Model";
		int index = file.getName().lastIndexOf('.');
	      if (index>0&& index <= file.getName().length() - 2 ) {
	      an=file.getName().substring(0, index);
	      }  
	    final IFile fileFxml = container.getFile(new Path(an+".fxml"));
	  
		try {
			InputStream stream = openContentStreamJavaFile(an,resource);
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		
		String an2 = "My_Model";
		int index2 = fileFxml.getName().lastIndexOf('.');
	      if (index2>0&& index2 <= fileFxml.getName().length() - 2 ) {
	      an2=fileFxml.getName().substring(0, index);
	      }  
		/*Create*.fxml file with the same filename!*/
		try {
			InputStream stream = openContentStreamFXMLFile(an2);
			if (fileFxml.exists()) {
				fileFxml.setContents(stream, true, true, monitor);
			} else {
				fileFxml.create(stream, true, monitor);
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
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}

	private InputStream openContentStreamJavaFile(String filename, IResource resource) {
		String linefeed = "\r\n";
		String head="import javafx.fxml.FXML;"
				+linefeed+linefeed+
				"class "+filename+"{" +linefeed+linefeed+
				"	def "+filename+"(){"+linefeed+
				"	def view=new CustomView();" +linefeed+
				"	view.setFxmlCanvas(\"id\",FileRoot.getFileRoot()+\""+resource.getFullPath()+"/"+filename+".fxml\",this);" +linefeed+linefeed+
				"	}"+linefeed+
				"}"+linefeed+
				"new "+filename+"()";
		
	
	//view.setSingleView(true);
	
	

		

		String contents = head;

		return new ByteArrayInputStream(contents.getBytes());
	}
	private InputStream openContentStreamFXMLFile(String filename) {
		String linefeed = "\r\n";
		String head="<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<?import java.lang.*?>" +linefeed+
				"<?import java.util.*?>" +linefeed+
				"<?import javafx.scene.layout.*?>" +linefeed+
				"<?import javafx.scene.paint.*?>" +linefeed+
				"<AnchorPane id=\"AnchorPane\" maxHeight=\"-Infinity\" maxWidth=\"-Infinity\" minHeight=\"-Infinity\" minWidth=\"-Infinity\" prefHeight=\"400.0\" prefWidth=\"600.0\" xmlns:fx=\"http://javafx.com/fxml\" />";


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