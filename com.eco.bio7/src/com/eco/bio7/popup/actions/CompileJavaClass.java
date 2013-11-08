/*package com.eco.bio7.popup.actions;

import java.io.File;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.compile.BeanShellInterpreter;

public class CompileJavaClass implements IObjectActionDelegate {

	public CompileJavaClass() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		String project = null;
		ISelection selection = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;
			if (strucSelection.size() == 0) {

			} else if (strucSelection.size() == 1) {
				Object selectedObj = strucSelection.getFirstElement();
				if (selectedObj instanceof IFile) {
					IFile selectedFile = (IFile) selectedObj;
					project = selectedFile.getLocation().toString();
					File file = selectedFile.getLocation().toFile();

					String toadd = "addClassPath(\"" + project + "\")";
					BeanShellInterpreter.interpret(toadd, null);
					Shell shell = new Shell();
					MessageDialog.openInformation(shell, "Library imported !",
							project);

				}

			} else {
				String name = "Added:";
				Iterator iter = strucSelection.iterator();
				while (iter.hasNext()) {
					IFile file = (IFile) iter.next();
					project = file.getLocation().toString();

					name = name + "\n" + file.getName();
					String toadd = "addClassPath(\"" + project + "\")";
					BeanShellInterpreter.interpret(toadd, null);

				}
				Shell shell = new Shell();
				MessageDialog
						.openInformation(shell, "Library imported !", name);

			}
		} 

	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
*/