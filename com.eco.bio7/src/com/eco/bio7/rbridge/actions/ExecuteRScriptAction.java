package com.eco.bio7.rbridge.actions;

import java.io.File;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rbridge.REval;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.views.RShellView;

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
			/* Started when it is used (preferences) */
			if (RTools.isAlive() || com.eco.bio7.rcp.AutoStart.startROnUse()) {

				/* Waits for R if it is busy (e.g., loading the workspace) */
				com.eco.bio7.rbridge.RWait.whenFree("the R script", () -> {
					RState.setBusy(true);
					final RInterpreterJob Do = new RInterpreterJob(null, path);
					Do.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							/* Free again also after an error or a stop (else R stayed busy) */
							int countDev = RTools.getDisplayNumber();
							RState.setBusy(false);
							if (event.getResult().isOK() && countDev > 0) {
								RTools.closeAndDisplay();
							}
							REval.listRObjects();
						}
					});
					Do.setUser(true);
					Do.schedule();
				});

			} else {

				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

				if (selectionConsole.equals("R")) {

					ConsolePageParticipant.pipeInputToConsole("source('" + path + "')", true, true);
					System.out.print("source('" + path + "')");
					System.out.println();
				} else {
					Bio7Dialog.message(com.eco.bio7.rcp.AutoStart.NOT_RUNNING_R);
				}

			}
		}

	}

}