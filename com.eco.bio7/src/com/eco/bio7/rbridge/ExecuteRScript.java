package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.rosuda.REngine.Rserve.RConnection;

/**
 * @author Bio7 A helper class to evaluate R scripts.
 */
public class ExecuteRScript {

	private RScriptExecuterInterface executer;
	private int progressSteps;
	private RConnection con;

	/**
	 * Constructor to execute a job which calls the interface class of the given
	 * RScriptExecuterInterface instance.
	 * 
	 * @param executer      a RScriptExecuterInterface class
	 * @param progressSteps steps as integer or 'IProgressMonitor.UNKNOWN' (which
	 *                      has value -1)
	 */
	public ExecuteRScript(RScriptExecuterInterface executer, int progressSteps) {
		this.executer = executer;
		this.progressSteps = progressSteps;
		if (RServe.isAlive()) {
			con = RServe.getConnection();
			if (RState.isBusy() == false) {
				/* Notify that R is busy! */
				RState.setBusy(true);
				executeRScript();
			} else {
				System.out.println("RServer is busy. Can't execute the R script!");
			}

		}
	}

	private void executeRScript() {
		Job job = new Job("R script execution") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				monitor.beginTask("Started execution ...", progressSteps);
				/* We create a feature stack. R connection not necessary! */

				executer.evaluate(con, monitor);

				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {

				if (event.getResult().isOK()) {
					int countDev = RServe.getDisplayNumber();
					RState.setBusy(false);
					if (countDev > 0) {
						RServe.closeAndDisplay();
					}

					RServeUtil.listRObjects();
					// BatchModel.resumeFlow();

				} else {
					RState.setBusy(false);
				}
			}
		});

		job.schedule();
	}

}
