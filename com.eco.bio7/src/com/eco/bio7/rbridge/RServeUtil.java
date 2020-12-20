package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.Console;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.StartBio7Utils;

/**
 * @author Bio7 A Rserve utility class to evaluate R scripts and code in an
 *         Eclipse job!
 */
public class RServeUtil {

	protected static REXP rexp;
	protected static Boolean val;

	/**
	 * Evaluates a script in R running in a job and joins threads!.
	 * 
	 * @param script a script.
	 * @param loc    the script location.
	 */
	public static void evalR(String script, String loc) {
		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				RInterpreterJob Do = new RInterpreterJob(script, loc);
				Do.setUser(true);
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							int countDev = RServe.getDisplayNumber();
							RState.setBusy(false);
							if (countDev > 0) {
								RServe.closeAndDisplay();
							}
							// BatchModel.resumeFlow();

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
	 * @param eval a R command.
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
	 * @param name   the object name in R.
	 * @param assign the object to assign.
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

	/**
	 * Evaluates a script in R running in a job without to join threads!.
	 * 
	 * @param script a script.
	 * @param loc    the script location.
	 */
	public static void evalR2(String script, String loc) {
		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				RInterpreterJob Do = new RInterpreterJob(script, loc);
				Do.setUser(true);
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							int countDev = RServe.getDisplayNumber();
							RState.setBusy(false);
							if (countDev > 0) {
								RServe.closeAndDisplay();
							}
							// BatchModel.resumeFlow();

						} else {
							RState.setBusy(false);
						}
					}
				});

				Do.schedule();

			} else {
				System.out.println("Rserve is busy. Can't execute the R script!");
			}
		}

	}

	/**
	 * Evaluates a script in R running in a job without using join for the plot
	 * job!.
	 * 
	 * @param script a script.
	 * @param loc    the script location.
	 */
	public static void evalR3(String script, String loc) {
		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				RInterpreterJob Do = new RInterpreterJob(script, loc);
				Do.setUser(true);
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							int countDev = RServe.getDisplayNumber();
							RState.setBusy(false);
							if (countDev > 0) {
								RServe.closeAndDisplayNoJoin();
							}

							// BatchModel.resumeFlow();

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
	 * Evaluates a script in R running in a job and joins threads!. Display thread
	 * is wrapped!
	 * 
	 * @param script a script.
	 * @param loc    the script location.
	 */
	public static void evalStringR(String script) {
		RServe.printJobJoin(script);
	}

	/**
	 * A method to trigger the update of the R object list in the R/Shell view.
	 */
	public static void listRObjects() {
		RShellView rShellView = RShellView.getInstance();
		if (rShellView != null) {
			rShellView.displayRObjects();
		}
	}

	/**
	 * A method to correct strings for R.
	 * 
	 * @param word
	 * @return a corrected String for use in R
	 */
	public static String replaceWrongRWord(String word) {
		String names;
		/* Replace wrong header chars! */
		String regEx = "[^a-zA-Z0-9_.]";
		if (word.substring(0, 1).matches("[0-9]")) {
			word = word.replaceFirst("[0-9]", "X" + word.charAt(0));
		} else if (word.substring(0, 1).matches(regEx)) {
			word = word.replaceFirst(regEx, "X.");
		}

		names = word.replaceAll(regEx, "_");

		return names;
	}

	/**
	 * A method to evaluate a R script from the given location.
	 * 
	 * @param loc The path to the script
	 */
	public static void evalRScript(String loc) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		final RConnection cscript = RServe.getConnection();

		if (RServe.isRrunning()) {
			if (cscript != null) {
				try {
					// boolean startShell =
					// Bio7Plugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.R_START_SHELL);

					if (Bio7Dialog.getOS().equals("Windows")) {
						/* If a location is given! */
						if (loc != null) {
							loc = loc.replace("/", "\\");

							cscript.assign(".bio7TempRScriptFile", loc);

							String rout = null;
							try {
								/* First write a message which file is sourced! */
								cscript.eval("message(paste0(\"> source('\",.bio7TempRScriptFile),\"')\",sep=\"\")");
								String options = store.getString("R_SOURCE_OPTIONS");
								String rCommand = "" + "paste(capture.output(tryCatch(source(.bio7TempRScriptFile," + options + "),error = function(e) {message(paste0(\"\n\",e))})),collapse=\"\\n\")";
								rout = cscript.eval(rCommand).asString();

							} catch (REXPMismatchException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Console cons = StartBio7Utils.getConsoleInstance().cons;
							cons.println(rout);
							/* Send also the output to the R console view! */
							if (RShellView.isConsoleExpanded()) {

								RShellView.setTextConsole(rout);

							}

						}
					}
					/* If Linux or MacOSX is the OS! */
					else {

						if (loc != null) {
							cscript.assign(".bio7TempRScriptFile", loc);
							/*
							 * if (startShell) { cscript.voidEval("try(source(.bio7TempRScriptFile))");
							 * 
							 * } else {
							 */
							String rout = null;
							try {
								cscript.eval("message(paste0(\"> source('\",.bio7TempRScriptFile),\"')\",sep=\"\")");
								String options = store.getString("R_SOURCE_OPTIONS");
								String rCommand = "" + "paste(capture.output(tryCatch(source(.bio7TempRScriptFile," + options + "),error = function(e) {message(paste0(\"\n\",e))})),collapse=\"\\n\")";
								rout = cscript.eval(rCommand).asString();
							} catch (REXPMismatchException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Console cons = StartBio7Utils.getConsoleInstance().cons;
							cons.println(rout);
							/* Send also the output to the R console view! */
							if (RShellView.isConsoleExpanded()) {
								RShellView.setTextConsole(rout);
							}
							// }

						}
					}

				} catch (RserveException e) {

					System.out.println("Rserve exception!");
				}

			}

		}

	}

}
