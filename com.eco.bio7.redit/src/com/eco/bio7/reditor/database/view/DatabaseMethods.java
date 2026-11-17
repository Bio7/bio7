package com.eco.bio7.reditor.database.view;

import com.eco.bio7.rbridge.REvaluator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Bio7Dialog;
import com.eco.bio7.rffm.RValue;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

public class DatabaseMethods {
	private String dir;

	public String[] rOdbcdrivers() {

		REvaluator c = REditor.getEvaluator();
		String[] res = null;
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);

				RValue bol = null;
				try {
					bol = c.eval("require(odbc)");
				} catch (RException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (bol.booleans()[0]) {

					try {
						c.eval("library(odbc);.odbc_driver_name <- odbcListDrivers()$name;"
								+ ".odbc_names<-unique(.odbc_driver_name);");
						res = c.eval(".odbc_names").strings();
						// con.eval("remove(c(.odbc_names,.odbc_driver_name))");
					} catch (RException | RTypeException e) {
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
			Bio7Dialog.message("R is not running!\nPlease start R (toolbar action \"Start R\").");
		}

		/*
		 * REvaluator con = REditor.getEvaluator(); if (con != null) { Job job =
		 * new Job("Evaluate") {
		 * 
		 * @Override protected IStatus run(IProgressMonitor monitor) {
		 * monitor.beginTask("Evaluate R code...", IProgressMonitor.UNKNOWN);
		 * 
		 * REvaluator c = REditor.getEvaluator(); if (c != null) { if
		 * (RState.isBusy() == false) { RState.setBusy(true);
		 * 
		 * try { con.eval("library(odbc);.odbc_driver_name <- odbcListDrivers()$name;" +
		 * ".odbc_names<-unique(.odbc_driver_name);"); res =
		 * con.eval(".odbc_names").strings(); //
		 * con.eval("remove(c(.odbc_names,.odbc_driver_name))"); } catch
		 * (RException | RTypeException e) { // TODO Auto-generated catch
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
		 * job.schedule(); } else { Bio7Dialog.message("R is not running!\nPlease start R (toolbar action \"Start R\")."); }
		 */
		return res;
	}

	/**
	 * Evaluates and prints multiple expressions to the Bio7 console.
	 * 
	 * @param expression a R expression as a string.
	 */
	public void evaluateInMultiplePrintJobs(String expression) {

		REvaluator con = REditor.getEvaluator();
		if (con != null) {
			Job job = new Job("Reload") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Reload package information...", IProgressMonitor.UNKNOWN);

					REvaluator c = REditor.getEvaluator();
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
													.firstString();

										} catch (RTypeException e) {

											e.printStackTrace();
										} catch (RException e) {
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
												+ ")),collapse=\"\\n\"))").firstString();
										System.out.println(result);
									} catch (RTypeException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

									// con.eval(expression);

									// System.out.println(result);

									// c.eval(expression);
								} catch (RException e) {

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
			Bio7Dialog.message("R is not running!\nPlease start R (toolbar action \"Start R\").");
		}

	}

	/**
	 * Evaluates and prints an expression to the Bio7 console.
	 * 
	 * @param expression a R expression as a string.
	 */
	public void evaluateInPrintJob(String expression) {

		REvaluator con = REditor.getEvaluator();
		if (con != null) {
			Job job = new Job("Reload") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Reload package information...", IProgressMonitor.UNKNOWN);

					REvaluator c = REditor.getEvaluator();
					if (c != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							try {

								String result;
								try {
									result = con.eval(
											"try(paste(capture.output(print(" + expression + ")),collapse=\"\\n\"))")
											.firstString();
									System.out.println(result);
								} catch (RTypeException e) {

									e.printStackTrace();
								}

							} catch (RException e) {

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
			Bio7Dialog.message("R is not running!\nPlease start R (toolbar action \"Start R\").");
		}

	}

}
