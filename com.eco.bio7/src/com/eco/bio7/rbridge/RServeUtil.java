package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.compile.RInterpreterJob;

/**
 * @author Bio7
 * A Rserve utility class to evaluate R scripts and code in an Eclipse job!
 */
public class RServeUtil {

	protected static REXP rexp;

	/**
	 * Evaluates a script in R running in a job.
	 * 
	 * @param script
	 *            a script.
	 * @param loc
	 *            the script location.
	 */
	public static void evalR(String script, String loc) {
		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				RInterpreterJob Do = new RInterpreterJob(script, false, loc);
				Do.setUser(true);
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							int countDev = RServe.getDisplayNumber();
							RState.setBusy(false);
							if (countDev > 0) {
								RServe.closeAndDisplay();
							}
							BatchModel.resumeFlow();

						} else {
							RState.setBusy(false);
						}
					}
				});

				Do.schedule();
				try {
					Do.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("Rserve is busy. Can't execute the R script!");
			}
		}

	}

	/**
	 * A method to return REXP objects of Rserve running in a job. This method also
	 * checks if a R job is already running.
	 * 
	 * @param eval
	 *            a R command.
	 * @return a REXP object.
	 */
	public static REXP fromR(String eval) {

		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Job job = new Job("Transfer from R") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
						try {
							rexp = RServe.getConnection().eval(eval);
							
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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

							RState.setBusy(false);
						}
					}
				});
				// job.setSystem(true);
				job.schedule();
				try {
					job.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("Rserve is busy. Can't execute the R script!");
			}
		}
		return rexp;
	}

	/**
	 * A method to assign values from Java to R.
	 * 
	 * @param name
	 *            the object name in R.
	 * @param assign
	 *            the object to assign.
	 */
	public static void toR(String name, Object... assign) {

		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Job job = new Job("Transfer from R") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
						try {
							if (assign[0] instanceof String) {
								RServe.getConnection().assign(name, (String) assign[0]);
							} else if (assign[0] instanceof String[]) {
								try {
									RServe.getConnection().assign(name, (String[]) assign[0]);
								} catch (REngineException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else if (assign[0] instanceof REXP) {
								RServe.getConnection().assign(name, (REXP) assign[0]);
							} else if (assign[0] instanceof double[]) {
								try {
									RServe.getConnection().assign(name, (double[]) assign[0]);
								} catch (REngineException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else if (assign[0] instanceof int[]) {
								try {
									RServe.getConnection().assign(name, (int[]) assign[0]);
								} catch (REngineException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else if (assign[0] instanceof byte[]) {
								try {
									RServe.getConnection().assign(name, (byte[]) assign[0]);
								} catch (REngineException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							else if (assign[0] instanceof REXP && assign[1] instanceof REXP) {
								try {
									RServe.getConnection().assign(name, (REXP) assign[0], (REXP) assign[1]);
								} catch (REngineException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							RState.setBusy(false);
							e.printStackTrace();
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

							RState.setBusy(false);
						}
					}
				});
				// job.setSystem(true);
				job.schedule();
				try {
					job.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				RState.setBusy(false);
			} else {
				System.out.println("Rserve is busy. Can't execute the R script!");
			}

		}

	}

}
