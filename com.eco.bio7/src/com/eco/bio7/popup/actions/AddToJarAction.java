package com.eco.bio7.popup.actions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class AddToJarAction implements IObjectActionDelegate {

	public AddToJarAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		String project = null;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;

			Object selectedObj = strucSelection.getFirstElement();
			if (selectedObj instanceof IFile) {
				IFile selectedFile = (IFile) selectedObj;

				int i = 0;
				String name = "Added:";
				String nameofiofile;
				Iterator iter = strucSelection.iterator();

				JarOutputStream target = null;
				String fi = selectedFile.getRawLocation().toString();
				String fil = null;

				fil = new File(fi).getParentFile().getPath().replace("\\", "/");
				
				try {
					target = new JarOutputStream(new FileOutputStream(fil + "/library.jar"));

				} catch (FileNotFoundException e) {
					
					e.printStackTrace();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
				BufferedInputStream in = null;
				try {
					while (iter.hasNext()) {
						i++;
						IFile file = (IFile) iter.next();
						nameofiofile = getFileName(file.getName());
						project = file.getLocation().toString();
						name = name + "\n" + file.getName();

						JarEntry entry = new JarEntry(file.getName());
						entry.setTime(file.getModificationStamp());

						target.putNextEntry(entry);

						in = new BufferedInputStream(new FileInputStream(new File(project)));

						byte[] buffer = new byte[1024];
						while (true) {
							int count = in.read(buffer);
							if (count == -1)
								break;
							target.write(buffer, 0, count);
						}
						target.closeEntry();

					}
					in.close();
					target.close();

				} catch (IOException e) {

					e.printStackTrace();
				}
				Shell shell = new Shell();
				MessageDialog.openInformation(shell, "Added to Jar !", name);

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
