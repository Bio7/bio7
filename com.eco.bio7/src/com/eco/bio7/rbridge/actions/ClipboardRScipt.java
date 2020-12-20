package com.eco.bio7.rbridge.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.util.Util;

public class ClipboardRScipt extends Action {

	protected String data;
	File temp = null;

	public ClipboardRScipt(String text, IWorkbenchWindow window) {
		super(text);

		setId("com.eco.bio7.rserve_clipboardR");
		setActionDefinitionId("com.eco.bio7.execute_r_clipboard");
	}

	public void run() {
		// IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		// boolean rPipe = store.getBoolean("r_pipe");

		if (RServe.getConnection() == null) {
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			if (selectionConsole.equals("R")) {
				ConsolePageParticipant.pipeInputToConsole("fileClipboardTemp<-file(\"clipboard\", open=\"r\");source(fileClipboardTemp,echo=T);close(fileClipboardTemp);remove(fileClipboardTemp)", true, true);
				System.out.println();
			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

		else {

			Clipboard cb = new Clipboard(Util.getDisplay());

			TextTransfer transfer = TextTransfer.getInstance();
			data = (String) cb.getContents(transfer);
			data = data.replace(System.lineSeparator(), "\n");

			System.out.println(data);

			try {

				// Work with a temporary file avoids a deadlock!
				temp = File.createTempFile("tempfile", ".tmp");

				// write it
				BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
				bw.write(data);
				bw.close();

			} catch (IOException e) {

				e.printStackTrace();

			}
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				RInterpreterJob clipboardJob = new RInterpreterJob(null, temp.getAbsolutePath());

				clipboardJob.addJobChangeListener(new JobChangeAdapter() {
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

				clipboardJob.schedule();
			} else {
				Bio7Dialog.message("Rserve is busy!");

				
			}

		}

	}

}