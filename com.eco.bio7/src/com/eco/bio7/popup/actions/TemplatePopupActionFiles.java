package com.eco.bio7.popup.actions;
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
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.compile.Compile;
import com.eco.bio7.compile.BeanShellInterpreter;

public class CompileAndLoadAction implements IObjectActionDelegate {

	public CompileAndLoadAction() {
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
			if (strucSelection.size() == 0) {

			} else if (strucSelection.size() == 1) {
				String nameofiofile;
				Object selectedObj = strucSelection.getFirstElement();
				if (selectedObj instanceof IFile) {
					IFile selectedFile = (IFile) selectedObj;
					nameofiofile = getFileName(selectedFile.getName());
					project = selectedFile.getLocation().toString();
					File file = selectedFile.getLocation().toFile();

					try {
						Compile.compile(BatchModel.fileToString(selectedFile.getLocation().toString()));
					} catch (InstantiationException e) {

						e.printStackTrace();
					}
					

				}
			} else {
				int i = 0;
				String name = "Added:";
				String nameofiofile;
				Iterator iter = strucSelection.iterator();
				while (iter.hasNext()) {
					i++;
					IFile file = (IFile) iter.next();
					nameofiofile = getFileName(file.getName());
					project = file.getLocation().toString();
					name = name + "\n" + file.getName();
					try {
						Compile.compile(BatchModel.fileToString(project));
					} catch (InstantiationException e) {

						e.printStackTrace();
					}
					String tointerpret = "Model " + nameofiofile + "=Compiled.getEcoclass();";
					BeanShellInterpreter.interpret(tointerpret, null);
					// System.out.println(nameofiofile);
				}
				Shell shell = new Shell();
				MessageDialog.openInformation(shell, "Objects to Beanshell !", name);

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

}*/
