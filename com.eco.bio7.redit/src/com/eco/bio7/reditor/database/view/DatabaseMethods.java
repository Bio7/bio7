package com.eco.bio7.reditor.database.view;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Bio7Dialog;

public class DatabaseMethods {
	private String dir;

	public String[] rOdbcdrivers() {

		RConnection c = REditor.getRserveConnection();
		String[] res = null;
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);

				REXPLogical bol = null;
				try {
					bol = (REXPLogical) c.eval("require(odbc)");
				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (bol.isTRUE()[0]) {

					try {
						c.eval("library(odbc);.odbc_driver_name <- odbcListDrivers()$name;"
								+ ".odbc_names<-unique(.odbc_driver_name);");
						res = c.eval(".odbc_names").asStrings();
						// con.eval("remove(c(.odbc_names,.odbc_driver_name))");
					} catch (RserveException | REXPMismatchException e) {
						// TODO Auto-generated catch block
						RState.setBusy(false);
						e.printStackTrace();

					}
				} else {

					Bio7Dialog.message("Library 'odbc' required!\nPlease install the 'odbc' package!");
				}

			}
			RState.setBusy(false);
		} else {
			Bio7Dialog.message("Rserve is not alive!");
		}

		/*
		 * RConnection con = REditor.getRserveConnection(); if (con != null) { Job job =
		 * new Job("Evaluate") {
		 * 
		 * @Override protected IStatus run(IProgressMonitor monitor) {
		 * monitor.beginTask("Evaluate R code...", IProgressMonitor.UNKNOWN);
		 * 
		 * RConnection c = REditor.getRserveConnection(); if (c != null) { if
		 * (RState.isBusy() == false) { RState.setBusy(true);
		 * 
		 * try { con.eval("library(odbc);.odbc_driver_name <- odbcListDrivers()$name;" +
		 * ".odbc_names<-unique(.odbc_driver_name);"); res =
		 * con.eval(".odbc_names").asStrings(); //
		 * con.eval("remove(c(.odbc_names,.odbc_driver_name))"); } catch
		 * (RserveException | REXPMismatchException e) { // TODO Auto-generated catch
		 * block e.printStackTrace(); }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * monitor.done(); return Status.OK_STATUS; }
		 * 
		 * }; job.addJobChangeListener(new JobChangeAdapter() { public void
		 * done(IJobChangeEvent event) {
		 * 
		 * if (event.getResult().isOK()) { RState.setBusy(false);
		 * 
		 * } else { RState.setBusy(false); } } }); // job.setSystem(true);
		 * job.schedule(); } else { Bio7Dialog.message("Rserve is not alive!"); }
		 */
		return res;
	}

	/**
	 * Evaluates and prints multiple expressions to the Bio7 console.
	 * 
	 * @param expression a R expression as a string.
	 */
	public void evaluateInMultiplePrintJobs(String expression) {

		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			Job job = new Job("Reload") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Reload package information...", IProgressMonitor.UNKNOWN);

					RConnection c = REditor.getRserveConnection();
					if (c != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);

							if (expression.contains(";")) {

								// See:
								// http://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
								// Changed to exclude quoted semicolons!
								String[] t = expression.split(";(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
								for (int i = 0; i < t.length; i++) {
									if (t[i].isEmpty() == false) {
										String trybegin = "try(";
										String tryend = ")";

										String out = null;
										try {
											out = con
													.eval("paste(capture.output(print(" + trybegin + trybegin + "("
															+ t[i] + ")" + tryend + tryend + ")),collapse=\"\\n\")")
													.asString();

										} catch (REXPMismatchException e) {

											e.printStackTrace();
										} catch (RserveException e) {
											System.out.println("Error : " + e.getMessage());
										}
										System.out.println();
									}
								}

							} else {
								try {

									String result;
									try {
										result = con.eval("try(paste(capture.output(print(" + expression
												+ ")),collapse=\"\\n\"))").asString();
										System.out.println(result);
									} catch (REXPMismatchException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

									// con.eval(expression);

									// System.out.println(result);

									// c.eval(expression);
								} catch (RserveException e) {

									e.printStackTrace();
								}

							}
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

			job.schedule();
		} else {
			Bio7Dialog.message("Rserve is not alive!");
		}

	}

	/**
	 * Evaluates and prints an expression to the Bio7 console.
	 * 
	 * @param expression a R expression as a string.
	 */
	public void evaluateInPrintJob(String expression) {

		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			Job job = new Job("Reload") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Reload package information...", IProgressMonitor.UNKNOWN);

					RConnection c = REditor.getRserveConnection();
					if (c != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							try {

								String result;
								try {
									result = con.eval(
											"try(paste(capture.output(print(" + expression + ")),collapse=\"\\n\"))")
											.asString();
									System.out.println(result);
								} catch (REXPMismatchException e) {

									e.printStackTrace();
								}

							} catch (RserveException e) {

								e.printStackTrace();
							}

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
			Bio7Dialog.message("Rserve is not alive!");
		}

	}

}
