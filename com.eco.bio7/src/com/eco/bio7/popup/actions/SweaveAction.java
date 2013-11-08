package com.eco.bio7.popup.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;

public class SweaveAction implements IObjectActionDelegate {

	private BufferedReader input;
	private OutputStream stdin;
	private Object dirPath;
	private String fi;
	private String name;

	public SweaveAction() {
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
					

					if (RServe.isAliveDialog()) {
						RConnection c = RServe.getConnection();

						try {
							c.eval("try(setwd('" + dirPath + "'));");
							c.eval("try(Sweave(\"" + project + "\"))");
							c.eval("try(dev.off());");

						} catch (RserveException e) {

							System.out.println(e.getMessage());
						}
					}
					
					
						//Process proc = Runtime.getRuntime().exec(
								//pdfLatexPath+"/pdflatex -interaction=nonstopmode -include-directory=" + sweaveScriptLocation + " " + "-output-directory=" + dirPath + " " + dirPath + "/" + name + ".tex");
						
						List<String> args = new ArrayList<String>();
						args.add(pdfLatexPath + "/pdflatex");
						args.add("-interaction=nonstopmode");
						args.add("-include-directory=" + sweaveScriptLocation);
						args.add("-output-directory=" + dirPath);
						args.add(dirPath + "/" + name + ".tex");

						Process proc = null;
						ProcessBuilder pb = new ProcessBuilder(args);
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
										Program.launch(dirPath + "/" + name + ".pdf");
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

	private void externalModify(IFile iFile) throws IOException {
		java.io.File file = iFile.getLocation().toFile();
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fOut.write("Written by FileOutputStream".getBytes());
		try {
			iFile.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
