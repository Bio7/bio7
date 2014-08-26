package com.eco.bio7.rbridge.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RClipboardScriptJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

public class ClipboardRScipt extends Action {

	public ClipboardRScipt(String text, IWorkbenchWindow window) {
		super(text);

		setId("com.eco.bio7.rserve_clipboardR");
		setActionDefinitionId("com.eco.bio7.execute_r_clipboard");
	}

	public void run() {
		//IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		//boolean rPipe = store.getBoolean("r_pipe");

		if (RServe.getConnection()==null) {
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			if (selectionConsole.equals("R")) {
				ConsolePageParticipant.pipeInputToConsole("fileClipboardTemp<-file(\"clipboard\", open=\"r\");source(fileClipboardTemp,echo=T);close(fileClipboardTemp);remove(fileClipboardTemp)",true,true);
				System.out.println();
			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

		else {

			/* Execute a script from the clipboard, if available! */
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				RClipboardScriptJob Do = new RClipboardScriptJob();
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							int countDev = RServe.getDisplayNumber();
							RState.setBusy(false);
							if (countDev > 0) {
								RServe.closeAndDisplay();
							}
						} else {
							RState.setBusy(false);
						}
					}
				});
				Do.setUser(true);
				Do.schedule();

			}

			else {

				Bio7Dialog.message("RServer is busy!");

			}
		}
	}
}