package com.eco.bio7.rbridge.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RClipboardValuesJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;

public class ClipboardRValues extends Action {

	public ClipboardRValues(String text, IWorkbenchWindow window) {
		super(text);

		setId("com.eco.bio7.rserve_clipboardRValues");
		setActionDefinitionId("com.eco.bio7.execute_r_clipboard_values");
	}

	public void run() {
		/* Execute a script from the clipboard, if available! */
		
		//IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		//boolean rPipe = store.getBoolean("r_pipe");

		if (RServe.getConnection()==null) {
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			if (selectionConsole.equals("R")) {

				ConsolePageParticipant.pipeInputToConsole("clipboardData<-read.delim(\"clipboard\");clipboardData",true,true);
				System.out.println();
			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

		else {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				RClipboardValuesJob Do = new RClipboardValuesJob();
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							RState.setBusy(false);
							RServeUtil.listRObjects();
						} else {
							RState.setBusy(false);
						}
					}
				});
				Do.setUser(true);
				Do.schedule();
			} else {
				MessageBox messageBox = new MessageBox(new Shell(),

				SWT.ICON_WARNING);
				messageBox.setMessage("Rserve is busy!");
				messageBox.open();

			}

		}
	}
}