package com.eco.bio7.reditor.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
import com.eco.bio7.util.Util;

public class RefreshLoadedPackagesForCompletion extends Action {

	public RefreshLoadedPackagesForCompletion(String text) {
		super(text);
		setActionDefinitionId("com.eco.bio7.reditor.rserve.refresh.packages");
		setId("com.eco.bio7.reditor.refresh.packages");

	}

	public void run() {

		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			Job job = new Job("formatR") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Format R source ...", IProgressMonitor.UNKNOWN);

					RConnection c = REditor.getRserveConnection();
					if (c != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							try {

								/*
								 * Function loaded at Rserve startup. Writes the
								 * available functions to a file!
								 */
								c.eval(".bio7WriteFunctionDef();");
							} catch (RserveException e) {

								e.printStackTrace();
							}

							/*
							 * Reload the code proposals (not the templates) for
							 * the R editor!
							 */
							CalculateRProposals.setStartupTemplate(false);
							CalculateRProposals.loadRCodePackageTemplates();
							CalculateRProposals.updateCompletions();
						}

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

}
