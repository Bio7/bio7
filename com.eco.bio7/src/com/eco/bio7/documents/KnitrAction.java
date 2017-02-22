package com.eco.bio7.documents;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.browser.MultiPageEditor;
import com.eco.bio7.collection.Work;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;

import net.sourceforge.texlipse.editor.TexEditor;

public class KnitrAction extends Action  {

	private BufferedReader input;
	private OutputStream stdin;
	private String fi;
	private String name;
	private String project;
	private String dirPath;

	public KnitrAction() {
		super();
		setId("com.eco.bio7.browser.knitr");
		setActionDefinitionId("com.eco.bio7.browser.knitrAction");
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/*public void run(IAction action) {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			 Bring the console to the front and clear it! 
			utils.cons.activate();
			utils.cons.clear();
		}
		String project = null;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;
			if (strucSelection.size() == 0) {

			} else if (strucSelection.size() == 1) {
				final String nameofiofile;
				Object selectedObj = strucSelection.getFirstElement();

				IResource resource = (IResource) strucSelection.getFirstElement();
				final IProject activeProject = resource.getProject();

				knitrFile(selectedObj, activeProject);

			}

		}

	}*/

	public void run() {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			/* Bring the console to the front and clear it! */
			utils.cons.activate();
			utils.cons.clear();
		}
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (editor == null) {

			return;
		}
		if (editor instanceof TexEditor || editor instanceof MultiPageEditor) {

			if (editor.isDirty()) {
				editor.doSave(new NullProgressMonitor());
			}

			IEditorInput editorInput = editor.getEditorInput();
			IFile aFile = null;

			if (editorInput instanceof IFileEditorInput) {
				aFile = ((IFileEditorInput) editorInput).getFile();
			}

			knitrFile(aFile, aFile.getProject());
		}
	}

	private void knitrFile(Object selectedObj, final IProject activeProject) {

		final String nameofiofile;
		if (selectedObj instanceof IFile) {
			IFile selectedFile = (IFile) selectedObj;
			final String selFile = selectedFile.getName();

			int extIndex = selFile.lastIndexOf(".");
			/* Get the file extension! */
			String extension = selFile.substring(extIndex + 1);
			/* The file extension for the output! */
			String fileext2 = extension.replace("R", "");
			final String fileext = fileext2.replace("r", "");

			final String theName = selFile.replaceFirst("[.][^.]+$", "");
			nameofiofile = getFileName(selFile);
			project = selectedFile.getLocation().toString();
			project = project.replace("\\", "/");
			fi = selectedFile.getRawLocation().toString();
			name = nameofiofile;
			// dirPath = null;

			dirPath = new File(fi).getParentFile().getPath().replace("\\", "/");

			


			Job job = new Job("Knitr file") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (extension.equals("tex")) {
						monitor.beginTask("LaTeX file...", IProgressMonitor.UNKNOWN);

						compileLatex(activeProject, theName, dirPath, true);
					}

					else if (extension.equals("rnw") || extension.equals("Rnw")) {

						if (RServe.isAliveDialog()) {
							RConnection c = RServe.getConnection();

							try {
								c.eval("try(setwd('" + dirPath + "'));");
								c.eval("try(Sweave(\"" + project + "\"))");
								// c.eval("try(dev.off());");//not needed with Rserve?

							} catch (RserveException e) {

								System.out.println(e.getMessage());
							}
						}

						compileLatex(activeProject, theName, dirPath, false);

					}

					else {
						monitor.beginTask("LaTeX file...", IProgressMonitor.UNKNOWN);
						if (RServe.isAliveDialog()) {
							if (RState.isBusy() == false) {

								RConnection c = RServe.getConnection();

								try {
									REXPLogical rl = (REXPLogical) c.eval("require(knitr)");
									if (!(rl.isTRUE()[0])) {

										Bio7Dialog.message("Cannot load 'knitr' package!");
									}

									c.eval("try(library(knitr))");
									c.eval("setwd('" + dirPath + "')");
									IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
									String knitrOptions = store.getString("knitroptions");
									if (fileext.equals("html")) {
										c.eval("try(" + knitrOptions + ")");
										// File file =
										// selectedFile.getLocation().toFile();
										String docTemp = BatchModel.fileToString(selectedFile.getLocation().toString());

										// String docTemp=doc.get();
										Document docHtml = Jsoup.parse(docTemp);
										/* Search for divs with the selected id! */
										Elements contents = docHtml.select("#knitrcode"); // a
																							// with
																							// href
										for (int i = 0; i < contents.size(); i++) {
											/*
											 * Replace in the div the linebreak and page tags with text linebreak(s)!
											 */
											contents.get(i).select("br").append("\\n");
											contents.get(i).select("p").prepend("\\n\\n");

											String cleaned = contents.get(i).text().replaceAll("\\\\n", "\n");
											/*
											 * Wrap the parsed div text in a knitr section!
											 */
											contents.get(i).after("<!--begin.rcode\n " + cleaned + " \nend.rcode-->");
											contents.get(i).remove();
										}
										/*
										 * Create a temp file for the parsed and edited *.html file for processing with knitr!
										 */
										File temp = null;
										try {
											temp = File.createTempFile(theName, ".tmp");
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}

										/*
										 * Write the changes to the file with the help of the ApacheIO lib!
										 */
										try {
											FileUtils.writeStringToFile(temp, docHtml.html());
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										/* Clean the path for R and knitr! */
										String cleanedPath = temp.getPath().replace("\\", "/");

										RServe.print("try(knit('" + cleanedPath + "','" + theName + "." + fileext + "'))");

									}

									else if (fileext.equals("tex")) {

										RServe.print("try(knit('" + selFile + "','" + theName + "." + fileext + "'))");
									}

								} catch (RserveException e1) {

									e1.printStackTrace();
								}

								IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
								IProject proj = root.getProject(activeProject.getName());
								try {
									proj.refreshLocal(IResource.DEPTH_INFINITE, null);
								} catch (CoreException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								if (fileext.equals("html")) {

									Display display = PlatformUI.getWorkbench().getDisplay();
									display.asyncExec(new Runnable() {

										public void run() {

											IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
											String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");

											String temp = "file:///" + dirPath + "/" + theName + ".html";
											String url = temp.replace("\\", "/");
											// System.out.println(url);
											if (openInJavaFXBrowser.equals("JAVAFX_BROWSER") == false) {
												Work.openView("com.eco.bio7.browser.Browser");
												BrowserView b = BrowserView.getBrowserInstance();
												b.browser.setJavascriptEnabled(true);
												b.setLocation(url);
											} else {
												boolean openInBrowserInExtraView = store.getBoolean("OPEN_BOWSER_IN_EXTRA_VIEW");
												if (openInBrowserInExtraView) {
													new JavaFXWebBrowser(true).createBrowser(url, theName + ".html");
												} else {
													new JavaFXWebBrowser(true).createBrowser(url, "R_Display");
												}

											}
										}
									});

								} else if (fileext.equals("tex")) {
									compileLatex(activeProject, theName, dirPath, true);

								}
							}

						}
					}
					monitor.done();
					return Status.OK_STATUS;
				}

			};
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

					} else {

					}
				}
			});
			// job.setSystem(true);
			job.schedule();

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

	private void compileLatex(final IProject activeProject, final String theName, String dirPath, boolean pureLatex) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String pdfLatexPath = store.getString("pdfLatex");
		boolean useBrowser = store.getBoolean("PDF_USE_BROWSER");
		String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");
		String latexEngine = store.getString("LATEX_ENGINE");
		String sweaveScriptLocation = store.getString("SweaveScriptLocation");
		boolean cleanFiles = store.getBoolean("LATEX_CLEAN_FILES");
		String fileTypes = store.getString("LATEX_FILES_EXT_DELETE");

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
			/*
			 * Eventually take care of whitespaces in path!
			 */

			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				args.add("\"" + pdfLatexPath + "/" + latexEngine + "\"");
			}

			else {
				args.add(pdfLatexPath + "/" + latexEngine);
			}
		}
		/* Try to start from the PATH environment! */
		else {

			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				args.add(latexEngine);
			}

			else {
				args.add(latexEngine);
			}

		}
		if (pureLatex) {
			args.add("-interaction=nonstopmode");
			args.add("-output-directory=" + dirPath);
			args.add(dirPath + "/" + theName + ".tex");
		} else {
			args.add("-interaction=nonstopmode");
			args.add("-include-directory=" + sweaveScriptLocation);
			args.add("-output-directory=" + dirPath);
			args.add(dirPath + "/" + theName + ".tex");
		}

		Process proc = null;
		ProcessBuilder pb = new ProcessBuilder(args);
		// set environment variable u
		/*
		 * String otexinputs =env.get("TEXINPUTS"); env.put("TEXINPUTS", otexinputs+"/"+dirPath);
		 */

		/*
		 * Set the working directory for the process from Java!
		 */
		pb.directory(new File(dirPath));

		pb.redirectErrorStream();
		try {
			proc = pb.start();

		} catch (IOException e) {

			e.printStackTrace();
			/*
			 * Bio7Dialog.message( "Rserve executable not available !" ); RServe.setConnection(null);
			 */
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

						/*
						 * if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) { RServe.plotLinux(dirPath + "/" + theName + ".pdf"); }
						 */

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
				if (cleanFiles) {
					deleteAuxiliaryFiles(dirPath, fileTypes);
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

	private void deleteAuxiliaryFiles(String dirPath, String fileTypes) {

		File dir = new File(dirPath);

		String[] extensions = fileTypes.split(",");
		// System.out.println("Getting all .txt and .jsp files in " + dir.getCanonicalPath()
		// + " including those in subdirectories");

		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		for (File file : files) {

			if (getFileName(file.getName()).equals(name)) {
				file.delete();
			}

		}
	}

}
