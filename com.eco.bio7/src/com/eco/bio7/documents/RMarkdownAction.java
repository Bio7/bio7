package com.eco.bio7.documents;

import java.io.File;
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
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.browser.MultiPageEditor;
import com.eco.bio7.collection.Work;
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.markdownedit.editors.MarkdownEditor;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.util.Util;

public class RMarkdownAction extends Action implements IObjectActionDelegate {

	// private BufferedReader input;
	// private OutputStream stdin;
	private String fi;
	// private String name;
	private String docType;
	protected boolean canOperate = true;
	private IEditorPart editor;

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
			/* Bring the console to the front and clear it! */
			utils.cons.activate();
			utils.cons.clear();
		}
		// String project = null;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;
			if (strucSelection.size() == 0) {

			} else if (strucSelection.size() == 1) {
				// final String nameofiofile;
				Object selectedObj = strucSelection.getFirstElement();

				IResource resource = (IResource) strucSelection.getFirstElement();
				final IProject activeProject = resource.getProject();
				if (canOperate) {
					markdownFile(selectedObj, activeProject);
				}

			}

		}

	}

	public void run() {

		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			/* Bring the console to the front and clear it! */
			utils.cons.activate();
			utils.cons.clear();
		}
		editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor == null || editor instanceof MarkdownEditor == false) {
			return;
		}
		if (editor.isDirty()) {
			editor.doSave(new NullProgressMonitor());
		}

		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
		}
		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		/*
		 * Extract the header information for the doctype and open a registered viewer in Bio7!
		 */
		String title = StringUtils.substringBetween(doc.get(), "---", "---");
		String sub = title.substring(title.lastIndexOf("output:") + 7);

		if (sub.contains("html_document") || sub.contains("ioslides_presentation") || sub.contains("slidy_presentation")) {

			docType = "Html";

		} else if (sub.contains("pdf_document") || sub.contains("beamer_presentation")) {
			docType = "Pdf";
		}

		else if (sub.contains("word_document")) {
			docType = "Word";
		} else {
			docType = "";
		}
		/* Can execute when the last job has been finished! */
		if (canOperate) {
			markdownFile(aFile, aFile.getProject());
		}
		//editor.getEditorSite().getPart().setFocus();
		//editor.setFocus();
		
	}

	private void markdownFile(Object selectedObj, final IProject activeProject) {
		String project;

		if (selectedObj instanceof IFile) {
			IFile selectedFile = (IFile) selectedObj;
			final String selFile = selectedFile.getName();

			final String theName = selFile.replaceFirst("[.][^.]+$", "");

			project = selectedFile.getLocation().toString();
			project = project.replace("\\", "/");
			fi = selectedFile.getRawLocation().toString();

			String dirPath = new File(fi).getParentFile().getPath().replace("\\", "/");

			System.out.println(dirPath);

			Job job = new Job("Markdown file") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Markdown file...", IProgressMonitor.UNKNOWN);
					canOperate = false;

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

								System.out.println(selFile);
								RServe.print("try(render(\"" + selFile + "\"))");

							} catch (RserveException e1) {

								e1.printStackTrace();
							}

							IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
							IProject proj = root.getProject(activeProject.getName());
							try {
								proj.refreshLocal(IResource.DEPTH_INFINITE, null);
							} catch (CoreException e) {

								e.printStackTrace();
							}
							if (docType.equals("Html")) {
								// boolean dec = Bio7Dialog.decision("Open JavaFX browser?");
								// if (dec == false) {

								// }
								Display display = PlatformUI.getWorkbench().getDisplay();
								display.asyncExec(new Runnable() {

									public void run() {
										String temp = "file:///" + dirPath + "/" + theName + ".html";
										String url = temp.replace("\\", "/");
										System.out.println(url);

										IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
										String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");

										if (openInJavaFXBrowser.equals("JAVAFX_BROWSER") == false) {
											Work.openView("com.eco.bio7.browser.Browser");
											BrowserView b = BrowserView.getBrowserInstance();
											b.browser.setJavascriptEnabled(true);
											b.setLocation(url);
										}

										else {

											boolean openInBrowserInExtraView = store.getBoolean("OPEN_BOWSER_IN_EXTRA_VIEW");
											if (openInBrowserInExtraView) {

												new JavaFXWebBrowser(true).createBrowser(url, theName + ".html");
											} else {
												new JavaFXWebBrowser(true).createBrowser(url, "Display");
											}
										}
									}
								});

							}

							else if (docType.equals("Pdf")) {

								IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
								boolean useBrowser = store.getBoolean("PDF_USE_BROWSER");
								String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");

								Display display = PlatformUI.getWorkbench().getDisplay();
								display.asyncExec(new Runnable() {

									public void run() {
										setPriority(Thread.MAX_PRIORITY);

										File fil = new File(dirPath + "/" + theName + ".pdf");
										if (fil.exists()) {

											RServe.openPDF(dirPath + "/", theName + ".pdf", useBrowser, openInJavaFXBrowser);

											// Program.launch(dirPath + "/" + theName + ".pdf");
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
								});

							} else if (docType.equals("Word")) {
								IPreferenceStore store = Activator.getDefault().getPreferenceStore();
								boolean openWordInView = store.getBoolean("OPEN_WORD_IN_VIEW");
								if (openWordInView == false) {
									Display display = PlatformUI.getWorkbench().getDisplay();
									display.asyncExec(new Runnable() {

										public void run() {
											setPriority(Thread.MAX_PRIORITY);
											// String line;

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
									});
									/* Open Word in view if possible! */
								} else if (openWordInView) {
									Display display = PlatformUI.getWorkbench().getDisplay();
									display.asyncExec(new Runnable() {

										public void run() {
											if (docType.equals("Word")) {
												if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
													File fil = new File(dirPath + "/" + theName + ".docx");
													if (fil.exists()) {
														new WordOleView(dirPath + "/" + theName + ".docx");
													}
												}
											}
										}
									});
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
						/* Set the flag that a new compilation is possible after the last job has been finished! */
						canOperate = true;
						
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
