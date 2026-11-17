/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.pybridge;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.util.Util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * The transfers of the Image-Methods view to the embedded Python (when Python
 * is selected in the view): numpy arrays and pandas DataFrames with the names
 * of the transfers to R. Called in the UI thread; the transfers run in jobs.
 */
public final class PythonImageTransfer {

	/* The data types of the combo of the view: Double, Integer, Byte, RGB Byte */
	private static final String[] DTYPES = { "f8", "i4", "u1", "u1" };

	private PythonImageTransfer() {
	}

	/** Starts the embedded Python if necessary (UI thread); false (with a message) if not available. */
	public static boolean ensurePython() {
		if (EmbeddedPython.isRunning()) {
			return true;
		}
		String[] error = new String[1];
		/* In a job: the user interface stays usable during the start (e.g., a slow computer) */
		error[0] = com.eco.bio7.rcp.BackgroundStart.run("Starting Python", EmbeddedPython::start);
		if (error[0] != null) {
			Bio7Dialog.message("Python could not be started:\n" + error[0]);
			return false;
		}
		return true;
	}

	/* Runs a transfer in a job: the value is assigned to the name */
	private interface Transfer {
		Map<String, PyValue> values() throws Exception;
	}

	private static void run(String title, Transfer transfer) {
		if (!ensurePython()) {
			return;
		}
		Job job = new Job(title) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
				try {
					Map<String, PyValue> values = transfer.values();
					if (values == null) {
						return Status.OK_STATUS;
					}
					PySession py = EmbeddedPython.session();
					StringBuilder names = new StringBuilder();
					for (Map.Entry<String, PyValue> e : values.entrySet()) {
						py.assign(e.getKey(), e.getValue());
						names.append(names.isEmpty() ? "" : ", ").append(e.getKey()).append(describe(e.getValue()));
					}
					EmbeddedPython.print("\nTransferred to Python: " + names + "\n", false);
					PythonShellView.refreshOpenViews();
					EmbeddedPython.prompt();
				} catch (Exception e) {
					EmbeddedPython.print("\n" + title + ": " + e.getMessage() + "\n", true);
					EmbeddedPython.prompt();
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private static String describe(PyValue v) {
		return switch (v) {
		case PyValue.Array a -> " (" + a.dtype() + " " + java.util.Arrays.toString(a.shape()) + ")";
		case PyValue.Frame f -> " (DataFrame " + f.length() + " rows, " + f.columns().size() + " columns)";
		case PyValue.PyList l -> " (list of " + l.length() + ")";
		default -> "";
		};
	}

	private static ImagePlus currentImage() throws Exception {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			throw new IllegalStateException("No image opened!");
		}
		return imp;
	}

	private static Map<String, PyValue> one(String name, PyValue value) {
		Map<String, PyValue> m = new LinkedHashMap<>();
		m.put(name, value);
		return m;
	}

	/* ---------------- The transfers ---------------- */

	/** "IJ RT": the results table as DataFrame IJResultsTable. */
	public static void resultsTable() {
		run("Transfer to Python", () -> {
			ResultsTable rt = ResultsTable.getResultsTable();
			if (rt == null || rt.size() == 0) {
				throw new IllegalStateException("The ImageJ results table is empty");
			}
			return one("IJResultsTable", PythonConvert.resultsTable(rt));
		});
	}

	/** "IJ Stack": the current image (stack) as numpy array imageStack (slices, height, width). */
	public static void stack() {
		run("Transfer ImageJ Stack", () -> one("imageStack", PythonConvert.image(currentImage())));
	}

	/** "ROI": the coordinates of the current selection (selectionx, selectiony). */
	public static void roi() {
		run("Transfer to Python", () -> {
			ImagePlus imp = currentImage();
			Roi roi = imp.getRoi();
			if (roi == null) {
				throw new IllegalStateException("The image has no selection");
			}
			PyValue.PyDict xy = PythonConvert.roi(roi);
			Map<String, PyValue> m = new LinkedHashMap<>();
			m.put("selectionx", xy.get("x"));
			m.put("selectiony", xy.get("y"));
			return m;
		});
	}

	/** "Particles": the particle analysis of ImageJ, the results as DataFrame Particles. */
	public static void particles() {
		if (!ensurePython()) {
			return;
		}
		if (WindowManager.getCurrentImage() == null) {
			Bio7Dialog.message("No image opened!");
			return;
		}
		/* The dialog of the analysis (macro) runs in its own thread */
		Thread t = new Thread(() -> {
			IJ.runMacro("run(\"Analyze Particles...\")");
			run("Transfer to Python", () -> {
				ResultsTable rt = ResultsTable.getResultsTable();
				if (rt == null || rt.size() == 0) {
					throw new IllegalStateException("No particles in the results table (select 'Display results')");
				}
				return one("Particles", PythonConvert.resultsTable(rt));
			});
		}, "Particle analysis");
		t.start();
	}

	/** "Pixel": the pixels of the selection (each slice a column) with an optional class. */
	public static void pixels(int transferType) {
		String[] name = { "imageSelection" };
		String[] signature = { "" };
		if (!askNameAndClass(name, signature)) {
			return;
		}
		run("Transfer Selection to Python", () -> {
			ImagePlus imp = currentImage();
			Roi roi = imp.getRoi();
			if (roi == null) {
				throw new IllegalStateException("No selection available!");
			}
			List<Roi> rois = List.of(roi);
			Integer cls = signature[0].isBlank() ? null : Integer.parseInt(signature[0].trim());
			return one(name[0], pixelFrame(imp, rois, cls == null ? null : new int[] { cls }, transferType));
		});
	}

	/**
	 * "Pixel RM" (as for R): the pixels of the selections of the ROI Manager (the
	 * selected ones, else all) in all opened images (the tabs of the ImageJ view,
	 * of a stack the current slice): a DataFrame per selection (named as the
	 * selection) with a column per image (layer1, layer2, ...) and the class of the
	 * dialog; roi_pixels: all selections (column ROI).
	 */
	public static void pixelsRoiManager(int transferType) {
		if (transferType == 3) {
			Bio7Dialog.message("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the (layer) selection in e.g. byte mode!");
			return;
		}
		Roi[] rois = roiManagerSelections();
		ImagePlus current = WindowManager.getCurrentImage();
		if (rois == null || current == null) {
			if (current == null) {
				Bio7Dialog.message("No image available!");
			}
			return;
		}
		if (current.getStackSize() > 1) {
			Bio7Dialog.message("Info: For a stack only the first slice (layer)\n" + "will be measured for all ROI's in the ROI Manager!");
		}
		if (!ensurePython()) {
			return;
		}
		com.eco.bio7.image.ImageRoiSelectionInputDialog dialog = new com.eco.bio7.image.ImageRoiSelectionInputDialog(Util.getShell());
		dialog.open();
		if (com.eco.bio7.image.ImageRoiSelectionTransferJob.cancelJob) {
			return;
		}
		String mode = com.eco.bio7.image.ImageRoiSelectionTransferJob.signatureMode();
		int number = com.eco.bio7.image.ImageRoiSelectionTransferJob.signatureNumber();
		/* The images of the tabs of the ImageJ view (the layers) */
		List<ImageProcessor> layers = new ArrayList<>();
		for (org.eclipse.swt.custom.CTabItem item : com.eco.bio7.image.ui.CanvasView.getCanvas_view().tabFolder.getItems()) {
			ImagePlus imp = com.eco.bio7.image.ui.IJTabs.getImagePlus(item);
			if (imp != null) {
				layers.add(imp.getProcessor());
			}
		}
		run("Transfer Selections to Python", () -> roiFrames(rois, layers, null, transferType, mode, number));
	}

	/**
	 * "Pixel RM Stack" (as for R): the pixels of the selections of the ROI Manager
	 * in the slices of the current stack (the range of the dialog): a DataFrame
	 * per selection with a column per slice and the class of the dialog;
	 * roi_pixels: all selections (column ROI).
	 */
	public static void pixelsRoiManagerStack(int transferType) {
		if (transferType == 3) {
			Bio7Dialog.message("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the(slice) selection in e.g. byte mode!");
			return;
		}
		Roi[] rois = roiManagerSelections();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (rois == null || imp == null) {
			if (imp == null) {
				Bio7Dialog.message("No image available!");
			}
			return;
		}
		if (imp.getStackSize() <= 1) {
			Bio7Dialog.message("Please select a stack with more than one image!");
			return;
		}
		if (!ensurePython()) {
			return;
		}
		com.eco.bio7.image.ImageRoiStackSelectionInputDialog dialog = new com.eco.bio7.image.ImageRoiStackSelectionInputDialog(Util.getShell());
		dialog.open();
		if (com.eco.bio7.image.ImageStackRoiSelectionTransferJob.cancelJob) {
			return;
		}
		String mode = com.eco.bio7.image.ImageStackRoiSelectionTransferJob.signatureMode();
		int number = com.eco.bio7.image.ImageStackRoiSelectionTransferJob.signatureNumber();
		int from = Math.max(1, com.eco.bio7.image.ImageStackRoiSelectionTransferJob.minStackSlice());
		int to = Math.min(imp.getStackSize(), com.eco.bio7.image.ImageStackRoiSelectionTransferJob.maxStackSlice());
		ImageStack stack = imp.getStack();
		List<ImageProcessor> layers = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (int i = from; i <= to; i++) {
			layers.add(stack.getProcessor(i));
			names.add("layer" + i);
		}
		if (layers.isEmpty()) {
			Bio7Dialog.message("No slices in the selected range!");
			return;
		}
		run("Transfer Selections to Python", () -> roiFrames(rois, layers, names, transferType, mode, number));
	}

	/* The selections of the ROI Manager: the selected ones, else all (null with a message) */
	private static Roi[] roiManagerSelections() {
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) {
			Bio7Dialog.message("Please add selections to the ROI Manager!");
			return null;
		}
		Roi[] rois = rm.getSelectedRoisAsArray();
		if (rois.length < 1) {
			Bio7Dialog.message("NO ROI's available in ROI Manager!");
			return null;
		}
		return rois;
	}

	/*
	 * A DataFrame per selection (Class, layer columns) and roi_pixels with all
	 * selections. layerNames null: layer1, layer2, ...
	 */
	private static Map<String, PyValue> roiFrames(Roi[] rois, List<ImageProcessor> layers, List<String> layerNames, int transferType, String mode, int number) {
		Map<String, PyValue> result = new LinkedHashMap<>();
		String dtype = DTYPES[Math.max(0, Math.min(transferType, 2))];
		/* The columns of all selections (roi_pixels) */
		List<String> allRoi = new ArrayList<>();
		List<Integer> allClass = new ArrayList<>();
		List<List<Double>> allLayers = new ArrayList<>();
		for (int k = 0; k < layers.size(); k++) {
			allLayers.add(new ArrayList<>());
		}
		boolean anyClass = false;
		for (int r = 0; r < rois.length; r++) {
			Roi roi = rois[r];
			java.awt.Point[] points = roi.getContainedPoints();
			Integer cls = switch (mode) {
			case "fixed" -> number;
			case "increasing" -> r + 1;
			case "group" -> roi.getGroup() > 0 ? roi.getGroup() : null;
			case "name" -> classFromName(roi.getName());
			default -> null;
			};
			Map<String, PyValue> cols = new LinkedHashMap<>();
			if (cls != null) {
				int[] c = new int[points.length];
				java.util.Arrays.fill(c, cls);
				cols.put("Class", PyValue.of(c));
				anyClass = true;
			}
			String roiName = roi.getName() == null ? "roi" + (r + 1) : roi.getName();
			for (int k = 0; k < layers.size(); k++) {
				ImageProcessor ip = layers.get(k);
				double[] v = new double[points.length];
				for (int i = 0; i < points.length; i++) {
					int x = points[i].x, y = points[i].y;
					if (x < 0 || y < 0 || x >= ip.getWidth() || y >= ip.getHeight()) {
						v[i] = Double.NaN;
						continue;
					}
					v[i] = transferType == 0 ? (ip instanceof ij.process.DoubleProcessor dp ? dp.getPixelValueDouble(x, y) : ip.getPixelValue(x, y))
							: transferType == 1 ? ip.getPixel(x, y) : ip.getPixel(x, y) & 0xff;
					allLayers.get(k).add(v[i]);
				}
				cols.put(layerNames == null ? "layer" + (k + 1) : layerNames.get(k), convert(PyValue.of(v), dtype));
			}
			for (int i = 0; i < points.length; i++) {
				allRoi.add(roiName);
				allClass.add(cls == null ? -1 : cls);
			}
			String name = pythonName(roiName, result.keySet());
			result.put(name, new PyValue.Frame(cols, null));
		}
		/* All selections in one DataFrame (e.g., for pandas or scikit-learn) */
		Map<String, PyValue> all = new LinkedHashMap<>();
		all.put("ROI", PyValue.of(allRoi.toArray(String[]::new)));
		if (anyClass) {
			all.put("Class", PyValue.of(allClass.stream().mapToInt(Integer::intValue).toArray()));
		}
		for (int k = 0; k < layers.size(); k++) {
			double[] v = allLayers.get(k).stream().mapToDouble(Double::doubleValue).toArray();
			all.put(layerNames == null ? "layer" + (k + 1) : layerNames.get(k), convert(PyValue.of(v), dtype));
		}
		result.put("roi_pixels", new PyValue.Frame(all, null));
		return result;
	}

	/* The class from the name of a selection: the number after the last '_' (as for R) */
	private static Integer classFromName(String name) {
		if (name == null) {
			return null;
		}
		int i = name.lastIndexOf('_');
		if (i < 0) {
			return null;
		}
		try {
			return Integer.parseInt(name.substring(i + 1).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/* A valid Python name from the name of a selection (unique in the transfer) */
	private static String pythonName(String name, java.util.Set<String> used) {
		String n = name.replaceAll("[^A-Za-z0-9_]", "_");
		if (n.isEmpty() || Character.isDigit(n.charAt(0))) {
			n = "X" + n;
		}
		if (java.util.Set.of("roi_pixels").contains(n) || isKeyword(n)) {
			n = n + "_";
		}
		String unique = n;
		for (int i = 2; used.contains(unique); i++) {
			unique = n + "_" + i;
		}
		return unique;
	}

	private static boolean isKeyword(String n) {
		return java.util.Set.of("False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except",
				"finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield")
				.contains(n);
	}

	/** "Selection RM": the coordinates of the selections of the ROI Manager (a list of dicts). */
	public static void selectionsRoiManager() {
		run("Transfer Selections to Python", () -> {
			RoiManager rm = RoiManager.getInstance();
			if (rm == null || rm.getCount() == 0) {
				throw new IllegalStateException("The ROI Manager has no selections");
			}
			List<PyValue> list = new ArrayList<>();
			for (Roi roi : rm.getRoisAsArray()) {
				PyValue.PyDict xy = PythonConvert.roi(roi);
				Map<String, PyValue> d = new LinkedHashMap<>();
				d.put("name", PyValue.of(roi.getName() == null ? "" : roi.getName()));
				d.putAll(xy.values());
				list.add(new PyValue.PyDict(d));
			}
			return one("roiSelections", new PyValue.PyList(list));
		});
	}

	/** "Pic&lt;-": the current image as numpy array (the data type of the view). */
	public static void imageToPython(String name, int transferType) {
		run("Transfer Image to Python", () -> {
			ImagePlus imp = currentImage();
			PyValue.Array a = PythonConvert.image(imp);
			boolean rgb = imp.getType() == ImagePlus.COLOR_RGB;
			if (!(rgb && transferType == 3)) {
				if (rgb) {
					/* The luminance of an RGB image */
					ImageStack gray = new ImageStack(imp.getWidth(), imp.getHeight());
					for (int s = 1; s <= imp.getStackSize(); s++) {
						gray.addSlice(null, imp.getStack().getProcessor(s).convertToFloat());
					}
					a = PythonConvert.image(new ImagePlus("", gray));
				}
				a = convert(a, DTYPES[Math.max(0, Math.min(transferType, 3))]);
			}
			return one(name, a);
		});
	}

	/**
	 * "Pic-&gt;": an ImageJ image from a numpy array.
	 *
	 * @param imageType the combo of the view: 0 Colour, 1 Greyscale, 2 Float, 3
	 *                  Short, 4 Double (Float)
	 */
	public static void imageFromPython(String name, int imageType) {
		if (!ensurePython()) {
			return;
		}
		Job job = new Job("Image from Python") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					PyValue v = EmbeddedPython.session().eval(name);
					PyValue.Array a;
					try {
						a = PythonConvert.toArray(v);
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException(name + ": " + e.getMessage());
					}
					String dtype = switch (imageType) {
					case 0, 1 -> "u1";
					case 3 -> "u2";
					default -> "f4";
					};
					if (imageType == 0 && !(a.shape().length >= 3 && a.shape()[a.shape().length - 1] == 3)) {
						throw new IllegalArgumentException("A colour image needs an array (height, width, 3)");
					}
					ImagePlus imp = PythonConvert.toImage(convert(a, dtype), name);
					Display.getDefault().asyncExec(imp::show);
				} catch (Exception e) {
					EmbeddedPython.print("\nImage from Python: " + e.getMessage() + "\n", true);
					EmbeddedPython.prompt();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/* ---------------- Helpers ---------------- */

	private static boolean askNameAndClass(String[] name, String[] signature) {
		InputDialog n = new InputDialog(Util.getShell(), "To Python", "The name of the data:", name[0], s -> s.matches("[A-Za-z_][A-Za-z0-9_]*") ? null : "Not a valid Python name");
		if (n.open() != Window.OK) {
			return false;
		}
		name[0] = n.getValue();
		InputDialog c = new InputDialog(Util.getShell(), "To Python", "The class of the pixels (a column 'Class'; empty: no class):", "",
				s -> s.isBlank() || s.trim().matches("-?\\d+") ? null : "An integer is expected");
		if (c.open() != Window.OK) {
			return false;
		}
		signature[0] = c.getValue();
		return true;
	}

	/*
	 * The pixels of selections: a DataFrame with the column Class (optional) and a
	 * column per slice (RGB: R, G, B per slice)
	 */
	private static PyValue.Frame pixelFrame(ImagePlus imp, List<Roi> rois, int[] classes, int transferType) {
		ImageStack stack = imp.getStack();
		int n = stack.getSize();
		boolean rgb = imp.getType() == ImagePlus.COLOR_RGB;
		List<int[]> points = new ArrayList<>();
		List<Integer> pointClass = new ArrayList<>();
		for (int r = 0; r < rois.size(); r++) {
			Roi roi = rois.get(r);
			Rectangle b = roi.getBounds();
			ImageProcessor mask = roi.getMask();
			for (int y = b.y; y < b.y + b.height; y++) {
				for (int x = b.x; x < b.x + b.width; x++) {
					if (x < 0 || y < 0 || x >= imp.getWidth() || y >= imp.getHeight()) {
						continue;
					}
					if (mask == null || mask.getPixel(x - b.x, y - b.y) != 0) {
						points.add(new int[] { x, y });
						pointClass.add(classes == null ? 0 : classes[Math.min(r, classes.length - 1)]);
					}
				}
			}
		}
		Map<String, PyValue> cols = new LinkedHashMap<>();
		if (classes != null) {
			int[] c = new int[points.size()];
			for (int i = 0; i < c.length; i++) {
				c[i] = pointClass.get(i);
			}
			cols.put("Class", PyValue.of(c));
		}
		String dtype = DTYPES[Math.max(0, Math.min(transferType, 3))];
		for (int s = 1; s <= n; s++) {
			ImageProcessor ip = stack.getProcessor(s);
			String suffix = n == 1 ? "" : String.valueOf(s);
			if (rgb) {
				for (int k = 0; k < 3; k++) {
					double[] v = new double[points.size()];
					for (int i = 0; i < v.length; i++) {
						int c = ip.getPixel(points.get(i)[0], points.get(i)[1]);
						v[i] = (c >> (16 - 8 * k)) & 0xff;
					}
					cols.put("RGB".charAt(k) + suffix, convert(PyValue.of(v), dtype));
				}
			} else {
				double[] v = new double[points.size()];
				for (int i = 0; i < v.length; i++) {
					v[i] = ip.getPixelValue(points.get(i)[0], points.get(i)[1]);
				}
				cols.put(n == 1 ? "Value" : "Slice" + s, convert(PyValue.of(v), dtype));
			}
		}
		return new PyValue.Frame(cols, null);
	}

	/* An array in another data type (u1 and u2 are clamped) */
	static PyValue.Array convert(PyValue.Array a, String dtype) {
		if (a.dtype().equals(dtype)) {
			return a;
		}
		int len = a.length();
		Object data = switch (dtype) {
		case "f8" -> a.asDoubles();
		case "f4" -> {
			float[] f = new float[len];
			for (int i = 0; i < len; i++) {
				f[i] = (float) a.getDouble(i);
			}
			yield f;
		}
		case "i4" -> a.asInts();
		case "u1" -> {
			byte[] b = new byte[len];
			for (int i = 0; i < len; i++) {
				b[i] = (byte) Math.max(0, Math.min(255, Math.round(a.getDouble(i))));
			}
			yield b;
		}
		case "u2" -> {
			short[] s = new short[len];
			for (int i = 0; i < len; i++) {
				s[i] = (short) Math.max(0, Math.min(65535, Math.round(a.getDouble(i))));
			}
			yield s;
		}
		default -> throw new IllegalArgumentException("Unknown dtype: " + dtype);
		};
		return new PyValue.Array(data, dtype, a.shape());
	}
}
