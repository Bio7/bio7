package com.eco.bio7.popup.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
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
import com.eco.bio7.collection.Work;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.StartBio7Utils;

public class RMarkdownAction extends Action implements IObjectActionDelegate {

	private BufferedReader input;
	private OutputStream stdin;
	private String fi;
	private String name;
	private String docType;

	public RMarkdownAction() {
		super();
		setId("com.eco.bio7.rmarkdown");
		setActionDefinitionId("com.eco.bio7.RMarkdownAction");
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			/*Bring the console to the front and clear it!*/
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

				markdownFile(selectedObj, activeProject);

			}
			
		}

	}
	public void run() {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			/*Bring the console to the front and clear it!*/
			utils.cons.activate();
			utils.cons.clear();
		}
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor.isDirty()) {
			editor.doSave(new NullProgressMonitor());
		}

		
		
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
		}
		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		/*Extract the header information for the doctype and open a registered viewer in Bio7!*/
		String title = StringUtils.substringBetween(doc.get(), "---", "---");
		String sub=title.substring(title.lastIndexOf("output:") + 7);
		System.out.println(":::::::"+sub);
		if (sub.contains("html_document")||sub.contains("ioslides_presentation")||sub.contains("slidy_presentation")) {
			
			docType="Html";
			
		}
		else if(sub.contains("pdf_document")||sub.contains("beamer_presentation")){
			docType="Pdf";
		}
		
		else if(sub.contains("word_document")){
			docType="Word";
		}
		else{
			docType="";
		}
		//System.out.println("title:" + title); // good
		
			markdownFile(aFile, aFile.getProject());
	}

	private void markdownFile(Object selectedObj, final IProject activeProject) {
		String project;
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
			//dirPath = null;

			String dirPath = new File(fi).getParentFile().getPath().replace("\\", "/");

			

			System.out.println(dirPath);

			Job job = new Job("Markdown file") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Markdown file...", IProgressMonitor.UNKNOWN);

					if (RServe.isAliveDialog()) {
						if (RState.isBusy() == false) {

							RConnection c = RServe.getConnection();

							try {
								REXPLogical rl = (REXPLogical) c.eval("require(rmarkdown)");
								if (!(rl.isTRUE()[0])) {

									Bio7Dialog.message("Cannot load 'markdown' package!");
								}

								c.eval("try(library(rmarkdown))");
								c.eval("setwd('" + dirPath + "')");
								IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
								//String knitrOptions = store.getString("knitroptions");
								
								
								 
									System.out.println(selFile);
									RServe.print("try(render(\"" + selFile +"\"))");
								
								
		                         
								
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
							if (docType.equals("Html")) {

								Work.openView("com.eco.bio7.browser.Browser");
								Display display = PlatformUI.getWorkbench().getDisplay();
								display.asyncExec(new Runnable() {

									public void run() {
										String temp = "file:///" + dirPath + "/" + theName + ".html";
										String url = temp.replace("\\", "/");
										System.out.println(url);
										BrowserView b = BrowserView.getBrowserInstance();
										b.setLocation(url);
									}
								});

							} 
								
								else if (docType.equals("Pdf")) {
								/*IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
								String pdfLatexPath = store.getString("pdfLatex");

								pdfLatexPath = pdfLatexPath.replace("\\", "/");

								// String temp=dirPath+"/" +
								// theName+".tex";
								// String url = temp.replace("\\", "/");

								// Process proc =
								// Runtime.getRuntime().exec(
								// pdfLatexPath+"/pdflatex -interaction=nonstopmode "
								// + "-output-directory=" + dirPath +
								// " " + dirPath + "/" + theName +
								// ".tex");
								List<String> args = new ArrayList<String>();
								args.add(pdfLatexPath + "/pdflatex");
								args.add("-interaction=nonstopmode");
								args.add("-output-directory=" + dirPath);
								args.add(dirPath + "/" + theName + ".tex");

								Process proc = null;
								ProcessBuilder pb = new ProcessBuilder(args);
								pb.redirectErrorStream();
								try {
									proc = pb.start();

								} catch (IOException e) {
									
									e.printStackTrace();
									
									 * Bio7Dialog.message(
									 * "Rserve executable not available !"
									 * ); RServe.setConnection(null);
									 
								}

								input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
								stdin = proc.getOutputStream();*/

								new Thread() {

									public void run() {
										setPriority(Thread.MAX_PRIORITY);
										String line;
										
											File fil = new File(dirPath + "/" + theName + ".pdf");
											if (fil.exists()) {
												Program.launch(dirPath + "/" + theName + ".pdf");
											} else {
												Bio7Dialog.message("*.pdf file was not created.\nPlease check the error messages!\nProbably an empty space in the file path caused the error!");
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
								else if (docType.equals("Word")) {
									

									new Thread() {

										public void run() {
											setPriority(Thread.MAX_PRIORITY);
											String line;
										
												File fil = new File(dirPath + "/" + theName + ".docx");
												if (fil.exists()) {
													Program.launch(dirPath + "/" + theName + ".docx");
												} else {
													Bio7Dialog.message("*.docx file was not created.\nPlease check the error messages!\nProbably an empty space in the file path caused the error!");
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

}
