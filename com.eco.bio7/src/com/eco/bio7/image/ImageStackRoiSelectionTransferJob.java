package com.eco.bio7.image;

import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
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
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.util.Util;

public class ImageStackRoiSelectionTransferJob extends WorkspaceJob implements IJobChangeListener {

	protected int selection;
	protected int response;
	private static String input;
	protected InputDialog inp;
	private static int setSigNumber;
	private static boolean doSignature;
	private static int minStackSlice;
	private static int maxStackSlice;
	public static boolean cancelJob = false;
	private static String RCode;
	private static boolean enableRCode;
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
	private int currentStackSize;

	public ImageStackRoiSelectionTransferJob(int selectedImageType) {
		super("Transfer progress....");
		this.transferType = selectedImageType;
		
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Transfer image pixels.....", IProgressMonitor.UNKNOWN);

		transfer();

		return Status.OK_STATUS;
	}

	public static void setMinStackSlice(int minStackSlice) {
		ImageStackRoiSelectionTransferJob.minStackSlice = minStackSlice;

	}

	public static void setMaxStackSlice(int maxStackSlice) {
		ImageStackRoiSelectionTransferJob.maxStackSlice = maxStackSlice;

	}

	public static void setSignatureNumber(int input2) {
		ImageStackRoiSelectionTransferJob.setSigNumber = input2;
	}

	public static void doSetSignature(boolean sel) {
		doSignature = sel;

	}

	public static void enableRCodeAfterSlice(boolean selection2) {
		ImageStackRoiSelectionTransferJob.enableRCode = selection2;

	}

	public static void setRCodeAfterSlice(String text) {
		ImageStackRoiSelectionTransferJob.RCode = text;

	}

	public void transfer() {

		if (transferType == 3) {
			Bio7Dialog.message("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the(slice) selection in e.g. byte mode!");
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
				currentStackSize = impd.getStackSize();
				if ((impd.getStackSize()) <= 1) {
					Bio7Dialog.message("Please select a stack with more than one image!");
					return;
				}

				Display.getDefault().syncExec(new Runnable() {

					public void run() {
						ImageRoiStackSelectionInputDialog in = new ImageRoiStackSelectionInputDialog(Util.getShell());
						in.open();

					}
				});

				if (cancelJob == false) {
					/* Get the image processor of the image ! */
					ImageProcessor ipSize = impd.getProcessor();
					int w = ipSize.getWidth();
					int h = ipSize.getHeight();

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
						for (int i = minStackSlice; i <= maxStackSlice; i++) {
							count = i;

							ImageStack stack = impd.getImageStack();
							/* Get the image processor of the image ! */
							ImageProcessor ip = stack.getProcessor(i);
							ip.setRoi(roiDefault);

							// imp.setRoi(roiDefault);
							/*if (roiDefault != null && !roiDefault.isArea()) {
								Bio7Dialog.message("The command require\n" + "an area selection, or no selection.");
								return;
							}*/

							if (transferType == 0) {
								valuesDouble = getROIPixelsDouble(ip, roiDefault);
								// System.out.println(valuesDouble.size());
								valD = new double[valuesDouble.size()];
								for (int u = 0; u < valD.length; u++) {
									valD[u] = valuesDouble.get(u);
								}
								l = "slice" + i;
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
								valuesInt = getROIPixelsInteger(ip, roiDefault);
								valI = new int[valuesInt.size()];
								for (int u = 0; u < valI.length; u++) {
									valI[u] = valuesInt.get(u);
								}
								l = "slice" + i;
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
								valuesByte = getROIPixelsByte(ip, roiDefault);
								valB = new byte[valuesByte.size()];
								for (int u = 0; u < valB.length; u++) {
									valB[u] = valuesByte.get(u);
								}
								l = "slice" + i;
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
					Bio7Dialog.message("Selected Pixels transferred to R!");
					impd = null;
				} else {
					return;
				}
			} else {

				return;// Exit the method!
			}
		} else {
			/* If ROI Manager isn't active! */

			Bio7Dialog.message("Please add selections to the ROI Manager!");

		}

	}

	
	private ArrayList<Double> getROIPixelsDouble(ImageProcessor ip, Roi roi) {
		
		ArrayList<Double> values = new ArrayList<Double>();
		for (Point p : roi.getContainedPoints()) {
			
			values.add(new Double(ip.getPixelValue(p.x, p.y)));
		}
		return values;
	}
	
	private ArrayList<Integer> getROIPixelsInteger(ImageProcessor ip, Roi roi) {
		
		ArrayList<Integer> values = new ArrayList<Integer>();
		for (Point p : roi.getContainedPoints()) {
			
			values.add(new Integer(ip.getPixel(p.x, p.y)));
		}
		return values;
	}
	
	private ArrayList<Byte> getROIPixelsByte(ImageProcessor ip, Roi roi) {
		ArrayList<Byte> values = new ArrayList<Byte>();
		for (Point p : roi.getContainedPoints()) {
			
			values.add(new Byte((byte) (ip.getPixel(p.x, p.y))));
		}
		return values;
	}
	/*private ArrayList<Double> getROIPixelsDouble(ImageProcessor ip, Roi roi) {
		// ImageProcessor ip = ipr;
		ImageProcessor mask = roi != null ? roi.getMask() : null;
		Rectangle r = roi != null ? roi.getBounds() : new Rectangle(0, 0, ip.getWidth(), ip.getHeight());

		int count = 0;
		ArrayList<Double> values = new ArrayList<Double>();
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				if (mask == null || mask.getPixel(x, y) != 0) {
					count++;
                  
					// ip.set(x + r.x, y + r.y, 0);
					values.add(new Double(ip.getPixelValue(x + r.x, y + r.y)));
				}
			}
		}
		return values;
	}*/

	/*private ArrayList<Integer> getROIPixelsInteger(ImageProcessor ip, Roi roi) {
		// ImageProcessor ip = imp.getProcessor();
		ImageProcessor mask = roi != null ? roi.getMask() : null;
		Rectangle r = roi != null ? roi.getBounds() : new Rectangle(0, 0, ip.getWidth(), ip.getHeight());

		int count = 0;
		ArrayList<Integer> values = new ArrayList<Integer>();
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				if (mask == null || mask.getPixel(x, y) != 0) {
					count++;

					// ip.set(x + r.x, y + r.y, 0);
					values.add(new Integer(ip.getPixel(x + r.x, y + r.y)));
				}
			}
		}
		return values;
	}*/

	/*private ArrayList<Byte> getROIPixelsByte(ImageProcessor ip, Roi roi) {
		// ImageProcessor ip = imp.getProcessor();
		ImageProcessor mask = roi != null ? roi.getMask() : null;
		Rectangle r = roi != null ? roi.getBounds() : new Rectangle(0, 0, ip.getWidth(), ip.getHeight());

		int count = 0;
		ArrayList<Byte> values = new ArrayList<Byte>();
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				if (mask == null || mask.getPixel(x, y) != 0) {
					count++;

					// ip.set(x + r.x, y + r.y, 0);
					values.add(new Byte((byte) (ip.getPixel(x + r.x, y + r.y))));
				}
			}
		}
		return values;
	}*/

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
		//IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		//String st = store.getString(PreferenceConstants.D_OPENOFFICE_HEAD);
		//String[] a = st.split(",");

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
