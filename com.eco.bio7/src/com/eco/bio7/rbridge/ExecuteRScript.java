package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * @author Bio7 A helper class to evaluate R scripts.
 */
public class ExecuteRScript {

	private RScriptExecuterInterface executer;
	private int progressSteps;
	/* embedded R */
	private com.eco.bio7.rbridge.backend.RBackend backend;

	/**
	 * Executes a script with the connection of Bio7 (embedded R) in a job,
	 * e.g., a lambda (RBackend r, IProgressMonitor monitor) -&gt; { r.eval("..."); }.
	 *
	 * @param progressSteps steps as integer or 'IProgressMonitor.UNKNOWN' (-1)
	 */
	public static ExecuteRScript run(RBackendScript script, int progressSteps) {
		return new ExecuteRScript(new RScriptExecuter() {
			@Override
			public void evaluate(com.eco.bio7.rbridge.backend.RBackend r, org.eclipse.core.runtime.IProgressMonitor monitor) {
				try {
					script.evaluate(r, monitor);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, progressSteps);
	}
	private boolean listRObjects;

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
		this.listRObjects = true;
		if (RTools.isAlive() || com.eco.bio7.rcp.AutoStart.startROnUse()) {
			backend = RBridge.getBackend();
			/* Waits for R if it is busy (it was not executed before, only a message on stdout) */
			RWait.whenFree("the R script", () -> {
				RState.setBusy(true);
				executeRScript();
			});

		}
	}
	
	/**
	 * Constructor to execute a job which calls the interface class of the given
	 * RScriptExecuterInterface instance.
	 * 
	 * @param executer      a RScriptExecuterInterface class
	 * @param progressSteps steps as integer or 'IProgressMonitor.UNKNOWN' (which
	 *                      has value -1)
	 *  @param listRObjects automatically list the R objects in the R-Shell view                     
	 */
	public ExecuteRScript(RScriptExecuterInterface executer, int progressSteps, boolean listRObjects) {
		this.executer = executer;
		this.progressSteps = progressSteps;
		this.listRObjects = listRObjects;
		if (RTools.isAlive() || com.eco.bio7.rcp.AutoStart.startROnUse()) {
			backend = RBridge.getBackend();
			/* Waits for R if it is busy (it was not executed before, only a message on stdout) */
			RWait.whenFree("the R script", () -> {
				RState.setBusy(true);
				executeRScript();
			});

		}
	}

	private void executeRScript() {
		Job job = new Job("R script execution") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				monitor.beginTask("Started execution ...", progressSteps);
				/* We create a feature stack. R connection not necessary! */

				executer.evaluate(backend, monitor);

				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {

				if (event.getResult().isOK()) {
					int countDev = RTools.getDisplayNumber();
					RState.setBusy(false);
					if (countDev > 0) {
						RTools.closeAndDisplay();
					}
					/*List the R objects in the R-Shell view!*/
					if (listRObjects) {
						REval.listRObjects();
					}
					// BatchModel.resumeFlow();

				} else {
					RState.setBusy(false);
				}
				/*
				 * If we use the RScriptExecuter class we can call the done method which can be
				 * overridden if necessary!
				 */
				if (executer instanceof RScriptExecuter) {
					RScriptExecuter exec = (RScriptExecuter) executer;
					exec.done(event);
				}
			}
		});

		job.schedule();
	}

}
