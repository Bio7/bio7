/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.rbridge.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.reditors.REditor;

public class InterpretR extends Action {

	/* The toolbar action to interpret the current R script! */

	public InterpretR(String text, IWorkbenchWindow window) {
		super(text);

		setId("com.eco.bio7.execute_r_script2");
		setActionDefinitionId("com.eco.bio7.execute_r_script2");
	}

	public void run() {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			utils.cons.clear();
		}
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (editor == null || editor instanceof REditor == false) {
			return;
		}

		if (editor.isDirty()) {
			editor.doSave(new NullProgressMonitor());
		}

		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		RConnection con = RServe.getConnection();
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
		}
		String loc = aFile.getLocation().toString();

		boolean remote = Bio7Plugin.getDefault().getPreferenceStore().getBoolean("REMOTE");
		//IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		// boolean rPipe = store.getBoolean("r_pipe");

		if (con == null) {
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

			if (selectionConsole.equals("R")) {

				ConsolePageParticipant.pipeInputToConsole("source('" + loc + "')", true, true);
				System.out.print("source('" + loc + "')");
				System.out.println();
			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		} else {

			String rCode = doc.get();

			if (RState.isBusy() == false) {
				RState.setBusy(true);
				final RInterpreterJob Do;
				if (remote == false) {
					Do = new RInterpreterJob(rCode, loc);

				} else {
					Do = new RInterpreterJob(rCode, null);
				}
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {

							int countDev = RServe.getDisplayNumber();
							RState.setBusy(false);
							if (countDev > 0) {
								RServe.closeAndDisplay();
							}
							RServeUtil.listRObjects();
						} else {
							RState.setBusy(false);
						}
					}
				});
				Do.setUser(true);
				Do.schedule();
			} else {

				Bio7Dialog.message("Rserve is busy!");

			}

			/*
			 * else {
			 * 
			 * MessageBox messageBox = new MessageBox(new Shell(),
			 * 
			 * SWT.ICON_WARNING); messageBox.setMessage( "RServer connection failed - Server is not running!"); messageBox.open();
			 * 
			 * }
			 */
		}

	}

}
