package com.eco.bio7.documents;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
import org.eclipse.swt.program.Program;
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
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;
import net.sourceforge.texlipse.editor.TexEditor;

public class LatexSweaveKnitrAction extends Action {

	private BufferedReader input;
	private OutputStream stdin;
	private String fi;
	private String name;
	private String project;
	private String dirPath;
	private IEditorPart editor;

	public LatexSweaveKnitrAction() {
		super();
		setId("com.eco.bio7.browser.knitr");
		setActionDefinitionId("com.eco.bio7.browser.knitrAction");
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/*
	 * public void run(IAction action) { StartBio7Utils utils =
	 * StartBio7Utils.getConsoleInstance(); if (utils != null) { Bring the console
	 * to the front and clear it! utils.cons.activate(); utils.cons.clear(); }
	 * String project = null; ISelection selection =
	 * PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().
	 * getSelection(); IStructuredSelection strucSelection = null; if (selection
	 * instanceof IStructuredSelection) { strucSelection = (IStructuredSelection)
	 * selection; if (strucSelection.size() == 0) {
	 * 
	 * } else if (strucSelection.size() == 1) { final String nameofiofile; Object
	 * selectedObj = strucSelection.getFirstElement();
	 * 
	 * IResource resource = (IResource) strucSelection.getFirstElement(); final
	 * IProject activeProject = resource.getProject();
	 * 
	 * knitrFile(selectedObj, activeProject);
	 * 
	 * }
	 * 
	 * }
	 * 
	 * }
	 */

	public void run() {

		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			/* Bring the console to the front and clear it! */
			utils.cons.activate();
			utils.cons.clear();
		}
		editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

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

			if (extension.equals("tex") == false) {

				if (RServe.isAliveDialog() == false) {
					return;
				}

			}
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
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Job job = new Job("Knitr file") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						if (extension.equals("tex")) {
							monitor.beginTask("LaTeX file...", IProgressMonitor.UNKNOWN);

							compileLatex(activeProject, theName, dirPath, true);

						}

						else if (extension.equals("rnw") || extension.equals("Rnw")) {

							RConnection c = RServe.getConnection();

							try {
								c.eval("try(.tempCurrentWd<-getwd());");
								c.eval("try(setwd('" + dirPath + "'));");
								c.eval("try(Sweave(\"" + project + "\"))");
								c.eval("try(setwd(.tempCurrentWd));");
								// c.eval("try(dev.off());");//not needed with Rserve?

							} catch (RserveException e) {

								System.out.println(e.getMessage());
							}

							compileLatex(activeProject, theName, dirPath, false);

						}

						else {
							monitor.beginTask("LaTeX file...", IProgressMonitor.UNKNOWN);

							RConnection c = RServe.getConnection();

							try {
								REXPLogical rl = (REXPLogical) c.eval("require(knitr)");
								if (!(rl.isTRUE()[0])) {

									Bio7Dialog.message("Cannot load 'knitr' package!");
								}

								c.eval("try(library(knitr))");
								c.eval("try(.tempCurrentWd<-getwd());");
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
									/* Remove the content editable attribute! */
									Elements elements = docHtml.select("body");
									elements.removeAttr("contenteditable");
									// System.out.println(docHtml.getElementsByTag("body"));
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
									 * Create a temp file for the parsed and edited *.html file for processing with
									 * knitr!
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

									RServe.print("knit('" + cleanedPath + "','" + theName + "." + fileext + "')");

								}

								else if (fileext.equals("tex")) {

									RServe.print("knit('" + selFile + "','" + theName + "." + fileext + "')");
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

										String temp = "file:///" + dirPath + "/" + theName + ".html";
										String url = temp.replace("\\", "/");
										/* The option for using an external Browser! */
										boolean useInternalSWTBrowser = store.getBoolean("PDF_USE_BROWSER");

										if (useInternalSWTBrowser == true) {
											Work.openView("com.eco.bio7.browser.Browser");
											BrowserView b = BrowserView.getBrowserInstance();
											b.browser.setJavascriptEnabled(true);
											b.setLocation(url);
										} else {

											Program.launch(url);

										}

									}
								});

							} else if (fileext.equals("tex")) {
								compileLatex(activeProject, theName, dirPath, true);

							}
							try {
								c.eval("try(setwd(.tempCurrentWd));");
							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
						monitor.done();
						return Status.OK_STATUS;
					}

				};
				job.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							RState.setBusy(false);
							/* Activate the editor again after the job! */
							// Util.activateEditorPage(editor);
							/* Only list objects when using R with latex! */
							if (extension.equals("tex") == false) {
								RServeUtil.listRObjects();
							}
						} else {
							RState.setBusy(false);
						}
					}
				});
				// job.setSystem(true);
				job.schedule();
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

	private void compileLatex(final IProject activeProject, final String theName, String dirPath, boolean pureLatex) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean includeBibTex = store.getBoolean("INCLUDE_BIBTEX");
		boolean includeMakeIndex = store.getBoolean("INCLUDE_BIBTEX");
		String bibtexEngine = store.getString("BIBTEX_ENGINE");
		/* With bibtex (optional biber) and make index! */
		if (includeBibTex && includeMakeIndex) {
			compileLatexWithBibtexAndIndex(activeProject, theName, dirPath, pureLatex, bibtexEngine);
		}
		/* With bibtex (optional biber) only! */
		else if (includeBibTex) {
			compileLatexWithBibtex(activeProject, theName, dirPath, pureLatex, bibtexEngine);
		}
		/* With makeindex only! */
		else if (includeMakeIndex) {
			compileLatexWithMakeIndex(activeProject, theName, dirPath, pureLatex);
		}
		/* LaTeX file only */
		else {
			compileLatexFinal(activeProject, theName, dirPath, pureLatex);
		}
	}

	private void compileLatexWithBibtex(final IProject activeProject, final String theName, String dirPath,
			boolean pureLatex, String bibtexEngine) {
		compileLatexPre(activeProject, theName, dirPath, pureLatex);

		compileBibtex(theName, dirPath, bibtexEngine);

		compileLatexPre(activeProject, theName, dirPath, pureLatex);

		compileLatexFinal(activeProject, theName, dirPath, pureLatex);
	}

	private void compileLatexWithMakeIndex(final IProject activeProject, final String theName, String dirPath,
			boolean pureLatex) {
		compileLatexPre(activeProject, theName, dirPath, pureLatex);

		compileMakeIndex(theName, dirPath);

		// compileLatexPre(activeProject, theName, dirPath, true);

		compileLatexFinal(activeProject, theName, dirPath, pureLatex);
	}

	private void compileLatexWithBibtexAndIndex(final IProject activeProject, final String theName, String dirPath,
			boolean pureLatex, String bibtexEngine) {

		compileLatexPre(activeProject, theName, dirPath, pureLatex);
		compileBibtex(theName, dirPath, bibtexEngine);

		compileLatexPre(activeProject, theName, dirPath, pureLatex);
		compileLatexPre(activeProject, theName, dirPath, pureLatex);
		compileMakeIndex(theName, dirPath);

		compileLatexFinal(activeProject, theName, dirPath, pureLatex);
	}

	private void compileLatexFinal(final IProject activeProject, final String theName, String dirPath,
			boolean pureLatex) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String pdfLatexPath = store.getString("pdfLatex");
		boolean useBrowser = store.getBoolean("PDF_USE_BROWSER");
		String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");
		String latexEngine = store.getString("LATEX_ENGINE");
		String sweaveScriptLocation = store.getString("SweaveScriptLocation");
		boolean cleanFiles = store.getBoolean("LATEX_CLEAN_FILES");
		String fileTypes = store.getString("LATEX_FILES_EXT_DELETE");
		String commandLineFlags = store.getString("LATEX_COMMANDLINE_OTIONS");

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
			 * Probably take care of whitespaces in path!
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
			if (commandLineFlags.isEmpty() == false) {
				String flags[] = commandLineFlags.split(" ");
				for (int i = 0; i < flags.length; i++) {
					args.add(flags[i]);
				}
			}
			args.add("-output-directory=" + dirPath);
			args.add(dirPath + "/" + theName + ".tex");
		} else {
			args.add("-interaction=nonstopmode");
			if (commandLineFlags.isEmpty() == false) {
				String flags[] = commandLineFlags.split(" ");
				for (int i = 0; i < flags.length; i++) {
					args.add(flags[i]);
				}

			}
			args.add("-include-directory=" + sweaveScriptLocation);
			args.add("-output-directory=" + dirPath);
			args.add(dirPath + "/" + theName + ".tex");
		}

		Process proc = null;
		ProcessBuilder pb = new ProcessBuilder(args);
		// set environment variable u
		/*
		 * String otexinputs =env.get("TEXINPUTS"); env.put("TEXINPUTS",
		 * otexinputs+"/"+dirPath);
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
			 * Bio7Dialog.message( "Rserve executable not available !" );
			 * RServe.setConnection(null);
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
						 * if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
						 * RServe.plotLinux(dirPath + "/" + theName + ".pdf"); }
						 */

						// else {

						// Program.launch(dirPath + "/" + theName + ".pdf");
						RServe.openPDF(dirPath + "/", theName + ".pdf", useBrowser, openInJavaFXBrowser, true, false);
						// }
					} else {
						Bio7Dialog.message(
								"*.pdf file was not created.\nPlease check the error messages!\nProbably an empty space in the file path caused the error!");
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

	/* We don't need a special path for bibtex. Should be the same as pdflatex! */
	private void compileMakeIndex(final String theName, String dirPath) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String pdfLatexPath = store.getString("pdfLatex");

		String latexEngine = "makeindex";

		List<String> args = new ArrayList<String>();

		if (pdfLatexPath.isEmpty() == false) {

			pdfLatexPath = pdfLatexPath.replace("\\", "/");

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

		args.add(dirPath + "/" + theName);

		ProcessBuilder pb = new ProcessBuilder(args);
		// set environment variable u
		/*
		 * String otexinputs =env.get("TEXINPUTS"); env.put("TEXINPUTS",
		 * otexinputs+"/"+dirPath);
		 */

		/*
		 * Set the working directory for the process from Java!
		 */
		pb.directory(new File(dirPath));

		pb.redirectErrorStream();

		Process p = null;
		try {
			p = pb.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			p.waitFor(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		stdin = p.getOutputStream();

		new Thread() {

			public void run() {
				setPriority(Thread.MAX_PRIORITY);
				String line;
				try {

					while ((line = input.readLine()) != null) {
						System.out.println(line);
					}

				} catch (IOException e) {

					e.printStackTrace();
				}

			}

		}.start();

	}

	/* We don't need a special path for bibtex. Should be the same as pdflatex! */
	private void compileBibtex(final String theName, String dirPath, String bibtexEngine) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String pdfLatexPath = store.getString("pdfLatex");

		String latexEngine = bibtexEngine;

		List<String> args = new ArrayList<String>();

		if (pdfLatexPath.isEmpty() == false) {

			pdfLatexPath = pdfLatexPath.replace("\\", "/");

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

		args.add(dirPath + "/" + theName);

		ProcessBuilder pb = new ProcessBuilder(args);
		// set environment variable u
		/*
		 * String otexinputs =env.get("TEXINPUTS"); env.put("TEXINPUTS",
		 * otexinputs+"/"+dirPath);
		 */

		/*
		 * Set the working directory for the process from Java!
		 */
		pb.directory(new File(dirPath));

		pb.redirectErrorStream();

		Process p = null;
		try {
			p = pb.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			p.waitFor(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		stdin = p.getOutputStream();

		new Thread() {

			public void run() {
				setPriority(Thread.MAX_PRIORITY);
				String line;
				try {

					while ((line = input.readLine()) != null) {
						System.out.println(line);
					}

				} catch (IOException e) {

					e.printStackTrace();
				}

			}

		}.start();

	}

	private void compileLatexPre(final IProject activeProject, final String theName, String dirPath,
			boolean pureLatex) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String pdfLatexPath = store.getString("pdfLatex");

		String latexEngine = store.getString("LATEX_ENGINE");
		String sweaveScriptLocation = store.getString("SweaveScriptLocation");

		String commandLineFlags = store.getString("LATEX_COMMANDLINE_OTIONS");

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
			 * Probably take care of whitespaces in path!
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
			if (commandLineFlags.isEmpty() == false) {
				String flags[] = commandLineFlags.split(" ");
				for (int i = 0; i < flags.length; i++) {
					args.add(flags[i]);
				}
			}
			args.add("-output-directory=" + dirPath);
			args.add(dirPath + "/" + theName + ".tex");
		} else {
			args.add("-interaction=nonstopmode");
			if (commandLineFlags.isEmpty() == false) {
				String flags[] = commandLineFlags.split(" ");
				for (int i = 0; i < flags.length; i++) {
					args.add(flags[i]);
				}

			}
			args.add("-include-directory=" + sweaveScriptLocation);
			args.add("-output-directory=" + dirPath);
			args.add(dirPath + "/" + theName + ".tex");
		}

		ProcessBuilder pb = new ProcessBuilder(args);
		// set environment variable u
		/*
		 * String otexinputs =env.get("TEXINPUTS"); env.put("TEXINPUTS",
		 * otexinputs+"/"+dirPath);
		 */

		/*
		 * Set the working directory for the process from Java!
		 */
		pb.directory(new File(dirPath));

		pb.redirectErrorStream();
		Process p = null;

		try {
			p = pb.start();

		} catch (IOException e) {

			e.printStackTrace();
			/*
			 * Bio7Dialog.message( "Rserve executable not available !" );
			 * RServe.setConnection(null);
			 */
		}
		try {
			p.waitFor(5, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		/*
		 * input = new BufferedReader(new InputStreamReader(p.getInputStream())); stdin
		 * = p.getOutputStream(); new Thread() {
		 * 
		 * public void run() { setPriority(Thread.MAX_PRIORITY); String line; try {
		 * 
		 * while ((line = input.readLine()) != null) { System.out.println(line); }
		 * 
		 * } catch (IOException e) {
		 * 
		 * e.printStackTrace(); }
		 * 
		 * }
		 * 
		 * }.start();
		 */

	}

	private void deleteAuxiliaryFiles(String dirPath, String fileTypes) {

		File dir = new File(dirPath);

		String[] extensions = fileTypes.split(",");

		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		for (File file : files) {

			if (getFileName(file.getName()).equals(name)) {
				file.delete();
			}

		}
	}

}
