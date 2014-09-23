package com.eco.bio7.wizards.java;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
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

public class JavaWindowBuilderBio7ClassWizard extends Wizard implements INewWizard {
	private JavaWindowBuilderBio7ClassWizardPage page;
	private ISelection selection;

	public JavaWindowBuilderBio7ClassWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new JavaWindowBuilderBio7ClassWizardPage(selection);
		addPage(page);
	}

	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
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
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	private void doFinish(String containerName, String fileName, IProgressMonitor monitor) throws CoreException {

		monitor.beginTask("Creating " + containerName, 2);

		CreateBio7JdtProject jdt = new CreateBio7JdtProject(containerName, monitor);

		monitor.beginTask("Creating " + fileName, 3);

		IContainer container = jdt.getSourceFolder();

		final IFile file = container.getFile(new Path(fileName));

		String an = "My_Model";
		int index = file.getName().lastIndexOf('.');
		if (index > 0 && index <= file.getName().length() - 2) {
			an = file.getName().substring(0, index);
		}
		final IFile fileFxml = container.getFile(new Path(an + ".java"));

		try {
			InputStream stream = openContentStreamJavaFile(an, container);
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		String an1 = "ModelGui";
		final IFile fileController = container.getFile(new Path(an1 + ".java"));
		try {
			InputStream stream = openContentStreamJavaControllerFile(an, container);
			if (fileController.exists()) {
				fileController.setContents(stream, true, true, monitor);
			} else {
				fileController.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}

		//String an2 = "My_Model";
		/*int index2 = fileFxml.getName().lastIndexOf('.');
		if (index2 > 0 && index2 <= fileFxml.getName().length() - 2) {
			an2 = fileFxml.getName().substring(0, index);
		}
		 Create*.fxml file with the same filename! 
		try {
			InputStream stream = openContentStreamFXMLFile(an2);
			if (fileFxml.exists()) {
				fileFxml.setContents(stream, true, true, monitor);
			} else {
				fileFxml.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}*/
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
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
		String head = "import org.eclipse.swt.SWT;" + linefeed +
				"import org.eclipse.swt.layout.FillLayout;" + linefeed +
				"import org.eclipse.swt.widgets.Composite;" + linefeed +
				"import com.eco.bio7.collection.CustomView;" + linefeed + linefeed +
				"public class " + filename + " extends com.eco.bio7.compile.Model {" + linefeed + linefeed +

		"\tpublic void setup(){" + linefeed + linefeed + 
		"\t\tCustomView view=new CustomView();" + linefeed + linefeed +
		"\t\tComposite parent = view.getComposite(\"guiId\");"+ linefeed + linefeed +
		"\t\tparent.setLayout(new FillLayout());"+ linefeed + linefeed +
		"\t\tnew ModelGui(parent,this,SWT.NONE);"+ linefeed + linefeed +
		"\t\tparent.layout(true);"+ linefeed + linefeed +
		 "\t}"+linefeed+linefeed+
		"\tpublic void run(){" + linefeed + linefeed + linefeed + "\t}" + linefeed + "}";

		// view.setSingleView(true);

		String contents = head;

		return new ByteArrayInputStream(contents.getBytes());
	}

	private InputStream openContentStreamJavaControllerFile(String filename, IResource resource) {
		
		

		String linefeed = "\r\n";
		String head = "import org.eclipse.swt.widgets.Composite;" + linefeed +linefeed+
				
				"public class ModelGui extends Composite {" + linefeed + linefeed + 
				"\tpublic ModelGui(Composite parent,"+filename+" model,int style) {" + linefeed +
				"\t\tsuper(parent, style);"
				+ linefeed + linefeed+

				"\t}" + linefeed + "}";

		// view.setSingleView(true);

		String contents = head;

		return new ByteArrayInputStream(contents.getBytes());
	}

	/*private InputStream openContentStreamFXMLFile(String filename) {
		String linefeed = "\r\n";
		String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<?import java.lang.*?>" + linefeed + "<?import java.util.*?>" + linefeed + "<?import javafx.scene.layout.*?>" + linefeed + "<?import javafx.scene.paint.*?>" + linefeed
				+ "<AnchorPane id=\"AnchorPane\" maxHeight=\"-Infinity\" maxWidth=\"-Infinity\" minHeight=\"-Infinity\" minWidth=\"-Infinity\" prefHeight=\"400.0\" prefWidth=\"600.0\" xmlns:fx=\"http://javafx.com/fxml\" />";

		String contents = head;

		return new ByteArrayInputStream(contents.getBytes());
	}*/

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "com.eco.bio7", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}