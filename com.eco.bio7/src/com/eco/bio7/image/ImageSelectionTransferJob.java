package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.process.ImageProcessor;
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

public class ImageSelectionTransferJob extends WorkspaceJob implements IJobChangeListener {

	protected int selection;
	protected int response;
	private static String input;
	protected InputDialog inp;
	private static int input2;
	private static boolean selectionSig;
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

	public ImageSelectionTransferJob(int selectedImageType) {
		super("Transfer progress....");
		this.transferType = selectedImageType;
		System.out.println(transferType);
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Transfer image pixels.....", IProgressMonitor.UNKNOWN);

		transfer();

		return Status.OK_STATUS;
	}

	public static void setInput(String input) {
		ImageSelectionTransferJob.input = input;
	}

	public static void setInput2(int input2) {
		ImageSelectionTransferJob.input2 = input2;
	}

	public static void setSignature(boolean sel) {
		selectionSig = sel;

	}

	public void transfer() {

		if (transferType == 3) {
			Bio7Dialog.message("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the(layer) selection in e.g. byte mode!");
			return;
		}

		// RConnection connection = RServe.getConnection();

		/*
		 * Standard procedure for single layer selections or multiple layer
		 * selections with or without signature!
		 */

		// int imageCount = WindowManager.getImageCount();
		ImagePlus impDefault = WindowManager.getCurrentWindow().getImagePlus();
		Roi roiDefault = impDefault.getRoi();
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				/* Get transfer type from combo selection! */
				transferType = ImageMethods.getTransferTypeCombo().getSelectionIndex();
				/*
				 * MessageBox message = new MessageBox(new Shell(),
				 * SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				 * message.setMessage("From all open images ?");
				 * message.setText("All images"); response = message.open();
				 */
			}
		});
		// if (response == SWT.YES) {

		items = CanvasView.getCanvas_view().tabFolder.getItems();
		RConnection con = RServe.getConnection();

		if (RServe.isAliveDialog()) {

			display.syncExec(new Runnable() {

				public void run() {
					ImageSelectionInputDialog in = new ImageSelectionInputDialog(new Shell());
					in.open();

					/*
					 * inp = new InputDialog(new Shell(), "To R",
					 * "Create a name for the matrix!", "matrix", null); input =
					 * "m";
					 * 
					 * if (inp.open() == Dialog.OK) { input = inp.getValue();
					 * 
					 * }
					 */
				}
			});

			int sig = input2;

			try {
				con.eval("" + input + "<-NULL");

			} catch (RserveException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			for (int i = 0; i < items.length; i++) {
				count = i;
				display.syncExec(new Runnable() {

					public void run() {
						title = items[count].getText();
						Vector<?> ve = (Vector<?>) items[count].getData();
						/* More secure to use the ImagePlus from the tabs! */
						imp = (ImagePlus) ve.get(0);
					}
				});

				// ImagePlus imp = WindowManager.getImage(title);
				imp.setRoi(roiDefault);
				if (roiDefault != null && !roiDefault.isArea()) {
					Bio7Dialog.message("The command require\n" + "an area selection, or no selection.");
					return;
				}
				if (transferType == 0) {
					valuesDouble = getROIPixelsDouble(imp, roiDefault);

					valD = new double[valuesDouble.size()];
					for (int u = 0; u < valD.length; u++) {
						valD[u] = valuesDouble.get(u);
					}
					l = "layer" + (i + 1);
					try {
						con.assign(l, valD);
					} catch (REngineException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					valuesDouble.clear();
					valD = null;
					try {
						con.eval("try(" + input + "<-cbind(" + input + "," + l + "))");
						con.eval("remove(" + l + ")");
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
						con.assign(l, valI);
					} catch (REngineException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					valuesInt.clear();
					valI = null;
					try {
						con.eval("try(" + input + "<-cbind(" + input + "," + l + "))");
						con.eval("remove(" + l + ")");
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
						con.assign(l, valB);
					} catch (REngineException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					valuesByte.clear();
					valB = null;
					try {
						con.eval("try(" + input + "<-cbind(" + input + "," + l + "))");
						con.eval("remove(" + l + ")");
					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

			}
			// System.out.println(selectionSig);
			if (selectionSig) {

				if (transferType == 0) {
					try {
						con.eval("" + input + "<-cbind(rep(c(" + sig + "),length(" + input + "[,1]))," + input + ")");
						con.eval("colnames(" + input + ")[1] <- \"Class\"");
					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				} else if (transferType == 1) {
					try {
						con.eval("" + input + "<-cbind(rep(c(as.integer(" + sig + ")),length(" + input + "[,1]))," + input + ")");
						con.eval("colnames(" + input + ")[1] <- \"Class\"");
					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				} else if (transferType == 2) {
					try {
						con.eval("" + input + "<-cbind(rep(c(as.raw(" + sig + ")),length(" + input + "[,1]))," + input + ")");
						con.eval("colnames(" + input + ")[1] <- \"Class\"");
					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}

			}
		}

		Bio7Dialog.message("Selected Pixels transferred to R!");
		imp = null;
	}

	private ArrayList<Double> getROIPixelsDouble(ImagePlus imp, Roi roi) {
		ImageProcessor ip = imp.getProcessor();
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
	}

	private ArrayList<Integer> getROIPixelsInteger(ImagePlus imp, Roi roi) {
		ImageProcessor ip = imp.getProcessor();
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
	}

	private ArrayList<Byte> getROIPixelsByte(ImagePlus imp, Roi roi) {
		ImageProcessor ip = imp.getProcessor();
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
	}

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
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String st = store.getString(PreferenceConstants.D_OPENOFFICE_HEAD);
		String[] a = st.split(",");

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
