/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import ij.io.Opener;

import java.awt.EventQueue;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.Console;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;

public class PlotJob extends WorkspaceJob {

	private static boolean pdf = true;

	private static double inchx = 8;

	private static double inchy = 8;

	private static int imageheight = 1000;

	private static int imagewidth = 1000;

	private static String path;

	private String plotCommand;

	private int identifier;

	/* Evaluates R expressions from the console of Bio7 ! */
	public PlotJob() {
		super("Plot Job");

	}

	public PlotJob(String plotInstructions, int identity) {
		super("Plot Job");
		this.plotCommand = plotInstructions;
		this.identifier = identity;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Plot Data", IProgressMonitor.UNKNOWN);
		plot(plotCommand, identifier);

		return Status.OK_STATUS;
	}

	/**
	 * Internal plot method of Bio7.
	 * 
	 * @param toplot
	 *            a R expression.
	 * @param number
	 *            an integer value as the suffix for the name of the plot.
	 */
	public void plot(String toplot, int number) {
		RConnection c = RServe.getConnection();
		String finalpath = null;
		String plot = null;
		String ploter = null;
		String start = null;
		String end = null;

		deleteTempPlot();

		Bundle bundle = Platform.getBundle("com.eco.bio7");

		URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationUrl);
		} catch (IOException e2) {

			e2.printStackTrace();
		}
		File fi = new File(fileUrl.getPath());
		String pathBundle = fi.toString();
		if (Bio7Dialog.getOS().equals("Windows")) {
			path = pathBundle + "\\bio7temp\\";
			path = path.replace("\\", "\\\\");
		} else {
			path = pathBundle + "/bio7temp/";
		}

		if (pdf) {
			finalpath = path + "temp_plot" + number + ".pdf";

			plot = toplot.replace('\r', ' ');// replace LineFeed for RServe
			// !
			ploter = plot.replace(';', '\n');// replace semicolon !

			// ploter = plot.replace('#', ' ');// replace comment !

			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

				start = "try(pdf(\"" + finalpath + "\",width=" + inchx + ",height=" + inchy + "));";
			}

			else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
				start = "pdf(\"" + "/" + finalpath + "\",width=" + inchx + ",height=" + inchy + ");";
			} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
				start = "try(pdf(\"" + "/" + finalpath + "\",width=" + inchx + ",height=" + inchy + "));";
			}

			end = "\ndev.off();";
		}

		else {

			plot = toplot.replace('\r', ' ');// replace LineFeed for RServe
			ploter = plot.replace(';', '\n');// replace semicolon !
			// ploter = plot.replace('#', ' ');// replace comment !

			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				finalpath = path + "temp_plot" + number + ".png";
				start = "png(\"" + finalpath + "\",width=" + imagewidth + ",height=" + imageheight + ");";
			} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
				finalpath = path + "temp_plot" + number + ".png";
				start = "png(\"" + "/" + finalpath + "\",width=" + imagewidth + ",height=" + imageheight + ");";
			} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
				finalpath = path + "temp_plot" + number + ".jpeg";
				start = "jpeg(\"" + "/" + finalpath + "\",width=" + imagewidth + ",height=" + imageheight + ",quality=100);";
			}
			end = "\ndev.off();";

		}
		saveScript(start + ploter + end);

		callRPlotScript(path + "tempPlot.R");
		if (Bio7Dialog.getOS().equals("Windows")) {
			try {
				c.eval("try(\ndev.off());");
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (Bio7Dialog.getOS().equals("Windows")) {
			plotResult(finalpath);
		} else if (Bio7Dialog.getOS().equals("Linux")) {
			plotResultLinux(finalpath);

		} else if (Bio7Dialog.getOS().equals("Mac")) {
			plotResultLinux(finalpath);

		}
	}

	private void plotResult(String finalpath) {
		if (pdf) {

			try {
				Program.launch(finalpath);
			} catch (RuntimeException e) {

				e.printStackTrace();
			}
		} else {

			final File pathfinal = new File(finalpath);

			if (com.eco.bio7.image.CanvasView.getCanvas_view() != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {

						Opener o = new Opener();

						o.open(pathfinal.toString());

					}
				});
			} else {
				MessageDialog.openWarning(new Shell(), "ImageJ", "You probably closed all ImageJ-Views");

			}

		}
	}

	private void plotResultLinux(String finalpath) {
		String defaultPdf = Messages.getString("pdfdevice.1");
		int defaultPdfApp = Integer.parseInt(defaultPdf);
		if (pdf) {
			if (defaultPdfApp == 1) {
				try {

					Runtime.getRuntime().exec("acroread " + finalpath);
				} catch (IOException e) {
					Bio7Dialog.message("Can't start Acrobat Reader!");
				}
			} else if (defaultPdfApp == 2) {
				try {
					Runtime.getRuntime().exec("kpdf " + finalpath);
				} catch (IOException e2) {

					Bio7Dialog.message("Can't start Kpdf!");
				}
			} else if (defaultPdfApp == 3) {
				try {
					Runtime.getRuntime().exec("xpdf " + finalpath);
				} catch (IOException e2) {

					Bio7Dialog.message("Can't start Xpdf!");
				}
			} else {
				try {
					Runtime.getRuntime().exec("evince " + finalpath);
				} catch (IOException e2) {

					Bio7Dialog.message("Can't starte Evince!");
				}
			}
		} else {

			final File pathfinal = new File(finalpath);

			if (com.eco.bio7.image.CanvasView.getCanvas_view() != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {

						Opener o = new Opener();

						o.open(pathfinal.toString());

					}
				});
			} else {
				MessageDialog.openWarning(new Shell(), "ImageJ", "You probably closed all ImageJ-Views");

			}

		}
	}

	protected void deleteTempPlot() {
		File files = scriptsInDirectory("bio7temp");
		File[] fil = ListFileDirectory(files);

		if (fil.length > 0) {
			for (int i = 0; i < fil.length; i++) {

				new File(fil[i].getAbsolutePath()).delete();

			}

		}
	}

	protected File[] ListFileDirectory(File file) {
		File dir = file;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of file or directory
				String filename = children[i];
			}
		}

		// Filter the extension
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(""));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}

	protected File scriptsInDirectory(String dir) {
		File file = null;
		Bundle bundle = Platform.getBundle("com.eco.bio7");

		URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationUrl);
		} catch (IOException e2) {

			e2.printStackTrace();
		}
		String path = fileUrl.getFile();

		if (dir.equals("scripts")) {
			file = new File(path + "/scripts");
		}

		else if (dir.equals("import")) {
			file = new File(path + "/importscripts");
		} else if (dir.equals("export")) {
			file = new File(path + "/exportscripts");
		} else if (dir.equals("bio7temp")) {
			file = new File(path + "/bio7temp");
		}
		return file;
		// lists all scripts in the directory

	}

	private void saveScript(String plot) {

		File fil = new File(path + "tempPlot.R");
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(fil);

			BufferedWriter buffWriter = new BufferedWriter(fileWriter);

			buffWriter.write(plot, 0, plot.length());
			buffWriter.close();
		} catch (IOException ex) {

		} finally {
			try {
				fileWriter.close();
			} catch (IOException ex) {

			}
		}

	}

	/*
	 * Method to create a plot file which will be opened with the selected software (ImageJ or a Pdf-viewer)
	 */
	private void callRPlotScript(String path) {
		/* Get the path and convert it for R (Windows) */
		String f = path;

		/*
		 * We have to replace "/" to "\\" ! For the use in Linux comment the following line out!
		 */

		try {
			/* Transfer path to R ! */
			RServe.getConnection().assign("fileroot", f);

			/* Call the custom Rscript ! */
			String rout = null;
			try {
				rout = RServe.getConnection().eval("" + "try(paste(capture.output(source(fileroot,echo=T)),collapse=\"\\n\"))").asString();
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

		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Set the plot size in inch for a pdf plot.
	 * 
	 * @param inchx
	 *            the width of the plot in inch units.
	 * @param inchy
	 *            the height of the plot in inch units.
	 */
	public static void setPlotInch(double inchx, double inchy) {
		PlotJob.inchx = inchx;
		PlotJob.inchy = inchy;
	}

	/**
	 * Set the plot size in pixel for a png plot.
	 * 
	 * @param imageWidth
	 *            the width of the plot in pixel units.
	 * @param imageHeight
	 *            the height of the plot in pixel units.
	 */
	public static void setPlotPixel(int imageWidth, int imageHeight) {
		imagewidth = imageWidth;
		imageheight = imageHeight;
	}

	/**
	 * Returns if the plotting device is the pdf device.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isPdf() {
		return pdf;
	}

	/**
	 * Set the plotting device to pdf.
	 * 
	 * @param pdf
	 *            a boolean value.
	 */
	public static void setPdf(boolean pdf) {
		PlotJob.pdf = pdf;
	}

}
