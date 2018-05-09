package com.eco.bio7.rbridge.actions;

import java.io.File;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

public class ExecuteRScriptAction extends Action {

	private final IWorkbenchWindow window;
	private File file;
	private String text;

	/* This is an extra action to execute r scripts in the R scripts menu! */
	public ExecuteRScriptAction(String text, IWorkbenchWindow window, File file) {
		super(text);
		this.window = window;
		this.file = file;
		this.text = text;
		setId("com.eco.bio7.execute_r_script");
		// setActionDefinitionId("com.eco.bio7.execute_r_scriptaction");

	}

	public void run() {
		if (text.equals("Empty")) {
			System.out.println("No script available!");
		}

		else {
			String path = file.toString().replace("\\", "/");
			if (RServe.isAlive()) {

				if (RState.isBusy() == false) {
					RState.setBusy(true);
					final RInterpreterJob Do = new RInterpreterJob(null, path);
					Do.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								int countDev = RServe.getDisplayNumber();
								RState.setBusy(false);
								if (countDev > 0) {
									RServe.closeAndDisplay();
								}

							}
						}
					});
					Do.setUser(true);
					Do.schedule();
				} else {

					Bio7Dialog.message("Rserve is busy!");
				}

			} else {

				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

				if (selectionConsole.equals("R")) {

					ConsolePageParticipant.pipeInputToConsole("source('" + path + "')", true, true);
					System.out.print("source('" + path + "')");
					System.out.println();
				} else {
					Bio7Dialog.message("Please start Rserve or the \"Native R\" shell in the Bio7 console!");
				}

			}
		}

	}

}