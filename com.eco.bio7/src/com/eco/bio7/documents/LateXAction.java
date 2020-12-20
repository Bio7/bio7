package com.eco.bio7.documents;
/*package com.eco.bio7.popup.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class LateXAction implements IObjectActionDelegate {

	private BufferedReader input;
	private OutputStream stdin;
	private String dirPath;
	private String fi;
	private String name;

	public LateXAction() {
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

				IResource resource = (IResource) strucSelection.getFirstElement();
				final IProject activeProject = resource.getProject();

				if (selectedObj instanceof IFile) {
					IFile selectedFile = (IFile) selectedObj;
					nameofiofile = getFileName(selectedFile.getName());
					project = selectedFile.getLocation().toString();
					project = project.replace("\\", "/");
					fi = selectedFile.getRawLocation().toString();
					name = nameofiofile;
					dirPath = null;

					dirPath = new File(fi).getParentFile().getPath().replace("\\", "/");
					IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
					String sweaveScriptLocation = store.getString("SweaveScriptLocation");
					String pdfLatexPath = store.getString("pdfLatex");
					sweaveScriptLocation = sweaveScriptLocation.replace("\\", "/");
					pdfLatexPath = pdfLatexPath.replace("\\", "/");

					// Process proc = Runtime.getRuntime().exec(
					// pdfLatexPath+"/pdflatex -interaction=nonstopmode
					// -include-directory=" + sweaveScriptLocation + " " +
					// "-output-directory=" + dirPath + " " + dirPath + "/" +
					// name + ".tex");
					List<String> args = new ArrayList<String>();

					if (pdfLatexPath.isEmpty() == false) {
						args.add(pdfLatexPath + "/pdflatex");
					}
					
					else {

						args.add("pdflatex");
					}
					args.add("-interaction=nonstopmode");
					args.add("-include-directory=" + sweaveScriptLocation);
					args.add("-output-directory=" + dirPath);
					args.add(dirPath + "/" + name + ".tex");

					Process proc = null;
					ProcessBuilder pb = new ProcessBuilder(args);
					// set environment variable u
					
					pb.directory(new File(dirPath));
					pb.redirectErrorStream();
					try {
						proc = pb.start();

					} catch (IOException e) {
						e.printStackTrace();

					}

					input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					stdin = proc.getOutputStream();

					new Thread() {

						public void run() {
							setPriority(Thread.MAX_PRIORITY);
							String line;
							try {

								while ((line = input.readLine()) != null) {
									System.out.println(line);
								}
								File fil = new File(dirPath + "/" + name + ".pdf");
								if (fil.exists()) {
									if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
										RServe.plotLinux(dirPath + "/" + name + ".pdf");
									}

									else {
										Program.launch(dirPath + "/" + name + ".pdf");
									}
								} else {
									Bio7Dialog.message("*.pdf file was not created.\nPlease check the error messages!\nProbably an empty space in the file path caused the error!");
								}

							} catch (IOException e) {

								e.printStackTrace();
							}
							IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
							IProject proj = root.getProject(activeProject.getName());
							try {
								proj.refreshLocal(IResource.DEPTH_INFINITE, null);
							} catch (CoreException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					}.start();

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

}
*/