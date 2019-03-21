/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.console.Console;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.documents.JavaFXWebBrowser;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.util.Util;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.FolderOpener;
import javafx.scene.web.WebEngine;

/**
 * This class provides methods for the communication with the Rserve
 * application.
 * 
 * @author Bio7
 * 
 */
public class RServe {

	private static String rout; // The output to the console

	private static boolean Rrunning = false;

	private static RConnection connection = null;

	private static boolean isconnecting = false;

	private static String[] selections = null;

	private static RSession rSession;

	/**
	 * Evaluates an R expression without running in a job. This method can be used
	 * to evaluate R scripts from e.g. Groovy (running already in a job). R plots a
	 * possible, too.
	 * 
	 * @param expression a R expression.
	 */
	public static void evaluateExt(String expression) {
		if (RServe.isAlive()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				try {
					RServe.getConnection().eval(expression);

					int countDev = RServe.getDisplayNumber();
					if (countDev > 0) {
						RServe.closeAndDisplay();
					}

				} catch (RserveException e) {
					RState.setBusy(false);
					System.out.println(e.getRequestErrorDescription());

				}
				RState.setBusy(false);

			} else {
				System.out.println("Rserve is busy!");
			}
		} else {
			System.out.println("No connection to Rserve available!");
		}
	}

	/**
	 * Evaluates and prints an expression to the Bio7 console executed in a job.
	 * 
	 * @param expression a R expression as a string.
	 */
	public static void printJob(String expression) {// helper class to print
		if (RState.isBusy() == false) {
			RState.setBusy(true);
			REvaluateJob job = new REvaluateJob(expression);
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

						int countDev = getDisplayNumber();
						RState.setBusy(false);
						if (countDev > 0) {
							RServe.closeAndDisplay();
						}
						System.out.flush();
						updatePackageImports();						
						RServeUtil.listRObjects();
					} else {
						RState.setBusy(false);

						System.out.flush();
					}
				}

			});

			// job.setSystem(true);
			job.schedule();
		} else {
			Process p;
			// IPreferenceStore store =
			// Bio7Plugin.getDefault().getPreferenceStore();
			// if (store.getBoolean("RSERVE_NATIVE_START")) {
			ConsolePageParticipant consol = ConsolePageParticipant.getConsolePageParticipantInstance();
			p = consol.getRProcess();
			/*
			 * } else {
			 * 
			 * p = RConnectionJob.getProc(); }
			 */

			// Write to the output!
			if (p != null) {
				final OutputStream os = p.getOutputStream();
				final OutputStreamWriter osw = new OutputStreamWriter(os);
				final BufferedWriter bw = new BufferedWriter(osw, 100);

				try {
					bw.write(expression);

					bw.newLine();

					os.flush();
					bw.flush();
					// bw.close();
					System.out.flush();
				} catch (IOException e) {
					System.err.println("");
				}

			}

			// Bio7Dialog.message("Rserve is busy!");

		}

	}

	/**
	 * Evaluates and prints an expression to the Bio7 console executed in a job with
	 * job join.
	 * 
	 * @param expression a R expression as a string.
	 */
	public static void printJobJoin(String expression) {// helper class to print
		if (RState.isBusy() == false) {
			RState.setBusy(true);
			REvaluateJob job = new REvaluateJob(expression);
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

						int countDev = getDisplayNumber();
						RState.setBusy(false);
						if (countDev > 0) {
							RServe.closeAndDisplay();
						}
						System.out.flush();
						updatePackageImports();
						RServeUtil.listRObjects();
					} else {
						RState.setBusy(false);

						System.out.flush();
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
			Process p;
			// IPreferenceStore store =
			// Bio7Plugin.getDefault().getPreferenceStore();
			// if (store.getBoolean("RSERVE_NATIVE_START")) {
			ConsolePageParticipant consol = ConsolePageParticipant.getConsolePageParticipantInstance();
			p = consol.getRProcess();
			/*
			 * } else {
			 * 
			 * p = RConnectionJob.getProc(); }
			 */

			// Write to the output!
			if (p != null) {
				final OutputStream os = p.getOutputStream();
				final OutputStreamWriter osw = new OutputStreamWriter(os);
				final BufferedWriter bw = new BufferedWriter(osw, 100);

				try {
					bw.write(expression);

					bw.newLine();

					os.flush();
					bw.flush();
					// bw.close();
					System.out.flush();
				} catch (IOException e) {
					System.err.println("");
				}

			}

			// Bio7Dialog.message("Rserve is busy!");

		}

	}

	/**
	 * Evaluates and prints an array of expressions to the Bio7 console executed in
	 * a job.
	 * 
	 * @param expressions a R expression as a string.
	 */
	public static void printJobs(String[] expressions) {// helper class to print
		if (RState.isBusy() == false) {
			RState.setBusy(true);
			REvaluateExpressionsJob job = new REvaluateExpressionsJob(expressions);
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

						int countDev = getDisplayNumber();

						RState.setBusy(false);
						if (countDev > 0) {
							RServe.closeAndDisplay();
						}
						System.out.flush();
						updatePackageImports();
						RServeUtil.listRObjects();
					} else {
						System.out.flush();
						RState.setBusy(false);
					}
				}
			});

			// job.setSystem(true);
			job.schedule();
		} else {

			Bio7Dialog.message("Rserve is busy!");

		}

	}

	/**
	 * Update the packages import if a library/require statement was detected!
	 * Method is called in the RShellView class!
	 */
	private static void updatePackageImports() {
		RShellView rShellInst = RShellView.getInstance();
		if (rShellInst != null) {
			rShellInst.updatePackageImports();
		}

	}

	/**
	 * Evaluates and prints an expression to the Bio7 console.
	 * 
	 * @param expression a R expression as a string.
	 */
	public static void print(String expression) {

		/*
		 * try { RServe.rout = RServe.connection.eval("try(paste(capture.output(print("
		 * + expression + ")),collapse=\"\\n\"))").asString(); } catch
		 * (REXPMismatchException e) {
		 * 
		 * e.printStackTrace(); } catch (RserveException e) {
		 * 
		 * e.printStackTrace(); }
		 */
		String trybegin = "tryCatch({";
		String tryend = "},error = function(e) {message(paste(\"Error: \",e$message))})";

		try {
			RServe.rout = RServe.getConnection().eval("paste(capture.output(print(" + trybegin + "(" + expression + ")" + tryend + ")),collapse=\"\\n\")").asString();

		} catch (REXPMismatchException e) {

			e.printStackTrace();
		} catch (RserveException e) {
			System.out.println("Error : " + e.getMessage());
		}

		// StartBio7Utils.getConsoleInstance().cons.println(RServe.rout);

		if (RServe.rout != null && RServe.rout.equals("NULL") == false) {
			StartBio7Utils.getConsoleInstance().cons.println(RServe.rout);
			/* Send also the output to the R console view! */
			if (RShellView.isConsoleExpanded()) {

				RShellView.setTextConsole(RServe.rout);

			}
		}

	}

	/**
	 * Returns a Rserve connection.
	 * 
	 * @return a R connection.
	 */
	public static RConnection getConnection() {
		return connection;
	}

	/**
	 * Returns if the connection to Rserve is alive visible by means of a dialog.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isAliveDialog() {
		if (RServe.connection == null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {

					MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_WARNING);
					messageBox.setMessage("Rserve connection failed\nServer is not running!");
					messageBox.open();
				}
			});

		}
		return Rrunning;

	}

	/**
	 * Returns if the connection to Rserve is alive.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isAlive() {
		return Rrunning;

	}

	/**
	 * Sets a Rserve connection.
	 * 
	 * @param connection a R connection.
	 */
	public static void setConnection(RConnection connection) {
		RServe.connection = connection;
	}

	/**
	 * Returns if the plotting device is the pdf device.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isPdf() {
		return PlotJob.isPdf();
	}

	/**
	 * Set the plotting device to pdf.
	 * 
	 * @param pdf a boolean value.
	 */
	public static void setPdf(boolean pdf) {
		PlotJob.setPdf(pdf);
	}

	/**
	 * Internal method of Bio7.
	 * 
	 * 
	 * @return a boolean value.
	 */
	public static boolean isIsconnecting() {
		return isconnecting;
	}

	/**
	 * Returns if the Rserve application is running.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isRrunning() {
		return Rrunning;
	}

	/**
	 * Set the Rserve running value.
	 * 
	 * @param running a boolean value.
	 */
	public static void setRrunning(boolean running) {
		Rrunning = running;
	}

	/**
	 * Internal method of Bio7.
	 * 
	 * @param rout a string value.
	 */
	public static void setRout(String rout) {
		RServe.rout = rout;
	}

	/**
	 * Internal method of Bio7.
	 * 
	 * @return a string value.
	 */
	public static String getRout() {
		return rout;
	}

	/**
	 * Returns if the Bio7 application is connected to the Rserve application.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isConnected() {
		boolean connected;
		if (RServe.getConnection() != null) {
			connected = true;
		} else {
			connected = false;
		}
		return connected;
	}

	/**
	 * Set the plot size in inch for a pdf plot.
	 * 
	 * @param inchx the width of the plot in inch units.
	 * @param inchy the height of the plot in inch units.
	 */
	public static void setPlotInch(double inchx, double inchy) {
		PlotJob.setPlotInch(inchx, inchy);
	}

	/**
	 * Set the plot size in pixel for a png plot.
	 * 
	 * @param imageWidth  the width of the plot in pixel units.
	 * @param imageHeight the height of the plot in pixel units.
	 */
	public static void setPlotPixel(int imageWidth, int imageHeight) {
		PlotJob.setPlotPixel(imageWidth, imageHeight);

	}

	/**
	 * A method to call a RScript.
	 * 
	 * @param path the relative path from the Bio7 Workspace to the script.
	 */
	public static void callRScript(String path) {
		/* Get the path and convert it for R (Windows) */
		String f = FileRoot.getFileRoot() + path;

		if (Bio7Dialog.getOS().equals("Windows")) {
			f = f.replace("/", "\\");
		}
		try {
			/* Transfer path to R ! */
			RServe.getConnection().assign("fileroot", f);

			/* Call the custom Rscript ! */
			String rout = null;
			/*
			 * try { rout = RServe.getConnection().eval("" +
			 * "try(paste(capture.output(source(fileroot,echo=T)),collapse=\"\\n\"))").
			 * asString(); } catch (REXPMismatchException e) {
			 * 
			 * e.printStackTrace(); }
			 */

			/* First write a message which file is sourced! */
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			RServe.getConnection().eval("message(paste0(\"> source('\",fileroot),\"')\",sep=\"\")");
			String options = store.getString("R_SOURCE_OPTIONS");
			String rCommand = "" + "paste(capture.output(tryCatch(source(fileroot," + options + "),error = function(e) {message(paste0(\"\n\",e))})),collapse=\"\\n\")";
			try {
				rout = RServe.getConnection().eval(rCommand).asString();
			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Console cons = StartBio7Utils.getConsoleInstance().cons;
			if (rout != null && rout.equals("NULL") == false) {
				cons.println(rout);
				/* Send also the output to the R console view! */
				if (RShellView.isConsoleExpanded()) {

					RShellView.setTextConsole(rout);

				}
			}

		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * A method to call a RScript without to capture (print) the output.
	 * 
	 * @param path the relative path from the Bio7 Workspace to the script.
	 * 
	 */
	public static void callRScriptSilent(String path) {
		/* Get the path and convert it for R (Windows) */
		String f = FileRoot.getFileRoot() + path;

		if (Bio7Dialog.getOS().equals("Windows")) {
			f = f.replace("/", "\\");
		}
		try {
			/* Transfer path to R ! */
			RServe.getConnection().assign("fileroot", f);

			/* Call the custom Rscript ! */

			RServe.getConnection().eval("tryCatch(source(fileroot,echo=F),error = function(e) {message(paste0(\"\n\",e))})");

		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * A method to get the selected variable names in the left Objects tab in the
	 * R-Shell view (variables in the R workspace!).
	 * 
	 * @return the selected workspace variables.
	 */
	public static String[] getShellSelections() {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				RShellView shell = RShellView.getInstance();
				if (shell != null) {
					selections = shell.getListShell().getSelection();
				}
			}
		});

		return selections;
	}

	public static void closeAndDisplay() {
		Job job = new Job("Add To ImageStack") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Create Plots ...", IProgressMonitor.UNKNOWN);

				finalCloseAndDisplay();

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
		/* Wait until finished! */
		try {
			job.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void closeAndDisplayNoJoin() {
		Job job = new Job("Add To ImageStack") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Create Plots ...", IProgressMonitor.UNKNOWN);

				finalCloseAndDisplay();

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

	}

	private static void finalCloseAndDisplay() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean customDevice = store.getBoolean("USE_CUSTOM_DEVICE");
		if (customDevice == true) {

			String plotPathR;
			if (Util.getOS().equals("Windows")) {
				plotPathR = store.getString(PreferenceConstants.P_TEMP_R) + "\\";
			} else {
				plotPathR = store.getString(PreferenceConstants.P_TEMP_R) + "/";
			}
			boolean isVirtualPlot = store.getBoolean("IMPORT_R_PLOT_VIRTUAL");
			String fileName = store.getString("DEVICE_FILENAME");
			boolean useBrowser = store.getBoolean("PDF_USE_BROWSER");
			String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");

			if (fileName.endsWith("pdf") || fileName.endsWith("eps") || fileName.endsWith("xfig") || fileName.endsWith("bitmap") || fileName.endsWith("pictex")) {

				openPDF(plotPathR, fileName, useBrowser, openInJavaFXBrowser, true, false);
			}

			else if (fileName.endsWith("svg")) {
				if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows") || ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
					Program.launch(plotPathR + fileName);
				} else {
					plotLinuxSVG(plotPathR + fileName);
				}

			}

			else {
				Work.openView("com.eco.bio7.imagej");

				boolean ijCreateSingle = store.getBoolean("IMAGEJ_CREATE_SINGLE_PLOTS");
				boolean enableIjMacro = store.getBoolean("IJMACRO_EXECUTE_AFTER_PLOT_ENABLE");
				String ijmacro = store.getString("IJMACRO_EXECUTE_AFTER_PLOT");

				// System.out.println(plotPathR);

				File[] files = ListFilesDirectory(new File(plotPathR), new String[] { ".tiff", ".tif", ".jpg", ".jpeg", ".png", ".bmp" });
				if (files != null && files.length > 0) {
					if (ijCreateSingle) {

						for (int i = 0; i < files.length; i++) {
							// System.out.println(files[i].toString());
							ImagePlus plus = new ImagePlus(files[i].toString());
							plus.show();
							if (enableIjMacro) {
								IJ.runMacro(ijmacro);
							}
							files[i].delete();

						}

					} else {
						if (isVirtualPlot) {

							ImagePlus imp = FolderOpener.open(plotPathR, "virtual");
							imp.show();

							if (enableIjMacro) {
								IJ.runMacro(ijmacro);
							}

							Program.launch(plotPathR);

						} else {
							ImagePlus plu = new ImagePlus(files[0].toString());
							ImageStack stack = new ImageStack(plu.getWidth(), plu.getHeight());
							// System.out.println(files.length);
							for (int i = 0; i < files.length; i++) {
								// System.out.println(files[i].toString());
								ImagePlus plus = new ImagePlus(files[i].toString());
								stack.addSlice(plus.getProcessor());
								files[i].delete();
							}

							new ImagePlus("Plot", stack).show();

							if (enableIjMacro) {
								IJ.runMacro(ijmacro);
							}

						}
						/*
						 * else { Update Plot in same panel!
						 * 
						 * if (plotInPlace && plu != null) { for (int i = 0; i < files.length; i++) {
						 * ImagePlus impCurr = WindowManager.getCurrentImage(); ImagePlus plu2 = new
						 * ImagePlus(files[i].toString()); ImageProcessor proc2 = plu2.getProcessor();
						 * impCurr.getProcessor().resize(plu2.getWidth(), plu2.getHeight());
						 * impCurr.setProcessor(proc2); impCurr.updateAndDraw();
						 * impCurr.updateAndRepaintWindow(); files[i].delete(); }
						 * 
						 * } else {
						 * 
						 * plu = new ImagePlus(files[0].toString()); ImageStack stack = new
						 * ImageStack(plu.getWidth(), plu.getHeight()); //
						 * System.out.println(files.length); for (int i = 0; i < files.length; i++) { //
						 * System.out.println(files[i].toString()); ImagePlus plus = new
						 * ImagePlus(files[i].toString()); stack.addSlice(plus.getProcessor());
						 * files[i].delete(); }
						 * 
						 * new ImagePlus("Plot", stack).show();
						 * 
						 * if (enableIjMacro) { IJ.runMacro(ijmacro); } }
						 */
						// ImageMethods.imageToR(plu.getShortTitle(), false, 1, plu);
					}

				}

			}

		}
	}

	/**
	 * @param String plotPathR. A file path
	 * @param String fileName the file name
	 * @param        boolean useBrowser if an internal or external browser should be
	 *               used
	 * @param        boolean openInJavaFXBrowser if the JavaFX browser component
	 *               should be used
	 * @param        boolean delete if the temporary file should be deleted. Not the
	 *               copy (for pdf.js)!
	 * @param        boolean activateProjectExplorer if the Project Explorer should
	 *               be activated again (to click multiple files- used in the
	 *               Project Explorer!)
	 */
	public static void openPDF(String plotPathR, String fileName, boolean useBrowser, String openInJavaFXBrowser, boolean delete, boolean activateProjectExplorer) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		if (openInJavaFXBrowser.equals("SWT_BROWSER")) {
			File tempFile = createTempFileFromPlot(plotPathR, fileName);

			String temp = "file:////" + tempFile;
			String url = temp.replace("\\", "/");
			/* Embedded in browser! */
			if (useBrowser) {

				// String pathBundle = getPdfjsPath();
				Display display = Util.getDisplay();
				display.asyncExec(new Runnable() {

					public void run() {
						Work.openView("com.eco.bio7.browser.Browser");

						BrowserView b = BrowserView.getBrowserInstance();
						b.browser.setJavascriptEnabled(true);
						/*
						 * System.out.println(url); boolean result =
						 * b.browser.execute("var DEFAULT_URL ='" + url + "'");
						 * 
						 * System.out.println(result); b.browser.setUrl("file:///" + pathBundle + "");
						 */
						b.setLocation(url);
						if (activateProjectExplorer) {
							Work.activateView("org.eclipse.ui.navigator.ProjectExplorer");
						}
					}
				});

			} else {
				/* We use an exteranl pdf device with special rules for Linux! */
				if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows") || ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
					Display display = Util.getDisplay();
					display.asyncExec(new Runnable() {

						public void run() {
							Program.launch(tempFile.getAbsolutePath());
							if (activateProjectExplorer) {
								Work.activateView("org.eclipse.ui.navigator.ProjectExplorer");
							}

						}
					});
				} else {
					plotLinux(plotPathR + fileName);
				}
			}
		} else {

			// FilenameUtils.removeExtension(fileName);

			// boolean openInJavaFXBrowser =
			// store.getBoolean("javafxbrowser");
			// String temp = plotPathR + fileName;
			// System.out.println(openInJavaFXBrowser);

			File tempFile = createTempFileFromPlot(plotPathR, fileName);

			// String temp = "../../../com.eco.bio7/bio7temp/tempDevicePlot.pdf";
			String temp = "file:////" + tempFile;
			String url = temp.replace("\\", "/");
			String pathBundle = getPdfjsPath();

			Display display = Util.getDisplay();
			display.asyncExec(new Runnable() {

				public void run() {
					JavaFXWebBrowser br = new JavaFXWebBrowser(false);
					WebEngine webEngine = br.getWebEngine();
					// System.out.println("Path to PDF file: " + tempFile);
					/* Copy path to clipboard! */
					if (store.getBoolean("COPY_PDF_PATH_TO_CLIP")) {
						String pathClip = tempFile.toString();
						if (pathClip.length() > 0) {
							Clipboard cb = new Clipboard(display);
							TextTransfer textTransfer = TextTransfer.getInstance();
							cb.setContents(new Object[] { pathClip }, new Transfer[] { textTransfer });
						}
					}

					/*
					 * Here we use a simple but effective trick. We define the default variable
					 * 'DEFAULT_URL' in JavaScript for the viewer.js (we comment the variable out
					 * there) to load and reload local documents which won't be possible if using
					 * the path as an argument, see: https://github.com/mozilla/pdf.js/issues/5057
					 */
					webEngine.executeScript("var DEFAULT_URL =\"" + url + "\"");

					// System.out.println("Path to URL file: " + url);

					IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
					boolean openInBrowserInExtraView = store.getBoolean("OPEN_BOWSER_IN_EXTRA_VIEW");
					if (openInBrowserInExtraView) {
						String id = UUID.randomUUID().toString();
						br.createBrowser("file:///" + pathBundle + "", id);
						// br.createBrowser("file:///" + pathBundle + "?file=" + url,id);
					}

					else {

						// br.createBrowser("file:///" + pathBundle + "?file=" + url,"Display");

						br.createBrowser("file:///" + pathBundle + "", "Display");
						/*
						 * webEngine.executeScript( "alert(pdfjsVersion);");
						 */

					}
					if (activateProjectExplorer) {
						Work.activateView("org.eclipse.ui.navigator.ProjectExplorer");
					}
				}
			});
			/*
			 * Delete not if opened from embedded Bio7 editor (also when using drag and
			 * drop)!
			 */
			if (delete) {
				/* Delete file in default Bio7 temporay folder. Not the copy for pdf.js! */
				File plotFile = new File(plotPathR + fileName);
				plotFile.delete();
			}

		}
	}

	private static File createTempFileFromPlot(String plotPathR, String fileName) {
		File dirFrom = new File(plotPathR + fileName);
		File tempFile = null;
		try {
			tempFile = File.createTempFile("tempRPlotPdf", ".pdf");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		/* Delete files when finishes! */
		tempFile.deleteOnExit();
		// File dirTo = new File(plotPathR + "browserTemp"+fileName);
		try {
			FileUtils.copyFile(dirFrom, tempFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tempFile;
	}

	public static int getDisplayNumber() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean customDevice = store.getBoolean("USE_CUSTOM_DEVICE");

		int countDev = 0;
		if (customDevice) {
			REXP devList = null;
			try {
				devList = RServe.getConnection().eval("length(dev.list())");
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				countDev = devList.asInteger();
				// System.out.println(countDev);
			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				RServe.getConnection().eval("if(length(dev.list())>0) dev.off()");
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return countDev;
	}

	public static File[] ListFilesDirectory(File filedirectory, final String[] extensions) {
		File dir = filedirectory;

		// Filter the extension of the file.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(extensions[0]) || name.endsWith(extensions[1]) || name.endsWith(extensions[2]) || name.endsWith(extensions[3]) || name.endsWith(extensions[4]) || name.endsWith(extensions[5]));
			}
		};

		File[] files = dir.listFiles(filter);
		/* Sort the filenames! */
		Arrays.sort(files, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);
		return files;
	}

	private static void plotLinuxSVG(String finalpath) {
		try {

			Runtime.getRuntime().exec("inkscape " + finalpath);
		} catch (IOException e) {
			Bio7Dialog.message("Can't start Inkscape!");
		}
	}

	public static void plotLinux(String finalpath) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String pdfReader = store.getString("PDF_READER");

		if (pdfReader.equals("ACROBAT")) {
			try {

				Runtime.getRuntime().exec("acroread " + finalpath);
			} catch (IOException e) {
				Bio7Dialog.message("Can't start Acrobat Reader!");
			}
		} else if (pdfReader.equals("KPDF")) {
			try {
				Runtime.getRuntime().exec("kpdf " + finalpath);
			} catch (IOException e2) {

				Bio7Dialog.message("Can't start Kpdf!");
			}
		} else if (pdfReader.equals("XPDF")) {
			try {
				Runtime.getRuntime().exec("xpdf " + finalpath);
			} catch (IOException e2) {

				Bio7Dialog.message("Can't start Xpdf!");
			}
		} else if (pdfReader.equals("EVINCE")) {
			try {
				Runtime.getRuntime().exec("evince " + finalpath);
			} catch (IOException e2) {

				Bio7Dialog.message("Can't start Evince!");
			}

		} else if (pdfReader.equals("OKULAR")) {
			try {
				Runtime.getRuntime().exec("okular " + finalpath);
			} catch (IOException e2) {

				Bio7Dialog.message("Can't start Okular!");
			}

		}
	}

	/**
	 * A method to store the current RSession.
	 * 
	 * @param rSess the RSession
	 */
	public static void setRsession(RSession rSess) {
		rSession = rSess;

	}

	/**
	 * A method to get the current stored RSession.
	 * 
	 * @return the RSession
	 */
	public static RSession getRsession() {

		return rSession;
	}

	private static String getPdfjsPath() {
		Bundle bundle = Platform.getBundle("com.eco.bio7.libs");
		Path path = new Path("pdfjs/web/viewer.html");
		URL locationURL = FileLocator.find(bundle, path, null);

		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationURL);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String pathBundle = fileUrl.getFile();
		return pathBundle;
	}

}
