/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.ApplicationActionBarAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.time.Time;

class PcaJob extends WorkspaceJob {
	private static int cluster = 1;
	private static String[] imagesToCluster;
	private static boolean doCluster = false;
	private static boolean onlyTransferImages;
	private static String textOptions;

	public static boolean isDoCluster() {
		return doCluster;
	}

	protected int selectionType;

	public PcaJob() {

		super("PCA");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		if (doCluster) {
			if (onlyTransferImages == false) {
				monitor.beginTask("PCA  Analysis", imagesToCluster.length + 2);
			} else {
				monitor.beginTask("Image transfer", imagesToCluster.length);
			}

			clusterImage(monitor);
		}
		return Status.OK_STATUS;
	}

	public void clusterImage(IProgressMonitor monitor) {
		/* Get the data type selection from the combo! */

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				selectionType = RImageMethodsView.getTransferTypeCombo().getSelectionIndex();
			}
		});

		RConnection c = RServe.getConnection();
		if (c != null) {
			long time = System.currentTimeMillis();
			ImagePlus imp = null;
			try {
				c.eval("clusterData<-NULL");
			} catch (RserveException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			int count = 1;
			for (int i = 0; i < imagesToCluster.length; i++) {

				monitor.setTaskName("Transfer image " + count + " to R");
				imp = WindowManager.getImage(imagesToCluster[i]);
				if (imp != null) {

					/* We transfer the necesssary data type for clustering! */
					ImageMethods.imagePlusToR(imp, "imageData", false, selectionType);
					if (onlyTransferImages == false) {
						monitor.setTaskName("Create data frame!");
						try {
							/* Double data and integer data can be clustered! */
							REXPLogical bol = (REXPLogical) c.eval("try(is.double(imageData))");
							boolean[] bo = bol.isTrue();
							if (bo[0]) {

								c.eval("clusterData<-cbind(clusterData,imageData)");
							} else {
								c.eval("try(data<-as.integer(imageData))");
								c.eval("clusterData<-cbind(clusterData,as.integer(data))");
								c.eval("try(remove(data))");
							}
							c.eval("try(remove(imageData));gc();");
							if (i == 0) {
								// c.eval("clusterData[1]<-NULL");
							}
						} catch (RserveException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} else {
						try {
							c.eval("try(image" + count + "<-imageData)");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
				monitor.worked(1);
				count++;
			}
			if (onlyTransferImages == false) {
				monitor.setTaskName("Analyse the transferred data");
				try {
					c.eval("try(remove(imageData));gc();");

				} catch (RserveException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				try {

					c.eval("try(pcaData <- prcomp(clusterData," + textOptions + "))");
					// c.eval(
					// "imageCluster<-matrix(clarax$cluster,imageSizeX,imageSizeY)"
					// );
					for (int i = 1; i <= cluster; i++) {

						c.eval("try(PCAComponent" + i + "<-pcaData$x[," + i + "])");
						monitor.setTaskName("Create image(s) from transferred data");
						// Transfer back as float and byte transfer!
						ImageMethods.imageFromR(2, "PCAComponent" + i + "", 0);
					}

					
					printInR("summary(pcaData)");
					System.out.println("\n");
					c.eval("try(remove(pcaData));gc();");
				} catch (RserveException e1) {

					e1.printStackTrace();
				}

				monitor.worked(1);
				try {

					c.eval("try(remove(clusterData))");
					// c.eval("remove(imageCluster)");
				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				monitor.worked(1);
				final double timeOut = (double) (System.currentTimeMillis() - time) / 1000;

				System.out.println("Image(s) analysed in: (seconds) " + timeOut);
				doCluster = false;
			} else {
				monitor.worked(1);
			}
		}

	}

	/* Necessary because R is blocked in a Job! */
	private void printInR(String expression) {
		String rout = null;
		try {
			rout = RServe.getConnection().eval("try(paste(capture.output(print(" + expression + ")),collapse=\"\\n\"))").asString();
		} catch (REXPMismatchException e) {

			e.printStackTrace();
		} catch (RserveException e) {

			e.printStackTrace();
		}

		StartBio7Utils.getConsoleInstance().cons.println(rout);
		/* Send also the output to the R console view! */
		if (RShellView.isConsoleExpanded()) {
			RShellView.setTextConsole(rout);
		}

	}

	public static void setCluster(int selection) {
		cluster = selection;

	}

	public static void setClusterImages(String[] items) {
		imagesToCluster = items;

	}

	public static void doClusterImages(boolean b) {
		doCluster = b;

	}

	public static void onlyImageTransfer(boolean selection) {
		onlyTransferImages = selection;

	}

	public static void setTextOptions(String text) {
		textOptions = text;

	}

}
