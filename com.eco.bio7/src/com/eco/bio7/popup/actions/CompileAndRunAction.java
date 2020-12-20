/*package com.eco.bio7.popup.actions;

import java.io.File;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.compile.Compile;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.methods.Compiled;

public class CompileAndRunAction implements IObjectActionDelegate {

	public CompileAndRunAction() {
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
				String nameofiofile;
				Object selectedObj = strucSelection.getFirstElement();
				if (selectedObj instanceof IFile) {
					IFile selectedFile = (IFile) selectedObj;
					nameofiofile = getFileName(selectedFile.getName());
					project = selectedFile.getLocation().toString();
					File file = selectedFile.getLocation().toFile();
					Compiled.setModel(null);
					try {
						Compile.compile(BatchModel.fileToString(selectedFile
								.getLocation().toString()));
					} catch (InstantiationException e) {

						e.printStackTrace();
					}
					if (Compiled.getModel() != null) {
						String tointerpret = "Model " + nameofiofile
								+ "=Compiled.getEcoclass();";
						BeanShellInterpreter.interpret(tointerpret, null);
						BeanShellInterpreter.interpretJob(nameofiofile + ".run();",
								null);
					}

				}
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
