package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Vector;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.util.Util;

public class ImageRoiSelectionTransferJob extends WorkspaceJob implements IJobChangeListener {

	protected int selection;
	protected int response;
	private static String input;
	protected InputDialog inp;
	private static int setSigNumber;
	private static boolean doSignature;
	public static boolean cancelJob = false;
	protected InputDialog inp2;
	private CTabItem[] items;
	protected String title;
	private int count;
	private ArrayList<Double> valuesDouble;
	private ArrayList<Integer> valuesInt;
	private ArrayList<Byte> valuesByte;
	private double[] valD;
	private int[] valI;
	private byte[] valB;
	private int transferType = 0;
	private String l;
	private String[] firstChar = { ".", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
	protected ImagePlus imp;

	public ImageRoiSelectionTransferJob(int selectedImageType) {
		super("Transfer progress....");
		this.transferType = selectedImageType;
		
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Transfer image pixels.....", IProgressMonitor.UNKNOWN);

		transfer();

		return Status.OK_STATUS;
	}

	public static void setInput(String input) {
		ImageRoiSelectionTransferJob.input = input;
	}

	public static void setSignatureNumber(int input2) {
		ImageRoiSelectionTransferJob.setSigNumber = input2;
	}

	public static void doSetSignature(boolean sel) {
		doSignature = sel;

	}

	public void transfer() {

		if (transferType == 3) {
			Bio7Dialog.message("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the(layer) selection in e.g. byte mode!");
			return;
		}

		RConnection connection = RServe.getConnection();

		if (RoiManager.getInstance() != null) {
			Roi[] r = RoiManager.getInstance().getSelectedRoisAsArray();
			if(r.length<1){
				Bio7Dialog.message("NO ROI's available in ROI Manager!");
				return;
			}
			if (RServe.isAliveDialog()) {
				ImagePlus impd = WindowManager.getCurrentImage();
				if ((impd.getStackSize()) > 1) {
					Bio7Dialog.message("Info: For a stack only the first slice (layer)\n" + "will be measured for all ROI's in the ROI Manager!");
				}

				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						ImageRoiSelectionInputDialog in = new ImageRoiSelectionInputDialog(Util.getShell());
						in.open();

					}
				});

				if (cancelJob == false) {

					/* Get the image processor of the image ! */
					ImageProcessor ip = impd.getProcessor();
					int w = ip.getWidth();
					int h = ip.getHeight();

					try {
						connection.eval("imageSizeY<-" + h);

						connection.eval("imageSizeX<-" + w);

					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					/*
					 * Only transfer selected ROI's from the ROI Manager or all
					 * if no ROI is selected!
					 */
					//Roi[] r = RoiManager.getInstance().getSelectedRoisAsArray();
					items = CanvasView.getCanvas_view().tabFolder.getItems();
					for (int ro = 0; ro < r.length; ro++) {

						Roi roiDefault = r[ro];
						String roiName = r[ro].getName();
						roiName = correctChars(roiName);
						try {
							connection.eval("try(" + roiName + "<-NULL)");

						} catch (RserveException e2) {
							// TODO Auto-generated catch block
							Bio7Dialog.message("Please select a proper name for the ROI's in the ROI Manager\n" + "Some special chars are not supported for the transfer to R!");
							return;
						}
						/* The amount of open Tabs! */
						for (int i = 0; i < items.length; i++) {
							count = i;
							Display display = PlatformUI.getWorkbench().getDisplay();
							display.syncExec(new Runnable() {

								public void run() {
									title = items[count].getText();
									Vector<?> ve = (Vector<?>) items[count].getData();
									/*
									 * More secure to use the ImagePlus from the
									 * tabs!
									 */
									imp = (ImagePlus) ve.get(0);
								}
							});
							// ImagePlus imp = WindowManager.getImage(title);
							imp.setRoi(roiDefault);
							/*if (roiDefault != null && !roiDefault.isArea()) {
								Bio7Dialog.message("The command requires\n" + "an area selection, or no selection.");
								return;
							}*/

							if (transferType == 0) {
								valuesDouble = getROIPixelsDouble(imp, roiDefault);
								// System.out.println(valuesDouble.size());
								valD = new double[valuesDouble.size()];
								for (int u = 0; u < valD.length; u++) {
									valD[u] = valuesDouble.get(u);
								}
								l = "layer" + (i + 1);
								try {
									connection.assign(l, valD);
								} catch (REngineException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

								valuesDouble.clear();
								valD = null;
								try {
									connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
									connection.eval("remove(" + l + ")");
								} catch (RserveException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							} else if (transferType == 1) {
								valuesInt = getROIPixelsInteger(imp, roiDefault);
								valI = new int[valuesInt.size()];
								for (int u = 0; u < valI.length; u++) {
									valI[u] = valuesInt.get(u);
								}
								l = "layer" + (i + 1);
								try {
									connection.assign(l, valI);
								} catch (REngineException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

								valuesInt.clear();
								valI = null;
								try {
									connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
									connection.eval("remove(" + l + ")");
								} catch (RserveException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

							} else if (transferType == 2) {
								valuesByte = getROIPixelsByte(imp, roiDefault);
								valB = new byte[valuesByte.size()];
								for (int u = 0; u < valB.length; u++) {
									valB[u] = valuesByte.get(u);
								}
								l = "layer" + (i + 1);
								try {
									connection.assign(l, valB);
								} catch (REngineException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

								valuesByte.clear();
								valB = null;
								try {
									connection.eval("try(" + roiName + "<-cbind(" + roiName + "," + l + "))");
									connection.eval("remove(" + l + ")");
								} catch (RserveException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}

						}

						if (transferType == 0) {
							try {
								if (doSignature) {
									connection.eval("" + roiName + "<-cbind(rep(c(" + setSigNumber + "),length(" + roiName + "[,1]))," + roiName + ")");
								} else {
									connection.eval("" + roiName + "<-cbind(rep(c(" + (ro + 1) + "),length(" + roiName + "[,1]))," + roiName + ")");
								}
								connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
							} catch (RserveException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						} else if (transferType == 1) {
							try {
								if (doSignature) {
									connection.eval("" + roiName + "<-cbind(rep(c(as.integer(" + setSigNumber + ")),length(" + roiName + "[,1]))," + roiName + ")");
								} else {
									connection.eval("" + roiName + "<-cbind(rep(c(as.integer(" + (ro + 1) + ")),length(" + roiName + "[,1]))," + roiName + ")");
								}
								connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
							} catch (RserveException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						} else if (transferType == 2) {
							try {
								if (doSignature) {
									connection.eval("" + roiName + "<-cbind(rep(c(as.raw(" + setSigNumber + ")),length(" + roiName + "[,1]))," + roiName + ")");
								} else {
									connection.eval("" + roiName + "<-cbind(rep(c(as.raw(" + (ro + 1) + ")),length(" + roiName + "[,1]))," + roiName + ")");
								}
								connection.eval("colnames(" + roiName + ")[1] <- \"Class\"");
							} catch (RserveException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						}

					}
					imp = null;
				}
			} else {

				return;// Exit the method!
			}
		} else {
			Bio7Dialog.message("The ROI Manager is not available!\nPlease add selections to the ROI manager" + "to transfer the \nselected pixel regions from one or multiple opened images!");
		}

	}

	private ArrayList<Double> getROIPixelsDouble(ImagePlus imp, Roi roi) {
		ImageProcessor ip = imp.getProcessor();
		ArrayList<Double> values = new ArrayList<Double>();
		for (Point p : roi.getContainedPoints()) {

			values.add(new Double(ip.getPixelValue(p.x, p.y)));
		}
		return values;
	}

	private ArrayList<Integer> getROIPixelsInteger(ImagePlus imp, Roi roi) {
		ImageProcessor ip = imp.getProcessor();
		ArrayList<Integer> values = new ArrayList<Integer>();
		for (Point p : roi.getContainedPoints()) {

			values.add(new Integer(ip.getPixel(p.x, p.y)));
		}
		return values;
	}

	private ArrayList<Byte> getROIPixelsByte(ImagePlus imp, Roi roi) {
		ImageProcessor ip = imp.getProcessor();
		ArrayList<Byte> values = new ArrayList<Byte>();
		for (Point p : roi.getContainedPoints()) {

			values.add(new Byte((byte) (ip.getPixel(p.x, p.y))));
		}
		return values;
	}

	/*
	 * private ArrayList<Double> getROIPixelsDouble(ImagePlus imp, Roi roi) {
	 * ImageProcessor ip = imp.getProcessor(); ImageProcessor mask = roi != null
	 * ? roi.getMask() : null; Rectangle r = roi != null ? roi.getBounds() : new
	 * Rectangle(0, 0, ip.getWidth(), ip.getHeight());
	 * 
	 * int count = 0; ArrayList<Double> values = new ArrayList<Double>(); for
	 * (int y = 0; y < r.height; y++) { for (int x = 0; x < r.width; x++) { if
	 * (mask == null || mask.getPixel(x, y) != 0) { count++;
	 * 
	 * // ip.set(x + r.x, y + r.y, 0); values.add(new Double(ip.getPixelValue(x
	 * + r.x, y + r.y))); } } } return values; }
	 * 
	 * private ArrayList<Integer> getROIPixelsInteger(ImagePlus imp, Roi roi) {
	 * ImageProcessor ip = imp.getProcessor(); ImageProcessor mask = roi != null
	 * ? roi.getMask() : null; Rectangle r = roi != null ? roi.getBounds() : new
	 * Rectangle(0, 0, ip.getWidth(), ip.getHeight());
	 * 
	 * int count = 0; ArrayList<Integer> values = new ArrayList<Integer>(); for
	 * (int y = 0; y < r.height; y++) { for (int x = 0; x < r.width; x++) { if
	 * (mask == null || mask.getPixel(x, y) != 0) { count++;
	 * 
	 * // ip.set(x + r.x, y + r.y, 0); values.add(new Integer(ip.getPixel(x +
	 * r.x, y + r.y))); } } } return values; }
	 * 
	 * private ArrayList<Byte> getROIPixelsByte(ImagePlus imp, Roi roi) {
	 * ImageProcessor ip = imp.getProcessor(); ImageProcessor mask = roi != null
	 * ? roi.getMask() : null; Rectangle r = roi != null ? roi.getBounds() : new
	 * Rectangle(0, 0, ip.getWidth(), ip.getHeight());
	 * 
	 * int count = 0; ArrayList<Byte> values = new ArrayList<Byte>(); for (int y
	 * = 0; y < r.height; y++) { for (int x = 0; x < r.width; x++) { if (mask ==
	 * null || mask.getPixel(x, y) != 0) { count++;
	 * 
	 * // ip.set(x + r.x, y + r.y, 0); values.add(new Byte((byte) (ip.getPixel(x
	 * + r.x, y + r.y)))); } } } return values; }
	 */

	public void aboutToRun(IJobChangeEvent event) {

	}

	public void awake(IJobChangeEvent event) {

	}

	public void done(IJobChangeEvent event) {

	}

	public void running(IJobChangeEvent event) {

	}

	public void scheduled(IJobChangeEvent event) {

	}

	public void sleeping(IJobChangeEvent event) {

	}

	public String correctChars(String name) {
		/*
		 * IPreferenceStore store =
		 * Bio7Plugin.getDefault().getPreferenceStore(); String st =
		 * store.getString(PreferenceConstants.D_OPENOFFICE_HEAD); String[] a =
		 * st.split(",");
		 */

		/*
		 * Replace the comma since it is the split argument!
		 */
		name = name.replace(",", ".");

		name = name.replace("-", "_");

		for (int j = 0; j < firstChar.length; j++) {

			if (name.startsWith(firstChar[j])) {
				name = name.replaceFirst(firstChar[j], "X" + firstChar[j]);
			}
		}

		return name;

	}

}
