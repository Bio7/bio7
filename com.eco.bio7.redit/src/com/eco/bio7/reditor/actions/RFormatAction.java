package com.eco.bio7.reditor.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorActionDelegate;
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
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;

public class RFormatAction implements IObjectActionDelegate, IEditorActionDelegate {

	private IProject iproj;

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		action.setActionDefinitionId("com.eco.bio7.reditor.rserve.format");
		if (targetEditor != null)
			targetEditor.getSite().getKeyBindingService().registerAction(action);
	}

	public void run(IAction action) {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		IEditorPart[] dirtyEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getDirtyEditors();
		for (IEditorPart iEditorPart : dirtyEditors) {
			iEditorPart.doSave(null);
		}
		/* Give editor time to save (since it is a non blocking operation!) */
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ITextEditor editor = (ITextEditor) editore;

		/*
		 * String selText = null; try { selText = doc.get(startOffset,
		 * selLength); } catch (BadLocationException e1) { // TODO
		 * Auto-generated catch block e1.printStackTrace(); }
		 */
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
			iproj = aFile.getProject();
		}
		String loc = aFile.getLocation().toString();

		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			Job job = new Job("formatR") {

				REXPLogical bol = null;

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Format R source ...", IProgressMonitor.UNKNOWN);

					if (con != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							try {
								bol = (REXPLogical) con.eval("require(formatR)");
							} catch (RserveException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							if (bol.isTRUE()[0]) {

								try {
									con.eval("library(formatR);try(tidy_source(source = \"" + loc + "\",file = \"" + loc + "\"))");
									// rcon.eval("tidy.source(source =
									// \""+loc+"\",file = \"clipboard\")");

									/*
									 * Display display = Display.getDefault();
									 * display.asyncExec(new Runnable() {
									 * 
									 * public void run() {
									 * //setClipboardData(doc); } });
									 */
								} catch (RserveException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							} else {
								message("Library 'formatR' required!\nPlease install the 'formatR' package!");
							}

						} else {
							message("Rserve is busy!");
						}
					}

					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IProject proj = root.getProject(iproj.getName());
					try {
						proj.refreshLocal(IResource.DEPTH_INFINITE, null);
					} catch (CoreException e) {
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

						RState.setBusy(false);
					} else {

					}
				}
			});
			// job.setSystem(true);
			job.schedule();
		} else {
			message("Rserve is not alive!");
		}

	}

	/**
	 * Opens a message Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 */
	public void message(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(Util.getShell(),

						SWT.ICON_WARNING);
				messageBox.setText("Info!");
				messageBox.setMessage(text);
				messageBox.open();
			}
		});
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

	}

}