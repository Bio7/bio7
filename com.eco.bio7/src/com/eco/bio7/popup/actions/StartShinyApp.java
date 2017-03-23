package com.eco.bio7.popup.actions;

import java.util.UUID;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.documents.JavaFXWebBrowser;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.actions.ProfileRScript;

public class StartShinyApp extends Action implements IObjectActionDelegate {

	private IPreferenceStore store;

	public StartShinyApp() {
		super();
		setId("com.eco.bio7.startshinyapp");
		setActionDefinitionId("com.eco.bio7.startshinyapp");
		 store = Bio7Plugin.getDefault().getPreferenceStore();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		PlatformUI.getWorkbench().saveAllEditors(true);
		

		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;

			Object selectedObj = strucSelection.getFirstElement();

			if (selectedObj instanceof IFolder || selectedObj instanceof IProject) {
				IFolder selectedFolder = null;
				String loc;
				if (selectedObj instanceof IProject) {
					IProject proj = (IProject) selectedObj;
					loc = proj.getLocation().toOSString();

				} else {
					selectedFolder = (IFolder) selectedObj;
					loc = selectedFolder.getLocation().toString();

				}

				String cleanedPath = loc.replace("\\", "/");

				if (RServe.isAlive()) {
					Bio7Action.callRserve();
				}
				int shinyPort = store.getInt("SHINY_PORT");
				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
				if (selectionConsole.equals("R")) {

					Job job = new Job("Shiny Server") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {

							monitor.beginTask("Start Shiny Server...", IProgressMonitor.UNKNOWN);

							// if (RState.isBusy() == false) {

							// RConnection con = RServe.getConnection();
							// try {

							// con.eval("try(setwd(\"" + cleanedPath + "\"))");
							// con.eval("try(library(shiny))");

							ConsolePageParticipant.pipeInputToConsole("setwd(\"" + cleanedPath + "\");library(shiny);runApp(port="+shinyPort+",launch.browser =FALSE)", true, true);
							System.out.println("runApp(port="+shinyPort+",launch.browser =FALSE)");

							// con.eval("try(runApp(port=5099,launch.browser =FALSE))");
							/*
							 * } catch (RserveException e) { // TODO Auto-generated catch block e.printStackTrace(); }
							 */

							// }

							monitor.done();
							return Status.OK_STATUS;
						}

					};
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								/* Activate the editor again after the job! */
								// Util.activateEditorPage(editor);
							} else {

							}
						}
					});
					job.schedule();

					Display display = PlatformUI.getWorkbench().getDisplay();
					display.asyncExec(new Runnable() {

						public void run() {

							
							String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");

							if (openInJavaFXBrowser.equals("SWT_BROWSER")) {
								Work.openView("com.eco.bio7.browser.Browser");
								BrowserView b = BrowserView.getBrowserInstance();
								b.browser.setJavascriptEnabled(true);
								b.setLocation("http://127.0.0.1:"+shinyPort+"");
							} else {
								boolean openInBrowserInExtraView = store.getBoolean("OPEN_BOWSER_IN_EXTRA_VIEW");
								if (openInBrowserInExtraView) {
									String id = UUID.randomUUID().toString();
									new JavaFXWebBrowser(true).createBrowser("http://127.0.0.1:"+shinyPort+"", id);
								} else {
									new JavaFXWebBrowser(true).createBrowser("http://127.0.0.1:"+shinyPort+"", "Display");
								}

							}
						}
					});
				} else {
					Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
				}
			}

		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
