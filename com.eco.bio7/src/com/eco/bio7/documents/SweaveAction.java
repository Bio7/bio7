package com.eco.bio7.documents;
/*package com.eco.bio7.popup.actions;

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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;
import net.sourceforge.texlipse.editor.TexEditor;

public class SweaveAction extends Action {

	private BufferedReader input;
	private OutputStream stdin;
	private String fi;

	private String project;

	public SweaveAction() {
		super();
		setId("com.eco.bio7.sweave");
		setActionDefinitionId("com.eco.bio7.sweaveAction");
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run() {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			 Bring the console to the front and clear it! 
			utils.cons.activate();
			utils.cons.clear();
		}
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (editor == null || editor instanceof TexEditor == false) {

			return;
		}
		if (editor instanceof TexEditor) {

			if (editor.isDirty()) {
				editor.doSave(new NullProgressMonitor());
			}

			IEditorInput editorInput = editor.getEditorInput();
			IFile aFile = null;

			if (editorInput instanceof IFileEditorInput) {
				aFile = ((IFileEditorInput) editorInput).getFile();
			}

			IFile selectedFile = (IFile) aFile;
			final String selFile = selectedFile.getName();

			final String theName = selFile.replaceFirst("[.][^.]+$", "");

			project = selectedFile.getLocation().toString();
			project = project.replace("\\", "/");
			fi = selectedFile.getRawLocation().toString();

			String dirPath = new File(fi).getParentFile().getPath().replace("\\", "/");

			//System.out.println(dirPath);

			System.out.println(dirPath);

			if (RServe.isAliveDialog()) {
				RConnection c = RServe.getConnection();

				try {
					c.eval("try(setwd('" + dirPath + "'));");
					c.eval("try(Sweave(\"" + project + "\"))");
					//c.eval("try(dev.off());");//not needed with Rserve?

				} catch (RserveException e) {

					System.out.println(e.getMessage());
				}
			}

			compileLatex(aFile.getProject(), theName, dirPath);

		}

	}

	private void compileLatex(final IProject activeProject, final String theName, String dirPath) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String pdfLatexPath = store.getString("pdfLatex");
		boolean useBrowser = store.getBoolean("PDF_USE_BROWSER");
		String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");
		String sweaveScriptLocation = store.getString("SweaveScriptLocation");
		sweaveScriptLocation = sweaveScriptLocation.replace("\\", "/");

		List<String> args = new ArrayList<String>();

		if (pdfLatexPath.isEmpty() == false) {

			pdfLatexPath = pdfLatexPath.replace("\\", "/");

			// String temp=dirPath+"/" +
			// theName+".tex";
			// String url = temp.replace("\\", "/");

			// Process proc =
			// Runtime.getRuntime().exec(
			// pdfLatexPath+"/pdflatex
			// -interaction=nonstopmode "
			// + "-output-directory=" + dirPath +
			// " " + dirPath + "/" + theName +
			// ".tex");
			
			 * Eventually take care of whitespaces in path!
			 

			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				args.add("\"" + pdfLatexPath + "/pdflatex" + "\"");
			}

			else {
				args.add(pdfLatexPath + "/pdflatex");
			}
		}
		 Try to start from the PATH environment! 
		else {

			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				args.add("pdflatex");
			}

			else {
				args.add("pdflatex");
			}

		}
		
		args.add("-interaction=nonstopmode");
		args.add("-include-directory=" + sweaveScriptLocation);
		args.add("-output-directory=" + dirPath);
		args.add(dirPath + "/" + theName + ".tex");

		Process proc = null;
		ProcessBuilder pb = new ProcessBuilder(args);
		// set environment variable u
		
		 * String otexinputs =env.get("TEXINPUTS"); env.put("TEXINPUTS", otexinputs+"/"+dirPath);
		 

		
		 * Set the working directory for the process from Java!
		 
		// pb.directory(new File(dirPath));

		pb.redirectErrorStream();
		try {
			proc = pb.start();

		} catch (IOException e) {

			e.printStackTrace();
			
			 * Bio7Dialog.message( "Rserve executable not available !" ); RServe.setConnection(null);
			 
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
					File fil = new File(dirPath + "/" + theName + ".pdf");
					if (fil.exists()) {

						
						 * if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) { RServe.plotLinux(dirPath + "/" + theName + ".pdf"); }
						 

						// else {

						// Program.launch(dirPath + "/" + theName + ".pdf");
						RServe.openPDF(dirPath + "/", theName + ".pdf", useBrowser, openInJavaFXBrowser);
						// }
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
					// TODO Auto-generated catch
					// block
					e.printStackTrace();
				}

			}
		}.start();
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
*/