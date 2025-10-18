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
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rcp.ApplicationActionBarAdvisor;
import com.eco.bio7.time.Time;

class ClusterJob extends WorkspaceJob {
	private static int cluster = 2;
	private static String[] imagesToCluster;
	private static boolean doCluster = false;
	private static boolean onlyTransferImages;
	private static String textOptions;
	private static String clusterAlgor;

	public static boolean isDoCluster() {
		return doCluster;
	}

	protected int selectionType;

	public ClusterJob() {

		super("Cluster Image");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		if (doCluster) {
			if (onlyTransferImages == false) {
				monitor.beginTask("Cluster Image", (imagesToCluster.length) + 2);
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
				/* Create an empty matrix! */
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

								c.eval("try(clusterData<-cbind(clusterData,imageData))");
							} else {
								c.eval("try(clusterData<-cbind(clusterData,as.integer(imageData)))");
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
							c.eval("image" + count + "<-imageData");
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
				monitor.setTaskName("Clustering the transferred data");
				try {
					c.eval("try(remove(imageData));gc();");

				} catch (RserveException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				if (clusterAlgor.equals("clara")) {
					try {
						c.eval("library(cluster)");
						System.out.println(
								"Executing: clara_result <- clara(clusterData," + cluster + "," + textOptions + ")");
						c.eval("try(clara_result <- clara(clusterData," + cluster + "," + textOptions + "))");
						c.eval("try(imageCluster<-as.raw(clara_result$cluster))");
						c.eval("remove(clusterData)");
						c.eval("gc();");
					} catch (RserveException e1) {

						e1.printStackTrace();
					}
				} else if (clusterAlgor.equals("kmeans")) {
					try {
						System.out.println(
								"Executing: kmeans_result <- kmeans(clusterData," + cluster + "," + textOptions + ")");
						c.eval("try(kmeans_result <- kmeans(clusterData," + cluster + "," + textOptions + "))");
						c.eval("try(imageCluster<-as.raw(kmeans_result$cluster))");
						c.eval("remove(clusterData)");
						c.eval("gc();");
					} catch (RserveException e1) {

						e1.printStackTrace();
					}
				} 
				monitor.worked(1);
				monitor.setTaskName("Create image from transferred data");
				// Transfer back as short and byte transfer!
				ImageMethods.imageFromR(3, "imageCluster", 1);
				try {

					c.eval("remove(imageCluster);gc();");
				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				monitor.worked(1);
				final double timeOut = (double) (System.currentTimeMillis() - time) / 1000;

				System.out.println("Image(s) clustered in: (seconds) " + timeOut);
				doCluster = false;
			} else {
				monitor.worked(1);
			}
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

	public static void setAlgorSelection(String selectedAlgor) {
		clusterAlgor = selectedAlgor;

	}

}
