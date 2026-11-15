package com.eco.bio7.documents;

import java.io.File;
import java.io.IOException;

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
import com.eco.bio7.rbridge.backend.RBackend;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.BrowserUtil;
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.markdownedit.editors.MarkdownEditor;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;

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
		/* A markdown file (*.md) is converted without R */
		ISelection sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (sel instanceof IStructuredSelection ss && ss.size() == 1 && ss.getFirstElement() instanceof IFile f && "md".equalsIgnoreCase(f.getFileExtension())) {
			markdownWithoutR(f);
			return;
		}

		if (RServe.isAliveDialog() == false) {
			return;
		}

		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			/* Bring the console to the front and clear it! */
			utils.cons.activate();
			utils.cons.clear();
		}
		// String project = null;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
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
		editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor == null || editor instanceof MarkdownEditor == false) {
			return;
		}
		/* A markdown file (*.md) is converted without R */
		if (editor.getEditorInput() instanceof IFileEditorInput in && "md".equalsIgnoreCase(in.getFile().getFileExtension())) {
			if (editor.isDirty()) {
				editor.doSave(new NullProgressMonitor());
			}
			markdownWithoutR(in.getFile());
			return;
		}

		if (RServe.isAliveDialog() == false) {
			return;
		}

		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			/* Bring the console to the front and clear it! */
			utils.cons.activate();
			utils.cons.clear();
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
		 * Extract the header information for the doctype and open a registered viewer
		 * in Bio7!
		 */
		String ext = aFile.getFileExtension();
		if (ext.equals("md")) {
			docType = "Html";
			/* Can execute when the last job has been finished! */
			if (canOperate) {
				markdownFile(aFile, aFile.getProject());
			}
		} else {
			String title = StringUtils.substringBetween(doc.get(), "---", "---");
			String sub = title.substring(title.lastIndexOf("output:") + 7);

			if (sub.contains("html_document") || sub.contains("ioslides_presentation")
					|| sub.contains("slidy_presentation")) {

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
		}
		// editor.getEditorSite().getPart().setFocus();
		// editor.setFocus();

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

			if (RState.isBusy() == false) {
				RState.setBusy(true);

				Job job = new Job("Markdown file") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Markdown file...", IProgressMonitor.UNKNOWN);
						canOperate = false;

						RBackend c = RBridge.getBackend();

						try {
							REXPLogical rl = (REXPLogical) c.eval("require(rmarkdown)");
							if (!(rl.isTRUE()[0])) {

								Bio7Dialog.message("Cannot load 'markdown' package!");
							}

							c.eval("try(library(rmarkdown))");
							/* pandoc (and LaTeX) for rmarkdown also without the PATH of a shell */
							c.voidEval(Pandoc.rSetup());
							c.eval("try(.tempCurrentWd<-getwd());");
							c.eval("setwd('" + dirPath + "')");

							System.out.println(selFile);
							RServe.print("render(\"" + selFile + "\")");

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
									IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

									/* The option for using an external Browser! */
									boolean useInternalSWTBrowser = store.getBoolean("PDF_USE_BROWSER");

									if (useInternalSWTBrowser == true) {
										//Work.openView("com.eco.bio7.browser.Browser");
										BrowserUtil.setLocationWithJS(url);
									} else {

										Program.launch(url);

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

										RServe.openPDF(dirPath + "/", theName + ".pdf", useBrowser, openInJavaFXBrowser,
												true, false);

										// Program.launch(dirPath + "/" + theName + ".pdf");
									} else {
										Bio7Dialog.message(
												"*.pdf file was not created.\nPlease check the error messages!\nProbably an empty space in the file path caused the error!");
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
											Bio7Dialog.message(
													"*.docx file was not created.\nPlease check the error messages!\nProbably an empty space in the file path caused the error!");
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
													// new WordOleView(dirPath + "/" + theName + ".docx");
												}
											}
										}
									}
								});
							}

						}
						try {
							c.eval("try(setwd(.tempCurrentWd));");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						monitor.done();
						return Status.OK_STATUS;
					}

				};
				job.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							/*
							 * Set the flag that a new compilation is possible after the last job has been
							 * finished!
							 */
							RState.setBusy(false);
							canOperate = true;
							RServeUtil.listRObjects();

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

	/*
	 * A markdown file (*.md): converted to HTML in Java (no R needed), written next
	 * to the file and opened like the HTML of R Markdown
	 */
	private void markdownWithoutR(IFile file) {
		if (file.getLocation() == null) {
			return;
		}
		java.nio.file.Path md = file.getLocation().toFile().toPath();
		Job job = new Job("Markdown") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					/* The format of the YAML header (output:), default HTML */
					String ext = Pandoc.extension(outputFormat(java.nio.file.Files.readString(md, java.nio.charset.StandardCharsets.UTF_8)));
					java.nio.file.Path result = ext.equals("html") ? com.eco.bio7.markdownedit.MarkdownToHtml.convert(md) : Pandoc.convert(md, ext);
					try {
						file.getParent().refreshLocal(IResource.DEPTH_ONE, null);
					} catch (CoreException e) {
						/* The file exists anyway */
					}
					Display.getDefault().asyncExec(() -> openResult(result, ext));
				} catch (IOException | RuntimeException e) {
					Display.getDefault().asyncExec(() -> Bio7Dialog.message("The markdown file could not be converted:\n" + e.getMessage()));
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/* The output format of the YAML header of a markdown file (e.g., pdf_document) or "html" */
	static String outputFormat(String markdown) {
		java.util.regex.Matcher y = java.util.regex.Pattern.compile("\\A---\\s*\\R(.*?)\\R(?:---|\\.\\.\\.)", java.util.regex.Pattern.DOTALL).matcher(markdown);
		if (y.find()) {
			java.util.regex.Matcher o = java.util.regex.Pattern.compile("(?m)^output:\\s*[\"']?([\\w.-]+)").matcher(y.group(1));
			if (o.find()) {
				return o.group(1).toLowerCase();
			}
		}
		return "html";
	}

	/* Opens a converted file: HTML and PDF in Bio7 (see the preferences), the others with the system */
	private static void openResult(java.nio.file.Path result, String ext) {
		switch (ext) {
		case "html" -> openHtml(result.toString());
		case "pdf" -> {
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			RServe.openPDF(result.getParent().toString().replace("\\", "/") + "/", result.getFileName().toString(), store.getBoolean("PDF_USE_BROWSER"),
					store.getString("BROWSER_SELECTION"), false, false);
		}
		default -> Program.launch(result.toString());
		}
	}

	/* Opens an HTML file in the Bio7 browser (or the system browser, see the preferences) */
	private static void openHtml(String path) {
		String url = ("file:///" + path).replace("\\", "/").replace("file:////", "file:///");
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		if (store.getBoolean("PDF_USE_BROWSER")) {
			BrowserUtil.setLocationWithJS(url);
		} else {
			Program.launch(url);
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
