package com.eco.bio7.rbridge.actions;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditors.REditor;

public class ProfileScriptAction implements IObjectActionDelegate, IEditorActionDelegate {

	protected boolean canEvaluate = true;

	
	
	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		action.setActionDefinitionId("com.eco.bio7.rserve_profile_script");
	     if(targetEditor!=null)
	     targetEditor.getSite().getKeyBindingService().registerAction(action);
		
	}

	public void run(IAction action) {
		if (canEvaluate) {

			IEditorPart rEditor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			// IPreferenceStore store =
			// Bio7Plugin.getDefault().getPreferenceStore();
			// boolean rPipe = store.getBoolean("r_pipe");
			RConnection con = RServe.getConnection();
			if (con == null) {
				if (rEditor instanceof REditor) {
					String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
					if (selectionConsole.equals("R")) {
						String editorScript = new ProfileRScript().profileSource(rEditor,false);
						ConsolePageParticipant.pipeInputToConsole(editorScript, true, true);
						System.out.println(editorScript);
					} else {
						Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
					}
				}

			}

			else {

				if (rEditor instanceof REditor) {

					if (RServe.isAliveDialog()) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							ProfileRScript prof=new ProfileRScript();
							String editorScript = prof.profileSourceRserve(rEditor,false);
							final RInterpreterJob Do = new RInterpreterJob(editorScript, true, null);
							Do.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										int countDev = RServe.getDisplayNumber();
										RState.setBusy(false);
										if (countDev > 0) {
											RServe.closeAndDisplay();
										}
										prof.openWebBrowser();

									}
								}
							});
							Do.setUser(true);
							Do.schedule();
						} else {

							Bio7Dialog.message("Rserve is busy!");
						}

					}

				} else {

					MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_WARNING);
					messageBox.setMessage("There is no Bio7 editor available !");
					messageBox.open();

				}

				/*
				 * else { MessageBox messageBox = new MessageBox(new Shell(),
				 * 
				 * SWT.ICON_WARNING); messageBox.setMessage(
				 * "RServer connection failed - Server is not running !");
				 * messageBox.open();
				 * 
				 * }
				 */
			}
		}
	}

	

	

	

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		
	}

	

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub
		
	}
}